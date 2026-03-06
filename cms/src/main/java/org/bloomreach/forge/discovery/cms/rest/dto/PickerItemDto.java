package org.bloomreach.forge.discovery.cms.rest.dto;

public record PickerItemDto(
        String id,
        String title,
        String imageUrl,
        String url,
        String price
) {
}
