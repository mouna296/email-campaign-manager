package com.untitled.ecm.testcommons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;

@Data
@EqualsAndHashCode(exclude = {"dakiyaSchedulerJobCampaignPreviousFireTime",
        "dakiyaSchedulerJobCampaignNextFireTime",
        "dakiyaSchedulerJobCampaignMayFireAgain"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class DakiyaCampaignJobDetail {
    private int dakiyaSchedulerJobCampaignID;
    private int dakiyaSchedulerJobCampaignMailID;
    private int dakiyaSchedulerJobCampaignVersion;
    private DateTime dakiyaSchedulerJobCampaignStartTime;
    private DateTime dakiyaSchedulerJobCampaignPreviousFireTime;
    private DateTime dakiyaSchedulerJobCampaignNextFireTime;
    private DateTime dakiyaSchedulerJobCampaignEndTime;
    private boolean dakiyaSchedulerJobCampaignMayFireAgain;
    private int noOfTriggers;
}
