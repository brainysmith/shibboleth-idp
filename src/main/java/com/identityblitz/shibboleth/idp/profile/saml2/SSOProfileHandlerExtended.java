package com.identityblitz.shibboleth.idp.profile.saml2;

import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPInTransportWithCookie;
import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPOutTransportWithCookie;
import com.identityblitz.shibboleth.idp.util.HttpHelper;
import edu.internet2.middleware.shibboleth.common.profile.ProfileException;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.Saml2LoginContext;
import edu.internet2.middleware.shibboleth.idp.profile.saml2.SSOProfileHandler;
import edu.internet2.middleware.shibboleth.idp.session.Session;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SSOProfileHandlerExtended extends SSOProfileHandler {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SSOProfileHandlerExtended.class);

    /** URL of the authentication manager Servlet. */
    private String authenticationManagerPath;

    /**
     * Constructor.
     *
     * @param authnManagerPath path to the authentication manager Servlet
     */
    public SSOProfileHandlerExtended(String authnManagerPath) {
        super(authnManagerPath);

        if (DatatypeHelper.isEmpty(authnManagerPath)) {
            throw new IllegalArgumentException("Authentication manager path may not be null");
        }
        if (authnManagerPath.startsWith("/")) {
            authenticationManagerPath = authnManagerPath;
        } else {
            authenticationManagerPath = "/" + authnManagerPath;
        }
    }

    @Override
    public void processRequest(HTTPInTransport inTransport, HTTPOutTransport outTransport) throws ProfileException {
        final HTTPInTransportWithCookie inTr = (HTTPInTransportWithCookie) inTransport;
        final HTTPOutTransportWithCookie outTr = (HTTPOutTransportWithCookie) outTransport;

        final LoginContext loginContext = HttpHelper.getLoginContext(getStorageService(), inTr, outTr);

        if(loginContext != null){
            HttpHelper.unbindLoginContext(getStorageService(), inTr, outTr);

            if(!(loginContext instanceof Saml2LoginContext)){
                log.debug("Incoming request contained a login context but it was not a Saml2LoginContext, processing as first leg of request");
                performAuthentication(inTransport, outTransport);
                return;
            }

            if(loginContext.isPrincipalAuthenticated()){
                log.debug("Incoming request contains a login context and indicates principal was authenticated, processing second leg of request");
                completeAuthenticationRequest((Saml2LoginContext)loginContext, inTransport, outTransport);
                return;
            }

            if(loginContext.getAuthenticationFailure() != null){
                log.debug("Incoming request contains a login context and indicates there was an error authenticating the principal, processing second leg of request");
                completeAuthenticationRequest((Saml2LoginContext)loginContext, inTransport, outTransport);
                return;
            }

            log.debug("Incoming request contains a login context but principal was not authenticated, processing first leg of request");
            performAuthentication(inTransport, outTransport);
            return;
        }

        log.debug("Incoming request does not contain a login context, processing as first leg of request");
        performAuthentication(inTransport, outTransport);
    }

    @Override
    protected void performAuthentication(HTTPInTransport inTransport, HTTPOutTransport outTransport) throws ProfileException {
        final HTTPInTransportWithCookie inTr = (HTTPInTransportWithCookie) inTransport;
        final HTTPOutTransportWithCookie outTr = (HTTPOutTransportWithCookie) outTransport;

        SSORequestContext requestContext = new SSORequestContextExtended();

        try {
            decodeRequest(requestContext, inTransport, outTransport);

            String relyingPartyId = requestContext.getInboundMessageIssuer();
            requestContext.setPeerEntityId(relyingPartyId);
            RelyingPartyConfiguration rpConfig = getRelyingPartyConfiguration(relyingPartyId);
            ProfileConfiguration ssoConfig = rpConfig.getProfileConfiguration(getProfileId());
            if (ssoConfig == null) {
                requestContext.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null,
                        "SAML 2 SSO profile not configured"));
                String msg = "SAML 2 SSO profile is not configured for relying party "
                        + requestContext.getInboundMessageIssuer();
                log.warn(msg);
                throw new ProfileException(msg);
            }

            log.debug("Creating login context and transferring control to authentication engine");
            Saml2LoginContext loginContext = new Saml2LoginContext(relyingPartyId, requestContext.getRelayState(),
                    requestContext.getInboundSAMLMessage());
            loginContext.setUnsolicited(requestContext.isUnsolicited());
            loginContext.setAuthenticationEngineURL(authenticationManagerPath);
            loginContext.setProfileHandlerURL(HttpHelper.getRequestUriWithoutContext(inTr));
            loginContext.setDefaultAuthenticationMethod(rpConfig.getDefaultAuthenticationMethod());

            HttpHelper.bindLoginContext(loginContext, getStorageService(), inTr, outTr);

            String authnEngineUrl = HttpHelper
                    .getContextRelativeUrl(inTr, authenticationManagerPath)
                    .buildURL();
            log.debug("Redirecting user to authentication engine at {}", authnEngineUrl);
            outTr.sendRedirect(authnEngineUrl);
        } catch (MarshallingException e) {
            requestContext.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null,
                    "Unable to marshall request"));
            log.error("Unable to marshall authentication request context");
            throw new ProfileException("Unable to marshall authentication request context", e);
        }
    }

    @Override
    protected Session getUserSession(InTransport inTransport) {
        return (Session) inTransport.getAttribute(Session.HTTP_SESSION_BINDING_ATTRIBUTE);
    }

    /** Stub in case we define additional context data. */
    /** In case we ever add something to the base context **/
    protected class SSORequestContextExtended extends SSORequestContext {
    }
}
