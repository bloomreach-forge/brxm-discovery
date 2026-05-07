package org.bloomreach.forge.discovery.site.service.discovery.autosuggest;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.bloomreach.forge.discovery.transport.DiscoveryTransportRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutosuggestApiClientTest {

    private static final DiscoverySettings TEST_SETTINGS = new DiscoverySettings(
            "https://core.dxpapi.com", "https://pathways.dxpapi.com",
            "https://suggest.dxpapi.com", 12, "");

    @Mock DiscoveryTransport transport;
    @Mock AutosuggestResponseMapper responseMapper;
    @Mock DiscoveryConfigProvider configProvider;

    private AutosuggestApiClientImpl client;
    private DiscoveryCredentials credentials;

    @BeforeEach
    void setUp() {
        lenient().when(configProvider.settings()).thenReturn(TEST_SETTINGS);
        client = new AutosuggestApiClientImpl(transport, configProvider, responseMapper, new DiscoveryRequestFactory());
        credentials = new DiscoveryCredentials("acct123", "myDomain", "secret-key", null, "PRODUCTION");
    }

    @Test
    void autosuggest_usesAutosuggestBaseUri() {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of("shirts"), List.of(), List.of());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toAutosuggestResult(anyString())).thenReturn(expected);

        AutosuggestResult result = client.autosuggest(query, credentials, ClientContext.EMPTY);

        assertSame(expected, result);
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://suggest.dxpapi.com"),
                "autosuggest must use suggest base URI");
    }

    @Test
    void autosuggest_pathContainsRequiredParams() {
        var query = new AutosuggestQuery("shi", 8);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toAutosuggestResult(anyString()))
                .thenReturn(new AutosuggestResult("shi", List.of(), List.of(), List.of()));

        client.autosuggest(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        String uri = captor.getValue().uri().toString();
        assertTrue(uri.contains("account_id=acct123"));
        assertTrue(uri.contains("domain_key=myDomain"));
        assertTrue(uri.contains("request_type=suggest"));
        assertTrue(uri.contains("q=shi"));
        assertTrue(uri.contains("catalog_views=myDomain"));
    }

    @Test
    void autosuggest_withCatalogViews_includesCatalogViewsParam() {
        var query = new AutosuggestQuery("shi", 8, "store:products_en", null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toAutosuggestResult(anyString()))
                .thenReturn(new AutosuggestResult("shi", List.of(), List.of(), List.of()));

        client.autosuggest(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("catalog_views=store:products_en"));
    }

    @Test
    void autosuggest_searchException_propagates() {
        var query = new AutosuggestQuery("shi", 8);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(SearchException.class, () -> client.autosuggest(query, credentials, ClientContext.EMPTY));
    }

    @Test
    void autosuggest_withApiKey_includesAuthKeyQueryParam() {
        var query = new AutosuggestQuery("shi", 8);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toAutosuggestResult(anyString()))
                .thenReturn(new AutosuggestResult("shi", List.of(), List.of(), List.of()));

        client.autosuggest(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("auth_key=secret-key"));
    }
}
