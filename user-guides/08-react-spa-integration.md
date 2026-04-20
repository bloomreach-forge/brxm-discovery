# React / SPA Integration Guide

This guide shows React/JS developers how to consume the brXM Discovery plugin via the **Page Model API**. Every component calls `request.setModel()` which drives both FTL templates and the headless JSON response.

---

## How it works

Each Discovery HST component serializes data into a `models` object in the Page Model API JSON response.

```
GET /site/resourceapi/search?q=shirt&page=1
```

```json
{
  "page": {
    "results": {
      "models": {
        "dataSourceMode": "search",
        "query": "shirt",
        "products": [ ... ],
        "pagination": { "total": 42, "page": 0, "pageSize": 12, "totalPages": 4 },
        "facets": { "brand": { ... }, "color": { ... } },
        "facetUrls": { "brand": { "Nike": "?q=shirt&filter.brand=Nike", ... } },
        "pageUrls": { "0": "?q=shirt", "1": "?q=shirt&page=1", "2": "?q=shirt&page=2" },
        "sortUrl": "?q=shirt",
        "activeFacets": {}
      }
    },
    "search-bar": {
      "models": {
        "query": "shirt",
        "placeholder": "Search...",
        "suggestionsEnabled": true
      }
    }
  }
}
```

With `@bloomreach/react-sdk`, retrieve models per component:

```tsx
import { BrComponent, BrPage } from '@bloomreach/react-sdk';

// inside a mapped component:
const models = component.getModels<ResultsComponentModels>();
```

---

## Component overview

The plugin ships two data-fetching components for search and commerce pages:

| Component class | Template | Purpose |
|---|---|---|
| `DiscoveryResultsComponent` | `brxdis-results` | Search results **and** category browse pages. One component handles both modes via the `Data source` parameter (`search` or `category`). Includes facets, pagination, and sort - all pre-built server-side. |
| `DiscoverySearchInputComponent` | `brxdis-search-input` | Standalone search bar placed in header, sidebar, or any zone. Handles autosuggest. Delegates actual search to a `DiscoveryResultsComponent` on the results page. |
| `DiscoveryRecommendationComponent` | `brxdis-recommendations` | Recommendation widget (v1 or v2 Pathways API). |
| `DiscoveryProductDetailComponent` | `brxdis-product-detail` | Product detail page - fetches one product by PID. |
| `DiscoveryProductHighlightComponent` | `brxdis-product-highlight` | Up to 4 curated product slots (editorial, not algorithmic). |
| `DiscoveryCategoryHighlightComponent` | `brxdis-category-highlight` | Up to 4 curated category tiles with optional product previews. |

---

## TypeScript interfaces

Paste these into a shared `discovery.types.ts` file.

