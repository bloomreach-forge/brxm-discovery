# Search and Category Pages

> **New to the plugin?** See [00-quick-start.md](00-quick-start.md) for the end-to-end setup walkthrough — dependencies, bean scanning, credential setup, and a minimal working search page — before reading the detailed parameter reference here.

## Overview

`DiscoverySearchComponent` and `DiscoveryCategoryComponent` call the Discovery Search / Category Browse API via CRISP. They expose data via:
- `request.setModel()` — consumed by the Page Model API for headless/SPA delivery
- `request.setAttribute()` — consumed by Freemarker templates for server-side rendering

Both components cache results on the underlying servlet request (`DiscoveryRequestCache`) for the duration of the page render, so sibling view components (`DiscoveryProductGridComponent`, `DiscoveryFacetComponent`) can read the same result without triggering a second API call.

`DiscoverySearchComponent` also handles autosuggest inline — it calls the Autosuggest API when `suggestionsEnabled = true` and exposes `autosuggestResult` alongside the main search result. No separate autosuggest component is needed.

Credentials are resolved from the shared Discovery config (`env -> sys -> JCR`) — see [02-discovery-config.md](02-discovery-config.md).

---

## HST configuration

### 1. Register the components

In your project's HCM site config:

**`components.yaml`**

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:components:
      /search-page:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
        hst:template: brxdis-search
      /category-page:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryCategoryComponent
        hst:template: brxdis-category
```

The bundled `brxdis-search` and `brxdis-category` templates are auto-registered under `hst:default`. Add your own `templates.yaml` only if you want to override them.

### 2. Add sitemap entries

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:sitemap:
      /search:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/search-page
      /category:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/category-page
```

---

## Request parameters

### Search (`DiscoverySearchComponent`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `q` | String | `*` | Search query. Pass `*` for all-products browse. Blank → no API call made. |
| `page` | int | `1` | 1-indexed page number. |
| `pageSize` | int | component param | Results per page. |
| `sort` | String | component param | Sort expression, e.g. `price asc`. |
| `filter.{attribute}` | String (repeatable) | — | Facet filter, e.g. `filter.brand=Nike`. |
| `brxdis_suggest` | `1` | — | Suggest-only mode: skips full search, returns only autosuggest results. |

Example: `GET /site/search?q=shirt&page=2&pageSize=24&sort=price+asc&filter.brand=Nike`

### Search — component parameters

Set in HST config via `@ParametersInfo` (visible in the Channel Manager component editor):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageSize` | int | `12` | Results per page (URL `pageSize` overrides). |
| `defaultSort` | String | `""` | Default sort expression (URL `sort` overrides). |
| `catalogName` | String | `""` | Non-product catalog to search (e.g. `blog_en`). Blank = product catalog. |
| `label` | String | `"default"` | Data band — links this component to sibling view components on the same page. |
| `placeholder` | String | `"Search..."` | Input placeholder text rendered in the FTL form. |
| `resultsPage` | String | `""` | Path to redirect to for full results. Blank = renders results on current page. |
| `suggestionsEnabled` | boolean | `true` | Enable autosuggest dropdown. |
| `suggestionsLimit` | int | `5` | Max suggestions shown per type. |
| `minChars` | int | `2` | Minimum characters before suggestions are fetched. |
| `debounceMs` | int | `250` | Debounce delay in milliseconds for suggestion requests. |

### Category (`DiscoveryCategoryComponent`)

**Request parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `categoryId` | String | — | Discovery category ID. Required. |
| `page` | int | `1` | 1-indexed page number. |
| `pageSize` | int | component param | Results per page. URL param overrides component param. |
| `sort` | String | component param | Sort expression. URL param overrides component param. |
| `filter.{attribute}` | String (repeatable) | — | Facet filter, e.g. `filter.color=red`. |

**Component parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageSize` | int | `12` | Component-level default page size. |
| `defaultSort` | String | `""` | Component-level default sort. |
| `label` | String | `"default"` | Data band name. |

Example: `GET /site/category?categoryId=sale&page=1`

---

## Models set on the request

