/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins;

import org.springframework.util.Assert;

/**
 * Manages a thread bound plugin manager instance.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public abstract class PluginManagerHolder {

    private static GrailsPluginManager gpm;
    private static Boolean inCreation = false;

    public static void setInCreation(boolean inCreation) {
        PluginManagerHolder.inCreation = inCreation;
    }

    /**
     * Bind the given GrailsPluginManager instance to the current Thread
     * @param pluginManager The GrailsPluginManager to expose
     *
     * @deprecated Use dependency injection instead (implement the {@link org.springframework.web.context.ServletContextAware} interface)
     */
    @Deprecated
    public static void setPluginManager(GrailsPluginManager pluginManager) {
        if (pluginManager != null) {
            inCreation = false;
        }
        gpm = pluginManager;
    }

    /**
     * Retrieves the GrailsPluginManager bound to the current Thread
     * @return The GrailsPluginManager or null
     *
     * @deprecated Use dependency injection instead (implement the {@link org.springframework.web.context.ServletContextAware} interface)
     */
    @Deprecated
    public static GrailsPluginManager getPluginManager() {
        while (inCreation) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                break;
            }
        }
        return gpm;
    }

    /**
     * Retrieves the bound GrailsPluginManager that resides in the current Thread
     * @return The GrailsPluginManager
     * @throws IllegalStateException When there is no bound GrailsPluginManager
     *
     * @deprecated Use dependency injection instead (implement the {@link org.springframework.web.context.ServletContextAware} interface)
     */
    @Deprecated
    public static GrailsPluginManager currentPluginManager() {
        GrailsPluginManager current = getPluginManager();
        Assert.state(current != null, "No PluginManager set");
        return current;
    }
}
