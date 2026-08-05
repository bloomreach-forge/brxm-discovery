[Documentation home](README.md) > Recommendations & Visual Search

# Recommendations & Visual Search

Recommendation widgets and visual (image) search are both powered by Discovery's **v2 Pathways API**, and both are configured largely by content editors rather than developers. This page covers the widget document types, the wizard editors used to configure them, and how to enable image-based search.

**ON THIS PAGE**
- [v1 vs. v2 — automatic version selection](#v1-vs-v2--automatic-version-selection)
- [Recommendation document types](#recommendation-document-types)
- [Placing a recommendation component](#placing-a-recommendation-component)
- [Visual search](#visual-search)

---

## v1 vs. v2 — automatic version selection

The plugin supports two Discovery recommendation APIs, and selects between them automatically:

- **v1** (`discoverySearchAPI`) — used when no `authKey` is configured.
- **v2 Pathways** (`discoveryPathwaysAPI`) — used automatically as soon as `authKey` is configured.

There is no configuration flag to set — the presence of `authKey` (via `BRXDIS_AUTH_KEY`, `-Dbrxdis.authKey`, the global config node, or a per-channel `discoveryAuthKeyEnvVar` override) is the switch. See [Configuration](03-configuration.md#injecting-credentials).

Visual search **requires** v2 — it has no v1 equivalent.

---

## Recommendation document types

Each recommendation widget is configured as a JCR document, authored through a purpose-built wizard rather than raw parameter fields:

| Document type | Wizard | Widget types it targets |
|---|---|---|
| Discovery Product Recommendation | 3-step product recommendation wizard | `co_viewed`, `co_bought`, `rt_recs`, `mlt` |
| Discovery Category Recommendation | 3-step category recommendation wizard | `category` |
| Discovery Global/Personalized Recommendation | 3-step global recommendation wizard | `bestseller`, `trending_product`, `jfy`, `past_purchases`, `recently_viewed` |
| Discovery Keyword Recommendation | 3-step keyword recommendation wizard | keyword/query-driven widgets |

### The 3-step wizard

> **[SCREENSHOT PLACEHOLDER: step 1 of the recommendation wizard — the list of available widgets for the channel, filtered to the relevant widget types.]**

1. **Widget** — choose from the recommendation widgets configured for your Discovery account. The list is automatically filtered to the widget types relevant to the document type you're editing.
2. **Context** *(product and category widgets only)* — choose whether the widget is always tied to a specific product/category ("Pinned"), or reads the current page's product/category ID from the URL at render time ("Dynamic"). Global and personalized widgets skip this step.

   > **[SCREENSHOT PLACEHOLDER: step 2 of the recommendation wizard — the Dynamic vs. Pinned choice, with an inline product/category search visible for the Pinned option.]**

3. **Review** — a summary of the chosen configuration with a live thumbnail preview of what the widget will show.

   > **[SCREENSHOT PLACEHOLDER: step 3 of the recommendation wizard — the review screen with a thumbnail strip of sample products.]**

Editors can test a Dynamic-mode widget during review by entering a sample product or category ID — this test value only drives the preview and is never saved.

---

## Placing a recommendation component

See [Component Parameters](04-component-parameters.md#recommendation-components) for the shared parameter table. Each component's `document` field points to the widget configuration authored through the wizard above — the component itself needs no other setup.

**Example: a "Similar Items" carousel on a product detail page.** Place `DiscoveryProductDetailComponent` and `DiscoveryProductRecommendationComponent` on the same page, both left in Dynamic mode. Navigating to a product with `?pid=SKU-123` in the URL feeds both components automatically — no extra wiring needed.

---

## Visual search

Visual search lets a shopper upload a photo instead of typing keywords, and see products that visually match it.

> **[SCREENSHOT PLACEHOLDER: the storefront search bar with the camera/image-search icon visible, and a second screenshot of the resulting visually-matched product grid.]**

### Enabling it

Visual search is a per-channel setting, configured in Channel Manager under **Channel Settings → Visual Search**:

| Field | Type | Default | Description |
|---|---|---|---|
| `discoveryVisualSearchEnabled` | boolean | `false` | Shows the camera button in the search bar and activates the upload endpoint. |
| `discoveryVisualSearchWidgetId` | String | `""` | The visual search widget ID from your Discovery dashboard. Required when enabled. |

> **[SCREENSHOT PLACEHOLDER: the Visual Search section of the Channel Settings panel.]**

If enabled without a widget ID, the plugin logs a warning and falls back to ordinary keyword search — visitors never see a broken page.

No component parameters are needed — the same `DiscoverySearchInputComponent` and `DiscoverySearchGridComponent` used for keyword search also handle visual search automatically.

### How it works

1. A shopper selects or takes a photo.
2. The image is uploaded to a server-side endpoint that the plugin exposes — Discovery credentials never reach the browser.
3. The plugin forwards the image to Discovery and receives back an image reference ID.
4. The shopper is redirected to the results page, which detects the image reference and calls Discovery's visual search API directly.
5. Matching products are returned in the same product grid used for keyword search (with facets and pagination unavailable for image-based results, since Discovery does not return them for this query type).

### Visual search mount placement

The plugin's visual search endpoints must be reachable through an HST mount nested **under the channel mount** that carries the Discovery channel settings — this is what lets the endpoint resolve the correct per-channel credentials.

```yaml
/commerce:
  jcr:primaryType: hst:mount
  hst:mountpoint: /hst:site/hst:sites/commerce
  hst:namedpipeline: resourceapi
  /_brxdis-api:
    jcr:primaryType: hst:mount
    hst:ismapped: false
    hst:types: [rest]
    hst:namedpipeline: BrxdisVisualSearchPipeline
```

Do not mount this at the host root or as a sibling of your channels — without a channel in its parent chain, the endpoint cannot resolve per-channel credentials and silently falls back to the global configuration.

### Troubleshooting

| Symptom | Likely cause |
|---|---|
| Camera button not visible | Visual search disabled, or no widget ID set, for this channel |
| Upload fails | Widget ID in Channel Settings doesn't match a widget in the Discovery dashboard |
| Results page falls back to keyword search | The image reference from the upload wasn't carried through to the results page redirect |
| `authKey` missing warning in logs | Visual search requires v2 Pathways credentials — see [Configuration](03-configuration.md#injecting-credentials) |

---

**Previous:** [Component Parameters](04-component-parameters.md) · **Next:** [CMS Document Types & Pickers](06-document-types-and-pickers.md)
