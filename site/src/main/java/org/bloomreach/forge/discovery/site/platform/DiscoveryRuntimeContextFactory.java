package org.bloomreach.forge.discovery.site.platform;

import jakarta.servlet.http.Cookie;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Map;
import java.util.Objects;

final class DiscoveryRuntimeContextFactory {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRuntimeContextFactory.class);
    private static final String ATTR = DiscoveryRuntimeContextFactory.class.getName();

    private final DiscoveryConfigProvider configProvider;

    DiscoveryRuntimeContextFactory(DiscoveryConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    DiscoveryRuntimeContext get(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        Object cached = requestContext.getAttribute(ATTR);
        if (cached instanceof DiscoveryRuntimeContext runtimeContext) {
            return runtimeContext;
        }

        DiscoveryConfig config = configFor(requestContext);
        String pageUrl = pageUrl(request);
        DiscoveryRuntimeContext runtimeContext = new DiscoveryRuntimeContext(
                config,
                clientContext(request),
                resolvePixelFlags(request),
                paramProvider(request),
                cookieValue(request, HstDiscoveryService.BR_UID_2),
                pageUrl,
                Objects.requireNonNullElse(request.getHeader("Referer"), pageUrl),
                extractClientIp(request)
        );
        requestContext.setAttribute(ATTR, runtimeContext);
        return runtimeContext;
    }

    DiscoveryConfig configFor(HstRequestContext ctx) {
        Session requestSession = null;
        try {
            requestSession = ctx.getSession();
        } catch (RepositoryException e) {
            log.debug("[configFor] Cannot acquire request JCR session: {}", e.getMessage());
        }
        DiscoveryConfig config = configProvider.get(requestSession);
        DiscoveryCredentials credentials = config.credentials();
        if (log.isDebugEnabled()) {
            log.debug("[configFor] accountId='{}' domainKey='{}' apiKey={} authKey={}",
                    credentials.accountId(), credentials.domainKey(),
                    maskSecret(credentials.apiKey()), maskSecret(credentials.authKey()));
        }
        validateCredentials(credentials);
        return config;
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
            return xff.split(",")[0].trim();
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

    private static String cookieValue(HstRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
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
