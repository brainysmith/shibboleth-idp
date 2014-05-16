package com.identityblitz.shibboleth.idp.md;

/**
 * User: agumerov
 * Date: 11.12.12
 */

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.session.Session;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AtrResolutionCtxHelper {

    private static Logger log = LoggerFactory.getLogger(AtrResolutionCtxHelper.class);

    /**
     * Get current session {@link edu.internet2.middleware.shibboleth.common.session.Session} .
     *
     * @param resolutionCtx {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext} attribute resolution context
     *
     * @throws {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException} if there is a problem in getting the current user
     */
    public static Session getUserSession(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        Session userSession = resolutionCtx.getAttributeRequestContext().getUserSession();
        if (userSession == null) {
            log.error("Can not get user session for principal {}", getPrincipalName(resolutionCtx));
            throw new AttributeResolutionException("Can not get user session for principal "
                    + getPrincipalName(resolutionCtx));
        }
        return userSession;
    }

    /**
     * Get current session principal name {@link String} .
     *
     * @param resolutionCtx {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext} attribute resolution context
     *
     * @throws {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException} if there is a problem in getting the principal name
     */
    public static String getPrincipalName(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        String principalName = resolutionCtx.getAttributeRequestContext().getPrincipalName();
        if (principalName == null) {
            log.error("Can not get principal name from ShibbolethResolutionContext");
            throw new AttributeResolutionException("Can not get principal name from ShibbolethResolutionContext");
        }
        return principalName;
    }

    /**
     * Get peer service provider entity Id {@link String}.
     *
     * @param resolutionCtx {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext} attribute resolution context
     *
     * @throws {@link }AttributeResolutionException} if there is a problem in getting the peer sp entity Id
     */
    public static String getPeerEntityId(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        String peerEntityId = resolutionCtx.getAttributeRequestContext().getPeerEntityId();
        if (peerEntityId == null) {
            log.error("Can not get peer sp entity id for principal {}", getPrincipalName(resolutionCtx));
            throw new AttributeResolutionException("Can not get peer sp entity for principal "
                    + getPrincipalName(resolutionCtx));
        }
        return peerEntityId;
    }

    /**
     * Get current session Id {@link String}.
     *
     * @param resolutionCtx {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext} attribute resolution context
     *
     * @throws {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException} if there is a problem in getting the session Id
     */
    public static String getSessionId(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        String sessionId = resolutionCtx.getAttributeRequestContext().getUserSession().getSessionID();
        if (sessionId == null) {
            log.error("Can not get sessionId for principal {} ", getPrincipalName(resolutionCtx));
            throw new AttributeResolutionException("Can not get sessionId for principal "
                    + getPrincipalName(resolutionCtx));
        }
        return sessionId;
    }

    /**
     * Get peer service provider entity metadata.
     *
     * @param resolutionCtx {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext} attribute resolution context
     *
     * @return {@link org.opensaml.saml2.metadata.EntityDescriptor } the metadata
     *
     * @throws {@link edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException} if there is a problem in getting the metadata
     */
    public static EntityDescriptor getPeerEntityMetadata(ShibbolethResolutionContext resolutionCtx) throws AttributeResolutionException {
        return resolutionCtx.getAttributeRequestContext().getPeerEntityMetadata();
    }
}
