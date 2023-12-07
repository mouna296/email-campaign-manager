package com.untitled.ecm.core;

import com.untitled.ecm.constants.Roles;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class DakiyaUser implements Principal {
    private final String email;
    private final String role;

    public DakiyaUser(final String email, final String role) {
        this.email = email;
        this.role = role;
    }

    @Override
    public String getName() {
        return this.role;
    }

    public String getEmail(){
        return this.email;
    }

    public List<String> getRoles(){
        switch(role){
            case Roles.SUPER_USER:
                return Roles.getAllRoles();
            case Roles.CAMPAIGN_MANAGER:
                return Arrays.asList(Roles.CAMPAIGN_MANAGER, Roles.CAMPAIGN_SUPERVISOR);
            case Roles.CAMPAIGN_SUPERVISOR:
                return Collections.singletonList(Roles.CAMPAIGN_SUPERVISOR);
            default:
                return new ArrayList<>();
        }
    }

}
