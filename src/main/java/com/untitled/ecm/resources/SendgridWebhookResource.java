package com.untitled.ecm.resources;


import com.untitled.ecm.dao.LogDAO;
import com.untitled.ecm.dtos.http.SendgridEventHookDto;
import lombok.AllArgsConstructor;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/sendgrid/")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor
public class SendgridWebhookResource {

    LogDAO logDAO;

    @POST
    @Path("event-hook/track")
    public Response receiveSendgridWebhook(List<SendgridEventHookDto> sendgridEventHookDtos) {

        for (SendgridEventHookDto sendgridEventHookDto : sendgridEventHookDtos) {
            logDAO.logAndUpdateMailEvents(sendgridEventHookDto.getEvent(), Integer.parseInt(sendgridEventHookDto.getDakiyaSchedulerJobCampaignID()), sendgridEventHookDto.getEmail());
        }
        return Response.ok().build();
    }

}
