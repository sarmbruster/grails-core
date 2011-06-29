package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib
import org.codehaus.groovy.grails.support.MockStringResourceLoader
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.web.servlet.support.RequestContextUtils

class ScaffoldingTagLibAssociationsTests extends AbstractGrailsTagTests {

    def authorInstance
	def bookInstance
    def resourceLoader

    protected void onSetUp() {
        gcl.parseClass '''
			@grails.persistence.Entity
			class Author {
				String name
				List books
				static hasMany = [books: Book]
				static constraints = {
					name blank: false
				}
			}
			@grails.persistence.Entity
            class Book {
                String title
				static belongsTo = [author: Author]
				static constraints = {
					title blank: false
				}
            }
		'''
    }

    @Override
    void setUp() {
        super.setUp()

        resourceLoader = new MockStringResourceLoader()
        appCtx.groovyPagesTemplateEngine.resourceLoader = resourceLoader

        webRequest.controllerName = "author"

        def author = ga.getDomainClass("Author")
        authorInstance = author.clazz.newInstance(name: "William Gibson")

        def book = ga.getDomainClass("Book")
        authorInstance.addToBooks book.clazz.newInstance(title: "Pattern Recognition")
        authorInstance.addToBooks book.clazz.newInstance(title: "Spook Country")
        authorInstance.addToBooks book.clazz.newInstance(title: "Zero History")

		bookInstance = authorInstance.books[0]
    }

    @Override
    void tearDown() {
        super.tearDown()

        RenderTagLib.TEMPLATE_CACHE.clear()
    }

    void testResolvesTemplateForManyToOneProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Author.name.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="bookInstance" property="author.name"/>', [bookInstance: bookInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testRendersManyToOneProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'bean=${bean.getClass().name}, property=${property}, value=${value}')

        assert applyTemplate('<g:scaffoldInput bean="bookInstance" property="author.name"/>', [bookInstance: bookInstance]) == "bean=Book, property=author.name, value=William Gibson"
    }

    void testRendersManyToOnePropertyWithErrors() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        bookInstance.errors.rejectValue("author.name", "invalid")

        assert applyTemplate('<g:scaffoldInput bean="bookInstance" property="author.name"/>', [bookInstance: bookInstance]) == "<em>invalid</em>"
    }

    void testManyToOnePropertyConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'blank=${constraints.blank}')

        assert applyTemplate('<g:scaffoldInput bean="bookInstance" property="author.name"/>', [bookInstance: bookInstance]) == "blank=false"
    }

    void testResolvesTemplateForOneToManyProperty() {
		resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'DEFAULT FIELD TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_java.lang.String.gsp", 'PROPERTY TYPE TEMPLATE')
        resourceLoader.registerMockResource("/grails-app/views/fields/_Book.title.gsp", 'CLASS AND PROPERTY TEMPLATE')

        assert applyTemplate('<g:scaffoldInput bean="authorInstance" property="books[0].title"/>', [authorInstance: authorInstance]) == "CLASS AND PROPERTY TEMPLATE"
    }

    void testRendersOneToManyProperty() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'bean=${bean.getClass().name}, property=${property}, value=${value}')

        assert applyTemplate('<g:scaffoldInput bean="authorInstance" property="books[0].title"/>', [authorInstance: authorInstance]) == "bean=Author, property=books[0].title, value=Pattern Recognition"
    }

    void testRendersOneToManyPropertyWithErrors() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", '<g:each var="error" in="${errors}"><em>${error}</em></g:each>')
        authorInstance.errors.rejectValue("books[0].title", "invalid")

        assert applyTemplate('<g:scaffoldInput bean="authorInstance" property="books[0].title"/>', [authorInstance: authorInstance]) == "<em>invalid</em>"
    }

    void testOneToManyPropertyConstraintsArePassedToTemplate() {
        resourceLoader.registerMockResource("/grails-app/views/fields/_default.gsp", 'blank=${constraints.blank}')

        assert applyTemplate('<g:scaffoldInput bean="authorInstance" property="books[0].title"/>', [authorInstance: authorInstance]) == "blank=false"
    }

}
