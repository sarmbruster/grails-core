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
package org.codehaus.groovy.grails.web.sitemesh;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Factory;
import com.opensymphony.module.sitemesh.HTMLPage;
import com.opensymphony.module.sitemesh.RequestConstants;
import com.opensymphony.sitemesh.*;
import com.opensymphony.sitemesh.compatability.Content2HTMLPage;
import com.opensymphony.sitemesh.compatability.DecoratorMapper2DecoratorSelector;
import com.opensymphony.sitemesh.webapp.ContainerTweaks;
import com.opensymphony.sitemesh.webapp.SiteMeshFilter;
import com.opensymphony.sitemesh.webapp.SiteMeshWebAppContext;
import grails.util.GrailsWebUtil;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.NullPersistentContextInterceptor;
import org.codehaus.groovy.grails.support.PersistenceContextInterceptor;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.exceptions.GroovyPagesException;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Extends the default page filter to overide the apply decorator behaviour
 * if the page is a GSP
 *
 * @author Graeme Rocher
 */
public class GrailsPageFilter extends SiteMeshFilter {

    private static final String ALREADY_APPLIED_KEY = "com.opensymphony.sitemesh.APPLIED_ONCE";
    private static final String HTML_EXT = ".html";
    private static final String UTF_8_ENCODING = "UTF-8";
    private static final String CONFIG_OPTION_GSP_ENCODING = "grails.views.gsp.encoding";
    public static final String GSP_SITEMESH_PAGE = GrailsPageFilter.class.getName() + ".GSP_SITEMESH_PAGE";

    private FilterConfig filterConfig;
    private ContainerTweaks containerTweaks;
    private WebApplicationContext applicationContext;
    private PersistenceContextInterceptor persistenceInterceptor = new NullPersistentContextInterceptor();
    private String defaultEncoding = UTF_8_ENCODING;
    private GroovyPagesTemplateEngine templateEngine;

    @Override
    public void init(FilterConfig fc) {
        super.init(fc);
        this.filterConfig = fc;
        containerTweaks = new ContainerTweaks();
        Config config = new Config(fc);
        //DefaultFactory defaultFactory = new DefaultFactory(config);
        Grails5535Factory defaultFactory = new Grails5535Factory(config);//TODO revert once Sitemesh bug is fixed
        config.getServletContext().setAttribute("sitemesh.factory", defaultFactory);
        defaultFactory.refresh();
        FactoryHolder.setFactory(defaultFactory);

        applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(fc.getServletContext());
        templateEngine = applicationContext.getBean(GroovyPagesTemplateEngine.BEAN_ID, GroovyPagesTemplateEngine.class);

        final GrailsApplication grailsApplication = GrailsWebUtil.lookupApplication(fc.getServletContext());
        String encoding = (String) grailsApplication.getFlatConfig().get(CONFIG_OPTION_GSP_ENCODING);
        if (encoding != null) {
            defaultEncoding = encoding;
        }

        Map<String, PersistenceContextInterceptor> interceptors = applicationContext.getBeansOfType(PersistenceContextInterceptor.class);
        if (!interceptors.isEmpty()) {
            persistenceInterceptor = interceptors.values().iterator().next();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        FactoryHolder.setFactory(null);
    }

    /*
     * TODO: This method has been copied from the parent to fix a bug in sitemesh 2.3. When sitemesh 2.4 is release this method and the two private methods below can removed
     *
     * Main method of the Filter.
     *
     * <p>Checks if the Filter has been applied this request. If not, parses the page
     * and applies {@link com.opensymphony.module.sitemesh.Decorator} (if found).
     */
    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
                throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;
        ServletContext servletContext = filterConfig.getServletContext();

        SiteMeshWebAppContext webAppContext = new SiteMeshWebAppContext(request, response, servletContext);
        ContentProcessor contentProcessor = initContentProcessor(webAppContext);
        DecoratorSelector decoratorSelector = initDecoratorSelector(webAppContext);

        if (filterAlreadyAppliedForRequest(request)) {
            // Prior to Servlet 2.4 spec, it was unspecified whether the filter should be called again upon an include().
            chain.doFilter(request, response);
            return;
        }

        if (!contentProcessor.handles(webAppContext)) {
            // Optimization: If the content doesn't need to be processed, bypass SiteMesh.
            chain.doFilter(request, response);
            return;
        }

        // clear the page in case it is already present
        request.removeAttribute(RequestConstants.PAGE);

        if (containerTweaks.shouldAutoCreateSession()) {
            request.getSession(true);
        }

        boolean dispatched = false;
        try {
            Content content = obtainContent(contentProcessor, webAppContext, request, response, chain);
            if (content == null || response.isCommitted()) {
                return;
            }

            detectContentTypeFromPage(content, response);
            Decorator decorator = decoratorSelector.selectDecorator(content, webAppContext);
            persistenceInterceptor.reconnect();

            decorator.render(content, webAppContext);
            dispatched = true;
        }
        catch (IllegalStateException e) {
            // Some containers (such as WebLogic) throw an IllegalStateException when an error page is served.
            // It may be ok to ignore this. However, for safety it is propegated if possible.
            if (!containerTweaks.shouldIgnoreIllegalStateExceptionOnErrorPage()) {
                throw e;
            }
        }
        finally {
            if (!dispatched) {
                // an error occured
                request.setAttribute(ALREADY_APPLIED_KEY, null);
            }
            if (persistenceInterceptor.isOpen()) {
                persistenceInterceptor.destroy();
            }
        }
    }

    private HTMLPage content2htmlPage(Content content) {
        HTMLPage htmlPage = null;
        if (content instanceof HTMLPage) {
            htmlPage = (HTMLPage)content;
        }
        else {
            htmlPage = new Content2HTMLPage(content);
        }
        return htmlPage;
    }

