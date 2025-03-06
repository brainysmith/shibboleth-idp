package com.identityblitz.shibboleth.idp.lb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class LoadBalancingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancingFilter.class);
    private static boolean isLoadBalancingFilterEnabled;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        /*todo: change*/
      isLoadBalancingFilterEnabled = true;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest shibRequest = (HttpServletRequest)servletRequest;
        log.debug("LoadBalancingFilter.doFilter");

        if(isLoadBalancingFilterEnabled ) {
           log.debug("Load balancing enabled.");
           shibRequest = new LBServletRequestWrapper((HttpServletRequest)servletRequest);
           log.debug("Request modified.");
        }
        log.debug("request.getContextPath() = " + shibRequest.getContextPath());
        log.debug("request.getQueryString() = " + shibRequest.getQueryString());
        log.debug("request.getPathInfo() = " + shibRequest.getPathInfo());
        log.debug("request.getServletPath() = " + shibRequest.getServletPath());
        log.debug("request.getRequestURL() = " + shibRequest.getRequestURL().toString());
        filterChain.doFilter(shibRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
