package com.identityblitz.saml2.binding.decoding;

import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPInTransportWithCookie;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPRedirectDeflateDecoder;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.parse.ParserPool;

import java.net.URL;

/**
  */
public class HTTPRedirectDeflateDecoderExtended extends HTTPRedirectDeflateDecoder {

    public HTTPRedirectDeflateDecoderExtended() {
        super();
    }

    public HTTPRedirectDeflateDecoderExtended(ParserPool pool) {
        super(pool);
    }

    @Override
    protected String getActualReceiverEndpointURI(SAMLMessageContext messageContext) throws MessageDecodingException {
        final HTTPInTransportWithCookie inTr = (HTTPInTransportWithCookie) messageContext.getInboundMessageTransport();
        final URL curUrl = (URL)inTr.getAttribute("URL");
        if (curUrl == null) {
            throw new IllegalArgumentException("URL attribute not specified in the inbound transport attributes");
        }

        return curUrl.getProtocol() + "://" +curUrl.getAuthority() + curUrl.getPath();
    }
}
