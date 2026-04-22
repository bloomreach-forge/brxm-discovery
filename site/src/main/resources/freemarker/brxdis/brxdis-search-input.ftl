<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#function slugify text>
  <#local s = (text!"")?lower_case>
  <#local s = s?replace("[^a-z0-9]+", "-", "r")>
  <#local s = s?replace("^-+|-+$", "", "r")>
  <#if s?has_content><#return s><#else><#return "product"></#if>
</#function>
<#if resultsPage?has_content>
  <#assign resolvedResultsPage><@hst.link path="${resultsPage}"/></#assign>
  <#assign resolvedResultsPage = resolvedResultsPage?trim>
<#else>
  <#assign resolvedResultsPage = "">
</#if>
<#assign resolvedProductPage><@hst.link path="/product"/></#assign>
<#assign resolvedProductPage = resolvedProductPage?trim>

<#-- @ftlvariable name="suggestionsEnabled" type="java.lang.Boolean" -->
<#-- @ftlvariable name="resultsPage"        type="java.lang.String" -->
<#-- @ftlvariable name="minChars"           type="java.lang.Integer" -->
<#-- @ftlvariable name="debounceMs"         type="java.lang.Integer" -->
<#-- @ftlvariable name="placeholder"        type="java.lang.String" -->
<#-- @ftlvariable name="query"              type="java.lang.String" -->
<#-- @ftlvariable name="autosuggestResult"  type="org.bloomreach.forge.discovery.search.model.AutosuggestResult" -->
<#-- @ftlvariable name="editMode"           type="java.lang.Boolean" -->

