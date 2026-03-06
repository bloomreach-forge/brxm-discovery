<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"] >
<#if document??>
  <@hst.manageContent hippobean=document parameterName="document" rootPath="test" defaultPath="test"/>
<#else>
  <@hst.manageContent parameterName="document" rootPath="test" defaultPath="test"/>
</#if>

<h1>Recommendations</h1>

<#if brxdis_warning??>
  <div style="border:2px solid #e07b00;background:#fff8e1;padding:10px 14px;
              border-radius:4px;font-size:12px;color:#5a3e00;margin-bottom:8px">
    <strong>&#9888; Recommendations not configured:</strong> ${brxdis_warning}
  </div>
</#if>

<#if widgetId?has_content>
  <p><small>Widget: ${widgetId}</small></p>
</#if>

<#if products?? && products?has_content>
  <ul>
    <#list products as product>
      <li>
        <a href="${product.url()!""}">${product.title()!""}</a>
        <#if product.imageUrl()?has_content><br/><img src="${product.imageUrl()}" alt="${product.title()!""}"/></#if>
      </li>
    </#list>
  </ul>
<#else>
  <p>No recommendations. <#if !widgetId?has_content>Select a widget document or pass <code>?widgetId=&lt;id&gt;</code>.</#if></p>
</#if>
