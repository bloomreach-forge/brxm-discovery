# Model Contract

This is the authoritative reference for every key set by Discovery HST components via `request.setModel()`. All key strings are constants in `DiscoveryModelKeys`. Frontend TypeScript types map directly to these keys.

---

## Model keys by component

### Universal — `AbstractDiscoveryComponent`

Set on every component render, regardless of type.

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `editMode` | `Boolean` | `boolean` | `true` when rendering inside Channel Manager or Experience Editor. Use to show empty-state placeholders. |

---

### `DiscoverySearchInputComponent`

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `query` | `String` | `string` | Current search term from `?q=`. |
| `placeholder` | `String` | `string` | Configured placeholder text. |
| `resultsPage` | `String` | `string` | URL of the results page for form submission. |
| `suggestionsEnabled` | `Boolean` | `boolean` | Whether the autosuggest dropdown is active. |
| `minChars` | `Integer` | `number` | Minimum characters before autosuggest fires. |
| `debounceMs` | `Integer` | `number` | Debounce delay in milliseconds. |
| `autosuggestResult` | `AutosuggestResult` | object | Populated when `?q=` present; see `AutosuggestResult` shape below. |
| `visualSearchEnabled` | `Boolean` | `boolean` | `true` when visual search is enabled for this channel. |
| `visualSearchUploadUrl` | `String` | `string` | Upload proxy URL (`/_brxdis-api/visual-search/{widgetId}/upload`). Present only when both enabled + widget ID resolved. |
| `visualSearchWidgetId` | `String` | `string` | Visual search widget ID. Present only when enabled + widget ID resolved. |

---

### `DiscoverySearchGridComponent` + `DiscoveryCategoryGridComponent` (shared)

| Key | Java type | JSON type | Notes |
|---|---|---|---|
| `dataSourceMode` | `String` | `"search"` \| `"category"` \| `"visual-search"` | Mode the component is operating in. |
| `products` | `List<ProductSummary>` | `ProductSummary[]` | Matching products. `null` when query is blank or category not configured. |
| `pagination` | `PaginationModel` | object | See `PaginationModel` shape below. `null` when `showPagination=false`. |
| `facets` | `Map<String,Facet>` | `Record<string, Facet>` | Facet name → `Facet`. `null` when `showFacets=false`. |
| `facetUrls` | `Map<String,Map<String,String>>` | `Record<string,Record<string,string>>` | facetName → facetValue → toggle URL. `null` when `showFacets=false`. |
| `activeFacets` | `Map<String,List<String>>` | `Record<string,string[]>` | Currently active filter values per facet. `null` when `showFacets=false`. |
| `clearAllFiltersUrl` | `String` | `string` | URL that removes all active filters. `null` when `showFacets=false`. |
| `pageUrls` | `Map<Integer,String>` | `Record<number,string>` | 0-indexed page → URL. `null` when `showPagination=false`. |
| `sortUrl` | `String` | `string` | Base URL for sort controls; append `&sort=value`. `null` when `showSort=false`. |
| `sortOptions` | `List<String>` | `string[]` | Available sort values. `null` when `showSort=false`. |
| `stats` | `Map<String,FieldStats>` | `Record<string, FieldStats>` | Per-field min/max/mean/count. Empty map unless `statsFields` param is set. |
| `campaign` | `Campaign` | object \| `null` | Active Discovery campaign, or `null`. |
| `document` | `DiscoveryDocumentBean` | object | Raw JCR document bean (Category document for category mode). |

### Search-only keys

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `query` | `String` | `string` | Trimmed search term. |
| `didYouMean` | `List<String>` | `string[]` | Did-you-mean suggestions. `null` when none returned or `showDidYouMean=false`. |
| `autoCorrectQuery` | `String` | `string` \| `null` | Auto-corrected query from Discovery. |
| `redirectUrl` | `String` | `string` \| `null` | Keyword redirect URL from Discovery. |
| `redirectQuery` | `String` | `string` \| `null` | The query that triggered the redirect. |

### Category-only keys

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `categoryId` | `String` | `string` | Resolved category ID. |
| `displayName` | `String` | `string` | Category display name from Discovery. |

> When `dataSourceMode` is `"visual-search"`, only `products` is set — facets, pagination, sort, and did-you-mean are absent.

---

