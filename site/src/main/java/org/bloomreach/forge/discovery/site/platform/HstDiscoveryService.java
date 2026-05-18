package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.component.SearchRequestOptions;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DeferredPixelInteractionHandler;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelConsentProvider;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryApiClient;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.CategoryPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.ProductPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.TrackingContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.WidgetView;
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
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.bloomreach.forge.discovery.request.DiscoveryRequestFactory.toV2WidgetType;

/**
 * HST-aware façade that absorbs config resolution, query building, and request-cache logic.
 * Components become thin: they extract raw HST params, delegate here, and set model/attributes.
 * <p>
 * Config is resolved via {@link DiscoveryConfigProvider} - JVM-lifetime cache with JCR
 * observation-driven invalidation. No per-request JCR reads.
 * <p>
 * Pixel events are fired asynchronously on cache-miss only (prevents double-firing when
 * multiple components share the same page render).
 */
public class HstDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(HstDiscoveryService.class);
    public static final String BR_UID_2 = "_br_uid_2";

    private final DiscoveryApiClient client;
    private final DiscoveryPixelService pixelService;
    private final DeferredPixelInteractionHandler deferredHandler;
    private final SoREnrichmentProvider enrichmentProvider;
    private final DiscoveryRuntimeContextFactory runtimeContextFactory;
    private final PixelConsentProvider consentProvider;

    public HstDiscoveryService(DiscoveryApiClient client,
                               DiscoveryRuntimeContextFactory runtimeContextFactory,
                               DiscoveryPixelService pixelService,
                               SoREnrichmentProvider enrichmentProvider) {
        this(client, runtimeContextFactory, pixelService, enrichmentProvider, null);
    }

    public HstDiscoveryService(DiscoveryApiClient client,
                               DiscoveryRuntimeContextFactory runtimeContextFactory,
                               DiscoveryPixelService pixelService,
                               SoREnrichmentProvider enrichmentProvider,
                               PixelConsentProvider consentProvider) {
        this.client = client;
        this.pixelService = pixelService;
        this.deferredHandler = pixelService != null ? new DeferredPixelInteractionHandler(pixelService) : null;
        this.enrichmentProvider = enrichmentProvider;
        this.runtimeContextFactory = runtimeContextFactory;
        this.consentProvider = consentProvider;
    }

    // ── Request-based API (used by HST components) ─────────────────────────────

    public SearchResponse search(HstRequest request) {
        return search(request, SearchRequestOptions.defaults());
    }

    public SearchResponse search(HstRequest request, SearchRequestOptions options) {
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        String catalogName = options.catalogName() != null ? options.catalogName() : runtimeContext.catalogName();
        SearchQuery baseQuery = QueryParamParser.toSearchQuery(
                runtimeContext.paramProvider(), runtimeContext.settings(),
                options.pageSize(), options.sort(), catalogName,
                runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.pageUrl());
        SearchQuery query = baseQuery
                .withFields(runtimeContext.settings().schemaConfig().defaultFieldList());
        query = options.statsFields() != null && !options.statsFields().isEmpty()
                ? query.withStatsFields(options.statsFields()) : query;
        if (query.segment() == null && options.segment() != null && !options.segment().isBlank()) {
            query = query.withSegment(options.segment());
        }
        if (options.efq() != null && !options.efq().isBlank()) {
            query = query.withEfq(options.efq());
        }
        SearchResponse response = client.search(query, runtimeContext.credentials(), runtimeContext.clientContext());
        response = applyEnrichment(response);
        if (shouldFirePixels(request, runtimeContext)) {
            deferredHandler.handleSearchInteraction(request, runtimeContext, query);
            TrackingContext searchTracking = new TrackingContext(
                    query.brUid2(), query.refUrl(), query.origRefUrl(), query.url(), runtimeContext.pageTitle());
            pixelService.fire(new SearchPageView(searchTracking, query.query(), response.result().products()),
                    runtimeContext.credentials(), runtimeContext.clientIp(),
                    runtimeContext.clientContext(), runtimeContext.pixelFlags());
        }
        return response;
    }

    public SearchResponse browse(HstRequest request, String categoryId) {
        return browse(request, categoryId, SearchRequestOptions.defaults());
    }

    public SearchResponse browse(HstRequest request, String categoryId, SearchRequestOptions options) {
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        String catalogName = options.catalogName() != null ? options.catalogName() : runtimeContext.catalogName();
        CategoryQuery baseQuery = QueryParamParser.toCategoryQuery(
                categoryId, runtimeContext.paramProvider(), runtimeContext.settings(),
                options.pageSize(), options.sort(),
                runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.pageUrl());
        CategoryQuery query = baseQuery
                .withFields(runtimeContext.settings().schemaConfig().defaultFieldList());
        if (catalogName != null) {
            query = query.withCatalogName(catalogName);
        }
        query = options.statsFields() != null && !options.statsFields().isEmpty()
                ? query.withStatsFields(options.statsFields()) : query;
        if (query.segment() == null && options.segment() != null && !options.segment().isBlank()) {
            query = query.withSegment(options.segment());
        }
        if (options.efq() != null && !options.efq().isBlank()) {
            query = query.withEfq(options.efq());
        }
        SearchResponse response = client.category(query, runtimeContext.credentials(), runtimeContext.clientContext());
        response = applyEnrichment(response);
        if (shouldFirePixels(request, runtimeContext)) {
            TrackingContext browseTracking = new TrackingContext(
                    query.brUid2(), query.refUrl(), query.origRefUrl(), query.url(), runtimeContext.pageTitle());
            pixelService.fire(new CategoryPageView(browseTracking, query.categoryId(), response.metadata().categoryName(), response.result().products()),
                    runtimeContext.credentials(), runtimeContext.clientIp(),
                    runtimeContext.clientContext(), runtimeContext.pixelFlags());
        }
        return response;
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String catId,
                                           String contextPageType,
                                           int limit, String fields, String filter) {
        return recommend(request, widgetId, widgetType, contextProductId, catId, contextPageType, null, limit, fields, filter);
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String catId,
                                           String contextPageType, String contextQuery,
                                           int limit, String fields, String filter) {
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                    effectiveWidgetId, widgetType);
            return RecommendationResult.of(List.of());
        }

        String effectiveFields = (fields != null && !fields.isBlank())
                ? fields : runtimeContext.settings().schemaConfig().defaultFieldList();
        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, catId, contextPageType,
                limit, effectiveFields, filter, runtimeContext.pageUrl(), runtimeContext.refUrl(),
                runtimeContext.brUid2(), runtimeContext.origRefUrl(), contextQuery, null);
        RecommendationResult fresh = client.recommend(query, runtimeContext.credentials(), runtimeContext.clientContext());
        List<ProductSummary> enriched = applyEnrichment(fresh.products());
        RecommendationResult result = fresh.withProducts(enriched);
        if (shouldFirePixels(request, runtimeContext)) {
            String resolvedWidgetId = result.widgetId() != null && !result.widgetId().isBlank()
                    ? result.widgetId() : query.widgetId();
            String resolvedWidgetType = result.widgetType() != null && !result.widgetType().isBlank()
                    ? result.widgetType() : query.widgetType();
            TrackingContext widgetTracking = new TrackingContext(
                    query.brUid2(), query.refUrl(), query.origRefUrl(), query.url(), runtimeContext.pageTitle());
            pixelService.fire(new WidgetView(widgetTracking, resolvedWidgetId, resolvedWidgetType,
                            result.widgetResultId(), query.contextProductId(), contextPageType, result.products()),
                    runtimeContext.credentials(), runtimeContext.clientIp(),
                    runtimeContext.clientContext(), runtimeContext.pixelFlags());
        }
        return result;
    }

    public List<ProductSummary> visualSearch(HstRequest request,
                                              String widgetId, String imageId,
                                              String objectId, int rows) {
        DiscoveryRuntimeContext ctx = runtimeContextFactory.get(request);
        VisualSearchQuery query = new VisualSearchQuery(
                widgetId, imageId, objectId, rows,
                ctx.settings().schemaConfig().defaultFieldList(),
                ctx.pageUrl(), ctx.refUrl(), ctx.brUid2());
        List<ProductSummary> products = client.visualSearch(query, ctx.credentials(), ctx.clientContext());
        return applyEnrichment(products);
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
                runtimeContext.settings().schemaConfig().defaultFieldList(),
                runtimeContext.credentials(), runtimeContext.clientContext());
        if (result.isEmpty()) {
            return Optional.empty();
        }

        ProductSummary product = result.get();
        if (shouldFirePixels(request, runtimeContext)) {
            deferredHandler.handleProductInteraction(request, runtimeContext, product);
            TrackingContext productTracking = new TrackingContext(
                    runtimeContext.brUid2(), runtimeContext.refUrl(), runtimeContext.origRefUrl(),
                    runtimeContext.pageUrl(), runtimeContext.pageTitle());
            pixelService.fire(new ProductPageView(productTracking, pid, product.title()),
                    runtimeContext.credentials(), runtimeContext.clientIp(),
                    runtimeContext.clientContext(), runtimeContext.pixelFlags());
        }
        if (enrichmentProvider != null) {
            List<ProductSummary> enriched = enrichmentProvider.enrich(List.of(product));
            if (enriched.isEmpty()) {
                return Optional.empty();
            }
            product = enriched.getFirst();
        }
        DiscoveryRequestCache.putFetchedProduct(request, pid, product);
        return Optional.of(product);
    }

    // ── Autosuggest (real-time, no caching, no pixels) ────────────────────────

    public AutosuggestResult autosuggest(HstRequest request, String query, int limit) {
        DiscoveryRuntimeContext runtimeContext = runtimeContextFactory.get(request);
        AutosuggestQuery suggestQuery = new AutosuggestQuery(query, limit, runtimeContext.catalogName(),
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

    /**
     * Single gate for all pixel firing decisions.
     * Resolution order:
     * 1. pixelService null or channel kill-switch off → suppress
     * 2. PixelConsentProvider registered → delegate to it
     * 3. discoveryPixelConsentCookie set → check request cookie presence
     * 4. No consent requirement configured → fire unconditionally
     */
    private boolean shouldFirePixels(HstRequest request, DiscoveryRuntimeContext ctx) {
        if (pixelService == null || !ctx.pixelFlags().enabled()) return false;
        if (consentProvider != null) return consentProvider.hasConsent(request);
        String cookieName = ctx.pixelConsentCookie();
        if (cookieName == null) return true;
        Cookie[] cookies = request.getCookies();
        return cookies != null && Arrays.stream(cookies).anyMatch(c -> cookieName.equals(c.getName()));
    }

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

}
