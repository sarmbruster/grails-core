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

    @Override
    void tearDown() {
        super.tearDown()

        appCtx.groovyPagesTemplateEngine.clearPageCache()
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

    void testResolvesTemplateFromControllerViewsDirectory() {
        resourceLoader.registerMockResource("/person/_password.gsp", '<input type="password" name="${property}" value="${value}">')

        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="password"/>', [personInstance: personInstance])

        assert output == '<input type="password" name="password" value="BlackBart">'
    }

    void testResolvesConfiguredTemplateForDomainClassProperty() {
        resourceLoader.registerMockResource("/scaffolding/_gender.gsp", '<g:radioGroup name="${property}" values="[\'Male\', \'Female\']" labels="[\'Male\', \'Female\']" value="${value}"><label>${it.label} ${it.radio}</label></g:radioGroup>')
        ga.config.scaffolding.template.Person.gender = "/scaffolding/gender"

        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="gender"/>', [personInstance: personInstance])

        assert output =~ /<label>Male <input type="radio" name="gender" checked="checked" value="Male" \/><\/label>/
		assert output =~ /<label>Female <input type="radio" name="gender" value="Female" \/><\/label>/
    }

    void testResolvesConfiguredTemplateForPropertyType() {
        resourceLoader.registerMockResource("/scaffolding/_date.gsp", '<input type="date" name="${property}" value="${formatDate(date: value, format: \'yyyy-MM-dd\')}">')
        ga.config.scaffolding.template.default."java.util.Date" = "/scaffolding/date"

        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="dateOfBirth"/>', [personInstance: personInstance])

        assert output == '<input type="date" name="dateOfBirth" value="1682-05-17">'
    }

    void testUsesDefaultRendering() {
        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance])

        assert output == '<input type="text" name="name" value="Bartholomew Roberts" id="name" />'
    }

	void testRenderingPrecedence() {
        resourceLoader.registerMockResource("/scaffolding/_string.gsp", 'TEMPLATE FOR PROPERTY TYPE')
        ga.config.scaffolding.template.default."java.lang.String" = "/scaffolding/string"
        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "TEMPLATE FOR PROPERTY TYPE"

        resourceLoader.registerMockResource("/person/_name.gsp", 'TEMPLATE BY CONVENTION')
        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "TEMPLATE BY CONVENTION"

        resourceLoader.registerMockResource("/scaffolding/_name.gsp", 'TEMPLATE FOR DOMAIN PROPERTY')
        ga.config.scaffolding.template.Person.name = "/scaffolding/name"
        assert applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance]) == "TEMPLATE FOR DOMAIN PROPERTY"
	}

}
