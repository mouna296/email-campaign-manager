package com.untitled.ecm.resources;

import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.dao.DakDAO;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.mail.Mailer;
import com.untitled.ecm.services.mail.MailerFactory;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/mails")
@Produces(MediaType.APPLICATION_JSON)
// allowing all to see the mails, one may want to reuse/refer the mails from campaigns created by others.
@RolesAllowed({Roles.CAMPAIGN_SUPERVISOR, Roles.CAMPAIGN_MANAGER})
public class DakResource {
    private DakDAO dakDAO;
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;

    public DakResource(DakDAO dakDAO, DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        if (dakDAO == null || dakiyaRuntimeSettings == null) {
            throw new InstantiationError("null value provided");
        }
        this.dakDAO = dakDAO;
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
    }

    @GET
    public Response getAllMails() {
        return Response.status(200).entity(this.dakDAO.findAll()).build();
    }

    @GET
    @Path("/{id}")
    public Response getMailById(@PathParam("id") int id) {
        Dak dak = this.dakDAO.findDakById(id);
        if (dak != null) {
            return Response.status(200).entity(dak).build();
        } else {
            throw new NotFoundException("no such mail exists");
        }

    }

    @GET
    @Path("/get-demo-mail/{mail-id}")
    public Message getDemoMail(@PathParam("mail-id") int id, @Context SecurityContext securityContext) {
        Dak dak = this.dakDAO.findDakById(id);
        if (dak == null) {
            throw new NotFoundException("no such mails exists");
        }

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        Mailer mailer = MailerFactory.getMailer(dakiyaRuntimeSettings.getEnvType(), dakiyaRuntimeSettings.getDefaultDomainSendGridAPIKey());
        mailer.addMailContainer(dak, null);

        if (!mailer.addRecipient(new DakEmail(dakiyaUser.getEmail()))) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }

        if (!mailer.sendMails(null, null)) {
            throw new InternalServerErrorException((JSONObject.valueToString(mailer.getErrors())));
        }

        return new Message("Mail sent to " + dakiyaUser.getEmail());
    }
}
