package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
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
    void getSearchResult_unknownBand_returnsEmpty() {
        assertTrue(DiscoveryRequestCache.getSearchResult(request, "foo").isEmpty());
    }

    @Test
    void putAndGet_searchResult_withNamedBand_roundTrips() {
        DiscoveryRequestCache.putSearchResult(request, "my-band", result);
        Optional<SearchResult> got = DiscoveryRequestCache.getSearchResult(request, "my-band");
        assertTrue(got.isPresent());
        assertSame(result, got.get());
    }

    @Test
    void searchResult_bands_areIndependent() {
        SearchResult r2 = new SearchResult(List.of(), 99L, 0, 5, Map.of());
        DiscoveryRequestCache.putSearchResult(request, "band-a", result);
        DiscoveryRequestCache.putSearchResult(request, "band-b", r2);

        assertSame(result, DiscoveryRequestCache.getSearchResult(request, "band-a").orElseThrow());
        assertSame(r2, DiscoveryRequestCache.getSearchResult(request, "band-b").orElseThrow());
        assertTrue(DiscoveryRequestCache.getSearchResult(request, "other").isEmpty());
    }

    @Test
    void noArgSearchResult_delegatesToDefaultBand() {
        DiscoveryRequestCache.putSearchResult(request, "default", result);
        Optional<SearchResult> noArgResult = DiscoveryRequestCache.getSearchResult(request);
        assertTrue(noArgResult.isPresent());
        assertSame(result, noArgResult.get());
    }

    @Test
    void noArgPutSearchResult_storedUnderDefaultBand() {
        DiscoveryRequestCache.putSearchResult(request, result);
        Optional<SearchResult> bandResult = DiscoveryRequestCache.getSearchResult(request, "default");
        assertTrue(bandResult.isPresent());
        assertSame(result, bandResult.get());
    }

    @Test
    void defaultBand_andNamedBand_areIndependent() {
        SearchResult other = new SearchResult(List.of(), 7L, 0, 5, Map.of());
        DiscoveryRequestCache.putSearchResult(request, result);      // → "default"
        DiscoveryRequestCache.putSearchResult(request, "xyz", other);

        assertSame(result, DiscoveryRequestCache.getSearchResult(request).orElseThrow());
        assertSame(other, DiscoveryRequestCache.getSearchResult(request, "xyz").orElseThrow());
    }

    // ── band-aware category ──────────────────────────────────────────────────

    @Test
    void getCategoryResult_unknownBand_returnsEmpty() {
        assertTrue(DiscoveryRequestCache.getCategoryResult(request, "foo").isEmpty());
    }

    @Test
    void putAndGet_categoryResult_withNamedBand_roundTrips() {
        DiscoveryRequestCache.putCategoryResult(request, "cat-band", result);
        Optional<SearchResult> got = DiscoveryRequestCache.getCategoryResult(request, "cat-band");
        assertTrue(got.isPresent());
        assertSame(result, got.get());
    }

    @Test
    void noArgCategoryResult_delegatesToDefaultBand() {
        DiscoveryRequestCache.putCategoryResult(request, "default", result);
        assertTrue(DiscoveryRequestCache.getCategoryResult(request).isPresent());
    }

    @Test
    void noArgPutCategoryResult_storedUnderDefaultBand() {
        DiscoveryRequestCache.putCategoryResult(request, result);
        assertTrue(DiscoveryRequestCache.getCategoryResult(request, "default").isPresent());
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
        assertTrue(DiscoveryRequestCache.getSearchResult(request, "default").isEmpty());
    }
}
