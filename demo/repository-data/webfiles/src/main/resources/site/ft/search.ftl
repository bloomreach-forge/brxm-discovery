<form method="get" action="">
  <input type="search" name="q" value="${query!""}" placeholder="Search products…" autofocus/>
  <button type="submit">Search</button>
</form>

<h1>Search: ${query!""}</h1>
<#if searchResult??>
  <p>${searchResult.total()} results (page ${searchResult.page() + 1})</p>
<#elseif query?has_content>
  <p>No results.</p>
</#if>
