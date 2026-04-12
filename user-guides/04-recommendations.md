# Recommendation Widgets

## Overview

`DiscoveryRecommendationComponent` calls the Discovery Recommendations API via CRISP and exposes a `products` list for your template. It supports both:

- **v1 API** (`discoverySearchAPI`) - used when `authKey` is not configured
- **v2 Pathways API** (`discoveryPathwaysAPI`) - used automatically when `authKey` is configured

Version selection is automatic - no configuration flag needed. Configure `authKey` through `BRXDIS_AUTH_KEY`, `-Dbrxdis.authKey`, `brxdis:authKey` in the global config node, or a channel-level `discoveryAuthKeyEnvVar` override to enable v2.

---

## Recommendation document types

The plugin ships three JCR document types for managing recommendation configurations. Each is authored with a **3-step wizard** Open UI extension and stores all configuration as a JSON string in `brxdis:config`.

| Document type | CND name | Wizard extension | Widget types |
|---|---|---|---|
| Discovery Product Recommendation | `brxdis:productRecommendationDocument` | `discoveryProductRecommendationWizard` | `co_viewed`, `co_bought`, `rt_recs`, `mlt` |
| Discovery Category Recommendation | `brxdis:categoryRecommendationDocument` | `discoveryCategoryRecommendationWizard` | `category` |
| Discovery Global/Personalized Recommendation | `brxdis:globalRecommendationDocument` | `discoveryGlobalRecommendationWizard` | `bestseller`, `trending_product`, `jfy`, `past_purchases`, `recently_viewed` |

### `brxdis:config` JSON schema

```json
{
  "widgetId":           "similar-items",
  "widgetName":         "Similar Items",
  "widgetType":         "item",
  "contextProductId":   "SKU-123",
  "contextProductName": "Classic T-Shirt",
  "contextCategoryId":  null,
  "contextCategoryName": null
}
```

- `contextProductId` / `contextProductName` are present on product-type documents only. A `null` value means "fall back to the `?pid=` URL param at render time".
- `contextCategoryId` / `contextCategoryName` are present on category-type documents only. A `null` value means "fall back to the `?category=` URL param".
- Global/personalized documents carry neither context field.

> **Migration note**: `brxdis:recommendationDocument` (which stored only `brxdis:widgetId`) is removed. Existing documents should be recreated using the appropriate typed document.

---

## HST configuration

### Register the component and template

```yaml
# components.yaml
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:components:
      /recommendations:
        jcr:primaryType: hst:component
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryRecommendationComponent
        hst:template: brxdis-recommendations
```

The bundled `brxdis-recommendations` template is auto-registered under `hst:default`, so no manual `templates.yaml` entry is required unless you want to override it. The plugin ships it as a ready-to-use horizontal-scroll carousel with scoped CSS injected via `<@hst.headContribution>`.

---

## Component parameters

Set in HST config via `@ParametersInfo` (visible in the Channel Manager component editor):

| Parameter | Group | Type | Default | Description |
|---|---|---|---|---|
| `document` | Recommendations | JCR path | - | Picker for any `brxdis:productRecommendationDocument`, `brxdis:categoryRecommendationDocument`, or `brxdis:globalRecommendationDocument`. Stores the widget config as JSON. When set, takes precedence over URL `widgetId`. |
| `limit` | Recommendations | int | `8` | Default number of recommendations. |
| `showPrice` | Recommendations | boolean | `true` | Whether the template shows price. |
| `showDescription` | Recommendations | boolean | `false` | Whether the template shows description. |
| `useProductDetailContext` | Advanced | boolean | `false` | When checked, reads the product shown by a Product Detail component on this page and recommends similar items. The Product Detail component must appear above this one in the layout. |
| `contextProductId` | Advanced | String | `""` | Explicit product ID to recommend against. Overrides automatic detection. Leave blank for auto. |
| `contextProductPidProperty` | Advanced | String | `"brxdis:pid"` | JCR property name for product ID resolution from the page content bean. Only change if your content model uses a custom property. |

---

## Request parameters (query string)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `widgetId` | String | - | Discovery widget ID. Overridden by `document` component param if set. |
| `contextProductId` | String | - | PID of the product currently being viewed. |
| `contextPageType` | String | - | Page context: `pdp`, `plp`, `home`, `cart`, or any custom value. |
| `limit` | int | component param | Maximum number of recommended products. |
| `fields` | String | - | Comma-separated field list (`fl` param, v2 only). Example: `pid,title,price`. |
| `filter` | String | - | Filter expression (`filter` param, v2 only). Example: `brand:"Nike"`. |

Example (v2): `GET /site/recommendations?widgetId=similar-items&contextProductId=SKU-123&contextPageType=pdp&limit=6`

---

## Models set on the request

