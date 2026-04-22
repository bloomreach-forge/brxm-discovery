# Bloomreach Discovery for brXM — SPA Integration Guide

This guide is for React and JavaScript developers building headless storefronts on top of brXM with the brxm-discovery plugin. You will learn how to install the plugin, configure credentials, wire all nine components into your SPA, and ship SEO-friendly product URLs.

> **Not using a SPA?** The plugin also ships ready-to-use Freemarker templates (`brxdis-*.ftl`) for HST-rendered sites. This guide focuses entirely on the Page Model API / headless path.

> 📸 **[IMAGE PLACEHOLDER]** — Architecture diagram: brXM + brxm-discovery on the left, Page Model API JSON in the centre, React SPA + Discovery API on the right, showing the full request/response cycle.

---

## How it works

Every Discovery HST component calls `request.setModel()` at render time. When your SPA fetches a page via the brXM **Page Model API** (`/site/resourceapi/...`), the component's data appears in the `models` object for that component in the JSON response.

```
GET /site/resourceapi/search?q=shirt
Accept: application/json
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
        "facets": { "brand": { ... } },
        "facetUrls": { "brand": { "Nike": "?q=shirt&filter.brand=Nike" } },
        "pageUrls": { "0": "?q=shirt", "1": "?q=shirt&page=1" },
        "sortUrl": "?q=shirt",
        "activeFacets": {}
      }
    }
  }
}
```

With the `@bloomreach/react-sdk`, retrieve models per component:

```tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ResultsComponentModels } from './discovery.types';

function SearchResults({ component }: BrComponentContext) {
  const models = component.getModels<ResultsComponentModels>();
  // ...
}
```

The plugin handles everything server-side — Discovery API calls, facet URL building, pagination, pixel firing, and `br_uid2` cookie management. Your SPA just renders the data.

---

## Part 1 — Installation

### Step 1: Add the plugin JARs

The plugin ships two artifacts. Add them to the correct Maven module in your brXM project.

```xml
<!-- Root POM dependencyManagement -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-cms</artifactId>
  <version>0.0.2-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>0.0.2-SNAPSHOT</version>
</dependency>
```

| Artifact | Add to |
|---|---|
| `brxm-discovery-cms` | CMS webapp (`cms/` module) |
| `brxm-discovery-site` | Site webapp (`site/webapp` module) |
| `brxm-discovery-site` | Also `site/components` if that module exists and compiles custom Java against plugin APIs |

> **Note:** You do not need to add `brxm-discovery-hcm-site` or the CRISP addon JARs separately — they are pulled in transitively.

Add the Bloomreach Maven repositories if not already present:

```xml
<repository>
  <id>bloomreach-maven2</id>
  <url>https://maven.bloomreach.com/maven2/</url>
</repository>
<repository>
  <id>bloomreach-maven2-enterprise</id>
  <url>https://maven.bloomreach.com/maven2-enterprise/</url>
</repository>
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a `pom.xml` editor in IntelliJ showing the two dependency blocks added.

### Step 2: Enable the CRISP broker

In your **site webapp** `hst-config.properties`, add:

```properties
crisp.broker.registerService = true
```

This registers the `ResourceServiceBroker` into `HippoServiceRegistry` at startup, which the plugin's service layer needs to make Discovery API calls. Without it, every request will throw a `ConfigurationException`.

### Step 3: What bootstraps automatically

On first startup, HCM applies everything the plugin needs. You do not need to configure these manually:

| What | JCR path |
|---|---|
| `brxdis` namespace + document types | `/hippo:namespaces/brxdis` |
| Picker REST daemon module | `/hippo:configuration/hippo:modules/brxm-discovery` |
| Open UI extensions (product/category/widget pickers) | `/hippo:configuration/hippo:frontend/cms/ui-extensions/...` |
| CRISP resource spaces (search, pathways, autosuggest) | `/hippo:configuration/hippo:modules/crispregistry/...` |
| Bundled FTL templates | `/hst:hst/hst:configurations/hst:default/hst:templates/brxdis-*` |

### Step 4: Verify startup

After starting the site and CMS webapps, check the CMS logs for:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

And verify the picker endpoint responds:

```
GET http://localhost:8080/cms/ws/discovery/picker/search
```

A JSON response (not a 404) confirms the CMS daemon is running.

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of IntelliJ/terminal log output showing both startup confirmation lines.

---

## Part 2 — Configuration

### Global Discovery config node

All channels share one JCR configuration node at a fixed path. Create it once in your HCM application or development module:

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'
      brxdis:apiKey: ''
      brxdis:authKey: ''
      brxdis:baseUri: 'https://core.dxpapi.com'
      brxdis:pathwaysBaseUri: 'https://pathways.dxpapi.com'
      brxdis:autosuggestBaseUri: 'https://suggest.dxpapi.com'
      brxdis:environment: 'PRODUCTION'
      brxdis:defaultPageSize: 12
      brxdis:defaultSort: ''
```

