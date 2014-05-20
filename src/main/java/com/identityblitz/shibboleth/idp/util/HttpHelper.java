package com.identityblitz.shibboleth.idp.util;

import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPInTransportWithCookie;
import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPOutTransportWithCookie;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContextEntry;
import org.opensaml.util.URLBuilder;
import org.opensaml.util.storage.StorageService;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.UUID;

/**
 */
public class HttpHelper {

    /** Name of the key to the current authentication login context: {@value} . */
    public static final String LOGIN_CTX_KEY_NAME = "_idp_authn_lc_key";

    /** Default name for the {@link StorageService} partition which holds {@link LoginContext}s: {@value} . */
    public static final String DEFAULT_LOGIN_CTX_PARITION = "loginContexts";

    /** Class logger. */
    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

    @SuppressWarnings("unchecked")
    public static LoginContext getLoginContext(final StorageService storageService,
                                               final HTTPInTransportWithCookie inTr,
                                               final HTTPOutTransportWithCookie outTr) {
        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (inTr == null) {
            throw new IllegalArgumentException("HTTP in transport may not be null");
        }
        if (outTr == null) {
            throw new IllegalArgumentException("HTTP out transport may not be null");
        }


        LoginContext loginContext = (LoginContext) outTr.getAttribute(LOGIN_CTX_KEY_NAME);
        if (loginContext != null){
            return loginContext;
        }

        final String loginContextKeyCookie = inTr.getCookie(LOGIN_CTX_KEY_NAME);
        if (loginContextKeyCookie == null) {
            log.debug("LoginContext key cookie was not present in request");
            return null;
        }

        final String loginContextKey = DatatypeHelper.safeTrimOrNullString(loginContextKeyCookie);
        if (loginContextKey == null) {
            log.warn("Corrupted LoginContext Key cookie, it did not contain a value");
        }

        final String partition = getPartition();
        log.trace("Looking up LoginContext with key {} from StorageService parition: {}", loginContextKey, DEFAULT_LOGIN_CTX_PARITION);
        LoginContextEntry entry = (LoginContextEntry) storageService.get(partition, loginContextKey);
        if (entry != null) {
            if (entry.isExpired()) {
                log.debug("LoginContext found but it was expired");
            } else {
                log.trace("Retrieved LoginContext with key {} from StorageService parition: {}", loginContextKey,
                        partition);
                outTr.setAttribute(LOGIN_CTX_KEY_NAME, entry.getLoginContext());

                return entry.getLoginContext();
            }
        } else {
            log.debug("No login context in storage service");
        }

        return null;

    }

    @SuppressWarnings("unchecked")
    public static void bindLoginContext(final LoginContext loginContext, final StorageService storageService,
                                        final HTTPInTransportWithCookie inTr, final HTTPOutTransportWithCookie outTr) {
        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (inTr == null) {
            throw new IllegalArgumentException("HTTP in transport may not be null");
        }
        if (outTr == null) {
            throw new IllegalArgumentException("HTTP out transport may not be null");
        }

        String partition = getPartition();
        log.debug("LoginContext partition: {}", partition);
        String contextKey = UUID.randomUUID().toString();
        while (storageService.contains(partition, contextKey)) {
            contextKey = UUID.randomUUID().toString();
        }
        LoginContextEntry entry = new LoginContextEntry(loginContext, 1800000);
        log.debug("Storing LoginContext to StorageService partition {}, key {}", partition, loginContext.getContextKey());
        storageService.put(partition, loginContext.getContextKey(), entry);

        log.debug("LoginContext key: {}", loginContext.getContextKey());
        outTr.addCookie(LOGIN_CTX_KEY_NAME, loginContext.getContextKey());
        outTr.setAttribute(LOGIN_CTX_KEY_NAME, loginContext);
    }

    @SuppressWarnings("unchecked")
    public static LoginContext unbindLoginContext(final StorageService storageService,
                                                  final HTTPInTransportWithCookie inTr,
                                                  final HTTPOutTransportWithCookie outTr) {
        log.debug("Unbinding LoginContext");

        if (storageService == null) {
            throw new IllegalArgumentException("Storage service may not be null");
        }
        if (inTr == null) {
            throw new IllegalArgumentException("HTTP in transport may not be null");
        }
        if (outTr == null) {
            throw new IllegalArgumentException("HTTP out transport may not be null");
        }


        final String loginContextKeyCookie = inTr.getCookie(LOGIN_CTX_KEY_NAME);
        if (loginContextKeyCookie == null) {
            log.debug("LoginContext key cookie was not present in request");
            return null;
        }

        final String loginContextKey = DatatypeHelper.safeTrimOrNullString(loginContextKeyCookie);
        if (loginContextKey == null) {
            log.warn("Corrupted LoginContext Key cookie, it did not contain a value");
            return null;
        }

        log.debug("Expiring LoginContext cookie");
        outTr.discardCookie(LOGIN_CTX_KEY_NAME);

        final String partition = getPartition();

        LoginContextEntry entry = (LoginContextEntry) storageService.remove(partition, loginContextKey);
        if (entry != null && !entry.isExpired()) {
            log.debug("Removed LoginContext, with key {}, from StorageService partition {}", loginContextKey, partition);
            return entry.getLoginContext();
        }

        return null;

    }

    public static String getRequestUriWithoutContext(final HTTPInTransportWithCookie inTr) {
        final URL curUrl = (URL)inTr.getAttribute("URL");
        final String contextPath = (String)inTr.getAttribute("CONTEXT_PATH");
        if (curUrl == null) {
            log.error("URL attribute not specified in the inbound transport attributes");
            throw new IllegalArgumentException("URL attribute not specified in the inbound transport attributes");
        }

        String path = curUrl.getPath();
        if (contextPath != null && path.startsWith(contextPath)) {
            path = path.replaceFirst(contextPath, "");
        }

        return path;
    }

    public static URLBuilder getContextRelativeUrl(final HTTPInTransportWithCookie inTr, String path) {
        final URL curUrl = (URL)inTr.getAttribute("URL");
        final String contextPath = (String)inTr.getAttribute("CONTEXT_PATH");
        if (curUrl == null) {
            log.error("URL attribute not specified in the inbound transport attributes");
            throw new IllegalArgumentException("URL attribute not specified in the inbound transport attributes");
        }

        final URLBuilder urlBuilder = new URLBuilder();
        urlBuilder.setScheme(curUrl.getProtocol());
        urlBuilder.setHost(curUrl.getHost());

        final StringBuilder pathBuilder = new StringBuilder();
        if (contextPath != null && !contextPath.isEmpty()) {
            pathBuilder.append(contextPath);
        }
        if (!path.startsWith("/")) {
            pathBuilder.append("/");
        }
        pathBuilder.append(DatatypeHelper.safeTrim(path));
        urlBuilder.setPath(pathBuilder.toString());

        return urlBuilder;
    }

    private static String getPartition() {
        return DEFAULT_LOGIN_CTX_PARITION;
    }

}
