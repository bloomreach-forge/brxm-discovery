[Documentation home](README.md) > Troubleshooting

# Troubleshooting

**ON THIS PAGE**
- [Installation](#installation)
- [Configuration and credentials](#configuration-and-credentials)
- [Search & category pages](#search--category-pages)
- [Recommendations](#recommendations)
- [Visual search](#visual-search)
- [Pixel tracking](#pixel-tracking)
- [Still stuck?](#still-stuck)

---

## Installation

| Symptom | Likely cause | Fix |
|---|---|---|
| `Required HST service is not available: org.bloomreach.forge.discovery.site.platform.HstDiscoveryService` | The site webapp is running against an older plugin build than what's installed. | Rebuild and redeploy the site webapp against the current plugin version. |
| Picker endpoint returns 404 at `{cms}/ws/discovery/picker/search` | `brxm-discovery-cms` is not on the CMS classpath, or the picker daemon module hasn't started. | Confirm the dependency is present and check the CMS log for the plugin's startup message (see [Installation](02-installation.md)). |
| Bundled templates not found / blank pages | Your site's HST configuration doesn't inherit from `hst:default`. | Add the missing `brxdis-*` template entries to your own site's template configuration, pointing at the plugin's bundled Freemarker files. |

## Configuration and credentials

| Symptom | Likely cause | Fix |
|---|---|---|
| `ConfigurationException: Discovery accountId is required` | Credentials aren't configured anywhere the plugin checks (environment, system property, or JCR). | Set `BRXDIS_ACCOUNT_ID`, `BRXDIS_DOMAIN_KEY`, `BRXDIS_API_KEY`. |
| Product grid renders empty with no error shown | `accountId` / `domainKey` don't match your actual Discovery account. | Verify both values against your Discovery dashboard. |
| A custom product attribute (e.g. `brand`) is missing from results | The field isn't included in the configured field list. | Add it to `brxdis:defaultFieldList` (global) or `discoveryDefaultFieldList` (per channel) — see [Configuration](03-configuration.md#the-product-field-list-fl). |
| Picker shows blank product names | The picker's title field doesn't match your catalog's actual field name. | Set `brxdis:pickerTitleField` to match your feed — see [Picker field mapping](03-configuration.md#picker-field-mapping). |

## Search & category pages

| Symptom | Likely cause | Fix |
|---|---|---|
| Category page renders nothing | No category is configured, and no URL parameter/path segment provides one. | Either pin a category via the document picker, or confirm the page is receiving the expected URL parameter. |
| A warning banner appears in Channel Manager preview but the page looks fine to visitors | This is expected — configuration warnings only render in preview mode; production visitors see a silent empty state instead of an error. | No action needed unless the underlying configuration issue should be fixed. |

## Recommendations

| Symptom | Likely cause | Fix |
|---|---|---|
| Recommendation widget renders empty | The Discovery widget may have no results yet, or the widget ID doesn't match one on your account. | Check the widget's status in the Discovery dashboard. |
| Recommendations always use v1 even though `authKey` is set | The `authKey` value isn't reaching the plugin — check which precedence layer (environment, system property, JCR, or channel override) you expect it to come from. | See [Configuration](03-configuration.md#injecting-credentials). |

## Visual search

See the dedicated troubleshooting table on [Recommendations & Visual Search](05-recommendations-and-visual-search.md#troubleshooting) — visual search has its own mount-placement pitfall that's easy to misconfigure.

## Pixel tracking

| Symptom | Likely cause | Fix |
|---|---|---|
| No pixel events appear in Discovery analytics | Tracking may be disabled at the deployment or channel level, or consent gating is blocking events. | Check the deployment kill switch, the channel's Pixel Tracking settings, and any consent cookie/provider configuration — see [Pixel Tracking & Consent](07-pixel-tracking.md). |
| All events show the same IP / generic browser in analytics | Headless front end isn't forwarding the visitor's real IP/User-Agent/locale headers. | Configure header forwarding in your SPA server layer. |

## Still stuck?

Check the plugin's log output at DEBUG level for the specific request path and error message — most failures surface a clear cause (a missing credential, a wrong ID, or an unreachable Discovery endpoint) once debug logging is enabled for the plugin's package.

---

**Previous:** [Pixel Tracking & Consent](07-pixel-tracking.md) · **Next:** [Release Notes](09-release-notes.md)
