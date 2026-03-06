<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-recs-css">
<style>
.brxdis-recs{font-family:system-ui,-apple-system,sans-serif;margin:1rem 0}
.brxdis-recs__header{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:.875rem}
.brxdis-recs__title{font-size:1.125rem;font-weight:700;color:#111827;margin:0}
.brxdis-recs__widget{font-size:.75rem;color:#9ca3af}
.brxdis-recs__misconfig{border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem}
.brxdis-recs__track{display:flex;gap:1rem;overflow-x:auto;padding-bottom:.75rem;scroll-snap-type:x mandatory;-ms-overflow-style:none;scrollbar-width:none}
.brxdis-recs__track::-webkit-scrollbar{display:none}
.brxdis-recs__card{flex:0 0 200px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;scroll-snap-align:start;transition:box-shadow .2s,transform .2s}
.brxdis-recs__card:hover{box-shadow:0 6px 20px rgba(0,0,0,.1);transform:translateY(-2px)}
.brxdis-recs__img{aspect-ratio:1;background:#f3f4f6;overflow:hidden}
.brxdis-recs__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-recs__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:2.25rem}
.brxdis-recs__body{padding:.75rem;flex:1;display:flex;flex-direction:column;gap:.25rem}
.brxdis-recs__name{font-size:.875rem;font-weight:600;color:#111827;line-height:1.35;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-recs__name a{color:inherit;text-decoration:none}
.brxdis-recs__name a:hover{color:#2563eb}
.brxdis-recs__price{font-size:.9375rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.375rem}
.brxdis-recs__cta{margin:.25rem .75rem .75rem;padding:.4375rem;background:#2563eb;color:#fff;border:none;border-radius:6px;font-size:.8125rem;font-weight:500;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-recs__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-recs__empty{padding:2rem 1rem;text-align:center;color:#6b7280;font-size:.875rem;border:1px dashed #e5e7eb;border-radius:8px}
</style>
</@hst.headContribution>

<#if document??>
  <@hst.manageContent hippobean=document parameterName="document" rootPath="test" defaultPath="test"/>
<#else>
  <@hst.manageContent parameterName="document" rootPath="test" defaultPath="test"/>
</#if>

<section class="brxdis-recs">
  <div class="brxdis-recs__header">
    <h2 class="brxdis-recs__title">You May Also Like</h2>
    <#if widgetId?has_content>
      <span class="brxdis-recs__widget">Widget:&nbsp;${widgetId}</span>
    </#if>
  </div>

  <#if brxdis_warning??>
    <div class="brxdis-recs__misconfig">
      &#9888;&nbsp;<strong>Not configured:</strong> ${brxdis_warning}
    </div>
  </#if>

  <#if products?? && products?has_content>
    <div class="brxdis-recs__track" role="list">
      <#list products as product>
        <article class="brxdis-recs__card" role="listitem">
          <div class="brxdis-recs__img">
            <#if product.imageUrl()?has_content>
              <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
            <#else>
              <div class="brxdis-recs__placeholder">&#128722;</div>
            </#if>
          </div>
          <div class="brxdis-recs__body">
            <h3 class="brxdis-recs__name">
              <a href="${product.url()!""}">${product.title()!"Untitled"}</a>
            </h3>
            <#if product.price()??>
              <p class="brxdis-recs__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
            </#if>
          </div>
          <a class="brxdis-recs__cta" href="${product.url()!""}">View Product</a>
        </article>
      </#list>
    </div>
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#elseif editMode>
    <div class="brxdis-recs__empty">&#128736; Select a <strong>Recommendation Document</strong> in component properties to show recommendations.</div>
  <#else>
    <div class="brxdis-recs__empty">No recommendations available.</div>
  </#if>
</section>
