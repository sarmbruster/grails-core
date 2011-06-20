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

    void testResolvesTemplateFromControllerViewsDirectory() {
        resourceLoader.registerMockResource("/person/_password.gsp", '<input type="password" name="${property}" value="${value}">')

        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="password"/>', [personInstance: personInstance])

        assert output == '<input type="password" name="password" value="BlackBart">'
    }

    // TODO: test for template provided by a plugin

    void testResolvesConfiguredTemplateForDomainClassProperty() {

    }

    void testResolvesConfiguredTemplateForPropertyType() {
        resourceLoader.registerMockResource("/templates/_date.gsp", '<input type="date" name="${property}" value="${formatDate(date: value, format: \'yyyy-MM-dd\')}">')

        withConfig('scaffolding.template.default."java.util.Date" = "/templates/date"') {
            println "In test: ${ga.config}"
            def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="dateOfBirth"/>', [personInstance: personInstance])

            assert output == '<input type="date" name="dateOfBirth" value="1682-05-17">'
        }
    }

    void testUsesDefaultRendering() {
        def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="name"/>', [personInstance: personInstance])

        assert output == '<input type="text" name="name" value="Bartholomew Roberts" id="name" />'
    }

}
