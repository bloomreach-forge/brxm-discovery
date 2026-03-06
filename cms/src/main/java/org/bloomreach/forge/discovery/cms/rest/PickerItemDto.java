package org.bloomreach.forge.discovery.cms.rest;

public record PickerItemDto(
        String id,
        String title,
        String imageUrl,
        String url,
        String price
) {
}
