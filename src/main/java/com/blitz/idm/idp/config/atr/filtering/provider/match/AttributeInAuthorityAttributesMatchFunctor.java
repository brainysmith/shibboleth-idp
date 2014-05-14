package com.blitz.idm.idp.config.atr.filtering.provider.match;

/**
 * User: vkarpov
 * Date: 06.10.11
 */

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.MatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.blitz.idm.idp.md.AtrMetadataHelper;

public class AttributeInAuthorityAttributesMatchFunctor implements MatchFunctor {

    private final Logger log = LoggerFactory.getLogger(AttributeInAuthorityAttributesMatchFunctor.class);

    /** {@inheritDoc} */
    @Override
    public boolean evaluatePolicyRequirement(ShibbolethFilteringContext shibbolethFilteringContext) throws FilterProcessingException {
        throw new FilterProcessingException("This function is not implemented for Policy Requirement");
    }

    /** {@inheritDoc} */
    @Override
    public boolean evaluatePermitValue(ShibbolethFilteringContext shibbolethFilteringContext, String attributeId,
                                       Object attributeValue) throws FilterProcessingException {
        return doEvaluateAttribute(shibbolethFilteringContext, attributeId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean evaluateDenyRule(ShibbolethFilteringContext shibbolethFilteringContext, String attributeId,
                                    Object attributeValue) throws FilterProcessingException {
        throw new FilterProcessingException("This function is not implemented for Deny Rule");
    }

    private boolean doEvaluateAttribute(ShibbolethFilteringContext shibbolethFilteringContext, String attributeId)
    {
        log.debug("Evaluating the attribute {}", attributeId);

        EntityDescriptor entityDescriptor = shibbolethFilteringContext.getAttributeRequestContext().getPeerEntityMetadata();
        if(entityDescriptor == null){
            log.info("PeerEntityMetadata is undefined");
            return false;
        }
        log.debug("Found the entity descriptor {}", entityDescriptor.getEntityID());

        if (AtrMetadataHelper.containsRequiredAttribute(entityDescriptor, attributeId)) {
            return true;
        } else {
            log.debug("Attribute with ID {} not found", attributeId);
            return false;
        }
    }
}
