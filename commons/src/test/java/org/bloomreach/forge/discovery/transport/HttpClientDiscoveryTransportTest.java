package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.exception.SearchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class HttpClientDiscoveryTransportTest {

    @Mock HttpClient httpClient;
    @Mock HttpResponse<String> httpResponse;

    HttpClientDiscoveryTransport transport;
    DiscoveryTransportRequest request;

    @BeforeEach
    void setUp() {
        transport = new HttpClientDiscoveryTransport(httpClient);
        request = DiscoveryTransportRequest.of(URI.create("https://core.dxpapi.com/api/v1/core/"), Map.of());
    }

    @Test
    void execute_200_returnsBody() throws Exception {
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());
        doReturn(200).when(httpResponse).statusCode();
        doReturn("{\"response\":{\"numFound\":5}}").when(httpResponse).body();

        assertEquals("{\"response\":{\"numFound\":5}}", transport.execute(request));
    }

    @Test
    void execute_404_throwsSearchException() throws Exception {
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());
        doReturn(404).when(httpResponse).statusCode();
        doReturn("Not Found").when(httpResponse).body();

        SearchException ex = assertThrows(SearchException.class, () -> transport.execute(request));
        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    void execute_500_throwsSearchException() throws Exception {
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());
        doReturn(500).when(httpResponse).statusCode();
        doReturn("Internal Server Error").when(httpResponse).body();

        SearchException ex = assertThrows(SearchException.class, () -> transport.execute(request));
        assertTrue(ex.getMessage().contains("500"));
    }

    @Test
    void execute_ioException_wrapsInSearchException() throws Exception {
        doThrow(new IOException("connection refused")).when(httpClient).send(any(HttpRequest.class), any());

        SearchException ex = assertThrows(SearchException.class, () -> transport.execute(request));
        assertTrue(ex.getMessage().contains("I/O error"));
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void execute_interrupted_restoresFlag() throws Exception {
        doThrow(new InterruptedException("interrupted")).when(httpClient).send(any(HttpRequest.class), any());

        assertThrows(SearchException.class, () -> transport.execute(request));
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted(); // clear for subsequent tests
    }

    @Test
    void execute_longErrorBody_isTruncated() throws Exception {
        String longBody = "x".repeat(500);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());
        doReturn(503).when(httpResponse).statusCode();
        doReturn(longBody).when(httpResponse).body();

        SearchException ex = assertThrows(SearchException.class, () -> transport.execute(request));
        assertTrue(ex.getMessage().contains("..."));
    }
}
