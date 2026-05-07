# Autosuggest / Search Bar

## Overview

`DiscoverySearchInputComponent` is a standalone search bar component. It:

- Renders a search input in any page zone (header, sidebar, or inline)
- Calls the Bloomreach Discovery Autosuggest API when `suggestionsEnabled=true`
- Exposes an `autosuggestResult` model for typeahead dropdown rendering
- Submits the search form to a configurable results page (which uses `DiscoverySearchGridComponent`)

Autosuggest results are fetched live via direct HTTP call (not cached) — they reflect the query state at the moment of the request.

---

## Enabling suggestions

All suggestion parameters are component parameters set in HST config:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `placeholder` | String | `"Search..."` | Input placeholder text rendered by the FTL. |
| `resultsPage` | String | `""` | Path to redirect to for full results. Blank = submit to current page. |
| `suggestionsEnabled` | boolean | `true` | Enable autosuggest dropdown. Set `false` to disable entirely. |
| `suggestionsLimit` | int | `5` | Max suggestions shown per category (query, attribute, product). |
| `minChars` | int | `2` | Minimum characters before the suggestion dropdown is triggered. |
| `debounceMs` | int | `250` | Debounce delay in milliseconds - prevents API calls on every keystroke. |

```yaml
/search-bar:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchInputComponent
  hst:template: brxdis-search-input
  hst:parameternames: [suggestionsEnabled, suggestionsLimit, minChars, debounceMs, placeholder, resultsPage]
  hst:parametervalues: [true, 5, 2, 250, 'Search products...', '/search']
```

---

## How it works with results

`DiscoverySearchInputComponent` and `DiscoverySearchGridComponent` are independent. The search bar submits a form to the results page (`resultsPage` param); the results page runs the full search.

Typical page layout:

```
Any page (e.g. homepage):
  └── search-bar: DiscoverySearchInputComponent (resultsPage=/search)

Search results page (/search):
  └── results: DiscoverySearchGridComponent (dataSource=search)
```

For pages where the search bar is on the same page as the results (e.g. a simple search page), set `resultsPage=""` - the form submits to the current page and `DiscoverySearchGridComponent` picks up the `q` parameter.

---

## Suggest-only mode

To use the component purely as a typeahead endpoint (e.g. called via AJAX before the user submits), add `brxdis_suggest=1` to the request:

> **Note:** `DiscoverySearchInputComponent` does not support suggest-only mode directly. For AJAX-based typeahead, call the Page Model API endpoint where your search bar component lives and read `autosuggestResult` from the models:

```
GET /site/resourceapi/<path-to-search-bar-page>?q=shi
```

Response includes `autosuggestResult` in the component models.

---

## Models set on the request

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term (empty string when blank) |
| `placeholder` | `String` | Input placeholder text |
| `resultsPage` | `String` | Configured results page path (empty = current page) |
| `suggestionsEnabled` | `boolean` | Whether suggestions are configured on this component |
| `minChars` | `int` | Minimum chars threshold |
| `debounceMs` | `int` | Debounce delay (for FTL to configure the JS handler) |
| `autosuggestResult` | `AutosuggestResult` | Suggestion payload (null when query is blank or `suggestionsEnabled=false`) |

### `AutosuggestResult` model

```
AutosuggestResult
├── String originalQuery              - the query as echoed by Discovery
├── List<String> querySuggestions     - suggested search terms
├── List<AttributeSuggestion> attributeSuggestions
│   ├── String name                   - attribute name (e.g. "brand")
│   ├── String value                  - attribute value (e.g. "Nike")
│   └── String attributeType          - "text", "number", etc.
└── List<ProductSummary> productSuggestions
    ├── String id
    ├── String title
    ├── String imageUrl
    ├── BigDecimal price
    └── String currency
```

---

## Plugin FTL template

`brxdis-search-input.ftl` renders a search form with an inline suggestion panel. The panel contains three sections (query suggestions, attribute suggestions, product cards) and uses `suggestionsEnabled`, `minChars`, and `debounceMs` to configure the JavaScript behaviour. Scoped CSS is injected via `<@hst.headContribution>`.

Register and use it directly:

```yaml
/brxdis-search-input:
  jcr:primaryType: hst:template
  hst:renderpath: webfile:/freemarker/brxdis/brxdis-search-input.ftl
```

---

## Custom FTL template

```ftl
<#if autosuggestResult?? && query?has_content>

  <#-- Query suggestions -->
  <#if autosuggestResult.querySuggestions()?has_content>
  <ul class="suggestions">
    <#list autosuggestResult.querySuggestions() as term>
    <li><a href="${(resultsPage!"")?has_content?then(resultsPage, "")}?q=${term?url('UTF-8')}">${term}</a></li>
    </#list>
  </ul>
  </#if>

  <#-- Product suggestions -->
  <#if autosuggestResult.productSuggestions()?has_content>
  <div class="product-strip">
    <#list autosuggestResult.productSuggestions() as product>
    <#assign _asPid = (product.id()!"")?url('UTF-8')>
    <#assign _asSlug = (product.title()!"")?lower_case?replace("[^a-z0-9]+", "-", "r")?replace("^-+|-+$", "", "r")>
    <a href="/product/${_asSlug}/p/${_asPid}" class="product-card">
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

For a headless/SPA implementation, call the page endpoint via AJAX:

```
GET /site/resourceapi/<search-bar-page>?q=shi
```

Response shape:

```json
{
  "page": {
    "search-bar": {
      "models": {
        "query": "shi",
        "placeholder": "Search products...",
        "suggestionsEnabled": true,
        "minChars": 2,
        "debounceMs": 250,
        "autosuggestResult": {
          "originalQuery": "shi",
          "querySuggestions": ["shirts", "shipping"],
          "attributeSuggestions": [
            { "name": "brand", "value": "Nike", "attributeType": "text" }
          ],
          "productSuggestions": [
            { "id": "p1", "title": "Blue Shirt", "price": 29.99 }
          ]
        }
      }
    }
  }
}
```

---

## Catalog views

To restrict suggestions to a specific catalog view (e.g. a locale-specific product catalog), use the `catalogViews` parameter on `HstDiscoveryService.autosuggest()`. This is not exposed as a component parameter on `DiscoverySearchInputComponent` - wire it programmatically in a custom component subclass if needed.

---

## Error handling

Discovery API errors are wrapped in `SearchException` (a `RuntimeException`). A blank or null query returns a null `autosuggestResult` without calling the API - templates should guard with `<#if autosuggestResult??>`. Autosuggest failures do not affect other components on the page.
