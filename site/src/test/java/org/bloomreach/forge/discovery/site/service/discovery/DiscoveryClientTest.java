package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.exception.RecommendationException;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
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
        var expected = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResult(resource, 0, 10)).thenReturn(expected);

        SearchResult result = client.search(query, config);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void search_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.search(query, config));
    }

    @Test
    void search_withCatalogName_includesCatalogNameParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, "blog_en");
        var expected = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(broker.resolve(eq("discoverySearchAPI"), contains("catalog_name=blog_en"), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toSearchResult(resource, 0, 10)).thenReturn(expected);

        SearchResult result = client.search(query, config);

        assertSame(expected, result);
    }

    @Test
    void search_noCatalogName_omitsCatalogNameParam() throws ResourceException {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expected = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(broker.resolve(eq("discoverySearchAPI"), argThat(path -> !path.contains("catalog_name")),
                any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResult(resource, 0, 10)).thenReturn(expected);

        SearchResult result = client.search(query, config);

        assertSame(expected, result);
    }

    // --- category ---

    @Test
    void category_usesSearchResourceSpace() throws ResourceException {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        var expected = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toSearchResult(resource, 0, 10)).thenReturn(expected);

        SearchResult result = client.category(query, config);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void category_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        when(broker.resolve(anyString(), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.category(query, config));
    }

    // --- recommend v1 ---

    @Test
    void recommend_noAuthKey_usesSearchResourceSpace() throws ResourceException {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expected = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null));
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expected);

        List<ProductSummary> result = client.recommend(query, config);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_v1_resourceException_wrapsInRecommendationException() throws ResourceException {
        var query = new RecQuery("widget-1", null, null, 8);
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, config));
    }

    // --- recommend v2 ---

    @Test
    void recommend_withAuthKey_usesPathwaysResourceSpace() throws ResourceException {
        var query = new RecQuery("item", "widget-1", "prod-42", "pdp", 6, null, null, null, null, null);
        var expected = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null));
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(expected);

        List<ProductSummary> result = client.recommend(query, v2Config);

        assertSame(expected, result);
        verify(broker).resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_withAuthKey_neverUsesSearchResourceSpace() throws ResourceException {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toRecommendationResult(resource)).thenReturn(List.of());

        client.recommend(query, v2Config);

        verify(broker, never()).resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class));
    }

    @Test
    void recommend_v2_resourceException_wrapsInRecommendationException() throws ResourceException {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(broker.resolve(eq("discoveryPathwaysAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, v2Config));
    }

    // --- pixel path builders ---

    @Test
    void buildSearchPixelPath_containsRequiredParams() {
        var query = new SearchQuery("shoes", 0, 10, null, null, "uid-val", "http://ref.com", "http://page.com");
        var result = new SearchResult(
                List.of(new ProductSummary("p1", null, null, null, null, null, null),
                        new ProductSummary("p2", null, null, null, null, null, null)),
                2L, 0, 10, Map.of());

        String path = client.buildSearchPixelPath(query, result, config);

        assertTrue(path.contains("type=SearchResponse"), "should contain type=SearchResponse");
        assertTrue(path.contains("ptype=search"), "should contain ptype=search");
        assertTrue(path.contains("q=shoes"), "should contain query term");
        assertTrue(path.contains("account_id=acct123"), "should contain account_id");
        assertTrue(path.contains("domain_key=myDomain"), "should contain domain_key");
        assertTrue(path.contains("sku=p1,p2"), "should contain comma-separated PIDs");
    }

    @Test
    void buildCategoryPixelPath_containsCategoryParams() {
        var query = new CategoryQuery("cat-99", 0, 10, null, null, "uid-val", null, "http://page.com");
        var result = new SearchResult(
                List.of(new ProductSummary("pid1", null, null, null, null, null, null)),
                1L, 0, 10, Map.of());

        String path = client.buildCategoryPixelPath(query, result, config);

        assertTrue(path.contains("type=CategoryView"), "should contain type=CategoryView");
        assertTrue(path.contains("ptype=category"), "should contain ptype=category");
        assertTrue(path.contains("cat_id=cat-99"), "should contain cat_id");
        assertTrue(path.contains("sku=pid1"), "should contain sku");
    }

    @Test
    void buildWidgetPixelPath_containsWidgetParams() {
        var query = new RecQuery("item", "w-123", null, null, 8, null, null, null, null, null);
        var products = List.of(new ProductSummary("prod-1", null, null, null, null, null, null));

        String path = client.buildWidgetPixelPath(query, products, config);

        assertTrue(path.contains("type=Widget"), "should contain type=Widget");
        assertTrue(path.contains("wrid=w-123"), "should contain wrid");
        assertTrue(path.contains("wrt=item"), "should contain wrt");
        assertTrue(path.contains("sku=prod-1"), "should contain sku");
    }

    @Test
    void buildSearchPixelPath_limitsSkuToFirst20() {
        var query = new SearchQuery("q", 0, 25, null, null, null, null, null);
        List<ProductSummary> manyProducts = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(i -> new ProductSummary("p" + i, null, null, null, null, null, null))
                .toList();
        var result = new SearchResult(manyProducts, 25L, 0, 25, Map.of());

        String path = client.buildSearchPixelPath(query, result, config);

        long skuCount = java.util.Arrays.stream(
                path.substring(path.indexOf("sku=") + 4).split(",")).count();
        assertEquals(20, skuCount, "pixel path should include at most 20 SKUs");
    }

    @Test
    void firePixelEvent_resourceException_doesNotThrow() throws Exception {
        when(broker.resolve(eq("discoverySearchAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertDoesNotThrow(() -> client.firePixelEvent("/api/v1/pixel/?type=SearchResponse", config));
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

        AutosuggestResult result = client.autosuggest(query, config);

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
        ), any(ExchangeHint.class))).thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config);

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

        AutosuggestResult result = client.autosuggest(query, config);

        assertSame(expected, result);
    }

    @Test
    void autosuggest_resourceException_wrapsInSearchException() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        when(broker.resolve(eq("discoveryAutosuggestAPI"), anyString(), any(ExchangeHint.class)))
                .thenThrow(new ResourceException("CRISP failure"));

        assertThrows(SearchException.class, () -> client.autosuggest(query, config));
    }

    @Test
    void autosuggest_withApiKey_includesAuthKeyQueryParam() throws ResourceException {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(broker.resolve(eq("discoveryAutosuggestAPI"),
                contains("auth_key=secret-key"), any(ExchangeHint.class)))
                .thenReturn(resource);
        when(responseMapper.toAutosuggestResult(resource)).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, config);

        assertSame(expected, result);
    }
}
