package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.servlet.support.RequestContextUtils

class ScaffoldingTagLibTests extends AbstractGrailsTagTests {

    def personInstance
    def resourceLoader

    protected void onSetUp() {
        gcl.parseClass '''
			@grails.persistence.Entity
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
    }

    @Override
    void setUp() {
        super.setUp()

        resourceLoader = new MockStringResourceLoader()
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

        webRequest.controllerName = "person"

        def person = ga.getDomainClass("Person")
        personInstance = person.clazz.newInstance(name: "Bart Simpson", password: "bartman", gender: "Male", dateOfBirth: new Date(87, 3, 19))

        def address = ga.classLoader.loadClass("Address")
        personInstance.address = address.newInstance(street: "94 Evergreen Terrace", city: "Springfield", country: "USA")
    }

    @Override
    void tearDown() {
        super.tearDown()

        RenderTagLib.TEMPLATE_CACHE.clear()
    }

    void testBeanAttributeIsRequired() {
        shouldFail(GrailsTagException) {
            applyTemplate('<g:scaffoldInput property="name"/>')
        }
    }

    void testPropertyAttributeIsRequired() {
        shouldFail(GrailsTagException) {
            applyTemplate('<g:scaffoldInput bean="${personInstance}"/>', [personInstance: personInstance])
        }
    }

    void testBeanAttributeMustBeNonNull() {
        shouldFail(GrailsTagException) {
            applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: null])
        }
    }

    void testBeanAttributeCanBeAString() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${bean.getClass().name}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person"
    }

    void testBeanAttributeStringMustReferToVariableInPage() {
        shouldFail(GrailsTagException) {
            applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>')
        }
    }

    void testUsesDefaultTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'DEFAULT FIELD TEMPLATE'
    }

    void testResolvesTemplateForPropertyType() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
    }

    void testResolvesTemplateForDomainClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Person.name.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testResolvesTemplateFromControllerViewsDirectory() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Person.name.gsp", 'CLASS AND PROPERTY TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/person/_name.gsp", 'CONTROLLER FIELD TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
    }

    void testBeanAndPropertyAttributesArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${bean.getClass().name}.${property}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "Person.name"
    }

    void testConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'nullable=${constraints.nullable}, blank=${constraints.blank}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "nullable=false, blank=true"
    }

    void testLabelIsResolvedByConventionAndPassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')
        messageSource.addMessage("Person.name.label", RequestContextUtils.getLocale(request), "Name of person")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testLabelIsDefaultedToNaturalPropertyName() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
    }

    void testLabelCanBeOverriddenByLabelAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testLabelCanBeOverriddenByLabelKeyAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')
        messageSource.addMessage("custom.name.label", RequestContextUtils.getLocale(request), "Name of person")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testValueIsDefaultedToPropertyValue() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:formatDate date="${value}" format="yyyy-MM-dd"/>')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="dateOfBirth"/>', [personInstance: personInstance]) == "1987-04-19"
    }

    void testValueIsOverriddenByValueAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name" value="Bartholomew J. Simpson"/>', [personInstance: personInstance]) == "Bartholomew J. Simpson"
    }

    void testValueFallsBackToDefault() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')
        personInstance.name = null

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
    }

    void testDefaultAttributeIsIgnoredIfPropertyHasNonNullValue() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bart Simpson"
    }

    void testErrorsPassedToTemplateIsAnEmptyCollectionForValidBean() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == ""
    }

    void testErrorsPassedToTemplateIsAnCollectionOfStrings() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        personInstance.errors.rejectValue("name", "blank")
        personInstance.errors.rejectValue("name", "nullable")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="name"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
    }

    void testResolvesTemplateForEmbeddedClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Address.city.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testRendersEmbeddedProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'bean=${bean.getClass().name}, property=${property}, value=${value}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "bean=Person, property=address.city, value=Springfield"
    }

    void testRendersEmbeddedPropertyWithErrors() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        personInstance.errors.rejectValue("address.city", "invalid")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="address.city"/>', [personInstance: personInstance]) == "<em>invalid</em>"
    }

    void testEmbeddedClassConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'inList=${constraints.inList}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="address.country"/>', [personInstance: personInstance]) == "inList=[USA, UK, Canada]"
    }

    void testErrorsAreResolvedCorrectlyForEmbeddedProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        personInstance.address.street = ""
        personInstance.validate()
        println personInstance.errors.allErrors
//        personInstance.address.errors.rejectValue("street", "blank")
//        personInstance.address.errors.rejectValue("street", "nullable")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="address.street"/>', [personInstance: personInstance]) == "<em>blank</em><em>nullable</em>"
    }

}
