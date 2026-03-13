# brxm-discovery

Bloomreach Discovery integration plugin for brXM 16.7.0.

This repository keeps Discovery search, category, recommendations, autosuggest, and the CMS picker aligned around one shared config model. CMS and site both resolve Discovery settings from the same env/sys/JCR precedence, and CRISP now uses generic resource spaces whose base URIs come from that shared config instead of duplicated production/staging resolver definitions.

## What it provides

- `cms/`: product picker UI, picker REST endpoints, HCM bootstrap, platform CRISP definitions
- `site/`: HST components, CRISP-backed client, pixel service, bundled Freemarker templates
- `shared/`: config model, request builders, shared query models, CRISP config-backed resolver
- `hcm-site/`: site HCM bootstrap for bundled templates
- `demo/`: runnable reference project

## Prerequisites

| Requirement | Version / Notes |
|---|---|
| brXM | 16.7.0 |
| Java | 17 |
| Maven | 3.8+ |
| Runtime model | Separate CMS and site webapps |

## Quick start

1. Add `brxm-discovery-cms` to the CMS runtime and `brxm-discovery-site` to the site webapp. Those are the two addon entry points.
   brXM still requires one entry point per runtime; the addon no longer requires extra CRISP or `hcm-site` dependencies on top of that.
2. Enable the site CRISP broker in `hst-config.properties`:

```properties
crisp.broker.registerService=true
```

3. Provide credentials through env vars, system properties, or the global config node at `/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig`.
4. Wire the HST components into your page config.

The plugin bootstraps generic CRISP spaces automatically:
- `discoverySearchAPI`
- `discoveryPathwaysAPI`
- `discoveryAutosuggestAPI`

Their active base URIs come from shared Discovery config. `environment=STAGING` switches defaults to the staging endpoints when explicit `brxdis:*BaseUri` properties are not set. On the site side, the addon also registers `DiscoveryConfigProvider` in `HippoServiceRegistry` so the CRISP addon-module resolvers can read the same config without direct sibling Spring bean refs.

## Config summary

Credentials resolve in this order:

`env var -> system property -> JCR`

Supported keys:

- `BRXDIS_ACCOUNT_ID` / `brxdis.accountId` / `brxdis:accountId`
- `BRXDIS_DOMAIN_KEY` / `brxdis.domainKey` / `brxdis:domainKey`
- `BRXDIS_API_KEY` / `brxdis.apiKey` / `brxdis:apiKey`
- `BRXDIS_AUTH_KEY` / `brxdis.authKey` / `brxdis:authKey`
- `BRXDIS_ENVIRONMENT` / `brxdis.environment` / `brxdis:environment`

Structural settings live on the same global node:

- `brxdis:baseUri`
- `brxdis:pathwaysBaseUri`
- `brxdis:autosuggestBaseUri`
- `brxdis:defaultPageSize`
- `brxdis:defaultSort`

## Build

```bash
mvn clean test
```

```bash
mvn -pl shared,cms,site -am test
```

## Troubleshooting

- `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService`
  Usually means the site webapp is running an older addon snapshot. Reinstall the addon locally, rebuild the host project, then restart the site webapp.
- `No resource space for 'discoverySearchAPI'`
  Usually means the site webapp did not pick up the current CRISP resolver wiring from the updated addon snapshot. Rebuild and redeploy the site webapp.

Typical local snapshot refresh:

```bash
cd /path/to/brxm-discovery
mvn -DskipTests install

cd /path/to/your-project
mvn clean install
```

## User guides

For most projects, installation is:
- one dependency in the CMS runtime: `brxm-discovery-cms`
- one dependency in the site webapp: `brxm-discovery-site`
- one site property: `crisp.broker.registerService=true`

- [Quick Start](user-guides/00-quick-start.md)
- [Installation](user-guides/01-installation.md)
- [Discovery Configuration](user-guides/02-discovery-config.md)
- [Search and Category](user-guides/03-search-and-category.md)
- [Recommendations](user-guides/04-recommendations.md)
- [Product Picker](user-guides/05-product-picker.md)
- [Credential Injection](user-guides/06-credential-injection.md)
- [Autosuggest](user-guides/07-autosuggest.md)
- [React / SPA Integration](user-guides/08-react-spa-integration.md)
- [Pixel Tracking](user-guides/09-pixel-tracking.md)
