package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;

public interface DiscoveryPixelTransport {

    String buildPath(PixelEvent event, DiscoveryCredentials credentials, String clientIp, PixelFlags flags);

    void fire(String path, ClientContext ctx, PixelFlags flags);
}
