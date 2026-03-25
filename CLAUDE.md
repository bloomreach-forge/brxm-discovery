# brxm-discovery — Bloomreach Discovery Plugin for brXM

## Project Overview
A brXM PaaS plugin that integrates Bloomreach Discovery with brXM. Discovery is the search/recommendations brain; brXM is the content and page composition layer. The plugin provides CRISP-backed services and native HST components for search, category browse, and recommendations. Product catalog sync is handled externally by commerce connectors — this plugin is read-only against Discovery.

## Module Layout
```
brxm-discovery/              (aggregator POM, packaging=pom)
├── shared/                  (brxm-discovery-shared — exceptions, DiscoveryConfig, DiscoveryConfigReader)
├── cms/                     (brxm-discovery-cms — CMS node types, Open UI extensions, picker daemon)
└── site/                    (brxm-discovery-site — domain model, services, CRISP, HST components)
```

## Tech Stack
- **Java 17** (LTS) — use records, sealed classes, switch expressions, `.toList()`
- **brXM / Hippo CMS 16.6.5** — parent POM: `hippo-cms7-project:16.6.5`
- **CRISP API** — REST resource broker for Discovery API calls
- **HST** — site delivery framework (hst-api, hst-commons, hst-client)
- **JUnit 5 + Mockito 5** — testing
- **Maven** — build tool (no wrapper; use `mvn` directly)

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
│   ├── service/discovery/config/    # DiscoveryConfigResolver, DiscoveryConfigProvider,
│   │                                #   CachingDiscoveryConfigProvider, DiscoveryConfigJcrListener
│   ├── service/discovery/pixel/     # DiscoveryPixelService (interface), DiscoveryPixelServiceImpl
│   ├── service/discovery/sor/       # SoREnrichmentProvider (interface; integrators implement)
│   ├── platform/                    # HstDiscoveryService, DiscoveryRequestCache
│   ├── component/                   # AbstractDiscoveryComponent + all HST components (flat):
│   │                                #   DiscoverySearchComponent, DiscoveryCategoryComponent,
│   │                                #   DiscoveryRecommendationComponent,
│   │                                #   DiscoveryProductDetailComponent,
│   │                                #   DiscoveryProductHighlightComponent,
│   │                                #   DiscoveryCategoryHighlightComponent,
│   │                                #   DiscoveryProductGridComponent (view),
│   │                                #   DiscoveryFacetComponent (view)
│   └── component/info/              # DiscoverySearchComponentInfo,
│                                    #   DiscoveryCategoryComponentInfo,
│                                    #   DiscoveryRecommendationComponentInfo,
│                                    #   DiscoveryDataSourceComponentInfo,
│                                    #   DiscoveryProductDetailComponentInfo,
│                                    #   DiscoveryProductHighlightComponentInfo,
│                                    #   DiscoveryCategoryHighlightComponentInfo
└── cms
    └── JCR node types, Open UI extensions, picker REST endpoints (DiscoveryPickerModule)
        Open UI extensions: discoveryProductPicker, discoveryWidgetPicker, discoveryCategoryPicker,
                            discoveryCategoryProductPreview (inline product count + live thumbnail preview)
```

## Key Conventions
- **All DTOs are records** — immutable, no setters
- **Sealed exception hierarchy** — enables exhaustive pattern matching; all exceptions are `RuntimeException` subtypes
- **All HST/CMS/CRISP deps are `provided` scope** — plugin is a library, host project supplies runtime
- **Constructor injection only** — no field injection (HST components use `HstServices.getComponentManager()`)
- **No `null` returns** — use `Optional<T>` or throw typed exceptions
- **TDD workflow** — RED → GREEN → REFACTOR for new features and logic changes

## Architecture
- **Discovery is read-only** — external commerce system feeds products into Discovery via connectors
- **CRISP resource spaces**: `discoverySearchAPI` (core.dxpapi.com), `discoveryPathwaysAPI` (pathways.dxpapi.com), `discoveryAutosuggestAPI` (suggest.dxpapi.com) — all three bootstrapped automatically by the plugin via `brxdis-crisp.yaml` in the CMS HCM config; no manual CRISP configuration required in the host project
- **Picker REST endpoints**: `GET /search`, `/items`, `/categories`, `/browse`, `/widgets`, `/category-products` — all at `{cms}/ws/discovery/picker/`; `/category-products` reads `brxdis:categoryId` from the handle's variant child (not the handle itself); also accepts direct `categoryId` param to bypass JCR for live pre-save preview
- **postMessage cross-field sync**: `picker-field.js` fires `cfg.onValueChange(value, documentId)` on pick/clear; `category-picker.html` broadcasts `{type:"brxdis:categoryChanged", documentId, categoryId}` to all same-origin sibling frames via `window.parent.frames`; `category-product-preview.html` listens and re-fetches immediately — no polling, no JCR save required
- **Config resolution** (single global node): all channels share one `brxdis:discoveryConfig` node at `/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig`; credentials use env→sys→JCR value; structural config uses JCR→coded default; no `discoveryConfigPath` mount parameter
- **Global JCR config node**: fixed path `ConfigDefaults.CONFIG_NODE_PATH`; `DiscoveryConfigProvider.get(session)` resolves it; `DiscoveryConfigJcrListener` invalidates cache on node changes; `DiscoveryChannelInfo` carries pixel fields only
- **Graceful degradation**: missing or absent JCR config node falls back to env/sys + coded defaults — no crash
- **v1/v2 auto-selection**: if `authKey` present → v2 Pathways API (`discoveryPathwaysAPI`); otherwise → v1 (`discoverySearchAPI`)
- **Request-scoped caching**: `DiscoveryRequestCache` deduplicates API calls within a single page render; config served from `CachingDiscoveryConfigProvider` (JVM-lifetime cache, JCR-observation-invalidated via `DiscoveryConfigJcrListener` — no per-request JCR reads)
- **Page Model API**: all components call `request.setModel()` for headless/SPA consumption and `request.setAttribute()` for FTL
- **HST component lookup**: `HstServices.getComponentManager().getComponent(ClassName.class.getName())`
- **CRISP broker lookup**: always lazy via `HippoServiceRegistry.getService(ResourceServiceBroker.class)` — never eagerly in constructors
- **JCR system sessions**: obtained via `HippoServiceRegistry.getService(HippoRepository.class).login(...)` — NOT via the `javax.jcr.Repository` HST bean (which is the pooled delivery repo and rejects system credentials)

## Dependency Scopes
| Dependency | Scope |
|---|---|
| CRISP API, HST (api/commons/client), Repository API, CMS API, JCR, Jackson, SLF4J, spring-beans | provided |
| JUnit 5, Mockito, CRISP Mock | test |

## Repositories
- `https://maven.bloomreach.com/maven2/` (public)
- `https://maven.bloomreach.com/maven2-enterprise/` (enterprise)
