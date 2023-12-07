package com.untitled.ecm.constants;

import java.util.List;

public final class CampaignStates {
    public static final String ARCHIVED = "archived";
    public static final String APPROVED = "approved";
    public static final String NOT_APPROVED = "not_approved";

    public static String getDefaultStateByRole(List<String> userRoles) {
        if (userRoles.contains(Roles.SUPER_USER) || userRoles.contains(Roles.CAMPAIGN_MANAGER)) {
            return CampaignStates.APPROVED;
        } else {
            return CampaignStates.NOT_APPROVED;
        }
    }
}
