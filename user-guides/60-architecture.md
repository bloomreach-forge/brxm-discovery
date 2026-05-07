# Architecture & Dependency Injection Guide

This guide documents the plugin's internal architecture for contributors and maintainers. It covers the module layout, Spring bean wiring, service lookup chain, caching strategy, and the rationale behind each design decision.

---

## Module layout

```
brxm-discovery/
├── commons/   brxm-discovery-commons
│   Domain model, config resolution, exceptions.
│   Zero framework dependencies (only JCR API as provided).
│   Depended on by both site and cms.
│
├── cms/       brxm-discovery-cms
│   CMS daemon module (picker REST endpoints, Open UI extensions).
│   Depends on commons only - never imports from site.
│
└── site/      brxm-discovery-site
    HST components, Discovery API clients, HTTP transport, pixel tracking.
    Depends on commons. All HST deps are provided scope.
```

**Dependency direction:** `site → commons ← cms`. The cms and site modules never import each other. The commons module defines the domain contract that both runtimes consume.

---

## Layer architecture

The plugin follows Clean Architecture principles. Dependencies flow inward - outer layers import inner layers, never the reverse.

```
┌─ PRESENTATION ──────────────────────────────────────────────┐
│                                                              │
│  AbstractDiscoveryComponent                                  │
│  ├── DiscoverySearchGridComponent                            │
│  ├── DiscoveryCategoryGridComponent                          │
│  ├── DiscoverySearchInputComponent                           │
│  ├── AbstractDiscoveryRecommendationComponent                │
│  │   ├── DiscoveryProductRecommendationComponent             │
│  │   ├── DiscoveryCategoryRecommendationComponent            │
│  │   └── DiscoveryGlobalRecommendationComponent              │
│  ├── DiscoveryProductDetailComponent                         │
│  ├── DiscoveryProductHighlightComponent                      │
│  └── DiscoveryCategoryHighlightComponent                     │
│                                                              │
│  ComponentInfo interfaces, DiscoveryModelKeys                │
│  Content beans (DiscoveryCategoryBean, DiscoveryProduct…)    │
│  FTL templates (brxdis-results.ftl, etc.)                    │
│                                                              │
├─ PLATFORM / HST ADAPTER ────────────────────────────────────┤
│                                                              │
│  HstDiscoveryService             (facade - single entry pt)  │
│  DiscoveryRuntimeContextFactory  (per-request context)       │
│  DiscoveryRuntimeContext         (immutable request context)  │
│  DiscoveryRequestCache           (request-scoped dedup)      │
│  CategoryPreviewCache            (JVM-level TTL cache)       │
│  DiscoveryBrUid2Service          (cookie/tracking ID mgmt)   │
│                                                              │
├─ SERVICE / INFRASTRUCTURE ──────────────────────────────────┤
│                                                              │
│  DiscoveryClientImpl             (thin coordinator)          │
│  SearchApiClientImpl             (search / category / PDP)   │
│  AutosuggestApiClientImpl        (typeahead)                 │
│  RecommendationApiClientImpl     (Pathways recommendations)  │
│  SearchResponseMapper, AutosuggestResponseMapper,            │
│    RecommendationResponseMapper  (JSON → domain records)     │
│  QueryParamParser                (URL → query objects)       │
│  DiscoveryPixelServiceImpl       (async pixel dispatch)      │
│  Shared DTOs: ProductDoc, ProductDocMapper, ApiResponseBody  │
│  Domain DTOs: search/dto/, autosuggest/dto/, rec/dto/        │
│                                                              │
├─ DOMAIN / SHARED ───────────────────────────────────────────┤
│                                                              │
│  Records: DiscoveryConfig, DiscoveryCredentials, Settings    │
│  Queries: SearchQuery, CategoryQuery, RecQuery, Autosuggest  │
│  Results: SearchResult, SearchResponse, ProductSummary, …    │
│  Exceptions: DiscoveryException (sealed)                     │
│    ├── SearchException                                       │
│    ├── RecommendationException                               │
│    └── ConfigurationException                                │
│  Config: DiscoveryConfigReader, ConfigDefaults, Resolver     │
│  Cache: CachingDiscoveryConfigProvider, ConfigJcrListener     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Key principle:** Components are thin. They extract HST parameters, delegate to `HstDiscoveryService`, and set model values. All business logic (query building, API calls, caching, pixel events, enrichment) lives in the platform and service layers.

---

## Spring bean inventory

All site-module beans are defined in a single XML file, loaded into a dedicated addon child `ApplicationContext` via the brXM addon module pattern:

**`site/src/main/resources/META-INF/hst-assembly/addon/brxm-discovery/brxm-discovery-site.xml`**

The addon module descriptor at `META-INF/hst-assembly/addon/module.xml` declares the module name `org.bloomreach.forge.discovery.site` and two `classpath*:` config-locations (the plugin beans + an `overrides/` path for integrators). `ModuleDescriptorUtils.collectAllModuleDefinitions()` finds all `module.xml` files across JARs via `ClassLoader.getResources()` (plural), so the plugin beans are always loaded regardless of JAR order on the classpath - unlike the `overrides/` pattern, which only scans the first matching directory.

| Bean ID | Class | Constructor args | Lifecycle | Lazy |
|---------|-------|------------------|-----------|------|
| `brxmdis.configReader` | `DiscoveryConfigReader` | - | - | no |
| `brxmdis.configProvider` | `DiscoveryConfigProviderBridge` | configReader | `start` / `close` | no |
| `brxmdis.httpClient` | `DiscoveryHttpClientFactory` | *(factory-method: `create()`)* | - | no |
| `brxmdis.transport` | `HttpClientDiscoveryTransport` | httpClient | - | no |
| `brxmdis.requestFactory` | `DiscoveryRequestFactory` | - | - | no |
| `brxmdis.objectMapper` | `ObjectMapper` | - | - | no |
| `brxmdis.searchResponseMapper` | `SearchResponseMapper` | objectMapper | - | no |
| `brxmdis.autosuggestResponseMapper` | `AutosuggestResponseMapper` | objectMapper | - | no |
| `brxmdis.recommendationResponseMapper` | `RecommendationResponseMapper` | objectMapper | - | no |
| `brxmdis.searchApiClient` | `SearchApiClientImpl` | transport, configProvider, searchResponseMapper, requestFactory | - | yes |
| `brxmdis.autosuggestApiClient` | `AutosuggestApiClientImpl` | transport, configProvider, autosuggestResponseMapper, requestFactory | - | yes |
| `brxmdis.recommendationApiClient` | `RecommendationApiClientImpl` | transport, configProvider, recommendationResponseMapper, requestFactory | - | yes |
| `brxmdis.pixelClientTransport` | `DefaultDiscoveryPixelTransport` | transport, configProvider | - | yes |
| `brxmdis.discoveryClient` | `DiscoveryClientImpl` | searchApiClient, autosuggestApiClient, recommendationApiClient, pixelClientTransport | - | yes |
| `brxmdis.pixelExecutor` | `ThreadPoolTaskExecutor` | *(property injection)* | - | no |
| `brxmdis.pixelService` | `DiscoveryPixelServiceImpl` | discoveryClient, pixelExecutor | - | yes |
| `brxmdis.runtimeContextFactory` | `DiscoveryRuntimeContextFactory` | configProvider, brUid2Service | - | yes |
| `o.b.f.d.s.platform.DiscoveryBrUid2Service` | `DiscoveryBrUid2Service` | - | - | yes |
| `o.b.f.d.s.platform.CategoryPreviewCache` | `CategoryPreviewCache` | - | - | yes |
| `o.b.f.d.s.platform.HstDiscoveryService` | `HstDiscoveryService` | discoveryClient, runtimeContextFactory, pixelService, `<null/>` (SoR), `<null/>` (consent) | - | yes |
| `brxmdis.sorEnrichmentProvider` | *(integrator-supplied)* | — | optional SPI; override in your assembly | — |
| `brxmdis.pixelConsentProvider` | *(integrator-supplied)* | — | optional SPI; override in your assembly | — |

**Naming convention:** Internal beans use the `brxmdis.` prefix. Beans that must be discoverable via `HstServices.getComponentManager().getComponent(Class, MODULE_NAME)` use the fully-qualified class name as their bean ID.

**No annotation-based injection.** Every dependency is wired via Spring XML constructor args. This is explicit and makes the entire dependency graph visible in one file.

**Addon child context.** The addon module's `ApplicationContext` has the main HST context as its parent. All intra-module bean refs work normally; beans in the main context (Spring framework beans, etc.) are resolved via parent-delegation. Components call `cm.getComponent(type, "org.bloomreach.forge.discovery.site")` to reach into the child context.

---

## Bean dependency graph

```
HstDiscoveryService  ←  [components call lookupService(HstDiscoveryService.class)]
├── DiscoveryClientImpl  (thin coordinator — delegates to 3 sub-clients)
│   ├── SearchApiClientImpl
│   │   ├── SearchResponseMapper → ObjectMapper
│   │   └── HttpClientDiscoveryTransport  ←  [Spring singleton]
│   ├── AutosuggestApiClientImpl
│   │   ├── AutosuggestResponseMapper → ObjectMapper
│   │   └── HttpClientDiscoveryTransport
│   └── RecommendationApiClientImpl
│       ├── RecommendationResponseMapper → ObjectMapper
│       └── HttpClientDiscoveryTransport
├── DiscoveryRuntimeContextFactory
│   ├── CachingDiscoveryConfigProvider
│   │   └── DiscoveryConfigReader          ← resolve + readWithDefaults + applyEnvSysCredentials
│   └── DiscoveryBrUid2Service
├── DiscoveryPixelServiceImpl
│   ├── DiscoveryClientImpl (as DiscoveryPixelTransport)
│   └── ThreadPoolTaskExecutor (core=2, max=4, queue=256, DiscardOldestPolicy, prefix=brxdis-pixel-)
├── SoREnrichmentProvider     →  <null/> (optional; register brxmdis.sorEnrichmentProvider)
└── PixelConsentProvider      →  <null/> (optional; register brxmdis.pixelConsentProvider)

