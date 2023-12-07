package com.untitled.ecm.resources;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.testcommons.DakiyaTestUser;
import com.untitled.ecm.testcommons.ResourceTestBase;
import com.untitled.ecm.testcommons.dtos.MetabasePreviewRequest;
import com.untitled.ecm.testcommons.dtos.MetabasePreviewResponse;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// these test cover code path which is path of campaign creation and update also
public class MetabseResourceTest extends ResourceTestBase {

    private final ImmutableList<String> forbiddenSQLCommands = populateForbiddenSQLCommands();
    private DakiyaTestUser testUser;

    @Before
    public void before2() {
        testUser = createDakiyaTestUser(Roles.CAMPAIGN_SUPERVISOR);
    }

    @Test
    public void ensureMetabseIsUp() {
        Response response = makeGetRequest(testUser, "metabases/status");
        assertEquals(OK.getStatusCode(), response.getStatus());
        Message message = response.readEntity(Message.class);
        assertEquals(DakiyaStrings.METABASE_STATUS_OK, message.message);
    }

    @Test
    public void ensureMetabsePreviewApiWorks() {
        MetabasePreviewRequest request = new MetabasePreviewRequest();
        request.setSql(String.format("select count(*) from %s", DUMMY_METABASE_DUMMY_TABLE_NAME));
        Response response = makePostRequest(testUser, "metabases/preview", request);
        assertEquals(OK.getStatusCode(), response.getStatus());

        MetabasePreviewResponse previewResponse = response.readEntity(MetabasePreviewResponse.class);
        assertEquals(1, previewResponse.getQueryResultCount());
        // metabase always sends the first column of queryresult
        assertEquals(dummyEmailsInMetabase.size(), Integer.parseInt(previewResponse.getPreviewEmails().get(0)));
        assertTrue(previewResponse.getQueryResultCount() >= previewResponse.getPreviewEmails().size());

        request.setSql(String.format("select * from %s", DUMMY_METABASE_DUMMY_TABLE_NAME + ";"));
        response = makePostRequest(testUser, "metabases/preview", request);
        assertEquals(OK.getStatusCode(), response.getStatus());

        previewResponse = response.readEntity(MetabasePreviewResponse.class);
        assertEquals(dummyEmailsInMetabase.size(), previewResponse.getQueryResultCount());
        assertTrue(previewResponse.getQueryResultCount() >= previewResponse.getPreviewEmails().size());
        assertTrue(previewResponse.getPreviewEmails().size() <= DakiyaStrings.METABASE_MAX_PREVIEW_COUNT);
        assertTrue(dummyEmailsInMetabase.containsAll(previewResponse.getPreviewEmails()));
    }

    @Test
    public void ensureMetabasePreviewApiAcceptsOnlyCorrectSelectQueries() {
        MetabasePreviewRequest request = new MetabasePreviewRequest();
        request.setSql(String.format("%s from %s", forbiddenSQLCommands.get(0), DUMMY_METABASE_DUMMY_TABLE_NAME));
        Response response;
        Message message;
        for (String command : this.forbiddenSQLCommands) {
            request.setSql(String.format("%s from %s", command, DUMMY_METABASE_DUMMY_TABLE_NAME));
            response = makePostRequest(testUser, "metabases/preview", request);
            assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
            message = response.readEntity(Message.class);
            assertEquals(DakiyaStrings.NON_SELECT_COMMAND_PRESENT_IN_SQl, message.message);
        }

        request.setSql("some random string sfasdfasfd");
        response = makePostRequest(testUser, "metabases/preview", request);
        message = response.readEntity(Message.class);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(DakiyaStrings.SELECT_COMMAND_NOT_PRESENT_IN_SQl, message.message);

        request.setSql(String.format("select select from %s", DUMMY_METABASE_DUMMY_TABLE_NAME));
        // this will send the sql syntax error in detail, if use want then he/she can use this response to debug
        response = makePostRequest(testUser, "metabases/preview", request);
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    private ImmutableList<String> populateForbiddenSQLCommands() {
        List<String> forbiddenSQLCommands = new ArrayList<>();
        // trailing space is important to prevent catching any column or table name

        // ddl
        forbiddenSQLCommands.add("create ");
        forbiddenSQLCommands.add("alter ");
        forbiddenSQLCommands.add("drop ");
        forbiddenSQLCommands.add("truncate ");
        forbiddenSQLCommands.add("comment ");
        forbiddenSQLCommands.add("rename ");

        // dml
        forbiddenSQLCommands.add("insert ");
        forbiddenSQLCommands.add("update ");
        forbiddenSQLCommands.add("delete ");
        forbiddenSQLCommands.add("merge ");
        forbiddenSQLCommands.add("call ");
        forbiddenSQLCommands.add("lock table ");

        // dcl
        forbiddenSQLCommands.add("grant ");
        forbiddenSQLCommands.add("revoke ");

        // tcl
        forbiddenSQLCommands.add("commit ");
        forbiddenSQLCommands.add("rollback ");
        return ImmutableList.copyOf(forbiddenSQLCommands);

    }

}
