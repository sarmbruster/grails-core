package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.springframework.datastore.mapping.model.PersistentProperty
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
        def beanClass = bean.getClass()
        def domainClass = resolveDomainClass(beanClass)
        def propertyName = attrs.property
        def property = resolveProperty(bean, propertyName)
        def persistentProperty = property.persistentProperty
        def value = property.value
        def type = persistentProperty.type

        // order of priority for template resolution
        // 1: grails-app/views/controller/_<property>.gsp
        // 2: grails-app/views/fields/_<class>.<property>.gsp
        // 3: grails-app/views/fields/_<type>.gsp
        // 4: grails-app/views/fields/_default.gsp
        // TODO: implications for templates supplied by plugins
        // TODO: recursive resolution for embedded types

        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views', controllerName, propertyName)
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', "${beanClass.name}.$propertyName".toString())
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', type.name)
        templateResolveOrder << '/grails-app/views/fields/default'

        // TODO: this is doing the entire resolution twice, we need to make some of the internal functionality of GroovyPagesTemplateEngine and RenderTagLib more accessible so that it can be shared by this code
        def template = templateResolveOrder.find {
            def gspPath = grailsAttributes.getTemplateUri(it, request)
            groovyPagesTemplateEngine.createTemplateForUri([gspPath] as String[]) != null
        }

        def model = [:]
        model.bean = bean
        model.property = propertyName
        model.label = resolveLabelText(persistentProperty, attrs)
        model.value = attrs.value ?: value ?: attrs.default
        model.constraints = domainClass.constrainedProperties[propertyName]
        model.errors = bean.errors.getFieldErrors(propertyName).collect { message(error: it) }

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

    private Map<String, Object> resolvePropertyFromPathComponents(BeanWrapper bean, GrailsDomainClass domainClass, List<String> path) {
        def propertyName = path.remove(0)
        def persistentProperty = domainClass.getPersistentProperty(propertyName)
        def value = bean.getPropertyValue(propertyName)
        if (path.empty) {
            [persistentProperty: persistentProperty, value: value]
        } else {
            resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(value), persistentProperty.component, path)
        }
    }

    private String resolveLabelText(GrailsDomainClassProperty property, Map attrs) {
        def label = attrs.label
        if (!label && attrs.labelKey) {
            label = message(code: attrs.labelKey)
        }
        label ?: message(code: "${property.domainClass.name}.${property.name}.label", default: property.naturalName)
    }
}