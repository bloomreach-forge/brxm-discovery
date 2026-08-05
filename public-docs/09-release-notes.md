[Documentation home](README.md) > Release Notes

# Release Notes

This page tracks notable changes to the brXM Discovery plugin by version. Versions follow the project's Maven artifact version, not calendar dates — see the module POMs for the exact version resolved by your build.

---

## Unreleased (`develop`)

Changes merged to `develop` since the `0.0.4` release, pending the next version bump.

**Reliability**
- Added a per-host [Resilience4j](https://resilience4j.readme.io/) circuit breaker in front of every outbound Discovery call (`CircuitBreakerDiscoveryTransport`), so a failing or slow endpoint (e.g. recommendations) degrades in isolation instead of also stalling calls to other Discovery APIs (e.g. search). Tuning is via environment variable or system property — see [Configuration → Circuit breaker tuning](03-configuration.md#circuit-breaker-tuning).

---

## 0.0.4 — Released 2026-05-18

**Fixed**
- Corrected rate-limiting and client IP address handling in the HTTP transport layer, addressing cases where the wrong address was attributed to a request.

---

## 0.0.3 — Released 2026-05-11

**Fixed**
- Corrected the resolution order for system-property-based configuration overrides, so system properties reliably take precedence the way [Configuration → Injecting credentials](03-configuration.md#injecting-credentials) documents.

---

## 0.0.2 — Released 2026-05-07

**Added**
- Visual (image) search support, exposed through `discoveryVisualSearchEnabled` / `discoveryVisualSearchWidgetId` on `DiscoveryChannelInfo` — see [Recommendations & Visual Search](05-recommendations-and-visual-search.md).
- A guided, multi-step wizard for authoring recommendation widgets and other CMS document types, replacing manual field-by-field entry.

**Changed**
- Broader architecture reshuffle across the plugin's module layout and component/service packages, and general SEO improvements to bundled templates.

---

## 0.0.1 — Released 2026-03-26

Initial release of the brXM Discovery plugin: HTTP/2-backed Discovery search, category browse, and recommendation services, with native HST components and CMS document types for authoring.

---

**Previous:** [Troubleshooting](08-troubleshooting.md)
