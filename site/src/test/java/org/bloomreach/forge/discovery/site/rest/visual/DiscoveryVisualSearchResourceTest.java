package org.bloomreach.forge.discovery.site.rest.visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.activation.DataHandler;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.rest.transport.DiscoveryMultipartHttpClient;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualSearchResult;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualSearchUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryVisualSearchResourceTest {

    @Mock private DiscoveryConfigProvider configProvider;
    @Mock private DiscoveryMultipartHttpClient httpClient;

    private DiscoveryVisualSearchResource resource;

    private static final DiscoveryConfig CONFIG = new DiscoveryConfig(
            "acct123", "domain123", null, "auth-key",
            "https://core.dxpapi.com", "https://pathways.dxpapi.com", null,
            "PRODUCTION", 12, null);

    private static final String UPLOAD_RESPONSE_JSON =
            "{\"response\":{\"image_id\":\"img-abc\",\"objects\":[{\"id\":1,\"bbox\":[0.1,0.2,0.3,0.4],\"object_type\":\"shoes\"}]}}";

    private static final String SEARCH_RESPONSE_JSON =
            "{\"response\":{\"numFound\":1,\"docs\":[{\"pid\":\"p1\",\"title\":\"Red Shoes\",\"url\":\"/shoes/1\",\"thumb_image\":\"/img/1.jpg\",\"price\":49.99}]}}";

    @BeforeEach
    void setUp() {
        resource = new DiscoveryVisualSearchResource(configProvider, httpClient, new ObjectMapper());
        lenient().when(configProvider.get()).thenReturn(CONFIG);
    }

    @Test
    void upload_returnsUploadResponse_withProductsAndObjects() throws Exception {
        when(httpClient.upload(anyString(), any(), anyString())).thenReturn(UPLOAD_RESPONSE_JSON);

        Attachment attachment = mockAttachment("image/jpeg");

        try (Response response = resource.upload("widget1", attachment)) {
            assertEquals(200, response.getStatus());
            VisualSearchUploadResponse body = (VisualSearchUploadResponse) response.getEntity();
            assertEquals("img-abc", body.imageId());
            assertEquals(1, body.objects().size());
            assertEquals("shoes", body.objects().get(0).objectType());
            assertEquals(0, body.products().size());
        }
    }

    @Test
    void upload_returns400_forInvalidWidgetId() {
        try (Response response = resource.upload("widget/bad!", null)) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void search_returnsProducts_forValidImageId() throws Exception {
        when(httpClient.get(anyString())).thenReturn(SEARCH_RESPONSE_JSON);

        try (Response response = resource.search("widget1", "img-abc", null, null, null, null, null)) {
            assertEquals(200, response.getStatus());
            VisualSearchResult body = (VisualSearchResult) response.getEntity();
            assertEquals(1, body.products().size());
            assertEquals("Red Shoes", body.products().get(0).title());
        }
    }

    @Test
    void search_returns400_whenImageIdMissing() {
        try (Response response = resource.search("widget1", null, null, null, null, null, null)) {
            assertEquals(400, response.getStatus());
        }
    }

    @Test
    void search_returns400_forInvalidWidgetId() {
        try (Response response = resource.search("../etc/passwd", "img-abc", null, null, null, null, null)) {
            assertEquals(400, response.getStatus());
        }
    }

    private static Attachment mockAttachment(String contentType) throws Exception {
        Attachment attachment = mock(Attachment.class);
        DataHandler dh = mock(DataHandler.class);
        when(attachment.getDataHandler()).thenReturn(dh);
        when(dh.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[100]));
        when(attachment.getContentType()).thenReturn(MediaType.valueOf(contentType));
        return attachment;
    }
}
