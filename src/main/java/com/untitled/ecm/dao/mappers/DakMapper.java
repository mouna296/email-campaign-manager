package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DakMapper implements ResultSetMapper<Dak> {

    public Dak map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        DakEmail from = new DakEmail();
        from.setName(r.getString("from_name"));
        from.setEmail(r.getString("from_email"));

        DakEmail replyTo = new DakEmail();
        replyTo.setName(r.getString("reply_to_name"));
        replyTo.setEmail(r.getString("reply_to_email"));

        return Dak.builder()
                .id(r.getInt("id"))
                .subject(r.getString("subject"))
                .content(r.getString("content"))
                .contentType(r.getString("content_type"))
                .creator(r.getString("creator"))
                .from(from).replyTo(replyTo).mailType(r.getString("mail_type"))
                .build();
    }
}
