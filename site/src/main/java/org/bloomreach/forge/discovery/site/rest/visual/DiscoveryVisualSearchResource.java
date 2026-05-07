package org.bloomreach.forge.discovery.site.rest.visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.bloomreach.forge.discovery.config.DiscoveryChannelConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.rest.AbstractDiscoveryResource;
import org.bloomreach.forge.discovery.rest.mapper.ApiError;
import org.bloomreach.forge.discovery.rest.transport.DiscoveryMultipartHttpClient;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualObject;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualSearchApiResponse;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualSearchResult;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualSearchUploadResponse;
import org.bloomreach.forge.discovery.site.rest.visual.dto.VisualUploadApiResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDoc;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Delivery-tier JAX-RS resource for the Discovery visual search feature.
 *
 * <p>Proxies two Discovery Pathways endpoints, adding server-side credentials so
 * the frontend never handles Discovery API keys:
 * <ul>
 *   <li>{@code POST /{widgetId}/upload} - uploads an image and returns the image ID,
 *       detected objects, and initial product results.</li>
 *   <li>{@code GET  /{widgetId}/search} - fetches product results for a previously
 *       uploaded image, optionally refined by object ID.</li>
 * </ul>
 *
 * <p>Mounted at {@code /_brxdis-api/visual-search} via {@code BrxdisVisualSearchPipeline}.
 */
