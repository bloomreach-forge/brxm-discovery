package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
class DiscoveryRecommendationComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstRequestContext requestContext;

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);
        lenient().when(requestContext.getContentBean()).thenReturn(null);
    }

    private TestableRecommendationComponent componentWith(String widgetId,
                                                           int componentLimit, String limitParam) {
        return new TestableRecommendationComponent(discoveryService, widgetId,
                componentLimit, limitParam);
    }

    // ── limit: component default vs URL override ────────────────────────────

    @Test
    void limitFromComponentInfo_usedWhenUrlParamAbsent() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any())).thenReturn(List.of());

        componentWith("w-1", 20, null).doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(20), any(), any());
    }

    @Test
    void limitFromUrlParam_overridesComponentInfo() {
        when(discoveryService.recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any())).thenReturn(List.of());

        componentWith("w-1", 20, "5").doBeforeRender(request, response);

        verify(discoveryService).recommend(eq(request), any(), any(), any(), any(),
                eq(5), any(), any());
    }

    // ── model keys ──────────────────────────────────────────────────────────

    @Test
    void setsProductsAndWidgetIdOnModel() {
        List<ProductSummary> products = List.of();
        when(discoveryService.recommend(any(), any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(products);

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

    // ── testable subclass ───────────────────────────────────────────────────

    private static class TestableRecommendationComponent extends DiscoveryRecommendationComponent {

        private final HstDiscoveryService service;
        private final String widgetId;
        private final int componentLimit;
        private final String limitParam;

        TestableRecommendationComponent(HstDiscoveryService service, String widgetId,
                                         int componentLimit, String limitParam) {
            this.service = service;
            this.widgetId = widgetId;
            this.componentLimit = componentLimit;
            this.limitParam = limitParam;
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
            };
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
                default -> null;
            };
        }
    }
}
