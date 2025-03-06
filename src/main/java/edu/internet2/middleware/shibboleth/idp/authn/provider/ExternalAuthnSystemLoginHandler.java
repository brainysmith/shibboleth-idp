package edu.internet2.middleware.shibboleth.idp.authn.provider;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.util.storage.StorageService;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;

/**
 * A login handler meant to bridge between the IdP and an external, web-based, authentication service.
 * 
 * This login handler will forward the user-agent to a context-relative path and include the following request
 * attributes: {@link #FORCE_AUTHN_PARAM}, {@link #PASSIVE_AUTHN_PARAM}, {@link #AUTHN_METHOD_PARAM}, and
 * {@link #RELYING_PARTY_PARAM}.
 * 
 * The external authentication system invocation Fileter/Servlet/JSP must, upon completion of authentication, set the
 * appropriate {@link javax.servlet.http.HttpServletRequest} attributes, as described by the
 * {@link edu.internet2.middleware.shibboleth.idp.authn.LoginHandler} interface and then invoke
 * {@link edu.internet2.middleware.shibboleth.idp.authn.AuthenticationEngine#returnToAuthenticationEngine(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
 * .
 */
public class ExternalAuthnSystemLoginHandler extends AbstractLoginHandler {

    /** Query parameter, {@value} , that indicates whether the authentication request requires forced authentication. */
    public static final String FORCE_AUTHN_PARAM = "forceAuthn";

    /** Query parameter, {@value} , that indicates whether the authentication requires passive authentication. */
    public static final String PASSIVE_AUTHN_PARAM = "isPassive";

    /** Query parameter, {@value} , that provides which authentication method should be attempted. */
    public static final String AUTHN_METHOD_PARAM = "authnMethod";

    /** Query parameter, {@value} , that provides the entity ID of the relying party that is requesting authentication. */
    public static final String RELYING_PARTY_PARAM = "relyingParty";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ExternalAuthnSystemLoginHandler.class);

    /** The context-relative path to the Filter, Servlet, or JSP that triggers the external authentication system. */
    private String externalAuthnPath;

    /** Constructor. */
    public ExternalAuthnSystemLoginHandler() {
        super();
    }

    /**
     * Get context-relative path to the Filter, Servlet, or JSP that triggers the external authentication system.
     * 
     * @return context-relative path to the Filter, Servlet, or JSP that triggers the external authentication system
     */
    public String getExternalAuthnPath() {
        return externalAuthnPath;
    }

    /**
     * Set context-relative path to the Filter, Servlet, or JSP that triggers the external authentication system.
     * 
     * @param path context-relative path to the Filter, Servlet, or JSP that triggers the external authentication
     *            system, may not be null or empty
     */
    public void setExternalAuthnPath(String path) {
        String trimmedPath = DatatypeHelper.safeTrimOrNullString(path);
        if (trimmedPath == null) {
            throw new IllegalArgumentException("External Authn path may not be null or empty");
        }

        externalAuthnPath = trimmedPath;
    }

    /** {@inheritDoc} */
    public void login(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        try {
            log.debug("Forwarding authentication request to {}", externalAuthnPath);
            populateRequestAttributes(httpRequest);
            RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(externalAuthnPath);
            dispatcher.forward(httpRequest, httpResponse);
            return;
        } catch (IOException e) {
            log.error("Unable to forward authentication request to external authentication system.", e);
        } catch (ServletException e) {
            log.error("Unable to forward authentication request to external authentication system.", e);
        }
    }

    /**
     * Sets the request attributes that will be sent to the external authentication service.
     * 
     * @param httpRequest current HTTP request
     */
    protected void populateRequestAttributes(HttpServletRequest httpRequest) {
        ServletContext servletContext = httpRequest.getSession().getServletContext();
        StorageService storageService = HttpServletHelper.getStorageService(servletContext);

        LoginContext loginContext = HttpServletHelper.getLoginContext(storageService, servletContext, httpRequest);
        if(loginContext == null){
            log.error("Unable to lookup login context for this request");
            return;
        }

        if (loginContext.isForceAuthRequired()) {
            httpRequest.setAttribute(FORCE_AUTHN_PARAM, Boolean.TRUE);
        } else {
            httpRequest.setAttribute(FORCE_AUTHN_PARAM, Boolean.FALSE);
        }

        if (loginContext.isPassiveAuthRequired()) {
            httpRequest.setAttribute(PASSIVE_AUTHN_PARAM, Boolean.TRUE);
        } else {
            httpRequest.setAttribute(PASSIVE_AUTHN_PARAM, Boolean.FALSE);
        }

        httpRequest.setAttribute(AUTHN_METHOD_PARAM, loginContext.getAttemptedAuthnMethod());

        httpRequest.setAttribute(RELYING_PARTY_PARAM, loginContext.getRelyingPartyId());
    }
}