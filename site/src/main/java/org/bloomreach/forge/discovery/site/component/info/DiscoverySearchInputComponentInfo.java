package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

@FieldGroupList({
    @FieldGroup(
        value = {"placeholder", "resultsPage", "suggestionsEnabled", "suggestionsLimit", "minChars"},
        titleKey = "search.input.group"
    ),
    @FieldGroup(
        value = {"debounceMs"},
        titleKey = "search.input.advanced.group"
    )
})
public interface DiscoverySearchInputComponentInfo {

    @Parameter(name = "placeholder", displayName = "Input placeholder text", defaultValue = "Search...")
    String getPlaceholder();

    @Parameter(name = "resultsPage", displayName = "Results page path", defaultValue = "")
    String getResultsPage();

    @Parameter(name = "suggestionsEnabled", displayName = "Enable suggestions dropdown", defaultValue = "true")
    boolean isSuggestionsEnabled();

    @Parameter(name = "suggestionsLimit", displayName = "Max suggestions shown", defaultValue = "5")
    int getSuggestionsLimit();

    @Parameter(name = "minChars", displayName = "Min chars to trigger suggestions", defaultValue = "2")
    int getMinChars();

    @Parameter(name = "debounceMs", displayName = "Input delay (ms)", defaultValue = "250")
    int getDebounceMs();
}
