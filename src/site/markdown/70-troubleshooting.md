# Troubleshooting

---

## Startup

### `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`

The site webapp is running a stale version of the plugin. Reinstall the addon and rebuild the host project:

```bash
# From the brxm-discovery project root
mvn -DskipTests install

# In your host project
mvn clean install
```

### Plugin beans missing / Spring context failures at startup

Ensure `brxm-discovery-site` is on the `site/webapp` classpath **and** on `site/components` (when that module exists). The addon assembly XML is only picked up when the JAR is present in the site webapp's `WEB-INF/lib`.

---

## Credentials and configuration

### Empty product grid — no error shown

Discovery credentials are missing or wrong. Verify using system properties to bypass JCR:

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY
```

Check the startup log for:
```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

If these lines are absent, the addon is not loaded.

### Per-channel credentials ignored — all channels use global credentials

`DiscoveryChannelInfo.discoveryAccountId` etc. override per-channel, but only when `hst:channelinfoclass` is set on the `hst:channel` node (not the mount). Verify with:

```bash
grep -r "hst:channelinfoclass" your-project/repository-data/
```

Should reference `org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo` (or a composite that extends it). See [11-channel-info.md](11-channel-info.md).

---

## Search and category

### `products: null` on a category page

Either no Category Document is configured, or the document is in Dynamic mode and the URL has no category ID.

- **Pinned mode:** open Channel Manager, select `DiscoveryCategoryGridComponent`, link a Category Document.
- **Dynamic mode:** confirm the URL matches `/category/{slug}/cid/{id}` or `?cid={id}`.

### `products: null` — keyword search

The search query `?q=` is blank or absent. Confirm the search form submits a non-empty `q` parameter.

### Facets not showing

`showFacets` component parameter defaults to `true`. Check that:
1. The component is `DiscoverySearchGridComponent` or `DiscoveryCategoryGridComponent` (not a custom subclass that bypasses facet model population).
2. The Discovery response actually returns facet data — some catalogs or queries may return none.

---

## Product detail

### `product: null` on PDP

Either no Product Detail Document is configured, or the document is in Dynamic mode and the URL has no PID.

- **Pinned mode:** link the Product Detail Document in Channel Manager.
- **Dynamic mode:** URL must be `/product/{slug}/pid/{id}` or `?pid={id}`.

---

## Recommendations

### Recommendation widget shows no products

1. Check that `authKey` (`BRXDIS_AUTH_KEY`) is set — v2 Pathways API requires it.
2. Verify the widget ID in the recommendation document matches a widget in the Discovery Dashboard.
3. For product-keyed widgets: confirm the page URL contains `?pid=` when in Dynamic mode, or that the document has a pinned `contextProductId`.
4. Confirm the widget is trained and active in the Discovery Dashboard.

---

## Visual search

### Camera button not visible

`discoveryVisualSearchEnabled` is `false` (default), or `discoveryVisualSearchWidgetId` is blank. Set both in **Channel Settings → Visual Search**.

### Upload returns `400 Bad Request`

Invalid or missing widget ID. The widget ID in Channel Settings must match a visual search widget in the Discovery Dashboard.

### Upload succeeds but results page shows keyword search

`imageId` URL param is present but `widgetId` param is missing. Check the redirect URL your template or frontend constructs after a successful upload.

### Wrong credentials used for visual search

`BrxdisVisualSearchPipeline` is mounted at the host root instead of under the channel mount. Move the `/_brxdis-api` mount to be a child of your channel mount — see [24-visual-search.md](24-visual-search.md#mount-placement).

---

## CMS picker

### Picker iframe shows blank or "Failed to load"

`brxm-discovery-cms` is not on the CMS webapp classpath, or `DiscoveryPickerModule` failed to start. Check the CMS startup log for exceptions from `DiscoveryPickerModule.initialize()`.

### Picker search returns 0 results

Discovery credentials are blank or incorrect. Check logs for HTTP errors from `DiscoveryPickerResource`.

### Product preview does not update after picking a new category

The category picker and the preview field must be in the same document, loaded in the same Channel Manager page. The `postMessage` sync only reaches same-origin sibling iframes — if the preview is in a different panel or browser window, the message will not arrive.

---

## Pixel tracking

### Pixels not firing

1. Check `brxdis.pixel.envEnabled` — if `false`, all pixels are globally disabled.
2. Check `discoveryPixelsEnabled` on the channel (`DiscoveryChannelInfo`) — if `false`, that channel's pixels are disabled.
3. For consent-gated channels: verify the consent cookie is present in the browser request before the page renders.

### Pixels firing in development / polluting analytics

Set `brxdis.pixel.envEnabled=false` locally, or use `brxdis.pixel.testData=true` to tag events with `test_data=true` so they can be filtered in the Discovery Dashboard.

---

## Logging

Enable `DEBUG` logging for the plugin:

```properties
# logback.xml or log4j2 configuration
log4j2.logger.discovery=DEBUG,org.bloomreach.forge.discovery
```

Key log lines:

| Message | Meaning |
|---|---|
| `Discovery pixel event fired: type=...` | Pixel dispatched successfully. |
| `Discovery pixel event failed: ...` | Pixel HTTP call failed (logged at WARN, silently discarded). |
| `Discovery API call: GET https://...` | HTTP request sent. |
| `HstDiscoveryService: no authKey configured, using v1 API` | Visual search and v2 recommendations will not work. |
| `DiscoveryConfigReader: no global config node found, using defaults` | JCR config node is missing; credentials must come from env/sys. |
