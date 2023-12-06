package com.untitled.ecm.resources;

import com.untitled.ecm.constants.MailType;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.core.DakiyaUtils;
import com.untitled.ecm.dao.DakiyaUserDAO;
import com.untitled.ecm.dao.DakiyaUserDetailsDAO;
import com.untitled.ecm.dtos.Dak;
import com.untitled.ecm.dtos.DakEmail;
import com.untitled.ecm.dtos.DakiyaUserDetails;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.dtos.http.CreateUserRequest;
import com.untitled.ecm.dtos.http.LoginRequest;
import com.untitled.ecm.services.mail.Mailer;
import com.untitled.ecm.services.mail.MailerFactory;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;


@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final DakiyaUserDetailsDAO dakiyaUserDetailsDAO;
    private final DakiyaRuntimeSettings dakiyaRuntimeSettings;
    private final DakiyaUserDAO dakiyaUserDAO;
    private final String dakiyaInstanceType;


    public UserResource(DakiyaUserDetailsDAO dakiyaUserDetailsDAO, DakiyaRuntimeSettings dakiyaRuntimeSettings,
                        DakiyaUserDAO dakiyaUserDAO, String dakiyaInstanceType) {
        if (dakiyaUserDetailsDAO == null || dakiyaRuntimeSettings == null || dakiyaUserDAO == null) {
            throw new InstantiationError("null value provided");
        }
        this.dakiyaUserDetailsDAO = dakiyaUserDetailsDAO;
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
        this.dakiyaUserDAO = dakiyaUserDAO;
        this.dakiyaInstanceType = dakiyaInstanceType;
    }

    @GET
    @RolesAllowed({Roles.CAMPAIGN_MANAGER, Roles.CAMPAIGN_SUPERVISOR})
    public List<DakiyaUserDetails> getAllUserDetails() {
        return dakiyaUserDetailsDAO.findAll();
    }

    /**
     * All user created via this api will be {@link Roles.CAMPAIGN_MANAGER}
     *
     * @param request
     * @return
     */
    @POST
    public DakiyaUserDetails createUser(@Valid @NotNull CreateUserRequest request) {
        dakiyaUserDAO.createDakiyaUser(request.getEmail(),
                DakiyaUtils.getBcryptHashedString(request.getPassword()), Roles.CAMPAIGN_MANAGER);
        dakiyaUserDetailsDAO.createDakiyaUserDetails(request.getFirstName(),
                request.getLastName(), request.getEmail());

        return dakiyaUserDetailsDAO.findByEmail(request.getEmail());
    }

    @GET
    @Path("/{email}")
    @RolesAllowed(Roles.SUPER_USER)
    public DakiyaUserDetails getUserDetailsByEmail(@PathParam("email") String email) {
        DakiyaUserDetails dakiyaUserDetails = dakiyaUserDetailsDAO.findByEmail(email);
        if (dakiyaUserDetails != null) {
            return dakiyaUserDetails;
        } else {
            throw new NotFoundException("no such user exist");
        }
    }


    @POST
    @Path("/login-and-get-details")
    public DakiyaUserDetails loginAndGetDetails(@Valid @NotNull LoginRequest loginRequest) {


        String storedHashedPassword = this.dakiyaUserDAO.getHashedPasswordByEmail(loginRequest.getEmail());
        if (storedHashedPassword == null) {
            throw new NotAuthorizedException("Invalid Credentials");
        } else if (!BCrypt.isPasswordMatch(loginRequest.getPassword(), storedHashedPassword)) {
            throw new NotAuthorizedException("Invalid Credentials");
        }

        return dakiyaUserDetailsDAO.findByEmail(loginRequest.getEmail());
    }

    @POST
    @Path(("/signup-and-get-details"))
    public DakiyaUserDetails signup(@Valid @NotNull CreateUserRequest request) {
        dakiyaUserDAO.createDakiyaUser(request.getEmail(),
                DakiyaUtils.getBcryptHashedString(request.getPassword()), Roles.CAMPAIGN_MANAGER);
        dakiyaUserDetailsDAO.createDakiyaUserDetails(request.getFirstName(),
                request.getLastName(), request.getEmail());

        return dakiyaUserDetailsDAO.findByEmail(request.getEmail());
    }

    @POST
    @Path("/reset-my-password")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public Message resetMyPassword(@Context SecurityContext securityContext) {
        final String sendGridAPIKey = this.dakiyaRuntimeSettings.getDefaultDomainSendGridAPIKey();
        if (sendGridAPIKey == null || sendGridAPIKey.length() == 0) {
            throw new InternalServerErrorException("check sendgrid api key " + sendGridAPIKey);
        }

        final DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        final String clearTextPassword = DakiyaUtils.getRandomAlphaNumericString();

        final int rowModified = dakiyaUserDAO.updateHashedPassword(dakiyaUser.getEmail(), DakiyaUtils.getBcryptHashedString(clearTextPassword));
        if (rowModified != 1) {
            throw new InternalServerErrorException("expected row to be modified: 1, found " + rowModified);
        }

        sendPasswordChangeMail(sendGridAPIKey, dakiyaUser.getEmail(), clearTextPassword, dakiyaInstanceType);

        return new Message("password reset and sent in a mail to " + dakiyaUser.getEmail());
    }

    @POST
    @Path("/reset-password/{email}")
    @RolesAllowed(Roles.SUPER_USER)
    public Message resetUserPassword(@Nonnull @PathParam("email") String email) {
        final String sendGridAPIKey = this.dakiyaRuntimeSettings.getDefaultDomainSendGridAPIKey();
        if (sendGridAPIKey == null || sendGridAPIKey.length() == 0) {
            throw new InternalServerErrorException("check sendgrid api key " + sendGridAPIKey);
        }

        if (StringUtils.isEmpty(email) || StringUtils.isWhitespace(email)) {
            throw new BadRequestException("invalid email");
        }

        DakiyaUserDetails dakiyaUserDetails = dakiyaUserDetailsDAO.findByEmail(email);
        if (dakiyaUserDetails == null) {
            throw new NotFoundException("no such user exist");
        }

        final String clearTextPassword = DakiyaUtils.getRandomAlphaNumericString();

        final int rowModified = dakiyaUserDAO.updateHashedPassword(email, DakiyaUtils.getBcryptHashedString(clearTextPassword));

        if (rowModified != 1) {
            throw new InternalServerErrorException("expected row to be modified: 1, found " + rowModified);
        }

        sendPasswordChangeMail(sendGridAPIKey, email, clearTextPassword, dakiyaInstanceType);

        return new Message("password reset and sent in a mail to " + email);
    }

    private void sendPasswordChangeMail(String sendGridAPIKey, String email,
                                        String clearTextPassword, String dakiyaInstanceType) {
        final Mailer mailer = MailerFactory.getMailer(dakiyaRuntimeSettings.getEnvType(), sendGridAPIKey);
        mailer.addMailContainer(generateMailForPasswordReset(email,
                clearTextPassword, dakiyaInstanceType), null);

        if (!mailer.addRecipient(new DakEmail(email))) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }

        if (!mailer.sendMails(null, null)) {
            throw new InternalServerErrorException(JSONObject.valueToString(mailer.getErrors()));
        }
    }

    @GET
    @Path("/me")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public DakiyaUserDetails getMyDetails(@Context SecurityContext sc) {
        DakiyaUser dakiyaUser = (DakiyaUser) sc.getUserPrincipal();
        return dakiyaUserDetailsDAO.findByEmail(dakiyaUser.getEmail());
    }

    private Dak generateMailForPasswordReset(String email, String clearTextPassword, String dakiyaInstanceType) {
        return Dak
                .builder()
                .id(-421)
                .mailType(MailType.STATIC).subject("Password for " + email + " of Dakiya: " + dakiyaInstanceType)
                .from(new DakEmail("no-reply-Team Untitled", "teamuntitled.272@gmail.com"))
                // don't send any email to a recipient can reply to, otherwise a password might get exposed to outside world
                .replyTo(new DakEmail(email, email))
                .contentType("html")
                .content("<h4>This is an automatically generated mail</h4>" +
                        "<p> Password Reset requested for email:" + email + "</p>" +
                        "<p> New password for " + email + " is <strong>" + clearTextPassword + "</strong></p>" +
                        "<p><strong> This password is for instance type " + dakiyaInstanceType + "</strong></p>" +
                        "<p> if you did not initiate this request contact dakiya maintainer</p>")
                .build();
    }

}
