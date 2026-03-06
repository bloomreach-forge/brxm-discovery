package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document", "contextProductId", "contextProductPidProperty", "limit",
                         "showPrice", "showDescription"},
                titleKey = "recommendation.group")
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

    @Parameter(name = "contextProductId",
               displayName = "Context product ID (optional override)",
               defaultValue = "")
    String getContextProductId();

    @Parameter(name = "contextProductPidProperty",
               displayName = "Product PID property name (advanced)",
               defaultValue = "brxdis:pid")
    String getContextProductPidProperty();

    @Parameter(name = "limit", displayName = "Maximum products", defaultValue = "8")
    int getLimit();

    @Parameter(name = "showPrice", displayName = "Show price", defaultValue = "true")
    boolean isShowPrice();

    @Parameter(name = "showDescription", displayName = "Show description", defaultValue = "false")
    boolean isShowDescription();
}
