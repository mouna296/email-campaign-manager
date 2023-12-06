package com.untitled.ecm.core;

import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dao.MailStatDAO;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.MailEvent;
import com.untitled.ecm.dtos.MailStat;
import lombok.Data;
import lombok.NonNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;

// todo in current implementation rate limit is assumed per day, make it flexible,
// note: db and dakiyaruntime is flexible enough to change this easily

/*******************************************************************************************
 !! THIS CLASS ASSUMES THAT ONLY ONE JOB WILL BE RUNNING AT A TIME !!
 ******************************************************************************************/
@Data
public class MailLimiter {
    private MailStatDAO mailStatDAO;
    private LogDAO logDAO;
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;
    private int dailyMailThreshold = 0;
    private Campaign campaign;
    private HashMap<String, MailStat> todayMailStats;
    private HashMap<String, MailStat> toBePersisted;
    private HashMap<String, Integer> campaignSentMailCounts;
    // O(1) lookups , can't keep this as list, also can't just not keep it in tobeupdated, boolean size < mailstat
    private HashMap<String, Boolean> persisted;
    private Date todayDate;
    private Logger logger;


    public MailLimiter(@NonNull final MailStatDAO mailStatDAO,
                       @NonNull final LogDAO logDAO,
                       @NonNull final DakiyaRuntimeSettings dakiyaRuntimeSettings,
                       @NonNull final DateTime currentDateTime,
                       @NonNull final Campaign campaign) {
        this.mailStatDAO = mailStatDAO;
        this.logDAO = logDAO;
        // currently only one setting is needed from this, but soon atleast two setting will be needed, thus this next line
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
        this.dailyMailThreshold = this.dakiyaRuntimeSettings.getMaxEmailsAllowedPerRecipientPerDay();
        this.campaign = campaign;
        this.todayDate = new Date(currentDateTime.getMillis());
        this.logger = LoggerFactory.getLogger(MailLimiter.class);
        this.todayMailStats = new HashMap<>();
        this.campaignSentMailCounts = new HashMap<>();
        this.toBePersisted = new HashMap<>();
        this.persisted = new HashMap<>();
        this.generateCounts();
    }

    private void generateCounts() {
        /*********************************************************************
         !! BEWARE !! THIS MAY CONTAIN STALE DATA AND EVERYTHING IS LOADED IN MEMORY
         ********************************************************************/
        List<MailEvent> mailEvents;
        List<MailStat> _todayMailStats;
        _todayMailStats = mailStatDAO.findStatByDate(todayDate);
        mailEvents = logDAO.getMailEventByCampaignID(campaign.getId());

        if (_todayMailStats == null || mailEvents == null) {
            throw new RuntimeException("null stats and or mail events");
        }

        // counts for daily limit
        for (MailStat stat : _todayMailStats) {
            todayMailStats.put(stat.getRecipient(), stat);
        }

        // for campaign per user mail limit
        for (MailEvent mailEvent : mailEvents) {
            if (campaignSentMailCounts.containsKey(mailEvent.getRecipient())) {
                campaignSentMailCounts
                        .put(mailEvent.getRecipient(), campaignSentMailCounts.get(mailEvent.getRecipient()) + 1);
            } else {
                campaignSentMailCounts.put(mailEvent.getRecipient(), 1);
            }
        }
    }

    public boolean canSend(String email) {
        // check if this email has already occurred for this run of the campaign
        if (toBePersisted.containsKey(email)) {
            logger.info("query returned duplicate mail for a campaign execution, ignoring: "
                    + DakiyaUtils.getObfuscatedEmail(email));
            return false;
        }

        // check if this email has been updated in db, this email occurred for this run of campaign
        if (persisted.containsKey(email)) {
            logger.info("query returned duplicate mail for a campaign execution, ignoring: "
                    + DakiyaUtils.getObfuscatedEmail(email));
            return false;
        }

        // check if this user has already received enough mail from this campaign
        if (campaignSentMailCounts.containsKey(email) &&
                campaignSentMailCounts.get(email) >= campaign.getMailLimit()) {
            return false;
        }

        // check if it is in current stats
        if (!todayMailStats.containsKey(email)) {
            // first mail for this recipient for today
            MailStat mailStat = new MailStat();
            mailStat.setSent_mail_count(1);
            mailStat.setRecipient(email);
            mailStat.setDate(todayDate);
            toBePersisted.put(email, mailStat);
            return true;
        }

        MailStat tempStat = todayMailStats.get(email);
        if (tempStat.getSent_mail_count() >= dailyMailThreshold) {
            return false;
        }

        MailStat mailStat = new MailStat();
        mailStat.setSent_mail_count(tempStat.getSent_mail_count() + 1);
        mailStat.setRecipient(email);
        mailStat.setDate(todayDate);
        toBePersisted.put(email, mailStat);
        return true;
    }

    public void flushStatsToDb() {
        mailStatDAO.updateStats(toBePersisted.values().iterator());
        for (String email : toBePersisted.keySet()) {
            persisted.put(email, true);
        }
        toBePersisted = new HashMap<>();
    }
}
