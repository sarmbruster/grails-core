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
package org.codehaus.groovy.grails.commons;

import grails.persistence.Entity;
import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.validation.ConstraintEvalUtils;

import java.util.Map;

/**
 * Evaluates the conventions that define a domain class in Grails.
 *
 * @author Graeme Rocher
 * @author Marc Palmer (marc@anyware.co.uk)
 */
public class DomainClassArtefactHandler extends ArtefactHandlerAdapter implements GrailsApplicationAware {

    public static final String TYPE = "Domain";

    private Map<String, Object> defaultConstraints;
    public DomainClassArtefactHandler() {
        super(TYPE, GrailsDomainClass.class, DefaultGrailsDomainClass.class, null, true);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        if (grailsApplication != null) {
            defaultConstraints = ConstraintEvalUtils.getDefaultConstraints(grailsApplication.getConfig());
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public GrailsClass newArtefactClass(Class artefactClass) {
        return new DefaultGrailsDomainClass(artefactClass, defaultConstraints);
    }

    /**
     * Sets up the relationships between the domain classes, this has to be done after
     * the intial creation to avoid looping
     */
    @Override
    public void initialize(ArtefactInfo artefacts) {
        log.debug("Configuring domain class relationships");
        GrailsDomainConfigurationUtil.configureDomainClassRelationships(
                artefacts.getGrailsClasses(),
                artefacts.getGrailsClassesByName());
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isArtefactClass(Class clazz) {
        return isDomainClass(clazz);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean isDomainClass(Class clazz) {
        // it's not a closure
        if (clazz == null) return false;

        if (clazz.getAnnotation(Entity.class) != null) {
            return true;
        }

        if (Closure.class.isAssignableFrom(clazz)) {
            return false;
        }

        if (GrailsClassUtils.isJdk5Enum(clazz)) return false;

        Class testClass = clazz;
        while (testClass != null && !testClass.equals(GroovyObject.class) && !testClass.equals(Object.class)) {
            try {
                // make sure the identify and version field exist
                testClass.getDeclaredField(GrailsDomainClassProperty.IDENTITY);
                testClass.getDeclaredField(GrailsDomainClassProperty.VERSION);

                // passes all conditions return true
                return true;
            }
            catch (SecurityException e) {
                // ignore
            }
            catch (NoSuchFieldException e) {
                // ignore
            }
            testClass = testClass.getSuperclass();
        }

        return false;
    }
}
