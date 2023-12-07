package com.untitled.ecm.resources;


import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.mail.Mailer;
import com.untitled.ecm.services.mail.MailerFactory;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/sendgrids")
@Produces(MediaType.APPLICATION_JSON)
public class SendGridResource {

    private DakiyaRuntimeSettings dakiyaRuntimeSettings;


    public SendGridResource(DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        // todo find out what is minimum length of key
        if (dakiyaRuntimeSettings == null) {
            throw new InstantiationError("null or empty sendgrid key provided");
        }
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
    }

    @GET
    @Path("/domain")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public Response getSendgridDomains() {
        return Response.status(200).entity(this.dakiyaRuntimeSettings.getAllSendGridDomains()).build();
    }

    @GET
    @Path("/spam-me")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public Message sendMeTestMail(@Context SecurityContext securityContext) {
        String sendGridAPIKey = this.dakiyaRuntimeSettings.getDefaultDomainSendGridAPIKey();
        if (sendGridAPIKey == null || sendGridAPIKey.length() == 0) {
            throw new InternalServerErrorException("check sendgrid api key " + sendGridAPIKey);
        }
        Dak dak = Dak.builder().build();
        dak.setId(-420);
        dak.setMailType(MailType.STATIC);
        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();
        dak.setSubject("Test mail");
        dak.setFrom(new DakEmail("Team Untitled-Dakiya", "teamuntitled.272@gmail.com"));
        dak.setReplyTo(new DakEmail(dakiyaUser.getEmail(), dakiyaUser.getEmail()));
        dak.setContentType("html");
        dak.setContent("<h4>Hello from Dakiya</h4> <p>This is test mail in html</p>" +
                "<p><strong>Few things to notice/review:</strong>" +
                "<br/> Reply address must be your login email" +
                "<br/> Other recipients emails must not be visible to you" +
                "<br/> Unsubscribe message" +
                "<br/>Mail should land in your updates section, not promo section and definitely not spam section" +
                "<br/> Click tracking, open tracking, subscription tracking are enabled, to verify click show original in inbox/gmail" +
                "<br/> Custom tracking is also enabled <a href=\"https://sendgrid.com/docs/API_Reference/SMTP_API/unique_arguments.html\">details</a> (code uses webapi for this)</p>");

        Map<String, String> customTrackingArgs = new HashMap<>();
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_ID, Integer.toString(dak.getId()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_VERSION, Integer.toString(dak.getId()));
        customTrackingArgs.put(DakiyaStrings.DAKIYA_SCHEDULER_JOB_CAMPAIGN_CATEGORY, "sendgrid test mail");
        customTrackingArgs.put(DakiyaStrings.DAKIYA_IS_TEST_MAIL, "yes");

        Mailer sendGridMailer = MailerFactory.getMailer(dakiyaRuntimeSettings.getEnvType(), sendGridAPIKey);
        sendGridMailer.addMailContainer(dak, customTrackingArgs);

        List<DakEmail> dakEmails = new ArrayList<>();
        dakEmails.add(new DakEmail(dakiyaUser.getEmail()));


        if (!sendGridMailer.addRecipients(dakEmails)) {
            throw new InternalServerErrorException(JSONObject.valueToString(sendGridMailer.getErrors()));

        }

        if (!sendGridMailer.sendMails(null, null)) {
            throw new InternalServerErrorException(JSONObject.valueToString(sendGridMailer.getErrors()));
        }

        return new Message("Mail sent to " + dakiyaUser.getEmail());
    }
}
