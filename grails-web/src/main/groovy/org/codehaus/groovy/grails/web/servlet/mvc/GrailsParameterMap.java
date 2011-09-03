/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package org.codehaus.groovy.grails.web.servlet.mvc;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.binding.GrailsDataBinder;
import org.codehaus.groovy.grails.web.binding.StructuredDateEditor;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.codehaus.groovy.grails.web.util.TypeConvertingMap;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * A parameter map class that allows mixing of request parameters and controller parameters. If a controller
 * parameter is set with the same name as a request parameter the controller parameter value is retrieved.
 *
 * @author Graeme Rocher
 * @author Kate Rhodes
 * @author Lari Hotari
 *
 * @since Oct 24, 2005
 */
public class GrailsParameterMap extends TypeConvertingMap implements Cloneable {

    private static final Log LOG = LogFactory.getLog(GrailsParameterMap.class);
    public static final String REQUEST_BODY_PARSED = "org.codehaus.groovy.grails.web.REQUEST_BODY_PARSED";

    private HttpServletRequest request;

    /**
     * Does not populate the GrailsParameterMap from the request but instead uses the supplied values.
     *
     * @param values The values to populate with
     * @param request The request object
     */
    public GrailsParameterMap(Map values,HttpServletRequest request) {
        this.request = request;
        this.wrappedMap.putAll(values);
    }

    /**
     * Creates a GrailsParameterMap populating from the given request object
     * @param request The request object
     */
    public GrailsParameterMap(HttpServletRequest request) {
        this.request = request;
        final Map requestMap = new LinkedHashMap(request.getParameterMap());
        if (requestMap.size() == 0 && "PUT".equals(request.getMethod()) && request.getAttribute(REQUEST_BODY_PARSED) == null) {
            // attempt manual parse of request body. This is here because some containers don't parse the request body automatically for PUT request
            String contentType = request.getContentType();
            if ("application/x-www-form-urlencoded".equals(contentType)) {
                try {
                	String contents=IOUtils.toString(request.getReader());
                    request.setAttribute(REQUEST_BODY_PARSED, true);
                    requestMap.putAll(org.codehaus.groovy.grails.web.util.WebUtils.fromQueryString(contents));
                } catch (Exception e) {
                    LOG.error("Error processing form encoded PUT request", e);
                }
            }

        }
        if (request instanceof MultipartHttpServletRequest) {
            Map<String,MultipartFile> fileMap = ((MultipartHttpServletRequest)request).getFileMap();
            for(Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                requestMap.put(entry.getKey(), entry.getValue());
            }
        }
        updateNestedKeys(requestMap);
    }

    void updateNestedKeys(Map keys) {
        for (Object keyObject : keys.keySet()) {
        	String key = (String)keyObject;
            Object paramValue = getParameterValue(keys, key);

            this.wrappedMap.put(key, paramValue);
            processNestedKeys(this.request, keys, key, key, this.wrappedMap);
        }
    }

    public Object clone() {
        return new GrailsParameterMap(new LinkedHashMap(this.wrappedMap), request);
    }

    private Object getParameterValue(Map requestMap, String key) {
        Object paramValue = requestMap.get(key);
        if (paramValue instanceof String[]) {
            if (((String[])paramValue).length == 1) {
                paramValue = ((String[])paramValue)[0];
            }
        }
        return paramValue;
    }

