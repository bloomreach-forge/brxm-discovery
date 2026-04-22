# Product Picker

## Overview

The Discovery Product Picker is an **Open UI document field extension** embedded as an iframe inside the brXM CMS editor. Editors can search products visually (with thumbnails and prices) and store the selected product ID (PID) in any `String` field on a document.

The picker talks to the CMS REST endpoint (`{cms}/ws/discovery/picker`) - not directly to the Discovery API - so no API credentials are ever exposed to the browser.

---

## What ships automatically

When `brxm-discovery-cms` is on the CMS classpath, HCM bootstraps the following Open UI extensions under `/hippo:configuration/hippo:frontend/cms/ui-extensions/`:

**Product / category pickers (dialogs - used directly on custom doc types)**
- **`discoveryProductPicker`** - full product search picker with category sidebar
- **`discoveryCategoryPicker`** - category picker (dialog)

**Product & category document wizards (built-in doc types)**
- **`discoveryProductWizard`** - 2-step wizard for `brxdis:productDetailDocument`; lets editors choose Dynamic (URL param) or Pinned (specific product) mode
- **`discoveryCategoryWizard`** - 2-step wizard for `brxdis:categoryDocument`; lets editors choose Dynamic or Pinned category mode

**Recommendation wizard fields**
- **`discoveryProductRecommendationWizard`** - wizard for `brxdis:productRecommendationDocument`; filters to product widget types (`co_viewed`, `co_bought`, `rt_recs`, `mlt`)
- **`discoveryCategoryRecommendationWizard`** - wizard for `brxdis:categoryRecommendationDocument`; filters to `category` widgets
- **`discoveryGlobalRecommendationWizard`** - wizard for `brxdis:globalRecommendationDocument`; filters to global/personalized types (`bestseller`, `trending_product`, `jfy`, `past_purchases`, `recently_viewed`)

**Type-filtered widget pickers (inline field, no wizard)**
- **`discoveryProductWidgetPicker`** - single-field widget picker scoped to product types
- **`discoveryCategoryWidgetPicker`** - single-field widget picker scoped to `category`
- **`discoveryGlobalWidgetPicker`** - single-field widget picker scoped to global types

