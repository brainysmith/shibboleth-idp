/*
 *  Copyright 2009 NIIF Institute.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package edu.internet2.middleware.shibboleth.idp.profile.saml2;

import edu.internet2.middleware.shibboleth.common.profile.ProfileException;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.saml2.LogoutRequestConfiguration;
import edu.internet2.middleware.shibboleth.common.session.SessionManager;
import edu.internet2.middleware.shibboleth.common.util.HttpHelper;
import edu.internet2.middleware.shibboleth.idp.session.Session;
import edu.internet2.middleware.shibboleth.idp.slo.HTTPClientInTransportAdapter;
import edu.internet2.middleware.shibboleth.idp.slo.HTTPClientOutTransportAdapter;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContext;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContext.LogoutInformation;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContextStorageHelper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.ConnectionPoolTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.BasicEndpointSelector;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.decoding.SAMLMessageDecoder;
import org.opensaml.common.binding.encoding.SAMLMessageEncoder;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.binding.decoding.HTTPSOAP11Decoder;
import org.opensaml.saml2.binding.encoding.HTTPSOAP11Encoder;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.NameIDImpl;
import org.opensaml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.Organization;
import org.opensaml.saml2.metadata.OrganizationDisplayName;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml2.metadata.ServiceName;
import org.opensaml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.soap.client.http.HttpClientBuilder;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SLOProfileHandler extends AbstractSAML2ProfileHandler {

    private static final Logger log =
            LoggerFactory.getLogger(SLOProfileHandler.class);
    public static final String IDP_INITIATED_LOGOUT_ATTR =
            "IDP_INITIATED_LOGOUT";
    public static final String SKIP_LOGOUT_QUESTION_ATTR =
            "SKIP_LOGOUT_QUESTION";
    private final SAMLObjectBuilder<SingleLogoutService> sloServiceBuilder;
    private final SAMLObjectBuilder<LogoutResponse> responseBuilder;
    private final SAMLObjectBuilder<NameID> nameIDBuilder;
    private final SAMLObjectBuilder<LogoutRequest> requestBuilder;
    private final SAMLObjectBuilder<Issuer> issuerBuilder;

    @SuppressWarnings("unchecked")
    public SLOProfileHandler() {
        super();
        sloServiceBuilder = (SAMLObjectBuilder<SingleLogoutService>) getBuilderFactory().getBuilder(
                SingleLogoutService.DEFAULT_ELEMENT_NAME);
        responseBuilder =
                (SAMLObjectBuilder<LogoutResponse>) getBuilderFactory().getBuilder(LogoutResponse.DEFAULT_ELEMENT_NAME);
        nameIDBuilder =
                (SAMLObjectBuilder<NameID>) getBuilderFactory().getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        requestBuilder =
                (SAMLObjectBuilder<LogoutRequest>) getBuilderFactory().getBuilder(LogoutRequest.DEFAULT_ELEMENT_NAME);
        issuerBuilder =
                (SAMLObjectBuilder<Issuer>) getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void populateSAMLMessageInformation(BaseSAMLProfileRequestContext requestContext)
            throws ProfileException {

        if (requestContext.getInboundSAMLMessage() instanceof LogoutRequest) {
            LogoutRequest request =
                    (LogoutRequest) requestContext.getInboundSAMLMessage();

            if (request != null) {
                request.getSessionIndexes(); //TODO session indexes?

                requestContext.setPeerEntityId(request.getIssuer().getValue());
                requestContext.setInboundSAMLMessageId(request.getID());
                if (request.getNameID() != null) {
                    requestContext.setSubjectNameIdentifier(request.getNameID());
                } else if (request.getEncryptedID() != null) {
                    requestContext.setSubjectNameIdentifier(request.getEncryptedID());
                } else {
                    throw new ProfileException("Incoming Logout Request did not contain SAML2 NameID.");
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void populateRelyingPartyInformation(BaseSAMLProfileRequestContext requestContext)
            throws ProfileException {
        super.populateRelyingPartyInformation(requestContext);

        EntityDescriptor relyingPartyMetadata =
                requestContext.getPeerEntityMetadata();
        if (relyingPartyMetadata != null) {
            requestContext.setPeerEntityRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
            requestContext.setPeerEntityRoleMetadata(relyingPartyMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS));
        }
    }

    @Override
    protected Endpoint selectEndpoint(BaseSAMLProfileRequestContext requestContext)
            throws ProfileException {
        Endpoint endpoint = null;

        if (getInboundBinding().equals(SAMLConstants.SAML2_SOAP11_BINDING_URI)) {

            endpoint = sloServiceBuilder.buildObject();
            endpoint.setBinding(SAMLConstants.SAML2_SOAP11_BINDING_URI);
        } else {
            BasicEndpointSelector endpointSelector = new BasicEndpointSelector();
            endpointSelector.setEndpointType(SingleLogoutService.DEFAULT_ELEMENT_NAME);
            endpointSelector.setMetadataProvider(getMetadataProvider());
            endpointSelector.setEntityMetadata(requestContext.getPeerEntityMetadata());
            endpointSelector.setEntityRoleMetadata(requestContext.getPeerEntityRoleMetadata());
            endpointSelector.setSamlRequest(requestContext.getInboundSAMLMessage());
            endpointSelector.getSupportedIssuerBindings().addAll(getSupportedOutboundBindings());
            endpoint = endpointSelector.selectEndpoint();
        }

        return endpoint;
    }

    @Override
    public String getProfileId() {
        return LogoutRequestConfiguration.PROFILE_ID;
    }

    public void processRequest(HTTPInTransport inTransport, HTTPOutTransport outTransport)
            throws ProfileException {

        HttpServletRequest servletRequest =
                ((HttpServletRequestAdapter) inTransport).getWrappedRequest();
        SingleLogoutContext sloContext =
                SingleLogoutContextStorageHelper.getSingleLogoutContext(servletRequest);

        //TODO RelayState is lost?!
        //TODO catch profileexception and respond with saml error.
        if (servletRequest.getParameter("SAMLResponse") != null) {
            log.debug("Processing incoming SAML LogoutResponse");
            processLogoutResponse(sloContext, inTransport, outTransport);
        } else if (servletRequest.getParameter("finish") != null) { //Front-channel case only
            //TODO this is just a hack
            if (sloContext.getRequesterEntityID() != null) {
                InitialLogoutRequestContext initialRequest =
                        buildRequestContext(sloContext, inTransport, outTransport);
                respondToInitialRequest(sloContext, initialRequest);
            }
        } else if (servletRequest.getParameter("action") != null) { //Front-channel case only, called by SLOServlet?action
            LogoutInformation nextActive = null;
            //try to retrieve the sp from request parameter
            String spEntityID = servletRequest.getParameter("entityID");
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
            processLogoutRequest(inTransport, outTransport);
        }
    }

    /**
     * Tries to decode logout response.
     * 
     * @param inTransport
     * @param outTransport
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected boolean processLogoutResponse(SingleLogoutContext sloContext,
            HTTPInTransport inTransport, HTTPOutTransport outTransport)
            throws ProfileException {

        LogoutRequestContext requestCtx = new LogoutRequestContext();
        requestCtx.setInboundMessageTransport(inTransport);
        SAMLMessageDecoder decoder =
                getMessageDecoders().get(getInboundBinding());
        LogoutResponse logoutResponse;
        try {
            decoder.decode(requestCtx);
            logoutResponse = requestCtx.getInboundSAMLMessage();
        } catch (MessageDecodingException ex) {
            log.warn("Cannot decode LogoutResponse", ex);
            throw new ProfileException(ex);
        } catch (SecurityException ex) {
            log.warn("Exception while validating LogoutResponse", ex);
            throw new ProfileException(ex);
        } catch (ClassCastException ex) {
            log.debug("Cannot decode LogoutResponse", ex);
            //this is the case when inbound message is LogoutRequest, so return silently
            return false;
        }

        String inResponseTo = logoutResponse.getInResponseTo();
        String spEntityID = requestCtx.getInboundMessageIssuer();

        log.debug("Received response from '{}' to request '{}'", spEntityID, inResponseTo);
        LogoutInformation serviceLogoutInfo =
                sloContext.getServiceInformation().get(spEntityID);
        if (serviceLogoutInfo == null) {
            throw new ProfileException("LogoutResponse issuer is unknown");
        }
        if (!serviceLogoutInfo.getLogoutRequestId().equals(inResponseTo)) {
            serviceLogoutInfo.setLogoutFailed();
            throw new ProfileException("LogoutResponse InResponseTo does not forceReauthn the LogoutRequest ID");
        }
        log.info("Logout status is '{}'", logoutResponse.getStatus().getStatusCode().getValue().toString());
        if (logoutResponse.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS_URI)) {
            serviceLogoutInfo.setLogoutSucceeded();
        } else {
            serviceLogoutInfo.setLogoutFailed();
        }

        return true;
    }

    /**
     * Continue logout processing.
     * 
     * @param inTransport
     * @param outTransport
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected void processLogoutRequest(HTTPInTransport inTransport, HTTPOutTransport outTransport)
            throws ProfileException {

        HttpServletRequest servletRequest =
                ((HttpServletRequestAdapter) inTransport).getWrappedRequest();
        Session idpSession = getUserSession(inTransport);
        boolean idpInitiatedLogout =
                servletRequest.getAttribute(IDP_INITIATED_LOGOUT_ATTR) != null;
        InitialLogoutRequestContext initialRequest = null;

        if (idpInitiatedLogout) {
            //idp initiated logout
            log.info("Starting the IdP-initiated logout process");
            initialRequest = createInitialLogoutRequestContext();
            initialRequest.setInboundMessageTransport(inTransport);
            servletRequest.setAttribute(SKIP_LOGOUT_QUESTION_ATTR, true);
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
        SingleLogoutContext sloContext =
                buildSingleLogoutContext(initialRequest, idpSession);
        destroySession(sloContext);

        if (getInboundBinding().equals(SAMLConstants.SAML2_SOAP11_BINDING_URI)) {
            log.info("Issuing Backchannel logout requests");
            initiateBackChannelLogout(sloContext);

            respondToInitialRequest(sloContext, initialRequest);
        } else {
            //skip logout question if the requesting sp is the only session participant
            //if (!idpInitiatedLogout && sloContext.getServiceInformation().size() == 1) {
                // TODO (gam) support partial logout (with answering question)
                servletRequest.setAttribute(SKIP_LOGOUT_QUESTION_ATTR, true);
            //}
            HttpServletResponse servletResponse =
                    ((HttpServletResponseAdapter) outTransport).getWrappedResponse();
            SingleLogoutContextStorageHelper.bindSingleLogoutContext(sloContext, servletRequest);
            populateServiceDisplayNames(sloContext);
            try {
                servletRequest.getRequestDispatcher("/SLOServlet").forward(servletRequest, servletResponse);
            } catch (ServletException ex) {
                String msg = "Cannot forward request to SLO Servlet";
                log.error(msg, ex);
                initialRequest.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null, msg));
                throw new ProfileException(ex);
            } catch (IOException ex) {
                String msg = "Cannot forward request to SLO Servlet";
                log.error(msg, ex);
                initialRequest.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null, msg));
                throw new ProfileException(ex);
            }
        }
    }

    /**
     * Issue back-channel logout requests to all session participants.
     * 
     * @param idpSession
     * @return
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    public SingleLogoutContext administrativeLogout(Session idpSession) throws ProfileException {
        log.info("Administratively logging out user '{}'", idpSession.getPrincipalName());
        InitialLogoutRequestContext initialRequest = createInitialLogoutRequestContext();
        SingleLogoutContext sloContext = SingleLogoutContext.createInstance(null, initialRequest, idpSession);
        try {
            initiateBackChannelLogout(sloContext);
        } catch (ProfileException e) {
            log.error("Exception was caught while administratively logging out user '{}'",
                    idpSession.getPrincipalName(), e);
        }
        destroySession(sloContext);
        return sloContext;
    }

    /**
     * Creates SAML2 LogoutRequest and corresponding context.
     *
     * @param sloContext
     * @param serviceLogoutInfo
     * @param endpoint
     * @return
     */
    private LogoutRequestContext createLogoutRequestContext(
            SingleLogoutContext sloContext,
            LogoutInformation serviceLogoutInfo,
            Endpoint endpoint) {

        String spEntityID = serviceLogoutInfo.getEntityID();
        log.debug("Trying SP: {}", spEntityID);
        LogoutRequest request = buildLogoutRequest(sloContext);

        serviceLogoutInfo.setLogoutRequestId(request.getID());

        NameID nameId = buildNameID(serviceLogoutInfo);
        if (nameId == null) {
            log.info("NameID is null, cannot crete logout request context");
            return null;
        }

        request.setNameID(nameId);
        request.setDestination(endpoint.getLocation());

        LogoutRequestContext requestCtx = new LogoutRequestContext();
        requestCtx.setCommunicationProfileId(getProfileId());
        requestCtx.setSecurityPolicyResolver(getSecurityPolicyResolver());
        requestCtx.setOutboundMessageIssuer(sloContext.getResponderEntityID());
        requestCtx.setInboundMessageIssuer(spEntityID);
        requestCtx.setPeerEntityEndpoint(endpoint);
        requestCtx.setPeerEntityRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);
        //TODO get credential configured for relying party
        Credential signingCredential =
                getRelyingPartyConfigurationManager().
                getDefaultRelyingPartyConfiguration().getDefaultSigningCredential();
        requestCtx.setOutboundSAMLMessageSigningCredential(signingCredential);
        requestCtx.setOutboundSAMLMessage(request);

        return requestCtx;
    }

    /**
     * Destroy idp session.
     *
     * @param sloContext
     */
    protected void destroySession(SingleLogoutContext sloContext) {
        log.info("Invalidating session '{}'.", sloContext.getIdpSessionID());
        getSessionManager().destroySession(sloContext.getIdpSessionID());
    }

    /**
     * Issues back channel logout request to every session participant.
     *
     * @param sloContext
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected void initiateBackChannelLogout(SingleLogoutContext sloContext) throws ProfileException {
        for (LogoutInformation serviceLogoutInfo : sloContext.getServiceInformation().values()) {
            if (serviceLogoutInfo.isLoggedIn()) {
                try {
                    initiateBackChannelLogout(sloContext, serviceLogoutInfo);
                } catch (ProfileException ex) {
                    log.warn("Caught exception while trying to issue LogoutRequest to '{}'",
                            serviceLogoutInfo.getEntityID(), ex);
                    serviceLogoutInfo.setLogoutFailed();
                }
            }
        }
    }

    /**
     * Issues back channel logout request to session participant.
     *
     * @param sloContext
     * @param serviceLogoutInfo
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    private void initiateBackChannelLogout(SingleLogoutContext sloContext, LogoutInformation serviceLogoutInfo)
            throws ProfileException {

        if (!serviceLogoutInfo.isLoggedIn()) {
            log.info("Logout status for entity is '{}', not attempting logout", serviceLogoutInfo.getLogoutStatus().toString());
            return;
        }

        String spEntityID = serviceLogoutInfo.getEntityID();
        Endpoint endpoint =
                getEndpointForBinding(spEntityID, SAMLConstants.SAML2_SOAP11_BINDING_URI);
        if (endpoint == null) {
            log.info("No SAML2 LogoutRequest SOAP endpoint found for entity '{}'", spEntityID);
            serviceLogoutInfo.setLogoutUnsupported();
            return;
        }

        serviceLogoutInfo.setLogoutAttempted();
        LogoutRequestContext requestCtx =
                createLogoutRequestContext(sloContext, serviceLogoutInfo, endpoint);
        if (requestCtx == null) {
            log.info("Cannot create LogoutRequest Context for entity '{}'", spEntityID);
            serviceLogoutInfo.setLogoutFailed();
            return;
        }
        HttpConnection httpConn = null;
        try {
            //prepare http message exchange for soap
            log.debug("Preparing HTTP transport for SOAP request");
            httpConn = createHttpConnection(serviceLogoutInfo, endpoint);
            if (httpConn == null) {
                log.warn("Unable to acquire usable http connection from the pool");
                serviceLogoutInfo.setLogoutFailed();
                return;
            }
            log.debug("Opening HTTP connection to '{}'", endpoint.getLocation());
            httpConn.open();
            if (!httpConn.isOpen()) {
                log.warn("HTTP connection could not be opened");
                serviceLogoutInfo.setLogoutFailed();
                return;
            }

            log.debug("Preparing transports and encoders/decoders");
            prepareSOAPTransport(requestCtx, httpConn, endpoint);
            SAMLMessageEncoder encoder = new HTTPSOAP11Encoder();
            SAMLMessageDecoder decoder =
                    new HTTPSOAP11Decoder(getParserPool());

            //encode and sign saml request
            encoder.encode(requestCtx);
            //TODO: audit log is still missing

            log.info("Issuing back-channel logout request to SP '{}'", spEntityID);
            //execute SOAP/HTTP call
            log.debug("Executing HTTP POST");
            if (!requestCtx.execute(httpConn)) {
                log.warn("Logout execution failed on SP '{}', HTTP status is '{}'",
                        spEntityID, requestCtx.getHttpStatus());
                serviceLogoutInfo.setLogoutFailed();

                return;
            }

            //decode saml response
            decoder.decode(requestCtx);

            LogoutResponse spResponse = requestCtx.getInboundSAMLMessage();
            StatusCode statusCode = spResponse.getStatus().getStatusCode();
            if (statusCode.getValue().equals(StatusCode.SUCCESS_URI)) {
                log.info("Logout was successful on SP '{}'.", spEntityID);
                serviceLogoutInfo.setLogoutSucceeded();
            } else {
                log.warn("Logout failed on SP '{}', logout status code is '{}'.", spEntityID, statusCode.getValue());
                StatusCode secondaryCode = statusCode.getStatusCode();
                if (secondaryCode != null) {
                    log.warn("Additional status code: '{}'", secondaryCode.getValue());
                }
                serviceLogoutInfo.setLogoutFailed();
            }
        } catch (SocketTimeoutException e) { //socket connect or read timeout
            log.info("Socket timeout while sending SOAP request to SP '{}'",
                    serviceLogoutInfo.getEntityID());
            serviceLogoutInfo.setLogoutFailed();
        } catch (IOException e) { //other networking error
            log.info("IOException caught while sending SOAP request", e);
            serviceLogoutInfo.setLogoutFailed();
        } catch (Throwable t) { //unexpected
            log.error("Unexpected exception caught while sending SAML Logout request", t);
            serviceLogoutInfo.setLogoutFailed();
        } finally { //
            requestCtx.releaseConnection();
            if (httpConn != null && httpConn.isOpen()) {
                log.debug("Closing HTTP connection");
                try {
                    httpConn.close();
                } catch (Throwable t) {
                    log.warn("Caught exception while closing HTTP Connection", t);
                }
            }
        }
    }

    protected InitialLogoutRequestContext createInitialLogoutRequestContext() {
        InitialLogoutRequestContext initialRequest = new InitialLogoutRequestContext();
        RelyingPartyConfiguration defaultRPC =
                getRelyingPartyConfigurationManager().getDefaultRelyingPartyConfiguration();
        initialRequest.setLocalEntityId(defaultRPC.getProviderId());
        initialRequest.setProfileConfiguration(
                (LogoutRequestConfiguration) defaultRPC.getProfileConfiguration(getProfileId()));

        return initialRequest;
    }

    /**
     * Reads SAML2 SingleLogoutService endpoint of the entity or
     * null if no metadata or endpoint found.
     *
     * @param spEntityID
     * @param bindingURI which binding to use
     * @return
     */
    private Endpoint getEndpointForBinding(String spEntityID, String bindingURI) {
        RoleDescriptor spMetadata = null;
        try {
            //retrieve metadata
            spMetadata =
                    getMetadataProvider().getRole(spEntityID, SPSSODescriptor.DEFAULT_ELEMENT_NAME, SAMLConstants.SAML20P_NS);
            if (spMetadata == null) {
                log.warn("SP Metadata is null");
                return null;
            }
        } catch (MetadataProviderException ex) {
            log.info("Cannot get SAML2 metadata for SP '{}'.", spEntityID);
            return null;
        }

        //find endpoint for SingleLogoutService
        BasicEndpointSelector es = new BasicEndpointSelector();
        es.setEndpointType(SingleLogoutService.DEFAULT_ELEMENT_NAME);
        es.setMetadataProvider(getMetadataProvider());
        es.getSupportedIssuerBindings().add(bindingURI);
        es.setEntityRoleMetadata(spMetadata);
        Endpoint endpoint = es.selectEndpoint();
        if (endpoint == null) {
            log.info("Cannot get SAML2 SingleLogoutService endpoint for SP '{}' and binding '{}'.", spEntityID, bindingURI);
            return null;
        }

        return endpoint;
    }

    /**
     * Builds NameID for the principal and the SP.
     *
     * TODO support encrypted nameid?
     *
     * @param serviceLogoutInfo
     * @return
     */
    private NameID buildNameID(LogoutInformation serviceLogoutInfo) {
        if (serviceLogoutInfo.getNameIdentifier() == null) {
            return null;
        }
        NameID nameId = nameIDBuilder.buildObject();
        nameId.setFormat(serviceLogoutInfo.getNameIdentifierFormat());
        nameId.setValue(serviceLogoutInfo.getNameIdentifier());
        nameId.setNameQualifier(serviceLogoutInfo.getNameQualifier());
        nameId.setSPNameQualifier(serviceLogoutInfo.getSPNameQualifier());

        return nameId;
    }

    /**
     * Build SAML request for issuing LogoutRequest.
     * 
     * @param sloContext
     //* @param spEntityID
     * @return
     */
    private LogoutRequest buildLogoutRequest(SingleLogoutContext sloContext) {
        LogoutRequest request = requestBuilder.buildObject();
        //build saml request
        DateTime issueInstant = new DateTime();
        request.setIssueInstant(issueInstant);
        request.setID(getIdGenerator().generateIdentifier());
        request.setVersion(SAMLVersion.VERSION_20);
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(sloContext.getResponderEntityID());
        request.setIssuer(issuer);

        return request;
    }

    /**
     * Populate service display names from metadata.
     * This method must be called once.
     *
     * @param sloContext
     */
    protected void populateServiceDisplayNames(SingleLogoutContext sloContext) {
        MetadataProvider mdProvider = getMetadataProvider();
        for (LogoutInformation serviceInfo : sloContext.getServiceInformation().values()) {
            EntityDescriptor spMetadata;
            String spEntityID = serviceInfo.getEntityID();
            try {
                spMetadata = mdProvider.getEntityDescriptor(spEntityID);
            } catch (MetadataProviderException ex) {
                log.warn("Can not get metadata for relying party '{}'", spEntityID);
                continue;
            }
            Map<String, String> serviceNames = extractServiceNames(spMetadata);
            if (serviceNames != null && serviceNames.size() > 0) {
                serviceInfo.setDisplayName(serviceNames);
            } else {
                Map<String, String> organizationDNames = extractOrganizationDisplayNames(spMetadata);
                if (organizationDNames != null && organizationDNames.size() > 0) {
                    serviceInfo.setDisplayName(organizationDNames);
                }
            }
        }
    }

    /**
     * Extracts ServiceName information from SP Entity Descriptor.
     * 
     * @param spMetadata
     * @return
     */
    private Map<String, String> extractServiceNames(EntityDescriptor spMetadata) {
        String spEntityID = spMetadata.getEntityID();
        SPSSODescriptor spDescr = spMetadata.getSPSSODescriptor(SAMLConstants.SAML20P_NS);
        if (spDescr == null) {
            log.debug("No SAML SPSSODescriptor found for relying party '{}'", spEntityID);
            return null;
        }
        AttributeConsumingService attrCs = spDescr.getDefaultAttributeConsumingService();
        if (attrCs == null) {
            List<AttributeConsumingService> attrCSList = spDescr.getAttributeConsumingServices();
            if (attrCSList != null && !attrCSList.isEmpty()) {
                attrCs = attrCSList.get(0);
            }
        }
        if (attrCs == null) {
            log.debug("No AttributeConsumingService found for relying party '{}'", spEntityID);
            return null;
        }
        List<ServiceName> sNameList = attrCs.getNames();
        if (sNameList == null) {
            log.debug("No ServiceName found for relying party '{}'", spEntityID);
            return null;
        }
        Map<String, String> serviceNames =
                new HashMap<String, String>(sNameList.size());
        for (ServiceName sName : sNameList) {
            serviceNames.put(sName.getName().getLanguage(), sName.getName().getLocalString());
        }
        return serviceNames;
    }

    /**
     * Extracts OrganizationDisplayName information from SP Entity Descriptor.
     *
     * @param spMetadata
     * @return
     */
    private Map<String, String> extractOrganizationDisplayNames(EntityDescriptor spMetadata) {
        String spEntityID = spMetadata.getEntityID();
        Organization spOrg = spMetadata.getOrganization();
        if (spOrg == null) {
            log.debug("Organization is not set for relying party '{}'", spEntityID);
            return null;
        }
        List<OrganizationDisplayName> dNameList =
                spOrg.getDisplayNames();
        if (dNameList == null) {
            log.debug("DisplayName is unset for relying party '{}'", spEntityID);
            return null;
        }
        Map<String, String> oDNames = new HashMap<String, String>(dNameList.size());
        for (OrganizationDisplayName dName : dNameList) {
            oDNames.put(dName.getName().getLanguage(), dName.getName().getLocalString());
        }
        return oDNames;
    }

    /**
     * Creates Http connection.
     *
     * @param serviceLogoutInfo
     * @param endpoint
     * @return
     * @throws org.apache.commons.httpclient.URIException
     * @throws java.security.GeneralSecurityException
     * @throws java.io.IOException
     */
    private HttpConnection createHttpConnection(
            LogoutInformation serviceLogoutInfo, Endpoint endpoint)
            throws URIException, GeneralSecurityException, IOException {

        HttpClientBuilder httpClientBuilder =
                new HttpClientBuilder();
        httpClientBuilder.setContentCharSet("UTF-8");
        SecureProtocolSocketFactory sf = new EasySSLProtocolSocketFactory();
        httpClientBuilder.setHttpsProtocolSocketFactory(sf);

        //build http connection
        HttpClient httpClient = httpClientBuilder.buildClient();

        HostConfiguration hostConfig = new HostConfiguration();
        URI location = new URI(endpoint.getLocation());
        hostConfig.setHost(location);

        LogoutRequestConfiguration config = (LogoutRequestConfiguration) getProfileConfiguration(
                serviceLogoutInfo.getEntityID(), getProfileId());
        if (log.isDebugEnabled()) {
            log.debug("Creating new HTTP connection with the following timeouts:");
            log.debug("Maximum waiting time for the connection pool is {}",
                    config.getBackChannelConnectionPoolTimeout());
            log.debug("Timeout for connection establishment is {}",
                    config.getBackChannelConnectionTimeout());
            log.debug("Timeout for soap response is {}",
                    config.getBackChannelResponseTimeout());
        }
        HttpConnection httpConn = null;
        try {
            httpConn = httpClient.getHttpConnectionManager().
                    getConnectionWithTimeout(hostConfig, config.getBackChannelConnectionPoolTimeout());
        } catch (ConnectionPoolTimeoutException e) {
            return null;
        }

        HttpConnectionParams params = new HttpConnectionParams();
        params.setConnectionTimeout(config.getBackChannelConnectionTimeout());
        params.setSoTimeout(config.getBackChannelResponseTimeout());
        httpConn.setParams(params);

        return httpConn;
    }

    /**
     * Adapts SOAP/HTTP client transport to SAML transports.
     * @param requestCtx
     * @param httpConn
     * @param endpoint
     */
    private void prepareSOAPTransport(LogoutRequestContext requestCtx,
            HttpConnection httpConn, Endpoint endpoint) {

        EntityEnclosingMethod method =
                new PostMethod(endpoint.getLocation());
        requestCtx.setPostMethod(method);
        HTTPOutTransport soapOutTransport =
                new HTTPClientOutTransportAdapter(httpConn, method);
        HTTPInTransport soapInTransport =
                new HTTPClientInTransportAdapter(httpConn, method);
        requestCtx.setOutboundMessageTransport(soapOutTransport);
        requestCtx.setInboundMessageTransport(soapInTransport);
    }

    /**
     * Issues front and back channel logout requests to session participants.
     * 
     * @param sloContext
     * @param serviceLogoutInfo
     * @param outTransport
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected void initiateFrontChannelLogout(
            SingleLogoutContext sloContext,
            LogoutInformation serviceLogoutInfo,
            HTTPOutTransport outTransport)
            throws ProfileException {

        if (!serviceLogoutInfo.isLoggedIn()) {
            log.info("Logout status for entity is '{}', not attempting logout", serviceLogoutInfo.getLogoutStatus().toString());
            return;
        }

        String spEntityID = serviceLogoutInfo.getEntityID();
        //prefer HTTP-Redirect binding
        Endpoint endpoint =
                getEndpointForBinding(spEntityID, SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        if (endpoint == null) {
            //fallback to HTTP-POST when no HTTP-Redirect is set
            endpoint =
                    getEndpointForBinding(spEntityID, SAMLConstants.SAML2_POST_BINDING_URI);
        }
        if (endpoint == null) {
            log.info("No SAML2 LogoutRequest front-channel endpoint found for entity '{}'", spEntityID);
            endpoint =
                    getEndpointForBinding(spEntityID, SAMLConstants.SAML2_SOAP11_BINDING_URI);
            if (endpoint != null) {
                //fallback to SOAP1.1 when no HTTP-POST is set
                initiateBackChannelLogout(sloContext, serviceLogoutInfo);
            } else {
                //no supported endpoints found
                serviceLogoutInfo.setLogoutUnsupported();
            }
            return;
        }
        SAMLMessageEncoder encoder =
                getMessageEncoders().get(endpoint.getBinding());
        if (encoder == null) {
            log.warn("No message encoder found for binding '{}'", endpoint.getBinding());
            serviceLogoutInfo.setLogoutUnsupported();
            return;
        }

        serviceLogoutInfo.setLogoutAttempted();
        LogoutRequestContext requestCtx =
                createLogoutRequestContext(sloContext, serviceLogoutInfo, endpoint);
        if (requestCtx == null) {
            log.info("Cannot create LogoutRequest Context for entity '{}'", spEntityID);
            serviceLogoutInfo.setLogoutFailed();
            return;
        }
        requestCtx.setOutboundMessageTransport(outTransport);

        try {
            encoder.encode(requestCtx);
        } catch (MessageEncodingException ex) {
            log.warn("Cannot encode LogoutRequest", ex);
            serviceLogoutInfo.setLogoutFailed();
            return;
        }
    }

    /**
     * Respond to LogoutRequest.
     * 
     * @param sloContext
     * @param initialRequest
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected void respondToInitialRequest(SingleLogoutContext sloContext, InitialLogoutRequestContext initialRequest)
            throws ProfileException {

        boolean success = true;
        for (LogoutInformation info : sloContext.getServiceInformation().values()) {
            if (!info.getLogoutStatus().equals(SingleLogoutContext.LogoutStatus.LOGOUT_SUCCEEDED)) {
                success = false;
            }
        }
        Status status;
        if (success) {
            log.info("Status of Single Log-out: success");
            status = buildStatus(StatusCode.SUCCESS_URI, null, null);
        } else {
            log.info("Status of Single Log-out: partial");
            status =
                    buildStatus(StatusCode.SUCCESS_URI, StatusCode.PARTIAL_LOGOUT_URI, null);
        }

        LogoutResponse samlResponse =
                buildLogoutResponse(initialRequest, status);
        populateRelyingPartyInformation(initialRequest);
        Endpoint endpoint = selectEndpoint(initialRequest);
        initialRequest.setPeerEntityEndpoint(endpoint);
        //
        initialRequest.setOutboundSAMLMessage(samlResponse);
        //
        initialRequest.setOutboundMessage(samlResponse);
        initialRequest.setOutboundSAMLMessageId(samlResponse.getID());
        initialRequest.setOutboundSAMLMessageIssueInstant(samlResponse.getIssueInstant());
        Credential signingCredential =
                initialRequest.getProfileConfiguration().getSigningCredential();
        if (signingCredential == null) {
            initialRequest.getRelyingPartyConfiguration().getDefaultSigningCredential();
        }
        initialRequest.setOutboundSAMLMessageSigningCredential(signingCredential);

        log.debug("Sending response to the original LogoutRequest");
        encodeResponse(initialRequest);
        writeAuditLogEntry(initialRequest);
    }

    /**
     * Builds new single log-out context for session store between logout events.
     *
     * @param initialRequest
     * @param idpSession
     * @return
     */
    protected SingleLogoutContext buildSingleLogoutContext(InitialLogoutRequestContext initialRequest, Session idpSession) {
        HttpServletRequest servletRequest =
                ((HttpServletRequestAdapter) initialRequest.getInboundMessageTransport()).getWrappedRequest();

        return SingleLogoutContext.createInstance(
                HttpHelper.getRequestUriWithoutContext(servletRequest),
                initialRequest,
                idpSession);
    }

    /**
     * Builds request context from information available after logout events.
     *
     * @param sloContext
     * @return
     */
    protected InitialLogoutRequestContext buildRequestContext(SingleLogoutContext sloContext,
            HTTPInTransport in, HTTPOutTransport out) throws ProfileException {

        InitialLogoutRequestContext initialRequest = new InitialLogoutRequestContext();

        initialRequest.setCommunicationProfileId(getProfileId());
        initialRequest.setMessageDecoder(getMessageDecoders().get(getInboundBinding()));
        initialRequest.setInboundMessageTransport(in);
        initialRequest.setInboundSAMLProtocol(SAMLConstants.SAML20P_NS);
        initialRequest.setOutboundMessageTransport(out);
        initialRequest.setOutboundSAMLProtocol(SAMLConstants.SAML20P_NS);
        initialRequest.setMetadataProvider(getMetadataProvider());
        initialRequest.setInboundSAMLMessageId(sloContext.getRequestSAMLMessageID());
        initialRequest.setInboundMessageIssuer(sloContext.getRequesterEntityID());
        initialRequest.setLocalEntityId(sloContext.getResponderEntityID());
        initialRequest.setPeerEntityId(sloContext.getRequesterEntityID());
        initialRequest.setSecurityPolicyResolver(getSecurityPolicyResolver());
        initialRequest.setProfileConfiguration(
                (LogoutRequestConfiguration) getProfileConfiguration(
                sloContext.getRequesterEntityID(), getProfileId()));
        initialRequest.setRelyingPartyConfiguration(
                getRelyingPartyConfiguration(sloContext.getRequesterEntityID()));

        return initialRequest;
    }

    /**
     * Builds Logout Response.
     *
     * @param initialRequest
     * @return
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException
     */
    protected LogoutResponse buildLogoutResponse(
            BaseSAML2ProfileRequestContext<?, ?, ?> initialRequest,
            Status status)
            throws ProfileException {

        DateTime issueInstant = new DateTime();

        LogoutResponse logoutResponse = responseBuilder.buildObject();
        logoutResponse.setIssueInstant(issueInstant);
        populateStatusResponse(initialRequest, logoutResponse);
        logoutResponse.setStatus(status);

        return logoutResponse;
    }

    /**
     * Decodes an incoming request and populates a created request context with the resultant information.
     *
     * @param inTransport inbound message transport
     * @param outTransport outbound message transport *
     * @param initialRequest request context to which decoded information should be added
     *
     * @throws edu.internet2.middleware.shibboleth.common.profile.ProfileException throw if there is a problem decoding the request
     */
    protected void decodeRequest(InitialLogoutRequestContext initialRequest,
            HTTPInTransport inTransport, HTTPOutTransport outTransport)
            throws ProfileException {
        log.debug("Decoding message with decoder binding '{}'", getInboundBinding());

        initialRequest.setCommunicationProfileId(getProfileId());

        MetadataProvider metadataProvider = getMetadataProvider();
        initialRequest.setMetadataProvider(metadataProvider);

        initialRequest.setInboundMessageTransport(inTransport);
        initialRequest.setInboundSAMLProtocol(SAMLConstants.SAML20P_NS);
        initialRequest.setSecurityPolicyResolver(getSecurityPolicyResolver());
        initialRequest.setPeerEntityRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);

        initialRequest.setOutboundMessageTransport(outTransport);
        initialRequest.setOutboundSAMLProtocol(SAMLConstants.SAML20P_NS);

        try {
            SAMLMessageDecoder decoder =
                    getInboundMessageDecoder(null);
            initialRequest.setMessageDecoder(decoder);
            decoder.decode(initialRequest);
            log.debug("Decoded request from relying party '{}'", initialRequest.getInboundMessage());

            //TODO
            /*if (!(initialRequest.getInboundSAMLMessage() instanceof LogoutRequest)) {
            log.warn("Incoming message was not a LogoutRequest, it was a {}", initialRequest.getInboundSAMLMessage().getClass().getName());
            initialRequest.setFailureStatus(buildStatus(StatusCode.REQUESTER_URI, null,
            "Invalid SAML LogoutRequest message."));
            throw new ProfileException("Invalid SAML LogoutRequest message.");
            }*/

        } catch (MessageDecodingException e) {
            String msg = "Error decoding logout request message";
            log.warn(msg, e);
            initialRequest.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null, msg));
            throw new ProfileException(msg);
        } catch (SecurityException e) {
            String msg = "Message did not meet security requirements";
            log.warn(msg, e);
            initialRequest.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, StatusCode.REQUEST_DENIED_URI, msg));
            throw new ProfileException(msg, e);
        } finally {
            // Set as much information as can be retrieved from the decoded message
            populateRequestContext(initialRequest);
        }
    }

    public class InitialLogoutRequestContext
            extends BaseSAML2ProfileRequestContext<LogoutRequest, LogoutResponse, LogoutRequestConfiguration> {
    }

    public class LogoutRequestContext
            extends BasicSAMLMessageContext<LogoutResponse, LogoutRequest, NameIDImpl> {

        EntityEnclosingMethod postMethod;

        EntityEnclosingMethod getPostMethod() {
            return postMethod;
        }

        void setPostMethod(EntityEnclosingMethod postMethod) {
            this.postMethod = postMethod;
        }

        boolean execute(HttpConnection conn) throws HttpException,
                IOException {
            return postMethod.execute(new HttpState(), conn) == HttpStatus.SC_OK;
        }

        String getHttpStatus() {
            return postMethod.getStatusCode() + " " + postMethod.getStatusText();
        }

        void releaseConnection() {
            if (postMethod != null) {
                postMethod.releaseConnection();
            }
        }
    }
}
