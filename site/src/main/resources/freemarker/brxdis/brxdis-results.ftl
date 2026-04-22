<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#function slugify text>
  <#local s = (text!"")?lower_case>
  <#local s = s?replace("[^a-z0-9]+", "-", "r")>
  <#local s = s?replace("^-+|-+$", "", "r")>
  <#if s?has_content><#return s><#else><#return "product"></#if>
</#function>
<#assign resolvedProductPage><@hst.link path="/product"/></#assign>
<#assign resolvedProductPage = resolvedProductPage?trim>

<#-- @ftlvariable name="editMode"           type="java.lang.Boolean" -->
<#-- @ftlvariable name="dataSourceMode"     type="java.lang.String" -->
<#-- @ftlvariable name="document"           type="org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean" -->
<#-- @ftlvariable name="query"              type="java.lang.String" -->
<#-- @ftlvariable name="categoryId"         type="java.lang.String" -->
<#-- @ftlvariable name="displayName"        type="java.lang.String" -->
<#-- @ftlvariable name="products"           type="java.util.List" -->
<#-- @ftlvariable name="pagination"         type="org.bloomreach.forge.discovery.search.model.PaginationModel" -->
<#-- @ftlvariable name="facets"             type="java.util.Map" -->
<#-- @ftlvariable name="facetUrls"          type="java.util.Map" -->
<#-- @ftlvariable name="activeFacets"       type="java.util.Map" -->
<#-- @ftlvariable name="clearAllFiltersUrl" type="java.lang.String" -->
<#-- @ftlvariable name="pageUrls"           type="java.util.Map" -->
<#-- @ftlvariable name="sortUrl"            type="java.lang.String" -->
<#-- @ftlvariable name="sortOptions"        type="java.util.List" -->
<#-- @ftlvariable name="didYouMean"         type="java.util.List" -->
<#-- @ftlvariable name="autoCorrectQuery"   type="java.lang.String" -->
<#-- @ftlvariable name="redirectUrl"        type="java.lang.String" -->
<#-- @ftlvariable name="redirectQuery"      type="java.lang.String" -->
<#-- @ftlvariable name="campaign"           type="org.bloomreach.forge.discovery.search.model.Campaign" -->

