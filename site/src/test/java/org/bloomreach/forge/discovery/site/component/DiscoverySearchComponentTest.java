package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
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
class DiscoverySearchComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult searchResult;

    @BeforeEach
    void setUp() {
        searchResult = new SearchResult(List.of(), 42L, 0, 12, Map.of());
    }

    private TestableSearchComponent componentWith(String q, int pageSize, String defaultSort) {
        return new TestableSearchComponent(discoveryService, q, pageSize, defaultSort);
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
        when(discoveryService.search(request, 24, "price asc")).thenReturn(searchResult);

        componentWith("shoes", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).search(request, 24, "price asc");
    }

    @Test
    void withQuery_delegatesToServiceWithZeroPageSizeWhenNotSet() {
        when(discoveryService.search(eq(request), eq(0), eq(""))).thenReturn(searchResult);

        componentWith("boots", 0, "").doBeforeRender(request, response);

        verify(discoveryService).search(request, 0, "");
    }

    // ── models published ───────────────────────────────────────────────────

    @Test
    void withQuery_setsQueryModel() {
        when(discoveryService.search(eq(request), anyInt(), any())).thenReturn(searchResult);

        componentWith("boots", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "boots");
        verify(request).setAttribute("query", "boots");
    }

    @Test
    void withQuery_setsSearchResultModel() {
        when(discoveryService.search(eq(request), anyInt(), any())).thenReturn(searchResult);

        componentWith("boots", 12, "").doBeforeRender(request, response);

        verify(request).setModel("searchResult", searchResult);
        verify(request).setAttribute("searchResult", searchResult);
    }

    // ── query trimming ─────────────────────────────────────────────────────

    @Test
    void query_leadingTrailingWhitespace_trimmedBeforeUse() {
        when(discoveryService.search(eq(request), anyInt(), any())).thenReturn(searchResult);

        componentWith("  shoes  ", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "shoes");
        verify(request).setAttribute("query", "shoes");
    }

    // ── testable subclass ──────────────────────────────────────────────────

    private static class TestableSearchComponent extends DiscoverySearchComponent {

        private final HstDiscoveryService service;
        private final String rawQuery;
        private final int pageSize;
        private final String defaultSort;

        TestableSearchComponent(HstDiscoveryService service, String rawQuery,
                                int pageSize, String defaultSort) {
            this.service = service;
            this.rawQuery = rawQuery;
            this.pageSize = pageSize;
            this.defaultSort = defaultSort;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoverySearchComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoverySearchComponentInfo() {
                @Override public int getPageSize() { return pageSize; }
                @Override public String getDefaultSort() { return defaultSort; }
                @Override public String getCatalogName() { return ""; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return "q".equals(name) ? rawQuery : null;
        }
    }
}
