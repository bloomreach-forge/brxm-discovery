# React / SPA Integration Guide

This guide shows React/JS developers how to consume the brXM Discovery plugin via the **Page Model API**. Every component sets the same data via `request.setModel()` that drives the FTL templates, so a headless front-end receives an identical payload.

---

## How it works

Each Discovery HST component calls `request.setModel(key, value)` alongside `request.setAttribute(key, value)`.
The Page Model API serializes all `setModel` values into the component's `models` object in the JSON response.

```
GET /site/resourceapi/search?q=shirt&page=1
```

```json
{
  "page": {
    "search-bar": {
      "models": {
        "query": "shirt",
        "searchResult": { ... },
        "autosuggestResult": { ... },
        "label": "main"
      }
    },
    "facets": {
      "models": {
        "facets": { ... },
        "label": "main",
        "labelConnected": true
      }
    },
    "product-grid": {
      "models": {
        "products": [ ... ],
        "pagination": { ... },
        "label": "main",
        "labelConnected": true
      }
    }
  }
}
```

With `@bloomreach/react-sdk`, retrieve models per component:

```tsx
import { BrComponent, BrPage } from '@bloomreach/react-sdk';

// inside a mapped component:
const { query, searchResult, autosuggestResult } = component.getModels();
```

---

## TypeScript interfaces

Paste these into a shared `discovery.types.ts` file. They map 1-to-1 to the Java records the components produce.

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

