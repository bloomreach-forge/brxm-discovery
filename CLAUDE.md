# brxm-discovery - Bloomreach Discovery Plugin for brXM

## Project Overview
A brXM PaaS plugin that integrates Bloomreach Discovery with brXM. Discovery is the search/recommendations brain; brXM is the content and page composition layer. The plugin provides CRISP-backed services and native HST components for search, category browse, and recommendations. Product catalog sync is handled externally by commerce connectors - this plugin is read-only against Discovery.

## Module Layout
```
brxm-discovery/              (aggregator POM, packaging=pom)
├── commons/                 (brxm-discovery-commons - exceptions, DiscoveryConfig, DiscoveryConfigReader)
├── cms/                     (brxm-discovery-cms - CMS node types, Open UI extensions, picker daemon)
├── hcm-site/                (brxm-discovery-hcm-site - HCM bootstrap: node types, catalog, CRISP resolvers, FTL templates)
└── site/                    (brxm-discovery-site - domain model, services, CRISP, HST components)
```

## Tech Stack
- **Java 17** (LTS) - use records, sealed classes, switch expressions, `.toList()`
- **brXM / Hippo CMS 16.7.0** - parent POM: `hippo-cms7-project:16.7.0`
- **CRISP API** - REST resource broker for Discovery API calls
- **HST** - site delivery framework (hst-api, hst-commons, hst-client)
- **JUnit 5 + Mockito 5** - testing
- **Maven** - build tool (no wrapper; use `mvn` directly)

## Build Commands
```bash
mvn clean compile          # Compile all modules
mvn clean test             # Run tests (site + cms + shared)
mvn clean test -pl site    # Run site module tests only
mvn clean test -Dtest=FooTest -pl site   # Run a single test
```

## Package Structure
```
org.bloomreach.forge.discovery
├── (shared)
│   ├── exception/                   # DiscoveryException (sealed) → SearchException,
│   │                                #   RecommendationException, ConfigurationException
│   ├── config/                      # DiscoveryConfigReader, ConfigDefaults
│   ├── config/model/                # DiscoveryConfig (record)
│   └── search/model/                # SearchQuery (shared between site + cms)
├── site
│   ├── service/discovery/           # DiscoveryClient (interface), DiscoveryClientImpl,
│   │                                #   DiscoveryResponseMapper
│   ├── service/discovery/dto/       # SearchApiResponse, ApiResponseBody, ProductDoc,
│   │                                #   FacetCounts, FacetFieldDto, RecommendationResponse,
│   │                                #   WidgetListResponse, AutosuggestResponse, etc.
│   ├── service/discovery/search/model/  # CategoryQuery, SearchResult, Facet,
│   │                                   #   FacetValue, PaginationModel, ProductSummary,
│   │                                   #   AutosuggestQuery, AutosuggestResult
│   ├── service/discovery/search/    # QueryParamParser
│   ├── service/discovery/recommendation/model/  # RecQuery, WidgetInfo
│   ├── service/discovery/recommendation/        # DiscoveryWidgetService/Impl
│   ├── service/discovery/config/    # DiscoveryConfigProvider, CachingDiscoveryConfigProvider
│   ├── service/discovery/pixel/     # DiscoveryPixelService (interface), DiscoveryPixelServiceImpl
│   ├── service/discovery/sor/       # SoREnrichmentProvider (interface; integrators implement)
│   ├── platform/                    # HstDiscoveryService, DiscoveryRequestCache
│   ├── component/                   # AbstractDiscoveryComponent + all HST components (flat):
│   │                                #   DiscoverySearchGridComponent (search results),
│   │                                #   DiscoveryCategoryGridComponent (category browse),
│   │                                #   DiscoverySearchInputComponent (search bar + autosuggest),
│   │                                #   AbstractDiscoveryRecommendationComponent,
│   │                                #     DiscoveryProductRecommendationComponent,
│   │                                #     DiscoveryCategoryRecommendationComponent,
│   │                                #     DiscoveryGlobalRecommendationComponent,
│   │                                #   DiscoveryProductDetailComponent,
│   │                                #   DiscoveryProductHighlightComponent,
│   │                                #   DiscoveryCategoryHighlightComponent
│   └── component/info/              # DiscoverySearchGridComponentInfo,
│                                    #   DiscoveryCategoryGridComponentInfo,
│                                    #   DiscoverySearchInputComponentInfo,
│                                    #   DiscoveryProductRecommendationComponentInfo,
│                                    #   DiscoveryCategoryRecommendationComponentInfo,
│                                    #   DiscoveryGlobalRecommendationComponentInfo,
│                                    #   DiscoveryProductDetailComponentInfo,
│                                    #   DiscoveryProductHighlightComponentInfo,
│                                    #   DiscoveryCategoryHighlightComponentInfo,
│                                    #   DiscoveryChannelInfo (per-channel credential overrides + pixel flags)
└── cms
    └── JCR node types, Open UI extensions, picker REST endpoints (DiscoveryPickerModule)
        Open UI extensions: discoveryProductPicker, discoveryWidgetPicker, discoveryCategoryPicker,
                            discoveryCategoryProductPreview (inline product count + live thumbnail preview)
```

