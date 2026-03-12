package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
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
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static org.bloomreach.forge.discovery.request.DiscoveryRequestFactory.toV2WidgetType;

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
    private final DiscoveryPixelService pixelService;
    private final SoREnrichmentProvider enrichmentProvider;
    private final DiscoveryRuntimeContextFactory runtimeContextFactory;

    public HstDiscoveryService(DiscoveryClient client,
                               DiscoveryConfigProvider configProvider,
                               DiscoveryPixelService pixelService,
                               SoREnrichmentProvider enrichmentProvider) {
        this.client = client;
        this.pixelService = pixelService;
        this.enrichmentProvider = enrichmentProvider;
        this.runtimeContextFactory = new DiscoveryRuntimeContextFactory(configProvider);
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
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        SearchQuery baseQuery = QueryParamParser.toSearchQuery(
                runtimeContext.paramProvider(), runtimeContext.settings(), componentPageSize, componentSort, catalogName,
                runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.pageUrl());
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
                    SearchResponse fresh = client.search(finalQuery, runtimeContext.credentials(), runtimeContext.clientContext());
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putSearchResponse(request, label, fresh);
                    if (pixelService != null && runtimeContext.pixelFlags().enabled()) {
                        pixelService.fireSearchEvent(finalQuery, fresh.result(), runtimeContext.credentials(),
                                runtimeContext.clientIp(), runtimeContext.clientContext(), runtimeContext.pixelFlags());
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
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        CategoryQuery baseQuery = QueryParamParser.toCategoryQuery(
                categoryId, runtimeContext.paramProvider(), runtimeContext.settings(), componentPageSize, componentSort,
                runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.pageUrl());
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
                    SearchResponse fresh = client.category(finalQuery, runtimeContext.credentials(), runtimeContext.clientContext());
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putCategoryResponse(request, label, fresh);
                    if (pixelService != null && runtimeContext.pixelFlags().enabled()) {
                        pixelService.fireCategoryEvent(finalQuery, fresh.result(), runtimeContext.credentials(),
                                runtimeContext.clientIp(), runtimeContext.clientContext(), runtimeContext.pixelFlags());
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
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                    effectiveWidgetId, widgetType);
            return RecommendationResult.of(List.of());
        }

        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, contextPageType,
                limit, fields, filter, runtimeContext.pageUrl(), runtimeContext.refUrl(), runtimeContext.brUid2());
        return DiscoveryRequestCache.getRecommendations(request, label, effectiveWidgetId)
                .orElseGet(() -> {
                    RecommendationResult fresh = client.recommend(query, runtimeContext.credentials(), runtimeContext.clientContext());
                    List<ProductSummary> enriched = applyEnrichment(fresh.products());
                    RecommendationResult result = new RecommendationResult(fresh.widgetResultId(), enriched);
                    DiscoveryRequestCache.putRecommendations(request, label, effectiveWidgetId, result);
                    if (pixelService != null && runtimeContext.pixelFlags().enabled()) {
                        pixelService.fireWidgetEvent(query, result, runtimeContext.credentials(),
                                runtimeContext.clientIp(), runtimeContext.clientContext(), runtimeContext.pixelFlags());
                    }
                    return result;
                });
    }

    public Optional<ProductSummary> fetchProduct(HstRequest request, String pid) {
        if (pid == null || pid.isBlank()) {
            return Optional.empty();
        }
        Optional<ProductSummary> cached = DiscoveryRequestCache.getFetchedProduct(request, pid);
        if (cached.isPresent()) {
            return cached;
        }

        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        Optional<ProductSummary> result = client.fetchProduct(pid, runtimeContext.pageUrl(),
                runtimeContext.credentials(), runtimeContext.clientContext());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        ProductSummary product = result.get();
        if (pixelService != null && runtimeContext.pixelFlags().enabled()) {
            pixelService.fireProductPageViewEvent(pid, product.title(), runtimeContext.brUid2(),
                    runtimeContext.refUrl(), runtimeContext.pageUrl(), runtimeContext.credentials(),
                    runtimeContext.clientIp(), runtimeContext.clientContext(), runtimeContext.pixelFlags());
        }
        if (enrichmentProvider != null) {
            List<ProductSummary> enriched = enrichmentProvider.enrich(List.of(product));
            if (enriched.isEmpty()) {
                return Optional.empty();
            }
            product = enriched.get(0);
        }
        DiscoveryRequestCache.putFetchedProduct(request, pid, product);
        return Optional.of(product);
    }

    // ── Autosuggest (real-time, no caching, no pixels) ────────────────────────

    public AutosuggestResult autosuggest(HstRequest request, String query, int limit) {
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        AutosuggestQuery suggestQuery = new AutosuggestQuery(query, limit, null,
                runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.pageUrl());
        return client.autosuggest(suggestQuery, runtimeContext.credentials(), runtimeContext.clientContext());
    }

    // ── Programmatic API (pre-built queries, no HST request param parsing) ──────

    public SearchResponse search(HstRequestContext ctx, SearchQuery query) {
        DiscoveryConfig config = runtimeContextFactory.configFor(ctx);
        return client.search(query, config.credentials(), ClientContext.EMPTY);
    }

    public SearchResponse browse(HstRequestContext ctx, CategoryQuery query) {
        DiscoveryConfig config = runtimeContextFactory.configFor(ctx);
        return client.category(query, config.credentials(), ClientContext.EMPTY);
    }

    public RecommendationResult recommend(HstRequestContext ctx, RecQuery query) {
        DiscoveryConfig config = runtimeContextFactory.configFor(ctx);
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

    static String resolvePixelRegion(DiscoveryChannelInfo channelInfo) {
        return DiscoveryRuntimeContextFactory.resolvePixelRegion(channelInfo);
    }
}
