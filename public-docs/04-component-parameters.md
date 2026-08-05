[Documentation home](README.md) > Component Parameters

# Component Parameters

Every component below is placed on a page through standard HST configuration and configured by editors through the parameter panel in the Channel Manager component editor. This page lists, for each component, its purpose, its parameters, and what it exposes to templates and the Page Model API.

For placement guidance and full page-composition examples, see the [Installation](02-installation.md) and [Configuration](03-configuration.md) pages. For the two components that use the v2 Pathways API, see [Recommendations & Visual Search](05-recommendations-and-visual-search.md).

**ON THIS PAGE**
- [Search Grid](#search-grid--discoverysearchgridcomponent)
- [Category Grid](#category-grid--discoverycategorygridcomponent)
- [Search Input](#search-input--discoverysearchinputcomponent)
- [Product Detail](#product-detail--discoveryproductdetailcomponent)
- [Product Highlight](#product-highlight--discoveryproducthighlightcomponent)
- [Category Highlight](#category-highlight--discoverycategoryhighlightcomponent)
- [Recommendation components](#recommendation-components)
- [Everything at a glance](#everything-at-a-glance)

---

## Search Grid — `DiscoverySearchGridComponent`

Keyword search results, including facets, pagination, sort, did-you-mean, auto-correct, keyword redirects, and image (visual) search.

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel for DiscoverySearchGridComponent, showing the Content, Display, and Advanced parameter groups.]**

| Parameter | Group | Type | Default | Description |
|---|---|---|---|---|
| `pageSize` | Content | int | `12` | Results per page. |
| `showFacets` | Display | boolean | `true` | Render the facet panel. |
| `showPagination` | Display | boolean | `true` | Render pagination controls. |
| `showSort` | Display | boolean | `true` | Render the sort control. |
| `showDidYouMean` | Display | boolean | `true` | Show did-you-mean suggestions. |
| `autoRedirect` | Display | boolean | `false` | Follow a Discovery keyword redirect automatically (server-side). |
| `defaultSort` | Advanced | String | `""` | Default sort expression; a URL `sort` parameter overrides it. |
| `catalogName` | Advanced | String | `""` | Non-product Discovery catalog to search (e.g. a content catalog). Blank searches the product catalog. |
| `statsFields` | Advanced | String | `""` | Comma-separated fields to compute min/max/mean statistics for. |
| `facetFields` | Advanced | String | `""` | Comma-separated facet fields to display; empty shows all facets Discovery returns. |
| `segment` | Advanced | String | `""` | Discovery visitor segment for personalized results. |
| `exclusionFilter` | Advanced | String | `""` | Server-side filter expression to exclude items from results. |

**Query parameters accepted:** `q` (search term), `page` (0-indexed), `sort`, and `filter.{attribute}` (repeatable facet filter).

**Key models:** `products`, `pagination`, `facets`, `facetUrls`, `activeFacets`, `clearAllFiltersUrl`, `sortUrl`, `didYouMean`, `autoCorrectQuery`, `redirectUrl`, `campaign`.

---

## Category Grid — `DiscoveryCategoryGridComponent`

Category browse, sharing the same facet/pagination/sort mechanics as search.

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel for DiscoveryCategoryGridComponent.]**

| Parameter | Group | Type | Default | Description |
|---|---|---|---|---|
| `document` | Content | JCR path (Category Document picker) | — | The category to browse. Only used in category mode; see [CMS Document Types & Pickers](06-document-types-and-pickers.md). |
| `pageSize` | Content | int | `12` | Results per page. |
| `showFacets` | Display | boolean | `true` | Render the facet panel. |
| `showPagination` | Display | boolean | `true` | Render pagination controls. |
| `showSort` | Display | boolean | `true` | Render the sort control. |
| `defaultSort` | Advanced | String | `""` | Default sort expression. |
| `statsFields` | Advanced | String | `""` | Comma-separated fields for min/max/mean statistics. |
| `facetFields` | Advanced | String | `""` | Comma-separated facet fields to display; empty shows all facets Discovery returns. |
| `segment` | Advanced | String | `""` | Discovery visitor segment. |
| `exclusionFilter` | Advanced | String | `""` | Server-side exclusion filter. |
| `categoryUrlParam` | Advanced | String | `cid` | URL parameter name for the category ID (path segment and query-string fallback). |

**Key models:** the same shared set as Search Grid, plus `categoryId`, `displayName`, and `stats`.

---

## Search Input — `DiscoverySearchInputComponent`

A standalone search bar with an autosuggest dropdown, placeable in any zone (header, sidebar, inline).

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel for DiscoverySearchInputComponent.]**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `placeholder` | String | `"Search..."` | Input placeholder text. |
| `resultsPage` | String | `""` | Path to redirect to for full results. Blank submits to the current page. |
| `suggestionsEnabled` | boolean | `true` | Enable the autosuggest dropdown. |
| `suggestionsLimit` | int | `5` | Maximum suggestions shown per category (query, attribute, product). |
| `minChars` | int | `2` | Minimum characters typed before suggestions are requested. |
| `debounceMs` | int | `250` | Debounce delay before firing an autosuggest request. |

The search bar is independent of the results page — it submits to whichever page runs `DiscoverySearchGridComponent`.

**Key models:** `query`, `autosuggestResult` (query, attribute, and product suggestions), `visualSearchEnabled`, `visualSearchUploadUrl` (see [Recommendations & Visual Search](05-recommendations-and-visual-search.md)).

---

## Product Detail — `DiscoveryProductDetailComponent`

Fetches a single product by ID for a product detail page.

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel for DiscoveryProductDetailComponent, with the product document picker field visible.]**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document` | JCR path (Product Detail Document picker) | — | **Required.** See [CMS Document Types & Pickers](06-document-types-and-pickers.md) for Pinned vs. Dynamic modes. |
| `productUrlParam` | String | `pid` | URL parameter name read in Dynamic mode. |

**Key models:** `product` (`null` if unresolved), `pid`, `document`, `editMode`.

---

## Product Highlight — `DiscoveryProductHighlightComponent`

Up to four hand-picked products for editorial merchandising slots (not algorithmic recommendations).

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel showing the four product document picker slots.]**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path (Product Detail Document picker) | `""` | Up to four products. Empty slots are skipped. |

**Key models:** `products` (always 4 entries; unfilled slots are `null`), `editMode`.

---

## Category Highlight — `DiscoveryCategoryHighlightComponent`

Up to four curated category navigation tiles, each with an optional live product-thumbnail preview.

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel showing the four category document picker slots.]**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path (Category Document picker) | `""` | Up to four categories. Empty slots are skipped. |

**Key models:** `categories`, `previewProducts` (keyed by category ID; populated only where the editor configured a product-preview count), `editMode`.

---

## Recommendation components

Four components share the same base parameters and each targets a different context. Full setup, including the v2 Pathways API and widget document types, is covered on [Recommendations & Visual Search](05-recommendations-and-visual-search.md).

| Component | Use when |
|---|---|
| `DiscoveryProductRecommendationComponent` | Recommendations keyed to a product ("similar items" on a product page) |
| `DiscoveryCategoryRecommendationComponent` | Recommendations keyed to a category ("trending in this category") |
| `DiscoveryGlobalRecommendationComponent` | Context-free global or personalized recommendations |
| `DiscoveryKeywordRecommendationComponent` | Recommendations driven by a search keyword |

> **[SCREENSHOT PLACEHOLDER: the Channel Manager parameter panel for a recommendation component, showing the widget document picker and the limit/showPrice/showDescription fields.]**

| Parameter | Group | Type | Default | Description |
|---|---|---|---|---|
| `document` | Recommendations | JCR path (Recommendation Document picker) | — | The widget configuration, authored through a 3-step wizard. |
| `limit` | Recommendations | int | `8` | Number of recommendations to request. |
| `showPrice` | Recommendations | boolean | `true` | Show price in the bundled template. |
| `showDescription` | Recommendations | boolean | `false` | Show description in the bundled template. |
| `productUrlParam` *(Product component only)* | Advanced | String | `pid` | URL parameter read in Dynamic mode. |
| `categoryUrlParam` *(Category component only)* | Advanced | String | `cid` | URL parameter read in Dynamic mode. |

**Key models:** `products`, `widgetId`.

---

## Everything at a glance

| Component | Template | Content type |
|---|---|---|
| `DiscoverySearchGridComponent` | `brxdis-results` | Keyword & visual search results |
| `DiscoveryCategoryGridComponent` | `brxdis-results` | Category browse |
| `DiscoverySearchInputComponent` | `brxdis-search-input` | Search bar + autosuggest |
| `DiscoveryProductRecommendationComponent` | `brxdis-recommendations-product` | Product-keyed recommendations |
| `DiscoveryCategoryRecommendationComponent` | `brxdis-recommendations-category` | Category-keyed recommendations |
| `DiscoveryGlobalRecommendationComponent` | `brxdis-recommendations-global` | Global / personalized recommendations |
| `DiscoveryKeywordRecommendationComponent` | `brxdis-recommendations-keyword` | Keyword-keyed recommendations |
| `DiscoveryProductDetailComponent` | `brxdis-product-detail` | Single product detail |
| `DiscoveryProductHighlightComponent` | `brxdis-product-highlight` | Curated product showcase |
| `DiscoveryCategoryHighlightComponent` | `brxdis-category-highlight` | Curated category navigation |

All bundled templates are registered automatically (see [Installation](02-installation.md)) and every component exposes its models through both Freemarker (`request.setAttribute()`) and the Page Model API (`request.setModel()`), so the same configuration serves a traditional site and a headless front end.

---

**Previous:** [Configuration](03-configuration.md) · **Next:** [Recommendations & Visual Search](05-recommendations-and-visual-search.md)
