package com.untitled.ecm.resources;

import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.testcommons.CampaignCommons;
import com.untitled.ecm.testcommons.dtos.TestCampaign;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DakResourceTest extends CampaignCommons {

    @Test
    public void ensureIncorrectMailIdReturns() {
        final Response response = makeGetRequest(dakiyaTestUser, "mails/-1");
        assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void ensureGetMailByIdWorks() {
        Pair<TestCampaign, Campaign> pair = attemptCampaignCreationViaRestApi();
        final Response response = makeGetRequest(dakiyaTestUser, "mails/" + pair.getRight().getMailIDinDB());
        assertEquals(OK.getStatusCode(), response.getStatus());
        final Dak dak = response.readEntity(Dak.class);
        assertEqualsMail(pair.getLeft().getMail(), dak);
        assertEquals(dakiyaTestUser.getEmail(), dak.getCreator());
        assertEquals(pair.getRight().getMailIDinDB(), dak.getId());
    }

    @Test
    public void ensureGetAllMailsWorks() {
        Response response = makeGetRequest(dakiyaTestUser, "mails");
        assertEquals(OK.getStatusCode(), response.getStatus());
        List<Dak> daks = response.readEntity(new GenericType<List<Dak>>() {
        });
        final int existingDaksCount = daks.size();
        final int newDaksCount = 5;

        Map<Integer, Pair<TestCampaign, Campaign>> map = new HashMap<>();
        Pair<TestCampaign, Campaign> pair;
        for (int i = 0; i < newDaksCount; i++) {
            pair = attemptCampaignCreationViaRestApi();
            map.put(pair.getRight().getMailIDinDB(), pair);
        }

        response = makeGetRequest(dakiyaTestUser, "mails");
        assertEquals(OK.getStatusCode(), response.getStatus());
        daks = response.readEntity(new GenericType<List<Dak>>() {
        });

        assertEquals(existingDaksCount + newDaksCount, daks.size());

        int validatedDaksCount = 0;

        for (Dak dak : daks) {
            if (!map.containsKey(dak.getId())) {
                continue;
            }
            pair = map.get(dak.getId());
            assertNotNull(pair);
            assertEqualsMail(pair.getLeft().getMail(), dak);
            assertEquals(dakiyaTestUser.getEmail(), dak.getCreator());
            assertEquals(pair.getRight().getMailIDinDB(), dak.getId());
            validatedDaksCount++;
        }

        assertEquals(newDaksCount, validatedDaksCount);
    }

}
