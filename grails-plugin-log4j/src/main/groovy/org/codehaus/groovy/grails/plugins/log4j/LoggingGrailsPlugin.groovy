/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.plugins.log4j

import grails.util.GrailsUtil

import org.apache.log4j.LogManager

import org.slf4j.bridge.SLF4JBridgeHandler
import org.codehaus.groovy.grails.plugins.log4j.web.util.Log4jConfigListener

/**
 * Provides a lazy initialized commons logging log property for all classes.
 *
 * @author Marc Palmer
 * @author Graeme Rocher
 *
 * @since 0.4
 */
class LoggingGrailsPlugin {

    def version = GrailsUtil.getGrailsVersion()
    def loadBefore = ['core']
    def observe = ['*']

    def doWithSpring = {
        def usebridge = application.config?.grails?.logging?.jul?.usebridge
        if (usebridge) {
            def juLogMgr = application.classLoader.loadClass("java.util.logging.LogManager").logManager
            juLogMgr.readConfiguration(new ByteArrayInputStream(".level=INFO".bytes))
            SLF4JBridgeHandler.install()
        }
    }

    def onConfigChange = { event ->
		Log4jConfig.initialize(event.source)
    }

    def doWithWebDescriptor = { webXml ->

        def mappingElement = webXml.'filter-mapping'
        mappingElement = mappingElement[mappingElement.size() - 1]

        mappingElement + {
            'listener' {
                'listener-class'(Log4jConfigListener.name)
            }
        }
    }
}
