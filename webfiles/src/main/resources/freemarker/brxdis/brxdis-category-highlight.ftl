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
      <#list categories as cat>
        <a class="brxdis-cathighlight__tile"
           href="${resolvedCategoryPage}?cat_id=${cat.categoryId!""}"
           aria-label="${cat.displayName!"Category"}">
          <span class="brxdis-cathighlight__icon">&#128722;</span>
          <span class="brxdis-cathighlight__name">${cat.displayName!"Unnamed"}</span>
        </a>
      </#list>
    </div>
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#elseif editMode>
    <div class="brxdis-cathighlight__empty">&#128736; Select <strong>Category Documents</strong> in component properties to show category tiles.</div>
  <#else>
    <div class="brxdis-cathighlight__empty">No categories available.</div>
  </#if>
</section>
