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
package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.mapper.DefaultDecorator;
import com.opensymphony.sitemesh.Content;
import grails.util.Environment;
import groovy.lang.GroovyObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the logic for GrailsLayoutDecoratorMapper without so many ties to the Sitemesh API
 *
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GroovyPageLayoutFinder {

    public static final String LAYOUT_ATTRIBUTE = "org.grails.layout.name";
    private static final Log LOG = LogFactory.getLog(GrailsLayoutDecoratorMapper.class);
    private static final long LAYOUT_CACHE_EXPIRATION_MILLIS =  Long.getLong("grails.gsp.reload.interval", 5000).longValue();

    private Map<String, DecoratorCacheValue> decoratorCache = new ConcurrentHashMap<String, DecoratorCacheValue>();
    private Map<LayoutCacheKey, DecoratorCacheValue> layoutDecoratorCache = new ConcurrentHashMap<LayoutCacheKey, DecoratorCacheValue>();

    private GrailsConventionGroovyPageLocator groovyPageLocator;
    private String defaultDecoratorName;
    private boolean gspReloadEnabled;
    private boolean cacheEnabled = (Environment.getCurrent() != Environment.DEVELOPMENT);

    public void setGroovyPageLocator(GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.groovyPageLocator = groovyPageLocator;
    }

    public void setDefaultDecoratorName(String defaultDecoratorName) {
        this.defaultDecoratorName = defaultDecoratorName;
    }

    public void setGspReloadEnabled(boolean gspReloadEnabled) {
        this.gspReloadEnabled = gspReloadEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public Decorator findLayout(HttpServletRequest request, Content page) {
        return findLayout(request, GroovyPageLayoutRenderer.content2htmlPage(page));
    }
    public Decorator findLayout(HttpServletRequest request, Page page) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Evaluating layout for request: " + request.getRequestURI());
        }
        Object layoutAttribute = request.getAttribute(LAYOUT_ATTRIBUTE);
        String layoutName = layoutAttribute != null ? layoutAttribute.toString() : null;

        if (layoutName == null) {
            layoutName = page.getProperty("meta.layout");
        }

        Decorator d = null;

        if (StringUtils.isBlank(layoutName)) {
            GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);
            if (controller != null) {
                String controllerName = (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_NAME_PROPERTY);
                String actionUri = (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found controller in request, location layout for controller [" +
                            controllerName + "] and action [" + actionUri + "]");
                }

                LayoutCacheKey cacheKey = null;
                boolean cachedIsNull = false;

                if (cacheEnabled) {
                    cacheKey = new LayoutCacheKey(controllerName, actionUri);
                    DecoratorCacheValue cacheValue = layoutDecoratorCache.get(cacheKey);
                    if (cacheValue != null && (!gspReloadEnabled || !cacheValue.isExpired())) {
                        d = cacheValue.getDecorator();
                        if (d == null) {
                            cachedIsNull = true;
                        }
                    }
                }

                if (d == null && !cachedIsNull) {
                    d = resolveDecorator(request, controller, controllerName, actionUri);
                    if (cacheEnabled) {
                        layoutDecoratorCache.put(cacheKey, new DecoratorCacheValue(d));
                    }
                }
            } else {
                d = getApplicationDefaultDecorator(request);
            }
        } else {
            d = getNamedDecorator(request, layoutName);
        }

        if (d != null) {
            return d;
        }
        return null;
    }

    protected Decorator getApplicationDefaultDecorator(HttpServletRequest request) {
        return getNamedDecorator(request, defaultDecoratorName);
    }

    public Decorator getNamedDecorator(HttpServletRequest request, String name) {
        if (StringUtils.isBlank(name)) return null;

        if (cacheEnabled) {
            DecoratorCacheValue cacheValue = decoratorCache.get(name);
            if (cacheValue != null && (!gspReloadEnabled || !cacheValue.isExpired())) {
                return cacheValue.getDecorator();
            }
        }

        Decorator d = null;
        GroovyObject controller = (GroovyObject)request.getAttribute(GrailsApplicationAttributes.CONTROLLER);

        GroovyPageScriptSource scriptSource = null;

        if (controller != null) {
            scriptSource = groovyPageLocator.findLayout(controller, name);
        }
        else {
            scriptSource = groovyPageLocator.findLayout(name);
        }

        if (scriptSource != null) {
            d =  createDecorator(name, scriptSource.getURI());
        }

        if (cacheEnabled) {
            decoratorCache.put(name, new DecoratorCacheValue(d));
        }
        return d;
    }

    private Decorator resolveDecorator(HttpServletRequest request,
                 GroovyObject controller, String controllerName, String actionUri) {
        Decorator d = null;

        Object layoutProperty = GrailsClassUtils.getStaticPropertyValue(controller.getClass(), "layout");
        if (layoutProperty instanceof CharSequence) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("layout property found in controller, looking for template named " + layoutProperty);
            }
            d = getNamedDecorator(request, layoutProperty.toString());
        } else {
            if (d == null && !StringUtils.isBlank(actionUri)) {
                d = getNamedDecorator(request, actionUri.substring(1));
            }

            if (d == null && !StringUtils.isBlank(controllerName)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Action layout not found, trying controller");
                }
                d = getNamedDecorator(request, controllerName);
            }

            if (d == null) {
                d = getApplicationDefaultDecorator(request);
            }
        }

        return d;
    }

    private Decorator createDecorator(String decoratorName, String decoratorPage) {
        return new DefaultDecorator(decoratorName, decoratorPage, Collections.EMPTY_MAP);
    }

    private static class LayoutCacheKey {
        private String controllerName;
        private String actionUri;

        public LayoutCacheKey(String controllerName, String actionUri) {
            this.controllerName = controllerName;
            this.actionUri = actionUri;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((actionUri == null) ? 0 : actionUri.hashCode());
            result = prime * result + ((controllerName == null) ? 0 : controllerName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LayoutCacheKey other = (LayoutCacheKey) obj;
            if (actionUri == null) {
                if (other.actionUri != null)
                    return false;
            } else if (!actionUri.equals(other.actionUri))
                return false;
            if (controllerName == null) {
                if (other.controllerName != null)
                    return false;
            } else if (!controllerName.equals(other.controllerName))
                return false;
            return true;
        }
    }

    private static class DecoratorCacheValue {
        Decorator decorator;
        long createTimestamp = System.currentTimeMillis();

        public DecoratorCacheValue(Decorator decorator) {
            this.decorator = decorator;
        }

        public Decorator getDecorator() {
            return decorator;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createTimestamp > LAYOUT_CACHE_EXPIRATION_MILLIS;
        }
    }
}
