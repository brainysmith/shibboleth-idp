package com.identityblitz.shibboleth.idp.saml.ws.transposrt;

import org.opensaml.ws.transport.http.HTTPInTransport;

/**
 */
public interface HTTPInTransportWithCookie extends HTTPInTransport {

    /**
     * Returns the cookie value with the specified name.
     * @param name cookie name
     * @return cookie value
     */
    public String getCookie(String name);

}
