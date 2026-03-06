# brxm-discovery — Bloomreach Discovery Plugin for brXM

## Project Overview
A brXM PaaS plugin that integrates Bloomreach Discovery with brXM. Discovery is the search/recommendations brain; brXM is the content and page composition layer. The plugin provides CRISP-backed services and native HST components for search, category browse, and recommendations. Product catalog sync is handled externally by commerce connectors — this plugin is read-only against Discovery.

## Module Layout
```
brxm-discovery/              (aggregator POM, packaging=pom)
├── cms/                     (brxm-discovery-cms — CMS node types, Open UI extensions)
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
mvn clean test             # Run tests
mvn clean test -pl site    # Run site module tests only
mvn clean test -Dtest=FooTest -pl site   # Run a single test
```

## Package Structure
```
org.bloomreach.forge.discovery
├── site
│   ├── service/
│   │   ├── search/model/        # SearchQuery, CategoryQuery, SearchResult, Facet, FacetValue,
│   │   │                        #   PaginationModel, ProductSummary
│   │   ├── search/              # QueryParamParser (toSearchQuery + toCategoryQuery, both with fallbacks)
│   │   ├── recommendation/model/# RecQuery, WidgetInfo
│   │   ├── recommendation/      # DiscoveryWidgetService/Impl
│   │   └── config/              # DiscoveryConfig (model), ConfigDefaults, DiscoveryConfigReader,
│   │                            #   DiscoveryConfigResolver, DiscoveryConfigProvider,
│   │                            #   CachingDiscoveryConfigProvider, DiscoveryConfigJcrListener
│   ├── api/                     # DiscoveryApiClient, ResourceMapper
│   ├── platform/                # HstDiscoveryService, DiscoveryRequestCache
│   ├── component/               # AbstractDiscoveryComponent,
│   │                            #   DiscoverySearchComponent, DiscoveryCategoryComponent,
│   │                            #   DiscoveryRecommendationComponent (data-fetching),
│   │                            #   DiscoveryProductGridComponent, DiscoveryFacetComponent,
│   │                            #   DiscoveryPaginationComponent (view/composable)
│   ├── component/info/          # DiscoverySearchComponentInfo, DiscoveryCategoryComponentInfo,
│   │                            #   DiscoveryRecommendationComponentInfo, DiscoveryDataSourceComponentInfo
│   └── exception/               # DiscoveryException (sealed) → SearchException,
│                                #   RecommendationException, ConfigurationException
└── cms
    └── JCR node types, Open UI extension, picker REST endpoints (DiscoveryPickerModule)
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
- **CRISP resource spaces**: `discoverySearchAPI` (core.dxpapi.com) + `discoveryPathwaysAPI` (pathways.dxpapi.com) — configured in host project
- **Config resolution** (two-tier): credentials (accountId, domainKey, apiKey, authKey, environment) use env→sys→JCR; structural config (baseUri, paths, pageSize) uses JCR→coded default; `discoveryConfigPath` mount param → module config JCR node
- **Graceful degradation**: missing or absent JCR config node falls back to env/sys + coded defaults — no crash
- **v1/v2 auto-selection**: if `authKey` present → v2 Pathways API (`discoveryPathwaysAPI`); otherwise → v1 (`discoverySearchAPI`)
- **Request-scoped caching**: `DiscoveryRequestCache` deduplicates API calls within a single page render; config served from `CachingDiscoveryConfigProvider` (JVM-lifetime cache, JCR-observation-invalidated — no per-request JCR reads)
- **Page Model API**: all components call `request.setModel()` for headless/SPA consumption and `request.setAttribute()` for FTL
- **HST component lookup**: `HstServices.getComponentManager().getComponent(ClassName.class.getName())`

## Dependency Scopes
| Dependency | Scope |
|---|---|
| CRISP API, HST (api/commons/client), Repository API, CMS API, JCR, Jackson, SLF4J | provided |
| JUnit 5, Mockito, CRISP Mock | test |

## Repositories
- `https://maven.bloomreach.com/maven2/` (public)
- `https://maven.bloomreach.com/maven2-enterprise/` (enterprise)