```ts
// ── Core models ──────────────────────────────────────────────────────────────

export interface ProductSummary {
  id: string;
  title: string;
  url: string;
  imageUrl: string;
  price: number | null;       // BigDecimal serialises as number
  currency: string;
  attributes: Record<string, unknown>;  // brand, description, sale_price, etc.
}

export interface FacetValue {
  name: string;
  count: number;
  catId: string | null;       // category facets only
  crumb: string | null;
  treePath: string | null;
  parent: string | null;
}

export interface Facet {
  name: string;
  type: string;               // "text", "number", "date", ...
  value: FacetValue[];
}

export interface PaginationModel {
  total: number;
  page: number;               // 0-based
  pageSize: number;
  totalPages: number;
}

export interface FieldStats {
  min: number;
  max: number;
  mean: number;
  count: number;
}

export interface Campaign {
  id: string;
  name: string;
  htmlText: string | null;
  bannerUrl: string | null;
  imageUrl: string | null;
}

// ── Autosuggest ───────────────────────────────────────────────────────────────

export interface AttributeSuggestion {
  name: string;
  value: string;
  attributeType: string;
}

export interface AutosuggestResult {
  originalQuery: string;
  querySuggestions: string[];
  attributeSuggestions: AttributeSuggestion[];
  productSuggestions: ProductSummary[];
}

// ── Component model shapes ────────────────────────────────────────────────────

export interface ResultsComponentModels {
  /** "search" or "category" - driven by the Data source component param */
  dataSourceMode: 'search' | 'category';

  // ── Search mode fields ────────────────────────────────────────────────────
  query: string;                          // trimmed search term; empty string when blank
  didYouMean: string[] | null;            // search mode only
  autoCorrectQuery: string | null;
  redirectUrl: string | null;             // keyword redirect target
  redirectQuery: string | null;

  // ── Category mode fields ──────────────────────────────────────────────────
  categoryId: string;                     // Discovery category ID; empty when not resolved
  displayName: string;                    // editorial category name from Discovery

  // ── Shared result fields ──────────────────────────────────────────────────
  products: ProductSummary[] | null;      // null when no query or category
  pagination: PaginationModel;            // total=0, page=0 when empty
  stats: Record<string, FieldStats>;      // keyed by field name; empty when statsFields not set

  // ── Pre-built navigation URLs (server-side, ready to use as href) ─────────
  /** Map of facetName → facetValue → ready-to-use toggle URL.
   *  Active values produce removal URLs; inactive values produce addition URLs.
   *  Null when showFacets is disabled on the component. */
  facetUrls: Record<string, Record<string, string>> | null;

  /** Currently active filter values per facet name. */
  activeFacets: Record<string, string[]> | null;

  /** URL that clears all active filters. Null when showFacets is disabled. */
  clearAllFiltersUrl: string | null;

  /** Map of 0-indexed page number → ready-to-use URL.
   *  Page 0 URL omits the page param. Null when showPagination is disabled. */
  pageUrls: Record<number, string> | null;

  /** Base URL for sort switching - strip existing sort, then append &sort=value.
   *  Null when showSort is disabled. */
  sortUrl: string | null;

  /** Facet map (name → Facet) for rendering facet labels and counts.
   *  Null when showFacets is disabled. */
  facets: Record<string, Facet> | null;

  campaign: Campaign | null;
  editMode: boolean;
}

export interface SearchInputModels {
  query: string;
  placeholder: string;
  resultsPage: string;
  suggestionsEnabled: boolean;
  minChars: number;
  debounceMs: number;
  autosuggestResult: AutosuggestResult | null;
  editMode: boolean;
}

export interface RecommendationModels {
  products: ProductSummary[];
  widgetId: string;
  editMode: boolean;
}

export interface ProductDetailModels {
  /** null when: no document configured, Dynamic mode with no ?pid= URL param, or product not found in Discovery */
  product: ProductSummary | null;
  /** Resolved PID; empty string when no document is configured or Dynamic mode with no URL param */
  pid: string;
  document: unknown | null;  // CMS-internal document bean - do not use in SPA code; use `pid` and `product` instead
  editMode: boolean;
}

export interface ProductHighlightModels {
  products: (ProductSummary | null)[];  // up to 4 slots; null = slot not filled
  productBeans: unknown[];              // CMS-internal document beans - do not use in SPA code
  editMode: boolean;
}

export interface CategoryHighlight {
  categoryId: string;
  displayName: string;
  productPreviewCount: number;  // 0–4; set per document in CMS
}

export interface CategoryHighlightModels {
  categories: CategoryHighlight[];
  previewProducts: Record<string, ProductSummary[]>;  // keyed by categoryId; empty when count=0
  editMode: boolean;
}
```

---

## URL parameters

### Search results (`DiscoveryResultsComponent` with `dataSource=search`)

| Parameter | Type | Description |
|---|---|---|
| `q` | string | Search query. Empty string → empty state (null products). |
| `page` | number | **0-based in URLs.** `page=0` or omit for first page. |
| `sort` | string | Sort expression, e.g. `price asc`, `name desc`. Overrides component default. |
| `filter.{field}` | string (repeatable) | Facet filter. e.g. `filter.brand=Nike&filter.color=Red`. |

```
/site/search?q=shirt&page=1&sort=price+asc&filter.brand=Nike
```

> **Note:** You rarely need to build these URLs manually - the server provides pre-built `facetUrls`, `pageUrls`, and `sortUrl` in the models. Just render `<a href={url}>`.

