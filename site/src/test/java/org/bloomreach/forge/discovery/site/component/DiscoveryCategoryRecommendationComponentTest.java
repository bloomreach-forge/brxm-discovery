package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.DiscoveryRecommendationConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import jakarta.servlet.http.HttpServletRequest;
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
class DiscoveryCategoryRecommendationComponentTest {

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
        build(configOf("w-1", null, null), 8, null)
                .withBlankConfig()
                .doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request).setModel("widgetId", "");
    }

    // ── catId resolution ─────────────────────────────────────────────────────

    @Test
    void configWithExplicitCatId_passedToService() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("doc-cat"),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "doc-cat", null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("doc-cat"),
                isNull(), anyInt(), any(), any());
    }

    @Test
    void configWithNullCatId_fallsBackToUrlParam() {
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("url-cat"),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", null, null), 8, "url-cat").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("url-cat"),
                isNull(), anyInt(), any(), any());
    }

    @Test
    void configWithNullCatId_pathParamTakesPrecedenceOverQueryParam() {
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/shop/mens-shoes/cid/path-cat");
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("path-cat"),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        // URL param is "url-cat" but path label wins
        build(configOf("w-1", null, null), 8, "url-cat").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("path-cat"),
                isNull(), anyInt(), any(), any());
        verify(discoveryService, never()).recommend(eq(request), any(), any(), any(), eq("url-cat"),
                any(), anyInt(), any(), any());
    }

    @Test
    void configWithNullCatId_fallsBackToQueryParam_whenPathParamAbsent() {
        // getServletRequest() returns null by default → path label absent → falls back to query param
        when(discoveryService.recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("url-cat"),
                isNull(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", null, null), 8, "url-cat").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), eq("category"), isNull(), eq("url-cat"),
                isNull(), anyInt(), any(), any());
    }

    @Test
    void configWithNullCatId_noUrlParam_setsEmptyProducts_noServiceCall() {
        build(configOf("w-1", null, null), 8, null).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
    }

    @Test
    void configWithNullCatId_noUrlParam_setsWarning_inEditMode() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        build(configOf("w-1", null, null), 8, null).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), argThat(msg ->
                msg.toString().contains("category")));
        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    // ── widgetType and context product ──────────────────────────────────────

    @Test
    void widgetType_isAlwaysCategory() {
        when(discoveryService.recommend(eq(request), any(), eq("category"), any(), any(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "cat-42", null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), eq("category"), any(), any(), any(),
                anyInt(), any(), any());
    }

    @Test
    void contextProductId_isAlwaysNull() {
        when(discoveryService.recommend(eq(request), any(), any(), isNull(), any(), any(),
                anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "cat-42", null), 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), isNull(), any(), any(),
                anyInt(), any(), any());
    }

    // ── limit ────────────────────────────────────────────────────────────────

    @Test
    void limitFromComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(), any(),
                eq(10), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        build(configOf("w-1", "cat-42", null), 10, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(), any(), eq(10), any(), any());
    }

    // ── products set on model ────────────────────────────────────────────────

    @Test
    void productsAndWidgetId_setOnModel() {
        List<ProductSummary> products = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null));
        when(discoveryService.recommend(any(), any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        build(configOf("w-5", "cat-42", null), 8, null).doBeforeRender(request, response);

        verify(request).setModel("products", products);
        verify(request).setModel("widgetId", "w-5");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static DiscoveryRecommendationConfig configOf(String widgetId, String contextCategoryId,
                                                           String contextCategoryName) {
        return new DiscoveryRecommendationConfig(widgetId, widgetId, "category",
                null, null, contextCategoryId, contextCategoryName);
    }

    private TestableCategoryComponent build(DiscoveryRecommendationConfig cfg, int limit, String urlCatId) {
        return new TestableCategoryComponent(discoveryService, cfg, true, limit, urlCatId, null);
    }

    // ── testable subclass ─────────────────────────────────────────────────────

    private static class TestableCategoryComponent extends DiscoveryCategoryRecommendationComponent {

        private final HstDiscoveryService service;
        private final DiscoveryRecommendationConfig cfg;
        private final boolean hasDocument;
        private final int limit;
        private final String urlCatId;
        private final String limitParam;
        private boolean blankConfig;

        TestableCategoryComponent(HstDiscoveryService service, DiscoveryRecommendationConfig cfg,
                                   boolean hasDocument, int limit, String urlCatId, String limitParam) {
            this.service = service;
            this.cfg = cfg;
            this.hasDocument = hasDocument;
            this.limit = limit;
            this.urlCatId = urlCatId;
            this.limitParam = limitParam;
        }

        TestableCategoryComponent withBlankConfig() { this.blankConfig = true; return this; }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) { return (T) service; }

        @Override
        protected DiscoveryCategoryRecommendationComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryCategoryRecommendationComponentInfo() {
                @Override public String getDocument()        { return hasDocument ? "rec/path" : null; }
                @Override public int getLimit()              { return limit; }
                @Override public boolean isShowPrice()       { return true; }
                @Override public boolean isShowDescription() { return false; }
                @Override public String getCategoryUrlParam(){ return "cid"; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> type) {
            if (!hasDocument || cfg == null) return null;
            DiscoveryRecommendationConfig resolvedCfg = blankConfig ? null : cfg;
            return type.cast(new DiscoveryCategoryRecommendationBean() {
                @Override public String getDisplayName() { return ""; }
                @Override public Optional<DiscoveryRecommendationConfig> getConfig() {
                    return Optional.ofNullable(resolvedCfg);
                }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return switch (name) {
                case "cid" -> urlCatId;
                case "limit"    -> limitParam;
                default         -> null;
            };
        }
    }
}
