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
package edu.internet2.middleware.shibboleth.idp.slo;

import edu.internet2.middleware.shibboleth.idp.authn.LoginContext;
import edu.internet2.middleware.shibboleth.idp.authn.LoginContextEntry;
import edu.internet2.middleware.shibboleth.idp.profile.saml2.SLOProfileHandler;
import edu.internet2.middleware.shibboleth.idp.slo.SingleLogoutContext.LogoutInformation;
import edu.internet2.middleware.shibboleth.idp.util.HttpServletHelper;
import org.opensaml.util.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * @author Adam Lantos  NIIF / HUNGARNET
 */
public class SLOServlet extends HttpServlet {

    private static final long serialVersionUID = -3562061733288921508L;
    // TODO remove once HttpServletHelper does redirects
    private static ServletContext context;
    /**
     * Storage service used to store {@link LoginContext}s while authentication is in progress.
     */
    private static StorageService<String, LoginContextEntry> storageService;
    static final Logger log = LoggerFactory.getLogger(SLOServlet.class);

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        storageService =
                (StorageService<String, LoginContextEntry>) HttpServletHelper.getStorageService(config.getServletContext());
        context = config.getServletContext();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            SingleLogoutContext sloContext =
                    SingleLogoutContextStorageHelper.getSingleLogoutContext(req);
            if (sloContext == null) {
                //remove stale cookie if exists
                SingleLogoutContextStorageHelper.removeSingleLogoutContextCookie(req, resp);
                resp.sendError(404, "Single Logout servlet can not be called directly");
                return;
            }
            resp.setHeader("Cache-Control", "no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");

            if (req.getParameter("status") != null) { //status query, response is JSON
                log.debug("checkStatus...");
                sloContext.checkTimeout();
                StringBuilder html = new StringBuilder();
                html.append("[");
                Iterator<SingleLogoutContext.LogoutInformation> it =
                        sloContext.getServiceInformation().values().iterator();
                log.debug("services found: " + sloContext.getServiceInformation().values().size());
                while (it.hasNext()) {
                    LogoutInformation service = it.next();
                    html.append("{\"entityID\":\"");
                    html.append(service.getEntityID());
                    html.append("\",\"logoutStatus\":\"");
                    html.append(service.getLogoutStatus().toString());
                    html.append("\"}");
                    log.debug("entityID:" + service.getEntityID() + ", logoutStatus:" + service.getLogoutStatus().toString());
                    if (it.hasNext()) {
                        html.append(",");
                    }
                }
                html.append("]");
                log.debug("Logout status json sending: " + html.toString());
                PrintWriter out = resp.getWriter();
                out.print(html.toString());
            } else if (req.getParameter("action") != null) { //forward to handler
                req.getRequestDispatcher(sloContext.getProfileHandlerURL()).forward(req, resp);
            } else if (req.getParameter("finish") != null) { //forward to handler
                SingleLogoutContextStorageHelper.unbindSingleLogoutContext(storageService, context, req, resp);
                req.getRequestDispatcher(sloContext.getProfileHandlerURL()).forward(req, resp);
            } else if (req.getParameter("logout") != null ||
                    req.getAttribute(SLOProfileHandler.SKIP_LOGOUT_QUESTION_ATTR) != null) {


                //respond with SLO Controller
                sloContext.checkTimeout();

                String sloControllerPage = "/sloController.jsp";
                req.getRequestDispatcher(sloControllerPage).forward(req, resp);        //sloController.jsp
                return;
            } else { //respond with confirmation dialog
                req.getRequestDispatcher("/sloQuestion.jsp").forward(req, resp);
                return;
            }
        } catch (Throwable e) {
            log.error("Error while processing logout: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
