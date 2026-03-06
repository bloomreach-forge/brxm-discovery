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
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Site module** (add to your site webapp and components JAR):

```xml
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## Module Layout

```
brxm-discovery/                          (aggregator POM, packaging=pom)
├── cms/                                 (brxm-discovery-cms — jar)
│   └── JCR node types, editor template, picker daemon module, Open UI extension
├── site/                                (brxm-discovery-site — jar)
│   └── Domain model, services, CRISP integration, HST components
└── demo/                                (standalone Maven project)
    └── Full brXM project for local end-to-end testing
```

---

## Quick Start

**1. Add dependencies**

Add `brxm-discovery-cms` to your CMS webapp and `brxm-discovery-site` to your site webapp as shown in [Maven Coordinates](#maven-coordinates).

**2. Configure the CRISP resource spaces**

The plugin bootstraps three CRISP resource spaces automatically via `brxdis-crisp.yaml` (included in the CMS module's HCM config):
- `discoverySearchAPI` — `https://core.dxpapi.com` (search, category, widgets)
- `discoveryPathwaysAPI` — `https://pathways.dxpapi.com` (v2 Pathways recommendations)
- `discoveryAutosuggestAPI` — `https://suggest.dxpapi.com` (autosuggest / typeahead)

No manual CRISP config is required. See [02-discovery-config.md](user-guides/02-discovery-config.md) for override options.

**3. Create a `brxdis:discoveryConfig` document**

In the CMS, create a **Discovery Config** document under Content > Administration. This holds the per-channel Discovery credentials and API paths. Note its JCR path.

**4. Set the mount parameter**

In your HST virtual host mount config:

```yaml
hst:parameternames: [discoveryConfigPath]
hst:parametervalues: ['/content/documents/administration/discovery-config/discovery-config']
```

**5. Wire HST components**

The plugin provides three data-fetching components and three composable view components:

**Data-fetching components** (call the Discovery API, populate the request cache):

| Component class | Model keys set | Use |
|---|---|---|
| `…component.DiscoverySearchComponent` | `query`, `searchResult` | Search results page |
| `…component.DiscoveryCategoryComponent` | `categoryId`, `categoryResult` | Category browse page |
| `…component.DiscoveryRecommendationComponent` | `products`, `widgetId` | Recommendation widget |
| `…component.DiscoveryProductDetailComponent` | `product`, `similarProducts` | Product detail page |
| `…component.DiscoveryAutosuggestComponent` | `query`, `autosuggestResult` | Typeahead / autosuggest |

**View components** (read from the servlet-request cache, expose focused model slices — add as siblings on the same page):

| Component class | Model key set | Use |
|---|---|---|
| `…component.DiscoveryProductGridComponent` | `products` | Product list only |
| `…component.DiscoveryFacetComponent` | `facets` | Facet navigation only |
| `…component.DiscoveryPaginationComponent` | `pagination` | Pagination metadata only |

All components expose data via both `request.setAttribute()` (FTL) and `request.setModel()` (Page Model API / headless SPA).

**6. Use the plugin FTL templates (optional)**

The site JAR ships ready-to-use Freemarker templates on the classpath. Register them in your `templates.yaml`:

```yaml
/brxdis-search:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-search.ftl
/brxdis-product-grid:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-product-grid.ftl
/brxdis-facets:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-facets.ftl
/brxdis-pagination:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-pagination.ftl
/brxdis-category:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-category.ftl
/brxdis-recommendations:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-recommendations.ftl
/brxdis-product-detail:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-product-detail.ftl
/brxdis-autosuggest:
  jcr:primaryType: hst:template
  hst:renderpath: classpath:/freemarker/brxdis/brxdis-autosuggest.ftl
```

Each template injects scoped CSS via `<@hst.headContribution>` — no external stylesheet required. See [03-search-and-category.md](user-guides/03-search-and-category.md) for the composable wiring pattern.

---

## Credential Injection

Credentials and environment are resolved at runtime using the following precedence (highest to lowest):

| Priority | Mechanism | Keys |
|---|---|---|
| 1 (highest) | Environment variable | `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`, `BRXDIS_AUTH_KEY`, `BRXDIS_ENVIRONMENT` |
| 2 | JVM system property | `brxdis.accountId`, `brxdis.domainKey`, `brxdis.apiKey`, `brxdis.authKey`, `brxdis.environment` |
| 3 (lowest) | JCR field on the module config node | Edited via JCR console |

`AUTH_KEY` is required for v2 Pathways recommendations. When absent, the plugin falls back to the v1 API automatically. `ENVIRONMENT` controls the API subdomain (`PRODUCTION` → `core.dxpapi.com`; `STAGING` → `staging-core.dxpapi.com`).

Environment variables are the recommended approach for production. See [06-credential-injection.md](user-guides/06-credential-injection.md) for deployment patterns.

---

## Product Picker

The CMS ships a visual product picker as an Open UI document field extension. Editors search the Discovery catalog directly inside the brXM editor and store the selected product ID (PID) in any `String` document field — no full product data is persisted in brXM.

The picker is bootstrapped automatically via HCM:
- **Daemon module**: `/hippo:configuration/hippo:modules/brxm-discovery-picker`
- **JAX-RS endpoints**: `{cms}/ws/discovery/picker/search`, `.../items`, and `.../widgets`
- **Open UI extension node**: `/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker`

For wiring the picker into a document type, see [05-product-picker.md](user-guides/05-product-picker.md).

---

## Build Commands

```bash
# Compile all modules
mvn clean compile

# Run all tests (192 site + 13 cms = 205 total)
mvn clean test

# Run site module tests only
mvn clean test -pl site

# Run a single test class
mvn clean test -Dtest=DiscoverySearchComponentTest -pl site

# Run with coverage report
mvn clean test -Pcoverage
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
| 07 | [Autosuggest / Typeahead](user-guides/07-autosuggest.md) |
