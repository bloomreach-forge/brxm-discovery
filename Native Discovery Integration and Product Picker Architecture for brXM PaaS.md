# Plan: Native Discovery Integration & Product Pickers for brXM PaaS

## 1\. Idea & Goals

### 1.1 High‑level idea

Build a **Discovery‑centric integration layer and UI** for **brXM PaaS**, so that:

* The **storefront** (HST / SPA) can use **Discovery** for search, category pages, and recommendations.  
* **Content editors** can **visually browse and pick products** (with images, titles, etc.) from Discovery inside the brXM editor using **Open UI document field extensions**, similar to Content SaaS.  
* We **do not** rely on:  
  * The old, deprecated Discovery connector.  
  * brX GraphQL / Apollo server as a mandatory layer.  
* We **do** rely on:  
  * **External commerce system** as the system of record (SoR) for products.  
  * Existing or future **connectors/feeds** to keep Discovery’s catalog in sync with the SoR.  
  * **CRISP API** as the HTTP integration backbone inside brXM.

In short: *Discovery is the “product search & browse brain”; brXM is the “page & content brain”; the plugin is the glue, and editors get a modern picker UI like Content SaaS.*

### 1.2 Non‑goals

* Do **not** model the product catalog in brXM as SoR.  
* Do **not** resurrect BRIEF/Java Commerce Accelerator.  
* Do **not** require brX GraphQL/Apollo for this to work.  
* Do **not** attempt to manage Discovery configuration, synonyms, etc. from brXM; that remains in the Discovery UI and connectors.

---

## 2\. Conceptual Model

### 2.1 Data ownership

* **SoR (Shopify/commercetools/etc.)**  
  * Owns product data, pricing, stock, orders, customers.  
  * Feeds products into **Discovery** via connectors/reference architectures.  
* **Discovery**  
  * Maintains its own **search index** of the commerce catalog.  
  * Exposes **Search / Category / Recommendations APIs**.  
  * Learns from traffic via **pixel events**.  
* **brXM PaaS**  
  * Owns **content** and **page composition**.  
  * Uses Discovery as a **read‑only search & recommendations service**.  
  * Holds **references** to products (IDs) in content, not full product entities.

### 2.2 Editor experience concept (Open UI picker)

Editors working in the brXM CMS:

* See a **“Discovery Product Picker”** field in their documents.  
* Click it to open a **visual product browser** in an embedded iframe:  
  * Search box and filters backed by Discovery.  
  * Grid/list of products (image, title, maybe price).  
* Select one or more products; the field stores **only product IDs**.

This is implemented using **Open UI document field extensions**, which allow embedding external UI inside the CMS and communicating via a JS client library.\[1\]\[2\]

### 2.3 Storefront concept

On the delivery side:

* **Search page**:  
  * HST component calls Discovery via CRISP to get a ranked list of products and facets.  
  * Template renders tiles (optionally enriched later via SoR).  
* **Category page**:  
  * Same as search, but based on category ID.  
* **Product detail page**:  
  * Optionally gets product content from SoR and recs from Discovery.  
* **Recommendation widgets**:  
  * HST component calls Discovery recs API, renders a strip of products (e.g. “related items”).

---

## 3\. Architecture

At a high level we’ll have:

1. **Site module (`discovery-site`)**  
   * CRISP resource configuration for Discovery.  
   * Java service layer that wraps Discovery APIs.  
   * HST components for SRP, PLP, recs.  
   * Templates and pixel hooks.  
2. **CMS module (`discovery-cms`)**  
   * `brxdis:discoveryConfig` document type and editor.  
   * Open UI extension registration for product picker.  
   * CMS‑side REST endpoints used by the picker UI.

### 3.1 Integration backbone: CRISP API

We will use **CRISP** as the standard way to call external HTTP APIs from brXM:

* CRISP provides:  
  * Generic **ResourceServiceBroker** and **ResourceResolver** abstraction.  
  * Centralized **HTTP client, connection pooling, caching**, and configuration.\[3\]\[4\]  
* We define a **`discoveryApi` resource space**:  
  * Base URI \= Discovery Search API base.  
  * Auth & account/domain/api key via properties.  
  * Optional caching of responses.

All Discovery calls inside brXM go through CRISP, not ad‑hoc HttpClient.

---

## 4\. Runtime Architecture (Site / Storefront)

### 4.1 Service layer (site)

In `discovery-site`:

**Models** (simplified):

```java
class SearchQuery {
    String query;
    int page;
    int pageSize;
    String sort;
    Map<String, List<String>> filters;
}

class CategoryQuery {
    String categoryId;
    int page;
    int pageSize;
    String sort;
    Map<String, List<String>> filters;
}

class RecQuery {
    String widgetId;
    String contextProductId;
    String contextPageType; // "pdp", "plp", "home", etc.
    int limit;
}

class ProductSummary {
    String id;
    String title;
    String url;
    String imageUrl;
    BigDecimal price;
    String currency;
    Map<String, Object> attributes;
}

class FacetValue {
    String value;
    long count;
    boolean selected;
}

class Facet {
    String name;
    List<FacetValue> values;
}

class SearchResult {
    List<ProductSummary> products;
    Map<String, Facet> facets;
    long total;
    int page;
    int pageSize;
}
```

