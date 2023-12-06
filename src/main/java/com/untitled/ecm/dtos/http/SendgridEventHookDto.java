package com.untitled.ecm.dtos.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendgridEventHookDto {

    @JsonProperty("DakiyaInstanceType")
    private String dakiyaInstanceType;

    @JsonProperty("category")
    private List<String> category;

    @JsonProperty("dakiyaSchedulerJobCampaignCategory")
    private String dakiyaSchedulerJobCampaignCategory;

    @JsonProperty("dakiyaSchedulerJobCampaignChunkNumber")
    private String dakiyaSchedulerJobCampaignChunkNumber;

    @JsonProperty("dakiyaSchedulerJobCampaignID")
    private String dakiyaSchedulerJobCampaignID;

    @JsonProperty("dakiyaSchedulerJobCampaignMailSubject")
    private String dakiyaSchedulerJobCampaignMailSubject;

    @JsonProperty("dakiyaSchedulerJobCampaignSendgridDomain")
    private String dakiyaSchedulerJobCampaignSendgridDomain;

    @JsonProperty("dakiyaSchedulerJobCampaignTotalChunkCount")
    private String dakiyaSchedulerJobCampaignTotalChunkCount;

    @JsonProperty("dakiyaSchedulerJobCampaignTriggerTime")
    private long dakiyaSchedulerJobCampaignTriggerTime;

    @JsonProperty("dakiyaSchedulerJobCampaignVersion")
    private String dakiyaSchedulerJobCampaignVersion;

    @JsonProperty("email")
    private String email;

    @JsonProperty("event")
    private String event;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("sg_content_type")
    private String sgContentType;

    @JsonProperty("sg_event_id")
    private String sgEventId;

    @JsonProperty("sg_machine_open")
    private boolean sgMachineOpen;

    @JsonProperty("sg_message_id")
    private String sgMessageId;

    @JsonProperty("useragent")
    private String useragent;
}