### Category browse (`DiscoveryResultsComponent` with `dataSource=category`)

| Parameter | Type | Description |
|---|---|---|
| `category` | string | Discovery category ID. Only read when the linked Category Document is in **Dynamic mode** (blank `brxdis:categoryId`). Ignored when the document is in Pinned mode. |
| `page` | number | 0-based page number. |
| `sort` | string | Sort expression. |
| `filter.{field}` | string (repeatable) | Facet filter. |

```
/site/category?category=117417&filter.brand=Adidas
```

### Recommendations (`DiscoveryRecommendationComponent`)

| Parameter | Type | Description |
|---|---|---|
| `widgetId` | string | Widget ID override (used when no document configured). |
| `contextProductId` | string | Context product PID for `item` widgets. |
| `contextPageType` | string | Page type hint, e.g. `pdp`, `cart`. |
| `limit` | number | Max products to return (overrides component param). |
| `fields` | string | Comma-separated product fields to return. |
| `filter` | string | EFQ filter expression. |

### Product detail (`DiscoveryProductDetailComponent`)

| Parameter | Type | Description |
|---|---|---|
| `pid` | string | Product ID. Only read when the linked Product Detail Document is in **Dynamic mode** (blank `brxdis:productId`). Ignored when the document is Pinned. A document is always required; this param alone is not sufficient. |

---

## Component reference

### `DiscoveryResultsComponent` → `ResultsComponentModels`

The main workhorse for both search and category pages. It fetches data from Discovery and pre-builds all navigation URLs server-side - your React code just renders `<a href={url}>`.

```tsx
// SearchResults.tsx - works for both search and category pages
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ResultsComponentModels } from './discovery.types';

export function SearchResults({ component }: BrComponentContext) {
  const models = component.getModels<ResultsComponentModels>();
  const {
    dataSourceMode, query, products, pagination,
    facets, facetUrls, activeFacets, clearAllFiltersUrl,
    pageUrls, sortUrl, didYouMean, autoCorrectQuery,
    redirectUrl, campaign,
  } = models;

  // Server-side redirect (when autoRedirect is enabled on the component)
  if (redirectUrl) {
    window.location.href = redirectUrl;
    return null;
  }

  if (!products || products.length === 0) {
    return <p>No results{query ? ` for "${query}"` : ''}.</p>;
  }

  return (
    <div className="search-layout">
      {campaign && <CampaignBanner campaign={campaign} />}

      {autoCorrectQuery && (
        <p>Showing results for: <strong>{autoCorrectQuery}</strong></p>
      )}
      {didYouMean && didYouMean.length > 0 && (
        <p>Did you mean: {didYouMean.map(s => (
          <a key={s} href={`?q=${encodeURIComponent(s)}`}>{s}</a>
        ))}</p>
      )}

      <div className="two-col">
        <aside>
          {facets && facetUrls && (
            <FacetSidebar
              facets={facets}
              facetUrls={facetUrls}
              activeFacets={activeFacets ?? {}}
              clearAllUrl={clearAllFiltersUrl ?? ''}
            />
          )}
        </aside>

        <main>
          {sortUrl && <SortBar sortUrl={sortUrl} />}
          <p>{pagination.total} results</p>

          <div className="product-grid">
            {products.map(p => <ProductCard key={p.id} product={p} />)}
          </div>

          {pageUrls && (
            <Pagination
              currentPage={pagination.page}
              totalPages={pagination.totalPages}
              pageUrls={pageUrls}
            />
          )}
        </main>
      </div>
    </div>
  );
}
```

**Key behaviour:**
- `products` is `null` when the query is blank (search mode), or when no Category Document is configured (category mode), or when the Category Document is in Dynamic mode and no `?category=` URL param is present.
- `stats` contains `FieldStats` per field only when `statsFields` is set on the component (e.g. `price` for a price range slider).
- `facetUrls`, `pageUrls`, `sortUrl`, and `activeFacets` are `null` when the corresponding display option is disabled on the component (`showFacets`, `showPagination`, `showSort`).

---

