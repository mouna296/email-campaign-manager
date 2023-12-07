package com.untitled.ecm.services.scheduler.jobs;

import com.untitled.ecm.constants.*;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.core.MailLimiter;
import com.untitled.ecm.dao.*;
import com.untitled.ecm.dao.external.RedshiftDao;
import com.untitled.ecm.dtos.*;
import com.untitled.ecm.services.mail.Mailer;
import com.untitled.ecm.services.mail.MailerFactory;
import com.untitled.ecm.services.scheduler.SchedulerUtils;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.quartz.*;
import org.skife.jdbi.v2.DBI;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@DisallowConcurrentExecution
@Slf4j
public class RunCampaign implements Job {
    private DateTime triggerDateTime;
    private Timestamp triggerTimeStamp;
    private MailStatDAO mailStatDAO;
    private DakiyaSettingDAO dakiyaSettingDAO;
    private DakDAO dakDAO;
    private LogDAO logDAO;
    private CampaignDAO campaignDAO;
    private RedshiftDao redshiftDao;
    private MailLimiter mailLimiter;
    private ArrayList<MailEvent> mailEvents;
    private CampaignEvent currentCampaignEvent;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        triggerDateTime = DateTime.now(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA));
        triggerTimeStamp = new Timestamp(triggerDateTime.getMillis());
        mailEvents = new ArrayList<>();
        initializeDB();

        final JobKey jobKey = context.getJobDetail().getKey();
        final JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        if (!jobKey.getGroup().equals(DakiyaStrings.DAKIYA_SCHEDULER_CAMPAIGN_JOBS_GROUP)) {
            throw new JobExecutionException("only one job group allowed. and the one for this job does not match the allowed one");
        }

        final int campaignID = jobDataMap.getInt(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID);

        final Campaign campaign;
        final Dak dak;
        try {
            campaign = campaignDAO.findById(campaignID);
            assert campaign != null;
            dak = dakDAO.findDakById(campaign.getMailIDinDB());
            assert dak != null;
            campaign.setMail(dak);
        } catch (Exception e) {
            log.error("unable to fetch campaign from db", e);// todo sent mail to zurg oncall
            return;
        }

        Integer chunkNumber = SchedulerUtils.getChunkNumber(context.getTrigger().getKey());
        if (chunkNumber == null) {
            chunkNumber = 1;
        }

        try {
            executeJob(campaign, jobDataMap, chunkNumber);
        } catch (Exception e) {
            log.error("exception occurred while executing campaign", e);// todo send mail to campaign creator
            return;
        }

        final int nextChunkNumber = chunkNumber + 1;

        // chunk number start from 1 which is also default
        if (nextChunkNumber > campaign.getChunkCount()) {
            return;
        }

        try {
            SchedulerUtils.scheduleCampaignChunk(campaign, nextChunkNumber);
        } catch (Exception e) {
            log.error("unable to scheduler task for next chunk for campaign " + campaignID, e); // todo sent mail to zurg oncall
        }

    }

    private void executeJob(@NonNull final Campaign campaign,
                            @NonNull final JobDataMap jobDataMap,
                            @NonNull final Integer chunkNumber) throws JobExecutionException {

        assertJobDataIsCorrect(campaign, jobDataMap);

        final DakiyaRuntimeSettings dakiyaRuntimeSettings = new DakiyaRuntimeSettings(dakiyaSettingDAO);
        mailLimiter = new MailLimiter(mailStatDAO, logDAO, dakiyaRuntimeSettings, triggerDateTime, campaign);

        Optional<String> sendgridDomainAPIKey = dakiyaRuntimeSettings.getSendGridAPIKeyByDomain(campaign.getSendgridDomain());
        if (!sendgridDomainAPIKey.isPresent()) {
            throw new JobExecutionException("Could not find the key for sendgrid domain: " + campaign.getSendgridDomain());
        }

        final CampaignContext campaignContext = getCampaignContext(campaign,
                dakiyaRuntimeSettings.getEnvType(),
                sendgridDomainAPIKey.get(),
                jobDataMap,
                chunkNumber);

        currentCampaignEvent = generateCampaignEvent(campaign, campaignContext.getRecipients().size(), chunkNumber);
        if (!dakiyaRuntimeSettings.isSendingEmailAllowed()) {
            currentCampaignEvent.setRemark(MailEventRemarks.SENDING_EMAIL_NOT_ALLOWED);
            return;
        }

        sendMails(campaignContext);

    }


    private void initializeDB() {
        final DBI dakiyaDBI;
        final DBI metabaseDBI;
        try {
            dakiyaDBI = DakiyaDBFactory.getDakiyaDB();
            metabaseDBI = DakiyaDBFactory.getMetabaseDB();
        } catch (SQLException e) {
            log.error("unable to initialize db ", e);
            throw new RuntimeException("unable to initialize db " + e.getMessage());
        }
        assert dakiyaDBI != null;
        assert metabaseDBI != null;
        // db stuff
        mailStatDAO = dakiyaDBI.onDemand(MailStatDAO.class);
        dakiyaSettingDAO = dakiyaDBI.onDemand(DakiyaSettingDAO.class);
        dakDAO = dakiyaDBI.onDemand(DakDAO.class);
        logDAO = dakiyaDBI.onDemand(LogDAO.class);
        campaignDAO = dakiyaDBI.onDemand(CampaignDAO.class);
        redshiftDao = new RedshiftDao(metabaseDBI);
    }

    private void assertJobDataIsCorrect(@NonNull final Campaign campaign,
                                        @NonNull final JobDataMap jobDataMap) throws JobExecutionException {
        try {
            int version = jobDataMap.getInt(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION);
            if (version != campaign.getVersion()) {
                throw new JobExecutionException("campaign version mismatch");
            }
            int mailID = jobDataMap.getInt(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_ID);
            if (mailID != campaign.getMailIDinDB()) {
                throw new JobExecutionException("campaign mail id in db mismatch");
            }
        } catch (Exception e) {
            throw new JobExecutionException("could not execute job. Causing: " + e.getMessage());
        }
    }

    private CampaignContext getCampaignContext(@NonNull final Campaign campaign,
                                               @NonNull final EnvironmentType environmentType,
                                               @NonNull final String sendGridKey,
                                               @NonNull final JobDataMap jobDataMap,
                                               @NonNull final Integer chunkNumber) throws JobExecutionException {
        final Dak dak = campaign.getMail();
        final String mailType = dak.getMailType();
        List<String> recipients;
        final Map<String, Map<String, Object>> dynamicMailData;
        switch (mailType) {
            case MailType.STATIC:
                dynamicMailData = null;
                recipients = redshiftDao.getAllRecipients(generateSql(campaign, chunkNumber));
                break;
            case MailType.DYNAMIC:
                dynamicMailData = getDynamicMailData(campaign, generateSql(campaign, chunkNumber));
                recipients = new ArrayList<>(dynamicMailData.keySet());
                break;
            default:
                throw new JobExecutionException("unknown mail type:" + mailType);
        }

        if (recipients == null) {
            recipients = new ArrayList<>();
        }

        HashMap<String, String> customTrackingArgs = getCustomTrackingArgs(campaign, chunkNumber);

        Mailer mailer = MailerFactory.getMailer(environmentType, sendGridKey);

        final Mustache mustache = new DefaultMustacheFactory()
                .compile(new StringReader(dak.getContent()), "mail_content");

        return CampaignContext
                .builder()
                .mail(dak)
                .mailType(mailType)
                .dynamicMailData(dynamicMailData)
                .mustache(mustache)
                .recipients(recipients)
                .mailer(mailer)
                .campaign(campaign)
                .customTrackingArgs(customTrackingArgs)
                .build();

    }

    private String generateSql(@NonNull final Campaign campaign,
                               @NonNull Integer chunkNumber) {
        assert campaign.getChunkCount() >= 1;
        // chunkNumber start from one and default is also 1
        assert chunkNumber >= 1;
        if (campaign.getChunkCount() == 1) {
            return campaign.getSql();
        }

        final int offset = (chunkNumber - 1) * campaign.getMailsPerChunk();
        final int limit = campaign.getMailsPerChunk();
        String sql = campaign.getSql();
        sql = StringUtils.trim(sql);
        sql = StringUtils.replacePattern(sql, ";$", "");
        sql = "SELECT * from (" + sql + ") AS x OFFSET " + offset + " LIMIT " + limit + ";";
        return sql;
    }

    private CampaignEvent generateCampaignEvent(@NonNull final Campaign campaign, final int recipientFilteredCount,
                                                @NonNull final Integer chunkNumber) {
        final CampaignEvent currentCampaignEvent = new CampaignEvent();
        currentCampaignEvent.setCampaign_id(campaign.getId());
        currentCampaignEvent.setTrigger_time(triggerTimeStamp);
        currentCampaignEvent.setVersion(campaign.getVersion());
        currentCampaignEvent.setCampaign_last_modified_by(campaign.getLastModifiedBy());
        currentCampaignEvent.setDakiya_instance_type(campaign.getDakiyaInstanceType());
        // this will not work if recipient is an iterator(cursor) and not a list
        currentCampaignEvent.setMails_filtered(recipientFilteredCount);
        currentCampaignEvent.setExpected_mails(0);
        currentCampaignEvent.setMails_sent(0);
        currentCampaignEvent.setTotal_chunk_count(campaign.getChunkCount());
        currentCampaignEvent.setChunk_number(chunkNumber);
        return currentCampaignEvent;
    }

    private HashMap<String, String> getCustomTrackingArgs(@NonNull final Campaign campaign, @NonNull final Integer chunkNumber) {
        final HashMap<String, String> customTrackingArgs = new HashMap<>();
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID, Integer.toString(campaign.getId()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION,
                Integer.toString(campaign.getVersion()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_TRIGGER_TIME,
                Long.toString(triggerDateTime.getMillis()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_SENDGRID_DOMAIN,
                campaign.getSendgridDomain());
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_SUBJECT,
                StringUtils.truncate(campaign.getMail().getSubject(), 30));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY, campaign.getCategory());
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_TOTAL_CHUNK_COUNT,
                Integer.toString(campaign.getChunkCount()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CHUNK_NUMBER, Integer.toString(chunkNumber));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_INSTANCE_TYPE, campaign.getDakiyaInstanceType());
        return customTrackingArgs;
    }

    private void sendMails(@NonNull final CampaignContext campaignContext) throws JobExecutionException {
        // performance knob, change this depending on how much desperate are you feeling :)
        // this param decides how frequently stats and events will be updated in db,
        // or in other words how many will be held in memory before flushing to db
        final int STATS_THRESHOLD = 100;

        if (campaignContext.getMailType().equals(MailType.STATIC)) {
            campaignContext.initializeStaticMail();
        }

        int statsCounter = 0;
        for (final String recipient : campaignContext.getRecipients()) {
            // An email of more than 100 length is likely to be a bad email
            if (StringUtils.isEmpty(recipient) || StringUtils.isWhitespace(recipient) || recipient.length() >= 100) {
                log.warn("found a invalid recipient email, ignoring: " + recipient);
                continue;
            }

            if (campaignContext.getMailType().equals(MailType.DYNAMIC)) {
                campaignContext.initializeDynamicMail(campaignContext.getMail(), recipient);
            }

            final MailEvent currentMailEvent = generateCurrentMailEvent(campaignContext, recipient);

            StringBuilder currentMailEventRemark = new StringBuilder();

            if (canSend(recipient, currentMailEvent, currentMailEventRemark, campaignContext.getCampaign().getState())) {
                if (!campaignContext.getMailer().addRecipient(new DakEmail(recipient))) {
                    log.debug("could not add " + recipient + " : cause: "
                            + campaignContext.getMailer().getErrors().get(campaignContext.getMailer().getErrors().size() - 1));
                } else {
                    currentCampaignEvent.setExpected_mails(currentCampaignEvent.getExpected_mails() + 1);
                }
            }

            mailEvents.add(currentMailEvent);

            statsCounter += 1;
            if (statsCounter == STATS_THRESHOLD) {
                flushStats();
                statsCounter = 0;
            }
        }
        // one final flush to db
        flushStats();
        // provide what and how to store in db, if not provided, log will be printed to console
        // runs on different thread, logs stored in db in separate thread
        if (!campaignContext.getMailer().sendMails(logDAO, currentCampaignEvent)) {
            // either this is a dry run or some other thing happened, either way, no mail was sent
            currentCampaignEvent.setRemark("No mail was sent");
            logDAO.saveCampaignEvent(currentCampaignEvent);
        }
    }

    private MailEvent generateCurrentMailEvent(@NonNull final CampaignContext campaignContext, @NonNull final String recipient) {
        final MailEvent currentMailEvent = new MailEvent();
        currentMailEvent.setTrigger_time(triggerTimeStamp);
        currentMailEvent.setCampaign_id(campaignContext.getCampaign().getId());
        currentMailEvent.setCampaign_version(campaignContext.getCampaign().getVersion());
        currentMailEvent.setRecipient(recipient);
        return currentMailEvent;
    }

    private Boolean canSend(@NonNull final String recipient,
                            @NonNull final MailEvent currentMailEvent,
                            @NonNull final StringBuilder currentMailEventRemark,
                            @NonNull final String campaignState) {
        Boolean canBeSend;
        // check clauses in increasing order of priority
        switch (campaignState) {
            case CampaignStates.APPROVED:
                currentMailEventRemark.append(MailEventRemarks.SCHEDULED_RUN);
                canBeSend = true;
                break;
            case CampaignStates.NOT_APPROVED:
                canBeSend = false;
                currentMailEventRemark.append(MailEventRemarks.SCHEDULED_RUN);
                currentMailEventRemark.append(MailEventRemarks.AND);
                currentMailEventRemark.append(MailEventRemarks.CAMPAIGN_NOT_APPROVED);
                break;
            default:
                canBeSend = false;
                currentMailEventRemark.append(MailEventRemarks.UNKNOWN_STATE);
        }

        // most important step
        if (!mailLimiter.canSend(recipient)) {
            canBeSend = false;
            currentMailEventRemark.append(MailEventRemarks.AND);
            currentMailEventRemark.append(MailEventRemarks.THRESHOLD_LIMIT_REACHED);
        }


        if (DakiyaUtils.isBadEmail(recipient)) {
            canBeSend = false;
            currentMailEventRemark.append(MailEventRemarks.AND);
            currentMailEventRemark.append(MailEventRemarks.BAD_EMAIL);
        }

        currentMailEvent.setRemark(currentMailEventRemark.toString());
        currentMailEvent.setMail_sent(canBeSend);
        return canBeSend;
    }

    private void flushStats() throws JobExecutionException {
        try {
            // mailLimiter must be called first even if saving events fails, we are still safe.
            mailLimiter.flushStatsToDb();
        } catch (Exception e) {
            throw new JobExecutionException("could not save stats in db. Causing: " + e.getMessage());
        }
        try {
            logDAO.saveMailEvents(mailEvents.iterator());
            // fresh start
            mailEvents.clear();
        } catch (Exception e) {
            throw new JobExecutionException("could not save mail events in db. Causing: " + e.getMessage());
        }
    }


    private Map<String, Map<String, Object>> getDynamicMailData(@NonNull final Campaign campaign,
                                                                @NonNull final String sql) throws JobExecutionException {
        final List<Map<String, Object>> resultSet = redshiftDao.getResultsetRows(sql);
        if (resultSet == null) {
            throw new JobExecutionException("could not fetch / populate recipients for dynamic mail");
        }
        return DynamicMailUtils.getDynamicMailData(resultSet, campaign.getMail());
    }


}

