package org.codehaus.groovy.grails.plugins.beanfields

import java.util.regex.Pattern
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.springframework.beans.BeanWrapper
import org.springframework.beans.PropertyAccessorFactory

class BeanPropertyAccessor {

    private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

    final GrailsDomainClass rootBeanClass
    final String pathFromRoot
    final GrailsDomainClass beanClass
    final String propertyName
    final value

    static BeanPropertyAccessor forBeanAndPath(GrailsApplication grailsApplication, bean, String property) {
        def domainClass = resolveDomainClass(grailsApplication, bean.getClass())
        def pathElements = property.tokenize(".")
        resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(bean), domainClass, property, domainClass, pathElements)
    }

    private static BeanPropertyAccessor resolvePropertyFromPathComponents(BeanWrapper bean, GrailsDomainClass rootClass, String pathFromRoot, GrailsDomainClass domainClass, List<String> pathElements) {
        def propertyName = pathElements.remove(0)
        def value = bean.getPropertyValue(propertyName)
        if (pathElements.empty) {
            def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (matcher.matches()) {
                new BeanPropertyAccessor(rootClass, pathFromRoot, domainClass, matcher[0][1], value)
            } else {
                new BeanPropertyAccessor(rootClass, pathFromRoot, domainClass, propertyName, value)
            }
        } else {
            def persistentProperty
            def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
            if (matcher.matches()) {
                persistentProperty = domainClass.getPersistentProperty(matcher[0][1])
            } else {
                persistentProperty = domainClass.getPersistentProperty(propertyName)
            }
            def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
            resolvePropertyFromPathComponents(PropertyAccessorFactory.forBeanPropertyAccess(value), rootClass, pathFromRoot, propertyDomainClass, pathElements)
        }
    }

    private static GrailsDomainClass resolvePropertyDomainClass(GrailsDomainClassProperty persistentProperty) {
        if (persistentProperty.embedded) {
            persistentProperty.component
        } else if (persistentProperty.association) {
            persistentProperty.referencedDomainClass
        } else {
            null
        }
    }

    private static GrailsDomainClass resolveDomainClass(grailsApplication, Class beanClass) {
        grailsApplication.getArtefact("Domain", beanClass.simpleName)
    }

    private BeanPropertyAccessor(GrailsDomainClass rootBeanClass, String pathFromRoot, GrailsDomainClass beanClass, String propertyName, value) {
        this.rootBeanClass = rootBeanClass
        this.pathFromRoot = pathFromRoot
        this.beanClass = beanClass
        this.propertyName = propertyName
        this.value = value
    }

    Class getRootBeanType() {
        rootBeanClass.clazz
    }

    Class getBeanType() {
        beanClass.clazz
    }

    Class getType() {
        persistentProperty.type
    }

    GrailsDomainClassProperty getPersistentProperty() {
        beanClass.getPersistentProperty(propertyName)
    }

    org.codehaus.groovy.grails.validation.ConstrainedProperty getConstraints() {
        beanClass.constrainedProperties[propertyName]
    }

}
