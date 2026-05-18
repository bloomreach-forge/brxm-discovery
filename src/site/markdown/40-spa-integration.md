# React / SPA Integration Guide

This guide is for React and JavaScript developers building headless storefronts on top of brXM with the brxm-discovery plugin. It covers how to consume all nine components via the **Page Model API**, wire your SPA, and handle SEO-friendly product URLs.

> **Not using a SPA?** The plugin also ships ready-to-use Freemarker templates (`brxdis-*.ftl`) for HST-rendered sites. This guide focuses entirely on the Page Model API / headless path.

> 📸 **[IMAGE PLACEHOLDER]** — Architecture diagram: brXM + brxm-discovery on the left, Page Model API JSON in the centre, React SPA + Discovery API on the right, showing the full request/response cycle.

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

The plugin ships dedicated components for each search and commerce page type:

| Component class | Template | Purpose |
|---|---|---|
| `DiscoverySearchGridComponent` | `brxdis-results` | Keyword search results. Includes facets, pagination, sort, did-you-mean, and keyword redirects - all pre-built server-side. |
| `DiscoveryCategoryGridComponent` | `brxdis-results` | Category browse. Includes facets, pagination, and sort - all pre-built server-side. |
| `DiscoverySearchInputComponent` | `brxdis-search-input` | Standalone search bar placed in header, sidebar, or any zone. Handles autosuggest. Delegates actual search to a `DiscoverySearchGridComponent` on the results page. |
| `DiscoveryProductRecommendationComponent` | `brxdis-recommendations-product` | Product recommendation widget (v1 or v2 Pathways API). |
| `DiscoveryCategoryRecommendationComponent` | `brxdis-recommendations-category` | Category recommendation widget. |
| `DiscoveryGlobalRecommendationComponent` | `brxdis-recommendations-global` | Global / trending recommendation widget. |
| `DiscoveryProductDetailComponent` | `brxdis-product-detail` | Product detail page - fetches one product by PID. |
| `DiscoveryProductHighlightComponent` | `brxdis-product-highlight` | Up to 4 curated product slots (editorial, not algorithmic). |
| `DiscoveryCategoryHighlightComponent` | `brxdis-category-highlight` | Up to 4 curated category tiles with optional product previews. |

---

## TypeScript interfaces

Paste these into a shared `discovery.types.ts` file.

```ts
// ── Core models ──────────────────────────────────────────────────────────────

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

export interface ProductSummary {
  id: string;
  title: string;
  url: string;
  imageUrl: string;
  price: number | null;       // BigDecimal serialises as number
  currency: string;
  attributes: Record<string, unknown>;  // brand, description, sale_price, etc.
  variants?: VariantSummary[];          // present when variant fields are in the fl param
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

### Search results (`DiscoverySearchGridComponent`)

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

### Category browse (`DiscoveryCategoryGridComponent`)

| Parameter | Type | Description |
|---|---|---|
| `cid` | string | Discovery category ID. Read as a URL path segment (`/category/{slug}/cid/{id}`) or query param (`?cid=`). Only used in Dynamic mode (no pinned `brxdis:categoryId` in the document). The param name is configurable via `categoryUrlParam` (default `cid`). |
| `page` | number | 0-based page number. |
| `sort` | string | Sort expression. |
| `filter.{field}` | string (repeatable) | Facet filter. |

```
/site/category/womens-shoes/cid/117417?filter.brand=Adidas
```

Query-param fallback: `/site/category?cid=117417&filter.brand=Adidas`

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

### `DiscoverySearchGridComponent` / `DiscoveryCategoryGridComponent` → `ResultsComponentModels`

Both components share the same `ResultsComponentModels` shape. They fetch data from Discovery and pre-build all navigation URLs server-side - your React code just renders `<a href={url}>`. Use `dataSourceMode` to distinguish search from category in a shared React component.

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
- `products` is `null` when the query is blank (search mode), or when no Category Document is configured (category mode), or when the Category Document is in Dynamic mode and no category ID is found in the URL path or `?cid=` query param.
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

A standalone search bar placed in any page zone (header, sidebar). It handles autosuggest suggestions but **does not render search results itself** - it submits to the page containing `DiscoverySearchGridComponent`.

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

### URL helpers

Product and category pages use SEO-friendly path URLs. Add this utility to your shared helpers:

```ts
// utils/slugify.ts
export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '') || 'product';
}
```

Use it to build internal links from any `ProductSummary` or `CategoryHighlight`:

```ts
// Product link
`/product/${slugify(product.title)}/pid/${product.id}`