CachingDiscoveryConfigProvider also implements EventListener:
└── observes /hippo:configuration for brxdis:discoveryConfig changes (start/close lifecycle)
    → calls invalidate()

Standalone utility beans:
└── CategoryPreviewCache  ←  [components call lookupService(CategoryPreviewCache.class)]
```

**No circular dependencies.** The graph is a strict DAG.

---

## Service lookup chain

HST instantiates component classes via reflection - they are not Spring beans, so constructor/field injection is impossible. Components obtain services at render-time via `AbstractDiscoveryComponent.lookupService(Class<T>)`:

```
lookupService(HstDiscoveryService.class)
  │
  └─ componentManager.getComponent(HstDiscoveryService.class, MODULE_NAME)
        → MODULE_NAME = "org.bloomreach.forge.discovery.site"
        → queries the addon child ApplicationContext directly
        → ConfigurationException if ComponentsException or null
```

Because beans live in the addon child context, the module name is required. `SpringComponentManager.getComponent(type, moduleName)` routes to `moduleInstance.getComponent(type)` and throws `ComponentsException` (which subclasses `ModuleNotFoundException`) if the module is absent.

---

## Component render lifecycle

`AbstractDiscoveryComponent.doBeforeRender` is `final`. Subclasses override `doDiscoveryBeforeRender` instead:

```
doBeforeRender(request, response)          ← final; called by HST
  ├── super.doBeforeRender(...)
  ├── sets editMode model
  └── try {
        doDiscoveryBeforeRender(request, response)   ← override this
      } catch (DiscoveryException e) {
        log.warn(...)
        if editMode → request.setAttribute("brxdis_warning", message)
      }
