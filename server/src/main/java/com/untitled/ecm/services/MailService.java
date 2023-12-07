package com.untitled.ecm.services;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dao.DakDAO;
import com.untitled.ecm.dao.DakiyaDBFactory;
import com.untitled.ecm.dao.external.RedshiftDao;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.dtos.DakV2;
import com.untitled.ecm.services.mail.Mailer;
import com.untitled.ecm.services.mail.MailerFactory;
import com.untitled.ecm.services.scheduler.jobs.DynamicMailUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import java.sql.SQLException;
import java.util.*;

@Slf4j
public class MailService {
    private final DakiyaRuntimeSettings dakiyaRuntimeSettings;
    private final DakDAO dakDAO;

    public MailService(@NonNull final DakDAO dakDAO,
                       @NonNull final DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
        this.dakDAO = dakDAO;
    }

    public void sendDemoMailOfCampaign(@NonNull final Campaign campaign,
                                       @NonNull final DakiyaUser dakiyaUser) {
        Optional<String> apiKey = dakiyaRuntimeSettings.getSendGridAPIKeyByDomain(campaign.getSendgridDomain());
        if (!apiKey.isPresent()) {
            throw new BadRequestException("could not find key to domain: " + campaign.getSendgridDomain());
        }

        if (campaign.getMail().getMailType().equals(MailType.DYNAMIC)) {
            campaign.setMail(compileSampleDynamicMail(campaign.getMail(), campaign.getSql()));
            campaign.getMail().setMailType(MailType.STATIC);
        }

        Mailer mailer = MailerFactory.getMailer(dakiyaRuntimeSettings.getEnvType(), apiKey.get());

        mailer.addMailContainer(campaign.getMail(), getTrackingDetailsForTestMail(campaign));

        if (!mailer.addRecipient(new DakEmail(dakiyaUser.getEmail()))) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }

