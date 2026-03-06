package org.bloomreach.forge.discovery.cms.rest;

import java.util.List;

public record PickerSearchResponse(
        List<PickerItemDto> items,
        long total,
        int page,
        int pageSize
) {
}
