package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"document", "limit", "showPrice", "showDescription"},
                titleKey = "recommendation.group")
})
public interface DiscoveryKeywordRecommendationComponentInfo {

    @Parameter(name = "document", displayName = "Recommendation Document")
    @JcrPath(
        pickerConfiguration = "cms-pickers/documents-only",
        pickerSelectableNodeTypes = {"brxdis:keywordRecommendationDocument"},
        pickerInitialPath = "widgets",
        isRelative = true
    )
    String getDocument();

    @Parameter(name = "limit", displayName = "Maximum products", defaultValue = "8")
    int getLimit();

    @Parameter(name = "showPrice", displayName = "Show price", defaultValue = "true")
    boolean isShowPrice();

    @Parameter(name = "showDescription", displayName = "Show description", defaultValue = "false")
    boolean isShowDescription();
}
