# Recommendation Widgets

## Overview

`DiscoveryRecommendationComponent` calls the Discovery Recommendations API via CRISP and exposes a `products` list for your template. It supports both:

- **v1 API** (`discoverySearchAPI`) — used when `authKey` is not configured
- **v2 Pathways API** (`discoveryPathwaysAPI`) — used automatically when `authKey` is configured

Version selection is automatic — no configuration flag needed. Configure `authKey` through `BRXDIS_AUTH_KEY`, `-Dbrxdis.authKey`, `brxdis:authKey` in the global config node, or a channel-level `discoveryAuthKeyEnvVar` override to enable v2.

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

| Parameter | Type | Default | Description |
|---|---|---|---|
| `document` | JCR path | — | `brxdis:recommendationDocument` picker. Stores the widget ID. When set, takes precedence over URL `widgetId`. |
| `contextProductId` | String | `""` | Static PID override — used as `contextProductId` when no product band is wired. |
| `contextProductPidProperty` | String | `"brxdis:pid"` | JCR property name on a product bean to read PID from (advanced). |
| `limit` | int | `8` | Default number of recommendations. |
| `showPrice` | boolean | `true` | Whether the template shows price. |
| `showDescription` | boolean | `false` | Whether the template shows description. |
| `dataSource` | String | `"standalone"` | `"standalone"` or `"productDetailBand"`. See [PDP band mode](#pdp-band-mode). |
| `band` | String | `"default"` | Band name to read the product PID from when `dataSource=productDetailBand`. |

---

## Request parameters (query string)

| Parameter | Type | Default | Description |
|---|---|---|---|
| `widgetId` | String | — | Discovery widget ID. Overridden by `document` component param if set. |
| `contextProductId` | String | — | PID of the product currently being viewed. |
| `contextPageType` | String | — | Page context: `pdp`, `plp`, `home`, `cart`, or any custom value. |
| `limit` | int | component param | Maximum number of recommended products. |
| `fields` | String | — | Comma-separated field list (`fl` param, v2 only). Example: `pid,title,price`. |
| `filter` | String | — | Filter expression (`filter` param, v2 only). Example: `brand:"Nike"`. |

Example (v2): `GET /site/recommendations?widgetId=similar-items&contextProductId=SKU-123&contextPageType=pdp&limit=6`

---

## Models set on the request

| Key | Type | Description |
|---|---|---|
| `products` | `List<ProductSummary>` | Recommended products from Discovery |
| `widgetId` | `String` | The resolved widget ID |

---

## PDP band mode

When `dataSource=productDetailBand`, the component reads the `contextProductId` from `DiscoveryRequestCache` populated by a sibling `DiscoveryProductDetailComponent` on the same page (identified by the matching `band` parameter). This allows a "Similar Items" widget to automatically use the current product's PID without any URL parameter wiring.

```yaml
/pdp-page:
  jcr:primaryType: hst:component
  hst:componentclassname: …DiscoveryProductDetailComponent
  hst:template: brxdis-product-detail
  hst:parameternames: [band]
  hst:parametervalues: [default]
  /similar:
    jcr:primaryType: hst:component
    hst:componentclassname: …DiscoveryRecommendationComponent
    hst:template: brxdis-recommendations
    hst:parameternames: [dataSource, band, limit]
    hst:parametervalues: [productDetailBand, default, 6]
```

In Channel Manager preview mode, if the band has not been populated (e.g. the product detail component is missing), a `brxdis_warning` attribute is set.

**Standalone mode** (`dataSource=standalone`, the default): reads `contextProductId` from the URL param or the `contextProductId` component parameter. Use for recommendation widgets not on a PDP.

---

## Dynamic widget resolution

When `widgetId` is not set (neither via document picker nor URL param) but a `widgetType` is needed, the component can auto-resolve the first enabled widget of that type via `DiscoveryWidgetService`. Results are cached in-process for 5 minutes.

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

`ConfigurationException` is thrown when required credentials are missing. Discovery API errors are wrapped in `RecommendationException`. An empty `products` list is returned when the API returns no results — the template should guard with `<#if products?has_content>`.

When `authKey` is absent, v2 mode is silently skipped — the component calls v1 without error. When `dataSource=productDetailBand` and the band has not been populated, an empty products list is returned (with a `brxdis_warning` in Channel Manager preview).
