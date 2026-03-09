package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
class DiscoveryRecommendationComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstRequestContext requestContext;

    private final Map<String, Object> attrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);
        lenient().when(requestContext.getContentBean()).thenReturn(null);
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> {
            attrs.put((String) inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(requestContext).setAttribute(anyString(), any());
    }

    private TestableRecommendationComponent componentWith(String widgetId,
                                                           int componentLimit, String limitParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, "standalone", "default", false, null);
    }

    private TestableRecommendationComponent componentWith(String widgetId, int componentLimit,
                                                           String limitParam,
                                                           String dataSource, String band) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, dataSource, band, false, null);
    }

    private TestableRecommendationComponent componentWithPpr(String widgetId, int componentLimit,
                                                              String limitParam,
                                                              String dataSource, String band,
                                                              String pidParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, dataSource, band, true, pidParam);
    }

    // ── limit: component default vs URL override ────────────────────────────

    @Test
    void limitFromComponentInfo_usedWhenUrlParamAbsent() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        componentWith("w-1", 20, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any(), any());
    }

    @Test
    void limitFromUrlParam_overridesComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        componentWith("w-1", 20, "5").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any(), any());
    }

    // ── model keys ──────────────────────────────────────────────────────────

    @Test
    void setsProductsAndWidgetIdOnModel() {
        List<ProductSummary> products = List.of();
        when(discoveryService.recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        componentWith("w-42", 8, null).doBeforeRender(request, response);

        verify(request).setModel("products", products);
        verify(request).setAttribute("products", products);
        verify(request).setModel("widgetId", "w-42");
        verify(request).setAttribute("widgetId", "w-42");
    }

    @Test
    void nullWidgetId_setsEmptyStringOnModel() {
        componentWith(null, 8, null).doBeforeRender(request, response);

        verify(request).setModel("widgetId", "");
        verify(request).setAttribute("widgetId", "");
    }

    // ── resolveWidgetId ─────────────────────────────────────────────────

    @Test
    void resolveWidgetId_fallsBackToUrlParam() {
        // widgetId from URL param returned when testable component's resolveWidgetId is NOT overridden
        String result = componentWith("url-widget", 8, null)
                .resolveWidgetId(request);
        assertEquals("url-widget", result);
    }

    // ── productDetailBand mode ───────────────────────────────────────────────

    @Test
    void productDetailBand_mode_readsPidFromCachedProduct() {
        ProductSummary cached = new ProductSummary("p-from-pdp", "T", null, null, null, null, Map.of());
        DiscoveryRequestCache.markProductDetailBandPresent(request, "default");
        DiscoveryRequestCache.putProductResult(request, "default", cached);
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("p-from-pdp"),
                any(), anyInt(), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        componentWith("w-1", 8, null, "productDetailBand", "default").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("p-from-pdp"),
                any(), anyInt(), any(), any(), any());
    }

    @Test
    void productDetailBand_mode_bandAbsent_returnsEmpty_live() {
        // band not marked → not edit mode → empty products, no warning
        componentWith("w-1", 8, null, "productDetailBand", "default").doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request, never()).setAttribute(eq("brxdis_warning"), any());
    }

    @Test
    void productDetailBand_mode_bandAbsent_setsWarning_inEditMode() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        componentWith("w-1", 8, null, "productDetailBand", "my-band").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), argThat(msg ->
                msg.toString().contains("my-band")));
        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void productDetailBand_mode_bandPresentNoProduct_returnsEmpty() {
        DiscoveryRequestCache.markProductDetailBandPresent(request, "default");
        // No putProductResult → cache empty

        componentWith("w-1", 8, null, "productDetailBand", "default").doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        verify(request).setModel("products", List.of());
    }

    @Test
    void productDetailBand_mode_bandPresentNoProduct_setsWarning_inEditMode() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        DiscoveryRequestCache.markProductDetailBandPresent(request, "default");

        componentWith("w-1", 8, null, "productDetailBand", "default").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), any());
        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
    }

    // ── productDetailBand PPR fallback ───────────────────────────────────────

    @Test
    void productDetailBand_pprMode_usesPidUrlParam() {
        // band NOT marked (PDP did not run), but PPR active and pid present in URL
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("abc"),
                any(), anyInt(), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        componentWithPpr("w-1", 8, null, "productDetailBand", "default", "abc")
                .doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("abc"),
                any(), anyInt(), any(), any(), any());
    }

    @Test
    void productDetailBand_pprMode_noPidParam_returnsEmpty() {
        // PPR active but no pid param — should not call service
        componentWithPpr("w-1", 8, null, "productDetailBand", "default", null)
                .doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any(), any());
        verify(request).setModel("products", List.of());
    }

    @Test
    void productDetailBand_pprMode_usesBackfilledPidFromPageTree() {
        // PPR active, band NOT in cache, but backfill finds PID from page tree
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("backfilled-pid"),
                any(), anyInt(), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        new TestableRecommendationComponent(discoveryService, "w-1", 8, null,
                "productDetailBand", "default", true, null, Optional.of("backfilled-pid"))
                .doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("backfilled-pid"),
                any(), anyInt(), any(), any(), any());
    }

    @Test
    void productDetailBand_pprMode_backfillEmpty_fallsBackToUrlPid() {
        // PPR active, backfill returns empty, URL pid used instead
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("url-pid"),
                any(), anyInt(), any(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        new TestableRecommendationComponent(discoveryService, "w-1", 8, null,
                "productDetailBand", "default", true, "url-pid", Optional.empty())
                .doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("url-pid"),
                any(), anyInt(), any(), any(), any());
    }

    // ── testable subclass ───────────────────────────────────────────────────

    private static class TestableRecommendationComponent extends DiscoveryRecommendationComponent {

        private final HstDiscoveryService service;
        private final String widgetId;
        private final int componentLimit;
        private final String limitParam;
        private final String dataSource;
        private final String band;
        private final boolean isolatedRender;
        private final String pidParam;
        private final Optional<String> backfilledPid;

        TestableRecommendationComponent(HstDiscoveryService service, String widgetId,
                                         int componentLimit, String limitParam,
                                         String dataSource, String band,
                                         boolean isolatedRender, String pidParam) {
            this(service, widgetId, componentLimit, limitParam, dataSource, band,
                    isolatedRender, pidParam, Optional.empty());
        }

        TestableRecommendationComponent(HstDiscoveryService service, String widgetId,
                                         int componentLimit, String limitParam,
                                         String dataSource, String band,
                                         boolean isolatedRender, String pidParam,
                                         Optional<String> backfilledPid) {
            this.service = service;
            this.widgetId = widgetId;
            this.componentLimit = componentLimit;
            this.limitParam = limitParam;
            this.dataSource = dataSource;
            this.band = band;
            this.isolatedRender = isolatedRender;
            this.pidParam = pidParam;
            this.backfilledPid = backfilledPid;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryRecommendationComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryRecommendationComponentInfo() {
                @Override public String getDocument() { return null; }
                @Override public String getContextProductId() { return ""; }
                @Override public String getContextProductPidProperty() { return ""; }
                @Override public int getLimit() { return componentLimit; }
                @Override public boolean isShowPrice() { return true; }
                @Override public boolean isShowDescription() { return false; }
                @Override public String getDataSource() { return dataSource; }
                @Override public String getConnectTo() { return band; }
            };
        }

        @Override
        protected boolean isIsolatedComponentRender(HstRequest request) {
            return isolatedRender;
        }

        @Override
        protected Optional<String> backfillProductDetailPid(HstRequest request, String label) {
            return backfilledPid;
        }

        @Override
        String resolveWidgetId(HstRequest request) {
            return widgetId;
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return switch (name) {
                case WIDGET_ID_PARAM -> widgetId;
                case LIMIT_PARAM -> limitParam;
                case "pid" -> pidParam;
                default -> null;
            };
        }
    }
}
