package com.untitled.ecm.dao;

import com.untitled.ecm.dao.binders.BindDak;
import com.untitled.ecm.dao.mappers.DakMapper;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakV2;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(DakMapper.class)
public interface DakDAO {

    @SqlQuery("SELECT * FROM dakiya_campaigns_mails")
    List<Dak> findAll();

    @SqlQuery("SELECT * FROM dakiya_campaigns_mails WHERE id = :id")
    Dak findDakById(@Bind("id") int id);

    // pg8.2
    @SqlQuery("INSERT INTO dakiya_campaigns_mails " +
            "(subject, content, content_type, mail_type, from_name, from_email, reply_to_name, reply_to_email, creator) " +
            "VALUES(:subject, :content, :content_type, :mail_type, :from_name, :from_email, :reply_to_name, :reply_to_email, :creator)" +
            "RETURNING id;")
    int saveMailInDBV2(@BindDak DakV2 dakV2);

    void close();
}
