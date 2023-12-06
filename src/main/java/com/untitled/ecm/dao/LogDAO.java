package com.untitled.ecm.dao;

import com.untitled.ecm.dao.mappers.MailEventMapper;
import com.untitled.ecm.dtos.CampaignEvent;
import com.untitled.ecm.dtos.MailEvent;
import lombok.extern.slf4j.Slf4j;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public interface LogDAO {

    Logger LOGGER = LoggerFactory.getLogger(LogDAO.class);
    @SqlBatch("INSERT INTO dakiya_mails_events (trigger_time, campaign_id, campaign_version, recipient, mail_sent, remark) " +
            "VALUES (:trigger_time, :campaign_id, :campaign_version, :recipient, :mail_sent, :remark) ")
    int[] saveMailEvents(@BindBean Iterator<MailEvent> mailEventIterator);

    @SqlUpdate("INSERT INTO dakiya_campaigns_events" +
            "(campaign_id, trigger_time, version, mails_filtered, expected_mails, mails_sent, remark," +
            "campaign_last_modified_by, dakiya_instance_type, chunk_number, total_chunk_count) " +
            "VALUES (:campaign_id, :trigger_time, :version, :mails_filtered, :expected_mails, :mails_sent, :remark," +
            ":campaign_last_modified_by, :dakiya_instance_type, :chunk_number, :total_chunk_count) " +
            "ON CONFLICT (campaign_id, trigger_time, version) Do UPDATE SET mails_sent=EXCLUDED.mails_sent, remark=EXCLUDED.remark ")
    int saveCampaignEvent(@BindBean CampaignEvent campaignEvent);

    @RegisterMapper(MailEventMapper.class)
    @SqlQuery("Select * from dakiya_mails_events " +
            "WHERE campaign_id = :campaignID AND campaign_version = :campaignVersion")
    List<MailEvent> getMailEventByCampaignIDandVersion(@Bind("campaignID") int campaignID, @Bind("campaignVersion") int campaignVersion);

    @RegisterMapper(MailEventMapper.class)
    @SqlQuery("Select * from dakiya_mails_events " +
            "WHERE campaign_id = :campaignID")
    List<MailEvent> getMailEventByCampaignID(@Bind("campaignID") int campaignID);


    @RegisterMapper(MailEventMapper.class)
    @SqlQuery("SELECT * from dakiya_mails_events where campaign_id = :campaignID AND email = :email")
    List<MailEvent> getMailEventByCampaignIDAndEmail(@Bind("campaignID") int campaignID, @Bind("email") String email);


    @SqlUpdate("UPDATE dakiya_mails_events set remark = :remark where campaign_id = :campaignId AND recipient = :email")
    int updateMailEvents(@Bind("remark") String remark, @Bind("campaignId") int campaignId, @Bind("email") String email);

    default int logAndUpdateMailEvents(String remark, int campaignId, String email) {
        LOGGER.info("Updating mail events with remark: {}, campaignId: {}, email: {}", remark, campaignId, email);
        return updateMailEvents(remark, campaignId, email);
    }

    void close();
}
