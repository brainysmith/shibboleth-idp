package com.identityblitz.shibboleth.idp.config.security.saml;


import com.identityblitz.saml2.binding.security.SAML2HTTPPostSimpleSignRuleExtended;
import org.opensaml.xml.security.keyinfo.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.KeyInfoProvider;
import org.opensaml.xml.security.keyinfo.provider.DSAKeyValueProvider;
import org.opensaml.xml.security.keyinfo.provider.InlineX509DataProvider;
import org.opensaml.xml.security.keyinfo.provider.RSAKeyValueProvider;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by szaytsev on 5/30/14.
 */
public class SAML2HTTPPostSimpleSignRuleExtendedBDP extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SAMLSecurityNamespaceHandlerExtended.NAMESPACE, "SAML2HTTPPostSimpleSign");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML2HTTPPostSimpleSignRuleExtended.class;
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "trustEngineRef")));

        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "parserPoolRef")));

        List<KeyInfoProvider> keyInfoProviders = new ArrayList<KeyInfoProvider>();
        keyInfoProviders.add(new DSAKeyValueProvider());
        keyInfoProviders.add(new RSAKeyValueProvider());
        keyInfoProviders.add(new InlineX509DataProvider());
        KeyInfoCredentialResolver keyInfoCredResolver = new BasicProviderKeyInfoCredentialResolver(keyInfoProviders);
        builder.addConstructorArgValue(keyInfoCredResolver);
    }

}