    /*
     * Builds up a multi dimensional hash structure from the parameters so that nested keys such as
     * "book.author.name" can be addressed like params['author'].name
     *
     * This also allows data binding to occur for only a subset of the properties in the parameter map.
     */
    private void processNestedKeys(HttpServletRequest request, Map requestMap, String key,
            String nestedKey, Map nestedLevel) {
        final int nestedIndex = nestedKey.indexOf('.');
        if (nestedIndex > -1) {
            // We have at least one sub-key, so extract the first element
            // of the nested key as the prfix. In other words, if we have
            // 'nestedKey' == "a.b.c", the prefix is "a".
            String nestedPrefix = nestedKey.substring(0, nestedIndex);
            boolean prefixedByUnderscore = false;

            // Use the same prefix even if it starts with an '_'
            if (nestedPrefix.startsWith("_")) {
                prefixedByUnderscore = true;
                nestedPrefix = nestedPrefix.substring(1);
            }
            // Let's see if we already have a value in the current map for the prefix.
            Object prefixValue = nestedLevel.get(nestedPrefix);
            if (prefixValue == null) {
                // No value. So, since there is at least one sub-key,
                // we create a sub-map for this prefix.
            	
                prefixValue = new GrailsParameterMap(new LinkedHashMap(), request);
                nestedLevel.put(nestedPrefix, prefixValue);
            }

            // If the value against the prefix is a map, then we store the sub-keys in that map.
            if (prefixValue instanceof Map) {
                Map nestedMap = (Map)prefixValue;
                if (nestedIndex < nestedKey.length()-1) {
                    String remainderOfKey = nestedKey.substring(nestedIndex + 1, nestedKey.length());
                    // GRAILS-2486 Cascade the '_' prefix in order to bind checkboxes properly
                    if (prefixedByUnderscore) {
                        remainderOfKey = '_' + remainderOfKey;
                    }
                    nestedMap.put(remainderOfKey,getParameterValue(requestMap, key));
                    if (remainderOfKey.indexOf('.') >-1) {
                        processNestedKeys(request, requestMap, key, remainderOfKey, nestedMap);
                    }
                }
            }
        }
    }

    /**
     * @return Returns the request.
     */
    public HttpServletRequest getRequest() { 
    	return request; 
    }

    private Map nestedDateMap = new LinkedHashMap();

    public Object get(Object key) {
        // removed test for String key because there
        // should be no limitations on what you shove in or take out
        Object returnValue = null;
        if (nestedDateMap.containsKey(key)) {
            returnValue = nestedDateMap.get(key);
        } else {
        	returnValue = this.wrappedMap.get(key);
        	if (returnValue instanceof String[]) {
	            String[] valueArray = (String[])returnValue;
	            if (valueArray.length == 1) {
	                returnValue = valueArray[0];
	            } else {
	                returnValue = valueArray;
	            }
        	}
        }
        if ("date.struct".equals(returnValue)) {
            returnValue = lazyEvaluateDateParam(key);
            nestedDateMap.put(key, returnValue);
        }
        return returnValue;
    }

    private Date lazyEvaluateDateParam(Object key) {
        // parse date structs automatically
        Map dateParams = new LinkedHashMap();
        for (Object entryObj : entrySet()) {
        	Map.Entry entry = (Map.Entry)entryObj;
            Object entryKey = entry.getKey();
            if (entryKey instanceof String) {
                String paramName = (String)entryKey;
                final String prefix = key + "_";
                if (paramName.startsWith(prefix)) {
                    dateParams.put(paramName.substring(prefix.length(), paramName.length()), entry.getValue());
                }
            }
        }

        DateFormat dateFormat = new SimpleDateFormat(GrailsDataBinder.DEFAULT_DATE_FORMAT,
                LocaleContextHolder.getLocale());
        StructuredDateEditor editor = new StructuredDateEditor(dateFormat, true);
        try {
            return (Date)editor.assemble(Date.class, dateParams);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Object put(Object key, Object value) {
        if (value instanceof CharSequence) value = value.toString();
        if (nestedDateMap.containsKey(key)) nestedDateMap.remove(key);
        return this.wrappedMap.put(key, value);
    }

    public Object remove(Object key) {
        nestedDateMap.remove(key);
        return this.wrappedMap.remove(key);
    }

    public void putAll(Map map) {
        for (Object entryObj : map.entrySet()) {
        	Map.Entry entry = (Map.Entry)entryObj;
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Converts this parameter map into a query String. Note that this will flatten nested keys separating them with the
     * . character and URL encode the result
     *
     * @return A query String starting with the ? character
     */
    public String toQueryString() {
        String encoding = request.getCharacterEncoding();
        try {
            return WebUtils.toQueryString(this,encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new ControllerExecutionException("Unable to convert parameter map [" + this +
                 "] to a query string: " + e.getMessage(), e);
        }
    }
}
