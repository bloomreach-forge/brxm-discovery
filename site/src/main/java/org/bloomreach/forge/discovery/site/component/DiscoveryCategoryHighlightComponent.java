package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryHighlightComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a grid of category navigation tiles sourced from up to 4 JCR
 * {@code brxdis:categoryDocument} pickers. Each tile links to the category
 * browse page via the {@code categoryId} field.
 */
@ParametersInfo(type = DiscoveryCategoryHighlightComponentInfo.class)
public class DiscoveryCategoryHighlightComponent extends AbstractDiscoveryComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryHighlightComponentInfo info = getComponentParametersInfo(request);

        List<DiscoveryCategoryBean> categories = new ArrayList<>();
        for (String path : new String[]{
                info.getDocument1(), info.getDocument2(),
                info.getDocument3(), info.getDocument4()}) {
            if (path != null && !path.isBlank()) {
                DiscoveryCategoryBean bean = getHippoBeanForPath(request, path, DiscoveryCategoryBean.class);
                if (bean != null) {
                    categories.add(bean);
                }
            }
        }

        if (categories.isEmpty() && isEditMode(request)) {
            request.setAttribute("brxdis_warning",
                    "No categories configured. Select Category Documents in component properties.");
        }

        setModelAndAttribute(request, "categories", categories);
    }
}
