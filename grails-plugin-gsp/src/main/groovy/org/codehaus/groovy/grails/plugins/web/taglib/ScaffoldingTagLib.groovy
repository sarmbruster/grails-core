package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

@Artefact("TagLibrary")
class ScaffoldingTagLib implements GrailsApplicationAware {

	GrailsApplication grailsApplication
    def groovyPagesTemplateEngine

	Closure scaffoldInput = { attrs ->
		if (!attrs.bean) throwTagError("Tag [scaffoldInput] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [scaffoldInput] is missing required attribute [property]")
		
		def bean = attrs.bean
		def property = attrs.property
		def value = bean."$property"
		def model = [property: property, value: value]
		
		def type = getPropertyType(bean, property)
		def template = grailsApplication.config.scaffolding.template.default[type.name]
		if (!template) {
            // attempt to use a template in the controller's view directory
            template = property
        }
	
		try {
			out << render(template: template, bean: bean, model: model)
		} catch (GrailsTagException e) {
			out << textField(name: property, value: value)
		}
	}
	
	private Class getPropertyType(Object bean, String property) {
		def dc = grailsApplication.getArtefact("Domain", bean.getClass().simpleName)
		dc.getPersistentProperty(property).type
	}
}