<@hst.headContribution keyHint="brxdis-results-css">
<style>
.brxdis-results{font-family:system-ui,-apple-system,sans-serif}
.brxdis-results__layout{display:grid;grid-template-columns:220px 1fr;gap:1.75rem;align-items:start}
.brxdis-results__layout--no-sidebar{grid-template-columns:1fr}
.brxdis-results__main{min-width:0}
.brxdis-results__header{margin-bottom:1.25rem}
.brxdis-results__title{font-size:1.5rem;font-weight:700;color:#111827;margin:0 0 .25rem}
.brxdis-results__autocorrect,.brxdis-results__dym{font-size:.875rem;color:#6b7280;margin-bottom:.75rem}
.brxdis-results__autocorrect a,.brxdis-results__dym a{color:#2563eb;text-decoration:none}
.brxdis-results__dym a:hover{text-decoration:underline}
.brxdis-results__redirect{background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:.75rem 1rem;margin-bottom:.75rem;font-size:.875rem;color:#1e40af}
.brxdis-results__redirect a{color:#1d4ed8;font-weight:500}
.brxdis-campaign{margin-bottom:1.25rem;border-radius:10px;overflow:hidden}
.brxdis-campaign a{display:block;text-decoration:none}
.brxdis-campaign img{width:100%;display:block}
.brxdis-campaign__text{padding:.75rem 1rem;background:#f0fdf4;border:1px solid #bbf7d0;color:#166534;font-size:.875rem}
.brxdis-results__toolbar{display:flex;align-items:center;justify-content:space-between;padding:.5rem 0 .75rem;border-bottom:1px solid #e5e7eb;margin-bottom:.25rem}
.brxdis-results__count{font-size:.875rem;color:#6b7280}
.brxdis-results__count strong{color:#111827;font-weight:600}
.brxdis-sort{display:flex;align-items:center;gap:.5rem;font-size:.875rem;color:#6b7280}
.brxdis-sort__select{padding:.3125rem .625rem;border:1px solid #e5e7eb;border-radius:6px;font-size:.875rem;background:#fff;color:#374151;cursor:pointer;outline:none}
.brxdis-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:1.25rem;padding:1rem 0}
.brxdis-card{background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;transition:box-shadow .2s,transform .2s}
.brxdis-card:hover{box-shadow:0 8px 28px rgba(0,0,0,.1);transform:translateY(-3px)}
.brxdis-card__img{aspect-ratio:4/3;background:#f3f4f6;overflow:hidden}
.brxdis-card__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-card__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:3rem}
.brxdis-card__body{padding:.875rem 1rem;flex:1;display:flex;flex-direction:column;gap:.25rem}
.brxdis-card__title{font-size:.9375rem;font-weight:600;color:#111827;line-height:1.4;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-card__title a{color:inherit;text-decoration:none}
.brxdis-card__title a:hover{color:#2563eb}
.brxdis-card__pid{font-size:.6875rem;color:#9ca3af;margin:.1rem 0 0}
.brxdis-card__price{font-size:1.0625rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.5rem}
.brxdis-card__cta{margin:.375rem .875rem .875rem;padding:.5625rem;background:#2563eb;color:#fff;border:none;border-radius:7px;font-size:.875rem;font-weight:500;cursor:pointer;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-card__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-facets{color:#374151;font-size:.875rem;line-height:1.5;position:sticky;top:1rem}
.brxdis-facets__chips{display:flex;flex-wrap:wrap;align-items:center;gap:.375rem;margin-bottom:1.125rem}
.brxdis-facets__chip{display:inline-flex;align-items:center;gap:.3125rem;background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;border-radius:2px;font-size:.75rem;font-weight:500;padding:.25rem .5rem .25rem .625rem;text-decoration:none;letter-spacing:.01em;transition:background .12s,border-color .12s,color .12s}
.brxdis-facets__chip:hover{background:#fee2e2;border-color:#fca5a5;color:#991b1b}
.brxdis-facets__chip-x{font-size:.875rem;line-height:1;opacity:.7;margin-top:-1px}
.brxdis-facets__clear{margin-left:.25rem;font-size:.75rem;color:#6b7280;text-decoration:none;padding:.25rem .375rem;border-radius:3px;white-space:nowrap;transition:color .12s,background .12s}
.brxdis-facets__clear:hover{color:#1e40af;background:#eff6ff}
.brxdis-facets__group{border-top:1px solid #e5e7eb}
.brxdis-facets__group:last-child{border-bottom:1px solid #e5e7eb}
.brxdis-facets__group details{padding:0}
.brxdis-facets__group summary{display:flex;justify-content:space-between;align-items:center;padding:.625rem 0;cursor:pointer;list-style:none;user-select:none;outline:none}
.brxdis-facets__group summary::-webkit-details-marker{display:none}
.brxdis-facets__group-name{font-size:.6875rem;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:#6b7280}
.brxdis-facets__chevron{width:12px;height:12px;flex-shrink:0;color:#9ca3af;transition:transform .18s ease}
.brxdis-facets__group details[open] .brxdis-facets__chevron{transform:rotate(180deg)}
.brxdis-facets__group-body{padding-bottom:.625rem;max-height:220px;overflow-y:auto}
.brxdis-facets__list{list-style:none;margin:0;padding:0}
.brxdis-facets__item a{display:flex;align-items:center;padding:.3125rem .25rem;border-radius:3px;text-decoration:none;color:#374151;transition:background .12s;gap:.5rem;outline-offset:2px}
.brxdis-facets__item a:hover{background:#f9fafb}
.brxdis-facets__item a:focus-visible{outline:2px solid #2563eb}
.brxdis-facets__check{width:14px;height:14px;border:1.5px solid #d1d5db;border-radius:2px;flex-shrink:0;background:#fff;transition:border-color .12s,background .12s;background-repeat:no-repeat;background-position:center;background-size:70%}
.brxdis-facets__item--active .brxdis-facets__check{background-color:#2563eb;border-color:#2563eb;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 10 8' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 4l3 3 5-6' stroke='%23fff' stroke-width='1.8' fill='none' stroke-linecap='round'/%3E%3C/svg%3E")}
.brxdis-facets__label{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:.8125rem}
.brxdis-facets__item--active .brxdis-facets__label{font-weight:600;color:#1e40af}
.brxdis-facets__count{font-size:.6875rem;color:#9ca3af;background:#f9fafb;border-radius:999px;padding:.05rem .4rem;flex-shrink:0;font-variant-numeric:tabular-nums}
.brxdis-facets__item--active .brxdis-facets__count{background:#eff6ff;color:#1e40af}
.brxdis-pagination{display:flex;align-items:center;justify-content:center;gap:.3rem;padding:1.5rem 0;flex-wrap:wrap}
.brxdis-pagination__btn{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;background:#fff;color:#374151;text-decoration:none;font-size:.875rem;transition:all .15s;white-space:nowrap}
.brxdis-pagination__btn:hover{background:#f3f4f6;border-color:#d1d5db}
.brxdis-pagination__btn[aria-current="page"]{background:#2563eb;border-color:#2563eb;color:#fff;font-weight:600;pointer-events:none}
.brxdis-pagination__disabled{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;background:#f9fafb;color:#d1d5db;font-size:.875rem;cursor:default;white-space:nowrap}
.brxdis-pagination__ellipsis{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;color:#9ca3af;font-size:.875rem}
.brxdis-empty{padding:3rem 1.5rem;text-align:center;color:#6b7280;font-size:.9375rem;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
.brxdis-warning{border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem}
.brxdis-hint{border:2px dashed #e5e7eb;padding:.75rem 1rem;border-radius:8px;font-size:.8125rem;color:#6b7280;text-align:center;margin:.5rem 0}
</style>
</@hst.headContribution>

<div class="brxdis-results">

<#-- Category Document inline editing / creation (category mode only) -->
<#if (dataSourceMode!"") == "category">
  <#if document??>
    <@hst.manageContent hippobean=document parameterName="document"
        rootPath="brxdis/categories"/>
  <#else>
    <@hst.manageContent documentTemplateQuery="new-brxdis-categoryDocument"
        parameterName="document" rootPath="brxdis/categories" defaultPath="categories"/>
  </#if>
</#if>

<#if brxdis_warning??>
  <div class="brxdis-warning">&#9888; ${brxdis_warning}</div>
</#if>

<#-- Campaign banner -->
<#if campaign??>
  <div class="brxdis-campaign">
    <#if campaign.bannerUrl()?has_content>
      <a href="${campaign.bannerUrl()}">
        <#if campaign.imageUrl()?has_content>
          <img src="${campaign.imageUrl()}" alt="${campaign.name()!""}"/>
        </#if>
      </a>
    <#elseif campaign.imageUrl()?has_content>
      <img src="${campaign.imageUrl()}" alt="${campaign.name()!""}"/>
    </#if>
    <#if campaign.htmlText()?has_content>
      <div class="brxdis-campaign__text">${campaign.htmlText()}</div>
    </#if>
  </div>
</#if>

<#-- Search-mode metadata: redirect, auto-correct, did-you-mean -->
<#if (dataSourceMode!"") == "search">
  <#if redirectUrl?has_content>
    <div class="brxdis-results__redirect">
      <strong>Looking for "${redirectQuery!query!""}"?</strong>
      <a href="${redirectUrl}">View curated results &rarr;</a>
    </div>
  </#if>
  <#if autoCorrectQuery?has_content>
    <p class="brxdis-results__autocorrect">
      Showing results for <strong>${autoCorrectQuery}</strong>.
      <a href="?q=${autoCorrectQuery?url('UTF-8')}">Search instead for "${query!""}"?</a>
    </p>
  </#if>
  <#if didYouMean?has_content>
    <p class="brxdis-results__dym">
      Did you mean:
      <#list didYouMean as suggestion>
        <a href="?q=${suggestion?url('UTF-8')}">${suggestion}</a><#sep>, </#sep>
      </#list>?
    </p>
  </#if>
</#if>

<#-- Two-column layout: sidebar + main -->
<div class="brxdis-results__layout<#if !facets?has_content> brxdis-results__layout--no-sidebar</#if>">

  <#-- ── Facets sidebar ──────────────────────────────────────────────────── -->
  <#if facets?has_content>
  <aside class="brxdis-facets" aria-label="Filter results">

    <#-- Active filter chips -->
    <#if activeFacets?has_content>
      <div class="brxdis-facets__chips">
        <#list activeFacets?keys as facetName>
          <#list activeFacets[facetName] as activeVal>
            <a href="${(facetUrls[facetName]!{})[activeVal]!""}"
               class="brxdis-facets__chip"
               title="Remove filter: ${facetName} = ${activeVal}">
              <span>${facetName}: ${activeVal}</span>
              <span class="brxdis-facets__chip-x" aria-hidden="true">&#x2715;</span>
            </a>
          </#list>
        </#list>
        <a href="${clearAllFiltersUrl!""}" class="brxdis-facets__clear">Clear all</a>
      </div>
    </#if>

    <#-- Facet groups -->
    <#list facets?values as facet>
      <#assign facetActiveVals = (activeFacets[facet.name()])![]>
      <div class="brxdis-facets__group">
        <details open>
          <summary>
            <span class="brxdis-facets__group-name">${facet.name()}</span>
            <svg class="brxdis-facets__chevron" width="12" height="12" viewBox="0 0 12 12"
                 fill="none" stroke="currentColor" stroke-width="2"
                 stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M 2.5 4.5 L 6 8 L 9.5 4.5"/>
            </svg>
          </summary>
          <div class="brxdis-facets__group-body">
            <ul class="brxdis-facets__list" role="group" aria-label="${facet.name()} filters">
              <#list facet.value() as fv>
                <#assign fvActive = facetActiveVals?seq_contains(fv.name())>
                <li class="brxdis-facets__item<#if fvActive> brxdis-facets__item--active</#if>">
                  <a href="${(facetUrls[facet.name()]!{})[fv.name()]!""}"
                     role="checkbox" aria-checked="${fvActive?c}"
                     title="<#if fvActive>Remove<#else>Filter by</#if> ${facet.name()}: ${fv.name()}">
                    <span class="brxdis-facets__check" aria-hidden="true"></span>
                    <span class="brxdis-facets__label">${fv.name()}</span>
                    <span class="brxdis-facets__count">${fv.count()}</span>
                  </a>
                </li>
              </#list>
            </ul>
          </div>
        </details>
      </div>
    </#list>

  </aside>
  </#if>

  <#-- ── Main content ────────────────────────────────────────────────────── -->
  <div class="brxdis-results__main">

    <#-- Category page title -->
    <#if (dataSourceMode!"") == "category" && (displayName?has_content || categoryId?has_content)>
      <div class="brxdis-results__header">
        <h1 class="brxdis-results__title">${displayName!categoryId!""}</h1>
      </div>
    </#if>

    <#-- Toolbar: result count + sort -->
    <#if pagination?? && (pagination.total() gt 0 || sortUrl?has_content)>
      <div class="brxdis-results__toolbar">
        <span class="brxdis-results__count">
          <#if pagination?? && pagination.total() gt 0>
            <strong>${pagination.total()}</strong> result<#if pagination.total() != 1>s</#if>
          </#if>
        </span>
        <#if sortUrl?has_content>
          <div class="brxdis-sort">
            <label for="brxdis-sort-sel">Sort:</label>
            <select id="brxdis-sort-sel" class="brxdis-sort__select"
                    data-sort-base="${sortUrl}">
              <option value="">Relevance</option>
              <#list sortOptions![] as opt>
              <option value="${opt.value}">${opt.label}</option>
              </#list>
            </select>
          </div>
          <script>
          (function () {
            var sel = document.getElementById('brxdis-sort-sel');
            if (!sel) return;
            var cur = new URLSearchParams(location.search).get('sort') || '';
            for (var i = 0; i < sel.options.length; i++) {
              if (sel.options[i].value === cur) { sel.selectedIndex = i; break; }
            }
            sel.addEventListener('change', function () {
              location.href = sel.dataset.sortBase + '&sort=' + encodeURIComponent(sel.value);
            });
          }());
          </script>
        </#if>
      </div>
    </#if>

    <#-- Product grid -->
    <#if products?has_content>
      <div class="brxdis-grid">
        <#list products as product>
          <#assign _slug = slugify(product.title()!"")>
          <#assign _pid  = (product.id()!"")?url('UTF-8')>
          <#assign _href = resolvedProductPage + "/" + _slug + "/pid/" + _pid>
          <article class="brxdis-card">
            <div class="brxdis-card__img">
              <#if product.imageUrl()?has_content>
                <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
              <#else>
                <div class="brxdis-card__placeholder">&#128722;</div>
              </#if>
            </div>
            <div class="brxdis-card__body">
              <h3 class="brxdis-card__title">
                <a href="${_href}">${product.title()!"Untitled product"}</a>
              </h3>
              <p class="brxdis-card__pid">PID:&nbsp;${product.id()!""}</p>
              <#if product.price()??>
                <p class="brxdis-card__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
              </#if>
            </div>
            <a class="brxdis-card__cta" href="${_href}">View Product</a>
          </article>
        </#list>
      </div>
    <#elseif (dataSourceMode!"") == "search" && (query!"") != "">
      <div class="brxdis-empty">
        <p>&#128269; No products found for "<strong>${query}</strong>". Try different keywords or clear filters.</p>
      </div>
    <#elseif (dataSourceMode!"") == "category">
      <div class="brxdis-empty">
        <p>&#128269; No products found in this category.</p>
      </div>
    <#else>
      <div class="brxdis-empty">
        <p>Enter a search term to find products.</p>
      </div>
    </#if>

    <#-- Pagination - uses pre-built pageUrls map (Map<Integer,String>, 0-indexed) -->
    <#if pageUrls?has_content && pagination?? && (pagination.totalPages() gt 1)>
      <#assign currentPage = pagination.page()>
      <#assign totalPages  = pagination.totalPages()>

      <#-- Resolve prev/next URLs from the map keys (avoids integer arithmetic on map keys) -->
      <#assign prevUrl = "">
      <#assign nextUrl = "">
      <#list pageUrls?keys as pk>
        <#if pk?number == currentPage - 1><#assign prevUrl = pageUrls[pk]></#if>
        <#if pk?number == currentPage + 1><#assign nextUrl = pageUrls[pk]></#if>
      </#list>

      <nav class="brxdis-pagination" aria-label="Pagination">

        <#if currentPage gt 0>
          <a href="${prevUrl}" class="brxdis-pagination__btn" aria-label="Previous page">&#8592; Prev</a>
        <#else>
          <span class="brxdis-pagination__disabled">&#8592; Prev</span>
        </#if>

        <#list pageUrls?keys as pk>
          <#assign pn = pk?number>
          <#assign dn = pn + 1>
          <#if pn == 0 || pn == totalPages - 1 || (pn gte currentPage - 2 && pn lte currentPage + 2)>
            <a href="${pageUrls[pk]}"
               class="brxdis-pagination__btn"<#if pn == currentPage> aria-current="page"</#if>>${dn}</a>
          <#elseif pn == 1 || pn == totalPages - 2>
            <span class="brxdis-pagination__ellipsis">&#8230;</span>
          </#if>
        </#list>

        <#if currentPage lt totalPages - 1>
          <a href="${nextUrl}" class="brxdis-pagination__btn" aria-label="Next page">Next &#8594;</a>
        <#else>
          <span class="brxdis-pagination__disabled">Next &#8594;</span>
        </#if>

      </nav>
    <#elseif (editMode!false) && pagination?? && (pagination.totalPages() lte 1)>
      <div class="brxdis-hint">&#128736; <strong>Pagination</strong> &mdash; controls appear here when results span multiple pages.</div>
    </#if>

  </div>
</div>
</div>
