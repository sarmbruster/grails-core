package org.codehaus.groovy.grails.plugins.beanfields.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader

class ScaffoldingTagLibMapTests extends org.codehaus.groovy.grails.web.taglib.AbstractGrailsTagTests {

    def personInstance
    def resourceLoader

    protected void onSetUp() {
        gcl.parseClass '''
			@grails.persistence.Entity
			class Person {
				String name
				Map addresses = [:]
				static hasMany = [addresses: Address]
			}
			@grails.persistence.Entity
            class Address {
                String street
                String city
                String country
                static belongsTo = Person
                static constraints = {
                    street blank: false
                    city blank: false
                    country blank: false
                }
            }
		'''
    }

    @Override
    void setUp() {
        super.setUp()

        resourceLoader = new MockStringResourceLoader()
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

        webRequest.controllerName = "person"

        def person = ga.getDomainClass("Person")
        personInstance = person.clazz.newInstance(name: "Homer Simpson")

        def address = ga.getDomainClass("Address")
        personInstance.addresses.home = address.clazz.newInstance(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
        personInstance.addresses.work = address.clazz.newInstance(street: "Springfield Nuclear Power Plant", city: "Springfield", country: "USA")
    }

    @Override
    void tearDown() {
        super.tearDown()

        RenderTagLib.TEMPLATE_CACHE.clear()
    }

    void testResolvesTemplateForMapProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Address.street.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="addresses[work].street"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testRendersMapProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'bean=${bean.getClass().name}, property=${property}, value=${value}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="addresses[work].street"/>', [personInstance: personInstance]) == "bean=Person, property=addresses[work].street, value=Springfield Nuclear Power Plant"
    }

    void testRendersMapPropertyWithErrors() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        personInstance.errors.rejectValue("addresses[work].street", "invalid")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="addresses[work].street"/>', [personInstance: personInstance]) == "<em>invalid</em>"
    }

    void testMapPropertyConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'blank=${constraints.blank}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="addresses[work].street"/>', [personInstance: personInstance]) == "blank=false"
    }
}
