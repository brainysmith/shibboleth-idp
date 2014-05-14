package com.blitz.idm.idp.storage;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContextEntry;
import edu.internet2.middleware.shibboleth.idp.session.Session;
import edu.internet2.middleware.shibboleth.idp.session.impl.SessionManagerEntry;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContext;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContextEntry;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContextStorageHelper;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;
import org.opensaml.util.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.blitz.idm.idp.util.DateTimeUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class CacheEntryManager {

    /**
     * Class Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(CacheEntryManager.class);

    public static final String LOGIN_CTX_CACHE_NAME = "loginContexts";
    public static final String LOGOUT_CTX_CACHE_NAME = "sloContexts";
    public static final String SESSION_CTX_CACHE_NAME = "session";
    public static final String REPLAY_CTX_CACHE_NAME = "replay";
    public static final String TRANSIENT_ID_CACHE_NAME = "transientId";
    public static final String ATTRIBUTES_CACHE_NAME = "attributes";

    /*todo: change to config*/
    private static final long attributeContextCacheLifetime = 1800 * 1000;
    private static final long loginContextCacheLifetime = 1800 * 1000;
    private static final long logoutContextCacheLifetime = 1800 * 1000;
    private static final long sessionCacheLifetime = 1800 * 1000;


    /**
     * Put attributes map into cache.
     *
     * @param storageService {@link StorageService<String,    AttributesEntry  >} attribute name
     * @param key            {@link String} cache entry key
     * @param attributes     {@link java.util.Map<String, Object>} cached attributes map
     */
    public static void cacheAttributes(StorageService<String, AttributesEntry> storageService, String key, Map<String, Object> attributes) {
        if (attributes != null && !attributes.isEmpty()) {
            AttributesEntry entry = new AttributesEntry(attributeContextCacheLifetime, attributes);
            storageService.put(ATTRIBUTES_CACHE_NAME, key, entry);
            log.debug("{} attributes entry with key {} have been put into the attribute cache", attributes.size(), key);
        }
    }

    /**
     * Retrieve attributes map from cache.
     *
     * @param storageService {@link StorageService<String, AttributesEntry>} storage service object
     * @param key            {@link String} cache entry key
     * @return {@link java.util.Map<String, Object>} cached attributes map
     */
    public static Map<String, Object> retrieveAttributes(StorageService<String, AttributesEntry> storageService, String key) {
        AttributesEntry entry = storageService.get(ATTRIBUTES_CACHE_NAME, key);
        if (entry != null && !entry.isExpired()) {
            return entry.getAttributes();
        }
        return null;
    }


    /**
     * Cache session, SSO and SLO context saved saved in request if changed and not expired.
     *
     * @param request {@link HttpServletRequest} http servlet request
     * @param storageService {@link StorageService<?, ?>} storage service object
     */
    public static void cacheContext(HttpServletRequest request, StorageService<?, ?> storageService) {
        LoginContext loginCtx = (LoginContext) request.getAttribute(HttpServletHelper.LOGIN_CTX_KEY_NAME);
        cacheSSOContext(storageService, loginCtx);
        SingleLogoutContext logoutCtx = SingleLogoutContextStorageHelper.getSingleLogoutContext(request);
        cacheSLOContext(storageService, logoutCtx);
        Session sessionCtx = HttpServletHelper.getUserSession(request);
        cacheSessionContext(storageService, sessionCtx);
    }

    /**
     * Cache session, SSO and SLO context saved saved in request if changed and not expired.
     *
     * @param request {@link HttpServletRequest} http servlet request
     */
    public static void cacheContext(HttpServletRequest request) {
        StorageService storageService = (StorageService) request.getAttribute(StorageServiceFilter.STORAGE_SERVICE_ATTR_NAME);
        cacheContext(request, storageService);
    }

    /**
     * Cache SSO context if changed and not expired.
     *
     * @param storageService {@link StorageService<String, LoginContextEntry>} storage service
     * @param loginContext   {@link LoginContext} cached login context
     */
    @SuppressWarnings("unchecked")
    private static void cacheSSOContext(StorageService storageService, LoginContext loginContext) {
        StorageService<String, LoginContextEntry> storageSrv = (StorageService<String, LoginContextEntry>) storageService;
        if (loginContext != null && loginContext.isChanged()) {

            LoginContextEntry entry;
            if (loginContext.getReplicationCount() == 0) {
                entry = new LoginContextEntry(loginContext, loginContextCacheLifetime);
                storageSrv.put(LOGIN_CTX_CACHE_NAME, loginContext.getContextKey(), entry);
            } else {
                entry = storageSrv.get(LOGIN_CTX_CACHE_NAME, loginContext.getContextKey());
                if (entry != null && !entry.isExpired()) {
                    long lifeTime = DateTimeUtil.getLifeTimeInMillis(entry.getExpirationTime());
                    entry = new LoginContextEntry(loginContext, lifeTime);
                    storageSrv.put(LOGIN_CTX_CACHE_NAME, loginContext.getContextKey(), entry);
                    log.debug("LoginContext entry {} with lifetime = {} updated in cache {}", new Object[]{loginContext.getContextKey(), lifeTime, LOGIN_CTX_CACHE_NAME});
                }
            }
        }
    }

    /**
     * Cache SLO context if changed and not expired.
     *
     * @param storageService {@link StorageService<String, SingleLogoutContextEntry>} storage service object
     * @param logoutContext  {@link SingleLogoutContext} cached logout context
     */
    @SuppressWarnings("unchecked")
    private static void cacheSLOContext(StorageService storageService, SingleLogoutContext logoutContext) {
        StorageService<String, SingleLogoutContextEntry> storageSrv = (StorageService<String, SingleLogoutContextEntry>) storageService;
        if (logoutContext != null && logoutContext.isChanged()) {
            SingleLogoutContextEntry entry;
            if (logoutContext.getReplicationCount() == 0) {
                entry = new SingleLogoutContextEntry(logoutContext, logoutContextCacheLifetime);
                storageSrv.put(LOGOUT_CTX_CACHE_NAME, logoutContext.getContextKey(), entry);
                log.debug("LogoutContext entry {} with lifetime = {} created in cache {}", new Object[]{logoutContext.getContextKey(), logoutContextCacheLifetime, LOGOUT_CTX_CACHE_NAME});

            } else {
                entry = storageSrv.get(LOGOUT_CTX_CACHE_NAME, logoutContext.getContextKey());
                if (entry != null && !entry.isExpired()) {
                    long lifeTime = DateTimeUtil.getLifeTimeInMillis(entry.getExpirationTime());
                    entry = new SingleLogoutContextEntry(logoutContext, lifeTime);
                    storageSrv.put(LOGOUT_CTX_CACHE_NAME, logoutContext.getContextKey(), entry);
                    log.debug("LogoutContext entry {} with lifetime = {} updated in cache {}", new Object[]{logoutContext.getContextKey(), lifeTime, LOGOUT_CTX_CACHE_NAME});
                }
            }
        }
    }

    /**
     * Cache session context if changed and not expired.
     *
     * @param storageService {@link StorageService} storage service object
     * @param session        {@link Session} cached session context
     */
    @SuppressWarnings("unchecked")
    private static void cacheSessionContext(StorageService storageService, Session session) {
        StorageService<String, SessionManagerEntry> storageSrv = (StorageService<String, SessionManagerEntry>) storageService;
        if (session != null && (session.isChanged() || !session.getIndexes().isEmpty())) {
            SessionManagerEntry entry;
            Set<String> creatingIndexes = new HashSet<String>();
            if (session.getReplicationCount() == 0) {
                creatingIndexes.addAll(session.getIndexes());
                entry = new SessionManagerEntry(session, sessionCacheLifetime);
                entry.getSessionIndexes().addAll(creatingIndexes);
                creatingIndexes.add(session.getSessionID());
                // save indexed session entry in store
                for (String index : creatingIndexes) {
                    storageSrv.put(SESSION_CTX_CACHE_NAME, index, entry);
                    log.debug("Session entry {} with lifetime = {} created in cache {}", new Object[]{index, sessionCacheLifetime, SESSION_CTX_CACHE_NAME});
                }
            } else {
                entry = storageSrv.get(SESSION_CTX_CACHE_NAME, session.getSessionID());
                if (entry != null && !entry.isExpired()) {
                    // save old entry indexes
                    if (session.isChanged()) {
                        creatingIndexes.addAll(entry.getSessionIndexes());
                    }
                    creatingIndexes.addAll(session.getIndexes());
                    long lifeTime = DateTimeUtil.getLifeTimeInMillis(entry.getExpirationTime());
                    entry = new SessionManagerEntry(session, lifeTime);
                    entry.getSessionIndexes().addAll(creatingIndexes);
                    creatingIndexes.add(session.getSessionID());
                    // save indexed session entry in store
                    for (String index : creatingIndexes) {
                        storageSrv.put(SESSION_CTX_CACHE_NAME, index, entry);
                        log.debug("Session entry {} with lifetime = {} updated in cache {}", new Object[]{index, lifeTime, SESSION_CTX_CACHE_NAME});
                    }
                }
            }
        }
    }

}