package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigResolver;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
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

    public SearchResult search(HstRequest request) {
        return search(request, 0, null);
    }

    public SearchResult search(HstRequest request, int componentPageSize, String componentSort) {
        return search(request, componentPageSize, componentSort, null);
    }

    public SearchResult search(HstRequest request, int componentPageSize, String componentSort,
                               String catalogName) {
        return search(request, componentPageSize, componentSort, catalogName, "default");
    }

    public SearchResult search(HstRequest request, int componentPageSize, String componentSort,
                               String catalogName, String band) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String brUid2 = cookieValue(request, "_br_uid_2");
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        SearchQuery query = QueryParamParser.toSearchQuery(
                paramProvider(request), config, componentPageSize, componentSort, catalogName,
                brUid2, refUrl, url);
        return DiscoveryRequestCache.getSearchResult(request, band)
                .orElseGet(() -> {
                    SearchResult fresh = client.search(query, config);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putSearchResult(request, band, fresh);
                    if (pixelService != null) {
                        pixelService.fireSearchEvent(query, fresh, config);
                    }
                    return fresh;
                });
    }

    public SearchResult browse(HstRequest request, String categoryId) {
        return browse(request, categoryId, 0, null);
    }

    public SearchResult browse(HstRequest request, String categoryId,
                               int componentPageSize, String componentSort) {
        return browse(request, categoryId, componentPageSize, componentSort, "default");
    }

    public SearchResult browse(HstRequest request, String categoryId,
                               int componentPageSize, String componentSort, String band) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String brUid2 = cookieValue(request, "_br_uid_2");
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        CategoryQuery query = QueryParamParser.toCategoryQuery(
                categoryId, paramProvider(request), config, componentPageSize, componentSort,
                brUid2, refUrl, url);
        return DiscoveryRequestCache.getCategoryResult(request, band)
                .orElseGet(() -> {
                    SearchResult fresh = client.category(query, config);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putCategoryResult(request, band, fresh);
                    if (pixelService != null) {
                        pixelService.fireCategoryEvent(query, fresh, config);
                    }
                    return fresh;
                });
    }

    public List<ProductSummary> recommend(HstRequest request,
                                          String widgetId, String widgetType,
                                          String contextProductId, String contextPageType,
                                          int limit, String fields, String filter) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                     effectiveWidgetId, widgetType);
            return List.of();
        }

        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        String brUid2 = cookieValue(request, "_br_uid_2");
        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, contextPageType,
                limit, fields, filter, url, refUrl, brUid2);
        return DiscoveryRequestCache.getRecommendations(request, effectiveWidgetId)
                .orElseGet(() -> {
                    List<ProductSummary> fresh = client.recommend(query, config);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putRecommendations(request, effectiveWidgetId, fresh);
                    if (pixelService != null) {
                        pixelService.fireWidgetEvent(query, fresh, config);
                    }
                    return fresh;
                });
    }

    public Optional<ProductSummary> fetchProduct(HstRequest request, String pid) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        Optional<ProductSummary> result = client.fetchProduct(pid, pageUrl(request), config);
        if (result.isPresent() && enrichmentProvider != null) {
            List<ProductSummary> enriched = enrichmentProvider.enrich(List.of(result.get()));
            return enriched.isEmpty() ? Optional.empty() : Optional.of(enriched.get(0));
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

    public SearchResult search(HstRequestContext ctx, SearchQuery query) {
        return client.search(query, configFor(ctx));
    }

    public SearchResult browse(HstRequestContext ctx, CategoryQuery query) {
        return client.category(query, configFor(ctx));
    }

    public List<ProductSummary> recommend(HstRequestContext ctx, RecQuery query) {
        return client.recommend(query, configFor(ctx));
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private SearchResult applyEnrichment(SearchResult result) {
        if (enrichmentProvider == null) {
            return result;
        }
        List<ProductSummary> enriched = enrichmentProvider.enrich(result.products());
        return new SearchResult(enriched, result.total(), result.page(), result.pageSize(), result.facets());
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