### Pre-built URLs - facets, pagination, sort

All navigation URLs are computed server-side and passed as ready-to-use strings. **No URL manipulation is needed in the browser.**

```tsx
// FacetSidebar.tsx - facetUrls are ready to use as href
function FacetSidebar({ facets, facetUrls, activeFacets, clearAllUrl }: {
  facets: Record<string, Facet>;
  facetUrls: Record<string, Record<string, string>>;
  activeFacets: Record<string, string[]>;
  clearAllUrl: string;
}) {
  const hasActiveFilters = Object.keys(activeFacets).length > 0;

  return (
    <nav aria-label="Filter results">
      {hasActiveFilters && (
        <a href={clearAllUrl}>Clear all filters</a>
      )}
      {Object.values(facets).map(facet => (
        <details key={facet.name} open>
          <summary>{facet.name}</summary>
          <ul>
            {facet.value.map(fv => {
              const isActive = activeFacets[facet.name]?.includes(fv.name) ?? false;
              const url = facetUrls[facet.name]?.[fv.name] ?? '#';
              return (
                <li key={fv.name}>
                  <a href={url} aria-pressed={isActive}>
                    {fv.name} <span>({fv.count})</span>
                  </a>
                </li>
              );
            })}
          </ul>
        </details>
      ))}
    </nav>
  );
}
```

```tsx
// Pagination.tsx - pageUrls are ready to use
function Pagination({ currentPage, totalPages, pageUrls }: {
  currentPage: number;
  totalPages: number;
  pageUrls: Record<number, string>;
}) {
  return (
    <nav aria-label="Pages">
      {Array.from({ length: totalPages }, (_, i) => (
        <a
          key={i}
          href={pageUrls[i] ?? '#'}
          aria-current={i === currentPage ? 'page' : undefined}
        >
          {i + 1}
        </a>
      ))}
    </nav>
  );
}
```

```tsx
// SortBar.tsx - sortUrl is the base; append &sort=value
const sortOptions = [
  { label: 'Relevance', value: '' },
  { label: 'Price: low to high', value: 'price asc' },
  { label: 'Price: high to low', value: 'price desc' },
  { label: 'Name A–Z', value: 'name asc' },
];

function SortBar({ sortUrl }: { sortUrl: string }) {
  return (
    <select onChange={e => {
      const value = e.target.value;
      window.location.href = value ? `${sortUrl}&sort=${encodeURIComponent(value)}` : sortUrl;
    }}>
      {sortOptions.map(opt => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  );
}
```

---

### `DiscoverySearchInputComponent` → `SearchInputModels`

A standalone search bar placed in any page zone (header, sidebar). It handles autosuggest suggestions but **does not render search results itself** - it submits to the page containing `DiscoveryResultsComponent`.

```tsx
// SearchBar.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { SearchInputModels, AutosuggestResult } from './discovery.types';

export function SearchBar({ component }: BrComponentContext) {
  const {
    query, placeholder, resultsPage,
    suggestionsEnabled, minChars, debounceMs,
    autosuggestResult,
  } = component.getModels<SearchInputModels>();

  const action = resultsPage || undefined; // blank = current page

  return (
    <form method="get" action={action}>
      <input
        type="search"
        name="q"
        defaultValue={query}
        placeholder={placeholder}
      />
      <button type="submit">Search</button>

      {suggestionsEnabled && autosuggestResult && (
        <AutosuggestDropdown result={autosuggestResult} />
      )}
    </form>
  );
}
```

**Autosuggest typeahead:** For a live dropdown as the user types, call the search-input page endpoint in suggest-only mode while the user types:

```ts
async function fetchSuggestions(
  query: string,
  resourceApiBase: string,
  searchBarPath: string
): Promise<AutosuggestResult | null> {
  if (query.length < 2) return null;

  const url = new URL(resourceApiBase + searchBarPath);
  url.searchParams.set('q', query);

  const res = await fetch(url.toString(), { headers: { Accept: 'application/json' } });
  const json = await res.json();

  // Navigate to the search bar component in the response
  const componentModels = json?.page?.['search-bar']?.models;
  return componentModels?.autosuggestResult ?? null;
}
```

