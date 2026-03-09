package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"connectTo"}, titleKey = "datasource.group")
})
public interface DiscoveryDataSourceComponentInfo {

    @Parameter(name = "connectTo", displayName = "Connects to label", defaultValue = "default")
    String getConnectTo();
}
