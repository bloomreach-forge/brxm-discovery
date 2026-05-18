package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window token bucket: allows up to {@code maxPerSecond} pixel fires per JVM per second.
 * On exhaustion, callers drop the event silently (analytics are best-effort).
 * Call {@link #drain()} when Discovery returns HTTP 429 to back off for the remainder of the window.
 */
public final class PixelRateLimiter implements AutoCloseable {

    private final int maxPerSecond;
    private final AtomicInteger tokens;
    private final ScheduledExecutorService refiller;

    public PixelRateLimiter(int maxPerSecond) {
        if (maxPerSecond <= 0) throw new IllegalArgumentException("maxPerSecond must be > 0");
        this.maxPerSecond = maxPerSecond;
        this.tokens = new AtomicInteger(maxPerSecond);
        this.refiller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "brxdis-pixel-ratelimit");
            t.setDaemon(true);
            return t;
        });
        refiller.scheduleAtFixedRate(() -> tokens.set(maxPerSecond), 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Attempts to consume one token. Returns {@code true} if the event may be sent,
     * {@code false} if the rate limit is currently exhausted.
     */
    public boolean tryAcquire() {
        int current;
        do {
            current = tokens.get();
            if (current <= 0) return false;
        } while (!tokens.compareAndSet(current, current - 1));
        return true;
    }

    /**
     * Drains all remaining tokens, forcing a backoff until the next refill (~1 second).
     * Call this when Discovery responds with HTTP 429.
     */
    public void drain() {
        tokens.set(0);
    }

    public int maxPerSecond() {
        return maxPerSecond;
    }

    @Override
    public void close() {
        refiller.shutdownNow();
    }
}
