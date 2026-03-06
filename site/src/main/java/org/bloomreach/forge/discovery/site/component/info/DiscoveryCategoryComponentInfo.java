package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document", "pageSize", "defaultSort"}, titleKey = "category.group")
})
public interface DiscoveryCategoryComponentInfo {

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

    @Parameter(name = "defaultSort", displayName = "Default sort order", defaultValue = "")
    String getDefaultSort();
}
