<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-category-css">
<style>
.brxdis-cat{font-family:system-ui,-apple-system,sans-serif}
.brxdis-cat__header{margin-bottom:1.25rem}
.brxdis-cat__title{font-size:1.5rem;font-weight:700;color:#111827;margin:0 0 .25rem}
.brxdis-cat__meta{font-size:.875rem;color:#6b7280}
.brxdis-cat__meta strong{color:#111827}
.brxdis-cat__layout{display:flex;gap:1.5rem;align-items:flex-start}
.brxdis-cat__sidebar{flex:0 0 220px;min-width:0}
.brxdis-cat__results{flex:1;min-width:0}
.brxdis-cf__group{margin-bottom:1rem;border-bottom:1px solid #f3f4f6;padding-bottom:1rem}
.brxdis-cf__group:last-child{border-bottom:none;padding-bottom:0}
.brxdis-cf__heading{font-size:.75rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#374151;margin:0 0 .5rem}
.brxdis-cf__list{list-style:none;margin:0;padding:0}
.brxdis-cf__item{margin:.1rem 0;font-size:.875rem;color:#6b7280}
.brxdis-cg{display:grid;grid-template-columns:repeat(auto-fill,minmax(185px,1fr));gap:1rem}
.brxdis-cc{background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;transition:box-shadow .2s,transform .2s}
.brxdis-cc:hover{box-shadow:0 6px 20px rgba(0,0,0,.1);transform:translateY(-2px)}
.brxdis-cc__img{aspect-ratio:4/3;background:#f3f4f6;overflow:hidden}
.brxdis-cc__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-cc__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:2.5rem}
.brxdis-cc__body{padding:.75rem;flex:1;display:flex;flex-direction:column;gap:.25rem}
.brxdis-cc__title{font-size:.875rem;font-weight:600;color:#111827;line-height:1.4;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-cc__title a{color:inherit;text-decoration:none}
.brxdis-cc__title a:hover{color:#2563eb}
.brxdis-cc__price{font-size:.9375rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.375rem}
.brxdis-cc__cta{margin:.25rem .75rem .75rem;padding:.5rem;background:#2563eb;color:#fff;border:none;border-radius:6px;font-size:.8125rem;font-weight:500;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-cc__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-cpag{display:flex;align-items:center;justify-content:center;gap:.3rem;padding:1.5rem 0;flex-wrap:wrap}
.brxdis-cpag a,.brxdis-cpag__cur,.brxdis-cpag__dis{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;font-size:.875rem;text-decoration:none;white-space:nowrap;transition:all .15s}
.brxdis-cpag a{background:#fff;color:#374151}
.brxdis-cpag a:hover{background:#f3f4f6}
.brxdis-cpag__cur{background:#2563eb;border-color:#2563eb;color:#fff;font-weight:600}
.brxdis-cpag__dis{background:#f9fafb;color:#d1d5db;cursor:default}
.brxdis-cpag__ell{display:inline-flex;align-items:center;min-width:2.25rem;height:2.25rem;justify-content:center;color:#9ca3af;font-size:.875rem}
.brxdis-empty{padding:3rem 1rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
@media(max-width:640px){.brxdis-cat__layout{flex-direction:column}.brxdis-cat__sidebar{flex:none;width:100%}}
</style>
</@hst.headContribution>

<#if document??>
  <@hst.manageContent hippobean=document parameterName="document" rootPath="brxm-discovery" defaultPath="brxm-discovery"/>
<#else>
  <@hst.manageContent parameterName="document" rootPath="brxm-discovery" defaultPath="brxm-discovery"/>
</#if>

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

<#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
<#if categoryResult??>
<div class="brxdis-cat">
    <#assign current = categoryResult.page() + 1>
    <#assign totalPages = (categoryResult.total() / categoryResult.pageSize())?ceiling>
    <#assign start = categoryResult.page() * categoryResult.pageSize() + 1>
    <#assign end = ((categoryResult.page() + 1) * categoryResult.pageSize())>
    <#if end gt categoryResult.total()><#assign end = categoryResult.total()></#if>

    <div class="brxdis-cat__header">
      <h1 class="brxdis-cat__title">${categoryId!""}</h1>
      <p class="brxdis-cat__meta">
        <strong>${categoryResult.total()}</strong> products
        <#if categoryResult.total() gt 0> &mdash; showing ${start}&ndash;${end}</#if>
      </p>
    </div>

    <div class="brxdis-cat__layout">

      <#if categoryResult.facets()?has_content>
        <aside class="brxdis-cat__sidebar">
          <#list categoryResult.facets()?values as facet>
            <div class="brxdis-cf__group">
              <p class="brxdis-cf__heading">${facet.name()}</p>
              <ul class="brxdis-cf__list">
                <#list facet.values() as fv>
                  <li class="brxdis-cf__item">${fv.value()} (${fv.count()})</li>
                </#list>
              </ul>
            </div>
          </#list>
        </aside>
      </#if>

      <div class="brxdis-cat__results">
        <#if categoryResult.products()?has_content>
          <div class="brxdis-cg">
            <#list categoryResult.products() as product>
              <article class="brxdis-cc">
                <div class="brxdis-cc__img">
                  <#if product.imageUrl()?has_content>
                    <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
                  <#else>
                    <div class="brxdis-cc__placeholder">&#128722;</div>
                  </#if>
                </div>
                <div class="brxdis-cc__body">
                  <h3 class="brxdis-cc__title">
                    <a href="${product.url()!""}">${product.title()!"Untitled"}</a>
                  </h3>
                  <#if product.price()??>
                    <p class="brxdis-cc__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
                  </#if>
                </div>
                <a class="brxdis-cc__cta" href="${product.url()!""}">View Product</a>
              </article>
            </#list>
          </div>

          <#if totalPages gt 1>
            <nav class="brxdis-cpag" aria-label="Pagination">
              <#if current gt 1>
                <a href="${pageHref(current - 1)}" aria-label="Previous">&#8592; Prev</a>
              <#else>
                <span class="brxdis-cpag__dis">&#8592; Prev</span>
              </#if>
              <#list 1..totalPages as p>
                <#if p == 1 || p == totalPages || (p gte current - 2 && p lte current + 2)>
                  <#if p == current>
                    <span class="brxdis-cpag__cur">${p}</span>
                  <#else>
                    <a href="${pageHref(p)}">${p}</a>
                  </#if>
                <#elseif p == 2 || p == totalPages - 1>
                  <span class="brxdis-cpag__ell">&#8230;</span>
                </#if>
              </#list>
              <#if current lt totalPages>
                <a href="${pageHref(current + 1)}" aria-label="Next">Next &#8594;</a>
              <#else>
                <span class="brxdis-cpag__dis">Next &#8594;</span>
              </#if>
            </nav>
          </#if>
        <#else>
          <div class="brxdis-empty">&#128269; No products in this category.</div>
        </#if>
      </div>

    </div>
  </#if>
</div>
<#elseif editMode>
  <div class="brxdis-empty">&#128736; <strong>Discovery Category</strong> &mdash; select a Category Document in component properties.</div>
</#if>
