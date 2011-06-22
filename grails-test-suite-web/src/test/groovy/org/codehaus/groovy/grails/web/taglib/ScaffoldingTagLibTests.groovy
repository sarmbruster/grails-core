package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.servlet.support.RequestContextUtils
import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib

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

    void testLabelIsResolvedByConventionAndPassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')
        messageSource.addMessage("Person.name.label", RequestContextUtils.getLocale(request), "Name of person")

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testLabelIsDefaultedToNaturalPropertyName() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "<label>Name</label>"
        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="dateOfBirth"/>', [personInstance: personInstance]) == "<label>Date Of Birth</label>"
    }

    void testLabelCanBeOverriddenByLabelAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name" label="Name of person"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testLabelCanBeOverriddenByLabelKeyAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<label>${label}</label>')
        messageSource.addMessage("custom.name.label", RequestContextUtils.getLocale(request), "Name of person")

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name" labelKey="custom.name.label"/>', [personInstance: personInstance]) == "<label>Name of person</label>"
    }

    void testValueIsDefaultedToPropertyValue() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:formatDate date="${value}" format="yyyy-MM-dd"/>')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="dateOfBirth"/>', [personInstance: personInstance]) == "1682-05-17"
    }

    void testValueIsOverriddenByValueAttribute() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name" value="Black Bart"/>', [personInstance: personInstance]) == "Black Bart"
    }

    void testValueFallsBackToDefault() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')
        personInstance.name = null

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "A. N. Other"
    }

    void testDefaultAttributeIsIgnoredIfPropertyHasNonNullValue() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '${value}')

        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name" default="A. N. Other"/>', [personInstance: personInstance]) == "Bartholomew Roberts"
    }
}