    @Override
    protected DecoratorSelector initDecoratorSelector(SiteMeshWebAppContext webAppContext) {
        // TODO: Remove heavy coupling on horrible SM2 Factory
        final Factory factory = Factory.getInstance(new Config(filterConfig));
        factory.refresh();
        return new DecoratorMapper2DecoratorSelector(factory.getDecoratorMapper()) {
            @Override
            public Decorator selectDecorator(Content content, final SiteMeshContext context) {
                SiteMeshWebAppContext siteMeshWebAppContext = (SiteMeshWebAppContext) context;
                final com.opensymphony.module.sitemesh.Decorator decorator =
                    factory.getDecoratorMapper().getDecorator(siteMeshWebAppContext.getRequest(), content2htmlPage(content));
                if (decorator == null || decorator.getPage() == null) {
                    return new GrailsNoDecorator();
                }

                return new Decorator() {
                    private void render(@SuppressWarnings("hiding") Content content, HttpServletRequest request,
                                        HttpServletResponse response, ServletContext servletContext) {

                        HTMLPage htmlPage = content2htmlPage(content);
                        request.setAttribute(RequestConstants.PAGE, htmlPage);

                        // see if the URI path (webapp) is set
                        if (decorator.getURIPath() != null) {
                            // in a security conscious environment, the servlet container
                            // may return null for a given URL
                            if (servletContext.getContext(decorator.getURIPath()) != null) {
                                servletContext = servletContext.getContext(decorator.getURIPath());
                            }
                        }
                        // get the dispatcher for the decorator
                        if (!response.isCommitted()) {
                            boolean dispatched = false;
                            try {
                                request.setAttribute(ALREADY_APPLIED_KEY, Boolean.TRUE);

                                GroovyPageView gspSpringView = new GroovyPageView();
                                gspSpringView.setServletContext(servletContext);
                                gspSpringView.setUrl(decorator.getPage());
                                gspSpringView.setApplicationContext(applicationContext);
                                gspSpringView.setTemplateEngine(templateEngine);

                                try {
                                    gspSpringView.render(Collections.<String, Object>emptyMap(), request, response);
                                    dispatched = true;
                                    if (!response.isCommitted()) {
                                        response.getWriter().flush();
                                    }
                                } catch (Exception e) {
                                    cleanRequestAttributes(request);
                                    throw new GroovyPagesException("Error applying layout : " + decorator.getPage(), e);
                                }
                            } finally {
                                if (!dispatched) {
                                    cleanRequestAttributes(request);
                                }
                            }
                        }

                        request.removeAttribute(RequestConstants.PAGE);
                    }

                    public void render(@SuppressWarnings("hiding") Content content, SiteMeshContext siteMeshContext) {
                        SiteMeshWebAppContext ctx = (SiteMeshWebAppContext) siteMeshContext;
                        render(content, ctx.getRequest(), ctx.getResponse(), ctx.getServletContext());
                    }
                };
            }
        };
    }

    private void cleanRequestAttributes(HttpServletRequest request) {
        request.removeAttribute(GrailsApplicationAttributes.PAGE_SCOPE);
        request.removeAttribute(GrailsLayoutDecoratorMapper.LAYOUT_ATTRIBUTE);
        request.setAttribute(ALREADY_APPLIED_KEY, null);
    }

    /**
     * Continue in filter-chain, writing all content to buffer and parsing
     * into returned {@link com.opensymphony.module.sitemesh.Page} object. If
     * {@link com.opensymphony.module.sitemesh.Page} is not parseable, null is returned.
     */
    private Content obtainContent(ContentProcessor contentProcessor, SiteMeshWebAppContext webAppContext,
                                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {

        Object oldGspSiteMeshPage=request.getAttribute(GSP_SITEMESH_PAGE);
        try {
            request.setAttribute(GSP_SITEMESH_PAGE, new GSPSitemeshPage());
            GrailsContentBufferingResponse contentBufferingResponse = new GrailsContentBufferingResponse(
                    response, contentProcessor, webAppContext);

            setDefaultConfiguredEncoding(request, contentBufferingResponse);
            chain.doFilter(request, contentBufferingResponse);
            // TODO: check if another servlet or filter put a page object in the request
            //            Content result = request.getAttribute(PAGE);
            //            if (result == null) {
            //                // parse the page
            //                result = pageResponse.getPage();
            //            }
            webAppContext.setUsingStream(contentBufferingResponse.isUsingStream());
            return contentBufferingResponse.getContent();
        }
        finally {
            if (oldGspSiteMeshPage != null) {
                request.setAttribute(GSP_SITEMESH_PAGE, oldGspSiteMeshPage);
            }
        }
    }

    private void setDefaultConfiguredEncoding(HttpServletRequest request, GrailsContentBufferingResponse contentBufferingResponse) {
        UrlPathHelper urlHelper = new UrlPathHelper();
        String requestURI = urlHelper.getOriginatingRequestUri(request);
        // static content?
        if (requestURI.endsWith(HTML_EXT)) {
            contentBufferingResponse.setContentType("text/html;charset="+defaultEncoding);
        }
    }

    private boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
        if (request.getAttribute(ALREADY_APPLIED_KEY) == Boolean.TRUE) {
            return true;
        }

        request.setAttribute(ALREADY_APPLIED_KEY, Boolean.TRUE);
        return false;
    }

    private void detectContentTypeFromPage(Content page, HttpServletResponse response) {
        String contentType = page.getProperty("meta.http-equiv.Content-Type");
        if (contentType != null && "text/html".equals(response.getContentType())) {
            response.setContentType(contentType);
        }
    }
}
