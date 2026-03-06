package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"pageSize", "defaultSort", "catalogName", "bandName"}, titleKey = "search.group")
})
public interface DiscoverySearchComponentInfo {

    @Parameter(name = "pageSize", displayName = "Results per page", defaultValue = "12")
    int getPageSize();

    @Parameter(name = "defaultSort", displayName = "Default sort order", defaultValue = "")
    String getDefaultSort();

    @Parameter(name = "catalogName", displayName = "Catalog name (blank = products)", defaultValue = "")
    String getCatalogName();

    @Parameter(name = "bandName", displayName = "Data band name", defaultValue = "default")
    String getBandName();
}
