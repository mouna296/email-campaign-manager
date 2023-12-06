package com.untitled.ecm.auth;

import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaUser;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@Slf4j
public class RBACSecurityContext implements SecurityContext {
    private final SecurityContext requestSecurityContext;
    private final Principal principal;

    public RBACSecurityContext(String email, String role, SecurityContext requestSecurityContext) {
        this.requestSecurityContext = requestSecurityContext;
        this.principal = new DakiyaUser(email, role);
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (principal.getName().equalsIgnoreCase(Roles.SUPER_USER)) {
            return true;
        } else return principal.getName().equalsIgnoreCase(role);
    }

    @Override
    public boolean isSecure() {
        return this.requestSecurityContext.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return "TEst Scheme";
    }
}
