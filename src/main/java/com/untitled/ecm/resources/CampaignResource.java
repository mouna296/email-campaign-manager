package com.untitled.ecm.resources;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.dao.CampaignDAO;
import com.untitled.ecm.dao.DakDAO;
import com.untitled.ecm.dao.external.RedshiftDao;
import com.untitled.ecm.dtos.ArchivedCampaignWithMail;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.dtos.http.DeliveryBreakdown;
import com.untitled.ecm.dtos.http.SuccessFailureBreakdown;
import com.untitled.ecm.exceptions.InvalidCampaignJsonException;
import com.untitled.ecm.resources.validators.CampaignValidator;
import com.untitled.ecm.services.CampaignService;
import com.untitled.ecm.services.MailService;
import com.untitled.ecm.services.scheduler.SchedulerManager;
import com.untitled.ecm.services.scheduler.SchedulerUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.quartz.SchedulerException;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/campaigns")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class CampaignResource {
    private final CampaignService campaignService;
    private final MailService mailService;
    private final RedshiftDao redshiftDao;
    private final DakiyaRuntimeSettings dakiyaRuntimeSettings;

    public CampaignResource(final CampaignDAO campaignDAO,
                            final DakDAO dakDAO,
                            final RedshiftDao redshiftDao,
                            final DakiyaRuntimeSettings dakiyaRuntimeSettings,
                            final SchedulerManager schedulerManager,
                            final String dakiyaInstanceType) {
        if (campaignDAO == null || dakiyaRuntimeSettings == null || dakDAO == null || schedulerManager == null || redshiftDao == null) {
            throw new InstantiationError("null value provided");
        }
        this.redshiftDao = redshiftDao;
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
        mailService = new MailService(dakDAO, dakiyaRuntimeSettings);
        campaignService = new CampaignService(campaignDAO, mailService, dakiyaInstanceType);
    }


    @GET
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public List<Campaign> findAll(@Context SecurityContext securityContext) {
        return campaignService.getAllCampaigns();
    }

    @GET
    @Path("/archived")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public List<ArchivedCampaignWithMail> findAllArchivedCampaigns(@Context SecurityContext securityContext) {
        return campaignService.getAllArchivedCampaigns();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public Campaign findById(@PathParam("id") int id, @Context SecurityContext sc) {
        return campaignService.getCampaignById(id);
    }


    @POST
    @RolesAllowed({Roles.CAMPAIGN_SUPERVISOR, Roles.SUPER_USER})
    @Consumes(MediaType.APPLICATION_JSON)
    public Campaign createCampaign(@Context HttpServletRequest request, @Context SecurityContext securityContext) {

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        JSONObject newCampaignJsonObject = this.validateAndGetCampaignJson(request);

        return campaignService.createNewCampaign(newCampaignJsonObject, dakiyaUser);
    }

    @POST
    @Path("/{id}")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    @Consumes(MediaType.APPLICATION_JSON)
    public Campaign updateCampaignByID(@PathParam("id") int id, @Context HttpServletRequest request, @Context SecurityContext securityContext) throws SchedulerException {

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        final Campaign oldCampaign = campaignService.getCampaignById(id);
        JSONObject newCampaignJsonObject = this.validateAndGetCampaignJson(request);

        if (!campaignService.archiveCampaign(oldCampaign, dakiyaUser)) {
            throw new InternalServerErrorException("internal db error, could not archive campaign");
        }

        Campaign newCampaign = campaignService.updateCampaign(oldCampaign, newCampaignJsonObject, dakiyaUser);

        SchedulerUtils.unscheduleCampaign((newCampaign.getId()));

        return newCampaign;
    }

    @GET
    @Path("/get-demo-mail/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    @Deprecated
    public Message sendDemoMailOfCampaign(@PathParam("campaign-id") int id, @Context SecurityContext securityContext) {
        Campaign campaign = campaignService.getCampaignById(id);

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        mailService.sendDemoMailOfCampaign(campaign, dakiyaUser);

        return new Message("Mail sent to " + dakiyaUser.getEmail());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/send-test-mails/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public List<String> sendTestMailsForCampaign(@PathParam("campaign-id") int id,
                                                 @Context SecurityContext securityContext,
                                                 List<String> testMailRecipients) {

        Campaign campaign = campaignService.getCampaignById(id);

        return mailService.sendTestMailsForCampaign(campaign,
                testMailRecipients,
                (DakiyaUser) securityContext.getUserPrincipal());
    }

    @POST
    @Path("/approve/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public Message approveCampaignById(@PathParam("campaign-id") int id, @Context SecurityContext securityContext) {
        Campaign campaign = campaignService.getCampaignById(id);

        if (campaign.getState().equals(CampaignStates.APPROVED)) {
            return new Message("campaign id " + Integer.toString(id) + " is already approved");
        }

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        campaignService.changeCampaignStateToApproved(campaign.getId(), dakiyaUser);
        return new Message("campaign id " + Integer.toString(id) + " approved");
    }

    @POST
    @Path("/archive/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public Message archiveCampaignById(@PathParam("campaign-id") int id,
                                       @Context SecurityContext securityContext) throws SchedulerException {
        Campaign campaign = campaignService.getCampaignById(id);

        SchedulerUtils.unscheduleCampaign((campaign.getId()));

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();

        if (!campaignService.archiveCampaign(campaign, dakiyaUser)) {
            throw new InternalServerErrorException("internal db error, could not copy campaign to archives");
        }

        campaignService.deleteCampaignById(id);

        return new Message("campaign id " + Integer.toString(id) + " deleted");
    }

    @GET
    @Path("{campaign-id}/analytics/delivery-breakdown")
    @RolesAllowed({Roles.CAMPAIGN_MANAGER, Roles.CAMPAIGN_SUPERVISOR})
    public DeliveryBreakdown getCampaignDeliveryBreakdownAnalytics(@PathParam("campaign-id") int id) {
        return campaignService.getDeliveryBreakdown(id);
    }

    @GET
    @Path("{campaign-id}/analytics/success-failure-breakdown")
    @RolesAllowed({Roles.CAMPAIGN_MANAGER, Roles.CAMPAIGN_SUPERVISOR})
    public SuccessFailureBreakdown getSuccessFailureBreakdown(@Context SecurityContext context, @PathParam("campaign-id") int id) {
        DakiyaUser dakiyaUser = (DakiyaUser) context.getUserPrincipal();
        return campaignService.getSuccessFailureBreakdown(id);
    }


    private JSONObject validateAndGetCampaignJson(HttpServletRequest httpServletRequest) {
        CampaignValidator campaignValidator;
        try {
            campaignValidator = new CampaignValidator(httpServletRequest.getInputStream(),
                    this.redshiftDao, this.dakiyaRuntimeSettings);
        } catch (Exception ex) {
            throw new BadRequestException("incorrect input");
        }

        if (!campaignValidator.validate()) {
            throw new InvalidCampaignJsonException(campaignValidator.getErrorList());
        }

        JSONObject newCampaignJSONObject = campaignValidator.getValidatedJSONObject();

        if (newCampaignJSONObject == null) {
            throw new InternalServerErrorException("could not validate json");
        }

        return newCampaignJSONObject;
    }
}
