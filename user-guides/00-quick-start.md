# Quick Start: Zero to Functional Search Page

This guide walks through every step from an empty brXM 16.7.0 project to a working search results page in the browser.

> **Fastest alternative**: The `demo/` directory at the root of this repository is a fully pre-wired brXM project. Run `cd demo && mvn clean install && mvn -P cargo.run cargo:run` to see the plugin working before touching your own project.

---

## Step 1 — Add dependencies

In your root `pom.xml` `<dependencyManagement>`:

```xml
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

Then wire each artifact into the correct runtime:

| Artifact | Add to |
|---|---|
| `brxm-discovery-cms` | CMS dependencies / CMS webapp |
| `brxm-discovery-site` | Site webapp |
| `brxm-discovery-site` | Site/components if that module exists and compiles custom code against addon APIs |

`brxm-discovery-site` already brings the plugin's HCM site bootstrap transitively. For production installs, keep it on the site webapp so the addon assembly and beans load at runtime. If your project has a separate `site/components` module, add the same artifact there as well when you compile custom Java against addon APIs, component types, or `DiscoveryChannelInfo`.

Bloomreach Maven repositories (add to `pom.xml` or `settings.xml` if not already present):

```xml
<repository>
  <id>bloomreach</id>
  <url>https://maven.bloomreach.com/maven2/</url>
</repository>
<repository>
  <id>bloomreach-enterprise</id>
  <url>https://maven.bloomreach.com/maven2-enterprise/</url>
</repository>
```

---

## Step 2 — Enable the CRISP broker

In your **site** webapp `hst-config.properties`:

```properties
crisp.broker.registerService = true
```

This registers `ResourceServiceBroker` into `HippoServiceRegistry` at startup. Without it, Discovery API calls will fail at request time.
The plugin also registers `DiscoveryConfigProvider` in `HippoServiceRegistry` so the CRISP addon-module resolvers can read the same Discovery config as the site service layer.

The plugin bootstraps the generic CRISP resource spaces (`discoverySearchAPI`, `discoveryPathwaysAPI`, `discoveryAutosuggestAPI`) automatically. Their active base URIs come from the shared Discovery config, so you do not need separate production and staging CRISP definitions in your project.

---

## Step 3 — Configure credentials

For local development, the quickest path is JVM system properties passed to Cargo or your app server:

```bash
mvn -P cargo.run cargo:run \
  -Dbrxdis.accountId=YOUR_ACCOUNT_ID \
  -Dbrxdis.domainKey=YOUR_DOMAIN_KEY \
  -Dbrxdis.apiKey=YOUR_API_KEY
```

For production, use environment variables (`BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`) or the global Discovery config node. If different channels need different account/domain values or env-var names for secrets, use the channel-level `DiscoveryChannelInfo` fields in `hst:channelinfo` — see [06-credential-injection.md](06-credential-injection.md) for deployment patterns.

---

## Step 4 — Create the Discovery config node (optional)

Skip this step if you supplied all credentials via env vars / system properties in Step 3 — the plugin will run without a JCR node.

To store credentials or structural config in the CMS, create the global config node in your HCM config (place in your application or development module):

```yaml
definitions:
  config:
    /hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig:
      jcr:primaryType: brxdis:discoveryConfig
      brxdis:accountId: 'your-account-id'
      brxdis:domainKey: 'your-domain-key'
      brxdis:apiKey: ''
      brxdis:defaultPageSize: 12
```

Leave `brxdis:apiKey` / `brxdis:authKey` blank and inject secrets via env vars. The node path is fixed — all channels share it. No mount parameter is required.

---

## Step 5 — Wire a search page

`DiscoveryResultsComponent` is the single component for search results pages — it handles data fetching, facets, pagination, and sort all in one. No additional view components are needed.

### `pages.yaml` (workspace page composition)

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:workspace/hst:pages:
      /search-page:
        jcr:primaryType: hst:component
        hst:referencecomponent: hst:abstractpages/base
        /main:
          jcr:primaryType: hst:component
          hst:template: search-layout
          /content:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /search-results:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryResultsComponent
              hst:template: brxdis-results
              hst:parameternames: [dataSource, pageSize]
              hst:parametervalues: [search, 12]
```

That's it — one component, one template. The bundled `brxdis-results` template renders the search form, facet sidebar, product grid, and pagination in one pass.

### Optional: add a standalone search bar to the header

Place `DiscoverySearchInputComponent` in any zone (header, sidebar) to provide a search input that submits to your results page:

```yaml
          /header:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /search-bar:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchInputComponent
              hst:template: brxdis-search-input
              hst:parameternames: [resultsPage, placeholder]
              hst:parametervalues: [/search, 'Search products...']
```

The search bar is independent — it submits to the `resultsPage` path where `DiscoveryResultsComponent` runs the actual search.

### `sitemap.yaml`

```yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:sitemap:
      /search:
        jcr:primaryType: hst:sitemapitem
        hst:componentconfigurationid: hst:pages/search-page
```

---

## Step 6 — Verify

Start the site webapp and open:

```
http://localhost:8080/site/search?q=shirt
```

**Expected:** A page with a search form, facet sidebar, and product grid populated from the Discovery API.

**Log lines to look for on startup:**

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

**If the product grid is empty but no error is shown**, check:
1. Credentials are set — add `-Dbrxdis.accountId=... -Dbrxdis.domainKey=... -Dbrxdis.apiKey=...` to your startup command.
2. If using JCR-based config: verify the node exists at `/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig`.

**If you see a `ConfigurationException: CRISP ResourceServiceBroker not found`**, `crisp.broker.registerService = true` is missing from the **site** webapp `hst-config.properties`.

**If you see `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`**, rebuild and redeploy the site webapp against the current addon snapshot.

**If you see `No resource space for 'discoverySearchAPI'`**, the site webapp is still using stale CRISP resolver wiring from an older addon snapshot. Reinstall the addon locally, rebuild the host project, and restart the site webapp.

**For the Page Model API** (headless delivery), call `http://localhost:8080/site/search?q=shirt` with `Accept: application/json` — the response will include `products`, `facets`, `pagination`, `facetUrls`, `pageUrls`, and `sortUrl` in the JSON model.

---

## What's next

| Guide | Topic |
|---|---|
| [02-discovery-config.md](02-discovery-config.md) | Config document fields, defaults, and CRISP wiring |
| [03-search-and-category.md](03-search-and-category.md) | Full parameter reference for search and category components |
| [04-recommendations.md](04-recommendations.md) | Recommendation widgets, v2 Pathways API |
| [06-credential-injection.md](06-credential-injection.md) | Credential precedence and deployment patterns |
| [07-autosuggest.md](07-autosuggest.md) | Autosuggest dropdown, suggest-only mode |
