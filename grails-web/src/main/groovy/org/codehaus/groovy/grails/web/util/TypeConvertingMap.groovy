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
package org.codehaus.groovy.grails.web.util

import org.apache.commons.lang.builder.HashCodeBuilder

/**
 * An category for use with maps that want type conversion capabilities
 *
 * Type converting maps have no inherent ordering. Two maps with identical entries
 * but arranged in a different order internally are considered equal.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
class TypeConvertingMap implements Map, Cloneable {

    protected Map wrappedMap

    TypeConvertingMap() {
        this([:])
    }

    TypeConvertingMap(Map map) {
        if (map == null) map = [:]
        wrappedMap = map
    }

    boolean equals(that) {
        if (this.is(that)) {
            return true
        }

        if (that == null) {
            return false
        }

        if (getClass() != that.getClass()) {
            return false
        }

        if (this.size() != that.size()) {
            return false
        }

        if (this.empty && that.empty) {
            return true
        }

        this.entrySet() == that.entrySet()
    }

    int hashCode() {
        def builder = new HashCodeBuilder(23, 31)
        for (entry in this.entrySet()) {
            builder.append(entry)
        }
        builder.toHashCode()
    }

    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Byte getByte(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return ((Number)o).byteValue()
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Byte.parseByte(string)
                }
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
     * Helper method for obtaining Character value from parameter
     * @param name The name of the parameter
     * @return The Character value or null if there isn't one
     */
    Character getChar(String name) {
        def o = get(name)
        if (o instanceof Character) {
            return o
        }

        if (o != null) {
            String string = o.toString()
            if (string && string.size() == 1) {
                return string.charAt(0)
            }
        }
        return null
    }

    /**
     * Helper method for obtaining integer value from parameter
     * @param name The name of the parameter
     * @return The integer value or null if there isn't one
     */
    Integer getInt(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return o.intValue()
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Integer.parseInt(string)
                }
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
     * Helper method for obtaining long value from parameter
     * @param name The name of the parameter
     * @return The long value or null if there isn't one
     */
    Long getLong(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return ((Number)o).longValue()
        }

        if (o != null) {
            try {
                return Long.parseLong(o.toString())
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
    * Helper method for obtaining short value from parameter
    * @param name The name of the parameter
    * @return The short value or null if there isn't one
    */
    Short getShort(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return ((Number)o).shortValue()
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Short.parseShort(string)
                }
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
    * Helper method for obtaining double value from parameter
    * @param name The name of the parameter
    * @return The double value or null if there isn't one
    */
    Double getDouble(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return ((Number)o).doubleValue()
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Double.parseDouble(string)
                }
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Float getFloat(String name) {
        def o = get(name)
        if (o instanceof Number) {
            return ((Number)o).floatValue()
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Float.parseFloat(string)
                }
            }
            catch (NumberFormatException e) {}
        }
    }

    /**
     * Helper method for obtaining float value from parameter
     * @param name The name of the parameter
     * @return The double value or null if there isn't one
     */
    Boolean getBoolean(String name) {
        def o = get(name)
        if (o instanceof Boolean) {
            return o
        }

        if (o != null) {
            try {
                String string = o.toString()
                if (string) {
                    return Boolean.parseBoolean(string)
                }
            }
            catch (e) {}
        }
    }

    /**
     * Helper method for obtaining a list of values from parameter
     * @param name The name of the parameter
     * @return A list of values
     */
    List getList(String name) {
        def paramValues = get(name)
        if (paramValues == null) {
            return []
        }

        if (paramValues?.getClass().isArray()) {
            return Arrays.asList(paramValues)
        }

        if (paramValues instanceof Collection) {
            return new ArrayList(paramValues)
        }

        return [paramValues]
    }

    Object put(Object k, Object v) {
        wrappedMap.put(k, v)
    }

    Object remove(Object o) {
        wrappedMap.remove(o)
    }

    int size() {
        wrappedMap.size()
    }

    boolean isEmpty() {
        wrappedMap.isEmpty()
    }

    boolean containsKey(Object k) {
        wrappedMap.containsKey(k)
    }

    boolean containsValue(Object v) {
        wrappedMap.containsValue(v)
    }

    Object get(Object k) {
        wrappedMap.get(k)
    }

    void putAll(Map m) {
        wrappedMap.putAll(m)
    }

    void clear() {
        wrappedMap.clear()
    }

    Set keySet() {
        wrappedMap.keySet()
    }

    Collection values() {
        wrappedMap.values()
    }

    Set entrySet() {
        wrappedMap.entrySet()
    }
}
