There are two distinct integration surfaces within brXM:
                                                                                                                                                                                                                                                                                                                          
---                                                                                      
1. Commerce Pickers (CMS Editor UI)

An Angular UI Extension that lets content editors select Discovery products, categories, and widgets inside the CMS document editor.

Flow:                                                                                                                                                                                                                                                                                                                   
CMS Document Editor                                                                                                                                                                                                                                                                                                     
→ Discovery Picker UI Extension (Angular)                                                                                                                                                                                                                                                                           
→ Apollo GraphQL Client
→ https://graphql-commerce-internal.bloomreach.io/graphql
(with QueryHint: accountId, domainKey, viewId, realm)
→ Returns Products / Categories / Widgets

Key files:
- commerce-pickers/src/main/resources/hcm-config/configuration/main.yaml — declares the 3 extension packages (discoveryProductPicker, discoveryCategoryPicker, discoveryWidgetPicker) and injects Discovery config
- commerce-pickers/src/app/cms-context.service.ts — reads discoveryAccountId, discoveryDomainKey, discoveryViewId, discoveryRealm from the extension config
- commerce-pickers/src/app/apollo-context.service.ts — builds the GraphQL client, handles auth, passes the QueryHint
- commerce-pickers/src/app/sm-api.service.ts — hits core.dxpapi.com/api/v1/merchant/widgets directly for widget listings

GraphQL queries pass a queryHint object:
{
accountId: string,
domainKey: string,
viewId?: string,
brEnvType?: "STAGING" | "PRODUCTION"
}

  ---
2. Content Feed / BrSM Search Client (Backend)

A Spring backend service that calls the Discovery Search API to power content search within the site delivery layer.

Flow:
Site Delivery Layer
→ BrSMSearchClient (via CRISP ResourceServiceBroker)
→ RestTemplate (pooled: 200/route, 1000 total)
→ https://core.dxpapi.com/api/v1/core/
(params: account_id, catalog_name, view_id, q, fq, fl, facet.field, sort)
→ Returns indexed content results + facets

Key files:
- content-feed/search/impl/site/src/main/java/.../BrSMSearchClient.java — core REST client; async execution via ExecutorService, request-scoped deduplication, EhCache (1000 entries, 60s TTL)
- content-feed/search/impl/repository/.../crisp/brsmsearchclient.xml — CRISP bean wiring (SimpleJacksonRestTemplateResourceResolver)
- content-feed/search/impl/site/src/main/java/.../AccountInfoImpl.java — holds accountId, API uri, and catalogs

Discovery-specific fields indexed under the xm* namespace: xmPrimaryDocType, xmChannelIds, xmHandleId, xmPath, xmDisplayName, etc.

  ---
Configuration Binding

Both surfaces are wired via channel parameters and properties:

# platform-dev.properties
public.brx.smAccountId=6429
public.brx.smDomainKey=brxsaas_eng01
public.brx.graphql.baseurl=https://graphql-commerce-internal.bloomreach.io
public.brx.graphql.tenantName=brx-saas-sample

# CRISP search
brx.search.uri=https://core.dxpapi.com/api/v1/core/
brx.search.accountId=...
brx.search.catalogs=...

Channel YAML (blueprint-blank-channel.yaml) exposes smAccountId and smDomainKey as required/optional channel params so each site can point to its own Discovery account.

  ---
Summary

┌────────────────────────┬─────────────────────┬─────────────────────────────────────────┐
│         Layer          │      Mechanism      │                Endpoint                 │
├────────────────────────┼─────────────────────┼─────────────────────────────────────────┤
│ CMS Editor (Pickers)   │ GraphQL via Apollo  │ graphql-commerce-internal.bloomreach.io │
├────────────────────────┼─────────────────────┼─────────────────────────────────────────┤
│ CMS Editor (Widgets)   │ REST (SmApiService) │ core.dxpapi.com/api/v1/merchant/widgets │
├────────────────────────┼─────────────────────┼─────────────────────────────────────────┤
│ Site Delivery (Search) │ REST via CRISP      │ core.dxpapi.com/api/v1/core/            │
└────────────────────────┴─────────────────────┴─────────────────────────────────────────┘

✻ Brewed for 2m 22s

❯ great, can you list all the files that relate to this and a brief as to what they do? In particular I'm interested in Discovery, but if there are files with shared responsibility, list those too.

⏺ Explore(List all Discovery integration files)                                                                                                                                                                                                                                                                           
⎿  Done (53 tool uses · 67.8k tokens · 1m 25s)                                                                                                                                                                                                                                                                          
(ctrl+o to expand)

⏺ Here's the complete file inventory:
                                                                                                                                                                                                                                                                                                                          
---             
Backend Search Integration

API (Interfaces & Models)

