package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware

@Artefact("TagLibrary")
class ScaffoldingTagLib implements GrailsApplicationAware {

    GrailsApplication grailsApplication
    def groovyPagesTemplateEngine

    Closure scaffoldInput = { attrs ->
        if (!attrs.bean) throwTagError("Tag [scaffoldInput] is missing required attribute [bean]")
        if (!attrs.property) throwTagError("Tag [scaffoldInput] is missing required attribute [property]")

        def bean = attrs.bean
        def beanClass = bean.getClass()
        def domainClass = getDomainClass(beanClass)
        def property = attrs.property
        def persistentProperty = domainClass.getPersistentProperty(property)
        def type = persistentProperty.type

        // order of priority for template resolution
        // 1: grails-app/views/controller/_<property>.gsp
        // 2: grails-app/views/fields/_<class>.<property>.gsp
        // 3: grails-app/views/fields/_<type>.gsp
        // 4: grails-app/views/fields/_default.gsp
        // TODO: implications for templates supplied by plugins
        // TODO: recursive resolution for embedded types

        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views', controllerName, property)
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', "${beanClass.name}.$property".toString())
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri('/grails-app/views/fields', type.name)
        templateResolveOrder << '/grails-app/views/fields/default'

        // TODO: this is doing the entire resolution twice, we need to make some of the internal functionality of GroovyPagesTemplateEngine and RenderTagLib more accessible so that it can be shared by this code
        def template = templateResolveOrder.find {
            def gspPath = grailsAttributes.getTemplateUri(it, request)
            groovyPagesTemplateEngine.createTemplateForUri([gspPath] as String[]) != null
        }

        def model = [:]
        model.bean = bean
        model.property = property
        model.label = resolveLabelText(persistentProperty, attrs)
        model.value = attrs.value ?: bean."$property" ?: attrs.default
        model.constraints = domainClass.constrainedProperties[property]
        model.errors = bean.errors.getFieldErrors(property).collect { message(error: it) }

        out << render(template: template, model: model)
    }

    private String resolveLabelText(GrailsDomainClassProperty property, Map attrs) {
        def label = attrs.label
        if (!label && attrs.labelKey) {
            label = message(code: attrs.labelKey)
        }
        label ?: message(code: "${property.domainClass.name}.${property.name}.label", default: property.naturalName)
    }

    private GrailsDomainClass getDomainClass(Class beanClass) {
        grailsApplication.getArtefact("Domain", beanClass.simpleName)
    }
}