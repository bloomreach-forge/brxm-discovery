package org.bloomreach.forge.discovery.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "brxdis:recommendationDocument")
public class DiscoveryRecommendationBean extends HippoDocument {

    public String getWidgetId() {
        return getSingleProperty("brxdis:widgetId");
    }

    public String getDisplayName() {
        return getSingleProperty("brxdis:displayName");
    }
}
