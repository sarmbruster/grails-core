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
package org.codehaus.groovy.grails.plugins.web.async.api

import javax.servlet.AsyncContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestContextHolder
import javax.servlet.http.HttpServletRequest
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.plugins.web.async.GrailsAsyncContext

/**
 * Support API for async request processing
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class ControllersAsyncApi {

    /**
     * Raw access to the Servlet 3.0 startAsync method
     *
     * @return
     */

    public AsyncContext startAsync(instance) {
        GrailsWebRequest webRequest = RequestContextHolder.currentRequestAttributes()
        HttpServletRequest request = webRequest.currentRequest
        def ctx = request.startAsync(request, webRequest.currentResponse)
        request.setAttribute(GrailsApplicationAttributes.ASYNC_STARTED, true)
        return new GrailsAsyncContext(ctx, webRequest)
    }
}
