package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.ArchivedCampaign;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ArchivedCampaignMapper implements ResultSetMapper<ArchivedCampaign> {

    public ArchivedCampaign map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        ArchivedCampaign campaign = new ArchivedCampaign();
        campaign.setId(r.getInt("id"));
        campaign.setSql(r.getString("sql"));
        campaign.setVersion(r.getInt("version"));
        campaign.setAbout(r.getString("about"));
        campaign.setTitle(r.getString("title"));
        campaign.setCreated_on(r.getTimestamp("created_on"));
        campaign.setCampaign_creator(r.getString("campaign_creator"));
        campaign.setStart_at(r.getTimestamp("start_at"));
        campaign.setEnd_at(r.getTimestamp("end_at"));
        campaign.setRepeat_period(r.getString("repeat_period"));
        campaign.setRepeat_threshold(r.getInt("repeat_threshold"));
        campaign.setMail_dbid(r.getInt("mail_dbid"));
        campaign.setLast_modified_time(r.getTimestamp("last_modified_time"));
        campaign.setLast_modified_by(r.getString("last_modified_by"));
        campaign.setState(r.getString("state"));
        campaign.setMail_limit(r.getInt("mail_limit"));
        campaign.setSendgrid_domain(r.getString("sendgrid_domain"));
        campaign.setCategory(r.getString("category"));
        campaign.setApproved_by(r.getString("approved_by"));
        campaign.setApproved_at(r.getTimestamp("approved_at"));
        campaign.setArchived_by(r.getString("archived_by"));
        campaign.setArchived_at(r.getTimestamp("archived_at"));
        campaign.setDakiya_instance_type(r.getString("dakiya_instance_type"));
        campaign.setChunk_count(r.getInt("chunk_count"));
        campaign.setMails_per_chunk(r.getInt("mails_per_chunk"));
        campaign.setDelay_per_chunk_in_minutes(r.getInt("delay_per_chunk_in_minutes"));
        return campaign;
    }

}
