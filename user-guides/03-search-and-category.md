# Search and Category Pages

> **New to the plugin?** See [00-quick-start.md](00-quick-start.md) for the end-to-end setup walkthrough - dependencies, credential setup, and a minimal working search page - before reading the detailed reference here.

## Overview

`DiscoveryResultsComponent` is the single component for both search results and category browse pages. It:

- Calls the Discovery Search or Browse API via CRISP
- Builds all navigation URLs server-side (facet toggles, pagination, sort) so templates receive ready-to-use `href` values
- Exposes data via `request.setModel()` (Page Model API / headless) and `request.setAttribute()` (Freemarker)
- Handles facets, pagination, sort, did-you-mean, and campaigns in one component

Set the **Data source** component parameter to `search` or `category` to switch modes.

Credentials are resolved from the shared Discovery config (`env → sys → JCR`) - see [02-discovery-config.md](02-discovery-config.md).

---

## HST configuration

### Register the component

The bundled `brxdis-results` template is auto-registered under `hst:default`. No manual `templates.yaml` entry is required unless you want to override the template.

**`pages.yaml`** - search page:

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:workspace/hst:pages:
      /search-page:
        jcr:primaryType: hst:component
        hst:referencecomponent: hst:abstractpages/base
        /main:
          jcr:primaryType: hst:component
          hst:template: search-layout
          /content:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /search-results:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryResultsComponent
              hst:template: brxdis-results
              hst:parameternames: [dataSource, pageSize]
              hst:parametervalues: [search, 12]
```

**Category page** - same component class, different `dataSource`:

```yaml
              hst:parameternames: [dataSource, pageSize]
              hst:parametervalues: [category, 24]
```

### Add sitemap entries

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

### Search mode (`dataSource=search`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `q` | String | - | Search query. Blank → empty state (no API call). |
| `page` | int | `0` | 0-indexed page number. |
| `sort` | String | component param | Sort expression, e.g. `price asc`. |
| `filter.{attribute}` | String (repeatable) | - | Facet filter, e.g. `filter.brand=Nike`. Multiple values for same field are OR'd. |

Example: `GET /site/search?q=shirt&page=1&sort=price+asc&filter.brand=Nike`

### Category mode (`dataSource=category`)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `category` | String | - | Discovery category ID. Falls back to this when no Category Document is configured on the component. |
| `page` | int | `0` | 0-indexed page number. |
| `sort` | String | component param | Sort expression. |
| `filter.{attribute}` | String (repeatable) | - | Facet filter. |

Example: `GET /site/category?category=sale&filter.brand=Adidas`

---

## Component parameters

Set in HST config via `@ParametersInfo` (visible in the Channel Manager component editor):

| Parameter | Group | Type | Default | Description |
|---|---|---|---|---|
| `dataSource` | Content | `search` \| `category` | `search` | Switches the component between search and category browse mode. |
| `document` | Content | JCR path | - | Category Document picker. Only used in `category` mode. |
| `pageSize` | Content | int | `12` | Results per page. |
| `showFacets` | Display | boolean | `true` | Render facet panel; includes `facetUrls`, `activeFacets`, `clearAllFiltersUrl` in models. |
| `showPagination` | Display | boolean | `true` | Include `pageUrls` in models. |
| `showSort` | Display | boolean | `true` | Include `sortUrl` in models. |
| `showDidYouMean` | Display | boolean | `true` | Include `didYouMean` suggestions (search only). |
| `autoRedirect` | Display | boolean | `false` | Server-side redirect on keyword redirect from Discovery. |
| `defaultSort` | Advanced | String | `""` | Default sort expression (URL `sort` overrides). Dropdown: price asc/desc, name asc/desc, sale_price asc/desc. |
| `catalogName` | Advanced | String | `""` | Non-product Discovery catalog to search (e.g. `blog_en`). Blank = product catalog. |
| `statsFields` | Advanced | String | `""` | Comma-separated fields to compute min/max/mean stats for (e.g. `price`). |
| `segment` | Advanced | String | `""` | Discovery visitor segment for personalised results. |
| `exclusionFilter` | Advanced | String | `""` | Server-side EFQ filter to exclude items from results. |

---

## Models set on the request

### Shared (both modes)

| Key | Type | Description |
|---|---|---|
| `dataSourceMode` | `String` | `"search"` or `"category"` |
| `products` | `List<ProductSummary>` | Matching products. `null` when query is blank or category not configured. |
| `pagination` | `PaginationModel` | total, page (0-based), pageSize, totalPages |
| `stats` | `Map<String,FieldStats>` | Min/max/mean per field (empty unless `statsFields` set) |
| `facets` | `Map<String,Facet>` | Facet map for label/count rendering. `null` when `showFacets=false`. |
| `facetUrls` | `Map<String,Map<String,String>>` | facetName → facetValue → toggle URL. `null` when `showFacets=false`. |
| `activeFacets` | `Map<String,List<String>>` | Currently active filter values per facet name. `null` when `showFacets=false`. |
| `clearAllFiltersUrl` | `String` | URL that clears all active filters. `null` when `showFacets=false`. |
| `pageUrls` | `Map<Integer,String>` | 0-indexed page → URL. Page 0 omits the page param. `null` when `showPagination=false`. |
| `sortUrl` | `String` | Base URL for sort switching; template appends `&sort=value`. `null` when `showSort=false`. |
| `campaign` | `Campaign` | Active Discovery campaign, or `null`. |

### Search-only models

| Key | Type | Description |
|---|---|---|
| `query` | `String` | Trimmed search term |
| `didYouMean` | `List<String>` | Did-you-mean suggestions. `null` when `showDidYouMean=false` or none returned. |
| `autoCorrectQuery` | `String` | Auto-corrected query from Discovery, or `null`. |
| `redirectUrl` | `String` | Keyword redirect URL from Discovery, or `null`. |
| `redirectQuery` | `String` | The query that triggered the redirect, or `null`. |

### Category-only models

| Key | Type | Description |
|---|---|---|
| `categoryId` | `String` | Resolved category ID (empty when not configured). |
| `displayName` | `String` | Category display name from Discovery. |

### `SearchResult` shape

```
SearchResult
├── long total
├── int page                     - 0-based
├── int pageSize
├── List<ProductSummary> products
│   ├── String id                - product ID (PID)
│   ├── String title
│   ├── String url
│   ├── String imageUrl
│   ├── BigDecimal price
│   ├── String currency
│   └── Map<String,Object> attributes  - brand, description, sale_price (when present)
└── Map<String,Facet> facets
    └── Facet
        ├── String name
        └── List<FacetValue> values
            ├── String name
            ├── long count
            └── boolean selected
