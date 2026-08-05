[Documentation home](README.md) > Pixel Tracking & Consent

# Pixel Tracking & Consent

After a search, category browse, recommendation, or product-detail page renders, the plugin fires a server-side analytics event ("pixel") back to Discovery, so impressions, clicks, and page views feed Discovery's ranking models and A/B testing. This happens automatically once credentials are configured — no additional setup is required for a standard production deployment.

Pixel calls run on a bounded background thread pool and never affect page rendering — a failed or slow pixel call cannot cause a slow or broken page.

**ON THIS PAGE**
- [What gets tracked](#what-gets-tracked)
- [Turning tracking off](#turning-tracking-off)
- [Consent gating](#consent-gating)
- [Accurate attribution in headless deployments](#accurate-attribution-in-headless-deployments)

---

## What gets tracked

| Event | Fired when |
|---|---|
| Search page view | A search results page renders |
| Search submit | A visitor submits a search from the search bar |
| Category page view | A category browse page renders |
| Product page view | A product detail page renders with a resolved product |
| Widget view | A recommendation widget renders with at least one product |
| Widget click | A visitor clicks a product inside a recommendation widget |
| Suggest click | A visitor clicks an autosuggest suggestion |
| Click-add | A visitor adds to cart directly from a recommendation widget |
| Quickview | A visitor opens a quickview from a recommendation widget |

---

## Turning tracking off

### Deployment-wide kill switch

A single system property disables all pixel calls across every channel on a JVM, regardless of any other setting:

| Property | Default | Effect |
|---|---|---|
| `brxdis.pixel.envEnabled` | `true` | Set to `false` to disable all pixel calls JVM-wide — useful for local development or automated test environments so developer traffic doesn't pollute production analytics. |

```bash
mvn -P cargo.run cargo:run -Dbrxdis.pixel.envEnabled=false
```

### Per-channel control

Channel Manager exposes checkboxes under **Channel Settings → Pixel Tracking** so a single channel can be tuned without touching the JVM:

> **[SCREENSHOT PLACEHOLDER: the Pixel Tracking section of the Channel Settings panel, showing the enabled/test-data/debug checkboxes and the region dropdown.]**

| Channel setting | Default | Effect |
|---|---|---|
| Pixels enabled | checked | Uncheck to suppress all pixel calls for this channel only. |
| Test data | unchecked | Check to tag every event from this channel as test data, excluded from production analytics. |
| Debug | unchecked | Check for verbose pixel logging. |
| Region | `US` | `US` or `EU` — must match where your Discovery account's data is stored. |

The deployment-wide kill switch always takes precedence: if it's off, no channel-level setting can turn tracking back on.

---

## Consent gating

Where regulations require consent before firing analytics events (GDPR, ePrivacy), the plugin supports two mechanisms — pick whichever matches your consent setup.

### Cookie-based (no code required)

Set a **Consent cookie name** on the channel. Once set, the plugin checks for that cookie's presence on every request before firing a pixel — its value doesn't matter, only whether it exists.

```yaml
/hst:hst/hst:configurations/<your-site>/hst:workspace/hst:channel:
  hst:parameternames:  [discoveryPixelConsentCookie]
  hst:parametervalues: [OptanonConsent]
```

Your consent banner is responsible for setting and clearing this cookie in the visitor's browser as they grant or withdraw consent.

### Custom consent provider (complex consent platforms)

For consent management platforms that encode consent state inside a structured cookie payload (rather than the cookie's mere presence), integrators can register a small Java class that inspects the request and returns a yes/no consent decision. This is a developer-level integration point — see the plugin's developer guide for the interface and a worked example if your project needs it.

### Resolution order

```
Deployment kill switch off        →  no pixels fire, anywhere
Channel pixels disabled           →  no pixels fire, for that channel
Custom consent provider present   →  its decision is the sole gate
Consent cookie name set           →  fires only if that cookie is present
Nothing configured                →  fires unconditionally (default)
```

---

## Accurate attribution in headless deployments

The plugin fires pixels from the server, not the browser — so for a decoupled SPA front end, the real visitor IP, browser, and locale must be forwarded from your Node.js/SPA server layer to brXM on every Page Model API request. Without this, every event will carry your SPA server's own IP and User-Agent instead of the visitor's, degrading personalization accuracy.

| Forwarding header | Used for |
|---|---|
| `X-Forwarded-For` | Visitor IP |
| `X-Forwarded-User-Agent` | Browser identification (also used to suppress bot/crawler traffic) |
| `X-Forwarded-Accept-Language` | Locale |

Confirm forwarding is configured correctly during any headless integration — see your SPA integration guide for framework-specific examples.

---

**Previous:** [CMS Document Types & Pickers](06-document-types-and-pickers.md) · **Next:** [Troubleshooting](08-troubleshooting.md)
