package com.identityblitz.saml2.binding.security;

import com.identityblitz.opensaml.common.binding.security.BaseSAMLSimpleSignatureSecurityPolicyRuleExtended;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.*;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCriteria;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.SignatureTrustEngine;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Security policy which evaluates simple "blob" signatures according to the SAML 2 HTTP-POST-SimpleSign binding.
 */
public class SAML2HTTPPostSimpleSignRuleExtended extends BaseSAMLSimpleSignatureSecurityPolicyRuleExtended {

    /** Logger. */
    private Logger log = LoggerFactory.getLogger(SAML2HTTPPostSimpleSignRuleExtended.class);

    /** Parser pool to use to process KeyInfo request parameter. */
    private ParserPool parser;

    /** KeyInfo resolver to use to process KeyInfo request parameter. */
    private KeyInfoCredentialResolver keyInfoResolver;

    /**
     * Constructor.
     *
     * @param engine the trust engine to use
     * @param parserPool the parser pool used to parse the KeyInfo request parameter
     * @param keyInfoCredResolver the KeyInfo credential resovler to use to extract credentials from the KeyInfo request
     *            parameter
     */
    public SAML2HTTPPostSimpleSignRuleExtended(SignatureTrustEngine engine, ParserPool parserPool,
                                       KeyInfoCredentialResolver keyInfoCredResolver) {
        super(engine);
        parser = parserPool;
        keyInfoResolver = keyInfoCredResolver;
    }

    @Override
    protected boolean ruleHandles(HTTPInTransport itr, SAMLMessageContext samlMsgCtx) throws SecurityPolicyException {
        return "POST".equals(itr.getHTTPMethod());
    }

    @Override
    protected byte[] getSignedContent(HTTPInTransport itr) throws SecurityPolicyException {
        StringBuilder builder = new StringBuilder();
        String samlMsg;
        try {
            if (itr.getParameterValue("SAMLRequest") != null) {
                samlMsg = new String(Base64.decode(itr.getParameterValue("SAMLRequest")), "UTF-8");
                builder.append("SAMLRequest=").append(samlMsg);
            } else if (itr.getParameterValue("SAMLResponse") != null) {
                samlMsg = new String(Base64.decode(itr.getParameterValue("SAMLResponse")), "UTF-8");
                builder.append("SAMLResponse=").append(samlMsg);
            } else {
                log.warn("Could not extract either a SAMLRequest or a SAMLResponse from the form control data");
                throw new SecurityPolicyException("Extract of SAMLRequest or SAMLResponse from form control data");
            }
        } catch (UnsupportedEncodingException e) {
            // All JVM's required to support UTF-8
        }

        if (itr.getParameterValue("RelayState") != null) {
            builder.append("&RelayState=").append(itr.getParameterValue("RelayState"));
        }

        builder.append("&SigAlg=").append(itr.getParameterValue("SigAlg"));

        String constructed = builder.toString();
        if (DatatypeHelper.isEmpty(constructed)) {
            log.warn("Could not construct signed content string from form control data");
            return null;
        }
        log.debug("Constructed signed content string for HTTP-Post-SimpleSign {}", constructed);

        try {
            return constructed.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // All JVM's required to support UTF-8
        }
        return null;
    }

    @Override
    protected List<Credential> getRequestCredentials(HTTPInTransport itr, SAMLMessageContext samlContext) throws SecurityPolicyException {
        String kiBase64 = itr.getParameterValue("KeyInfo");
        if (DatatypeHelper.isEmpty(kiBase64)) {
            log.debug("Form control data did not contain a KeyInfo");
            return null;
        } else {
            log.debug("Found a KeyInfo in form control data, extracting validation credentials");
        }

        Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory()
                .getUnmarshaller(KeyInfo.DEFAULT_ELEMENT_NAME);
        if (unmarshaller == null) {
            throw new SecurityPolicyException("Could not obtain a KeyInfo unmarshaller");
        }

        ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(kiBase64));
        KeyInfo keyInfo = null;
        try {
            Document doc = parser.parse(is);
            keyInfo = (KeyInfo) unmarshaller.unmarshall(doc.getDocumentElement());
        } catch (XMLParserException e) {
            log.warn("Error parsing KeyInfo data", e);
            throw new SecurityPolicyException("Error parsing KeyInfo data", e);
        } catch (UnmarshallingException e) {
            log.warn("Error unmarshalling KeyInfo data", e);
            throw new SecurityPolicyException("Error unmarshalling KeyInfo data", e);
        }

        if (keyInfo == null) {
            log.warn("Could not successfully extract KeyInfo object from the form control data");
            return null;
        }

        List<Credential> credentials = new ArrayList<Credential>();
        CriteriaSet criteriaSet = new CriteriaSet(new KeyInfoCriteria(keyInfo));
        try {
            for (Credential cred : keyInfoResolver.resolve(criteriaSet)) {
                credentials.add(cred);
            }
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.warn("Error resolving credentials from KeyInfo", e);
            throw new SecurityPolicyException("Error resolving credentials from KeyInfo", e);
        }

        return credentials;
    }
}
