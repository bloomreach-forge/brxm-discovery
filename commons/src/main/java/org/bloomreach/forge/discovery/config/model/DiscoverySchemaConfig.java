package org.bloomreach.forge.discovery.config.model;

import org.bloomreach.forge.discovery.config.ConfigDefaults;

import java.util.List;

/**
 * Schema-related configuration: which fields to request, how to display sort options,
 * and which feed field names the CMS picker uses for display.
 */
public record DiscoverySchemaConfig(
        String defaultFieldList,
        List<String> sortOptions,
        String pickerIdField,
        String pickerTitleField,
        String pickerImageField,
        String pickerPriceField
) {
    public static final DiscoverySchemaConfig DEFAULT = new DiscoverySchemaConfig(
            ConfigDefaults.DEFAULT_FIELD_LIST,
            ConfigDefaults.DEFAULT_SORT_OPTIONS,
            ConfigDefaults.PICKER_ID_FIELD_DEFAULT,
            ConfigDefaults.PICKER_TITLE_FIELD_DEFAULT,
            ConfigDefaults.PICKER_IMAGE_FIELD_DEFAULT,
            ConfigDefaults.PICKER_PRICE_FIELD_DEFAULT
    );

    public DiscoverySchemaConfig withDefaultFieldList(String fieldList) {
        return new DiscoverySchemaConfig(fieldList, sortOptions, pickerIdField,
                pickerTitleField, pickerImageField, pickerPriceField);
    }

    public DiscoverySchemaConfig {
        defaultFieldList = defaultFieldList != null ? defaultFieldList : ConfigDefaults.DEFAULT_FIELD_LIST;
        sortOptions = (sortOptions != null && !sortOptions.isEmpty()) ? List.copyOf(sortOptions) : ConfigDefaults.DEFAULT_SORT_OPTIONS;
        pickerIdField    = pickerIdField    != null ? pickerIdField    : ConfigDefaults.PICKER_ID_FIELD_DEFAULT;
        pickerTitleField = pickerTitleField != null ? pickerTitleField : ConfigDefaults.PICKER_TITLE_FIELD_DEFAULT;
        pickerImageField = pickerImageField != null ? pickerImageField : ConfigDefaults.PICKER_IMAGE_FIELD_DEFAULT;
        pickerPriceField = pickerPriceField != null ? pickerPriceField : ConfigDefaults.PICKER_PRICE_FIELD_DEFAULT;
    }
}
