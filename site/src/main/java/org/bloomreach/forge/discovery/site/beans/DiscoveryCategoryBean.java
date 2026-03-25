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

    public int getProductPreviewCount() {
        String val = getSingleProperty("brxdis:productPreviewCount");
        if (val == null || val.isBlank()) return 0;
        try {
            return Math.min(4, Math.max(0, Integer.parseInt(val.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
