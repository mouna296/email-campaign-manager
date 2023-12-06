package com.untitled.ecm.constants;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Roles {
    public final static String SUPER_USER = "super_user";
    public final static String CAMPAIGN_MANAGER = "campaign_manager";
    public final static String CAMPAIGN_SUPERVISOR = "campaign_supervisor";

    public static List<String> getAllRoles() {
        return new ArrayList<String>(Arrays.asList(SUPER_USER, CAMPAIGN_MANAGER, CAMPAIGN_SUPERVISOR));
    }
}
