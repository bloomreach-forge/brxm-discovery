# Overview

**brxm-discovery** integrates [Bloomreach Discovery](https://documentation.bloomreach.com/discovery/) with [brXM 17](https://documentation.bloomreach.com/content) — Bloomreach's content and page composition platform.

Discovery is the search and merchandising brain: it indexes your product catalog, powers keyword search, category browse, recommendation widgets, and autosuggest. brXM is the delivery layer: HST components render pages, editors manage content in Channel Manager, and the Page Model API feeds headless frontends.

> **Alpha — Early Development.** APIs, configuration schemas, HST component interfaces, and JCR node types are subject to change without notice between releases.

---

## What the plugin does

- Exposes HST components that call the Discovery API and expose typed models for FTL templates and the Page Model API.
- Provides CMS Open UI extensions (product picker, category picker, widget picker) backed by server-side REST proxy endpoints.
- Manages a server-side pixel service that fires impression/click events to `p.brsrvr.com` asynchronously.
- Proxies visual search image uploads so Discovery API keys are never exposed to the browser.
- Resolves credentials from environment variables, system properties, or a shared JCR config node — no per-request JCR reads in the critical path.

---

## Module map

| Module | Artifact | Runtime |
|---|---|---|
| `cms/` | `brxm-discovery-cms` | CMS webapp |
| `site/` | `brxm-discovery-site` | Site webapp |
| `commons/` | `brxm-discovery-commons` | Both (transitive) |
| `hcm-site/` | `brxm-discovery-hcm-site` | Bundled via transitive HCM site bootstrap |
| `demo/` | `brxm-discovery-demo` | Standalone reference project |

---

## Component summary

| Component class | Purpose |
|---|---|
| `DiscoverySearchGridComponent` | Keyword and visual search results (facets, pagination, sort, did-you-mean) |
| `DiscoveryCategoryGridComponent` | Category browse (facets, pagination, sort) |
| `DiscoverySearchInputComponent` | Search bar + autosuggest dropdown |
| `DiscoveryProductRecommendationComponent` | Product-keyed recommendations (PDP carousel) |
| `DiscoveryCategoryRecommendationComponent` | Category-keyed recommendations (PLP trending) |
| `DiscoveryGlobalRecommendationComponent` | Context-free global or personalised recommendations |
| `DiscoveryKeywordRecommendationComponent` | Query-driven recommendations |
| `DiscoveryProductDetailComponent` | Single-product detail page |
| `DiscoveryProductHighlightComponent` | Curated up-to-4 product showcase |
| `DiscoveryCategoryHighlightComponent` | Curated up-to-4 category navigation tiles |

---

## Documentation map

| Section | Guides |
|---|---|
| Getting started | [Quick Start](01-quick-start.md), [Installation](02-installation.md) |
| Configuration | [Discovery Config](10-discovery-config.md), [Channel Info](11-channel-info.md), [Credential Injection](12-credential-injection.md), [Plugin Setup](13-plugin-setup.md) |
| Components | [Search & Category](20-search-and-category.md), [Search Input & Autosuggest](21-search-input-autosuggest.md), [Recommendations](22-recommendations.md), [Product Detail & Highlights](23-product-detail-highlights.md), [Visual Search](24-visual-search.md) |
| CMS | [Product Picker](30-product-picker.md) |
| Integration | [SPA / React](40-spa-integration.md), [SEO URLs](41-seo.md) |
| Operations | [Pixel Tracking](50-pixel-tracking.md) |
| Reference | [Architecture](60-architecture.md), [Model Contract](61-model-contract.md), [REST Endpoints](62-rest-endpoints.md), [Release Notes](63-release-notes.md) |
| Help | [Troubleshooting](70-troubleshooting.md) |
