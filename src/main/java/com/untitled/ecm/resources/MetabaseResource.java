package com.untitled.ecm.resources;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.dao.external.RedshiftDao;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.resources.validators.MetabaseSQLValidator;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.skife.jdbi.v2.ResultIterator;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
@Path("/metabases")
public class MetabaseResource {
    private RedshiftDao redshiftDao;

    public MetabaseResource(RedshiftDao redshiftDao) {
        if (redshiftDao == null) {
            throw new InstantiationError("null value provided");
        }
        this.redshiftDao = redshiftDao;
    }

    @GET
    @Path("/status")
    public Message getStatus() {
        MetabaseSQLValidator metabaseSQLValidator = new MetabaseSQLValidator(this.redshiftDao);
        if (!metabaseSQLValidator.isRedshiftUpAndAccessible()) {
            throw new InternalServerErrorException("Redshift is down");
        } else {
            return new Message(DakiyaStrings.METABASE_STATUS_OK);
        }
    }


    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @Path("/preview")
    public Response preview(@Context HttpServletRequest httpServletRequest) throws IOException {

        JSONObject jsonObject = new JSONObject(new JSONTokener(httpServletRequest.getInputStream()));
        String userSQL = jsonObject.getString("sql");
        String sql = userSQL.toLowerCase();
        MetabaseSQLValidator metabaseSQLValidator = new MetabaseSQLValidator(this.redshiftDao);
        Optional<String> metabaseSQLErrors = metabaseSQLValidator.validate(sql);
        if (metabaseSQLErrors.isPresent()) {
            throw new BadRequestException(metabaseSQLErrors.get());
        }

        try {
            ResultIterator<String> recipientsIterator = this.redshiftDao.getAllRecipientsIterator(userSQL);
            List<String> previewEmails = new ArrayList<>();
            int count = 0;
            while (recipientsIterator.hasNext() && count < DakiyaStrings.METABASE_MAX_PREVIEW_COUNT) {
                previewEmails.add(recipientsIterator.next());
                count++;
            }

            recipientsIterator.close();

            if (previewEmails.size() == 0) {
                count = 0;
            } else {
                count = Integer.parseInt(this.redshiftDao.getRowCountOfSqlQueryResult(userSQL));
            }

            JSONObject responseJson = new JSONObject();
            responseJson.put("queryResultCount", count);
            responseJson.put("previewEmails", previewEmails);
            return Response.status(200).entity(responseJson.toString()).build();
        } catch (Exception e) {
            throw new BadRequestException("Could not execute query. Causing: " + e.getMessage());
        }

    }
}
