/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.lifecycle.ShutdownOperations
import grails.util.ClosureToMapPopulator

/**
 * Utility methods for configuring constraints
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ConstraintEvalUtils {

    static {
        ShutdownOperations.addOperation({
            clearDefaultConstraints()
        } as Runnable)
    }

    private static defaultConstraintsMap = null
    private static configId

    /**
     * Looks up the default configured constraints from the given configuration
     */
    public static Map<String, Object> getDefaultConstraints(ConfigObject config) {
        def cid = System.identityHashCode(config)
        if(defaultConstraintsMap == null || configId != cid) {
            configId = cid
            def constraints = config?.grails?.gorm?.default?.constraints
            if (constraints instanceof Closure) {
                defaultConstraintsMap = new ClosureToMapPopulator().populate((Closure<?>) constraints);
            }
            else {
                defaultConstraintsMap = Collections.emptyMap()
            }
        }
        return defaultConstraintsMap
    }

    public static void clearDefaultConstraints() {
        defaultConstraintsMap =  null
        configId = null
    }

}