// Category link
`/category/${slugify(cat.displayName || cat.categoryId)}/cid/${cat.categoryId}`
```

Both URLs also work as query-param fallbacks (`?pid=` / `?cid=`) — the slug segment is purely decorative and is ignored by the server.

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
          <a key={p.id} href={`/product/${slugify(p.title)}/pid/${p.id}`} className="carousel-card">
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
          <h3><a href={`/product/${slugify(p.title)}/pid/${p.id}`}>{p.title}</a></h3>
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
          <a key={cat.categoryId} href={`/category/${slugify(cat.displayName || cat.categoryId)}/cid/${cat.categoryId}`}>
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
- Category page URL follows the `/{slug}/cid/{id}` convention. The `cid` label matches the `categoryUrlParam` component property (default `cid`). Use `?cid={id}` as the query-param fallback if you need to link without a slug.

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

## Full wiring example

Complete component mapping and page wiring for a full e-commerce SPA.

```tsx
// componentMapping.ts
import { SearchResults } from './components/SearchResults';
import { SearchBar } from './components/SearchBar';
import { RecommendationsCarousel } from './components/RecommendationsCarousel';
import { ProductDetail } from './components/ProductDetail';
import { ProductHighlight } from './components/ProductHighlight';
import { CategoryHighlight } from './components/CategoryHighlight';

export const componentMapping = {
  // Both search and category use the same React component — check dataSourceMode inside
  'DiscoverySearchGridComponent': SearchResults,
  'DiscoveryCategoryGridComponent': SearchResults,
  'DiscoverySearchInputComponent': SearchBar,
  'DiscoveryProductRecommendationComponent': RecommendationsCarousel,
  'DiscoveryCategoryRecommendationComponent': RecommendationsCarousel,
  'DiscoveryGlobalRecommendationComponent': RecommendationsCarousel,
  'DiscoveryProductDetailComponent': ProductDetail,
  'DiscoveryProductHighlightComponent': ProductHighlight,
  'DiscoveryCategoryHighlightComponent': CategoryHighlight,
};
```

```tsx
// App.tsx
import { BrPage } from '@bloomreach/react-sdk';
import { componentMapping } from './componentMapping';

const BR_CONFIG = {
  endpoint: 'http://localhost:8080/site/resourceapi',
  // ... other brXM SPA SDK config
};

