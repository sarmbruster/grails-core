package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.test.mixin.TestFor
import grails.util.Environment
import spock.lang.*

@TestFor(FormFieldsTagLib)
class FormFieldsTagLibSpec extends Specification {

	def setup() {
		Object.metaClass.encodeAsHTML = {-> delegate } // TODO: need to tear this down properly
	}

	@Unroll({"input for a $type.name property matches '$outputPattern'"})
	def "input types"() {
		given:
		def model = [type: type, property: "prop", constraints: [:]]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		type    | outputPattern
		String  | /input type="text"/
		Boolean | /input type="checkbox"/
		boolean | /input type="checkbox"/
		int     | /input type="number"/
		Integer | /input type="number"/
		URL     | /input type="url"/
		Byte[]  | /input type="file"/
		byte[]  | /input type="file"/
	}

	@Unroll({"input for ${required ? 'a required' : 'an optional'} property ${required ? 'has' : 'does not have'} the required attribute"})
	def "required attribute"() {
		given:
		def model = [type: String, property: "prop", required: required, constraints: [:]]

		expect:
		tagLib.renderInput(model).contains('required=""') || !required

		where:
		required << [true, false]
	}

	@Unroll({"input for ${invalid ? 'an invalid' : 'a valid'} property ${invalid ? 'has' : 'does not have'} the invalid attribute"})
	def "invalid attribute"() {
		given:
		def model = [type: String, property: "prop", invalid: invalid, constraints: [:]]

		expect:
		tagLib.renderInput(model).contains('invalid=""') || !invalid

		where:
		invalid << [true, false]
	}

	def "input for an enum property is a select"() {
		given:
		def model = [type: Environment, property: "prop", constraints: [:]]

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
		def model = [type: type, property: "prop", constraints: [:]]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		type          | outputPattern
		Date          | /select name="prop_day"/
		Calendar      | /select name="prop_day"/
		java.sql.Date | /select name="prop_day"/
		java.sql.Time | /select name="prop_day"/
		TimeZone      | /<option value="Europe\/London"/
		Locale        | /<option value="en_GB"/
		Currency      | /<option value="GBP"/
	}

	@Unroll({"input for a String property with $constraints constraints matches $outputPattern"})
	def "input types for String properties are appropriate for constraints"() {
		given:
		def model = [type: String, property: "prop", constraints: constraints]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		constraints      | outputPattern
		[email: true]    | /type="email"/
		[url: true]      | /type="url"/
		[password: true] | /type="password"/
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

	@Unroll({"input for a $type property with $constraints constraints matches $outputPattern"})
	def "inputs have constraint-driven attributes where appropriate"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints]

		expect:
		tagLib.renderInput(model) =~ outputPattern

		where:
		type   | constraints       | outputPattern
		int    | [min: 0]          | /min="0"/
		int    | [max: 10]         | /max="10"/
		int    | [min: 0, max: 10] | /min="0"/
		int    | [min: 0, max: 10] | /max="10"/
		String | [maxSize: 32]     | /maxlength="32"/
		String | [matches: /\d+/]  | /pattern="\\d\+"/
		String | [editable: false] | /readonly=""/
	}

	@Unroll({"input for a $type property with an inList constraint of $inListConstraint is a select"})
	def "inputs for properties with inList constraint are selects"() {
		given:
		def model = [type: type, property: "prop", constraints: [inList: inListConstraint]]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /select name="prop"/
		inListConstraint.every {
			output =~ /option value="$it"/
		}

		where:
		type   | inListConstraint
		int    | [1, 3, 5]
		String | ["catflap", "rubberplant", "marzipan"]
	}

	@Unroll({"input for an optional $type property ${constraints ? "with $constraints constraints " : ''}has a no-selection option"})
	def "optional properties with select inputs have a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /<option value=""><\/option>/

		where:
		type        | constraints
		Environment | [:]
		int         | [inList: [1, 3, 5]]
		String      | [inList: ["catflap", "rubberplant", "marzipan"]]
	}

	@Unroll({"input for a required $type property ${constraints ? "with $constraints constraints " : ''}does not have a no-selection option"})
	def "required properties with select inputs have a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, required: true]

		when:
		def output = tagLib.renderInput(model)

		then:
		!(output =~ /<option value=""><\/option>/)

		where:
		type        | constraints
		Environment | [:]
		int         | [inList: [1, 3, 5]]
		String      | [inList: ["catflap", "rubberplant", "marzipan"]]
	}

}
