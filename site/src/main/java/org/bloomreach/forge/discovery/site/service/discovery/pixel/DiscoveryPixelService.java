package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;

/**
 * Fires server-side Discovery pixel events asynchronously.
 * Implementations must swallow all errors - pixel failure must never affect page rendering.
 * <p>
 * Callers must pass a resolved {@link PixelFlags} instance; passing {@link PixelFlags#DISABLED}
 * guarantees that no pixel traffic is produced regardless of env/channel configuration.
 */
public interface DiscoveryPixelService {

    void fire(PixelEvent event, DiscoveryCredentials credentials, String clientIp,
              ClientContext ctx, PixelFlags flags);
}
