package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.exceptions.InvalidCampaignJsonException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class InvalidCampaignJsonExceptionMapper implements ExceptionMapper<InvalidCampaignJsonException> {
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
    public Response toResponse(InvalidCampaignJsonException exception) {
        return Response.status(400).entity(exception.getErrors()).build();
    }
}
