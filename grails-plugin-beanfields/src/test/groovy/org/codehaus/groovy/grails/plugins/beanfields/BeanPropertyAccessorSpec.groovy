package org.codehaus.groovy.grails.plugins.beanfields

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.plugins.beanfields.taglib.ScaffoldingTagLib
import spock.lang.Specification

@TestFor(ScaffoldingTagLib)
@Mock([Person, Address])
class BeanPropertyAccessorSpec extends Specification {

    def "can resolve basic property on a domain instance"() {
        given:
        def address = new Address(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
        def person = new Person(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19), address: address)
        person.save(failOnError: true)

        when:
        def propertyAccessor = BeanPropertyAccessor.forBeanAndPath(grailsApplication, person, "name")

        then:
        propertyAccessor.value == person.name
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
