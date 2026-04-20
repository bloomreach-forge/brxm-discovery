# SEO-Friendly URLs

## Overview

The plugin generates clean, crawlable path-based URLs for product detail and category browse pages. Search results remain query-parameter-based (`?q=`), which is the standard for search engines.

| Page | URL pattern | Example |
|---|---|---|
| Product detail | `/product/{title-slug}/p/{pid}` | `/product/classic-cotton-t-shirt/p/SKU-123` |
| Category browse | `/category/{category-id}` | `/category/womens-shoes` |
| Search results | `/search?q={term}` | `/search?q=shirt` |

---

## How it works

### Product URLs

`brxdis-results.ftl` builds product links client-side using a `slugify` function applied to the product title, followed by the stable PID:

```
/product/{slugify(product.title)}/p/{pid}
```

The slug is for readability only — the component resolves the product using only the PID. Old bookmarks and `?pid=` query-param URLs continue to work.

**Slug derivation** (inside `brxdis-results.ftl`):
- Lowercases the title
- Replaces non-alphanumeric runs with `-`
- Strips leading/trailing hyphens
- Falls back to `"product"` if the title is empty

### Category URLs

`DiscoveryCategoryGridComponent` reads the first path segment captured by the HST sitemap wildcard:

```
/category/{category-id}  →  sitemap param "1"  →  component uses as category ID
```

### ID resolution precedence (both components)

1. Configured document (static ID authored in CMS)
2. URL path segment (`/product/…/p/{pid}` or `/category/{id}`)
3. Query parameter (`?pid=` or `?category=`)

---

## Required sitemap configuration

Add `_any_` wildcard children under your `product` and `category` sitemap items so the path-based URLs resolve to the correct pages.

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:sitemap:
      /category:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/category-page
        /_any_:
          jcr:primaryType: hst:sitemapitem
          hst:componentconfigurationid: hst:pages/category-page
      /product:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/product-detail-page
        /_any_:
          jcr:primaryType: hst:sitemapitem
          /p:
            jcr:primaryType: hst:sitemapitem
            /_any_:
              jcr:primaryType: hst:sitemapitem
              hst:componentconfigurationid: hst:pages/product-detail-page
```

The flat `/product` and `/category` sitemap items are preserved — they handle `?pid=` and `?category=` fallback requests.

---

## Backward compatibility

No breaking changes. Components check path segments first, then fall back to query params. Existing integrations using `?pid=` or `?category=` continue to work without changes to HST config or templates.

---

## Slug stability

The product slug is derived from the **title returned by Discovery at render time**. If a product title changes in Discovery, the slug in newly generated links changes on the next render. The PID remains the stable identifier — old URLs with the previous slug still resolve because the component uses only the PID for lookup and ignores the slug entirely.

For this reason, canonical `<link>` tags in your templates should point to the current slug form (re-rendered each request) rather than a stored slug.

---

## Configuring the URL parameter names

By default the components read `?pid=` and `?category=` as fallback query params. Both names are configurable via Channel Manager component parameters:

| Component | Parameter | Default |
|---|---|---|
| `DiscoveryProductDetailComponent` | `productUrlParam` | `pid` |
| `DiscoveryCategoryGridComponent` | `categoryUrlParam` | `category` |
| `DiscoveryProductRecommendationComponent` | `productUrlParam` | `pid` |
| `DiscoveryCategoryRecommendationComponent` | `categoryUrlParam` | `category` |

Only change these if your existing site uses a different parameter name (e.g. `?sku=`).

---

## Pixel tracking

URL structure does not affect pixel tracking. The pixel reads identifiers from the `br_data` JavaScript object, not URL parameters. See [09-pixel-tracking.md](09-pixel-tracking.md) for details.
