package com.identityblitz.shibboleth.idp.config.profile;

import com.identityblitz.shibboleth.idp.config.profile.saml2.SAML2SSOProfileHandlerBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;


/**
 * Spring namespace handler for profile handler configurations.
 */
public class ProfileHandlerNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace URI. */
    public static final String NAMESPACE = "urn:identityblitz:shibboleth:2.0:idp:profile-handler";

    /** {@inheritDoc} */
    public void init() {

        registerBeanDefinitionParser(SAML2SSOProfileHandlerBeanDefinitionParser.SCHEMA_TYPE,
                new SAML2SSOProfileHandlerBeanDefinitionParser());

    }
}