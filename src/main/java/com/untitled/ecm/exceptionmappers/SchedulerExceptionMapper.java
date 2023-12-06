package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class SchedulerExceptionMapper implements ExceptionMapper<SchedulerException> {
    @Override
    public Response toResponse(SchedulerException exception) {
        if (exception instanceof org.quartz.ObjectAlreadyExistsException) {
            return Response.status(409).entity(new Message(exception.getMessage())).build();
        }
        log.error("scheduler exception occurred", exception);
        return Response.status(500).entity(new Message(exception.getMessage())).build();
    }
}
