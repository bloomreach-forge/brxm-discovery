# Phase 7B & 7C — Requirements

## Phase 7B — Search Envelope

### B1: "Did You Mean?" / Auto-Correction

**Problem:** Discovery returns `did_you_mean` and `autoCorrectQuery` fields in search responses. The plugin discards them today — editors can do nothing about typo correction.

**Acceptance criteria:**
- `did_you_mean` (String, nullable) is available as a model attribute on every search result page.
- `autoCorrectQuery` (String, nullable) is available as a model attribute on every search result page.
- When both are null, no change in rendered output.
- The bundled `brxdis-search.ftl` renders a "Did you mean: X?" link when `did_you_mean` is present.
- The bundled `brxdis-search.ftl` renders "Showing results for X" when `autoCorrectQuery` is present.
- Unit test: `DiscoveryResponseMapper` correctly maps both fields from the raw API JSON.
- Unit test: model attributes are set on the request when values are present.

---

### B2: Keyword Redirects

**Problem:** Merchandisers configure keyword redirects in the Discovery Dashboard (e.g. "sale" → `/sale-landing-page`). The plugin discards the `keywordRedirect` response field — the redirect never fires and the merchandiser configuration has zero effect.

**Acceptance criteria:**
- A new `autoRedirect` boolean component parameter on `DiscoverySearchComponentInfo` (default `false`).
- When the Discovery response includes a `keywordRedirect` and `autoRedirect=true`: the component issues a server-side HTTP redirect before rendering. No products are fetched or rendered.
- When `autoRedirect=false` (default): `redirectUrl` (String) and `redirectQuery` (String) are set as model attributes. The template or SPA decides how to handle them.
- When `keywordRedirect` is absent in the response, no redirect or model change occurs.
- Unit test: `autoRedirect=true` triggers redirect and skips the rest of `doBeforeRender`.
- Unit test: `autoRedirect=false` sets model attributes without redirecting.
- Unit test: absent `keywordRedirect` → no redirect, no model attributes set.
- The bundled `brxdis-search.ftl` renders a JavaScript redirect snippet when `redirectUrl` is present and `autoRedirect=false`.

---

### B3: `stats.field` — Field Statistics

**Problem:** Price range sliders require knowing the min/max of `price` (or any numeric field) in the current result set. Discovery returns this via `stats`, but the plugin never requests it and discards the response if present.

**Acceptance criteria:**
- A new `statsFields` String component parameter on `DiscoverySearchComponentInfo` and `DiscoveryCategoryComponentInfo` (default `""`). Accepts a comma-separated list of field names (e.g. `"price,sale_price"`).
- When non-blank, the plugin appends `&stats.field=X` for each field to the API request.
- `stats` is surfaced as a `Map<String, FieldStats>` model attribute, where `FieldStats` is a record with `min` (double), `max` (double), `mean` (double), `count` (long).
- When `statsFields` is blank or Discovery returns no stats, the model attribute is an empty map (never null).
- Unit test: `DiscoveryClientImpl.buildSearchPath()` includes `stats.field` params when configured.
- Unit test: `DiscoveryResponseMapper` correctly maps the `stats` section into `Map<String, FieldStats>`.
- Unit test: empty map is set when stats not requested.

---

## Phase 7C — Personalization & Merchandising

### C1: Segment Parameter

**Problem:** Discovery's relevance engine supports customer-segment-aware ranking (e.g. geo, loyalty tier). The plugin never sends the `segment` parameter — all users get undifferentiated results and any segment-based merchandising rules in the Discovery Dashboard are ignored.

**Acceptance criteria:**
- A new `segment` String component parameter on `DiscoverySearchComponentInfo` and `DiscoveryCategoryComponentInfo` (default `""`). Set by authors/developers at the channel level.
- A `seg` URL request parameter is also read at request time. URL param takes precedence over component param.
- When the resolved segment is non-blank, `&segment=` is appended to the Discovery API search and category requests.
- Segment is NOT sent on recommendation or autosuggest requests (those use separate APIs).
- Unit test: URL param overrides component param.
- Unit test: non-blank segment appears in the built search path.
- Unit test: blank segment produces no `segment` query param.

---

### C2: Campaign Banners

**Problem:** Merchandisers configure promotional banners in the Discovery Dashboard tied to specific queries or categories. The plugin discards the `campaign` response field — banners never appear in the storefront.

**Acceptance criteria:**
- A `Campaign` record: `id` (String), `name` (String), `htmlText` (String), `bannerUrl` (String), `imageUrl` (String).
- `campaign` (Campaign, nullable) is surfaced as a model attribute on search and category result pages.
- When `campaign` is null (Discovery returned none), the model attribute is not set / is null.
- No component parameter is needed — if Discovery returns a campaign, it appears automatically.
- Unit test: `DiscoveryResponseMapper` correctly maps the `campaign` section from the raw API JSON.
- Unit test: null campaign → no model attribute set (or null).
- The bundled `brxdis-search.ftl` and `brxdis-category.ftl` render the campaign as an optional banner zone (image link or HTML) above the product grid when `campaign` is non-null.

---

### C3: Exclusion Filter (`efq`)

**Problem:** The `efq` Discovery parameter excludes matching products from results (e.g. hide out-of-stock). Today it is used only internally for PID lookups. Authors cannot exclude products based on attributes without customising the plugin.

**Acceptance criteria:**
- A new `exclusionFilter` String component parameter on `DiscoverySearchComponentInfo` and `DiscoveryCategoryComponentInfo` (default `""`). Accepts a raw Discovery filter expression (e.g. `availability:false`).
- When non-blank, `&efq={expression}` is appended to the Discovery API search and category requests.
- The expression is passed through as-is — no parsing, validation, or encoding beyond what `UriComponentsBuilder` applies.
- Unit test: non-blank `exclusionFilter` appears in the built search path.
- Unit test: blank `exclusionFilter` produces no `efq` query param.

---

## Implementation Order

| Step | Item | Why first |
|------|------|-----------|
| 1 | B3 `stats.field` | Pure addition — no existing behaviour changes; establishes `SearchMetadata` envelope pattern |
| 2 | B1 `did_you_mean` / `autoCorrectQuery` | Builds on the same envelope; read-only display |
| 3 | C2 Campaign banners | Same envelope pass; also read-only |
| 4 | C1 Segment | Query-param threading — touches `SearchQuery`, `CategoryQuery`, `DiscoveryClientImpl`, component infos |
| 5 | C3 `efq` | Same threading pattern as segment; trivial once segment is done |
| 6 | B2 Keyword redirects | Last because it requires component-level control flow change (`autoRedirect` toggle) |
