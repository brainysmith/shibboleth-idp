package com.identityblitz.shibboleth.idp.profile.saml2;

import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPInTransportWithCookie;
import com.identityblitz.shibboleth.idp.saml.ws.transposrt.HTTPOutTransportWithCookie;
import com.identityblitz.shibboleth.idp.slo.SLOHelper;
import com.identityblitz.shibboleth.idp.util.HttpHelper;
import edu.internet2.middleware.shibboleth.common.profile.ProfileException;
import edu.internet2.middleware.shibboleth.common.session.SessionManager;
import edu.internet2.middleware.shibboleth.idp.profile.saml2.SLOProfileHandler;
import edu.internet2.middleware.shibboleth.idp.session.Session;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SLOProfileHandlerExtended extends SLOProfileHandler {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SLOProfileHandlerExtended.class);


    public SLOProfileHandlerExtended() {
        super();
    }

    @Override
    public void processRequest(HTTPInTransport inTransport, HTTPOutTransport outTransport) throws ProfileException {
        final HTTPInTransportWithCookie itr = (HTTPInTransportWithCookie) inTransport;
        final HTTPOutTransportWithCookie otr = (HTTPOutTransportWithCookie) outTransport;

        SingleLogoutContext sloContext = SLOHelper.getSingleLogoutContext(otr);

        if (itr.getParameterValue("SAMLResponse") != null) {
            log.debug("Processing incoming SAML LogoutResponse");
            processLogoutResponse(sloContext, inTransport, outTransport);
        } else if (itr.getParameterValue("finish") != null) { //Front-channel case only
            //TODO this is just a hack
            if (sloContext.getRequesterEntityID() != null) {
                InitialLogoutRequestContext initialRequest = buildRequestContext(sloContext, inTransport, outTransport);
                respondToInitialRequest(sloContext, initialRequest);
            }
        } else if (itr.getParameterValue("action") != null) { //Front-channel case only, called by SLOServlet?action
            SingleLogoutContext.LogoutInformation nextActive = null;
            //try to retrieve the sp from request parameter
            String spEntityID = itr.getParameterValue("entityID");
            if (spEntityID != null) {
                spEntityID = spEntityID.trim();
                nextActive = sloContext.getServiceInformation().get(spEntityID);
            }
            if (nextActive == null) {
                throw new ProfileException("Requested SP could not be found");
            }
            if (!nextActive.isLoggedIn()) {
                throw new ProfileException("Already attempted to log out this service");
            }

            initiateFrontChannelLogout(sloContext, nextActive, outTransport);
        } else {
            processLogoutRequest(itr, otr);
        }
    }

    @Override
    protected void processLogoutRequest(HTTPInTransport inTransport, HTTPOutTransport outTransport) throws ProfileException {
        final HTTPInTransportWithCookie itr = (HTTPInTransportWithCookie) inTransport;
        final HTTPOutTransportWithCookie otr = (HTTPOutTransportWithCookie) outTransport;

        Session idpSession = getUserSession(inTransport);
        boolean idpInitiatedLogout = otr.getAttribute(IDP_INITIATED_LOGOUT_ATTR) != null;
        InitialLogoutRequestContext initialRequest = null;

        if (idpInitiatedLogout) {
            //idp initiated logout
            log.info("Starting the IdP-initiated logout process");
            initialRequest = createInitialLogoutRequestContext();
            initialRequest.setInboundMessageTransport(inTransport);
            otr.setAttribute(SKIP_LOGOUT_QUESTION_ATTR, true);
        } else {
            //sp initiated logout
            initialRequest = new InitialLogoutRequestContext();
            log.info("Processing incoming LogoutRequest");
            decodeRequest(initialRequest, inTransport, outTransport);
            checkSamlVersion(initialRequest);

            //if session is null, try to find nameid-bound one
            if (idpSession == null) {
                NameID nameID =
                        initialRequest.getInboundSAMLMessage().getNameID();
                SessionManager<Session> sessionManager = getSessionManager();
                String nameIDIndex = sessionManager.getIndexFromNameID(nameID);
                log.info("Session not found in request, trying to resolve session from NameID '{}'",
                        nameIDIndex);
                idpSession = sessionManager.getSession(nameIDIndex);
            }
        }

        if (idpSession == null) {
            log.warn("Cannot find IdP Session");
            initialRequest.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, StatusCode.UNKNOWN_PRINCIPAL_URI, null));
            throw new ProfileException("Cannot find IdP Session for principal");
        }

        if (!idpInitiatedLogout
                && !idpSession.getServicesInformation().keySet().
                contains(initialRequest.getInboundMessageIssuer())) {
            String msg = "Requesting entity is not session participant";
            log.warn(msg);
            initialRequest.setFailureStatus(buildStatus(StatusCode.REQUESTER_URI, StatusCode.REQUEST_DENIED_URI, msg));
            throw new ProfileException(msg);
        }

        // gam TODO support partial logout (not delete all session but delete sp info from session)
        SingleLogoutContext sloContext = buildSingleLogoutContext(initialRequest, idpSession);
        destroySession(sloContext);

        if (getInboundBinding().equals(SAMLConstants.SAML2_SOAP11_BINDING_URI)) {
            log.info("Issuing Backchannel logout requests");
            initiateBackChannelLogout(sloContext);

            respondToInitialRequest(sloContext, initialRequest);
        } else {
            //skip logout question if the requesting sp is the only session participant
            //if (!idpInitiatedLogout && sloContext.getServiceInformation().size() == 1) {
            // TODO (gam) support partial logout (with answering question)
            otr.setAttribute(SKIP_LOGOUT_QUESTION_ATTR, true);
            //}

            SLOHelper.bindSingleLogoutContext(sloContext, otr);
            populateServiceDisplayNames(sloContext);
            otr.sendRedirect("/SLOServlet");
        }
    }

    @Override
    protected SingleLogoutContext buildSingleLogoutContext(InitialLogoutRequestContext initialRequest, Session idpSession) {
        final HTTPInTransportWithCookie itr = (HTTPInTransportWithCookie) initialRequest.getInboundMessageTransport();

        return SingleLogoutContext.createInstance(
                HttpHelper.getRequestUriWithoutContext(itr),
                initialRequest,
                idpSession);
    }

    @Override
    protected Session getUserSession(InTransport inTransport) {
        return (Session) inTransport.getAttribute(Session.HTTP_SESSION_BINDING_ATTRIBUTE);
    }
}
