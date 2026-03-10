# Credential Injection

This guide covers every supported method for injecting Discovery API credentials into the plugin without hardcoding them in the JCR repository.

## Resolution precedence

Credentials are resolved in two tiers.

### Channel identifiers (`accountId`, `domainKey`)

Applied per-request. Channel Manager is the authoritative source so that different mounts can use different Discovery accounts.

```
discoveryAccountId / discoveryDomainKey  (Channel Manager — wins if non-blank)
  → BRXDIS_ACCOUNT_ID / BRXDIS_DOMAIN_KEY  (global env var)
    → brxdis.accountId / brxdis.domainKey  (JVM system property)
      → brxdis:accountId / brxdis:domainKey  (JCR document field)
```

### API secrets (`apiKey`, `authKey`)

Applied per-request. Per-channel env var names are stored in Channel Manager; the secret values live only on the server.

```
env var named by discoveryApiKeyEnvVar / discoveryAuthKeyEnvVar  (per-channel env var — wins if non-blank)
  → BRXDIS_API_KEY / BRXDIS_AUTH_KEY  (global env var)
    → brxdis.apiKey / brxdis.authKey  (JVM system property)
      → brxdis:apiKey / brxdis:authKey  (JCR document field)
```

---

## Credential reference

| Credential | Channel Manager param | Global env var | System property | JCR property | Required |
|---|---|---|---|---|---|
| Account ID | `discoveryAccountId` | `BRXDIS_ACCOUNT_ID` | `brxdis.accountId` | `brxdis:accountId` | Yes |
| Domain Key | `discoveryDomainKey` | `BRXDIS_DOMAIN_KEY` | `brxdis.domainKey` | `brxdis:domainKey` | Yes |
| API Key | `discoveryApiKeyEnvVar` (env var name) | `BRXDIS_API_KEY` | `brxdis.apiKey` | `brxdis:apiKey` | Yes |
| Auth Key | `discoveryAuthKeyEnvVar` (env var name) | `BRXDIS_AUTH_KEY` | `brxdis.authKey` | `brxdis:authKey` | No (enables v2 Pathways) |
| Environment | — | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `brxdis:environment` | No (default: `PRODUCTION`) |

`authKey` is only required when using the v2 Pathways recommendations API. When absent, the plugin falls back to v1 automatically — no error is thrown. `environment` accepts `PRODUCTION` or `STAGING`; the value drives API subdomain selection.

> **Note on `discoveryApiKeyEnvVar`:** this Channel Manager field stores the **name** of an environment variable (e.g. `PACIFICHOME_API_KEY`), not the key value itself. The actual secret must be present in the server's environment. This keeps secrets out of the CMS database while allowing per-channel key configuration.

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
brxdis.accountId=YOUR_ACCOUNT_ID
brxdis.domainKey=YOUR_DOMAIN_KEY
brxdis.apiKey=YOUR_API_KEY
brxdis.authKey=YOUR_AUTH_KEY
# brxdis.environment=PRODUCTION
# brxdis.pixelBaseUri=https://p-eu.brsrvr.com
```

Alternatively, pass them inline without a file:

```bash
cd demo && mvn -P cargo.run \
  -Dbrxdis.accountId=XXX \
  -Dbrxdis.domainKey=YYY \
  -Dbrxdis.apiKey=ZZZ \
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

## Method 3: JCR document field (last resort for secrets)

If no env var or system property is set, `DiscoveryConfigReader` falls back to the value stored in the `brxdis:discoveryConfig` JCR document.

Use this when:
- You cannot inject env vars at the infrastructure level.
- You need per-channel structural config (base URIs, page size, sort) — create one config doc per channel.

Drawbacks for secrets:
- Credentials are stored in the JCR repository (persisted in the filestore, exported by auto-export in dev, visible to CMS admins).
- Changes require a CMS document save. The updated value is cached at JVM lifetime — subsequent requests pick it up automatically, but there is no instant live update.

---

## Method 4: Channel Manager parameters (recommended for multi-channel)

Set `discoveryAccountId` and `discoveryDomainKey` directly on the HST mount via Channel Manager. These override the global env/sys/JCR values on a per-channel basis.

For API secrets, set `discoveryApiKeyEnvVar` to the name of a server-side env var (e.g. `PACIFICHOME_API_KEY`). The secret itself stays in the server environment.

```
Channel Manager → mount → channel properties:
  discoveryAccountId    = 6413
  discoveryDomainKey    = pacifichome
  discoveryApiKeyEnvVar = PACIFICHOME_API_KEY     ← env var name, not the key
  discoveryAuthKeyEnvVar = PACIFICHOME_AUTH_KEY   ← optional, for v2 Pathways
```

Server environment:
```bash
PACIFICHOME_API_KEY=your-actual-api-key
PACIFICHOME_AUTH_KEY=your-actual-auth-key
```

This is the recommended pattern for a deployment hosting multiple Discovery channels on one JVM.

---

## Security notes

- Never commit `local.properties` — it is listed in `demo/.gitignore`.
- Never check in HCM content YAML files with real credentials. The bootstrapped `discovery-config.yaml` uses empty strings deliberately.
- For production, prefer method 1 (global env vars) or method 4 (Channel Manager + per-channel env vars). Secrets managers (AWS Secrets Manager, HashiCorp Vault) can inject env vars at container startup without ever writing the value to disk.
- The JCR fallback is provided for convenience; it is not recommended for storing secrets in production environments that handle real customer data.
- Credential changes in Channel Manager or server env vars take effect on the next request — no CMS save or JVM restart required.
