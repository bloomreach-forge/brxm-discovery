package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigResolver;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.sor.SoREnrichmentProvider;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClientImpl.toV2WidgetType;

/**
 * HST-aware façade that absorbs config resolution, query building, and request-cache logic.
 * Components become thin: they extract raw HST params, delegate here, and set model/attributes.
 * <p>
 * Config is resolved via {@link DiscoveryConfigProvider} — JVM-lifetime cache with JCR
 * observation-driven invalidation. No per-request JCR reads.
 * <p>
 * Pixel events are fired asynchronously on cache-miss only (prevents double-firing when
 * multiple components share the same page render).
 */
public class HstDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(HstDiscoveryService.class);
    public static final String BR_UID_2 = "_br_uid_2";

    private final DiscoveryClient client;
    private final DiscoveryConfigProvider configProvider;
    private final DiscoveryPixelService pixelService;
    private final SoREnrichmentProvider enrichmentProvider;

    public HstDiscoveryService(DiscoveryClient client,
                               DiscoveryConfigProvider configProvider,
                               DiscoveryPixelService pixelService,
                               SoREnrichmentProvider enrichmentProvider) {
        this.client = client;
        this.configProvider = configProvider;
        this.pixelService = pixelService;
        this.enrichmentProvider = enrichmentProvider;
    }

    // ── Request-based API (used by HST components) ─────────────────────────────

    public SearchResponse search(HstRequest request) {
        return search(request, 0, null, null, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort) {
        return search(request, componentPageSize, componentSort, null, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName) {
        return search(request, componentPageSize, componentSort, catalogName, "default", List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label) {
        return search(request, componentPageSize, componentSort, catalogName, label, List.of());
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label, List<String> statsFields) {
        return search(request, componentPageSize, componentSort, catalogName, label, statsFields, null, null);
    }

    public SearchResponse search(HstRequest request, int componentPageSize, String componentSort,
                                 String catalogName, String label, List<String> statsFields,
                                 String componentSegment, String efq) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        SearchQuery baseQuery = QueryParamParser.toSearchQuery(
                paramProvider(request), config, componentPageSize, componentSort, catalogName,
                brUid2, refUrl, url);
        SearchQuery query = statsFields != null && !statsFields.isEmpty()
                ? baseQuery.withStatsFields(statsFields) : baseQuery;
        if (query.segment() == null && componentSegment != null && !componentSegment.isBlank()) {
            query = query.withSegment(componentSegment);
        }
        if (efq != null && !efq.isBlank()) {
            query = query.withEfq(efq);
        }
        final SearchQuery finalQuery = query;
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getSearchResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.search(finalQuery, config, ctx);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putSearchResponse(request, label, fresh);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireSearchEvent(finalQuery, fresh.result(), config, clientIp, ctx, pixelFlags);
                    }
                    return fresh;
                });
    }

    public SearchResponse browse(HstRequest request, String categoryId) {
        return browse(request, categoryId, 0, null, "default", List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort) {
        return browse(request, categoryId, componentPageSize, componentSort, "default", List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort, String label) {
        return browse(request, categoryId, componentPageSize, componentSort, label, List.of());
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort,
                                 String label, List<String> statsFields) {
        return browse(request, categoryId, componentPageSize, componentSort, label, statsFields, null, null);
    }

    public SearchResponse browse(HstRequest request, String categoryId,
                                 int componentPageSize, String componentSort,
                                 String label, List<String> statsFields,
                                 String componentSegment, String efq) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        CategoryQuery baseQuery = QueryParamParser.toCategoryQuery(
                categoryId, paramProvider(request), config, componentPageSize, componentSort,
                brUid2, refUrl, url);
        CategoryQuery query = statsFields != null && !statsFields.isEmpty()
                ? baseQuery.withStatsFields(statsFields) : baseQuery;
        if (query.segment() == null && componentSegment != null && !componentSegment.isBlank()) {
            query = query.withSegment(componentSegment);
        }
        if (efq != null && !efq.isBlank()) {
            query = query.withEfq(efq);
        }
        final CategoryQuery finalQuery = query;
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getCategoryResponse(request, label)
                .orElseGet(() -> {
                    SearchResponse fresh = client.category(finalQuery, config, ctx);
                    fresh = applyEnrichment(fresh);
                    DiscoveryRequestCache.putCategoryResponse(request, label, fresh);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireCategoryEvent(finalQuery, fresh.result(), config, clientIp, ctx, pixelFlags);
                    }
                    return fresh;
                });
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String contextPageType,
                                           int limit, String fields, String filter) {
        return recommend(request, widgetId, widgetType, contextProductId, contextPageType,
                limit, fields, filter, "default");
    }

    public RecommendationResult recommend(HstRequest request,
                                           String widgetId, String widgetType,
                                           String contextProductId, String contextPageType,
                                           int limit, String fields, String filter,
                                           String label) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String effectiveWidgetId = widgetId != null ? widgetId : "";

        if (widgetType != null && "item".equals(toV2WidgetType(widgetType))
                && (contextProductId == null || contextProductId.isBlank())) {
            log.warn("Skipping item widget '{}' (type='{}'): item_ids not resolved.",
                     effectiveWidgetId, widgetType);
            return RecommendationResult.of(List.of());
        }

        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        String brUid2 = cookieValue(request, BR_UID_2);
        RecQuery query = new RecQuery(widgetType, effectiveWidgetId, contextProductId, contextPageType,
                limit, fields, filter, url, refUrl, brUid2);
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        return DiscoveryRequestCache.getRecommendations(request, label, effectiveWidgetId)
                .orElseGet(() -> {
                    RecommendationResult fresh = client.recommend(query, config, ctx);
                    List<ProductSummary> enriched = applyEnrichment(fresh.products());
                    RecommendationResult result = new RecommendationResult(fresh.widgetResultId(), enriched);
                    DiscoveryRequestCache.putRecommendations(request, label, effectiveWidgetId, result);
                    if (pixelService != null && pixelFlags.enabled()) {
                        String clientIp = extractClientIp(request);
                        pixelService.fireWidgetEvent(query, result, config, clientIp, ctx, pixelFlags);
                    }
                    return result;
                });
    }

    public Optional<ProductSummary> fetchProduct(HstRequest request, String pid) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String url = pageUrl(request);
        String brUid2 = cookieValue(request, BR_UID_2);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        ClientContext ctx = clientContext(request);
        PixelFlags pixelFlags = resolvePixelFlags(request);
        Optional<ProductSummary> result = client.fetchProduct(pid, url, config, ctx);
        if (result.isPresent()) {
            if (pixelService != null && pixelFlags.enabled()) {
                String clientIp = extractClientIp(request);
                String prodName = result.get().title();
                pixelService.fireProductPageViewEvent(pid, prodName, brUid2, refUrl, url, config, clientIp, ctx, pixelFlags);
            }
            if (enrichmentProvider != null) {
                List<ProductSummary> enriched = enrichmentProvider.enrich(List.of(result.get()));
                return enriched.isEmpty() ? Optional.empty() : Optional.of(enriched.get(0));
            }
        }
        return result;
    }

    // ── Autosuggest (real-time, no caching, no pixels) ────────────────────────

    public AutosuggestResult autosuggest(HstRequest request, String query, int limit) {
        DiscoveryConfig config = configFor(request.getRequestContext());
        String brUid2 = cookieValue(request, BR_UID_2);
        String url = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), url);
        AutosuggestQuery suggestQuery = new AutosuggestQuery(query, limit, null,
                brUid2, refUrl, url);
        return client.autosuggest(suggestQuery, config, clientContext(request));
    }

    // ── Programmatic API (pre-built queries, no HST request param parsing) ──────

    public SearchResponse search(HstRequestContext ctx, SearchQuery query) {
        return client.search(query, configFor(ctx), ClientContext.EMPTY);
    }

    public SearchResponse browse(HstRequestContext ctx, CategoryQuery query) {
        return client.category(query, configFor(ctx), ClientContext.EMPTY);
    }

    public RecommendationResult recommend(HstRequestContext ctx, RecQuery query) {
        return client.recommend(query, configFor(ctx), ClientContext.EMPTY);
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private SearchResponse applyEnrichment(SearchResponse response) {
        if (enrichmentProvider == null) return response;
        SearchResult r = response.result();
        List<ProductSummary> enriched = enrichmentProvider.enrich(r.products());
        SearchResult enrichedResult = new SearchResult(enriched, r.total(), r.page(), r.pageSize(), r.facets());
        return new SearchResponse(enrichedResult, response.metadata());
    }

    private List<ProductSummary> applyEnrichment(List<ProductSummary> products) {
        if (enrichmentProvider == null) {
            return products;
        }
        return enrichmentProvider.enrich(products);
    }

    /**
     * Resolves pixel control flags for the current request.
     * <ul>
     *   <li>Env kill switch ({@code brxdis.pixel.envEnabled=false}) disables all pixels globally.</li>
     *   <li>{@link DiscoveryChannelInfo} typed accessors drive per-channel flags.</li>
     *   <li>If no channel info is configured, falls back to env/system property defaults.</li>
     * </ul>
     */
    private static PixelFlags resolvePixelFlags(HstRequest request) {
        if (!PixelFlags.envEnabled()) return PixelFlags.DISABLED;
        Mount mount = request.getRequestContext().getResolvedMount().getMount();
        DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
        String region = resolvePixelRegion(channelInfo);
        if (channelInfo == null) {
            return new PixelFlags(true, PixelFlags.envTestData(), PixelFlags.envDebug(), region);
        }
        if (!channelInfo.getDiscoveryPixelsEnabled()) return PixelFlags.DISABLED;
        return new PixelFlags(true, channelInfo.getDiscoveryPixelTestData(), channelInfo.getDiscoveryPixelDebug(), region);
    }

    /**
     * Resolves the pixel endpoint region.
     * Precedence: system property {@code brxdis.pixel.region} → Channel Manager → "US".
     */
    static String resolvePixelRegion(DiscoveryChannelInfo channelInfo) {
        String sysProp = System.getProperty("brxdis.pixel.region");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.toUpperCase();
        if (channelInfo != null) {
            String channelRegion = channelInfo.getPixelRegion();
            if (channelRegion != null && !channelRegion.isBlank()) return channelRegion.toUpperCase();
        }
        return "US";
    }

    private DiscoveryConfig configFor(HstRequestContext ctx) {
        Mount mount = ctx.getResolvedMount().getMount();
        String path = mount.getParameter(DiscoveryConfigResolver.CONFIG_PATH_PARAM);
        DiscoveryConfig config = configProvider.get(path);
        if (log.isDebugEnabled()) {
            log.debug("[configFor] configPath='{}' accountId='{}' domainKey='{}' apiKey={} authKey={}",
                    path, config.accountId(), config.domainKey(),
                    maskSecret(config.apiKey()), maskSecret(config.authKey()));
            DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
            log.debug("[configFor] channelInfo={}", channelInfo == null ? "null" : channelInfo.getClass().getName());
            if (channelInfo != null) {
                log.debug("[configFor] channelInfo.accountId='{}' domainKey='{}' apiKeyEnvVar='{}' authKeyEnvVar='{}'",
                        channelInfo.getAccountId(), channelInfo.getDomainKey(),
                        channelInfo.getApiKeyEnvVar(), channelInfo.getAuthKeyEnvVar());
            }
        }
        DiscoveryConfig patched = patchFromChannelInfo(config, mount.getChannelInfo());
        if (log.isDebugEnabled()) {
            if (patched != config) {
                log.debug("[configFor] patched: accountId='{}' domainKey='{}' apiKey={} authKey={}",
                        patched.accountId(), patched.domainKey(),
                        maskSecret(patched.apiKey()), maskSecret(patched.authKey()));
            } else {
                log.debug("[configFor] no patch applied (config unchanged)");
            }
        }
        validateConfig(patched);
        return patched;
    }

    private static String maskSecret(String s) {
        return s == null ? "null" : (s.isBlank() ? "blank" : "set");
    }

    private static void validateConfig(DiscoveryConfig config) {
        if (isBlank(config.accountId())) throw new ConfigurationException(
                "Discovery accountId is required — set BRXDIS_ACCOUNT_ID env var, -Dbrxdis.accountId, brxdis:accountId JCR property, or discoveryAccountId in Channel Manager");
        if (isBlank(config.domainKey())) throw new ConfigurationException(
                "Discovery domainKey is required — set BRXDIS_DOMAIN_KEY env var, -Dbrxdis.domainKey, brxdis:domainKey JCR property, or discoveryDomainKey in Channel Manager");
        if (isBlank(config.apiKey())) throw new ConfigurationException(
                "Discovery apiKey is required — set BRXDIS_API_KEY env var, -Dbrxdis.apiKey, brxdis:apiKey JCR property, or discoveryApiKeyEnvVar in Channel Manager");
    }

    /**
     * Applies Channel Manager credentials as the authoritative source for per-channel identifiers.
     * <ul>
     *   <li>{@code accountId} / {@code domainKey}: Channel Manager wins if non-blank (non-secret channel IDs).</li>
     *   <li>{@code apiKey} / {@code authKey}: per-channel env var name is tried first; global config is fallback.</li>
     * </ul>
     */
    static DiscoveryConfig patchFromChannelInfo(DiscoveryConfig config, DiscoveryChannelInfo channelInfo) {
        return patchFromChannelInfo(config, channelInfo, System::getenv);
    }

    /**
     * Testable form — accepts an env lookup seam so tests can inject a controlled lambda.
     */
    static DiscoveryConfig patchFromChannelInfo(DiscoveryConfig config, DiscoveryChannelInfo channelInfo,
                                                 Function<String, String> envLookup) {
        if (channelInfo == null) return config;

        // accountId / domainKey: channelInfo is source of truth (non-secret channel identifiers)
        String ciAccountId = channelInfo.getAccountId();
        String ciDomainKey = channelInfo.getDomainKey();
        String accountId = !isBlank(ciAccountId) ? ciAccountId : config.accountId();
        String domainKey = !isBlank(ciDomainKey) ? ciDomainKey : config.domainKey();

        // apiKey / authKey: per-channel env var lookup wins; fall back to whatever global config has
        String resolvedApiKey  = lookupEnvVar(channelInfo.getApiKeyEnvVar(),  envLookup);
        String resolvedAuthKey = lookupEnvVar(channelInfo.getAuthKeyEnvVar(), envLookup);
        String apiKey  = !isBlank(resolvedApiKey)  ? resolvedApiKey  : config.apiKey();
        String authKey = !isBlank(resolvedAuthKey) ? resolvedAuthKey : config.authKey();

        if (Objects.equals(accountId, config.accountId()) && Objects.equals(domainKey, config.domainKey())
                && Objects.equals(apiKey, config.apiKey()) && Objects.equals(authKey, config.authKey())) return config;
        return new DiscoveryConfig(accountId, domainKey, apiKey, authKey,
                config.baseUri(), config.pathwaysBaseUri(), config.environment(),
                config.defaultPageSize(), config.defaultSort());
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    /**
     * Looks up an env var by name. Returns the value if found and non-blank; {@code null} otherwise.
     * Returning {@code null} signals "no resolution" — the caller preserves the original config value.
     */
    private static String lookupEnvVar(String name, Function<String, String> envLookup) {
        if (name == null || name.isBlank()) return null;
        String value = envLookup.apply(name);
        return (value != null && !value.isBlank()) ? value : null;
    }

    private static ClientContext clientContext(HstRequest request) {
        return new ClientContext(
                request.getHeader("User-Agent"),
                request.getHeader("Accept-Language"),
                request.getHeader("X-Forwarded-For")
        );
    }

    private static String extractClientIp(HstRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }

    private static QueryParamParser.RequestParamProvider paramProvider(HstRequest request) {
        jakarta.servlet.http.HttpServletRequest servletRequest =
                request.getRequestContext().getServletRequest();
        return new QueryParamParser.RequestParamProvider() {
            @Override public String getParameter(String name) { return servletRequest.getParameter(name); }
            @Override public Map<String, String[]> getParameterMap() { return servletRequest.getParameterMap(); }
        };
    }

    private static String cookieValue(HstRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private static String pageUrl(HstRequest request) {
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getServerName())
                .replacePath(request.getRequestURI());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            urlBuilder.port(port);
        }
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            urlBuilder.query(query);
        }
        return urlBuilder.build(false).toUriString();
    }
}
