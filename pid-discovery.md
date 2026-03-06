So, it looks like we're missing a flow with the recommendations path, we are missing item_ids, but there is a whole flow according to glean. Lets take a step back and figure out the best appraoch for our consumers that are building a website through brXM and are using this       
plugin: Here’s a concrete, “natural” flow you can implement for item-based Pathways widgets (e.g. `4le608d9`), especially on PDPs.
                                                                                                                                                                                                                                                                                          
---                                                                                                                                                                                                                                                                                     

### 1. Decide where item widgets are allowed

Item widgets only make sense when you have a **current product**:

- **Allowed**: PDP, quick-view, cart (multi-item).
- **Not great**: homepage, generic landing (use `global` / `keyword` / `personalized` widgets instead).

So: on PDPs, render the “Similar items / Co-viewed / FBT” component; elsewhere, either hide it or use a non-item widget.
                                                                                                                                                                                                                                                                                      
---                                                                                                                                                                                                                                                                                 

### 2. Model widget config in brXM

In your brXM document / component parameters, store at least:

- `widgetId` (e.g. `4le608d9`)
- `widgetType` (e.g. `item`) – optional but helpful
- `rows` (e.g. `8`)
- `title`, `cols`, etc., if needed

You already have something like this in the ProductRecs component design:

  ```json                                                                                                                                                                                                                                                                             
  {                                                                                                                                                                                                                                                                                   
    "widgetId": "4le608d9",                                                                                                                                                                                                                                                           
    "widgetType": "item",                                                                                                                                                                                                                                                             
    "rows": 8                                                                                                                                                                                                                                                                         
  }                                                                                                                                                                                                                                                                                   
  ```                                                                                                                                                                                                                                                                                 

This makes the rendering flow independent of the merchant widgets API.
                                                                                                                                                                                                                                                                                      
---                                                                                                                                                                                                                                                                                 

### 3. Ensure every PDP knows its Discovery PID

For each product page, you want a **Discovery pid** available on the document:

- Add a field like `brxdis:pid` (or reuse an existing one) on the product document.
- Populate it during import / sync from Discovery (or align local product ID == Discovery `pid`).

At render time:

  ```java                                                                                                                                                                                                                                                                             
  String discoveryPid = productDocument.getSingleProperty("brxdis:pid");                                                                                                                                                                                                              
  // or derive from URL if your scheme is 1:1 with pid                                                                                                                                                                                                                                
  ```                                                                                                                                                                                                                                                                                 

If `discoveryPid` is null, **don’t** call Pathways – just skip the widget gracefully.
                                                                                                                                                                                                                                                                                      
---                                                                                                                                                                                                                                                                                 

### 4. Build the Pathways item request

For item widget v2, required params include `item_ids` (PIDs) plus the usual context params.

Server-side Java-ish pseudo:

  ```java                                                                                                                                                                                                                                                                             
  URI uri = UriComponentsBuilder                                                                                                                                                                                                                                                      
      .fromHttpUrl("https://pathways.dxpapi.com/api/v2/widgets/item/" + widgetId)                                                                                                                                                                                                     
      .queryParam("account_id", accountId)                                                                                                                                                                                                                                            
      .queryParam("domain_key", domainKey)                                                                                                                                                                                                                                            
      .queryParam("item_ids", discoveryPid)        // <-- key bit                                                                                                                                                                                                                     
      .queryParam("request_id", System.currentTimeMillis())                                                                                                                                                                                                                           
      .queryParam("_br_uid_2", brUid2)                                                                                                                                                                                                                                                
      .queryParam("url", currentPageUrl)           // CMS internal or live URL                                                                                                                                                                                                        
      .queryParam("ref_url", referrerUrlOrSiteRoot)                                                                                                                                                                                                                                   
      .queryParam("rows", rows)                                                                                                                                                                                                                                                       
      .build(true)                                                                                                                                                                                                                                                                    
      .toUri();                                                                                                                                                                                                                                                                       
                                                                                                                                                                                                                                                                                      
  HttpHeaders headers = new HttpHeaders();                                                                                                                                                                                                                                            
  headers.set("auth-key", authKey);               // header, not query param                                                                                                                                                                                                          
                                                                                                                                                                                                                                                                                      
  ResponseEntity<PathwaysResponse> resp =                                                                                                                                                                                                                                             
      restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), PathwaysResponse.class);                                                                                                                                                                                  
  ```                                                                                                                                                                                                                                                                                 

