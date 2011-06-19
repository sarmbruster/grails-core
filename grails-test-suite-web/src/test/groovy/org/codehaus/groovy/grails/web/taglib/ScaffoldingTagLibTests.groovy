package org.codehaus.groovy.grails.web.taglib

import org.junit.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.web.taglib.*
import org.codehaus.groovy.grails.web.taglib.exceptions.*

class ScaffoldingTagLibTests extends AbstractGrailsTagTests {
	
	def personInstance
	def resourceLoader
	
	void setUp() {
		super.setUp()
		
        resourceLoader = new MockStringResourceLoader()
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

        webRequest.controllerName = "person"

		personInstance = new Person(name: "Bartholomew Roberts", password: "BlackBart", gender: "Male", dateOfBirth: new Date(1682, 4, 17))
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
		// a template in the controller's views directory should get used by preference
        resourceLoader.registerMockResource("/person/_password.gsp", '<input type="password" name="${property}" value="${value}">')
		
		def output = applyTemplate('<g:scaffoldInput bean="${personInstance}" property="password"/>', [personInstance: personInstance])
		
		assert output == '<input type="password" name="password" value="BlackBart">'
	}
	
	// TODO: test for template provided by a plugin
	
	void testResolvesConfiguredTemplateForDomainClassProperty() {
		
	}
	
	void testResolvesConfiguredTemplateForPropertyType() {
		
	}
	
	void testUsesDefaultRendering() {
		
	}

}

class Person {
	String name
	String password
	String gender
	Date dateOfBirth
}