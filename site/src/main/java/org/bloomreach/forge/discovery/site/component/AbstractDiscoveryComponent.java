package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
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
     * On a PPR (isolated component re-render), walks the page's HST component
     * configuration tree to check whether a data component with the matching
     * band is actually configured on this page. Returns false on all non-PPR
     * requests (live delivery and full-page preview) — no performance impact.
     */
    protected boolean isBandConfiguredOnPage(HstRequest request, String label) {
        return isBandConfiguredOnPage(request, label, DiscoverySearchComponent.class)
            || isBandConfiguredOnPage(request, label, DiscoveryCategoryComponent.class);
    }

    protected boolean isBandConfiguredOnPage(HstRequest request, String label, Class<?> dataComponentClass) {
        return findDataComponentConfig(request, label, dataComponentClass) != null;
    }

    /**
     * In PPR (isolated component re-render) mode, the data-fetching component (Search/Category)
     * does not execute, leaving the request cache empty. This method locates the producer's
     * component config from the page tree, re-runs the API call with its stored parameters,
     * and returns the result so the view component can display live data in the editor.
     *
     * <p>Returns {@link Optional#empty()} when not in PPR mode, when no matching producer is
     * found, or when the fetch fails (exception is logged as WARN).
     */
    protected Optional<SearchResponse> backfillSearchResponse(HstRequest request, String label) {
        Optional<SearchResponse> category = backfillCategoryResponse(request, label);
        if (category.isPresent()) return category;
        return backfillSearchBrowseResponse(request, label);
    }

    private Optional<SearchResponse> backfillCategoryResponse(HstRequest request, String label) {
        HstComponentConfiguration catConfig = findDataComponentConfig(request, label, DiscoveryCategoryComponent.class);
        if (catConfig == null) return Optional.empty();
        String docPath = catConfig.getParameter("document");
        DiscoveryCategoryBean doc = getHippoBeanForPath(request, docPath, DiscoveryCategoryBean.class);
        String categoryId = doc != null && doc.getCategoryId() != null && !doc.getCategoryId().isBlank()
                ? doc.getCategoryId()
                : getPublicRequestParameter(request, DiscoveryCategoryComponent.CAT_ID_PARAM);
        if (categoryId == null || categoryId.isBlank()) return Optional.empty();
        int pageSize = parseIntOrDefault(catConfig.getParameter("pageSize"), ConfigDefaults.DEFAULT_PAGE_SIZE);
        String sort = catConfig.getParameter("defaultSort");
        List<String> statsFields = parseStatsFields(catConfig.getParameter("statsFields"));
        String segment = catConfig.getParameter("segment");
        String efq = catConfig.getParameter("exclusionFilter");
        try {
            return Optional.of(getDiscoveryService()
                    .browse(request, categoryId, pageSize, sort, label, statsFields, segment, efq));
        } catch (Exception e) {
            log.warn("PPR backfill for category label '{}' failed: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SearchResponse> backfillSearchBrowseResponse(HstRequest request, String label) {
        HstComponentConfiguration searchConfig = findDataComponentConfig(request, label, DiscoverySearchComponent.class);
        if (searchConfig == null) return Optional.empty();
        String query = getPublicRequestParameter(request, "q");
        if (query == null || query.isBlank()) return Optional.empty();
        int pageSize = parseIntOrDefault(searchConfig.getParameter("pageSize"), ConfigDefaults.DEFAULT_PAGE_SIZE);
        String sort = searchConfig.getParameter("defaultSort");
        String catalogName = searchConfig.getParameter("catalogName");
        List<String> statsFields = parseStatsFields(searchConfig.getParameter("statsFields"));
        String segment = searchConfig.getParameter("segment");
        String efq = searchConfig.getParameter("exclusionFilter");
        try {
            return Optional.of(getDiscoveryService().search(request, pageSize, sort,
                    blankToNull(catalogName), label, statsFields, segment, efq));
        } catch (Exception e) {
            log.warn("PPR backfill for search label '{}' failed: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * In PPR mode, the PDP component did not run, so the request cache has no product PID.
     * This method locates the {@link DiscoveryProductDetailComponent} config from the page tree,
     * extracts the PID via the same two-stage resolution the component itself uses:
     * <ol>
     *   <li>Document picker path → bean → {@code productId} field</li>
     *   <li>URL param name stored in component config (default: {@code "pid"})</li>
     * </ol>
     * Returns {@link Optional#empty()} when not in PPR mode, when no matching producer is found,
     * or when no PID can be resolved.
     */
    protected Optional<String> backfillProductDetailPid(HstRequest request, String label) {
        HstComponentConfiguration pdpConfig =
                findDataComponentConfig(request, label, DiscoveryProductDetailComponent.class);
        if (pdpConfig == null) return Optional.empty();

        // Stage 1: document picker path → bean → productId field
        String docPath = pdpConfig.getParameter("document");
        DiscoveryProductDetailBean doc =
                getHippoBeanForPath(request, docPath, DiscoveryProductDetailBean.class);
        if (doc != null) {
            String pid = doc.getProductId();
            if (pid != null && !pid.isBlank()) return Optional.of(pid);
        }

        // Stage 2: URL param name stored in component config (default: "pid")
        String urlParam = pdpConfig.getParameter("productUrlParam");
        if (urlParam == null || urlParam.isBlank()) urlParam = "pid";
        String pid = getPublicRequestParameter(request, urlParam);
        return (pid != null && !pid.isBlank()) ? Optional.of(pid) : Optional.empty();
    }

    private HstComponentConfiguration findDataComponentConfig(HstRequest request, String label, Class<?> dataClass) {
        if (!isIsolatedComponentRender(request)) return null;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) return null;
        var siteMapItem = ctx.getResolvedSiteMapItem();
        if (siteMapItem == null) return null;
        var pageConfig = siteMapItem.getHstComponentConfiguration();
        if (pageConfig == null) return null;
        String targetClassName = dataClass.getName();
        return pageConfig.flattened()
                .filter(c -> targetClassName.equals(c.getComponentClassName()))
                .filter(c -> label.equals(effectiveLabel(c.getParameter("label"))))
                .findFirst()
                .orElse(null);
    }

    private static String effectiveLabel(String param) {
        return (param == null || param.isBlank()) ? "default" : param;
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
     * Probes the request cache for search and category results, runs a PPR backfill
     * when in isolated-component-render mode, and sets {@code label} / {@code labelConnected}
     * on the model. Also emits a CMS-preview warning when no data source is wired.
     */
    protected DataSourceResult resolveDataSource(HstRequest request, String label) {
        Optional<SearchResponse> cached = DiscoveryRequestCache.getSearchResponse(request, label)
                .or(() -> DiscoveryRequestCache.getCategoryResponse(request, label));
        boolean backfilled = false;
        if (cached.isEmpty() && isIsolatedComponentRender(request)) {
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
        setModelAndAttribute(request, "label", label);
        setModelAndAttribute(request, "labelConnected", labelConnected);
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
