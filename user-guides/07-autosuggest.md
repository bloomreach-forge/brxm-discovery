# Autosuggest / Search Bar

## Overview

Autosuggest (typeahead) is built into `DiscoverySearchComponent` — no separate component is needed. When `suggestionsEnabled = true` (the default), the search component calls the Bloomreach Discovery Autosuggest API at `suggest.dxpapi.com` and exposes an `autosuggestResult` model alongside the main search result.

The search component is the single entry point for both full-text search and real-time typeahead suggestions. Suggestions are always fetched live (not cached) — they reflect the query state at the moment the request is made.

The `discoveryAutosuggestAPI` CRISP resource space is bootstrapped automatically by the plugin.

---

## Enabling suggestions on a search component

All suggestion parameters are set as component parameters in the HST config:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `suggestionsEnabled` | boolean | `true` | Enable autosuggest dropdown. Set `false` to disable entirely. |
| `suggestionsLimit` | int | `5` | Max suggestions shown per category (query, attribute, product). |
| `minChars` | int | `2` | Minimum characters before the suggestion dropdown is triggered. |
| `debounceMs` | int | `250` | Debounce delay in milliseconds — prevents API calls on every keystroke. |
| `placeholder` | String | `"Search..."` | Input placeholder text rendered by the FTL. |
| `resultsPage` | String | `""` | Path to redirect to for full results. Blank = render results on the current page. |

```yaml
/search:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
  hst:template: brxdis-search
  hst:parameternames: [suggestionsEnabled, suggestionsLimit, minChars, debounceMs, placeholder, resultsPage]
  hst:parametervalues: [true, 5, 2, 250, 'Search products...', '/search']
```

---

## Suggest-only mode

To use the component purely as a typeahead endpoint (e.g. called via AJAX before the user submits), add `brxdis_suggest=1` to the request:

```
GET /site/search?q=shi&brxdis_suggest=1
```

In suggest-only mode, the full Discovery search call is skipped — only `autosuggestResult` is populated. This is useful for wiring up a JavaScript typeahead that calls the page endpoint via `fetch`.

---

## Models set on the request

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term (empty string when blank) |
| `autosuggestResult` | `AutosuggestResult` | Suggestion payload (null when query is blank or `suggestionsEnabled=false`) |
| `suggestionsEnabled` | `boolean` | Whether suggestions are configured on this component |
| `minChars` | `int` | Minimum chars threshold (for FTL to render correctly) |
| `debounceMs` | `int` | Debounce delay (for FTL to configure the JS handler) |
| `placeholder` | `String` | Input placeholder text |
| `resultsPage` | `String` | Configured results page path |
| `suggestOnlyMode` | `boolean` | True when `brxdis_suggest=1` |

### `AutosuggestResult` model

```
AutosuggestResult
├── String originalQuery              — the query as echoed by Discovery
├── List<String> querySuggestions     — suggested search terms
├── List<AttributeSuggestion> attributeSuggestions
│   ├── String name                   — attribute name (e.g. "brand")
│   ├── String value                  — attribute value (e.g. "Nike")
│   └── String attributeType          — "text", "number", etc.
└── List<ProductSummary> productSuggestions
    ├── String id
    ├── String title
    ├── String imageUrl
    ├── BigDecimal price
    └── String currency
```

---

## Plugin FTL template

`brxdis-search.ftl` renders a search form with an inline suggestion panel. The panel contains three sections (query suggestions, attribute suggestions, product cards) and uses `suggestionsEnabled`, `minChars`, and `debounceMs` to configure the JavaScript behaviour. Scoped CSS is injected via `<@hst.headContribution>`.

Register and use it directly:

```yaml
/brxdis-search:
  jcr:primaryType: hst:template
  hst:renderpath: webfile:/freemarker/brxdis/brxdis-search.ftl
```

---

## Custom FTL template

```ftl
<#if autosuggestResult?? && query?has_content>

  <#-- Query suggestions -->
  <#if autosuggestResult.querySuggestions()?has_content>
  <ul class="suggestions">
    <#list autosuggestResult.querySuggestions() as term>
    <li><a href="?q=${term?url('UTF-8')}">${term}</a></li>
    </#list>
  </ul>
  </#if>

  <#-- Product suggestions -->
  <#if autosuggestResult.productSuggestions()?has_content>
  <div class="product-strip">
    <#list autosuggestResult.productSuggestions() as product>
    <a href="/product?pid=${product.id()!""?url('UTF-8')}" class="product-card">
      <#if product.imageUrl()?has_content>
        <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
      </#if>
      <span>${product.title()!"Untitled"}</span>
      <#if product.price()??><span>${product.currency()!""} ${product.price()?string("0.00")}</span></#if>
    </a>
    </#list>
  </div>
  </#if>

</#if>
```

---

## Page Model API shape

For a headless/SPA implementation, call the search endpoint in suggest-only mode via AJAX:

```
GET /site/search?q=shi&brxdis_suggest=1
```

Response shape:

```json
{
  "page": {
    "search": {
      "models": {
        "query": "shi",
        "autosuggestResult": {
          "originalQuery": "shi",
          "querySuggestions": ["shirts", "shipping"],
          "attributeSuggestions": [
            { "name": "brand", "value": "Nike", "attributeType": "text" }
          ],
          "productSuggestions": [
            { "id": "p1", "title": "Blue Shirt", "price": 29.99 }
          ]
        },
        "suggestOnlyMode": true
      }
    }
  }
}
```

---

## Catalog views

To restrict suggestions to a specific catalog view (e.g. a locale-specific product catalog), use the `catalogViews` parameter on `HstDiscoveryService.autosuggest()`. This is not exposed as a component parameter on `DiscoverySearchComponent` — wire it programmatically if needed in a custom component subclass.

---

## Error handling

Discovery API errors are wrapped in `SearchException` (a `RuntimeException`). A blank or null query returns a null `autosuggestResult` without calling the API — templates should guard with `<#if autosuggestResult??>`. Autosuggest failures do not affect the main search result.
