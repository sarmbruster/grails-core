package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import java.util.regex.Pattern
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory

@Artefact("TagLibrary")
class ScaffoldingTagLib implements GrailsApplicationAware {

    GrailsApplication grailsApplication
    def groovyPagesTemplateEngine

    Closure scaffoldInput = { attrs ->
        if (!attrs.bean) throwTagError("Tag [scaffoldInput] is missing required attribute [bean]")
        if (!attrs.property) throwTagError("Tag [scaffoldInput] is missing required attribute [property]")

        def bean = resolveBean(attrs)
        def propertyPath = attrs.property
        def propertyResolver = PropertyResolver.forBeanAndPath(grailsApplication, bean, propertyPath)

        // order of priority for template resolution
        // 1: grails-app/views/controller/_<property>.gsp
        // 2: grails-app/views/fields/_<class>.<property>.gsp
        // 3: grails-app/views/fields/_<type>.gsp
        // 4: grails-app/views/fields/_default.gsp
        // TODO: implications for templates supplied by plugins
        // TODO: recursive resolution for embedded types

        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views', controllerName, propertyPath)
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', "${propertyResolver.beanType.name}.${propertyResolver.propertyName}".toString())
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', propertyResolver.type.name)
        templateResolveOrder << '/grails-app/views/fields/default'

        // TODO: this is doing the entire resolution twice, we need to make some of the internal functionality of GroovyPagesTemplateEngine and RenderTagLib more accessible so that it can be shared by this code
        def template = templateResolveOrder.find {
            def gspPath = grailsAttributes.getTemplateUri(it, request)
            groovyPagesTemplateEngine.createTemplateForUri([gspPath] as String[]) != null
        }

        def model = [:]
        model.bean = bean
        model.property = propertyPath
        model.label = resolveLabelText(propertyResolver.persistentProperty, attrs)
        model.value = attrs.value ?: propertyResolver.value ?: attrs.default
        model.constraints = propertyResolver.constraints
        model.errors = bean.errors.getFieldErrors(propertyPath).collect { message(error: it) }

        out << render(template: template, model: model)
    }

    private resolveBean(Map attrs) {
        pageScope.variables[attrs.bean] ?: attrs.bean
    }

    private GrailsDomainClass resolveDomainClass(bean) {
        resolveDomainClass(bean.getClass())
    }

    private GrailsDomainClass resolveDomainClass(Class beanClass) {
        grailsApplication.getArtefact("Domain", beanClass.simpleName)
    }

    private Map<String, Object> resolveProperty(bean, String property) {
        def domainClass = resolveDomainClass(bean.getClass())
        def path = property.tokenize(".")
        resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), domainClass, path)
    }

    private String resolveLabelText(GrailsDomainClassProperty property, Map attrs) {
        def label = attrs.label
        if (!label && attrs.labelKey) {
            label = message(code: attrs.labelKey)
        }
        label ?: message(code: "${property.domainClass.name}.${property.name}.label", default: property.naturalName)
    }
}

class PropertyResolver {

    private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(\d+)\]$/

    final GrailsDomainClass rootBeanClass
    final String pathFromRoot
    final GrailsDomainClass beanClass
    final String propertyName
    final value

    static PropertyResolver forBeanAndPath(GrailsApplication grailsApplication, bean, String property) {
        def domainClass = resolveDomainClass(grailsApplication, bean.getClass())
        def path = property.tokenize(".")
        resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), domainClass, property, domainClass, path)
    }

    private static PropertyResolver resolvePropertyFromPathComponents(BeanWrapper bean, GrailsDomainClass rootClass, String pathFromRoot, GrailsDomainClass domainClass, List<String> path) {
        def propertyName = path.remove(0)
        def value = bean.getPropertyValue(propertyName)
        if (path.empty) {
            new PropertyResolver(rootClass, pathFromRoot, domainClass, propertyName, value)
        } else {
            def persistentProperty
            def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (matcher.matches()) {
                persistentProperty = domainClass.getPersistentProperty(matcher[0][1])
            } else {
                persistentProperty = domainClass.getPersistentProperty(propertyName)
            }
            def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
            resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(value), rootClass, pathFromRoot, propertyDomainClass, path)
        }
    }

    private static GrailsDomainClass resolvePropertyDomainClass(GrailsDomainClassProperty persistentProperty) {
        if (persistentProperty.association) {
            persistentProperty.referencedDomainClass
        } else if (persistentProperty.embedded) {
            persistentProperty.component
        } else {
            null
        }
    }

    private static GrailsDomainClass resolveDomainClass(grailsApplication, Class beanClass) {
        grailsApplication.getArtefact("Domain", beanClass.simpleName)
    }

    private PropertyResolver(GrailsDomainClass rootBeanClass, String pathFromRoot, GrailsDomainClass beanClass, String propertyName, value) {
        this.rootBeanClass = rootBeanClass
        this.pathFromRoot = pathFromRoot
        this.beanClass = beanClass
        this.propertyName = propertyName
        this.value = value
    }

    Class getRootBeanType() {
        rootBeanClass.clazz
    }

    Class getBeanType() {
        beanClass.clazz
    }

    Class getType() {
        persistentProperty.type
    }

    GrailsDomainClassProperty getPersistentProperty() {
        beanClass.getPersistentProperty(propertyName)
    }

    ConstrainedProperty getConstraints() {
        beanClass.constrainedProperties[propertyName]
    }

}