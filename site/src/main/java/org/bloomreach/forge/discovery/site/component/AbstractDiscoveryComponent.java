package org.bloomreach.forge.discovery.site.component;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;

/**
 * Base class for Discovery HST components.
 * Provides typed service lookup, bean path resolution, and int-parsing utilities.
 */
public abstract class AbstractDiscoveryComponent extends BaseHstComponent {

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
        if (!resultEmpty) return;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx != null && ctx.isChannelManagerPreviewRequest()) {
            String source = isCategory ? "Category" : "Search";
            request.setAttribute("brxdis_warning",
                    "No " + source.toLowerCase() + " data in cache. " +
                    "Add a Discovery" + source + "Component to this page.");
        }
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
