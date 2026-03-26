package org.bloomreach.forge.discovery.site.component.constants;

/**
 * Typed string constants for every {@code request.setModel(key, value)} call made by
 * Discovery HST components. Use these instead of inline string literals to keep the
 * Java-side contract in sync with the TypeScript interfaces in the frontend.
 */
public final class DiscoveryModelKeys {

    private DiscoveryModelKeys() {}

    // Universal — set by AbstractDiscoveryComponent for every component
    public static final String EDIT_MODE          = "editMode";

    // Label wiring — producers set LABEL, view components set LABEL + LABEL_CONNECTED
    public static final String LABEL              = "label";
    public static final String LABEL_CONNECTED    = "labelConnected";

    // Search (DiscoverySearchComponent)
    public static final String QUERY              = "query";
    public static final String SUGGESTIONS_ENABLED = "suggestionsEnabled";
    public static final String SUGGEST_ONLY_MODE  = "suggestOnlyMode";
    public static final String RESULTS_PAGE       = "resultsPage";
    public static final String MIN_CHARS          = "minChars";
    public static final String DEBOUNCE_MS        = "debounceMs";
    public static final String PLACEHOLDER        = "placeholder";
    public static final String SEARCH_RESULT      = "searchResult";
    public static final String STATS              = "stats";
    public static final String DID_YOU_MEAN       = "didYouMean";
    public static final String AUTO_CORRECT_QUERY = "autoCorrectQuery";
    public static final String REDIRECT_URL       = "redirectUrl";
    public static final String REDIRECT_QUERY     = "redirectQuery";
    public static final String CAMPAIGN           = "campaign";
    public static final String AUTOSUGGEST_RESULT = "autosuggestResult";

    // Category (DiscoveryCategoryComponent)
    public static final String CATEGORY_ID        = "categoryId";
    public static final String CATEGORY_RESULT    = "categoryResult";
    public static final String DISPLAY_NAME       = "displayName";

    // Product Grid (DiscoveryProductGridComponent)
    public static final String PRODUCTS           = "products";
    public static final String PAGINATION         = "pagination";

    // Facets (DiscoveryFacetComponent)
    public static final String FACETS             = "facets";

    // Product Detail (DiscoveryProductDetailComponent)
    public static final String PRODUCT            = "product";
    public static final String PID                = "pid";

    // Recommendations (DiscoveryRecommendationComponent)
    public static final String WIDGET_ID          = "widgetId";
    public static final String WIDGET_TYPE        = "widgetType";
    public static final String WIDGET_RESULT_ID   = "widgetResultId";
    public static final String WIDGET_QUERY       = "widgetQuery";
    public static final String DATA_SOURCE        = "dataSource";
    public static final String SHOW_PRICE         = "showPrice";
    public static final String SHOW_DESCRIPTION   = "showDescription";

    // Category Highlight (DiscoveryCategoryHighlightComponent)
    public static final String CATEGORIES         = "categories";
    public static final String PREVIEW_PRODUCTS   = "previewProducts";
}
