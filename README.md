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
| CRISP addons | Repository addon in CMS runtime, HST addon in site runtime |

## Quick start

1. Add `brxm-discovery-cms` to the CMS webapp and `brxm-discovery-site` to the site components/webapp.
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

Their active base URIs come from shared Discovery config. `environment=STAGING` switches defaults to the staging endpoints when explicit `brxdis:*BaseUri` properties are not set.

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

## User guides

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
