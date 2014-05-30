package com.identityblitz.saml2.binding.security;

import com.identityblitz.opensaml.common.binding.security.BaseSAMLSimpleSignatureSecurityPolicyRuleExtended;
import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPInTransportWithQueryString;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPTransportUtils;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Security policy which evaluates simple "blob" signatures according to the SAML 2 HTTP-Redirect DEFLATE binding.
 */
public class SAML2HTTPRedirectDeflateSignatureRuleExtended extends BaseSAMLSimpleSignatureSecurityPolicyRuleExtended {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(SAML2HTTPRedirectDeflateSignatureRuleExtended.class);

    public SAML2HTTPRedirectDeflateSignatureRuleExtended(SignatureTrustEngine engine) {
        super(engine);
    }

    @Override
    protected boolean ruleHandles(HTTPInTransport itr, SAMLMessageContext samlMsgCtx) throws SecurityPolicyException {
        return "GET".equals(itr.getHTTPMethod());
    }

    @Override
    protected byte[] getSignedContent(HTTPInTransport httpInTransport) throws SecurityPolicyException {
        // We need the raw non-URL-decoded query string param values for HTTP-Redirect DEFLATE simple signature
        // validation.
        // We have to construct a string containing the signature input by accessing the
        // request directly. We can't use the decoded parameters because we need the raw
        // data and URL-encoding isn't canonical.

        if (!(httpInTransport instanceof HTTPInTransportWithQueryString)) {
            log.debug("Invalid inbound message transport type, this rule only supports HTTPInTransportWithQueryString");
            throw new SecurityPolicyException("Invalid inbound message transport type, this rule only supports HTTPInTransportWithQueryString");
        }
        HTTPInTransportWithQueryString itr = (HTTPInTransportWithQueryString)httpInTransport;
        String queryString = itr.getQueryString();
        log.debug("Constructing signed content string from URL query string {}", queryString);

        String constructed = buildSignedContentString(queryString);
        if (DatatypeHelper.isEmpty(constructed)) {
            log.warn("Could not extract signed content string from query string");
            return null;
        }
        log.debug("Constructed signed content string for HTTP-Redirect DEFLATE {}", constructed);

        try {
            return constructed.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // JVM is required to support UTF-8
        }
        return null;
    }

    /**
     * Extract the raw request parameters and build a string representation of the content that was signed.
     *
     * @param queryString the raw HTTP query string from the request
     * @return a string representation of the signed content
     * @throws SecurityPolicyException thrown if there is an error during request processing
     */
    private String buildSignedContentString(String queryString) throws SecurityPolicyException {
        StringBuilder builder = new StringBuilder();

        // One of these two is mandatory
        if (!appendParameter(builder, queryString, "SAMLRequest")) {
            if (!appendParameter(builder, queryString, "SAMLResponse")) {
                log.warn("Could not extract either a SAMLRequest or a SAMLResponse from the query string");
                throw new SecurityPolicyException("Extract of SAMLRequest or SAMLResponse from query string failed");
            }
        }
        // This is optional
        appendParameter(builder, queryString, "RelayState");
        // This is mandatory, but has already been checked in superclass
        appendParameter(builder, queryString, "SigAlg");

        return builder.toString();
    }

    /**
     * Find the raw query string parameter indicated and append it to the string builder.
     *
     * The appended value will be in the form 'paramName=paramValue' (minus the quotes).
     *
     * @param builder string builder to which to append the parameter
     * @param queryString the URL query string containing parameters
     * @param paramName the name of the parameter to append
     * @return true if parameter was found, false otherwise
     */
    private boolean appendParameter(StringBuilder builder, String queryString, String paramName) {
        String rawParam = HTTPTransportUtils.getRawQueryStringParameter(queryString, paramName);
        if (rawParam == null) {
            return false;
        }

        if (builder.length() > 0) {
            builder.append('&');
        }

        builder.append(rawParam);

        return true;
    }

}