### `DiscoveryProductDetailComponent`

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `product` | `ProductSummary` \| `null` | object \| `null` | The resolved product. `null` when not found or no `pid`. |
| `pid` | `String` | `string` | Resolved product ID (from document or `?pid=` URL param). |
| `document` | `DiscoveryProductDetailBean` | object | Raw JCR document bean. |

---

### `DiscoveryProductRecommendationComponent`, `DiscoveryCategoryRecommendationComponent`, `DiscoveryGlobalRecommendationComponent`, `DiscoveryKeywordRecommendationComponent`

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `products` | `List<ProductSummary>` | `ProductSummary[]` | Recommended products. |
| `widgetId` | `String` | `string` | Resolved widget ID. |
| `widgetType` | `String` | `string` | Discovery widget type (e.g. `co_viewed`, `trending_product`). |
| `widgetResultId` | `String` | `string` | Opaque result ID from the Discovery API response. |
| `widgetQuery` | `String` | `string` | Context value sent to Discovery (PID for product recs, category ID for category recs, keyword for keyword recs). |
| `showPrice` | `Boolean` | `boolean` | Whether price should be rendered. |
| `showDescription` | `Boolean` | `boolean` | Whether description should be rendered. |
| `document` | bean | object | Raw JCR recommendation document bean. |

---

### `DiscoveryProductHighlightComponent`

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `products` | `List<ProductSummary>` | `ProductSummary[]` | Up to 4 curated products. May contain `null` entries for empty picker slots. |
| `productBeans` | `List<DiscoveryProductDetailBean>` | bean array | Raw JCR beans for advanced templates. |

---

### `DiscoveryCategoryHighlightComponent`

| Key | Java type | JSON type | Description |
|---|---|---|---|
| `categories` | `List<CategoryHighlight>` | object array | Up to 4 category tiles; each has `categoryId`, `displayName`, `productPreviewCount`. |
| `previewProducts` | `Map<String, List<ProductSummary>>` | `Record<string, ProductSummary[]>` | categoryId → preview product list. |
| `categoryBeans` | `List<DiscoveryCategoryBean>` | bean array | Raw JCR beans for advanced templates. |

---

## Response shapes

### `ProductSummary`

Java record — serialized to JSON by Jackson.

| Field | Java type | JSON type | Notes |
|---|---|---|---|
| `id` | `String` | `string` | Discovery product ID (PID). |
| `title` | `String` | `string` | |
| `url` | `String` | `string` | Product page URL (SEO-friendly path when `pid` is in index). |
| `imageUrl` | `String` | `string` | Primary image URL. |
| `price` | `BigDecimal` | `number` \| `null` | May be null if Discovery does not return a price. |
| `currency` | `String` | `string` | ISO currency code, e.g. `USD`. |
| `attributes` | `Map<String,Object>` | `Record<string,unknown>` | Open map — backend passes whatever fields were requested via the `fl` param. Common keys: `brand`, `description`, `sale_price`, `thumb_image`. |
| `variants` | `List<VariantSummary>` | `VariantSummary[]` | Variant list. Empty when Discovery returns no variant data. |

TypeScript:

```typescript
export interface ProductSummary {
  id: string;
  title: string;
  url: string;
  imageUrl: string;
  price: number | null;
  currency: string;
  attributes: Record<string, unknown>;
  variants: VariantSummary[];
}
```

---

### `VariantSummary`

| Field | Java type | JSON type |
|---|---|---|
| `skuId` | `String` | `string` |
| `color` | `String` | `string` |
| `colorGroup` | `String` | `string` |
| `size` | `String` | `string` |
| `price` | `BigDecimal` | `number` \| `null` |
| `salePrice` | `BigDecimal` | `number` \| `null` |
| `thumbnailImages` | `List<String>` | `string[]` |
| `largeImages` | `List<String>` | `string[]` |
| `swatchImages` | `List<String>` | `string[]` |
| `attributes` | `Map<String,Object>` | `Record<string,unknown>` |

TypeScript:

```typescript
export interface VariantSummary {
  skuId: string;
  color: string;
  colorGroup: string;
  size: string;
  price: number | null;
  salePrice: number | null;
  thumbnailImages: string[];
  largeImages: string[];
  swatchImages: string[];
  attributes: Record<string, unknown>;
}
```

---

### `PaginationModel`

| Field | Java type | JSON type | Notes |
|---|---|---|---|
| `total` | `long` | `number` | Total matching items. |
| `page` | `int` | `number` | Current page, **0-based**. |
| `pageSize` | `int` | `number` | Items per page. |
| `totalPages` | `int` | `number` | `ceil(total / pageSize)`. |

