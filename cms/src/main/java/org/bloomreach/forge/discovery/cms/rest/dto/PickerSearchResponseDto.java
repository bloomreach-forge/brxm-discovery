package org.bloomreach.forge.discovery.cms.rest.dto;

import java.util.List;

public record PickerSearchResponseDto(
        List<PickerItemDto> items,
        long total,
        int page,
        int pageSize
) {
}
