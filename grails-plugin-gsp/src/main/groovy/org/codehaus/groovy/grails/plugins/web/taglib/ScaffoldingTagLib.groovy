package org.codehaus.groovy.grails.plugins.web.taglib

import grails.artefact.Artefact
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils

@Artefact("TagLibrary")
class ScaffoldingTagLib {
	
    def groovyPagesTemplateEngine

	Closure scaffoldInput = { attrs ->
		if (!attrs.bean) throwTagError("Tag [scaffoldInput] is missing required attribute [bean]")
		if (!attrs.property) throwTagError("Tag [scaffoldInput] is missing required attribute [property]")
		
        def uri = grailsAttributes.getTemplateUri(attrs.property, request)
        def template = groovyPagesTemplateEngine.createTemplateForUri([uri] as String[])

		if (template) {
			out << render(template: attrs.property, bean: attrs.bean)
		}
	}
	
}