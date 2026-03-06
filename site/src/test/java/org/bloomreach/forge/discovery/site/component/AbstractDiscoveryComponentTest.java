package org.bloomreach.forge.discovery.site.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AbstractDiscoveryComponent} utility methods.
 * Config resolution + caching is tested through the concrete component integration
 * since mocking HST request/context types requires the servlet API at test scope.
 */
class AbstractDiscoveryComponentTest {

    @Test
    void parseIntOrDefault_validNumber_returnsIt() {
        assertEquals(42, AbstractDiscoveryComponent.parseIntOrDefault("42", 10));
    }

    @Test
    void parseIntOrDefault_null_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault(null, 10));
    }

    @Test
    void parseIntOrDefault_blank_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault("  ", 10));
    }

    @Test
    void parseIntOrDefault_invalid_returnsDefault() {
        assertEquals(10, AbstractDiscoveryComponent.parseIntOrDefault("abc", 10));
    }

    @Test
    void parseIntOrDefault_negativeNumber_returnsIt() {
        assertEquals(-5, AbstractDiscoveryComponent.parseIntOrDefault("-5", 10));
    }

    @Test
    void parseIntOrDefault_whitespace_trimmed() {
        assertEquals(7, AbstractDiscoveryComponent.parseIntOrDefault("  7  ", 10));
    }
}
