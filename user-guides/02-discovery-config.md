# Discovery Configuration

Configuration lives on the module config JCR node bootstrapped by the CMS plugin under `/hippo:configuration/hippo:modules/brxm-discovery-picker/hippo:moduleconfig/discoveryConfig`. Credentials can be overridden by environment variables or JVM system properties — the JCR node is the last resort and acts as a CMS-editable fallback.

---

## Field reference

### Credentials (resolved: env var → sys prop → JCR → `null`)

| JCR property | Env var | System property | Description |
|---|---|---|---|
| `brxdis:accountId` | `BRXDIS_ACCOUNT_ID` | `brxdis.accountId` | Bloomreach Discovery account ID |
| `brxdis:domainKey` | `BRXDIS_DOMAIN_KEY` | `brxdis.domainKey` | Discovery domain key (catalog identifier) |
| `brxdis:apiKey` | `BRXDIS_API_KEY` | `brxdis.apiKey` | Discovery API key for authenticated requests |
| `brxdis:authKey` | `BRXDIS_AUTH_KEY` | `brxdis.authKey` | Auth key for v2 Pathways API (`auth-key` header). Optional — when absent, the plugin uses the v1 API. |
| `brxdis:environment` | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `PRODUCTION` (default) or `STAGING`. Drives API subdomain switching. |

`accountId`, `domainKey`, and `apiKey` are required. If none of the three resolution sources provide them, a `ConfigurationException` is thrown at request time.

### Structural config (resolved: JCR → coded default)

| JCR property | Default | Description |
|---|---|---|
| `brxdis:baseUri` | `https://core.dxpapi.com` | Base URL of the Discovery Search/Category API |
| `brxdis:pathwaysBaseUri` | `https://pathways.dxpapi.com` | Base URL of the Pathways recommendations API |
| `brxdis:searchBasePath` | `/api/v1/core` | Path for search API calls |
| `brxdis:categoryBasePath` | `/api/v1/core` | Path for category browse calls |
| `brxdis:recsBasePath` | `/api/v2/widgets` | Path prefix for recommendation widget calls |
| `brxdis:defaultPageSize` | `12` | Results per page when not specified in the request |
| `brxdis:defaultSort` | `` | Default sort expression, e.g. `price asc`. Blank = relevance. |

Structural fields are edited in the JCR console at the module config path. Coded defaults apply when the property is absent — no JCR node required to run the plugin.

---

## Credential injection

Credentials are resolved in this precedence (highest wins):

| Priority | Source | When to use |
|---|---|---|
| 1 | Environment variable | Containers, Kubernetes, CI/CD pipelines |
| 2 | JVM system property | Local development, shell scripts |
| 3 | JCR module config node | CMS-editable fallback |

Leave the JCR field blank when injecting from the environment. See [06-credential-injection.md](06-credential-injection.md) for deployment-specific guidance.

---

## JCR-less operation

If `discoveryConfigPath` is not set on the mount, or the JCR node is missing, the plugin builds `DiscoveryConfig` entirely from environment variables / system properties + coded defaults. No JCR node is required to run the plugin — credentials must come from the environment in that case.

---

## CRISP resource spaces

The CMS module bootstraps all three CRISP resource spaces automatically via `cms/src/main/resources/hcm-config/brxdis-crisp.yaml`. No manual CRISP configuration is required in your host project.

| Resource space | Base URI | Used for |
|---|---|---|
| `discoverySearchAPI` | `https://core.dxpapi.com` | Search, category browse, widget listing, v1 recommendations |
| `discoveryPathwaysAPI` | `https://pathways.dxpapi.com` | v2 Pathways recommendations (requires `authKey`) |
| `discoveryAutosuggestAPI` | `https://suggest.dxpapi.com` | Autosuggest / typeahead |

The `account_id`, `domain_key`, and `auth-key` parameters are added per-request — they do not go in the CRISP config.

### Overriding base URIs (staging / private cloud)

If you need a non-default base URI (e.g. a staging endpoint), override the CRISP property in your host project's HCM config:

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer/discoverySearchAPI:
      crisp:propvalues:
        - 'https://staging-core.dxpapi.com'
```

---

## Link the config to a channel

Add `discoveryConfigPath` as a mount parameter pointing at the module config node:

```yaml
definitions:
  config:
    /hst:hst/hst:hosts/dev-localhost/localhost/hst:root:
      hst:parameternames: [discoveryConfigPath]
      hst:parametervalues: ['/hippo:configuration/hippo:modules/brxm-discovery-picker/hippo:moduleconfig/discoveryConfig']
```

All HST components read `discoveryConfigPath` from the resolved mount and pass it to `DiscoveryConfigResolver`. When absent or pointing to a missing node, they fall back to env/sys + coded defaults.

If you run multiple channels with different Discovery accounts, set `discoveryConfigPath` to different node paths on each mount.
