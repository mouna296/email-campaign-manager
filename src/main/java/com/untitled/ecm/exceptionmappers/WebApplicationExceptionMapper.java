package com.untitled.ecm.exceptionmappers;


import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

@Slf4j
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

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
    public Response toResponse(WebApplicationException exception) {
        log.info("Exception in processing request", exception);

        int statusCode;
        if (exception.getResponse().getStatus() > 0) {
            statusCode = exception.getResponse().getStatus();
        } else {
            statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }

        Response.Status status = Response.Status.fromStatusCode(statusCode);

        if (status.getStatusCode() >= 500) {
            log.error("web app exception", exception);
        } else {
            log.info("web app exception", exception);
        }

        return Response.status(status)
                .entity(new Message(exception.getMessage()))
                .build();
    }
}
