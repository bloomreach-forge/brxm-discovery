package org.bloomreach.forge.discovery.rest.transport;

import org.bloomreach.forge.discovery.exception.SearchException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DiscoveryMultipartHttpClientImplTest {

    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static InputStream streamOf(int size) {
        return stream(new byte[size]);
    }

    @Test
    void upload_returnsResponseBody_onSuccess() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"response\":{\"image_id\":\"abc123\"}}");
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(httpClient);
        String result = client.upload(
                "https://pathways.dxpapi.com/api/v2/widgets/visual/upload/wid1",
                streamOf(100), "image/jpeg");

        assertEquals("{\"response\":{\"image_id\":\"abc123\"}}", result);
    }

    @Test
    void upload_throwsSearchException_onNon200() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn("bad request");
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(httpClient);
        assertThrows(SearchException.class, () ->
                client.upload("https://example.com/upload", streamOf(100), "image/png"));
    }

    @Test
    void upload_throwsSearchException_whenImageExceeds2MB() {
        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(mock(HttpClient.class));
        byte[] oversized = new byte[MAX_BYTES + 1];

        assertThrows(SearchException.class, () ->
                client.upload("https://example.com/upload", stream(oversized), "image/jpeg"));
    }

    @Test
    void upload_throwsSearchException_forNonImageContentType() {
        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(mock(HttpClient.class));

        assertThrows(SearchException.class, () ->
                client.upload("https://example.com/upload", streamOf(100), "application/pdf"));
    }

    @Test
    void get_returnsResponseBody_onSuccess() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"response\":{\"docs\":[]}}");
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(httpClient);
        assertEquals("{\"response\":{\"docs\":[]}}", client.get("https://example.com/search"));
    }

    @Test
    void get_throwsSearchException_onNon200() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("error");
        doReturn(response).when(httpClient).send(any(HttpRequest.class), any());

        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(httpClient);
        assertThrows(SearchException.class, () -> client.get("https://example.com/search"));
    }

    @Test
    void upload_throwsSearchException_onIOException() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doThrow(new java.io.IOException("network")).when(httpClient).send(any(HttpRequest.class), any());

        DiscoveryMultipartHttpClientImpl client = new DiscoveryMultipartHttpClientImpl(httpClient);
        assertThrows(SearchException.class, () ->
                client.upload("https://example.com/upload", streamOf(100), "image/jpeg"));
    }
}
