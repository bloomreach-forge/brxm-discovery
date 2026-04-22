# Plugin Setup Guide

This guide picks up where installation ends. After adding the Maven dependencies, enabling the CRISP broker, and confirming the plugin bootstrapped, walk through the steps below to configure the plugin for your project.

> **Already here?** If you haven't installed yet, start at [01-installation.md](01-installation.md).

---

## How configuration works

The plugin has two config layers that combine at request time:

```
Global JCR node            Per-channel hst:channelinfo
─────────────────────      ───────────────────────────
accountId                  discoveryAccountId (override)
domainKey                  discoveryDomainKey (override)
apiKey                     discoveryApiKeyEnvVar (override)
authKey                    discoveryAuthKeyEnvVar (override)
defaultFieldList     ←     discoveryDefaultFieldList (override)
defaultPageSize
defaultSort
sortOptions
environment
```

**Global node** — one node shared by all channels. Set here what applies across the whole deployment (credentials, default field list, page size, sort).

**Per-channel** — optional overrides in `hst:channelinfo`. Use when channels point at different Discovery accounts or catalog schemas.

Resolved at request time. Channel value wins when set; otherwise the global node value applies; otherwise the coded default applies.

---

## Step 1 — Create the global config node

The plugin reads a single fixed JCR node:

```
/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig
```

Create it in your HCM config (place the file in your application or development module, e.g. `repository-data/application/src/main/resources/hcm-config/`):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig

      # ── Credentials ─────────────────────────────────────────────────────────
      # accountId and domainKey: identifiers, not secrets.
      # Safe to store in JCR; also configurable via env var or system property.
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'

      # apiKey and authKey: secrets.
      # Leave blank here — inject via BRXDIS_API_KEY / BRXDIS_AUTH_KEY env vars.
      # authKey is only required for v2 Pathways recommendations.
      brxdis:apiKey: ''
      brxdis:authKey: ''

      # ── Environment ──────────────────────────────────────────────────────────
      # PRODUCTION (default) or STAGING. Selects Discovery endpoints automatically.
      brxdis:environment: 'PRODUCTION'

      # ── Structural ──────────────────────────────────────────────────────────
      brxdis:defaultPageSize: 12
      brxdis:defaultSort: ''

      # ── Schema config ────────────────────────────────────────────────────────
      # Comma-separated fl param sent to Discovery. Covers Bloomreach reserved
      # attributes used by the reference templates out of the box.
      # Per-channel discoveryDefaultFieldList overrides this for individual channels.
      # brxdis:defaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description'
```

Only `accountId`, `domainKey`, and `apiKey` are required to run the plugin. Everything else has a coded default.

The node is optional: if it doesn't exist, the plugin builds config entirely from env vars / system properties + coded defaults.

---

## Step 2 — Inject credentials

Credentials resolve with env var → system property → JCR precedence. Secrets should never be stored in JCR in production.

### Production (containers / Kubernetes)

```yaml
# deployment.yaml
env:
  - name: BRXDIS_ACCOUNT_ID
    valueFrom:
      secretKeyRef:
        name: discovery-credentials
        key: accountId
  - name: BRXDIS_DOMAIN_KEY
    valueFrom:
      secretKeyRef:
        name: discovery-credentials
        key: domainKey
  - name: BRXDIS_API_KEY
    valueFrom:
      secretKeyRef:
        name: discovery-credentials
        key: apiKey
  # authKey only needed for v2 Pathways recommendations
  - name: BRXDIS_AUTH_KEY
    valueFrom:
      secretKeyRef:
        name: discovery-credentials
        key: authKey
```

### Local development (Cargo)

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY \
  -Dbrxdis.authKey=YOUR_AUTH_KEY
```

### Staging endpoints

Set `environment` to route all Discovery API calls to the Bloomreach staging tier:

```bash
BRXDIS_ENVIRONMENT=STAGING
# or
-Dbrxdis.environment=STAGING
```