Leave `brxdis:apiKey` and `brxdis:authKey` **blank**. Inject secrets via environment variables instead (see below).

> **Note:** The config node is optional. If it does not exist, the plugin falls back entirely to environment variables and JVM system properties. For local development you can skip the JCR node entirely.

### Credential resolution

The plugin resolves credentials in this order — highest priority wins:

| Priority | Source | Examples |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY` |
| 2 | JVM system property | `-Dbrxdis.accountId=...`, `-Dbrxdis.domainKey=...`, `-Dbrxdis.apiKey=...` |
| 3 (lowest) | JCR global config node | `brxdis:accountId`, `brxdis:domainKey`, `brxdis:apiKey` |

**Recommended production setup:**

```bash
# Kubernetes / Docker env vars
BRXDIS_ACCOUNT_ID=6413
BRXDIS_DOMAIN_KEY=pacifichome
BRXDIS_API_KEY=<secret>          # never store in JCR
BRXDIS_AUTH_KEY=<secret>         # only needed for v2 Pathways recommendations
```

Store `accountId` and `domainKey` in the JCR node as a non-secret fallback. Keep secrets (`apiKey`, `authKey`) in environment variables only.

**For local development**, pass as JVM args:

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY
```

### Staging environment

Set `brxdis:environment: STAGING` on the config node (or `-Dbrxdis.environment=STAGING`) to automatically switch all three CRISP base URIs to the Discovery staging endpoints. No other changes needed.

For custom endpoints (private cloud, proxy):

```yaml
brxdis:baseUri: 'https://custom-core.example'
brxdis:pathwaysBaseUri: 'https://custom-pathways.example'
brxdis:autosuggestBaseUri: 'https://custom-suggest.example'
```

### Per-channel credential overrides

For multi-brand deployments where each channel needs different Discovery credentials, extend `DiscoveryChannelInfo` in your project and set the following properties on `hst:channelinfo`:

| Property | Purpose |
|---|---|
| `discoveryAccountId` | Override account ID for this channel only |
| `discoveryDomainKey` | Override domain key for this channel only |
| `discoveryApiKeyEnvVar` | Name of the env var holding this channel's API key |
| `discoveryAuthKeyEnvVar` | Name of the env var holding this channel's Pathways auth key |

```yaml
/hst:hst/hst:configurations/my-channel/hst:workspace/hst:channel/hst:channelinfo:
  jcr:primaryType: hst:channelinfo
  discoveryAccountId: '9001'
  discoveryDomainKey: my-brand
  discoveryApiKeyEnvVar: MY_BRAND_API_KEY
```

> 📸 **[IMAGE PLACEHOLDER]** — Channel Manager screenshot showing the "Channel Properties" dialog with the Discovery credential override fields filled in.

---

## Part 3 — How the Page Model API works

Every Discovery component populates a `models` key in the Page Model API JSON. Your SPA reads models with `component.getModels<T>()` from `@bloomreach/react-sdk`.

### Component overview

| HST component class | Template | TypeScript model | Purpose |
|---|---|---|---|
| `DiscoverySearchGridComponent` | `brxdis-results` | `ResultsComponentModels` | Keyword search results with facets, pagination, sort |
| `DiscoveryCategoryGridComponent` | `brxdis-results` | `ResultsComponentModels` | Category browse with facets, pagination, sort |
| `DiscoverySearchInputComponent` | `brxdis-search-input` | `SearchInputModels` | Search bar + autosuggest |
| `DiscoveryProductRecommendationComponent` | `brxdis-recommendations-product` | `RecommendationModels` | Product recommendations (v1 or v2 Pathways API) |
| `DiscoveryCategoryRecommendationComponent` | `brxdis-recommendations-category` | `RecommendationModels` | Category recommendations |
| `DiscoveryGlobalRecommendationComponent` | `brxdis-recommendations-global` | `RecommendationModels` | Global / trending recommendations |
| `DiscoveryProductDetailComponent` | `brxdis-product-detail` | `ProductDetailModels` | Single product detail page |
| `DiscoveryProductHighlightComponent` | `brxdis-product-highlight` | `ProductHighlightModels` | Up to 4 hand-picked editorial products |
| `DiscoveryCategoryHighlightComponent` | `brxdis-category-highlight` | `CategoryHighlightModels` | Up to 4 curated category tiles |

> 📸 **[IMAGE PLACEHOLDER]** — Side-by-side screenshot: brXM Channel Manager showing component list on the left, corresponding rendered SPA search page on the right.

### TypeScript interfaces

