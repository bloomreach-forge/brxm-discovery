package org.bloomreach.forge.discovery.site.service.discovery.search.model;

public record Campaign(
        String id,
        String name,
        String htmlText,
        String bannerUrl,
        String imageUrl
) {}
