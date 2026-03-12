package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.exception.RecommendationException;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.request.DiscoveryRequestSpec;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    private static final String PIXEL_PATH = "/pix.gif";
    private static final String DEFAULT_FIELDS = "pid,title,brand,price,sale_price,thumb_image,url,description";
    private static final int PIXEL_MAX_SKUS = 20;

    private static final String SEARCH_RESOURCE_SPACE = "discoverySearchAPI";
    private static final String PATHWAYS_RESOURCE_SPACE = "discoveryPathwaysAPI";
    private static final String AUTOSUGGEST_RESOURCE_SPACE = "discoveryAutosuggestAPI";
    private static final String PIXEL_RESOURCE_SPACE = "discoveryPixelAPI";
    private static final String PIXEL_RESOURCE_SPACE_EU = "discoveryPixelAPIEU";

    // Discovery API — request_type, search_type, and pixel type/group parameter values
    private static final String REQUEST_TYPE_SEARCH   = "search";
    private static final String REQUEST_TYPE_CATEGORY = "category";
    private static final String PAGE_TYPE_PAGEVIEW    = "pageview";
    private static final String PAGE_TYPE_EVENT       = "event";
    private static final String PAGE_TYPE_WIDGET      = "widget";
    private static final String PAGE_TYPE_VIEW        = "view";
    private static final String HEADER_AUTH_KEY       = "auth-key";

    private final ResourceServiceBroker broker;
    private final DiscoveryResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;

    public DiscoveryClientImpl(ResourceServiceBroker broker, DiscoveryResponseMapper responseMapper) {
        this(broker, responseMapper, new DiscoveryRequestFactory());
    }

    DiscoveryClientImpl(ResourceServiceBroker broker, DiscoveryResponseMapper responseMapper,
                        DiscoveryRequestFactory requestFactory) {
        this.broker = broker;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    @Override
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildAutosuggestPath(query, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery autosuggest [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(AUTOSUGGEST_RESOURCE_SPACE, path, buildHint(ctx));
            AutosuggestResult result = responseMapper.toAutosuggestResult(resource);
            log.debug("Discovery autosuggest returned {} query suggestions [request_id={}]",
                    result.querySuggestions().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery autosuggest failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Autosuggest request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildSearchPath(query, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery search [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery search returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (ResourceException e) {
            log.error("Discovery search failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Search request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildCategoryPath(query, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery category browse [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery category returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (ResourceException e) {
            log.error("Discovery category failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Category request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Routes to v2 Pathways API when {@code credentials.authKey()} is present; otherwise v1.
     */
    @Override
    public RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            return recommendV2(query, credentials, ctx);
        }
        return recommendV1(query, credentials, ctx);
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildFetchProductPath(pid, url, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery fetchProduct [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
            SearchResult result = responseMapper.toSearchResult(resource, 0, 1);
            log.debug("Discovery fetchProduct pid='{}' found={} [request_id={}]",
                    pid, !result.products().isEmpty(), requestLog.requestId());
            return result.products().isEmpty() ? Optional.empty() : Optional.of(result.products().get(0));
        } catch (ResourceException e) {
            log.warn("Discovery fetchProduct failed [request_id={}] for pid '{}': {}",
                    requestLog.requestId(), pid, e.getMessage());
            throw new SearchException("fetchProduct request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV1(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildRecommendationPath(query, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery recommendations v1 [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(SEARCH_RESOURCE_SPACE, path, buildHint(ctx));
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v1 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v1 failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new RecommendationException("Recommendation request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV2(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = buildRecommendationV2Path(query, credentials);
        RequestLogContext requestLog = requestLog(path);
        log.debug("Discovery recommendations v2 (Pathways) [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = broker.resolve(PATHWAYS_RESOURCE_SPACE, path, buildV2Hint(credentials, ctx));
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v2 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v2 failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new RecommendationException("Pathways recommendation request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a copy of {@code path} with the {@code auth_key} query-parameter
     * value replaced by {@code ***}. Safe to pass to log statements.
     */
    static String redactPath(String path) {
        int start = path.indexOf("auth_key=");
        if (start < 0) {
            return path;
        }
        int valueStart = start + "auth_key=".length();
        int end = path.indexOf('&', valueStart);
        if (end < 0) {
            end = path.length();
        }
        return path.substring(0, valueStart) + "***" + path.substring(end);
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

    static ExchangeHint buildV2Hint(DiscoveryCredentials credentials, ClientContext ctx) {
        ExchangeHintBuilder hintBuilder = ExchangeHintBuilder.create()
                .methodName("GET")
                .requestHeader("Content-Type", "application/json");
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            hintBuilder.requestHeader(HEADER_AUTH_KEY, credentials.authKey());
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

    private String buildSearchPath(SearchQuery query, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.search(query, credentials));
    }

    private String buildAutosuggestPath(AutosuggestQuery query, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.autosuggest(query, credentials));
    }

    private String buildCategoryPath(CategoryQuery query, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.category(query, credentials));
    }

    /**
     * Translates a merchant-API widget type to a valid v2 Pathways path segment.
     */
    public static String toV2WidgetType(String rawType) {
        return DiscoveryRequestFactory.toV2WidgetType(rawType);
    }

    private String buildRecommendationPath(RecQuery query, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.recommendationV1(query, credentials));
    }

    private String buildRecommendationV2Path(RecQuery query, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.recommendationV2(query, credentials));
    }

    // ── Pixel events ────────────────────────────────────────────────────────────

    @Override
    public String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
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
    public String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                         String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
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
    public String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                       String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
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
                                                 DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(PIXEL_PATH);
        appendPixelCommonParams(builder, credentials);
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
    public void firePixelEvent(String pixelPath, ClientContext ctx, PixelFlags flags) {
        log.debug("Discovery pixel event: {}", pixelPath);
        try {
            broker.resolve(pixelResourceSpace(flags), pixelPath, buildHint(ctx));
        } catch (ResourceException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("JSON processing error")) {
                // CRISP tries to parse the image/gif response as JSON — HTTP 200 was received, pixel fired OK
                log.debug("Discovery pixel event fired (non-JSON response ignored) — path={}", pixelPath);
            } else {
                log.warn("Discovery pixel event failed — path={}: {}", pixelPath, e.getMessage());
            }
        }
    }

    private static void appendPixelCommonParams(UriComponentsBuilder builder, DiscoveryCredentials credentials) {
        builder.queryParam("acct_id", credentials.accountId())
               .queryParam("domain_key", credentials.domainKey());
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

    private String buildFetchProductPath(String pid, String url, DiscoveryCredentials credentials) {
        return toRelativePath(requestFactory.productLookup(pid, url, credentials));
    }

    private static String toRelativePath(DiscoveryRequestSpec request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.path());
        request.forEachQueryParameter((name, value) -> builder.queryParam(name, value));
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

    private static RequestLogContext requestLog(String path) {
        return new RequestLogContext(requestId(path), redactPath(path));
    }

    private record RequestLogContext(String requestId, String redactedPath) {
    }
}
