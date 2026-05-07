package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryGlobalRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryGlobalRecommendationComponentInfo;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryGlobalRecommendationComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstRequestContext requestContext;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);
    }

    // ── null / empty config guard ───────────────────────────────────────────

    @Test
    void nullDocument_setsEmptyProducts_noServiceCall() {
        build(null, 8).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    @Test
    void blankConfig_setsEmptyProducts_noServiceCall() {
        build(configOf("w-1", "global"), 8)
                .withBlankConfig()
                .doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    // ── widgetType from config ───────────────────────────────────────────────

    @Test
    void widgetType_global_fromConfig_passedToService() {
        when(discoveryService.recommend(eq(request), any(), eq("global"), isNull(), isNull(),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "global"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), eq("global"), isNull(), isNull(),
                isNull(), anyInt(), any(), any());
    }

    @Test
    void widgetType_personalized_fromConfig_passedToService() {
        when(discoveryService.recommend(eq(request), any(), eq("personalized"), isNull(), isNull(),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "personalized"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), eq("personalized"), isNull(), isNull(),
                isNull(), anyInt(), any(), any());
    }

    // ── no context IDs ───────────────────────────────────────────────────────

    @Test
    void contextProductId_isAlwaysNull() {
        when(discoveryService.recommend(eq(request), any(), any(), isNull(), any(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "global"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), isNull(), any(), any(),
                anyInt(), any(), any());
    }

    @Test
    void catId_isAlwaysNull() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), isNull(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "global"), 8).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), isNull(), any(),
                anyInt(), any(), any());
    }

    // ── limit ────────────────────────────────────────────────────────────────

    @Test
    void limitFromComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(), any(),
                eq(12), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "global"), 12).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(), any(), eq(12), any(), any());
    }

    // ── products set on model ────────────────────────────────────────────────

    @Test
    void productsAndWidgetId_setOnModel() {
        List<ProductSummary> products = List.of(new ProductSummary("p1", "Trending Shoe", null, null, null, null, null, List.of()));
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        build(configOf("w-7", "global"), 8).doBeforeRender(request, response);

        verify(request).setModel("products", products);
        verify(request).setModel("widgetId", "w-7");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static DiscoveryRecommendationConfig configOf(String widgetId, String widgetType) {
        return new DiscoveryRecommendationConfig(widgetId, widgetId, widgetType,
                null, null, null, null, null, null);
    }

    private TestableGlobalComponent build(DiscoveryRecommendationConfig cfg, int limit) {
        return new TestableGlobalComponent(discoveryService, cfg, true, limit);
    }

    // ── testable subclass ─────────────────────────────────────────────────────

    private static class TestableGlobalComponent extends DiscoveryGlobalRecommendationComponent {

        private final HstDiscoveryService service;
        private final DiscoveryRecommendationConfig cfg;
        private final boolean hasDocument;
        private final int limit;
        private boolean blankConfig;

        TestableGlobalComponent(HstDiscoveryService service, DiscoveryRecommendationConfig cfg,
                                 boolean hasDocument, int limit) {
            this.service = service;
            this.cfg = cfg;
            this.hasDocument = hasDocument;
            this.limit = limit;
        }

        TestableGlobalComponent withBlankConfig() { this.blankConfig = true; return this; }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) { return (T) service; }

        @Override
        protected DiscoveryGlobalRecommendationComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryGlobalRecommendationComponentInfo() {
                @Override public String getDocument()        { return hasDocument ? "rec/path" : null; }
                @Override public int getLimit()              { return limit; }
                @Override public boolean isShowPrice()       { return true; }
                @Override public boolean isShowDescription() { return false; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> type) {
            if (!hasDocument || cfg == null) return null;
            DiscoveryRecommendationConfig resolvedCfg = blankConfig ? null : cfg;
            return type.cast(new DiscoveryGlobalRecommendationBean() {
                @Override public String getDisplayName() { return ""; }
                @Override public Optional<DiscoveryRecommendationConfig> getConfig() {
                    return Optional.ofNullable(resolvedCfg);
                }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return "limit".equals(name) ? null : null;
        }
    }
}
