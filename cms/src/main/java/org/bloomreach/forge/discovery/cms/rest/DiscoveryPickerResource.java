package org.bloomreach.forge.discovery.cms.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.WidgetInfo;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li>{@code GET /widgets}    — list available Discovery widgets</li>
 * </ul>
 *
 * <p>All endpoints require a {@code configPath} query param pointing to a
 * {@code brxdis:discoveryConfig} JCR node. API keys never leave the server.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryPickerResource {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SEARCH_PATH = "/api/v1/core/";
    private static final String WIDGETS_PATH = "/api/v1/merchant/widgets";
    private static final String DEFAULT_FIELDS = "pid,title,brand,price,sale_price,thumb_image,url,description";
    private static final int MAX_PAGE_SIZE = 100;
    private static final long CACHE_TTL_MS = 60_000L;

    // CXF injects a per-request proxy into this field even though the resource is a singleton.
    @Context
    private UriInfo uriInfo;

    private final Session moduleSession;
    private final DiscoveryConfigReader configReader;
    // Extracted as a Function to allow injection of a test double
    final Function<String, String> httpGateway;
    private final ConcurrentHashMap<String, CachedConfig> configCache = new ConcurrentHashMap<>();

    public DiscoveryPickerResource(Session moduleSession, DiscoveryConfigReader configReader,
                                   Function<String, String> httpGateway) {
        this.moduleSession = moduleSession;
        this.configReader = configReader;
        this.httpGateway = httpGateway;
    }

    @GET
    @Path("/search")
    public PickerSearchResponse search(
            @QueryParam("configPath") String configPath,
            @QueryParam("q") @DefaultValue("*") String q,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize) {

        int safePageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        DiscoveryConfig config = resolveConfig(configPath);
        SearchQuery query = new SearchQuery(q, page, safePageSize, config.defaultSort(), Map.of(), null, null, null);
        String url = buildSearchUrl(query, config, requestUrl(), brUid2());
        String json = httpGateway.apply(url);
        return parseSearchResponse(json, page, safePageSize);
    }

    /**
     * Looks up specific products by PID. Used to reload already-selected items
     * when an editor re-opens a document.
     *
     * @param configPath JCR path to the {@code brxdis:discoveryConfig} node
     * @param ids        comma-separated product IDs (PIDs)
     */
    @GET
    @Path("/items")
    public PickerSearchResponse items(
            @QueryParam("configPath") String configPath,
            @QueryParam("ids") String ids) {

        if (ids == null || ids.isBlank()) {
            return new PickerSearchResponse(List.of(), 0L, 0, 0);
        }
        List<String> pidList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(MAX_PAGE_SIZE)
                .toList();
        if (pidList.isEmpty()) {
            return new PickerSearchResponse(List.of(), 0L, 0, 0);
        }

        DiscoveryConfig config = resolveConfig(configPath);
        // fq=pid:"id1"&fq=pid:"id2" — multiple values produce OR within same field
        SearchQuery query = new SearchQuery("*", 0, pidList.size(), null,
                Map.of("pid", pidList), null, null, null);
        String url = buildSearchUrl(query, config, requestUrl(), brUid2());
        String json = httpGateway.apply(url);
        return parseSearchResponse(json, 0, pidList.size());
    }

    /**
     * Returns browsable categories matching a search query.
     *
     * <p>Uses {@code search_type=category&rows=0} against the core API so only
     * the {@code category_map} is returned — no product rows are fetched.
     *
     * @param configPath JCR path to the {@code brxdis:discoveryConfig} node
     */
    @GET
    @Path("/categories")
    public List<PickerCategoryDto> categories(@QueryParam("configPath") String configPath) {
        DiscoveryConfig config = resolveConfig(configPath);
        String url = buildCategoryUrl(config, requestUrl(), brUid2());
        String json = httpGateway.apply(url);
        return parseCategoryMapResponse(json);
    }

    /**
     * Returns available Discovery widgets for the given account.
     * Matches {@code DiscoveryClientImpl.listWidgets()} — only {@code account_id} is sent.
     */
    @GET
    @Path("/widgets")
    public List<WidgetInfo> widgets(@QueryParam("configPath") String configPath) {
        DiscoveryConfig config = resolveConfig(configPath);
        String url = UriBuilder.fromUri(config.baseUri())
                .path(WIDGETS_PATH)
                .queryParam("account_id", config.accountId())
                .build().toString();
        String json = httpGateway.apply(url);
        return parseWidgetsResponse(json);
    }

    private static String buildSearchUrl(SearchQuery query, DiscoveryConfig config,
                                         String requestUrl, String brUid2) {
        UriBuilder ub = UriBuilder.fromUri(config.baseUri())
                .path(SEARCH_PATH)
                .queryParam("account_id", config.accountId())
                .queryParam("domain_key", config.domainKey());
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            ub.queryParam("auth_key", config.apiKey());
        }
        ub.queryParam("request_type", "search")
          .queryParam("search_type", "keyword")
          .queryParam("q", query.query() != null ? query.query() : "*")
          .queryParam("fl", DEFAULT_FIELDS)
          .queryParam("_br_uid_2", brUid2)
          .queryParam("url", requestUrl)
          .queryParam("start", (long) query.page() * query.pageSize())
          .queryParam("rows", query.pageSize());
        if (query.sort() != null && !query.sort().isBlank()) {
            ub.queryParam("sort", query.sort());
        }
        if (query.filters() != null) {
            for (Map.Entry<String, List<String>> entry : query.filters().entrySet()) {
                for (String value : entry.getValue()) {
                    ub.queryParam("fq", entry.getKey() + ":\"" + value + "\"");
                }
            }
        }
        return ub.build().toString();
    }

    private static String buildCategoryUrl(DiscoveryConfig config,
                                           String requestUrl, String brUid2) {
        UriBuilder ub = UriBuilder.fromUri(config.baseUri())
                .path(SEARCH_PATH)
                .queryParam("account_id", config.accountId())
                .queryParam("domain_key", config.domainKey());
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            ub.queryParam("auth_key", config.apiKey());
        }
        // q="" (empty) means root/all categories for search_type=category.
        // rows=0 so only category_map is returned (no product docs needed for the picker).
        return ub.queryParam("request_type", "search")
                 .queryParam("search_type", "category")
                 .queryParam("q", "")
                 .queryParam("fl", DEFAULT_FIELDS)
                 .queryParam("_br_uid_2", brUid2)
                 .queryParam("url", requestUrl)
                 .queryParam("start", 0)
                 .queryParam("rows", 0)
                 .build().toString();
    }

    /** Per-request UID derived from the CMS base URL; falls back to localhost when not in a live request (tests). */
    private String requestUrl() {
        return uriInfo != null ? uriInfo.getBaseUri().toString() : "http://localhost/cms";
    }

    /** Generates a fresh UUID-based tracking value per request. */
    private static String brUid2() {
        return "uid=" + UUID.randomUUID();
    }

    private DiscoveryConfig resolveConfig(String configPath) {
        validateConfigPath(configPath);

        CachedConfig cached = configCache.get(configPath);
        if (cached != null && !cached.isExpired()) {
            return cached.config();
        }

        DiscoveryConfig config;
        synchronized (moduleSession) {
            // double-check after acquiring lock — another thread may have populated the cache
            cached = configCache.get(configPath);
            if (cached != null && !cached.isExpired()) {
                return cached.config();
            }
            try {
                moduleSession.refresh(false);
                config = configReader.read(moduleSession.getNode(configPath));
            } catch (RepositoryException e) {
                throw new InternalServerErrorException("Failed to read Discovery configuration", e);
            }
        }

        configCache.put(configPath, new CachedConfig(config, System.currentTimeMillis()));
        return config;
    }

    private static void validateConfigPath(String configPath) {
        if (configPath == null || configPath.isBlank()) {
            throw new BadRequestException("configPath query parameter is required");
        }
        if (!configPath.startsWith("/") || configPath.contains("..") || configPath.contains("\0")) {
            throw new BadRequestException("Invalid configPath");
        }
    }

    private static PickerSearchResponse parseSearchResponse(String json, int page, int pageSize) {
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
            return new PickerSearchResponse(items, total, page, pageSize);
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse Discovery API response", e);
        }
    }

    private static List<PickerCategoryDto> parseCategoryMapResponse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode categoryMap = root.path("category_map");
            List<PickerCategoryDto> result = new ArrayList<>();
            categoryMap.fields().forEachRemaining(entry -> {
                String id   = entry.getKey();
                String name = entry.getValue().path("name").asText(id);
                result.add(new PickerCategoryDto(id, name));
            });
            return result;
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse category_map response", e);
        }
    }

    private static List<WidgetInfo> parseWidgetsResponse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode widgets = root.path("response").path("widgets");
            List<WidgetInfo> result = new ArrayList<>();
            for (JsonNode w : widgets) {
                result.add(new WidgetInfo(
                        w.path("id").asText(null),
                        w.path("name").asText(null),
                        w.path("type").asText(null),
                        w.path("enabled").asBoolean(false),
                        w.path("description").asText(null)
                ));
            }
            return result;
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to parse widgets response", e);
        }
    }

    private record CachedConfig(DiscoveryConfig config, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
