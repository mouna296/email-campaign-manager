package com.untitled.ecm.services.mail;

import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dtos.CampaignEvent;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMailer extends Mailer {
    private static final Map<String, Integer> dynamicDakRecipientToDakOffsetMap = new HashMap<>();
    // don't make this final, needed for some reflection magic
    private static Map<Integer, MailRecord> SENT_MAILS_RECORDS = new HashMap<>();
    private final Map<Integer, MailRecord> mailRecords = new HashMap<>();
    private boolean isMailSendingOrAddingProhibited = false;
    private int currentMailBatchId = -1;
    // this is to keep track of dynamic mails, which contains data specific to particular recipients only
    private int offset = Integer.MAX_VALUE / 2;
    private boolean isCurrentMailBatchDynamic = false;

    public static MailRecord getSentMailRecord(int dakId) {
        return SENT_MAILS_RECORDS.get(dakId);
    }

    public static MailRecord getSentDynamicMailRecord(String recipient) {
        return SENT_MAILS_RECORDS.get(dynamicDakRecipientToDakOffsetMap.get(recipient));
    }

    @Override
    public void addMailContainer(Dak dak, Map<String, String> mailTrackingDetails) {
        if (isMailSendingOrAddingProhibited) {
            throw new IllegalArgumentException("mail sending now allowed");
        }

        Optional<List<String>> err = Mailer.validateDak(dak);

        if (err.isPresent()) {
            getErrors().addAll(err.get());
            throw new IllegalArgumentException(String.valueOf(err.get()));
        }

        if (dak.getMailType().equals(MailType.DYNAMIC)) {
            isCurrentMailBatchDynamic = true;
            offset++;
            currentMailBatchId = dak.getId() + offset;
            mailRecords.put(currentMailBatchId, new MailRecord(dak, mailTrackingDetails));
            return;
        } else {
            isCurrentMailBatchDynamic = false;
        }

        if (mailRecords.containsKey(dak.getId())) {
            return;
        }
        mailRecords.put(dak.getId(), new MailRecord(dak, mailTrackingDetails));
        currentMailBatchId = dak.getId();
    }

    @Override
    public boolean addRecipient(DakEmail dakEmail) {
        if (isMailSendingOrAddingProhibited) {
            return false;
        }
        if (isValidDakEmail(dakEmail)) {
            Instant now;
            // sometimes instant.Now is not different event in two subsequent calls to this function
            // this is necessary for avoid scenario where this function gets called again before Instant.now value changes
            do {
                now = Instant.now();
            } while (mailRecords.get(currentMailBatchId).getRecipients().get(now) != null);

            mailRecords.get(currentMailBatchId).getRecipients().put(now, dakEmail);

            if (isCurrentMailBatchDynamic) {
                dynamicDakRecipientToDakOffsetMap.put(dakEmail.getEmail(), currentMailBatchId);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addRecipients(List<DakEmail> dakEmails) {
        if (isMailSendingOrAddingProhibited) {
            return false;
        }
        for (DakEmail dakEmail : dakEmails) {
            if (!addRecipient(dakEmail)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean sendMails(LogDAO logDAO, CampaignEvent campaignEvent) {
        if (isMailSendingOrAddingProhibited) {
            return false;
        }
        // special case, a mail batch of dynamic mail can have only one recipient
        if (isCurrentMailBatchDynamic) {
            SENT_MAILS_RECORDS.putAll(mailRecords);
            isMailSendingOrAddingProhibited = false;
            return true;
        }

        // if this static mail then each dak can have multiple recipient
        for (Map.Entry<Integer, MailRecord> entry : mailRecords.entrySet()) {
            if (SENT_MAILS_RECORDS.containsKey(entry.getKey())) {
                // merge recipients
                SENT_MAILS_RECORDS.get(entry.getKey()).getRecipients().putAll(entry.getValue().getRecipients());
            } else {
                SENT_MAILS_RECORDS.put(entry.getKey(), entry.getValue());
            }
        }
        isMailSendingOrAddingProhibited = true;
        return true;
    }

    private boolean isValidDakEmail(DakEmail dakEmail) {
        return !(validateNameOfDakEmail(dakEmail.getName()).isPresent() || validateEmail(dakEmail.getEmail()).isPresent());
    }

    @Data
    public class MailRecord {
        final Dak dak;
        final Map<String, String> trackingDetails;
        final Map<Instant, DakEmail> recipients = new HashMap<>();
    }

}