```

This means a transient Discovery API failure or misconfiguration in one component cannot propagate a 500 to the whole page. In Channel Manager / Experience Editor preview, `brxdis_warning` is set so the FTL template can surface an in-place notice to the author:

```ftl
<#if brxdis_warning??>
  <div style="border:2px dashed #f59e0b;padding:1rem;color:#92400e">⚠ ${brxdis_warning}</div>
</#if>
```

All bundled templates include this block. Custom templates should include it too.

---

## HTTP transport

All Discovery API calls go through `HttpClientDiscoveryTransport`, a Spring singleton wrapping a `java.net.http.HttpClient` created by `DiscoveryHttpClientFactory.create()`:

- **Executor:** `Executors.newVirtualThreadPerTaskExecutor()` — one virtual thread per in-flight request; no fixed pool to tune.
- **Protocol:** HTTP/2 with fallback to HTTP/1.1.
- **Connect timeout:** 10 s.
- **Redirects:** `NORMAL` (follows redirects automatically).

The `HttpClient` is shared across all three sub-clients and the pixel transport. It is created once at Spring context startup and is thread-safe.

---

## Pixel executor

Pixel events are dispatched asynchronously through `brxmdis.pixelExecutor`, a dedicated `ThreadPoolTaskExecutor` separate from the HTTP transport executor:

| Property | Value |
|---|---|
| Core pool size | 2 |
| Max pool size | 4 |
| Queue capacity | 256 |
| Thread name prefix | `brxdis-pixel-` |
| Rejection policy | `DiscardOldestPolicy` — oldest queued pixel is silently dropped when the queue is full |

This executor is **not** the same as the virtual-thread transport executor used for API calls. Pixel calls run on bounded platform threads so that a flood of pixel events cannot exhaust virtual thread resources.

---

## CMS config provider

The CMS webapp needs `DiscoveryConfigProvider` for picker REST endpoints. `DiscoveryPickerModule` (implements `DaemonModule`) creates its own `CachingDiscoveryConfigProvider` instance in `initialize()`, independent from the site webapp's instance.

```
initialize(Session moduleSession)
  ├── Creates CachingDiscoveryConfigProvider; calls start()
  └── Registers DiscoveryConfigProvider in HippoServiceRegistry
      → DiscoveryPickerResource (JAX-RS) uses this instance

