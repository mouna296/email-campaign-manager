package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.MailStat;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MailStatMapper implements ResultSetMapper<MailStat> {
    public MailStat map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        MailStat mailStat = new MailStat();
        mailStat.setDate(r.getDate("date"));
        mailStat.setRecipient(r.getString("recipient"));
        mailStat.setSent_mail_count(r.getInt("sent_mail_count"));
        return mailStat;
    }
}