The plugin substitutes `staging-core.dxpapi.com`, `staging-pathways.dxpapi.com`, and `staging-suggest.dxpapi.com` automatically. No CRISP node changes are needed.

See [06-credential-injection.md](06-credential-injection.md) for more deployment patterns.

---

## Step 3 — Configure the field list

The `fl` parameter controls which fields Discovery returns for each product. Only requested fields appear in the `ProductSummary.attributes` map that your templates and Page Model API consumers read.

### The default

The coded default covers all Bloomreach reserved attributes used by the reference templates:

```
pid,title,thumb_image,url,price,brand,sale_price,description
```

This works out of the box for any standard Discovery account. If your catalog only uses standard reserved attributes, you don't need to configure anything here.

### Extending for custom catalog fields

If your Discovery catalog has custom fields beyond the reserved set (e.g. `pet_type`, `tags`, `availability`), set `brxdis:defaultFieldList` on the global config node:

```yaml
brxdis:defaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description,pet_type,tags,availability'
```

This replaces the coded default entirely. Include all the fields you need — the value is not appended to the default, it replaces it.

### Trimming the field list (lightweight channels)

A mobile or performance-sensitive channel might want a smaller Page Model API response. Set a narrower list to reduce payload:

```yaml
brxdis:defaultFieldList: 'pid,title,thumb_image,url,price'
```

Fields absent from the `fl` list are never returned by Discovery and never appear in the Page Model API response.

---

## Step 4 — Configure sort options

The sort dropdown in Experience Manager and the `sortOptions` model key in the Page Model API both read from `brxdis:sortOptions`. Each entry is `value=Display label`.

```yaml
brxdis:sortOptions:
  - 'price asc=Price: Low to High'
  - 'price desc=Price: High to Low'
  - 'name asc=Name: A-Z'
  - 'name desc=Name: Z-A'
  - 'review_count desc=Most Reviewed'
```

When absent, the plugin falls back to:

```
price asc=Price: Low to High
price desc=Price: High to Low
name asc=Name: A-Z
name desc=Name: Z-A
```

The same list feeds both the CMS component config dropdown and the `sortOptions` model key — there is one source of truth.

---

## Step 5 — Configure picker display fields (optional)

The product picker in the CMS reads four fields from each Discovery result to display product rows. These default to Bloomreach's reserved names:

| Purpose | JCR property | Default |
|---|---|---|
| Product ID | `brxdis:pickerIdField` | `pid` |
| Display name | `brxdis:pickerTitleField` | `title` |
| Thumbnail | `brxdis:pickerImageField` | `thumb_image` |
| Price label | `brxdis:pickerPriceField` | `price` |

If your feed uses different field names (e.g. `sku` instead of `pid`, or `product_name` instead of `title`):

```yaml
brxdis:pickerIdField: 'sku'
brxdis:pickerTitleField: 'product_name'
brxdis:pickerImageField: 'image_url'
brxdis:pickerPriceField: 'regular_price'
```

The picker dialog HTML is not affected — it always receives stable `id`, `title`, `imageUrl`, `price` properties regardless of the feed field names.

---

## Step 6 — Per-channel setup

Use per-channel config when your deployment has multiple channels pointing at different Discovery accounts or catalog schemas. Set the overrides in `hst:channelinfo`:

```yaml
/hst:hst/hst:configurations/<your-site>/hst:workspace/hst:channel/hst:channelinfo:
  jcr:primaryType: hst:channelinfo

  # Credentials for this channel's Discovery account
  discoveryAccountId: '7291'
  discoveryDomainKey: 'petstore-uk'
  discoveryApiKeyEnvVar: BRXDIS_API_KEY_PETSTORE_UK
  discoveryAuthKeyEnvVar: BRXDIS_AUTH_KEY_PETSTORE_UK

  # Field list for this channel's catalog schema.
  # Replaces the global default — include all fields this channel needs.
  discoveryDefaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description,pet_type,species'
```

