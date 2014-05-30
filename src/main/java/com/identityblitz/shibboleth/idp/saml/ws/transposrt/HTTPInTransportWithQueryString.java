package com.identityblitz.shibboleth.idp.saml.ws.transposrt;

/**
 */
public interface HTTPInTransportWithQueryString extends HTTPInTransportWithCookie {

    /**
     * Returns the raw query string.
     * @return raw query string
     */
    public String getQueryString();

}
