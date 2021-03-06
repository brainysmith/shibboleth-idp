/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.util;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.util.URLBuilder;
import org.opensaml.util.storage.StorageService;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine;
import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.SAMLMDRelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.session.SessionManager;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContextEntry;
import edu.internet2.middleware.shibboleth.idp.profile.IdPProfileHandlerManager;
import edu.internet2.middleware.shibboleth.idp.session.Session;

import java.util.UUID;

/** A helper class that provides access to internal state from Servlets and hence also JSPs. */
public class HttpServletHelper {

    /** Name of the context initialization parameter that stores the domain to use for all cookies. */
    public static final String COOKIE_DOMAIN_PARAM = "cookieDomain";

    /** Name of the cookie containing the IdP session ID: {@value} . */
    public static final String IDP_SESSION_COOKIE = "_idp_session";

    /** Name of the key to the current authentication login context: {@value} . */
    public static final String LOGIN_CTX_KEY_NAME = "_idp_authn_lc_key";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine} service: {@value} . */
    public static final String ATTRIBUTE_FILTER_ENGINE_SID_CTX_PARAM = "AttributeFilterEngineId";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver} service: {@value} . */
    public static final String ATTRIBUTE_RESOLVER_SID_CTX_PARAM = "AttributeResolverId";

    /**
     * {@link ServletContext} parameter name bearing the name of the {@link StorageService} partition into which
     * {@link LoginContext}s are stored: {@value} .
     */
    public static final String LOGIN_CTX_PARTITION_CTX_PARAM = "loginContextPartitionName";

    /** {@link ServletContext} parameter name bearing the ID of the {@link IdPProfileHandlerManager} service: {@value} . */
    public static final String PROFILE_HANDLER_MNGR_SID_CTX_PARAM = "ProfileHandlerMngrId";

    /**
     * {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} service: * *
     * * {@value} .
     */
    public static final String RP_CONFIG_MNGR_SID_CTX_PARAM = "RelyingPartyConfigurationManagerId";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority} service: {@value} . */
    public static final String SAML1_AA_SID_CTX_PARAM = "SAML1AttributeAuthorityId";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority} service: {@value} . */
    public static final String SAML2_AA_SID_CTX_PARAM = "SAML2AttributeAuthorityId";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.session.SessionManager} service: {@value} . */
    public static final String SESSION_MNGR_SID_CTX_PARAM = "SessionManagerId";

    /** {@link ServletContext} parameter name bearing the ID of the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority} service: {@value} . */
    public static final String STORAGE_SERVICE_SID_CTX_PARAM = "StorageServiceId";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_ATTRIBUTE_FILTER_ENGINE_SID = "shibboleth.AttributeFilterEngine";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_ATTRIBUTE_RESOLVER_SID = "shibboleth.AttributeResolver";

    /** Default name for the {@link StorageService} partition which holds {@link LoginContext}s: {@value} . */
    public static final String DEFAULT_LOGIN_CTX_PARITION = "loginContexts";

    /** Default ID by which the {@link IdPProfileHandlerManager} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_PROFILE_HANDLER_MNGR_SID = "shibboleth.HandlerManager";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_RP_CONFIG_MNGR_SID = "shibboleth.RelyingPartyConfigurationManager";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_SAML1_AA_SID = "shibboleth.SAML1AttributeAuthority";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_SAML2_AA_SID = "shibboleth.SAML2AttributeAuthority";

    /** Default ID by which the {@link edu.internet2.middleware.shibboleth.common.session.SessionManager} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_SESSION_MNGR_SID = "shibboleth.SessionManager";

    /** Default ID by which the {@link StorageService} is know within the Servlet context: {@value} . */
    public static final String DEFAULT_STORAGE_SERVICE_SID = "shibboleth.StorageService";

    /** Class logger. */
    private static final Logger log = LoggerFactory.getLogger(HttpServletHelper.class);

    /**
     * Binds a {@link LoginContext} to the current request.
     * 
     * @param loginContext login context to be bound
     * @param httpRequest current HTTP request
     * 
     * @deprecated
     */
    public static void bindLoginContext(LoginContext loginContext, HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        httpRequest.setAttribute(LOGIN_CTX_KEY_NAME, loginContext);
    }

    /**
     * Binds a {@link LoginContext} to the issuer of the current request. The binding is done by creating a random UUID,
     * placing that in a cookie in the request, and storing the context in to the storage service under that key.
     * 
     * @param loginContext the login context to be bound
     * @param storageService the storage service which will hold the context
     * @param context the Servlet context
     * @param httpRequest the current HTTP request
     * @param httpResponse the current HTTP response
     */
    public static void bindLoginContext(LoginContext loginContext, StorageService storageService,
            ServletContext context, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (loginContext == null) {
            return;
        }

        String partition = getContextParam(context, LOGIN_CTX_PARTITION_CTX_PARAM, DEFAULT_LOGIN_CTX_PARITION);
        log.debug("LoginContext partition: {}", partition);

        String contextKey = UUID.randomUUID().toString();
        while (storageService.contains(partition, contextKey)) {
            contextKey = UUID.randomUUID().toString();
        }
        LoginContextEntry entry = new LoginContextEntry(loginContext, 1800000);
        log.debug("Storing LoginContext to StorageService partition {}, key {}", partition, loginContext.getContextKey());
        storageService.put(partition, loginContext.getContextKey(), entry);

        log.debug("LoginContext key: {}", loginContext.getContextKey());

        String cookieDomain = getCookieDomain(context);

        Cookie contextKeyCookie = new Cookie(LOGIN_CTX_KEY_NAME, loginContext.getContextKey());

        /* BLITZ patch (deleted) : As not all browser support cookies version 1.
        contextKeyCookie.setVersion(1);
        */

        if (cookieDomain != null) {
            contextKeyCookie.setDomain(cookieDomain);
        }
        contextKeyCookie.setPath("".equals(httpRequest.getContextPath()) ? "/" : httpRequest.getContextPath());

        /* BLITZ patch (added) : Security reason */
        contextKeyCookie.setSecure(true);
        contextKeyCookie.setHttpOnly(true); //httpRequest.isSecure()
        //

        httpResponse.addCookie(contextKeyCookie);
        httpRequest.setAttribute(LOGIN_CTX_KEY_NAME, loginContext);
    }

    /**
     * Gets the domain to use for all cookies.
     * 
     * @param context web application context
     * 
     * @return domain to use for all cookies
     */
    public static String getCookieDomain(ServletContext context) {
        return context.getInitParameter(COOKIE_DOMAIN_PARAM);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static AttributeFilteringEngine<?> getAttributeFilterEnginer(ServletContext context) {
        return getAttributeFilterEnginer(context,
                getContextParam(context, ATTRIBUTE_FILTER_ENGINE_SID_CTX_PARAM, DEFAULT_ATTRIBUTE_FILTER_ENGINE_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.filtering.AttributeFilteringEngine} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static AttributeFilteringEngine<?> getAttributeFilterEnginer(ServletContext context, String serviceId) {
        return (AttributeFilteringEngine<?>) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static AttributeResolver<?> getAttributeResolver(ServletContext context) {
        return getAttributeResolver(context,
                getContextParam(context, ATTRIBUTE_RESOLVER_SID_CTX_PARAM, DEFAULT_ATTRIBUTE_RESOLVER_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static AttributeResolver<?> getAttributeResolver(ServletContext context, String serviceId) {
        return (AttributeResolver<?>) context.getAttribute(serviceId);
    }

    /**
     * Gets a value for a given context parameter. If no value is present the default value is used.
     * 
     * @param context the Servlet context
     * @param name name of the context parameter
     * @param defaultValue default value of the parameter
     * 
     * @return the value of the context parameter or the default value if the parameter is not set or does not contain a
     *         value
     */
    public static String getContextParam(ServletContext context, String name, String defaultValue) {
        String value = DatatypeHelper.safeTrimOrNullString(context.getInitParameter(name));
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets the first {@link Cookie} whose name matches the given name.
     * 
     * @param cookieName the cookie name
     * @param httpRequest HTTP request from which the cookie should be extracted
     * 
     * @return the cookie or null if no cookie with that name was given
     */
    public static Cookie getCookie(HttpServletRequest httpRequest, String cookieName) {
        Cookie[] requestCookies = httpRequest.getCookies();
        if (requestCookies != null) {
            for (Cookie requestCookie : requestCookies) {
                if (requestCookie != null && DatatypeHelper.safeEquals(requestCookie.getName(), cookieName)) {
                    return requestCookie;
                }
            }
        }

        return null;
    }

    /**
     * Gets the login context from the current request. The login context is only in this location while the request is
     * being transferred from the authentication engine back to the profile handler.
     * 
     * This method only works during the first hand-off from the authentication engine to the login handler. Afterwords
     * you must use {@link #getLoginContext(StorageService, ServletContext, HttpServletRequest)}.
     * 
     * @param httpRequest current HTTP request
     * 
     * @return the login context or null if no login context is bound to the request
     * 
     * @deprecated use {@link #getLoginContext(StorageService, ServletContext, HttpServletRequest)} instead
     */
    public static LoginContext getLoginContext(HttpServletRequest httpRequest) {
        ServletContext servletContext = httpRequest.getSession().getServletContext();
        StorageService storageService = HttpServletHelper.getStorageService(servletContext);
        return HttpServletHelper.getLoginContext(storageService, servletContext, httpRequest);        
    }

    /**
     * Gets the {@link LoginContext} for the user issuing the HTTP request. Note, login contexts are only available
     * during the authentication process.
     * 
     * @param context the Servlet context
     * @param storageService storage service to use when retrieving the login context
     * @param httpRequest current HTTP request
     * 
     * @return the login context or null if none is available
     */
    @SuppressWarnings("unchecked")
    public static LoginContext getLoginContext(StorageService storageService, ServletContext context,
            HttpServletRequest httpRequest) {
        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Servlet context may not be null");
        }
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }

        /* BLITZ patch (added) : External storage service support */
        // return more fresh login context from request context as storage entry may be old.
        LoginContext loginContext = (LoginContext) httpRequest.getAttribute(HttpServletHelper.LOGIN_CTX_KEY_NAME);
        if (loginContext != null){
            return loginContext;
        }
        //

        Cookie loginContextKeyCookie = getCookie(httpRequest, LOGIN_CTX_KEY_NAME);
        if (loginContextKeyCookie == null) {
            log.debug("LoginContext key cookie was not present in request");
            return null;
        }

        String loginContextKey = DatatypeHelper.safeTrimOrNullString(loginContextKeyCookie.getValue());
        if (loginContextKey == null) {
            log.warn("Corrupted LoginContext Key cookie, it did not contain a value");
        }

        String partition = getContextParam(context, LOGIN_CTX_PARTITION_CTX_PARAM, DEFAULT_LOGIN_CTX_PARITION);
        log.trace("Looking up LoginContext with key {} from StorageService parition: {}", loginContextKey, partition);
        LoginContextEntry entry = (LoginContextEntry) storageService.get(partition, loginContextKey);
        if (entry != null) {
            if (entry.isExpired()) {
                log.debug("LoginContext found but it was expired");
            } else {
                log.trace("Retrieved LoginContext with key {} from StorageService parition: {}", loginContextKey,
                        partition);

                /* BLITZ patch (added) : External storage service support */
                // save login context to request context
                httpRequest.setAttribute(HttpServletHelper.LOGIN_CTX_KEY_NAME, entry.getLoginContext());
                //

                return entry.getLoginContext();
            }
        } else {
            log.debug("No login context in storage service");
        }

        return null;
    }

    /**
     * Gets the {@link IdPProfileHandlerManager} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static IdPProfileHandlerManager getProfileHandlerManager(ServletContext context) {
        return getProfileHandlerManager(context,
                getContextParam(context, PROFILE_HANDLER_MNGR_SID_CTX_PARAM, DEFAULT_PROFILE_HANDLER_MNGR_SID));
    }

    /**
     * Gets the {@link IdPProfileHandlerManager} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static IdPProfileHandlerManager getProfileHandlerManager(ServletContext context, String serviceId) {
        return (IdPProfileHandlerManager) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static RelyingPartyConfigurationManager getRelyingPartyConfigurationManager(ServletContext context) {
        return getRelyingPartyConfigurationManager(context,
                getContextParam(context, RP_CONFIG_MNGR_SID_CTX_PARAM, DEFAULT_RP_CONFIG_MNGR_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static RelyingPartyConfigurationManager getRelyingPartyConfigurationManager(ServletContext context,
            String serviceId) {
        return (RelyingPartyConfigurationManager) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     * 
     * @deprecated use {@link #getRelyingPartyConfigurationManager(ServletContext)}
     */
    public static RelyingPartyConfigurationManager getRelyingPartyConfirmationManager(ServletContext context) {
        return getRelyingPartyConfirmationManager(context,
                getContextParam(context, RP_CONFIG_MNGR_SID_CTX_PARAM, DEFAULT_RP_CONFIG_MNGR_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     * 
     * @deprecated use {@link #getRelyingPartyConfigurationManager(ServletContext, String)}

     */
    public static RelyingPartyConfigurationManager getRelyingPartyConfirmationManager(ServletContext context,
            String serviceId) {
        return (RelyingPartyConfigurationManager) context.getAttribute(serviceId);
    }

    /**
     * Gets the metatdata for a given relying party.
     * 
     * @param relyingPartyEntityId the ID of the relying party
     * @param rpConfigMngr relying party configuration manager
     * 
     * @return the metadata for the relying party or null if no SAML metadata exists for the given relying party
     */
    public static EntityDescriptor getRelyingPartyMetadata(String relyingPartyEntityId,
            RelyingPartyConfigurationManager rpConfigMngr) {
        if (rpConfigMngr instanceof SAMLMDRelyingPartyConfigurationManager) {
            SAMLMDRelyingPartyConfigurationManager samlRpConfigMngr = (SAMLMDRelyingPartyConfigurationManager) rpConfigMngr;
            try {
                return samlRpConfigMngr.getMetadataProvider().getEntityDescriptor(relyingPartyEntityId);
            } catch (MetadataProviderException e) {

            }
        }

        return null;
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static SAML1AttributeAuthority getSAML1AttributeAuthority(ServletContext context) {
        return getSAML1AttributeAuthority(context,
                getContextParam(context, SAML1_AA_SID_CTX_PARAM, DEFAULT_SAML1_AA_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static SAML1AttributeAuthority getSAML1AttributeAuthority(ServletContext context, String serviceId) {
        return (SAML1AttributeAuthority) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static SAML2AttributeAuthority getSAML2AttributeAuthority(ServletContext context) {
        return getSAML2AttributeAuthority(context,
                getContextParam(context, SAML2_AA_SID_CTX_PARAM, DEFAULT_SAML2_AA_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static SAML2AttributeAuthority getSAML2AttributeAuthority(ServletContext context, String serviceId) {
        return (SAML2AttributeAuthority) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.session.SessionManager} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static SessionManager<Session> getSessionManager(ServletContext context) {
        return getSessionManager(context,
                getContextParam(context, SESSION_MNGR_SID_CTX_PARAM, DEFAULT_SESSION_MNGR_SID));
    }

    /**
     * Gets the {@link edu.internet2.middleware.shibboleth.common.session.SessionManager} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    @SuppressWarnings("unchecked")
    public static SessionManager<Session> getSessionManager(ServletContext context, String serviceId) {
        return (SessionManager<Session>) context.getAttribute(serviceId);
    }

    /**
     * Gets the {@link StorageService} service bound to the Servlet context.
     * 
     * @param context the Servlet context
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static StorageService<?, ?> getStorageService(ServletContext context) {
        return getStorageService(context,
                getContextParam(context, STORAGE_SERVICE_SID_CTX_PARAM, DEFAULT_STORAGE_SERVICE_SID));
    }

    /**
     * Gets the {@link StorageService} bound to the Servlet context.
     * 
     * @param context the Servlet context
     * @param serviceId the ID under which the service bound
     * 
     * @return the service or null if there is no such service bound to the context
     */
    public static StorageService<?, ?> getStorageService(ServletContext context, String serviceId) {
        return (StorageService<?, ?>) context.getAttribute(serviceId);
    }

    /**
     * Gets the user session from the request. Retrieving the session in this manner does NOT update the last activity
     * time of the session.
     * 
     * @param httpRequest current request
     * 
     * @return the users session, if one exists
     */
    public static Session getUserSession(HttpServletRequest httpRequest) {
        return (Session) httpRequest.getAttribute(Session.HTTP_SESSION_BINDING_ATTRIBUTE);
    }

    /**
     * Unbinds a {@link LoginContext} from the current request. The unbinding results in the destruction of the
     * associated context key cookie and removes the context from the storage service.
     * 
     * @param storageService storage service holding the context
     * @param context the Servlet context
     * @param httpRequest current HTTP request
     * @param httpResponse current HTTP response
     * 
     * @return the login context that was unbound or null if there was no bound context
     */
    @SuppressWarnings("unchecked")
    public static LoginContext unbindLoginContext(StorageService storageService, ServletContext context,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        log.debug("Unbinding LoginContext");
        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Servlet context may not be null");
        }
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (httpResponse == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }

        Cookie loginContextKeyCookie = getCookie(httpRequest, LOGIN_CTX_KEY_NAME);
        if (loginContextKeyCookie == null) {
            log.debug("No LoginContext cookie available, no unbinding necessary.");
            return null;
        }

        String loginContextKey = DatatypeHelper.safeTrimOrNullString(loginContextKeyCookie.getValue());
        if (loginContextKey == null) {
            log.warn("Corrupted LoginContext Key cookie, it did not contain a value");
            return null;
        }

        log.debug("Expiring LoginContext cookie");
        loginContextKeyCookie.setMaxAge(0);
        loginContextKeyCookie.setPath("".equals(httpRequest.getContextPath()) ? "/" : httpRequest.getContextPath());
        /* BLITZ patch (deleted) : As not all browser support cookies version 1.
        loginContextKeyCookie.setVersion(1);
        */

        /* BLITZ patch (added) : Security reason */
        loginContextKeyCookie.setSecure(true);
        loginContextKeyCookie.setHttpOnly(true);
        //

        httpResponse.addCookie(loginContextKeyCookie);

        String storageServicePartition = getContextParam(context, LOGIN_CTX_PARTITION_CTX_PARAM,
                DEFAULT_LOGIN_CTX_PARITION);

        LoginContextEntry entry = (LoginContextEntry) storageService.remove(storageServicePartition, loginContextKey);
        if (entry != null && !entry.isExpired()) {
            log.debug("Removed LoginContext, with key {}, from StorageService partition {}", loginContextKey,
                    storageServicePartition);
            return entry.getLoginContext();
        }

        return null;
    }

    /**
     * Builds a URL, up to and including the servlet context path. URL does not include a trailing "/".
     * 
     * @param httpRequest httpRequest made to the servlet in question
     * 
     * @return URL builder containing the scheme, server name, server port, and context path
     */
    public static URLBuilder getServletContextUrl(HttpServletRequest httpRequest) {
        URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setScheme(httpRequest.getScheme());
        urlBuilder.setHost(httpRequest.getServerName());
        urlBuilder.setPort(httpRequest.getServerPort());
        urlBuilder.setPath(httpRequest.getContextPath());
        return urlBuilder;
    }

    /**
     * Builds a URL to a path that is meant to be relative to the Servlet context.
     * 
     * @param httpRequest current HTTP request
     * @param path path relative to the context, may start with a "/"
     * 
     * @return URL builder containing the scheme, server name, server port, and full path
     */
    public static URLBuilder getContextRelativeUrl(HttpServletRequest httpRequest, String path) {
        URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setScheme(httpRequest.getScheme());
        urlBuilder.setHost(httpRequest.getServerName());

        // Patch (add) : Load balancing support
        /*todo: cut off*/
        boolean  isLoadBalancingEnable = false;
        if (!isLoadBalancingEnable) {
         urlBuilder.setPort(httpRequest.getServerPort());// urlBuilder.setPort(httpRequest.getServerPort());
        }
        //

        StringBuilder pathBuilder = new StringBuilder();
        if (!"".equals(httpRequest.getContextPath())) {
            pathBuilder.append(httpRequest.getContextPath());
        }
        if (!path.startsWith("/")) {
            pathBuilder.append("/");
        }
        pathBuilder.append(DatatypeHelper.safeTrim(path));
        urlBuilder.setPath(pathBuilder.toString());

        return urlBuilder;
    }

}