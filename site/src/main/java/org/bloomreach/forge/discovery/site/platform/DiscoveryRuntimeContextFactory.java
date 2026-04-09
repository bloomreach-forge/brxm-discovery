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
        String pageUrl = PageContextResolver.pageUrl(request);
        String refUrl = Objects.requireNonNullElse(request.getHeader("Referer"), pageUrl);
        String pageType = PageContextResolver.pageType(request);
        DiscoveryRuntimeContext runtimeContext = new DiscoveryRuntimeContext(
                config,
                ClientContextExtractor.clientContext(request),
                PixelFlagsResolver.resolvePixelFlags(request),
                paramProvider(request),
                brUid2Service.ensure(request),
                pageUrl,
                PageContextResolver.pageTitle(request, pageType),
                pageType,
                refUrl,
                PageContextResolver.originalRefUrl(request, refUrl),
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
        return overrides != null ? config.withCredentials(overrides) : config;
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

    private static String maskSecret(String s) {
        return s == null ? "null" : (s.isBlank() ? "blank" : "set");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
