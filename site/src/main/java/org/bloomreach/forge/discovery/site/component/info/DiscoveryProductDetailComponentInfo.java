package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document"},
                titleKey = "pdp.group"),
    @FieldGroup(value = {"productPidProperty", "productUrlParam"},
                titleKey = "pdp.advanced.group")
})
public interface DiscoveryProductDetailComponentInfo {

    @Parameter(name = "document", displayName = "Product Detail Document")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:productDetailDocument"},
        pickerInitialPath = "products",
        isRelative = true
    )
    String getDocument();

    @Parameter(name = "productPidProperty",
               displayName = "Product ID field name",
               defaultValue = "brxdis:pid")
    String getProductPidProperty();

    @Parameter(name = "productUrlParam", displayName = "URL parameter", defaultValue = "pid")
    String getProductUrlParam();

}
