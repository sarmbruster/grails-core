package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader

class ScaffoldingTagLibSimpleCollectionTests extends AbstractGrailsTagTests {

    def personInstance
    def resourceLoader

    protected void onSetUp() {
        gcl.parseClass '''
			@grails.persistence.Entity
			class Person {
				String name
				Map emails = [:]
				static hasMany = [emails: String]
                static constraints = {
                    emails blank: false
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
        personInstance = person.clazz.newInstance(name: "Homer Simpson", emails: [work: "homer@compuglobalhypermega.net", home: "homer.j.simpson@gmail.com"])
    }

    @Override
    void tearDown() {
        super.tearDown()

        RenderTagLib.TEMPLATE_CACHE.clear()
    }

    void testResolvesTemplateForMapProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Person.emails.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="emails[work]"/>', [personInstance: personInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testRendersMapProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'bean=${bean.getClass().name}, property=${property}, value=${value}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="emails[work]"/>', [personInstance: personInstance]) == "bean=Person, property=emails[work], value=homer@compuglobalhypermega.net"
    }

    void testRendersMapPropertyWithErrors() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        personInstance.errors.rejectValue("emails[work]", "invalid")

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="emails[work]"/>', [personInstance: personInstance]) == "<em>invalid</em>"
    }

    void testMapPropertyConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'blank=${constraints.blank}')

        assert applyTemplate('<g:scaffoldInput bean="personInstance" property="emails[work]"/>', [personInstance: personInstance]) == "blank=false"
    }
}
