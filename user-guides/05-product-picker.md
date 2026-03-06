# Product Picker

## Overview

The Discovery Product Picker is an **Open UI document field extension** embedded as an iframe inside the brXM CMS editor. Editors can search products visually (with thumbnails and prices) and store the selected product ID (PID) in any `String` field on a document.

The picker talks to the CMS REST endpoint (`{cms}/ws/discovery/picker`) — not directly to the Discovery API — so no API credentials are ever exposed to the browser.

---

## What ships automatically

When `brxm-discovery-cms` is on the CMS classpath, HCM bootstraps:

- **`discoveryProductPicker`** Open UI extension node at
  `/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker`
- **Picker daemon module** at
  `/hippo:configuration/hippo:modules/brxm-discovery-picker`
  which registers the JAX-RS endpoint `{cms}/ws/discovery/picker/search` and `.../items`
- **Static HTML/JS app** served at
  `{cms}/discovery-picker/index.html`

You do not need to configure any of this. You only need to:
1. Point the extension at your `brxdis:discoveryConfig` document (one-time, per installation).
2. Add the picker field to your document types.

---

## Step 1: Set the configPath on the extension node

The extension node has a `frontend:config` property that tells the picker JS where to find the Discovery config. Update it to the JCR path of your `brxdis:discoveryConfig` document.

In your project's HCM config (runs once, can be in your application or development module):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker:
      frontend:config: '{"configPath":"/content/documents/administration/discovery-config/discovery-config"}'
```

Replace the path with wherever you created your `brxdis:discoveryConfig` document.

---

## Step 2: Add the picker field to a document type

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
3. The SDK passes `frontend:config` (JSON with `configPath`) to the picker JS.
4. The picker reads the current field value (`ui.document.field.getValue()`) and pre-selects it if present.
5. The picker calls `GET {cms}/ws/discovery/picker/search?configPath=...&q=...` via `fetch` with session cookies.
6. The backend (`DiscoveryPickerResource`) reads the `brxdis:discoveryConfig` node, calls the Discovery API, and returns a slim product list.
7. When the editor clicks a product card, the picker calls `ui.document.field.setValue(productId)`.

The stored value is a single PID string (e.g. `"SKU-12345"`).

---

## REST endpoint reference

Both endpoints are available at `{cms}/ws/discovery/picker/`:

### `GET /search`

| Parameter | Required | Description |
|---|---|---|
| `configPath` | Yes | Absolute JCR path to `brxdis:discoveryConfig` |
| `q` | No | Search query. Defaults to `*`. |
| `page` | No | Zero-based page. Defaults to `0`. |
| `pageSize` | No | Results per page. Defaults to `12`. |

### `GET /items`

| Parameter | Required | Description |
|---|---|---|
| `configPath` | Yes | Absolute JCR path to `brxdis:discoveryConfig` |
| `ids` | No | Comma-separated list of PIDs to fetch by ID. Returns empty list if blank. |

### Response format (both endpoints)

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

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Picker iframe shows "No 'configPath' set" | `frontend:config` on the `discoveryProductPicker` node is missing or malformed |
| Picker shows "Failed to connect to CMS" | `brxm-discovery-cms` not on classpath, or daemon module not started |
| Search returns 0 results | Discovery credentials blank or incorrect; check logs for HTTP errors from `DiscoveryPickerResource` |
| Field saves but value disappears on reload | Property not declared in the CND / node type definition |