**Live preview fields**
- **`discoveryRecommendationPreview`** - shows sample product thumbnails for the selected recommendation config (listens for `brxdis:configChanged` from the wizard field above it)
- **`discoveryProductDetailPreview`** - shows the thumbnail of the product configured in a `brxdis:productDetailDocument` (works alongside the product wizard)
- **`discoveryCategoryProductPreview`** - inline product count selector + live thumbnail preview for `brxdis:categoryDocument` (see [below](#discoverycategoryproductpreview-inline-field))

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

The field will store the **product ID (PID)** returned by Discovery - a plain string.

---

## How the picker works

1. The CMS renders the `frontend:uiExtension` field as an iframe pointing to `{cms}/discovery-picker/index.html`.
2. The iframe loads the `@bloomreach/ui-extension` SDK and calls `UiExtension.register()`.
3. The picker reads the current field value and pre-selects it in the footer bar if present.
4. Editors can browse by category (left sidebar) or search by keyword (top bar). The sidebar filter input narrows the category list by name or category ID. Each category item shows its display name and ID in monospace below it.
5. Clicking a product card **highlights** it (`.selected` state) and shows the product ID and title in the footer bar. The picker no longer closes immediately on card click.
6. The footer has **Cancel** and **Select →** buttons. Cancel closes the dialog with no change; Select → calls `ui.dialog.close(productId)` and the parent field stores the value.
7. The backend (`DiscoveryPickerResource`) resolves Discovery config server-side, calls the Discovery API, and returns a slim product list.

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
| `documentId` | `""` | Handle UUID - used to derive channel credentials. |
| `channelId` | `""` | Explicit channel ID override. |

### `GET /items`

| Parameter | Default | Description |
|---|---|---|
| `ids` | - | Comma-separated PIDs. Returns empty list if blank. |
| `documentId` | `""` | Handle UUID. |
| `channelId` | `""` | Channel ID override. |

### `GET /categories`

Returns the full category tree for the configured channel (used by the category picker dialog).

### `GET /browse`

| Parameter | Default | Description |
|---|---|---|
| `catId` | - | Category ID to browse. |
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
| `categoryId` | `""` | Direct category ID. When present, JCR is not read - this is the live pre-save value forwarded by the category picker via `postMessage`. |
| `count` | `4` | Number of products to return (capped at 4). |
| `channelId` | `""` | Channel ID override. |

Returns a JSON array of `PickerItemDto` (`id`, `title`, `imageUrl`, `price`).

### `GET /recommendation-products`

Used by `discoveryRecommendationPreview` to show sample results for a recommendation widget config.

| Parameter | Default | Description |
|---|---|---|
| `documentId` | `""` | Handle UUID. Used to read `brxdis:config` JSON from the JCR draft when `configJson` is not supplied. |
| `configJson` | `""` | Direct config JSON (live pre-save value from the wizard). When present, JCR is not read. |
| `count` | `4` | Number of products to return (capped at 12). |
| `channelId` | `""` | Channel ID override. |

Returns empty when: the config JSON is missing or invalid; `widgetId` is blank; widget type requires a product context but `contextProductId` is null; widget type requires a category context but `contextCategoryId` is null.

### `GET /product-detail`

Used by `discoveryProductDetailPreview` to show a thumbnail of the selected product in a `brxdis:productDetailDocument`.

| Parameter | Default | Description |
|---|---|---|
| `documentId` | `""` | Handle UUID. Used to read `brxdis:productId` from the JCR draft when `productId` is not supplied. |
| `productId` | `""` | Direct product ID (live pre-save value from the product picker). When present, JCR is not read. |
| `channelId` | `""` | Channel ID override. |

Returns a single-element JSON array of `PickerItemDto`, or an empty array when no product ID can be resolved.

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

The plugin deliberately stores only the ID - full product data (price, stock, images) should always be fetched at render time from the SoR or Discovery, never persisted in brXM.

---

## `discoveryCategoryProductPreview` inline field

This extension is used on the built-in `brxdis:categoryDocument` type. It lets editors choose how many product thumbnails (0–4) to show inside each category tile on the site, and previews those thumbnails live inside the document editor without saving.

### How it works

1. The CMS renders the field as a compact inline iframe.
2. On load, the field reads its stored value (the count) and fetches thumbnails via `GET /category-products`.
3. When the **category picker** above changes the selected category, it broadcasts a `brxdis:categoryChanged` message to all sibling iframes via `window.parent.frames`. The product preview field receives it, stores the live `categoryId`, and immediately re-fetches - no JCR save required.
4. On dropdown change, the field updates its stored value via `ui.document.field.setValue()` and re-fetches.

### Site-side behaviour

`DiscoveryCategoryHighlightComponent` reads `getProductPreviewCount()` from each `DiscoveryCategoryBean` at render time. For each category with `count > 0`, it checks a JVM-level cache (`CategoryPreviewCache`, ~5-minute TTL with ±20% jitter) before calling Discovery. On a cache miss it calls `HstDiscoveryService.browse()`, stores the result, and sets the `previewProducts` model - a `Map<String, List<ProductSummary>>` keyed by `categoryId`. The bundled `brxdis-category-highlight.ftl` renders product thumbnails inside each tile when `previewProducts` contains entries.

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

The extension reads the `categoryId` via `postMessage` from the category picker in the same document. For this to work, your document must also use `discoveryCategoryPicker` for the category ID field - it is the broadcaster.

---

## Product & category document wizards

### Product wizard (`discoveryProductWizard`)

Used on `brxdis:productDetailDocument`. Makes the product ID mode explicit so editors always know what the component will use at runtime.

**Step 1 - Mode selection + optional picker**

Two radio options are shown:
- **Dynamic** - the component reads `?pid=` from the URL at render time. No product is stored.
- **Pinned** - the editor picks a specific product. An inline product search (search bar + category sidebar + product grid) appears immediately below the radio for finding and selecting a product.

**Step 2 - Review**

Shows a summary of the chosen mode and a live product card for Pinned selections (fetched via `GET /product-detail`). In Dynamic mode a notice is shown explaining the `?pid=` runtime behaviour.

**Stored value**: `brxdis:productId` - empty string for Dynamic, plain PID for Pinned.

---

### Category wizard (`discoveryCategoryWizard`)

Used on `brxdis:categoryDocument`. Same 2-step structure as the product wizard.

**Step 1 - Mode selection + optional picker**

- **Dynamic** - the component reads the category ID from the URL at render time: first from a path segment (`/category/{slug}/cid/{id}`), then from `?cid=`.
- **Pinned** - an inline filterable category list lets the editor select a specific category.

**Step 2 - Review**

Shows the mode summary and a 4-product thumbnail grid for Pinned selections (fetched via `GET /browse`). Dynamic mode shows a URL parameter notice (the default parameter name is `cid`).

**Stored value**: `brxdis:categoryId` - empty string for Dynamic, plain category ID for Pinned.

---

### Runtime enforcement

Both wizards enforce the document-required contract at the HST component level:

- **No document configured** on the component → component renders nothing; Channel Manager shows the document picker field for configuration.
- **Document in Dynamic mode** + URL param present → product/category fetched from URL param.
- **Document in Dynamic mode** + no URL param → component renders nothing; Channel Manager shows a warning ("dynamic mode, no URL param found").
- **Document in Pinned mode** → URL param is ignored; the pinned ID is always used.

---

## `discoveryProductDetailPreview` inline field

The built-in `brxdis:productDetailDocument` type includes a `brxdis:_preview` field backed by `discoveryProductDetailPreview`. It renders a live thumbnail of the selected product inside the document editor alongside the product wizard field.

### How it works

1. On load, the field reads the stored `brxdis:productId` and fetches the thumbnail via `GET /product-detail?documentId=...`.
2. When the **product wizard** dialog saves a new product selection, it broadcasts a `brxdis:productChanged` message to sibling iframes via `window.parent.frames`. The preview field receives it and re-fetches immediately with the live `productId` - no JCR save required.
3. When Dynamic mode is selected in the wizard, the stored `productId` is empty and the preview field shows a placeholder ("Dynamic - determined by URL parameter").

---

## Recommendation wizard

The recommendation wizard is a 3-step dialog for configuring recommendation documents. It is used by `discoveryProductRecommendationWizard`, `discoveryCategoryRecommendationWizard`, and `discoveryGlobalRecommendationWizard`.

### Steps

**Step 1 - Widget**: Lists all recommendation widgets available for the channel. The widget list is pre-filtered to the widget types declared in `frontend:config.widgetTypes` of the extension registration. Clicking a row advances to step 2.

**Step 2 - Context** (product and category types only):
- **Product types** (`co_viewed`, `co_bought`, `rt_recs`, `mlt`): choose between "Use URL param (`?pid=`)" or "Pick a specific product" (inline product search, same backend as `/search`).
- **Category type**: choose between "Use URL param" or "Pick a specific category" (inline category list, same backend as `/categories`). In Dynamic mode the category ID is read from the URL path (`/category/{slug}/cid/{id}`) or `?cid=` query param (configurable via `categoryUrlParam`, default `cid`).
- Global/personalized types skip this step entirely.

**Step 3 - Review**: Shows the resolved config summary and a live thumbnail strip (via `GET /recommendation-products`). Click **Save** to write the config.

When the editor chose "Use URL param" context (product or category types), Step 3 shows a **Test with** input bar instead of a blank preview. The editor can type a product ID or category ID to fire a preview fetch; the test value is never saved - it only drives the thumbnail strip during review.

### Stored value

The wizard stores a JSON string in `brxdis:config`:

```json
{
  "widgetId": "similar-items",
  "widgetName": "Similar Items",
  "widgetType": "item",
  "contextProductId": "SKU-123",
  "contextProductName": "Classic T-Shirt"
}
```

For category type, `contextProductId`/`contextProductName` are replaced by `contextCategoryId`/`contextCategoryName`. For global types, none of the context fields are present. A `null` context field means "fall back to the URL at runtime": for product contexts, reads `/{label}/{pid}` path or `?{label}=` query (default label: `pid`); for category contexts, reads `/{label}/{cid}` path or `?{label}=` query (default label: `cid`).

### postMessage cross-field sync

After the wizard saves, `recommendation-wizard.html` broadcasts:
```json
{ "type": "brxdis:configChanged", "documentId": "...", "configJson": "..." }
```
to all same-origin sibling iframes via `window.parent.frames`. `recommendation-preview.html` listens and immediately re-fetches the preview - no JCR save required.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Picker iframe shows blank or "Failed to load" | `brxm-discovery-cms` not on classpath, or daemon module not started |
| Search returns 0 results | Discovery credentials blank or incorrect; check logs for HTTP errors from `DiscoveryPickerResource` |
| Field saves but value disappears on reload | Property not declared in the CND / node type definition |
| Product preview shows thumbnails from old category after picking a new one | Category picker and preview field are not in the same document / same CMS page - `postMessage` only reaches same-origin sibling iframes |
