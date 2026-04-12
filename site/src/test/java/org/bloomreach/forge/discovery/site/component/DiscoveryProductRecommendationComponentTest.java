package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductRecommendationComponentInfo;
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
class DiscoveryProductRecommendationComponentTest {

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
        build(null, 8, null).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    @Test
    void blankConfig_setsEmptyProducts_noServiceCall() {
        build(configOf(null, null, null, null, null), 8, null)
                .withBlankConfig()
                .doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    // ── pid resolution ──────────────────────────────────────────────────────

    @Test
    void configWithExplicitPid_passedToService() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("item"), eq("doc-pid"),
                isNull(), isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "item", "doc-pid", null, null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("item"), eq("doc-pid"),
                isNull(), isNull(), anyInt(), any(), any());
    }

    @Test
    void configWithNullPid_fallsBackToUrlParam() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("recs"), eq("url-pid"),
                isNull(), isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "recs", null, null, null), 8, "url-pid").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("recs"), eq("url-pid"),
                isNull(), isNull(), anyInt(), any(), any());
    }

    @Test
    void configWithNullPid_noUrlParam_passesNullToService() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("mlt"), isNull(),
                isNull(), isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "mlt", null, null, null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("mlt"), isNull(),
                isNull(), isNull(), anyInt(), any(), any());
    }

    // ── widgetType and model keys ────────────────────────────────────────────

    @Test
    void widgetType_fromConfig_passedToService() {
        when(discoveryService.recommend(eq(request), any(), eq("item"), any(), any(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "item", "pid", null, null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), eq("item"), any(), any(), any(),
                anyInt(), any(), any());
    }

    @Test
    void catId_isAlwaysNull() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), isNull(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "item", "pid", null, null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), isNull(), any(),
                anyInt(), any(), any());
    }

    // ── limit ────────────────────────────────────────────────────────────────

    @Test
    void limitFromComponentInfo_usedWhenUrlParamAbsent() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(), any(),
                eq(20), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "item", "pid", null, null), 20, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(), any(), eq(20), any(), any());
    }

    @Test
    void limitFromUrlParam_overridesComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(), any(),
                eq(5), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "item", "pid", null, null), 20, null)
                .withLimitParam("5")
                .doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(), any(), eq(5), any(), any());
    }

    // ── products set on model ────────────────────────────────────────────────

    @Test
    void productsAndWidgetId_setOnModel() {
        List<ProductSummary> products = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null));
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        build(configOf("w-42", "item", "pid", null, null), 8, null).doBeforeRender(request, response);

        verify(request).setModel("products", products);
        verify(request).setModel("widgetId", "w-42");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static DiscoveryRecommendationConfig configOf(String widgetId, String widgetType,
                                                            String contextProductId, String contextProductName,
                                                            String contextCategoryId) {
        return new DiscoveryRecommendationConfig(widgetId, widgetId, widgetType,
                contextProductId, contextProductName, contextCategoryId, null);
    }

    private TestableProductComponent build(DiscoveryRecommendationConfig cfg, int limit, String urlPid) {
        return new TestableProductComponent(discoveryService, cfg, true, limit, urlPid, null);
    }

    // ── testable subclass ─────────────────────────────────────────────────────

    private static class TestableProductComponent extends DiscoveryProductRecommendationComponent {

        private final HstDiscoveryService service;
        private final DiscoveryRecommendationConfig cfg;
        private final boolean hasDocument;
        private final int limit;
        private final String urlPid;
        private String limitParam;
        private boolean blankConfig;

        TestableProductComponent(HstDiscoveryService service, DiscoveryRecommendationConfig cfg,
                                  boolean hasDocument, int limit, String urlPid, String limitParam) {
            this.service = service;
            this.cfg = cfg;
            this.hasDocument = hasDocument;
            this.limit = limit;
            this.urlPid = urlPid;
            this.limitParam = limitParam;
        }

        TestableProductComponent withLimitParam(String lp) { this.limitParam = lp; return this; }
        TestableProductComponent withBlankConfig()          { this.blankConfig = true; return this; }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) { return (T) service; }

        @Override
        protected DiscoveryProductRecommendationComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryProductRecommendationComponentInfo() {
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
            return type.cast(new DiscoveryProductRecommendationBean() {
                @Override public String getDisplayName() { return ""; }
                @Override public Optional<DiscoveryRecommendationConfig> getConfig() {
                    return Optional.ofNullable(resolvedCfg);
                }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return switch (name) {
                case "pid"   -> urlPid;
                case "limit" -> limitParam;
                default      -> null;
            };
        }
    }
}
