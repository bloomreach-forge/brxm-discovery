package org.bloomreach.forge.discovery.site.platform;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * Derives page-type, page-title, page-URL and original referrer from an HST request.
 * All methods are pure functions; no state.
 */
final class PageContextResolver {

    private PageContextResolver() {
    }

    static String pageType(HstRequest request) {
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

    static String pageTitle(HstRequest request, String pageType) {
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

    static String pageUrl(HstRequest request) {
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

    static String originalRefUrl(HstRequest request, String fallbackRefUrl) {
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
}