### `DiscoverySearchComponent`

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term |
| `searchResult` | `SearchResult` | Full Discovery response (null when query is blank or suggest-only) |
| `autosuggestResult` | `AutosuggestResult` | Suggestions (null when `suggestionsEnabled=false` or query is blank) |
| `label` | `String` | Band name (for FTL conditional logic) |
| `suggestionsEnabled` | `boolean` | Whether suggestions are enabled |
| `suggestOnlyMode` | `boolean` | True when `brxdis_suggest=1` |
| `resultsPage` | `String` | Configured results page path |
| `placeholder` | `String` | Input placeholder text |
| `minChars` | `int` | Min chars for suggestions |
| `debounceMs` | `int` | Debounce delay |

### `DiscoveryCategoryComponent`

| Key | Type | Description |
|---|---|---|
| `categoryId` | `String` | Resolved category ID |
| `categoryResult` | `SearchResult` | Full Discovery response |
| `label` | `String` | Band name |

### `SearchResult` model

```
SearchResult
├── long total                   — total matching products
├── int page                     — current page (zero-based internally)
├── int pageSize                 — results per page
├── List<ProductSummary> products
│   ├── String id                — product ID (PID)
│   ├── String title
│   ├── String url
│   ├── String imageUrl
│   ├── BigDecimal price
│   ├── String currency
│   └── Map<String,Object> attributes  — brand, description, sale_price (when present)
└── Map<String,Facet> facets
    └── Facet
        ├── String name
        └── List<FacetValue> values
            ├── String value
            ├── long count
            └── boolean selected
```

Access extra attributes in FTL:

```ftl
${product.attributes()["brand"]!""}
<#if product.attributes()["sale_price"]??>${product.attributes()["sale_price"]?string("0.00")}</#if>
```

### CMS preview diagnostics

When a view component (`DiscoveryProductGridComponent`, `DiscoveryFacetComponent`) is on a page without a matching data-fetching component on the same band, it sets `brxdis_warning` in Channel Manager preview mode:

```ftl
<#if brxdis_warning??>
  <div style="border:2px dashed #f59e0b;padding:1rem;color:#92400e">⚠ ${brxdis_warning}</div>
</#if>
```

---

## Plugin FTL templates

All `brxdis-*` templates are bundled inside `brxm-discovery-site` and auto-registered under `hst:default`. Each template injects scoped CSS via `<@hst.headContribution>` — no external stylesheet required.

**Monolithic templates** (`brxdis-search.ftl`, `brxdis-category.ftl`): single component renders the full page — search form/bar, suggestion dropdown, facet sidebar, product grid, and pagination all in one template. Use for quick integration.

**Composable templates** (`brxdis-product-grid.ftl`, `brxdis-facets.ftl`): one template per view component. Use with the composable wiring pattern below for independent slot placement.

---

## Rendering facets

Facet filter params use the `filter.{attribute}` convention (e.g. `filter.brand=Nike`). `QueryParamParser` reads all `filter.*` request params and converts them to `&fq=attribute:"value"` in the Discovery API call.

To build clickable filter links that preserve all other query params (including `q`, `sort`, page state), read `hstRequest.requestContext.servletRequest.parameterMap` directly — `<@hst.renderURL>` only carries HST-managed params and will strip `q`, `filter.*`, etc.

```ftl
<#assign sr = hstRequest.requestContext.servletRequest>

<#function addFilter key val>
  <#local p = []>
  <#list sr.parameterMap?keys as k>
    <#if k != "page">
      <#list sr.parameterMap[k] as v>
        <#local p = p + [k + "=" + v?url('UTF-8')]>
      </#list>
    </#if>
  </#list>
  <#local p = p + ["filter." + key?url('UTF-8') + "=" + val?url('UTF-8')]>
  <#return "?" + p?join("&")>
</#function>
```

> The plugin's `brxdis-facets.ftl` template provides a polished version of this pattern with active-filter chips and a dismiss control.

---

## Composable view components

For layouts where the product grid and facets are separate HST components on the same page, add the view components as siblings of the data-fetching component. Results flow via `DiscoveryRequestCache` — the Discovery API is called exactly once per page render.

`DiscoveryProductGridComponent` exposes both `products` (list) and `pagination` (`PaginationModel`) — no separate pagination component is needed.

