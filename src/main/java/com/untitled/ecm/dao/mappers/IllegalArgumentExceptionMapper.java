package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.Message;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(400).entity(new Message(exception.getMessage())).build();
    }
}
