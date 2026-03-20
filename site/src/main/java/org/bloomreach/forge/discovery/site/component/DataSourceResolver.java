package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Non-HST helper that encapsulates data-source backfill and cache-probe logic for view components.
 * Extracted from {@link AbstractDiscoveryComponent} to be testable without a live HST container.
 *
 * <p>Receives all HST collaboration via constructor-injected functional interfaces so tests can
 * substitute fakes without subclassing {@code BaseHstComponent}.
 */
class DataSourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DataSourceResolver.class);

    private final Supplier<HstDiscoveryService> serviceSupplier;
    private final BiFunction<HstRequest, String, String> paramAccessor;

    DataSourceResolver(Supplier<HstDiscoveryService> serviceSupplier,
                       BiFunction<HstRequest, String, String> paramAccessor) {
        this.serviceSupplier = serviceSupplier;
        this.paramAccessor = paramAccessor;
    }

    // ── Backfill ──────────────────────────────────────────────────────────────

    Optional<SearchResponse> backfillSearchResponse(HstRequest request, String label) {
        Optional<SearchResponse> category = backfillCategoryResponse(request, label);
        if (category.isPresent()) return category;
        return backfillSearchBrowseResponse(request, label);
    }

    private Optional<SearchResponse> backfillCategoryResponse(HstRequest request, String label) {
        HstComponentConfiguration catConfig = findDataComponentConfig(request, label, DiscoveryCategoryComponent.class);
        if (catConfig == null) return Optional.empty();
        String docPath = catConfig.getParameter("document");
        DiscoveryCategoryBean doc = beanForPath(request, docPath, DiscoveryCategoryBean.class);
        String categoryId = doc != null && doc.getCategoryId() != null && !doc.getCategoryId().isBlank()
                ? doc.getCategoryId()
                : paramAccessor.apply(request, DiscoveryCategoryComponent.CAT_ID_PARAM);
        if (categoryId == null || categoryId.isBlank()) return Optional.empty();
        int pageSize = parseIntOrDefault(catConfig.getParameter("pageSize"), ConfigDefaults.DEFAULT_PAGE_SIZE);
        String sort = catConfig.getParameter("defaultSort");
        List<String> statsFields = parseStatsFields(catConfig.getParameter("statsFields"));
        String segment = catConfig.getParameter("segment");
        String efq = catConfig.getParameter("exclusionFilter");
        try {
            SearchResponse result = serviceSupplier.get()
                    .browse(request, categoryId, new SearchRequestOptions(pageSize, sort, null, label, statsFields, segment, efq));
            DiscoveryRequestCache.markCategoryBandPresent(request, label);
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Backfill for category label '{}' failed: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SearchResponse> backfillSearchBrowseResponse(HstRequest request, String label) {
        HstComponentConfiguration searchConfig = findDataComponentConfig(request, label, DiscoverySearchComponent.class);
        if (searchConfig == null) return Optional.empty();
        String query = paramAccessor.apply(request, "q");
        if (query == null || query.isBlank()) return Optional.empty();
        int pageSize = parseIntOrDefault(searchConfig.getParameter("pageSize"), ConfigDefaults.DEFAULT_PAGE_SIZE);
        String sort = searchConfig.getParameter("defaultSort");
        String catalogName = searchConfig.getParameter("catalogName");
        List<String> statsFields = parseStatsFields(searchConfig.getParameter("statsFields"));
        String segment = searchConfig.getParameter("segment");
        String efq = searchConfig.getParameter("exclusionFilter");
        try {
            SearchResponse result = serviceSupplier.get().search(request,
                    new SearchRequestOptions(pageSize, sort, blankToNull(catalogName), label, statsFields, segment, efq));
            DiscoveryRequestCache.markSearchBandPresent(request, label);
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("Backfill for search label '{}' failed: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<String> backfillProductDetailPid(HstRequest request, String label) {
        HstComponentConfiguration pdpConfig =
                findDataComponentConfig(request, label, DiscoveryProductDetailComponent.class);
        if (pdpConfig == null) return Optional.empty();

        // Stage 1: document picker path → bean → productId field
        String docPath = pdpConfig.getParameter("document");
        DiscoveryProductDetailBean doc = beanForPath(request, docPath, DiscoveryProductDetailBean.class);
        if (doc != null) {
            String pid = doc.getProductId();
            if (pid != null && !pid.isBlank()) return Optional.of(pid);
        }

        // Stage 2: URL param name stored in component config (default: "pid")
        String urlParam = pdpConfig.getParameter("productUrlParam");
        if (urlParam == null || urlParam.isBlank()) urlParam = "pid";
        String pid = paramAccessor.apply(request, urlParam);
        return (pid != null && !pid.isBlank()) ? Optional.of(pid) : Optional.empty();
    }

    // ── Component-tree traversal ───────────────────────────────────────────────

    HstComponentConfiguration findDataComponentConfig(HstRequest request, String label, Class<?> dataClass) {
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

    // ── Bean resolution ───────────────────────────────────────────────────────

    private static <T extends HippoBean> T beanForPath(HstRequest request, String path, Class<T> type) {
        if (path == null || path.isBlank()) return null;
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) return null;
        var siteBase = ctx.getSiteContentBaseBean();
        if (siteBase == null) return null;
        return siteBase.getBean(path, type);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static List<String> parseStatsFields(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