@Path("/visual-search")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryVisualSearchResource extends AbstractDiscoveryResource {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryVisualSearchResource.class);

    private static final Pattern WIDGET_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String UPLOAD_PATH = "/api/v2/widgets/visual/upload/";
    private static final String SEARCH_PATH = "/api/v2/widgets/visual/search/";

    private final DiscoveryMultipartHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DiscoveryVisualSearchResource(DiscoveryConfigProvider configProvider,
                                         DiscoveryMultipartHttpClient httpClient,
                                         ObjectMapper objectMapper) {
        super(configProvider);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Uploads an image to Discovery and returns combined upload + search results.
     */
    @POST
    @Path("/{widgetId}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(
            @PathParam("widgetId") String widgetId,
            @Multipart("image") Attachment image) {

        if (!isValidWidgetId(widgetId)) {
            return badRequest("Invalid widgetId");
        }

        DiscoveryConfig config = resolveConfigForRequest();
        DiscoveryCredentials creds = requireCredentials(config);
        String brUid2 = brUid2(servletRequest);

        InputStream imageStream;
        String contentType;
        try {
            imageStream = image.getDataHandler().getInputStream();
            contentType = image.getContentType().toString();
        } catch (IOException e) {
            throw new SearchException("Failed to read uploaded image", e);
        }

        String uploadUrl = buildUrl(config.pathwaysBaseUri(), UPLOAD_PATH + widgetId,
                creds, brUid2, null, null, null, null);
        String uploadJson = httpClient.upload(uploadUrl, imageStream, contentType);

        VisualUploadApiResponse uploadResponse = parseJson(uploadJson, VisualUploadApiResponse.class);
        String imageId = uploadResponse.response() != null ? uploadResponse.response().imageId() : null;
        List<VisualObject> objects = parseObjects(uploadResponse);

        return Response.ok(new VisualSearchUploadResponse(imageId, objects, Collections.emptyList())).build();
    }

    /**
     * Returns products for a previously uploaded image.
     */
    @GET
    @Path("/{widgetId}/search")
    public Response search(
            @PathParam("widgetId") String widgetId,
            @QueryParam("imageId") String imageId,
            @QueryParam("objectId") String objectId,
            @QueryParam("rows") Integer rows,
            @QueryParam("fields") String fields,
            @QueryParam("refUrl") String refUrl,
            @QueryParam("url") String url) {

        if (!isValidWidgetId(widgetId)) {
            return badRequest("Invalid widgetId");
        }
        if (imageId == null || imageId.isBlank()) {
            return badRequest("imageId is required");
        }

        DiscoveryConfig config = resolveConfigForRequest();
        DiscoveryCredentials creds = requireCredentials(config);
        String brUid2 = brUid2(servletRequest);

        // The browser fetch() sends the results page URL as Referer; use it when url/refUrl are absent
        String referer = servletRequest != null ? servletRequest.getHeader("Referer") : null;
        String effectiveUrl    = (url    != null && !url.isBlank())    ? url    : referer;
        String effectiveRefUrl = (refUrl != null && !refUrl.isBlank()) ? refUrl : referer;

        String searchUrl = buildUrl(config.pathwaysBaseUri(), SEARCH_PATH + widgetId,
                creds, brUid2, effectiveRefUrl, effectiveUrl, imageId, objectId);
        String searchJson = httpClient.get(searchUrl);
        List<ProductSummary> products = parseProducts(searchJson);

        return Response.ok(new VisualSearchResult(products)).build();
    }

    private DiscoveryConfig resolveConfigForRequest() {
        DiscoveryConfig config = resolveConfig();
        if (servletRequest == null) {
            return config;
        }
        HstRequestContext ctx = HstRequestUtils.getHstRequestContext(servletRequest);
        if (ctx == null || ctx.getResolvedMount() == null) {
            return config;
        }
        try {
            Mount mount = ctx.getResolvedMount().getMount();
            DiscoveryChannelInfo channelInfo = null;
            while (mount != null && channelInfo == null) {
                ChannelInfo raw = mount.getChannelInfo();
                if (raw instanceof DiscoveryChannelInfo dci) {
                    channelInfo = dci;
                }
                mount = mount.getParent();
            }
            if (channelInfo == null) {
                return config;
            }
            DiscoveryCredentials overrides = DiscoveryChannelConfigReader.resolveOverrides(
                    channelInfo.getDiscoveryAccountId(),
                    channelInfo.getDiscoveryDomainKey(),
                    channelInfo.getDiscoveryApiKeyEnvVar(),
                    channelInfo.getDiscoveryAuthKeyEnvVar());
            return overrides != null ? config.withCredentials(overrides) : config;
        } catch (Exception e) {
            log.debug("Cannot apply channel credential overrides for visual search: {}", e.getMessage());
            return config;
        }
    }

    private static boolean isValidWidgetId(String widgetId) {
        return widgetId != null && WIDGET_ID_PATTERN.matcher(widgetId).matches();
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("BAD_REQUEST", message))
                .build();
    }

    private static String buildUrl(String baseUri, String path,
                                   DiscoveryCredentials creds,
                                   String brUid2, String refUrl, String url,
                                   String imageId, String objectId) {
        StringBuilder sb = new StringBuilder(baseUri).append(path).append('?');
        sb.append("account_id=").append(encode(creds.accountId()));
        sb.append("&domain_key=").append(encode(creds.domainKey()));
        if (creds.authKey() != null && !creds.authKey().isBlank()) {
            sb.append("&auth_key=").append(encode(creds.authKey()));
        }
        if (brUid2 != null && !brUid2.isBlank()) {
            sb.append("&_br_uid_2=").append(encode(brUid2));
        }
        if (refUrl != null && !refUrl.isBlank()) {
            sb.append("&ref_url=").append(encode(refUrl));
        }
        if (url != null && !url.isBlank()) {
            sb.append("&url=").append(encode(url));
        }
        if (imageId != null && !imageId.isBlank()) {
            sb.append("&image_id=").append(encode(imageId));
        }
        if (objectId != null && !objectId.isBlank()) {
            sb.append("&object_id=").append(encode(objectId));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private <T> T parseJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new SearchException("Failed to parse Discovery response: " + e.getMessage(), e);
        }
    }

    private List<ProductSummary> parseProducts(String json) {
        VisualSearchApiResponse response = parseJson(json, VisualSearchApiResponse.class);
        if (response.response() == null || response.response().docs() == null) {
            return Collections.emptyList();
        }
        return response.response().docs().stream()
                .map(doc -> new ProductSummary(
                        doc.pid(), doc.title(), doc.url(), doc.thumbImage(),
                        doc.price(), doc.currency(), Collections.emptyMap(), Collections.emptyList()))
                .toList();
    }

    private static List<VisualObject> parseObjects(VisualUploadApiResponse uploadResponse) {
        if (uploadResponse.response() == null || uploadResponse.response().objects() == null) {
            return Collections.emptyList();
        }
        return uploadResponse.response().objects().stream()
                .map(o -> new VisualObject(o.id(), o.bbox(), o.objectType()))
                .toList();
    }
}
