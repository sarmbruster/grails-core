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
package grails.gsp

import java.security.Principal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.servlet.RequestDispatcher
import javax.servlet.ServletContext
import javax.servlet.ServletInputStream
import javax.servlet.ServletOutputStream
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import org.apache.commons.collections.iterators.IteratorEnumeration
import org.codehaus.groovy.grails.web.pages.FastStringWriter
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriSupport
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.web.context.ServletContextAware
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.ServletContextResourceLoader
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageResourceScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageCompiledScriptSource
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator

/**
 * Simplified API for rendering GSP pages from services, jobs and other non-request classes.
 *
 *
 * @author Graeme Rocher
 * @since 1.4
 */
class PageRenderer implements ApplicationContextAware, ServletContextAware{

    private GroovyPagesTemplateEngine templateEngine
    GrailsConventionGroovyPageLocator groovyPageLocator
    ApplicationContext applicationContext
    ServletContext servletContext

    PageRenderer(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine
    }


    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    String render(Map args) {
        def fsw = new FastStringWriter()
        renderViewToWriter(args, fsw)
        return fsw.toString()
    }

    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param writer The target writer
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */
    void renderTo(Map args, Writer writer) {
        renderViewToWriter(args, writer)
    }
    /**
     * Renders a page and returns the contents
     *
     * @param args The named arguments
     * @param stream The target stream
     *
     * @arg view The view to render. Must be an absolute view path since the controller name is unknown.
     * @arg template The template to render. Must be an absolute template path since the controller name is unknown.
     * @arg model The model to use for rendering
     *
     * @return The resulting string contents
     */

    void renderTo(Map args, OutputStream stream) {
        renderTo(args, new OutputStreamWriter(stream))
    }


    private void renderViewToWriter(Map args, Writer writer) {
        def source = null
        if(args.view) {
           source = groovyPageLocator.findViewByPath(args.view.toString())
        }
        else if(args.template) {
            source = groovyPageLocator.findTemplateByPath(args.template.toString())
        }
        if (source != null) {
            try {
                def webRequest = new GrailsWebRequest(new PageRenderRequest(source.URI), new PageRenderResponse(writer instanceof PrintWriter ? writer : new PrintWriter(writer)), servletContext, applicationContext)
                RequestContextHolder.setRequestAttributes(webRequest)
                def template = null
                if(source instanceof GroovyPageResourceScriptSource)
                    template = templateEngine.createTemplate(source.resource, true)
                else if(source instanceof GroovyPageCompiledScriptSource) {
                    template = templateEngine.createTemplate(source.compiledClass)
                }
                if(template != null) {
                    final writable = template.make(args.model ?: [:])
                    writable.writeTo(writer)
                }
            } finally {
                RequestContextHolder.setRequestAttributes(null)
            }

        }
    }

    protected GroovyPageScriptSource findResource(String basePath) {
        return groovyPageLocator.findViewByPath(basePath)
    }

    /*
     * A request object used during the GSP rendering pipeline for render operations outside a web request
     */
    class PageRenderRequest implements HttpServletRequest {


        PageRenderRequest(String requestURI) {
            this.requestURI = requestURI
        }

        def params = new ConcurrentHashMap()
        def attributes = new ConcurrentHashMap()


        String contentType
        String requestURI
        String characterEncoding = "UTF-8"

        @Override
        String getAuthType() { null }

        @Override
        Cookie[] getCookies() { return new Cookie[0] }

        @Override
        long getDateHeader(String name) { -1L }

        @Override
        String getHeader(String name) { null }

        @Override
        Enumeration getHeaders(String name) {
            return new IteratorEnumeration([].iterator())
        }

        @Override
        Enumeration getHeaderNames() {
            return new IteratorEnumeration([].iterator())
        }

        @Override
        int getIntHeader(String name) { -1 }

        @Override
        String getMethod() {"GET"}

        @Override
        String getPathInfo() {""}

        @Override
        String getPathTranslated() {""}

        @Override
        String getContextPath() {"/"}

        @Override
        String getQueryString() { ""}

        @Override
        String getRemoteUser() { null }

        @Override
        boolean isUserInRole(String role) { false }

        @Override
        Principal getUserPrincipal() { null }

        @Override
        String getRequestedSessionId() { null }


        @Override
        StringBuffer getRequestURL() {
            return new StringBuffer(getRequestURI())
        }

        @Override
        String getServletPath() {
            return "/"
        }

        @Override
        HttpSession getSession(boolean create) { throw new UnsupportedOperationException("You cannot use the session in non-request rendering operations") }

        @Override
        HttpSession getSession() { throw new UnsupportedOperationException("You cannot use the session in non-request rendering operations") }

        @Override
        boolean isRequestedSessionIdValid() { true }

