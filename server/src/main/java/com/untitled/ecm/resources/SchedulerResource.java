package com.untitled.ecm.resources;

import com.untitled.ecm.constants.CampaignStates;
import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.core.DakiyaUser;
import com.untitled.ecm.dao.CampaignDAO;
import com.untitled.ecm.dao.DakDAO;
import com.untitled.ecm.dtos.Campaign;
import com.untitled.ecm.dtos.Message;
import com.untitled.ecm.services.CampaignService;
import com.untitled.ecm.services.MailService;
import com.untitled.ecm.services.scheduler.SchedulerManager;
import com.untitled.ecm.services.scheduler.SchedulerUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.List;

@Path("/schedulers")
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerResource {
    private final CampaignService campaignService;
    private final CampaignDAO campaignDAO;

    public SchedulerResource(final CampaignDAO campaignDAO,
                             final DakDAO dakDAO,
                             final DakiyaRuntimeSettings dakiyaRuntimeSettings,
                             final String dakiyaInstanceType) {
        if (campaignDAO == null) {
            throw new InstantiationError("null value provided");
        }
        this.campaignDAO = campaignDAO;
        this.campaignService = new CampaignService(campaignDAO,
                new MailService(dakDAO, dakiyaRuntimeSettings),
                dakiyaInstanceType);
    }

    @GET
    @Path("/status")
    @PermitAll // authentication is still required
    public Message isStarted() throws SchedulerException {
        final Scheduler scheduler = SchedulerManager.getQuartzScheduler();
        final String msg;
        if (scheduler.isStarted()) {
            msg = scheduler.getSchedulerName() + " is running";
        } else {
            msg = scheduler.getSchedulerName() + " is not running";
        }
        return new Message(msg);
    }

    @GET
    @Path("/campaigns")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public HashMap<String, HashMap<String, Object>> listAllCampaignsJobs() throws SchedulerException {
        return SchedulerUtils.getAllCampaignJobsDetails();
    }

    @GET
    @Path("/campaigns/{id}")
    @RolesAllowed(Roles.CAMPAIGN_SUPERVISOR)
    public HashMap<String, Object> listCampaignJobByID(@PathParam("id") int id) {
        HashMap<String, Object> jobDetails = SchedulerUtils.getJobDetailsByCampaignID(id);
        if (jobDetails.containsKey("error")) {
            throw new NotFoundException(jobDetails.get("error").toString());
        }
        return jobDetails;
    }

    @POST
    @Path("/execute-campaign-now/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public Message scheduleCampaignNow(@PathParam("campaign-id") int id,
                                       @Context SecurityContext securityContext) throws SchedulerException {

        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();
        final Campaign campaign = campaignService.getCampaignById(id);

        if (!campaign.getState().equals(CampaignStates.APPROVED) && dakiyaUser.getRoles().contains(Roles.CAMPAIGN_MANAGER)) {
            campaignService.changeCampaignStateToApproved(campaign.getId(), dakiyaUser);
        }

        SchedulerUtils.triggerRunCampaignJob(campaign);

        return new Message("job scheduled for campaign " + id);
    }

    @POST
    @Path("/unschedule-campaign/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public Message unscheduleCampaign(@PathParam("campaign-id") int id) throws SchedulerException {

        Campaign campaign = campaignService.getCampaignById(id);
        campaignDAO.updateCampaignScheduled(0, campaign.getId());

        SchedulerUtils.unscheduleCampaign(id);

        return new Message(id + " unscheduled ");

    }

    @POST
    @Path("/unschedule-category/{category}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public List<Integer> unscheduleCategory(@PathParam("category") String category) throws SchedulerException {
        final List<Integer> filteredCampaignsIdsByCategory = campaignService.getAllCampaignIdsByCategory(category);

        SchedulerUtils.unscheduleMultipleCampaigns(filteredCampaignsIdsByCategory);

        return filteredCampaignsIdsByCategory;
    }

    @POST
    @Path("/schedule-campaign/{campaign-id}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public HashMap<String, Object> scheduleCampaign(@PathParam("campaign-id") int id,
                                                    @QueryParam("ignore-state") boolean ignoreState,
                                                    @Context SecurityContext securityContext) throws SchedulerException {
        Campaign campaign;
        DakiyaUser dakiyaUser = (DakiyaUser) securityContext.getUserPrincipal();
        campaign = campaignService.getCampaignById(id);
        campaignDAO.updateCampaignScheduled(1, campaign.getId());

        if (campaign == null) {
            throw new NotFoundException("no such campaign exist");
        }
        if (!campaign.getState().equals(CampaignStates.APPROVED)) {
            if (ignoreState && dakiyaUser.getRoles().contains(Roles.CAMPAIGN_MANAGER)) {
                // this is being called by campaign manager, approve this campaign implicitly
                campaignService.changeCampaignStateToApproved(campaign.getId(), dakiyaUser);
            } else {
                throw new BadRequestException("campaign is not approved," +
                        " pass query param ignore-state while logged in as campaign_manager as true to " +
                        " force creating schedule");
            }
        }

        SchedulerUtils.scheduleCampaign(campaign);

        return SchedulerUtils.getJobDetailsByCampaignID(id);
    }
}
