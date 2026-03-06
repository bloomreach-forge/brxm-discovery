<h1>Category: ${categoryId!""}</h1>
<#if categoryResult??>
  <p>${categoryResult.total()} products (page ${categoryResult.page() + 1})</p>

  <#if categoryResult.facets()?has_content>
    <aside>
      <#list categoryResult.facets()?values as facet>
        <h3>${facet.name()}</h3>
        <ul>
          <#list facet.values() as fv>
            <li>${fv.value()} (${fv.count()})</li>
          </#list>
        </ul>
      </#list>
    </aside>
  </#if>

  <ul>
    <#list categoryResult.products() as product>
      <li>
        <a href="${product.url()!""}">${product.title()!""}</a>
        <#if product.imageUrl()?has_content><br/><img src="${product.imageUrl()}" alt="${product.title()!""}"/></#if>
      </li>
    </#list>
  </ul>
<#else>
  <p>No products.</p>
</#if>
