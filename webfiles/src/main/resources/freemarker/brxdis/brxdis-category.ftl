<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-category-css">
<style>
.brxdis-cat{font-family:system-ui,-apple-system,sans-serif}
.brxdis-cat__header{margin-bottom:1.25rem}
.brxdis-cat__title{font-size:1.5rem;font-weight:700;color:#111827;margin:0 0 .25rem}
.brxdis-cat__meta{font-size:.875rem;color:#6b7280}
.brxdis-cat__meta strong{color:#111827}
.brxdis-empty{padding:3rem 1rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
</style>
</@hst.headContribution>

<#if document??>
  <@hst.manageContent hippobean=document parameterName="document" rootPath="brxm-discovery" defaultPath="brxm-discovery"/>
<#else>
  <@hst.manageContent parameterName="document" rootPath="brxm-discovery" defaultPath="brxm-discovery"/>
</#if>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
<#-- @ftlvariable name="dataBand" type="java.lang.String" -->
<#if editMode?? && editMode && dataBand?has_content && dataBand != "default">
  <div style="display:inline-block;margin-bottom:.5rem;background:#dbeafe;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
    Writes to band: <strong>${dataBand}</strong>
  </div>
</#if>
<#if categoryResult??>
  <#assign start = categoryResult.page() * categoryResult.pageSize() + 1>
  <#assign end = ((categoryResult.page() + 1) * categoryResult.pageSize())>
  <#if end gt categoryResult.total()><#assign end = categoryResult.total()></#if>
  <div class="brxdis-cat">
    <div class="brxdis-cat__header">
      <h1 class="brxdis-cat__title">${categoryId!""}</h1>
      <p class="brxdis-cat__meta">
        <strong>${categoryResult.total()}</strong> products
        <#if categoryResult.total() gt 0> &mdash; showing ${start}&ndash;${end}</#if>
      </p>
    </div>
  </div>
<#elseif editMode?? && editMode>
  <div class="brxdis-empty">&#128736; <strong>Discovery Category</strong> &mdash; select a Category Document in component properties.</div>
</#if>
