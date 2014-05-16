package com.identityblitz.shibboleth.idp.lb;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Zaitsev Igor
 * Date: 13.12.2010
 */
public class LBServletRequestWrapper extends HttpServletRequestWrapper {
    HttpServletRequest originalRequest;

    public LBServletRequestWrapper(HttpServletRequest request) {
        super(request);
        originalRequest = request;
    }


    public HttpServletRequest getOriginalRequest() {
        return  originalRequest;
    }

    @Override
    public String getAuthType() {
        return originalRequest.getAuthType();
    }

    @Override
    public Cookie[] getCookies() {
        return originalRequest.getCookies();
    }

    @Override
    public long getDateHeader(String s) {
        return originalRequest.getDateHeader(s);
    }

    @Override
    public String getHeader(String s) {
        return originalRequest.getHeader(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        return originalRequest.getHeaders(s);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return originalRequest.getHeaderNames();
    }

    @Override
    public int getIntHeader(String s) {
        return originalRequest.getIntHeader(s);
    }

    @Override
    public String getMethod() {
        return originalRequest.getMethod();
    }

    @Override
    public String getPathInfo() {
        return originalRequest.getPathInfo();
    }

    @Override
    public String getPathTranslated() {
        return originalRequest.getPathTranslated();
    }

    @Override
    public String getContextPath() {
        return originalRequest.getContextPath();
    }

    @Override
    public String getQueryString() {
        return originalRequest.getQueryString();
    }

    @Override
    public String getRemoteUser() {
        return originalRequest.getRemoteUser();
    }

    @Override
    public boolean isUserInRole(String s) {
        return originalRequest.isUserInRole(s);
    }

    @Override
    public Principal getUserPrincipal() {
        return originalRequest.getUserPrincipal();
    }

    @Override
    public String getRequestedSessionId() {
        return originalRequest.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return originalRequest.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
       StringBuffer originalUrl = originalRequest.getRequestURL();
       StringBuffer requestUrl = new StringBuffer(originalUrl);
       if(requestUrl.indexOf("https://") == -1) {
          requestUrl = new StringBuffer(originalUrl.toString().replaceAll("http", "https"));
       }
       // remove port. admins can't remove it itself yet
     	String newStr = requestUrl.toString();
		Pattern pattern = Pattern.compile(":\\d+/");
		Matcher match = pattern.matcher(newStr);
		if(match.find()) {
		 newStr = match.replaceFirst("/");
		}
       return new StringBuffer(newStr);
    }

    @Override
    public String getServletPath() {
        return originalRequest.getServletPath();
    }

    @Override
    public HttpSession getSession(boolean b) {
        return originalRequest.getSession(b);
    }

    @Override
    public HttpSession getSession() {
        return originalRequest.getSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return originalRequest.isRequestedSessionIdValid();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return originalRequest.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return originalRequest.isRequestedSessionIdFromURL();
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return originalRequest.isRequestedSessionIdFromUrl();
    }

    @Override
    public Object getAttribute(String s) {
        return originalRequest.getAttribute(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return originalRequest.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return originalRequest.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        originalRequest.setCharacterEncoding(s);
    }

    @Override
    public int getContentLength() {
        return originalRequest.getContentLength();
    }

    @Override
    public String getContentType() {
        return originalRequest.getContentType();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return originalRequest.getInputStream();
    }

    @Override
    public String getParameter(String s) {
        return originalRequest.getParameter(s);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return originalRequest.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String s) {
        return originalRequest.getParameterValues(s);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return originalRequest.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return originalRequest.getProtocol();
    }

    @Override
    public String getScheme() {
        return "https";
    }

    @Override
    public String getServerName() {
        return originalRequest.getServerName();
    }

    @Override
    public int getServerPort() {
        return originalRequest.getServerPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return originalRequest.getReader();
    }

    @Override
    public String getRemoteAddr() {
        return originalRequest.getRemoteAddr();
    }

    @Override
    public String getRemoteHost() {
        return originalRequest.getRemoteHost();
    }

    @Override
    public void setAttribute(String s, Object o) {
        originalRequest.setAttribute(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        originalRequest.removeAttribute(s);
    }

    @Override
    public Locale getLocale() {
        return originalRequest.getLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return originalRequest.getLocales();
    }

    @Override
    public boolean isSecure() {
        return originalRequest.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return originalRequest.getRequestDispatcher(s);
    }

    public String toString() {
        return originalRequest.toString();
    }

    public int hashCode() {
     return originalRequest.hashCode();
    }

    public boolean equals(Object obj) {
     return originalRequest.equals(obj);   
    }
}