| Key | Type | Description |
|---|---|---|
| `products` | `List<ProductSummary>` | Recommended products from Discovery |
| `widgetId` | `String` | The resolved widget ID |

---

## Product Detail page - "Similar Items" carousel

To show a "Similar Items" carousel on a PDP that automatically uses the current product's PID, use `DiscoveryProductDetailComponent` alongside `DiscoveryRecommendationComponent` and check **Link to Product Detail on page** on the recommendations component.

### Channel Manager setup

1. Add `DiscoveryProductDetailComponent` to the page layout.
2. Add `DiscoveryRecommendationComponent` **below** it on the same page.
3. On the recommendations component, set **Link to Product Detail on page** = checked.
4. Pick a recommendation widget document (or pass `?widgetId=` in the URL).

### HST `pages.yaml` example

```yaml
/pdp-page:
  jcr:primaryType: hst:component
  /main:
    jcr:primaryType: hst:component
    hst:template: pdp-layout
    /product-detail:
      jcr:primaryType: hst:containercomponent
      hst:xtype: hst.nomarkup
      /product:
        jcr:primaryType: hst:containeritemcomponent
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryProductDetailComponent
        hst:template: brxdis-product-detail
    /similar:
      jcr:primaryType: hst:containercomponent
      hst:xtype: hst.nomarkup
      /recs:
        jcr:primaryType: hst:containeritemcomponent
        hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryRecommendationComponent
        hst:template: brxdis-recommendations
        hst:parameternames: [useProductDetailContext, limit]
        hst:parametervalues: [true, 6]
```

When `useProductDetailContext=true`, the recommendation component reads the PID resolved by `DiscoveryProductDetailComponent` on the same page. If the Product Detail component is missing or has no PID, an empty products list is returned (with a `brxdis_warning` in Channel Manager preview).

**Standalone mode** (the default, `useProductDetailContext=false`): reads `contextProductId` from the URL param or the `contextProductId` component parameter. Use this for recommendation widgets not on a PDP.

---

## Dynamic widget resolution

When `widgetId` is not set (neither via document picker nor URL param), the component can auto-resolve the first enabled widget of the appropriate type via `DiscoveryWidgetService`. Results are cached in-process for 5 minutes.

```yaml
/recs:
  jcr:primaryType: hst:component
  hst:componentclassname: …DiscoveryRecommendationComponent
  hst:template: brxdis-recommendations
  hst:parameternames: [limit]
  hst:parametervalues: [6]
```

---

## Freemarker template example

The plugin provides `brxdis-recommendations.ftl` as the recommended starting point. For a custom template:

```ftl
<#if products?? && products?has_content>
<section class="recommendations">
  <h2>You may also like</h2>
  <div class="product-strip">
    <#list products as item>
    <div class="product-card">
      <#if item.imageUrl()?has_content>
      <img src="${item.imageUrl()}" alt="${item.title()!""}"/>
      </#if>
      <a href="${item.url()!""}">${item.title()!item.id()}</a>
      <#if item.price()??>
      <span class="price">${item.currency()!""}&nbsp;${item.price()?string("0.00")}</span>
      </#if>
    </div>
    </#list>
  </div>
</section>
</#if>
```

---

## v2 Pathways API

When `authKey` is configured, the component automatically calls the v2 Pathways API:

- URL pattern: `/api/v2/widgets/{widgetType}/{widgetId}?account_id=...&domain_key=...&rows=...`
- Auth: `auth-key` header added per-request from `config.authKey()`
- `contextPageType` is sent as `context.page_type` (v2 param name)
- `fields` and `filter` params are only sent in v2 mode

---

## Curated product showcase (Product Highlight)

For static, hand-picked product placements (homepage hero, featured sale items), use `DiscoveryProductHighlightComponent` instead of `DiscoveryRecommendationComponent`. Editors select up to 4 `brxdis:productDetailDocument` pickers in the Channel Manager. Each product is fetched individually from Discovery at render time.

```yaml
/highlight:
  jcr:primaryType: hst:component
  hst:componentclassname: …DiscoveryProductHighlightComponent
  hst:template: brxdis-product-highlight
```

Models set: `products` (`List<ProductSummary>`, may contain nulls for slots with no document), `productBeans` (the raw `DiscoveryProductDetailBean` list for advanced templates).

---

## Error handling

`ConfigurationException` is thrown when required credentials are missing. Discovery API errors are wrapped in `RecommendationException`. An empty `products` list is returned when the API returns no results - the template should guard with `<#if products?has_content>`.

When `authKey` is absent, v2 mode is silently skipped - the component calls v1 without error. When `useProductDetailContext=true` and no product is on the page, an empty products list is returned (with a `brxdis_warning` in Channel Manager preview).
