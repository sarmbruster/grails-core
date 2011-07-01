package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import java.util.regex.Pattern
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory

@Artefact("TagLibrary")
class ScaffoldingTagLib implements GrailsApplicationAware {

    GrailsApplication grailsApplication
    GrailsConventionGroovyPageLocator groovyPageLocator

    Closure scaffoldInput = { attrs ->
        if (!attrs.bean) throwTagError("Tag [scaffoldInput] is missing required attribute [bean]")
        if (!attrs.property) throwTagError("Tag [scaffoldInput] is missing required attribute [property]")

        def bean = resolveBean(attrs)
        def propertyPath = attrs.property
        def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, bean, propertyPath)

        def template = resolveTemplate(propertyAccessor)

        def model = [:]
        model.bean = bean
        model.property = propertyPath
        model.label = resolveLabelText(propertyAccessor.persistentProperty, attrs)
        model.value = attrs.value ?: propertyAccessor.value ?: attrs.default
        model.constraints = propertyAccessor.constraints
        model.errors = bean.errors.getFieldErrors(propertyPath).collect { message(error: it) }

        out << render(template: template, model: model)
    }

    // TODO: cache the result of this lookup
    private def resolveTemplate(BeanPropertyAccessor propertyAccessor) {
        // order of priority for template resolution
        // 1: grails-app/views/controller/_<property>.gsp
        // 2: grails-app/views/fields/_<class>.<property>.gsp
        // 3: grails-app/views/fields/_<type>.gsp
        // 4: grails-app/views/fields/_default.gsp
        // TODO: implications for templates supplied by plugins
        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/", controllerName, propertyAccessor.pathFromRoot)
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", "${propertyAccessor.beanType.name}.${propertyAccessor.propertyName}".toString())
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/fields", propertyAccessor.type.name)
        templateResolveOrder << "/fields/default"

        def template = templateResolveOrder.find {
            groovyPageLocator.findTemplateByPath(it)
        }
        return template
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

class BeanPropertyAccessor {

    private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

    final GrailsDomainClass rootDomainClass
    final String pathFromRoot
    final GrailsDomainClass beanClass
    final String propertyName
    final value

    static BeanPropertyAccessor forBeanAndPath(GrailsApplication grailsApplication, bean, String property) {
        def domainClass = resolveDomainClass(grailsApplication, bean.getClass())
        def pathElements = property.tokenize(".")
        resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), domainClass, property, domainClass, pathElements)
    }

    private static BeanPropertyAccessor resolvePropertyFromPathComponents(BeanWrapper bean, GrailsDomainClass rootClass, String pathFromRoot, GrailsDomainClass domainClass, List<String> pathElements) {
        def propertyName = pathElements.remove(0)
        def value = bean.getPropertyValue(propertyName)
        if (pathElements.empty) {
            def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (matcher.matches()) {
                new BeanPropertyAccessor(rootClass, pathFromRoot, domainClass, matcher[0][1], value)
            } else {
                new BeanPropertyAccessor(rootClass, pathFromRoot, domainClass, propertyName, value)
            }
        } else {
            def persistentProperty
            def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (matcher.matches()) {
                persistentProperty = domainClass.getPersistentProperty(matcher[0][1])
            } else {
                persistentProperty = domainClass.getPersistentProperty(propertyName)
            }
            def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
            resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(value), rootClass, pathFromRoot, propertyDomainClass, pathElements)
        }
    }

    private static GrailsDomainClass resolvePropertyDomainClass(GrailsDomainClassProperty persistentProperty) {
        if (persistentProperty.embedded) {
            persistentProperty.component
        } else if (persistentProperty.association) {
            persistentProperty.referencedDomainClass
        } else {
            null
        }
    }

    private static GrailsDomainClass resolveDomainClass(grailsApplication, Class beanClass) {
        grailsApplication.getArtefact("Domain", beanClass.simpleName)
    }

    private BeanPropertyAccessor(GrailsDomainClass rootDomainClass, String pathFromRoot, GrailsDomainClass beanClass, String propertyName, value) {
        this.rootDomainClass = rootDomainClass
        this.pathFromRoot = pathFromRoot
        this.beanClass = beanClass
        this.propertyName = propertyName
        this.value = value
    }

    Class getRootBeanType() {
        rootDomainClass.clazz
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