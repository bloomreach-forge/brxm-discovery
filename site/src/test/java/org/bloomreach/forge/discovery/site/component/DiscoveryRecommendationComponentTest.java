package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
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

    /** Standalone mode (useProductDetailContext=false). */
    private TestableRecommendationComponent standalone(String widgetId,
                                                        int componentLimit, String limitParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, false, false, null);
    }

    /** useProductDetailContext mode. */
    private TestableRecommendationComponent pdpContext(String widgetId, int componentLimit,
                                                        String limitParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, true, false, null);
    }

    /** useProductDetailContext mode, in PPR (isolated render). */
    private TestableRecommendationComponent pdpContextPpr(String widgetId, int componentLimit,
                                                            String limitParam, String pidParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam, true, true, pidParam);
    }

    // ── limit: component default vs URL override ────────────────────────────

    @Test
    void limitFromComponentInfo_usedWhenUrlParamAbsent() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        standalone("w-1", 20, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any());
    }

    @Test
    void limitFromUrlParam_overridesComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        standalone("w-1", 20, "5").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any());
    }

    // ── model keys ──────────────────────────────────────────────────────────

    @Test
    void setsProductsAndWidgetIdOnModel() {
        List<ProductSummary> products = List.of();
        when(discoveryService.recommend(any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(RecommendationResult.of(products));

        standalone("w-42", 8, null).doBeforeRender(request, response);

        verify(request).setModel("products", products);
        verify(request).setModel("widgetId", "w-42");
    }

    @Test
    void nullWidgetId_setsEmptyStringOnModel() {
        standalone(null, 8, null).doBeforeRender(request, response);

        verify(request).setModel("widgetId", "");
    }

    // ── useProductDetailContext mode ─────────────────────────────────────────

    @Test
    void pdpContext_readsPidFromCachedProduct() {
        ProductSummary cached = new ProductSummary("p-from-pdp", "T", null, null, null, null, Map.of());
        DiscoveryRequestCache.markProductDetailRendered(request);
        DiscoveryRequestCache.putProductResult(request, cached);
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("p-from-pdp"),
                any(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        pdpContext("w-1", 8, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("p-from-pdp"),
                any(), anyInt(), any(), any());
    }

    @Test
    void pdpContext_bandAbsent_noPidParam_returnsEmpty_live() {
        // PDP not marked, no pid param → not edit mode → empty products, no warning
        pdpContext("w-1", 8, null).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
        verify(request, never()).setAttribute(eq("brxdis_warning"), any());
    }

    @Test
    void pdpContext_bandAbsent_noPidParam_setsWarning_inEditMode() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        pdpContext("w-1", 8, null).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), argThat(msg ->
                msg.toString().contains("Product Detail")));
        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void pdpContext_bandPresentNoProduct_returnsEmpty() {
        DiscoveryRequestCache.markProductDetailRendered(request);
        // No putProductResult → cache empty

        pdpContext("w-1", 8, null).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
    }

    @Test
    void pdpContext_bandPresentNoProduct_setsWarning_inEditMode() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);
        DiscoveryRequestCache.markProductDetailRendered(request);

        pdpContext("w-1", 8, null).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), any());
        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    // ── useProductDetailContext PPR fallback ─────────────────────────────────

    @Test
    void pdpContext_pprMode_usesPidUrlParam() {
        // PDP not marked, but pid present in URL — PPR fallback
        when(discoveryService.recommend(eq(request), eq("w-1"), any(), eq("abc"),
                any(), anyInt(), any(), any())).thenReturn(RecommendationResult.of(List.of()));

        pdpContextPpr("w-1", 8, null, "abc").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), eq("w-1"), any(), eq("abc"),
                any(), anyInt(), any(), any());
    }

    @Test
    void pdpContext_pprMode_noPidParam_returnsEmpty() {
        // PPR active but no pid param — should not call service
        pdpContextPpr("w-1", 8, null, null).doBeforeRender(request, response);

        verify(discoveryService, never()).recommend(any(), any(), any(), any(), any(), anyInt(), any(), any());
        verify(request).setModel("products", List.of());
    }

    // ── testable subclass ───────────────────────────────────────────────────

    private static class TestableRecommendationComponent extends DiscoveryRecommendationComponent {

        private final HstDiscoveryService service;
        private final String widgetId;
        private final int componentLimit;
        private final String limitParam;
        private final boolean useProductDetailContext;
        private final boolean isolatedRender;
        private final String pidParam;

        TestableRecommendationComponent(HstDiscoveryService service, String widgetId,
                                         int componentLimit, String limitParam,
                                         boolean useProductDetailContext,
                                         boolean isolatedRender, String pidParam) {
            this.service = service;
            this.widgetId = widgetId;
            this.componentLimit = componentLimit;
            this.limitParam = limitParam;
            this.useProductDetailContext = useProductDetailContext;
            this.isolatedRender = isolatedRender;
            this.pidParam = pidParam;
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
                @Override public boolean isUseProductDetailContext() { return useProductDetailContext; }
            };
        }

        @Override
        protected boolean isIsolatedComponentRender(HstRequest request) {
            return isolatedRender;
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
