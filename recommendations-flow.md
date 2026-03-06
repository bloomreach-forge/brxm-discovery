This is a very solid, “natural” flow for a webmaster‑driven CMS integration. The big pieces (separate **widget document**, picker talking to **merchant widgets API**, **context PID** resolution, v2 mapping, and a clean PDP + demo story) are all in the right place.

Below are honest, critical points where you can tighten it up for both correctness and editor experience.

---

### 1. Widget type modelling (v1 vs v2)

Right now:

- You store only **`widgetId`** in the document.
- At render time you:
    - Call widgets API
    - Get `type="mlt"`
    - Map `mlt → item` and build `/api/v2/widgets/item/{id}`.

That’s logically correct: *MLT is deprecated; v2 item widget is the right endpoint*.

**Improvements:**

- **Persist the v2 widget type at selection time**:
    - When the picker row is chosen, also save `brxdis:widgetType` in `brxdis:recommendationDocument`.
    - Normalize there: if API says `mlt`, store `item`. If it’s already `item`, `keyword`, `category`, `global`, `personalized`, store as‑is.
    - Then, in rendering, you don’t need a widget list lookup just to know the type.

- **Make the component type‑aware**:
    - If `widgetType=item`, require/resolve a PID.
    - If `widgetType=category`, require a category id (different context).
    - If `widgetType=global`, no context needed.
    - If `widgetType=keyword`, expect a query string instead of PID.

That makes usage much clearer to webmasters: they pick a widget with an obvious type and the component’s expectations match.

---

### 2. Context resolution & editor ergonomics

Your priority chain:

1. URL param `?contextProductId=PID`
2. Component param `contextProductId` (Channel Manager)
3. Page bean field `brxdis:pid` (PDP)
4. Else null → no call

is exactly the right shape.

**Critiques / tweaks:**

- For real sites, **2 and 3 are the primary UX**:
    - 3 (auto from PDP) is the “just works” path.
    - 2 is a **curated override** (“always show recommendations for this hero product”).
- 1 is great for **debugging & demos**, but I wouldn’t rely on it as a core authoring tool; editors shouldn’t have to hand‑edit URLs.

**UX upgrade:**

- In **preview mode**, if `widgetType=item` and all three (a/b/c) failed:
    - Don’t just return `List.of()`.
    - Render a small diagnostic in the component region:  
      *“Item widget ‘pDAaaD’: no context product ID resolved (no brxdis:pid on page, no override set). Nothing will be rendered live.”*
- In **live mode**, your `List.of()` guard is fine (better to fail closed than throw a 400).

This one change dramatically improves how “discoverable” misconfig is for editors.

---

### 3. API correctness & error handling

You’re already:

- Mapping `mlt → item`.
- Hitting `/api/v2/widgets/item/{widgetId}`.
- Passing `account_id`, `domain_key`, `request_id`, `url`, `ref_url`, `_br_uid_2`, `rows`, `item_ids` and `auth-key` as a header – all aligned with v2 specs.

**Points to watch:**

- **Guard before calling** (which you already started):
    - If `widgetType=item` and `contextProductId` is blank, **don’t call Pathways** – you avoid noisy 400s.
- **Log at WARN** when you skip:
    - Include widgetId, page path, and why (no PID). This helps ops see integration issues.
- Consider a **soft timeout** and fallback in the component (e.g. “no recs” or nothing) if CRISP is slow, to keep page render resilient.

---

### 4. Multi‑item contexts (cart, bundles)

Your `contextProductId` is a single string. For FBT / cart cross‑sell you’ll eventually want:

- `List<String> contextProductIds` → joined as `item_ids=PID1,PID2,PID3`.

Suggestion:

- Model the **service signature** as taking a `List<String>` early:
    - In PDP, you pass `List.of(singlePid)`.
    - In cart pages, you pass all line item PIDs.
- Then `DiscoveryApiClient` can always do:

```java
String itemIds = String.join(",", contextPids);
```

without changing its interface later.

---

### 5. Performance + caching

Currently:

- Widget list from merchant API is cached for 5 minutes – good.
- Pathways responses are uncached (as far as described).

Consider:

- Once you store `widgetType` on the document, you can **drop the widget list lookup in the rendering path** completely.
- For high‑traffic PLPs/PDPs, a **short in‑memory cache** for Pathways responses (per widgetId + itemIds combo, for anonymous traffic) can significantly lower QPS. This is optional but worth thinking about if this ships to big merchants.

---

### 6. CMS authoring experience

From a webmaster’s perspective, your flow is already pretty natural:

- Pick a widget from a list (name, type, enabled).
- Drop that widget‑doc into any page.
- On PDPs, “it just works” via `brxdis:pid`.
- For non‑PDPs / demos:
    - Either a single **“Context PID override”** field in the component.
    - Or a URL param for dev.

**A couple of polish ideas:**

- When they pick a widget, **show its type + description** in the inspector (“Item widget – Similar Products”) so they know it expects a product context.
- For non‑PDP pages, if widget is item‑based and there’s no PID, let them fill a simple **“Example PID for preview”** param. That just feeds your `contextProductId` path (2b).

---

### 7. Abstractions & naming

Your abstraction stack is quite clean already:

- `DiscoveryRecommendationComponent`:
    - Resolves content bean
    - Resolves context PID(s)
    - Delegates to `HstDiscoveryService`.

- `HstDiscoveryService`:
    - Decides widget type, builds `RecQuery`.

- `DiscoveryApiClient`:
    - Knows how to talk to core/Pathways and do v1→v2 mapping.

Two concrete suggestions:

- **Single canonical mapping function** for widget type:

```java
String toV2WidgetType(String merchantType) {
  return switch (merchantType) {
    case "mlt" -> "item";
    default    -> merchantType;
  };
}
```

Use that in:

- the picker (to display effective type),
- when persisting `brxdis:widgetType`,
- and in `DiscoveryApiClient`.

- Align naming of `contextProductId` and `item_ids` in your code comments so no one confuses them; maybe call it `contextPid` internally to echo Discovery’s `pid`.

---

### Bottom line

- Conceptually, your **end‑to‑end story is strong** and aligns with how the v2 APIs are meant to be used.
- The main gaps are **editor diagnostics**, **persisting the normalized widget type**, and being more **type‑aware** (item vs category vs global) in the component.
- If you add:
    - `brxdis:widgetType` on the document,
    - a preview‑only warning when an item widget has no PID,
    - and (eventually) multi‑PID support,

you’ll have a plugin that feels “native” for webmasters and is robust for production traffic.

Would you like a short mapping snippet or a sample server-side (Java) implementation for this flow?