Create a shared `discovery.types.ts` file and paste in the complete type definitions. These match exactly what the plugin serialises via `request.setModel()`.

```ts
// ── Core models ──────────────────────────────────────────────────────────────

export interface ProductSummary {
  id: string;
  title: string;
  url: string;             // Discovery catalog URL — use buildProductUrl() for SPA navigation
  imageUrl: string;
  price: number | null;
  currency: string;
  attributes: Record<string, unknown>;  // brand, description, sale_price, etc.
}

export interface FacetValue {
  name: string;
  count: number;
  catId: string | null;    // populated on category facets only
  crumb: string | null;
  treePath: string | null;
  parent: string | null;
}

export interface Facet {
  name: string;
  type: string;            // "text", "number", "date", ...
  value: FacetValue[];
}

export interface PaginationModel {
  total: number;
  page: number;            // 0-based
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
  dataSourceMode: 'search' | 'category';

  // Search mode
  query: string;
  didYouMean: string[] | null;
  autoCorrectQuery: string | null;
  redirectUrl: string | null;
  redirectQuery: string | null;

  // Category mode
  categoryId: string;
  displayName: string;

  // Shared
  products: ProductSummary[] | null;
  pagination: PaginationModel;
  stats: Record<string, FieldStats>;

  // Pre-built navigation URLs — just use as href values
  facetUrls: Record<string, Record<string, string>> | null;
  activeFacets: Record<string, string[]> | null;
  clearAllFiltersUrl: string | null;
  pageUrls: Record<number, string> | null;
  sortUrl: string | null;
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
  product: ProductSummary | null;
  pid: string;
  document: unknown | null;  // CMS-internal — do not use; use `product` and `pid`
  editMode: boolean;
}

export interface ProductHighlightModels {
  products: (ProductSummary | null)[];  // up to 4 slots; null = unfilled
  productBeans: unknown[];              // CMS-internal — do not use
  editMode: boolean;
}

export interface CategoryHighlight {
  categoryId: string;
  displayName: string;
  productPreviewCount: number;  // 0–4
}

export interface CategoryHighlightModels {
  categories: CategoryHighlight[];
  previewProducts: Record<string, ProductSummary[]>;  // keyed by categoryId
  editMode: boolean;
}
```

---

## Part 4 — Component reference

### 4.1 Search Results

**Component:** `DiscoverySearchGridComponent`
**Model:** `ResultsComponentModels` with `dataSourceMode: 'search'`

The search results component handles everything server-side: it queries Discovery, builds facet toggle URLs, pagination URLs, sort URLs, and applies auto-correction. Your SPA only needs to render the output.

**URL parameters:**

| Parameter | Type | Description |
|---|---|---|
| `q` | string | Search query. Blank → `products: null`. |
| `page` | number | Page number (0-based in the URL). |
| `sort` | string | Sort expression, e.g. `price asc`, `name desc`. |
| `filter.{field}` | string (repeatable) | Active facet filter, e.g. `filter.brand=Nike`. |

```
/search?q=shirt&page=1&sort=price+asc&filter.brand=Nike
```

> **Note:** You rarely need to build these URLs manually. The server provides `facetUrls`, `pageUrls`, and `sortUrl` as ready-to-use strings — just render `<a href={url}>`.

