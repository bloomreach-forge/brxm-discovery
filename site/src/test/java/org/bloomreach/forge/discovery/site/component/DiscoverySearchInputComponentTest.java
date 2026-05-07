package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
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
import java.util.Map;

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

    // ── Visual search ──────────────────────────────────────────────────────

    @Test
    void visualSearch_noChannelInfo_setsEnabledFalseAndOmitsUrls() {
        // no channel info configured → null from getChannelInfo
        build(null, false, 5, "", 2, 250).doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", false);
        verify(request, never()).setModel(eq("visualSearchUploadUrl"), any());
        verify(request, never()).setModel(eq("visualSearchWidgetId"), any());
    }

    @Test
    void visualSearch_disabled_setsEnabledFalseAndOmitsUrls() {
        buildVs(false, null).doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", false);
        verify(request, never()).setModel(eq("visualSearchUploadUrl"), any());
        verify(request, never()).setModel(eq("visualSearchWidgetId"), any());
    }

    @Test
    void visualSearch_enabled_blankWidgetId_omitsUrls() {
        buildVs(true, "").doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", true);
        verify(request, never()).setModel(eq("visualSearchUploadUrl"), any());
        verify(request, never()).setModel(eq("visualSearchWidgetId"), any());
    }

    @Test
    void visualSearch_enabledWithWidgetId_setsUploadUrlAndWidgetId() {
        when(request.getContextPath()).thenReturn("");
        buildVs(true, "cam123").doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", true);
        verify(request).setModel("visualSearchUploadUrl", "/_brxdis-api/visual-search/cam123/upload");
        verify(request).setModel("visualSearchWidgetId", "cam123");
    }

    @Test
    void visualSearch_enabledWithWidgetId_prefixesContextPath() {
        when(request.getContextPath()).thenReturn("/site");
        buildVs(true, "cam123").doBeforeRender(request, response);

        verify(request).setModel("visualSearchUploadUrl", "/site/_brxdis-api/visual-search/cam123/upload");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TestableSearchInput build(String query, boolean suggestionsEnabled,
            int suggestionsLimit, String resultsPage, int minChars, int debounceMs) {
        return new TestableSearchInput(discoveryService, query, suggestionsEnabled,
                suggestionsLimit, resultsPage, minChars, debounceMs);
    }

    /** VS tests: override getChannelInfo in anonymous subclass with controlled stub. */
    private TestableSearchInput buildVs(boolean enabled, String widgetId) {
        return new TestableSearchInput(discoveryService, null, false, 5, "", 2, 250) {
            @Override
            protected DiscoveryChannelInfo getChannelInfo(HstRequest req) {
                if (!enabled) return null;
                return new DiscoveryChannelInfo() {
                    @Override public String getDiscoveryAccountId()            { return ""; }
                    @Override public String getDiscoveryDomainKey()            { return ""; }
                    @Override public String getDiscoveryApiKeyEnvVar()         { return ""; }
                    @Override public String getDiscoveryAuthKeyEnvVar()        { return ""; }
                    @Override public String getDiscoveryDefaultFieldList()     { return ""; }
                    @Override public String getDiscoveryCatalogName()          { return ""; }
                    @Override public boolean getDiscoveryPixelsEnabled()       { return true; }
                    @Override public String getDiscoveryPixelConsentCookie()   { return ""; }
                    @Override public boolean getDiscoveryPixelTestData()       { return false; }
                    @Override public boolean getDiscoveryPixelDebug()          { return false; }
                    @Override public String getPixelRegion()                   { return "US"; }
                    @Override public boolean getDiscoveryVisualSearchEnabled() { return true; }
                    @Override public String getDiscoveryVisualSearchWidgetId() { return widgetId != null ? widgetId : ""; }
                    @Override public Map<String, Object> getProperties()       { return Map.of(); }
                };
            }
        };
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
