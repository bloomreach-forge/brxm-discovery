package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigResolver;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.sor.SoREnrichmentProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Cookie;
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
        String brUid2 = cookieValue(request, "_br_uid_2");
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        SearchQuery baseQuery = QueryParamParser.toSearchQuery(
                paramProvider(request), config, componentPageSize, componentSort, catalogName,
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
        return DiscoveryRequestCache.getSearchResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.search(finalQuery, config);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putSearchResponse(request, label, fresh);
                    if (pixelService != null) {
                        String clientIp = extractClientIp(request);
                        String userAgent = request.getHeader("User-Agent");
                        pixelService.fireSearchEvent(finalQuery, fresh.result(), config, clientIp, userAgent);
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
        String brUid2 = cookieValue(request, "_br_uid_2");
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        CategoryQuery baseQuery = QueryParamParser.toCategoryQuery(
                categoryId, paramProvider(request), config, componentPageSize, componentSort,
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
        return DiscoveryRequestCache.getCategoryResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.category(finalQuery, config);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putCategoryResponse(request, label, fresh);
                    if (pixelService != null) {
                        String clientIp = extractClientIp(request);
                        String userAgent = request.getHeader("User-Agent");
                        pixelService.fireCategoryEvent(finalQuery, fresh.result(), config, clientIp, userAgent);
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
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                     effectiveWidgetId, widgetType);
            return RecommendationResult.of(List.of());
        }

        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        String brUid2 = cookieValue(request, "_br_uid_2");
        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, contextPageType,
                limit, fields, filter, url, refUrl, brUid2);
        return DiscoveryRequestCache.getRecommendations(request, label, effectiveWidgetId)
                .orElseGet(() -> {
                    RecommendationResult fresh = client.recommend(query, config);
                    List<ProductSummary> enriched = applyEnrichment(fresh.products());
                    RecommendationResult result = new RecommendationResult(fresh.widgetResultId(), enriched);
                    DiscoveryRequestCache.putRecommendations(request, label, effectiveWidgetId, result);
                    if (pixelService != null) {
                        String clientIp = extractClientIp(request);
                        String userAgent = request.getHeader("User-Agent");
                        pixelService.fireWidgetEvent(query, result, config, clientIp, userAgent);
                    }
                    return result;
                });
    }

    public Optional<ProductSummary> fetchProduct(HstRequest request, String pid) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String url = pageUrl(request);
        String brUid2 = cookieValue(request, "_br_uid_2");
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        Optional<ProductSummary> result = client.fetchProduct(pid, url, config);
        if (result.isPresent()) {
            if (pixelService != null) {
                String clientIp = extractClientIp(request);
                String userAgent = request.getHeader("User-Agent");
                pixelService.fireProductPageViewEvent(pid, brUid2, refUrl, url, config, clientIp, userAgent);
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
        String brUid2 = cookieValue(request, "_br_uid_2");
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        AutosuggestQuery suggestQuery = new AutosuggestQuery(query, limit, null,
                brUid2, refUrl, url);
        return client.autosuggest(suggestQuery, config);
    }

    // ── Programmatic API (pre-built queries, no HST request param parsing) ──────

    public SearchResponse search(HstRequestContext ctx, SearchQuery query) {
        return client.search(query, configFor(ctx));
    }

    public SearchResponse browse(HstRequestContext ctx, CategoryQuery query) {
        return client.category(query, configFor(ctx));
    }

    public RecommendationResult recommend(HstRequestContext ctx, RecQuery query) {
        return client.recommend(query, configFor(ctx));
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

    private DiscoveryConfig configFor(HstRequestContext ctx) {
        String path = ctx.getResolvedMount().getMount()
                .getParameter(DiscoveryConfigResolver.CONFIG_PATH_PARAM);
        return configProvider.get(path);
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
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }
        url.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            url.append("?").append(query);
        }
        return url.toString();
    }
}
