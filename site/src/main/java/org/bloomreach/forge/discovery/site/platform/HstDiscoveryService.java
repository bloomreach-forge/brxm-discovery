package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.sor.SoREnrichmentProvider;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl.toV2WidgetType;

/**
 * HST-aware façade that absorbs config resolution, query building, and request-cache logic.
 * Components become thin: they extract raw HST params, delegate here, and set model/attributes.
 * <p>
 * Config is resolved via {@link DiscoveryConfigProvider} — JVM-lifetime cache with JCR
 * observation-driven invalidation. No per-request JCR reads.
 * <p>
 * Pixel events are fired asynchronously on cache-miss only (prevents double-firing when
 * multiple components share the same page render).
 */
public class HstDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(HstDiscoveryService.class);
    public static final String BR_UID_2 = "_br_uid_2";

    private final DiscoveryClient client;
    private final DiscoveryConfigProvider configProvider;
    private final DiscoveryPixelService pixelService;
    private final SoREnrichmentProvider enrichmentProvider;

    public HstDiscoveryService(DiscoveryClient client,
                               DiscoveryConfigProvider configProvider,
                               DiscoveryPixelService pixelService,
                               SoREnrichmentProvider enrichmentProvider) {
        this.client = client;
        this.configProvider = configProvider;
        this.pixelService = pixelService;
        this.enrichmentProvider = enrichmentProvider;
    }

    // ── Request-based API (used by HST components) ─────────────────────────────

    public SearchResponse search(HstRequest request) {
        return search(request, 0, null, null, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort) {
        return search(request, componentPageSize, componentSort, null, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName) {
        return search(request, componentPageSize, componentSort, catalogName, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label) {
        return search(request, componentPageSize, componentSort, catalogName, label, List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label, List<String> statsFields) {
        return search(request, componentPageSize, componentSort, catalogName, label, statsFields, null, null);
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label, List<String> statsFields,
                                 String componentSegment, String efq) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        DiscoveryCredentials credentials = config.credentials();
        DiscoverySettings settings = config.settings();
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        SearchQuery baseQuery = QueryParamParser.toSearchQuery(
                paramProvider(request), settings, componentPageSize, componentSort, catalogName,
                brUid2, refUrl, url);
        SearchQuery query = statsFields != null && !statsFields.isEmpty()
                ? baseQuery.withStatsFields(statsFields) : baseQuery;
        if (query.segment() == null && componentSegment != null && !componentSegment.isBlank()) {
            query = query.withSegment(componentSegment);
        }
        if (efq != null && !efq.isBlank()) {
            query = query.withEfq(efq);
        }
        final SearchQuery finalQuery = query;
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getSearchResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.search(finalQuery, credentials, ctx);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putSearchResponse(request, label, fresh);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireSearchEvent(finalQuery, fresh.result(), credentials, clientIp, ctx, pixelFlags);
                    }
                    return fresh;
                });
    }

    public SearchResponse browse(HstRequest request, String categoryId) {
        return browse(request, categoryId, 0, null, "default", List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort) {
        return browse(request, categoryId, componentPageSize, componentSort, "default", List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort, String label) {
        return browse(request, categoryId, componentPageSize, componentSort, label, List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort,
                                 String label, List<String> statsFields) {
        return browse(request, categoryId, componentPageSize, componentSort, label, statsFields, null, null);
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort,
                                 String label, List<String> statsFields,
                                 String componentSegment, String efq) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        DiscoveryCredentials credentials = config.credentials();
        DiscoverySettings settings = config.settings();
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        CategoryQuery baseQuery = QueryParamParser.toCategoryQuery(
                categoryId, paramProvider(request), settings, componentPageSize, componentSort,
                brUid2, refUrl, url);
        CategoryQuery query = statsFields != null && !statsFields.isEmpty()
                ? baseQuery.withStatsFields(statsFields) : baseQuery;
        if (query.segment() == null && componentSegment != null && !componentSegment.isBlank()) {
            query = query.withSegment(componentSegment);
        }
        if (efq != null && !efq.isBlank()) {
            query = query.withEfq(efq);
        }
        final CategoryQuery finalQuery = query;
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getCategoryResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.category(finalQuery, credentials, ctx);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putCategoryResponse(request, label, fresh);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireCategoryEvent(finalQuery, fresh.result(), credentials, clientIp, ctx, pixelFlags);
                    }
                    return fresh;
                });
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String contextPageType,
                                           int limit, String fields, String filter) {
        return recommend(request, widgetId, widgetType, contextProductId, contextPageType,
                limit, fields, filter, "default");
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String contextPageType,
                                           int limit, String fields, String filter,
                                           String label) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        DiscoveryCredentials credentials = config.credentials();
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                     effectiveWidgetId, widgetType);
            return RecommendationResult.of(List.of());
        }

        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        String brUid2 = cookieValue(request, BR_UID_2);
        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, contextPageType,
                limit, fields, filter, url, refUrl, brUid2);
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getRecommendations(request, label, effectiveWidgetId)
                .orElseGet(() -> {
                    RecommendationResult fresh = client.recommend(query, credentials, ctx);
                    List<ProductSummary> enriched = applyEnrichment(fresh.products());
                    RecommendationResult result = new RecommendationResult(fresh.widgetResultId(), enriched);
                    DiscoveryRequestCache.putRecommendations(request, label, effectiveWidgetId, result);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireWidgetEvent(query, result, credentials, clientIp, ctx, pixelFlags);
                    }
                    return result;
                });
    }

    public Optional<ProductSummary> fetchProduct(HstRequest request, String pid) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        DiscoveryCredentials credentials = config.credentials();
        String url = pageUrl(request);
        String brUid2 = cookieValue(request, BR_UID_2);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        Optional<ProductSummary> result = client.fetchProduct(pid, url, credentials, ctx);
        if (result.isPresent()) {
            if (pixelService != null && pixelFlags.enabled()) {
                String clientIp = extractClientIp(request);
                String prodName = result.get().title();
                pixelService.fireProductPageViewEvent(pid, prodName, brUid2, refUrl, url,
                        credentials, clientIp, ctx, pixelFlags);
            }
            if (enrichmentProvider != null) {
                List<ProductSummary> enriched = enrichmentProvider.enrich(List.of(result.get()));
                return enriched.isEmpty() ? Optional.empty() : Optional.of(enriched.get(0));
            }
        }
        return result;
    }

    // ── Autosuggest (real-time, no caching, no pixels) ────────────────────────

    public AutosuggestResult autosuggest(HstRequest request, String query, int limit) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        DiscoveryCredentials credentials = config.credentials();
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        AutosuggestQuery suggestQuery = new AutosuggestQuery(query, limit, null,
                brUid2, refUrl, url);
        return client.autosuggest(suggestQuery, credentials, clientContext(request));
    }

    // ── Programmatic API (pre-built queries, no HST request param parsing) ──────

    public SearchResponse search(HstRequestContext ctx, SearchQuery query) {
        DiscoveryConfig config = configFor(ctx);
        return client.search(query, config.credentials(), ClientContext.EMPTY);
    }

    public SearchResponse browse(HstRequestContext ctx, CategoryQuery query) {
        DiscoveryConfig config = configFor(ctx);
        return client.category(query, config.credentials(), ClientContext.EMPTY);
    }

    public RecommendationResult recommend(HstRequestContext ctx, RecQuery query) {
        DiscoveryConfig config = configFor(ctx);
        return client.recommend(query, config.credentials(), ClientContext.EMPTY);
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private SearchResponse applyEnrichment(SearchResponse response) {
        if (enrichmentProvider == null) return response;
        SearchResult r = response.result();
        List<ProductSummary> enriched = enrichmentProvider.enrich(r.products());
        SearchResult enrichedResult = new SearchResult(enriched, r.total(), r.page(), r.pageSize(), r.facets());
        return new SearchResponse(enrichedResult, response.metadata());
    }

    private List<ProductSummary> applyEnrichment(List<ProductSummary> products) {
        if (enrichmentProvider == null) {
            return products;
        }
        return enrichmentProvider.enrich(products);
    }

    /**
     * Resolves pixel control flags for the current request.
     * <ul>
     *   <li>Env kill switch ({@code brxdis.pixel.envEnabled=false}) disables all pixels globally.</li>
     *   <li>{@link DiscoveryChannelInfo} typed accessors drive per-channel flags.</li>
     *   <li>If no channel info is configured, falls back to env/system property defaults.</li>
     * </ul>
     */
    private static PixelFlags resolvePixelFlags(HstRequest request) {
        if (!PixelFlags.envEnabled()) return PixelFlags.DISABLED;
        Mount mount = request.getRequestContext().getResolvedMount().getMount();
        DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
        String region = resolvePixelRegion(channelInfo);
        if (channelInfo == null) {
            return new PixelFlags(true, PixelFlags.envTestData(), PixelFlags.envDebug(), region);
        }
        if (!channelInfo.getDiscoveryPixelsEnabled()) return PixelFlags.DISABLED;
        return new PixelFlags(true, channelInfo.getDiscoveryPixelTestData(), channelInfo.getDiscoveryPixelDebug(), region);
    }

    /**
     * Resolves the pixel endpoint region.
     * Precedence: system property {@code brxdis.pixel.region} → Channel Manager → "US".
     */
    static String resolvePixelRegion(DiscoveryChannelInfo channelInfo) {
        String sysProp = System.getProperty("brxdis.pixel.region");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.toUpperCase();
        if (channelInfo != null) {
            String channelRegion = channelInfo.getPixelRegion();
            if (channelRegion != null && !channelRegion.isBlank()) return channelRegion.toUpperCase();
        }
        return "US";
    }

    private DiscoveryConfig configFor(HstRequestContext ctx) {
        Session requestSession = null;
        try {
            requestSession = ctx.getSession();
        } catch (RepositoryException e) {
            log.debug("[configFor] Cannot acquire request JCR session: {}", e.getMessage());
        }
        DiscoveryConfig config = configProvider.get(requestSession);
        DiscoveryCredentials credentials = config.credentials();
        if (log.isDebugEnabled()) {
            log.debug("[configFor] accountId='{}' domainKey='{}' apiKey={} authKey={}",
                    credentials.accountId(), credentials.domainKey(),
                    maskSecret(credentials.apiKey()), maskSecret(credentials.authKey()));
        }
        validateCredentials(credentials);
        return config;
    }

    private static String maskSecret(String s) {
        return s == null ? "null" : (s.isBlank() ? "blank" : "set");
    }

    private static void validateCredentials(DiscoveryCredentials credentials) {
        if (isBlank(credentials.accountId())) throw new ConfigurationException(
                "Discovery accountId is required — set BRXDIS_ACCOUNT_ID env var, -Dbrxdis.accountId, or brxdis:accountId JCR property");
        if (isBlank(credentials.domainKey())) throw new ConfigurationException(
                "Discovery domainKey is required — set BRXDIS_DOMAIN_KEY env var, -Dbrxdis.domainKey, or brxdis:domainKey JCR property");
        if (isBlank(credentials.apiKey())) throw new ConfigurationException(
                "Discovery apiKey is required — set brxdis:apiKey in the config node, BRXDIS_API_KEY env var, or -Dbrxdis.apiKey");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static ClientContext clientContext(HstRequest request) {
        return new ClientContext(
                request.getHeader("User-Agent"),
                request.getHeader("Accept-Language"),
                request.getHeader("X-Forwarded-For")
        );
    }

    private static String extractClientIp(HstRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }

    private static QueryParamParser.RequestParamProvider paramProvider(HstRequest request) {
        jakarta.servlet.http.HttpServletRequest servletRequest =
                request.getRequestContext().getServletRequest();
        return new QueryParamParser.RequestParamProvider() {
            @Override public String getParameter(String name) { return servletRequest.getParameter(name); }
            @Override public Map<String, String[]> getParameterMap() { return servletRequest.getParameterMap(); }
        };
    }

    private static String cookieValue(HstRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private static String pageUrl(HstRequest request) {
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .replacePath(request.getRequestURI());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            urlBuilder.port(port);
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            urlBuilder.query(query);
        }
        return urlBuilder.build(false).toUriString();
    }
}
