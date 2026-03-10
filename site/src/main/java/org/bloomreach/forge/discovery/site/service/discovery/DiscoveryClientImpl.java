package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.exception.RecommendationException;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private static final String PIXEL_RESOURCE_SPACE_EU = "discoveryPixelAPIEU";

    private static final String SEARCH_RESOURCE_SPACE_STAGING      = "discoverySearchAPIStaging";
    private static final String PATHWAYS_RESOURCE_SPACE_STAGING    = "discoveryPathwaysAPIStaging";
    private static final String AUTOSUGGEST_RESOURCE_SPACE_STAGING = "discoveryAutosuggestAPIStaging";

    // Discovery API — request_type, search_type, and pixel type/group parameter values
    private static final String REQUEST_TYPE_SEARCH   = "search";
    private static final String REQUEST_TYPE_KEYWORD  = "keyword";
    private static final String REQUEST_TYPE_CATEGORY = "category";
    private static final String PAGE_TYPE_PAGEVIEW    = "pageview";
    private static final String PAGE_TYPE_EVENT       = "event";
    private static final String PAGE_TYPE_WIDGET      = "widget";
    private static final String PAGE_TYPE_VIEW        = "view";
    private static final String PAGE_TYPE_ITEM        = "item";
    private static final String HEADER_AUTH_KEY       = "auth-key";

    /**
     * Valid v2 Pathways widget type path segments.
     * Merchant widget API may return legacy types (e.g. "mlt") that are not valid v2 path
     * segments — those must be translated before building the request URL.
     */
    private static final java.util.Set<String> V2_WIDGET_TYPES = java.util.Set.of(
            PAGE_TYPE_ITEM, REQUEST_TYPE_KEYWORD, REQUEST_TYPE_CATEGORY, "personalized", "global", "visual"
    );
    private static final java.util.Map<String, String> V2_TYPE_MAP = java.util.Map.of(
            "mlt", PAGE_TYPE_ITEM   // More Like This (legacy) → item widget with algorithm in Dashboard
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
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryConfig config, ClientContext ctx) {
        String path = buildAutosuggestPath(query, config);
        log.debug("Discovery autosuggest [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? AUTOSUGGEST_RESOURCE_SPACE_STAGING : AUTOSUGGEST_RESOURCE_SPACE, path, buildHint(ctx));
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
    public SearchResponse search(SearchQuery query, DiscoveryConfig config, ClientContext ctx) {
        String path = buildSearchPath(query, config);
        log.debug("Discovery search [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? SEARCH_RESOURCE_SPACE_STAGING : SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
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
    public SearchResponse category(CategoryQuery query, DiscoveryConfig config, ClientContext ctx) {
        String path = buildCategoryPath(query, config);
        log.debug("Discovery category browse [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? SEARCH_RESOURCE_SPACE_STAGING : SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
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
    public RecommendationResult recommend(RecQuery query, DiscoveryConfig config, ClientContext ctx) {
        if (config.authKey() != null && !config.authKey().isBlank()) {
            return recommendV2(query, config, ctx);
        }
        return recommendV1(query, config, ctx);
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryConfig config, ClientContext ctx) {
        String path = buildFetchProductPath(pid, url, config);
        log.debug("Discovery fetchProduct [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? SEARCH_RESOURCE_SPACE_STAGING : SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
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

    private RecommendationResult recommendV1(RecQuery query, DiscoveryConfig config, ClientContext ctx) {
        String path = buildRecommendationPath(query, config);
        log.debug("Discovery recommendations v1 [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? SEARCH_RESOURCE_SPACE_STAGING : SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
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

    private RecommendationResult recommendV2(RecQuery query, DiscoveryConfig config, ClientContext ctx) {
        String path = buildRecommendationV2Path(query, config);
        log.debug("Discovery recommendations v2 (Pathways) [request_id={}]: {}", requestId(path), redactPath(path));
        try {
            Resource resource = getBroker().resolve(isStaging(config) ? PATHWAYS_RESOURCE_SPACE_STAGING : PATHWAYS_RESOURCE_SPACE, path, buildV2Hint(config, ctx));
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

    private static boolean isStaging(DiscoveryConfig config) {
        return ConfigDefaults.STAGING_ENVIRONMENT.equalsIgnoreCase(config.environment());
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

    private static boolean isForwardClientHeaders() {
        return Boolean.parseBoolean(System.getProperty("brxdis.forwardClientHeaders", "true"));
    }

    static ExchangeHint buildHint(ClientContext ctx) {
        ExchangeHintBuilder hintBuilder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (isForwardClientHeaders()) applyClientHeaders(hintBuilder, ctx);
        return hintBuilder.build();
    }

    static ExchangeHint buildV2Hint(DiscoveryConfig config, ClientContext ctx) {
        ExchangeHintBuilder hintBuilder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (config.authKey() != null && !config.authKey().isBlank()) {
            hintBuilder.requestHeader(HEADER_AUTH_KEY, config.authKey());
        }
        if (isForwardClientHeaders()) applyClientHeaders(hintBuilder, ctx);
        return hintBuilder.build();
    }

    private static void applyClientHeaders(ExchangeHintBuilder hintBuilder, ClientContext ctx) {
        if (ctx == null) return;
        if (ctx.userAgent() != null && !ctx.userAgent().isBlank())
            hintBuilder.requestHeader("User-Agent", ctx.userAgent());
        if (ctx.acceptLanguage() != null && !ctx.acceptLanguage().isBlank())
            hintBuilder.requestHeader("Accept-Language", ctx.acceptLanguage());
        if (ctx.xForwardedFor() != null && !ctx.xForwardedFor().isBlank())
            hintBuilder.requestHeader("X-Forwarded-For", ctx.xForwardedFor());
    }

    private String buildSearchPath(SearchQuery query, DiscoveryConfig config) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SEARCH_PATH);
        appendCommonParams(builder, config);
        builder.queryParam("request_type", REQUEST_TYPE_SEARCH)
               .queryParam("search_type", REQUEST_TYPE_KEYWORD)
               .queryParam("q", query.query() != null ? query.query() : "*")
               .queryParam("request_id", UUID.randomUUID())
               .queryParam("fl", DEFAULT_FIELDS);
        if (query.catalogName() != null && !query.catalogName().isBlank()) {
            builder.queryParam("catalog_name", query.catalogName());
        }
        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        return builder.build(false).toUriString();
    }

    private String buildAutosuggestPath(AutosuggestQuery query, DiscoveryConfig config) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(AUTOSUGGEST_PATH);
        appendCommonParams(builder, config);
        String catalogViews = (query.catalogViews() != null && !query.catalogViews().isBlank())
                ? query.catalogViews() : config.domainKey();
        builder.queryParam("request_type", "suggest")
               .queryParam("q", query.query() != null ? query.query() : "")
               .queryParam("request_id", UUID.randomUUID())
               .queryParam("catalog_views", catalogViews);
        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        return builder.build(false).toUriString();
    }

    private String buildCategoryPath(CategoryQuery query, DiscoveryConfig config) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(CATEGORY_PATH);
        appendCommonParams(builder, config);
        builder.queryParam("request_type", REQUEST_TYPE_SEARCH)
               .queryParam("search_type", REQUEST_TYPE_CATEGORY)
               .queryParam("q", query.categoryId() != null ? query.categoryId() : "")
               .queryParam("request_id", UUID.randomUUID())
               .queryParam("fl", DEFAULT_FIELDS);
        appendTracking(builder, query.brUid2(), query.refUrl(), query.url());
        appendPagination(builder, query.page(), query.pageSize());
        appendSort(builder, query.sort());
        appendFilters(builder, query.filters());
        appendStatsFields(builder, query.statsFields());
        appendSegment(builder, query.segment());
        appendEfq(builder, query.efq());
        return builder.build(false).toUriString();
    }

    /**
     * Translates a merchant-API widget type to a valid v2 Pathways path segment.
     */
    public static String toV2WidgetType(String rawType) {
        if (rawType == null || rawType.isBlank()) return PAGE_TYPE_ITEM;
        String mapped = V2_TYPE_MAP.getOrDefault(rawType, rawType);
        if (!V2_WIDGET_TYPES.contains(mapped)) {
            log.warn("Unknown widget type '{}' — defaulting to 'item' for v2 path", rawType);
            return PAGE_TYPE_ITEM;
        }
        return mapped;
    }

    private String buildRecommendationPath(RecQuery query, DiscoveryConfig config) {
        String widgetType = toV2WidgetType(query.widgetType());
        String widgetId = query.widgetId() != null ? query.widgetId() : "";
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(RECS_PATH + "/" + widgetType + "/" + widgetId);
        appendCommonParams(builder, config);
        builder.queryParam("request_id", UUID.randomUUID());
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            builder.queryParam("item_ids", query.contextProductId());
        }
        if (query.contextPageType() != null && !query.contextPageType().isBlank()) {
            builder.queryParam("type", query.contextPageType());
        }
        builder.queryParam("rows", query.limit());
        String fields = (query.fields() != null && !query.fields().isBlank()) ? query.fields() : DEFAULT_FIELDS;
        builder.queryParam("fl", fields);
        return builder.build(false).toUriString();
    }

    private String buildRecommendationV2Path(RecQuery query, DiscoveryConfig config) {
        String widgetType = toV2WidgetType(query.widgetType());
        String widgetId = query.widgetId() != null ? query.widgetId() : "";
        // v2: account_id + domain_key only — auth is the auth-key header, NOT auth_key query param
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(RECS_PATH + "/" + widgetType + "/" + widgetId)
                .queryParam("account_id", config.accountId())
                .queryParam("domain_key", config.domainKey())
                .queryParam("request_id", UUID.randomUUID());
        if (query.url() != null && !query.url().isBlank()) {
            builder.queryParam("url", query.url());
        }
        if (query.refUrl() != null && !query.refUrl().isBlank()) {
            builder.queryParam("ref_url", query.refUrl());
        }
        if (query.brUid2() != null && !query.brUid2().isBlank()) {
            builder.queryParam("_br_uid_2", query.brUid2());
        }
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            builder.queryParam("item_ids", query.contextProductId());
        }
        if (query.contextPageType() != null && !query.contextPageType().isBlank()) {
            builder.queryParam("context.page_type", query.contextPageType());
        }
        builder.queryParam("rows", query.limit());
        String fields = (query.fields() != null && !query.fields().isBlank()) ? query.fields() : DEFAULT_FIELDS;
        builder.queryParam("fields", fields);
        if (query.filters() != null && !query.filters().isBlank()) {
            builder.queryParam("filter", query.filters());
        }
        return builder.build(false).toUriString();
    }

    // ── Pixel events ────────────────────────────────────────────────────────────

    @Override
    public String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryConfig config,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, config);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
               .queryParam("ptype", REQUEST_TYPE_SEARCH);
        if (query.query() != null && !query.query().isBlank()) {
            builder.queryParam("search_term", query.query());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                                         String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, config);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
               .queryParam("ptype", REQUEST_TYPE_CATEGORY);
        if (query.categoryId() != null && !query.categoryId().isBlank()) {
            builder.queryParam("cat_id", query.categoryId());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, config);
        builder.queryParam("type", PAGE_TYPE_EVENT)
               .queryParam("group", PAGE_TYPE_WIDGET)
               .queryParam("etype", PAGE_TYPE_VIEW);
        if (query.widgetId() != null && !query.widgetId().isBlank()) {
            builder.queryParam("wid", query.widgetId());
        }
        if (query.widgetType() != null && !query.widgetType().isBlank()) {
            builder.queryParam("wty", query.widgetType());
        }
        if (result.widgetResultId() != null && !result.widgetResultId().isBlank()) {
            builder.queryParam("wrid", result.widgetResultId());
        }
        if (query.contextProductId() != null && !query.contextProductId().isBlank()) {
            builder.queryParam("wq", query.contextProductId());
        }
        appendPixelTracking(builder, query.brUid2(), query.refUrl(), query.url(), clientIp);
        appendPixelSkus(builder, result.products());
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    @Override
    public String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl, String url,
                                                 DiscoveryConfig config, String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, config);
        builder.queryParam("type", PAGE_TYPE_PAGEVIEW)
               .queryParam("ptype", "product")
               .queryParam("prod_id", pid);
        if (prodName != null && !prodName.isBlank()) {
            builder.queryParam("prod_name", prodName);
        }
        appendPixelTracking(builder, brUid2, refUrl, url, clientIp);
        appendPixelFlags(builder, flags);
        return builder.build(false).toUriString();
    }

    private static void appendPixelFlags(UriComponentsBuilder builder, PixelFlags flags) {
        if (flags.testData()) builder.queryParam("test_data", "true");
        if (flags.debug())    builder.queryParam("debug", "true");
    }

    private static String pixelResourceSpace(PixelFlags flags) {
        return "EU".equals(flags.region()) ? PIXEL_RESOURCE_SPACE_EU : PIXEL_RESOURCE_SPACE;
    }

    /**
     * Fires a pixel GET via the CRISP broker; catches {@link ResourceException} and logs at WARN.
     */
    @Override
    public void firePixelEvent(String pixelPath, DiscoveryConfig config, ClientContext ctx, PixelFlags flags) {
        log.debug("Discovery pixel event: {}", pixelPath);
        try {
            getBroker().resolve(pixelResourceSpace(flags), pixelPath, buildHint(ctx));
        } catch (ResourceException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON processing error")) {
                // CRISP tries to parse the image/gif response as JSON — HTTP 200 was received, pixel fired OK
                log.debug("Discovery pixel event fired (non-JSON response ignored) — path={}", pixelPath);
            } else {
                log.warn("Discovery pixel event failed — path={}: {}", pixelPath, e.getMessage());
            }
        }
    }

    private static void appendPixelCommonParams(UriComponentsBuilder builder, DiscoveryConfig config) {
        builder.queryParam("acct_id", config.accountId())
               .queryParam("domain_key", config.domainKey());
    }

    private static void appendPixelSkus(UriComponentsBuilder builder, List<ProductSummary> products) {
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
            builder.queryParam("sku", skus);
        }
    }

    private static void appendCommonParams(UriComponentsBuilder builder, DiscoveryConfig config) {
        builder.queryParam("account_id", config.accountId())
               .queryParam("domain_key", config.domainKey());
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            builder.queryParam("auth_key", config.apiKey());
        }
    }

    private static void appendTracking(UriComponentsBuilder builder, String brUid2, String refUrl, String url) {
        if (brUid2 != null && !brUid2.isBlank()) {
            builder.queryParam("_br_uid_2", brUid2);
        }
        if (refUrl != null && !refUrl.isBlank()) {
            builder.queryParam("ref_url", refUrl);
        }
        if (url != null && !url.isBlank()) {
            builder.queryParam("url", url);
        }
    }

    private static void appendPixelTracking(UriComponentsBuilder builder, String brUid2, String refUrl,
                                             String url, String clientIp) {
        if (brUid2 != null && !brUid2.isBlank()) {
            // Cookie arrives from browser already percent-encoded; decode once so CRISP single-encodes correctly
            String decodedBrUid2 = URLDecoder.decode(brUid2, StandardCharsets.UTF_8);
            builder.queryParam("cookie2", decodedBrUid2);
        }
        if (refUrl != null && !refUrl.isBlank()) {
            builder.queryParam("ref", refUrl);
        }
        if (url != null && !url.isBlank()) {
            int queryStart = url.indexOf('?');
            builder.queryParam("url", queryStart >= 0 ? url.substring(0, queryStart) : url);
        }
        builder.queryParam("version", "ss-v0.1")
               .queryParam("rand", UUID.randomUUID())
               .queryParam("client_ts", System.currentTimeMillis());
        if (clientIp != null && !clientIp.isBlank()) {
            builder.queryParam("client_ip", clientIp);
        }
    }

    private static void appendPagination(UriComponentsBuilder builder, int page, int pageSize) {
        builder.queryParam("start", (long) page * pageSize)
               .queryParam("rows", pageSize);
    }

    private static void appendSort(UriComponentsBuilder builder, String sort) {
        if (sort != null && !sort.isBlank()) {
            builder.queryParam("sort", sort);
        }
    }

    private static void appendStatsFields(UriComponentsBuilder builder, List<String> statsFields) {
        if (statsFields == null) return;
        for (String field : statsFields) {
            if (field != null && !field.isBlank()) {
                builder.queryParam("stats.field", field);
            }
        }
    }

    private static void appendSegment(UriComponentsBuilder builder, String segment) {
        if (segment != null && !segment.isBlank()) {
            builder.queryParam("segment", segment);
        }
    }

    private static void appendEfq(UriComponentsBuilder builder, String efq) {
        if (efq != null && !efq.isBlank()) {
            builder.queryParam("efq", efq);
        }
    }

    private static void appendFilters(UriComponentsBuilder builder, Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            for (String value : entry.getValue()) {
                builder.queryParam("fq", entry.getKey() + ":\"" + value + "\"");
            }
        }
    }

    private String buildFetchProductPath(String pid, String url, DiscoveryConfig config) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SEARCH_PATH);
        appendCommonParams(builder, config);
        builder.queryParam("search_type", REQUEST_TYPE_KEYWORD)
               .queryParam("request_type", REQUEST_TYPE_SEARCH)
               .queryParam("request_id", UUID.randomUUID())
               .queryParam("q", "*");
        if (pid != null && !pid.isBlank()) {
            builder.queryParam("efq", "pid:(" + pid + ")");
        }
        if (url != null && !url.isBlank()) {
            builder.queryParam("url", url);
        }
        builder.queryParam("fl", DEFAULT_FIELDS)
               .queryParam("rows", 1);
        return builder.build(false).toUriString();
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