shutdown()
  ├── Calls CachingDiscoveryConfigProvider.close()
  └── Unregisters from HippoServiceRegistry
```

The site Spring XML creates its own `DiscoveryConfigProviderBridge` bean (`brxmdis.configProvider`) independently — it does not share the CMS instance.

---

## Caching strategy

The plugin uses three independent caching tiers:

### Tier 1 - Configuration cache (JVM lifetime, observation-invalidated)

**Class:** `CachingDiscoveryConfigProvider`

- Reads `DiscoveryConfig` from JCR on first access, caches indefinitely.
- `CachingDiscoveryConfigProvider` implements `EventListener` directly and observes `/hippo:configuration` for `brxdis:discoveryConfig` node changes → calls `invalidate()`. The observation session is started in `start()` and released in `close()` (Spring lifecycle).
- Environment variable and system property overrides are applied on every read (not cached), so env changes take effect without restart.
- Thread-safe via double-checked locking on a `volatile` field.

### Tier 2 - Request-scoped cache (single page render)

**Class:** `DiscoveryRequestCache`

- Static methods using `HstRequestContext.setAttribute()` / `getAttribute()`.
- Prevents duplicate Discovery API calls when multiple components on the same page need the same data.
- Keys are namespaced: `org.bloomreach.forge.discovery.requestCache.{suffix}`.
- Recommendation cache key includes `widgetId + query.hashCode()` for per-widget dedup.
- Also stores the resolved product (PDP component) so sibling recommendation components can read the PID.

### Tier 3 - Category preview cache (JVM-level, TTL-based)

**Class:** `CategoryPreviewCache`

- `ConcurrentHashMap<Key, Entry>` with 5-minute TTL + 20% jitter.
- Key: `(categoryId, count)`.
- Prevents per-request Discovery API calls for category highlight product thumbnails.
- Passive eviction on `put()` - no background thread.
- Jitter prevents synchronized cache stampede across a cluster.

---

## CMS daemon module

`DiscoveryPickerModule` implements `DaemonModule` (brXM lifecycle interface for CMS-side services):

```
initialize(Session moduleSession)
  ├── Creates DiscoveryConfigReader
  ├── Creates CachingDiscoveryConfigProvider (independent instance); calls start()
  ├── Registers DiscoveryConfigProvider in HippoServiceRegistry
  └── Registers DiscoveryPickerResource as JAX-RS endpoint
      at {cms}/ws/discovery/picker/

