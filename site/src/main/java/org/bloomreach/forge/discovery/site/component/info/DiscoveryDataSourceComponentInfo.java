package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"dataSource"}, titleKey = "datasource.group")
})
public interface DiscoveryDataSourceComponentInfo {

    @Parameter(name = "dataSource", displayName = "Data source (search or category)", defaultValue = "search")
    String getDataSource();
}
