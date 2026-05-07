package org.bloomreach.forge.discovery.site.component;

import jakarta.servlet.http.HttpServletRequest;
import org.bloomreach.forge.discovery.site.beans.DiscoveryKeywordRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryKeywordRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.DiscoveryRecommendationConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.content.beans.standard.HippoBean;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryKeywordRecommendationComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstRequestContext requestContext;
    @Mock HttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);
    }

    // ── null / empty config guard ─────────────────────────────────────────

    @Test
    void nullDocument_setsEmptyProducts_noServiceCall() {
        build(null, 8).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    @Test
    void documentWithNoConfig_setsEmptyProducts_noServiceCall() {
        build(/* emptyConfig */ true, null, 8).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    // ── specific mode: uses the authored fixed query ───────────────────────

    @Test
    void specificMode_passesFixedQueryToService() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("keyword"), isNull(), isNull(), isNull(),
                eq("winter boots"), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-1", "keyword", "winter boots", "specific"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("keyword"),
                isNull(), isNull(), isNull(), eq("winter boots"), anyInt(), any(), any());
    }

    @Test
    void specificMode_nullFixedQuery_passesNullToService() {
        when(discoveryService.recommend(eq(request), any(), any(), isNull(), isNull(), isNull(),
                isNull(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-1", "keyword", null, "specific"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), isNull(), isNull(), isNull(),
                isNull(), anyInt(), any(), any());
    }

    // ── url mode: reads ?q= from the servlet request ──────────────────────

    @Test
    void urlMode_readsQParamFromServletRequest() {
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getParameter("q")).thenReturn("running shoes");
        when(discoveryService.recommend(eq(request), any(), any(), isNull(), isNull(), isNull(),
                eq("running shoes"), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-2", "keyword", null, "url"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), isNull(), isNull(), isNull(),
                eq("running shoes"), anyInt(), any(), any());
    }

    @Test
    void urlMode_missingQParam_passesNullToService() {
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getParameter("q")).thenReturn(null);
        when(discoveryService.recommend(any(), any(), any(), isNull(), isNull(), isNull(),
                isNull(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-2", "keyword", "fixed", "url"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(any(), any(), any(), isNull(), isNull(), isNull(),
                isNull(), anyInt(), any(), any());
    }

    // ── limit ─────────────────────────────────────────────────────────────

    @Test
    void limit_fromComponentInfo_passedToService() {
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), any(), eq(5), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-1", "keyword", "boots", "specific"), 5).doBeforeRender(request, response);

        verify(discoveryService).recommend(any(), any(), any(), any(), any(), any(), any(), eq(5), any(), any());
    }

    // ── model keys ────────────────────────────────────────────────────────

    @Test
    void products_setOnModel() {
        List<ProductSummary> products = List.of(new ProductSummary("p1", "Boot", null, null, null, null, Map.of(), List.of()));
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        build(cfgOf("w-1", "keyword", "boots", "specific"), 8).doBeforeRender(request, response);

        verify(request).setModel("products", products);
    }

    @Test
    void widgetId_fromServiceResponse_setOnModel() {
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(new RecommendationResult("resolved-wid", "keyword", "result-id-1", List.of()));

        build(cfgOf("w-cfg", "keyword", "boots", "specific"), 8).doBeforeRender(request, response);

        verify(request).setModel("widgetId", "resolved-wid");
    }

    @Test
    void widgetId_blankInServiceResponse_fallsBackToConfigWidgetId() {
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(new RecommendationResult("", "keyword", "result-id-1", List.of()));

        build(cfgOf("w-fallback", "keyword", "boots", "specific"), 8).doBeforeRender(request, response);

        verify(request).setModel("widgetId", "w-fallback");
    }

    @Test
    void showPrice_showDescription_setOnModel() {
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(List.of()));

        build(cfgOf("w-1", "keyword", "boots", "specific"), 8).doBeforeRender(request, response);

        verify(request).setModel("showPrice", true);
        verify(request).setModel("showDescription", false);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static DiscoveryRecommendationConfig cfgOf(String widgetId, String widgetType,
                                                        String contextQuery, String contextQueryMode) {
        return new DiscoveryRecommendationConfig(
                widgetId, widgetId, widgetType, null, null, null, null, contextQuery, contextQueryMode);
    }

    private TestableKeywordComponent build(DiscoveryRecommendationConfig cfg, int limit) {
        return new TestableKeywordComponent(discoveryService, false, cfg, limit);
    }

    /** Overload for the "document exists but getConfig() returns empty" case. */
    private TestableKeywordComponent build(boolean emptyConfig, DiscoveryRecommendationConfig cfg, int limit) {
        return new TestableKeywordComponent(discoveryService, emptyConfig, cfg, limit);
    }

    // ── testable subclass ─────────────────────────────────────────────────

    private static class TestableKeywordComponent extends DiscoveryKeywordRecommendationComponent {

        private final HstDiscoveryService service;
        private final boolean emptyConfig;
        private final DiscoveryRecommendationConfig cfg;
        private final int limit;

        TestableKeywordComponent(HstDiscoveryService service, boolean emptyConfig,
                                  DiscoveryRecommendationConfig cfg, int limit) {
            this.service = service;
            this.emptyConfig = emptyConfig;
            this.cfg = cfg;
            this.limit = limit;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) { return (T) service; }

        @Override
        protected DiscoveryKeywordRecommendationComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryKeywordRecommendationComponentInfo() {
                @Override public String getDocument()        { return cfg != null ? "widgets/path" : null; }
                @Override public int getLimit()              { return limit; }
                @Override public boolean isShowPrice()       { return true; }
                @Override public boolean isShowDescription() { return false; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> type) {
            if (cfg == null && !emptyConfig) return null;
            return type.cast(new DiscoveryKeywordRecommendationBean() {
                @Override public String getDisplayName() { return "Test Widget"; }
                @Override public Optional<DiscoveryRecommendationConfig> getConfig() {
                    return emptyConfig ? Optional.empty() : Optional.of(cfg);
                }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) { return null; }
    }
}
