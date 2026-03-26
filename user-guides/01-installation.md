# Installation

## Prerequisites

| Requirement | Version |
|---|---|
| brXM / Hippo CMS | 16.7.0 |
| Java | 17 (LTS) |
| Maven | 3.8+ |
| Runtime model | separate CMS and site webapps |

---

## Add the plugin JARs

In your project's dependency management (or directly in the relevant `pom.xml` files):

```xml
<!-- In your root POM dependencyManagement -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-cms</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The addon is designed around the two brXM runtimes:
- CMS runtime: add `brxm-discovery-cms`
- site webapp: add `brxm-discovery-site`
- site/components: also add `brxm-discovery-site` when that module exists and compiles custom code against addon APIs or extends `DiscoveryChannelInfo`

There is not a single universal addon artifact, because brXM loads CMS and site code in separate runtimes. The production-safe baseline is one addon dependency per runtime, plus the same site artifact in `site/components` for split-site projects so compile-time and runtime classpaths stay aligned.

You do not need to add `brxm-discovery-hcm-site` or the CRISP addon artifacts separately. They are pulled in by the addon entry points.

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

Add `brxm-discovery-site` to your site `webapp` WAR:

```xml
<!-- In your site webapp -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
</dependency>
```

This dependency is the runtime entry point. It loads the addon assembly, Spring beans, CRISP resolvers, bundled templates, and the transitive `brxm-discovery-hcm-site` bootstrap.

### Site/components module

If your project has a separate `site/components` JAR, add `brxm-discovery-site` there as well whenever that module compiles custom Java against addon APIs, HST component classes, or `DiscoveryChannelInfo`.

This is the safest production-ready pattern for split site projects because it keeps compile-time and runtime classpaths aligned.

```xml
<!-- In your site/components module -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
</dependency>
```

This JAR provides its core site wiring through the addon assembly loaded from `META-INF/hst-assembly/addon/module.xml` and `META-INF/hst-assembly/addon/brxm-discovery-site.xml`:

| Bean | Role |
|---|---|
| `HstDiscoveryService` | HST façade: config, cookie/URL extraction, caching, Discovery API calls |
| `DiscoveryClientImpl` | CRISP broker calls; builds all API request paths |
| `CachingDiscoveryConfigProvider` | JVM-lifetime config cache, JCR-observation-invalidated |
| `DiscoveryConfigJcrListener` | Invalidates config cache on CMS node changes (no restart needed) |
| `DiscoveryConfigResolver` | Two-tier config resolution (env/sys/JCR + coded defaults) |
| `ConfigBackedDiscoveryResourceResolver` | CRISP resolver that reads active base URIs from shared Discovery config |
| `DiscoveryConfigProviderServiceRegistration` | Registers `DiscoveryConfigProvider` in `HippoServiceRegistry` for CRISP resolver access |
| `DiscoveryPixelServiceImpl` | Fire-and-forget pixel event calls on an injected executor |

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

On first startup, HCM applies the following from `brxm-discovery-cms.jar` and `brxm-discovery-site.jar`:

| What | JCR path |
|---|---|
| `brxdis` namespace + CND | `/hippo:namespaces/brxdis` |
| `brxdis:discoveryConfig` document type | `/hippo:namespaces/brxdis/discoveryConfig` |
| Picker daemon module | `/hippo:configuration/hippo:modules/brxm-discovery` |
| `discoveryProductPicker` Open UI extension | `/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker` |
| CRISP resource spaces (generic search/pathways/autosuggest/pixel) | `/hippo:configuration/hippo:modules/crispregistry/…` |
| Bundled plugin FTL templates | `/hst:hst/hst:configurations/hst:default/hst:templates/brxdis-*` |

The template nodes are registered under `hst:default`, so every site configuration that inherits from it gets them automatically. No per-project `templates.yaml` is required unless you want to override the bundled templates.

You still need to:
1. **Enable the CRISP broker** in your **site** webapp `hst-config.properties`:
   ```properties
   crisp.broker.registerService = true
   ```
   This causes `ResourceBrokerServiceRegistrationBean` to register the `ResourceServiceBroker` in
   `HippoServiceRegistry`, making it accessible to the plugin's service beans at request time.
   Without this setting, all Discovery API calls will throw `ConfigurationException` with a clear message.
   The plugin also registers `DiscoveryConfigProvider` into `HippoServiceRegistry` on the site side so the CRISP addon-module resolvers can reuse shared Discovery config without direct Spring refs across addon modules.
2. Set credentials via env vars / system properties, via the global JCR config node, or via optional channel-level overrides in `hst:channelinfo` (see [06-credential-injection.md](06-credential-injection.md))
3. Wire HST components into your HST page configuration (see [00-quick-start.md](00-quick-start.md))

> **Troubleshooting — templates not found:** If your site's HST configuration chain does not inherit from `hst:default` (e.g. a project using a deep custom inheritance hierarchy that bypasses `hst:default`), templates will not be resolved automatically. In that case, add the missing `brxdis-*` entries to your own site's `hst:templates` YAML pointing at `classpath:/freemarker/brxdis/brxdis-*.ftl`.

---

## Verify installation

After startup, check the CMS logs for:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

And navigate to `http://localhost:8080/cms/ws/discovery/picker/search` — a JSON response (not a 404) confirms the endpoint is live.

## Troubleshooting

### `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`

This usually means the site webapp is running an older addon snapshot or was not redeployed after the addon changed. Reinstall the addon locally, rebuild the host project, and restart the site webapp.

### `No resource space for 'discoverySearchAPI'`

This usually means the site webapp is still using stale CRISP resolver wiring from an older addon snapshot. Rebuild and redeploy the site webapp so the current resolver assembly is loaded.

### Local snapshot refresh sequence

```bash
cd /path/to/brxm-discovery
mvn -DskipTests install

cd /path/to/your-project
mvn clean install
```

If your project has a separate `site/components` module, keep `brxm-discovery-site` there as well so custom code and typed channel info interfaces compile against the same addon version as the site runtime.

---

## Fastest path: run the demo project

The `demo/` directory at the root of this repository is a complete, self-contained brXM project with all plugin components pre-wired. It is the fastest way to see the plugin running end-to-end on your local machine before setting up your own project:

```bash
cd demo
mvn clean install
mvn -P cargo.run cargo:run
```

Then open `http://localhost:8080/site/search?q=shirt` — you should see a product grid populated from the Discovery API (once credentials are configured).
