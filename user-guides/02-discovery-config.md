# Discovery Configuration

Configuration lives in a single `brxdis:discoveryConfig` JCR node at a fixed global path. All channels share this node. Credentials use a three-tier env var → sys prop → JCR resolution; structural config uses JCR → coded defaults.

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

### Other credentials

| JCR property | Env var | System property | Description |
|---|---|---|---|
| `brxdis:environment` | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `PRODUCTION` (default) or `STAGING`. Drives API subdomain switching. |

### Required fields

`accountId`, `domainKey`, and `apiKey` are required. If none of the resolution sources provide them after all layers are evaluated, a `ConfigurationException` is thrown at request time.

### Structural config (resolved: JCR → coded default)

| JCR property | Default | Description |
|---|---|---|
| `brxdis:baseUri` | `https://core.dxpapi.com` | Base URL of the Discovery Search/Category API |
| `brxdis:pathwaysBaseUri` | `https://pathways.dxpapi.com` | Base URL of the Pathways recommendations API |
| `brxdis:defaultPageSize` | `12` | Results per page when not specified in the request |
| `brxdis:defaultSort` | `` | Default sort expression, e.g. `price asc`. Blank = relevance. |

Structural fields are edited in the CMS on the `brxdis:discoveryConfig` node. Coded defaults apply when the property is absent — no JCR node is required to run the plugin if credentials are supplied via environment variables.

---

## Global config node path

All channels read from a single fixed JCR node:

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
      brxdis:environment: 'PRODUCTION'
      brxdis:defaultPageSize: 12
      brxdis:defaultSort: ''
```

Leave `brxdis:apiKey` / `brxdis:authKey` blank and inject the actual secrets via env vars (see [06-credential-injection.md](06-credential-injection.md)).

---

## Credential injection

The recommended production setup:

- Set `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY` as env vars or sys props — or in the JCR global node as fallback.
- Set `BRXDIS_API_KEY` and `BRXDIS_AUTH_KEY` as env vars (never store secrets in JCR).
- Leave the JCR node fields blank for secrets — env var resolution takes precedence automatically.

See [06-credential-injection.md](06-credential-injection.md) for deployment-specific patterns.

---

## JCR-less operation

If the global config node is missing, the plugin builds `DiscoveryConfig` entirely from environment variables / system properties + coded defaults. No JCR node is required to run the plugin — credentials must come from the environment in that case.

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

## Cache behaviour

Structural config (`baseUri`, `defaultPageSize`, `defaultSort`) is JVM-lifetime cached. The cache is invalidated automatically when you save the `brxdis:discoveryConfig` node in the CMS — no JVM restart required. Changes take effect on the first request after the save triggers cache invalidation.

Credentials (`accountId`, `domainKey`, `apiKey`, `authKey`) are also cached at JVM lifetime and invalidated by the same CMS-save event. To pick up credential changes from env vars / sys props without a CMS save, trigger an invalidation by making a trivial edit to the JCR config node.
