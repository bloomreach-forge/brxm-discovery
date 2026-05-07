# Release Notes

> **Alpha — Early Development**
> APIs, configuration schemas, HST component interfaces, and JCR node types are subject to change without notice between releases. Do not treat any aspect of this plugin as stable until a non-SNAPSHOT version is published to Bloomreach Forge.

---

## 0.0.2-SNAPSHOT

**Status:** Alpha. Not yet released to Bloomreach Forge.

- Four recommendation components: `DiscoveryProductRecommendationComponent`, `DiscoveryCategoryRecommendationComponent`, `DiscoveryGlobalRecommendationComponent`, `DiscoveryKeywordRecommendationComponent`
- Three-step wizard Open UI extensions for all recommendation and search document types
- Visual search proxy (`BrxdisVisualSearchPipeline`, `/_brxdis-api/visual-search/`) with channel-credential resolution
- SEO-friendly path-based URLs for product and category pages (`/product/{slug}/pid/{id}`, `/category/{slug}/cid/{id}`)
- `DiscoveryChannelInfo` extended with pixel tracking flags, consent gating, and visual search fields (13 fields total)
- `DiscoveryKeywordRecommendationComponent` with `specific` / `url` resolution modes
- Removed `DiscoveryRecommendationComponent` (replaced by four typed components)
- Removed CRISP environment synchronizer; credential injection is now entirely JCR/env/sys based

---

## 0.0.1-SNAPSHOT

**Status:** Alpha. Not yet released to Bloomreach Forge.

Initial snapshot. Coverage includes Discovery-powered search, category browse, recommendations (v1 and v2 Pathways), autosuggest, CMS product/widget/category pickers, pixel tracking, and per-channel credential overrides layered on top of shared global configuration.
