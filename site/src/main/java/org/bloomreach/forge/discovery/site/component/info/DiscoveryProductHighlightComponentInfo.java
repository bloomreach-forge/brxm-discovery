package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document1", "document2", "document3", "document4"},
                titleKey = "producthighlight.group")
})
public interface DiscoveryProductHighlightComponentInfo {

    @Parameter(name = "document1", displayName = "Product 1")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:productDetailDocument"},
        pickerInitialPath = "products",
        isRelative = true
    )
    String getDocument1();

    @Parameter(name = "document2", displayName = "Product 2")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:productDetailDocument"},
        pickerInitialPath = "products",
        isRelative = true
    )
    String getDocument2();

    @Parameter(name = "document3", displayName = "Product 3")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:productDetailDocument"},
        pickerInitialPath = "products",
        isRelative = true
    )
    String getDocument3();

    @Parameter(name = "document4", displayName = "Product 4")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:productDetailDocument"},
        pickerInitialPath = "products",
        isRelative = true
    )
    String getDocument4();
}
