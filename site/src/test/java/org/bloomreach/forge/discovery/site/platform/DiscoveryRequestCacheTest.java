package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DiscoveryRequestCacheTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;

    private final Map<String, Object> attrs = new HashMap<>();
    private final SearchResult result = new SearchResult(List.of(), 10L, 0, 10, Map.of());
    private final SearchResponse response = new SearchResponse(result, SearchMetadata.empty());

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> {
            attrs.put((String) inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(requestContext).setAttribute(anyString(), any());
    }

    // ── band-aware search ────────────────────────────────────────────────────

    @Test
    void getSearchResponse_unknownBand_returnsEmpty() {
        assertTrue(DiscoveryRequestCache.getSearchResponse(request, "foo").isEmpty());
    }

    @Test
    void putAndGet_searchResponse_withNamedBand_roundTrips() {
        DiscoveryRequestCache.putSearchResponse(request, "my-band", response);
        Optional<SearchResponse> got = DiscoveryRequestCache.getSearchResponse(request, "my-band");
        assertTrue(got.isPresent());
        assertSame(response, got.get());
    }

    @Test
    void searchResponse_bands_areIndependent() {
        SearchResult r2Result = new SearchResult(List.of(), 99L, 0, 5, Map.of());
        SearchResponse r2 = new SearchResponse(r2Result, SearchMetadata.empty());
        DiscoveryRequestCache.putSearchResponse(request, "band-a", response);
        DiscoveryRequestCache.putSearchResponse(request, "band-b", r2);

        assertSame(response, DiscoveryRequestCache.getSearchResponse(request, "band-a").orElseThrow());
        assertSame(r2, DiscoveryRequestCache.getSearchResponse(request, "band-b").orElseThrow());
        assertTrue(DiscoveryRequestCache.getSearchResponse(request, "other").isEmpty());
    }

    @Test
    void noArgSearchResponse_delegatesToDefaultBand() {
        DiscoveryRequestCache.putSearchResponse(request, "default", response);
        Optional<SearchResponse> noArgResult = DiscoveryRequestCache.getSearchResponse(request);
        assertTrue(noArgResult.isPresent());
        assertSame(response, noArgResult.get());
    }

    @Test
    void noArgPutSearchResponse_storedUnderDefaultBand() {
        DiscoveryRequestCache.putSearchResponse(request, response);
        Optional<SearchResponse> bandResult = DiscoveryRequestCache.getSearchResponse(request, "default");
        assertTrue(bandResult.isPresent());
        assertSame(response, bandResult.get());
    }

    @Test
    void defaultBand_andNamedBand_areIndependent() {
        SearchResponse other = new SearchResponse(new SearchResult(List.of(), 7L, 0, 5, Map.of()), SearchMetadata.empty());
        DiscoveryRequestCache.putSearchResponse(request, response);      // → "default"
        DiscoveryRequestCache.putSearchResponse(request, "xyz", other);

        assertSame(response, DiscoveryRequestCache.getSearchResponse(request).orElseThrow());
        assertSame(other, DiscoveryRequestCache.getSearchResponse(request, "xyz").orElseThrow());
    }

    // ── band-aware category ──────────────────────────────────────────────────

    @Test
    void getCategoryResponse_unknownBand_returnsEmpty() {
        assertTrue(DiscoveryRequestCache.getCategoryResponse(request, "foo").isEmpty());
    }

    @Test
    void putAndGet_categoryResponse_withNamedBand_roundTrips() {
        DiscoveryRequestCache.putCategoryResponse(request, "cat-band", response);
        Optional<SearchResponse> got = DiscoveryRequestCache.getCategoryResponse(request, "cat-band");
        assertTrue(got.isPresent());
        assertSame(response, got.get());
    }

    @Test
    void noArgCategoryResponse_delegatesToDefaultBand() {
        DiscoveryRequestCache.putCategoryResponse(request, "default", response);
        assertTrue(DiscoveryRequestCache.getCategoryResponse(request).isPresent());
    }

    @Test
    void noArgPutCategoryResponse_storedUnderDefaultBand() {
        DiscoveryRequestCache.putCategoryResponse(request, response);
        assertTrue(DiscoveryRequestCache.getCategoryResponse(request, "default").isPresent());
    }

    // ── band-presence markers ────────────────────────────────────────────────

    @Test
    void isSearchBandPresent_returnsFalse_beforeMark() {
        assertFalse(DiscoveryRequestCache.isSearchBandPresent(request, "default"));
    }

    @Test
    void isCategoryBandPresent_returnsFalse_beforeMark() {
        assertFalse(DiscoveryRequestCache.isCategoryBandPresent(request, "default"));
    }

    @Test
    void markSearchBandPresent_roundTrips() {
        DiscoveryRequestCache.markSearchBandPresent(request, "my-band");
        assertTrue(DiscoveryRequestCache.isSearchBandPresent(request, "my-band"));
    }

    @Test
    void markCategoryBandPresent_roundTrips() {
        DiscoveryRequestCache.markCategoryBandPresent(request, "cat-band");
        assertTrue(DiscoveryRequestCache.isCategoryBandPresent(request, "cat-band"));
    }

    @Test
    void searchMarker_doesNotLeakToCategory() {
        DiscoveryRequestCache.markSearchBandPresent(request, "shared");
        assertFalse(DiscoveryRequestCache.isCategoryBandPresent(request, "shared"));
    }

    @Test
    void categoryMarker_doesNotLeakToSearch() {
        DiscoveryRequestCache.markCategoryBandPresent(request, "shared");
        assertFalse(DiscoveryRequestCache.isSearchBandPresent(request, "shared"));
    }

    @Test
    void searchBandMarker_independentOfResultCache() {
        // Marker present, no result → valid state (data source on page, no query typed yet)
        DiscoveryRequestCache.markSearchBandPresent(request, "default");
        assertTrue(DiscoveryRequestCache.isSearchBandPresent(request, "default"));
        assertTrue(DiscoveryRequestCache.getSearchResponse(request, "default").isEmpty());
    }

    // ── product detail band ──────────────────────────────────────────────────

    private final ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of());

    @Test
    void isProductDetailBandPresent_returnsFalse_beforeMark() {
        assertFalse(DiscoveryRequestCache.isProductDetailBandPresent(request, "default"));
    }

    @Test
    void markProductDetailBandPresent_roundTrips() {
        DiscoveryRequestCache.markProductDetailBandPresent(request, "pdp-band");
        assertTrue(DiscoveryRequestCache.isProductDetailBandPresent(request, "pdp-band"));
    }

    @Test
    void putAndGet_productResult_withNamedBand_roundTrips() {
        DiscoveryRequestCache.putProductResult(request, "pdp-band", product);
        Optional<ProductSummary> got = DiscoveryRequestCache.getProductResult(request, "pdp-band");
        assertTrue(got.isPresent());
        assertSame(product, got.get());
    }

    @Test
    void productDetailBands_areIndependent() {
        ProductSummary p2 = new ProductSummary("p-2", "U", null, null, null, null, Map.of());
        DiscoveryRequestCache.putProductResult(request, "band-a", product);
        DiscoveryRequestCache.putProductResult(request, "band-b", p2);

        assertSame(product, DiscoveryRequestCache.getProductResult(request, "band-a").orElseThrow());
        assertSame(p2,      DiscoveryRequestCache.getProductResult(request, "band-b").orElseThrow());
        assertTrue(DiscoveryRequestCache.getProductResult(request, "other").isEmpty());
    }

    @Test
    void productDetailMarker_doesNotLeakToSearchOrCategory() {
        DiscoveryRequestCache.markProductDetailBandPresent(request, "shared");
        assertFalse(DiscoveryRequestCache.isSearchBandPresent(request, "shared"));
        assertFalse(DiscoveryRequestCache.isCategoryBandPresent(request, "shared"));
    }

    @Test
    void productDetailBandMarker_independentOfResultCache() {
        // Marker present, no result → valid state (PDP ran, but no product found)
        DiscoveryRequestCache.markProductDetailBandPresent(request, "default");
        assertTrue(DiscoveryRequestCache.isProductDetailBandPresent(request, "default"));
        assertTrue(DiscoveryRequestCache.getProductResult(request, "default").isEmpty());
    }

    @Test
    void putAndGet_fetchedProductByPid_roundTrips() {
        DiscoveryRequestCache.putFetchedProduct(request, "sku-1", product);

        Optional<ProductSummary> got = DiscoveryRequestCache.getFetchedProduct(request, "sku-1");

        assertTrue(got.isPresent());
        assertSame(product, got.get());
    }

    @Test
    void fetchedProducts_areIndependentByPid() {
        ProductSummary p2 = new ProductSummary("p-2", "U", null, null, null, null, Map.of());
        DiscoveryRequestCache.putFetchedProduct(request, "sku-1", product);
        DiscoveryRequestCache.putFetchedProduct(request, "sku-2", p2);

        assertSame(product, DiscoveryRequestCache.getFetchedProduct(request, "sku-1").orElseThrow());
        assertSame(p2, DiscoveryRequestCache.getFetchedProduct(request, "sku-2").orElseThrow());
        assertTrue(DiscoveryRequestCache.getFetchedProduct(request, "missing").isEmpty());
    }

    // ── query-aware recommendations ─────────────────────────────────────────

    @Test
    void getRecommendations_unknownQuery_returnsEmpty() {
        RecQuery query = new RecQuery("item", "w1", "sku-1", null, 8, null, null, "/page", "/ref", "uid");
        assertTrue(DiscoveryRequestCache.getRecommendations(request, query).isEmpty());
    }

    @Test
    void putAndGet_recommendations_roundTrips() {
        RecQuery query = new RecQuery("item", "w1", "sku-1", null, 8, null, null, "/page", "/ref", "uid");
        RecommendationResult recResult = RecommendationResult.of(List.of(product));
        DiscoveryRequestCache.putRecommendations(request, query, recResult);
        Optional<RecommendationResult> got = DiscoveryRequestCache.getRecommendations(request, query);
        assertTrue(got.isPresent());
        assertSame(recResult, got.get());
    }

    @Test
    void putAndGetRecommendations_roundTripsRecommendationResult() {
        RecQuery query = new RecQuery("item", "w2", "sku-1", null, 8, null, null, "/page", "/ref", "uid");
        RecommendationResult recResult = new RecommendationResult("rid-xyz", List.of(product));
        DiscoveryRequestCache.putRecommendations(request, query, recResult);
        Optional<RecommendationResult> got = DiscoveryRequestCache.getRecommendations(request, query);
        assertTrue(got.isPresent());
        assertSame(recResult, got.get());
        assertEquals("rid-xyz", got.get().widgetResultId());
    }

    @Test
    void recommendations_sameWidgetId_differentQueryShape_areIndependent() {
        RecQuery first = new RecQuery("item", "w1", "sku-1", null, 8, null, null, "/page-a", "/ref", "uid");
        RecQuery second = new RecQuery("item", "w1", "sku-2", null, 8, null, null, "/page-b", "/ref", "uid");
        RecommendationResult r1 = RecommendationResult.of(List.of(product));
        RecommendationResult r2 = RecommendationResult.of(List.of(new ProductSummary("p-2", "U", null, null, null, null, Map.of())));
        DiscoveryRequestCache.putRecommendations(request, first, r1);
        DiscoveryRequestCache.putRecommendations(request, second, r2);

        assertSame(r1, DiscoveryRequestCache.getRecommendations(request, first).orElseThrow());
        assertSame(r2, DiscoveryRequestCache.getRecommendations(request, second).orElseThrow());
    }

}