**Config POJO:**

```java
class DiscoveryConfig {
    String accountId;
    String domainKey;
    String apiKey;
    String searchBasePath;
    String categoryBasePath;
    String recsBasePath;
    int defaultPageSize;
    String defaultSort;
}
```

**Interfaces:**

```java
public interface DiscoverySearchService {
    SearchResult search(SearchQuery query, DiscoveryConfig config);
    SearchResult category(CategoryQuery query, DiscoveryConfig config);
}

public interface DiscoveryRecommendationService {
    List<ProductSummary> recommend(RecQuery query, DiscoveryConfig config);
}
```

**CRISP‑backed impl sketch:**

```java
public class DiscoverySearchServiceImpl implements DiscoverySearchService {

    private final ResourceServiceBroker broker;
    private final DiscoveryResourceMapper mapper;

    @Override
    public SearchResult search(SearchQuery q, DiscoveryConfig cfg) {
        Map<String, Object> params = mapper.toSearchParams(q, cfg);
        Resource r = broker.resolve("discoveryApi", cfg.getSearchBasePath(),
                                    "get", params, new ExchangeHint());
        return mapper.toSearchResult(r, cfg);
    }

    @Override
    public SearchResult category(CategoryQuery q, DiscoveryConfig cfg) {
        Map<String, Object> params = mapper.toCategoryParams(q, cfg);
        Resource r = broker.resolve("discoveryApi", cfg.getCategoryBasePath(),
                                    "get", params, new ExchangeHint());
        return mapper.toSearchResult(r, cfg);
    }
}
```

A similar implementation exists for recommendations.

`DiscoveryResourceMapper` encapsulates JSON → DTO mapping using CRISP’s `Resource` API.

### 4.2 HST components

**Search page component:**

```java
public class DiscoverySearchComponent extends BaseHstComponent {

    @Inject DiscoveryConfigResolver configResolver;
    @Inject DiscoverySearchService searchService;

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        DiscoveryConfig cfg = configResolver.resolveForRequest(request);
        SearchQuery query = QueryParamParser.fromRequest(request, cfg);
        SearchResult result = searchService.search(query, cfg);

        request.setAttribute("searchResult", result);
        request.setAttribute("query", query.getQuery());
    }
}
```

**Category page component:**

Similar to search, but category ID is resolved from sitemap or component parameter.

**Recommendations component:**

Takes `widgetId` and context (productId/pageType) from component params/request and calls `DiscoveryRecommendationService`.

Templates (Freemarker/JSP) render the data returned from Discovery.

### 4.3 Pixel

We add a small client‑side script:

* On SRP:  
  * Sends a “search performed” event to Discovery with query, page, and SKUs.  
* On PDP:  
  * Sends “product view” event.  
* On clicks:  
  * Sends “product click” event (SKU \+ position).

This is injected by a small HST component or template fragment. Exact pixel API usage depends on Discovery’s current event schema but conceptually it mirrors existing Discovery pixel integrations.

---

## 5\. CMS Architecture (Config \+ Product Picker)

### 5.1 Discovery config document

In `discovery-cms`, define a `brxdis:discoveryConfig` type:

* Fields:  
  * Account ID, domain key, API key.  
  * Search/category/recs base paths.  
  * Default page size/sort.  
* Editor template so admins can manage these per environment.  
* Channel parameter `discoveryConfigPath` references the config doc; `DiscoveryConfigResolver` reads it and builds the `DiscoveryConfig` POJO.

### 5.2 Open UI product picker (document field extension)

We use **Open UI Extensions** with the **document.field** extension point:\[1\]\[2\]\[5\]

1. **Register the extension** under `/hippo:configuration/hippo:frontend/cms/ui-extensions`:

```
/hippo:configuration/hippo:frontend/cms/ui-extensions/discoveryProductPicker:
  jcr:primaryType: frontend:uiExtension
  frontend:extensionPoint: document.field
  frontend:displayName: Discovery Product Picker
  frontend:initialHeightInPixels: 300
  frontend:url: /discovery-picker/index.html  # served by us
  frontend:config: >-
    {
      "backend.baseUrl": "/cms/rest/discovery-picker",
      "imageAttribute": "image_url",
      "titleAttribute": "title"
    }
```

2. **Wiring into a document type**:  
     
   In the doc type you want to enhance (e.g. `myproject:campaign`), add an `OpenUiString` field and set its `ui.extension` to `"discoveryProductPicker"` as shown in the “Configure Open UI Pickers from Scratch” docs.\[5\]  
     
   The field stores **one or more product IDs** (Discovery/SoR IDs), nothing else.

### 5.3 Picker frontend

A small JS app (React/Vue/vanilla) that:

1. Loads the Open UI library:

```javascript
import UiExtension from '@bloomreach/ui-extension';
```

2. Registers:

