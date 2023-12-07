package com.untitled.ecm.testcommons;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.dtos.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;

public class CampaignCommons extends ResourceTestBase {

    protected Pair<TestCampaign, Campaign> attemptCampaignCreationViaRestApi() {
        return attemptCampaignCreationViaRestApi(dakiyaTestUser, getTestCampaignDefaultSchedule());
    }

    protected Pair<TestCampaign, Campaign> attemptCampaignCreationViaRestApi(DakiyaTestUser dakiyaTestUser, TestCampaignScheduleModel scheduleModel) {
        final TestCampaign testCampaign = createDefaultTestCampaign(scheduleModel);
        return attemptCampaignCreationViaRestApi(dakiyaTestUser, testCampaign);
    }

    protected Pair<TestCampaign, Campaign> attemptCampaignCreationViaRestApi(DakiyaTestUser dakiyaTestUser, TestCampaign testCampaign) {
        final Response response = makePostRequest(dakiyaTestUser, "campaigns", testCampaign);
        assertEquals(OK.getStatusCode(), response.getStatus());

        Campaign campaign = response.readEntity(Campaign.class);

        assertEqualsCampaign(testCampaign, campaign, dakiyaTestUser);

        assertEqualsCampaign(testCampaign, getCampaignByIdViaRestApi(campaign.getId()), dakiyaTestUser);

        ensureCampaignScheduleDoesNotExist(campaign.getId());

        return Pair.of(testCampaign, campaign);
    }

    protected void ensureCampaignSanityAfterUpdate(Campaign oldCampaign, Campaign updatedCampaign) {
        assertEquals(oldCampaign.getId(), updatedCampaign.getId());
        assertEquals(oldCampaign.getCreatedOn(), updatedCampaign.getCreatedOn());
        assertNotEquals(oldCampaign.getLastModifiedTime(), updatedCampaign.getLastModifiedTime());
        assertTrue(DateTime.parse(oldCampaign.getLastModifiedTime()).isBefore(DateTime.parse(updatedCampaign.getLastModifiedTime())));
        ensureCampaignScheduleDoesNotExist(updatedCampaign.getId());
    }

    protected Campaign getCampaignByIdViaRestApi(int id) {
        final Response response = makeGetRequest(dakiyaTestUser, "campaigns/" + id);
        assertEquals(OK.getStatusCode(), response.getStatus());
        return response.readEntity(Campaign.class);
    }

    protected DakiyaCampaignJobDetail scheduleCampaign(Campaign campaign) {
        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        Response response = makePostRequest(campaignManager, "schedulers/schedule-campaign/" + campaign.getId() + "?ignore-state=true", "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());

        final DakiyaCampaignJobDetail inputJobDetails = response.readEntity(DakiyaCampaignJobDetail.class);
        ensureDakiyaJobDetailIsValid(campaign, inputJobDetails);

        response = makeGetRequest(dakiyaTestUser, "schedulers/campaigns/" + campaign.getId());
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(inputJobDetails, response.readEntity(DakiyaCampaignJobDetail.class));

        return inputJobDetails;
    }

    protected void ensureDakiyaJobDetailIsValid(Campaign campaign, DakiyaCampaignJobDetail jobDetails) {
        assertEquals(campaign.getId(), jobDetails.getDakiyaSchedulerJobCampaignID());
        assertEquals(campaign.getMailIDinDB(), jobDetails.getDakiyaSchedulerJobCampaignMailID());
        assertEquals(campaign.getStartAt(), jobDetails.getDakiyaSchedulerJobCampaignStartTime().toString());
        assertEquals(campaign.getEndAt(), jobDetails.getDakiyaSchedulerJobCampaignEndTime().toString());
        assertEquals(1, jobDetails.getNoOfTriggers());
        assertEquals(campaign.getVersion(), jobDetails.getDakiyaSchedulerJobCampaignVersion());
    }

    public TestCampaign createDefaultTestCampaign(TestCampaignScheduleModel scheduleModel) {
        final String sendgridDomainForThisCampaign = sendridDomains.iterator().next();
        return TestCampaign
                .builder()
                .title("campaign " + UUID.randomUUID().toString())
                .about("test campaign created for testing purposes only")
                // get all the emails available in dummy metabase table
                .sql(String.format("select * from %s", DUMMY_METABASE_DUMMY_TABLE_NAME))
                .startAt(scheduleModel.getStartAt().toString())
                .endAt(scheduleModel.getEndAt().toString())
                .repeatPeriod(scheduleModel.getRepeatPeriod().toString())
                .repeatThreshold(scheduleModel.getRepeatThreshold())
                .mailLimit(scheduleModel.getMailLimit())
                .sendgridDomain(sendgridDomainForThisCampaign)
                .category("test")
                .mail(createStaticTestMail(sendgridDomainForThisCampaign))
                .build();
    }

    protected void ensureCampaignScheduleDoesNotExist(int id) {
        final Response response = makeGetRequest(dakiyaTestUser, "schedulers/campaigns/" + id);
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    protected TestCampaignScheduleModel getTestCampaignDefaultSchedule() {
        return TestCampaignScheduleModel
                .builder()
                .startAt(DateTime.now().plusYears(10).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)))
                .endAt(DateTime.now().plusYears(20).withZone(DateTimeZone.forID(DakiyaStrings.TIMEZONE_LA)))
                .repeatPeriod(Duration.standardDays(3).toPeriod())
                .repeatThreshold(0)
                .mailLimit(2)
                .build();
    }