For **multi-item contexts** (e.g. cart):

  ```java                                                                                                                                                                                                                                                                             
  String itemIds = String.join(",", cartPids);    // "PID1,PID2,PID3"                                                                                                                                                                                                                 
  .queryParam("item_ids", itemIds)                                                                                                                                                                                                                                                    
  ```                                                                                                                                                                                                                                                                                 

For **local CMS preview**, keep the same pattern, but:

- `url` = `http://localhost:8080/site/_cmsinternal/...`
- `ref_url` = `http://localhost:8080/cms/angular/hippo-cm/`
- `_br_uid_2` = some synthetic value if you don’t have the real cookie yet.

  ---                                                                                                                                                                                                                                                                                 

### 5. Rendering the component

Once you have `resp.response.docs[]` from Pathways:

- Map to your view model (thumbnail URL, name, price, PDP URL).
- Render a simple grid using your existing product tile component.
- If `docs` is empty, either hide the block or show a “no recommendations” fallback.

  ---                                                                                                                                                                                                                                                                                 

### 6. Summary of the “natural” request flow per page

On every PDP request:

1. Resolve product document → read `brxdis:pid`.
2. Read widget config (widgetId, rows, etc.) from the CMS component.
3. Build Pathways `item` URL with:
    - `item_ids=<brxdis:pid>`
    - standard context params (`account_id`, `domain_key`, `url`, `ref_url`, `_br_uid_2`, `request_id`, `rows`).
4. Call Pathways with `auth-key` header (server-side).
5. Render returned products into the PDP template.

That gives you a single, consistent flow that works for:

- Live site vs CMS preview (only URLs / cookies differ).
- Single-product vs multi-product contexts (change how you build `item_ids`).
- Different item-based algorithms (configured in Dashboard; your integration only cares about widgetId + context pid).

Would you like a ready-to-run server-side example (Java or curl) that implements this flow?

⏺ This is the right moment to step back. Let me look at what we actually have today vs. what the Glean flow describes, and map it to the plugin's responsibility vs. the consumer's responsibility:

⏺ Read 2 files (ctrl+o to expand)

⏺ Now I have the full picture. Let me reason through the responsibility split clearly.

What the plugin already has:
- contextProductId comes from URL param ?contextProductId=PID — passed as item_ids
- widgetType is a Channel Manager component parameter (can be blank → auto-resolved from widget list)
- widgetId comes from the recommendation document OR URL param

What's missing for a clean consumer experience:

Gap 1 — No guard against calling item widget without item_ids.
If the resolved widget type is item and contextProductId is blank, the v2 API returns 400. The component should return empty products instead of throwing.

Gap 2 — No way to auto-detect item_ids from the current page context.
In the Glean flow, on a PDP, item_ids comes from the product document being viewed — not a URL param. In brXM terms, this is the page's content bean. The plugin should let consumers configure which property on the page content bean to read the PID from.

Gap 3 — contextProductId has no component parameter fallback.
Currently it's only readable from the URL. An editor cannot configure a static item_ids via Channel Manager.

Proposed changes (3 focused touches):

1. DiscoveryRecommendationComponentInfo — add getContextProductPidProperty() (@Parameter(name="contextProductPidProperty", defaultValue="brxdis:pid")). This tells the component which JCR property on the page's content bean holds the Discovery PID.
2. DiscoveryRecommendationComponent.doBeforeRender() — after URL param check, also try reading contextProductPidProperty from the page's content bean (if the current page has a content bean that is not the recommendation document itself). Order: URL param → content bean property
   → null.
3. HstDiscoveryService.recommend() — guard: if resolved widget type maps to item (via DiscoveryApiClient.toV2WidgetType()) and contextProductId is blank, log WARN and return List.of() instead of firing a 400.

What this gives consumers:

┌───────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────────┐
│                           Use case                            │                          How it works                           │
├───────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ PDP with product document that has brxdis:pid                 │ Auto-detected from content bean, zero config                    │
├───────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ PDP where product PID is in the URL                           │ Pass ?contextProductId=PID — already works                      │
├───────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ Consumer product doc uses a different property name           │ Set contextProductPidProperty=ns:myPidField via Channel Manager │
├───────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ Global/keyword/category widget on any page                    │ No item_ids needed — guard lets it pass through                 │
├───────────────────────────────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ Item widget without any product context (e.g., demo homepage) │ Guard returns empty gracefully instead of 400                   │
└───────────────────────────────────────────────────────────────┴─────────────────────────────────────────────────────────────────┘

For the demo: the /recommendations/foo page has a brxdis:recommendationDocument as its content bean, not a product. So item_ids would be null → guard returns empty. To test a real item widget, the demo consumer would need a product page with a PID field, or pass
?contextProductId=some-pid in the URL.

Ready to implement all three changes?
