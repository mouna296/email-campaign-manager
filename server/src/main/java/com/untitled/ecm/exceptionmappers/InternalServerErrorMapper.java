package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class InternalServerErrorMapper implements ExceptionMapper<InternalServerErrorException> {
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
    public Response toResponse(InternalServerErrorException exception) {
        log.error("internal server exception", exception);
        return Response.status(500).entity(new Message(exception.getMessage())).build();
    }
}
