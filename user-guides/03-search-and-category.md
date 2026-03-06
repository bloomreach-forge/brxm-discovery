# Search and Category Pages

## Overview

`DiscoverySearchComponent` and `DiscoveryCategoryComponent` extend `AbstractDiscoveryComponent` and call the Discovery Search / Category Browse API via CRISP. They expose data via:
- `request.setModel()` — consumed by the Page Model API for headless/SPA delivery
- `request.setAttribute()` — consumed by Freemarker templates for server-side rendering

Both components cache config and API results on the underlying servlet request (`DiscoveryRequestCache` uses `request.getRequestContext().getServletRequest()` for cross-component visibility) for the duration of the page render, so sibling view components (`DiscoveryProductGridComponent`, `DiscoveryFacetComponent`, `DiscoveryPaginationComponent`) can read the same result without triggering a second API call.

Credentials are resolved from the `discoveryConfigPath` mount parameter — see [02-discovery-config.md](02-discovery-config.md).

---

## HST configuration

### 1. Register the components and templates

In your project's HCM site config (typically `repository-data/site/src/main/resources/hcm-config/hst/configurations/<your-site>/`):

**`components.yaml`**

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:components:
      /search-page:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
        hst:template: search-page
      /category-page:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryCategoryComponent
        hst:template: category-page
```

**`templates.yaml`**

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:templates:
      /search-page:
        jcr:primaryType: hst:template
        hst:renderpath: 'webfile:/ft/search.ftl'
      /category-page:
        jcr:primaryType: hst:template
        hst:renderpath: 'webfile:/ft/category.ftl'
```

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
| `q` | String | `*` | Search query. Pass `*` for all-products browse. |
| `page` | int | `1` | 1-indexed page number (`page=1` is the first page). |
| `pageSize` | int | `brxdis:defaultPageSize` | Results per page. |
| `sort` | String | `brxdis:defaultSort` | Sort expression, e.g. `price asc`. |
| `filter.{attribute}` | String (repeatable) | — | Facet filter, e.g. `filter.brand=Nike`. May be repeated. |

Example: `GET /site/search?q=shirt&page=2&pageSize=24&sort=price+asc&filter.brand=Nike`

### Category (`DiscoveryCategoryComponent`)

**Request parameters** (query string):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `categoryId` | String | — | Discovery category ID. Required. |
| `page` | int | `1` | 1-indexed page number (`page=1` is the first page). |
| `pageSize` | int | component param → `brxdis:defaultPageSize` | Results per page. URL param overrides component param. |
| `sort` | String | component param → `brxdis:defaultSort` | Sort expression. URL param overrides component param. |
| `filter.{attribute}` | String (repeatable) | — | Facet filter, e.g. `filter.color=red`. May be repeated. |

**Component parameters** (set in HST config, not query string):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageSize` | int | `12` | Component-level default page size. |
| `defaultSort` | String | `""` | Component-level default sort. |

Example: `GET /site/category?categoryId=sale&page=1`

### `DiscoverySearchComponent` — component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `pageSize` | int | `12` | Results per page (URL `pageSize` overrides). |
| `defaultSort` | String | `""` | Default sort expression (URL `sort` overrides). |
| `catalogName` | String | `""` | Non-product catalog to search (e.g. `blog_en`). Blank = product catalog. |

Use `catalogName` to search content catalogs (blog articles, FAQs) indexed in Discovery alongside the product catalog.

---

## Request attributes and models

Both components set attributes (`request.setAttribute`) and Page Model models (`request.setModel`):

**`DiscoverySearchComponent`**

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term (empty string when blank) |
| `searchResult` | `SearchResult` | Full Discovery response (null when query is blank) |

**`DiscoveryCategoryComponent`**

| Key | Type | Description |
|---|---|---|
| `categoryId` | `String` | Resolved category ID |
| `categoryResult` | `SearchResult` | Full Discovery response |

To render products, facets, and pagination, use the composable view components (see [Composable view components](#composable-view-components)) or the plugin's built-in monolithic FTL templates (see [Plugin FTL templates](#plugin-ftl-templates)).

### `SearchResult` model

```
SearchResult
├── long total                   — total matching products
├── int page                     — current page (zero-based)
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

The `attributes` map is populated from Discovery's response fields. Standard keys: `brand` (String), `description` (String), `sale_price` (BigDecimal). Access in FTL:

```ftl
${product.attributes()["brand"]!""}
<#if product.attributes()["sale_price"]??>${product.attributes()["sale_price"]?string("0.00")}</#if>
```

### CMS preview diagnostics

When a view component (`DiscoveryProductGridComponent`, `DiscoveryFacetComponent`, `DiscoveryPaginationComponent`) is on a page without a corresponding data-fetching component, it sets `brxdis_warning` on the request in Channel Manager preview mode. Display it in your FTL to give editors actionable feedback:

```ftl
<#if brxdis_warning??>
  <div style="border:2px dashed #f59e0b;padding:1rem;color:#92400e">⚠ ${brxdis_warning}</div>
</#if>
```

---

## Plugin FTL templates

