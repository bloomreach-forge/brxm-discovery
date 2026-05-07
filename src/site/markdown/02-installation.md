# Installation

## Prerequisites

| Requirement | Version |
|---|---|
| brXM / Hippo CMS | 17.0.0 |
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
  <version>${project.version}</version>
</dependency>
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
  <version>${project.version}</version>
</dependency>
```

The addon is designed around the two brXM runtimes:
- CMS runtime: add `brxm-discovery-cms`
- site webapp: add `brxm-discovery-site`
- site/components: also add `brxm-discovery-site` when that module exists and compiles custom code against addon APIs or extends `DiscoveryChannelInfo`

There is not a single universal addon artifact, because brXM loads CMS and site code in separate runtimes. The production-safe baseline is one addon dependency per runtime, plus the same site artifact in `site/components` for split-site projects so compile-time and runtime classpaths stay aligned.

You do not need to add `brxm-discovery-hcm-site` separately. It is pulled in by the addon entry points.

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
- `DiscoveryPickerModule` daemon - registers the picker REST endpoint at `{cms}/ws/discovery/picker`
- Open UI extension node `discoveryProductPicker` (pre-wired; you link your document fields to it)
- Static web resource: `{cms}/discovery-picker/index.html` (the picker iframe)

### Site webapp

Add `brxm-discovery-site` to your site `webapp` WAR:

```xml
<!-- In your site webapp -->
<dependency>
  <groupId>org.bloomreach.forge.discovery</groupId>
  <artifactId>brxm-discovery-site</artifactId>
</dependency>
```

This dependency is the runtime entry point. It loads the addon assembly, Spring beans, bundled templates, and the transitive `brxm-discovery-hcm-site` bootstrap.

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

This JAR provides its core site wiring through an addon module assembly (`META-INF/hst-assembly/addon/brxm-discovery/brxm-discovery-site.xml`), discovered automatically by brXM's `ModuleDescriptorUtils` across all JARs:

| Bean | Role |
|---|---|
| `HstDiscoveryService` | HST façade: config, cookie/URL extraction, caching, Discovery API calls |
| `DiscoveryClientImpl` | HTTP/2 client calls (virtual threads); thin coordinator for search, autosuggest, and recommendations |
| `CachingDiscoveryConfigProvider` | JVM-lifetime config cache; implements `EventListener` to self-invalidate on CMS config saves |
| `DiscoveryPixelServiceImpl` | Fire-and-forget pixel event calls on an injected executor |

**HST components** (reference by fully-qualified class name in your HST config):
- `DiscoverySearchGridComponent` - keyword search results (includes facets, pagination, sort, did-you-mean)
- `DiscoveryCategoryGridComponent` - category browse (includes facets, pagination, sort)
- `DiscoverySearchInputComponent` - standalone search bar with autosuggest
- `DiscoveryProductRecommendationComponent` - product recommendation widgets (v1 and v2 Pathways API)
- `DiscoveryCategoryRecommendationComponent` - category recommendation widgets
- `DiscoveryGlobalRecommendationComponent` - global / trending recommendation widgets
- `DiscoveryProductDetailComponent` - product detail page
- `DiscoveryProductHighlightComponent` - up to 4 curated product slots
- `DiscoveryCategoryHighlightComponent` - up to 4 curated category tiles with optional product previews

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
| Bundled plugin FTL templates | `/hst:hst/hst:configurations/hst:default/hst:templates/brxdis-*` |

The template nodes are registered under `hst:default`, so every site configuration that inherits from it gets them automatically. No per-project `templates.yaml` is required unless you want to override the bundled templates.

You still need to:
1. Set credentials via env vars / system properties, via the global JCR config node, or via optional channel-level overrides in `hst:channelinfo` (see [12-credential-injection.md](12-credential-injection.md))
2. Wire HST components into your HST page configuration (see [01-quick-start.md](01-quick-start.md))
3. **(If using Visual Search)** Add an HST mount pointing to `BrxdisVisualSearchPipeline` nested under your channel mount — see [24-visual-search.md](24-visual-search.md#mount-placement).

---

## Visual search delivery endpoints

The plugin ships a server-side proxy for visual search via `BrxdisVisualSearchPipeline` — an HST named pipeline. These endpoints are served through the normal HST request chain; no additional servlet or `web.xml` configuration is required.

To enable them, add an `hst:mount` entry pointing at `BrxdisVisualSearchPipeline` in your host configuration, nested under the channel mount whose `DiscoveryChannelInfo` holds the Discovery credentials you want used. See [24-visual-search.md](24-visual-search.md#mount-placement) for the YAML example and mount placement guidance.

> **Troubleshooting - templates not found:** If your site's HST configuration chain does not inherit from `hst:default` (e.g. a project using a deep custom inheritance hierarchy that bypasses `hst:default`), templates will not be resolved automatically. In that case, add the missing `brxdis-*` entries to your own site's `hst:templates` YAML pointing at `classpath:/freemarker/brxdis/brxdis-*.ftl`.

---

## Verify installation

After startup, check the CMS logs for:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

And navigate to `http://localhost:8080/cms/ws/discovery/picker/search` - a JSON response (not a 404) confirms the endpoint is live.

## Troubleshooting

### `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`

This usually means the site webapp is running an older addon snapshot or was not redeployed after the addon changed. Reinstall the addon locally, rebuild the host project, and restart the site webapp.

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

Then open `http://localhost:8080/site/search?q=shirt` - you should see a product grid populated from the Discovery API (once credentials are configured).
