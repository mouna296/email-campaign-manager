package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.MailEvent;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MailEventMapper implements ResultSetMapper<MailEvent> {
    @Override
    public MailEvent map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        MailEvent mailEvent = new MailEvent();
        mailEvent.setCampaign_id(r.getInt("campaign_id"));
        mailEvent.setCampaign_version(r.getInt("campaign_version"));
        mailEvent.setTrigger_time(r.getTimestamp("trigger_time"));
        mailEvent.setRecipient(r.getString("recipient"));
        mailEvent.setMail_sent(r.getBoolean("mail_sent"));
        mailEvent.setRemark(r.getString("remark"));
        return mailEvent;
    }
}
