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
package org.codehaus.groovy.grails.web.errors;

import grails.util.Environment;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.exceptions.DefaultStackTraceFilterer;
import org.codehaus.groovy.grails.exceptions.GrailsRuntimeException;
import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.mapping.UrlMappingInfo;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.GrailsMVCException;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

/**
 * Wraps any runtime exceptions with a GrailsWrappedException instance.
 *
 * @author Graeme Rocher
 */
public class GrailsExceptionResolver extends SimpleMappingExceptionResolver implements ServletContextAware, GrailsApplicationAware {

    public static final String EXCEPTION_ATTRIBUTE = "exception";
    private ServletContext servletContext;

    private static final Log LOG = LogFactory.getLog(GrailsExceptionResolver.class);
    private GrailsApplication grailsApplication;
    private StackTraceFilterer stackFilterer;

    /* (non-Javadoc)
     * @see org.springframework.web.servlet.handler.SimpleMappingExceptionResolver#resolveException(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object, java.lang.Exception)
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        if ((ex instanceof InvokerInvocationException)||(ex instanceof GrailsMVCException)) {
            Throwable t = getRootCause(ex);
            if (t instanceof Exception) {
                ex = (Exception) t;
            }
        }

        ModelAndView mv = super.resolveException(request, response, handler, ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // expose the servlet 2.3 specs status code request attribute as 500
        request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        stackFilterer.filter(ex, true);

        GrailsWrappedRuntimeException gwrex = new GrailsWrappedRuntimeException(servletContext, ex);
        mv.addObject(EXCEPTION_ATTRIBUTE,gwrex);

        UrlMappingsHolder urlMappings = null;
        try {
            urlMappings = WebUtils.lookupUrlMappings(servletContext);
        }
        catch (Exception e) {
            // ignore, no app ctx in this case.
        }

        LOG.error(getRequestLogMessage(ex, request), ex);

        if (urlMappings != null) {
            UrlMappingInfo info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
            if (info == null) {
                info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        getRootCause(ex));
            }
            if (info == null) {
                info = urlMappings.matchStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            try {
                if (info != null && info.getViewName() != null) {
                    ViewResolver viewResolver = WebUtils.lookupViewResolver(servletContext);
                    View v = WebUtils.resolveView(request, info, info.getViewName(),viewResolver);
                    if (v != null) {
                        mv.setView(v);
                    }
                }
                else if (info != null && info.getControllerName() != null) {
                    String uri;
                    if (request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) {
                        uri = (String)request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
                    }
                    else {
                        uri = request.getRequestURI();
                    }

                    if (!response.isCommitted()) {
                        info.configure(WebUtils.retrieveGrailsWebRequest());
                        String forwardUrl = WebUtils.forwardRequestForUrlMappingInfo(
                                request, response, info, mv.getModel());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Matched URI [" + uri + "] to URL mapping [" + info +
                                    "], forwarding to [" + forwardUrl + "] with response [" + response.getClass() + "]");
                        }
                        // return an empty ModelAndView since the error handler has been processed
                        return new ModelAndView();
                    }
                }
            }
            catch (Exception e) {
                LOG.error("Unable to render errors view: " + e.getMessage(), e);
                throw new GrailsRuntimeException(e);
            }
        }

        return mv;
    }

    /**
     * Obtains the root cause of the given exception
     * @param ex The exception
     * @return The root cause
     */
    public static Throwable getRootCause(Throwable ex) {
        while (ex.getCause() != null && !ex.equals(ex.getCause())) {
            ex = ex.getCause();
        }
        return ex;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public static int extractLineNumber(CompilationFailedException e) {
        int lineNumber = -1;
        if (e instanceof MultipleCompilationErrorsException) {
            MultipleCompilationErrorsException mcee = (MultipleCompilationErrorsException)e;
            Object message = mcee.getErrorCollector().getErrors().iterator().next();
            if (message instanceof SyntaxErrorMessage) {
                SyntaxErrorMessage sem = (SyntaxErrorMessage)message;
                lineNumber = sem.getCause().getLine();
            }
        }
        return lineNumber;
    }

    public static RuntimeException getFirstRuntimeException(Throwable e) {
        if (e instanceof RuntimeException) return (RuntimeException) e;

        Throwable ex = e;
        while (ex.getCause() != null && !ex.equals(ex.getCause())) {
            ex = ex.getCause();
            if (ex instanceof RuntimeException) return (RuntimeException) ex;
        }
        return null;
    }

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private String getRequestLogMessage(String exceptionName, HttpServletRequest request, String message) {
        StringBuilder sb = new StringBuilder();


        sb.append(exceptionName)
          .append(" occurred when processing request: ")
          .append("[").append(request.getMethod().toUpperCase()).append("] ");

        if (request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) {
            sb.append(request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE));
        } else {
            sb.append(request.getRequestURI());
        }

        @SuppressWarnings("rawtypes") Map flatConfig = grailsApplication != null ? grailsApplication.getFlatConfig() : Collections.emptyMap();
        final boolean shouldLogRequestParameters;

        if (flatConfig.containsKey("grails.exceptionresolver.logRequestParameters")) {
            shouldLogRequestParameters = Boolean.TRUE.equals(flatConfig.get("grails.exceptionresolver.logRequestParameters"));
        } else {
            shouldLogRequestParameters = Environment.getCurrent() == Environment.DEVELOPMENT;
        }
        if (shouldLogRequestParameters) {
            @SuppressWarnings("unchecked")
            Enumeration<String> params = request.getParameterNames();

            if (params.hasMoreElements()) {
                String param;
                String values[];
                int i;

                sb.append(" - parameters:");
                @SuppressWarnings("unchecked")
                List<String> blackList = (List<String>) flatConfig.get(
                                "grails.exceptionresolver.params.exclude");

                if (blackList == null) {
                    blackList = Collections.emptyList();
                }
                while (params.hasMoreElements()) {
                    param = params.nextElement();
                    values = request.getParameterValues(param);

                    if (values != null) {
                        for (i = 0; i < values.length; i++) {
                            sb.append(LINE_SEPARATOR).append(param).append(": ");

                            if (blackList.contains(param)) {
                                sb.append("***");
                            } else {
                                sb.append(values[i]);
                            }
                        }
                    }
                }
            }
        }

        sb.append(LINE_SEPARATOR);
        if(message != null) {
            sb.append(message).append(". ");
        }
        sb.append("Stacktrace follows:");

        return sb.toString();
    }

    public String getRequestLogMessage(Throwable e, HttpServletRequest request) {
        Throwable cause = getRootCause(e);
        String exceptionName = cause.getClass().getSimpleName();
        return getRequestLogMessage(exceptionName, request, cause.getMessage());
    }

    public String getRequestLogMessage(HttpServletRequest request) {
        return getRequestLogMessage("Exception", request, null);
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        createStackFilterer();
    }

    private void createStackFilterer() {
        try {
            stackFilterer = (StackTraceFilterer)GrailsClassUtils.instantiateFromFlatConfig(
                    grailsApplication.getFlatConfig(), "grails.logging.stackTraceFiltererClass", DefaultStackTraceFilterer.class.getName());
        }
        catch (Throwable t) {
            logger.error("Problem instantiating StackTracePrinter class, using default: " + t.getMessage());
            stackFilterer = new DefaultStackTraceFilterer();
        }
    }
}
