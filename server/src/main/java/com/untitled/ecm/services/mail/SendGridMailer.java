package com.untitled.ecm.services.mail;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.core.DakiyaBackGroundTasksManager;
import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dtos.CampaignEvent;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.sendgrid.*;
import lombok.Getter;
import lombok.NonNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.util.*;

/**********************************************************************************************
 *
 * BEWARE!, this test cases for this codepath does not exist, every change to this class
 * must be validated by running a instance locally (environment variable in config yaml must not be "TEST")
 * and then hitting sendgrid spam-me endpoint or campaign get demo mail
 * to be extra sure verify via execute campaign now call
 *
 **********************************************************************************************/
public class SendGridMailer extends Mailer {
    // range [1 : 1000]
    private static final int SENDGRID_PER_API_CALL_RECIPIENTS_THRESHOLD = 1000;
    @Getter
    private Dak dak;
    private SendGrid sendGrid;
    private Logger logger;
    private Mail sendGridMail;
    private List<Mail> mailsYetToBeSend;
    private boolean canAddRecipient;
    private int sendgridCurrentMailRecipientsCount;

    SendGridMailer(String sendGridAPIKey) {
        if (sendGridAPIKey == null || sendGridAPIKey.length() == 0) {
            throw new InstantiationError("null or empty sendgrid api key provided");
        }

        this.sendGrid = new SendGrid(sendGridAPIKey);
        this.logger = LoggerFactory.getLogger(SendGridMailer.class);
        this.mailsYetToBeSend = new ArrayList<>();
        this.sendgridCurrentMailRecipientsCount = 0;
        this.canAddRecipient = true;
    }

