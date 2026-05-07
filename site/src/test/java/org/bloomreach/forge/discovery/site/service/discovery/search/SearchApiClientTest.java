package org.bloomreach.forge.discovery.site.service.discovery.search;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.bloomreach.forge.discovery.transport.DiscoveryTransportRequest;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchApiClientTest {

    private static final DiscoverySettings TEST_SETTINGS = new DiscoverySettings(
            "https://core.dxpapi.com", "https://pathways.dxpapi.com",
            "https://suggest.dxpapi.com", 12, "");

    @Mock DiscoveryTransport transport;
    @Mock SearchResponseMapper responseMapper;
    @Mock DiscoveryConfigProvider configProvider;

    private SearchApiClientImpl client;
    private DiscoveryCredentials credentials;
    private DiscoveryCredentials v2Credentials;

    @BeforeEach
    void setUp() {
        lenient().when(configProvider.settings()).thenReturn(TEST_SETTINGS);
        client = new SearchApiClientImpl(transport, configProvider, responseMapper, new DiscoveryRequestFactory());
        credentials = new DiscoveryCredentials("acct123", "myDomain", "secret-key", null, "PRODUCTION");
        v2Credentials = new DiscoveryCredentials("acct123", "myDomain", "secret-key", "my-auth-key", "PRODUCTION");
    }

    // --- search ---

    @Test
    void search_usesSearchBaseUri() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expectedResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        var expectedResponse = new SearchResponse(expectedResult, SearchMetadata.empty());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10))).thenReturn(expectedResponse);

        SearchResponse response = client.search(query, credentials, ClientContext.EMPTY);

        assertSame(expectedResult, response.result());
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://core.dxpapi.com"),
                "search must use core base URI");
    }

    @Test
    void search_searchException_propagates() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(SearchException.class, () -> client.search(query, credentials, ClientContext.EMPTY));
    }

    @Test
    void search_withCatalogName_includesCatalogNameParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, "blog_en");
        var expectedResponse = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10))).thenReturn(expectedResponse);

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("catalog_name=blog_en"));
    }

    @Test
    void search_pathContainsRequestId() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("request_id="));
    }

    @Test
    void search_noCatalogName_omitsCatalogNameParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertFalse(captor.getValue().uri().toString().contains("catalog_name"));
    }

    @Test
    void search_withStatsFields_appendsStatsFieldParams() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null,
                List.of("price", "sale_price"));
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        String uri = captor.getValue().uri().toString();
        assertTrue(uri.contains("stats.field=price"));
        assertTrue(uri.contains("stats.field=sale_price"));
    }

    @Test
    void search_withoutStatsFields_noStatsFieldParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertFalse(captor.getValue().uri().toString().contains("stats.field"));
    }

    @Test
    void search_withSegment_appendsSegmentParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null, List.of(), "NorthAmerica", null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("segment=NorthAmerica"));
    }

    @Test
    void search_withoutSegment_noSegmentParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertFalse(captor.getValue().uri().toString().contains("segment="));
    }

    @Test
    void search_withEfq_appendsEfqParam() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null, null, List.of(), null, "price:[10 TO *]");
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResponse(anyString(), eq(0), eq(10)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.search(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("efq="));
    }

    // --- category ---

    @Test
    void category_usesSearchBaseUri() {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        var expectedResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        var expectedResponse = new SearchResponse(expectedResult, SearchMetadata.empty());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toBrowseResponse(anyString(), eq(0), eq(10), eq("shoes-cat"))).thenReturn(expectedResponse);

        SearchResponse response = client.category(query, credentials, ClientContext.EMPTY);

        assertSame(expectedResult, response.result());
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://core.dxpapi.com"),
                "category must use core base URI");
    }

    @Test
    void category_pathContainsRequestId() {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toBrowseResponse(anyString(), eq(0), eq(10), eq("shoes-cat")))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty()));

        client.category(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("request_id="));
    }

    @Test
    void category_searchException_propagates() {
        var query = new CategoryQuery("shoes-cat", 0, 10, null, null, null, null, null);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(SearchException.class, () -> client.category(query, credentials, ClientContext.EMPTY));
    }

    @Test
    void category_withStatsFields_appendsStatsFieldParams() {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null, List.of("price"));
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toBrowseResponse(anyString(), eq(0), eq(12), eq("sale")))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty()));

        client.category(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("stats.field=price"));
    }

    @Test
    void category_withSegment_appendsSegmentParam() {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null, List.of(), "SouthAmerica", null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toBrowseResponse(anyString(), eq(0), eq(12), eq("sale")))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty()));

        client.category(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("segment=SouthAmerica"));
    }

    @Test
    void category_withEfq_appendsEfqParam() {
        var query = new CategoryQuery("sale", 0, 12, null, null, null, null, null, List.of(), null, "out_of_stock:false");
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toBrowseResponse(anyString(), eq(0), eq(12), eq("sale")))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 0L, 0, 12, Map.of()), SearchMetadata.empty()));

        client.category(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("efq="));
    }

    // --- fetchProduct ---

    @Test
    void fetchProduct_pathContainsRequestId() {
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResult(anyString(), eq(0), eq(1)))
                .thenReturn(new SearchResult(List.of(), 0L, 0, 1, Map.of()));

        client.fetchProduct("pid-1", null, "pid,title", credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("request_id="));
    }

    @Test
    void fetchProduct_pathContainsFlParam() {
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResult(anyString(), eq(0), eq(1)))
                .thenReturn(new SearchResult(List.of(), 0L, 0, 1, Map.of()));

        client.fetchProduct("pid-1", null, "pid,title,thumb_image", credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("fl=pid"));
    }

    @Test
    void fetchProduct_notFound_returnsEmpty() {
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toSearchResult(anyString(), eq(0), eq(1)))
                .thenReturn(new SearchResult(List.of(), 0L, 0, 1, Map.of()));

        Optional<ProductSummary> result = client.fetchProduct("pid-1", null, "pid", credentials, ClientContext.EMPTY);

        assertTrue(result.isEmpty());
    }

    // --- visualSearch ---

    @Test
    void visualSearch_usesPathwaysBaseUri() {
        var query = new VisualSearchQuery("wid1", "img-abc", null, 10, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toVisualSearchProducts(anyString()))
                .thenReturn(List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null, List.of())));

        List<ProductSummary> products = client.visualSearch(query, v2Credentials, ClientContext.EMPTY);

        assertEquals(1, products.size());
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://pathways.dxpapi.com"),
                "visual search must use pathways base URI");
    }

    @Test
    void visualSearch_pathContainsWidgetIdAndImageId() {
        var query = new VisualSearchQuery("wid1", "img-abc", null, 10, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toVisualSearchProducts(anyString())).thenReturn(List.of());

        client.visualSearch(query, v2Credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        String uri = captor.getValue().uri().toString();
        assertTrue(uri.contains("/visual/search/wid1"));
        assertTrue(uri.contains("image_id=img-abc"));
    }

    @Test
    void visualSearch_searchException_propagates() {
        var query = new VisualSearchQuery("wid1", "img-abc", null, 10, null, null, null, null);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(SearchException.class,
                () -> client.visualSearch(query, v2Credentials, ClientContext.EMPTY));
    }
}
