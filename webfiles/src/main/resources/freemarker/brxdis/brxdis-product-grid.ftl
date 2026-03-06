<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-pagination-css">
<style>
.brxdis-pagination{display:flex;align-items:center;justify-content:center;gap:.3rem;padding:1.5rem 0;flex-wrap:wrap;font-family:system-ui,-apple-system,sans-serif}
.brxdis-pagination__btn{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;background:#fff;color:#374151;text-decoration:none;font-size:.875rem;transition:all .15s;white-space:nowrap}
.brxdis-pagination__btn:hover{background:#f3f4f6;border-color:#d1d5db}
.brxdis-pagination__btn[aria-current="page"]{background:#2563eb;border-color:#2563eb;color:#fff;font-weight:600;pointer-events:none}
.brxdis-pagination__disabled{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1px solid #e5e7eb;border-radius:7px;background:#f9fafb;color:#d1d5db;font-size:.875rem;cursor:default;white-space:nowrap}
.brxdis-pagination__ellipsis{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;color:#9ca3af;font-size:.875rem}
</style>
</@hst.headContribution>
<@hst.headContribution keyHint="brxdis-product-grid-css">
<style>
.brxdis-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:1.25rem;padding:1rem 0;font-family:system-ui,-apple-system,sans-serif}
.brxdis-card{background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;transition:box-shadow .2s,transform .2s}
.brxdis-card:hover{box-shadow:0 8px 28px rgba(0,0,0,.1);transform:translateY(-3px)}
.brxdis-card__img{aspect-ratio:4/3;background:#f3f4f6;overflow:hidden;position:relative}
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
.brxdis-empty{padding:3rem 1.5rem;text-align:center;color:#6b7280;font-size:.9375rem;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
</style>
</@hst.headContribution>

<#if brxdis_warning??>
  <div style="border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem">
    &#9888;&nbsp;<strong>Warning:</strong> ${brxdis_warning}
  </div>
</#if>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
<#-- @ftlvariable name="dataBand" type="java.lang.String" -->
<#-- @ftlvariable name="bandConnected" type="java.lang.Boolean" -->
<#if editMode?? && editMode>
  <#assign _band = dataBand!"default">
  <#if bandConnected?? && bandConnected>
    <div style="display:inline-block;margin-bottom:.5rem;background:#dcfce7;border:1px solid #86efac;color:#166534;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
      &#10003; Band: <strong>${_band}</strong> &middot; ${products?size} product<#if products?size != 1>s</#if>
    </div>
  <#elseif _band != "default">
    <div style="display:inline-block;margin-bottom:.5rem;background:#dbeafe;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
      Band: <strong>${_band}</strong>
    </div>
  </#if>
</#if>
<#if products?has_content>
  <div class="brxdis-grid">
    <#list products as product>
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
            <a href="/product?pid=${product.id()!""}">${product.title()!"Untitled product"}</a>
          </h3>
          <p class="brxdis-card__pid">PID:&nbsp;${product.id()!""}</p>
          <#if product.price()??>
            <p class="brxdis-card__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
          </#if>
        </div>
        <a class="brxdis-card__cta" href="/product?pid=${product.id()!""}">View Product</a>
      </article>
    </#list>
  </div>
<#elseif editMode>
  <#if bandConnected?? && bandConnected>
    <div class="brxdis-empty">&#128270; <strong>Product Grid</strong> &mdash; connected to band <strong>${dataBand!"default"}</strong>. Products will appear here once a search query or category is active.</div>
  <#else>
    <div class="brxdis-empty">&#128736; <strong>Product Grid</strong> &mdash; no data source connected. Add a Discovery Search or Category component with a matching band name.</div>
  </#if>
<#else>
  <div class="brxdis-empty">
    <p>&#128269; No products found. Try adjusting your search or filters.</p>
  </div>
</#if>

<#-- Pagination -->
<#if pagination?? && (pagination.totalPages() gt 1)>
  <#assign sr = hstRequest.requestContext.servletRequest>
  <#assign current = pagination.page() + 1>
  <#assign total = pagination.totalPages()>

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

  <nav class="brxdis-pagination" aria-label="Pagination">

    <#-- Previous -->
    <#if current gt 1>
      <a href="${pageHref(current - 1)}" class="brxdis-pagination__btn" aria-label="Previous page">&#8592; Prev</a>
    <#else>
      <span class="brxdis-pagination__disabled">&#8592; Prev</span>
    </#if>

    <#-- Page numbers with ellipsis -->
    <#list 1..total as p>
      <#if p == 1 || p == total || (p gte current - 2 && p lte current + 2)>
        <a href="${pageHref(p)}" class="brxdis-pagination__btn"<#if p == current> aria-current="page"</#if>>${p}</a>
      <#elseif p == 2 || p == total - 1>
        <span class="brxdis-pagination__ellipsis">&#8230;</span>
      </#if>
    </#list>

    <#-- Next -->
    <#if current lt total>
      <a href="${pageHref(current + 1)}" class="brxdis-pagination__btn" aria-label="Next page">Next &#8594;</a>
    <#else>
      <span class="brxdis-pagination__disabled">Next &#8594;</span>
    </#if>

  </nav>
<#elseif editMode && pagination?? && (pagination.totalPages() lte 1)>
  <div style="border:2px dashed #e5e7eb;padding:.75rem 1rem;border-radius:8px;font-size:.8125rem;color:#6b7280;text-align:center">
    &#128736; <strong>Pagination</strong> &mdash; page controls will appear here when results span multiple pages.
  </div>
</#if>
