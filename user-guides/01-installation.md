# Installation

## Prerequisites

| Requirement | Version |
|---|---|
| brXM / Hippo CMS | 16.6.5 |
| Java | 17 (LTS) |
| Maven | 3.8+ |
| CRISP addon | enabled in site WAR |

The CRISP addon (`hippo-addon-crisp-repository` + `hippo-addon-crisp-hst`) must be on the classpath of your site webapp. It ships with the standard `hippo-package-site-dependencies` BOM.

---

## Add the plugin JARs

In your project's dependency management (or directly in the relevant `pom.xml` files):

```xml
<!-- In your root POM dependencyManagement -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-cms</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### CMS dependencies module

Add `brxm-discovery-cms` to your CMS dependencies POM (the `pom`-packaged module that feeds your `cms.war`):

```xml
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-cms</artifactId>
</dependency>
```

This JAR provides:
- `brxdis:discoveryConfig` JCR node type and CMS editor template
- `DiscoveryPickerModule` daemon — registers the picker REST endpoint at `{cms}/ws/discovery/picker`
- Open UI extension node `discoveryProductPicker` (pre-wired; you link your document fields to it)
- Static web resource: `{cms}/discovery-picker/index.html` (the picker iframe)
- CRISP resource space bootstrap via `brxdis-crisp.yaml` (HCM config)

### Site webapp

Add `brxm-discovery-site` to your site `components` JAR and `webapp` WAR:

```xml
<!-- In your site components JAR -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
</dependency>
```

This JAR provides Spring beans auto-registered via `META-INF/hst-assembly/overrides/brxm-discovery-site.xml`:

| Bean | Role |
|---|---|
| `HstDiscoveryService` | HST façade: config, cookie/URL extraction, caching, Discovery API calls |
| `DiscoveryClientImpl` | CRISP broker calls; builds all API request paths |
| `CachingDiscoveryConfigProvider` | JVM-lifetime config cache, JCR-observation-invalidated |
| `DiscoveryConfigJcrListener` | Invalidates config cache on CMS node changes (no restart needed) |
| `DiscoveryConfigResolver` | Two-tier config resolution (env/sys/JCR + coded defaults) |
| `DiscoveryWidgetServiceImpl` | Widget listing from merchant widgets API (5-min in-process cache) |
| `DiscoveryPixelServiceImpl` | Fire-and-forget pixel event calls on a dedicated bounded thread pool |

**HST components** (reference by fully-qualified class name in your HST config):
- Data-fetching: `DiscoverySearchComponent`, `DiscoveryCategoryComponent`, `DiscoveryRecommendationComponent`, `DiscoveryProductDetailComponent`, `DiscoveryProductHighlightComponent`, `DiscoveryCategoryHighlightComponent`
- Composable view: `DiscoveryProductGridComponent`, `DiscoveryFacetComponent`

All components expose data via `request.setModel()` (Page Model API / headless) and `request.setAttribute()` (FTL).

---

## Maven repositories

If not already in your project:

```xml
<repository>
  <id>bloomreach-maven2</id>
  <url>https://maven.bloomreach.com/maven2/</url>
</repository>
<repository>
  <id>bloomreach-maven2-enterprise</id>
  <url>https://maven.bloomreach.com/maven2-enterprise/</url>
</repository>
```

---

## What bootstraps automatically

On first startup, HCM applies the following from `brxm-discovery-cms.jar`:

| What | JCR path |
|---|---|
| `brxdis` namespace + CND | `/hippo:namespaces/brxdis` |
| `brxdis:discoveryConfig` document type | `/hippo:namespaces/brxdis/discoveryConfig` |
| Picker daemon module | `/hippo:configuration/hippo:modules/brxm-discovery-picker` |
| `discoveryProductPicker` Open UI extension | `/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker` |
| CRISP resource spaces (all 3) | `/hippo:configuration/hippo:modules/crispregistry/…` |

You still need to:
1. **Enable the CRISP broker** in your site `hst-config.properties`:
   ```properties
   crisp.broker.registerService = true
   ```
   This causes `ResourceBrokerServiceRegistrationBean` to register the `ResourceServiceBroker` in
   `HippoServiceRegistry`, making it accessible to the plugin's service beans at request time.
   Without it, all Discovery API calls will throw `NullPointerException`.
2. Set credentials via env vars / system properties, or via the JCR config document (see [06-credential-injection.md](06-credential-injection.md))
3. Wire the `discoveryConfigPath` mount parameter and HST components (see [03-search-and-category.md](03-search-and-category.md))

---

## Verify installation

After startup, check the CMS logs for:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

And navigate to `http://localhost:8080/cms/ws/discovery/picker/search?configPath=…` — a `400 Bad Request` (not a 404) confirms the endpoint is live.
