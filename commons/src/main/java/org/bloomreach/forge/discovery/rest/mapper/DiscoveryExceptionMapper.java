package org.bloomreach.forge.discovery.rest.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.exception.DiscoveryException;

@Provider
public class DiscoveryExceptionMapper implements ExceptionMapper<DiscoveryException> {

    @Override
    public Response toResponse(DiscoveryException exception) {
        int status = exception instanceof ConfigurationException ? 503 : 502;
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(exception.getClass().getSimpleName(), exception.getMessage()))
                .build();
    }
}
