<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-search-css">
<style>
.brxdis-search{font-family:system-ui,-apple-system,sans-serif}
.brxdis-search__form{display:flex;gap:.5rem;margin-bottom:1.25rem}
.brxdis-search__input{flex:1;padding:.625rem .875rem;border:1.5px solid #e5e7eb;border-radius:8px;font-size:.9375rem;outline:none;transition:border-color .15s}
.brxdis-search__input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.brxdis-search__submit{padding:.625rem 1.25rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:.9375rem;font-weight:500;cursor:pointer;white-space:nowrap;transition:background .15s}
.brxdis-search__submit:hover{background:#1d4ed8}
.brxdis-search__meta{font-size:.875rem;color:#6b7280;margin-bottom:1rem}
.brxdis-search__meta strong{color:#111827}
.brxdis-empty{padding:3rem 1rem;text-align:center;color:#6b7280;border:2px dashed #e5e7eb;border-radius:10px;margin:1rem 0}
</style>
</@hst.headContribution>

<div class="brxdis-search">
  <#-- @ftlvariable name="dataBand" type="java.lang.String" -->
  <#if editMode?? && editMode && dataBand?has_content && dataBand != "default">
    <div style="display:inline-block;margin-bottom:.5rem;background:#dbeafe;border:1px solid #bfdbfe;color:#1e40af;border-radius:999px;font-size:.75rem;font-weight:600;padding:.2rem .65rem">
      Writes to band: <strong>${dataBand}</strong>
    </div>
  </#if>
  <form class="brxdis-search__form" method="get" action="">
    <input class="brxdis-search__input" type="search" name="q" value="${query!""}" placeholder="Search products…" autofocus/>
    <button class="brxdis-search__submit" type="submit">Search</button>
  </form>

  <#if searchResult??>
    <#assign start = searchResult.page() * searchResult.pageSize() + 1>
    <#assign end = ((searchResult.page() + 1) * searchResult.pageSize())>
    <#if end gt searchResult.total()><#assign end = searchResult.total()></#if>
    <p class="brxdis-search__meta">
      <strong>${searchResult.total()}</strong> results
      <#if searchResult.total() gt 0> &mdash; showing ${start}&ndash;${end}</#if>
    </p>
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#elseif editMode?? && editMode>
    <div class="brxdis-empty">&#128736; <strong>Discovery Search</strong> &mdash; type a query in the field above to preview results in the CMS.</div>
  <#else>
    <div class="brxdis-empty">Enter a search term above to find products.</div>
  </#if>
</div>
