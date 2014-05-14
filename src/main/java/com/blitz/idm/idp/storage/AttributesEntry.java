package com.blitz.idm.idp.storage;

import org.joda.time.DateTime;
import org.opensaml.util.storage.AbstractExpiringObject;

import java.util.Map;

/**
 * User: agumerov
 * Date: 12.12.12
 */

/**
 * Storage service entry used to store attributes resolved by idp.
 */
public class AttributesEntry extends AbstractExpiringObject {

    /** Attributes name to value map. */
    private Map<String, Object> attributes;

    /**
     * Constructor.
     *
     * @param lifetime          lifetime of the entry in milliseconds
     * @param attributes        attributes map
     */
    public AttributesEntry(long lifetime, Map<String, Object> attributes ) {
        super(new DateTime().plus(lifetime));
        this.attributes = attributes;
    }

    /**
     * Gets the attributes map.
     *
     * @return attributes map
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}

