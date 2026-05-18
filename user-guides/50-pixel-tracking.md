# Pixel Tracking Controls

## Introduction

After each search, category browse, recommendation, or product-detail page render, the plugin fires a server-side pixel event — a GET request to `p.brsrvr.com/pix.gif` — so Bloomreach Discovery can record impression data for analytics, ranking models, and A/B testing.

The pixel call is non-blocking: it runs on a bounded thread pool (`brxdis-pixel-*`) and never propagates exceptions to the page render. Failed pixel calls are logged at WARN and silently discarded.

### Pixel event types

The plugin fires nine distinct event types:

| Event | Fired when |
|---|---|
| `SearchPageView` | `DiscoverySearchGridComponent` renders a search results page |
| `SearchSubmit` | URL contains a `_brxdis_submit` parameter (deferred from a form submit) |
| `CategoryPageView` | `DiscoveryCategoryGridComponent` renders a category browse page |
| `ProductPageView` | `DiscoveryProductDetailComponent` renders with a resolved product |
| `WidgetView` | Any recommendation component renders with at least one product |
| `WidgetClick` | URL contains a `_brxdis_wclick` parameter (deferred widget click) |
| `SuggestClick` | URL contains a `_brxdis_sclick` parameter (deferred autosuggest click) |
| `ClickAdd` | URL contains a `_brxdis_cadd` parameter (deferred add-to-cart from a widget) |
| `Quickview` | URL contains a `_brxdis_qview` parameter (deferred quickview from a widget) |

`SearchSubmit`, `SuggestClick`, `WidgetClick`, `ClickAdd`, and `Quickview` are **deferred interactions** — the frontend embeds the event data as a URL parameter, and the plugin fires the pixel on the next page render (handled by `DeferredPixelInteractionHandler`).

You may need to suppress or tag pixels when:
- Running the app locally (avoid polluting production analytics with developer traffic)
- Running automated QA / load tests (mark traffic as test data or disable entirely)
- Operating a multi-tenant deployment where one channel must not emit pixels

---

## Default behaviour

Once the plugin is installed, `brxmdis.pixelService` is wired automatically from the site addon assembly. Pixels fire on cache-miss page renders as soon as valid Discovery credentials are configured.

No extra configuration is required for production.

---

## Environment-level kill switch

Three JVM system properties control pixel behaviour globally across all channels:

| Property | Default | Effect |
|---|---|---|
| `brxdis.pixel.envEnabled` | `true` | `false` = disable all pixel calls JVM-wide |
| `brxdis.pixel.testData` | `false` | `true` = append `&test_data=true` to all pixel paths |
| `brxdis.pixel.debug` | `false` | `true` = append `&debug=true` to all pixel paths |
| `brxdis.pixel.region` | `US` | Pixel endpoint region: `US` or `EU`. Must match where your Discovery account's pixel data is stored. Per-channel `discoveryPixelRegion` overrides this. |

### Cargo `-D` flags (local development)

```bash
cd demo && mvn -P cargo.run \
  -Dbrxdis.pixel.envEnabled=false \
  cargo:run
```

### `local.properties` file

```properties
brxdis.pixel.envEnabled=false
# brxdis.pixel.testData=true
# brxdis.pixel.debug=true
```

### Docker / Kubernetes env vars

System properties can be forwarded via the `JAVA_OPTS` or `CATALINA_OPTS` environment variable:

```dockerfile
ENV JAVA_OPTS="-Dbrxdis.pixel.envEnabled=false"
```

Kubernetes:

```yaml
env:
  - name: JAVA_OPTS
    value: "-Dbrxdis.pixel.envEnabled=false"
```

---

## Channel-level override (Channel Manager)

Three checkboxes under "Pixel Tracking" in Channel Manager let you override pixel behaviour for a specific channel without touching the JVM.

| Channel parameter | Default | Effect |
|---|---|---|
| `discoveryPixelsEnabled` | checked (`true`) | Uncheck to suppress all pixel calls for this channel |
| `discoveryPixelTestData` | unchecked (`false`) | Check to append `test_data=true` to all pixel calls |
| `discoveryPixelDebug` | unchecked (`false`) | Check to append `debug=true` to all pixel calls |
| `discoveryPixelRegion` | `US` | Pixel endpoint region: `US` or `EU`. Overrides `brxdis.pixel.region` for this channel. |

