<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-facets-css">
<style>
.brxdis-facets{font-family:system-ui,-apple-system,sans-serif}
.brxdis-facets__chips{display:flex;flex-wrap:wrap;gap:.375rem;margin-bottom:1rem}
.brxdis-facets__chip{display:inline-flex;align-items:center;gap:.3rem;background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.8125rem;padding:.25rem .625rem;text-decoration:none;transition:all .15s}
.brxdis-facets__chip:hover{background:#fee2e2;border-color:#fca5a5;color:#991b1b}
.brxdis-facets__group{margin-bottom:1.125rem;border-bottom:1px solid #f3f4f6;padding-bottom:1.125rem}
.brxdis-facets__group:last-child{border-bottom:none;padding-bottom:0}
.brxdis-facets__heading{font-size:.75rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#374151;margin:0 0 .5rem}
.brxdis-facets__list{list-style:none;margin:0;padding:0}
.brxdis-facets__item{margin:.1rem 0}
.brxdis-facets__item a{display:flex;align-items:center;justify-content:space-between;padding:.3125rem .375rem;border-radius:6px;text-decoration:none;font-size:.875rem;color:#374151;transition:background .15s;gap:.5rem}
.brxdis-facets__item a:hover{background:#f3f4f6}
.brxdis-facets__item--active a{background:#eff6ff;color:#1e40af;font-weight:600}
.brxdis-facets__item--active a:hover{background:#fee2e2;color:#991b1b}
.brxdis-facets__label{display:flex;align-items:center;gap:.4rem;flex:1;min-width:0}
.brxdis-facets__check{width:14px;height:14px;border:1.5px solid #d1d5db;border-radius:3px;display:inline-block;flex-shrink:0;background:#fff}
.brxdis-facets__item--active .brxdis-facets__check{background:#2563eb;border-color:#2563eb;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 10 8' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 4l3 3 5-6' stroke='%23fff' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");background-repeat:no-repeat;background-position:center;background-size:70%}
.brxdis-facets__value{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.brxdis-facets__count{font-size:.75rem;color:#9ca3af;background:#f3f4f6;border-radius:999px;padding:.05rem .45rem;flex-shrink:0}
.brxdis-facets__item--active .brxdis-facets__count{background:#dbeafe;color:#1e40af}
</style>
</@hst.headContribution>

<#if brxdis_warning??>
  <div style="border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem">
    &#9888;&nbsp;<strong>Warning:</strong> ${brxdis_warning}
  </div>
</#if>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
<#if facets?has_content>
  <#assign sr = hstRequest.requestContext.servletRequest>

  <#function buildUrl extraKey="" extraVal="" removeKey="" removeVal="">
    <#local p = []>
    <#list sr.parameterMap?keys as k>
      <#if k != "page">
        <#list sr.parameterMap[k] as v>
          <#if !(k == removeKey && v == removeVal)>
            <#local p = p + [k + "=" + v?url('UTF-8')]>
          </#if>
        </#list>
      </#if>
    </#list>
    <#if extraKey != ""><#local p = p + [extraKey + "=" + extraVal?url('UTF-8')]></#if>
    <#return "?" + p?join("&")>
  </#function>

  <nav class="brxdis-facets" aria-label="Filters">

    <#-- Active filter chips -->
    <#assign anyActive = false>
    <#list facets?values as facet>
      <#if (sr.getParameterValues("filter." + facet.name()))??><#assign anyActive = true></#if>
    </#list>

    <#if anyActive>
      <div class="brxdis-facets__chips">
        <#list facets?values as facet>
          <#assign fp = "filter." + facet.name()>
          <#assign active = (sr.getParameterValues(fp))![]>
          <#list active as av>
            <a href="${buildUrl("","",fp,av)}" class="brxdis-facets__chip" title="Remove ${facet.name()}: ${av}">
              ${facet.name()}: ${av} &#x2715;
            </a>
          </#list>
        </#list>
      </div>
    </#if>

    <#-- Facet groups -->
    <#list facets?values as facet>
      <#assign fp = "filter." + facet.name()>
      <#assign activeVals = (sr.getParameterValues(fp))![]>
      <div class="brxdis-facets__group">
        <p class="brxdis-facets__heading">${facet.name()}</p>
        <ul class="brxdis-facets__list">
          <#list facet.values() as fv>
            <#assign isActive = activeVals?seq_contains(fv.value())>
            <li class="brxdis-facets__item<#if isActive> brxdis-facets__item--active</#if>">
              <#if isActive>
                <a href="${buildUrl("","",fp,fv.value())}" aria-pressed="true">
              <#else>
                <a href="${buildUrl(fp,fv.value(),"","")}">
              </#if>
                <span class="brxdis-facets__label">
                  <span class="brxdis-facets__check"></span>
                  <span class="brxdis-facets__value">${fv.value()}</span>
                </span>
                <span class="brxdis-facets__count">${fv.count()}</span>
              </a>
            </li>
          </#list>
        </ul>
      </div>
    </#list>

  </nav>
<#elseif editMode>
  <div style="border:2px dashed #e5e7eb;padding:1rem;border-radius:8px;font-size:.8125rem;color:#6b7280;text-align:center">
    &#128736; <strong>Facets</strong> &mdash; facet filters will appear here once the parent search or category component returns results.
  </div>
</#if>
