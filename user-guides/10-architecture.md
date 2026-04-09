# Architecture & Dependency Injection Guide

This guide documents the plugin's internal architecture for contributors and maintainers. It covers the module layout, Spring bean wiring, service lookup chain, caching strategy, and the rationale behind each design decision.

---

## Module layout

```
brxm-discovery/
├── shared/    brxm-discovery-shared
│   Domain model, config resolution, exceptions.
│   Zero framework dependencies (only JCR API as provided).
│   Depended on by both site and cms.
│
├── cms/       brxm-discovery-cms
│   CMS daemon module (picker REST endpoints, Open UI extensions).
│   Depends on shared only — never imports from site.
│
└── site/      brxm-discovery-site
    HST components, Discovery API clients, CRISP wiring, pixel tracking.
    Depends on shared. All HST/CRISP deps are provided scope.
```

**Dependency direction:** `site → shared ← cms`. The cms and site modules never import each other. The shared module defines the domain contract that both runtimes consume.

---

## Layer architecture

The plugin follows Clean Architecture principles. Dependencies flow inward — outer layers import inner layers, never the reverse.

```
┌─ PRESENTATION ──────────────────────────────────────────────┐
│                                                              │
│  AbstractDiscoveryComponent                                  │
│  ├── DiscoveryResultsComponent                               │
│  ├── DiscoverySearchInputComponent                           │
│  ├── DiscoveryRecommendationComponent                        │
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
│  HstDiscoveryService             (facade — single entry pt)  │
│  DiscoveryRuntimeContextFactory  (per-request context)       │
│  DiscoveryRuntimeContext         (immutable request context)  │
│  DiscoveryRequestCache           (request-scoped dedup)      │
│  CategoryPreviewCache            (JVM-level TTL cache)       │
│  DiscoveryBrUid2Service          (cookie/tracking ID mgmt)   │
│  SearchRequestOptions            (value object for params)   │
│                                                              │
├─ SERVICE / INFRASTRUCTURE ──────────────────────────────────┤
│                                                              │
│  DiscoveryClientImpl             (CRISP API client)          │
│  DiscoveryResponseMapper         (JSON → domain records)     │
│  QueryParamParser                (URL → query objects)        │
│  DiscoveryPixelServiceImpl       (async pixel dispatch)      │
│  DTOs: SearchApiResponse, RecommendationResponse, etc.       │
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

The addon module descriptor at `META-INF/hst-assembly/addon/module.xml` declares the module name `org.bloomreach.forge.discovery.site` and two `classpath*:` config-locations (the plugin beans + an `overrides/` path for integrators). `ModuleDescriptorUtils.collectAllModuleDefinitions()` finds all `module.xml` files across JARs via `ClassLoader.getResources()` (plural), so the plugin beans are always loaded regardless of JAR order on the classpath — unlike the `overrides/` pattern, which only scans the first matching directory.

| Bean ID | Class | Constructor args | Lifecycle | Lazy |
|---------|-------|------------------|-----------|------|
| `brxmdis.configReader` | `DiscoveryConfigReader` | — | — | no |
| `brxmdis.responseMapper` | `DiscoveryResponseMapper` | `ObjectMapper` (inline bean) | — | no |
| `brxmdis.discoveryClient` | `DiscoveryClientImpl` | responseMapper | — | yes |
| `brxmdis.configProvider` | `CachingDiscoveryConfigProvider` | configReader | `start` / `close` | no |
| `brxmdis.pixelExecutor` | `ThreadPoolTaskExecutor` | *(property injection)* | — | no |
| `brxmdis.pixelService` | `DiscoveryPixelServiceImpl` | discoveryClient, pixelExecutor | — | yes |
| `brxmdis.runtimeContextFactory` | `DiscoveryRuntimeContextFactory` | configProvider, brUid2Service | — | yes |
| `o.b.f.d.s.platform.DiscoveryBrUid2Service` | `DiscoveryBrUid2Service` | — | — | yes |
| `o.b.f.d.s.platform.CategoryPreviewCache` | `CategoryPreviewCache` | — | — | yes |
| `o.b.f.d.s.platform.HstDiscoveryService` | `HstDiscoveryService` | discoveryClient, runtimeContextFactory, pixelService, `<null/>` | — | yes |

**Naming convention:** Internal beans use the `brxmdis.` prefix. Beans that must be discoverable via `HstServices.getComponentManager().getComponent(Class, MODULE_NAME)` use the fully-qualified class name as their bean ID.

**No annotation-based injection.** Every dependency is wired via Spring XML constructor args. This is explicit and makes the entire dependency graph visible in one file.

**Addon child context.** The addon module's `ApplicationContext` has the main HST context as its parent. All intra-module bean refs work normally; beans in the main context (Spring framework beans, CRISP, etc.) are resolved via parent-delegation. Components call `cm.getComponent(type, "org.bloomreach.forge.discovery.site")` to reach into the child context.

---

## Bean dependency graph

```
HstDiscoveryService  ←  [components call lookupService(HstDiscoveryService.class)]
├── DiscoveryClientImpl
│   ├── ResourceServiceBroker  ←  [HippoServiceRegistry.getService(ResourceServiceBroker.class), lazy on first use]
│   └── DiscoveryResponseMapper
│       └── ObjectMapper (Jackson, inline bean)
├── DiscoveryRuntimeContextFactory
│   ├── CachingDiscoveryConfigProvider
│   │   └── DiscoveryConfigReader          ← resolve + readWithDefaults + applyEnvSysCredentials
│   └── DiscoveryBrUid2Service
├── DiscoveryPixelServiceImpl
│   ├── DiscoveryClientImpl (as DiscoveryPixelTransport)
│   └── ThreadPoolTaskExecutor (core=2, max=4, queue=256)
└── SoREnrichmentProvider  →  <null/> (optional; integrators override)

