package com.untitled.ecm.filter;

import com.untitled.ecm.auth.RBACSecurityContext;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;

@Priority(Priorities.AUTHORIZATION)
@Slf4j
public class RBACFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method resourceMethod = resourceInfo.getResourceMethod();
        String email = requestContext.getHeaderString(DakiyaStrings.USER_EMAIL_HEADER);
        String userRole = requestContext.getHeaderString(DakiyaStrings.USER_ROLE_HEADER);

        if (!resourceMethod.isAnnotationPresent(RolesAllowed.class)) {
            return;
        }

        if (Objects.isNull(userRole) || Objects.isNull(email)) {
            setFailureResponse(requestContext);
            return;
        }

        if (userRole.equalsIgnoreCase(Roles.SUPER_USER)) {
            requestContext.setSecurityContext(new RBACSecurityContext(email, userRole, requestContext.getSecurityContext()));
            return;
        }

        RolesAllowed rolesAllowed = resourceMethod.getAnnotation(RolesAllowed.class);
        String[] roles = rolesAllowed.value();

        for (String role : roles) {
            if (role.equalsIgnoreCase(userRole)) {
                requestContext.setSecurityContext(new RBACSecurityContext(email, userRole, requestContext.getSecurityContext()));
                return;
            }
        }
        setFailureResponse(requestContext);
    }

    private void setFailureResponse(ContainerRequestContext containerRequestContext) {
        Response response = Response
                .status(Response.Status.UNAUTHORIZED) // Set appropriate status code
                .entity("Access denied: User does not have the required role") // Set your error message
                .build();

        containerRequestContext.abortWith(response);
    }
}
