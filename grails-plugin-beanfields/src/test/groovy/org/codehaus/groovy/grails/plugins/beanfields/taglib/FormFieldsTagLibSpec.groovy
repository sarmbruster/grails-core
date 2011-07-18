package org.codehaus.groovy.grails.plugins.beanfields.taglib

import grails.persistence.Entity
import grails.util.Environment
import grails.test.mixin.*
import org.codehaus.groovy.grails.commons.*
import spock.lang.*

@TestFor(FormFieldsTagLib)
@Mock(Person)
class FormFieldsTagLibSpec extends Specification {

	@Shared def basicProperty = new MockPersistentProperty()
	@Shared def oneToOneProperty = new MockPersistentProperty(oneToOne: true)
	@Shared def manyToOneProperty = new MockPersistentProperty(manyToOne: true)

	def setup() {
		Object.metaClass.encodeAsHTML = {-> delegate } // TODO: need to tear this down properly
	}

	@Unroll({"input for a $type.name property matches '$outputPattern'"})
	def "input types"() {
		given:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty]

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
		def model = [type: String, property: "prop", required: required, constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderInput(model).contains('required=""') ^ !required

		where:
		required << [true, false]
	}

	@Unroll({"input for ${invalid ? 'an invalid' : 'a valid'} property ${invalid ? 'has' : 'does not have'} the invalid attribute"})
	def "invalid attribute"() {
		given:
		def model = [type: String, property: "prop", invalid: invalid, constraints: [:], persistentProperty: basicProperty]

		expect:
		tagLib.renderInput(model).contains('invalid=""') ^ !invalid

		where:
		invalid << [true, false]
	}

	def "input for an enum property is a select"() {
		given:
		def model = [type: Environment, property: "prop", constraints: [:], persistentProperty: basicProperty]

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
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: basicProperty]

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
		def model = [type: String, property: "prop", constraints: constraints, persistentProperty: basicProperty]

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
		def model = [type: Integer, property: "prop", constraints: [range: (0..10)], persistentProperty: basicProperty]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /input type="range"/
		output =~ /min="0"/
		output =~ /max="10"/
	}

	@Unroll({"input for a $type.name property with $constraints constraints matches $outputPattern"})
	def "inputs have constraint-driven attributes where appropriate"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, persistentProperty: basicProperty]

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

	@Unroll({"input for a $type.name property with an inList constraint of $inListConstraint is a select"})
	def "inputs for properties with inList constraint are selects"() {
		given:
		def model = [type: type, property: "prop", constraints: [inList: inListConstraint], persistentProperty: basicProperty]

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

	@Unroll({"input for an optional $type.name property ${constraints ? "with $constraints constraints " : ''}has a no-selection option"})
	def "optional properties with select inputs have a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, persistentProperty: basicProperty]

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

	@Unroll({"input for a required $type.name property ${constraints ? "with $constraints constraints " : ''}has a no-selection option"})
	def "required properties with select inputs have a no-selection option"() {
		given:
		def model = [type: type, property: "prop", constraints: constraints, required: true, persistentProperty: basicProperty]

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

	@Unroll({"input for a $description property is a select"})
	def "inputs for n-to-one associations are selects"() {
		given:
		["Bart Simpson", "Homer Simpson", "Monty Burns"].each {
			new Person(name: it).save(failOnError: true)
		}

		and:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty]

		when:
		def output = tagLib.renderInput(model)

		then:
		output =~ /select name="prop.id"/
		output =~ /id="prop"/
		output =~ /option value="1" >Bart Simpson/
		output =~ /option value="2" >Homer Simpson/
		output =~ /option value="3" >Monty Burns/

		where:
		type   | persistentProperty | description
		Person | oneToOneProperty   | "one-to-one"
		Person | manyToOneProperty  | "many-to-one"
	}

	@Unroll({"input for ${required ? 'a required' : 'an optional'} $description property ${required ? 'has' : 'does not have'} a no-selection option"})
	def "optional inputs for n-to-one associations have no-selection options"() {
		given:
		["Bart Simpson", "Homer Simpson", "Monty Burns"].each {
			new Person(name: it).save(failOnError: true)
		}

		and:
		def model = [type: type, property: "prop", constraints: [:], persistentProperty: persistentProperty, required: required]

		when:
		def output = tagLib.renderInput(model)

		then:
		output.contains('<option value="null"></option>') ^ required

		where:
		type   | persistentProperty | required | description
		Person | oneToOneProperty   | true     | "one-to-one"
		Person | manyToOneProperty  | true     | "many-to-one"
		Person | oneToOneProperty   | false    | "one-to-one"
		Person | manyToOneProperty  | false    | "many-to-one"
	}

}

@Entity
class Person {
	String name

	@Override
	String toString() {
		name
	}

}

class MockPersistentProperty implements GrailsDomainClassProperty {
	boolean association
	boolean basicCollectionType
	boolean bidirectional
	boolean circular
	GrailsDomainClass component
	boolean derived
	GrailsDomainClass domainClass
	boolean embedded
	int fetchMode
	String fieldName
	boolean hasOne
	boolean identity
	boolean inherited
	boolean manyToMany
	boolean manyToOne
	String name
	boolean oneToMany
	boolean oneToOne
	boolean optional
	GrailsDomainClassProperty otherSide
	boolean owningSide
	String naturalName
	boolean persistent
	GrailsDomainClass referencedDomainClass
	String referencedPropertyName
	Class referencedPropertyType
	Class type
	String typePropertyName
	boolean isEnum

	boolean isEnum() { isEnum }
}