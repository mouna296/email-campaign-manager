package com.untitled.ecm.resources;

import com.untitled.ecm.constants.Roles;
import com.untitled.ecm.core.DakiyaRuntimeSettings;
import com.untitled.ecm.dtos.DakiyaSetting;
import com.untitled.ecm.dtos.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.NoSuchElementException;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(Roles.SUPER_USER)
public class DakiyaSettingResource {
    private DakiyaRuntimeSettings dakiyaRuntimeSettings;
    private Logger logger;

    public DakiyaSettingResource(DakiyaRuntimeSettings dakiyaRuntimeSettings) {
        if (dakiyaRuntimeSettings == null) {
            throw new InstantiationError("null value provided");
        }
        this.dakiyaRuntimeSettings = dakiyaRuntimeSettings;
        this.logger = LoggerFactory.getLogger(DakiyaSetting.class);
    }

    @GET
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public List<DakiyaSetting> getAllSettings() {
        return dakiyaRuntimeSettings.getAllSettings();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Message saveSetting(@FormParam("key") String key, @FormParam("value") String value) {
        if (key == null || value == null) {
            throw new BadRequestException("key and/or value not provided");
        }
        this.dakiyaRuntimeSettings.saveSettingByKey(key, value);
        return new Message(key + ":" + value + " saved");
    }

    @GET
    @Path("/{key}")
    @RolesAllowed(Roles.CAMPAIGN_MANAGER)
    public String getSettingByKey(@PathParam("key") String key) {
        try {
            return dakiyaRuntimeSettings.getSettingByKey(key);
        } catch (NoSuchElementException e) {
            this.logger.error(e.getMessage());
            throw new NotFoundException("no such key exists");
        }
    }

    @POST
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Message updateSetting(@PathParam("key") String key, @FormParam("value") String value) {
        if (key == null || value == null) {
            throw new BadRequestException("key and/or value not provided");
        }

        try {
            this.dakiyaRuntimeSettings.getSettingByKey(key);
        } catch (NoSuchElementException e) {
            throw new NotFoundException("no such settings found");
        }

        this.dakiyaRuntimeSettings.saveSettingByKey(key, value);
        return new Message(key + ":" + value + " updated");
    }

}