```javascript
UiExtension.register().then(ui => {
  const config = ui.extension.config; // parsed frontend:config JSON
  const currentValue = ui.document.field.getValue(); // IDs

  // Render search UI and current selections...
});
```

3. Renders:  
     
   * Search box and filters, sending queries to a CMS REST endpoint.  
   * Product cards (image, title, ID).  
   * On selection change, calls `ui.document.field.setValue([...ids])`.

The picker UI talks **only to our own CMS REST backend** (not directly to Discovery), so no CORS or secret leakage.

### 5.4 CMS REST backend for the picker

A JAX‑RS resource in `/cms` (e.g. `/rest/discovery-picker`) that:

* Accepts requests like:  
    
  * `GET /search?q=&page=&pageSize=&filters=…`  
  * `GET /items?ids=1,2,3`


* Uses the **same `DiscoverySearchService`** (via CRISP) to query Discovery.  
    
* Maps Discovery responses to a slim DTO for the picker:

```json
{
  "items": [
    {
      "id": "12345",
      "title": "Product name",
      "imageUrl": "https://…",
      "price": "19.99",
      "currency": "USD"
    }
  ],
  "total": 42
}
```

The picker frontend doesn’t know anything about Discovery or the SoR; it only renders that JSON.

---

## 6\. Implementation Phases

### Phase 1 – Core Discovery services (site)

* Configure CRISP with `discoveryApi` resource resolver (base URI \+ auth).  
* Implement:  
  * `DiscoverySearchServiceImpl`  
  * `DiscoveryRecommendationServiceImpl`  
  * `DiscoveryResourceMapper`  
* Create basic HST components for SRP/PLP/recs and wire a simple template.

### Phase 2 – CMS config and resolver

* Define `brxdis:discoveryConfig` type and editor template.  
* Implement `DiscoveryConfigResolver` (lookup doc path from channel param, map to POJO).  
* Add admin docs on how to create/configure the discovery config per channel.

### Phase 3 – Product picker (Open UI)

* Register `discoveryProductPicker` Open UI extension node.  
* Add `OpenUiString` field to at least one document type and wire `ui.extension` to the picker.  
* Implement:  
  * CMS JAX‑RS endpoint `/rest/discovery-picker`.  
  * Picker frontend app using `@bloomreach/ui-extension`:  
    * list/search products  
    * show thumbnails  
    * set selected IDs via `ui.document.field.setValue()`.

### Phase 4 – Polish and enrichment

* Add more filters/facets in picker and storefront, matching Discovery’s attributes.  
* Add pixel events into storefront templates.  
* Optionally add **SoR enrichment** later (price/stock) in service layer and picker backend, without changing the editor UI.

---

## References

1. **Open UI Extensions – Introduction**  
   Explains the concept of Open UI extension points (document fields, page tools) and that extensions run in iframes and communicate via a JS library.  
   [https://xmdocumentation.bloomreach.com/library/concepts/open-ui/introduction.html](https://xmdocumentation.bloomreach.com/library/concepts/open-ui/introduction.html)  
     
2. **Use the Open UI Extension Client Library**  
   Shows how to use `@bloomreach/ui-extension`, `UiExtension.register()`, and access `ui.extension.config`, user info, etc.  
   [https://xmdocumentation.bloomreach.com/library/concepts/open-ui/open-ui-extension-client-library.html](https://xmdocumentation.bloomreach.com/library/concepts/open-ui/open-ui-extension-client-library.html)  
     
3. **Introduction to CRISP API**  
   Describes CRISP as a generic external API integration layer with ResourceServiceBroker/ResourceResolver, caching, configuration in the repository, etc.  
   [https://xmdocumentation.bloomreach.com/library/concepts/crisp-api/introduction.html](https://xmdocumentation.bloomreach.com/library/concepts/crisp-api/introduction.html)  
     
4. **Configuring CRISP Addon**  
   Shows how to define CRISP ResourceResolvers (baseUri, RestTemplate, properties) either in the repository or Spring XML.  
   [https://xmdocumentation.bloomreach.com/library/concepts/crisp-api/configuring-crisp-addon.html](https://xmdocumentation.bloomreach.com/library/concepts/crisp-api/configuring-crisp-addon.html)  
     
5. **Configure Open UI Pickers from Scratch**  
   Documents how to configure commerce Open UI pickers in non‑Accelerator projects, including registering `frontend:uiExtension` nodes for product/category pickers and wiring them into document types as `OpenUiString` fields.  
   [https://xmdocumentation.bloomreach.com/library/solutions/commerce-starterstore/graphql-service/configure-open-ui-pickers-from-scratch.html](https://xmdocumentation.bloomreach.com/library/solutions/commerce-starterstore/graphql-service/configure-open-ui-pickers-from-scratch.html)  
     
6. **External Document Picker (Open UI) – GitHub**  
   Example repo showing an Open UI‑based external document picker with a CMS REST backend and an Angular-based picker UI, a good pattern to copy for our Discovery product picker.  
   [https://github.com/bloomreach/brxm-open-ui-external-document-picker](https://github.com/bloomreach/brxm-open-ui-external-document-picker)

