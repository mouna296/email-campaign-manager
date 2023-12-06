package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    /**
     * Map an exception to a {@link Response}. Returning
     * {@code null} results in a {@link Response.Status#NO_CONTENT}
     * response. Throwing a runtime exception results in a
     * {@link Response.Status#INTERNAL_SERVER_ERROR} response.
     *
     * @param exception the exception to map to a response.
     * @return a response mapped from the supplied exception.
     */
    @Override
    public Response toResponse(ForbiddenException exception) {
        log.debug("forbidden exception", exception);
        return Response.status(403).entity(new Message(exception.getMessage())).build();
    }
}