## Key Conventions
- **All DTOs are records** - immutable, no setters
- **Sealed exception hierarchy** - enables exhaustive pattern matching; all exceptions are `RuntimeException` subtypes
- **All HST/CMS/CRISP deps are `provided` scope** - plugin is a library, host project supplies runtime
- **Constructor injection only** - no field injection (HST components use `HstServices.getComponentManager()`)
- **No `null` returns** - use `Optional<T>` or throw typed exceptions
- **TDD workflow** - RED → GREEN → REFACTOR for new features and logic changes

## Architecture
- **Discovery is read-only** - external commerce system feeds products into Discovery via connectors
- **CRISP resource spaces**: `discoverySearchAPI` (core.dxpapi.com), `discoveryPathwaysAPI` (pathways.dxpapi.com), `discoveryAutosuggestAPI` (suggest.dxpapi.com) - all three bootstrapped automatically by the plugin via `brxdis-crisp.yaml` in the CMS HCM config; no manual CRISP configuration required in the host project
- **Picker REST endpoints**: `GET /search`, `/items`, `/categories`, `/browse`, `/widgets`, `/category-products` - all at `{cms}/ws/discovery/picker/`; `/category-products` reads `brxdis:categoryId` from the handle's variant child (not the handle itself); also accepts direct `categoryId` param to bypass JCR for live pre-save preview
- **postMessage cross-field sync**: `picker-field.js` fires `cfg.onValueChange(value, documentId)` on pick/clear; `category-picker.html` broadcasts `{type:"brxdis:categoryChanged", documentId, categoryId}` to all same-origin sibling frames via `window.parent.frames`; `category-product-preview.html` listens and re-fetches immediately - no polling, no JCR save required
- **Config resolution** (single global node): all channels share one `brxdis:discoveryConfig` node at `/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig`; credentials use env→sys→JCR value; structural config uses JCR→coded default; no `discoveryConfigPath` mount parameter
- **Global JCR config node**: fixed path `ConfigDefaults.CONFIG_NODE_PATH`; `DiscoveryConfigProvider.get(session)` resolves it; `CachingDiscoveryConfigProvider` implements `EventListener` directly and invalidates cache on node changes; `DiscoveryChannelInfo` carries per-channel credential override fields (`discoveryAccountId`, `discoveryDomainKey`, `discoveryApiKeyEnvVar`, `discoveryAuthKeyEnvVar`) AND pixel tracking flags
- **Graceful degradation**: missing or absent JCR config node falls back to env/sys + coded defaults - no crash
- **v1/v2 auto-selection**: if `authKey` present → v2 Pathways API (`discoveryPathwaysAPI`); otherwise → v1 (`discoverySearchAPI`)
- **Request-scoped caching**: `DiscoveryRequestCache` deduplicates API calls within a single page render; config served from `CachingDiscoveryConfigProvider` (JVM-lifetime cache, JCR-observation-invalidated directly via `EventListener` - no per-request JCR reads)
- **Page Model API**: all components call `request.setModel()` for headless/SPA consumption and `request.setAttribute()` for FTL. Every component also sets `editMode: boolean`. The TypeScript contract lives in `src/types/discovery.ts` in the frontend project.
- **HST component lookup**: `HstServices.getComponentManager().getComponent(ClassName.class.getName())`
- **CRISP broker lookup**: always lazy via `HippoServiceRegistry.getService(ResourceServiceBroker.class)` - never eagerly in constructors
- **JCR system sessions**: obtained via `HippoServiceRegistry.getService(HippoRepository.class).login(...)` - NOT via the `javax.jcr.Repository` HST bean (which is the pooled delivery repo and rejects system credentials)

