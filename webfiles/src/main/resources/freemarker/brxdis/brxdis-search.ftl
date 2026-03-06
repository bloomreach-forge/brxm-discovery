<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-search-css">
<style>
.brxdis-search{font-family:system-ui,-apple-system,sans-serif}
.brxdis-search__form{display:flex;gap:.5rem;margin-bottom:1.25rem}
.brxdis-search__input{flex:1;padding:.625rem .875rem;border:1.5px solid #e5e7eb;border-radius:8px;font-size:.9375rem;outline:none;transition:border-color .15s}
.brxdis-search__input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.brxdis-search__submit{padding:.625rem 1.25rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:.9375rem;font-weight:500;cursor:pointer;white-space:nowrap;transition:background .15s}
.brxdis-search__submit:hover{background:#1d4ed8}
.brxdis-search__meta{font-size:.875rem;color:#6b7280;margin-bottom:1rem}
.brxdis-search__meta strong{color:#111827}
.brxdis-search__layout{display:flex;gap:1.5rem;align-items:flex-start}
.brxdis-search__sidebar{flex:0 0 220px;min-width:0}
.brxdis-search__results{flex:1;min-width:0}
/* Facets (inline) */
.brxdis-sf__group{margin-bottom:1rem;border-bottom:1px solid #f3f4f6;padding-bottom:1rem}
.brxdis-sf__group:last-child{border-bottom:none;padding-bottom:0}
.brxdis-sf__heading{font-size:.75rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#374151;margin:0 0 .5rem}
.brxdis-sf__list{list-style:none;margin:0;padding:0}
.brxdis-sf__item{margin:.1rem 0;font-size:.875rem;color:#6b7280}
/* Grid */
.brxdis-sg{display:grid;grid-template-columns:repeat(auto-fill,minmax(185px,1fr));gap:1rem}
.brxdis-sc{background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;transition:box-shadow .2s,transform .2s}
.brxdis-sc:hover{box-shadow:0 6px 20px rgba(0,0,0,.1);transform:translateY(-2px)}
.brxdis-sc__img{aspect-ratio:4/3;background:#f3f4f6;overflow:hidden}
.brxdis-sc__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-sc__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:2.5rem}
.brxdis-sc__body{padding:.75rem;flex:1;display:flex;flex-direction:column;gap:.25rem}
.brxdis-sc__title{font-size:.875rem;font-weight:600;color:#111827;line-height:1.4;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-sc__title a{color:inherit;text-decoration:none}
.brxdis-sc__title a:hover{color:#2563eb}
.brxdis-sc__price{font-size:.9375rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.375rem}
.brxdis-sc__cta{margin:.25rem .75rem .75rem;padding:.5rem;background:#2563eb;color:#fff;border:none;border-radius:6px;font-size:.8125rem;font-weight:500;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-sc__cta:hover{background:#1d4ed8;color:#fff}
/* Pagination */
.brxdis-spag{display:flex;align-items:center;justify-content:center;gap:.3rem;padding:1.5rem 0;flex-wrap:wrap}
.brxdis-spag a,.brxdis-spag__cur,.brxdis-spag__dis{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;font-size:.875rem;text-decoration:none;white-space:nowrap;transition:all .15s}
.brxdis-spag a{background:#fff;color:#374151}
.brxdis-spag a:hover{background:#f3f4f6}
.brxdis-spag__cur{background:#2563eb;border-color:#2563eb;color:#fff;font-weight:600}
.brxdis-spag__dis{background:#f9fafb;color:#d1d5db;cursor:default}
.brxdis-spag__ell{display:inline-flex;align-items:center;min-width:2.25rem;height:2.25rem;justify-content:center;color:#9ca3af;font-size:.875rem}
/* Empty */
.brxdis-empty{padding:3rem 1rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
@media(max-width:640px){.brxdis-search__layout{flex-direction:column}.brxdis-search__sidebar{flex:none;width:100%}}
</style>
</@hst.headContribution>

<#assign sr = hstRequest.requestContext.servletRequest>

<#function pageHref p>
  <#local parts = []>
  <#list sr.parameterMap?keys as k>
    <#if k != "page">
      <#list sr.parameterMap[k] as v>
        <#local parts = parts + [k + "=" + v?url('UTF-8')]>
      </#list>
    </#if>
  </#list>
  <#return "?" + (parts + ["page=" + p])?join("&")>
</#function>

<div class="brxdis-search">
  <form class="brxdis-search__form" method="get" action="">
    <input class="brxdis-search__input" type="search" name="q" value="${query!""}" placeholder="Search products…" autofocus/>
    <button class="brxdis-search__submit" type="submit">Search</button>
  </form>

  <#if searchResult??>
    <#assign current = searchResult.page() + 1>
    <#assign totalPages = (searchResult.total() / searchResult.pageSize())?ceiling>
    <#assign start = searchResult.page() * searchResult.pageSize() + 1>
    <#assign end = ((searchResult.page() + 1) * searchResult.pageSize())>
    <#if end gt searchResult.total()><#assign end = searchResult.total()></#if>

    <p class="brxdis-search__meta">
      <strong>${searchResult.total()}</strong> results
      <#if searchResult.total() gt 0> &mdash; showing ${start}&ndash;${end}</#if>
    </p>

    <div class="brxdis-search__layout">

      <#-- Facets sidebar -->
      <#if searchResult.facets()?has_content>
        <aside class="brxdis-search__sidebar">
          <#list searchResult.facets()?values as facet>
            <div class="brxdis-sf__group">
              <p class="brxdis-sf__heading">${facet.name()}</p>
              <ul class="brxdis-sf__list">
                <#list facet.values() as fv>
                  <li class="brxdis-sf__item">${fv.value()} (${fv.count()})</li>
                </#list>
              </ul>
            </div>
          </#list>
        </aside>
      </#if>

      <div class="brxdis-search__results">
        <#if searchResult.products()?has_content>
          <div class="brxdis-sg">
            <#list searchResult.products() as product>
              <article class="brxdis-sc">
                <div class="brxdis-sc__img">
                  <#if product.imageUrl()?has_content>
                    <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
                  <#else>
                    <div class="brxdis-sc__placeholder">&#128722;</div>
                  </#if>
                </div>
                <div class="brxdis-sc__body">
                  <h3 class="brxdis-sc__title">
                    <a href="${product.url()!""}">${product.title()!"Untitled"}</a>
                  </h3>
                  <#if product.price()??>
                    <p class="brxdis-sc__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
                  </#if>
                </div>
                <a class="brxdis-sc__cta" href="${product.url()!""}">View Product</a>
              </article>
            </#list>
          </div>

          <#if totalPages gt 1>
            <nav class="brxdis-spag" aria-label="Pagination">
              <#if current gt 1>
                <a href="${pageHref(current - 1)}" aria-label="Previous">&#8592; Prev</a>
              <#else>
                <span class="brxdis-spag__dis">&#8592; Prev</span>
              </#if>
              <#list 1..totalPages as p>
                <#if p == 1 || p == totalPages || (p gte current - 2 && p lte current + 2)>
                  <#if p == current>
                    <span class="brxdis-spag__cur">${p}</span>
                  <#else>
                    <a href="${pageHref(p)}">${p}</a>
                  </#if>
                <#elseif p == 2 || p == totalPages - 1>
                  <span class="brxdis-spag__ell">&#8230;</span>
                </#if>
              </#list>
              <#if current lt totalPages>
                <a href="${pageHref(current + 1)}" aria-label="Next">Next &#8594;</a>
              <#else>
                <span class="brxdis-spag__dis">Next &#8594;</span>
              </#if>
            </nav>
          </#if>
        <#else>
          <div class="brxdis-empty">&#128269; No results for &ldquo;${query!""}&rdquo;. Try different keywords.</div>
        </#if>
      </div>

    </div>
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#elseif editMode>
    <div class="brxdis-empty">&#128736; <strong>Discovery Search</strong> &mdash; type a query in the field above to preview results in the CMS.</div>
  <#else>
    <div class="brxdis-empty">Enter a search term above to find products.</div>
  </#if>
</div>
