package org.codehaus.groovy.grails.plugins.beanfields

import java.util.regex.Pattern
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.FieldError
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.*

interface BeanPropertyAccessor {
	def getRootBean()
	GrailsDomainClass getRootBeanClass()
	String getPathFromRoot()
	GrailsDomainClass getBeanClass()
	String getPropertyName()
	def getValue()
	Class getRootBeanType()
	Class getBeanType()
	Class getType()
	GrailsDomainClassProperty getPersistentProperty()
	ConstrainedProperty getConstraints()
	String getLabelKey()
	String getDefaultLabel()
	List<FieldError> getErrors()
}

class BeanPropertyAccessorFactory implements GrailsApplicationAware {

	private static final Pattern INDEXED_PROPERTY_PATTERN = ~/^(\w+)\[(.+)\]$/

	GrailsApplication grailsApplication

	BeanPropertyAccessor accessorFor(bean, String property) {
		def rootBeanClass = resolveDomainClass(bean.getClass())
		def pathElements = property.tokenize(".")
		resolvePropertyFromPathComponents(bean, PropertyAccessorFactory.forBeanPropertyAccess(bean), rootBeanClass, property, rootBeanClass, pathElements)
	}

	private BeanPropertyAccessor resolvePropertyFromPathComponents(rootBean, BeanWrapper bean, GrailsDomainClass rootBeanClass, String pathFromRoot, GrailsDomainClass beanClass, List<String> pathElements) {
		def propertyName = pathElements.remove(0)
		def value = bean?.getPropertyValue(propertyName)
		if (pathElements.empty) {
			def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
			if (matcher.matches()) {
				new BeanPropertyAccessorImpl(rootBean, rootBeanClass, pathFromRoot, beanClass, matcher[0][1], value)
			} else {
				new BeanPropertyAccessorImpl(rootBean, rootBeanClass, pathFromRoot, beanClass, propertyName, value)
			}
		} else {
			def persistentProperty
			def matcher = propertyName =~ INDEXED_PROPERTY_PATTERN
			if (matcher.matches()) {
				persistentProperty = beanClass.getPersistentProperty(matcher[0][1])
			} else {
				persistentProperty = beanClass.getPersistentProperty(propertyName)
			}
			def propertyDomainClass = resolvePropertyDomainClass(persistentProperty)
			resolvePropertyFromPathComponents(rootBean, value ? PropertyAccessorFactory.forBeanPropertyAccess(value) : null, rootBeanClass, pathFromRoot, propertyDomainClass, pathElements)
		}
	}

	private GrailsDomainClass resolvePropertyDomainClass(GrailsDomainClassProperty persistentProperty) {
		if (persistentProperty.embedded) {
			persistentProperty.component
		} else if (persistentProperty.association) {
			persistentProperty.referencedDomainClass
		} else {
			null
		}
	}

	private GrailsDomainClass resolveDomainClass(Class beanClass) {
		grailsApplication.getDomainClass(beanClass.name)
	}

	static class BeanPropertyAccessorImpl implements BeanPropertyAccessor {

		final rootBean
		final GrailsDomainClass rootBeanClass
		final String pathFromRoot
		final GrailsDomainClass beanClass
		final String propertyName
		final value

		BeanPropertyAccessorImpl(rootBean, GrailsDomainClass rootBeanClass, String pathFromRoot, GrailsDomainClass beanClass, String propertyName, value) {
			this.rootBean = rootBean
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

		ConstrainedProperty getConstraints() {
			beanClass.constrainedProperties[propertyName]
		}

		String getLabelKey() {
			"${beanClass.clazz.simpleName}.${propertyName}.label"
		}

		String getDefaultLabel() {
			persistentProperty.naturalName
		}

		List<FieldError> getErrors() {
			rootBean.errors.getFieldErrors(pathFromRoot)
		}

	}
}
