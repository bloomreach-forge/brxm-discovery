package org.bloomreach.forge.discovery.visual.model;

public record VisualSearchQuery(
        String widgetId,
        String imageId,
        String objectId,
        int rows,
        String fields,
        String url,
        String refUrl,
        String brUid2) {}
