package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * Channel Manager parameters for the Discovery Product Grid - Search component.
 * Contains only search-relevant parameters; category-specific fields (document picker,
 * category ID) are intentionally absent.
 */
@FieldGroupList({
    @FieldGroup(
        value = {"pageSize"},
        titleKey = "results.content.group"
    ),
    @FieldGroup(
        value = {"showFacets", "showPagination", "showSort", "showDidYouMean", "autoRedirect"},
        titleKey = "results.display.group"
    ),
    @FieldGroup(
        value = {"defaultSort", "catalogName", "statsFields", "segment", "exclusionFilter"},
        titleKey = "results.advanced.group"
    )
})
public interface DiscoverySearchGridComponentInfo {

    @Parameter(name = "pageSize", displayName = "Results per page", defaultValue = "12")
    int getPageSize();

    @Parameter(name = "defaultSort", displayName = "Default sort", defaultValue = "")
    @DropDownList({"", "price asc", "price desc", "name asc", "name desc", "sale_price asc", "sale_price desc"})
    String getDefaultSort();

    @Parameter(name = "catalogName", displayName = "Catalog", defaultValue = "")
    String getCatalogName();

    @Parameter(name = "statsFields", displayName = "Statistics fields", defaultValue = "")
    String getStatsFields();

    @Parameter(name = "segment", displayName = "Visitor segment", defaultValue = "")
    String getSegment();

    @Parameter(name = "exclusionFilter", displayName = "Exclusion filter", defaultValue = "")
    String getExclusionFilter();

    @Parameter(name = "showFacets", displayName = "Show facets panel", defaultValue = "true")
    boolean isShowFacets();

    @Parameter(name = "showPagination", displayName = "Show pagination", defaultValue = "true")
    boolean isShowPagination();

    @Parameter(name = "showSort", displayName = "Show sort options", defaultValue = "true")
    boolean isShowSort();

    @Parameter(name = "showDidYouMean", displayName = "Show did-you-mean suggestions", defaultValue = "true")
    boolean isShowDidYouMean();

    @Parameter(name = "autoRedirect", displayName = "Auto-redirect on corrections", defaultValue = "false")
    boolean isAutoRedirect();
}
