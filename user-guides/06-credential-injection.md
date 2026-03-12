# Credential Injection

This guide covers every supported method for injecting Discovery API credentials into the plugin without hardcoding them in the JCR repository.

## Resolution precedence

Credentials are resolved in two tiers.

### Channel identifiers (`accountId`, `domainKey`)

Resolved from the per-channel JCR config node. Env var and system property overrides apply at the JVM level (override all channels).

```
BRXDIS_ACCOUNT_ID / BRXDIS_DOMAIN_KEY  (global env var — wins if non-blank)
  → brxdis.accountId / brxdis.domainKey  (JVM system property)
    → brxdis:accountId / brxdis:domainKey  (JCR channel node field)
```

### API secrets (`apiKey`, `authKey`)

Resolved using the same three-tier chain as the channel identifiers.

```
BRXDIS_API_KEY / BRXDIS_AUTH_KEY  (global env var — wins if non-blank)
  → brxdis.apiKey / brxdis.authKey  (JVM system property)
    → brxdis:apiKey / brxdis:authKey  (JCR channel node field)
```

---

## Credential reference

| Credential | JCR channel node property | Global env var | System property | Required |
|---|---|---|---|---|
| Account ID | `brxdis:accountId` (value) | `BRXDIS_ACCOUNT_ID` | `brxdis.accountId` | Yes |
| Domain Key | `brxdis:domainKey` (value) | `BRXDIS_DOMAIN_KEY` | `brxdis.domainKey` | Yes |
| API Key | `brxdis:apiKey` (value) | `BRXDIS_API_KEY` | `brxdis.apiKey` | Yes |
| Auth Key | `brxdis:authKey` (value) | `BRXDIS_AUTH_KEY` | `brxdis.authKey` | No (enables v2 Pathways) |
| Environment | `brxdis:environment` (value) | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | No (default: `PRODUCTION`) |

`authKey` is only required when using the v2 Pathways recommendations API. When absent, the plugin falls back to v1 automatically — no error is thrown. `environment` accepts `PRODUCTION` or `STAGING`; the value drives API subdomain selection.

## Pixel base URI override

The pixel endpoint defaults to `https://p.brsrvr.com`. For regional deployments (e.g. EU), override it via env var or system property — no JCR edit required.

| Variable | Env var | System property | Default |
|---|---|---|---|
| Pixel base URI | `BRXDIS_PIXEL_BASEURI` | `brxdis.pixelBaseUri` | `https://p.brsrvr.com` |

Resolution order: env var → system property → CRISP JCR default (unchanged).

The override is applied by `DiscoveryPickerModule` at CMS startup, before CRISP reads its configuration, so the new value takes effect without any JCR edit or CRISP restart.

---

## Method 1: Environment variables (recommended for production)

Set the env vars before starting the JVM. In Docker:

```dockerfile
ENV BRXDIS_ACCOUNT_ID=your_account_id
ENV BRXDIS_DOMAIN_KEY=your_domain_key
ENV BRXDIS_API_KEY=your_api_key
# Optional: override pixel endpoint for regional deployments
# ENV BRXDIS_PIXEL_BASEURI=https://p-eu.brsrvr.com
```

In Kubernetes, reference a Secret:

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
  - name: BRXDIS_ENVIRONMENT
    value: "PRODUCTION"
  # Optional: override pixel endpoint for regional deployments
  # - name: BRXDIS_PIXEL_BASEURI
  #   value: "https://p-eu.brsrvr.com"
```

Create the secret:

```bash
kubectl create secret generic discovery-credentials \
  --from-literal=accountId=YOUR_ACCOUNT_ID \
  --from-literal=domainKey=YOUR_DOMAIN_KEY \
  --from-literal=apiKey=YOUR_API_KEY \
  --from-literal=authKey=YOUR_AUTH_KEY
```

Leave the JCR fields blank — the env vars take precedence regardless.

---

## Method 2: JVM system properties (local development)

Pass `-D` flags to the JVM at startup:

```bash
java -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
     -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
     -Dbrxdis.apiKey=YOUR_API_KEY \
     -jar server.jar
```

### Demo: local.properties file

The demo project reads system properties from a gitignored `conf/local.properties` file before Cargo starts:

```bash
cp demo/conf/local.properties.example demo/conf/local.properties
# Edit demo/conf/local.properties — fill in your credentials
cd demo && mvn -P cargo.run cargo:run
```

`local.properties` format:

```properties
# accountId and domainKey are stored in the global JCR config node (discovery-demo-channel.yaml)
# — no entry needed here unless you want to override them.
brxdis.apiKey=YOUR_API_KEY
brxdis.authKey=YOUR_AUTH_KEY
# brxdis.environment=PRODUCTION
# brxdis.pixelBaseUri=https://p-eu.brsrvr.com
```

Alternatively, pass them inline without a file:

```bash
cd demo && mvn -P cargo.run \
  -Dbrxdis.apiKey=ZZZ \
  -Dbrxdis.authKey=WWW \
  cargo:run
```

To replicate this pattern in your own project's `cargo.run` profile:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>properties-maven-plugin</artifactId>
  <version>1.2.1</version>
  <executions>
    <execution>
      <phase>initialize</phase>
      <goals><goal>read-project-properties</goal></goals>
      <configuration>
        <files><file>${project.basedir}/conf/local.properties</file></files>
        <quiet>true</quiet>  <!-- silently skip if file absent -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

Then in Cargo `<systemProperties>`:

```xml
<brxdis.accountId>${brxdis.accountId}</brxdis.accountId>
<brxdis.domainKey>${brxdis.domainKey}</brxdis.domainKey>
<brxdis.apiKey>${brxdis.apiKey}</brxdis.apiKey>
<brxdis.authKey>${brxdis.authKey}</brxdis.authKey>
```

---

## Method 3: JCR global config node (structural config and credential fallback)

If no env var or system property is set, `DiscoveryConfigReader` falls back to the value stored in the single global `brxdis:discoveryConfig` JCR node at:

```
/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig
```

Use this when:
- You cannot inject env vars at the infrastructure level.
- You want to manage structural config (base URIs, page size, sort) via the CMS without a restart.

Example YAML (place in your HCM application or development module):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'
      brxdis:apiKey: ''          # blank: falls back to BRXDIS_API_KEY env var or sys prop
      brxdis:authKey: ''         # optional, for v2 Pathways
      brxdis:baseUri: 'https://core.dxpapi.com'
      brxdis:pathwaysBaseUri: 'https://pathways.dxpapi.com'
      brxdis:environment: 'PRODUCTION'
      brxdis:defaultPageSize: 12
      brxdis:defaultSort: ''
```

Drawbacks for secrets:
- Credentials stored in the JCR node are persisted in the filestore, exported by auto-export in dev, and visible to CMS admins.
- Changes are cached at JVM lifetime. The cache is invalidated automatically when you save the node in the CMS — subsequent requests pick up the new value without a restart.

---

## Security notes

- Never commit `local.properties` — it is listed in `demo/.gitignore`.
- Prefer env vars (`BRXDIS_API_KEY` / `BRXDIS_AUTH_KEY`) or sys props over storing values in JCR — secrets in JCR are persisted in the filestore and visible to CMS admins.
- For production, prefer method 1 (global env vars). Secrets managers (AWS Secrets Manager, HashiCorp Vault) can inject env vars at container startup without ever writing the value to disk.
- `accountId` and `domainKey` are non-secret channel identifiers; storing them in JCR is safe.
- Credential changes in server env vars take effect on the next request after the JVM-lifetime cache is invalidated (triggered automatically by CMS node saves) — no restart required.
