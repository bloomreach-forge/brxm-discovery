package org.bloomreach.forge.discovery.cms.rest;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.bloomreach.forge.discovery.config.DiscoveryChannelConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.request.DiscoveryRequestSpec;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPickerResource.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final DiscoveryRequestFactory REQUEST_FACTORY = new DiscoveryRequestFactory();

    // CXF injects a per-request proxy into this field even though the resource is a singleton.
    @Context
    private UriInfo uriInfo;

    private final Session moduleSession;
    private final DiscoveryConfigProvider configProvider;
    private final DiscoveryPickerResponseMapper responseMapper;
    // Extracted as a Function to allow injection of a test double
    final Function<String, String> httpGateway;
    private final Function<String, String> envResolver;

    public DiscoveryPickerResource(Session moduleSession, DiscoveryConfigProvider configProvider,
                                   Function<String, String> httpGateway) {
        this(moduleSession, configProvider, httpGateway, new DiscoveryPickerResponseMapper());
    }

    DiscoveryPickerResource(Session moduleSession, DiscoveryConfigProvider configProvider,
                            Function<String, String> httpGateway,
                            DiscoveryPickerResponseMapper responseMapper) {
        this(moduleSession, configProvider, httpGateway, responseMapper, System::getenv);
    }

    /** Package-private seam for tests — allows injecting a custom env resolver. */
    DiscoveryPickerResource(Session moduleSession, DiscoveryConfigProvider configProvider,
                            Function<String, String> httpGateway,
                            DiscoveryPickerResponseMapper responseMapper,
                            Function<String, String> envResolver) {
        this.moduleSession = moduleSession;
        this.configProvider = configProvider;
        this.httpGateway = httpGateway;
        this.responseMapper = responseMapper;
        this.envResolver = envResolver;
    }

    @GET
    @Path("/search")
    public PickerSearchResponseDto search(
            @QueryParam("channelId") @DefaultValue("") String channelId,
            @QueryParam("documentId") @DefaultValue("") String documentId,
            @QueryParam("q") @DefaultValue("*") String q,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize) {

        int safePageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        DiscoveryConfig config = resolveConfig(channelId, documentId);
        DiscoveryCredentials credentials = config.credentials();
        DiscoverySettings settings = config.settings();
        SearchQuery query = new SearchQuery(q, page, safePageSize, settings.defaultSort(), Map.of(), brUid2(), null, requestUrl());
        String url = buildAbsoluteUrl(settings, REQUEST_FACTORY.search(query, credentials));
        String json = httpGateway.apply(url);
        return responseMapper.toSearchResponse(json, page, safePageSize);
    }

    /**
     * Looks up specific products by PID. Used to reload already-selected items
     * when an editor re-opens a document.
     *
     * @param ids comma-separated product IDs (PIDs)
     */
    @GET
    @Path("/items")
    public PickerSearchResponseDto items(
            @QueryParam("channelId") @DefaultValue("") String channelId,
            @QueryParam("documentId") @DefaultValue("") String documentId,
            @QueryParam("ids") String ids) {

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

        DiscoveryConfig config = resolveConfig(channelId, documentId);
        DiscoveryCredentials credentials = config.credentials();
        // fq=pid:"id1"&fq=pid:"id2" — multiple values produce OR within same field
        SearchQuery query = new SearchQuery("*", 0, pidList.size(), null,
                Map.of("pid", pidList), brUid2(), null, requestUrl());
        String url = buildAbsoluteUrl(config.settings(), REQUEST_FACTORY.search(query, credentials));
        String json = httpGateway.apply(url);
        return responseMapper.toSearchResponse(json, 0, pidList.size());
    }

    /**
     * Returns recommendation widgets available for the channel.
     */
    @GET
    @Path("/widgets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PickerWidgetDto> listWidgets(
            @QueryParam("channelId") @DefaultValue("") String channelId,
            @QueryParam("documentId") @DefaultValue("") String documentId) {
        DiscoveryConfig config = resolveConfig(channelId, documentId);
        String url = buildAbsoluteUrl(config.settings(), REQUEST_FACTORY.merchantWidgets(config.credentials()));
        String json = httpGateway.apply(url);
        return responseMapper.toWidgets(json);
    }

    /**
     * Returns browsable categories for the channel.
     *
     * <p>Uses {@code search_type=category&rows=0} against the core API so only
     * the {@code category_map} is returned — no product rows are fetched.
     */
    @GET
    @Path("/categories")
    public List<PickerCategoryDto> categories(
            @QueryParam("channelId") @DefaultValue("") String channelId,
            @QueryParam("documentId") @DefaultValue("") String documentId) {
        DiscoveryConfig config = resolveConfig(channelId, documentId);
        CategoryQuery query = new CategoryQuery("", 0, 0, null, Map.of(), brUid2(), null, requestUrl());
        String url = buildAbsoluteUrl(config.settings(), REQUEST_FACTORY.category(query, config.credentials()));
        String json = httpGateway.apply(url);
        return responseMapper.toCategories(json);
    }

    private static String buildAbsoluteUrl(DiscoverySettings settings, DiscoveryRequestSpec request) {
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

    private void refreshSession() {
        try {
            moduleSession.refresh(false);
        } catch (RepositoryException e) {
            log.warn("brxm-discovery: could not refresh module session: {}", e.getMessage());
        }
    }

    private DiscoveryConfig resolveConfig(String channelId, String documentId) {
        try {
            refreshSession();
            DiscoveryConfig base = configProvider.get(moduleSession);
            String effectiveChannelId = !channelId.isBlank()
                    ? channelId
                    : resolveChannelFromDocument(documentId, moduleSession);
            DiscoveryCredentials overrides = DiscoveryChannelConfigReader.resolveFromJcr(effectiveChannelId, moduleSession, envResolver);
            DiscoveryConfig config = overrides != null ? base.withCredentialOverrides(overrides) : base;
            validateConfig(config);
            return config;
        } catch (NotFoundException | InternalServerErrorException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InternalServerErrorException("Failed to resolve Discovery configuration", e);
        }
    }

    private String resolveChannelFromDocument(String documentId, Session session) {
        if (documentId == null || documentId.isBlank()) return "";
        try {
            Node docNode = session.getNodeByIdentifier(documentId);
            String[] parts = docNode.getPath().split("/");
            // /content/documents/{siteName}/... → parts[3] = siteName
            return parts.length >= 4 ? parts[3] : "";
        } catch (RepositoryException e) {
            log.debug("[resolveChannelFromDocument] documentId='{}': {}", documentId, e.getMessage());
            return "";
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
}
