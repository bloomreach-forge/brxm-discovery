# Credential Injection

This guide covers the supported ways to provide Discovery credentials and related runtime settings.

The plugin uses one global config node:

```text
/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig
```

The plugin also supports optional per-channel overrides through `hst:channelinfo` when your channel info interface extends `org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo`.

## Resolution precedence

**Credentials** (`accountId`, `domainKey`, `apiKey`, `authKey`, `environment`) are secrets and runtime values. They resolve via `env var -> system property -> JCR`:

| Setting | Env var | System property | JCR property | Required |
|---|---|---|---|---|
| Account ID | `BRXDIS_ACCOUNT_ID` | `brxdis.accountId` | `brxdis:accountId` | Yes |
| Domain Key | `BRXDIS_DOMAIN_KEY` | `brxdis.domainKey` | `brxdis:domainKey` | Yes |
| API Key | `BRXDIS_API_KEY` | `brxdis.apiKey` | `brxdis:apiKey` | Yes |
| Auth Key | `BRXDIS_AUTH_KEY` | `brxdis.authKey` | `brxdis:authKey` | No |
| Environment | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `brxdis:environment` | No |

`authKey` enables v2 Pathways recommendations. When it is absent, recommendations fall back to v1 automatically.

**Schema config** (`defaultFieldList`, sort options, picker field aliases) is application config, not secrets. It resolves `JCR -> coded default` only — no env var or system property is consulted. Set it in the JCR config node or via per-channel override in `hst:channelinfo`.

## Optional per-channel overrides

After the global config is resolved, the site layer can apply per-channel overrides from `hst:channelinfo`:

| Channel property | Meaning |
|---|---|
| `discoveryAccountId` | Channel-specific account ID |
| `discoveryDomainKey` | Channel-specific domain key |
| `discoveryApiKeyEnvVar` | Env-var name to read the API key from for this channel |
| `discoveryAuthKeyEnvVar` | Env-var name to read the auth key from for this channel |
| `discoveryDefaultFieldList` | Comma-separated `fl` field list for this channel's Discovery catalog (replaces global default) |

That means:

- account ID and domain key can be overridden directly per channel
- API key and auth key remain secret values in environment variables; the channel stores only the env-var names
- channels pointing at different Discovery accounts or catalogs declare their own field list, so the page model API response is trimmed to what that schema actually provides

The `discoveryDefaultFieldList` value is a **full replacement**. Leave it blank to inherit the global JCR default or the coded default (`pid,title,thumb_image,url,price,brand,sale_price,description`).

Example:

```yaml
/hst:hst/hst:configurations/<your-site>/hst:workspace/hst:channel/hst:channelinfo:
  jcr:primaryType: hst:channelinfo
  discoveryAccountId: '6413'
  discoveryDomainKey: pacifichome
  discoveryApiKeyEnvVar: BRXDIS_API_KEY
  discoveryAuthKeyEnvVar: BRXDIS_AUTH_KEY
  # Override field list for this catalog's schema (replaces global default)
  discoveryDefaultFieldList: 'pid,title,thumb_image,url,price,brand,sale_price,description,pet_type,tags'
```

## Structural settings

These settings are read from the same JCR node. They are not configurable via env var or system property — they are application config, not secrets.

- `brxdis:baseUri`
- `brxdis:pathwaysBaseUri`
- `brxdis:autosuggestBaseUri`
- `brxdis:defaultPageSize`
- `brxdis:defaultSort`
- `brxdis:defaultFieldList`

If a base URI property is absent, the default comes from `environment`:

- `PRODUCTION`: `core.dxpapi.com`, `pathways.dxpapi.com`, `suggest.dxpapi.com`
- `STAGING`: `staging-core.dxpapi.com`, `staging-pathways.dxpapi.com`, `staging-suggest.dxpapi.com`

## Switching to the staging endpoint

Set `environment` to `STAGING` to route all Discovery API calls to the Bloomreach staging tier:

```bash
# env var (recommended for containers / CI)
BRXDIS_ENVIRONMENT=STAGING

# system property (useful for local dev with mvn cargo)
-Dbrxdis.environment=STAGING
```

Or via the JCR config node (takes effect after the cache invalidates):

```yaml
brxdis:environment: 'STAGING'
```

When `environment = STAGING` the plugin automatically substitutes the staging base URIs:

| API | Staging URI |
|---|---|
| Core search / category | `https://staging-core.dxpapi.com` |
| Pathways recommendations | `https://staging-pathways.dxpapi.com` |
| Autosuggest | `https://staging-suggest.dxpapi.com` |

If you also set `brxdis:baseUri`, `brxdis:pathwaysBaseUri`, or `brxdis:autosuggestBaseUri` explicitly, those always win — the `environment` value only applies when the corresponding URI property is absent.

The change is hot-loaded: a JCR edit to `brxdis:environment` triggers cache invalidation and CRISP resource-space re-initialization without a server restart.

## Recommended deployment pattern

Use env vars for secrets and keep JCR as fallback or structural config only.

```yaml
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
  - name: BRXDIS_AUTH_KEY
    valueFrom:
      secretKeyRef:
        name: discovery-credentials
        key: authKey
```

For local development, system properties are often simplest:

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY \
  -Dbrxdis.authKey=YOUR_AUTH_KEY
```

If your project uses multiple channels, a production-friendly pattern is:

- keep shared defaults and structural settings in the global config node
- keep secrets in env vars
- use `hst:channelinfo` for channel-specific account/domain overrides, env-var names, and field list overrides when channels point at different Discovery catalogs

## JCR config example

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

## Cache behaviour

- The resolved base config is cached in-process.
- JCR changes invalidate that cache automatically through observation.
- Env var and system property credential overrides are re-applied on each provider read.
- On the site side, `DiscoveryConfigProvider` is also registered in `HippoServiceRegistry` so the CRISP addon-module resolvers can read the same active settings at runtime.

That means:

- env/sys credential changes are picked up on the next config read
- JCR structural changes are picked up after the config node changes and the cache invalidates

## Pixel base URI override

Pixel traffic uses a separate override path:

| Env var | System property | Default |
|---|---|---|
| `BRXDIS_PIXEL_BASEURI` | `brxdis.pixelBaseUri` | `https://p.brsrvr.com` |

This override is applied by the CMS module at startup and is separate from the Discovery config node.

## Security notes

- Prefer env vars or system properties for secrets.
- Treat JCR secrets as compatibility fallback, not the primary deployment model.
- `accountId` and `domainKey` are identifiers, not secrets.
