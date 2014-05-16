package com.identityblitz.shibboleth.idp.dc;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;
import edu.internet2.middleware.shibboleth.common.session.Session;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.util.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.identityblitz.shibboleth.idp.authn.principal.IdpPrincipalHelper;
import com.identityblitz.shibboleth.idp.md.AtrMetadataHelper;
import com.identityblitz.shibboleth.idp.md.AtrResolutionCtxHelper;
import com.identityblitz.shibboleth.idp.authn.principal.IdpPrincipal;
import com.identityblitz.shibboleth.idp.storage.AttributesEntry;
import com.identityblitz.shibboleth.idp.storage.CacheEntryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: agumerov
 * Date: 12.12.12
 */
public abstract class AbstractCachedDataConnector extends BaseDataConnector {
    private static Logger log = LoggerFactory.getLogger(AbstractCachedDataConnector.class);

    /**
     * Attributes entry storage service.
     */
    StorageService<String, AttributesEntry> storageService;

    public void setStorageService(StorageService<String, AttributesEntry> storage) {
        storageService = storage;
    }

    /**
     * Get connector resolved group of attributes.
     *
     * @param requiredAttributes {@link java.util.Set<String>  } the peer service provider required attributes
     * @param showOrgData {@link boolean } check if the peer service provider required org data attributes
     * @return resolved group set {@link java.util.Set< AttributeGroup > }
     */
    protected abstract Set<AttributeGroup> getAttributeGroups(Set<String> requiredAttributes, boolean showOrgData);

/*    *//**
     * Get session cache entry key for specific attribute group.
     *
     * @param sessionId    {@link String} current session Id
     * @param principal    {@link IdpInternalPrincipal} actual principal
     * @param peerEntityId {@link String} peer service provider entity id*
     * @param grp          {@link AttributeGroup} group of attributes
     * @return cache entry key {@link String }
     *//*
    protected abstract String getCacheKey(String sessionId, IdpInternalPrincipal principal, String peerEntityId, AttributeGroupEnum grp);*/

    /**
     * Check whether group attributes is need to cache.
     *
     * @param grp {@link AttributeGroup} attribute group
     * @return true-for cached group, false - for ithers
     */
    protected abstract boolean isCached(AttributeGroup grp);

    /**
     * Populate attributes of specific attribute group.
     *
     * @param grp          {@link AttributeGroup} populated attribute group
     * @param principal    {@link IdpPrincipal} actual principal
     * @param sessionId    {@link String}  principal session id
     * @param peerEntityId    {@link String}  peer service provider entity id
     *
     * @return populated attribute map {@link java.util.Map<String, Object> }
     * @throws edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException thrown if there is a problem in generating the token
     */
    protected abstract Map<String, Object> populateAttributes(AttributeGroup grp, IdpPrincipal principal, String sessionId, String peerEntityId)
            throws AttributeResolutionException;


    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws AttributeResolutionException {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        Session userSession = AtrResolutionCtxHelper.getUserSession(resolutionCtx);
        String sessionId = userSession.getSessionID();
        IdpPrincipal principal = IdpPrincipalHelper.getPrincipal(userSession.getSubject());
        if (principal == null) {
            log.error("Idp principal not found during attribute resolution for session {}",
                    new Object[]{userSession.getPrincipalName(), sessionId});
            throw new AttributeResolutionException("Idp principal not found during attribute resolution");
        }

        String peerEntityId = AtrResolutionCtxHelper.getPeerEntityId(resolutionCtx);
        EntityDescriptor entityDescriptor =  AtrResolutionCtxHelper.getPeerEntityMetadata(resolutionCtx);
        boolean showOrgData = principal.showOrgData(entityDescriptor);

        log.debug("Resolving attributes : peer entity id {}, session {}, principal {}", new Object[]{peerEntityId, sessionId, principal.getName()});
        Map<String, BaseAttribute> allAttributes = new HashMap<String, BaseAttribute>();


        for (AttributeGroup grp : getAttributeGroups(AtrMetadataHelper.getRequiredAttributeNames(entityDescriptor), showOrgData)) {
            Map<String, Object> grpAttributes = null;

            String key = genSessionCacheKey(sessionId, principal, grp);
            log.debug("Attribute group cache key {} ", key);
            if (isCached(grp)) {
                log.debug("Attempt to get attributes from the cache");
                grpAttributes = CacheEntryManager.retrieveAttributes(storageService, key);
            } else {
                log.debug("Attribute group {} is not cached", grp.getSysname());
            }
            if (grpAttributes == null) {
                grpAttributes = populateAttributes(grp, principal, sessionId, peerEntityId);
                log.debug("Populate {} attributes of group {} with key {} ",
                        new Object[]{grpAttributes.size(), grp.getSysname(), key});
                if (isCached(grp)) {
                    CacheEntryManager.cacheAttributes(storageService, key, grpAttributes);
                    log.debug("{} attributes of group {} with key {} put into cache",
                            new Object[]{grpAttributes.size(), grp.getSysname(), key});
                }
            } else {
                log.debug("Retrieved from cache {} attributes of group {} with key ",
                        new Object[]{grpAttributes.size(), grp.getSysname(), key});
            }
            allAttributes.putAll(buildAttributeMap(grpAttributes));
        }
        return allAttributes;
    }

    /**
     * Build single-value attribute {@link edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute}.
     *
     * @param attrName  {@link String} attribute name
     * @param attrValue {@link Object} attribute value
     * @return built base attribute {@link edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute<Object>}
     */
    private BaseAttribute<Object> buildAttribute(String attrName, Object attrValue) {
        BaseAttribute<Object> attribute = new BasicAttribute<Object>(attrName);
        attribute.getValues().add(attrValue);
        return attribute;
    }

    /**
     * Build single-value attribute map {@link java.util.Map<String,  edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute>}.
     *
     * @param name2ValueMap {@link java.util.Map<String, Object>} attribute name  to value map
     * @return built base attribute map {@link edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute<Object>}
     */
    private Map<String, BaseAttribute> buildAttributeMap(Map<String, Object> name2ValueMap) {
        Map<String, BaseAttribute> attributeMap = new HashMap<String, BaseAttribute>(name2ValueMap.size());
        for (Map.Entry<String, Object> entry : name2ValueMap.entrySet()) {
            attributeMap.put(entry.getKey(), buildAttribute(entry.getKey(), entry.getValue()));
        }
        return attributeMap;
    }

    /**
     * Generate key for session level cache entry.
     *
     * @param sessionId {@link String} current session id
     * @param principal {@link IdpPrincipal} current session principal
     * @param group   {@link AttributeGroup} attribute group
     * @return {@link String} attribute group key.
     */
    private String genSessionCacheKey(String sessionId, IdpPrincipal principal, AttributeGroup group) {
        return sessionId + "|" + principal.getOid() + "|" + group.getSysname();
    }

}
