package org.bloomreach.forge.discovery.request;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.RangeSelection;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class DiscoveryRequestFactory {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRequestFactory.class);

    public static final String CORE_PATH = "/api/v1/core/";
    public static final String AUTOSUGGEST_PATH = "/api/v2/suggest/";
    public static final String WIDGETS_PATH = "/api/v1/merchant/widgets";
    public static final String RECOMMENDATION_PATH = "/api/v2/widgets";

    private static final String REQUEST_TYPE_SEARCH = "search";
    private static final String REQUEST_TYPE_SUGGEST = "suggest";
    private static final String SEARCH_TYPE_KEYWORD = "keyword";
    private static final String SEARCH_TYPE_CATEGORY = "category";
    private static final String PAGE_TYPE_ITEM = "item";

    private static final java.util.Set<String> V2_WIDGET_TYPES = java.util.Set.of(
            // Family types (v2 API path segments)
            PAGE_TYPE_ITEM, SEARCH_TYPE_KEYWORD, SEARCH_TYPE_CATEGORY, "personalized", "global", "visual",
            // Algorithm types returned by /api/v1/merchant/widgets
            "mlt", "co_viewed", "co_bought", "rt_recs",
            "bestseller", "trending_product",
            "jfy", "past_purchases", "recently_viewed",
            "search"
    );

    private static final java.util.Map<String, String> V2_TYPE_MAP = java.util.Map.of(
            // item-family algorithm types
            "mlt",              PAGE_TYPE_ITEM,
            "co_viewed",        PAGE_TYPE_ITEM,
            "co_bought",        PAGE_TYPE_ITEM,
            "rt_recs",          PAGE_TYPE_ITEM,
            // global-family algorithm types
            "bestseller",       "global",
            "trending_product", "global",
            // personalized-family algorithm types
            "jfy",              "personalized",
            "past_purchases",   "personalized",
            "recently_viewed",  "personalized",
            // keyword-family algorithm type
            "search",           SEARCH_TYPE_KEYWORD
    );

    private final Supplier<String> requestIdSupplier;

    public DiscoveryRequestFactory() {
        this(() -> UUID.randomUUID().toString());
    }

    DiscoveryRequestFactory(Supplier<String> requestIdSupplier) {
        this.requestIdSupplier = Objects.requireNonNull(requestIdSupplier);
    }

    public DiscoveryRequestSpec search(SearchQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_KEYWORD)
                .queryParam("q", query.query() != null ? query.query() : "*")
                .queryParam("request_id", nextRequestId())
                .queryParamIfNotBlank("fl", query.fields())
                .queryParamIfNotBlank("catalog_name", query.catalogName());

        builder.queryParam("facet.version", "3.0");
        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendRangeFilters(builder, query.rangeFilters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        builder.queryParamIfNotBlank("view_id", query.viewId());
        return builder.build();
    }

    public DiscoveryRequestSpec category(CategoryQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_CATEGORY)
                .queryParam("q", query.categoryId() != null && !query.categoryId().isBlank() ? query.categoryId() : "*")
                .queryParam("request_id", nextRequestId())
                .queryParamIfNotBlank("fl", query.fields())
                .queryParamIfNotBlank("catalog_name", query.catalogName());

        builder.queryParam("facet.version", "3.0");
        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendRangeFilters(builder, query.rangeFilters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        builder.queryParamIfNotBlank("view_id", query.viewId());
        return builder.build();
    }

    public DiscoveryRequestSpec productLookup(String pid, String url, String fields, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseCoreRequest(credentials)
                .queryParam("request_type", REQUEST_TYPE_SEARCH)
                .queryParam("search_type", SEARCH_TYPE_KEYWORD)
                .queryParam("request_id", nextRequestId())
                .queryParam("q", "*")
                .queryParam("rows", 1)
                .queryParamIfNotBlank("fl", fields)
                .queryParamIfNotBlank("url", url);

        if (pid != null && !pid.isBlank()) {
            builder.queryParam("efq", "pid:(" + pid + ")");
        }
        return builder.build();
    }

    public DiscoveryRequestSpec autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials) {
        String catalogViews = (query.catalogViews() != null && !query.catalogViews().isBlank())
                ? query.catalogViews() : credentials.domainKey();

        DiscoveryRequestSpec.Builder builder = baseStandardRequest(AUTOSUGGEST_PATH, credentials)
                .queryParam("request_type", REQUEST_TYPE_SUGGEST)
                .queryParam("q", query.query() != null ? query.query() : "")
                .queryParam("request_id", nextRequestId())
                .queryParam("catalog_views", catalogViews);

        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        return builder.build();
    }

    public DiscoveryRequestSpec recommendationV1(RecQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = baseStandardRequest(recommendationPath(query.widgetType(), query.widgetId()), credentials)
                .queryParam("request_id", nextRequestId())
                .queryParamIfNotBlank("item_ids", query.contextProductId())
                .queryParamIfNotBlank("cat_id", query.catId())
                .queryParamIfNotBlank("type", query.contextPageType())
                .queryParam("rows", query.limit())
                .queryParamIfNotBlank("fl", query.fields());
        return builder.build();
    }

    public DiscoveryRequestSpec recommendationV2(RecQuery query, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = DiscoveryRequestSpec.builder(recommendationPath(query.widgetType(), query.widgetId()))
                .queryParam("account_id", credentials.accountId())
                .queryParam("domain_key", credentials.domainKey())
                .queryParam("request_id", nextRequestId())
                .queryParamIfNotBlank("url", query.url())
                .queryParamIfNotBlank("ref_url", query.refUrl())
                .queryParamIfNotBlank("_br_uid_2", query.brUid2())
                .queryParamIfNotBlank("item_ids", query.contextProductId())
                .queryParamIfNotBlank("cat_id", query.catId())
                .queryParamIfNotBlank("context.page_type", query.contextPageType())
                .queryParam("rows", query.limit())
                .queryParamIfNotBlank("fields", query.fields())
                .queryParamIfNotBlank("filter", query.filters())
                .queryParamIfNotBlank("query", query.query())
                .queryParamIfNotBlank("view_id", query.viewId());
        return builder.build();
    }

    public DiscoveryRequestSpec visualSearch(VisualSearchQuery query, DiscoveryCredentials credentials) {
        return DiscoveryRequestSpec.builder("/api/v2/widgets/visual/search/" + query.widgetId())
                .queryParam("account_id", credentials.accountId())
                .queryParam("domain_key", credentials.domainKey())
                .queryParam("request_id", nextRequestId())
                .queryParamIfNotBlank("image_id", query.imageId())
                .queryParamIfNotBlank("object_id", query.objectId())
                .queryParam("rows", query.rows())
                .queryParamIfNotBlank("fields", query.fields())
                .queryParamIfNotBlank("url", query.url())
                .queryParamIfNotBlank("ref_url", query.refUrl())
                .queryParamIfNotBlank("_br_uid_2", query.brUid2())
                .build();
    }

    public DiscoveryRequestSpec merchantWidgets(DiscoveryCredentials credentials) {
        return baseStandardRequest(WIDGETS_PATH, credentials).build();
    }

    public static String toV2WidgetType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return PAGE_TYPE_ITEM;
        }
        String mapped = V2_TYPE_MAP.getOrDefault(rawType, rawType);
        if (!V2_WIDGET_TYPES.contains(mapped)) {
            log.warn("Unknown widget type '{}' - defaulting to 'item' for v2 path", rawType);
            return PAGE_TYPE_ITEM;
        }
        return mapped;
    }

    private DiscoveryRequestSpec.Builder baseCoreRequest(DiscoveryCredentials credentials) {
        return baseStandardRequest(CORE_PATH, credentials);
    }

    private DiscoveryRequestSpec.Builder baseStandardRequest(String path, DiscoveryCredentials credentials) {
        DiscoveryRequestSpec.Builder builder = DiscoveryRequestSpec.builder(path)
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

    private static String recommendationPath(String widgetType, String widgetId) {
        return RECOMMENDATION_PATH + "/" + toV2WidgetType(widgetType) + "/" + (widgetId != null ? widgetId : "");
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

    private static void appendRangeFilters(DiscoveryRequestSpec.Builder builder,
                                           Map<String, RangeSelection> rangeFilters) {
        if (rangeFilters == null || rangeFilters.isEmpty()) {
            return;
        }
        for (Map.Entry<String, RangeSelection> entry : rangeFilters.entrySet()) {
            String start = entry.getValue().start() != null ? String.valueOf(entry.getValue().start()) : "*";
            String end = entry.getValue().end() != null ? String.valueOf(entry.getValue().end()) : "*";
            builder.queryParam("fq", entry.getKey() + ":[" + start + " TO " + end + "]");
        }
    }
}
