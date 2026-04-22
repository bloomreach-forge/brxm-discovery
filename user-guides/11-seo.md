# SEO-Friendly URLs

## Overview

The plugin generates clean, crawlable path-based URLs for product detail and category browse pages. Search results remain query-parameter-based (`?q=`), which is the standard for search engines.

| Page | URL pattern | Example |
|---|---|---|
| Product detail | `/product/{title-slug}/pid/{pid}` | `/product/classic-cotton-t-shirt/pid/SKU-123` |
| Category browse | `/category/{name-slug}/cid/{id}` | `/category/womens-shoes/cid/117417` |
| Search results | `/search?q={term}` | `/search?q=shirt` |

The label segment (`pid`, `cid`) is the URL parameter name configured on the component (default `pid` / `cid`). The slug segment is for readability only — components use the value after the label to resolve the item.

---

## How it works

### URL label/value convention

Both product and category URLs follow the same `/{label}/{value}` convention:

```
/product/{title-slug}/pid/{pid}
/category/{name-slug}/cid/{category-id}
```

At render time, each component scans `HttpServletRequest.getPathInfo()` for a segment equal to the configured URL parameter name, then takes the next segment as the value. No HST sitemap parameter configuration is needed — the sitemap only needs `_any_` wildcard children deep enough for the URL to resolve to the correct page.

### Product URLs

All product-linked templates (`brxdis-results.ftl`, `brxdis-recommendations-*.ftl`, `brxdis-product-highlight.ftl`, `brxdis-search-input.ftl`) build links as:

```
resolvedProductPage + "/" + slugify(title) + "/pid/" + pid
```

The `pid` label matches the `productUrlParam` component property (default `pid`).

**Slug derivation** (the `slugify` FTL function):
- Lowercases the title
- Replaces non-alphanumeric runs with `-`
- Strips leading/trailing hyphens
- Falls back to `"product"` if the title is empty

The slug is decorative — the component resolves the product using only the PID. Old bookmarks and `?pid=` query-param fallback URLs continue to work.

### Category URLs

`brxdis-category-highlight.ftl` builds category tile links as:

```
resolvedCategoryPage + "/" + slugify(displayName) + "/cid/" + categoryId
```

The `cid` label matches the `categoryUrlParam` component property (default `cid`).

`DiscoveryCategoryGridComponent` reads the category ID from the URL at render time using the same label/value scan.

### ID resolution precedence

For both product and category components, the resolved ID follows this priority:

1. **Pinned document** — the document stored in the component has a non-blank ID; it is always used regardless of the URL.
2. **URL path segment** — `getPathInfo()` is scanned for `/{label}/{value}` where `label` matches the configured URL param name.
3. **Query parameter** — `?{label}={value}` fallback (same configurable name).

The path scan and query param fallback share the same configurable name. If `productUrlParam = "pid"`, the component reads both `/product/slug/pid/SKU-123` (path) and `?pid=SKU-123` (query) automatically.

---

## Required sitemap configuration

Add `_any_` wildcard children under your `product` and `category` sitemap items to depth. The sitemap does not need to declare named parameters — the component reads the URL directly via `getPathInfo()`.

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:sitemap:
      /category:
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
```

The flat `/product` and `/category` sitemap items are retained — they handle `?pid=` and `?cid=` query-param fallback requests (e.g. from older bookmarks or external integrations).

> **If you change `productUrlParam` or `categoryUrlParam`**, update the fixed label segment in the sitemap accordingly (e.g. if `productUrlParam = "sku"`, replace `/pid:` with `/sku:`).

---

## Backward compatibility

No breaking changes. Components check the path first, then fall back to query params. Existing integrations using `?pid=` or `?cid=` continue to work without changes to HST config or templates.

---

## Slug stability

Product and category slugs are derived from the **title/name returned by Discovery at render time**. If a title changes in Discovery, the slug in newly generated links changes on the next render. The PID or category ID remains the stable identifier — old URLs with the previous slug still resolve because the component uses only the ID for lookup and ignores the slug entirely.

For this reason, canonical `<link>` tags in your templates should point to the current slug form (re-rendered each request) rather than a stored slug.

---

## Configuring the URL parameter names

The `/{label}/{value}` label and the fallback query param name are the same configurable string. Both defaults work correctly with the bundled sitemap YAML above; only change these if your existing site already uses a different URL structure.

| Component | Parameter | Default | Path segment | Query fallback |
|---|---|---|---|---|
| `DiscoveryProductDetailComponent` | `productUrlParam` | `pid` | `/product/{slug}/pid/{id}` | `?pid={id}` |
| `DiscoveryCategoryGridComponent` | `categoryUrlParam` | `cid` | `/category/{slug}/cid/{id}` | `?cid={id}` |
| `DiscoveryProductRecommendationComponent` | `productUrlParam` | `pid` | `/product/{slug}/pid/{id}` | `?pid={id}` |
| `DiscoveryCategoryRecommendationComponent` | `categoryUrlParam` | `cid` | `/category/{slug}/cid/{id}` | `?cid={id}` |

---

## Pixel tracking

URL structure does not affect pixel tracking. The pixel reads identifiers from the `br_data` JavaScript object, not URL parameters. See [09-pixel-tracking.md](09-pixel-tracking.md) for details.