        if (!mailer.sendMails(null, null)) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }
    }

    public List<String> sendTestMailsForCampaign(Campaign campaign, List<String> testMailRecipients, DakiyaUser dakiyaUser) {
        if (testMailRecipients == null || testMailRecipients.size() == 0) {
            throw new BadRequestException("Minimum one test recipients required");
        }

        Optional<String> apiKey = dakiyaRuntimeSettings.getSendGridAPIKeyByDomain(campaign.getSendgridDomain());
        if (!apiKey.isPresent()) {
            throw new BadRequestException("could not find key to domain: " + campaign.getSendgridDomain());
        }

        if (campaign.getMail().getMailType().equals(MailType.DYNAMIC)) {
            campaign.setMail(compileSampleDynamicMail(campaign.getMail(), campaign.getSql()));
            campaign.getMail().setMailType(MailType.STATIC);
        }

        Mailer mailer = MailerFactory.getMailer(dakiyaRuntimeSettings.getEnvType(), apiKey.get());
        mailer.addMailContainer(campaign.getMail(), getTrackingDetailsForTestMail(campaign));

        if (!testMailRecipients.contains(dakiyaUser.getEmail())) {
            testMailRecipients.add(dakiyaUser.getEmail());
        }

        int count = 0;
        final int MAX_TEST_EMAIL_RECIPIENTS = 10;
        List<String> actualRecipients = new ArrayList<>();

        for (String recipient : testMailRecipients) {
            if (count > MAX_TEST_EMAIL_RECIPIENTS) {
                log.warn("get test mail: recipient count exceeded, remaining recipients will be ignored");
                break;
            }
            if (!StringUtils.isEmpty(recipient) && mailer.addRecipient(new DakEmail(recipient))) {
                actualRecipients.add(recipient);
                count++;
            }
        }

        if (!mailer.sendMails(null, null)) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }

        return actualRecipients;
    }

    private Map<String, String> getTrackingDetailsForTestMail(Campaign campaign) {
        Map<String, String> customTrackingArgs = new HashMap<>();
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID, Integer.toString(campaign.getId()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION, Integer.toString(campaign.getVersion()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY, campaign.getCategory());
        customTrackingArgs.put(DakiyaStrings.DAKIYA_IS_TEST_MAIL, "yes");
        return customTrackingArgs;
    }

    private Dak compileSampleDynamicMail(@NonNull final Dak source, @NonNull final String sql) {
        StringBuilder mailContent = new StringBuilder("=============================<br/><br/>");
        final String testMessagefooter = "<br/>=============================<br/><br/>";
        RedshiftDao redshiftDao;
        try {
            redshiftDao = new RedshiftDao(DakiyaDBFactory.getMetabaseDB());
        } catch (SQLException e) {
            throw new InternalServerErrorException("could not initialize metabase connection");
        }

        final List<Map<String, Object>> resultset = redshiftDao.getSingleResultsetRow(sql);
        if (resultset == null || resultset.size() == 0) {
            mailContent.append("no data returned with the campaign sql query");
            mailContent.append(testMessagefooter);
            mailContent.append(source.getContent());
        }

        if (resultset == null || resultset.size() != 1) {
            throw new AssertionError("expected 1 row found none or more than 1");
        }

        final Map<String, Map<String, Object>> mailData = DynamicMailUtils
                .getDynamicMailData(resultset, source);

        if (mailData.isEmpty()) {
            mailContent.append("The query contained no recipient even though it returned something. " +
                    "A campaign with dynamic mail must have sql query which returns column named email");
            mailContent.append(testMessagefooter);
            return source
                    .toBuilder()
                    .content(mailContent.toString())
                    .build();
        }

        final String recipient = (String) mailData.keySet().toArray()[0];
        final String dakContent = DynamicMailUtils.compileMustache(source.getContent(),
                mailData.get(recipient));
        final String dakSubject = DynamicMailUtils.compileMustache(source.getSubject(), mailData.get(recipient));

        if (DakiyaUtils.isEmptyOrWhitespace(dakContent) ||
                DakiyaUtils.isEmptyOrWhitespace(dakSubject) ||
                (source.getContent().equals(dakContent) && source.getSubject().equals(dakSubject))) {
            mailContent.append("Empty or unprocessable mail. Ensure that you pass mail type as Dynamic with correct query");
            mailContent.append(testMessagefooter);
            mailContent.append(source.getContent());
        } else {
            mailContent.append("Mail generated with data for recipient: ");
            mailContent.append(recipient);
            mailContent.append(testMessagefooter);
            mailContent.append(dakContent);
        }

        return source
                .toBuilder()
                .subject(dakSubject)
                .content(mailContent.toString())
                .build();
    }

    int saveMailinDB(JSONObject mail, String creator) {
        try {
            DakV2 dakV2 = new ObjectMapper().readValue(mail.toString(), DakV2.class);
            dakV2.setCreator(creator);
            if (Mailer.validateDak(getDakFromDakv2(dakV2)).isPresent()) {
                throw new BadRequestException("invalid mail");
            }
            final int id = dakDAO.saveMailInDBV2(dakV2);
            if (id < 0) {
                throw new InternalServerErrorException("internal db error, could not save mail in db");
            }
            return id;
        } catch (Exception e) {
            throw new InternalServerErrorException("internal error, could not save mail in db");
        }
    }

    private Dak getDakFromDakv2(DakV2 v2) {
        return Dak.builder()
                .id(-1)
                .subject(v2.getSubject())
                .content(v2.getContent())
                .contentType(v2.getContentType())
                .from(v2.getFrom())
                .replyTo(v2.getFrom())
                .creator(v2.getCreator())
                .mailType(v2.getMailType())
                .build();
    }

    Dak getMailById(int id) {
        final Dak dak = dakDAO.findDakById(id);
        if (dak == null) {
            throw new RuntimeException("anomaly detected, no mail exist in db with id" + id);
        }
        return dak;
    }
}

