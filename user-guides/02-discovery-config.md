# Discovery Configuration

Configuration lives on a `brxdis:discoveryConfig` JCR document created in the CMS. Credentials can be overridden by environment variables or JVM system properties — the JCR document is the last resort and acts as a CMS-editable fallback.

---

## Field reference

### Credentials (resolved: env var → sys prop → JCR)

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
| `brxdis:defaultPageSize` | `12` | Results per page when not specified in the request |
| `brxdis:defaultSort` | `` | Default sort expression, e.g. `price asc`. Blank = relevance. |

Structural fields are edited in the CMS on the `brxdis:discoveryConfig` document. Coded defaults apply when the property is absent — no JCR document is required to run the plugin if credentials are supplied via environment variables.

---

## Credential injection

Credentials are resolved in this precedence (highest wins):

| Priority | Source | When to use |
|---|---|---|
| 1 | Environment variable | Containers, Kubernetes, CI/CD pipelines |
| 2 | JVM system property | Local development, shell scripts |
| 3 | JCR document field | CMS-editable fallback; per-channel override |

Leave the JCR field blank when injecting from the environment. See [06-credential-injection.md](06-credential-injection.md) for deployment-specific guidance.

---

## JCR-less operation

If `discoveryConfigPath` is not set on the mount, or the JCR document is missing, the plugin builds `DiscoveryConfig` entirely from environment variables / system properties + coded defaults. No JCR document is required to run the plugin — credentials must come from the environment in that case.

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

Add `discoveryConfigPath` as a mount parameter pointing at your `brxdis:discoveryConfig` document:

```yaml
definitions:
  config:
    /hst:hst/hst:hosts/dev-localhost/localhost/hst:root:
      hst:parameternames: [discoveryConfigPath]
      hst:parametervalues: ['/content/documents/administration/discovery-config/discovery-config']
```

All HST components read `discoveryConfigPath` from the resolved mount. When absent or pointing to a missing document, they fall back to env/sys + coded defaults.

If you run multiple channels with different Discovery accounts, set `discoveryConfigPath` to different document paths on each mount. The config is JVM-lifetime cached per path and invalidated automatically when the CMS document changes — no JVM restart required.