export function App() {
  return (
    <BrPage configuration={BR_CONFIG} mapping={componentMapping}>
      {/* Your page layout renders BrComponent zones here */}
    </BrPage>
  );
}
```

**Route → component mapping:**

| Route | Components to place |
|---|---|
| `/search` | `DiscoverySearchGridComponent` + `DiscoverySearchInputComponent` (header) |
| `/products/{slug}/cid/{id}` | `DiscoveryCategoryGridComponent` + `DiscoveryGlobalRecommendationComponent` |
| `/product/{slug}/pid/{id}` | `DiscoveryProductDetailComponent` + `DiscoveryProductRecommendationComponent` |
| Homepage | `DiscoveryCategoryHighlightComponent` + `DiscoveryProductHighlightComponent` + `DiscoveryGlobalRecommendationComponent` |

`DiscoverySearchGridComponent` and `DiscoveryCategoryGridComponent` are self-contained — they own data fetching, facet rendering, pagination, and sort. No sibling components are required.

> 📸 **[IMAGE PLACEHOLDER]** — Side-by-side screenshot: Channel Manager page structure panel on the left listing Discovery components, and the corresponding `componentMapping.ts` file open in VS Code on the right.

---

## Forwarding browser context for accurate pixel tracking

The plugin fires Discovery pixel events from the JVM, not the browser. For Discovery to correctly attribute impressions (analytics, personalisation, A/B tests), it needs the real browser IP, User-Agent, and locale on every brXM request — not the SPA server's own network identity.

Your SPA server must forward three headers when calling brXM's Page Model API:

| Header sent to brXM | Source on your SPA server | Purpose |
|---|---|---|
| `X-Forwarded-For` | Incoming `X-Forwarded-For` or connection IP | Real client IP sent as `client_ip` on the pixel |
| `X-Forwarded-User-Agent` | Incoming `User-Agent` | Browser UA — enables bot/crawler suppression |
| `X-Forwarded-Accept-Language` | Incoming `Accept-Language` | Locale context on the pixel request |

Without these headers brXM falls back to the server-to-server request's own values: Discovery sees your Node.js server's IP and UA for every event. Pixels that look like server-side crawlers are silently suppressed.

### Next.js example

```ts
// lib/brxm.ts — helper used by any server-side fetch to brXM
import type { IncomingMessage } from 'http';

export function forwardHeaders(req: IncomingMessage): Record<string, string> {
  const headers: Record<string, string> = {};
  const xff = req.headers['x-forwarded-for'];
  if (xff) headers['X-Forwarded-For'] = Array.isArray(xff) ? xff[0] : xff;
  const ua = req.headers['user-agent'];
  if (ua) headers['X-Forwarded-User-Agent'] = ua;
  const lang = req.headers['accept-language'];
  if (lang) headers['X-Forwarded-Accept-Language'] = lang;
  return headers;
}
```

```ts
// pages/search.tsx — getServerSideProps
export async function getServerSideProps({ req, query }: GetServerSidePropsContext) {
  const pageApiUrl = `${process.env.BRXM_ENDPOINT}/search?q=${query.q ?? ''}`;
  const res = await fetch(pageApiUrl, {
    headers: { Accept: 'application/json', ...forwardHeaders(req) },
  });
  // ...
}
```

### Express example

```ts
app.get('/api/page/*', async (req, res) => {
  const upstream = await fetch(brxmEndpoint + req.path, {
    headers: {
      Accept: 'application/json',
      'X-Forwarded-For':           (req.headers['x-forwarded-for'] as string) ?? req.ip ?? '',
      'X-Forwarded-User-Agent':    req.headers['user-agent'] ?? '',
      'X-Forwarded-Accept-Language': req.headers['accept-language'] ?? '',
    },
  });
  // ...
});
```

### Reverse proxy configuration

If a reverse proxy (nginx, AWS ALB, Cloudflare, etc.) sits between the SPA server and brXM, configure it to forward — not replace — the `X-Forwarded-For` header.

**nginx:**

```nginx
location /site/resourceapi/ {
    proxy_pass          http://brxm:8080;
    proxy_set_header    X-Forwarded-For  $proxy_add_x_forwarded_for;
    proxy_set_header    Host             $host;
}
```

`$proxy_add_x_forwarded_for` appends the connecting IP to any existing XFF chain. The plugin always reads the **leftmost** IP — the original client:

```
X-Forwarded-For: 203.0.113.42, 10.0.0.5, 172.16.0.1
                  ↑
                  used as client_ip in the pixel
```

> **Security note:** If brXM is not behind a controlled proxy, clients could forge `X-Forwarded-For`. Configure your edge proxy to **replace** (not append) the header with the verified connection IP before it reaches brXM.

### Loopback addresses

Loopback IPs (`127.x.x.x`, `::1`, `0:0:0:0:0:0:0:1`) are automatically ignored. If `X-Forwarded-For` resolves to a loopback address, the plugin falls back to `request.getRemoteAddr()`. In local development where all traffic is on localhost, `client_ip` will be empty on pixel events — this is expected.

---

## `editMode` and Channel Manager preview

Every component sets `editMode: boolean`. It is `true` only when rendered inside the brXM Channel Manager preview. In normal delivery it is always `false`.

Use `editMode` to show empty-state placeholders in the Channel Manager while hiding them in the live site:

```tsx
const { products, editMode } = component.getModels<ProductHighlightModels>();