        @Override
        boolean isRequestedSessionIdFromCookie() { false }

        @Override
        boolean isRequestedSessionIdFromURL() { false }

        @Override
        boolean isRequestedSessionIdFromUrl() { false }

        @Override
        Object getAttribute(String name) {
            return attributes[name]
        }

        @Override
        Enumeration getAttributeNames() {
            return attributes.keys()
        }


        @Override
        int getContentLength() { 0 }


        @Override
        ServletInputStream getInputStream() {
            throw new UnsupportedOperationException("You cannot read the input stream in non-request rendering operations")
        }

        @Override
        String getParameter(String name) {
            return params[name]
        }

        @Override
        Enumeration getParameterNames() {
            return params.keys()
        }

        @Override
        String[] getParameterValues(String name) {
            return new String[0]
        }

        @Override
        Map getParameterMap() {
            return params
        }

        @Override
        String getProtocol() {
            throw new UnsupportedOperationException("You cannot read the protocol in non-request rendering operations")
        }

        @Override
        String getScheme() {
            throw new UnsupportedOperationException("You cannot read the scheme in non-request rendering operations")
        }

        @Override
        String getServerName() {
            throw new UnsupportedOperationException("You cannot read server name in non-request rendering operations")
        }

        @Override
        int getServerPort() {
            throw new UnsupportedOperationException("You cannot read the server port in non-request rendering operations")
        }

        @Override
        BufferedReader getReader() {
            throw new UnsupportedOperationException("You cannot read input in non-request rendering operations")
        }

        @Override
        String getRemoteAddr() {
            throw new UnsupportedOperationException("You cannot read the remote address in non-request rendering operations")
        }

        @Override
        String getRemoteHost() {
            throw new UnsupportedOperationException("You cannot read the remote host in non-request rendering operations")
        }

        @Override
        void setAttribute(String name, Object o) {
            attributes[name] = o
        }

        @Override
        void removeAttribute(String name) {
            attributes.remove name
        }

        @Override
        Locale getLocale() {
            return Locale.getDefault()
        }

        @Override
        Enumeration getLocales() {
            return new IteratorEnumeration(Locale.getAvailableLocales().iterator())
        }

        @Override
        boolean isSecure() { false }

        @Override
        RequestDispatcher getRequestDispatcher(String path) {
            throw new UnsupportedOperationException("You cannot use the request dispatcher in non-request rendering operations")
        }

        @Override
        String getRealPath(String path) {
            return requestURI
        }

        @Override
        int getRemotePort() {
            throw new UnsupportedOperationException("You cannot read the remote port in non-request rendering operations")
        }

        @Override
        String getLocalName() {
            return "localhost"
        }

        @Override
        String getLocalAddr() {
            return "127.0.0.1"
        }

        @Override
        int getLocalPort() {
            return 80
        }

    }


    class PageRenderResponse implements HttpServletResponse {

        String characterEncoding = "UTF-8"
        String contentType
        Locale locale = Locale.getDefault()
        PrintWriter writer
        int bufferSize = 0


        PageRenderResponse(PrintWriter writer) {
            this.writer = writer
        }

        @Override
        void addCookie(Cookie cookie) {
            // no-op
        }

        @Override
        boolean containsHeader(String name) { false }

        @Override
        String encodeURL(String url) { url }

        @Override
        String encodeRedirectURL(String url) { url }

        @Override
        String encodeUrl(String url) { url }

        @Override
        String encodeRedirectUrl(String url) { url }

        @Override
        void sendError(int sc, String msg) {
            // no-op
        }

        @Override
        void sendError(int sc) {
            // no-op
        }

        @Override
        void sendRedirect(String location) {
            // no-op
        }

        @Override
        void setDateHeader(String name, long date) {
            // no-op
        }

        @Override
        void addDateHeader(String name, long date) {
            // no-op
        }

        @Override
        void setHeader(String name, String value) {
            // no-op
        }

        @Override
        void addHeader(String name, String value) {
            // no-op
        }

        @Override
        void setIntHeader(String name, int value) {
            // no-op
        }

        @Override
        void addIntHeader(String name, int value) {
            // no-op
        }

        @Override
        void setStatus(int sc) {
            // no-op
        }

        @Override
        void setStatus(int sc, String sm) {
            // no-op
        }


        @Override
        ServletOutputStream getOutputStream() {
            throw new UnsupportedOperationException("You cannot use the OutputStream in non-request rendering operations. Use getWriter() instead")
        }

        @Override
        void setContentLength(int len) {
            // no-op
        }

        @Override
        void flushBuffer() {
           // no-op
        }

        @Override
        void resetBuffer() {
           // no-op
        }

        @Override
        boolean isCommitted() { false }

        @Override
        void reset() {
            // no-op
        }


    }
}
