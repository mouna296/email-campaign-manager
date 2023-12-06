package com.untitled.ecm.resources;

import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.CampaignCommons;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.dtos.EmailAddress;
import com.untitled.ecm.testcommons.dtos.TestCampaign;
import com.untitled.ecm.testcommons.dtos.TestCampaignScheduleModel;
import com.untitled.ecm.testcommons.dtos.TestMail;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;

public class CampaignWithDynamicMailTest extends CampaignCommons {
    // range [2:9]
    private final int noOfColumn = 6;
    // this will occur as many times as the column number
    private final int columnWhoseValueOccursMoreThanOnceInMailContent = 4;
    private final String emailColumnKey = "email";// column0;
    private final int noOfRecipients = 10;
    private final Map<String, Map<String, String>> metabaseData = new HashMap<>();

    @Test
    public void ensureCampaignWithDynamicMailWorks() throws Exception {
        resetDB();

        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(dakiyaTestUser, getCampaignWithDynamicMail());
        final Instant mailsWereSentAfterThis = Instant.now();
        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);

        final Response response = makePostRequest(campaignManager, "schedulers/execute-campaign-now/" + pair.getRight().getId(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());

        validateAfterMathOfCampaignWithDynamicMailExecution(pair.getLeft(), pair.getRight(), mailsWereSentAfterThis, 1);

        resetDB();
    }

