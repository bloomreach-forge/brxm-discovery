package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.exception.RecommendationException;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
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
class RecommendationApiClientTest {

    private static final DiscoverySettings TEST_SETTINGS = new DiscoverySettings(
            "https://core.dxpapi.com", "https://pathways.dxpapi.com",
            "https://suggest.dxpapi.com", 12, "");

    @Mock DiscoveryTransport transport;
    @Mock RecommendationResponseMapper responseMapper;
    @Mock DiscoveryConfigProvider configProvider;

    private RecommendationApiClientImpl client;
    private DiscoveryCredentials credentials;
    private DiscoveryCredentials v2Credentials;

    @BeforeEach
    void setUp() {
        lenient().when(configProvider.settings()).thenReturn(TEST_SETTINGS);
        client = new RecommendationApiClientImpl(transport, configProvider, responseMapper, new DiscoveryRequestFactory());
        credentials = new DiscoveryCredentials("acct123", "myDomain", null, null, "PRODUCTION");
        v2Credentials = new DiscoveryCredentials("acct123", "myDomain", null, "secret-key", "PRODUCTION");
    }

    @Test
    void recommend_noAuthKey_usesSearchBaseUri() {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expected = RecommendationResult.of(List.of());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toRecommendationResult(anyString())).thenReturn(expected);

        RecommendationResult result = client.recommend(query, credentials, ClientContext.EMPTY);

        assertSame(expected, result);
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://core.dxpapi.com"),
                "v1 must use core base URI");
    }

    @Test
    void recommend_withAuthKey_usesPathwaysBaseUri() {
        var query = new RecQuery("item", "widget-1", "prod-42", "pdp", 6, null, null, null, null, null);
        var expected = RecommendationResult.of(List.of());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toRecommendationResult(anyString())).thenReturn(expected);

        RecommendationResult result = client.recommend(query, v2Credentials, ClientContext.EMPTY);

        assertSame(expected, result);
        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().startsWith("https://pathways.dxpapi.com"),
                "v2 must use pathways base URI");
    }

    @Test
    void recommend_withAuthKey_neverUsesCoreBaseUri() {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toRecommendationResult(anyString())).thenReturn(RecommendationResult.of(List.of()));

        client.recommend(query, v2Credentials, ClientContext.EMPTY);

        verify(transport, never()).execute(argThat(req ->
                req.uri().toString().startsWith("https://core.dxpapi.com")));
    }

    @Test
    void recommend_v1_searchException_wrapsInRecommendationException() {
        var query = new RecQuery("widget-1", null, null, 8);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, credentials, ClientContext.EMPTY));
    }

    @Test
    void recommend_v2_searchException_wrapsInRecommendationException() {
        var query = new RecQuery("item", "widget-1", null, null, 8, null, null, null, null, null);
        when(transport.execute(any())).thenThrow(new SearchException("transport failure"));

        assertThrows(RecommendationException.class, () -> client.recommend(query, v2Credentials, ClientContext.EMPTY));
    }

    @Test
    void recommend_returnsRecommendationResult() {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expected = new RecommendationResult("rid-1", List.of());
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toRecommendationResult(anyString())).thenReturn(expected);

        RecommendationResult result = client.recommend(query, credentials, ClientContext.EMPTY);

        assertNotNull(result);
        assertEquals("rid-1", result.widgetResultId());
    }

    @Test
    void recommend_v1_pathContainsRequestId() {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        when(transport.execute(any())).thenReturn("{}");
        when(responseMapper.toRecommendationResult(anyString())).thenReturn(RecommendationResult.of(List.of()));

        client.recommend(query, credentials, ClientContext.EMPTY);

        var captor = ArgumentCaptor.forClass(DiscoveryTransportRequest.class);
        verify(transport).execute(captor.capture());
        assertTrue(captor.getValue().uri().toString().contains("request_id="));
    }
}
