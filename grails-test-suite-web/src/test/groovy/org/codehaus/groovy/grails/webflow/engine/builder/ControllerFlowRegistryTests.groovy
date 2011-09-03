package org.codehaus.groovy.grails.webflow.engine.builder

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerTests
import org.springframework.binding.convert.service.DefaultConversionService
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry
import org.springframework.webflow.engine.builder.support.FlowBuilderServices
import org.springframework.webflow.expression.DefaultExpressionParserFactory
import org.springframework.webflow.mvc.builder.MvcViewFactoryCreator

/**
 * Tests for the ControllerFlowRegistry class.
 *
* @author Graeme Rocher
* @since 0.6
*/
class ControllerFlowRegistryTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass('''
class FooController {
    def searchService = [executeSearch: { ['book'] }]
    def shoppingCartFlow = {
        enterPersonalDetails {
            on("submit").to "enterShipping"
        }
        enterShipping {
            on("back").to "enterPersonDetails"
            on("submit").to "enterPayment"
        }
        enterPayment {
            on("back").to "enterShipping"
            on("submit").to "confirmPurchase"
        }
        confirmPurchase {
            on("confirm").to "processPurchaseOrder"
        }
        processPurchaseOrder {
            action {
                println "processing purchase order"
                [order:"done"]
            }
            on("error").to "confirmPurchase"
            on(Exception).to "confirmPurchase"
            on("success").to "displayInvoice"
        }
        displayInvoice()
    }

    def anotherAction = {

    }
}
        ''')
    }

    void testFlowRegsitry() {
        def ga = creategGrailsApplication()
        ga.initialise()

        assertEquals 1, ga.controllerClasses.size()

        ControllerFlowRegistry factoryBean = new ControllerFlowRegistry()
        factoryBean.grailsApplication = ga
        def flowBuilderServices = new FlowBuilderServices()
        MvcViewFactoryCreator viewCreator = new MvcViewFactoryCreator()
        viewCreator.applicationContext = new GrailsWebApplicationContext()
        flowBuilderServices.viewFactoryCreator = viewCreator
        flowBuilderServices.conversionService = new DefaultConversionService()
        flowBuilderServices.expressionParser = DefaultExpressionParserFactory.getExpressionParser()
        factoryBean.flowBuilderServices = flowBuilderServices

        factoryBean.afterPropertiesSet()

        FlowDefinitionRegistry registry = factoryBean.getObject()

        assert registry
        assertEquals 1,registry.getFlowDefinitionCount()
        def cartFlow = registry.getFlowDefinition("foo/shoppingCart")
        assert cartFlow
        assertEquals 6,cartFlow.stateCount
    }
}
