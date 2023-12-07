package com.untitled.ecm.exceptionmappers;

import com.untitled.ecm.dtos.Message;
import lombok.extern.slf4j.Slf4j;
import org.skife.jdbi.v2.exceptions.DBIException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * jdbi exceptions contain entire sql which are not ideal to send over http
 * but it is best way now to debug thus avoid removing them for now
 */
@Slf4j
public class JdbiExceptionMapper implements ExceptionMapper<org.skife.jdbi.v2.exceptions.DBIException> {
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
    public Response toResponse(DBIException exception) {
        if (exception instanceof UnableToExecuteStatementException) {
            final String errorMesg = exception.getMessage();
            if (errorMesg.contains("ERROR: duplicate key")) {
                return Response
                        .status(Response.Status.CONFLICT)
                        .entity(new Message(errorMesg))
                        .build();
            }
        }
        log.error("db error", exception);
        return Response.status(500)
                .entity(new Message("internal db error: " + exception.getMessage()))
                .build();
    }
}
