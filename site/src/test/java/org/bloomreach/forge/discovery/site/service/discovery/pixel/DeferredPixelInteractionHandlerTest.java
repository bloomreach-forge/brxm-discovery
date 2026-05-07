package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRuntimeContext;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchSubmit;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SuggestClick;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.WidgetClick;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeferredPixelInteractionHandlerTest {

    @Mock DiscoveryPixelService pixelService;
    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock HttpServletRequest servletRequest;

    private DeferredPixelInteractionHandler handler;
    private DiscoveryRuntimeContext runtimeContext;
    private SearchQuery searchQuery;
    private ProductSummary product;

    /** Backs request-context attribute storage so deduplication state persists within a test. */
    private final Map<String, Object> ctxAttrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        handler = new DeferredPixelInteractionHandler(pixelService);

        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        lenient().doAnswer(inv -> ctxAttrs.put(inv.getArgument(0), inv.getArgument(1)))
                .when(requestContext).setAttribute(anyString(), any());
        lenient().doAnswer(inv -> ctxAttrs.get(inv.getArgument(0, String.class)))
                .when(requestContext).getAttribute(anyString());

        DiscoveryConfig config = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com",
                "https://suggest.dxpapi.com", "PRODUCTION", 12, "");
        runtimeContext = new DiscoveryRuntimeContext(
                config, new ClientContext(null, null, null), PixelFlags.DISABLED,
                null, "uid2", "https://example.com/search", "Search",
                "https://example.com", null, "1.2.3.4", null, null);

        searchQuery = new SearchQuery("shoes", 0, 12, "", Map.of(), "uid2",
                "https://ref.com", "https://example.com/search", null,
                null, List.of(), null, null, Map.of(), null, null);

        product = new ProductSummary("pid-1", "Running Shoe", "/shoe", null,
                BigDecimal.TEN, "USD", Map.of(), List.of());
    }

    // ── handleSearchInteraction - no event ────────────────────────────────

    @Test
    void handleSearchInteraction_noEventParam_noPixelFired() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn(null);

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleSearchInteraction_blankEventParam_noPixelFired() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("  ");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleSearchInteraction_unknownEvent_noPixelFired() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("page-view");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verifyNoInteractions(pixelService);
    }

    // ── handleSearchInteraction - search-submit ───────────────────────────

    @Test
    void handleSearchInteraction_searchSubmit_firesSearchSubmitEvent() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("search-submit");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        ArgumentCaptor<PixelEvent> captor = ArgumentCaptor.forClass(PixelEvent.class);
        verify(pixelService).fire(captor.capture(), any(), any(), any(), any());
        SearchSubmit event = assertInstanceOf(SearchSubmit.class, captor.getValue());
        assertEquals("suggest", event.group());
        assertEquals("submit", event.etype());
        assertEquals("shoes", event.query());
        assertEquals("search", event.ptype());
    }

    @Test
    void handleSearchInteraction_searchSubmit_deduplicates_secondCallSkipped() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("search-submit");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);
        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verify(pixelService, times(1)).fire(any(), any(), any(), any(), any());
    }

    // ── handleSearchInteraction - suggest-click ───────────────────────────

    @Test
    void handleSearchInteraction_suggestClick_missingAutoQuery_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("suggest-click");
        when(servletRequest.getParameter("brxdis_aq")).thenReturn(null);

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleSearchInteraction_suggestClick_blankAutoQuery_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("suggest-click");
        when(servletRequest.getParameter("brxdis_aq")).thenReturn("");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleSearchInteraction_suggestClick_firesEvent() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("suggest-click");
        when(servletRequest.getParameter("brxdis_aq")).thenReturn("sho");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        ArgumentCaptor<PixelEvent> captor = ArgumentCaptor.forClass(PixelEvent.class);
        verify(pixelService).fire(captor.capture(), any(), any(), any(), any());
        SuggestClick event = assertInstanceOf(SuggestClick.class, captor.getValue());
        assertEquals("suggest", event.group());
        assertEquals("click", event.etype());
        assertEquals("sho", event.autoQuery());
        assertEquals("shoes", event.query());
    }

    @Test
    void handleSearchInteraction_suggestClick_deduplicates() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("suggest-click");
        when(servletRequest.getParameter("brxdis_aq")).thenReturn("sho");

        handler.handleSearchInteraction(request, runtimeContext, searchQuery);
        handler.handleSearchInteraction(request, runtimeContext, searchQuery);

        verify(pixelService, times(1)).fire(any(), any(), any(), any(), any());
    }

    // ── handleProductInteraction - no / wrong event ───────────────────────

    @Test
    void handleProductInteraction_noEvent_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn(null);

        handler.handleProductInteraction(request, runtimeContext, product);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleProductInteraction_wrongEvent_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("search-submit");

        handler.handleProductInteraction(request, runtimeContext, product);

        verifyNoInteractions(pixelService);
    }

    // ── handleProductInteraction - widget-click validation ────────────────

    @Test
    void handleProductInteraction_widgetClick_missingWidgetId_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn(null);
        when(servletRequest.getParameter("brxdis_wty")).thenReturn("item");
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("result-1");

        handler.handleProductInteraction(request, runtimeContext, product);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleProductInteraction_widgetClick_missingWidgetType_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn("widget-1");
        when(servletRequest.getParameter("brxdis_wty")).thenReturn(null);
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("result-1");

        handler.handleProductInteraction(request, runtimeContext, product);

        verifyNoInteractions(pixelService);
    }

    @Test
    void handleProductInteraction_widgetClick_missingWidgetResultId_noFire() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn("widget-1");
        when(servletRequest.getParameter("brxdis_wty")).thenReturn("item");
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("");

        handler.handleProductInteraction(request, runtimeContext, product);

        verifyNoInteractions(pixelService);
    }

    // ── handleProductInteraction - widget-click fires ─────────────────────

    @Test
    void handleProductInteraction_widgetClick_allParams_firesEvent() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn("widget-1");
        when(servletRequest.getParameter("brxdis_wty")).thenReturn("item");
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("result-1");
        when(servletRequest.getParameter("brxdis_wq")).thenReturn("shoes");

        handler.handleProductInteraction(request, runtimeContext, product);

        ArgumentCaptor<PixelEvent> captor = ArgumentCaptor.forClass(PixelEvent.class);
        verify(pixelService).fire(captor.capture(), any(), any(), any(), any());
        WidgetClick event = assertInstanceOf(WidgetClick.class, captor.getValue());
        assertEquals("widget", event.group());
        assertEquals("widget-click", event.etype());
        assertEquals("widget-1", event.widgetId());
        assertEquals("item", event.widgetType());
        assertEquals("result-1", event.widgetResultId());
        assertEquals("pid-1", event.itemId());
    }

    @Test
    void handleProductInteraction_widgetClick_nullProduct_itemIdIsNull() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn("widget-1");
        when(servletRequest.getParameter("brxdis_wty")).thenReturn("item");
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("result-1");
        when(servletRequest.getParameter("brxdis_wq")).thenReturn(null);

        handler.handleProductInteraction(request, runtimeContext, null);

        ArgumentCaptor<PixelEvent> captor = ArgumentCaptor.forClass(PixelEvent.class);
        verify(pixelService).fire(captor.capture(), any(), any(), any(), any());
        WidgetClick event = assertInstanceOf(WidgetClick.class, captor.getValue());
        assertNull(event.itemId());
    }

    @Test
    void handleProductInteraction_widgetClick_deduplicates() {
        when(servletRequest.getParameter("brxdis_event")).thenReturn("widget-click");
        when(servletRequest.getParameter("brxdis_wid")).thenReturn("widget-1");
        when(servletRequest.getParameter("brxdis_wty")).thenReturn("item");
        when(servletRequest.getParameter("brxdis_wrid")).thenReturn("result-1");
        when(servletRequest.getParameter("brxdis_wq")).thenReturn(null);

        handler.handleProductInteraction(request, runtimeContext, product);
        handler.handleProductInteraction(request, runtimeContext, product);

        verify(pixelService, times(1)).fire(any(), any(), any(), any(), any());
    }
}
