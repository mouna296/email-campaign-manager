package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class NotFoundExeceptionMapper implements ExceptionMapper<NotFoundException> {
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
    public Response toResponse(NotFoundException exception) {
        log.debug("not found", exception);
        return Response.status(404).entity(new Message(exception.getMessage())).build();
    }
}
