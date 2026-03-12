package org.bloomreach.forge.discovery.cms.rest;

import jakarta.ws.rs.InternalServerErrorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryHttpGatewayTest {

    @Test
    void apply_returnsResponseBodyForHttp200() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = response(200, "{\"ok\":true}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        String body = new DiscoveryHttpGateway(client).apply("https://example.com/api");

        assertEquals("{\"ok\":true}", body);
    }

    @Test
    void apply_non200ThrowsServerError() throws Exception {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> response = response(503, "down");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class,
                () -> new DiscoveryHttpGateway(client).apply("https://example.com/api"));

        assertEquals("Discovery API returned HTTP 503", exception.getMessage());
    }

    @Test
    void apply_ioFailureWrapsAsServerError() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("socket closed"));

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class,
                () -> new DiscoveryHttpGateway(client).apply("https://example.com/api"));

        assertEquals("Discovery API request failed: socket closed", exception.getMessage());
    }

    @Test
    void apply_interruptionRestoresInterruptFlag() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("cancelled"));

        try {
            assertThrows(InternalServerErrorException.class,
                    () -> new DiscoveryHttpGateway(client).apply("https://example.com/api"));
            org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private static HttpResponse<String> response(int statusCode, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(java.util.Map.of(), (a, b) -> true));
        when(response.previousResponse()).thenReturn(Optional.empty());
        return response;
    }
}
