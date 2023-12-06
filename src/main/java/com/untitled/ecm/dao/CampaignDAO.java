package com.untitled.ecm.dao;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.dao.mappers.ArchivedCampaignMapper;
import com.untitled.ecm.dao.mappers.CampaignMapper;
import com.untitled.ecm.dao.mappers.DeliveryBreakdownMapper;
import com.untitled.ecm.dao.mappers.SuccessFailureBreakdownMapper;
import com.untitled.ecm.dtos.ArchivedCampaign;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.http.DeliveryBreakdown;
import com.untitled.ecm.dtos.http.SuccessFailureBreakdown;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.sql.Timestamp;
import java.util.List;

@RegisterMapper({CampaignMapper.class, ArchivedCampaignMapper.class, DeliveryBreakdownMapper.class, SuccessFailureBreakdownMapper.class})
public interface CampaignDAO {
    @SqlQuery("SELECT * FROM dakiya_campaigns WHERE id = :id")
    Campaign findByID(@Bind("id") int id);

    @SqlQuery("SELECT * FROM dakiya_campaigns")
    List<Campaign> findAll();

    @SqlQuery("SELECT * FROM dakiya_campaigns_archives where id not in (select id from dakiya_campaigns)")
    List<ArchivedCampaign> findArchivedCampaigns();

    @SqlQuery("SELECT * FROM dakiya_campaigns WHERE campaign_creator = :email")
    List<Campaign> findByEmail(@Bind("email") String email);

    @SqlQuery("SELECT * FROM dakiya_campaigns WHERE id = :id AND campaign_creator = :email")
    Campaign findByIdAndCreator(@Bind("id") int id, @Bind("email") String email);

    @SqlQuery("SELECT * FROM dakiya_campaigns WHERE id = :id AND last_modified_by = :email")
    Campaign findByIdAndLastModifier(@Bind("id") int id, @Bind("email") String email);

    @SqlQuery("SELECT * FROM dakiya_campaigns WHERE id = :id")
    Campaign findById(@Bind("id") int id);

    @SqlQuery("INSERT INTO dakiya_campaigns" +
            "(sql, version, about, title, created_on, state, mail_dbid, " +
            "campaign_creator, last_modified_time, last_modified_by, " +
            "start_at, end_at, repeat_period, repeat_threshold, mail_limit, " +
            "sendgrid_domain, category, dakiya_instance_type, chunk_count, mails_per_chunk, delay_per_chunk_in_minutes) " +
            "Values (:sql, :version, :about, :title, :created_on, :state, :mail_dbid, " +
            ":campaign_creator, :last_modified_time, :last_modified_by, " +
            ":start_at, :end_at, :repeat_period, :repeat_threshold, :mail_limit, " +
            ":sendgrid_domain, :category, :dakiya_instance_type, :chunk_count, :mails_per_chunk, :delay_per_chunk_in_minutes) " +
            "RETURNING id;")
    int saveCampaign(@Bind("sql") String sql,
                     @Bind("version") int version,
                     @Bind("title") String title,
                     @Bind("about") String about,
                     @Bind("created_on") Timestamp createdOn,
                     @Bind("state") String state,
                     @Bind("mail_dbid") int maidDBID,
                     @Bind("campaign_creator") String campaignCreator,
                     @Bind("last_modified_time") Timestamp lastModifiedTime,
                     @Bind("last_modified_by") String lastModifiedBy,
                     @Bind("start_at") Timestamp startAt,
                     @Bind("end_at") Timestamp endAt,
                     @Bind("repeat_period") String repeatPeriod,
                     @Bind("repeat_threshold") int repeatThreshold,
                     @Bind("mail_limit") int mailLimit,
                     @Bind("sendgrid_domain") String sendgridDomain,
                     @Bind("category") String category,
                     @Bind("dakiya_instance_type") String dakiyaInstanceType,
                     @Bind("chunk_count") Integer chunkCount,
                     @Bind("mails_per_chunk") Integer mailsPerChunk,
                     @Bind("delay_per_chunk_in_minutes") Integer delayPerChunkInMinutes);

    @SqlUpdate("INSERT INTO dakiya_campaigns_archives" +
            "(id, sql, version, about, title, created_on, state, mail_dbid, " +
            "campaign_creator, last_modified_by, " +
            "start_at, end_at, repeat_period, repeat_threshold, mail_limit, sendgrid_domain, category, " +
            "approved_by, approved_at, archived_by, archived_at, dakiya_instance_type, " +
            "chunk_count, mails_per_chunk, delay_per_chunk_in_minutes) " +
            "Values (:id, :sql, :version, :about, :title, :created_on, :state, :mail_dbid, " +
            ":campaign_creator, :last_modified_by, " +
            ":start_at, :end_at, :repeat_period, :repeat_threshold, :mail_limit, :sendgrid_domain, :category, " +
            ":approved_by, :approved_at, :archived_by, :archived_at, :dakiya_instance_type, " +
            ":chunk_count, :mails_per_chunk, :delay_per_chunk_in_minutes)")
    void archiveCampaign(@BindBean ArchivedCampaign archivedCampaign);

