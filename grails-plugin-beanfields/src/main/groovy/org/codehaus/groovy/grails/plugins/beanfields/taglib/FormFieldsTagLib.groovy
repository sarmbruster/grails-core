package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.artefact.Artefact
import javax.annotation.PostConstruct
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator
import org.springframework.beans.PropertyAccessorFactory
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.beanfields.*

@Artefact("TagLibrary")
class FormFieldsTagLib implements GrailsApplicationAware {

	static namespace = "form"

    GrailsApplication grailsApplication
    GrailsConventionGroovyPageLocator groovyPageLocator
	BeanPropertyAccessorFactory beanPropertyAccessorFactory

	@PostConstruct void initialize() {
		beanPropertyAccessorFactory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)
	}

    Closure field = { attrs ->
        if (!attrs.bean) throwTagError("Tag [field] is missing required attribute [bean]")
        if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

        def bean = resolveBean(attrs)
        def propertyPath = attrs.property
        def propertyAccessor = beanPropertyAccessorFactory.accessorFor(bean, propertyPath)

        def template = resolveTemplate(propertyAccessor)

        def model = [:]
        model.bean = propertyAccessor.rootBean
        model.property = propertyAccessor.pathFromRoot
        model.label = resolveLabelText(propertyAccessor, attrs)
        model.value = attrs.value ?: propertyAccessor.value ?: attrs.default
        model.constraints = propertyAccessor.constraints
        model.errors = propertyAccessor.errors.collect { message(error: it) }

        out << render(template: template, model: model)
    }

    // TODO: cache the result of this lookup
    private def resolveTemplate(BeanPropertyAccessor propertyAccessor) {
        // order of priority for template resolution
        // 1: grails-app/views/controller/<property>/_field.gsp
        // 2: grails-app/views/forms/<class>.<property>/_field.gsp
        // 3: grails-app/views/forms/<type>/_field.gsp
        // 4: grails-app/views/forms/default/_field.gsp
        // TODO: implications for templates supplied by plugins
        def templateResolveOrder = []
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/", controllerName, propertyAccessor.propertyName, "field")
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", propertyAccessor.beanClass.propertyName, propertyAccessor.propertyName, "field")
        templateResolveOrder << GrailsResourceUtils.appendPiecesForUri("/forms", propertyAccessor.type.name, "field")
        templateResolveOrder << "/forms/default/field"

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

    private String resolveLabelText(BeanPropertyAccessor propertyAccessor, Map attrs) {
        def label = attrs.label
        if (!label && attrs.labelKey) {
            label = message(code: attrs.labelKey)
        }
        label ?: message(code: propertyAccessor.labelKey, default: propertyAccessor.defaultLabel)
    }
}