The site JAR ships ready-to-use Freemarker templates on the classpath. Register them in your `templates.yaml` and point your component's `hst:template` at them:

```yaml
/brxdis-search:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-search.ftl
/brxdis-category:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-category.ftl
/brxdis-product-grid:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-product-grid.ftl
/brxdis-facets:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-facets.ftl
/brxdis-pagination:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-pagination.ftl
```

Each template injects scoped CSS via `<@hst.headContribution keyHint="...">` — no external stylesheet required.

**Monolithic templates** (`brxdis-search.ftl`, `brxdis-category.ftl`): single component renders the full page — search form, facet sidebar, product grid, and pagination all in one template. Use for quick integration.

**Composable templates** (`brxdis-product-grid.ftl`, `brxdis-facets.ftl`, `brxdis-pagination.ftl`): one template per view component. Use with the composable wiring pattern (see [Composable view components](#composable-view-components)) for independent slot placement.

---

## Rendering facets

Facet filter params use the `filter.{attribute}` convention (e.g. `filter.brand=Nike`). `QueryParamParser` reads all `filter.*` request params and converts them to `&fq=attribute:"value"` in the Discovery API call.

To build clickable filter links that add or remove a filter while preserving all other query params (including `q`, `sort`, page state), read `hstRequest.requestContext.servletRequest.parameterMap` directly — `<@hst.renderURL>` only carries HST-managed params and will strip `q`, `filter.*`, etc.

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

<#function removeFilter key val>
  <#local p = []>
  <#list sr.parameterMap?keys as k>
    <#if k != "page">
      <#list sr.parameterMap[k] as v>
        <#if !(k == "filter." + key && v == val)>
          <#local p = p + [k + "=" + v?url('UTF-8')]>
        </#if>
      </#list>
    </#if>
  </#list>
  <#return "?" + p?join("&")>
</#function>

<#if facets?has_content>
  <#list facets?values as facet>
  <div class="facet">
    <h4>${facet.name()}</h4>
    <ul>
      <#list facet.values() as fv>
      <#assign isActive = (sr.getParameter("filter." + facet.name()) == fv.value())>
      <li>
        <#if isActive>
          <a href="${removeFilter(facet.name(), fv.value())}">[x] ${fv.value()} (${fv.count()})</a>
        <#else>
          <a href="${addFilter(facet.name(), fv.value())}">${fv.value()} (${fv.count()})</a>
        </#if>
      </li>
      </#list>
    </ul>
  </div>
  </#list>
</#if>
```

> The plugin's `brxdis-facets.ftl` template (see [Plugin FTL templates](#plugin-ftl-templates)) provides a polished version of this pattern with active-filter chips and a dismiss control.

---

## Composable view components

For layouts where product grid, facets, and pagination are separate HST components on the same page, add the view components as siblings. The data-fetching parent (`DiscoverySearchComponent`) stores results in `DiscoveryRequestCache` on the servlet request; sibling view components read from the same cache — the Discovery API is called exactly once per page render.

**`pages.yaml`** (workspace):

```yaml
/search-page:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
  hst:template: my-search-form
  /facets:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryFacetComponent
    hst:template: brxdis-facets
  /grid:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryProductGridComponent
    hst:template: brxdis-product-grid
  /pagination:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryPaginationComponent
    hst:template: brxdis-pagination
```

**`templates.yaml`** — register the plugin classpath templates once:

```yaml
/brxdis-product-grid:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-product-grid.ftl
/brxdis-facets:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-facets.ftl
/brxdis-pagination:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-pagination.ftl
```

All three view components accept a `dataSource` component parameter (`search` or `category`).

| View component | `setModel` key | Type | Description |
|---|---|---|---|
| `DiscoveryProductGridComponent` | `products` | `List<ProductSummary>` | Product list only |
| `DiscoveryFacetComponent` | `facets` | `Map<String,Facet>` | Facet navigation only |
| `DiscoveryPaginationComponent` | `pagination` | `PaginationModel` | `total`, `page`, `pageSize`, `totalPages` |

---

## Page Model API shape

For headless delivery, the Page Model API renders each component's models. A search page with all view components produces:

```json
{
  "page": {
    "search": {
      "models": {
        "query": "shoes",
        "searchResult": { "total": 42, "products": [...], "facets": {...} }
      }
    },
    "grid":       { "models": { "products": [...] } },
    "facets":     { "models": { "facets": { "brand": [...], "color": [...] } } },
    "pagination": { "models": { "pagination": { "total": 42, "page": 0, "pageSize": 12, "totalPages": 4 } } }
  }
}
```

> `page` in the JSON is the internal 0-indexed value. The `page` URL parameter is 1-indexed — `QueryParamParser` converts `page=1` (URL) → `page: 0` (internal).

---

## Error handling

Both components throw `HstComponentException` if:
- The JCR session cannot be obtained

`ConfigurationException` is thrown if required credentials (`accountId`, `domainKey`, `apiKey`) are missing from all resolution sources. Discovery API errors are wrapped in `SearchException` (a `RuntimeException` subtype). When the `discoveryConfigPath` mount parameter is missing or the JCR node is absent, the plugin falls back to env/sys + coded defaults rather than failing.