    private TestMail createStaticTestMail(String sendgridDomainForThisCampaign) {
        return TestMail
                .builder()
                .mailType(MailType.STATIC)
                .subject("mail " + UUID.randomUUID().toString())
                .content(" test content for test campaign")
                .from(EmailAddress.of("from-test", "test@" + sendgridDomainForThisCampaign))
                .replyTo(EmailAddress.of("test-reply-to", "test@example.com"))
                .build();
    }

    protected void assertMailTrackingDetailsAreCorrent(Campaign expected, Map<String, String> actual) {
        assertEquals(Integer.toString(expected.getId()), actual.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID));
        assertEquals(Integer.toString(expected.getVersion()), actual.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION));
        assertEquals(expected.getSendgridDomain(), actual.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_SENDGRID_DOMAIN));
        assertEquals(StringUtils.truncate(expected.getMail().getSubject(), 30), actual.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_MAIL_SUBJECT));
        assertEquals(expected.getCategory(), actual.get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY));
    }

    protected void assertEqualsCampaign(TestCampaign expected, Campaign actual, DakiyaTestUser creator) {
        assertTrue(actual.getId() >= 0);
        assertEquals(actual.getCampaignCreator(), creator.getEmail());
        if (actual.getLastModifiedBy().equals(actual.getCampaignCreator())) {
            assertEquals(CampaignStates.getDefaultStateByRole(Lists.newArrayList(creator.getRole())), actual.getState());
        }

        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getAbout(), actual.getAbout());
        assertEquals(expected.getSql(), actual.getSql());

        assertEqualsCampaignSchedule(expected, actual);

        assertEquals(expected.getSendgridDomain(), actual.getSendgridDomain());
        assertEquals(expected.getCategory(), actual.getCategory());

        assertTrue(actual.getMail().getId() >= 0);
        assertEquals(actual.getMail().getId(), actual.getMailIDinDB());

        assertEqualsMail(expected.getMail(), actual.getMail());

        assertEquals(RULE.getConfiguration().getInstanceType(), actual.getDakiyaInstanceType());

    }

    private void assertEqualsCampaignSchedule(TestCampaign expected, Campaign actual) {
        assertEquals(expected.getStartAt(), actual.getStartAt());
        assertEquals(expected.getEndAt(), actual.getEndAt());
        assertEquals(expected.getRepeatPeriod(), actual.getRepeatPeriod());
        assertEquals(expected.getRepeatThreshold(), actual.getRepeatThreshold());
        assertEquals(expected.getMailLimit(), actual.getMailLimit());
    }

    protected void assertEqualsMail(TestMail expectedMail, Dak actualMail) {
        assertEqualsMail(expectedMail, actualMail, true);
    }

    protected void assertEqualsMail(TestMail expectedMail, Dak actualMail, boolean isStaticMail) {
        assertEquals(expectedMail.getMailType(), actualMail.getMailType());
        assertEquals(expectedMail.getSubject(), actualMail.getSubject());
        if (isStaticMail) {
            assertEquals(expectedMail.getContent(), actualMail.getContent());
        }
        assertEquals(expectedMail.getContentType(), "html");

        assertEqualsEmailAddress(expectedMail.getFrom(), actualMail.getFrom());
        assertEqualsEmailAddress(expectedMail.getReplyTo(), actualMail.getReplyTo());
    }

    protected void validateTrackingDetailsOfTestMail(Campaign campaign, InMemoryMailer.MailRecord mailRecord) {
        assertEquals(mailRecord.getTrackingDetails().get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID), Integer.toString(campaign.getId()));
        assertEquals(mailRecord.getTrackingDetails().get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION), Integer.toString(campaign.getVersion()));
        assertEquals(mailRecord.getTrackingDetails().get(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY), campaign.getCategory());
        assertEquals(mailRecord.getTrackingDetails().get(DakiyaStrings.DAKIYA_IS_TEST_MAIL), "yes");
    }

    protected void assertEqualsEmailAddress(EmailAddress expectedEMailAddress, DakEmail actualEmailAddress) {
        assertEquals(expectedEMailAddress.getName(), actualEmailAddress.getName());
        assertEquals(expectedEMailAddress.getEmail(), actualEmailAddress.getEmail());
    }

}
