package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.test.mixin.TestFor
import grails.util.Environment
import spock.lang.*

@TestFor(FormFieldsTagLib)
class FormFieldsTagLibSpec extends Specification {

	def setup() {
		Object.metaClass.encodeAsHTML = {-> delegate }
	}

	@Unroll({"input for a $type.name property matches '$outputPattern'"})
	def "input types"() {
		given:
		def model = [type: type, property: "prop", constraints: [:]]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		type          | outputPattern
		String        | /input type="text"/
		Boolean       | /input type="checkbox"/
		boolean       | /input type="checkbox"/
		int           | /input type="number"/
		Integer       | /input type="number"/
		URL           | /input type="url"/
		Date          | /select name="prop_day"/
		Calendar      | /select name="prop_day"/
		java.sql.Date | /select name="prop_day"/
		java.sql.Time | /select name="prop_day"/
		Byte[]        | /input type="file"/
		byte[]        | /input type="file"/
	}

	@Unroll({"input for ${required ? 'a required' : 'an optional'} property ${required ? 'has' : 'does not have'} the required attribute"})
	def "required attribute"() {
		given:
		def model = [type: String, property: "prop", required: required]

		expect:
		tagLib.renderInput(model).contains('required=""') || !required

		where:
		required << [true, false]
	}

	def "input for an enum property is a select"() {
		given:
		def model = [type: Environment, property: "prop"]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /select name="prop"/
		Environment.values().every {
			output =~ /option value="$it"/
		}
	}

	@Unroll({"input for a $type.name property is a special select type"})
	def "special select types"() {
		given:
		def model = [type: type, property: "prop"]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		type     | outputPattern
		TimeZone | /<option value="Europe\/London"/
		Locale   | /<option value="en_GB"/
		Currency | /<option value="GBP"/
	}

	def "input for a numeric property with a range constraint is a range"() {
		given:
		def model = [type: Integer, property: "prop", constraints: [range: (0..10)]]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /input type="range"/
		output =~ /min="0"/
		output =~ /max="10"/
	}

	@Unroll({"input for a property with $constraints constraints matches $outputPattern"})
	def "inputs for numeric properties have min and max attributes where appropriate"() {
		given:
		def model = [type: Integer, property: "prop", constraints: constraints]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		constraints       | outputPattern
		[min: 0]          | /min="0"/
		[max: 10]         | /max="10"/
		[min: 0, max: 10] | /min="0"/
		[min: 0, max: 10] | /max="10"/
	}

	def "inputs for String properties with matches constraints have pattern attributes"() {

	}

}
