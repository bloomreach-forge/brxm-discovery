package org.bloomreach.forge.discovery.rest.transport;

import org.bloomreach.forge.discovery.exception.SearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public class DiscoveryMultipartHttpClientImpl implements DiscoveryMultipartHttpClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryMultipartHttpClientImpl.class);
    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final HttpClient httpClient;

    public DiscoveryMultipartHttpClientImpl() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    /** Package-private seam for tests to inject a mock HttpClient. */
    DiscoveryMultipartHttpClientImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String upload(String url, InputStream imageStream, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new SearchException("Visual search upload rejected: content type must be image/*, got: " + contentType);
        }

        byte[] imageBytes;
        try {
            imageBytes = imageStream.readNBytes(MAX_IMAGE_BYTES + 1);
        } catch (IOException e) {
            throw new SearchException("Visual search upload failed: could not read image stream", e);
        }
        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new SearchException("Visual search upload rejected: image exceeds 2 MB limit");
        }

        String boundary = UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, imageBytes, contentType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Visual search upload returned {}: {}", response.statusCode(), response.body());
                throw new SearchException("Visual search upload failed with HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new SearchException("Visual search upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Visual search GET returned {}: {}", response.statusCode(), response.body());
                throw new SearchException("Visual search GET failed with HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new SearchException("Visual search GET failed: " + e.getMessage(), e);
        }
    }

    private static byte[] buildMultipartBody(String boundary, byte[] imageBytes, String contentType) {
        String prologue = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\"image\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String epilogue = "\r\n--" + boundary + "--\r\n";

        byte[] prologueBytes = prologue.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] epilogueBytes = epilogue.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] body = new byte[prologueBytes.length + imageBytes.length + epilogueBytes.length];
        System.arraycopy(prologueBytes, 0, body, 0, prologueBytes.length);
        System.arraycopy(imageBytes, 0, body, prologueBytes.length, imageBytes.length);
        System.arraycopy(epilogueBytes, 0, body, prologueBytes.length + imageBytes.length, epilogueBytes.length);
        return body;
    }
}
