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

	@PostConstruct
	void initialize() {
		beanPropertyAccessorFactory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)
	}

	Closure field = { attrs ->
		def propertyAccessor = resolveProperty(attrs)
		def template = resolveFieldTemplate(propertyAccessor)
		def model = buildModel(propertyAccessor, attrs)
		model.input = renderInput(model)

		out << render(template: template, model: model)
	}

	Closure input = { attrs ->
		def propertyAccessor = resolveProperty(attrs)
		def model = buildModel(propertyAccessor, attrs)
		out << renderInput(model)
	}

	private Map buildModel(BeanPropertyAccessor propertyAccessor, attrs) {
		[
				bean: propertyAccessor.rootBean,
				property: propertyAccessor.pathFromRoot,
				type: propertyAccessor.type,
				label: resolveLabelText(propertyAccessor, attrs),
				value: attrs.value ?: propertyAccessor.value ?: attrs.default,
				constraints: propertyAccessor.constraints,
				errors: propertyAccessor.errors.collect { message(error: it) },
				required: attrs.containsKey("required") ? Boolean.valueOf(attrs.required) : propertyAccessor.required,
				invalid: attrs.containsKey("invalid") ? Boolean.valueOf(attrs.invalid) : propertyAccessor.invalid,
		]
	}

	private BeanPropertyAccessor resolveProperty(attrs) {
		if (!attrs.bean) throwTagError("Tag [field] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [field] is missing required attribute [property]")

		def bean = resolveBean(attrs)
		def propertyPath = attrs.property
		def propertyAccessor = beanPropertyAccessorFactory.accessorFor(bean, propertyPath)
		return propertyAccessor
	}

	private String renderInput(Map attrs) {
		def model = [:]
		model.name = attrs.property
		model.value = attrs.value
		if (attrs.required) model.required = ""
		switch (attrs.type) {
			case String:
				return g.textField(model)
			case boolean:
			case Boolean:
				return g.checkBox(model)
		}
	}

	// TODO: cache the result of this lookup
	private String resolveFieldTemplate(BeanPropertyAccessor propertyAccessor) {
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

