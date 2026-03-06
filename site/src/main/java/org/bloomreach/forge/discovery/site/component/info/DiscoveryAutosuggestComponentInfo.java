package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"limit", "catalogViews"}, titleKey = "autosuggest.group")
})
public interface DiscoveryAutosuggestComponentInfo {

    @Parameter(name = "limit", displayName = "Max suggestions", defaultValue = "8")
    int getLimit();

    @Parameter(name = "catalogViews", displayName = "Catalog views (e.g. store:products_en)", defaultValue = "")
    String getCatalogViews();
}
