package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.Campaign;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CampaignMapper implements ResultSetMapper<Campaign> {

    public Campaign map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Campaign campaign = new Campaign();
        campaign.setId(r.getInt("id"));
        campaign.setSql(r.getString("sql"));
        campaign.setVersion(r.getInt("version"));
        campaign.setAbout(r.getString("about"));
        campaign.setTitle(r.getString("title"));
        campaign.setCreatedOn(r.getTimestamp("created_on"));
        campaign.setCampaignCreator(r.getString("campaign_creator"));
        campaign.setStartAt(r.getTimestamp("start_at"));
        campaign.setEndAt(r.getTimestamp("end_at"));
        campaign.setRepeatPeriod(r.getString("repeat_period"));
        campaign.setRepeatThreshold(r.getInt("repeat_threshold"));
        campaign.setMailIDinDB(r.getInt("mail_dbid"));
        campaign.setLastModifiedTime(r.getTimestamp("last_modified_time"));
        campaign.setLastModifiedBy(r.getString("last_modified_by"));
        campaign.setState(r.getString("state"));
        campaign.setMailLimit(r.getInt("mail_limit"));
        campaign.setSendgridDomain(r.getString("sendgrid_domain"));
        campaign.setCategory(r.getString("category"));
        campaign.setApprovedBy(r.getString("approved_by"));
        campaign.setApprovedAt(r.getTimestamp("approved_at"));
        campaign.setDakiyaInstanceType(r.getString("dakiya_instance_type"));
        campaign.setChunkCount(r.getInt("chunk_count"));
        campaign.setMailsPerChunk(r.getInt("mails_per_chunk"));
        campaign.setDelayPerChunkInMinutes(r.getInt("delay_per_chunk_in_minutes"));
        campaign.setScheduled(r.getInt("scheduled"));
        return campaign;
    }
}