┌──────────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────────┐                                                                                                                                              
│                                 File                                 │                                           Description                                           │                                                                                                                                              
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                                              
│ content-feed/search/api/src/main/java/.../BrSmConstants.java         │ Constants for Discovery-specific Solr/search field names (xmPrimaryDocType, xmChannelIds, etc.) │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../SearchClient.java          │ Interface that BrSMSearchClient implements                                                      │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../ExternalSearchService.java │ Service interface exposed to the delivery layer                                                 │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../QueryBuilder.java          │ Interface for constructing Discovery search queries                                             │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../AccountInfo.java           │ Model holding accountId, API uri, and catalogs                                                  │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../SearchQuery.java           │ Query model (q, fq, fl, facets, sort, pagination)                                               │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../SearchResult.java          │ Result model wrapping documents + facets                                                        │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../QueryResponse.java         │ Raw API response wrapper                                                                        │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../Document.java              │ Individual indexed document interface                                                           │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../FacetCountResult.java      │ Facet counts returned by the API                                                                │
├──────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/api/src/main/java/.../QueryException.java        │ Exception thrown on search failures                                                             │
└──────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────────┘

Implementation

┌────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                      File                                      │                                                  Description                                                  │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../BrSMSearchClient.java          │ Core REST client; calls core.dxpapi.com/api/v1/core/; async via ExecutorService; request-scoped deduplication │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../ExternalSearchServiceImpl.java │ Wires BrSMSearchClient into the delivery layer                                                                │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../AccountInfoImpl.java           │ Binds brx.search.uri, brx.search.accountId, brx.search.catalogs properties                                    │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../QueryBuilderImpl.java          │ Builds query param map for the Discovery API                                                                  │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../QueryFactoryImpl.java          │ Creates SearchQuery instances                                                                                 │
├────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/main/java/.../DocumentImpl.java              │ Deserializes Discovery API response documents                                                                 │
└────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

CRISP / Configuration

┌────────────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                File                                │                                                                       Description                                                                        │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/repository/.../crisp/brsmsearchclient.xml │ Spring bean definition for the CRISP SimpleJacksonRestTemplateResourceResolver; connection pool (200/route, 1000 total); EhCache (1000 entries, 60s TTL) │
├────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ content-feed/search/impl/repository/.../crisp/crispregistry.yaml   │ Registers brSMSearchClient bean and maps property injections                                                                                             │
└────────────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

Tests

┌────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────┐
│                                    File                                    │                    Description                    │
├────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/test/java/.../BrSMSearchClientTest.java  │ Unit tests for the search client                  │
├────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ content-feed/search/impl/site/src/test/java/.../DocumentImplDeserTest.java │ Deserialization tests for Discovery API responses │
├────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────┤
│ content-feed/tests/search/src/test/java/.../BrSMSearchClientMock.java      │ Mock for use across integration tests             │
└────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────┘

  ---
Commerce Pickers (Angular UI Extension)

Core Services

┌────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                        File                        │                                                       Description                                                        │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/cms-context.service.ts    │ Reads Discovery config from UI extension props (discoveryAccountId, discoveryDomainKey, discoveryViewId, discoveryRealm) │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/apollo-context.service.ts │ Builds Apollo GraphQL client; handles auth; injects QueryHint (accountId, domainKey, viewId, realm) into every request   │
├────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/sm-api.service.ts         │ Calls core.dxpapi.com/api/v1/merchant/widgets directly for widget listings; maps staging vs. production subdomains       │
└────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

GraphQL Queries

┌────────────────────────────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────┐
│                                        File                                        │                               Description                                │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/items.ts                             │ GraphQL queries findItemsByKeyword, findItemById — both accept queryHint │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/category.ts                          │ GraphQL query findCategories with queryHint                              │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/__generated__/globalTypes.ts         │ Generated QueryHintInput type and other shared GraphQL types             │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/__generated__/Items.ts               │ Generated types for items query response                                 │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/__generated__/Categories.ts          │ Generated types for categories query response                            │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/__generated__/ItemFragment.ts        │ Shared item fields fragment                                              │
├────────────────────────────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/modules/commerce/queries/__generated__/ItemVariantFragment.ts │ Shared variant fields fragment                                           │
└────────────────────────────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────┘

UI Components

┌───────────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────┐
│                                         File                                          │                      Description                      │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/products-dialog/products-dialog.component.ts                 │ Dialog for searching and selecting Discovery products │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/products-field/products-field.component.ts                   │ CMS document field that renders selected products     │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/product-variants-dialog/product-variants-dialog.component.ts │ Dialog for selecting product variants                 │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/product-variants-field/product-variants-field.component.ts   │ CMS document field for product variants               │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/categories-dialog/categories-dialog.component.ts             │ Dialog for browsing/selecting Discovery categories    │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/categories-field/categories-field.component.ts               │ CMS document field for selected categories            │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/sm-widgets-dialog/sm-widgets-dialog.component.ts             │ Dialog for selecting Discovery SM widgets             │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/sm-widgets-field/sm-widgets-field.component.ts               │ CMS document field for selected widgets               │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/accounts-dialog/accounts-dialog.component.ts                 │ Dialog for switching between Discovery accounts       │
├───────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/accounts-field/accounts-field.component.ts                   │ CMS document field for account selection              │
└───────────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────┘

