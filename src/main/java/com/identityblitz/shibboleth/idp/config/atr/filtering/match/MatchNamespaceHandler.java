package com.identityblitz.shibboleth.idp.config.atr.filtering.match;

/**
 * User: vkarpov
 * Date: 06.10.11
 */

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchNamespaceHandler extends BaseSpringNamespaceHandler {

    public static String NAMESPACE = "urn:blitz:shibboleth:2.0:afp";

    private static Logger log = LoggerFactory.getLogger(MatchNamespaceHandler.class);

    @Override
    public void init() {
        registerBeanDefinitionParser(AttributeInAuthorityAttributesBeanDefinitionParser.SCHEMA_TYPE, new AttributeInAuthorityAttributesBeanDefinitionParser());
        log.debug("AttributeInAuthorityAttributesBeanDefinitionParser has been loaded");
    }
}
