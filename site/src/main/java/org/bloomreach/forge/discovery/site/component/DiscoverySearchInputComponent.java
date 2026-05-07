package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchInputComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

/**
 * Stateless search bar component for placement in any page zone (header, sidebar).
 * Handles autosuggest but delegates the actual search to {@link DiscoverySearchGridComponent}
 * on the results page. There is no data sharing with other components.
 */
@ParametersInfo(type = DiscoverySearchInputComponentInfo.class)
public class DiscoverySearchInputComponent extends AbstractDiscoveryComponent {

    @Override
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        DiscoverySearchInputComponentInfo info = getComponentParametersInfo(request);

        String query = getPublicRequestParameter(request, "q");
        query = query != null ? query.trim() : "";

        // Config models always set so the FTL input renders correctly before any query is typed
        request.setModel(DiscoveryModelKeys.PLACEHOLDER, info.getPlaceholder());
        request.setModel(DiscoveryModelKeys.RESULTS_PAGE, info.getResultsPage());
        request.setModel(DiscoveryModelKeys.SUGGESTIONS_ENABLED, info.isSuggestionsEnabled());
        request.setModel(DiscoveryModelKeys.MIN_CHARS, info.getMinChars());
        request.setModel(DiscoveryModelKeys.DEBOUNCE_MS, info.getDebounceMs());
        request.setModel(DiscoveryModelKeys.QUERY, query);

        DiscoveryChannelInfo channelInfo = getChannelInfo(request);
        boolean vsEnabled = channelInfo != null && channelInfo.getDiscoveryVisualSearchEnabled();
        request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_ENABLED, vsEnabled);
        if (vsEnabled) {
            String widgetId = resolveVisualSearchWidgetId(request, channelInfo);
            if (widgetId != null && !widgetId.isBlank()) {
                String vsBase = request.getContextPath() + "/_brxdis-api/visual-search/" + widgetId;
                request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_UPLOAD_URL, vsBase + "/upload");
                request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_WIDGET_ID, widgetId);
            }
        }

        if (!query.isBlank() && info.isSuggestionsEnabled()) {
            HstDiscoveryService svc = getDiscoveryService();
            AutosuggestResult suggestions = svc.autosuggest(request, query, info.getSuggestionsLimit());
            request.setModel(DiscoveryModelKeys.AUTOSUGGEST_RESULT, suggestions);
        } else {
            request.setModel(DiscoveryModelKeys.AUTOSUGGEST_RESULT, null);
        }
    }

}