```tsx
// SearchResults.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ResultsComponentModels, Facet } from './discovery.types';

export function SearchResults({ component }: BrComponentContext) {
  const {
    query, products, pagination,
    facets, facetUrls, activeFacets, clearAllFiltersUrl,
    pageUrls, sortUrl,
    didYouMean, autoCorrectQuery, redirectUrl,
    campaign, editMode,
  } = component.getModels<ResultsComponentModels>();

  if (redirectUrl) {
    // Server-side keyword redirect (only when autoRedirect=true on component)
    window.location.href = redirectUrl;
    return null;
  }

  if (!products || products.length === 0) {
    return <p>No results{query ? ` for "${query}"` : ''}.</p>;
  }

  return (
    <div className="search-layout">
      {campaign?.imageUrl && (
        <aside className="campaign-banner">
          <img src={campaign.imageUrl} alt={campaign.name} />
        </aside>
      )}

      {autoCorrectQuery && (
        <p>Showing results for: <strong>{autoCorrectQuery}</strong></p>
      )}
      {!autoCorrectQuery && didYouMean && didYouMean.length > 0 && (
        <p>Did you mean:{' '}
          {didYouMean.map(s => (
            <a key={s} href={`?q=${encodeURIComponent(s)}`}>{s}</a>
          ))}
        </p>
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

**Facets, pagination, and sort — pre-built URLs:**

```tsx
// FacetSidebar.tsx
function FacetSidebar({ facets, facetUrls, activeFacets, clearAllUrl }: {
  facets: Record<string, Facet>;
  facetUrls: Record<string, Record<string, string>>;
  activeFacets: Record<string, string[]>;
  clearAllUrl: string;
}) {
  const hasActive = Object.keys(activeFacets).length > 0;

  return (
    <nav aria-label="Filter results">
      {hasActive && <a href={clearAllUrl}>Clear all filters</a>}
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
                    {fv.name} ({fv.count})
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

// Pagination.tsx
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

// SortBar.tsx
const SORT_OPTIONS = [
  { label: 'Relevance', value: '' },
  { label: 'Price: low to high', value: 'price asc' },
  { label: 'Price: high to low', value: 'price desc' },
  { label: 'Name A–Z', value: 'name asc' },
];

function SortBar({ sortUrl }: { sortUrl: string }) {
  return (
    <select onChange={e => {
      const value = e.target.value;
      window.location.href = value
        ? `${sortUrl}&sort=${encodeURIComponent(value)}`
        : sortUrl;
    }}>
      {SORT_OPTIONS.map(opt => (
        <option key={opt.value} value={opt.value}>{opt.label}</option>
      ))}
    </select>
  );
}
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a search results page with the facet sidebar on the left, product grid in the centre, and pagination at the bottom. Annotate the three pre-built URL types (facet, pagination, sort).

---

### 4.2 Category Browse

**Component:** `DiscoveryCategoryGridComponent`
**Model:** `ResultsComponentModels` with `dataSourceMode: 'category'`

The category browse component uses the same `ResultsComponentModels` shape as search. Use `dataSourceMode` to distinguish between them in a shared React component.

**URL parameters:**

| Parameter | Type | Description |
|---|---|---|
| `cid` | string | Discovery category ID. Read from path (`/cid/{id}`) first, then `?cid=`. |
| `page` | number | 0-based page number. |
| `sort` | string | Sort expression. |
| `filter.{field}` | string (repeatable) | Facet filter. |

```
/products/womens-shoes/cid/117417?filter.brand=Adidas
```

**Category document modes:**

- **Pinned** — the Category Document in the Channel Manager stores a specific `brxdis:categoryId`. The same category is always shown regardless of URL.
- **Dynamic** — the Category Document has no pinned ID. The component reads `cid` from the URL path or `?cid=` query param at render time. Use this for a single template that serves all categories.

The `categoryId` and `displayName` fields in the model come from the Discovery API response, not from CMS content, so they reflect the live category name.

```tsx
// CategoryPage.tsx — shares SearchResults component; check dataSourceMode
export function CategoryPage({ component }: BrComponentContext) {
  const { dataSourceMode, categoryId, displayName, products } =
    component.getModels<ResultsComponentModels>();

  if (!products || products.length === 0) {
    return <p>No products in this category.</p>;
  }

  return (
    <>
      <h1>{displayName || categoryId}</h1>
      {/* Reuse the same SearchResults layout */}
      <SearchResults component={component} />
    </>
  );
}
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a category browse page with the category name as a heading, facet sidebar, and product grid.

---

### 4.3 Search Bar + Autosuggest

**Component:** `DiscoverySearchInputComponent`
**Model:** `SearchInputModels`

The search bar is a standalone component placed in any page zone (header, sidebar). It does not fetch search results itself — it submits a form to the page where `DiscoverySearchGridComponent` runs.

**Component parameters (set in Channel Manager):**

| Parameter | Default | Description |
|---|---|---|
| `placeholder` | `Search...` | Input placeholder text |
| `resultsPage` | (blank) | Submit to this path; blank = current page |
| `suggestionsEnabled` | `true` | Fetch autosuggest and populate `autosuggestResult` |
| `suggestionsLimit` | `5` | Max suggestions per group |
| `minChars` | `2` | Min characters before fetching suggestions |
| `debounceMs` | `250` | Debounce delay for suggestion requests (ms) |

**Basic render:**

```tsx
// SearchBar.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { SearchInputModels } from './discovery.types';

export function SearchBar({ component }: BrComponentContext) {
  const {
    query, placeholder, resultsPage,
    suggestionsEnabled, autosuggestResult,
  } = component.getModels<SearchInputModels>();

  return (
    <form method="get" action={resultsPage || undefined}>
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

**Live typeahead as the user types:**

For a live dropdown, call the Page Model API on the search bar's own page while the user types. The `autosuggestResult` in the model updates with each request.

```ts
async function fetchSuggestions(
  query: string,
  resourceApiBase: string,
  searchBarPagePath: string
): Promise<AutosuggestResult | null> {
  if (query.length < 2) return null;

  const url = new URL(resourceApiBase + searchBarPagePath);
  url.searchParams.set('q', query);

  const res = await fetch(url.toString(), { headers: { Accept: 'application/json' } });
  const json = await res.json();

  // Navigate to the search bar component in the JSON response
  return json?.page?.['search-bar']?.models?.autosuggestResult ?? null;
}
```

Use `debounceMs` from the model to control the debounce delay:

```tsx
const { debounceMs } = component.getModels<SearchInputModels>();

const debouncedFetch = useMemo(
  () => debounce(fetchSuggestions, debounceMs),
  [debounceMs]
);
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a search bar with an open autosuggest dropdown showing three sections: "Suggestions" (query completions), "Categories" (attribute suggestions), and "Products" (product thumbnails with titles).

---

### 4.4 Product Recommendations

**Component:** `DiscoveryProductRecommendationComponent`
**Model:** `RecommendationModels`

Shows products recommended based on the item currently being viewed. Supports both v1 Discovery API and v2 Pathways API — switching is automatic: configure `authKey` (`BRXDIS_AUTH_KEY`) to enable v2.

**Context product resolution (server-side, transparent to client):**
1. `contextProductId` URL parameter
2. `contextProductId` set in the Recommendation Document in the Channel Manager
3. Product Detail component on the same page (when "Link to Product Detail" is checked)
4. Page content bean `brxdis:pid` property

**PDP "Similar Items" pattern:**

Place `DiscoveryProductDetailComponent` and `DiscoveryProductRecommendationComponent` on the same page. Set both to Dynamic mode. When the user navigates to `/product/slug/pid/SKU-123`, both components read `SKU-123` from the URL automatically — no extra wiring needed.

```tsx
// RecommendationsCarousel.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { RecommendationModels } from './discovery.types';
import { buildProductUrl } from './utils/slugify';

export function RecommendationsCarousel({ component }: BrComponentContext) {
  const { products } = component.getModels<RecommendationModels>();

  if (products.length === 0) return null;

  return (
    <section aria-label="Recommended products">
      <h2>You may also like</h2>
      <div className="carousel-track">
        {products.map(p => (
          <a
            key={p.id}
            href={buildProductUrl(p.id, p.title)}
            className="carousel-card"
          >
            {p.imageUrl && <img src={p.imageUrl} alt={p.title} />}
            <p className="product-title">{p.title}</p>
            {p.price != null && (
              <p className="product-price">{p.currency} {p.price.toFixed(2)}</p>
            )}
          </a>
        ))}
      </div>
    </section>
  );
}
```

**Component parameters:**

| Parameter | Default | Description |
|---|---|---|
| `document` | — | Recommendation Document picker (configured via the recommendation wizard) |
| `limit` | `8` | Max recommended products |
| `showPrice` | `true` | FTL template shows price (for SPA, use `product.price` directly) |
| `showDescription` | `false` | FTL template shows description |
| `productUrlParam` | `pid` | URL param name for Dynamic mode (change to `sku` to use `?sku=` instead) |

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a product detail page with a "You may also like" horizontal scrolling carousel of product cards below the main product content.

---

### 4.5 Category Recommendations

**Component:** `DiscoveryCategoryRecommendationComponent`
**Model:** `RecommendationModels`

Identical to product recommendations, but the context is a category ID. Use this for "trending in this category" carousels on category browse pages.

The context category resolves from: URL path segment (`/cid/{id}`) → `?cid=` query param → pinned category in the Recommendation Document.

**Component parameters:**

| Parameter | Default | Description |
|---|---|---|
| `document` | — | Category Recommendation Document picker |
| `limit` | `8` | Max recommended products |
| `categoryUrlParam` | `cid` | URL param name — used as both path-segment label and query-param fallback |

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a category page with a "Trending in Women's Shoes" product carousel at the bottom.

---

### 4.6 Global / Trending Recommendations

**Component:** `DiscoveryGlobalRecommendationComponent`
**Model:** `RecommendationModels`

Context-free recommendations — bestsellers, trending, "just for you" (personalised), recently viewed, past purchases. If no widget ID is set, the component auto-resolves the first enabled widget of the appropriate type from Discovery.

**Component parameters:** same as `DiscoveryProductRecommendationComponent` minus `productUrlParam`.

```tsx
// GlobalRecommendations.tsx — same RecommendationsCarousel component works here
export function TrendingNow({ component }: BrComponentContext) {
  const { products, widgetId } = component.getModels<RecommendationModels>();
  if (products.length === 0) return null;
  return <RecommendationsCarousel products={products} title="Trending Now" />;
}
```

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a homepage section with a "Trending Now" label and a horizontal product carousel.

---

### 4.7 Product Detail Page

**Component:** `DiscoveryProductDetailComponent`
**Model:** `ProductDetailModels`

Fetches a single product from Discovery by PID. A `brxdis:productDetailDocument` must be attached to the component — this document controls whether the PID is pinned or read from the URL.

**Document modes:**

- **Pinned** — the product wizard stores a specific PID. The same product is always shown.
- **Dynamic** — no pinned PID. The component reads `?pid=` (or the configured `productUrlParam`) from the URL. Use this for a single PDP template that works for all products.

```tsx
// ProductDetail.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductDetailModels } from './discovery.types';

export function ProductDetail({ component }: BrComponentContext) {
  const { product, pid, editMode } = component.getModels<ProductDetailModels>();

  if (!product) {
    if (editMode) {
      return <p>Configure a Product Detail Document in the component properties panel.</p>;
    }
    return null;
  }

  const brand = product.attributes['brand'] as string | undefined;
  const description = product.attributes['description'] as string | undefined;
  const salePrice = product.attributes['sale_price'] as number | undefined;
  const hasSale = salePrice != null && salePrice < (product.price ?? Infinity);

  return (
    <article>
      {product.imageUrl && (
        <img src={product.imageUrl} alt={product.title} />
      )}
      <h1>{product.title}</h1>
      {brand && <p className="brand">{brand}</p>}

      <p className="price">
        {hasSale ? (
          <>
            <s>{product.currency} {product.price?.toFixed(2)}</s>
            {' '}
            <strong>{product.currency} {salePrice!.toFixed(2)}</strong>
          </>
        ) : (
          <>{product.currency} {product.price?.toFixed(2)}</>
        )}
      </p>

      {description && <p className="description">{description}</p>}
    </article>
  );
}
```

**Error states:**

| Situation | What you receive |
|---|---|
| No document configured on component | `product: null`, `pid: ""` |
| Dynamic mode, no `?pid=` in URL | `product: null`, `pid: ""` |
| Dynamic mode, `?pid=` present but product not in Discovery | `product: null`, `pid: "<id>"` |
| Pinned mode, product not in Discovery | `product: null`, `pid: "<pinned-id>"` |

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a product detail page showing the product image on the left, and title, brand, sale price (with strikethrough original), and description on the right.

**Component parameters:**

| Parameter | Default | Description |
|---|---|---|
| `document` | — | Product Detail Document picker (required; use product wizard) |
| `productUrlParam` | `pid` | URL param name for Dynamic mode |

---

### 4.8 Product Highlight (Editorial)

**Component:** `DiscoveryProductHighlightComponent`
**Model:** `ProductHighlightModels`

Up to 4 hand-picked products curated by editors in the Channel Manager. Products are fetched individually from Discovery by PID at render time. Use this for hero sections, featured sale items, or campaign placements.

```tsx
// ProductHighlight.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { ProductHighlightModels } from './discovery.types';
import { buildProductUrl } from './utils/slugify';

export function ProductHighlight({ component }: BrComponentContext) {
  const { products, editMode } = component.getModels<ProductHighlightModels>();
  const filled = products.filter((p): p is NonNullable<typeof p> => p !== null);

  if (!editMode && filled.length === 0) return null;

  return (
    <section className="product-highlight">
      {filled.map(p => (
        <article key={p.id}>
          {p.imageUrl && <img src={p.imageUrl} alt={p.title} />}
          <h3>
            <a href={buildProductUrl(p.id, p.title)}>{p.title}</a>
          </h3>
          <p>{p.currency} {p.price?.toFixed(2)}</p>
        </article>
      ))}
    </section>
  );
}
```

> **Note:** `products` always has up to 4 items. Unfilled slots are `null`. Filter them before rendering unless you want to show placeholders in edit mode.

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a homepage "Featured Products" section with a 4-column grid of hand-picked product cards.

**Component parameters:** `document1` through `document4` — each is a Product Detail Document picker.

---

### 4.9 Category Highlight

**Component:** `DiscoveryCategoryHighlightComponent`
**Model:** `CategoryHighlightModels`

Up to 4 curated category tiles, each with optional product thumbnail previews fetched from Discovery. Use this for homepage navigation tiles ("Shop Women's", "Shop Men's").

```tsx
// CategoryHighlight.tsx
import type { BrComponentContext } from '@bloomreach/react-sdk';
import type { CategoryHighlightModels } from './discovery.types';
import { buildCategoryUrl } from './utils/slugify';

export function CategoryHighlight({ component }: BrComponentContext) {
  const { categories, previewProducts, editMode } = component.getModels<CategoryHighlightModels>();

  if (!editMode && categories.length === 0) return null;

  return (
    <nav className="category-highlight">
      {categories.map(cat => {
        const previews = previewProducts[cat.categoryId] ?? [];
        const href = buildCategoryUrl(cat.categoryId, cat.displayName || cat.categoryId);

        return (
          <a key={cat.categoryId} href={href} className="category-tile">
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

> **Note:** `previewProducts` is keyed by `categoryId` and is empty when all `productPreviewCount` values are `0`. Preview products are served from a JVM-level cache with a ~5-minute TTL.

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of a homepage category tile strip: four tiles side by side, each with 3 product thumbnail images and the category name below.

**Component parameters:** `document1` through `document4` — each is a Category Document picker.

---

## Part 5 — SEO-Friendly URLs

The plugin generates clean, crawlable path URLs for product and category pages.

| Page | URL pattern | Example |
|---|---|---|
| Product detail | `/product/{title-slug}/pid/{pid}` | `/product/classic-cotton-t-shirt/pid/SKU-123` |
| Category browse | `/products/{name-slug}/cid/{id}` | `/products/womens-shoes/cid/117417` |
| Search results | `/search?q={term}` | `/search?q=shirt` |

The slug segment is decorative — components use only the value after the label (`pid` or `cid`) for lookup. Old `?pid=` and `?cid=` query-param URLs continue to work as a fallback.

### URL helper utilities

Add these to a shared file (e.g. `utils/slugify.ts`):

```ts
// utils/slugify.ts

export function slugify(text: string): string {
  const s = (text ?? '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return s || 'product';
}

/** SEO product URL: /product/{slug}/pid/{id} */
export function buildProductUrl(id: string, title: string): string {
  return `/product/${slugify(title)}/pid/${encodeURIComponent(id)}`;
}

/** SEO category URL: /products/{slug}/cid/{id} */
export function buildCategoryUrl(id: string, displayName: string): string {
  return `/products/${slugify(displayName)}/cid/${encodeURIComponent(id)}`;
}
```

Use these whenever you build navigation links from a `ProductSummary` or `CategoryHighlight`:

```ts
// Product card link
<a href={buildProductUrl(product.id, product.title)}>{product.title}</a>

// Category tile link
<a href={buildCategoryUrl(cat.categoryId, cat.displayName || cat.categoryId)}>
  {cat.displayName}
</a>
```

### Required sitemap configuration

The brXM sitemap needs explicit nested `_any_` wildcards to resolve multi-segment SEO paths. Add these entries to your `sitemap.yaml`:

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:sitemap:
      /product:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/product-detail-page
        /_any_:                          # matches {title-slug}
          jcr:primaryType: hst:sitemapitem
          hst:componentconfigurationid: hst:pages/product-detail-page
          /pid:
            jcr:primaryType: hst:sitemapitem
            /_any_:                      # matches {pid}
              jcr:primaryType: hst:sitemapitem
              hst:componentconfigurationid: hst:pages/product-detail-page
      /products:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/category-page
        /_any_:                          # matches {name-slug}
          jcr:primaryType: hst:sitemapitem
          hst:componentconfigurationid: hst:pages/category-page
          /cid:
            jcr:primaryType: hst:sitemapitem
            /_any_:                      # matches {category-id}
              jcr:primaryType: hst:sitemapitem
              hst:componentconfigurationid: hst:pages/category-page
```

The flat `/product` and `/products` sitemap entries are retained to handle `?pid=` / `?cid=` query-param fallback requests.

> **If you change `productUrlParam` or `categoryUrlParam`** on the component, update the fixed label segment in the sitemap accordingly (e.g. if `productUrlParam = "sku"`, replace `/pid:` with `/sku:`).

### Slug stability

Product and category slugs are derived from titles returned by Discovery at render time. If a product title changes in Discovery, the slug in newly generated links changes on the next render. The PID or category ID is the stable identifier — old slugs still resolve because the component ignores the slug and uses only the ID for lookup.

For canonical `<link>` tags, use the current slug form (re-rendered each request) rather than a stored slug.

> 📸 **[IMAGE PLACEHOLDER]** — Split screenshot: browser address bar showing `/product/classic-cotton-t-shirt/pid/SKU-123` (new SEO URL) above `/product?pid=SKU-123` (old query-param URL), with a note that both resolve to the same page.

---

## Part 6 — Pixel Tracking

Pixel tracking is **passive from the SPA's perspective** — the plugin fires all analytics pixels server-side. Your frontend does not need to call any tracking endpoints.

**What fires automatically:**
- Search event on each search request
- Category view event on each category browse request
- Product view event when `DiscoveryProductDetailComponent` renders
- Recommendation widget impression/click events on recommendation renders

### `br_uid2` cookie

The plugin sets a `br_uid2` visitor identity cookie on the first request via `Set-Cookie`. The browser sends it automatically on all subsequent requests. Your SPA does not need to read, write, or manage this cookie.

### Channel-level controls (Channel Manager)

In the Channel Manager channel properties, the following flags control pixel behaviour per channel:

| Property | Default | Description |
|---|---|---|
| `discoveryPixelsEnabled` | `true` | Enable or disable all pixel firing for this channel |
| `discoveryPixelTestData` | `false` | Mark events as test data (excluded from production analytics) |
| `discoveryPixelDebug` | `false` | Enable verbose pixel logging in the CMS logs |

### Environment-level kill switches

Set globally via JVM system properties:

```properties
brxdis.pixel.envEnabled=false      # disable pixels in all channels (e.g. non-prod environments)
brxdis.pixel.testData=true         # mark all events as test data
```

### Verifying pixel events

Check the CMS/site logs for entries prefixed `[Pixel]`. In debug mode (`discoveryPixelDebug=true`), the full pixel request URL is logged per event.

> 📸 **[IMAGE PLACEHOLDER]** — Screenshot of the Channel Manager "Channel Properties" dialog showing the three pixel tracking checkboxes (Pixels Enabled, Test Data, Debug).

---

## Part 7 — Channel Manager / CMS Integration

### `editMode`

Every component includes `editMode: boolean` in its model. It is `true` only when the page is rendered inside the brXM Channel Manager preview. In all delivery requests it is `false`.

Use `editMode` to show empty-state placeholders in the Channel Manager while hiding them in the live site:

```tsx
const { products, editMode } = component.getModels<ProductHighlightModels>();

// Hide in delivery if no products; show placeholder in CMS preview
if (!editMode && products.every(p => p === null)) return null;

return editMode && products.every(p => p === null)
  ? <p className="cms-placeholder">Add products using the component panel.</p>
  : <ProductHighlight products={products} />;
```

### Document pickers and wizards

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

## Part 8 — Complete Wiring Example

Here is the complete component mapping and page wiring for a full e-commerce SPA.

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

**Route examples:**

| Route | What to wire |
|---|---|
| `/search` | `DiscoverySearchGridComponent` + `DiscoverySearchInputComponent` (header) |
| `/products/{slug}/cid/{id}` | `DiscoveryCategoryGridComponent` + `DiscoveryGlobalRecommendationComponent` |
| `/product/{slug}/pid/{id}` | `DiscoveryProductDetailComponent` + `DiscoveryProductRecommendationComponent` |
| Homepage | `DiscoveryCategoryHighlightComponent` + `DiscoveryProductHighlightComponent` + `DiscoveryGlobalRecommendationComponent` |

> 📸 **[IMAGE PLACEHOLDER]** — Side-by-side screenshot: Channel Manager page structure panel on the left listing Discovery components, and the corresponding `componentMapping.ts` file open in VS Code on the right.

---

## Part 9 — Troubleshooting

### `ConfigurationException: CRISP ResourceServiceBroker not found`

`crisp.broker.registerService = true` is missing from the **site webapp** `hst-config.properties`. Add it and restart the site webapp.

### `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`

The site webapp is running a stale version of the plugin. Reinstall the addon locally and rebuild:

```bash
# From the brxm-discovery project root
mvn -DskipTests install

# Then in your host project
mvn clean install
```

### `No resource space for 'discoverySearchAPI'`

The CRISP resolver wiring from an older addon snapshot is still loaded. Rebuild and redeploy the site webapp so the current resolver assembly takes effect.

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

### CRISP calling wrong base URI

Verify `brxdis:environment` on the config node or confirm that explicit `brxdis:baseUri` / `brxdis:pathwaysBaseUri` / `brxdis:autosuggestBaseUri` properties are set correctly. The resolved URIs are logged at `DEBUG` level on the first request after a cache invalidation.

---

## Price range slider (advanced)

When the `Statistics fields` component parameter is set to `price` on a results component, the `stats.price` field is populated:

```ts
const priceStats = models.stats?.['price'] as FieldStats | undefined;
// { min: 9.99, max: 499.99, mean: 89.50, count: 142 }
```

Pass a selected range back as a filter using Discovery's `{min}:{max}` syntax:

```ts
params.set('filter.price', `${minPrice}:${maxPrice}`);
// e.g. filter.price=10:100
```

The plugin converts this to the Discovery `fq=price:[10 TO 100]` format automatically.

---

## What's next

| Guide | Topic |
|---|---|
| [`02-discovery-config.md`](02-discovery-config.md) | Full credential and structural config reference |
| [`04-recommendations.md`](04-recommendations.md) | v2 Pathways API, recommendation document types, widget auto-resolution |
| [`09-pixel-tracking.md`](09-pixel-tracking.md) | Pixel tracking in depth — channel flags, environment kill switches, verification |
| [`10-architecture.md`](10-architecture.md) | Plugin internals — Spring beans, caching layers, CRISP resolver chain |
| [`11-seo.md`](11-seo.md) | SEO URL patterns — sitemap YAML, slug algorithm, configurable URL param names |
