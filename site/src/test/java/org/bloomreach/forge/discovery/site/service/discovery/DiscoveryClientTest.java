package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.exception.RecommendationException;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryClientTest {

    @Mock ResourceServiceBroker broker;
    @Mock DiscoveryResponseMapper responseMapper;
    @Mock Resource resource;

    private DiscoveryClientImpl client;
    private DiscoveryConfig config;
    private DiscoveryConfig v2Config;

    @BeforeEach
    void setUp() {
        client = new DiscoveryClientImpl(broker, responseMapper);
        config = new DiscoveryConfig(
                "acct123", "myDomain", "secret-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "price asc");
        v2Config = new DiscoveryConfig(
                "acct123", "myDomain", "secret-key", "my-auth-key",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "price asc");
    }

    // --- search ---

    @Test
    void search_usesSearchResourceSpace() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        var expectedResponse = new SearchResponse(expectedResult, SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        SearchResponse response = client.search(query, config, ClientContext.EMPTY);

        assertSame(expectedResult, response.result());
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void search_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.search(query, config, ClientContext.EMPTY));
    }

    @Test
    void search_withCatalogName_includesCatalogNameParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, "blog_en");
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), contains("catalog_name=blog_en"), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        SearchResponse response = client.search(query, config, ClientContext.EMPTY);

        assertNotNull(response);
    }

    @Test
    void search_pathContainsRequestId() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), contains("request_id="), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        assertNotNull(client.search(query, config, ClientContext.EMPTY));
    }

    @Test
    void search_noCatalogName_omitsCatalogNameParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), argThat(path -> !path.contains("catalog_name")),
                any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        assertNotNull(client.search(query, config, ClientContext.EMPTY));
    }

    @Test
    void search_withStatsFields_appendsStatsFieldParams() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null,
                List.of("price", "sale_price"));
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.search(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        String path = pathCaptor.getValue();
        assertTrue(path.contains("stats.field=price"), "Should contain stats.field=price");
        assertTrue(path.contains("stats.field=sale_price"), "Should contain stats.field=sale_price");
    }

    @Test
    void search_withoutStatsFields_noStatsFieldParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.search(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertFalse(pathCaptor.getValue().contains("stats.field"), "Should not contain stats.field when not configured");
    }

    // --- category ---

    @Test
    void category_usesSearchResourceSpace() throws ResourceException {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        var expectedResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        var expectedResponse = new SearchResponse(expectedResult, SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        SearchResponse response = client.category(query, config, ClientContext.EMPTY);

        assertSame(expectedResult, response.result());
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void category_pathContainsRequestId() throws ResourceException {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPI"), contains("request_id="), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        assertNotNull(client.category(query, config, ClientContext.EMPTY));
    }

    @Test
    void category_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.category(query, config, ClientContext.EMPTY));
    }

    @Test
    void category_withStatsFields_appendsStatsFieldParams() throws ResourceException {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null,
                List.of("price"));
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 12)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.category(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertTrue(pathCaptor.getValue().contains("stats.field=price"), "Should contain stats.field=price");
    }

    // --- segment (C1) ---

    @Test
    void search_withSegment_appendsSegmentParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null, List.of(), "NorthAmerica", null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.search(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertTrue(pathCaptor.getValue().contains("segment=NorthAmerica"));
    }

    @Test
    void search_withoutSegment_noSegmentParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.search(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertFalse(pathCaptor.getValue().contains("segment="), "Should not contain segment param");
    }

    @Test
    void category_withSegment_appendsSegmentParam() throws ResourceException {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null, List.of(), "SouthAmerica", null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 12)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.category(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertTrue(pathCaptor.getValue().contains("segment=SouthAmerica"));
    }

    // --- efq exclusion filter (C3) ---

    @Test
    void search_withEfq_appendsEfqParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null, List.of(), null, "price:[10 TO *]");
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.search(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertTrue(pathCaptor.getValue().contains("efq="), "Should contain efq param");
    }

    @Test
    void category_withEfq_appendsEfqParam() throws ResourceException {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null, List.of(), null, "out_of_stock:false");
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty());
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 12)).thenReturn(expectedResponse);

        var pathCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        client.category(query, config, ClientContext.EMPTY);

        verify(broker).resolve(anyString(), pathCaptor.capture(), nullable(ExchangeHint.class));
        assertTrue(pathCaptor.getValue().contains("efq="), "Should contain efq param");
    }

    // --- recommend v1 ---

    @Test
    void recommend_noAuthKey_usesSearchResourceSpace() throws ResourceException {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expected = RecommendationResult.of(List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null)));
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expected);

        RecommendationResult result = client.recommend(query, config, ClientContext.EMPTY);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_v1_resourceException_wrapsInRecommendationException() throws ResourceException {
        var query = new RecQuery("widget-1", null, null, 8);
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, config, ClientContext.EMPTY));
    }

    // --- recommend v2 ---

    @Test
    void recommend_withAuthKey_usesPathwaysResourceSpace() throws ResourceException {
        var query = new RecQuery("item", "widget-1", "prod-42", "pdp", 6, null, null, null, null, null);
        var expected = RecommendationResult.of(List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null)));
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expected);

        RecommendationResult result = client.recommend(query, v2Config, ClientContext.EMPTY);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_withAuthKey_neverUsesSearchResourceSpace() throws ResourceException {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(RecommendationResult.of(List.of()));

        client.recommend(query, v2Config, ClientContext.EMPTY);

        verify(broker, never()).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_v2_resourceException_wrapsInRecommendationException() throws ResourceException {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, v2Config, ClientContext.EMPTY));
    }

    // --- pixel path builders ---

    @Test
    void buildSearchPixelPath_containsRequiredParams() {
        var query = new SearchQuery("shoes", 0, 10, null, null, "uid-val", "http://ref.com", "http://page.com");
        var result = new SearchResult(
                List.of(new ProductSummary("p1", null, null, null, null, null, null),
                        new ProductSummary("p2", null, null, null, null, null, null)),
                2L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=pageview"), "should contain type=pageview");
        assertTrue(path.contains("ptype=search"), "should contain ptype=search");
        assertTrue(path.contains("search_term=shoes"), "should contain search_term");
        assertTrue(path.contains("acct_id=acct123"), "should contain acct_id");
        assertTrue(path.contains("domain_key=myDomain"), "should contain domain_key");
        assertTrue(path.contains("sku=p1,p2"), "should contain comma-separated PIDs");
        assertTrue(path.contains("version=ss-v0.1"), "should contain version=ss-v0.1");
    }

    @Test
    void buildCategoryPixelPath_containsCategoryParams() {
        var query = new CategoryQuery("cat-99", 0, 10, null, null, "uid-val", null, "http://page.com");
        var result = new SearchResult(
                List.of(new ProductSummary("pid1", null, null, null, null, null, null)),
                1L, 0, 10, Map.of());

        String path = client.buildCategoryPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=pageview"), "should contain type=pageview");
        assertTrue(path.contains("ptype=category"), "should contain ptype=category");
        assertTrue(path.contains("cat_id=cat-99"), "should contain cat_id");
        assertTrue(path.contains("sku=pid1"), "should contain sku");
        assertTrue(path.contains("version=ss-v0.1"), "should contain version=ss-v0.1");
    }

    @Test
    void buildWidgetPixelPath_containsWidgetParams() {
        var query = new RecQuery("item", "w-123", "ctx-pid", null, 8, null, null, "http://page.com", "http://ref.com", "uid-val");
        var result = new RecommendationResult("rid-abc", List.of(new ProductSummary("prod-1", null, null, null, null, null, null)));

        String path = client.buildWidgetPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=event"), "should contain type=event");
        assertTrue(path.contains("group=widget"), "should contain group=widget");
        assertTrue(path.contains("etype=view"), "should contain etype=view");
        assertTrue(path.contains("wid=w-123"), "should contain wid");
        assertTrue(path.contains("wty=item"), "should contain wty");
        assertTrue(path.contains("wrid=rid-abc"), "should contain wrid");
        assertTrue(path.contains("wq=ctx-pid"), "should contain wq");
        assertTrue(path.contains("cookie2=uid-val"), "should contain cookie2");
        assertTrue(path.contains("sku=prod-1"), "should contain sku");
        assertTrue(path.contains("version=ss-v0.1"), "should contain version=ss-v0.1");
    }

    @Test
    void buildSearchPixelPath_typeIsLowercasePageview() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=pageview"), "spec requires lowercase 'pageview'");
        assertFalse(path.contains("type=pageView"), "mixed-case 'pageView' is wrong per spec");
    }

    @Test
    void buildCategoryPixelPath_typeIsLowercasePageview() {
        var query = new CategoryQuery("cat-1", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildCategoryPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=pageview"), "spec requires lowercase 'pageview'");
        assertFalse(path.contains("type=pageView"), "mixed-case 'pageView' is wrong per spec");
    }

    @Test
    void buildWidgetPixelPath_hasCorrectEventParams() {
        var query = new RecQuery("item", "w-1", "pid-ctx", "pdp", 8, null, null, "http://page.com", "http://ref.com", "uid-123");
        var result = new RecommendationResult("rid-xyz", List.of(new ProductSummary("p1", null, null, null, null, null, null)));

        String path = client.buildWidgetPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=event"), "widget pixel type must be 'event'");
        assertTrue(path.contains("group=widget"), "group=widget required");
        assertTrue(path.contains("etype=view"), "etype=view required");
        assertTrue(path.contains("wid=w-1"), "wid required");
        assertTrue(path.contains("wty=item"), "wty required");
        assertTrue(path.contains("wrid=rid-xyz"), "wrid from RecommendationResult required");
        assertTrue(path.contains("wq=pid-ctx"), "wq is context product id");
        assertTrue(path.contains("cookie2=uid-123"), "tracking: cookie2 required");
        assertTrue(path.contains("ref=http://ref.com"), "tracking: ref required");
        assertTrue(path.contains("sku=p1"), "sku list required");
    }

    @Test
    void buildWidgetPixelPath_noWrid_omitsParam() {
        var query = new RecQuery("item", "w-1", null, null, 8, null, null, null, null, null);
        var result = RecommendationResult.of(List.of());

        String path = client.buildWidgetPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertFalse(path.contains("wrid="), "wrid must be absent when widgetResultId is null");
    }

    @Test
    void buildProductPageViewPixelPath_hasRequiredParams() {
        String path = client.buildProductPageViewPixelPath("pid-42", "Blue Sneakers", "uid-val", "http://ref.com", "http://page.com", config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("type=pageview"), "type=pageview required");
        assertTrue(path.contains("ptype=product"), "ptype=product required");
        assertTrue(path.contains("prod_id=pid-42"), "prod_id required");
        assertTrue(path.contains("prod_name="), "prod_name required");
        assertTrue(path.contains("acct_id=acct123"), "acct_id required");
        assertTrue(path.contains("domain_key=myDomain"), "domain_key required");
        assertTrue(path.contains("cookie2=uid-val"), "tracking: cookie2 required");
        assertTrue(path.contains("ref=http://ref.com"), "tracking: ref required");
        assertTrue(path.contains("url=http://page.com"), "tracking: url required");
        assertTrue(path.contains("version=ss-v0.1"), "version=ss-v0.1 required");
    }

    // --- Phase 2: server-side pixel params ---

    @Test
    void buildSearchPixelPath_includesRandAndClientTs() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("rand="), "rand nonce required");
        assertTrue(path.contains("client_ts="), "client_ts required");
    }

    @Test
    void buildSearchPixelPath_stripsQueryStringFromUrl() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null,
                "https://example.com/search?q=shoes&page=2");
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("url=https://example.com/search"), "url must be base URL only");
        assertFalse(path.contains("url=https://example.com/search?"), "url must not include query string");
    }

    @Test
    void buildSearchPixelPath_includesClientIp() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, "203.0.113.42", PixelFlags.DISABLED);

        assertTrue(path.contains("client_ip=203.0.113.42"), "client_ip required");
        assertFalse(path.contains("user_agent="), "user_agent must not appear in path (forwarded as header)");
    }

    @Test
    void buildSearchPixelPath_blankClientIp_omitsParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, "", PixelFlags.DISABLED);

        assertFalse(path.contains("client_ip="), "blank clientIp must be omitted");
    }

    @Test
    void buildWidgetPixelPath_serverSideParams_present() {
        var query = new RecQuery("item", "w-1", "ctx-pid", null, 8, null, null, null, null, null);
        var result = RecommendationResult.of(List.of());

        String path = client.buildWidgetPixelPath(query, result, config, "10.0.0.1", PixelFlags.DISABLED);

        assertTrue(path.contains("rand="), "rand required");
        assertTrue(path.contains("client_ts="), "client_ts required");
        assertTrue(path.contains("client_ip=10.0.0.1"), "client_ip required");
        assertFalse(path.contains("user_agent="), "user_agent must not appear in path (forwarded as header)");
    }

    @Test
    void buildProductPageViewPixelPath_serverSideParams_present() {
        String path = client.buildProductPageViewPixelPath("pid-42", null, null, null,
                "https://example.com/product?id=42", config, "192.168.1.1", PixelFlags.DISABLED);

        assertTrue(path.contains("rand="), "rand required");
        assertTrue(path.contains("client_ts="), "client_ts required");
        assertTrue(path.contains("client_ip=192.168.1.1"), "client_ip required");
        assertFalse(path.contains("user_agent="), "user_agent must not appear in path (forwarded as header)");
        assertTrue(path.contains("url=https://example.com/product"), "url base required");
        assertFalse(path.contains("url=https://example.com/product?"), "query string must be stripped");
    }

    @Test
    void recommend_returnsRecommendationResult() throws ResourceException {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expectedResult = new RecommendationResult("rid-1", List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null)));
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expectedResult);

        RecommendationResult result = client.recommend(query, config, ClientContext.EMPTY);

        assertNotNull(result);
        assertEquals("rid-1", result.widgetResultId());
        assertEquals(1, result.products().size());
    }

    @Test
    void buildSearchPixelPath_limitsSkuToFirst20() {
        var query = new SearchQuery("q", 0, 25, null, null, null, null, null);
        List<ProductSummary> manyProducts = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new ProductSummary("p" + i, null, null, null, null, null, null))
                .toList();
        var result = new SearchResult(manyProducts, 25L, 0, 25, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        long skuCount = java.util.Arrays.stream(
                path.substring(path.indexOf("sku=") + 4).split(",")).count();
        assertEquals(20, skuCount, "pixel path should include at most 20 SKUs");
    }

    @Test
    void firePixelEvent_resourceException_doesNotThrow() throws Exception {
        when(broker.resolve(eq("discoveryPixelAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertDoesNotThrow(() -> client.firePixelEvent("/pix.gif?type=pageView", config, ClientContext.EMPTY,
                new PixelFlags(true, false, false, "US")));
    }

    @Test
    void buildSearchPixelPath_cookie2_decodesAlreadyEncodedValue() {
        // brUid2 arrives from browser percent-encoded; must be decoded once so CRISP single-encodes
        var query = new SearchQuery("shoes", 0, 10, null, null, "uid%3Dfoo%3Av%3D15.0", null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertTrue(path.contains("cookie2=uid=foo:v=15.0"), "brUid2 must be URL-decoded before appending");
        assertFalse(path.contains("cookie2=uid%3Dfoo"), "double-encoded value must not appear in path");
    }


    // ── PixelFlags appended to pixel paths ──────────────────────────────────

    @Test
    void buildSearchPixelPath_withTestData_appendsParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null,
                new PixelFlags(true, true, false, "US"));

        assertTrue(path.contains("&test_data=true"), "test_data=true must be appended");
        assertFalse(path.contains("debug=true"), "debug must be absent");
    }

    @Test
    void buildSearchPixelPath_withDebug_appendsParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null,
                new PixelFlags(true, false, true, "US"));

        assertTrue(path.contains("&debug=true"), "debug=true must be appended");
        assertFalse(path.contains("test_data=true"), "test_data must be absent");
    }

    @Test
    void buildSearchPixelPath_noFlags_noExtraParams() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config, null, PixelFlags.DISABLED);

        assertFalse(path.contains("test_data="), "test_data must be absent when not set");
        assertFalse(path.contains("debug="), "debug must be absent when not set");
    }

    // ── toV2WidgetType ───────────────────────────────────────────────────────

    @Test
    void toV2WidgetType_knownV2Types_passThrough() {
        for (String type : List.of("item", "keyword", "category", "personalized", "global", "visual")) {
            assertEquals(type, DiscoveryClientImpl.toV2WidgetType(type));
        }
    }

    @Test
    void toV2WidgetType_mlt_mapsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType("mlt"));
    }

    @Test
    void toV2WidgetType_unknownType_defaultsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType("legacy_unknown"));
    }

    @Test
    void toV2WidgetType_nullOrBlank_defaultsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType(null));
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType(""));
    }

    @Test
    void recommend_v1_pathContainsRequestId() throws ResourceException {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        when(broker.resolve(eq("discoverySearchAPI"), contains("request_id="), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(RecommendationResult.of(List.of()));

        assertNotNull(client.recommend(query, config, ClientContext.EMPTY));
    }

    @Test
    void fetchProduct_pathContainsRequestId() throws ResourceException {
        when(broker.resolve(eq("discoverySearchAPI"), contains("request_id="), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toSearchResult(resource, 0, 1))
                .thenReturn(new SearchResult(List.of(), 0L, 0, 1, Map.of()));

        assertNotNull(client.fetchProduct("pid-1", null, config, ClientContext.EMPTY));
    }

    @Test
    void requestId_extractsValueFromPath() {
        String path = "/api/v1/core/?account_id=123&request_id=abc-uuid-123&q=shoes";
        assertEquals("abc-uuid-123", DiscoveryClientImpl.requestId(path));
    }

    @Test
    void requestId_lastParam_extractsCorrectly() {
        String path = "/api/v1/core/?account_id=123&request_id=my-uuid";
        assertEquals("my-uuid", DiscoveryClientImpl.requestId(path));
    }

    @Test
    void requestId_missing_returnsNa() {
        assertEquals("n/a", DiscoveryClientImpl.requestId("/api/v1/core/?account_id=123"));
    }

    @Test
    void redactPath_replacesAuthKeyValue() {
        String path = "/api/v1/core?account_id=123&auth_key=super-secret&q=shoes";

        assertEquals("/api/v1/core?account_id=123&auth_key=***&q=shoes",
                DiscoveryClientImpl.redactPath(path));
    }

    @Test
    void redactPath_noAuthKey_returnsPathUnchanged() {
        String path = "/api/v1/core?account_id=123&q=shoes";

        assertEquals(path, DiscoveryClientImpl.redactPath(path));
    }

    // --- autosuggest ---

    @Test
    void autosuggest_usesAutosuggestResourceSpace() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of("shirts"), List.of(), List.of());
        when(broker.resolve(eq("discoveryAutosuggestAPI"), anyString(), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config, ClientContext.EMPTY);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoveryAutosuggestAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void autosuggest_pathContainsRequiredParams() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(broker.resolve(eq("discoveryAutosuggestAPI"), argThat(path ->
                path.contains("account_id=acct123")
                && path.contains("domain_key=myDomain")
                && path.contains("request_type=suggest")
                && path.contains("q=shi")
                && path.contains("catalog_views=myDomain")
        ), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config, ClientContext.EMPTY);

        assertSame(expected, result);
    }

    @Test
    void autosuggest_withCatalogViews_includesCatalogViewsParam() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8, "store:products_en", null, null, null);
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(broker.resolve(eq("discoveryAutosuggestAPI"),
                contains("catalog_views=store:products_en"), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config, ClientContext.EMPTY);

        assertSame(expected, result);
    }

    @Test
    void autosuggest_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        when(broker.resolve(eq("discoveryAutosuggestAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.autosuggest(query, config, ClientContext.EMPTY));
    }

    @Test
    void autosuggest_withApiKey_includesAuthKeyQueryParam() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(broker.resolve(eq("discoveryAutosuggestAPI"),
                contains("auth_key=secret-key"), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config, ClientContext.EMPTY);

        assertSame(expected, result);
    }

    // ── buildHint / buildV2Hint ────────────────────────────────────────────────

    @Test
    void buildHint_withClientContext_includesAllThreeHeaders() {
        var ctx = new ClientContext("Mozilla/5.0", "en-US,en;q=0.9", "203.0.113.42");

        ExchangeHint hint = DiscoveryClientImpl.buildHint(ctx);

        var headers = hint.getRequestHeaders();
        assertTrue(headers.containsKey("User-Agent"), "User-Agent header should be present");
        assertEquals("Mozilla/5.0", headers.get("User-Agent").get(0));
        assertTrue(headers.containsKey("Accept-Language"), "Accept-Language header should be present");
        assertEquals("en-US,en;q=0.9", headers.get("Accept-Language").get(0));
        assertTrue(headers.containsKey("X-Forwarded-For"), "X-Forwarded-For header should be present");
        assertEquals("203.0.113.42", headers.get("X-Forwarded-For").get(0));
    }

    @Test
    void buildHint_withEmptyContext_noClientHeaders() {
        ExchangeHint hint = DiscoveryClientImpl.buildHint(ClientContext.EMPTY);

        var headers = hint.getRequestHeaders();
        assertFalse(headers.containsKey("User-Agent"), "User-Agent must be absent for EMPTY ctx");
        assertFalse(headers.containsKey("Accept-Language"), "Accept-Language must be absent for EMPTY ctx");
        assertFalse(headers.containsKey("X-Forwarded-For"), "X-Forwarded-For must be absent for EMPTY ctx");
        assertTrue(headers.containsKey("Content-Type"), "Content-Type must always be present");
    }

    @Test
    void buildHint_withPartialContext_onlyPresent() {
        var ctx = new ClientContext("Mozilla/5.0", null, "");

        ExchangeHint hint = DiscoveryClientImpl.buildHint(ctx);

        var headers = hint.getRequestHeaders();
        assertTrue(headers.containsKey("User-Agent"), "User-Agent should be present");
        assertFalse(headers.containsKey("Accept-Language"), "null Accept-Language must be absent");
        assertFalse(headers.containsKey("X-Forwarded-For"), "blank X-Forwarded-For must be absent");
    }

    @Test
    void buildV2Hint_withClientContext_includesAuthKeyAndClientHeaders() {
        var ctx = new ClientContext("TestBrowser/1.0", "de-DE", "10.0.0.1");

        ExchangeHint hint = DiscoveryClientImpl.buildV2Hint(v2Config, ctx);

        var headers = hint.getRequestHeaders();
        assertTrue(headers.containsKey("auth-key"), "auth-key header required for v2");
        assertEquals("my-auth-key", headers.get("auth-key").get(0));
        assertTrue(headers.containsKey("User-Agent"));
        assertEquals("TestBrowser/1.0", headers.get("User-Agent").get(0));
        assertTrue(headers.containsKey("X-Forwarded-For"));
        assertEquals("10.0.0.1", headers.get("X-Forwarded-For").get(0));
    }

    @Test
    void firePixelEvent_usesEuResourceSpace_whenRegionIsEU() throws Exception {
        client.firePixelEvent("/pix.gif?type=pageview", config, ClientContext.EMPTY,
                new PixelFlags(true, false, false, "EU"));
        verify(broker).resolve(eq("discoveryPixelAPIEU"), anyString(), any(ExchangeHint.class));
        verify(broker, never()).resolve(eq("discoveryPixelAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void firePixelEvent_usesUsResourceSpace_byDefault() throws Exception {
        client.firePixelEvent("/pix.gif?type=pageview", config, ClientContext.EMPTY,
                new PixelFlags(true, false, false, "US"));
        verify(broker).resolve(eq("discoveryPixelAPI"), anyString(), any(ExchangeHint.class));
        verify(broker, never()).resolve(eq("discoveryPixelAPIEU"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void buildHint_nullContext_noClientHeaders() {
        ExchangeHint hint = DiscoveryClientImpl.buildHint(null);

        var headers = hint.getRequestHeaders();
        assertFalse(headers.containsKey("User-Agent"));
        assertTrue(headers.containsKey("Content-Type"));
    }

    // --- staging routing ---

    @Test
    void search_stagingConfig_usesSearchStagingResourceSpace() throws ResourceException {
        var stagingConfig = new DiscoveryConfig(
                "acct123", "myDomain", "secret-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "STAGING", 10, "price asc");
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(broker.resolve(eq("discoverySearchAPIStaging"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResponse(resource, 0, 10)).thenReturn(expectedResponse);

        client.search(query, stagingConfig, ClientContext.EMPTY);

        verify(broker).resolve(eq("discoverySearchAPIStaging"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommendV2_stagingConfig_usesPathwaysStagingResourceSpace() throws ResourceException {
        var stagingV2Config = new DiscoveryConfig(
                "acct123", "myDomain", "secret-key", "my-auth-key",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "STAGING", 10, "price asc");
        var query = new RecQuery("w-1", null, null, 8);
        var expectedResult = RecommendationResult.of(List.of());
        when(broker.resolve(eq("discoveryPathwaysAPIStaging"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expectedResult);

        client.recommend(query, stagingV2Config, ClientContext.EMPTY);

        verify(broker).resolve(eq("discoveryPathwaysAPIStaging"), anyString(), any(ExchangeHint.class));
    }
}
