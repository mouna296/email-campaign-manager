package com.untitled.ecm.dao;

import com.untitled.ecm.dao.mappers.MailStatMapper;
import com.untitled.ecm.dtos.MailStat;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;

@RegisterMapper(MailStatMapper.class)
public interface MailStatDAO {
    @SqlQuery("SELECT * FROM dakiya_mails_stats WHERE date = :date")
    List<MailStat> findStatByDate(@Bind("date") Date date);

    // pg9.5
    @SqlBatch("INSERT INTO dakiya_mails_stats (date, recipient, sent_mail_count) " +
            "VALUES (:date, :recipient, :sent_mail_count) " +
            "ON CONFLICT (date, recipient) Do UPDATE SET sent_mail_count=EXCLUDED.sent_mail_count ")
    int[] updateStats(@BindBean Iterator<MailStat> mailStatIterator);

    void close();
}
