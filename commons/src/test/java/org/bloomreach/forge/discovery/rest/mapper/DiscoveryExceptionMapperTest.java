package org.bloomreach.forge.discovery.rest.mapper;

import jakarta.ws.rs.core.Response;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.exception.RecommendationException;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryExceptionMapperTest {

    private final DiscoveryExceptionMapper mapper = new DiscoveryExceptionMapper();

    @Test
    void configurationException_returns503() {
        try (Response r = mapper.toResponse(new ConfigurationException("no config"))) {
            assertEquals(503, r.getStatus());
            ApiError error = (ApiError) r.getEntity();
            assertEquals("ConfigurationException", error.code());
            assertEquals("no config", error.message());
        }
    }

    @Test
    void searchException_returns502() {
        try (Response r = mapper.toResponse(new SearchException("upstream down"))) {
            assertEquals(502, r.getStatus());
            ApiError error = (ApiError) r.getEntity();
            assertEquals("SearchException", error.code());
        }
    }

    @Test
    void recommendationException_returns502() {
        try (Response r = mapper.toResponse(new RecommendationException("widget error"))) {
            assertEquals(502, r.getStatus());
            ApiError error = (ApiError) r.getEntity();
            assertEquals("RecommendationException", error.code());
        }
    }
}