The env kill switch (`brxdis.pixel.envEnabled=false`) cannot be overridden by a channel setting — if it is false, no pixels fire anywhere.

If the channel has no `DiscoveryChannelInfo` configured at all, the env/system property defaults apply (`envEnabled=true`, `testData=false`, `debug=false`).

---

## Consent gating

Some regulatory requirements (GDPR, ePrivacy) demand that you obtain user consent before firing analytics pixels. The plugin supports two gating mechanisms: a cookie-based check (zero Java code) and a full SPI for complex consent management platforms.

### Resolution order

```
pixelService null OR channel kill-switch off  →  SUPPRESSED
  │
  └─ PixelConsentProvider bean registered  →  delegate to hasConsent(request) result
  │
  └─ discoveryPixelConsentCookie set (non-blank)  →  fire only if that cookie is present
  │
  └─ (nothing configured)  →  FIRE unconditionally  ← default / backward compatible
```

### Cookie-based consent (Channel Manager)

Set the **Consent cookie name** field in Channel Manager (or directly on the `hst:channel` node):

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  hst:parameternames:  [discoveryPixelConsentCookie]
  hst:parametervalues: [OptanonConsent]
```

When `discoveryPixelConsentCookie` is non-blank, the plugin checks whether a cookie with that name exists in the incoming request before firing any pixel. The **value** of the cookie is not inspected — only its presence matters. If the cookie is absent, all pixel calls for that channel are suppressed.

**Frontend responsibility:** Your consent banner must set/delete the cookie on the user's device:

```javascript
// Consent granted — set the cookie (no specific value required)
document.cookie = "OptanonConsent=1; path=/; max-age=31536000; SameSite=Lax";

// Consent withdrawn — delete the cookie
document.cookie = "OptanonConsent=; path=/; max-age=0";
```

The cookie must be present on the **request** (sent by the browser). Server-side Set-Cookie from the page render itself is too late — the pixel guard fires during that same render, before any response headers are sent.

### SPI — `PixelConsentProvider` (complex CMPs)

For consent management platforms that encode their state in structured cookie payloads (OneTrust, Cookiebot, Didomi, etc.), implement `PixelConsentProvider` and register it as a Spring bean named `brxmdis.pixelConsentProvider` in your site assembly.

The interface is a `@FunctionalInterface`:

```java
package org.bloomreach.forge.discovery.site.service.discovery.pixel;

@FunctionalInterface
public interface PixelConsentProvider {
    boolean hasConsent(HstRequest request);
}
```

**OneTrust example** (analytics category `C0002`):

```java
@Component("brxmdis.pixelConsentProvider")
public class OneTrustPixelConsentProvider implements PixelConsentProvider {
    @Override
    public boolean hasConsent(HstRequest request) {
        String otz = CookieUtils.getCookieValue(request, "OptanonConsent");
        return otz != null && otz.contains("C0002%3A1");
    }
}
```

Wire it in your site Spring assembly (e.g. `brxm-discovery-overrides.xml`):

```xml
<bean id="brxmdis.pixelConsentProvider"
      class="com.example.site.consent.OneTrustPixelConsentProvider"/>
```

When `brxmdis.pixelConsentProvider` is present, the `discoveryPixelConsentCookie` channel parameter is **ignored** — the SPI result is the sole consent gate. You do not need to configure the channel cookie name.

### Updated resolution summary

```
brxdis.pixel.envEnabled=false  →  DISABLED (global - all channels)
  │
  └─ DiscoveryChannelInfo.getDiscoveryPixelsEnabled() = false  →  DISABLED (this channel only)
  └─ DiscoveryChannelInfo.getDiscoveryPixelsEnabled() = true   →  proceed to consent check
      │
      └─ brxmdis.pixelConsentProvider registered  →  hasConsent(request) gates every pixel
      │
      └─ discoveryPixelConsentCookie set (non-blank)  →  fire only if cookie present
      │
      └─ (nothing)  →  FIRE unconditionally

