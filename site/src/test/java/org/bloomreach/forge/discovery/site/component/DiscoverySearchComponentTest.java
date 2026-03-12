package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoverySearchComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult searchResult;
    private SearchResponse searchResponse;

    @BeforeEach
    void setUp() {
        searchResult = new SearchResult(List.of(), 42L, 0, 12, Map.of());
        searchResponse = new SearchResponse(searchResult, SearchMetadata.empty());
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
    }

    private TestableSearchComponent componentWith(String q, int pageSize, String defaultSort) {
        return new TestableSearchComponent(discoveryService, q, pageSize, defaultSort, "default",
                false, 5, "Search...", "", 2, 250, null);
    }

    private TestableSearchComponent componentWith(String q, int pageSize, String defaultSort, String band) {
        return new TestableSearchComponent(discoveryService, q, pageSize, defaultSort, band,
                false, 5, "Search...", "", 2, 250, null);
    }

    private TestableSearchComponent componentWithSuggestions(String q, boolean suggestionsEnabled,
                                                              int limit, String resultsPage,
                                                              String suggestParam) {
        return new TestableSearchComponent(discoveryService, q, 12, "", "default",
                suggestionsEnabled, limit, "Search...", resultsPage, 2, 250, suggestParam);
    }

    // ── blank query guard ───────────────────────────────────────────────────

    @Test
    void nullQuery_noServiceCall_setsEmptyState() {
        componentWith(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
        verify(request).setModel("searchResult", null);
        verify(request).setAttribute("query", "");
    }

    @Test
    void blankQuery_treatedAsEmpty() {
        componentWith("   ", 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
    }

    @Test
    void emptyQuery_treatedAsEmpty() {
        componentWith("", 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
    }

    // ── component pageSize / sort forwarded ────────────────────────────────

    @Test
    void withQuery_delegatesToServiceWithComponentPageSize() {
        when(discoveryService.search(request, 24, "price asc", null, "default", List.of(), "", "")).thenReturn(searchResponse);

        componentWith("shoes", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).search(request, 24, "price asc", null, "default", List.of(), "", "");
    }

    @Test
    void withQuery_delegatesToServiceWithZeroPageSizeWhenNotSet() {
        when(discoveryService.search(eq(request), eq(0), eq(""), isNull(), eq("default"), eq(List.of()), eq(""), eq("")))
                .thenReturn(searchResponse);

        componentWith("boots", 0, "").doBeforeRender(request, response);

        verify(discoveryService).search(request, 0, "", null, "default", List.of(), "", "");
    }

    // ── models published ───────────────────────────────────────────────────

    @Test
    void withQuery_setsQueryModel() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(searchResponse);

        componentWith("boots", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "boots");
        verify(request).setAttribute("query", "boots");
    }

    @Test
    void withQuery_setsSearchResultModel() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(searchResponse);

        componentWith("boots", 12, "").doBeforeRender(request, response);

        verify(request).setModel("searchResult", searchResult);
        verify(request).setAttribute("searchResult", searchResult);
    }

    // ── query trimming ─────────────────────────────────────────────────────

    @Test
    void query_leadingTrailingWhitespace_trimmedBeforeUse() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(searchResponse);

        componentWith("  shoes  ", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "shoes");
        verify(request).setAttribute("query", "shoes");
    }

    // ── label model ────────────────────────────────────────────────────────

    @Test
    void withQuery_setsLabelModel_defaultLabel() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), eq("default"), any(), any(), any()))
                .thenReturn(searchResponse);

        componentWith("boots", 12, "", "default").doBeforeRender(request, response);

        verify(request).setModel("label", "default");
        verify(request).setAttribute("label", "default");
    }

    @Test
    void withQuery_setsLabelModel_namedLabel() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), eq("search-bar"), any(), any(), any()))
                .thenReturn(searchResponse);

        componentWith("boots", 12, "", "search-bar").doBeforeRender(request, response);

        verify(request).setModel("label", "search-bar");
        verify(request).setAttribute("label", "search-bar");
    }

    // ── bar config models always set ──────────────────────────────────────

    @Test
    void configBarModels_alwaysSet_regardlessOfQuery() {
        componentWithSuggestions(null, true, 5, "/search", null)
                .doBeforeRender(request, response);

        verify(request).setModel("suggestionsEnabled", true);
        verify(request).setModel("resultsPage", "/search");
        verify(request).setModel("minChars", 2);
        verify(request).setModel("debounceMs", 250);
        verify(request).setModel("placeholder", "Search...");
        verify(request).setModel("query", "");
    }

    // ── suggestions enabled + query → autosuggest called ──────────────────

    @Test
    void suggestionsEnabled_withQuery_callsAutosuggestAndSetsResult() {
        var autosuggestResult = new AutosuggestResult("boots", List.of("boots"), List.of(), List.of());
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(searchResponse);
        when(discoveryService.autosuggest(request, "boots", 5)).thenReturn(autosuggestResult);

        componentWithSuggestions("boots", true, 5, "", null)
                .doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "boots", 5);
        verify(request).setModel("autosuggestResult", autosuggestResult);
    }

    // ── suggestions disabled → no autosuggest call ────────────────────────

    @Test
    void suggestionsDisabled_noAutosuggestCall() {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(searchResponse);

        componentWithSuggestions("boots", false, 5, "", null)
                .doBeforeRender(request, response);

        verify(discoveryService, never()).autosuggest(any(), any(), anyInt());
        verify(request).setModel("autosuggestResult", null);
    }

    // ── blank query → no search, no autosuggest ───────────────────────────

    @Test
    void blankQuery_noSearchCall_noAutosuggestCall() {
        componentWithSuggestions("", true, 5, "", null)
                .doBeforeRender(request, response);

        verify(discoveryService, never()).search(any(), anyInt(), any(), any(), any(), any(), any(), any());
        verify(discoveryService, never()).autosuggest(any(), any(), anyInt());
        verify(request).setModel("autosuggestResult", null);
    }

    // ── suggestOnlyMode → skip search, still call autosuggest ─────────────

    @Test
    void suggestOnlyMode_skipsSearchCall_stillCallsAutosuggest() {
        var autosuggestResult = new AutosuggestResult("hat", List.of("hats"), List.of(), List.of());
        when(discoveryService.autosuggest(eq(request), eq("hat"), anyInt())).thenReturn(autosuggestResult);

        componentWithSuggestions("hat", true, 5, "", "1")
                .doBeforeRender(request, response);

        verify(discoveryService, never()).search(any(), anyInt(), any(), any(), any(), any(), any(), any());
        verify(discoveryService).autosuggest(request, "hat", 5);
        verify(request).setModel("suggestOnlyMode", true);
    }

    @Test
    void suggestOnlyMode_false_whenParamAbsent() {
        componentWithSuggestions(null, true, 5, "", null)
                .doBeforeRender(request, response);

        verify(request).setModel("suggestOnlyMode", false);
    }

    // ── keyword redirect ──────────────────────────────────────────────────

    @Test
    void autoRedirect_false_withRedirectUrl_setsModelAttributesOnly() throws Exception {
        var redirectMeta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", "sale shoes");
        var redirectResponse = new SearchResponse(searchResult, redirectMeta);
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(redirectResponse);

        componentWith("shoes", 12, "", "default").doBeforeRender(request, response);

        verify(request).setModel("redirectUrl", "https://example.com/sale");
        verify(request).setAttribute("redirectUrl", "https://example.com/sale");
        verify(request).setModel("redirectQuery", "sale shoes");
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void autoRedirect_true_withRedirectUrl_callsSendRedirect() throws Exception {
        var redirectMeta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", "sale shoes");
        var redirectResponse = new SearchResponse(searchResult, redirectMeta);
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(redirectResponse);

        new TestableSearchComponent(discoveryService, "shoes", 12, "", "default",
                false, 5, "Search...", "", 2, 250, null, true)
                .doBeforeRender(request, response);

        verify(response).sendRedirect("https://example.com/sale");
    }

    @Test
    void autoRedirect_true_noRedirectUrl_proceedsNormally() throws Exception {
        when(discoveryService.search(eq(request), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(searchResponse);

        new TestableSearchComponent(discoveryService, "shoes", 12, "", "default",
                false, 5, "Search...", "", 2, 250, null, true)
                .doBeforeRender(request, response);

        verify(response, never()).sendRedirect(any());
        verify(request).setModel("searchResult", searchResult);
    }

    // ── testable subclass ──────────────────────────────────────────────────

    private static class TestableSearchComponent extends DiscoverySearchComponent {

        private final HstDiscoveryService service;
        private final String rawQuery;
        private final int pageSize;
        private final String defaultSort;
        private final String band;
        private final boolean suggestionsEnabled;
        private final int suggestionsLimit;
        private final String placeholder;
        private final String resultsPage;
        private final int minChars;
        private final int debounceMs;
        private final String suggestParam;
        private final boolean autoRedirect;

        TestableSearchComponent(HstDiscoveryService service, String rawQuery,
                                int pageSize, String defaultSort, String band,
                                boolean suggestionsEnabled, int suggestionsLimit,
                                String placeholder, String resultsPage,
                                int minChars, int debounceMs, String suggestParam) {
            this(service, rawQuery, pageSize, defaultSort, band,
                    suggestionsEnabled, suggestionsLimit, placeholder, resultsPage,
                    minChars, debounceMs, suggestParam, false);
        }

        TestableSearchComponent(HstDiscoveryService service, String rawQuery,
                                int pageSize, String defaultSort, String band,
                                boolean suggestionsEnabled, int suggestionsLimit,
                                String placeholder, String resultsPage,
                                int minChars, int debounceMs, String suggestParam, boolean autoRedirect) {
            this.service = service;
            this.rawQuery = rawQuery;
            this.pageSize = pageSize;
            this.defaultSort = defaultSort;
            this.band = band;
            this.suggestionsEnabled = suggestionsEnabled;
            this.suggestionsLimit = suggestionsLimit;
            this.placeholder = placeholder;
            this.resultsPage = resultsPage;
            this.minChars = minChars;
            this.debounceMs = debounceMs;
            this.suggestParam = suggestParam;
            this.autoRedirect = autoRedirect;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoverySearchComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoverySearchComponentInfo() {
                @Override public int getPageSize()             { return pageSize; }
                @Override public String getDefaultSort()       { return defaultSort; }
                @Override public String getCatalogName()       { return ""; }
                @Override public String getLabel()             { return band; }
                @Override public String getPlaceholder()       { return placeholder; }
                @Override public String getResultsPage()       { return resultsPage; }
                @Override public boolean isSuggestionsEnabled(){ return suggestionsEnabled; }
                @Override public int getSuggestionsLimit()     { return suggestionsLimit; }
                @Override public int getMinChars()             { return minChars; }
                @Override public int getDebounceMs()           { return debounceMs; }
                @Override public String getStatsFields()       { return ""; }
                @Override public boolean isAutoRedirect()      { return autoRedirect; }
                @Override public String getSegment()           { return ""; }
                @Override public String getExclusionFilter()   { return ""; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("q".equals(name)) return rawQuery;
            if ("brxdis_suggest".equals(name)) return suggestParam;
            return null;
        }
    }
}
