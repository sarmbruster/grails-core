package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareAspectJAwareAdvisorAutoProxyCreator
import org.codehaus.groovy.grails.aop.framework.autoproxy.GroovyAwareInfrastructureAdvisorAutoProxyCreator
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.test.AbstractGrailsMockTests
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.beans.factory.config.RuntimeBeanReference

class CoreGrailsPluginTests extends AbstractGrailsMockTests {

    void testComponentScan() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        def pluginManager = new MockGrailsPluginManager()
        ctx.registerMockBean(GrailsPluginManager.BEAN_NAME, pluginManager)
        ga.config.grails.spring.bean.packages = ['org.codehaus.groovy.grails.plugins.test']

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

    }
    void testCorePlugin() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()

        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
        assert appCtx.getBean("org.springframework.aop.config.internalAutoProxyCreator") instanceof GroovyAwareAspectJAwareAdvisorAutoProxyCreator
    }

    void testDisableAspectj() {
        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")

        def plugin = new DefaultGrailsPlugin(pluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        springConfig.servletContext = createMockServletContext()
        ga.config.grails.spring.disable.aspectj.autoweaving=true
        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assert appCtx.containsBean("classLoader")
        assert appCtx.containsBean("customEditors")
        assert appCtx.getBean("org.springframework.aop.config.internalAutoProxyCreator") instanceof GroovyAwareInfrastructureAdvisorAutoProxyCreator

    }

    protected void onSetUp() {
        // needed for testBeanPropertyOverride
        gcl.parseClass("""
            class SomeTransactionalService {
                boolean transactional = true
                Integer i
            }
            class NonTransactionalService {
                boolean transactional = false
                Integer i
            }
        """)
    }

    /**
     * Tests the ability to set bean properties via the application config.
     *
     * @author Luke Daley
     */
    void testBeanPropertyOverride() {
        ga.config = new ConfigSlurper().parse('''
            dataSource {
                pooled = true
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                dbCreate = "create-drop"
            }
            beans {
                someTransactionalService {
                    i = 1
                }
                nonTransactionalService {
                    i = 2
                }
            }
        ''')

        def corePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        def corePlugin = new DefaultGrailsPlugin(corePluginClass,ga)
        def dataSourcePluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
        def dataSourcePlugin = new DefaultGrailsPlugin(dataSourcePluginClass, ga)

        def springConfig = new WebRuntimeSpringConfiguration(ctx)

        def txMgr = springConfig.addSingletonBean("transactionManager", DataSourceTransactionManager)
        txMgr.addProperty("dataSource", new RuntimeBeanReference("dataSource"))
        springConfig.servletContext = createMockServletContext()

        corePlugin.doWithRuntimeConfiguration(springConfig)
        dataSourcePlugin.doWithRuntimeConfiguration(springConfig)

        def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.services.ServicesGrailsPlugin")
        def plugin = new DefaultGrailsPlugin(pluginClass, ga)
        plugin.doWithRuntimeConfiguration(springConfig)

        def appCtx = springConfig.getApplicationContext()

        assertEquals(1, appCtx.getBean('someTransactionalService').i)
        assertEquals(2, appCtx.getBean('nonTransactionalService').i)

        // test that the overrides are applied on a reload - GRAILS-5763
        plugin.manager = [informObservers: { String pluginName, Map event -> }] as GrailsPluginManager
        plugin.applicationContext = appCtx

        ["SomeTransactionalService", "NonTransactionalService"].each {
            plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, gcl.loadClass(it))
        }

        assertEquals(1, appCtx.getBean('someTransactionalService').i)
        assertEquals(2, appCtx.getBean('nonTransactionalService').i)
    }
}
