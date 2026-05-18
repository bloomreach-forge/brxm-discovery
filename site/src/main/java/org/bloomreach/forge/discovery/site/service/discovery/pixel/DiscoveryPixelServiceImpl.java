package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryPixelTransport;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public class DiscoveryPixelServiceImpl implements DiscoveryPixelService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPixelServiceImpl.class);

    private final DiscoveryPixelTransport transport;
    private final Executor executor;

    public DiscoveryPixelServiceImpl(DiscoveryPixelTransport transport, Executor executor) {
        this.transport = transport;
        this.executor = executor;
    }

    @Override
    public void fire(PixelEvent event, DiscoveryCredentials credentials, String clientIp,
                     ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        if (!PixelUserAgentPolicy.isAllowed(ctx.userAgent())) {
            if (PixelUserAgentPolicy.shouldWarn(ctx.userAgent())) {
                log.warn("Discovery pixel event skipped: UA '{}' is a server-side HTTP client. "
                        + "SSR callers must forward the browser UA via X-Forwarded-User-Agent.", ctx.userAgent());
            }
            return;
        }
        log.debug("Dispatching pixel event [type={}]", event.getClass().getSimpleName());
        ClientContext effectiveCtx = enrichXff(ctx, clientIp);
        submitQuietly(() -> {
            String path = transport.buildPath(event, credentials, clientIp, flags);
            fireQuietly(path, effectiveCtx, flags);
        });
    }

    private static ClientContext enrichXff(ClientContext ctx, String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return ctx;
        if (ctx.xForwardedFor() != null && !ctx.xForwardedFor().isBlank()) return ctx;
        return new ClientContext(ctx.userAgent(), ctx.acceptLanguage(), clientIp);
    }

    private void submitQuietly(Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.warn("Discovery pixel event failed before send: {}", e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Discovery pixel event dropped: executor rejected task: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Discovery pixel event submission failed: {}", e.getMessage());
        }
    }

    private void fireQuietly(String path, ClientContext ctx, PixelFlags flags) {
        try {
            transport.fire(path, ctx, flags);
        } catch (Exception e) {
            log.warn("Discovery pixel event failed: {}", e.getMessage());
        }
    }
}
