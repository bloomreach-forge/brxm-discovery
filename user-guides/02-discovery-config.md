# Discovery Configuration

Configuration has two layers:

- a global `brxdis:discoveryConfig` JCR node for shared defaults and structural settings
- optional per-channel overrides through `hst:channelinfo`

Global credentials resolve with `env -> sys -> JCR` precedence. Channel-level overrides are applied on top of that base config for `accountId`, `domainKey`, and env-var names for `apiKey` / `authKey`. Structural settings still resolve from the global JCR node when present and otherwise fall back to environment-aware defaults.

---

## Field reference

### Channel identifiers — `accountId` and `domainKey`

Resolved per-request in this order (highest wins):

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY` |
| 2 | JVM system property | `brxdis.accountId`, `brxdis.domainKey` |
| 3 (lowest) | JCR global node | `brxdis:accountId`, `brxdis:domainKey` |

### API secrets — `apiKey` and `authKey`

Resolved per-request in this order:

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY` |
| 2 | JVM system property | `brxdis.apiKey`, `brxdis.authKey` |
| 3 (lowest) | JCR global node | `brxdis:apiKey`, `brxdis:authKey` |

`authKey` is only required for v2 Pathways recommendations; when absent the plugin uses the v1 API automatically.

### Channel-level overrides — `hst:channelinfo`

Optional per-channel overrides are resolved after the global config:

| Channel property | Purpose |
|---|---|
| `discoveryAccountId` | Override account ID for this channel |
| `discoveryDomainKey` | Override domain key for this channel |
| `discoveryApiKeyEnvVar` | Name of the env var to read the API key from for this channel |
| `discoveryAuthKeyEnvVar` | Name of the env var to read the Pathways auth key from for this channel |

This lets one deployment share the addon while keeping channel-specific values in Channel Manager and secrets in environment variables.

### Other credentials

| JCR property | Env var | System property | Description |
|---|---|---|---|
| `brxdis:environment` | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `PRODUCTION` (default) or `STAGING`. Selects the default Discovery endpoints when explicit base URIs are not set. |

### Required fields

`accountId`, `domainKey`, and `apiKey` are required. If none of the resolution sources provide them after all layers are evaluated, a `ConfigurationException` is thrown at request time.

### Structural config (resolved: JCR -> environment-aware default)

| JCR property | Default | Description |
|---|---|---|
| `brxdis:baseUri` | `https://core.dxpapi.com` or `https://staging-core.dxpapi.com` | Base URL of the Discovery Search/Category API |
| `brxdis:pathwaysBaseUri` | `https://pathways.dxpapi.com` or `https://staging-pathways.dxpapi.com` | Base URL of the Pathways recommendations API |
| `brxdis:autosuggestBaseUri` | `https://suggest.dxpapi.com` or `https://staging-suggest.dxpapi.com` | Base URL of the Autosuggest API |
| `brxdis:defaultPageSize` | `12` | Results per page when not specified in the request |
| `brxdis:defaultSort` | `` | Default sort expression, e.g. `price asc`. Blank = relevance. |

Structural fields are edited in the CMS on the `brxdis:discoveryConfig` node. If a base URI property is absent, the plugin derives the default from `environment`.

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

Leave `brxdis:apiKey` / `brxdis:authKey` blank and inject the actual secrets via env vars (see [06-credential-injection.md](06-credential-injection.md)).

---

## Channel config example

If your project uses `DiscoveryChannelInfo` or a composite interface that extends it, you can set channel-specific overrides in `hst:channelinfo`:

```yaml
/hst:hst/hst:configurations/<your-site>/hst:workspace/hst:channel/hst:channelinfo:
  jcr:primaryType: hst:channelinfo
  discoveryAccountId: '6413'
  discoveryDomainKey: pacifichome
  discoveryApiKeyEnvVar: BRXDIS_API_KEY
  discoveryAuthKeyEnvVar: BRXDIS_AUTH_KEY
```

## Credential injection

The recommended production setup:

- Set `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY` as env vars or sys props — or in the JCR global node as fallback.
- Set `BRXDIS_API_KEY` and `BRXDIS_AUTH_KEY` as env vars (never store secrets in JCR).
- If channels need different account/domain values or different secret env-var names, set `discoveryAccountId`, `discoveryDomainKey`, `discoveryApiKeyEnvVar`, and `discoveryAuthKeyEnvVar` on `hst:channelinfo`.
- Leave the JCR node fields blank for secrets — env var resolution takes precedence automatically.

See [06-credential-injection.md](06-credential-injection.md) for deployment-specific patterns.

---

## JCR-less operation

If the global config node is missing, the plugin builds `DiscoveryConfig` entirely from environment variables / system properties + coded defaults. No JCR node is required to run the plugin — credentials must come from the environment in that case.

---

## CRISP resource spaces

The CMS module bootstraps the Discovery CRISP resource spaces automatically via `cms/src/main/resources/hcm-config/brxdis-crisp.yaml`. The site also ships matching fallback resolver beans. Both runtimes now use the same config-backed resolver model.

| Resource space | Base URI | Used for |
|---|---|---|
| `discoverySearchAPI` | Resolved from `brxdis:baseUri` or environment default | Search, category browse, widget listing, v1 recommendations |
| `discoveryPathwaysAPI` | Resolved from `brxdis:pathwaysBaseUri` or environment default | v2 Pathways recommendations |
| `discoveryAutosuggestAPI` | Resolved from `brxdis:autosuggestBaseUri` or environment default | Autosuggest / typeahead |

The request credentials are added per request. `account_id` and `domain_key` go in the query string, `auth_key` is used for standard Discovery requests, and the Pathways v2 call sends `auth-key` as a header.

On the site side, the resolver does not direct-wire `DiscoveryConfigProvider` from the Discovery addon Spring context. Instead, the site addon registers `DiscoveryConfigProvider` in `HippoServiceRegistry`, and the CRISP resolver looks it up there at runtime. This avoids cross-addon Spring visibility issues in real host projects.

### Overriding base URIs (staging / private cloud)

Set the Discovery config properties instead of editing CRISP nodes directly:

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
