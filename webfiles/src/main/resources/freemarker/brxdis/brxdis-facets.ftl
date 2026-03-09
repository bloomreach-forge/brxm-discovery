<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-facets-css">
<style>
nav.brxdis-facets{font-family:system-ui,-apple-system,sans-serif;color:#374151;font-size:.875rem;line-height:1.5;position:sticky;top:1rem}

/* Active filter chips */
.brxdis-facets__chips{display:flex;flex-wrap:wrap;align-items:center;gap:.375rem;margin-bottom:1.125rem}
.brxdis-facets__chip{display:inline-flex;align-items:center;gap:.3125rem;background:#eff6ff;border:1px solid #bfdbfe;color:#1e40af;border-radius:2px;font-size:.75rem;font-weight:500;padding:.25rem .5rem .25rem .625rem;text-decoration:none;letter-spacing:.01em;transition:background .12s,border-color .12s,color .12s}
.brxdis-facets__chip:hover{background:#fee2e2;border-color:#fca5a5;color:#991b1b}
.brxdis-facets__chip-x{font-size:.875rem;line-height:1;opacity:.7;margin-top:-1px}
.brxdis-facets__chip-x:hover{opacity:1}
.brxdis-facets__clear{margin-left:.25rem;font-size:.75rem;color:#6b7280;text-decoration:none;padding:.25rem .375rem;border-radius:3px;white-space:nowrap;transition:color .12s,background .12s}
.brxdis-facets__clear:hover{color:#1e40af;background:#eff6ff}

/* Facet groups — collapsible via <details> */
.brxdis-facets__group{border-top:1px solid #e5e7eb}
.brxdis-facets__group:last-child{border-bottom:1px solid #e5e7eb}
.brxdis-facets__group details{padding:0}
.brxdis-facets__group summary{display:flex;justify-content:space-between;align-items:center;padding:.625rem 0;cursor:pointer;list-style:none;user-select:none;outline:none}
.brxdis-facets__group summary::-webkit-details-marker{display:none}
.brxdis-facets__group-name{font-size:.6875rem;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:#6b7280}
.brxdis-facets__chevron{width:12px;height:12px;flex-shrink:0;color:#9ca3af;transition:transform .18s ease}
.brxdis-facets__group details[open] .brxdis-facets__chevron{transform:rotate(180deg)}
.brxdis-facets__group-body{padding-bottom:.625rem;max-height:220px;overflow-y:auto}

/* Facet value list */
.brxdis-facets__list{list-style:none;margin:0;padding:0}
.brxdis-facets__item{margin:0}
.brxdis-facets__item a{display:flex;align-items:center;padding:.3125rem .25rem;border-radius:3px;text-decoration:none;color:#374151;transition:background .12s;gap:.5rem;outline-offset:2px}
.brxdis-facets__item a:hover{background:#f9fafb}
.brxdis-facets__item a:focus-visible{outline:2px solid #2563eb}

/* Checkbox indicator */
.brxdis-facets__check{width:14px;height:14px;border:1.5px solid #d1d5db;border-radius:2px;flex-shrink:0;background:#fff;position:relative;transition:border-color .12s,background .12s;background-repeat:no-repeat;background-position:center;background-size:70%}
.brxdis-facets__item--active .brxdis-facets__check{background-color:#2563eb;border-color:#2563eb;background-image:url("data:image/svg+xml,%3Csvg viewBox='0 0 10 8' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 4l3 3 5-6' stroke='%23fff' stroke-width='1.8' fill='none' stroke-linecap='round'/%3E%3C/svg%3E")}

/* Value label + count */
.brxdis-facets__label{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:.8125rem}
.brxdis-facets__item--active .brxdis-facets__label{font-weight:600;color:#1e40af}
.brxdis-facets__count{font-size:.6875rem;color:#9ca3af;background:#f9fafb;border-radius:999px;padding:.05rem .4rem;flex-shrink:0;font-variant-numeric:tabular-nums}
.brxdis-facets__item--active .brxdis-facets__count{background:#eff6ff;color:#1e40af}

</style>
</@hst.headContribution>

<#-- @ftlvariable name="editMode"        type="java.lang.Boolean" -->
<#-- @ftlvariable name="label"          type="java.lang.String" -->
<#-- @ftlvariable name="labelConnected" type="java.lang.Boolean" -->
<#-- @ftlvariable name="facets"        type="java.util.Map" -->

<#if brxdis_warning??>
  <div style="border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem">
    &#9888;&nbsp;<strong>Warning:</strong> ${brxdis_warning}
  </div>
</#if>

<#if editMode?? && editMode>
  <#assign _label = label!"default">
  <#if labelConnected?? && labelConnected>
    <#assign _fc = facets?size>
    <div style="display:inline-block;margin-bottom:.5rem;background:#dcfce7;border:1px solid #86efac;color:#166534;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
      &#10003; Label: <strong>${_label}</strong> &middot; ${_fc} filter group<#if _fc != 1>s</#if>
    </div>
  <#elseif _label != "default">
    <div style="display:inline-block;margin-bottom:.5rem;background:#dbeafe;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
      Label: <strong>${_label}</strong>
    </div>
  </#if>
</#if>

<#if facets?has_content>
  <#assign sr = hstRequest.requestContext.servletRequest>

  <#-- URL helpers -->
  <#function buildUrl extraKey="" extraVal="" removeKey="" removeVal="">
    <#local p = []>
    <#list sr.parameterMap?keys as k>
      <#if k != "page">
        <#list sr.parameterMap[k] as v>
          <#if !(k == removeKey && v == removeVal)>
            <#local p = p + [k?url('UTF-8') + "=" + v?url('UTF-8')]>
          </#if>
        </#list>
      </#if>
    </#list>
    <#if extraKey != ""><#local p = p + [extraKey + "=" + extraVal?url('UTF-8')]></#if>
    <#return "?" + p?join("&")>
  </#function>

  <#function buildClearAllUrl>
    <#local p = []>
    <#list sr.parameterMap?keys as k>
      <#if k != "page" && !k?starts_with("filter.")>
        <#list sr.parameterMap[k] as v>
          <#local p = p + [k?url('UTF-8') + "=" + v?url('UTF-8')]>
        </#list>
      </#if>
    </#list>
    <#if p?has_content><#return "?" + p?join("&")><#else><#return "?"></#if>
  </#function>

  <nav class="brxdis-facets" aria-label="Filter results">

    <#-- Active chips -->
    <#assign anyActive = false>
    <#list facets?values as facet>
      <#if (sr.getParameterValues("filter." + facet.name()))??><#assign anyActive = true></#if>
    </#list>

    <#if anyActive>
      <div class="brxdis-facets__chips">
        <#list facets?values as facet>
          <#assign fp = "filter." + facet.name()>
          <#assign activeVals = (sr.getParameterValues(fp))![]>
          <#list activeVals as av>
            <a href="${buildUrl("","",fp,av)}" class="brxdis-facets__chip" title="Remove filter: ${facet.name()} = ${av}">
              <span>${facet.name()}: ${av}</span>
              <span class="brxdis-facets__chip-x" aria-hidden="true">&#x2715;</span>
            </a>
          </#list>
        </#list>
        <a href="${buildClearAllUrl()}" class="brxdis-facets__clear" title="Remove all active filters">Clear all</a>
      </div>
    </#if>

    <#-- Facet groups -->
    <#list facets?values as facet>
      <#assign fp = "filter." + facet.name()>
      <#assign activeVals = (sr.getParameterValues(fp))![]>
      <div class="brxdis-facets__group">
        <details open>
          <summary>
            <span class="brxdis-facets__group-name">${facet.name()}</span>
            <svg class="brxdis-facets__chevron" width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M 2.5 4.5 L 6 8 L 9.5 4.5"/></svg>
          </summary>
          <div class="brxdis-facets__group-body">
            <ul class="brxdis-facets__list" role="group" aria-label="${facet.name()} filters">
              <#list facet.value() as fv>
                <#assign isActive = activeVals?seq_contains(fv.name())>
                <li class="brxdis-facets__item<#if isActive> brxdis-facets__item--active</#if>">
                  <#if isActive>
                    <a href="${buildUrl("","",fp,fv.name())}" role="checkbox" aria-checked="true" title="Remove ${facet.name()}: ${fv.name()}">
                  <#else>
                    <a href="${buildUrl(fp,fv.name(),"","")}" role="checkbox" aria-checked="false" title="Filter by ${facet.name()}: ${fv.name()}">
                  </#if>
                    <span class="brxdis-facets__check" aria-hidden="true"></span>
                    <span class="brxdis-facets__label">${fv.name()}</span>
                    <span class="brxdis-facets__count">${fv.count()}</span>
                  </a>
                </li>
              </#list>
            </ul>
          </div>
        </details>
      </div>
    </#list>

  </nav>

<#elseif editMode?? && editMode>
  <#assign _label = label!"default">
  <#if labelConnected?? && labelConnected>
    <div class="brxdis-empty">&#128270; <strong>Facets</strong> &mdash; connected to label <strong>${_label}</strong>.<br>Filters appear here once a search or category query is active.</div>
  <#else>
    <div class="brxdis-empty">&#128736; <strong>Facets</strong> &mdash; no data source connected.<br>Add a Discovery Search or Category component with a matching label.</div>
  </#if>
</#if>
