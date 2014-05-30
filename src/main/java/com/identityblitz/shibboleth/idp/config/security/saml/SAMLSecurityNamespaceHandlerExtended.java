package com.identityblitz.shibboleth.idp.config.security.saml;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 */
public class SAMLSecurityNamespaceHandlerExtended extends BaseSpringNamespaceHandler {

    /** Namespace for SAML security elements. */
    public static final String NAMESPACE = "urn:identityblitz:shibboleth:2.0:security:saml";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SAML2HTTPRedirectDeflateSignatureRuleExtendedBDP.SCHEMA_TYPE,
                new SAML2HTTPRedirectDeflateSignatureRuleExtendedBDP());

        registerBeanDefinitionParser(SAML2HTTPPostSimpleSignRuleExtendedBDP.SCHEMA_TYPE,
                new SAML2HTTPPostSimpleSignRuleExtendedBDP());
    }

}
