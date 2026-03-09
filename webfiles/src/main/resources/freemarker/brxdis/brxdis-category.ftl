<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-category-css">
<style>
.brxdis-cat{font-family:system-ui,-apple-system,sans-serif;position:relative}
.brxdis-cat__header{margin-bottom:1.25rem}
.brxdis-cat__title{font-size:1.5rem;font-weight:700;color:#111827;margin:0 0 .25rem}
.brxdis-cat__meta{font-size:.875rem;color:#6b7280}
.brxdis-cat__meta strong{color:#111827}
.brxdis-empty{padding:3rem 1rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0;position:relative}
.brxdis-campaign{margin-bottom:1.25rem;border-radius:10px;overflow:hidden}
.brxdis-campaign a{display:block;text-decoration:none}
.brxdis-campaign img{width:100%;display:block}
.brxdis-campaign__text{padding:.75rem 1rem;background:#f0fdf4;border:1px solid #bbf7d0;color:#166534;font-size:.875rem}
</style>
</@hst.headContribution>

<#-- @ftlvariable name="editMode"  type="java.lang.Boolean" -->
<#-- @ftlvariable name="label"    type="java.lang.String" -->
<#-- @ftlvariable name="campaign" type="org.bloomreach.forge.discovery.site.service.discovery.search.model.Campaign" -->
<#if editMode?? && editMode && label?has_content && label != "default">
  <div style="display:inline-block;margin-bottom:.5rem;background:#dbeafe;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
    Label: <strong>${label}</strong>
  </div>
</#if>
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
<#if categoryResult??>
  <#assign start = categoryResult.page() * categoryResult.pageSize() + 1>
  <#assign end = ((categoryResult.page() + 1) * categoryResult.pageSize())>
  <#if end gt categoryResult.total()><#assign end = categoryResult.total()></#if>
  <div class="brxdis-cat">
    <#if document??>
      <@hst.manageContent hippobean=document parameterName="document" rootPath="brxm-discovery"/>
    <#else>
      <@hst.manageContent parameterName="document" rootPath="brxm-discovery"/>
    </#if>
    <div class="brxdis-cat__header">
      <h1 class="brxdis-cat__title">${categoryId!""}</h1>
      <p class="brxdis-cat__meta">
        <strong>${categoryResult.total()}</strong> products
        <#if categoryResult.total() gt 0> &mdash; showing ${start}&ndash;${end}</#if>
      </p>
    </div>
  </div>
<#elseif editMode?? && editMode>
  <div class="brxdis-empty">
    <@hst.manageContent parameterName="document" rootPath="brxm-discovery"/>
    &#128736; <strong>Discovery Category</strong> &mdash; select a Category Document in component properties.
  </div>
</#if>
