package org.bloomreach.forge.discovery.request;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class DiscoveryCoreRequestFactory {

    public static final String CORE_PATH = "/api/v1/core/";
    public static final String DEFAULT_FIELDS = "pid,title,brand,price,sale_price,thumb_image,url,description";

    private static final String REQUEST_TYPE_SEARCH = "search";
    private static final String SEARCH_TYPE_KEYWORD = "keyword";
    private static final String SEARCH_TYPE_CATEGORY = "category";

    private final Supplier<String> requestIdSupplier;

    public DiscoveryCoreRequestFactory() {
        this(() -> UUID.randomUUID().toString());
    }

    DiscoveryCoreRequestFactory(Supplier<String> requestIdSupplier) {
        this.requestIdSupplier = Objects.requireNonNull(requestIdSupplier);
    }

    public DiscoveryRequestSpec search(SearchQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_KEYWORD)
                .queryParam("q", query.query() != null ? query.query() : "*")
                .queryParam("request_id", nextRequestId())
                .queryParam("fl", DEFAULT_FIELDS)
                .queryParamIfNotBlank("catalog_name", query.catalogName());

        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        return builder.build();
    }

    public DiscoveryRequestSpec category(CategoryQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_CATEGORY)
                .queryParam("q", query.categoryId() != null ? query.categoryId() : "")
                .queryParam("request_id", nextRequestId())
                .queryParam("fl", DEFAULT_FIELDS);

        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        return builder.build();
    }

    public DiscoveryRequestSpec productLookup(String pid, String url, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_KEYWORD)
                .queryParam("request_id", nextRequestId())
                .queryParam("q", "*")
                .queryParam("fl", DEFAULT_FIELDS)
                .queryParam("rows", 1)
                .queryParamIfNotBlank("url", url);

        if (pid != null && !pid.isBlank()) {
            builder.queryParam("efq", "pid:(" + pid + ")");
        }
        return builder.build();
    }

    private DiscoveryRequestSpec.Builder baseCoreRequest(DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = DiscoveryRequestSpec.builder(CORE_PATH)
                .queryParam("account_id", credentials.accountId())
                .queryParam("domain_key", credentials.domainKey());
        if (credentials.apiKey() != null && !credentials.apiKey().isBlank()) {
            builder.queryParam("auth_key", credentials.apiKey());
        }
        return builder;
    }

    private String nextRequestId() {
        return requestIdSupplier.get();
    }

    private static void appendTracking(DiscoveryRequestSpec.Builder builder, String brUid2, String refUrl, String url) {
        builder.queryParamIfNotBlank("_br_uid_2", brUid2)
                .queryParamIfNotBlank("ref_url", refUrl)
                .queryParamIfNotBlank("url", url);
    }

    private static void appendPagination(DiscoveryRequestSpec.Builder builder, int page, int pageSize) {
        builder.queryParam("start", (long) page * pageSize)
                .queryParam("rows", pageSize);
    }

    private static void appendSort(DiscoveryRequestSpec.Builder builder, String sort) {
        builder.queryParamIfNotBlank("sort", sort);
    }

    private static void appendStatsFields(DiscoveryRequestSpec.Builder builder, List<String> statsFields) {
        if (statsFields == null) {
            return;
        }
        statsFields.stream()
                .filter(field -> field != null && !field.isBlank())
                .forEach(field -> builder.queryParam("stats.field", field));
    }

    private static void appendSegment(DiscoveryRequestSpec.Builder builder, String segment) {
        builder.queryParamIfNotBlank("segment", segment);
    }

    private static void appendEfq(DiscoveryRequestSpec.Builder builder, String efq) {
        builder.queryParamIfNotBlank("efq", efq);
    }

    private static void appendFilters(DiscoveryRequestSpec.Builder builder, Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            for (String value : entry.getValue()) {
                builder.queryParam("fq", entry.getKey() + ":\"" + value + "\"");
            }
        }
    }
}
