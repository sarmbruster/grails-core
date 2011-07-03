package org.codehaus.groovy.grails.plugins.beanfields

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.taglib.ScaffoldingTagLib
import org.junit.Before
import org.junit.Test

@TestFor(ScaffoldingTagLib)
@Mock([Person, Address])
class BeanPropertyAccessorTests {

	def address
	def person

	@Before
	void setUpPerson() {
		address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
		person = new Person(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), address: address)
		person.save(failOnError: true)
	}

	@Test
	void canResolveBasicProperty() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "name")

		expect:
		assert propertyAccessor.value == person.name
		assert propertyAccessor.rootBeanType == Person
		assert propertyAccessor.rootBeanClass.clazz == Person
		assert propertyAccessor.beanType == Person
		assert propertyAccessor.beanClass.clazz == Person
		assert propertyAccessor.pathFromRoot == "name"
		assert propertyAccessor.propertyName == "name"
		assert propertyAccessor.type == String
		assert propertyAccessor.persistentProperty.name == "name"
	}

	@Test
	void resolvesBasicPropertyConstraints() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "name")

		expect:
		assert !propertyAccessor.constraints.nullable
		assert !propertyAccessor.constraints.blank
	}

	@Test
	void resolvesOtherPropertyTypes() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "dateOfBirth")

		expect:
		assert propertyAccessor.type == Date
		assert propertyAccessor.value == new Date(87, 3, 19)
	}

	@Test
	void canResolveEmbeddedProperty() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "address.city")

		expect:
		assert propertyAccessor.value == address.city
		assert propertyAccessor.rootBeanType == Person
		assert propertyAccessor.rootBeanClass.clazz == Person
		assert propertyAccessor.beanType == Address
		assert propertyAccessor.beanClass.clazz == Address
		assert propertyAccessor.pathFromRoot == "address.city"
		assert propertyAccessor.propertyName == "city"
		assert propertyAccessor.type == String
		assert propertyAccessor.persistentProperty.name == "city"
	}

	@Test
	void resolvesEmbeddedPropertyConstraints() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "address.country")

		expect:
		assert !propertyAccessor.constraints.nullable
		assert propertyAccessor.constraints.inList == ["USA", "UK", "Canada"]
	}

	@Test
	void labelKeyIsSameAsScaffoldingConvention() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "address.city")

		expect:
		assert propertyAccessor.labelKey == "address.city.label"
	}

	@Test
	void defaultLabelIsPropertyNaturalName() {
		given:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "dateOfBirth")

		expect:
		assert propertyAccessor.defaultLabel == "Date Of Birth"
	}

	@Test
	void resolvesErrorsForBasicProperty() {
		given:
		person.name = ""

		and:
		def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "name")

		expect:
		assert !person.validate()

		and:
		assert propertyAccessor.errors
	}

}

@Entity
class Person {
	String name
	String password
	String gender
	Date dateOfBirth
	Address address
	static embedded = ['address']
	static constraints = {
		name blank: false
	}
}

@Entity
class Address {
	String street
	String city
	String country
	static constraints = {
		street blank: false
		city blank: false
		country inList: ["USA", "UK", "Canada"]
	}
}
