# Autosuggest / Typeahead

## Overview

`DiscoveryAutosuggestComponent` calls the Bloomreach Discovery Autosuggest API at `suggest.dxpapi.com` and exposes three suggestion lists for your template:

- **Query suggestions** — search term completions (e.g. "shi" → "shirts", "shipping")
- **Attribute suggestions** — brand / category completions (e.g. "Nike", "Running Shoes")
- **Product suggestions** — matching products with image, price, and title

Autosuggest responses are **not cached** — each request goes to the API immediately. This is intentional: typeahead is real-time and should not be served from a stale page-render cache.

The required `discoveryAutosuggestAPI` CRISP resource space is bootstrapped automatically by the plugin — no manual CRISP configuration required.

---

## HST configuration

### Register the component and template

```yaml
# components.yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:components:
      /autosuggest:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryAutosuggestComponent
        hst:template: brxdis-autosuggest
```

```yaml
# templates.yaml — use the plugin classpath template
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:templates:
      /brxdis-autosuggest:
        jcr:primaryType: hst:template
        hst:renderpath: classpath:/freemarker/brxdis/brxdis-autosuggest.ftl
```

### Sitemap entry

```yaml
/autosuggest:
  jcr:primaryType: hst:sitemapitem
  hst:componentconfigurationid: hst:pages/autosuggest-page
```

Or wire autosuggest as an AJAX endpoint: the Page Model API response at `/autosuggest?q=shi` returns `autosuggestResult` as JSON for SPA / JavaScript consumption.

---

## Request parameters

| Parameter | Type | Description |
|---|---|---|
| `q` | String | Partial query to complete. Blank = returns no results (no API call made). |

---

## Component parameters

Set in HST config via `@ParametersInfo` — visible in the Channel Manager component editor.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `limit` | int | `8` | Maximum number of suggestions per type to return. |
| `catalogViews` | String | `""` | Catalog view filter, e.g. `store:products_en`. Blank = all catalogs. |

---

## Request attributes and models

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term (empty string when blank) |
| `autosuggestResult` | `AutosuggestResult` | Suggestion payload (null when query is blank) |

Both `request.setAttribute()` and `request.setModel()` are set — the component works for FTL and Page Model API (headless) delivery.

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
    ├── String currency
    └── Map<String,Object> attributes  — brand, description, sale_price
```

---

## Plugin FTL template

`brxdis-autosuggest.ftl` renders a search form plus a suggestion panel when `autosuggestResult` is present. The panel contains three sections (query suggestions, attribute suggestions, product cards) and injects scoped CSS via `<@hst.headContribution>` — no external stylesheet required.

Register and use it directly:

```yaml
/brxdis-autosuggest:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-autosuggest.ftl
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

For a headless/SPA implementation, call the autosuggest endpoint via AJAX:

```
GET /site/autosuggest?q=shi
```

Response shape:

```json
{
  "page": {
    "autosuggest": {
      "models": {
        "query": "shi",
        "autosuggestResult": {
          "originalQuery": "shi",
          "querySuggestions": ["shirts", "shipping"],
          "attributeSuggestions": [
            { "name": "brand", "value": "Nike", "attributeType": "text" }
          ],
          "productSuggestions": [
            { "id": "p1", "title": "Blue Shirt", "price": 29.99, ... }
          ]
        }
      }
    }
  }
}
```

---

## Catalog views

Use `catalogViews` to restrict suggestions to a specific catalog view (e.g. a locale-specific product catalog):

```yaml
/autosuggest:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryAutosuggestComponent
  hst:template: brxdis-autosuggest
  hst:parameternames: [limit, catalogViews]
  hst:parametervalues: [8, 'store:products_en']
```

---

## Error handling

Discovery API errors are wrapped in `SearchException` (a `RuntimeException`). Blank or null query returns a null `autosuggestResult` without calling the API — templates should guard with `<#if autosuggestResult??>`.
