package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

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
			}
		'''
    }

    @Override
    void setUp() {
        super.setUp()

        resourceLoader = new MockStringResourceLoader()
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

        webRequest.controllerName = "person"

        def domain = ga.getDomainClass("Person")
        personInstance = domain.clazz.newInstance(name: "Bartholomew Roberts", password: "BlackBart", gender: "Male", dateOfBirth: new Date(-218, 4, 17))
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

    void testUsesDefaultTemplate() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == 'DEFAULT FIELD TEMPLATE'
    }

    void testResolvesTemplateForPropertyType() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == 'PROPERTY TYPE TEMPLATE'
    }

    void testResolvesTemplateForDomainClassProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Person.name.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testResolvesTemplateFromControllerViewsDirectory() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Person.name.gsp", 'CLASS AND PROPERTY TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/person/_name.gsp", 'CONTROLLER FIELD TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == 'CONTROLLER FIELD TEMPLATE'
    }

}