Use `debounceMs` from the model to control the delay:

```ts
const { debounceMs } = component.getModels<SearchInputModels>();

const debouncedFetch = useMemo(
  () => debounce(fetchSuggestions, debounceMs),
  [debounceMs]
);
```

---

### `DiscoveryRecommendationComponent` → `RecommendationModels`

```tsx
// RecommendationsCarousel.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { RecommendationModels } from './discovery.types';

export function RecommendationsCarousel({ component }: BrComponentContext) {
  const { products } = component.getModels<RecommendationModels>();

  if (products.length === 0) return null;

  return (
    <section aria-label="Recommended products">
      <div className="carousel-track">
        {products.map(p => (
          <a key={p.id} href={`/product?pid=${p.id}`} className="carousel-card">
            {p.imageUrl && <img src={p.imageUrl} alt={p.title} />}
            <p>{p.title}</p>
            <p>{p.currency} {p.price?.toFixed(2)}</p>
          </a>
        ))}
      </div>
    </section>
  );
}
```

**Context product resolution (server-side, transparent to client):**
1. `contextProductId` URL param
2. `contextProductId` component parameter (Channel Manager)
3. Product Detail component on the same page (when `Link to Product Detail on page` is checked)
4. Page content bean `brxdis:pid` property

For a PDP "Similar Items" carousel, check `Link to Product Detail on page` in the Channel Manager - the recommendation component will automatically use the PID from the Product Detail component on the same page.

---

### `DiscoveryProductDetailComponent` → `ProductDetailModels`

```tsx
// ProductDetail.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductDetailModels } from './discovery.types';

export function ProductDetail({ component }: BrComponentContext) {
  const { product, pid, editMode } = component.getModels<ProductDetailModels>();

  if (!product) {
    // No document configured, Dynamic mode with no ?pid= param, or product not in Discovery.
    // In edit mode the Channel Manager shows the document picker; render nothing in delivery.
    if (editMode) return <p>Configure a Product Detail Document in component properties.</p>;
    return null;
  }

  const brand = product.attributes['brand'] as string | undefined;
  const description = product.attributes['description'] as string | undefined;
  const salePrice = product.attributes['sale_price'] as number | undefined;

  return (
    <article>
      {product.imageUrl && (
        <img src={product.imageUrl} alt={product.title} />
      )}
      <h1>{product.title}</h1>
      {brand && <p className="brand">{brand}</p>}
      <p className="price">
        {salePrice != null
          ? <><s>{product.currency} {product.price?.toFixed(2)}</s> {product.currency} {salePrice.toFixed(2)}</>
          : <>{product.currency} {product.price?.toFixed(2)}</>
        }
      </p>
      {description && <p>{description}</p>}
    </article>
  );
}
```

**Product ID resolution (document-dictated, server-side):**

A `brxdis:productDetailDocument` is required on the component. The document mode is configured in the CMS via the product wizard:

- **Pinned**: the document stores a specific product ID → that ID is always used; the `?pid=` URL param is ignored.
- **Dynamic**: the document has no pinned ID → reads the `?pid=` URL param at render time.

If no document is configured on the component, `product: null` is returned and the component renders nothing.

For a URL-driven PDP, attach a Product Detail Document configured in **Dynamic mode** - the component then reads `?pid=<id>` from the URL on each request.

---

### `DiscoveryProductHighlightComponent` → `ProductHighlightModels`

```tsx
// ProductHighlight.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductHighlightModels } from './discovery.types';

export function ProductHighlight({ component }: BrComponentContext) {
  const { products } = component.getModels<ProductHighlightModels>();
  const filled = products.filter((p): p is NonNullable<typeof p> => p !== null);

  if (filled.length === 0) return null;

  return (
    <section className="product-highlight">
      {filled.map(p => (
        <article key={p.id}>
          {p.imageUrl && <img src={p.imageUrl} alt={p.title} />}
          <h3><a href={`/product?pid=${p.id}`}>{p.title}</a></h3>
          <p>{p.currency} {p.price?.toFixed(2)}</p>
        </article>
      ))}
    </section>
  );
}
```

