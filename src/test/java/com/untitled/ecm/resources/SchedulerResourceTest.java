package com.untitled.ecm.resources;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.CampaignCommons;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.dtos.DakiyaCampaignJobDetail;
import com.untitled.ecm.testcommons.dtos.TestCampaign;
import com.untitled.ecm.testcommons.dtos.TestCampaignScheduleModel;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.*;

public class SchedulerResourceTest extends CampaignCommons {

    @Test
    public void ensureGetSchedulerStatusWorks() {
        final Response response = makeGetRequest(dakiyaTestUser, "schedulers/status");
        assertEquals(OK.getStatusCode(), response.getStatus());
        final Message message = response.readEntity(Message.class);
        // hacky
        assertTrue(message.message.endsWith("is running"));
    }

    @Test
    public void onlySuperUserOrCampaignManagerShouldBeAbleToScheduleOrUnScheduleACampaign() {
        DakiyaTestUser unPrivilegedDakiyaTestUser;

        if (dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) || dakiyaTestUser.getRole().equals(Roles.SUPER_USER)) {
            unPrivilegedDakiyaTestUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        } else {
            unPrivilegedDakiyaTestUser = dakiyaTestUser;
        }

        Response response = makePostRequest(unPrivilegedDakiyaTestUser, "schedulers/schedule-campaign/-1", "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

        response = makePostRequest(unPrivilegedDakiyaTestUser, "schedulers/execute-campaign-now/-1", "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

        response = makePostRequest(unPrivilegedDakiyaTestUser, "schedulers/unschedule-campaign/-1", "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

    }

    @Test
    public void campaignScheduleMustNotExistAfterCreation() {
        // this function internally ensures that schedule doesn't get created during campaign creation
        attemptCampaignCreationViaRestApi();
    }

    @Test
    public void ensureCampaignSchedulingWorks() {
        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();
        scheduleCampaign(pair.getRight());
    }

    @Test
    public void ensureGetAllSchedulesWorks() {
        Response response = makeGetRequest(dakiyaTestUser, "schedulers/campaigns");
        assertEquals(OK.getStatusCode(), response.getStatus());

        Map<String, DakiyaCampaignJobDetail> jobDetails = response.readEntity(new GenericType<Map<String, DakiyaCampaignJobDetail>>() {
        });
        final int existingJobsCount = jobDetails.size();

        final int newJobsCount = 5;

        Map<Integer, Pair<TestCampaign, Campaign>> campaignMap = new HashMap<>();
        Map<Integer, DakiyaCampaignJobDetail> jobDetailMap = new HashMap<>();
        Pair<TestCampaign, Campaign> pair;

        for (int i = 0; i < newJobsCount; i++) {
            pair = attemptCampaignCreationViaRestApi();
            campaignMap.put(pair.getRight().getId(), pair);
            jobDetailMap.put(pair.getRight().getId(), scheduleCampaign(pair.getRight()));
        }

        response = makeGetRequest(dakiyaTestUser, "schedulers/campaigns");
        assertEquals(OK.getStatusCode(), response.getStatus());


        Map<String, DakiyaCampaignJobDetail> allJobs = response.readEntity(new GenericType<Map<String, DakiyaCampaignJobDetail>>() {
        });

        assertEquals(existingJobsCount + newJobsCount, allJobs.size());

        int validatedJobDetailsCount = 0;

        for (DakiyaCampaignJobDetail jobDetail : allJobs.values()) {
            if (!campaignMap.containsKey(jobDetail.getDakiyaSchedulerJobCampaignID())) {
                continue;
            }
            ensureDakiyaJobDetailIsValid(campaignMap.get(jobDetail.getDakiyaSchedulerJobCampaignID()).getRight(), jobDetail);
            assertEquals(jobDetailMap.get(jobDetail.getDakiyaSchedulerJobCampaignID()), jobDetail);
            validatedJobDetailsCount++;
        }

        assertEquals(newJobsCount, validatedJobDetailsCount);

    }

    @Test
    public void ensureUnSchedulingCampaignWorks() {
        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();
        scheduleCampaign(pair.getRight());
        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        final Response response = makePostRequest(campaignManager, "schedulers/unschedule-campaign/" + pair.getRight().getId(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        ensureCampaignScheduleDoesNotExist(pair.getRight().getId());
    }

    @Test
    public void ensureUnSchedulingCategoryWorks() throws Exception {
        resetDB();

        List<Integer> campaignsIds = new ArrayList<>();
        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        Response response = makePostRequest(campaignManager, "schedulers/unschedule-category/test", "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<Integer> unscheduledCampaigns = response.readEntity(new GenericType<List<Integer>>() {
        });
        assertEquals(0, unscheduledCampaigns.size());

        final int totalCampaigns = 7;
        final int campaignWithCategoryToBeUnscheduledCount = 5;

        final String category = UUID.randomUUID().toString();
        final TestCampaignScheduleModel model = getTestCampaignDefaultSchedule();

        Pair<TestCampaign, Campaign> pair;
        for (int i = 0; i < totalCampaigns; i++) {
            if (i < (totalCampaigns - campaignWithCategoryToBeUnscheduledCount)) {
                pair = attemptCampaignCreationViaRestApi(campaignManager, createDefaultTestCampaign(model));
                scheduleCampaign(pair.getRight());
                campaignsIds.add(pair.getRight().getId());
                continue;
            }
            pair = attemptCampaignCreationViaRestApi(campaignManager, createDefaultTestCampaign(model).toBuilder().category(category).build());
            scheduleCampaign(pair.getRight());
            campaignsIds.add(pair.getRight().getId());
        }

        response = makePostRequest(campaignManager, "schedulers/unschedule-category/" + category, "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        unscheduledCampaigns = response.readEntity(new GenericType<List<Integer>>() {
        });
        assertEquals(campaignWithCategoryToBeUnscheduledCount, unscheduledCampaigns.size());

        for (int i = 0; i < totalCampaigns; i++) {
            if (i < (totalCampaigns - campaignWithCategoryToBeUnscheduledCount)) {
                assertTrue(!unscheduledCampaigns.contains(campaignsIds.get(i)));
                continue;
            }
            assertTrue(unscheduledCampaigns.contains(campaignsIds.get(i)));
            ensureCampaignScheduleDoesNotExist(campaignsIds.get(i));
        }

        resetDB();
    }


    @Test
    public void ensureExecuteCampaignNowWorks() throws Exception {
        resetDB();

        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();
        final Campaign campaign = pair.getRight();
        final TestCampaign testCampaign = pair.getLeft();
        final int mailLimit = testCampaign.getMailLimit();
        final Instant mailsWereSentAfterThis = Instant.now();
        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);

        Response response;

        for (int runCount = 1; runCount <= mailLimit; runCount++) {
            response = makePostRequest(campaignManager, "schedulers/execute-campaign-now/" + campaign.getId(), "{}");
            assertEquals(OK.getStatusCode(), response.getStatus());
            validateAfterMathOfCampaignExecution(testCampaign, campaign, mailsWereSentAfterThis, runCount);
        }

        response = makePostRequest(campaignManager, "schedulers/execute-campaign-now/" + campaign.getId(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());

        validateAfterMathOfCampaignExecution(testCampaign, campaign, mailsWereSentAfterThis, mailLimit);

        resetDB();
    }

    @Test
    public void ensureScheduledCampaignGetsExecuted() throws Exception {
        resetDB();

        final int secondsDelay = 2;

        final TestCampaignScheduleModel model = TestCampaignScheduleModel
                .builder()
                .startAt(DateTime.now().plusSeconds(secondsDelay).withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA)))
                .endAt(DateTime.now().plusYears(20).withZone(DateTimeZone.forID(DakiyaStrings.DAKIYA_TIMEZONE_INDIA)))
                .repeatPeriod(Duration.standardHours(3).toPeriod())
                // this campaign should execute upto one time only
                .repeatThreshold(0)
                .mailLimit(2)
                .build();

        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(dakiyaTestUser, model);
        final Campaign campaign = pair.getRight();
        final TestCampaign testCampaign = pair.getLeft();

        final DakiyaTestUser campaignManager = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_MANAGER) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);

        final Response response = makePostRequest(campaignManager, "schedulers/schedule-campaign/" + campaign.getId() + "?ignore-state=true", "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());

        final Instant mailsWereSentAfterThis = Instant.now();

        Thread.sleep(secondsDelay * 1000);
        validateAfterMathOfCampaignExecution(testCampaign, campaign, mailsWereSentAfterThis, 1);

        Thread.sleep(secondsDelay * 1000);
        ensureCampaignScheduleDoesNotExist(campaign.getId());

        resetDB();
    }

    private void validateAfterMathOfCampaignExecution(TestCampaign testCampaign,
                                                      Campaign campaign,
                                                      Instant mailsWereSentAfterThis,
                                                      int maxMailSentPerRecipient) throws InterruptedException {
        // let all the chain of events unfold in time, (quartz)
        Thread.sleep(1000);
        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(campaign.getMailIDinDB());
        assertNotNull(mailRecord);
        assertEqualsMail(testCampaign.getMail(), mailRecord.getDak());
        assertMailTrackingDetailsAreCorrent(campaign, mailRecord.getTrackingDetails());
        assertEquals(dummyEmailsInMetabase.size() * maxMailSentPerRecipient, mailRecord.getRecipients().size());

        for (String recipient : dummyEmailsInMetabase) {
            ensureMailRecordContainsRecipient(mailRecord, recipient, mailsWereSentAfterThis, maxMailSentPerRecipient);
        }
    }



}
