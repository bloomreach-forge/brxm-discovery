package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * Channel Manager parameters for the Discovery Product Grid - Category component.
 * Contains only category-browse-relevant parameters; search-specific fields
 * (did-you-mean, auto-redirect) are intentionally absent.
 */
@FieldGroupList({
    @FieldGroup(
        value = {"document", "pageSize", "defaultSort"},
        titleKey = "results.content.group"
    ),
    @FieldGroup(
        value = {"showFacets", "showPagination", "showSort"},
        titleKey = "results.display.group"
    )
})
public interface DiscoveryCategoryGridComponentInfo {

    @Parameter(name = "document", displayName = "Category Document")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:categoryDocument"},
        pickerInitialPath = "categories",
        isRelative = true
    )
    String getDocument();

    @Parameter(name = "pageSize", displayName = "Results per page", defaultValue = "12")
    int getPageSize();

    @Parameter(name = "defaultSort", displayName = "Default sort", defaultValue = "")
    @DropDownList({"", "price asc", "price desc", "name asc", "name desc", "sale_price asc", "sale_price desc"})
    String getDefaultSort();

    @Parameter(name = "showFacets", displayName = "Show facets panel", defaultValue = "true")
    boolean isShowFacets();

    @Parameter(name = "showPagination", displayName = "Show pagination", defaultValue = "true")
    boolean isShowPagination();

    @Parameter(name = "showSort", displayName = "Show sort options", defaultValue = "true")
    boolean isShowSort();
}
