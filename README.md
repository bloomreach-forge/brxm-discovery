# brxm-discovery

> **Alpha - Early Development**
> APIs, configuration schemas, and component interfaces are subject to change without notice between releases.

Bloomreach Discovery integration plugin for brXM 17.0.0.

Integrates Discovery search, category browse, recommendations, autosuggest, and the CMS picker with brXM. The site delivers all Discovery API calls via a `java.net.http.HttpClient` with virtual threads and HTTP/2.

## Prerequisites

| Requirement | Version |
|---|---|
| brXM | 17.0.0 |
| Java | 17 |
| Maven | 3.8+ |

## Build

```bash
mvn clean test
mvn clean test -pl site         # site module only
mvn clean test -Dtest=FooTest -pl site
```

## Documentation

### Getting Started

- [Overview](user-guides/00-overview.md)
- [Quick Start](user-guides/01-quick-start.md)
- [Installation](user-guides/02-installation.md)

### Configuration

- [Discovery Configuration](user-guides/10-discovery-config.md) — global JCR config node, credential precedence, API endpoints
- [Channel Info Reference](user-guides/11-channel-info.md) — all 13 `DiscoveryChannelInfo` fields: credentials, schema, pixel, visual search
- [Credential Injection](user-guides/12-credential-injection.md) — env-var indirection, multi-tenant overrides
- [Plugin Setup](user-guides/13-plugin-setup.md) — post-install checklist, defaults, sort options

### Components

- [Search and Category](user-guides/20-search-and-category.md) — `DiscoverySearchGridComponent`, `DiscoveryCategoryGridComponent`
- [Search Input and Autosuggest](user-guides/21-search-input-autosuggest.md) — `DiscoverySearchInputComponent`, typeahead panel
- [Recommendations](user-guides/22-recommendations.md) — four recommendation components + document types
- [Product Detail and Highlights](user-guides/23-product-detail-highlights.md) — `DiscoveryProductDetailComponent`, product and category highlights
- [Visual Search](user-guides/24-visual-search.md) — image-based search, upload proxy, channel setup

### CMS

- [Product Picker](user-guides/30-product-picker.md) — CMS picker UI, REST endpoints, preview fields

### Integration

- [SPA / React Integration](user-guides/40-spa-integration.md) — Page Model API contract, TypeScript types, full wiring example
- [SEO-Friendly URLs](user-guides/41-seo.md) — URL shapes, slugification, sitemap entries

### Operations

- [Pixel Tracking](user-guides/50-pixel-tracking.md) — event types, kill switches, consent gating

### Reference

- [Architecture](user-guides/60-architecture.md) — Spring wiring, transport executors, caching, pipelines
- [Model Contract](user-guides/61-model-contract.md) — all `DiscoveryModelKeys` constants + response shapes
- [REST Endpoints](user-guides/62-rest-endpoints.md) — complete endpoint table
- [Release Notes](user-guides/63-release-notes.md)

### Help

- [Troubleshooting](user-guides/70-troubleshooting.md)
