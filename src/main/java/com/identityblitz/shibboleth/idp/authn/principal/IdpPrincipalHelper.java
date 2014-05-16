package com.identityblitz.shibboleth.idp.authn.principal;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * User: agumerov
 * Date: 11.12.12
 * Time: 15:16
 */
public class IdpPrincipalHelper {
    /**
     * Class logger.
     */
    private final static Logger log = LoggerFactory.getLogger(IdpPrincipalHelper.class);

    public static IdpPrincipal getPrincipal(Subject subject) {
        if (subject == null) {
            log.error("Session subject can't be null");
            return null;
        }

        if (subject.getPrincipals() == null || subject.getPrincipals().isEmpty()) {
            log.error("Principals not found for session subject");
            return null;
        }
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof IdpPrincipal) {
                return (IdpPrincipal) principal;
            }
        }
        log.error("Idp principal not found for session subject");
        return null;
    }

}
