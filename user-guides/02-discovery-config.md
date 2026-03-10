# Discovery Configuration

Configuration lives on a `brxdis:discoveryConfig` JCR document created in the CMS. Channel Manager parameters on the mount act as the per-channel source of truth for account identifiers and point to server-side secret env vars for API keys.

---

## Field reference

### Channel identifiers — `accountId` and `domainKey`

Resolved per-request in this order (highest wins):

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Channel Manager | `discoveryAccountId`, `discoveryDomainKey` on the mount |
| 2 | Environment variable | `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY` |
| 3 | JVM system property | `brxdis.accountId`, `brxdis.domainKey` |
| 4 (lowest) | JCR document | `brxdis:accountId`, `brxdis:domainKey` |

Channel Manager wins whenever non-blank. This allows different channels to use different Discovery accounts without separate JVM deployments.

### API secrets — `apiKey` and `authKey`

Resolved per-request in this order:

| Priority | Source | How to set |
|---|---|---|
| 1 (highest) | Per-channel env var | Name specified in `discoveryApiKeyEnvVar` / `discoveryAuthKeyEnvVar` on the mount |
| 2 | Global environment variable | `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY` |
| 3 | JVM system property | `brxdis.apiKey`, `brxdis.authKey` |
| 4 (lowest) | JCR document | `brxdis:apiKey`, `brxdis:authKey` |

`discoveryApiKeyEnvVar` stores the **name** of an env var (e.g. `PACIFICHOME_API_KEY`), not the key value itself. The secret lives only in the server environment. Multi-channel deployments can point each channel's mount at a different env var.

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

Structural fields are edited in the CMS on the `brxdis:discoveryConfig` document. Coded defaults apply when the property is absent — no JCR document is required to run the plugin if credentials are supplied via environment variables.

---

## Credential injection

See the field tables above for full per-dimension precedence. The recommended setup for a multi-channel production deployment:

- Set `discoveryAccountId` and `discoveryDomainKey` in Channel Manager — each channel's mount points to its own Discovery account.
- Set `discoveryApiKeyEnvVar=MY_CHANNEL_API_KEY` in Channel Manager — the actual secret lives in the server environment, never in JCR.
- Use global env vars (`BRXDIS_API_KEY`) as a single-channel fallback or for local development.

See [06-credential-injection.md](06-credential-injection.md) for deployment-specific patterns.

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

If you run multiple channels with different Discovery accounts, set `discoveryConfigPath` to different document paths on each mount and set `discoveryAccountId`/`discoveryDomainKey` in Channel Manager on each mount.

**Cache behaviour:** structural config (`baseUri`, `defaultPageSize`, `defaultSort`) is JVM-lifetime cached per `discoveryConfigPath`. The cache is invalidated automatically when you save the `brxdis:discoveryConfig` document in the CMS — no JVM restart required. Changes are not instant; they take effect on the first request that arrives after the save triggers cache invalidation. Credentials (`accountId`, `domainKey`, `apiKey`, `authKey`) are re-evaluated from Channel Manager and server env vars on every request, so credential changes in Channel Manager or the server environment are picked up without a CMS save.
