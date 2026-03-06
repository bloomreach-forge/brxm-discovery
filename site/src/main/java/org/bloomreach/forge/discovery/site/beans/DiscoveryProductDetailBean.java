package org.bloomreach.forge.discovery.site.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType = "brxdis:productDetailDocument")
public class DiscoveryProductDetailBean extends HippoDocument {

    public String getProductId() {
        return getSingleProperty("brxdis:productId");
    }

    public String getDisplayName() {
        return getSingleProperty("brxdis:displayName");
    }
}
