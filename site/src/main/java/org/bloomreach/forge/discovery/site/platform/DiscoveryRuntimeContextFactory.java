package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryChannelConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class DiscoveryRuntimeContextFactory {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRuntimeContextFactory.class);
    private static final String ATTR = DiscoveryRuntimeContextFactory.class.getName();

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

    /** Seam for tests - allows injecting a custom env resolver. */
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
        DiscoveryRuntimeContext runtimeContext = new DiscoveryRuntimeContext(
                config,
                ClientContextExtractor.clientContext(request),
                PixelFlagsResolver.resolvePixelFlags(request),
                paramProvider(request),
                brUid2Service.ensure(request),
                pageUrl,
                pageTitle(request),
                refUrl,
                originalRefUrl(request, refUrl),
                ClientContextExtractor.extractClientIp(request)
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
        if (overrides != null) {
            config = config.withCredentials(overrides);
        }
        String fl = channelInfo.getDiscoveryDefaultFieldList();
        if (fl != null && !fl.isBlank()) {
            config = config.withFieldList(fl);
        }
        return config;
    }

    private static void validateCredentials(DiscoveryCredentials credentials) {
        if (isBlank(credentials.accountId())) {
            throw new ConfigurationException(
                    "Discovery accountId is required - set BRXDIS_ACCOUNT_ID env var, -Dbrxdis.accountId, or brxdis:accountId JCR property");
        }
        if (isBlank(credentials.domainKey())) {
            throw new ConfigurationException(
                    "Discovery domainKey is required - set BRXDIS_DOMAIN_KEY env var, -Dbrxdis.domainKey, or brxdis:domainKey JCR property");
        }
        if (isBlank(credentials.apiKey())) {
            throw new ConfigurationException(
                    "Discovery apiKey is required - set brxdis:apiKey in the config node, BRXDIS_API_KEY env var, or -Dbrxdis.apiKey");
        }
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

    private static String pageUrl(HstRequest request) {
        StringBuilder sb = new StringBuilder()
                .append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            sb.append(':').append(port);
        }
        sb.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            sb.append('?').append(query);
        }
        return sb.toString();
    }

    private static String pageTitle(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext != null) {
            ResolvedSiteMapItem siteMapItem = requestContext.getResolvedSiteMapItem();
            if (siteMapItem != null) {
                String title = siteMapItem.getPageTitle();
                if (title != null && !title.isBlank()) {
                    return title;
                }
            }
        }
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            return "Home";
        }
        return requestUri;
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

    private static String maskSecret(String s) {
        return s == null ? "null" : (s.isBlank() ? "blank" : "set");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