// Hide in delivery if no products; show placeholder in CMS preview
if (!editMode && products.every(p => p === null)) return null;

return editMode && products.every(p => p === null)
  ? <p className="cms-placeholder">Add products using the component panel.</p>
  : <ProductHighlight products={products} />;
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of the Channel Manager "Channel Properties" dialog showing the Discovery component configuration panel.

---

## Document pickers and wizards

The plugin ships Open UI extensions that let CMS editors configure components visually without writing code:

| Wizard | Used for | Document type created |
|---|---|---|
| Product picker wizard | Select a specific product or enable Dynamic mode | `brxdis:productDetailDocument` |
| Category picker wizard | Select a category or enable Dynamic mode | `brxdis:categoryDocument` |
| Product Recommendation wizard | Choose a widget, context, and limit | `brxdis:productRecommendationDocument` |
| Category Recommendation wizard | Choose a category widget | `brxdis:categoryRecommendationDocument` |
| Global Recommendation wizard | Choose a trending/personalised widget | `brxdis:globalRecommendationDocument` |

Editors open a wizard by clicking the document picker in the component properties panel. The wizard walks through a 3-step flow: choose widget type → configure context → review. The result is stored as JSON in `brxdis:config` on the document.

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of the Channel Manager with the "Component Properties" panel open on the right, showing the Product Detail Document picker with a "Choose a product" button and the product search dialog open.

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
| `DiscoverySearchGridComponent` | `pageSize` | `12` | Results per page |
| | `defaultSort` | `""` | Default sort expression (URL `sort` overrides) |
| | `showFacets` | `true` | Include facets, facetUrls, activeFacets in models |
| | `showPagination` | `true` | Include pageUrls in models |
| | `showSort` | `true` | Include sortUrl in models |
| | `showDidYouMean` | `true` | Include didYouMean in models |
| | `autoRedirect` | `false` | Server-side redirect on keyword redirect |
| `DiscoveryCategoryGridComponent` | `document` | - | Category Document picker |
| | `pageSize` | `12` | Results per page |
| | `defaultSort` | `""` | Default sort expression (URL `sort` overrides) |
| | `showFacets` | `true` | Include facets, facetUrls, activeFacets in models |
| | `showPagination` | `true` | Include pageUrls in models |
| | `showSort` | `true` | Include sortUrl in models |
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
| | `categoryUrlParam` | `cid` | URL parameter name for Dynamic mode — used as both the path-segment label (`/category/{slug}/cid/{id}`) and the query-param fallback (`?cid=`) |
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
| Category Document in Dynamic mode, no `cid` in path or `?cid=` query param | `products: null`, `categoryId: ""` |
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

---

## Troubleshooting

### `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`

The site webapp is running a stale version of the plugin. Reinstall the addon locally and rebuild:

```bash
# From the brxm-discovery project root
mvn -DskipTests install

# Then in your host project
mvn clean install
```

### `products: null` on category page

Either:
- No Category Document is configured on `DiscoveryCategoryGridComponent`, or
- The Category Document is in Dynamic mode and the URL does not contain a `cid` path segment or `?cid=` query param.

Open Channel Manager, select the component, and verify the Category Document is linked. For Dynamic mode, confirm the URL matches the sitemap pattern (`/products/{slug}/cid/{id}` or `?cid={id}`).

### `product: null` on PDP

Either:
- No Product Detail Document is configured on `DiscoveryProductDetailComponent`, or
- The document is in Dynamic mode and the URL has no `pid` value.

Confirm the document is linked and the URL matches `/product/{slug}/pid/{id}` or `?pid={id}`.

### Empty product grid — no error shown

Credentials are missing or invalid. Verify with:

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY
```

Check the CMS log for `ConfigurationException` or `SearchException` entries.
