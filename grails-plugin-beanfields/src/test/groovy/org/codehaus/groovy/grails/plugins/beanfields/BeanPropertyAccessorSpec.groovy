package org.codehaus.groovy.grails.plugins.beanfields

import grails.persistence.Entity
import org.codehaus.groovy.grails.plugins.beanfields.taglib.ScaffoldingTagLib
import grails.test.mixin.*
import spock.lang.*

@TestFor(ScaffoldingTagLib)
@Mock([Person, Address])
class BeanPropertyAccessorSpec extends Specification {

	BeanPropertyAccessorFactory factory = new BeanPropertyAccessorFactory(grailsApplication: grailsApplication)
	def address
	def person

	def setup() {
		address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
		person = new Person(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), address: address)
		person.save(failOnError: true)
	}

	def "can resolve basic property of domain class"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "name")

		expect:
		propertyAccessor.value == person.name
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person
		propertyAccessor.beanType == Person
		propertyAccessor.beanClass.clazz == Person
		propertyAccessor.pathFromRoot == "name"
		propertyAccessor.propertyName == "name"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "name"
	}

	def "resolves constraints of basic domain class property"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "name")

		expect:
		!propertyAccessor.constraints.nullable
		!propertyAccessor.constraints.blank
	}

	def "resolves type of property"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "dateOfBirth")

		expect:
		propertyAccessor.type == Date
		propertyAccessor.value == new Date(87, 3, 19)
	}

	def "resolves embedded property of domain class"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "address.city")

		expect:
		propertyAccessor.value == address.city
		propertyAccessor.rootBeanType == Person
		propertyAccessor.rootBeanClass.clazz == Person
		propertyAccessor.beanType == Address
		propertyAccessor.beanClass.clazz == Address
		propertyAccessor.pathFromRoot == "address.city"
		propertyAccessor.propertyName == "city"
		propertyAccessor.type == String
		propertyAccessor.persistentProperty.name == "city"
	}

	def "resolves constraints of embedded property"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "address.country")

		expect:
		!propertyAccessor.constraints.nullable
		propertyAccessor.constraints.inList == ["USA", "UK", "Canada"]
	}

	def "label key is the same as the scaffolding convention"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, "address.city")

		expect:
		propertyAccessor.labelKey == "address.city.label"
	}

	@Unroll
	def "default label is the property's natural name"() {
		given:
		def propertyAccessor = factory.forBeanAndPath(person, property)

		expect:
		propertyAccessor.defaultLabel == label

		where:
		property       | label
		"name"         | "Name"
		"dateOfBirth"  | "Date Of Birth"
		"address.city" | "City"
	}

	def "resolves errors for a basic property"() {
		given:
		person.name = ""

		and:
		def propertyAccessor = factory.forBeanAndPath(person, "name")

		expect:
		!person.validate()

		and:
		propertyAccessor.errors.find {
			it.code == "blank"
		}
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
