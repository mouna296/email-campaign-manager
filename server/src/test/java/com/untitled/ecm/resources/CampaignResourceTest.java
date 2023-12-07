package com.untitled.ecm.resources;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.ArchivedCampaignWithMail;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.CampaignCommons;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.dtos.TestCampaign;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;

public class CampaignResourceTest extends CampaignCommons {
    @Test
    public void ensureCreateCampaignWorks() {
        attemptCampaignCreationViaRestApi();
    }

    @Test
    public void getByIncorrectCampaignIdMustReturn404() {
        final Response response = makeGetRequest(dakiyaTestUser, "campaigns/-1");
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void updateByIncorrectCampaignIdMustNotBeAllowed() {
        final Response response = makePostRequest(dakiyaTestUser, "campaigns/-1", TestCampaign.builder().build());
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void ensureUpdateCampaignWorks() {
        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();

        final TestCampaign updatedTestCampaign = pair.getLeft().toBuilder().about("updated campaign " + pair.getRight().getId()).build();

        Response response = makePostRequest(dakiyaTestUser, "campaigns/" + pair.getRight().getId(), updatedTestCampaign);
        assertEquals(OK.getStatusCode(), response.getStatus());
        final Campaign updatedCampaign = response.readEntity(Campaign.class);

        Campaign oldCampaign = pair.getRight();

        ensureCampaignSanityAfterUpdate(oldCampaign, updatedCampaign);

        assertNotEquals(oldCampaign.getMailIDinDB(), updatedCampaign.getMailIDinDB());

        assertEqualsCampaign(updatedTestCampaign, updatedCampaign, dakiyaTestUser);

        assertEqualsCampaign(updatedTestCampaign, getCampaignByIdViaRestApi(updatedCampaign.getId()), dakiyaTestUser);

    }

    @Test
    public void ensureBulkFetchWorks() {
        Response response = makeGetRequest(dakiyaTestUser, "campaigns");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<Campaign> campaigns = response.readEntity(new GenericType<List<Campaign>>() {
        });
        assertNotNull(campaigns);
        final int existingCampaignCountInDB = campaigns.size();


        Map<Integer, Pair<TestCampaign, Campaign>> map = new HashMap<>();
        final int newCampaignCount = 5;
        Pair<TestCampaign, Campaign> pair;
        for (int i = 0; i < newCampaignCount; i++) {
            pair = attemptCampaignCreationViaRestApi();
            map.put(pair.getRight().getId(), pair);
        }
        response = makeGetRequest(dakiyaTestUser, "campaigns");
        assertEquals(OK.getStatusCode(), response.getStatus());
        campaigns = response.readEntity(new GenericType<List<Campaign>>() {
        });

        assertNotNull(campaigns);
        assertEquals(existingCampaignCountInDB + newCampaignCount, campaigns.size());

        int verifiedCampaigns = 0;
        for (Campaign campaign : campaigns) {
            if (!map.containsKey(campaign.getId())) {
                continue;
            }
            pair = map.get(campaign.getId());
            assertEqualsCampaign(pair.getLeft(), pair.getRight(), dakiyaTestUser);
            assertEqualsCampaign(pair.getLeft(), getCampaignByIdViaRestApi(campaign.getId()), dakiyaTestUser);
            verifiedCampaigns++;
        }
        assertEquals(newCampaignCount, verifiedCampaigns);
    }

    @Test
    public void ensureCampaignApprovalWorks() {
        final DakiyaTestUser campaignSuperVisor = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_SUPERVISOR) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(campaignSuperVisor, getTestCampaignDefaultSchedule());
        Campaign unApprovedCampaign = pair.getRight();
        assertEquals(CampaignStates.NOT_APPROVED, unApprovedCampaign.getState());
        assertNull(unApprovedCampaign.getApprovedBy());
        assertNull(unApprovedCampaign.getApprovedAt());

        // campaign supervisor must not be allowed to approve campaign
        Response response = makePostRequest(campaignSuperVisor, "campaigns/approve/" + unApprovedCampaign.getId(), "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

        final DakiyaTestUser campaignManager = createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        response = makePostRequest(campaignManager, "campaigns/approve/" + unApprovedCampaign.getId(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        Message message = response.readEntity(Message.class);
        // hacky
        assertTrue(message.message.contains(Integer.toString(unApprovedCampaign.getId())));

        Campaign approvedCampaign = getCampaignByIdViaRestApi(unApprovedCampaign.getId());

        assertEquals(unApprovedCampaign.getId(), approvedCampaign.getId());
        assertEquals(unApprovedCampaign.getCreatedOn(), approvedCampaign.getCreatedOn());
        assertEquals(unApprovedCampaign.getLastModifiedTime(), approvedCampaign.getLastModifiedTime());
        assertEquals(campaignManager.getEmail(), approvedCampaign.getApprovedBy());
        assertNotNull(approvedCampaign.getApprovedAt());
        assertNotNull(approvedCampaign.getApprovedBy());
        assertTrue(DateTime.parse(approvedCampaign.getApprovedAt()).isAfter(DateTime.parse(approvedCampaign.getLastModifiedTime())));
        ensureCampaignScheduleDoesNotExist(approvedCampaign.getId());
        assertEquals(CampaignStates.APPROVED, approvedCampaign.getState());

    }

    @Test
    public void ensureCampaignArchivalWorks() throws Exception {
        resetDB();
        final DakiyaTestUser campaignSuperVisor = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_SUPERVISOR) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(campaignSuperVisor, getTestCampaignDefaultSchedule());
        Campaign campaign = pair.getRight();

        // campaign supervisor must not be allowed to archive campaign
        Response response = makePostRequest(campaignSuperVisor,
                "campaigns/archive/" + campaign.getId(), "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());

        final DakiyaTestUser campaignManager = createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        response = makePostRequest(campaignManager, "campaigns/archive/" + campaign.getId(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());

        Message message = response.readEntity(Message.class);
        assertTrue(message.message.contains(Integer.toString(campaign.getId())));

        response = makeGetRequest(dakiyaTestUser, "campaigns/" + campaign.getId());
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());

        response = makeGetRequest(dakiyaTestUser, "campaigns/archived");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<ArchivedCampaignWithMail> campaigns = response.readEntity(new GenericType<List<ArchivedCampaignWithMail>>(){});

        assertEquals(1, campaigns.size());
        assertEquals("archived", campaigns.get(0).getState());
        assertEquals(campaigns.get(0).getMail().getId(), campaigns.get(0).getMail_dbid());
        assertEquals(1, campaigns.get(0).getId());
        assertEquals(0, campaigns.get(0).getVersion());
    }

    @Test
    public void ensureGetDemoMailWorks() {
        final DakiyaTestUser campaignSuperVisor = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_SUPERVISOR) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(campaignSuperVisor, getTestCampaignDefaultSchedule());
        Campaign campaign = pair.getRight();
        final Instant mailsWereSentAfterThis = Instant.now();
        final Response response = makeGetRequest(dakiyaTestUser, "campaigns/get-demo-mail/" + campaign.getId());
        assertEquals(OK.getStatusCode(), response.getStatus());

        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(campaign.getMailIDinDB());

        assertEqualsMail(pair.getLeft().getMail(), mailRecord.getDak());
        validateTrackingDetailsOfTestMail(campaign, mailRecord);
        ensureMailRecordContainsRecipient(mailRecord, campaign.getCampaignCreator(), mailsWereSentAfterThis, 1);
    }

    @Test
    public void ensureSendTestMailsWorks() {
        final DakiyaTestUser campaignSuperVisor = dakiyaTestUser.getRole().equals(Roles.CAMPAIGN_SUPERVISOR) ? dakiyaTestUser : createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi(campaignSuperVisor, getTestCampaignDefaultSchedule());
        Campaign campaign = pair.getRight();
        final Instant mailsWereSentAfterThis = Instant.now();
        final int apiHitCount = 3;

        final List<String> testMailRecipients = Lists.newArrayList(UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com",
                UUID.randomUUID().toString() + "@gmail.com");

        Response response;

        for (int i = 0; i < apiHitCount; i++) {
            response = makePostRequest(dakiyaTestUser, "campaigns/send-test-mails/" + campaign.getId(), testMailRecipients);
            assertEquals(OK.getStatusCode(), response.getStatus());
        }

        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(campaign.getMailIDinDB());

        assertEqualsMail(pair.getLeft().getMail(), mailRecord.getDak());

        validateTrackingDetailsOfTestMail(campaign, mailRecord);

        for (String recipient : testMailRecipients) {
            ensureMailRecordContainsRecipient(mailRecord, recipient, mailsWereSentAfterThis, apiHitCount);
        }

        ensureMailRecordContainsRecipient(mailRecord, campaign.getCampaignCreator(), mailsWereSentAfterThis, apiHitCount);
    }

    @Test
    public void ensureCampaignCannotBeCreatedAndUpdatedWithInvalidParams() {
        TestCampaign testCampaign = createDefaultTestCampaign(getTestCampaignDefaultSchedule())
                .toBuilder()
                .sql("rubbish swlsdfa")
                .sendgridDomain("adfasdfad")
                .repeatPeriod("adfadf")
                .startAt("adfafd")
                .endAt("-adfadfa").build();
        // there are more ways to send incorrect data but for now we will just stick to these.
        Response response = makePostRequest(dakiyaTestUser, "campaigns", testCampaign);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

        List<String> errors = response.readEntity(new GenericType<List<String>>() {
        });
        assertNotNull(errors);
        // one for each error = 5 and + 1 for "5 schema violation found"
        assertEquals(6, errors.size());

        // now check for update path
        final Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();
        response = makePostRequest(dakiyaTestUser, "campaigns/" + pair.getRight().getId(), testCampaign);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        errors = response.readEntity(new GenericType<List<String>>() {
        });
        assertNotNull(errors);
        // one for each error = 5 and + 1 for "5 schema violation found"
        assertEquals(6, errors.size());
    }

}
