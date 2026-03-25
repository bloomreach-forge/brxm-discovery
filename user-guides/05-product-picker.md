# Product Picker

## Overview

The Discovery Product Picker is an **Open UI document field extension** embedded as an iframe inside the brXM CMS editor. Editors can search products visually (with thumbnails and prices) and store the selected product ID (PID) in any `String` field on a document.

The picker talks to the CMS REST endpoint (`{cms}/ws/discovery/picker`) — not directly to the Discovery API — so no API credentials are ever exposed to the browser.

---

## What ships automatically

When `brxm-discovery-cms` is on the CMS classpath, HCM bootstraps:

- **`discoveryProductPicker`** — product search picker (dialog)
- **`discoveryWidgetPicker`** — recommendation widget picker (dialog)
- **`discoveryCategoryPicker`** — category picker (dialog)
- **`discoveryCategoryProductPreview`** — inline product count selector + live thumbnail preview (see [below](#discoverycategoryproductpreview-inline-field))

All four are registered under `/hippo:configuration/hippo:frontend/cms/ui-extensions/`.

- **Picker daemon module** at
  `/hippo:configuration/hippo:modules/brxm-discovery`
  which registers the JAX-RS endpoints at `{cms}/ws/discovery/picker/`:
  `search`, `items`, `categories`, `browse`, `widgets`, `category-products`
- **Static HTML/JS** served at `{cms}/discovery-picker/`

You do not need to configure any of this. You only need to add the picker field to your document types.

---

## Step 1: Add the picker field to a document type

In your document type's editor template YAML, add an `OpenUiStringFieldPlugin` field and set `uiExtension` to `discoveryProductPicker`:

```yaml
/my-product-ref:
  jcr:primaryType: frontend:plugin
  caption: 'Featured Product'
  field: 'myns:productId'
  plugin.class: 'org.onehippo.cms7.frontend.plugin.field.OpenUiStringFieldPlugin'
  uiExtension: 'discoveryProductPicker'
  wicket.id: '${cluster.id}.field'
```

And in the node type definition, declare the property:

```yaml
/myns:productId:
  jcr:primaryType: hipposysedit:field
  hipposysedit:mandatory: false
  hipposysedit:multiple: false
  hipposysedit:ordered: false
  hipposysedit:type: 'String'
```

The field will store the **product ID (PID)** returned by Discovery — a plain string.

---

## How the picker works

1. The CMS renders the `frontend:uiExtension` field as an iframe pointing to `{cms}/discovery-picker/index.html`.
2. The iframe loads the `@bloomreach/ui-extension` SDK and calls `UiExtension.register()`.
3. The picker reads the current field value (`ui.document.field.getValue()`) and pre-selects it if present.
4. The picker calls `GET {cms}/ws/discovery/picker/search?q=...` via `fetch` with session cookies.
5. The backend (`DiscoveryPickerResource`) resolves Discovery config server-side, calls the Discovery API, and returns a slim product list.
6. When the editor clicks a product card, the picker calls `ui.document.field.setValue(productId)`.

The stored value is a single PID string (e.g. `"SKU-12345"`).

---

## REST endpoint reference

All endpoints are at `{cms}/ws/discovery/picker/`.

### `GET /search`

| Parameter | Default | Description |
|---|---|---|
| `q` | `*` | Search query. |
| `page` | `0` | Zero-based page. |
| `pageSize` | `12` | Results per page. |
| `documentId` | `""` | Handle UUID — used to derive channel credentials. |
| `channelId` | `""` | Explicit channel ID override. |

### `GET /items`

| Parameter | Default | Description |
|---|---|---|
| `ids` | — | Comma-separated PIDs. Returns empty list if blank. |
| `documentId` | `""` | Handle UUID. |
| `channelId` | `""` | Channel ID override. |

### `GET /categories`

Returns the full category tree for the configured channel (used by the category picker dialog).

### `GET /browse`

| Parameter | Default | Description |
|---|---|---|
| `catId` | — | Category ID to browse. |
| `page` | `0` | Zero-based page. |
| `pageSize` | `9` | Results per page. |
| `documentId` | `""` | Handle UUID. |

### `GET /widgets`

Returns all available recommendation widgets for the channel.

### `GET /category-products`

Used by the `discoveryCategoryProductPreview` inline field to fetch a thumbnail preview.

| Parameter | Default | Description |
|---|---|---|
| `documentId` | `""` | Handle UUID. Used to read `brxdis:categoryId` from the JCR draft variant when `categoryId` is not supplied. |
| `categoryId` | `""` | Direct category ID. When present, JCR is not read — this is the live pre-save value forwarded by the category picker via `postMessage`. |
| `count` | `4` | Number of products to return (capped at 4). |
| `channelId` | `""` | Channel ID override. |

Returns a JSON array of `PickerItemDto` (`id`, `title`, `imageUrl`, `price`).

### Response format (`/search`, `/items`, `/browse`)

```json
{
  "items": [
    {
      "id": "SKU-12345",
      "title": "Classic T-Shirt",
      "imageUrl": "https://cdn.example.com/img/sku-12345.jpg",
      "price": "29.99"
    }
  ],
  "total": 142
}
```

---

## Using the stored PID in delivery

The document field stores the raw PID string. In your HST component, read it from the JCR node:

```java
String productId = document.getHippoBean().getSingleProperty("myns:productId");
```

Then use the PID to:
- Call your commerce SoR (Shopify, commercetools) to fetch full product details.
- Pass it as `contextProductId` to `DiscoveryRecommendationComponent` for related-product widgets.
- Construct a product URL for the storefront.

The plugin deliberately stores only the ID — full product data (price, stock, images) should always be fetched at render time from the SoR or Discovery, never persisted in brXM.

---

## `discoveryCategoryProductPreview` inline field

This extension is used on the built-in `brxdis:categoryDocument` type. It lets editors choose how many product thumbnails (0–4) to show inside each category tile on the site, and previews those thumbnails live inside the document editor without saving.

### How it works

1. The CMS renders the field as a compact inline iframe.
2. On load, the field reads its stored value (the count) and fetches thumbnails via `GET /category-products`.
3. When the **category picker** above changes the selected category, it broadcasts a `brxdis:categoryChanged` message to all sibling iframes via `window.parent.frames`. The product preview field receives it, stores the live `categoryId`, and immediately re-fetches — no JCR save required.
4. On dropdown change, the field updates its stored value via `ui.document.field.setValue()` and re-fetches.

### Site-side behaviour

`DiscoveryCategoryHighlightComponent` reads `getProductPreviewCount()` from each `DiscoveryCategoryBean` at render time. For each category with `count > 0`, it checks a JVM-level cache (`CategoryPreviewCache`, ~5-minute TTL with ±20% jitter) before calling Discovery. On a cache miss it calls `HstDiscoveryService.browse()`, stores the result, and sets the `previewProducts` model — a `Map<String, List<ProductSummary>>` keyed by `categoryId`. The bundled `brxdis-category-highlight.ftl` renders product thumbnails inside each tile when `previewProducts` contains entries.

### Using on a custom document type

If you want the same inline preview on your own document type (which stores a category ID), register a new document field pointing to the extension:

```yaml
/myProductPreviewCount:
  jcr:primaryType: frontend:plugin
  caption: 'Product Preview'
  field: myns:productPreviewCount
  plugin.class: 'org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin'
  wicket.id: '${cluster.id}.field'
  /cluster.options:
    jcr:primaryType: frontend:pluginconfig
    ui.extension: discoveryCategoryProductPreview
```

The extension reads the `categoryId` via `postMessage` from the category picker in the same document. For this to work, your document must also use `discoveryCategoryPicker` for the category ID field — it is the broadcaster.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Picker iframe shows blank or "Failed to load" | `brxm-discovery-cms` not on classpath, or daemon module not started |
| Search returns 0 results | Discovery credentials blank or incorrect; check logs for HTTP errors from `DiscoveryPickerResource` |
| Field saves but value disappears on reload | Property not declared in the CND / node type definition |
| Product preview shows thumbnails from old category after picking a new one | Category picker and preview field are not in the same document / same CMS page — `postMessage` only reaches same-origin sibling iframes |
