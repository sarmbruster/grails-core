package org.codehaus.groovy.grails.scaffolding.view

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.scaffolding.DefaultGrailsTemplateGenerator
import org.codehaus.groovy.grails.scaffolding.Test
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

import org.springframework.core.io.DefaultResourceLoader
import org.springframework.mock.web.MockServletContext
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ScaffoldingViewResolverTests extends GroovyTestCase {

    void testScaffoldingViewResolver() {
        GrailsWebRequest webRequest = GrailsWebUtil.bindMockWebRequest()

        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())
        gpte.afterPropertiesSet()

        def ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication())

        def viewResolver = new TestScaffoldingViewResolver()
        viewResolver.applicationContext = ctx
        viewResolver.templateEngine = gpte
        viewResolver.servletContext = webRequest.getServletContext()
        viewResolver.groovyPageLocator = new GrailsConventionGroovyPageLocator(resourceLoader: new DefaultResourceLoader())

        viewResolver.templateGenerator = new DefaultGrailsTemplateGenerator()
        webRequest.actionName = "list"
        webRequest.controllerName = "foo"

        viewResolver.scaffoldedActionMap = [foo:['list']]
        viewResolver.scaffoldedDomains = [foo:new DefaultGrailsDomainClass(Test)]

        def view = viewResolver.loadView("/foo/list", Locale.getDefault())
        assert view
        assertTrue "should be an instanceof ScaffoldedGroovyPageView", view instanceof ScaffoldedGroovyPageView

        def model = [foo:"bar"]
        view.render(model, webRequest.currentRequest, webRequest.currentResponse)

        assertEquals "successbar", webRequest.currentResponse.contentAsString
    }
}

class TestScaffoldingViewResolver extends ScaffoldingViewResolver {
    protected String generateViewSource(String viewName, GrailsDomainClass domainClass) {
        if ("/list" == viewName) {
            return "<%='success'+foo%>"
        }
        return null
    }
}
