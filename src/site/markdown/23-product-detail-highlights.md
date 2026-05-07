# Product Detail and Highlight Components

This guide covers the three components used for editorial curation and product detail pages:

| Component | Template | Purpose |
|---|---|---|
| `DiscoveryProductDetailComponent` | `brxdis-product-detail` | Product detail page — fetches one product by PID |
| `DiscoveryProductHighlightComponent` | `brxdis-product-highlight` | Up to 4 curated product slots (editorial, not algorithmic) |
| `DiscoveryCategoryHighlightComponent` | `brxdis-category-highlight` | Up to 4 curated category navigation tiles with optional product previews |

All three use document pickers configured via the Channel Manager. See [30-product-picker.md](30-product-picker.md) for the CMS picker REST endpoints and [40-spa-integration.md](40-spa-integration.md) for TypeScript types.

---

## `DiscoveryProductDetailComponent`

Fetches a single product from the Discovery Search API by product ID (PID) and exposes it via the Page Model API.

### Component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document` | JCR path | — | Path to a `brxdis:productDetailDocument` node. **Required.** Configure via the product picker wizard. |
| `productUrlParam` | `String` | `pid` | URL parameter name for Dynamic mode. Change to `sku` if your URLs use `?sku=` instead of `?pid=`. |

### Document modes

A `brxdis:productDetailDocument` is created via the product picker wizard in the Channel Manager:

- **Pinned** — stores a specific PID in the document; the same product is always shown regardless of URL.
- **Dynamic** — the document has no pinned ID; the component reads `?pid=<id>` (or the configured `productUrlParam`) from the URL at render time.

### Models set on request

| Key | Type | Description |
|---|---|---|
| `product` | `ProductSummary\|null` | The fetched product. `null` if no document is configured, Dynamic mode has no `?pid=` in the URL, or the product is not in Discovery. |
| `pid` | `String` | The resolved PID. Blank when `product` is `null`. |
| `document` | `HippoBean\|null` | The linked `brxdis:productDetailDocument`. |
| `editMode` | `boolean` | `true` when rendered inside Channel Manager preview. |

### React / SPA usage

```tsx
// ProductDetail.tsx
export function ProductDetail({ component }: BrComponentContext) {
  const { product, pid, editMode } = component.getModels<ProductDetailModels>();

  if (!product) {
    if (editMode) return <p>Configure a Product Detail Document in component properties.</p>;
    return null;
  }

  const brand = product.attributes['brand'] as string | undefined;
  const salePrice = product.attributes['sale_price'] as number | undefined;

  return (
    <article>
      {product.imageUrl && <img src={product.imageUrl} alt={product.title} />}
      <h1>{product.title}</h1>
      {brand && <p className="brand">{brand}</p>}
      <p className="price">
        {salePrice != null
          ? <><s>{product.currency} {product.price?.toFixed(2)}</s> {product.currency} {salePrice.toFixed(2)}</>
          : <>{product.currency} {product.price?.toFixed(2)}</>
        }
      </p>
    </article>
  );
}
```

### FTL template

`brxdis-product-detail.ftl` renders the product using Freemarker. Access the product via `${product.title!""}`, `${product.imageUrl!""}`, `${product.price?c!""}`.

---

## `DiscoveryProductHighlightComponent`

Renders up to 4 hand-picked products in an editorial slot grid. Products are fetched individually by PID from the Discovery Search API at render time.

### Component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path | `""` | Paths to `brxdis:productDetailDocument` nodes. Empty slots are skipped. |

### Models set on request

| Key | Type | Description |
|---|---|---|
| `products` | `List<ProductSummary\|null>` | Always 4 entries. Unfilled slots are `null`. |
| `editMode` | `boolean` | `true` when rendered inside Channel Manager preview. |

### Notes

- Products are fetched individually at render time by PID — not via a search query.
- Unfilled slots (`null`) should be hidden in delivery and shown as empty placeholders in the Channel Manager.

### React / SPA usage

```tsx
// ProductHighlight.tsx
export function ProductHighlight({ component }: BrComponentContext) {
  const { products, editMode } = component.getModels<ProductHighlightModels>();
  const filled = products.filter((p): p is NonNullable<typeof p> => p !== null);

  if (!editMode && filled.length === 0) return null;

  return editMode && filled.length === 0
    ? <p className="cms-placeholder">Add products using the component panel.</p>
    : (
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

---

## `DiscoveryCategoryHighlightComponent`

Renders up to 4 curated category navigation tiles. Each tile can optionally show product thumbnail previews sourced from the Discovery Search API.

### Component parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document1`–`document4` | JCR path | `""` | Paths to `brxdis:categoryDocument` nodes. Empty slots are skipped. |

### Models set on request

| Key | Type | Description |
|---|---|---|
| `categories` | `List<DiscoveryCategoryBean>` | Resolved category beans in slot order. |
| `previewProducts` | `Map<String, List<ProductSummary>>` | Preview products keyed by `categoryId`. Served from a JVM-level cache (~5 min TTL). Empty map when all `productPreviewCount` values are `0`. |
| `editMode` | `boolean` | `true` when rendered inside Channel Manager preview. |

### `DiscoveryCategoryBean` accessors

| Method | Returns | Description |
|---|---|---|
| `categoryId()` | `String` | Discovery category ID stored in `brxdis:categoryId` |
| `displayName()` | `String` | Editorial label stored in `brxdis:displayName`. Fall back to `categoryId()` if blank. |
| `productPreviewCount()` | `int` | Number of product thumbnail previews to show (0–4) |

### Notes

- Preview products are cached in the JVM with a ~5-minute TTL — they are not fetched live on every request.
- `previewProducts` is empty when all `productPreviewCount` values are `0`.
- The category page URL follows `/{slug}/cid/{id}`. The `cid` label matches the `categoryUrlParam` component property on `DiscoveryCategoryGridComponent` (default `cid`). Use `?cid={id}` as the query-param fallback.

### FTL template access

```ftl
<#assign previewProds = (previewProducts!{})[cat.categoryId()!""]![]>
<#list previewProds as p>
  <img src="${p.imageUrl()!""}" alt="${(p.title()!"")?html}">
</#list>
```

### React / SPA usage

```tsx
// CategoryHighlight.tsx
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
