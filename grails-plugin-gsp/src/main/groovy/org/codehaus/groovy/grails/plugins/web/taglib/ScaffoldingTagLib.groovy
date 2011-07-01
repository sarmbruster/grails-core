package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware

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
        def propertyAccessor = org.codehaus.groovy.grails.plugins.beanfields.BeanPropertyAccessor.forBeanAndPath(grailsApplication, bean, propertyPath)

        // order of priority for template resolution
        // 1: grails-app/views/controller/_<property>.gsp
        // 2: grails-app/views/fields/_<class>.<property>.gsp
        // 3: grails-app/views/fields/_<type>.gsp
        // 4: grails-app/views/fields/_default.gsp
        // TODO: implications for templates supplied by plugins
        // TODO: recursive resolution for embedded types

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

    private def resolveTemplate(def propertyAccessor) {
        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views', controllerName, propertyAccessor.pathFromRoot)
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', "${propertyAccessor.beanType.name}.${propertyAccessor.propertyName}".toString())
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', propertyAccessor.type.name)
        templateResolveOrder << '/grails-app/views/fields/default'

        // TODO: this is doing the entire resolution twice, we need to make some of the internal functionality of GroovyPagesTemplateEngine and RenderTagLib more accessible so that it can be shared by this code
        def template = templateResolveOrder.find {
            def gspPath = grailsAttributes.getTemplateUri(it, request)
            groovyPagesTemplateEngine.createTemplateForUri([gspPath] as String[]) != null
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

