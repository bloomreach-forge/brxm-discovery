package org.bloomreach.forge.discovery.site.component;

import jakarta.servlet.http.HttpServletRequest;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryResultsComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Self-contained component for search results and category browse pages.
 * Replaces the three-component stack (Search/Category + ProductGrid + Facets) with a single
 * intent-aligned unit that owns its own data fetch and URL construction.
 *
 * <p>URL construction for facets, pagination, and sort happens in Java and is passed as
 * pre-built model maps. Templates render {@code <a href="${url}">} without accessing
 * {@code servletRequest} directly.
 *
 * <p>Two HST catalog entries reference this class with different {@code dataSource} defaults:
 * <ul>
 *   <li>{@code /search-results} — {@code dataSource=search}</li>
 *   <li>{@code /category-browse} — {@code dataSource=category}</li>
 * </ul>
 */
@ParametersInfo(type = DiscoveryResultsComponentInfo.class)
public class DiscoveryResultsComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryResultsComponent.class);
    static final String CAT_ID_PARAM = "category";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryResultsComponentInfo info = getComponentParametersInfo(request);
        String dataSource = info.getDataSource();
        Map<String, String[]> params = getServletParameters(request);

        if ("category".equals(dataSource)) {
            handleCategoryMode(request, response, info, params);
        } else {
            handleSearchMode(request, response, info, params);
        }
    }

    // ── Search mode ───────────────────────────────────────────────────────

    private void handleSearchMode(HstRequest request, HstResponse response,
            DiscoveryResultsComponentInfo info, Map<String, String[]> params) {
        String query = getPublicRequestParameter(request, "q");
        query = query != null ? query.trim() : "";

        request.setModel(DiscoveryModelKeys.QUERY, query);
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "search");

        if (query.isBlank()) {
            setEmptyState(request);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        SearchResponse searchResponse = svc.search(request, new SearchRequestOptions(
                info.getPageSize(), info.getDefaultSort(), blankToNull(info.getCatalogName()),
                parseStatsFields(info.getStatsFields()),
                info.getSegment(), info.getExclusionFilter()));

        if (info.isShowDidYouMean()) {
            request.setModel(DiscoveryModelKeys.DID_YOU_MEAN, searchResponse.metadata().didYouMean());
        }
        request.setModel(DiscoveryModelKeys.AUTO_CORRECT_QUERY, searchResponse.metadata().autoCorrectQuery());
        request.setModel(DiscoveryModelKeys.REDIRECT_URL, searchResponse.metadata().redirectUrl());
        request.setModel(DiscoveryModelKeys.REDIRECT_QUERY, searchResponse.metadata().redirectQuery());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, searchResponse.metadata().campaign());

        String redirectUrl = searchResponse.metadata().redirectUrl();
        if (info.isAutoRedirect() && redirectUrl != null && !redirectUrl.isBlank()) {
            try {
                response.sendRedirect(redirectUrl);
            } catch (IOException e) {
                log.warn("Keyword redirect to '{}' failed: {}", redirectUrl, e.getMessage());
            }
            return;
        }

        log.debug("Discovery search '{}' → {} results", query, searchResponse.result().total());
        populateResultModels(request, searchResponse, info, params);
    }

    // ── Category mode ─────────────────────────────────────────────────────

    private void handleCategoryMode(HstRequest request, HstResponse response,
            DiscoveryResultsComponentInfo info, Map<String, String[]> params) {
        DiscoveryCategoryBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryCategoryBean.class);

        String categoryId = document != null
                && document.getCategoryId() != null
                && !document.getCategoryId().isBlank()
                ? document.getCategoryId()
                : getPublicRequestParameter(request, CAT_ID_PARAM);

        request.setModel(DiscoveryModelKeys.CATEGORY_ID, categoryId != null ? categoryId : "");
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "category");

        if (categoryId == null || categoryId.isBlank()) {
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                        "No category configured. Attach a Category Document to this component " +
                        "or pass a '?category=' URL parameter.");
            }
            setEmptyState(request);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        SearchResponse browseResponse = svc.browse(request, categoryId, new SearchRequestOptions(
                info.getPageSize(), info.getDefaultSort(), null,
                parseStatsFields(info.getStatsFields()),
                info.getSegment(), info.getExclusionFilter()));

        request.setModel(DiscoveryModelKeys.DISPLAY_NAME, browseResponse.metadata().categoryName());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, browseResponse.metadata().campaign());
        request.setModel(DiscoveryModelKeys.STATS, browseResponse.metadata().stats());

        log.debug("Discovery category '{}' → {} results", categoryId, browseResponse.result().total());
        populateResultModels(request, browseResponse, info, params);
    }

    // ── Common result model population ───────────────────────────────────

    private void populateResultModels(HstRequest request, SearchResponse searchResponse,
            DiscoveryResultsComponentInfo info, Map<String, String[]> params) {
        SearchResult result = searchResponse.result();
        PaginationModel pagination = new PaginationModel(result.total(), result.page(), result.pageSize());

        request.setModel(DiscoveryModelKeys.PRODUCTS, result.products());
        request.setModel(DiscoveryModelKeys.PAGINATION, pagination);
        request.setModel(DiscoveryModelKeys.STATS, searchResponse.metadata().stats());

        if (info.isShowFacets()) {
            request.setModel(DiscoveryModelKeys.FACETS, result.facets());
            request.setModel(DiscoveryModelKeys.FACET_URLS, buildFacetToggleUrls(params, result.facets()));
            request.setModel(DiscoveryModelKeys.ACTIVE_FACETS, buildActiveFacetValues(params, result.facets()));
            request.setModel(DiscoveryModelKeys.CLEAR_FILTERS_URL, buildClearAllUrl(params));
        }

        if (info.isShowPagination()) {
            request.setModel(DiscoveryModelKeys.PAGE_URLS, buildPageUrls(params, pagination.totalPages()));
        }

        if (info.isShowSort()) {
            request.setModel(DiscoveryModelKeys.SORT_URL, buildSortUrl(params));
        }
    }

    private void setEmptyState(HstRequest request) {
        request.setModel(DiscoveryModelKeys.PRODUCTS, null);
        request.setModel(DiscoveryModelKeys.PAGINATION, new PaginationModel(0L, 0, 0));
    }

    /** Reads servlet parameters. Overridable in tests. */
    protected Map<String, String[]> getServletParameters(HstRequest request) {
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) return Map.of();
        HttpServletRequest sr = ctx.getServletRequest();
        if (sr == null) return Map.of();
        return sr.getParameterMap();
    }

    static Map<String, Map<String, String>> buildFacetToggleUrls(
            Map<String, String[]> params, Map<String, Facet> facets) {
        return DiscoveryUrlBuilder.buildFacetToggleUrls(params, facets);
    }

    static Map<String, List<String>> buildActiveFacetValues(
            Map<String, String[]> params, Map<String, Facet> facets) {
        return DiscoveryUrlBuilder.buildActiveFacetValues(params, facets);
    }

    static String buildClearAllUrl(Map<String, String[]> params) {
        return DiscoveryUrlBuilder.buildClearAllUrl(params);
    }

    static Map<String, String> buildPageUrls(Map<String, String[]> params, int totalPages) {
        return DiscoveryUrlBuilder.buildPageUrls(params, totalPages);
    }

    static String buildSortUrl(Map<String, String[]> params) {
        return DiscoveryUrlBuilder.buildSortUrl(params);
    }
}
