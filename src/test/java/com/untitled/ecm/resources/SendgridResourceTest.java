package com.untitled.ecm.resources;

import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.mail.InMemoryMailer;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.ResourceTestBase;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SendgridResourceTest extends ResourceTestBase {

    @Test
    public void ensureGetSengridDomainWorks() {
        final DakiyaTestUser dakiyaTestUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        final Response response = makeGetRequest(dakiyaTestUser, "sendgrids/domain");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<String> domains = response.readEntity(new GenericType<List<String>>() {
        });
        assertEquals(sendridDomains.size(), domains.size());
        assertTrue(sendridDomains.containsAll(domains));
    }

    @Test
    public void ensureUserIsAbleToSelfSpam() {
        final DakiyaTestUser dakiyaTestUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
        final Instant mailsWereSentAfterThis = Instant.now();
        final Response response = makeGetRequest(dakiyaTestUser, "sendgrids/spam-me");
        assertEquals(OK.getStatusCode(), response.getStatus());
        final Message message = response.readEntity(Message.class);
        assertTrue(message.message.contains(dakiyaTestUser.getEmail()));

        InMemoryMailer.MailRecord mailRecord = InMemoryMailer.getSentMailRecord(-420);
        ensureMailRecordContainsRecipient(mailRecord, dakiyaTestUser.getEmail(), mailsWereSentAfterThis, 1);
    }

}
