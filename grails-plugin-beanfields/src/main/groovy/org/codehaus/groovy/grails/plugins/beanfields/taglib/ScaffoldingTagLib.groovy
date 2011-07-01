package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.artefact.Artefact
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.beanfields.BeanPropertyAccessor
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
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

