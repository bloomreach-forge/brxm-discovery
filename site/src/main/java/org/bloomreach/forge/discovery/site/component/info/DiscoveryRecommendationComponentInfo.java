package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document", "limit", "showPrice", "showDescription"},
                titleKey = "recommendation.group"),
    @FieldGroup(value = {"useProductDetailContext", "contextProductId", "contextProductPidProperty"},
                titleKey = "recommendation.advanced.group")
})
public interface DiscoveryRecommendationComponentInfo {

    @Parameter(name = "document", displayName = "Recommendation Document")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:recommendationDocument"},
        pickerInitialPath = "widgets",
        isRelative = true
    )
    String getDocument();

    @Parameter(name = "contextProductPidProperty",
               displayName = "Product ID field name",
               defaultValue = "brxdis:pid")
    String getContextProductPidProperty();

    @Parameter(name = "limit", displayName = "Maximum products", defaultValue = "8")
    int getLimit();

    @Parameter(name = "showPrice", displayName = "Show price", defaultValue = "true")
    boolean isShowPrice();

    @Parameter(name = "showDescription", displayName = "Show description", defaultValue = "false")
    boolean isShowDescription();

    @Parameter(name = "useProductDetailContext",
               displayName = "Link to Product Detail on page",
               defaultValue = "false")
    boolean isUseProductDetailContext();

    @Parameter(name = "contextProductId",
            displayName = "Product ID override",
            defaultValue = "")
    String getContextProductId();
}
