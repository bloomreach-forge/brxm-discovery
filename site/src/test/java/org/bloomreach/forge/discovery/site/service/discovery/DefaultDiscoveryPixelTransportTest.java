package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelRateLimiter;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.CategoryPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.ClickAdd;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.ProductPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.Quickview;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchPageView;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchSubmit;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SuggestClick;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.TrackingContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.WidgetClick;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.WidgetView;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.bloomreach.forge.discovery.transport.DiscoveryTransportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDiscoveryPixelTransportTest {

    private static final DiscoverySettings TEST_SETTINGS = new DiscoverySettings(
            "https://core.dxpapi.com", "https://pathways.dxpapi.com",
            "https://suggest.dxpapi.com", 12, "");

    @Mock DiscoveryTransport transport;
    @Mock DiscoveryConfigProvider configProvider;

    private DefaultDiscoveryPixelTransport pixelTransport;
    private DiscoveryCredentials credentials;

    private static final PixelFlags FLAGS = new PixelFlags(true, false, false, "US");
    private static final TrackingContext EMPTY_CTX = new TrackingContext(null, null, null, null, null);

    @BeforeEach
    void setUp() {
        lenient().when(configProvider.settings()).thenReturn(TEST_SETTINGS);
        pixelTransport = new DefaultDiscoveryPixelTransport(transport, configProvider, new PixelRateLimiter(1000));
        credentials = new DiscoveryCredentials("acct", "domain", "key", null, "PRODUCTION");
    }

    private String path(org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent event) {
        return pixelTransport.buildPath(event, credentials, null, FLAGS);
    }

    // ── SearchPageView ────────────────────────────────────────────────────

    @Test
    void buildPath_searchPageView_typeAndPtype() {
        String p = path(new SearchPageView(EMPTY_CTX, "boots", List.of()));
        assertContains(p, "type=pageview");
        assertContains(p, "ptype=search");
        assertContains(p, "search_term=boots");
        assertNotContains(p, "group=");
        assertNotContains(p, "etype=");
    }

    @Test
    void buildPath_searchPageView_blankSearchTerm_omitted() {
        String p = path(new SearchPageView(EMPTY_CTX, "", List.of()));
        assertNotContains(p, "search_term=");
    }

    @Test
    void buildPath_searchPageView_skusIncluded() {
        var products = List.of(
                new ProductSummary("sku-1", null, null, null, null, null, Map.of(), List.of()),
                new ProductSummary("sku-2", null, null, null, null, null, Map.of(), List.of()));
        String p = path(new SearchPageView(EMPTY_CTX, "boots", products));
        assertContains(p, "sku=sku-1,sku-2");
    }

    // ── CategoryPageView ──────────────────────────────────────────────────

    @Test
    void buildPath_categoryPageView_typeAndPtype() {
        String p = path(new CategoryPageView(EMPTY_CTX, "cat-1", "Boots", List.of()));
        assertContains(p, "type=pageview");
        assertContains(p, "ptype=category");
        assertContains(p, "cat_id=cat-1");
        assertContains(p, "cat=Boots");
        assertNotContains(p, "etype=");
    }

    @Test
    void buildPath_categoryPageView_noCatWhenNameBlank() {
        String p = path(new CategoryPageView(EMPTY_CTX, "cat-1", null, List.of()));
        assertContains(p, "cat_id=cat-1");
        assertNotContains(p, "cat=");
    }

    // ── ProductPageView ───────────────────────────────────────────────────

    @Test
    void buildPath_productPageView_typeAndPtype() {
        String p = path(new ProductPageView(EMPTY_CTX, "pid-42", "Red Boot"));
        assertContains(p, "type=pageview");
        assertContains(p, "ptype=product");
        assertContains(p, "prod_id=pid-42");
        assertContains(p, "prod_name=Red Boot");
        assertNotContains(p, "etype=");
    }

    @Test
    void buildPath_productPageView_blankProdName_omitted() {
        String p = path(new ProductPageView(EMPTY_CTX, "pid-42", null));
        assertNotContains(p, "prod_name=");
    }

    // ── WidgetView ────────────────────────────────────────────────────────

    @Test
    void buildPath_widgetView_groupAndEtype() {
        String p = path(new WidgetView(EMPTY_CTX, "w-1", "keyword", "rid-1", null, null, List.of()));
        assertContains(p, "type=event");
        assertContains(p, "group=widget");
        assertContains(p, "etype=widget-view");
        assertContains(p, "ptype=content");
        assertContains(p, "wid=w-1");
        assertContains(p, "wty=keyword");
        assertContains(p, "wrid=rid-1");
    }

    @Test
    void buildPath_widgetView_customPtype() {
        String p = path(new WidgetView(EMPTY_CTX, "w-1", "keyword", "rid-1", null, "search", List.of()));
        assertContains(p, "ptype=search");
    }

    // ── WidgetClick ───────────────────────────────────────────────────────

    @Test
    void buildPath_widgetClick_groupAndEtype() {
        String p = path(new WidgetClick(EMPTY_CTX, "w-1", "item", "rid-1", "ctx-pid", "item-1", null));
        assertContains(p, "type=event");
        assertContains(p, "group=widget");
        assertContains(p, "etype=widget-click");
        assertContains(p, "ptype=content");
        assertContains(p, "wid=w-1");
        assertContains(p, "item_id=item-1");
    }

    // ── SearchSubmit ──────────────────────────────────────────────────────

    @Test
    void buildPath_searchSubmit_groupAndEtype() {
        String p = path(new SearchSubmit(EMPTY_CTX, "boots", null));
        assertContains(p, "type=event");
        assertContains(p, "group=suggest");
        assertContains(p, "etype=submit");
        assertContains(p, "ptype=search");
        assertContains(p, "q=boots");
    }

    // ── SuggestClick ──────────────────────────────────────────────────────

    @Test
    void buildPath_suggestClick_groupAndEtype() {
        String p = path(new SuggestClick(EMPTY_CTX, "boots", "boo", null));
        assertContains(p, "type=event");
        assertContains(p, "group=suggest");
        assertContains(p, "etype=click");
        assertContains(p, "ptype=search");
        assertContains(p, "q=boots");
        assertContains(p, "aq=boo");
    }

    // ── ClickAdd ──────────────────────────────────────────────────────────

    @Test
    void buildPath_clickAdd_groupAndEtype() {
        String p = path(new ClickAdd(EMPTY_CTX, "item-1", "sku-1", null));
        assertContains(p, "type=event");
        assertContains(p, "group=cart");
        assertContains(p, "etype=click-add");
        assertContains(p, "ptype=product");
        assertContains(p, "item_id=item-1");
        assertContains(p, "sku=sku-1");
    }

    // ── Quickview ─────────────────────────────────────────────────────────

    @Test
    void buildPath_quickview_groupAndEtype() {
        String p = path(new Quickview(EMPTY_CTX, "item-1", "sku-1", null));
        assertContains(p, "type=event");
        assertContains(p, "group=product");
        assertContains(p, "etype=quickview");
        assertContains(p, "ptype=product");
        assertContains(p, "item_id=item-1");
    }

    // ── Common ────────────────────────────────────────────────────────────

    @Test
    void buildPath_alwaysContainsAccountAndDomainKey() {
        String p = path(new SearchPageView(EMPTY_CTX, null, List.of()));
        assertContains(p, "acct_id=acct");
        assertContains(p, "domain_key=domain");
    }

    @Test
    void buildPath_testDataFlag_appendsTestDataParam() {
        PixelFlags testFlags = new PixelFlags(true, true, false, "US");
        String p = pixelTransport.buildPath(new SearchPageView(EMPTY_CTX, null, List.of()), credentials, null, testFlags);
        assertContains(p, "test_data=true");
    }

    @Test
    void buildPath_debugFlag_appendsDebugParam() {
        PixelFlags debugFlags = new PixelFlags(true, false, true, "US");
        String p = pixelTransport.buildPath(new SearchPageView(EMPTY_CTX, null, List.of()), credentials, null, debugFlags);
        assertContains(p, "debug=true");
    }

    @Test
    void buildPath_clientIpPresent_appendsClientIp() {
        String p = pixelTransport.buildPath(new SearchPageView(EMPTY_CTX, null, List.of()), credentials, "10.0.0.1", FLAGS);
        assertContains(p, "client_ip=10.0.0.1");
    }

    @Test
    void buildPath_clientTsInMicroseconds() {
        long before = System.currentTimeMillis() * 1000L;
        String p = path(new SearchPageView(EMPTY_CTX, null, List.of()));
        long after = System.currentTimeMillis() * 1000L;

        String tsParam = extractParam(p, "client_ts");
        assertNotNull(tsParam, "client_ts should be present");
        long ts = Long.parseLong(tsParam);
        assertTrue(ts >= before && ts <= after, "client_ts=" + ts + " should be in [" + before + "," + after + "]");
    }

    // ── Bug-fix regressions ───────────────────────────────────────────────

    @Test
    void buildPath_widgetView_etypeIsWidgetView_notView() {
        String p = path(new WidgetView(EMPTY_CTX, "w-1", "keyword", "rid-1", null, null, List.of()));
        assertContains(p, "etype=widget-view");
        assertNotContains(p, "etype=view");
    }

    @Test
    void buildPath_widgetClick_etypeIsWidgetClick_notClick() {
        String p = path(new WidgetClick(EMPTY_CTX, "w-1", "item", "rid-1", null, null, null));
        assertContains(p, "etype=widget-click");
        assertNotContains(p, "etype=click");
    }

    @Test
    void buildPath_cookie2_decodesAlreadyEncodedBrUid2() {
        TrackingContext ctx = new TrackingContext("uid%3Dfoo%3Av%3D15.0", null, null, null, null);
        String p = path(new SearchPageView(ctx, "shoes", List.of()));
        assertContains(p, "cookie2=uid=foo:v=15.0");
        assertNotContains(p, "cookie2=uid%3D");
    }

    @Test
    void buildPath_productPageView_urlStripsQueryString() {
        TrackingContext ctx = new TrackingContext(null, null, null, "https://example.com/product?pid=42", null);
        String p = path(new ProductPageView(ctx, "pid-42", null));
        assertContains(p, "url=https://example.com/product");
        assertNotContains(p, "url=https://example.com/product?");
    }

    @Test
    void buildPath_productPageView_keepsSuggestQueryParam() {
        TrackingContext ctx = new TrackingContext(null, null, null,
                "https://example.com/product?pid=42&_br_psugg_q=boots&page=2", null);
        String p = path(new ProductPageView(ctx, "pid-42", null));
        assertContains(p, "url=https://example.com/product?_br_psugg_q=boots");
        assertNotContains(p, "page=2");
    }

    @Test
    void buildPath_searchPageView_urlStripsQueryString() {
        TrackingContext ctx = new TrackingContext(null, null, null,
                "https://example.com/search?q=shoes&page=2", null);
        String p = path(new SearchPageView(ctx, "shoes", List.of()));
        assertContains(p, "url=https://example.com/search");
        assertNotContains(p, "url=https://example.com/search?");
    }

    @Test
    void buildPath_skuListLimitedToFirst20() {
        List<ProductSummary> manyProducts = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new ProductSummary("p" + i, null, null, null, null, null, Map.of(), List.of()))
                .toList();
        String p = path(new SearchPageView(EMPTY_CTX, "q", manyProducts));
        String skuValue = extractParam(p, "sku");
        assertNotNull(skuValue);
        assertEquals(20, java.util.Arrays.stream(skuValue.split(",")).count(),
                "pixel must include at most 20 SKUs");
    }

    // ── fire - EU / US base URI ───────────────────────────────────────────

    @Test
    void fire_euRegion_usesEuPixelBaseUri() {
        PixelFlags euFlags = new PixelFlags(true, false, false, "EU");
        pixelTransport.fire("/pix.gif?type=pageview", ClientContext.EMPTY, euFlags);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://p-eu.brsrvr.com"),
                "EU region must use EU pixel base URI");
    }

    @Test
    void fire_usRegion_usesDefaultPixelBaseUri() {
        pixelTransport.fire("/pix.gif?type=pageview", ClientContext.EMPTY, FLAGS);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://p.brsrvr.com"),
                "US region must use default pixel base URI");
    }

    // ── rate limiting ─────────────────────────────────────────────────────

    @Test
    void fire_rateLimitExhausted_secondCallDropped() {
        var tightLimiter = new PixelRateLimiter(1);
        var tightTransport = new DefaultDiscoveryPixelTransport(transport, configProvider, tightLimiter);

        tightTransport.fire("/pix.gif?type=pageview", ClientContext.EMPTY, FLAGS); // consumes the 1 token
        tightTransport.fire("/pix.gif?type=pageview", ClientContext.EMPTY, FLAGS); // must be dropped

        verify(transport, times(1)).execute(any());
        tightLimiter.close();
    }

    @Test
    void fire_429Response_drainsRateLimiter_subsequentCallDropped() {
        var limiter = new PixelRateLimiter(10);
        var t = new DefaultDiscoveryPixelTransport(transport, configProvider, limiter);
        when(transport.execute(any()))
                .thenThrow(new SearchException("Discovery API returned HTTP 429: Too Many Requests"));

        t.fire("/pix.gif?type=pageview", ClientContext.EMPTY, FLAGS); // 429 → drain
        t.fire("/pix.gif?type=pageview", ClientContext.EMPTY, FLAGS); // drained → dropped

        verify(transport, times(1)).execute(any());
        limiter.close();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static void assertContains(String path, String fragment) {
        assertTrue(path.contains(fragment), "Expected '" + fragment + "' in: " + path);
    }

    private static void assertNotContains(String path, String fragment) {
        assertFalse(path.contains(fragment), "Expected '" + fragment + "' to be absent from: " + path);
    }

    private static String extractParam(String path, String key) {
        int idx = path.indexOf(key + "=");
        if (idx < 0) return null;
        int start = idx + key.length() + 1;
        int end   = path.indexOf('&', start);
        return end < 0 ? path.substring(start) : path.substring(start, end);
    }
}
