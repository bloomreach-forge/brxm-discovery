<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-pdp-css">
<style>
.brxdis-pdp{font-family:system-ui,-apple-system,sans-serif;padding:1.5rem 0}
.brxdis-pdp__hero{display:flex;gap:2rem;align-items:flex-start;flex-wrap:wrap}
.brxdis-pdp__img{flex:0 0 420px;max-width:100%;border-radius:12px;overflow:hidden;background:#f3f4f6;aspect-ratio:4/3;display:flex;align-items:center;justify-content:center}
.brxdis-pdp__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-pdp__img-placeholder{font-size:5rem;color:#d1d5db}
.brxdis-pdp__details{flex:1;min-width:260px;display:flex;flex-direction:column;gap:.75rem}
.brxdis-pdp__brand{font-size:.8125rem;font-weight:600;color:#6b7280;text-transform:uppercase;letter-spacing:.05em;margin:0}
.brxdis-pdp__title{font-size:1.75rem;font-weight:800;color:#111827;margin:0;line-height:1.25}
.brxdis-pdp__pid{font-size:.75rem;color:#9ca3af;margin:0}
.brxdis-pdp__price{font-size:1.5rem;font-weight:700;color:#111827;margin:.25rem 0}
.brxdis-pdp__desc{font-size:.9375rem;color:#374151;line-height:1.65;margin:.25rem 0}
.brxdis-pdp__cta{margin-top:.75rem;padding:.75rem 1.5rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:1rem;font-weight:600;cursor:pointer;text-align:center;text-decoration:none;display:inline-block;transition:background .15s;align-self:flex-start}
.brxdis-pdp__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-pdp__notfound{padding:3rem 1.5rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px}
.brxdis-pdp__notfound h2{font-size:1.25rem;margin:0 0 .5rem}
</style>
</@hst.headContribution>

<#if document??>
  <@hst.manageContent hippobean=document parameterName="document" rootPath="brxdis/products" defaultPath="brxdis/products"/>
<#else>
  <@hst.manageContent parameterName="document" rootPath="brxdis/products" defaultPath="brxdis/products"/>
</#if>

<div class="brxdis-pdp">
  <#if product??>
    <#-- Product hero section -->
    <div class="brxdis-pdp__hero">
      <div class="brxdis-pdp__img">
        <#if product.imageUrl()?has_content>
          <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
        <#else>
          <div class="brxdis-pdp__img-placeholder">&#128722;</div>
        </#if>
      </div>
      <div class="brxdis-pdp__details">
        <#if product.attributes()?? && product.attributes()["brand"]?has_content>
          <p class="brxdis-pdp__brand">${product.attributes()["brand"]}</p>
        </#if>
        <h1 class="brxdis-pdp__title">${product.title()!"Untitled product"}</h1>
        <p class="brxdis-pdp__pid">PID:&nbsp;${product.id()!""}</p>
        <#if product.price()??>
          <p class="brxdis-pdp__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
        </#if>
        <#if product.attributes()?? && product.attributes()["description"]?has_content>
          <p class="brxdis-pdp__desc">${product.attributes()["description"]}</p>
        </#if>
        <a class="brxdis-pdp__cta" href="#">Add to Cart</a>
      </div>
    </div>

  <#else>
    <div class="brxdis-pdp__notfound">
      <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
      <#if editMode>
        <h2>&#128736; Not configured</h2>
        <p>Select a <strong>Product Detail Document</strong> in component properties, or add <code>?pid=&lt;product-id&gt;</code> to the URL.</p>
      <#else>
        <h2>&#128269; Product not found</h2>
        <#if RequestContext.getServletRequest().getParameter("pid")?has_content>
          <p>No product found for PID: <code>${RequestContext.getServletRequest().getParameter("pid")}</code></p>
        <#else>
          <p>No product ID specified. Add <code>?pid=&lt;product-id&gt;</code> to the URL.</p>
        </#if>
      </#if>
    </div>
  </#if>
</div>
