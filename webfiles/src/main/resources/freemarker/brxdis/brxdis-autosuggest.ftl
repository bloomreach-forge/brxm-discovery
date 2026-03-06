<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-autosuggest-css">
<style>
.brxdis-as{font-family:system-ui,-apple-system,sans-serif;position:relative}
.brxdis-as__form{display:flex;gap:.5rem;margin-bottom:.25rem}
.brxdis-as__input{flex:1;padding:.625rem .875rem;border:1.5px solid #e5e7eb;border-radius:8px;font-size:.9375rem;outline:none;transition:border-color .15s}
.brxdis-as__input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.1)}
.brxdis-as__submit{padding:.625rem 1.25rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:.9375rem;font-weight:500;cursor:pointer;transition:background .15s}
.brxdis-as__submit:hover{background:#1d4ed8}
.brxdis-as__panel{border:1px solid #e5e7eb;border-radius:10px;background:#fff;box-shadow:0 8px 24px rgba(0,0,0,.12);padding:1rem;margin-top:.25rem}
.brxdis-as__section{margin-bottom:1rem}
.brxdis-as__section:last-child{margin-bottom:0}
.brxdis-as__heading{font-size:.6875rem;font-weight:700;text-transform:uppercase;letter-spacing:.06em;color:#9ca3af;margin:0 0 .5rem}
.brxdis-as__list{list-style:none;margin:0;padding:0}
.brxdis-as__list li{padding:.375rem 0;font-size:.875rem;color:#374151;border-bottom:1px solid #f3f4f6}
.brxdis-as__list li:last-child{border-bottom:none}
.brxdis-as__list a{color:#374151;text-decoration:none}
.brxdis-as__list a:hover{color:#2563eb}
.brxdis-as__attr{font-size:.75rem;color:#6b7280;background:#f3f4f6;display:inline-block;padding:.125rem .5rem;border-radius:4px;margin:.125rem 0}
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

<div class="brxdis-as">
  <form class="brxdis-as__form" method="get" action="">
    <input class="brxdis-as__input" type="search" name="q" value="${query!""}" placeholder="Search products..." autocomplete="off" autofocus/>
    <button class="brxdis-as__submit" type="submit">Search</button>
  </form>

  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#if autosuggestResult??>
    <div class="brxdis-as__panel">

      <#-- Query suggestions -->
      <#if autosuggestResult.querySuggestions()?has_content>
        <div class="brxdis-as__section">
          <p class="brxdis-as__heading">Suggestions</p>
          <ul class="brxdis-as__list">
            <#list autosuggestResult.querySuggestions() as suggestion>
              <li><a href="?q=${suggestion?url('UTF-8')}">${suggestion}</a></li>
            </#list>
          </ul>
        </div>
      </#if>

      <#-- Attribute suggestions -->
      <#if autosuggestResult.attributeSuggestions()?has_content>
        <div class="brxdis-as__section">
          <p class="brxdis-as__heading">Filters</p>
          <ul class="brxdis-as__list">
            <#list autosuggestResult.attributeSuggestions() as attr>
              <li>
                <span class="brxdis-as__attr">${attr.name()}</span>
                ${attr.value()}
              </li>
            </#list>
          </ul>
        </div>
      </#if>

      <#-- Product suggestions -->
      <#if autosuggestResult.productSuggestions()?has_content>
        <div class="brxdis-as__section">
          <p class="brxdis-as__heading">Products</p>
          <div class="brxdis-as__products">
            <#list autosuggestResult.productSuggestions() as product>
              <a class="brxdis-as__prod" href="/product?pid=${product.id()!""?url('UTF-8')}">
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

    </div>
  <#elseif editMode>
    <div class="brxdis-as__panel">
      <div class="brxdis-as__empty">&#128736; <strong>Discovery Autosuggest</strong> &mdash; type in the search field above to preview suggestions.</div>
    </div>
  </#if>
</div>
