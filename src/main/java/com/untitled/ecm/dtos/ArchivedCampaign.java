package com.untitled.ecm.dtos;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class ArchivedCampaign {

    private int id;
    private String sql;
    private int version;
    private String about;
    private String title;
    private Timestamp created_on;
    private String campaign_creator;
    private Timestamp start_at;
    private Timestamp end_at;
    private String repeat_period;
    private int repeat_threshold;
    private Timestamp last_modified_time;
    private String last_modified_by;
    private int mail_dbid;
    private String state;
    private int mail_limit;
    private String sendgrid_domain;
    private String category;
    private String approved_by;
    private Timestamp approved_at;
    private String archived_by;
    private Timestamp archived_at;
    private String dakiya_instance_type;
    private Integer chunk_count;
    private Integer mails_per_chunk;
    private Integer delay_per_chunk_in_minutes;

    public ArchivedCampaign(final Campaign campaign, final DakiyaUser dakiyaUser) {
        this.id = campaign.getId();
        this.sql = campaign.getSql();
        this.version = campaign.getVersion();
        this.about = campaign.getAbout();
        this.title = campaign.getTitle();
        this.created_on = new Timestamp(DateTime.parse(campaign.getCreatedOn()).getMillis());
        this.campaign_creator = campaign.getCampaignCreator();
        this.start_at = new Timestamp(DateTime.parse(campaign.getStartAt()).getMillis());
        this.end_at = new Timestamp(DateTime.parse(campaign.getEndAt()).getMillis());
        this.repeat_period = campaign.getRepeatPeriod();
        this.repeat_threshold = campaign.getRepeatThreshold();
        this.last_modified_by = campaign.getLastModifiedBy();
        this.last_modified_time = new Timestamp(DateTime.parse(campaign.getLastModifiedTime()).getMillis());
        this.mail_dbid = campaign.getMailIDinDB();
        this.state = CampaignStates.ARCHIVED;
        this.mail_limit = campaign.getMailLimit();
        this.sendgrid_domain = campaign.getSendgridDomain();
        this.category = campaign.getCategory();
        this.approved_by = campaign.getApprovedBy();
        this.approved_at = campaign.getApprovedAt() == null ? null : new Timestamp(DateTime.parse(campaign.getLastModifiedTime()).getMillis());
        this.archived_by = dakiyaUser.getEmail();
        this.archived_at = new Timestamp(DateTime.now().withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA)).getMillis());
        this.dakiya_instance_type = campaign.getDakiyaInstanceType();
        this.chunk_count = campaign.getChunkCount();
        this.mails_per_chunk = campaign.getMailsPerChunk();
        this.delay_per_chunk_in_minutes = campaign.getDelayPerChunkInMinutes();
    }

    public ArchivedCampaign(final ArchivedCampaign archivedCampaign) {
        this.id = archivedCampaign.getId();
        this.sql = archivedCampaign.getSql();
        this.version = archivedCampaign.getVersion();
        this.about = archivedCampaign.getAbout();
        this.title = archivedCampaign.getTitle();
        this.created_on = archivedCampaign.getCreated_on();
        this.campaign_creator = archivedCampaign.getCampaign_creator();
        this.start_at = archivedCampaign.getStart_at();
        this.end_at = archivedCampaign.getEnd_at();
        this.repeat_period = archivedCampaign.getRepeat_period();
        this.repeat_threshold = archivedCampaign.getRepeat_threshold();
        this.last_modified_by = archivedCampaign.getLast_modified_by();
        this.last_modified_time = archivedCampaign.getLast_modified_time();
        this.mail_dbid = archivedCampaign.getMail_dbid();
        this.state = archivedCampaign.getState();
        this.mail_limit = archivedCampaign.getMail_limit();
        this.sendgrid_domain = archivedCampaign.getSendgrid_domain();
        this.category = archivedCampaign.getCategory();
        this.approved_at = archivedCampaign.getApproved_at();
        this.approved_by = archivedCampaign.getApproved_by();
        this.archived_at = archivedCampaign.getArchived_at();
        this.archived_by = archivedCampaign.getArchived_by();
        this.dakiya_instance_type = archivedCampaign.getDakiya_instance_type();
        this.chunk_count = archivedCampaign.getChunk_count();
        this.mails_per_chunk = archivedCampaign.getMails_per_chunk();
        this.delay_per_chunk_in_minutes = archivedCampaign.getDelay_per_chunk_in_minutes();
    }
}
