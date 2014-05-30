package com.identityblitz.shibboleth.idp.config.security.saml;

import com.identityblitz.saml2.binding.security.SAML2HTTPRedirectDeflateSignatureRuleExtended;
import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 */
public class SAML2HTTPRedirectDeflateSignatureRuleExtendedBDP extends AbstractSingleBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(SAMLSecurityNamespaceHandlerExtended.NAMESPACE, "SAML2HTTPRedirectSimpleSign");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return SAML2HTTPRedirectDeflateSignatureRuleExtended.class;
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        builder.addConstructorArgReference(DatatypeHelper.safeTrimOrNullString(element.getAttributeNS(null,
                "trustEngineRef")));

    }

}