**`pages.yaml`** (workspace):

```yaml
/search-page:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
  hst:template: brxdis-search
  /facets:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryFacetComponent
    hst:template: brxdis-facets
  /grid:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryProductGridComponent
    hst:template: brxdis-product-grid
```

**View component models:**

| View component | Model keys | Types | Description |
|---|---|---|---|
| `DiscoveryProductGridComponent` | `products`, `pagination`, `label`, `labelConnected` | `List<ProductSummary>`, `PaginationModel`, `String`, `boolean` | Product list + pagination |
| `DiscoveryFacetComponent` | `facets`, `label`, `labelConnected` | `Map<String,Facet>`, `String`, `boolean` | Facet navigation |

Both view components accept a `connectTo` component parameter that matches the `label` value on the data-fetching component. Auto-detection probes both search and category presence markers, so `dataSource` no longer needs to be specified.

---

## Named data bands

When a page has multiple search results sections (e.g. a search bar + a featured products section), assign distinct `label` values so view components know which data source to read:

```yaml
/featured:
  hst:componentclassname: …DiscoverySearchComponent
  hst:parameternames: [label, pageSize]
  hst:parametervalues: [featured, 4]
  /grid:
    hst:componentclassname: …DiscoveryProductGridComponent
    hst:parameternames: [connectTo]
    hst:parametervalues: [featured]
```

---

## Page Model API shape

For headless delivery, a search page with composable components produces:

```json
{
  "page": {
    "search": {
      "models": {
        "query": "shoes",
        "searchResult": { "total": 42, "products": [...], "facets": {...} },
        "autosuggestResult": { "querySuggestions": [...], "productSuggestions": [...] }
      }
    },
    "grid":   { "models": { "products": [...], "pagination": { "total": 42, "page": 0, "pageSize": 12, "totalPages": 4 } } },
    "facets": { "models": { "facets": { "brand": [...], "color": [...] } } }
  }
}
```

> `page` in the JSON is the internal 0-indexed value. The `page` URL parameter is 1-indexed — `QueryParamParser` converts `page=1` (URL) → `page: 0` (internal).

---

---

## `DiscoveryCategoryHighlightComponent`

Renders a grid of curated category tiles (up to 4) sourced from `brxdis:categoryDocument` pickers configured in the Channel Manager. Each tile optionally shows a row of product thumbnails below the category name, driven by the `brxdis:productPreviewCount` field on the document.

### Component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path | `""` | Paths to `brxdis:categoryDocument` nodes. Empty slots are skipped. |

### Models set on the request

| Key | Type | Description |
|---|---|---|
| `categories` | `List<DiscoveryCategoryBean>` | Resolved category beans in slot order. |
| `previewProducts` | `Map<String, List<ProductSummary>>` | Preview products keyed by `categoryId`. Empty map when all counts are zero or `HstDiscoveryService` is unavailable. Served from `CategoryPreviewCache` (~5-min TTL) — not re-fetched from Discovery on every request. |

### `DiscoveryCategoryBean` accessors

| Method | Returns | Description |
|---|---|---|
| `getCategoryId()` | `String` | The Discovery category ID stored in `brxdis:categoryId`. |
| `getDisplayName()` | `String` | Editor-facing label stored in `brxdis:displayName`. |
| `getProductPreviewCount()` | `int` | Number of preview products (0–4) stored in `brxdis:productPreviewCount`. Returns `0` if absent or unparseable. |

### FTL access

```ftl
<#assign previewProds = (previewProducts!{})[cat.categoryId!""]![]>
<#list previewProds as p>
  <img src="${p.imageUrl()!""}" alt="${(p.title()!"")?html}">
</#list>
```

The bundled `brxdis-category-highlight.ftl` template handles this rendering automatically.

### HST configuration

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:components:
      /category-highlight:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryCategoryHighlightComponent
        hst:template: brxdis-category-highlight
```

---

## Error handling

`ConfigurationException` is thrown if required credentials (`accountId`, `domainKey`, `apiKey`) are missing from all resolution sources. Discovery API errors are wrapped in `SearchException` (a `RuntimeException` subtype). When the global JCR config node is absent, the plugin falls back to env/sys + coded defaults rather than failing.
