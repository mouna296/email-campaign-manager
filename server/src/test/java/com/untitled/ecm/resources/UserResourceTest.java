package com.untitled.ecm.resources;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.DakiyaUserDetails;
import com.untitled.ecm.dtos.http.CreateUserRequest;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.ResourceTestBase;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;

public class UserResourceTest extends ResourceTestBase {

    @Test
    public void ensureThatUserIsAbleToLoginWithCorrectCredentials() {
        DakiyaTestUser testUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Response response = makeGetRequest(testUser, "users/me");
        assertEquals(OK.getStatusCode(), response.getStatus());
        DakiyaUserDetails dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        assertEquals(testUser.getEmail(), dakiyaUserDetails.getEmail());
        assertEquals(testUser.getFirstName(), dakiyaUserDetails.getFirstName());
        assertEquals(testUser.getLastName(), dakiyaUserDetails.getLastName());
        assertEquals(testUser.getRole(), dakiyaUserDetails.getRole());
    }

    @Test
    public void ensureNoLoginWithIncorrectCredentials() {
        DakiyaTestUser testUser = createDakiyaTestUser(Roles.SUPER_USER);
        String correctPass = testUser.getPassword();
        String incorrectPass;
        do {
            incorrectPass = RandomStringUtils.randomAlphanumeric(1000);
        } while (correctPass.equals(incorrectPass));
        Response response = makeGetRequest(testUser.toBuilder().password(incorrectPass).build(), "users/me");
        assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

        // this user doesn't even exist
        // internally this will assign a new email
        DakiyaTestUser testUser1 = DakiyaTestUser
                .builder()
                .email(UUID.randomUUID().toString() + "@gmail.com")
                .role(Roles.SUPER_USER).password("adfasdfasdfasd").build();
        response = makeGetRequest(testUser1, "users/me");
        assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void ensureOnlySuperUserCanAccessOtherUserDetails() throws Exception {
        // this will also create a default dakiya test user
        resetDB();
        DakiyaTestUser superUser = createDakiyaTestUser(Roles.SUPER_USER);
        Response response = makeGetRequest(superUser, "users/me");
        DakiyaUserDetails dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(superUser, dakiyaUserDetails);

        DakiyaTestUser campaignManager = createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        response = makeGetRequest(superUser, "users/" + campaignManager.getEmail());
        assertEquals(OK.getStatusCode(), response.getStatus());
        dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(campaignManager, dakiyaUserDetails);

        DakiyaTestUser supervisor = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        response = makeGetRequest(superUser, "users/" + supervisor.getEmail());
        assertEquals(OK.getStatusCode(), response.getStatus());
        dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(supervisor, dakiyaUserDetails);

        response = makeGetRequest(superUser, "users");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<DakiyaUserDetails> dakiyaUserDetailsList = response.readEntity(new GenericType<List<DakiyaUserDetails>>() {
        });
        assertEquals(4, dakiyaUserDetailsList.size());

        // verify created on
        List<DakiyaUserDetails> orderedDakiyaUserDetailsList = dakiyaUserDetailsList
                .stream()
                .sorted(Comparator.comparing(o -> DateTime.parse(o.getCreatedOn())))
                .collect(Collectors.toList());

        // ensure all the users are there with correct info
        ensureUserDetailsMatch(superUser, orderedDakiyaUserDetailsList.get(1));
        ensureUserDetailsMatch(campaignManager, orderedDakiyaUserDetailsList.get(2));
        ensureUserDetailsMatch(supervisor, orderedDakiyaUserDetailsList.get(3));


        // ensure no one else can access this urls
        response = makeGetRequest(campaignManager, "users/" + campaignManager.getEmail());
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
        response = makeGetRequest(supervisor, "users/" + campaignManager.getEmail());
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
        response = makeGetRequest(campaignManager, "users");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
        response = makeGetRequest(supervisor, "users");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void ensureUserCanResetOwnPassword() throws Exception {
        resetDB();
        final Instant mailsWereSentAfterThis = Instant.now();
        final DakiyaTestUser campaignSupervisor = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Response response = makePostRequest(campaignSupervisor, "users/reset-my-password", "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(-421);
        assertNotNull(mailRecord);
        assertEquals(1, mailRecord.getRecipients().size());
        ensureMailRecordContainsRecipient(mailRecord, campaignSupervisor.getEmail(), mailsWereSentAfterThis, 1);
        final String newPassword = extractNewPasswordFromMailContent(mailRecord.getDak().getContent());
        // ensure user can login with new password
        DakiyaTestUser afterPassReset = campaignSupervisor.toBuilder().password(newPassword).build();
        response = makeGetRequest(afterPassReset, "users/me");
        assertEquals(OK.getStatusCode(), response.getStatus());
        DakiyaUserDetails dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(afterPassReset, dakiyaUserDetails);
    }

    @Test
    public void ensureSuperUserCanResetPasswords() throws Exception {
        resetDB();

        final Instant mailsWereSentAfterThis = Instant.now();
        final DakiyaTestUser superUser = createDakiyaTestUser(Roles.SUPER_USER);

        final DakiyaTestUser campaignSupervisor = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        Response response = makeGetRequest(campaignSupervisor, "users/me");
        assertEquals(OK.getStatusCode(), response.getStatus());
        DakiyaUserDetails dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(campaignSupervisor, dakiyaUserDetails);

        response = makePostRequest(superUser, "users/reset-password/" + campaignSupervisor.getEmail(), "{}");
        assertEquals(OK.getStatusCode(), response.getStatus());
        final InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(-421);
        assertNotNull(mailRecord);
        assertEquals(1, mailRecord.getRecipients().size());
        ensureMailRecordContainsRecipient(mailRecord, campaignSupervisor.getEmail(), mailsWereSentAfterThis, 1);
        final String newPassword = extractNewPasswordFromMailContent(mailRecord.getDak().getContent());

        response = makeGetRequest(campaignSupervisor, "users/me");
        assertEquals(UNAUTHORIZED.getStatusCode(), response.getStatus());

        // ensure user can login with new password
        final DakiyaTestUser afterPassReset = campaignSupervisor.toBuilder().password(newPassword).build();
        response = makeGetRequest(afterPassReset, "users/me");
        assertEquals(OK.getStatusCode(), response.getStatus());
        dakiyaUserDetails = response.readEntity(DakiyaUserDetails.class);
        ensureUserDetailsMatch(afterPassReset, dakiyaUserDetails);
    }

    @Test
    public void ensureOnlySuperUserCanResetOtherUserPassword() {
        final DakiyaTestUser campaignSupervisor = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        final DakiyaTestUser campaignManager = createDakiyaTestUser(Roles.CAMPAIGN_MANAGER);
        Response response = makePostRequest(campaignManager, "users/reset-password/" + UUID.randomUUID().toString() + "@gmail.com", "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
        response = makePostRequest(campaignSupervisor, "users/reset-password/" + UUID.randomUUID().toString() + "@gmail.com", "{}");
        assertEquals(FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void ensureNewUserAccountsCanBeCreated() {
        final DakiyaTestUser superUser = createDakiyaTestUser(Roles.SUPER_USER);
        final CreateUserRequest request = new CreateUserRequest();
        request.setEmail(UUID.randomUUID().toString() + "@gmail.com");
        request.setPassword(UUID.randomUUID().toString());
        request.setFirstName(request.getEmail());
        request.setLastName(request.getEmail());

        Response response = makePostRequest(superUser, "users", request);
        assertEquals(OK.getStatusCode(), response.getStatus());

        final DakiyaUserDetails newUserDetails = response.readEntity(DakiyaUserDetails.class);

        assertEquals(request.getEmail(), newUserDetails.getEmail());
        assertEquals(request.getFirstName(), newUserDetails.getFirstName());
        assertEquals(request.getLastName(), newUserDetails.getLastName());
        assertEquals(Roles.CAMPAIGN_MANAGER, newUserDetails.getRole());

        final DakiyaTestUser newUser = DakiyaTestUser
                .builder()
                .email(request.getEmail())
                .role(Roles.CAMPAIGN_MANAGER)
                .password(request.getPassword())
                .build();

        // assert that user can login
        response = makeGetRequest(newUser, "users/me");

        assertEquals(OK.getStatusCode(), response.getStatus());
    }

    private String extractNewPasswordFromMailContent(String mailContent) {
        final String regex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}@gmail\\.com is <strong>[0-9a-zA-Z]{" + DakiyaStrings.MIN_PASS_LENGHT + "}";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(mailContent);
        assertTrue(matcher.find());
        return matcher.group().split("<strong>")[1].trim();
    }

    private void ensureUserDetailsMatch(DakiyaTestUser expected, DakiyaUserDetails dakiyaUserDetails) {
        assertNotNull(dakiyaUserDetails);
        assertEquals(expected.getEmail(), dakiyaUserDetails.getEmail());
        assertEquals(expected.getFirstName(), dakiyaUserDetails.getFirstName());
        assertEquals(expected.getLastName(), dakiyaUserDetails.getLastName());
        assertEquals(expected.getRole(), dakiyaUserDetails.getRole());
    }


}
