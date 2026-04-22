package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for Discovery HST components.
 * Provides typed service lookup, bean path resolution, and int-parsing utilities.
 */
public abstract class AbstractDiscoveryComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractDiscoveryComponent.class);
    static final String MODULE_NAME = "org.bloomreach.forge.discovery.site";

    /**
     * Sets {@code editMode} on the FTL model, then delegates to
     * {@link #doDiscoveryBeforeRender}. Catches any {@link DiscoveryException}
     * (transient API failures or misconfiguration) so a single unavailable
     * Discovery service cannot cause a 500 on the whole page.
     * In edit mode an {@code brxdis_warning} request attribute is set so
     * the FTL template can surface a notice to the author.
     */
    @Override
    public final void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        request.setModel(DiscoveryModelKeys.EDIT_MODE, isEditMode(request));
        try {
            doDiscoveryBeforeRender(request, response);
        } catch (DiscoveryException e) {
            log.warn("Discovery service error in {} during render: {}",
                    getClass().getSimpleName(), e.getMessage(), e);
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning", "Discovery service error: " + e.getMessage());
            }
        } catch (RuntimeException e) {
            log.warn("Unexpected runtime error in {} during render: {}",
                    getClass().getSimpleName(), e.getMessage(), e);
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning", "Discovery unavailable: " + e.getMessage());
            }
        }
    }

    /**
     * Extension point for subclasses. Called by {@link #doBeforeRender} inside a
     * {@link DiscoveryException} safety net.
     */
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response)
            throws HstComponentException {
        // no-op — subclasses override to add component-specific render logic
    }

    protected <T> T lookupService(Class<T> type) {
        if (!HstServices.isAvailable() || HstServices.getComponentManager() == null) {
            throw new ConfigurationException("HST component manager is not available while resolving service: " + type.getName());
        }

        ComponentManager cm = HstServices.getComponentManager();
        T component = null;
        try {
            component = cm.getComponent(type, MODULE_NAME);
        } catch (RuntimeException e) {
            log.warn("Service {} lookup failed in addon module {}: {}", type.getName(), MODULE_NAME, e.getMessage(), e);
        }
        if (component == null) {
            throw new ConfigurationException("Required HST service is not available: " + type.getName());
        }
        return component;
    }

    /**
     * Resolves a site-content-relative path to a typed content bean.
     * Mirrors {@code CommonComponent.getHippoBeanForPath()} but takes the current
     * request explicitly so tests can mock it without a live HST container.
     */
    protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request,
                                                           String path,
                                                           Class<T> beanClass) {
        if (path == null || path.isBlank()) return null;
        var ctx = request.getRequestContext();
        if (ctx == null) return null;
        var siteBase = ctx.getSiteContentBaseBean();
        if (siteBase == null) return null;
        return siteBase.getBean(path, beanClass);
    }

    protected static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    protected static java.util.List<String> parseStatsFields(String csv) {
        if (csv == null || csv.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .toList();
    }

    /**
     * Resolves a URL-driven context parameter by name.
     * Checks the sitemap item's named parameter first (covers SEO path segments when the sitemap
     * defines the name→position mapping), then falls back to a query string parameter with the
     * same name. Returns {@code null} if neither source provides a non-blank value.
     */
    protected String resolveUrlParam(HstRequest request, String paramName) {
        String fromPath = getPathSegmentParam(request, paramName);
        return fromPath != null ? fromPath : blankToNull(getPublicRequestParameter(request, paramName));
    }

    /**
     * Scans the servlet path for a label/value pair.
     *
     * <p>Given a URL like {@code /category/mens-shoes/category/root-cat-id}, calling
     * {@code getPathSegmentParam(request, "category")} returns {@code "root-cat-id"} —
     * the segment that immediately follows the label that equals {@code paramName}.
     *
     * <p>No sitemap configuration is needed; the URL structure alone drives resolution.
     * The admin configures {@code categoryUrlParam = "category"} (or any chosen name) and
     * the sitemap maps that label into the URL path.
     *
     * @param paramName the URL label segment (e.g. {@code "pid"}, {@code "category"})
     * @return the value segment that follows the label, or {@code null} if absent or blank
     */
    protected static String getPathSegmentParam(HstRequest request, String paramName) {
        var ctx = request.getRequestContext();
        if (ctx == null) return null;
        var sr = ctx.getServletRequest();
        if (sr == null) return null;
        String pathInfo = sr.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) return null;
        String[] segments = pathInfo.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (paramName.equals(segments[i])) {
                String val = segments[i + 1];
                return (val != null && !val.isBlank()) ? val : null;
            }
        }
        return null;
    }

    /**
     * Returns true when rendered inside the Channel Manager / Experience Editor.
     */
    protected boolean isEditMode(HstRequest request) {
        HstRequestContext ctx = request.getRequestContext();
        return ctx != null && ctx.isChannelManagerPreviewRequest();
    }

    /**
     * Returns true when this is an isolated component re-render triggered by
     * Channel Manager (PPR). The signal is a non-null
     * {@code componentRenderingWindowReferenceNamespace} on the base URL.
     * On a full-page preview load the namespace is null → method returns false.
     */
    protected boolean isIsolatedComponentRender(HstRequest request) {
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null || !ctx.isChannelManagerPreviewRequest()) return false;
        var baseUrl = ctx.getBaseURL();
        return baseUrl != null && baseUrl.getComponentRenderingWindowReferenceNamespace() != null;
    }

    /**
     * Reads a public URL parameter and parses it as an int, falling back to
     * {@code defaultValue} if absent or unparseable.
     */
    protected int getPublicRequestParameterAsInt(HstRequest request, String param, int defaultValue) {
        return parseIntOrDefault(getPublicRequestParameter(request, param), defaultValue);
    }

    /**
     * Named accessor for the HST Discovery service bean.
     */
    protected HstDiscoveryService getDiscoveryService() {
        return lookupService(HstDiscoveryService.class);
    }

    protected static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