**Notes:**
- `products` is always an array of up to 4 items. Unfilled slots are `null`.
- Products are fetched individually at render time by PID - no search results involved.

---

### `DiscoveryCategoryHighlightComponent` → `CategoryHighlightModels`

```tsx
// CategoryHighlight.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { CategoryHighlightModels } from './discovery.types';

export function CategoryHighlight({ component }: BrComponentContext) {
  const { categories, previewProducts } = component.getModels<CategoryHighlightModels>();

  if (categories.length === 0) return null;

  return (
    <nav className="category-highlight">
      {categories.map(cat => {
        const previews = previewProducts[cat.categoryId] ?? [];
        return (
          <a key={cat.categoryId} href={`/category?category=${cat.categoryId}`}>
            <span className="cat-name">{cat.displayName || cat.categoryId}</span>
            {previews.length > 0 && (
              <div className="cat-previews">
                {previews.map(p => (
                  <img key={p.id} src={p.imageUrl} alt={p.title} />
                ))}
              </div>
            )}
          </a>
        );
      })}
    </nav>
  );
}
```

**Notes:**
- `displayName` is an optional editorial label; fall back to `categoryId` if blank.
- `previewProducts` is keyed by `categoryId`. Empty when all `productPreviewCount` values are `0`.
- Preview products are served from a JVM-level cache with a ~5-minute TTL - not fetched live on every request.
- Category page URL uses `?category=` (not `?categoryId=`).

---

## Price range slider (stats)

When `Statistics fields` is set to `price` on a Results component, `stats.price` is populated:

```ts
const priceStats = models.stats?.['price'] as FieldStats | undefined;
```

Pass the selected range back as a filter. Discovery uses `fq=price:[min TO max]` syntax - the plugin handles the conversion automatically when you pass `filter.price=100:500` (colon-separated range):

```ts
params.set('filter.price', `${minPrice}:${maxPrice}`);
```

---

## Full search page wiring example

```tsx
// SearchPage.tsx
import { BrPage, BrComponent } from '@bloomreach/react-sdk';

const componentMapping = {
  'DiscoveryResultsComponent': SearchResults,
  'DiscoverySearchInputComponent': SearchBar,
  'DiscoveryRecommendationComponent': RecommendationsCarousel,
  'DiscoveryProductDetailComponent': ProductDetail,
  'DiscoveryProductHighlightComponent': ProductHighlight,
  'DiscoveryCategoryHighlightComponent': CategoryHighlight,
};

export function SearchPage() {
  return (
    <BrPage configuration={brConfig} mapping={componentMapping}>
      <div className="search-layout">
        <BrComponent path="main/content" />  {/* DiscoveryResultsComponent */}
      </div>
    </BrPage>
  );
}
```

The `DiscoveryResultsComponent` is self-contained - it owns data fetching, facet rendering, pagination, and sort. No sibling components are required.

---

## `editMode` and Channel Manager preview

Every component sets `editMode: boolean`. It is `true` only when rendered inside the brXM Channel Manager preview. In normal delivery it is always `false`.

```tsx
const { products, editMode } = component.getModels<ProductHighlightModels>();
if (!editMode && products.every(p => p === null)) return null;  // hide in delivery, show placeholder in preview
```

---

## Campaign banners

When a Bloomreach Discovery campaign is active for a search or category query, `campaign` is populated:

```tsx
const { campaign } = component.getModels<ResultsComponentModels>();

{campaign && (
  <aside className="campaign-banner">
    {campaign.imageUrl && <img src={campaign.imageUrl} alt={campaign.name} />}
    {campaign.htmlText && <p>{campaign.name}</p>}
  </aside>
)}
```

`campaign.htmlText` may contain HTML - sanitize with DOMPurify before setting innerHTML.

---

## Component parameter reference

Set in Channel Manager. These drive server-side behaviour - they are **not** in `getModels()`.