CachingDiscoveryConfigProvider also implements EventListener:
└── observes /hippo:configuration for brxdis:discoveryConfig changes (start/close lifecycle)
    → calls invalidate()

Standalone utility beans:
└── CategoryPreviewCache  ←  [components call lookupService(CategoryPreviewCache.class)]
```

**No circular dependencies.** The graph is a strict DAG.

---

## Service lookup chain

HST instantiates component classes via reflection — they are not Spring beans, so constructor/field injection is impossible. Components obtain services at render-time via `AbstractDiscoveryComponent.lookupService(Class<T>)`:

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

## CRISP broker resolution

The CRISP `ResourceServiceBroker` is obtained via `CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager())` — the standard CRISP pattern. No custom factory is needed; CRISP handles lifecycle internally.

```xml
<bean id="brxmdis.resourceServiceBroker"
      class="org.onehippo.cms7.crisp.hst.module.CrispHstServices"
      factory-method="getDefaultResourceServiceBroker"
      lazy-init="true">
    <constructor-arg>
        <bean class="org.hippoecm.hst.site.HstServices" factory-method="getComponentManager"/>
    </constructor-arg>
</bean>
```

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

The site Spring XML creates its own `CachingDiscoveryConfigProvider` bean (`brxmdis.configProvider`) independently — it does not share the CMS instance. CRISP resource resolvers in the site webapp use hardcoded default base URIs via `crisp:propvalues` in `brxdis-crisp.yaml`; no `HippoServiceRegistry` bridge is required.

---

## Caching strategy

The plugin uses three independent caching tiers:

### Tier 1 — Configuration cache (JVM lifetime, observation-invalidated)

**Class:** `CachingDiscoveryConfigProvider`

- Reads `DiscoveryConfig` from JCR on first access, caches indefinitely.
- `CachingDiscoveryConfigProvider` implements `EventListener` directly and observes `/hippo:configuration` for `brxdis:discoveryConfig` node changes → calls `invalidate()`. The observation session is started in `start()` and released in `close()` (Spring lifecycle).
- Environment variable and system property overrides are applied on every read (not cached), so env changes take effect without restart.
- Thread-safe via double-checked locking on a `volatile` field.

### Tier 2 — Request-scoped cache (single page render)

**Class:** `DiscoveryRequestCache`

- Static methods using `HstRequestContext.setAttribute()` / `getAttribute()`.
- Prevents duplicate Discovery API calls when multiple components on the same page need the same data.
- Keys are namespaced: `org.bloomreach.forge.discovery.requestCache.{suffix}`.
- Recommendation cache key includes `widgetId + query.hashCode()` for per-widget dedup.
- Also stores the resolved product (PDP component) so sibling recommendation components can read the PID.

### Tier 3 — Category preview cache (JVM-level, TTL-based)

**Class:** `CategoryPreviewCache`

- `ConcurrentHashMap<Key, Entry>` with 5-minute TTL + 20% jitter.
- Key: `(categoryId, count)`.
- Prevents per-request Discovery API calls for category highlight product thumbnails.
- Passive eviction on `put()` — no background thread.
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

The CMS module depends only on `shared` — it never imports from `site`. It creates its own config provider instance because it runs in a different classloader.

---

## CRISP resource resolvers

Five CRISP resource spaces are bootstrapped automatically by the plugin via:

**`cms/src/main/resources/hcm-config/brxdis-crisp.yaml`**

| Resource space | Default base URI | Purpose |
|----------------|----------------|---------|
| `discoverySearchAPI` | `https://core.dxpapi.com` | Search, category browse, product fetch |
| `discoveryPathwaysAPI` | `https://pathways.dxpapi.com` | v2 Pathways recommendations |
| `discoveryAutosuggestAPI` | `https://suggest.dxpapi.com` | Autosuggest / typeahead |
| `discoveryPixelAPI` | `https://p.brsrvr.com` | Pixel events (US) |
| `discoveryPixelAPIEU` | `https://p-eu.brsrvr.com` | Pixel events (EU) |

