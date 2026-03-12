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

Then wire each artifact into the correct module:

| Artifact | Add to |
|---|---|
| `brxm-discovery-cms` | CMS webapp `pom.xml` |
| `brxm-discovery-site` | Site components JAR `pom.xml` |

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

For production, use environment variables (`BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`) or the global Discovery config node — see [06-credential-injection.md](06-credential-injection.md) for deployment patterns.

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

This is the minimum page composition for a working search page: one data-fetching component (`DiscoverySearchComponent`) and one view component (`DiscoveryProductGridComponent`) connected by a shared `label` / `connectTo` value.

### How `label` and `connectTo` work

- The data-fetching component writes its result to the request cache under a **label** (default `"default"`).
- View components on the same page read from the cache using **connectTo** to identify which label to read.
- Both default to `"default"` — a single search + grid page needs no explicit label configuration.
- Multiple search sections on the same page (e.g. a featured section + a main search section) use distinct label values to keep results separate.

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
          /search:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /search-bar:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoverySearchComponent
              hst:template: brxdis-search
              hst:parameternames: [label, pageSize]
              hst:parametervalues: [default, 12]
          /sidebar:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /facets:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryFacetComponent
              hst:template: brxdis-facets
              hst:parameternames: [connectTo]
              hst:parametervalues: [default]
          /content:
            jcr:primaryType: hst:containercomponent
            hst:xtype: hst.nomarkup
            /product-grid:
              jcr:primaryType: hst:containeritemcomponent
              hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryProductGridComponent
              hst:template: brxdis-product-grid
              hst:parameternames: [connectTo]
              hst:parametervalues: [default]
```

The `label=default` on `DiscoverySearchComponent` and `connectTo=default` on both view components are the pairing. The Discovery API is called exactly once per page render — the request cache deduplicates all subsequent reads.

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

**For the Page Model API** (headless delivery), call `http://localhost:8080/site/search?q=shirt` with `Accept: application/json` — the response will include `searchResult`, `products`, `facets`, and `pagination` in the JSON model.

---

## What's next

| Guide | Topic |
|---|---|
| [02-discovery-config.md](02-discovery-config.md) | Config document fields, defaults, and CRISP wiring |
| [03-search-and-category.md](03-search-and-category.md) | Full parameter reference for search and category components |
| [04-recommendations.md](04-recommendations.md) | Recommendation widgets, v2 Pathways API |
| [06-credential-injection.md](06-credential-injection.md) | Credential precedence and deployment patterns |
| [07-autosuggest.md](07-autosuggest.md) | Autosuggest dropdown, suggest-only mode |
