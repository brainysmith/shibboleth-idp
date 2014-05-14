package com.blitz.idm.idp.md;

import org.opensaml.common.xml.SAMLConstants;

import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AtrMetadataHelper {

    /**
     * Class logger.
     */
    private static final Logger log = LoggerFactory.getLogger(AtrMetadataHelper.class);

    public static boolean containsRequiredAttribute(EntityDescriptor entityDescriptor, String attributeId) {
        if (attributeId == null) {
            log.debug("Attribute id can't be null");
            throw new NullPointerException("Attribute id name can't be null");
        }
        log.debug("Check whether the entity descriptor contains the attribute with id {}", attributeId);

        Set<String> names = getRequiredAttributeNames(entityDescriptor);
        return (names != null && names.contains(attributeId));
    }

    public static Set<String> getRequiredAttributeNames(EntityDescriptor entityDescriptor) {
        if (entityDescriptor == null) {
            log.info("PeerEntityMetadata is undefined");
            return null;
        }
        log.debug("Found the entity descriptor {}", entityDescriptor.getEntityID());

        AttributeAuthorityDescriptor attributeAuthDescriptor = entityDescriptor.getAttributeAuthorityDescriptor(SAMLConstants.SAML20P_NS);
        if (attributeAuthDescriptor == null) {
            log.info("AttributeAuthorityDescriptor is undefined");
            return null;
        }
        log.debug("Found the attribute authority descriptor");

        List<Attribute> attributes = attributeAuthDescriptor.getAttributes();
        if (attributes == null) {
            log.info("A list of the attributes is undefined");
            return null;
        }
        Set<String> names = new HashSet<String>(attributes.size());
        for (Attribute attr : attributes) {
            names.add(attr.getName());
        }
        return names;
    }

}