### When to use per-channel overrides

| Scenario | Override |
|---|---|
| Multiple brands, each with a separate Discovery account | `discoveryAccountId` + `discoveryDomainKey` |
| Channel-specific API keys (per-account secrets) | `discoveryApiKeyEnvVar` / `discoveryAuthKeyEnvVar` |
| Different catalog schemas (different custom fields per account) | `discoveryDefaultFieldList` |
| Mobile channel that wants a smaller payload | `discoveryDefaultFieldList` with a trimmed list |

Overrides are applied after global config is resolved. Any property left blank falls through to the global node or coded default.

### Multi-catalog pattern

A deployment with two Discovery accounts that have different custom fields:

```
Channel: /site/us     → accountId: acme-us, fl: pid,title,...,size,color
Channel: /site/pets   → accountId: acme-pets, fl: pid,title,...,pet_type,species
```

```yaml
# us channel
discoveryAccountId: 'acme-us'
discoveryDomainKey: 'acme-us'
discoveryApiKeyEnvVar: BRXDIS_API_KEY_US
discoveryDefaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description,size,color'

# pets channel
discoveryAccountId: 'acme-pets'
discoveryDomainKey: 'acme-pets'
discoveryApiKeyEnvVar: BRXDIS_API_KEY_PETS
discoveryDefaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description,pet_type,species'
```

Each channel requests only the fields relevant to its catalog. The global node still holds the shared structural defaults (`defaultPageSize`, `defaultSort`, etc.).

---

## Step 7 — Verify

Start the CMS and site webapps and check the logs:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

**Picker endpoint alive:**
```
GET http://localhost:8080/cms/ws/discovery/picker/search?q=shirt
```
Expected: a JSON array of products, not a 404.

**Site search working:**
```
GET http://localhost:8080/site/search?q=shirt
```
Expected: a rendered search results page with products.

**Page Model API (headless):**
```
GET http://localhost:8080/site/search?q=shirt
Accept: application/json
```
Expected: JSON response containing `products`, `facets`, `pagination`, `facetUrls`, `pageUrls`, and `sortUrl` under the search grid component models.

**Verify field list is correct:**
Check that `products[0].attributes` in the Page Model API response contains the fields you configured. Any field in `brxdis:defaultFieldList` (or the channel override) that Discovery returns will be present in `attributes`.

### Common issues

| Symptom | Cause | Fix |
|---|---|---|
| `ConfigurationException: CRISP ResourceServiceBroker not found` | `crisp.broker.registerService = true` missing | Add to site webapp `hst-config.properties` |
| `ConfigurationException: Discovery accountId is required` | Credentials not configured | Set `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY` env vars |
| Products grid empty, no error | Wrong account / domain key | Verify `accountId` and `domainKey` match your Discovery dashboard |
| `brand` / `description` missing from `attributes` | Custom `fl` excludes them | Add to `brxdis:defaultFieldList` or channel `discoveryDefaultFieldList` |
| Picker shows blank product names | `pickerTitleField` doesn't match your feed | Set `brxdis:pickerTitleField` to the correct feed field name |

---

## What to read next

| Guide | When you need it |
|---|---|
| [03-search-and-category.md](03-search-and-category.md) | Component parameters, URL params, facet and pagination wiring |
| [04-recommendations.md](04-recommendations.md) | Recommendation widgets, v2 Pathways API setup |
| [05-product-picker.md](05-product-picker.md) | CMS product/category picker for document types |
| [06-credential-injection.md](06-credential-injection.md) | Credential precedence, multi-environment patterns |
| [08-react-spa-integration.md](08-react-spa-integration.md) | Page Model API shapes, TypeScript types, React component examples |
| [09-pixel-tracking.md](09-pixel-tracking.md) | Pixel event configuration and per-channel flags |
