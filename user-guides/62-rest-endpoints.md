# REST Endpoints

The plugin exposes two groups of REST endpoints: the delivery-tier visual search proxy and the CMS picker endpoints.

---

## Visual search proxy (delivery tier)

Registered via `BrxdisVisualSearchPipeline`. Mounted at `/_brxdis-api/visual-search` — must be a child of the channel mount so per-channel credentials resolve correctly. See [24-visual-search.md](24-visual-search.md) for mount configuration.

Base path: `{channelMount}/_brxdis-api/visual-search`

| Method | Path | Description |
|---|---|---|
| `POST` | `/{widgetId}/upload` | Upload an image; returns `imageId`, detected objects, and initial results. |
| `GET` | `/{widgetId}/search` | Fetch products for a previously uploaded image. |

### `POST /{widgetId}/upload`

**Request:** `multipart/form-data` with a field named `image`.

**Query params:** none.

**Response:**

```json
{
  "imageId": "abc123",
  "objects": [
    { "id": 0, "bbox": [0.1, 0.2, 0.5, 0.8], "objectType": "shoe" }
  ],
  "results": []
}
```

| Field | Description |
|---|---|
| `imageId` | Opaque ID; pass as `?imageId=` in the results-page redirect. |
| `objects` | Detected objects. Pass an object's `id` as `?objectId=` to narrow results. |
| `results` | Always empty — products come from the grid component's server render. |

### `GET /{widgetId}/search`

Called server-side by `HstDiscoveryService.visualSearch()` during grid render. Not called directly by the browser.

| Query param | Required | Default | Description |
|---|---|---|---|
| `imageId` | yes | — | Image ID from the upload response. |
| `objectId` | no | — | Narrows results to a specific detected object. |
| `rows` | no | component `pageSize` | Number of results. |

---

## CMS picker endpoints

Registered by `DiscoveryPickerModule` (a `DaemonModule`). Exposed under the CMS REST API at:

`{cmsContext}/ws/discovery/picker/`

All endpoints are `GET`, produce `application/json`, and accept two common query params:

| Param | Required | Description |
|---|---|---|
| `channelId` | no | Channel ID to select per-channel credentials. Uses global credentials when blank. |
| `documentId` | no | UUID of the open document. Used to resolve per-document credentials and to read `brxdis:*` JCR fields. |

| Method | Path | Description |
|---|---|---|
| `GET` | `/search` | Search products by keyword. Used by the product picker for free-text search. |
| `GET` | `/items` | Fetch specific products by PID list. Used to reload already-selected items. |
| `GET` | `/browse` | Browse products within a category. Used by category-aware pickers. |
| `GET` | `/categories` | List all browsable categories. Used to populate the category picker tree. |
| `GET` | `/category-products` | Thumbnail preview of products in a category document. |
| `GET` | `/widgets` | List available recommendation widgets. Used by the widget picker. |
| `GET` | `/recommendation-products` | Thumbnail preview of products a recommendation widget will return. |
| `GET` | `/product-detail` | Thumbnail preview for a product-detail document. |

### `/search`

| Param | Default | Description |
|---|---|---|
| `q` | `*` | Search query. `*` returns all products. |
| `page` | `0` | 0-indexed page. |
| `pageSize` | `12` | Items per page (capped at 100). |
| `catId` | `""` | Narrows to a specific category ID. |

### `/items`

| Param | Required | Description |
|---|---|---|
| `ids` | yes | Comma-separated product IDs (PIDs). |

### `/browse`

| Param | Default | Description |
|---|---|---|
| `catId` | `""` | Category ID to browse. |
| `page` | `0` | 0-indexed page. |
| `pageSize` | `9` | Items per page (capped at 100). |

### `/category-products`

| Param | Default | Description |
|---|---|---|
| `categoryId` | `""` | Category ID. When blank, read from `brxdis:categoryId` on the document. |
| `count` | `4` | Number of product thumbnails to return. |

### `/widgets`

No extra params beyond `channelId` / `documentId`.

### `/recommendation-products`

| Param | Default | Description |
|---|---|---|
| `configJson` | `""` | Recommendation config JSON (live pre-save). When blank, read from `brxdis:config` on the document. |
| `count` | `4` | Number of product thumbnails. |

### `/product-detail`

| Param | Default | Description |
|---|---|---|
| `productId` | `""` | Product ID. When blank, read from `brxdis:productId` on the document. |
