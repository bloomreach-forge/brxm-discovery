package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.exception.RecommendationException;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the full HTTP contract for all Discovery API calls.
 * Builds request paths, selects CRISP resource spaces, executes via the broker,
 * and maps responses — keeping services free of HTTP/CRISP concerns.
 * <p>
 * CRISP's {@code SimpleJacksonRestTemplateResourceResolver} uses
 * {@code UriComponentsBuilder.fromUriString(...).build().encode()} internally,
 * so all query-parameter values are passed as plain strings here —
 * pre-encoding would cause double-encoding on the wire.
 */
public class DiscoveryClientImpl implements DiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryClientImpl.class);

    private static final String AUTOSUGGEST_PATH = "/api/v2/suggest/";
    private static final String SEARCH_PATH = "/api/v1/core/";
    private static final String CATEGORY_PATH = "/api/v1/core/";
    private static final String RECS_PATH = "/api/v2/widgets";
    private static final String PIXEL_PATH = "/pix.gif";
    private static final String DEFAULT_FIELDS = "pid,title,brand,price,sale_price,thumb_image,url,description";
    private static final int PIXEL_MAX_SKUS = 20;

    private static final String SEARCH_RESOURCE_SPACE = "discoverySearchAPI";
    private static final String PATHWAYS_RESOURCE_SPACE = "discoveryPathwaysAPI";
    private static final String AUTOSUGGEST_RESOURCE_SPACE = "discoveryAutosuggestAPI";
    private static final String PIXEL_RESOURCE_SPACE = "discoveryPixelAPI";

    /**
     * Valid v2 Pathways widget type path segments.
     * Merchant widget API may return legacy types (e.g. "mlt") that are not valid v2 path
     * segments — those must be translated before building the request URL.
     */
    private static final java.util.Set<String> V2_WIDGET_TYPES = java.util.Set.of(
            "item", "keyword", "category", "personalized", "global", "visual"
    );
    private static final java.util.Map<String, String> V2_TYPE_MAP = java.util.Map.of(
            "mlt", "item"   // More Like This (legacy) → item widget with algorithm in Dashboard
    );

    private final ResourceServiceBroker broker;
    private final DiscoveryResponseMapper responseMapper;

    public DiscoveryClientImpl(DiscoveryResponseMapper responseMapper) {
        this.broker = null; // resolved lazily via HippoServiceRegistry in getBroker()
        this.responseMapper = responseMapper;
    }

    /**
     * Used by tests — broker injected directly.
     */
    public DiscoveryClientImpl(ResourceServiceBroker broker, DiscoveryResponseMapper responseMapper) {
        this.broker = broker;
        this.responseMapper = responseMapper;
    }

    @Override
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryConfig config) {
        String path = buildAutosuggestPath(query, config);
        log.debug("Discovery autosuggest [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(AUTOSUGGEST_RESOURCE_SPACE, path, getHint());
            AutosuggestResult result = responseMapper.toAutosuggestResult(resource);
            log.debug("Discovery autosuggest returned {} query suggestions [request_id={}]",
                    result.querySuggestions().size(), requestId(path));
            return result;
        } catch (ResourceException e) {
            log.error("Discovery autosuggest failed [request_id={}] for path {}: {}",
                    requestId(path), redactPath(path), e.getMessage());
            throw new SearchException("Autosuggest request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse search(SearchQuery query, DiscoveryConfig config) {
        String path = buildSearchPath(query, config);
        log.debug("Discovery search [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(SEARCH_RESOURCE_SPACE, path, getHint());
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery search returned {} results [request_id={}]",
                    response.result().total(), requestId(path));
            return response;
        } catch (ResourceException e) {
            log.error("Discovery search failed [request_id={}] for path {}: {}",
                    requestId(path), redactPath(path), e.getMessage());
            throw new SearchException("Search request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse category(CategoryQuery query, DiscoveryConfig config) {
        String path = buildCategoryPath(query, config);
        log.debug("Discovery category browse [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(SEARCH_RESOURCE_SPACE, path, getHint());
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery category returned {} results [request_id={}]",
                    response.result().total(), requestId(path));
            return response;
        } catch (ResourceException e) {
            log.error("Discovery category failed [request_id={}] for path {}: {}",
                    requestId(path), redactPath(path), e.getMessage());
            throw new SearchException("Category request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Routes to v2 Pathways API when {@code config.authKey()} is present; otherwise v1.
     */
    @Override
    public RecommendationResult recommend(RecQuery query, DiscoveryConfig config) {
        if (config.authKey() != null && !config.authKey().isBlank()) {
            return recommendV2(query, config);
        }
        return recommendV1(query, config);
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryConfig config) {
        String path = buildFetchProductPath(pid, url, config);
        log.debug("Discovery fetchProduct [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(SEARCH_RESOURCE_SPACE, path, getHint());
            SearchResult result = responseMapper.toSearchResult(resource, 0, 1);
            log.debug("Discovery fetchProduct pid='{}' found={} [request_id={}]",
                    pid, !result.products().isEmpty(), requestId(path));
            return result.products().isEmpty() ? Optional.empty() : Optional.of(result.products().get(0));
        } catch (ResourceException e) {
            log.warn("Discovery fetchProduct failed [request_id={}] for pid '{}': {}",
                    requestId(path), pid, e.getMessage());
            throw new SearchException("fetchProduct request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV1(RecQuery query, DiscoveryConfig config) {
        String path = buildRecommendationPath(query, config);
        log.debug("Discovery recommendations v1 [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(SEARCH_RESOURCE_SPACE, path, getHint());
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v1 returned {} products [request_id={}]",
                    result.products().size(), requestId(path));
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v1 failed [request_id={}] for path {}: {}",
                    requestId(path), redactPath(path), e.getMessage());
            throw new RecommendationException("Recommendation request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV2(RecQuery query, DiscoveryConfig config) {
        String path = buildRecommendationV2Path(query, config);
        log.debug("Discovery recommendations v2 (Pathways) [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(PATHWAYS_RESOURCE_SPACE, path, getV2Hint(config));
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v2 returned {} products [request_id={}]",
                    result.products().size(), requestId(path));
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v2 failed [request_id={}] for path {}: {}",
                    requestId(path), redactPath(path), e.getMessage());
            throw new RecommendationException("Pathways recommendation request failed: " + e.getMessage(), e);
        }
    }

    private ResourceServiceBroker getBroker() {
        if (broker != null) return broker;
        return HippoServiceRegistry.getService(ResourceServiceBroker.class);
    }

    /**
     * Returns a copy of {@code path} with the {@code auth_key} query-parameter
     * value replaced by {@code ***}. Safe to pass to log statements.
     */
    static String redactPath(String path) {
        return path.replaceAll("auth_key=[^&]*", "auth_key=***");
    }

    private static ExchangeHint getHint() {
        return ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json")
                .build();
    }

    private static ExchangeHint getV2Hint(DiscoveryConfig config) {
        ExchangeHintBuilder builder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (config.authKey() != null && !config.authKey().isBlank()) {
            builder.requestHeader("auth-key", config.authKey());
        }
        return builder.build();
    }

    private String buildSearchPath(SearchQuery query, DiscoveryConfig config) {
        StringBuilder sb = new StringBuilder(SEARCH_PATH);
        appendCommonParams(sb, config);
        sb.append("&request_type=search");
        sb.append("&search_type=keyword");
        sb.append("&q=").append(query.query() != null ? query.query() : "*");
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        sb.append("&fl=").append(DEFAULT_FIELDS);
        if (query.catalogName() != null && !query.catalogName().isBlank()) {
            sb.append("&catalog_name=").append(query.catalogName());
        }
        appendTracking(sb, query.brUid2(), query.refUrl(), query.url());
        appendPagination(sb, query.page(), query.pageSize());
        appendSort(sb, query.sort());
        appendFilters(sb, query.filters());
        appendStatsFields(sb, query.statsFields());
        appendSegment(sb, query.segment());
        appendEfq(sb, query.efq());
        return sb.toString();
    }

    private String buildAutosuggestPath(AutosuggestQuery query, DiscoveryConfig config) {
        StringBuilder sb = new StringBuilder(AUTOSUGGEST_PATH);
        appendCommonParams(sb, config);
        sb.append("&request_type=suggest");
        sb.append("&q=").append(query.query() != null ? query.query() : "");
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        String catalogViews = (query.catalogViews() != null && !query.catalogViews().isBlank())
                ? query.catalogViews() : config.domainKey();
        sb.append("&catalog_views=").append(catalogViews);
        appendTracking(sb, query.brUid2(), query.refUrl(), query.url());
        return sb.toString();
    }

    private String buildCategoryPath(CategoryQuery query, DiscoveryConfig config) {
        StringBuilder sb = new StringBuilder(CATEGORY_PATH);
        appendCommonParams(sb, config);
        sb.append("&request_type=search");
        sb.append("&search_type=category");
        sb.append("&q=").append(query.categoryId() != null ? query.categoryId() : "");
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        sb.append("&fl=").append(DEFAULT_FIELDS);
        appendTracking(sb, query.brUid2(), query.refUrl(), query.url());
        appendPagination(sb, query.page(), query.pageSize());
        appendSort(sb, query.sort());
        appendFilters(sb, query.filters());
        appendStatsFields(sb, query.statsFields());
        appendSegment(sb, query.segment());
        appendEfq(sb, query.efq());
        return sb.toString();
    }

    /**
     * Translates a merchant-API widget type to a valid v2 Pathways path segment.
     */
    public static String toV2WidgetType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "item";
        String mapped = V2_TYPE_MAP.getOrDefault(rawType, rawType);
        if (!V2_WIDGET_TYPES.contains(mapped)) {
            log.warn("Unknown widget type '{}' — defaulting to 'item' for v2 path", rawType);
            return "item";
        }
        return mapped;
    }

    private String buildRecommendationPath(RecQuery query, DiscoveryConfig config) {
        String widgetType = toV2WidgetType(query.widgetType());
        String widgetId = query.widgetId() != null ? query.widgetId() : "";
        StringBuilder sb = new StringBuilder(RECS_PATH);
        sb.append("/").append(widgetType);
        sb.append("/").append(widgetId);
        appendCommonParams(sb, config);
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            sb.append("&item_ids=").append(query.contextProductId());
        }
        if (query.contextPageType() != null && !query.contextPageType().isBlank()) {
            sb.append("&type=").append(query.contextPageType());
        }
        sb.append("&rows=").append(query.limit());
        String fields = (query.fields() != null && !query.fields().isBlank()) ? query.fields() : DEFAULT_FIELDS;
        sb.append("&fl=").append(fields);
        return sb.toString();
    }

    private String buildRecommendationV2Path(RecQuery query, DiscoveryConfig config) {
        String widgetType = toV2WidgetType(query.widgetType());
        String widgetId = query.widgetId() != null ? query.widgetId() : "";
        StringBuilder sb = new StringBuilder(RECS_PATH);
        sb.append("/").append(widgetType);
        sb.append("/").append(widgetId);
        // v2: account_id + domain_key only — auth is the auth-key header, NOT auth_key query param
        sb.append("?account_id=").append(config.accountId());
        sb.append("&domain_key=").append(config.domainKey());
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        if (query.url() != null && !query.url().isBlank()) {
            sb.append("&url=").append(query.url());
        }
        if (query.refUrl() != null && !query.refUrl().isBlank()) {
            sb.append("&ref_url=").append(query.refUrl());
        }
        if (query.brUid2() != null && !query.brUid2().isBlank()) {
            sb.append("&_br_uid_2=").append(query.brUid2());
        }
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            sb.append("&item_ids=").append(query.contextProductId());
        }
        if (query.contextPageType() != null && !query.contextPageType().isBlank()) {
            sb.append("&context.page_type=").append(query.contextPageType());
        }
        sb.append("&rows=").append(query.limit());
        String fields = (query.fields() != null && !query.fields().isBlank()) ? query.fields() : DEFAULT_FIELDS;
        sb.append("&fields=").append(fields);
        if (query.filters() != null && !query.filters().isBlank()) {
            sb.append("&filter=").append(query.filters());
        }
        return sb.toString();
    }

    // ── Pixel events ────────────────────────────────────────────────────────────

    @Override
    public String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryConfig config,
                                       String clientIp, String userAgent) {
        StringBuilder sb = new StringBuilder(PIXEL_PATH);
        appendPixelCommonParams(sb, config);
        sb.append("&type=pageview");
        sb.append("&ptype=search");
        if (query.query() != null && !query.query().isBlank()) {
            sb.append("&q=").append(query.query());
        }
        appendPixelTracking(sb, query.brUid2(), query.refUrl(), query.url(), clientIp, userAgent);
        appendPixelSkus(sb, result.products());
        return sb.toString();
    }

    @Override
    public String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                                         String clientIp, String userAgent) {
        StringBuilder sb = new StringBuilder(PIXEL_PATH);
        appendPixelCommonParams(sb, config);
        sb.append("&type=pageview");
        sb.append("&ptype=category");
        if (query.categoryId() != null && !query.categoryId().isBlank()) {
            sb.append("&cat_id=").append(query.categoryId());
        }
        appendPixelTracking(sb, query.brUid2(), query.refUrl(), query.url(), clientIp, userAgent);
        appendPixelSkus(sb, result.products());
        return sb.toString();
    }

    @Override
    public String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                                       String clientIp, String userAgent) {
        StringBuilder sb = new StringBuilder(PIXEL_PATH);
        appendPixelCommonParams(sb, config);
        sb.append("&type=event");
        sb.append("&group=widget");
        sb.append("&etype=view");
        if (query.widgetId() != null && !query.widgetId().isBlank()) {
            sb.append("&wid=").append(query.widgetId());
        }
        if (query.widgetType() != null && !query.widgetType().isBlank()) {
            sb.append("&wty=").append(query.widgetType());
        }
        if (result.widgetResultId() != null && !result.widgetResultId().isBlank()) {
            sb.append("&wrid=").append(result.widgetResultId());
        }
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            sb.append("&wq=").append(query.contextProductId());
        }
        appendPixelTracking(sb, query.brUid2(), query.refUrl(), query.url(), clientIp, userAgent);
        appendPixelSkus(sb, result.products());
        return sb.toString();
    }

    @Override
    public String buildProductPageViewPixelPath(String pid, String brUid2, String refUrl, String url,
                                                 DiscoveryConfig config, String clientIp, String userAgent) {
        StringBuilder sb = new StringBuilder(PIXEL_PATH);
        appendPixelCommonParams(sb, config);
        sb.append("&type=pageview");
        sb.append("&ptype=product");
        sb.append("&prod_id=").append(pid);
        appendPixelTracking(sb, brUid2, refUrl, url, clientIp, userAgent);
        return sb.toString();
    }

    /**
     * Fires a pixel GET via the CRISP broker; catches {@link ResourceException} and logs at WARN.
     */
    @Override
    public void firePixelEvent(String pixelPath, DiscoveryConfig config) {
        log.debug("Discovery pixel event: {}", pixelPath);
        try {
            getBroker().resolve(PIXEL_RESOURCE_SPACE, pixelPath, getHint());
        } catch (ResourceException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON processing error")) {
                // CRISP tries to parse the image/gif response as JSON — HTTP 200 was received, pixel fired OK
                log.debug("Discovery pixel event fired (non-JSON response ignored) — path={}", pixelPath);
            } else {
                log.warn("Discovery pixel event failed — path={}: {}", pixelPath, e.getMessage());
            }
        }
    }

    private static void appendPixelCommonParams(StringBuilder sb, DiscoveryConfig config) {
        sb.append("?acct_id=").append(config.accountId());
        sb.append("&domain_key=").append(config.domainKey());
    }

    private static void appendPixelSkus(StringBuilder sb, List<ProductSummary> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        String skus = products.stream()
                .limit(PIXEL_MAX_SKUS)
                .map(ProductSummary::id)
                .filter(id -> id != null && !id.isBlank())
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
        if (skus != null) {
            sb.append("&sku=").append(skus);
        }
    }

    private static void appendCommonParams(StringBuilder sb, DiscoveryConfig config) {
        sb.append("?account_id=").append(config.accountId());
        sb.append("&domain_key=").append(config.domainKey());
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            sb.append("&auth_key=").append(config.apiKey());
        }
    }

    private static void appendTracking(StringBuilder sb, String brUid2, String refUrl, String url) {
        if (brUid2 != null && !brUid2.isBlank()) {
            sb.append("&_br_uid_2=").append(brUid2);
        }
        if (refUrl != null && !refUrl.isBlank()) {
            sb.append("&ref_url=").append(refUrl);
        }
        if (url != null && !url.isBlank()) {
            sb.append("&url=").append(url);
        }
    }

    private static void appendPixelTracking(StringBuilder sb, String brUid2, String refUrl, String url,
                                             String clientIp, String userAgent) {
        if (brUid2 != null && !brUid2.isBlank()) {
            // Cookie arrives from browser already percent-encoded; decode once so CRISP single-encodes correctly
            String decodedBrUid2 = java.net.URLDecoder.decode(brUid2, java.nio.charset.StandardCharsets.UTF_8);
            sb.append("&cookie2=").append(decodedBrUid2);
        }
        if (refUrl != null && !refUrl.isBlank()) {
            sb.append("&ref=").append(refUrl);
        }
        if (url != null && !url.isBlank()) {
            int idx = url.indexOf('?');
            sb.append("&url=").append(idx >= 0 ? url.substring(0, idx) : url);
        }
        sb.append("&rand=").append(java.util.UUID.randomUUID());
        sb.append("&client_ts=").append(System.currentTimeMillis());
        if (clientIp != null && !clientIp.isBlank()) {
            sb.append("&client_ip=").append(clientIp);
        }
        if (userAgent != null && !userAgent.isBlank()) {
            sb.append("&user_agent=").append(userAgent);
        }
    }

    private static void appendPagination(StringBuilder sb, int page, int pageSize) {
        sb.append("&start=").append((long) page * pageSize);
        sb.append("&rows=").append(pageSize);
    }

    private static void appendSort(StringBuilder sb, String sort) {
        if (sort != null && !sort.isBlank()) {
            sb.append("&sort=").append(sort);
        }
    }

    private static void appendStatsFields(StringBuilder sb, List<String> statsFields) {
        if (statsFields == null) return;
        for (String field : statsFields) {
            if (field != null && !field.isBlank()) {
                sb.append("&stats.field=").append(field);
            }
        }
    }

    private static void appendSegment(StringBuilder sb, String segment) {
        if (segment != null && !segment.isBlank()) {
            sb.append("&segment=").append(java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static void appendEfq(StringBuilder sb, String efq) {
        if (efq != null && !efq.isBlank()) {
            sb.append("&efq=").append(java.net.URLEncoder.encode(efq, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static void appendFilters(StringBuilder sb, Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            for (String value : entry.getValue()) {
                sb.append("&fq=").append(entry.getKey()).append(":\"").append(value).append("\"");
            }
        }
    }

    private String buildFetchProductPath(String pid, String url, DiscoveryConfig config) {
        StringBuilder sb = new StringBuilder(SEARCH_PATH);
        appendCommonParams(sb, config);
        sb.append("&search_type=keyword");
        sb.append("&request_type=search");
        sb.append("&request_id=").append(java.util.UUID.randomUUID());
        sb.append("&q=*");
        if (pid != null && !pid.isBlank()) {
            sb.append("&efq=pid:(").append(pid).append(")");
        }
        if (url != null && !url.isBlank()) {
            sb.append("&url=").append(url);
        }
        sb.append("&fl=").append(DEFAULT_FIELDS);
        sb.append("&rows=1");
        return sb.toString();
    }

    /**
     * Extracts the {@code request_id} value from a path string for log correlation.
     * Returns {@code "n/a"} if the parameter is not present.
     */
    static String requestId(String path) {
        int start = path.indexOf("request_id=");
        if (start < 0) return "n/a";
        int valueStart = start + "request_id=".length();
        int end = path.indexOf('&', valueStart);
        return end < 0 ? path.substring(valueStart) : path.substring(valueStart, end);
    }
}
