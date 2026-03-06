package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryAutosuggestComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryAutosuggestComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;

    private TestableAutosuggestComponent componentWith(String q, int limit, String catalogViews) {
        return new TestableAutosuggestComponent(discoveryService, q, limit, catalogViews);
    }

    // ── blank query guard ─────────────────────────────────────────────────

    @Test
    void nullQuery_noServiceCall_setsNullModel() {
        componentWith(null, 8, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
        verify(request).setModel("autosuggestResult", null);
    }

    @Test
    void blankQuery_treatedAsEmpty() {
        componentWith("   ", 8, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
    }

    // ── successful delegation ──────────────────────────────────────────────

    @Test
    void withQuery_delegatesToServiceWithLimitAndCatalogViews() {
        var result = new AutosuggestResult("shi", List.of("shirts"), List.of(), List.of());
        when(discoveryService.autosuggest(request, "shi", 10, "store:products_en"))
                .thenReturn(result);

        componentWith("shi", 10, "store:products_en").doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "shi", 10, "store:products_en");
    }

    @Test
    void withQuery_blankCatalogViews_callsWithoutCatalogViews() {
        var result = new AutosuggestResult("shoes", List.of("shoes"), List.of(), List.of());
        when(discoveryService.autosuggest(request, "shoes", 8)).thenReturn(result);

        componentWith("shoes", 8, "").doBeforeRender(request, response);

        verify(discoveryService).autosuggest(request, "shoes", 8);
    }

    // ── models published ───────────────────────────────────────────────────

    @Test
    void withQuery_setsModels() {
        var result = new AutosuggestResult("boots", List.of("boots"), List.of(), List.of());
        when(discoveryService.autosuggest(eq(request), eq("boots"), anyInt())).thenReturn(result);

        componentWith("boots", 8, "").doBeforeRender(request, response);

        verify(request).setModel("query", "boots");
        verify(request).setModel("autosuggestResult", result);
        verify(request).setAttribute("query", "boots");
        verify(request).setAttribute("autosuggestResult", result);
    }

    // ── query trimming ────────────────────────────────────────────────────

    @Test
    void query_leadingTrailingWhitespace_trimmedBeforeUse() {
        var result = new AutosuggestResult("shoes", List.of(), List.of(), List.of());
        when(discoveryService.autosuggest(eq(request), eq("shoes"), anyInt())).thenReturn(result);

        componentWith("  shoes  ", 8, "").doBeforeRender(request, response);

        verify(request).setModel("query", "shoes");
    }

    // ── testable subclass ─────────────────────────────────────────────────

    private static class TestableAutosuggestComponent extends DiscoveryAutosuggestComponent {

        private final HstDiscoveryService service;
        private final String rawQuery;
        private final int limit;
        private final String catalogViews;

        TestableAutosuggestComponent(HstDiscoveryService service, String rawQuery,
                                     int limit, String catalogViews) {
            this.service = service;
            this.rawQuery = rawQuery;
            this.limit = limit;
            this.catalogViews = catalogViews;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryAutosuggestComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryAutosuggestComponentInfo() {
                @Override public int getLimit() { return limit; }
                @Override public String getCatalogViews() { return catalogViews; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return "q".equals(name) ? rawQuery : null;
        }
    }
}