    @SqlUpdate("UPDATE dakiya_campaigns " +
            "SET (sql, version, about, title, created_on, state, mail_dbid, " +
            "campaign_creator, last_modified_time, last_modified_by, " +
            "start_at, end_at, repeat_period, repeat_threshold, mail_limit, sendgrid_domain, category, " +
            "chunk_count, mails_per_chunk, delay_per_chunk_in_minutes) = " +
            "(:sql, :version, :about, :title, :created_on, :state, :mail_dbid, " +
            ":campaign_creator, :last_modified_time, :last_modified_by, " +
            ":start_at, :end_at, :repeat_period, :repeat_threshold, :mail_limit, :sendgrid_domain, :category, " +
            ":chunk_count, :mails_per_chunk, :delay_per_chunk_in_minutes) " +
            "WHERE id = :id;")
    int updateCampaign(@Bind("id") int id,
                       @Bind("sql") String sql,
                       @Bind("version") int version,
                       @Bind("title") String title,
                       @Bind("about") String about,
                       @Bind("created_on") Timestamp createdOn,
                       @Bind("state") String state,
                       @Bind("mail_dbid") int maidDBID,
                       @Bind("campaign_creator") String campaignCreator,
                       @Bind("last_modified_time") Timestamp lastModifiedTime,
                       @Bind("last_modified_by") String lastModifiedBy,
                       @Bind("start_at") Timestamp startAt,
                       @Bind("end_at") Timestamp endAt,
                       @Bind("repeat_period") String repeatPeriod,
                       @Bind("repeat_threshold") int repeatThreshold,
                       @Bind("mail_limit") int mailLimit,
                       @Bind("sendgrid_domain") String sendgridDomain,
                       @Bind("category") String category,
                       @Bind("dakiya_instance_type") String dakiyaInstanceType,
                       @Bind("chunk_count") Integer chunkCount,
                       @Bind("mails_per_chunk") Integer mailsPerChunk,
                       @Bind("delay_per_chunk_in_minutes") Integer delayPerChunkInMinutes);

    @SqlUpdate("UPDATE dakiya_campaigns " +
            "SET (state, last_modified_time, last_modified_by) = (:state, :last_modified_time, :last_modified_by) " +
            "WHERE id = :id;")
    int updateStateByCampaignId(@Bind("id") int id,
                                @Bind("state") String state,
                                @Bind("last_modified_time") Timestamp lastModifiedTime,
                                @Bind("last_modified_by") String lastModifiedBy);

    @SqlUpdate("UPDATE dakiya_campaigns " +
            "SET (state, approved_by, approved_at) = (\'" + CampaignStates.APPROVED + "\', :approved_by, :approved_at) " +
            "WHERE id = :id;")
    int changeCampaignStateToApproved(@Bind("id") int id,
                                      @Bind("approved_by") String approvedBy,
                                      @Bind("approved_at") Timestamp approvedAt);

    @SqlUpdate("DELETE FROM dakiya_campaigns WHERE id = :id;")
    int deleteCampaignById(@Bind("id") int id);


    @SqlQuery("SELECT id from dakiya_campaigns where category = :category")
    List<Integer> getCampaignsByCategory(@Bind("category") String category);


    @SqlQuery("SELECT\n" +
            "  SUM(CASE WHEN remark = 'open' THEN 1 ELSE 0 END) AS open_count,\n" +
            "  SUM(CASE WHEN remark = 'delivered' THEN 1 ELSE 0 END) AS delivered_count,\n" +
            "  SUM(CASE WHEN remark = 'scheduled_run' THEN 1 ELSE 0 END) AS scheduled_run_count,\n" +
            "  SUM(CASE WHEN remark NOT IN ('open', 'delivered', 'scheduled_run') THEN 1 ELSE 0 END) AS other_count\n" +
            "FROM\n" +
            "  dakiya_mails_events\n" +
            "WHERE\n" +
            "  campaign_id = :campaignId;")
    DeliveryBreakdown getCampaignDeliveryBreakdown(@Bind("campaignId") int campaignId);


    @SqlQuery("SELECT\n" +
            "  SUM(CASE WHEN mail_sent = 'true' THEN 1 ELSE 0 END) AS success_count,\n" +
            "  SUM(CASE WHEN mail_sent = 'false' OR mail_sent is NULL THEN 1 ELSE 0 END) AS failure_count\n" +
            "FROM\n" +
            "  dakiya_mails_events\n" +
            "WHERE\n" +
            "  campaign_id = :campaignId;")
    SuccessFailureBreakdown getSuccessFailureBreakdown(@Bind("campaignId") int campaignId);

    void close();

}
