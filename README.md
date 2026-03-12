# brxm-discovery

**Bloomreach Discovery integration plugin for brXM (Hippo CMS) 16.6.5**

Bloomreach Discovery acts as the search brain — indexing the product catalog (fed by your external commerce system via connectors), resolving queries, surfacing facets, and powering recommendation widgets. brXM acts as the content brain — managing editorial content and channel configuration, storing product references by ID. This plugin is the glue: it exposes CRISP-backed HST components for search, category browse, and recommendations, and provides editors with a visual product picker inside the CMS editor — all without syncing any product catalog data into brXM.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Maven Coordinates](#maven-coordinates)
- [Module Layout](#module-layout)
- [Quick Start](#quick-start)
- [Credential Injection](#credential-injection)
- [Product Picker](#product-picker)
- [Build Commands](#build-commands)
- [User Guides](#user-guides)

---

## Prerequisites

| Requirement | Version / Notes |
|---|---|
| brXM (Hippo CMS) | 16.6.5 |
| Java | 17 (LTS) |
| Maven | 3.8+ (no wrapper; use `mvn` directly) |
| CRISP addon | Must be enabled in the site webapp |
| Bloomreach Discovery account | Account ID, Domain Key, API Key, and Auth Key (v2 Pathways) |

---

## Maven Coordinates

All platform dependencies (`hst-api`, `crisp-api`, `hippo-repository-api`, etc.) are `provided` scope — the host project supplies them at runtime.

**Bloomreach Maven repositories** (add to your `pom.xml` or `settings.xml` if not already present):

```xml
<repositories>
  <repository>
    <id>bloomreach</id>
    <url>https://maven.bloomreach.com/maven2/</url>
  </repository>
  <repository>
    <id>bloomreach-enterprise</id>
    <url>https://maven.bloomreach.com/maven2-enterprise/</url>
  </repository>
</repositories>
```

**CMS module** (add to your CMS webapp dependencies):

```xml
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-cms</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Site module** (add to your site webapp and components JAR):

```xml
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

## Module Layout

```
brxm-discovery/                          (aggregator POM, packaging=pom)
├── cms/                                 (brxm-discovery-cms — jar)
│   └── JCR node types, editor template, picker daemon module, Open UI extension
├── site/                                (brxm-discovery-site — jar)
│   └── Domain model, services, CRISP integration, HST components, bundled FTL templates
└── demo/                                (standalone Maven project)
    └── Full brXM project for local end-to-end testing
```

---

## Quick Start

**1. Add dependencies**

Add `brxm-discovery-cms` to your CMS webapp and `brxm-discovery-site` to your site webapp as shown in [Maven Coordinates](#maven-coordinates).

**2. Configure the CRISP broker**

Enable the CRISP broker in your **site** webapp `hst-config.properties`:

```properties
crisp.broker.registerService = true
```

This registers `ResourceServiceBroker` into `HippoServiceRegistry`, making it accessible to the plugin's service beans at request time.

The three CRISP resource spaces (`discoverySearchAPI`, `discoveryPathwaysAPI`, `discoveryAutosuggestAPI`) are bootstrapped automatically by the plugin — no manual CRISP configuration is required.

**3. Create the global config node (optional)**

Create a `brxdis:discoveryConfig` node at the fixed global path — all channels share it. Credentials can alternatively be supplied entirely via env vars / sys props.

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'
      brxdis:defaultPageSize: 12
```

See [06-credential-injection.md](user-guides/06-credential-injection.md) for the full property reference and deployment patterns.

**4. Wire HST components**

See [00-quick-start.md](user-guides/00-quick-start.md) for a step-by-step walkthrough from a blank brXM project to first search results in the browser — including catalog YAML, sitemap, page composition, and bean scanning setup.

**6. Use the bundled FTL templates (optional)**

All `brxdis-*` templates are bundled in `brxm-discovery-site` and auto-registered under `hst:default` by the plugin's HCM config — no manual `templates.yaml` entries required. Each template injects scoped CSS via `<@hst.headContribution>` — no external stylesheet required. See [03-search-and-category.md](user-guides/03-search-and-category.md) for the composable wiring pattern.

---

## Credential Injection

All credentials are resolved from a single global JCR config node at `/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig`. No per-channel configuration is required.

**Account ID, Domain Key, API Key, Auth Key** — all resolved with the same three-tier chain:

| Priority | Mechanism |
|---|---|
| 1 (highest) | Environment variable — `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY` |
| 2 | JVM system property — `brxdis.accountId`, `brxdis.domainKey`, `brxdis.apiKey`, `brxdis.authKey` |
| 3 (lowest) | JCR global node field — `brxdis:accountId`, `brxdis:domainKey`, `brxdis:apiKey`, `brxdis:authKey` |

`AUTH_KEY` is only required for v2 Pathways recommendations; when absent the plugin uses the v1 API automatically. `ENVIRONMENT` controls the API subdomain (`PRODUCTION` → `core.dxpapi.com`; `STAGING` → `staging-core.dxpapi.com`).

See [06-credential-injection.md](user-guides/06-credential-injection.md) for deployment patterns.

---

## Product Picker

The CMS ships a visual product picker as an Open UI document field extension. Editors search the Discovery catalog directly inside the brXM editor and store the selected product ID (PID) in any `String` document field — no full product data is persisted in brXM.

The picker is bootstrapped automatically via HCM:
- **Daemon module**: `/hippo:configuration/hippo:modules/brxm-discovery`
- **JAX-RS endpoints**: `{cms}/ws/discovery/picker/search`, `.../items`, `.../categories`, and `.../widgets`
- **Open UI extension node**: `/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker`

For wiring the picker into a document type, see [05-product-picker.md](user-guides/05-product-picker.md).

---

## Build Commands

```bash
# Compile all modules
mvn clean compile

# Run all tests
mvn clean test

# Run site module tests only
mvn clean test -pl site

# Run a single test class
mvn clean test -Dtest=DiscoverySearchComponentTest -pl site
```

---

## User Guides

| # | Guide |
|---|---|
| 01 | [Installation & Maven Setup](user-guides/01-installation.md) |
| 02 | [Discovery Config & CRISP Resource Space](user-guides/02-discovery-config.md) |
| 03 | [Search & Category Pages](user-guides/03-search-and-category.md) |
| 04 | [Recommendation Widgets](user-guides/04-recommendations.md) |
| 05 | [Product Picker](user-guides/05-product-picker.md) |
| 06 | [Credential Injection](user-guides/06-credential-injection.md) |
| 07 | [Autosuggest / Search Bar](user-guides/07-autosuggest.md) |
| 08 | [React SPA Integration](user-guides/08-react-spa-integration.md) |
| 09 | [Pixel Tracking](user-guides/09-pixel-tracking.md) |
