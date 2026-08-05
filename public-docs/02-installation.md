[Documentation home](README.md) > Installation

# Installation

**ON THIS PAGE**
- [Prerequisites](#prerequisites)
- [Step 1 — Add the Maven repositories](#step-1--add-the-maven-repositories)
- [Step 2 — Add the plugin dependencies](#step-2--add-the-plugin-dependencies)
- [What each artifact provides](#what-each-artifact-provides)
- [What bootstraps automatically](#what-bootstraps-automatically)
- [Verifying the installation](#verifying-the-installation)

---

## Prerequisites

| Requirement | Version |
|---|---|
| brXM / Bloomreach Experience Manager | 17.0.0 |
| Java | 17 (LTS) |
| Maven | 3.8+ |
| Runtime model | separate CMS and site webapps (brXM's standard two-runtime deployment) |

---

## Step 1 — Add the Maven repositories

If they aren't already present in your project:

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

## Step 2 — Add the plugin dependencies

brXM loads CMS and site code in separate runtimes, so there is no single universal artifact — each runtime gets its own dependency.

In your root POM's `<dependencyManagement>`:

```xml
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

Then add each artifact to the matching module:

| Artifact | Add to |
|---|---|
| `brxm-discovery-cms` | Your CMS dependencies POM (the module that feeds `cms.war`) |
| `brxm-discovery-site` | Your site webapp |
| `brxm-discovery-site` | Your `site/components` module, if it exists, whenever it compiles custom Java against plugin classes |

You do not need to add `brxm-discovery-hcm-site` separately — it is pulled in automatically by `brxm-discovery-site`.

---

## What each artifact provides

### `brxm-discovery-cms`

- The `brxdis:discoveryConfig` JCR node type and its CMS editor template
- The picker daemon, which registers a REST endpoint at `{cms}/ws/discovery/picker`
- The Open UI picker and wizard extensions used by document editors
- The static picker UI assets served at `{cms}/discovery-picker/`

### `brxm-discovery-site`

- The runtime entry point: Spring beans, the addon module assembly, and bundled Freemarker templates
- All HST components (see [Component Parameters](04-component-parameters.md) for the full list)
- The transitive `brxm-discovery-hcm-site` bootstrap

---

## What bootstraps automatically

On first startup, the following is created without any manual configuration:

| What | JCR path |
|---|---|
| `brxdis` namespace and node types | `/hippo:namespaces/brxdis` |
| Picker daemon module | `/hippo:configuration/hippo:modules/brxm-discovery` |
| Open UI picker/wizard extensions | `/hippo:configuration/hippo:frontend/cms/ui-extensions/` |
| Bundled HST templates | `/hst:hst/hst:configurations/hst:default/hst:templates/brxdis-*` |

Because the templates register under `hst:default`, any site configuration that inherits from it receives them automatically — no `templates.yaml` entry is required unless you want to override a bundled template.

You still need to:

1. Provide Discovery credentials — see [Configuration](03-configuration.md).
2. Add the HST components you want to your page configuration — see [Component Parameters](04-component-parameters.md).
3. If you plan to use visual search, add an HST mount for the visual search pipeline — see [Recommendations & Visual Search](05-recommendations-and-visual-search.md#visual-search-mount-placement).

---

## Verifying the installation

After startup, check the CMS log for:

```
brxm-discovery: registered picker endpoint at /discovery/picker
brxm-discovery: Registered JCR observation listener on '/hippo:configuration'
```

Then confirm the picker endpoint responds (a JSON response, not a 404):

```
GET http://localhost:8080/cms/ws/discovery/picker/search
```

> This check assumes you've already completed step 1 above (Discovery credentials). If you hit this endpoint before configuring credentials, it will return a `404` with a body like `{"message":"Discovery accountId is required at ..."}` — that's expected, not a sign the plugin failed to install. Add credentials first (see [Configuration](03-configuration.md)), then retry.

> **[SCREENSHOT PLACEHOLDER: browser or API client (e.g. Postman) showing a successful JSON response from the picker `/search` endpoint, to visually confirm the installation worked.]**

If you see `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`, the site webapp is running against an older plugin build than what was installed — rebuild and redeploy the site webapp.

See [Troubleshooting](08-troubleshooting.md) for more installation issues.

---

**Previous:** [About](01-about.md) · **Next:** [Configuration](03-configuration.md)
