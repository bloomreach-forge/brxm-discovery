package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Base class for Discovery HST components.
 * Provides typed service lookup, bean path resolution, and int-parsing utilities.
 */
public abstract class AbstractDiscoveryComponent extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractDiscoveryComponent.class);
    private static final String DISCOVERY_ADDON_MODULE = "org.bloomreach.forge.discovery.site";

    private final DataSourceResolver dataSourceResolver =
            new DataSourceResolver(this::getDiscoveryService, this::getPublicRequestParameter);

    /**
     * Sets {@code editMode} on the FTL model once, before every component renders.
     * All brxdis templates use {@code (editMode!false)} so this guarantees a non-null
     * value without any per-component boilerplate.
     */
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        request.setModel("editMode", isEditMode(request));
    }

    protected <T> T lookupService(Class<T> type) {
        if (!HstServices.isAvailable() || HstServices.getComponentManager() == null) {
            throw new ConfigurationException("HST component manager is not available while resolving service: " + type.getName());
        }

        ComponentManager componentManager = HstServices.getComponentManager();
        T component = null;
        try {
            component = componentManager.getComponent(type);
        } catch (ComponentsException e) {
            log.debug("Typed HST lookup failed for {}. Falling back to bean-name lookup.", type.getName(), e);
        }
        if (component == null) {
            component = type.cast(componentManager.getComponent(type.getName()));
        }
        if (component == null) {
            component = lookupAddonModuleService(componentManager, type);
        }
        if (component == null) {
            throw new ConfigurationException("Required HST service is not available: " + type.getName());
        }
        return component;
    }

    private <T> T lookupAddonModuleService(ComponentManager componentManager, Class<T> type) {
        try {
            return componentManager.getComponent(type, DISCOVERY_ADDON_MODULE);
        } catch (ComponentsException e) {
            log.debug("Addon-module typed HST lookup failed for {} in {}. Falling back to bean-name lookup.",
                    type.getName(), DISCOVERY_ADDON_MODULE, e);
        }
        try {
            return type.cast(componentManager.getComponent(type.getName(), DISCOVERY_ADDON_MODULE));
        } catch (RuntimeException e) {
            log.debug("Addon-module bean-name HST lookup failed for {} in {}.",
                    type.getName(), DISCOVERY_ADDON_MODULE, e);
            return null;
        }
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
    protected void warnIfMissingDataSource(HstRequest request, boolean resultEmpty, String label) {
        if (!resultEmpty) return;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx != null && ctx.isChannelManagerPreviewRequest()) {
            request.setAttribute("brxdis_warning",
                "No data for label '" + label + "'. " +
                "Add a DiscoverySearchComponent or DiscoveryCategoryComponent " +
                "with label='" + label + "' to this page.");
        }
    }

    protected void warnIfMissingDataSource(HstRequest request, boolean resultEmpty,
                                            String dataSource, String label) {
        if (!resultEmpty) return;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx != null && ctx.isChannelManagerPreviewRequest()) {
            request.setAttribute("brxdis_warning",
                    "No " + dataSource + " data for label '" + label + "'. " +
                    "Add a Discovery" + capitalize(dataSource) + "Component with label='" + label + "' to this page.");
        }
    }

    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    protected static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Walks the page's HST component configuration tree to check whether a
     * data component with the matching band is actually configured on this page.
     * Uses the in-memory config model — no JCR reads.
     */
    protected boolean isBandConfiguredOnPage(HstRequest request, String label) {
        return isBandConfiguredOnPage(request, label, DiscoverySearchComponent.class)
            || isBandConfiguredOnPage(request, label, DiscoveryCategoryComponent.class);
    }

    protected boolean isBandConfiguredOnPage(HstRequest request, String label, Class<?> dataComponentClass) {
        return dataSourceResolver.findDataComponentConfig(request, label, dataComponentClass) != null;
    }

    /**
     * When the request cache is empty (consumer ran before producer, or PPR mode),
     * locates the producer's component config from the page tree, re-runs the API call
     * with its stored parameters, and populates the cache so subsequent components
     * get a cache hit.
     *
     * <p>Returns {@link Optional#empty()} when no matching producer is found on the page
     * or when the fetch fails (exception is logged as WARN).
     */
    protected Optional<SearchResponse> backfillSearchResponse(HstRequest request, String label) {
        return dataSourceResolver.backfillSearchResponse(request, label);
    }

    /**
     * When the PDP component has not yet run (consumer-before-producer or PPR mode),
     * locates the {@link DiscoveryProductDetailComponent} config from the page tree
     * and extracts the PID via two-stage resolution:
     * <ol>
     *   <li>Document picker path → bean → {@code productId} field</li>
     *   <li>URL param name stored in component config (default: {@code "pid"})</li>
     * </ol>
     * Returns {@link Optional#empty()} when no matching producer is found on the page
     * or when no PID can be resolved.
     */
    protected Optional<String> backfillProductDetailPid(HstRequest request, String label) {
        return dataSourceResolver.backfillProductDetailPid(request, label);
    }

    protected static List<String> parseStatsFields(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Holds the result of {@link #resolveDataSource}: the optional search/category result
     * and whether a matching data-source component is wired to the given label.
     */
    protected record DataSourceResult(Optional<SearchResult> result, boolean labelConnected) {}

    /**
     * Shared data-source resolution used by view components (ProductGrid, Facets).
     * Probes the request cache for search and category results, backfills from the
     * page's component tree on cache miss, and sets {@code label} / {@code labelConnected}
     * on the model. Also emits a CMS-preview warning when no data source is wired.
     */
    protected DataSourceResult resolveDataSource(HstRequest request, String label) {
        Optional<SearchResponse> cached = DiscoveryRequestCache.getSearchResponse(request, label)
                .or(() -> DiscoveryRequestCache.getCategoryResponse(request, label));
        boolean backfilled = false;
        if (cached.isEmpty()) {
            cached = backfillSearchResponse(request, label);
            backfilled = cached.isPresent();
        }
        Optional<SearchResult> result = cached.map(SearchResponse::result);

        boolean labelConnected = DiscoveryRequestCache.isSearchBandPresent(request, label)
                || DiscoveryRequestCache.isCategoryBandPresent(request, label);
        if (!labelConnected) {
            labelConnected = backfilled || isBandConfiguredOnPage(request, label);
        }
        warnIfMissingDataSource(request, !labelConnected, label);
        request.setModel("label", label);
        request.setModel("labelConnected", labelConnected);
        return new DataSourceResult(result, labelConnected);
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
