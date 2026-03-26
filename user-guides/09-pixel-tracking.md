# Pixel Tracking Controls

## Introduction

After each search, category browse, recommendation, or product-detail page render, the plugin fires a server-side pixel event — a GET request to `p.brsrvr.com/pix.gif` — so Bloomreach Discovery can record impression data for analytics, ranking models, and A/B testing.

The pixel call is non-blocking: it runs on a bounded thread pool (`brxdis-pixel-*`) and never propagates exceptions to the page render. Failed pixel calls are logged at WARN and silently discarded.

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

### Resolution summary

```
brxdis.pixel.envEnabled=false  →  DISABLED (global — all channels)
  │
  └─ DiscoveryChannelInfo.getDiscoveryPixelsEnabled() = false  →  DISABLED (this channel only)
  └─ DiscoveryChannelInfo.getDiscoveryPixelsEnabled() = true   →  ENABLED  (this channel only)

DiscoveryChannelInfo.getDiscoveryPixelTestData() = true  →  test_data=true on this channel
DiscoveryChannelInfo.getDiscoveryPixelDebug()    = true  →  debug=true on this channel
```

The env kill switch (`brxdis.pixel.envEnabled=false`) cannot be overridden by a channel setting — if it is false, no pixels fire anywhere.

If the channel has no `DiscoveryChannelInfo` configured at all, the env/system property defaults apply (`envEnabled=true`, `testData=false`, `debug=false`).

---

## Setting parameters without Channel Manager UI

Pixel flags are managed by the typed `DiscoveryChannelInfo` interface and are stored by Channel Manager in the channel workspace — do not add them to `hst:parameternames`/`hst:parametervalues` manually, as that creates a second source of truth.

`hst:channelinfoclass` must be set on the `hst:channel` node (not the mount):

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  jcr:primaryType: hst:channel
  hst:channelinfoclass: org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo
```

---

## Wiring the Channel Manager UI

`hst:channelinfoclass` belongs on the `hst:channel` node — not the mount. Channel Manager reads the interface from the channel node, writes values through the typed `ChannelInfo` proxy, and surfaces the three pixel fields as **checkboxes** under "Pixel Tracking" in the Channel Settings panel.

### Case 1 — No existing ChannelInfo

**`hst:workspace/channel.yaml`** — set `hst:channelinfoclass` on the channel node:

```yaml
/hst:hst/hst:configurations/demo/hst:workspace/hst:channel:
  jcr:primaryType: hst:channel
  hst:name: My Site
  hst:type: website
  hst:channelinfoclass: org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo
```

**`hst:hosts/hosts.yaml`** — no Discovery-specific mount parameters are required:

```yaml
/hst:root:
  jcr:primaryType: hst:mount
  # No discoveryConfigPath needed — plugin reads from the global JCR config node
```

Do not add pixel parameters to `hst:parameternames`/`hst:parametervalues` — that creates a stale duplicate that Channel Manager cannot manage.

### Case 2 — Existing ChannelInfo in your project

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

No other changes are needed — HST discovers `@Parameter`-annotated getters from all interfaces in the hierarchy.

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
