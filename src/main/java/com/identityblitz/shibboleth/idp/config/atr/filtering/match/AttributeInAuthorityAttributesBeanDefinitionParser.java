package com.identityblitz.shibboleth.idp.config.atr.filtering.match;

/**
 * Created by IntelliJ IDEA.
 * User: vkarpov
 * Date: 06.10.11
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */

import edu.internet2.middleware.shibboleth.common.config.attribute.filtering.BaseFilterBeanDefinitionParser;
import com.identityblitz.shibboleth.idp.config.atr.filtering.provider.match.AttributeInAuthorityAttributesMatchFunctor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;
import javax.xml.namespace.QName;

public class AttributeInAuthorityAttributesBeanDefinitionParser
        extends BaseFilterBeanDefinitionParser {

    public static final QName SCHEMA_TYPE = new QName(MatchNamespaceHandler.NAMESPACE,
            "AttributeInAuthorityAttributes");

    protected void doParse(Element configElement, BeanDefinitionBuilder builder) {
        super.doParse(configElement, builder);
    }

    protected Class getBeanClass(Element arg0) {
        return AttributeInAuthorityAttributesMatchFunctor.class;
    }
}