---

### `Facet`

| Field | Java type | JSON type | Notes |
|---|---|---|---|
| `name` | `String` | `string` | Facet attribute name. |
| `type` | `String` | `string` | `"text"` or `"range"`. |
| `value` | `List<FacetValue>` | `FacetValue[]` | Facet options. |

### `FacetValue`

| Field | Java type | JSON type | Notes |
|---|---|---|---|
| `name` | `String` | `string` | Option label. |
| `count` | `long` | `number` | Result count. |
| `catId` | `String` | `string` \| `null` | Category ID (category facets only). |
| `crumb` | `String` | `string` \| `null` | Breadcrumb path (category facets). |
| `treePath` | `String` | `string` \| `null` | Full tree path (category facets). |
| `parent` | `String` | `string` \| `null` | Parent ID (category facets). |
| `start` | `Double` | `number` \| `null` | Range start (range facets only). |
| `end` | `Double` | `number` \| `null` | Range end (range facets only). |

---

### `Campaign`

| Field | Java type | JSON type |
|---|---|---|
| `id` | `String` | `string` |
| `name` | `String` | `string` |
| `htmlText` | `String` | `string` |
| `bannerUrl` | `String` | `string` |
| `imageUrl` | `String` | `string` |

---

### `FieldStats`

Populated per field when the `statsFields` component parameter is set.

| Field | Java type | JSON type |
|---|---|---|
| `min` | `double` | `number` |
| `max` | `double` | `number` |
| `mean` | `double` | `number` |
| `count` | `long` | `number` |

---

### `AutosuggestResult`

| Field | Java type | JSON type | Description |
|---|---|---|---|
| `originalQuery` | `String` | `string` | The query as submitted. |
| `querySuggestions` | `List<String>` | `string[]` | Suggested full queries. |
| `attributeSuggestions` | `List<AttributeSuggestion>` | object array | Attribute-value pairs (e.g. brand = Nike). |
| `productSuggestions` | `List<ProductSummary>` | `ProductSummary[]` | Top matching products. |

### `AttributeSuggestion`

| Field | Java type | JSON type |
|---|---|---|
| `name` | `String` | `string` |
| `value` | `String` | `string` |
| `attributeType` | `String` | `string` |

---

## Complete TypeScript interfaces

```typescript
import type { ProductSummary, VariantSummary, PaginationModel } from './discovery';

export interface ProductGridModels {
  editMode: boolean;
  dataSourceMode: 'search' | 'category' | 'visual-search';
  products: ProductSummary[] | null;
  pagination: PaginationModel | null;
  facets: Record<string, Facet> | null;
  facetUrls: Record<string, Record<string, string>> | null;
  activeFacets: Record<string, string[]> | null;
  clearAllFiltersUrl: string | null;
  pageUrls: Record<number, string> | null;
  sortUrl: string | null;
  sortOptions: string[] | null;
  stats: Record<string, FieldStats>;
  campaign: Campaign | null;
  // search-only
  query?: string;
  didYouMean?: string[] | null;
  autoCorrectQuery?: string | null;
  redirectUrl?: string | null;
  redirectQuery?: string | null;
  // category-only
  categoryId?: string;
  displayName?: string;
}

export interface SearchInputModels {
  editMode: boolean;
  query: string;
  placeholder: string;
  resultsPage: string;
  suggestionsEnabled: boolean;
  minChars: number;
  debounceMs: number;
  autosuggestResult: AutosuggestResult | null;
  visualSearchEnabled: boolean;
  visualSearchUploadUrl: string | null;
  visualSearchWidgetId: string | null;
}

export interface RecommendationModels {
  editMode: boolean;
  products: ProductSummary[];
  widgetId: string;
  widgetType: string;
  widgetResultId: string;
  widgetQuery: string;
  showPrice: boolean;
  showDescription: boolean;
}

export interface ProductDetailModels {
  editMode: boolean;
  product: ProductSummary | null;
  pid: string;
}

export interface ProductHighlightModels {
  editMode: boolean;
  products: (ProductSummary | null)[];
}

export interface CategoryHighlightModels {
  editMode: boolean;
  categories: CategoryHighlight[];
  previewProducts: Record<string, ProductSummary[]>;
}

export interface CategoryHighlight {
  categoryId: string;
  displayName: string;
  productPreviewCount: number;
}
```
