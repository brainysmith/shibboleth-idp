package com.blitz.idm.idp.storage;

import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;
import org.opensaml.util.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filter which save storage as request attribute at the start of request processing.
 * and save session, login and logout context to storage at the end of request processing.
 * It should be installed as near as possible to the beginning of the effective filter chain.
 */
public class StorageServiceFilter implements Filter {

    public static final String STORAGE_SERVICE_ATTR_NAME = "storageService";
    /**
     * Class Logger.
     */
    private final Logger log = LoggerFactory.getLogger(StorageServiceFilter.class);

    private static ServletContext context;
    /**
     * Storage service used to store login context.
     */
    private static StorageService<?, ?> storageService;
    /**
     * Storage service used to store single logout context.
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        storageService = null;
        context = null;
    }

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        context = filterConfig.getServletContext();
        if (context == null) {
            log.error("Servlet context can't be null");
            throw new NullPointerException("Servlet context can't be null");
        }
        storageService = HttpServletHelper.getStorageService(context);
        if (storageService == null) {
            log.error("Storage service can't be null");
            throw new NullPointerException("Storage service can't be null");
        }

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            req.setAttribute(STORAGE_SERVICE_ATTR_NAME, storageService);
            log.debug("Bind storage service {} to request", storageService.getClass().getSimpleName());
            chain.doFilter(request, response);
            CacheEntryManager.cacheContext((HttpServletRequest)request, storageService);

        } catch (Throwable e) {
            log.error("Error while context caching: {} ", e.getStackTrace());
        }
    }
}