# Discovery Configuration

Configuration has two layers:

- a global `brxdis:discoveryConfig` JCR node for shared defaults and structural settings
- optional per-channel overrides through `hst:channelinfo`

**Credentials** (`accountId`, `domainKey`, `apiKey`, `authKey`) resolve with `env -> sys -> JCR` precedence — secrets belong in the environment, not JCR.

**Schema config** (`defaultFieldList`, sort options, picker field aliases) is application config, not secrets. It resolves `JCR -> coded default` only. No env var or system property is consulted. Per-channel `discoveryDefaultFieldList` on `hst:channelinfo` takes precedence over the global JCR node.

---

## Field reference

### Channel identifiers - `accountId` and `domainKey`

Resolved per-request in this order (highest wins):

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY` |
| 2 | JVM system property | `brxdis.accountId`, `brxdis.domainKey` |
| 3 (lowest) | JCR global node | `brxdis:accountId`, `brxdis:domainKey` |

### API secrets - `apiKey` and `authKey`

Resolved per-request in this order:

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY` |
| 2 | JVM system property | `brxdis.apiKey`, `brxdis.authKey` |
| 3 (lowest) | JCR global node | `brxdis:apiKey`, `brxdis:authKey` |

`authKey` is only required for v2 Pathways recommendations; when absent the plugin uses the v1 API automatically.

### Channel-level overrides — `hst:channelinfo`

Optional per-channel overrides let each channel point at a different Discovery account, catalog, or env-var set. All 13 `DiscoveryChannelInfo` fields are documented in [11-channel-info.md](11-channel-info.md).

### Other credentials

| JCR property | Env var | System property | Description |
|---|---|---|---|
| `brxdis:environment` | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `PRODUCTION` (default) or `STAGING`. Selects the default Discovery endpoints when explicit base URIs are not set. |

### Required fields

`accountId`, `domainKey`, and `apiKey` are required. If none of the resolution sources provide them after all layers are evaluated, a `ConfigurationException` is thrown at request time.

### Structural config (resolved: JCR -> coded default)

These fields are application config, not secrets. They are read from JCR only — no env var or system property is consulted.

| JCR property | Default | Description |
|---|---|---|
| `brxdis:baseUri` | `https://core.dxpapi.com` (staging: `https://staging-core.dxpapi.com`) | Base URL of the Discovery Search/Category API |
| `brxdis:pathwaysBaseUri` | `https://pathways.dxpapi.com` (staging: `https://staging-pathways.dxpapi.com`) | Base URL of the Pathways recommendations API |
| `brxdis:autosuggestBaseUri` | `https://suggest.dxpapi.com` (staging: `https://staging-suggest.dxpapi.com`) | Base URL of the Autosuggest API |
| `brxdis:defaultPageSize` | `12` | Results per page when not specified in the request |
| `brxdis:defaultSort` | `` | Default sort expression, e.g. `price asc`. Blank = relevance. |
| `brxdis:defaultFieldList` | `pid,title,thumb_image,url,price,brand,sale_price,description` | Comma-separated list of Discovery fields to request (`fl` param). Covers Bloomreach reserved attributes used by the reference templates. Per-channel `discoveryDefaultFieldList` overrides this. |

If a base URI property is absent, the default is derived from `environment`.

---

## Global config node path

All channels can read shared defaults from a single fixed JCR node:

```
/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig
```

To create the node in your HCM config (runs once; place in your application or development module):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'
      brxdis:apiKey: ''
      brxdis:authKey: ''
      brxdis:baseUri: 'https://core.dxpapi.com'
      brxdis:pathwaysBaseUri: 'https://pathways.dxpapi.com'
      brxdis:autosuggestBaseUri: 'https://suggest.dxpapi.com'
      brxdis:environment: 'PRODUCTION'
      brxdis:defaultPageSize: 12
      brxdis:defaultSort: ''
```

Leave `brxdis:apiKey` / `brxdis:authKey` blank and inject the actual secrets via env vars (see [12-credential-injection.md](12-credential-injection.md)).

---

See [12-credential-injection.md](12-credential-injection.md) for production credential deployment patterns.

---

## JCR-less operation

If the global config node is missing, the plugin builds `DiscoveryConfig` entirely from environment variables / system properties + coded defaults. No JCR node is required to run the plugin - credentials must come from the environment in that case.

---

## Discovery API endpoints

The plugin calls Discovery APIs directly over HTTP/2 using `java.net.http.HttpClient`. Credentials and base URIs are resolved from `DiscoveryConfig` at request time — `account_id` and `domain_key` go in the query string, `auth_key` is used for standard requests, and Pathways v2 calls send `auth-key` as a header.

| API | Default base URI | Used for |
|---|---|---|
| Search API | `brxdis:baseUri` or env default (`core.dxpapi.com`) | Search, category browse, widget listing, v1 recommendations |
| Pathways API | `brxdis:pathwaysBaseUri` or env default (`pathways.dxpapi.com`) | v2 Pathways recommendations |
| Autosuggest API | `brxdis:autosuggestBaseUri` or env default (`suggest.dxpapi.com`) | Autosuggest / typeahead |

### Overriding base URIs (staging / private cloud)

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      brxdis:baseUri: 'https://custom-core.example'
      brxdis:pathwaysBaseUri: 'https://custom-pathways.example'
      brxdis:autosuggestBaseUri: 'https://custom-suggest.example'
```

---

## Cache behaviour

The resolved base config is cached at JVM lifetime and invalidated automatically when you save the `brxdis:discoveryConfig` node in the CMS.

Credential overrides from env vars and system properties are re-applied each time the provider is read, so env/sys credential changes do not depend on a JCR save. Structural JCR settings (`baseUri`, `pathwaysBaseUri`, `autosuggestBaseUri`, `defaultPageSize`, `defaultSort`) still depend on cache invalidation after a CMS save.
