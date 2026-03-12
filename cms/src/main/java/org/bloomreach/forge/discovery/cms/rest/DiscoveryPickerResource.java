package org.bloomreach.forge.discovery.cms.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerCategoryDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerItemDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerSearchResponseDto;
import org.bloomreach.forge.discovery.cms.rest.dto.PickerWidgetDto;
import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.request.DiscoveryCoreRequestFactory;
import org.bloomreach.forge.discovery.request.DiscoveryRequestSpec;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;

import javax.jcr.Session;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CMS JAX-RS endpoint used by the Discovery product picker Open UI extension.
 *
 * <p>Registered via {@link org.bloomreach.forge.discovery.cms.picker.DiscoveryPickerModule}
 * at address {@code /discovery/picker}, accessible from the CMS at
 * {@code {cmsContext}/ws/discovery/picker}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /search}     — full-text product search</li>
 *   <li>{@code GET /items}      — look up specific products by PID</li>
 *   <li>{@code GET /categories} — list browsable categories</li>
 *   <li>{@code GET /widgets}    — list available recommendation widgets</li>
 * </ul>
 *
 * <p>All configuration is read from the global Discovery config node at
 * {@link ConfigDefaults#CONFIG_NODE_PATH}. API keys never leave the server.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryPickerResource {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String WIDGETS_PATH = "/api/v1/merchant/widgets";
    private static final int MAX_PAGE_SIZE = 100;
    private static final DiscoveryCoreRequestFactory CORE_REQUEST_FACTORY = new DiscoveryCoreRequestFactory();

    // CXF injects a per-request proxy into this field even though the resource is a singleton.
    @Context
    private UriInfo uriInfo;

    private final Session moduleSession;
    private final DiscoveryConfigProvider configProvider;
    // Extracted as a Function to allow injection of a test double
    final Function<String, String> httpGateway;

    public DiscoveryPickerResource(Session moduleSession, DiscoveryConfigProvider configProvider,
                                   Function<String, String> httpGateway) {
        this.moduleSession = moduleSession;
        this.configProvider = configProvider;
        this.httpGateway = httpGateway;
    }

    @GET
    @Path("/search")
    public PickerSearchResponseDto search(
            @QueryParam("q") @DefaultValue("*") String q,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize) {

        int safePageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        DiscoveryConfig config = resolveConfig();
        DiscoveryCredentials credentials = config.credentials();
        DiscoverySettings settings = config.settings();
        SearchQuery query = new SearchQuery(q, page, safePageSize, settings.defaultSort(), Map.of(), brUid2(), null, requestUrl());
        String url = buildCoreUrl(settings, CORE_REQUEST_FACTORY.search(query, credentials));
        String json = httpGateway.apply(url);
        return parseSearchResponse(json, page, safePageSize);
    }

    /**
     * Looks up specific products by PID. Used to reload already-selected items
     * when an editor re-opens a document.
     *
     * @param ids comma-separated product IDs (PIDs)
     */
    @GET
    @Path("/items")
    public PickerSearchResponseDto items(@QueryParam("ids") String ids) {

        if (ids == null || ids.isBlank()) {
            return new PickerSearchResponseDto(List.of(), 0L, 0, 0);
        }
        List<String> pidList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(MAX_PAGE_SIZE)
                .toList();
        if (pidList.isEmpty()) {
            return new PickerSearchResponseDto(List.of(), 0L, 0, 0);
        }

        DiscoveryConfig config = resolveConfig();
        DiscoveryCredentials credentials = config.credentials();
        // fq=pid:"id1"&fq=pid:"id2" — multiple values produce OR within same field
        SearchQuery query = new SearchQuery("*", 0, pidList.size(), null,
                Map.of("pid", pidList), brUid2(), null, requestUrl());
        String url = buildCoreUrl(config.settings(), CORE_REQUEST_FACTORY.search(query, credentials));
        String json = httpGateway.apply(url);
        return parseSearchResponse(json, 0, pidList.size());
    }

    /**
     * Returns recommendation widgets available for the channel.
     */
    @GET
    @Path("/widgets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PickerWidgetDto> listWidgets() {
        DiscoveryConfig config = resolveConfig();
        String url = buildWidgetsUrl(config.settings(), config.credentials());
        String json = httpGateway.apply(url);
        return parseWidgetsResponse(json);
    }

    /**
     * Returns browsable categories for the channel.
     *
     * <p>Uses {@code search_type=category&rows=0} against the core API so only
     * the {@code category_map} is returned — no product rows are fetched.
     */
    @GET
    @Path("/categories")
    public List<PickerCategoryDto> categories() {
        DiscoveryConfig config = resolveConfig();
        CategoryQuery query = new CategoryQuery("", 0, 0, null, Map.of(), brUid2(), null, requestUrl());
        String url = buildCoreUrl(config.settings(), CORE_REQUEST_FACTORY.category(query, config.credentials()));
        String json = httpGateway.apply(url);
        return parseCategoryMapResponse(json);
    }

    private static String buildCoreUrl(DiscoverySettings settings, DiscoveryRequestSpec request) {
        UriBuilder builder = UriBuilder.fromUri(settings.baseUri()).path(request.path());
        request.forEachQueryParameter((name, value) -> builder.queryParam(name, value));
        return builder.build().toString();
    }

    /** Per-request UID derived from the CMS base URL; falls back to localhost when not in a live request (tests). */
    private String requestUrl() {
        return uriInfo != null ? uriInfo.getBaseUri().toString() : "http://localhost/cms";
    }

    /** Generates a fresh UUID-based tracking value per request. */
    private static String brUid2() {
        return "uid=" + java.util.UUID.randomUUID();
    }

    private DiscoveryConfig resolveConfig() {
        try {
            DiscoveryConfig config = configProvider.get(moduleSession);
            validateConfig(config);
            return config;
        } catch (NotFoundException | InternalServerErrorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InternalServerErrorException("Failed to resolve Discovery configuration", e);
        }
    }

    private static void validateConfig(DiscoveryConfig config) {
        DiscoveryCredentials credentials = config.credentials();
        if (isBlank(credentials.accountId())) {
            throw new NotFoundException("Discovery accountId is required at "
                    + ConfigDefaults.CONFIG_NODE_PATH + " or via BRXDIS_ACCOUNT_ID / -Dbrxdis.accountId");
        }
        if (isBlank(credentials.domainKey())) {
            throw new NotFoundException("Discovery domainKey is required at "
                    + ConfigDefaults.CONFIG_NODE_PATH + " or via BRXDIS_DOMAIN_KEY / -Dbrxdis.domainKey");
        }
        if (isBlank(credentials.apiKey())) {
            throw new NotFoundException("Discovery apiKey is required at "
                    + ConfigDefaults.CONFIG_NODE_PATH + " or via BRXDIS_API_KEY / -Dbrxdis.apiKey");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static PickerSearchResponseDto parseSearchResponse(String json, int page, int pageSize) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode response = root.path("response");
            long total = response.path("numFound").asLong(0);
            List<PickerItemDto> items = new ArrayList<>();
            for (JsonNode doc : response.path("docs")) {
                String price = doc.path("price").isNumber()
                        ? doc.path("price").asText()
                        : null;
                items.add(new PickerItemDto(
                        doc.path("pid").asText(null),
                        doc.path("title").asText(null),
                        doc.path("thumb_image").asText(null),
                        doc.path("url").asText(null),
                        price));
            }
            return new PickerSearchResponseDto(items, total, page, pageSize);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse Discovery API response", e);
        }
    }

    private String buildWidgetsUrl(DiscoverySettings settings, DiscoveryCredentials credentials) {
        StringBuilder sb = new StringBuilder(settings.baseUri())
                .append(WIDGETS_PATH)
                .append("?account_id=").append(encode(credentials.accountId()))
                .append("&domain_key=").append(encode(credentials.domainKey()));
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            sb.append("&auth_key=").append(encode(credentials.authKey()));
        }
        return sb.toString();
    }

    private static List<PickerWidgetDto> parseWidgetsResponse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode widgets = root.path("response").path("widgets");
            List<PickerWidgetDto> result = new ArrayList<>();
            for (JsonNode w : widgets) {
                result.add(new PickerWidgetDto(
                        w.path("id").asText(null),
                        w.path("name").asText(null),
                        w.path("type").asText(null),
                        w.path("enabled").asBoolean(false),
                        w.path("description").asText(null)));
            }
            return result;
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<PickerCategoryDto> parseCategoryMapResponse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode categoryMap = root.path("category_map");
            List<PickerCategoryDto> result = new ArrayList<>();
            categoryMap.fields().forEachRemaining(entry -> {
                String id   = entry.getKey();
                String name = entry.getValue().asText(id);
                result.add(new PickerCategoryDto(id, name));
            });
            return result;
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse category_map response", e);
        }
    }

    private static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
