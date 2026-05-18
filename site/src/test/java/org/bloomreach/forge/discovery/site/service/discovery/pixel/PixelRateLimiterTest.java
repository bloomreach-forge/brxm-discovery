package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelRateLimiterTest {

    @Test
    void tryAcquire_consumesToken() {
        var limiter = new PixelRateLimiter(5);
        assertTrue(limiter.tryAcquire());
        limiter.close();
    }

    @Test
    void tryAcquire_returnsFalseWhenExhausted() {
        var limiter = new PixelRateLimiter(3);
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        limiter.close();
    }

    @Test
    void drain_preventsSubsequentAcquire() {
        var limiter = new PixelRateLimiter(100);
        assertTrue(limiter.tryAcquire());
        limiter.drain();
        assertFalse(limiter.tryAcquire());
        limiter.close();
    }

    @Test
    void constructor_rejectsNonPositiveRate() {
        assertThrows(IllegalArgumentException.class, () -> new PixelRateLimiter(0));
        assertThrows(IllegalArgumentException.class, () -> new PixelRateLimiter(-1));
    }

    @Test
    void maxPerSecond_returnsConfiguredValue() {
        var limiter = new PixelRateLimiter(42);
        assertEquals(42, limiter.maxPerSecond());
        limiter.close();
    }
}
