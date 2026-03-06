# Credential Injection

This guide covers every supported method for injecting Discovery API credentials into the plugin without hardcoding them in the JCR repository.

## Resolution precedence

`DiscoveryConfigReader` resolves each credential in this order, stopping at the first non-blank value:

```
BRXDIS_ACCOUNT_ID (env var)
  → brxdis.accountId (system property)
    → brxdis:accountId (JCR module config node)
```

| Priority | Source | When to use |
|---|---|---|
| 1 | Environment variable | Containers, Kubernetes, CI/CD pipelines |
| 2 | JVM system property | Local development, shell scripts |
| 3 | JCR module config node | CMS-editable fallback |

---

## Credential reference

| Credential | Env var | System property | JCR property | Required |
|---|---|---|---|---|
| Account ID | `BRXDIS_ACCOUNT_ID` | `brxdis.accountId` | `brxdis:accountId` | Yes |
| Domain Key | `BRXDIS_DOMAIN_KEY` | `brxdis.domainKey` | `brxdis:domainKey` | Yes |
| API Key | `BRXDIS_API_KEY` | `brxdis.apiKey` | `brxdis:apiKey` | Yes |
| Auth Key | `BRXDIS_AUTH_KEY` | `brxdis.authKey` | `brxdis:authKey` | No (enables v2 Pathways) |
| Environment | `BRXDIS_ENVIRONMENT` | `brxdis.environment` | `brxdis:environment` | No (default: `PRODUCTION`) |

`authKey` is only required when using the v2 Pathways recommendations API. When absent, the plugin falls back to v1 automatically — no error is thrown. `environment` accepts `PRODUCTION` or `STAGING`; the value drives API subdomain selection.

---

## Method 1: Environment variables (recommended for production)

Set the env vars before starting the JVM. In Docker:

```dockerfile
ENV BRXDIS_ACCOUNT_ID=your_account_id
ENV BRXDIS_DOMAIN_KEY=your_domain_key
ENV BRXDIS_API_KEY=your_api_key
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

## Method 3: JCR document field (last resort)

If no env var or system property is set, `DiscoveryConfigReader` falls back to the value stored in the `brxdis:discoveryConfig` JCR document.

Use this when:
- You cannot inject env vars at the infrastructure level.
- You need per-channel credentials (create one config doc per channel; the `discoveryConfigPath` mount param selects which one is used).

Drawbacks:
- Credentials are stored in the JCR repository (persisted in the filestore, exported by auto-export in dev, visible to CMS admins).
- Changes require a CMS edit + publish rather than a config change.

---

## Security notes

- Never commit `local.properties` — it is listed in `demo/.gitignore`.
- Never check in HCM content YAML files with real credentials. The bootstrapped `discovery-config.yaml` uses empty strings deliberately.
- For production, prefer method 1 (env vars). Secrets managers (AWS Secrets Manager, HashiCorp Vault) can inject env vars at container startup without ever writing the value to disk.
- The JCR fallback is provided for convenience; it is not recommended for production environments that handle real customer data.
