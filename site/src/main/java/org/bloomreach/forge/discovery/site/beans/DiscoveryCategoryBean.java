package org.bloomreach.forge.discovery.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "brxdis:categoryDocument")
public class DiscoveryCategoryBean extends HippoDocument {

    public String getCategoryId() {
        return getSingleProperty("brxdis:categoryId");
    }

    public String getDisplayName() {
        return getSingleProperty("brxdis:displayName");
    }
}
