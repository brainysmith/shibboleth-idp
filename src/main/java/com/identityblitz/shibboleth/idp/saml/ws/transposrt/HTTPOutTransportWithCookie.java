package com.identityblitz.shibboleth.idp.saml.ws.transposrt;

import org.opensaml.ws.transport.http.HTTPOutTransport;

/**
 */
public interface HTTPOutTransportWithCookie extends HTTPOutTransport {

    /**
     * Adds the given cookie with the given value.
     *
     * @param name cookie name
     * @param value cookie value
     */
    public void addCookie(String name, String value);


    /**
     * Discards cookie with the specified name.
     *
     * @param name cookie name
     */
    public void discardCookie(String name);
}