<@hst.headContribution keyHint="brxdis-search-input-css">
<style>
.brxdis-sb{font-family:system-ui,-apple-system,sans-serif;position:relative}
.brxdis-sb__form{display:flex;gap:.5rem}
.brxdis-sb__input{flex:1;padding:.625rem .875rem;border:1.5px solid #e5e7eb;border-radius:8px;font-size:.9375rem;outline:none;transition:border-color .15s}
.brxdis-sb__input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.brxdis-sb__submit{padding:.625rem 1.25rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:.9375rem;font-weight:500;cursor:pointer;white-space:nowrap;transition:background .15s}
.brxdis-sb__submit:hover{background:#1d4ed8}
.brxdis-as__panel{border:1px solid #e5e7eb;border-radius:10px;background:#fff;box-shadow:0 8px 24px rgba(0,0,0,.12);padding:1rem;margin-top:.25rem;position:absolute;left:0;right:0;z-index:100}
.brxdis-as__section{margin-bottom:1rem}
.brxdis-as__section:last-child{margin-bottom:0}
.brxdis-as__heading{font-size:.6875rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#9ca3af;margin:0 0 .5rem}
.brxdis-as__list{list-style:none;margin:0;padding:0}
.brxdis-as__list li{padding:.375rem 0;font-size:.875rem;color:#374151;border-bottom:1px solid #f3f4f6}
.brxdis-as__list li:last-child{border-bottom:none}
.brxdis-as__list a{color:#374151;text-decoration:none}
.brxdis-as__list a:hover{color:#2563eb}
.brxdis-as__products{display:flex;gap:.75rem;overflow-x:auto;padding:.25rem 0}
.brxdis-as__prod{flex:0 0 140px;text-align:center;text-decoration:none;color:#111827;display:block}
.brxdis-as__prod:hover{color:#2563eb}
.brxdis-as__prod-img{width:100%;aspect-ratio:1;background:#f3f4f6;border-radius:8px;overflow:hidden;margin-bottom:.375rem}
.brxdis-as__prod-img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-as__prod-title{font-size:.75rem;font-weight:500;line-height:1.3;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-as__prod-price{font-size:.8125rem;font-weight:700;margin-top:.125rem}
.brxdis-as__empty{padding:1.5rem;text-align:center;color:#9ca3af;font-size:.875rem}
</style>
</@hst.headContribution>

<div class="brxdis-sb"
     data-suggestions-enabled="${(suggestionsEnabled!true)?c}"
     data-min-chars="${minChars!2}"
     data-debounce="${debounceMs!250}"
     data-results-page="${resolvedResultsPage}">

  <form class="brxdis-sb__form" method="get" action="${resolvedResultsPage}">
    <input type="hidden" name="brxdis_event" value="search-submit"/>
    <input id="brxdis-sb-input"
           class="brxdis-sb__input"
           type="search"
           name="q"
           value="${query!""}"
           placeholder="${placeholder!"Search..."}"
           autocomplete="off"
           aria-label="${placeholder!"Search"}"/>
    <button class="brxdis-sb__submit" type="submit">Search</button>
  </form>

  <div id="brxdis-sb-panel" class="brxdis-as__panel" hidden>
    <#if autosuggestResult??>
      <@autosuggestPanel autosuggestResult=autosuggestResult />
    </#if>
  </div>

</div>
<script>
(function () {
  'use strict';
  var wrapper = document.currentScript.previousElementSibling;
  if (!wrapper) return;
  var enabled    = wrapper.dataset.suggestionsEnabled === 'true';
  var minChars   = parseInt(wrapper.dataset.minChars,  10) || 2;
  var debounce   = parseInt(wrapper.dataset.debounce,  10) || 250;
  var resultsUrl = wrapper.dataset.resultsPage || window.location.pathname;
  var input      = wrapper.querySelector('#brxdis-sb-input');
  var panel      = wrapper.querySelector('#brxdis-sb-panel');

  if (!enabled || !input || !panel) return;

  var timer = null;

  function showPanel(html) {
    var doc = new DOMParser().parseFromString(html, 'text/html');
    var src = doc.getElementById('brxdis-sb-panel');
    if (!src) return;
    panel.replaceChildren.apply(panel, Array.from(src.childNodes));
    panel.removeAttribute('hidden');
  }

  function hidePanel() {
    panel.setAttribute('hidden', '');
    panel.replaceChildren();
  }

  function fetchSuggestions(q) {
    var url = (resultsUrl || window.location.pathname)
            + '?q=' + encodeURIComponent(q)
            + '&brxdis_suggest=1';
    fetch(url, {headers: {'X-Requested-With': 'XMLHttpRequest'}})
      .then(function (r) { return r.ok ? r.text() : Promise.reject(r.status); })
      .then(showPanel)
      .catch(hidePanel);
  }

  input.addEventListener('input', function () {
    clearTimeout(timer);
    var q = input.value.trim();
    if (q.length < minChars) { hidePanel(); return; }
    timer = setTimeout(function () { fetchSuggestions(q); }, debounce);
  });

  document.addEventListener('click', function (e) {
    if (!wrapper.contains(e.target)) hidePanel();
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') hidePanel();
  });
}());
</script>

<#-- ── Autosuggest panel macro ─────────────────────────────────────────── -->
<#macro autosuggestPanel autosuggestResult>
  <#assign productSuggestQuery = autosuggestResult.originalQuery()!"">
  <#if autosuggestResult.querySuggestions()?has_content>
    <#assign productSuggestQuery = autosuggestResult.querySuggestions()[0]>
  </#if>

  <#if autosuggestResult.querySuggestions()?has_content>
    <div class="brxdis-as__section">
      <p class="brxdis-as__heading">Suggestions</p>
      <ul class="brxdis-as__list">
        <#list autosuggestResult.querySuggestions() as s>
          <li>
            <a href="${resolvedResultsPage}?q=${s?url('UTF-8')}&brxdis_event=suggest-click&brxdis_aq=${(autosuggestResult.originalQuery()!"")?url('UTF-8')}">${s}</a>
          </li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if autosuggestResult.attributeSuggestions()?has_content>
    <div class="brxdis-as__section">
      <p class="brxdis-as__heading">Filters</p>
      <ul class="brxdis-as__list">
        <#list autosuggestResult.attributeSuggestions() as attr>
          <li>${attr.name()}: ${attr.value()}</li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if autosuggestResult.productSuggestions()?has_content>
    <div class="brxdis-as__section">
      <p class="brxdis-as__heading">Products</p>
      <div class="brxdis-as__products">
        <#list autosuggestResult.productSuggestions() as product>
          <#assign _asPid  = (product.id()!"")?url('UTF-8')>
          <#assign _asSlug = slugify(product.title()!"")>
          <a class="brxdis-as__prod"
             href="${resolvedProductPage}/${_asSlug}/pid/${_asPid}?_br_psugg_q=${productSuggestQuery?url('UTF-8')}">
            <div class="brxdis-as__prod-img">
              <#if product.imageUrl()?has_content>
                <img src="${product.imageUrl()}" alt="${product.title()!""}"/>
              </#if>
            </div>
            <div class="brxdis-as__prod-title">${product.title()!"Untitled"}</div>
            <#if product.price()??>
              <div class="brxdis-as__prod-price">${product.currency()!""} ${product.price()?string("0.00")}</div>
            </#if>
          </a>
        </#list>
      </div>
    </div>
  </#if>

  <#if !autosuggestResult.querySuggestions()?has_content
       && !autosuggestResult.attributeSuggestions()?has_content
       && !autosuggestResult.productSuggestions()?has_content>
    <div class="brxdis-as__empty">No suggestions found.</div>
  </#if>
</#macro>