```

Access extra attributes in FTL:

```ftl
${product.attributes()["brand"]!""}
<#if product.attributes()["sale_price"]??>${product.attributes()["sale_price"]?string("0.00")}</#if>
```

---

## Plugin FTL template

`brxdis-results.ftl` is the bundled template for `DiscoveryResultsComponent`. It renders the full page in one template - search form, facet panel, product grid, pagination, sort bar, and did-you-mean - using the pre-built URL models. No `servletRequest` access is needed.

```yaml
/brxdis-results:
  jcr:primaryType: hst:template
  hst:renderpath: webfile:/freemarker/brxdis/brxdis-results.ftl
```

Scoped CSS is injected via `<@hst.headContribution>` - no external stylesheet required.

---

## Server-side URL building

All navigation URLs (facet toggles, pagination, sort) are built server-side and passed as model values. In FTL, just use them directly:

```ftl
<#-- Facet toggle link -->
<a href="${facetUrls[facet.name][fv.name]!""}">${fv.name} (${fv.count})</a>

<#-- Page link -->
<a href="${pageUrls[p]!""}">${p + 1}</a>

<#-- Sort link -->
<a href="${sortUrl!""}&sort=${sortValue?url('UTF-8')}">${sortLabel}</a>

<#-- Clear all filters -->
<a href="${clearAllFiltersUrl!""}">Clear filters</a>
```

In React/SPA mode, the same URL strings come through in the JSON models - no URL manipulation in the browser.

---

## CMS preview diagnostics

When `DiscoveryResultsComponent` is in category mode but no category is configured (no document, no `?category=` URL param), it sets `brxdis_warning` in Channel Manager preview mode:

```ftl
<#if brxdis_warning??>
  <div style="border:2px dashed #f59e0b;padding:1rem;color:#92400e">⚠ ${brxdis_warning}</div>
</#if>
```

---

## Page Model API shape

For headless delivery, a search page produces:

```json
{
  "page": {
    "search-results": {
      "models": {
        "dataSourceMode": "search",
        "query": "shoes",
        "products": [{ "id": "p1", "title": "Running Shoes", "price": 89.99 }],
        "pagination": { "total": 42, "page": 0, "pageSize": 12, "totalPages": 4 },
        "facets": { "brand": { "name": "brand", "value": [{ "name": "Nike", "count": 12 }] } },
        "facetUrls": { "brand": { "Nike": "?q=shoes&filter.brand=Nike" } },
        "pageUrls": { "0": "?q=shoes", "1": "?q=shoes&page=1" },
        "sortUrl": "?q=shoes",
        "activeFacets": {},
        "clearAllFiltersUrl": "?q=shoes"
      }
    }
  }
}
```

> `page` in the JSON is the internal 0-indexed value. `page=0` is first page; `page=1` is second. The URL parameter `page` also uses 0-based indexing.

---

## `DiscoveryCategoryHighlightComponent`

For curated category navigation tiles (up to 4), use `DiscoveryCategoryHighlightComponent`. Each tile can optionally show product thumbnail previews sourced from Discovery.

See [03-search-and-category.md → Category Highlight](#discoverycategoryhighlightcomponent) or the separate component reference in the installation guide for details.

### Component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path | `""` | Paths to `brxdis:categoryDocument` nodes. Empty slots are skipped. |

### Models

| Key | Type | Description |
|---|---|---|
| `categories` | `List<DiscoveryCategoryBean>` | Resolved category beans in slot order. |
| `previewProducts` | `Map<String, List<ProductSummary>>` | Preview products keyed by `categoryId`. Served from a JVM-level cache (~5-min TTL). Empty map when all `productPreviewCount` values are `0`. |

### `DiscoveryCategoryBean` accessors

| Method | Returns | Description |
|---|---|---|
| `categoryId()` | `String` | Discovery category ID stored in `brxdis:categoryId`. |
| `displayName()` | `String` | Editorial label stored in `brxdis:displayName`. |
| `productPreviewCount()` | `int` | Number of preview products (0–4). |

### FTL access

```ftl
<#assign previewProds = (previewProducts!{})[cat.categoryId()!""]![]>
<#list previewProds as p>
  <img src="${p.imageUrl()!""}" alt="${(p.title()!"")?html}">
</#list>
```

---

## Error handling

`ConfigurationException` is thrown if required credentials (`accountId`, `domainKey`, `apiKey`) are missing. Discovery API errors are wrapped in `SearchException` (a `RuntimeException` subtype). When the global JCR config node is absent, the plugin falls back to env/sys + coded defaults.