DiscoveryChannelInfo.getDiscoveryPixelTestData() = true  →  test_data=true on this channel
DiscoveryChannelInfo.getDiscoveryPixelDebug()    = true  →  debug=true on this channel
```

---

## Setting parameters without Channel Manager UI

Pixel flags are managed by the typed `DiscoveryChannelInfo` interface and are stored by Channel Manager in the channel workspace - do not add them to `hst:parameternames`/`hst:parametervalues` manually, as that creates a second source of truth.

`hst:channelinfoclass` must be set on the `hst:channel` node (not the mount):

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  jcr:primaryType: hst:channel
  hst:channelinfoclass: org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo
```

---

## Wiring the Channel Manager UI

`hst:channelinfoclass` belongs on the `hst:channel` node - not the mount. Channel Manager reads the interface from the channel node, writes values through the typed `ChannelInfo` proxy, and surfaces the three pixel fields as **checkboxes** under "Pixel Tracking" in the Channel Settings panel.

### Case 1 - No existing ChannelInfo

**`hst:workspace/channel.yaml`** - set `hst:channelinfoclass` on the channel node:

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  jcr:primaryType: hst:channel
  hst:name: My Site
  hst:type: website
  hst:channelinfoclass: org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo
```

**`hst:hosts/hosts.yaml`** - no Discovery-specific mount parameters are required:

```yaml
/hst:root:
  jcr:primaryType: hst:mount
  # No discoveryConfigPath needed - plugin reads from the global JCR config node
```

Do not add pixel parameters to `hst:parameternames`/`hst:parametervalues` - that creates a stale duplicate that Channel Manager cannot manage.

### Case 2 - Existing ChannelInfo in your project

Create a composite interface that extends both and point `hst:channelinfoclass` at it:

```java
package com.example.site.channel;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import com.example.site.channel.ExistingChannelInfo;

public interface MyChannelInfo extends DiscoveryChannelInfo, ExistingChannelInfo { }
```

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  hst:channelinfoclass: com.example.site.channel.MyChannelInfo
```

No other changes are needed - HST discovers `@Parameter`-annotated getters from all interfaces in the hierarchy.

---

## Client context forwarding

The plugin fires pixels from the JVM, not the browser. For Discovery to correctly attribute events, the real browser IP, User-Agent, and locale must reach brXM via forwarding headers on the Page Model API request.

| Header read by brXM | Pixel use |
|---|---|
| `X-Forwarded-For` | `client_ip` param; also forwarded on the pixel request to Discovery |
| `X-Forwarded-User-Agent` | UA filtering (bot/crawler suppression); falls back to `User-Agent` |
| `X-Forwarded-Accept-Language` | Locale; falls back to `Accept-Language` |

**Your SPA server layer is responsible for forwarding these headers** on every Page Model API call. If absent, all pixel events carry your Node.js server's IP and UA — degrading personalisation accuracy and potentially triggering bot suppression silently on every render.

See the [SPA Integration Guide](40-spa-integration.html#forwarding-browser-context-for-accurate-pixel-tracking) for per-framework forwarding examples and reverse proxy configuration.

> **Loopback fallback:** If `X-Forwarded-For` resolves to `127.x`, `::1`, or `0:0:0:0:0:0:0:1`, the plugin ignores it and falls back to `request.getRemoteAddr()`. In local development all pixel events will have an empty `client_ip` — this is expected and harmless.

---

## Verifying pixel events

Pixel calls are logged at DEBUG level. To see them:

```properties
# logback.xml or log4j2 configuration
log4j2.logger.discovery=DEBUG,org.bloomreach.forge.discovery
```

A successful pixel fire looks like:

```
DEBUG DiscoveryPixelServiceImpl - Discovery pixel event fired: type=SearchResponse, q=running shoes
```

A suppressed pixel (disabled flag or CMS preview) produces no log line. A failed pixel call logs:

```
WARN  DiscoveryPixelServiceImpl - Discovery pixel event failed: ...
```
