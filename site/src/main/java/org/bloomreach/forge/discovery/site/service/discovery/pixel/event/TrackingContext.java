package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

public record TrackingContext(
        String brUid2,
        String refUrl,
        String origRefUrl,
        String url,
        String title) {}