## Page Model Contract

All model keys are constants in `DiscoveryModelKeys`. Frontend types are in `src/types/discovery.ts`.

| Component | TypeScript type | Key model fields set |
|---|---|---|
| `DiscoverySearchGridComponent` | `ProductGridModels` | `dataSourceMode:"search"`, `query`, `products`, `pagination`, `facets`*, `facetUrls`*, `activeFacets`*, `clearAllFiltersUrl`*, `pageUrls`*, `sortUrl`*, `sortOptions`*, `didYouMean`, `autoCorrectQuery`, `redirectUrl`, `redirectQuery`, `campaign` |
| `DiscoveryCategoryGridComponent` | `ProductGridModels` | `dataSourceMode:"category"`, `document`, `categoryId`, `displayName`, `products`, `pagination`, `facets`*, `facetUrls`*, `activeFacets`*, `clearAllFiltersUrl`*, `pageUrls`*, `sortUrl`*, `sortOptions`*, `campaign`, `stats` |
| `DiscoverySearchInputComponent` | `SearchInputModels` | `query`, `placeholder`, `resultsPage`, `suggestionsEnabled`, `minChars`, `debounceMs`, `autosuggestResult` |
| `DiscoveryProductDetailComponent` | `ProductDetailModels` | `product` (`ProductSummary\|null`), `pid`, `document` |
| `DiscoveryProductRecommendationComponent` | `RecommendationModels` | `products`, `widgetId`, `widgetType`, `widgetResultId`, `widgetQuery` (pid), `showPrice`, `showDescription`, `document` |
| `DiscoveryCategoryRecommendationComponent` | `RecommendationModels` | same as above; `widgetQuery` is category ID |
| `DiscoveryGlobalRecommendationComponent` | `RecommendationModels` | `products`, `widgetId`, `widgetType`, `widgetResultId`, `showPrice`, `showDescription`, `document` |
| `DiscoveryProductHighlightComponent` | `ProductHighlightModels` | `products` (list, nulls for empty slots) |
| `DiscoveryCategoryHighlightComponent` | `CategoryHighlightModels` | `categories` (list of `{categoryId, displayName, productPreviewCount}`), `previewProducts` (map categoryId → ProductSummary[]) |

`*` — only present when the corresponding component parameter (`showFacets`, `showPagination`, `showSort`) is enabled.

**`ProductSummary` shape** (Java record → JSON):
- `id`, `title`, `url`, `imageUrl`, `price` (BigDecimal → number\|null), `currency`
- `attributes`: open map — backend passes whatever fields were in the `fl` param. Common keys: `brand`, `description`, `sale_price`, `thumb_image`.

**`PaginationModel`**: `{ total, page (0-based), pageSize, totalPages }`

## Dependency Scopes
| Dependency | Scope |
|---|---|
| CRISP API, HST (api/commons/client), Repository API, CMS API, JCR, Jackson, SLF4J, spring-beans | provided |
| JUnit 5, Mockito, CRISP Mock | test |

## Repositories
- `https://maven.bloomreach.com/maven2/` (public)
- `https://maven.bloomreach.com/maven2-enterprise/` (enterprise)