@Getter
@Value
@Builder
class CampaignContext {
    @NonNull
    Mustache mustache;
    @NonNull
    List<String> recipients;
    @Nullable
    Map<String, Map<String, Object>> dynamicMailData;
    @NonNull
    Dak mail;
    @NonNull
    String mailType;
    @NonNull
    Mailer mailer;
    @NonNull
    Campaign campaign;
    @NonNull
    HashMap<String, String> customTrackingArgs;

    @NonFinal
    boolean staticMailInitialized;

    void initializeStaticMail() {
        if (mailType.equals(MailType.STATIC) && !staticMailInitialized) {
            mailer.addMailContainer(campaign.getMail(), customTrackingArgs);
            staticMailInitialized = true;
            return;
        }
        throw new IllegalArgumentException("either mail type is not static or it has already been initialized");
    }

    void initializeDynamicMail(@NonNull final Dak dak, @NonNull final String recipient) {
        if (mailType.equals(MailType.DYNAMIC)) {
            final String dakContentForThisRecipient = DynamicMailUtils
                    .compileMustache(mustache, dynamicMailData.get(recipient));
            final String subject = DynamicMailUtils
                    .compileMustache(dak.getSubject(), dynamicMailData.get(recipient));
            final Dak dakForThisRecipient = dak
                    .toBuilder()
                    .subject(subject)
                    .content(dakContentForThisRecipient)
                    .build();

            mailer.addMailContainer(dakForThisRecipient, customTrackingArgs);
            return;
        }
        throw new IllegalArgumentException("mailtype is not dynamic");
    }

}
