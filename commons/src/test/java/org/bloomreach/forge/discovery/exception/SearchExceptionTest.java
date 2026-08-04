package org.bloomreach.forge.discovery.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchExceptionTest {

    @Test
    void messageOnlyConstructor_hasSentinelStatusCode() {
        SearchException ex = new SearchException("boom");
        assertEquals(-1, ex.statusCode());
    }

    @Test
    void messageAndCauseConstructor_hasSentinelStatusCode() {
        SearchException ex = new SearchException("boom", new RuntimeException("cause"));
        assertEquals(-1, ex.statusCode());
        assertEquals("cause", ex.getCause().getMessage());
    }

    @Test
    void messageAndStatusCodeConstructor_exposesStatusCode() {
        SearchException ex = new SearchException("HTTP 503", 503);
        assertEquals(503, ex.statusCode());
        assertEquals("HTTP 503", ex.getMessage());
        assertNull(ex.getCause());
    }
}
