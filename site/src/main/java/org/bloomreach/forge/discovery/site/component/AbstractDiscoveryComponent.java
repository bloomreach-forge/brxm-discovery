package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;

/**
 * Base class for Discovery HST components.
 * Provides typed service lookup, bean path resolution, and int-parsing utilities.
 */
public abstract class AbstractDiscoveryComponent extends BaseHstComponent {

    /**
     * Sets {@code editMode} on the FTL model once, before every component renders.
     * All brxdis templates use {@code (editMode!false)} so this guarantees a non-null
     * value without any per-component boilerplate.
     */
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        setModelAndAttribute(request, "editMode", isEditMode(request));
    }

    protected <T> T lookupService(Class<T> type) {
        return type.cast(HstServices.getComponentManager().getComponent(type.getName()));
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

    /**
     * Sets a CMS-preview diagnostic warning when the named data source is absent from cache.
     * No-op in live delivery mode.
     */
    protected void warnIfMissingDataSource(HstRequest request, boolean resultEmpty, boolean isCategory) {
        warnIfMissingDataSource(request, resultEmpty, isCategory, "default");
    }

    protected void warnIfMissingDataSource(HstRequest request, boolean resultEmpty,
                                            boolean isCategory, String band) {
        String source = isCategory ? "category" : "search";
        warnIfMissingDataSource(request, resultEmpty, source, band);
    }

    protected void warnIfMissingDataSource(HstRequest request, boolean resultEmpty,
                                            String dataSource, String band) {
        if (!resultEmpty) return;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx != null && ctx.isChannelManagerPreviewRequest()) {
            String capitalized = Character.toUpperCase(dataSource.charAt(0)) + dataSource.substring(1);
            request.setAttribute("brxdis_warning",
                    "No " + dataSource + " data for band '" + band + "'. " +
                    "Add a Discovery" + capitalized + "Component with bandName='" + band + "' to this page.");
        }
    }

    /**
     * On a PPR (isolated component re-render), walks the page's HST component
     * configuration tree to check whether a data component with the matching
     * band is actually configured on this page. Returns false on all non-PPR
     * requests (live delivery and full-page preview) — no performance impact.
     */
    protected boolean isBandConfiguredOnPage(HstRequest request, String band, Class<?> dataComponentClass) {
        if (!isIsolatedComponentRender(request)) return false;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) return false;
        var siteMapItem = ctx.getResolvedSiteMapItem();
        if (siteMapItem == null) return false;
        var pageConfig = siteMapItem.getHstComponentConfiguration();
        if (pageConfig == null) return false;
        String targetClassName = dataComponentClass.getName();
        return pageConfig.flattened()
                .filter(c -> targetClassName.equals(c.getComponentClassName()))
                .anyMatch(c -> band.equals(effectiveBandName(c.getParameter("bandName"))));
    }

    private static String effectiveBandName(String param) {
        return (param == null || param.isBlank()) ? "default" : param;
    }

    /**
     * Returns true when rendered inside the Channel Manager / Experience Editor.
     * Mirrors {@code CommonComponent} "editMode" semantics so customers who previously
     * extended CommonComponent find the same named helper here.
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
     * Sets a value on both the Page Model API ({@code setModel}) and the FTL attribute map
     * ({@code setAttribute}) in one call. Every Discovery component needs both; this
     * eliminates the mechanical two-line pattern.
     */
    protected void setModelAndAttribute(HstRequest request, String key, Object value) {
        request.setModel(key, value);
        request.setAttribute(key, value);
    }

    /**
     * Reads a public URL parameter and parses it as an int, falling back to
     * {@code defaultValue} if absent or unparseable.
     * Mirrors {@code CommonComponent.getAnyIntParameter(request, param, default)}.
     */
    protected int getPublicRequestParameterAsInt(HstRequest request, String param, int defaultValue) {
        return parseIntOrDefault(getPublicRequestParameter(request, param), defaultValue);
    }

    /**
     * Named accessor for the HST Discovery service bean.
     * Prefer this over the raw {@code lookupService(HstDiscoveryService.class)} call.
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
