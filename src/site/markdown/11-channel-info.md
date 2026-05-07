# DiscoveryChannelInfo Reference

`DiscoveryChannelInfo` is an HST channel-info interface that lets each brXM channel override global Discovery settings without touching the shared JCR config node. It is implemented as an `hst:channelinfo` node in your HST configuration.

The 13 available properties are grouped into four areas:

| Group | Properties |
|---|---|
| Credentials | `discoveryAccountId`, `discoveryDomainKey`, `discoveryApiKeyEnvVar`, `discoveryAuthKeyEnvVar` |
| Schema | `discoveryDefaultFieldList`, `discoveryCatalogName` |
| Pixel tracking | `discoveryPixelsEnabled`, `discoveryPixelConsentCookie`, `discoveryPixelTestData`, `discoveryPixelDebug`, `discoveryPixelRegion` |
| Visual search | `discoveryVisualSearchEnabled`, `discoveryVisualSearchWidgetId` |

---

## How channel info interacts with global config

1. Global config (`brxdis:discoveryConfig` JCR node) is resolved first.
2. Channel-info fields are applied on top — any non-blank value replaces its global counterpart for that channel's requests.
3. Channels with no channel-info node (or blank fields) inherit the global config unchanged.

---

## Group 1 — Credentials

These four fields let one deployment serve multiple Discovery accounts, environments, or tenants.

| Property | Type | Default | Description |
|---|---|---|---|
| `discoveryAccountId` | `String` | `""` | Override the Discovery account ID for this channel |
| `discoveryDomainKey` | `String` | `""` | Override the domain key for this channel |
| `discoveryApiKeyEnvVar` | `String` | `""` | Name of the environment variable that holds the API key for this channel (e.g. `BRXDIS_STAGING_API_KEY`) |
| `discoveryAuthKeyEnvVar` | `String` | `""` | Name of the environment variable that holds the Pathways v2 auth key for this channel |

`discoveryApiKeyEnvVar` and `discoveryAuthKeyEnvVar` are indirection pointers — they name an env var, they do not hold the secret directly. This keeps secrets out of JCR and allows per-channel secret rotation without a CMS redeploy. See [12-credential-injection.md](12-credential-injection.md) for deployment patterns.

---

## Group 2 — Schema

| Property | Type | Default | Description |
|---|---|---|---|
| `discoveryDefaultFieldList` | `String` | `""` | Comma-separated list of Discovery `fl` fields for this channel. Replaces the global default. Leave blank to use the global default. |
| `discoveryCatalogName` | `String` | `""` | Discovery catalog name for multi-catalog deployments. When set, passed as `catalog_name` on every API request for this channel. |

`discoveryDefaultFieldList` is a **full replacement**, not an append. A channel needing fewer fields for a mobile experience sets only the fields it uses. When blank, the global `brxdis:defaultFieldList` (or coded default `pid,title,thumb_image,url,price,brand,sale_price,description`) applies.

---

## Group 3 — Pixel tracking

| Property | Type | Default | Description |
|---|---|---|---|
| `discoveryPixelsEnabled` | `boolean` | `true` | Enable or disable all pixel firing for this channel |
| `discoveryPixelConsentCookie` | `String` | `""` | Name of a consent cookie. When set, pixels only fire if the browser sends this cookie with a non-empty value. Leave blank to fire unconditionally. |
| `discoveryPixelTestData` | `boolean` | `false` | Mark all pixel events for this channel as test data (excluded from production analytics) |
| `discoveryPixelDebug` | `boolean` | `false` | Enable verbose pixel logging to the site/CMS log |
| `discoveryPixelRegion` | `String` | `US` | Pixel endpoint region: `US` or `EU`. Must match where your Discovery account's pixel data is stored. |

`discoveryPixelRegion` corresponds to the `brxdis.pixel.region` JVM system property, which applies globally. The channel property takes precedence over the system property for that channel's requests.

For full pixel tracking documentation including all event types and environment-level kill switches, see [50-pixel-tracking.md](50-pixel-tracking.md).

---

## Group 4 — Visual search

| Property | Type | Default | Description |
|---|---|---|---|
| `discoveryVisualSearchEnabled` | `boolean` | `false` | Enable visual (image) search for this channel |
| `discoveryVisualSearchWidgetId` | `String` | `""` | Discovery widget ID to use for visual search queries |

Visual search requires a v2 Pathways API credential (`discoveryAuthKeyEnvVar` / `BRXDIS_AUTH_KEY`). When disabled, `DiscoverySearchGridComponent` ignores image-search requests and returns no results.

For visual search endpoint wiring (default `/_brxdis-api/visual-search/` mount), see [24-visual-search.md](24-visual-search.md).

---

## HCM YAML example

```yaml
/hst:hst/hst:configurations/<your-site>/hst:workspace/hst:channel/hst:channelinfo:
  jcr:primaryType: hst:channelinfo
  # Credentials
  discoveryAccountId: '6413'
  discoveryDomainKey: pacifichome
  discoveryApiKeyEnvVar: BRXDIS_API_KEY
  discoveryAuthKeyEnvVar: BRXDIS_AUTH_KEY
  # Schema
  discoveryDefaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description'
  discoveryCatalogName: ''
  # Pixel
  discoveryPixelsEnabled: true
  discoveryPixelConsentCookie: ''
  discoveryPixelTestData: false
  discoveryPixelDebug: false
  discoveryPixelRegion: US
  # Visual search
  discoveryVisualSearchEnabled: false
  discoveryVisualSearchWidgetId: ''
```

---

## Multi-tenant pattern

When multiple sites share one brXM deployment but connect to different Discovery accounts:

```yaml
# site-a/hst:channelinfo
discoveryAccountId: 'account-a'
discoveryDomainKey: site-a
discoveryApiKeyEnvVar: BRXDIS_SITE_A_API_KEY

# site-b/hst:channelinfo  
discoveryAccountId: 'account-b'
discoveryDomainKey: site-b
discoveryApiKeyEnvVar: BRXDIS_SITE_B_API_KEY
```

Both channels share the global `brxdis:discoveryConfig` node for base URIs and structural defaults; only the per-account credentials differ.
