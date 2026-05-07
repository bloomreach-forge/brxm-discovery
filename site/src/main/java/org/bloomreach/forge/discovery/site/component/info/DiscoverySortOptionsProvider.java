package org.bloomreach.forge.discovery.site.component.info;

import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Supplies sort option values for the {@code defaultSort} dropdown in
 * {@link DiscoverySearchGridComponentInfo} and {@link DiscoveryCategoryGridComponentInfo}.
 * Reads from {@code brxdis:sortOptions} JCR property via the cached config provider;
 * falls back to {@link ConfigDefaults#DEFAULT_SORT_OPTIONS} when the provider is unavailable.
 *
 * <p>Each entry uses the format {@code "sort-expression=Display Label"} (e.g. {@code "price asc=Price: Low to High"}).
 * If no {@code =} is present, the full entry is used as both value and label.
 */
public class DiscoverySortOptionsProvider implements ValueListProvider {

    private static final Logger log = LoggerFactory.getLogger(DiscoverySortOptionsProvider.class);

    private final List<String> fixedEntries;

    /** No-arg constructor used by HST via reflection. */
    public DiscoverySortOptionsProvider() {
        this.fixedEntries = null;
    }

    /** Package-private constructor for tests - bypasses HstServices. */
    DiscoverySortOptionsProvider(List<String> entries) {
        this.fixedEntries = List.copyOf(entries);
    }

    @Override
    public List<String> getValues() {
        return entries().stream().map(DiscoverySortOptionsProvider::parseValue).toList();
    }

    @Override
    public String getDisplayValue(String value) {
        return getDisplayValue(value, null);
    }

    @Override
    public String getDisplayValue(String value, Locale locale) {
        return entries().stream()
                .filter(e -> parseValue(e).equals(value))
                .map(DiscoverySortOptionsProvider::parseLabel)
                .findFirst()
                .orElse(value);
    }

    private List<String> entries() {
        if (fixedEntries != null) {
            return fixedEntries;
        }
        try {
            DiscoveryConfigProvider provider = HstServices.getComponentManager()
                    .getComponent(DiscoveryConfigProvider.class.getName());
            if (provider != null) {
                return provider.get().schemaConfig().sortOptions();
            }
        } catch (Exception e) {
            log.warn("brxm-discovery: Could not resolve sort options from config - using defaults. Cause: {}", e.getMessage());
        }
        return ConfigDefaults.DEFAULT_SORT_OPTIONS;
    }

    public static String parseValue(String entry) {
        int eq = entry.indexOf('=');
        return eq < 0 ? entry.trim() : entry.substring(0, eq).trim();
    }

    public static String parseLabel(String entry) {
        int eq = entry.indexOf('=');
        return eq < 0 ? entry.trim() : entry.substring(eq + 1).trim();
    }
}
