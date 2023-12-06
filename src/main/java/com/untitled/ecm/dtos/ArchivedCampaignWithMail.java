package com.untitled.ecm.dtos;

import com.untitled.ecm.core.DakiyaUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArchivedCampaignWithMail extends ArchivedCampaign {

    private Dak mail;

    public ArchivedCampaignWithMail(final Campaign campaign, final DakiyaUser dakiyaUser) {
        super(campaign, dakiyaUser);
        this.mail = campaign.getMail();
    }

    public ArchivedCampaignWithMail(final ArchivedCampaign archivedCampaign, final Dak mail) {
        super(archivedCampaign);
        this.mail = mail;
    }
}