| Component | Parameter | Default | Effect |
|---|---|---|---|
| `DiscoveryResultsComponent` | `dataSource` | `search` | `search` or `category` mode |
| | `document` | - | Category Document picker (category mode only) |
| | `pageSize` | `12` | Results per page |
| | `defaultSort` | `""` | Default sort expression (URL `sort` overrides) |
| | `catalogName` | `""` | Discovery catalog for non-product catalogs |
| | `showFacets` | `true` | Include facets, facetUrls, activeFacets in models |
| | `showPagination` | `true` | Include pageUrls in models |
| | `showSort` | `true` | Include sortUrl in models |
| | `showDidYouMean` | `true` | Include didYouMean in models (search only) |
| | `autoRedirect` | `false` | Server-side redirect on keyword redirect |
| | `statsFields` | `""` | Fields to compute min/max/mean stats for (e.g. `price`) |
| | `segment` | `""` | Discovery visitor segment for personalised results |
| | `exclusionFilter` | `""` | Server-side EFQ filter to exclude items |
| `DiscoverySearchInputComponent` | `placeholder` | `Search...` | Input placeholder text |
| | `resultsPage` | `""` | Submit form to this path; blank = current page |
| | `suggestionsEnabled` | `true` | Fetch autosuggest; populates `autosuggestResult` |
| | `suggestionsLimit` | `5` | Max suggestions per group |
| | `minChars` | `2` | Min characters before suggestions are fetched |
| | `debounceMs` | `250` | Debounce delay for suggestion requests |
| `DiscoveryProductRecommendationComponent` | `document` | - | Product Recommendation Document picker |
| | `limit` | `8` | Max recommended products |
| | `showPrice` | `true` | Template shows product price |
| | `showDescription` | `false` | Template shows product description |
| | `productUrlParam` | `pid` | URL param name for Dynamic mode (change to use `?sku=` etc.) |
| `DiscoveryCategoryRecommendationComponent` | `document` | - | Category Recommendation Document picker |
| | `limit` | `8` | Max recommended products |
| | `showPrice` | `true` | Template shows product price |
| | `showDescription` | `false` | Template shows product description |
| | `categoryUrlParam` | `category` | URL param name for Dynamic mode |
| `DiscoveryGlobalRecommendationComponent` | `document` | - | Global/Personalized Recommendation Document picker |
| | `limit` | `8` | Max recommended products |
| | `showPrice` | `true` | Template shows product price |
| | `showDescription` | `false` | Template shows product description |
| `DiscoveryProductDetailComponent` | `document` | - | Product Detail Document picker (required; use the product wizard to choose Dynamic or Pinned mode) |
| | `productUrlParam` | `pid` | URL param name used in Dynamic mode (e.g. change to `sku` to read `?sku=` instead) |
| `DiscoveryProductHighlightComponent` | `document1`–`document4` | `""` | Up to 4 Product Detail Document pickers |
| `DiscoveryCategoryHighlightComponent` | `document1`–`document4` | `""` | Up to 4 Category Document pickers |

---

## Error states

| Condition | What you receive |
|---|---|
| `q` is blank (search mode) | `products: null`, `pagination: {total:0}` |
| No Category Document on component | `products: null`, `categoryId: ""` |
| Category Document in Dynamic mode, no `?category=` URL param | `products: null`, `categoryId: ""` |
| Category Document in Pinned mode | `categoryId` = pinned value, products fetched |
| No widget configured on recommendations | `products: []`, `widgetId: ""` |
| No Product Detail Document on component | `product: null`, `pid: ""` |
| Product Detail Document in Dynamic mode, no `?pid=` URL param | `product: null`, `pid: ""` |
| Product Detail Document in Dynamic mode, `?pid=` present but product not in Discovery | `product: null`, `pid: "<id>"` |
| Product Detail Document in Pinned mode, product not in Discovery | `product: null`, `pid: "<pinned-id>"` |
| No documents on ProductHighlight | `products: [null, null, null, null]` - all slots empty |
| No documents on CategoryHighlight | `categories: []`, `previewProducts: {}` |
| CategoryHighlight with `productPreviewCount=0` | `categories` populated, `previewProducts: {}` |
| Discovery API unreachable | `SearchException` thrown server-side → component renders error page |
