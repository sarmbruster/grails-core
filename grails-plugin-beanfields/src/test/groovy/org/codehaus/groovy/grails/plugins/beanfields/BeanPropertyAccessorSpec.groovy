package org.codehaus.groovy.grails.plugins.beanfields

import grails.test.mixin.Mock
import spock.lang.Specification

@Mock(Person)
class BeanPropertyAccessorSpec extends Specification {

    def gcl = new GroovyClassLoader()
    def personClass
    def addressClass

    def "can resolve basic property on a domain instance"() {
        given:
        def address = addressClass.newInstance(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
        def person = person.newInstance(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), address: address)
        person.save(failOnError: true)

        when:
        def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "name")

        then:
        propertyAccessor.value == person.name
    }

    def setup() {
        gcl.parseClass '''
class Person {
    String name
    String password
    String gender
    Date dateOfBirth
    Address address
    static embedded = ['address']
}

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
'''

        personClass = grailsApplication.getDomainClass("Person").clazz
        addressClass = grailsApplication.classLoader.loadClass("Address")
    }
}