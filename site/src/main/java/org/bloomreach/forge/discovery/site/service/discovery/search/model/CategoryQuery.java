package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.util.List;
import java.util.Map;

public record CategoryQuery(
        String categoryId,
        int page,
        int pageSize,
        String sort,
        Map<String, List<String>> filters,
        String brUid2,
        String refUrl,
        String url
) {
}