Models

┌────────────────────────────────────────────┬────────────────────────────────────────────────────────┐
│                    File                    │                      Description                       │
├────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/sm-widget.ts      │ SmWidget model for Discovery widget API responses      │
├────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/account.ts        │ Account model                                          │
├────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/item-id.ts        │ Item ID model (shared between product/variant pickers) │
├────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/product-field.ts  │ Product field value model                              │
├────────────────────────────────────────────┼────────────────────────────────────────────────────────┤
│ commerce-pickers/src/app/category-field.ts │ Category field value model                             │
└────────────────────────────────────────────┴────────────────────────────────────────────────────────┘

  ---
Configuration & Repository Data

┌────────────────────────────────────────────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                File                                                │                                                         Description                                                         │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ commerce-pickers/src/main/resources/hcm-config/configuration/main.yaml                             │ Declares the 3 UI extension packages with all Discovery endpoint URLs and account defaults injected as extension properties │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/application/.../ui-extensions/product-picker-ui-extension.yaml         │ HCM config for the product picker UI extension                                                                              │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/application/.../ui-extensions/category-picker-ui-extension.yaml        │ HCM config for the category picker UI extension                                                                             │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/application/.../ui-extensions/product-variant-picker-ui-extension.yaml │ HCM config for the product variant picker UI extension                                                                      │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/application/.../ui-extensions/widget-picker-ui-extension.yaml          │ HCM config for the widget picker UI extension                                                                               │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/application/.../brsm-widget-types.yaml                                 │ Value list resource bundle defining Discovery widget type options                                                           │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/best/site/.../channel/smAccountId.yaml                                                        │ Channel parameter definition for smAccountId                                                                                │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/repository-data/common/site/.../blueprint-blank-channel.yaml                                  │ Blueprint channel declaring smAccountId and graphqlTenantName as channel params                                             │
├────────────────────────────────────────────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/conf/platform-dev.properties                                                                  │ Dev defaults: GraphQL base URL, tenant name, account ID, domain key                                                         │
└────────────────────────────────────────────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

  ---
Management APIs (Shared Responsibility)

┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                        File                                                        │                                              Description                                              │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ management-apis/ui/src/app/integrations/shared/constants/available-integrations.json                               │ Lists all connectable integrations including Discovery; drives the Setup App UI                       │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ management-apis/ui/src/app/integrations/ui-extension/services/ui-extension-form.service.ts                         │ Form logic for configuring a UI extension integration (shared across all pickers including Discovery) │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ management-apis/ui/src/app/integrations/ui-extension/components/integration-detail/integration-detail.component.ts │ Displays configured integration details including Discovery-specific fields                           │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ management-apis/ui/src/app/integrations/shared/services/config-schema.service.ts                                   │ Validates integration config schemas (shared, covers Discovery schema)                                │
├────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ management-apis/ui/src/app/integrations/shared/models/api/open-ui-extension/ui-extension.ts                        │ UiExtension model shared across all extension-based integrations                                      │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────┘

  ---
End-to-End Tests

┌───────────────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 File                                  │                                          Description                                           │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../packageData/Discovery.java                         │ Constants for Discovery test data (account IDs, GraphQL URL, widget URL, connector ID, realms) │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../CommercePickersIntegrationSteps.java               │ Cucumber step definitions for the Commerce Pickers integration setup flow                      │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../CommercePickersIntegration.feature                 │ Feature file for installing/configuring Discovery pickers via Setup App                        │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../ReferenceSPACommerceFunctionalities.feature        │ E2E tests for product/category/widget picker functionality in the SPA                          │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../FieldExtensionWithIntegrations.feature             │ Tests for Discovery fields in document type editor                                             │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../NewAccountUserBrSMGlobalExpectedMenuStructure.json │ Expected nav menu structure when a Discovery account is linked                                 │
├───────────────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────┤
│ saas/e2e-tests/.../ITTestBloomreachIntegrationAPI.java                │ Integration API tests including Discovery package registration                                 │
└───────────────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────────┘

  ---
CI/CD

┌───────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────┐
│             File              │                                   Description                                   │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┤
│ saas/Jenkinsfile.brsm_brx_uui │ Jenkins pipeline that runs the 3 UUI Discovery test jobs against a deployed CMS │
└───────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────┘