export interface SearchResult {
  products: ProductSummary[];
  total: number;
  page: number;               // 0-based internally; page=1 URL → page:0 JSON
  pageSize: number;
  facets: Record<string, Facet>;
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

export interface SearchMetadata {
  stats: Record<string, FieldStats>;
  didYouMean: string[] | null;
  autoCorrectQuery: string | null;
  redirectUrl: string | null;
  redirectQuery: string | null;
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

export interface SearchComponentModels {
  query: string;
  label: string;
  searchResult: SearchResult | null;
  autosuggestResult: AutosuggestResult | null;
  stats: Record<string, FieldStats>;
  didYouMean: string[] | null;
  autoCorrectQuery: string | null;
  redirectUrl: string | null;
  redirectQuery: string | null;
  suggestionsEnabled: boolean;
  suggestOnlyMode: boolean;
  resultsPage: string;
  placeholder: string;
  minChars: number;
  debounceMs: number;
}

export interface CategoryComponentModels {
  categoryId: string;
  label: string;
  categoryResult: SearchResult;
  stats: Record<string, FieldStats>;
}

export interface ProductGridModels {
  products: ProductSummary[];
  pagination: PaginationModel;
  label: string;
  labelConnected: boolean;
}

export interface FacetModels {
  facets: Record<string, Facet>;
  label: string;
  labelConnected: boolean;
}

export interface RecommendationModels {
  products: ProductSummary[];
  widgetId: string;
  label: string;
  dataSource: string;
}

export interface ProductDetailModels {
  product: ProductSummary | null;
  label: string;
}

export interface AutosuggestComponentModels {
  autosuggestResult: AutosuggestResult | null;
  query: string;
}
```

---

## URL parameters

All URL parameters are read from the public (non-HST) request. Pass them as normal query string params.

### Search (`DiscoverySearchComponent`)

| Parameter | Type | Description |
|---|---|---|
| `q` | string | Search query. Empty string → no API call, null models. |
| `page` | number | **1-indexed.** `page=1` → internal `page=0`. Omit for first page. |
| `sort` | string | Sort expression, e.g. `price asc`, `title desc`. Overrides component default. |
| `filter.{field}` | string (repeatable) | Facet filter. e.g. `filter.brand=Nike&filter.color=Red`. |
| `brxdis_suggest` | `1` | Suggest-only mode: skips full search, populates only `autosuggestResult`. |

```
/site/search?q=shirt&page=2&sort=price+asc&filter.brand=Nike&filter.color=Red
```

### Category browse (`DiscoveryCategoryComponent`)

| Parameter | Type | Description |
|---|---|---|
| `categoryId` | string | Discovery category ID. Used only when no document is configured on the component. |
| `page` | number | 1-indexed page number. |
| `sort` | string | Sort expression. |
| `filter.{field}` | string (repeatable) | Facet filter. |

```
/site/category?categoryId=117417&page=1&filter.brand=Adidas
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
| `pid` | string | Product ID. Read first (unless a document is configured, which wins). |

---

## Component reference

### `DiscoverySearchComponent` → `SearchComponentModels`

Handles both full-text search and inline autosuggest. Place one per search bar on the page.

```tsx
// SearchBar.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { SearchComponentModels } from './discovery.types';

export function SearchBar({ component }: BrComponentContext) {
  const models = component.getModels<SearchComponentModels>();
  const {
    query, searchResult, autosuggestResult,
    suggestionsEnabled, minChars, debounceMs, placeholder,
    didYouMean, autoCorrectQuery, redirectUrl,
  } = models;

  // Redirect on keyword redirect (server already handles autoRedirect=true)
  if (redirectUrl) {
    window.location.href = redirectUrl;
    return null;
  }

  return (
    <form method="get">
      <input
        type="search"
        name="q"
        defaultValue={query}
        placeholder={placeholder}
        // Autosuggest: fire brxdis_suggest=1 requests as the user types
      />
      <button type="submit">Search</button>

      {didYouMean && didYouMean.length > 0 && (
        <p>Did you mean: {didYouMean.map(s => <a key={s} href={`?q=${s}`}>{s}</a>)}</p>
      )}
      {autoCorrectQuery && (
        <p>Showing results for: <strong>{autoCorrectQuery}</strong></p>
      )}

      {/* Autosuggest dropdown — see Autosuggest section below */}
    </form>
  );
}
```

**Key behaviours:**
- `searchResult` is `null` when `q` is blank or in suggest-only mode.
- `autosuggestResult` is `null` when `suggestionsEnabled=false` or `q` is blank.
- `stats` contains `FieldStats` per requested field (price range slider data). Only populated when `statsFields` component param is set (e.g. `price`).

---

### `DiscoveryProductGridComponent` → `ProductGridModels`

A view component — it reads from the search/category cache rather than calling Discovery directly. Place it as a sibling of the data component on the same page with a matching `connectTo` label.

```tsx
// ProductGrid.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductGridModels } from './discovery.types';

export function ProductGrid({ component }: BrComponentContext) {
  const { products, pagination, labelConnected } = component.getModels<ProductGridModels>();

  if (!labelConnected) {
    // No data-fetching component connected — normal in live mode (just return null or a placeholder)
    return null;
  }

  return (
    <div>
      <p>{pagination.total} results</p>

      <div className="product-grid">
        {products.map(p => (
          <ProductCard key={p.id} product={p} />
        ))}
      </div>

      <Pagination
        page={pagination.page}          // 0-based
        totalPages={pagination.totalPages}
      />
    </div>
  );
}

function ProductCard({ product }: { product: ProductSummary }) {
  const brand = product.attributes['brand'] as string | undefined;
  const salePrice = product.attributes['sale_price'] as number | undefined;

  return (
    <article>
      {product.imageUrl && <img src={product.imageUrl} alt={product.title} />}
      <h3><a href={`/product?pid=${product.id}`}>{product.title}</a></h3>
      {brand && <p>{brand}</p>}
      <p>
        {salePrice != null
          ? <><s>{product.currency} {product.price?.toFixed(2)}</s> {product.currency} {salePrice.toFixed(2)}</>
          : <>{product.currency} {product.price?.toFixed(2)}</>
        }
      </p>
    </article>
  );
}
```

**Pagination — building URLs:**
`page` in the JSON is 0-based. The URL parameter `page` is 1-based. Preserve all other query params (q, sort, filter.*) when changing pages.

```ts
function buildPageUrl(targetPage: number, currentSearch: string): string {
  const params = new URLSearchParams(currentSearch);
  params.set('page', String(targetPage + 1));   // 0-based → 1-based URL
  return '?' + params.toString();
}
```

---

### `DiscoveryFacetComponent` → `FacetModels`

Another view component. Reads `facets` from the same request-scope cache as `ProductGrid`.

```tsx
// FacetSidebar.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { FacetModels } from './discovery.types';

export function FacetSidebar({ component }: BrComponentContext) {
  const { facets, labelConnected } = component.getModels<FacetModels>();

  if (!labelConnected || Object.keys(facets).length === 0) return null;

  return (
    <nav aria-label="Filter results">
      {Object.values(facets).map(facet => (
        <FacetGroup key={facet.name} facet={facet} />
      ))}
    </nav>
  );
}

function FacetGroup({ facet }: { facet: Facet }) {
  const params = new URLSearchParams(window.location.search);
  const activeValues = params.getAll(`filter.${facet.name}`);

  return (
    <details open>
      <summary>{facet.name}</summary>
      <ul>
        {facet.value.map(fv => {
          const isActive = activeValues.includes(fv.name);
          const href = buildFacetUrl(facet.name, fv.name, isActive);
          return (
            <li key={fv.name}>
              <a href={href} aria-pressed={isActive}>
                <span>{fv.name}</span>
                <span>({fv.count})</span>
              </a>
            </li>
          );
        })}
      </ul>
    </details>
  );
}

// Toggle a filter value on/off while preserving q, sort, other filters; reset to page 1
function buildFacetUrl(field: string, value: string, currentlyActive: boolean): string {
  const params = new URLSearchParams(window.location.search);
  params.delete('page');
  const key = `filter.${field}`;

  if (currentlyActive) {
    // Remove this specific value (URLSearchParams.delete removes ALL values for the key)
    const remaining = params.getAll(key).filter(v => v !== value);
    params.delete(key);
    remaining.forEach(v => params.append(key, v));
  } else {
    params.append(key, value);
  }

  return '?' + params.toString();
}
```

---

### Label/connectTo wiring

The `label`/`connectTo` system decouples data producers from view consumers on the same page. You don't need to worry about this in your React code — it is purely a server-side routing mechanism that runs before the JSON is produced. The JSON you receive already contains the correctly resolved data.

What matters for your UI:
- `labelConnected: true` means the data source ran successfully for this component's label.
- `labelConnected: false` means no matching data component is on the page — the `products` / `facets` arrays will be empty.

In production delivery `labelConnected` is always `true` (the page is correctly configured). It is only `false` during Channel Manager preview when an editor drags a view component onto a page without the corresponding data component.

---

### `DiscoveryRecommendationComponent` → `RecommendationModels`

```tsx
// RecommendationsCarousel.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { RecommendationModels } from './discovery.types';

export function RecommendationsCarousel({ component }: BrComponentContext) {
  const { products, widgetId } = component.getModels<RecommendationModels>();

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

**Context product resolution order (server-side, fully transparent to the client):**
1. `contextProductId` URL param
2. `contextProductId` component param (Channel Manager)
3. Page content bean `brxdis:pid` property
4. `pid` URL param (PDP pages carry the PID this way)

For a PDP "Similar Items" carousel, just ensure `pid=<productId>` is in the URL — the component picks it up automatically.

---

### `DiscoveryProductDetailComponent` → `ProductDetailModels`

```tsx
// ProductDetail.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductDetailModels } from './discovery.types';

export function ProductDetail({ component }: BrComponentContext) {
  const { product } = component.getModels<ProductDetailModels>();

  if (!product) return <p>Product not found.</p>;

  const brand = product.attributes['brand'] as string | undefined;
  const description = product.attributes['description'] as string | undefined;
  const salePrice = product.attributes['sale_price'] as number | undefined;

  return (
    <article>
      {product.imageUrl && (
        <img src={product.imageUrl} alt={product.title} style={{ aspectRatio: '16/9' }} />
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
      <a href={product.url}>
        <button>Add to Cart</button>
      </a>
    </article>
  );
}
```

**PID resolution order (server-side):**
1. `DiscoveryProductDetailDocument` configured on the component (document picker in Channel Manager) — **wins over URL param**
2. `pid` URL param
3. Page content bean property (advanced — requires `productPidProperty` component param)

For a URL-driven PDP (most common), leave the document param empty in the component config and pass `?pid=<id>` in the URL.

---

### Autosuggest

Autosuggest is built into `DiscoverySearchComponent` — `autosuggestResult` is in the same component model as `searchResult`. For a live typeahead dropdown, make XHR/fetch calls in suggest-only mode while the user types:

```ts
// Fetch suggestions without a full page reload
async function fetchSuggestions(
  query: string,
  resourceApiBase: string
): Promise<AutosuggestResult | null> {
  if (query.length < 2) return null;             // mirrors minChars=2 default

  const url = new URL(resourceApiBase + window.location.pathname);
  url.searchParams.set('q', query);
  url.searchParams.set('brxdis_suggest', '1');   // suggest-only mode

  const res = await fetch(url.toString(), {
    headers: { Accept: 'application/json' }
  });
  const json = await res.json();

  // Navigate to the search bar component in the response tree
  const searchBarModels = json?.page?.['search-bar']?.models;
  return searchBarModels?.autosuggestResult ?? null;
}
```

```tsx
// AutosuggestDropdown.tsx
function AutosuggestDropdown({ result, onSelect }: {
  result: AutosuggestResult;
  onSelect: (query: string) => void;
}) {
  return (
    <div role="listbox">
      {result.querySuggestions.length > 0 && (
        <section>
          <h4>Suggestions</h4>
          {result.querySuggestions.map(s => (
            <div key={s} role="option" onClick={() => onSelect(s)}>{s}</div>
          ))}
        </section>
      )}

      {result.attributeSuggestions.length > 0 && (
        <section>
          <h4>Categories</h4>
          {result.attributeSuggestions.map(a => (
            <div key={`${a.name}:${a.value}`} role="option"
                 onClick={() => onSelect(a.value)}>
              {a.name}: {a.value}
            </div>
          ))}
        </section>
      )}

      {result.productSuggestions.length > 0 && (
        <section>
          <h4>Products</h4>
          {result.productSuggestions.map(p => (
            <a key={p.id} href={`/product?pid=${p.id}`} role="option">
              {p.imageUrl && <img src={p.imageUrl} alt={p.title} width={40} />}
              <span>{p.title}</span>
            </a>
          ))}
        </section>
      )}
    </div>
  );
}
```

**Debouncing** — the `debounceMs` model value (default `250`) comes from the component config. Use it directly:

```ts
const debounceMs = component.getModels<SearchComponentModels>().debounceMs;

const debouncedFetch = useMemo(
  () => debounce(fetchSuggestions, debounceMs),
  [debounceMs]
);
```

---

## Price range slider (stats)

When `statsFields=price` is set on a Search or Category component, `stats.price` is populated:

```ts
interface PriceRangeFilter {
  min: number;
  max: number;
  mean: number;
  count: number;
}

const priceStats = models.stats?.['price'] as FieldStats | undefined;
```

Pass the selected range back as filter params. Discovery uses `fq=price:[min TO max]` syntax — the plugin handles the conversion automatically when you pass `filter.price=100:500` (colon-separated range):

```ts
// Encode a range filter
params.set('filter.price', `${minPrice}:${maxPrice}`);
```

---

## Full search page wiring example

A realistic search page composing the three search components:

```tsx
// SearchPage.tsx
import { BrPage, BrComponent } from '@bloomreach/react-sdk';

const componentMapping = {
  'DiscoverySearchComponent': SearchBar,
  'DiscoveryFacetComponent': FacetSidebar,
  'DiscoveryProductGridComponent': ProductGrid,
};

export function SearchPage() {
  return (
    <BrPage configuration={brConfig} mapping={componentMapping}>
      <div className="search-layout">
        <BrComponent path="main/search" />   {/* search bar + autosuggest */}

        <div className="two-col">
          <aside>
            <BrComponent path="main/sidebar" />  {/* facets */}
          </aside>
          <main>
            <BrComponent path="main/content" />  {/* product grid + pagination */}
          </main>
        </div>
      </div>
    </BrPage>
  );
}
```

The three components share the same `label` value in their HST config (`label=main` for search bar, `connectTo=main` for facets and grid). This is transparent to you — the server wires results into the right models before the JSON is produced.

---

## Category browse

Category pages work identically to search pages. The only differences are:
- The data component is `DiscoveryCategoryComponent` which sets `categoryResult` (same `SearchResult` shape)
- The category ID comes from the document picker or `?categoryId=` URL param
- There is no `q` model — use `categoryId` as the page title/identifier

```tsx
// CategoryHeader.tsx — render what DiscoveryCategoryComponent sets
export function CategoryHeader({ component }: BrComponentContext) {
  const { categoryId, categoryResult } = component.getModels<CategoryComponentModels>();
  return (
    <header>
      <h1>Category: {categoryId}</h1>
      <p>{categoryResult.total} products</p>
    </header>
  );
}
```

The `ProductGrid` and `FacetSidebar` components from the search page work unchanged on a category page — they read from whichever data source (`search` or `category`) is connected via the matching label. No code change needed.

---

## Component parameter reference

These are set in Channel Manager and come through as part of the component's HST configuration. They are **not** in `getModels()` — they drive server-side behaviour only. You do not need to read them in your React components.

| Component | Parameter | Default | Effect |
|---|---|---|---|
| `DiscoverySearchComponent` | `pageSize` | `12` | Results per page |
| | `defaultSort` | `""` | Default sort (URL `sort` overrides) |
| | `label` | `"default"` | Links this component to view components with matching `connectTo` |
| | `suggestionsEnabled` | `true` | Whether autosuggest is active |
| | `resultsPage` | `""` | Redirect search form to a different page |
| `DiscoveryCategoryComponent` | `pageSize` | `12` | Results per page |
| | `label` | `"default"` | Links to view components |
| `DiscoveryProductGridComponent` | `connectTo` | `"default"` | Subscribe to a search or category label |
| `DiscoveryFacetComponent` | `connectTo` | `"default"` | Subscribe to a search or category label |
| `DiscoveryRecommendationComponent` | `connectTo` | `"default"` | Used with `dataSource=productDetailBand` |
| `DiscoveryProductDetailComponent` | `productUrlParam` | `"pid"` | URL param name for the product ID |
| | `label` | `"default"` | Exposes resolved PID to sibling recommendation components |

---

## Error states

| Condition | What you receive |
|---|---|
| `q` is blank | `searchResult: null`, `autosuggestResult: null` |
| No category configured (no document, no `?categoryId=`) | `categoryResult.total: 0`, `categoryResult.products: []` |
| No widget configured on a recommendations component | `products: []`, `widgetId: ""` |
| No PID resolved on a product detail component | `product: null` |
| `labelConnected: false` on grid/facets | `products: []` / `facets: {}` — data component not on page |
| Discovery API unreachable | `SearchException` thrown server-side → component renders error page |
