package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(value = {"pageSize", "defaultSort", "catalogName", "bandName"}, titleKey = "search.group"),
    @FieldGroup(value = {"placeholder", "resultsPage"}, titleKey = "search.bar.group"),
    @FieldGroup(value = {"suggestionsEnabled", "suggestionsLimit", "minChars", "debounceMs"}, titleKey = "search.suggestions.group")
})
public interface DiscoverySearchComponentInfo {

    @Parameter(name = "pageSize", displayName = "Results per page", defaultValue = "12")
    int getPageSize();

    @Parameter(name = "defaultSort", displayName = "Default sort order", defaultValue = "")
    String getDefaultSort();

    @Parameter(name = "catalogName", displayName = "Catalog name (blank = products)", defaultValue = "")
    String getCatalogName();

    @Parameter(name = "bandName", displayName = "Data band name", defaultValue = "default")
    String getBandName();

    @Parameter(name = "placeholder", displayName = "Input placeholder text", defaultValue = "Search...")
    String getPlaceholder();

    @Parameter(name = "resultsPage", displayName = "Search results page path (blank = current page)", defaultValue = "")
    String getResultsPage();

    @Parameter(name = "suggestionsEnabled", displayName = "Enable suggestions dropdown", defaultValue = "true")
    boolean isSuggestionsEnabled();

    @Parameter(name = "suggestionsLimit", displayName = "Max suggestions shown", defaultValue = "5")
    int getSuggestionsLimit();

    @Parameter(name = "minChars", displayName = "Min chars to trigger suggestions", defaultValue = "2")
    int getMinChars();

    @Parameter(name = "debounceMs", displayName = "Debounce delay (ms)", defaultValue = "250")
    int getDebounceMs();
}