    @Override
    public void addMailContainer(@NonNull final Dak dak,
                                 @Nullable Map<String, String> mailTrackingDetails) {
        if (mailTrackingDetails == null) {
            mailTrackingDetails = new HashMap<>();
        }
        Optional<List<String>> err = Mailer.validateDak(dak);
        if (err.isPresent()) {
            getErrors().add("invalid dak");
            getErrors().addAll(err.get());
            throw new IllegalArgumentException("invalid dak");
        }

        this.dak = dak;

        if (sendGridMail != null
                && sendGridMail.getPersonalization() != null
                && sendGridMail.getPersonalization().size() > 0) {
            // save current mail to list
            mailsYetToBeSend.add(sendGridMail);
        }

        // this will create a new instance of mail
        sendGridMail = this.createNewSendGridMail(dak);

        assert sendGridMail != null;
        for (Map.Entry<String, String> entry : mailTrackingDetails.entrySet()) {
            //todo  add check if string are way too long
            sendGridMail.addCustomArg(entry.getKey(), entry.getValue());
        }
        if (mailTrackingDetails.containsKey(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY)) {
            sendGridMail.addCategory(mailTrackingDetails.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY));
        }
    }

    @Override
    public boolean addRecipient(DakEmail dakEmail) {
        if (!canAddRecipient) {
            getErrors().add("adding mails not allowed after calling sendmail");
            return false;
        }
        Personalization personalization = new Personalization();
        Email to = new Email();

        Optional<List<String>> err = Mailer.validateEmail(dakEmail.getEmail());

        if (err.isPresent()) {
            getErrors().addAll(err.get());
            return false;
        }

        to.setEmail(dakEmail.getEmail());

        // we can do without name
        if (!Mailer.validateNameOfDakEmail(dakEmail.getName()).isPresent()) {
            to.setName(dakEmail.getName());
        }

        personalization.addTo(to);
        sendGridMail.addPersonalization(personalization);

        this.sendgridCurrentMailRecipientsCount += 1;

        if (this.sendgridCurrentMailRecipientsCount == SENDGRID_PER_API_CALL_RECIPIENTS_THRESHOLD) {
            this.sendgridCurrentMailRecipientsCount = 0;
            Map<String, String> mailTrackingDetails = sendGridMail.getCustomArgs();
            addMailContainer(dak, mailTrackingDetails);
        }
        return true;
    }

    @Override
    public boolean addRecipients(List<DakEmail> dakEmails) {
        if (!canAddRecipient) {
            getErrors().add("adding mails not allowed after calling sendmail");
            return false;
        }
        Boolean noErrors = true;
        for (DakEmail dakEmail : dakEmails) {
            if (!this.addRecipient(dakEmail)) {
                noErrors = false;
            }
        }
        return noErrors;
    }

    /**
     * this methods submits the task to dakiyabackgroudtask manager which will spawn a new thread and
     * makes call to sendgrid per object in mails yet to be sent list
     * is async
     *
     * @return
     */
    @Override
    public boolean sendMails(LogDAO logDAO, CampaignEvent campaignEvent_) {
        if (!this.canSendMails()) {
            return false;
        }
        CampaignEvent campaignEvent = campaignEvent_;
        if (campaignEvent == null) {
            campaignEvent = getDummyCampaignEvent();
        }

        canAddRecipient = false;
        // pass it to runnable so that it can update campaign event in db when task is finished
        campaignEvent.setMails_sent(0);
        campaignEvent.setRemark("Attempting to start sending mails now at : "
                + DateTime.now(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)).toString());
        SendGridAPICaller sendGridAPICaller = new SendGridAPICaller(sendGrid, mailsYetToBeSend, getErrors(), logDAO, campaignEvent);


        try {
            // save this in db,
            // incase if any error happens while sending mails and error cannot be logged in db,
            // this will be keep record till this line
            if (logDAO != null) {
                logDAO.saveCampaignEvent(campaignEvent);
            }
        } catch (Exception e) {
            logger.error("could not save campaign event in db. Causing: " + e.getMessage());
        }

        return DakiyaBackGroundTasksManager.submitTask(sendGridAPICaller);
    }

    private CampaignEvent getDummyCampaignEvent() {
        CampaignEvent campaignEvent;
        campaignEvent = new CampaignEvent();
        campaignEvent.setCampaign_id(-1);
        campaignEvent.setExpected_mails(-1);
        campaignEvent.setMails_filtered(-1);
        campaignEvent.setMails_sent(-1);
        campaignEvent.setTrigger_time(new Timestamp(DateTime.now(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)).getMillis()));
        campaignEvent.setVersion(-1);
        campaignEvent.setRemark("This event is not for a campaign");
        return campaignEvent;
    }

    private Mail createNewSendGridMail(Dak dak) {
        if (sendGrid == null) {
            getErrors().add("sendgrid null");
            return null;
        }

        Mail mail = new Mail();

        Email from = new Email();
        from.setEmail(dak.getFrom().getEmail());
        from.setName(dak.getFrom().getName());
        mail.setFrom(from);
        Email replyTo = new Email();
        replyTo.setEmail(dak.getReplyTo().getEmail());
        replyTo.setName(dak.getReplyTo().getName());
        mail.setReplyTo(replyTo);

        mail.setSubject(dak.getSubject());

        Content content = new Content();
        content.setType(mimes.get(dak.getContentType()));
        content.setValue(dak.getContent());
        mail.addContent(content);

        mail.setTrackingSettings(getSendGridTrackingSettings());

        return mail;
    }

    private TrackingSettings getSendGridTrackingSettings() {
        ClickTrackingSetting clickTrackingSetting = new ClickTrackingSetting();
        clickTrackingSetting.setEnable(true);
        clickTrackingSetting.setEnableText(true);

        OpenTrackingSetting openTrackingSetting = new OpenTrackingSetting();
        openTrackingSetting.setEnable(true);

        SubscriptionTrackingSetting subscriptionTrackingSetting = new SubscriptionTrackingSetting();
        subscriptionTrackingSetting.setEnable(true);
        TrackingSettings trackingSettings = new TrackingSettings();
        trackingSettings.setClickTrackingSetting(clickTrackingSetting);
        trackingSettings.setOpenTrackingSetting(openTrackingSetting);
        trackingSettings.setSubscriptionTrackingSetting(subscriptionTrackingSetting);

        return trackingSettings;
    }

    private boolean canSendMails() {
        if (sendGrid == null) {
            getErrors().add("sendgrid object is null");
            return false;
        }
        // check if there is atleast one recipients and add the last batch
        if (sendGridMail != null && sendGridMail.getPersonalization() != null
                && sendGridMail.getPersonalization().size() > 0) {
            mailsYetToBeSend.add(sendGridMail);
        }

        if (mailsYetToBeSend.size() == 0) {
            getErrors().add("no mail or recipients found");
            return false;
        }
        return true;
    }

}
