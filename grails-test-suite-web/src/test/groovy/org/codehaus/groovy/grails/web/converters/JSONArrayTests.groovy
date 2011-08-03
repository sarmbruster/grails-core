package org.codehaus.groovy.grails.web.converters

import static org.junit.Assert.assertEquals

import org.codehaus.groovy.grails.web.json.JSONArray
import org.junit.Test

class JSONArrayTests {

    @Test
    void testEquals() {
        assertEquals(getJSONArray(), getJSONArray())
    }

    @Test
    void testHashCode() {
        assertEquals(getJSONArray().hashCode(), getJSONArray().hashCode())
    }

    private getJSONArray() { new JSONArray(['a', 'b', 'c']) }
}
