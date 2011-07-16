package org.codehaus.groovy.grails.compiler.web

import grails.util.BuildSettings
import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class MethodActionTransformerSpec extends Specification {

    def gcl

    void setup() {
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'true'
        gcl = new GrailsAwareClassLoader()
        def transformer = new MethodActionTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
         def transformer2 = new ControllerTransformer() {
                @Override
                boolean shouldInject(URL url) {
                    return true;
                }

            }
        gcl.classInjectors = [transformer,transformer2] as ClassInjector[]
        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def appCtx = new GrailsWebApplicationContext()
        def servletContext = webRequest.servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
    }

    void "Test that a closure action has changed to method"() {

        when:
            def cls = gcl.parseClass('''
            class TestTransformedToController {

                def action = {
                }

                }
            ''')
            def controller = cls.newInstance()

        then:
          controller
          controller.getClass().getMethod("action", [] as Class[]) != null
    }

   /* void "Test annotated controllers"() {
        when:
        def cls = gcl.parseClass('''
            class AnnotatedControllerTransformer1Controller {
                def action = {
                }
            }
            ''')

        def controller = cls.newInstance()

        then:
        controller
        controller.getClass().getMethod("action", [] as Class[]) != null

    }
*/

    def cleanup() {
        RequestContextHolder.setRequestAttributes(null)
        System.properties[BuildSettings.CONVERT_CLOSURES_KEY] = 'false'
    }
}