All five use CRISP's standard `SimpleJacksonRestTemplateResourceResolver` with `crisp:propnames` / `crisp:propvalues` for the base URI. The default value is the production endpoint; override via system property or the JCR `crisp:propvalues` node if needed.

**CRISP caching is disabled** (`cacheEnabled=false`) on all resolvers because every Discovery API URL includes a unique `request_id` UUID — the CRISP cache would never produce a hit.

---

## Design patterns reference

| Pattern | Implementation | Why |
|---------|---------------|-----|
| **Facade** (GoF) | `HstDiscoveryService` | Single entry point for components; absorbs config resolution, query building, caching, pixel, enrichment |
| **Service Locator** | `AbstractDiscoveryComponent.lookupService()` (2-step: typed → name) | HST instantiates components outside Spring; field injection is impossible |
| **Factory Method** | `CrispHstServices.getDefaultResourceServiceBroker()` | CRISP broker resolved via standard CRISP HST helper; deferred until first use |
| **Request-Scoped Cache** | `DiscoveryRequestCache` via `HstRequestContext` attributes | Dedup API calls across sibling components in one page render |
| **Observer** | `CachingDiscoveryConfigProvider` implements `EventListener` directly | Invalidate config cache on CMS save without restart; JCR listener lifecycle co-located with the cache |
| **Sealed Types** | `DiscoveryException` hierarchy | Exhaustive error handling via pattern matching |
| **Records** | All queries, results, config, DTOs | Immutability, value semantics, no boilerplate |

---

## Testing strategy

Components are tested without a Spring context or HST container:

1. **Subclass override pattern:** Test creates a subclass that overrides `lookupService()` to return a mock, and overrides `getComponentParametersInfo()` to return a stub.
2. **Request context simulation:** `HstRequestContext.getAttribute/setAttribute` mocked via `HashMap` + `doAnswer`.
3. **CRISP mocking:** `ResourceServiceBroker` is mocked at the interface level. Resource payloads are built with real `ObjectMapper`.
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
