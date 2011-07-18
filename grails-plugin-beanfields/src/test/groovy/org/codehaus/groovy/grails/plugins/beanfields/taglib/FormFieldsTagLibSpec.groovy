package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(FormFieldsTagLib)
class FormFieldsTagLibSpec extends Specification {

	def setup() {
		String.metaClass.encodeAsHTML = { -> delegate }
	}

	def "input types"() {
		given:
		def model = [type: String, property: "name", value: "Bart Simpson"]

		expect:
		tagLib.renderInput(model) == '<input type="text" name="name" value="Bart Simpson" required="" id="name" />'
	}

}
