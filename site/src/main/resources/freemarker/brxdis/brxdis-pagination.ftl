<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#-- @ftlvariable name="pagination"      type="org.bloomreach.forge.discovery.site.service.discovery.search.model.PaginationModel" -->
<#-- @ftlvariable name="editMode"        type="java.lang.Boolean" -->
<#-- @ftlvariable name="label"           type="java.lang.String" -->
<#-- @ftlvariable name="labelConnected"  type="java.lang.Boolean" -->
<#-- @ftlvariable name="brxdis_warning"  type="java.lang.String" -->

<@hst.headContribution keyHint="brxdis-pagination-css">
<style>
.brxdis-pagination{font-family:system-ui,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;gap:.375rem;flex-wrap:wrap;padding:.75rem 0}
.brxdis-pagination__btn{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;padding:0 .625rem;border:1.5px solid #e5e7eb;border-radius:6px;font-size:.875rem;color:#374151;text-decoration:none;background:#fff;transition:border-color .12s,background .12s,color .12s;font-variant-numeric:tabular-nums}
.brxdis-pagination__btn:hover{border-color:#2563eb;color:#2563eb;background:#eff6ff}
.brxdis-pagination__btn--active{border-color:#2563eb;background:#2563eb;color:#fff;font-weight:600;cursor:default}
.brxdis-pagination__btn--active:hover{background:#2563eb;color:#fff}
.brxdis-pagination__btn--disabled{opacity:.4;pointer-events:none;cursor:default}
.brxdis-pagination__ellipsis{display:inline-flex;align-items:center;justify-content:center;min-width:2.25rem;height:2.25rem;font-size:.875rem;color:#9ca3af;user-select:none}
.brxdis-pagination__summary{font-size:.8125rem;color:#6b7280;text-align:center;margin-top:.25rem}
</style>
</@hst.headContribution>

<#if brxdis_warning??>
  <div style="border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem">
    &#9888;&nbsp;<strong>Warning:</strong> ${brxdis_warning}
  </div>
</#if>

<#if pagination?? && pagination.totalPages() gt 1>
  <#assign currentPage = pagination.page()>
  <#assign totalPages  = pagination.totalPages()>
  <#assign sr = hstRequest.requestContext.servletRequest>

  <#function pageUrl p>
    <#local parts = []>
    <#list sr.parameterMap?keys as k>
      <#if k != "page">
        <#list sr.parameterMap[k] as v>
          <#local parts = parts + [k?url('UTF-8') + "=" + v?url('UTF-8')]>
        </#list>
      </#if>
    </#list>
    <#if p gt 0>
      <#local parts = parts + ["page=" + p?c]>
    </#if>
    <#return "?" + parts?join("&")>
  </#function>

  <nav class="brxdis-pagination" aria-label="Pagination">

    <#-- Previous -->
    <a class="brxdis-pagination__btn<#if currentPage == 0> brxdis-pagination__btn--disabled</#if>"
       href="<#if currentPage gt 0>${pageUrl(currentPage - 1)}<#else>#</#if>"
       aria-label="Previous page"
       <#if currentPage == 0>aria-disabled="true"</#if>>&lsaquo; Prev</a>

    <#-- Page numbers with ellipsis (window of ±2 around current) -->
    <#assign window = 2>
    <#assign lo = (currentPage - window)?max(0)>
    <#assign hi = (currentPage + window)?min(totalPages - 1)>

    <#if lo gt 0>
      <a class="brxdis-pagination__btn" href="${pageUrl(0)}" aria-label="Page 1">1</a>
      <#if lo gt 1><span class="brxdis-pagination__ellipsis">…</span></#if>
    </#if>

    <#list lo..hi as p>
      <#if p == currentPage>
        <span class="brxdis-pagination__btn brxdis-pagination__btn--active" aria-current="page">${p + 1}</span>
      <#else>
        <a class="brxdis-pagination__btn" href="${pageUrl(p)}" aria-label="Page ${p + 1}">${p + 1}</a>
      </#if>
    </#list>

    <#if hi lt totalPages - 1>
      <#if hi lt totalPages - 2><span class="brxdis-pagination__ellipsis">…</span></#if>
      <a class="brxdis-pagination__btn" href="${pageUrl(totalPages - 1)}" aria-label="Page ${totalPages}">${totalPages}</a>
    </#if>

    <#-- Next -->
    <a class="brxdis-pagination__btn<#if currentPage >= totalPages - 1> brxdis-pagination__btn--disabled</#if>"
       href="<#if currentPage lt totalPages - 1>${pageUrl(currentPage + 1)}<#else>#</#if>"
       aria-label="Next page"
       <#if currentPage >= totalPages - 1>aria-disabled="true"</#if>>Next &rsaquo;</a>

  </nav>

  <#assign start = currentPage * pagination.pageSize() + 1>
  <#assign end   = ((currentPage + 1) * pagination.pageSize())?min(pagination.total())>
  <p class="brxdis-pagination__summary">Showing ${start}–${end} of ${pagination.total()} results</p>

<#elseif editMode?? && editMode>
  <#assign _label = label!"default">
  <#if labelConnected?? && labelConnected>
    <div class="brxdis-empty">&#128196; <strong>Pagination</strong> &mdash; connected to label <strong>${_label}</strong>.<br>Controls appear once results span more than one page.</div>
  <#else>
    <div class="brxdis-empty">&#128736; <strong>Pagination</strong> &mdash; no data source connected.<br>Add a Discovery Search or Category component with a matching label.</div>
  </#if>
</#if>