shutdown()
  ├── Unregisters JAX-RS endpoint
  ├── Calls CachingDiscoveryConfigProvider.close() (removes JCR listener)
  └── Unregisters DiscoveryConfigProvider from HippoServiceRegistry
```

The CMS module depends only on `commons` - it never imports from `site`. It creates its own config provider instance because it runs in a different classloader.

---

## Design patterns reference

| Pattern | Implementation | Why |
|---------|---------------|-----|
| **Facade** (GoF) | `HstDiscoveryService` | Single entry point for components; absorbs config resolution, query building, caching, pixel, enrichment |
| **Service Locator** | `AbstractDiscoveryComponent.lookupService()` (2-step: typed → name) | HST instantiates components outside Spring; field injection is impossible |
| **Factory Method** | `DiscoveryHttpClientFactory.create()` | `HttpClient` built once with virtual thread executor; shared across all transports |
| **Request-Scoped Cache** | `DiscoveryRequestCache` via `HstRequestContext` attributes | Dedup API calls across sibling components in one page render |
| **Observer** | `CachingDiscoveryConfigProvider` implements `EventListener` directly | Invalidate config cache on CMS save without restart; JCR listener lifecycle co-located with the cache |
| **Sealed Types** | `DiscoveryException` hierarchy | Exhaustive error handling via pattern matching |
| **Records** | All queries, results, config, DTOs | Immutability, value semantics, no boilerplate |

---

## Testing strategy

Components are tested without a Spring context or HST container:

1. **Subclass override pattern:** Test creates a subclass that overrides `lookupService()` to return a mock, and overrides `getComponentParametersInfo()` to return a stub.
2. **Request context simulation:** `HstRequestContext.getAttribute/setAttribute` mocked via `HashMap` + `doAnswer`.
3. **Transport mocking:** `DiscoveryTransport` is mocked at the interface level. Response payloads are built with a real `ObjectMapper`.
4. **No `mockStatic`:** The project uses `mockito-core` (not `mockito-inline`). All static dependencies are accessed through overridable instance methods.

---

## Potential future improvements

These are flagged for awareness, not planned for implementation:

| Observation | Impact | Action threshold |
|-------------|--------|-----------------|
| `HstDiscoveryService` deferred pixel methods (~80 lines) are a distinct concern | SRP | Extract if the class exceeds ~400 lines |
| `DiscoveryRuntimeContextFactory` name undersells its responsibilities | Readability | Rename to `DiscoveryRequestContextBuilder` if refactoring the area |
| `SearchQuery` has 4 backwards-compatible constructors | Maintenance | Switch to builder if constructor count reaches 6+ |
| No interface for `DiscoveryRuntimeContextFactory`, `CategoryPreviewCache` | Flexibility | Add interface only if a second implementation is needed |
