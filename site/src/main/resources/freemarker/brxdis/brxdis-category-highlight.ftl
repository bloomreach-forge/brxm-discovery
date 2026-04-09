<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#assign resolvedCategoryPage><@hst.link path="/category"/></#assign>
<#assign resolvedCategoryPage = resolvedCategoryPage?trim>
<@hst.headContribution keyHint="brxdis-cathighlight-css">
<style>
.brxdis-cathighlight{font-family:system-ui,-apple-system,sans-serif;margin:1rem 0}
.brxdis-cathighlight__title{font-size:1.125rem;font-weight:700;color:#111827;margin:0 0 .875rem}
.brxdis-cathighlight__misconfig{border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem}
.brxdis-cathighlight__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:1rem}
.brxdis-cathighlight__tile{display:flex;flex-direction:column;align-items:center;justify-content:center;padding:1.5rem 1rem;background:#f9fafb;border:1px solid #e5e7eb;border-radius:12px;text-decoration:none;color:#111827;transition:background .2s,box-shadow .2s,transform .2s;text-align:center;min-height:110px}
.brxdis-cathighlight__tile:hover{background:#eff6ff;border-color:#bfdbfe;box-shadow:0 4px 12px rgba(37,99,235,.12);transform:translateY(-2px);color:#1d4ed8}
.brxdis-cathighlight__icon{font-size:2rem;margin-bottom:.5rem;line-height:1}
.brxdis-cathighlight__name{font-size:.9375rem;font-weight:600;line-height:1.3}
.brxdis-cathighlight__empty{padding:2rem 1rem;text-align:center;color:#6b7280;font-size:.875rem;border:1px dashed #e5e7eb;border-radius:8px}
.brxdis-cat-products{display:flex;align-items:flex-end;justify-content:center;margin-top:.75rem;padding:4px 4px 2px;overflow:visible}
.brxdis-cat-product-thumb{position:relative;flex-shrink:0;margin-left:-14px;transition:transform .2s}
.brxdis-cat-product-thumb:first-child{margin-left:0}
.brxdis-cat-product-thumb:nth-child(1){transform:rotate(-7deg) translateY(3px);z-index:1}
.brxdis-cat-product-thumb:nth-child(2){transform:rotate(4deg) translateY(-3px);z-index:2}
.brxdis-cat-product-thumb:nth-child(3){transform:rotate(-3deg) translateY(2px);z-index:3}
.brxdis-cat-product-thumb:nth-child(4){transform:rotate(6deg) translateY(-2px);z-index:4}
.brxdis-cat-product-thumb img{width:46px;height:46px;object-fit:cover;border-radius:4px;border:2px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.18)}
.brxdis-thumb-placeholder{width:46px;height:46px;border-radius:4px;border:2px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,.1);background:#f3f4f6;display:flex;align-items:center;justify-content:center;color:#9ca3af;font-size:1.1rem}
</style>
</@hst.headContribution>

<section class="brxdis-cathighlight">
  <h2 class="brxdis-cathighlight__title">Shop by Category</h2>

  <#if brxdis_warning??>
    <div class="brxdis-cathighlight__misconfig">
      &#9888;&nbsp;<strong>Not configured:</strong> ${brxdis_warning}
    </div>
  </#if>

  <#-- @ftlvariable name="categories" type="java.util.List" -->
  <#if categories?? && categories?has_content>
    <div class="brxdis-cathighlight__grid">
      <#-- @ftlvariable name="previewProducts" type="java.util.Map" -->
      <#list categories as cat>
        <a class="brxdis-cathighlight__tile"
           href="${resolvedCategoryPage}?category=${(cat.categoryId()!"")?url('UTF-8')}"
           aria-label="${cat.displayName()!"Category"}">
          <span class="brxdis-cathighlight__icon">&#128722;</span>
          <span class="brxdis-cathighlight__name">${cat.displayName()!"Unnamed"}</span>
          <#assign catProds = (previewProducts!{})[cat.categoryId()!""]![]>
          <#if catProds?has_content>
            <div class="brxdis-cat-products">
              <#list catProds as p>
                <div class="brxdis-cat-product-thumb">
                  <#if (p.imageUrl()!"") != "">
                    <img src="${p.imageUrl()}" alt="${(p.title()!"")?html}" loading="lazy">
                  <#else>
                    <div class="brxdis-thumb-placeholder">&#128795;</div>
                  </#if>
                </div>
              </#list>
            </div>
          </#if>
        </a>
      </#list>
    </div>
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#elseif (editMode!false)>
    <div class="brxdis-cathighlight__empty">&#128736; Select <strong>Category Documents</strong> in component properties to show category tiles.</div>
  <#else>
    <div class="brxdis-cathighlight__empty">No categories available.</div>
  </#if>
</section>
