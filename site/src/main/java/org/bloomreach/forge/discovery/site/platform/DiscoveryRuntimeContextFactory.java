package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryChannelConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class DiscoveryRuntimeContextFactory {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRuntimeContextFactory.class);
    private static final String ATTR = DiscoveryRuntimeContextFactory.class.getName();
    private static final Pattern IP_PATTERN =
            Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]+$");

    private final DiscoveryConfigProvider configProvider;
    private final DiscoveryBrUid2Service brUid2Service;
    private final Function<String, String> envResolver;

    public DiscoveryRuntimeContextFactory(DiscoveryConfigProvider configProvider) {
        this(configProvider, new DiscoveryBrUid2Service(), System::getenv);
    }

    public DiscoveryRuntimeContextFactory(DiscoveryConfigProvider configProvider,
                                          DiscoveryBrUid2Service brUid2Service) {
        this(configProvider, brUid2Service, System::getenv);
    }

    /** Seam for tests — allows injecting a custom env resolver. */
    public DiscoveryRuntimeContextFactory(DiscoveryConfigProvider configProvider,
                                          Function<String, String> envResolver) {
        this(configProvider, new DiscoveryBrUid2Service(), envResolver);
    }

    DiscoveryRuntimeContextFactory(DiscoveryConfigProvider configProvider,
                                   DiscoveryBrUid2Service brUid2Service,
                                   Function<String, String> envResolver) {
        this.configProvider = configProvider;
        this.brUid2Service = brUid2Service;
        this.envResolver = envResolver;
    }

    DiscoveryRuntimeContext get(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        Object cached = requestContext.getAttribute(ATTR);
        if (cached instanceof DiscoveryRuntimeContext runtimeContext) {
            return runtimeContext;
        }

        DiscoveryConfig rawConfig = configProvider.get(sessionOf(requestContext));
        DiscoveryConfig config = applyChannelOverrides(rawConfig, requestContext);
        logCredentials(config.credentials());
        validateCredentials(config.credentials());
        String pageUrl = pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), pageUrl);
        String pageType = pageType(request);
        DiscoveryRuntimeContext runtimeContext = new DiscoveryRuntimeContext(
                config,
                clientContext(request),
                resolvePixelFlags(request),
                paramProvider(request),
                brUid2Service.ensure(request),
                pageUrl,
                pageTitle(request, pageType),
                pageType,
                refUrl,
                originalRefUrl(request, refUrl),
                extractClientIp(request)
        );
        requestContext.setAttribute(ATTR, runtimeContext);
        return runtimeContext;
    }

    DiscoveryConfig configFor(HstRequestContext ctx) {
        DiscoveryConfig config = configProvider.get(sessionOf(ctx));
        validateCredentials(config.credentials());
        return config;
    }

    private static Session sessionOf(HstRequestContext ctx) {
        try {
            return ctx.getSession();
        } catch (RepositoryException e) {
            log.debug("[configFor] Cannot acquire request JCR session: {}", e.getMessage());
            return null;
        }
    }

    private static void logCredentials(DiscoveryCredentials credentials) {
        if (log.isDebugEnabled()) {
            log.debug("[configFor] accountId='{}' domainKey='{}' apiKey={} authKey={}",
                    credentials.accountId(), credentials.domainKey(),
                    maskSecret(credentials.apiKey()), maskSecret(credentials.authKey()));
        }
    }

    private DiscoveryConfig applyChannelOverrides(DiscoveryConfig config, HstRequestContext ctx) {
        Mount mount = ctx.getResolvedMount().getMount();
        DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
        if (channelInfo == null) {
            return config;
        }
        DiscoveryCredentials overrides = DiscoveryChannelConfigReader.resolveOverrides(
                channelInfo.getDiscoveryAccountId(),
                channelInfo.getDiscoveryDomainKey(),
                channelInfo.getDiscoveryApiKeyEnvVar(),
                channelInfo.getDiscoveryAuthKeyEnvVar(),
                envResolver);
        return overrides != null ? config.withCredentials(overrides) : config;
    }

    static String resolvePixelRegion(DiscoveryChannelInfo channelInfo) {
        String sysProp = System.getProperty("brxdis.pixel.region");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.toUpperCase();
        }
        if (channelInfo != null) {
            String channelRegion = channelInfo.getPixelRegion();
            if (channelRegion != null && !channelRegion.isBlank()) {
                return channelRegion.toUpperCase();
            }
        }
        return "US";
    }

    private static PixelFlags resolvePixelFlags(HstRequest request) {
        if (!PixelFlags.envEnabled()) {
            return PixelFlags.DISABLED;
        }
        Mount mount = request.getRequestContext().getResolvedMount().getMount();
        DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
        String region = resolvePixelRegion(channelInfo);
        if (channelInfo == null) {
            return new PixelFlags(true, PixelFlags.envTestData(), PixelFlags.envDebug(), region);
        }
        if (!channelInfo.getDiscoveryPixelsEnabled()) {
            return PixelFlags.DISABLED;
        }
        return new PixelFlags(true, channelInfo.getDiscoveryPixelTestData(), channelInfo.getDiscoveryPixelDebug(), region);
    }

    private static void validateCredentials(DiscoveryCredentials credentials) {
        if (isBlank(credentials.accountId())) {
            throw new ConfigurationException(
                    "Discovery accountId is required — set BRXDIS_ACCOUNT_ID env var, -Dbrxdis.accountId, or brxdis:accountId JCR property");
        }
        if (isBlank(credentials.domainKey())) {
            throw new ConfigurationException(
                    "Discovery domainKey is required — set BRXDIS_DOMAIN_KEY env var, -Dbrxdis.domainKey, or brxdis:domainKey JCR property");
        }
        if (isBlank(credentials.apiKey())) {
            throw new ConfigurationException(
                    "Discovery apiKey is required — set brxdis:apiKey in the config node, BRXDIS_API_KEY env var, or -Dbrxdis.apiKey");
        }
    }

    private static String maskSecret(String s) {
        return s == null ? "null" : (s.isBlank() ? "blank" : "set");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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
            String candidate = xff.split(",")[0].trim();
            if (IP_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
            log.debug("Ignoring malformed X-Forwarded-For value: {}", candidate);
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }

    private static QueryParamParser.RequestParamProvider paramProvider(HstRequest request) {
        jakarta.servlet.http.HttpServletRequest servletRequest =
                request.getRequestContext().getServletRequest();
        return new QueryParamParser.RequestParamProvider() {
            @Override
            public String getParameter(String name) {
                return servletRequest.getParameter(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return servletRequest.getParameterMap();
            }
        };
    }

    private static String originalRefUrl(HstRequest request, String fallbackRefUrl) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext == null || requestContext.getServletRequest() == null) {
            return null;
        }
        String fromParam = requestContext.getServletRequest().getParameter("orig_ref_url");
        if (fromParam != null && !fromParam.isBlank()) {
            return fromParam;
        }
        String fromHeader = request.getHeader("X-Brxdis-Orig-Ref-Url");
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader;
        }
        return fallbackRefUrl;
    }

    private static String pageTitle(HstRequest request, String pageType) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext != null) {
            ResolvedSiteMapItem siteMapItem = requestContext.getResolvedSiteMapItem();
            if (siteMapItem != null) {
                String pageTitle = siteMapItem.getPageTitle();
                if (pageTitle != null && !pageTitle.isBlank()) {
                    return pageTitle;
                }
            }
        }
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            return "homepage".equals(pageType) ? "Home" : pageType;
        }
        return requestUri;
    }

    private static String pageType(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext != null && requestContext.getServletRequest() != null) {
            String override = requestContext.getServletRequest().getParameter("brxdis_ptype");
            if (override != null && !override.isBlank()) {
                return override;
            }
        }
        String headerOverride = request.getHeader("X-Brxdis-Ptype");
        if (headerOverride != null && !headerOverride.isBlank()) {
            return headerOverride;
        }
        String pid = requestContext != null && requestContext.getServletRequest() != null
                ? requestContext.getServletRequest().getParameter("pid") : null;
        if (pid != null && !pid.isBlank()) {
            return "product";
        }
        String query = requestContext != null && requestContext.getServletRequest() != null
                ? requestContext.getServletRequest().getParameter("q") : null;
        if (query != null && !query.isBlank()) {
            return "search";
        }
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            return "homepage";
        }
        String normalized = requestUri.toLowerCase();
        if (normalized.contains("/product")) {
            return "product";
        }
        if (normalized.contains("/category")) {
            return "category";
        }
        return "content";
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
