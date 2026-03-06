# Recommendation Widgets

## Overview

`DiscoveryRecommendationComponent` calls the Discovery Recommendations API via CRISP and exposes a `products` list for your template. It supports both:

- **v1 API** (`discoverySearchAPI` resource space, `core.dxpapi.com`) — used when `authKey` is not configured
- **v2 Pathways API** (`discoveryPathwaysAPI` resource space, `pathways.dxpapi.com`) — used automatically when `authKey` is configured

Version selection is automatic — no configuration flag needed. Configure `authKey` (via `BRXDIS_AUTH_KEY` env var or JCR) to enable v2.

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

```yaml
# templates.yaml — use the plugin classpath template (no custom FTL required)
definitions:
  config:
    /hst:hst/hst:configurations/<your-site>/hst:templates:
      /brxdis-recommendations:
        jcr:primaryType: hst:template
        hst:renderpath: classpath:/freemarker/brxdis/brxdis-recommendations.ftl
```

The plugin ships `brxdis-recommendations.ftl` as a ready-to-use horizontal-scroll carousel with scoped CSS injected via `<@hst.headContribution>`. Point `hst:template` at it directly and skip writing a custom template.

### Sitemap entry

```yaml
/recommendations:
  jcr:primaryType: hst:sitemapitem
  hst:componentconfigurationid: hst:pages/recommendations-page
```

Or embed the component as a named child inside an existing page component rather than giving it its own sitemap entry.

---

## Request parameters

All parameters are query string parameters read by `getPublicRequestParameter`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `widgetId` | String | — | Discovery widget ID. If absent and `widgetType` is set as a component param, the component auto-resolves the first enabled widget of that type from the merchant widgets API. |
| `contextProductId` | String | — | PID of the product currently being viewed. Used for "similar items" and "also viewed" widgets. |
| `contextPageType` | String | — | Page context: `pdp`, `plp`, `home`, `cart`, or any custom value. v1 sends as `type`; v2 sends as `context.page_type`. |
| `limit` | int | `8` | Maximum number of recommended products to return. |
| `fields` | String | — | Comma-separated field list to return (`fl` parameter, v2 only). Example: `pid,title,price`. |
| `filter` | String | — | Filter expression (`filter` parameter, v2 only). Example: `brand:"Nike"`. |

**Component parameters** (set in HST config, not query string — exposed via `@ParametersInfo` for EM authoring):

| Parameter | Type | Default | Description |
|---|---|---|---|
| `widgetType` | String | `""` | Widget type for dynamic resolution: `item`, `keyword`, `category`, `personalized`, `global`. Used when `widgetId` is not passed at request time. |
| `limit` | int | `8` | Default number of recommendations. Can be overridden per-request via the `limit` query parameter. |

Example (v2): `GET /site/recommendations?widgetId=similar-items&contextProductId=SKU-123&contextPageType=pdp&limit=6&fields=pid,title,price`

---

## Request attributes and models

| Key | Type | setAttribute | setModel | Description |
|---|---|---|---|---|
| `products` | `List<ProductSummary>` | ✓ | ✓ | Recommended products from Discovery |
| `widgetId` | `String` | ✓ | ✓ | The resolved widget ID |

### `ProductSummary` model

```
ProductSummary
├── String id           — product ID (PID)
├── String title
├── String url
├── String imageUrl
├── BigDecimal price    — may be null if Discovery does not return price
├── String currency
└── Map<String,Object> attributes
```

---

## Freemarker template example

The plugin provides `brxdis-recommendations.ftl` (classpath) as the recommended starting point. For a custom template, the model keys are:

- `products` — `List<ProductSummary>` (methods: `id()`, `title()`, `url()`, `imageUrl()`, `price()`, `currency()`)
- `widgetId` — `String`

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

When `authKey` is configured, the component automatically calls the v2 Pathways API at `pathways.dxpapi.com`:

- URL pattern: `/api/v2/widgets/{widgetType}/{widgetId}?account_id=...&domain_key=...&rows=...`
- Auth: `auth-key` header added per-request from `config.authKey()`
- `contextPageType` is sent as `context.page_type` (v2 param name)
- `fields` and `filter` params are only sent in v2 mode

**Required CRISP config for v2:**

The `discoveryPathwaysAPI` resource space must be configured in your host project. See [02-discovery-config.md](02-discovery-config.md#discoveryPathwaysAPI-required-for-v2-recommendations).

---

## Dynamic widget resolution

When `widgetId` is not passed as a request parameter but `widgetType` is set as a component parameter, the component calls `DiscoveryWidgetService.findByType(widgetType, config)` which fetches the merchant widgets list (`GET /api/v1/merchant/widgets?account_id=...`) and returns the first enabled widget of that type. Results are cached in-process for 5 minutes.

```yaml
/recs:
  jcr:primaryType: hst:component
  hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryRecommendationComponent
  hst:template: recommendations-fragment
  hst:parameternames: [widgetType, limit]
  hst:parametervalues: [item, 6]
```

With this config, the component auto-resolves the first enabled `item` widget and returns up to 6 products. No hardcoded widget ID needed. The `limit` can still be overridden per-request via `?limit=N`.

---

## Embedding on a PDP

To render recommendations on a product detail page, include the recommendation component as a named child of your PDP page component:

```yaml
/pdp-page:
  jcr:primaryType: hst:component
  hst:componentclassname: com.example.site.component.ProductDetailComponent
  hst:template: pdp
  /recs:
    jcr:primaryType: hst:component
    hst:componentclassname: org.bloomreach.forge.discovery.site.component.DiscoveryRecommendationComponent
    hst:template: recommendations-fragment
```

In the PDP template, dispatch to the child component's output via `<@hst.include ref="recs"/>`.

Pass the current product's PID as a request attribute and forward it to the recs component query string, or set `contextProductId` as a component parameter in the HST config if it is static.

---

## Error handling

`DiscoveryRecommendationComponent` throws `HstComponentException` on JCR session failures. `ConfigurationException` is thrown when required credentials are missing. Discovery API errors are wrapped in `RecommendationException`. An empty `products` list is returned when the API returns no results — the template should handle this gracefully (the `<#if products?size gt 0>` guard above is sufficient).

When `authKey` is absent, v2 mode is silently skipped — the component calls v1 without error. When dynamic widget resolution fails (API unreachable or no matching widget), the component proceeds with an empty widget ID and the Discovery API determines the fallback behavior.
