package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchInputComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoverySearchInputComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
    }

    // ── Config models always set ────────────────────────────────────────────

    @Test
    void configModels_alwaysSet_regardlessOfQuery() {
        build(null, true, 5, "/search", 2, 250).doBeforeRender(request, response);

        verify(request).setModel("placeholder", "Search...");
        verify(request).setModel("resultsPage", "/search");
        verify(request).setModel("suggestionsEnabled", true);
        verify(request).setModel("minChars", 2);
        verify(request).setModel("debounceMs", 250);
        verify(request).setModel("query", "");
    }

    @Test
    void nullQuery_treatedAsBlank_setsEmptyQuery() {
        build(null, false, 5, "", 2, 250).doBeforeRender(request, response);

        verify(request).setModel("query", "");
    }

    // ── Suggestions - enabled with query ──────────────────────────────────

    @Test
    void suggestionsEnabled_withQuery_callsAutosuggestAndSetsResult() {
        var result = new AutosuggestResult("boots", List.of("boots"), List.of(), List.of());
        when(discoveryService.autosuggest(request, "boots", 5)).thenReturn(result);

        build("boots", true, 5, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "boots", 5);
        verify(request).setModel("autosuggestResult", result);
    }

    @Test
    void suggestionsEnabled_withQuery_usesConfiguredLimit() {
        when(discoveryService.autosuggest(eq(request), eq("hat"), eq(10)))
                .thenReturn(new AutosuggestResult("hat", List.of(), List.of(), List.of()));

        build("hat", true, 10, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "hat", 10);
    }

    // ── Suggestions - disabled or no query ────────────────────────────────

    @Test
    void suggestionsDisabled_noAutosuggestCall() {
        build("boots", false, 5, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService, never()).autosuggest(any(), any(), anyInt());
        verify(request).setModel("autosuggestResult", null);
    }

    @Test
    void blankQuery_noAutosuggestCall() {
        build("  ", true, 5, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService, never()).autosuggest(any(), any(), anyInt());
        verify(request).setModel("autosuggestResult", null);
    }

    @Test
    void nullQuery_noAutosuggestCall() {
        build(null, true, 5, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService, never()).autosuggest(any(), any(), anyInt());
        verify(request).setModel("autosuggestResult", null);
    }

    // ── Query trimmed ──────────────────────────────────────────────────────

    @Test
    void query_trimmedBeforeUse() {
        when(discoveryService.autosuggest(eq(request), eq("boots"), anyInt()))
                .thenReturn(new AutosuggestResult("boots", List.of(), List.of(), List.of()));

        build("  boots  ", true, 5, "", 2, 250).doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "boots", 5);
        verify(request).setModel("query", "boots");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TestableSearchInput build(String query, boolean suggestionsEnabled,
            int suggestionsLimit, String resultsPage, int minChars, int debounceMs) {
        return new TestableSearchInput(discoveryService, query, suggestionsEnabled,
                suggestionsLimit, resultsPage, minChars, debounceMs);
    }

    private static class TestableSearchInput extends DiscoverySearchInputComponent {

        private final HstDiscoveryService service;
        private final String rawQuery;
        private final boolean suggestionsEnabled;
        private final int suggestionsLimit;
        private final String resultsPage;
        private final int minChars;
        private final int debounceMs;

        TestableSearchInput(HstDiscoveryService service, String rawQuery,
                boolean suggestionsEnabled, int suggestionsLimit,
                String resultsPage, int minChars, int debounceMs) {
            this.service = service;
            this.rawQuery = rawQuery;
            this.suggestionsEnabled = suggestionsEnabled;
            this.suggestionsLimit = suggestionsLimit;
            this.resultsPage = resultsPage;
            this.minChars = minChars;
            this.debounceMs = debounceMs;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoverySearchInputComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoverySearchInputComponentInfo() {
                @Override public String getPlaceholder()        { return "Search..."; }
                @Override public String getResultsPage()        { return resultsPage; }
                @Override public boolean isSuggestionsEnabled() { return suggestionsEnabled; }
                @Override public int getSuggestionsLimit()      { return suggestionsLimit; }
                @Override public int getMinChars()              { return minChars; }
                @Override public int getDebounceMs()            { return debounceMs; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("q".equals(name)) return rawQuery;
            return null;
        }
    }
}