    @Test
    public void ensureSendTestMailsWorksWithDynamic() {
        final TestCampaign inputTestCampaign = getCampaignWithDynamicMail().toBuilder().sql(StringUtils.replacePattern(getCampaignWithDynamicMail().getSql(), ";$", " liMiT 500;")).build();
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(dakiyaTestUser, inputTestCampaign);
        final TestCampaign testCampaign = pair.getLeft();
        final Campaign campaign = pair.getRight();

        final Instant mailsWereSentAfterThis = Instant.now();

        final List<String> testMailRecipients = Lists.newArrayList(UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com");
        testMailRecipients.add(campaign.getCampaignCreator());

        final Response response = makePostRequest(dakiyaTestUser, "campaigns/send-test-mails/" + campaign.getId(), testMailRecipients);
        assertEquals(OK.getStatusCode(), response.getStatus());

        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(pair.getRight().getMailIDinDB());
        assertNotNull(mailRecord);

        assertEquals(testMailRecipients.size(), mailRecord.getRecipients().size());
        assertEquals("html", mailRecord.getDak().getContentType());

        assertEqualsEmailAddress(testCampaign.getMail().getFrom(), campaign.getMail().getFrom());
        assertEqualsEmailAddress(testCampaign.getMail().getReplyTo(), campaign.getMail().getReplyTo());

        validateTrackingDetailsOfTestMail(campaign, mailRecord);

        String mailContent = mailRecord.getDak().getContent();
        String sampleRecipient = null;
        for (String recipient : metabaseData.keySet()) {
            if (mailContent.contains(recipient)) {
                sampleRecipient = recipient;
                break;
            }
        }

        assertNotNull(sampleRecipient);
        assertDynamicMailWasParsedCorrectly(sampleRecipient, mailRecord.getDak(), metabaseData.get(sampleRecipient));

        for (String recipient : testMailRecipients) {
            ensureMailRecordContainsRecipient(mailRecord, recipient, mailsWereSentAfterThis, 1);
        }
    }

    private void validateAfterMathOfCampaignWithDynamicMailExecution(TestCampaign testCampaign,
                                                                     Campaign campaign,
                                                                     Instant mailsWereSentAfterThis,
                                                                     int maxMailSentPerRecipient) throws InterruptedException {
        // let all the chain of events unfold in time, (quartz)
        Thread.sleep(5000);
        InMemoryMailer.MailRecord mailRecord;

        for (Map.Entry<String, Map<String, String>> row : metabaseData.entrySet()) {
            mailRecord = InMemoryMailer.getSentDynamicMailRecord(row.getKey());
            assertNotNull(mailRecord);
            assertEquals(1, mailRecord.getRecipients().size());
            assertEqualsMail(testCampaign.getMail(), mailRecord.getDak(), false);
            assertMailTrackingDetailsAreCorrent(campaign, mailRecord.getTrackingDetails());
            ensureMailRecordContainsRecipient(mailRecord, row.getKey(), mailsWereSentAfterThis, maxMailSentPerRecipient);
            assertDynamicMailWasParsedCorrectly(row.getKey(), mailRecord.getDak(), row.getValue());
        }

    }

    private void assertDynamicMailWasParsedCorrectly(String recipient, Dak dak, Map<String, String> data) {
        assertEquals(1, StringUtils.countMatches(dak.getContent(), createKeyValueSnippet("email", recipient, false)));

        for (Map.Entry<String, String> dataEntry : data.entrySet()) {
            if (dataEntry.getKey().equals("column" + columnWhoseValueOccursMoreThanOnceInMailContent)) {
                assertEquals(columnWhoseValueOccursMoreThanOnceInMailContent,
                        StringUtils.countMatches(dak.getContent(),
                                createKeyValueSnippet(dataEntry.getKey(), dataEntry.getValue(), false)));
                continue;
            }
            assertEquals(1, StringUtils.countMatches(dak.getContent(),
                    createKeyValueSnippet(dataEntry.getKey(), dataEntry.getValue(), false)));
        }

    }

    private TestCampaign getCampaignWithDynamicMail() {
        TestCampaignScheduleModel scheduleModel = getTestCampaignDefaultSchedule();
        final String sendgridDomainForThisCampaign = sendridDomains.iterator().next();
        return TestCampaign
                .builder()
                .title("campaign with dynamic mail" + UUID.randomUUID().toString())
                .about("test campaign created for testing purposes only")
                // get all the emails available in dummy metabase table
                .sql(populateMetabaseDataAndCreateSql())
                .startAt(scheduleModel.getStartAt().toString())
                .endAt(scheduleModel.getEndAt().toString())
                .repeatPeriod(scheduleModel.getRepeatPeriod().toString())
                .repeatThreshold(scheduleModel.getRepeatThreshold())
                .mailLimit(scheduleModel.getMailLimit())
                .sendgridDomain(sendgridDomainForThisCampaign)
                .category("test")
                .mail(createDynamicMail(sendgridDomainForThisCampaign))
                .build();
    }

    private TestMail createDynamicMail(String sendGridDomainForThisCampaign) {
        assertTrue(noOfColumn > 2);
        StringBuilder mailContent = new StringBuilder();
        mailContent.append("mail starts | ");
        mailContent.append(createKeyValueSnippet("email", null, true));
        mailContent.append(" | ");
        for (int i = 1; i <= noOfColumn; i++) {
            mailContent.append(createKeyValueSnippet("column" + i, null, true));
            mailContent.append(" | ");
        }
        for (int i = 0; i < columnWhoseValueOccursMoreThanOnceInMailContent - 1; i++) {
            mailContent.append(createKeyValueSnippet("column" + columnWhoseValueOccursMoreThanOnceInMailContent, null, true));
        }

        mailContent.append(" | mail ends");

        return TestMail.builder()
                .mailType(MailType.DYNAMIC)
                .subject("dynamic mail " + UUID.randomUUID().toString())
                .content(mailContent.toString())
                .from(EmailAddress.of("from-test", "test@" + sendGridDomainForThisCampaign))
                .replyTo(EmailAddress.of("test-reply-to", "test@example.com"))
                .build();
    }

    private String createKeyValueSnippet(String key, String value, boolean forTemplate) {
        if (forTemplate) {
            return key + ":{{" + key + "}}";
        } else {
            return key + ":" + value;
        }
    }

    private String populateMetabaseDataAndCreateSql() {
        StringBuilder mail = new StringBuilder();
        for (int i = 0; i < noOfRecipients; i++) {
            Pair<String, Map<String, String>> row = getMetabaseRow();
            metabaseData.put(row.getLeft(), row.getRight());
            mail.append("select \'" + row.getLeft() + "\' as email" + expandDataToSqlString(row.getRight()));
            if (i == (noOfRecipients - 1)) {
                mail.append(";");
            } else {
                mail.append(" union ");
            }
        }
        return mail.toString();
    }

    private String expandDataToSqlString(Map<String, String> data) {
        StringBuilder sql = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            sql.append(", \'" + entry.getValue() + "\' as " + entry.getKey());
        }
        return sql.toString();
    }

    private Pair<String, Map<String, String>> getMetabaseRow() {
        Map<String, String> data = new HashMap<>();
        for (int i = 1; i <= noOfColumn; i++) {
            data.put("column" + i, RandomStringUtils.randomAlphanumeric(10, 20));
        }
        return Pair.of(UUID.randomUUID().toString() + "@gmail.com", data);
    }

}
