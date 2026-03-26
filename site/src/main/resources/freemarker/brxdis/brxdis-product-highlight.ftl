<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#assign resolvedProductPage><@hst.link path="/product"/></#assign>
<#assign resolvedProductPage = resolvedProductPage?trim>
<@hst.headContribution keyHint="brxdis-prodhighlight-css">
<style>
.brxdis-prodhighlight{font-family:system-ui,-apple-system,sans-serif;margin:1rem 0;position:relative}
.brxdis-prodhighlight__title{font-size:1.125rem;font-weight:700;color:#111827;margin:0 0 .875rem}
.brxdis-prodhighlight__misconfig{border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem}
.brxdis-prodhighlight__grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:1.25rem}
.brxdis-prodhighlight__card{background:#fff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;display:flex;flex-direction:column;transition:box-shadow .2s,transform .2s}
.brxdis-prodhighlight__card:hover{box-shadow:0 8px 24px rgba(0,0,0,.1);transform:translateY(-3px)}
.brxdis-prodhighlight__img{aspect-ratio:4/3;background:#f3f4f6;overflow:hidden}
.brxdis-prodhighlight__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-prodhighlight__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:3rem}
.brxdis-prodhighlight__body{padding:1rem;flex:1;display:flex;flex-direction:column;gap:.375rem}
.brxdis-prodhighlight__brand{font-size:.75rem;font-weight:500;color:#6b7280;text-transform:uppercase;letter-spacing:.05em}
.brxdis-prodhighlight__name{font-size:1rem;font-weight:700;color:#111827;line-height:1.3;margin:0}
.brxdis-prodhighlight__name a{color:inherit;text-decoration:none}
.brxdis-prodhighlight__name a:hover{color:#2563eb}
.brxdis-prodhighlight__desc{font-size:.8125rem;color:#6b7280;line-height:1.45;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-prodhighlight__price{font-size:1.125rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.5rem}
.brxdis-prodhighlight__cta{margin:.25rem 1rem 1rem;padding:.5625rem;background:#2563eb;color:#fff;border:none;border-radius:8px;font-size:.875rem;font-weight:500;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-prodhighlight__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-prodhighlight__empty{padding:2rem 1rem;text-align:center;color:#6b7280;font-size:.875rem;border:1px dashed #e5e7eb;border-radius:8px}
.brxdis-prodhighlight__slot{display:flex;align-items:center;justify-content:center;min-height:220px;border:2px dashed #d1d5db;border-radius:12px;color:#9ca3af;font-size:.875rem;background:#f9fafb}
</style>
</@hst.headContribution>

<section class="brxdis-prodhighlight">
  <h2 class="brxdis-prodhighlight__title">Featured Products</h2>

  <#if brxdis_warning??>
    <div class="brxdis-prodhighlight__misconfig">
      &#9888;&nbsp;<strong>Not configured:</strong> ${brxdis_warning}
    </div>
  </#if>

  <#-- @ftlvariable name="products" type="java.util.List" -->
  <#-- @ftlvariable name="productBeans" type="java.util.List" -->
  <#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
  <#assign inEditMode = editMode?? && editMode>

  <#if inEditMode>
    <#-- Edit mode: render all 4 slots with per-product manageContent -->
    <div class="brxdis-prodhighlight__grid" role="list">
      <#list products as product>
        <#assign slotNum = product?index + 1>
        <#assign bean = (productBeans[product?index])!>
        <#if product??>
          <#assign prodImageUrl = product.imageUrl()!>
          <#assign prodTitle = product.title()!"">
          <#assign prodAttrs = product.attributes()!{}>
          <#assign prodPrice = product.price()>
          <#assign _rawUrl = product.url()!>
          <#assign prodUrl = _rawUrl?has_content?then(_rawUrl, resolvedProductPage + '?pid=' + (product.id()!''))>
          <article class="brxdis-prodhighlight__card" role="listitem">
            <#if bean??>
              <@hst.manageContent hippobean=bean parameterName="document${slotNum}"
                  rootPath="brxdis/products"/>
            <#else>
              <@hst.manageContent parameterName="document${slotNum}"
                  rootPath="brxdis/products"/>
            </#if>
            <div class="brxdis-prodhighlight__img">
              <#if prodImageUrl?has_content>
                <img src="${prodImageUrl}" alt="${prodTitle}"/>
              <#else>
                <div class="brxdis-prodhighlight__placeholder">&#127873;</div>
              </#if>
            </div>
            <div class="brxdis-prodhighlight__body">
              <#if prodAttrs["brand"]??>
                <p class="brxdis-prodhighlight__brand">${prodAttrs["brand"]}</p>
              </#if>
              <h3 class="brxdis-prodhighlight__name">
                <a href="${prodUrl}">${prodTitle?has_content?then(prodTitle, "Untitled")}</a>
              </h3>
              <#if prodAttrs["description"]??>
                <p class="brxdis-prodhighlight__desc">${prodAttrs["description"]}</p>
              </#if>
              <#if prodPrice??>
                <p class="brxdis-prodhighlight__price">${product.currency()!""}&nbsp;${prodPrice?string("0.00")}</p>
              </#if>
            </div>
            <a class="brxdis-prodhighlight__cta" href="${prodUrl}">View Product</a>
          </article>
        <#else>
          <div class="brxdis-prodhighlight__slot">
            <@hst.manageContent parameterName="document${slotNum}"
                rootPath="brxdis/products"/>
            &#43; Product ${slotNum}
          </div>
        </#if>
      </#list>
    </div>
  <#else>
    <#-- Delivery mode: only render non-null products -->
    <#assign anyProduct = false>
    <#list products as p><#if p??><#assign anyProduct = true></#if></#list>
    <#if anyProduct>
      <div class="brxdis-prodhighlight__grid" role="list">
        <#list products as product>
          <#if product??>
            <#assign prodImageUrl = product.imageUrl()!>
            <#assign prodTitle = product.title()!"">
            <#assign prodAttrs = product.attributes()!{}>
            <#assign prodPrice = product.price()>
            <#assign _rawUrl = product.url()!>
            <#assign prodUrl = _rawUrl?has_content?then(_rawUrl, resolvedProductPage + '?pid=' + (product.id()!''))>
            <article class="brxdis-prodhighlight__card" role="listitem">
              <div class="brxdis-prodhighlight__img">
                <#if prodImageUrl?has_content>
                  <img src="${prodImageUrl}" alt="${prodTitle}"/>
                <#else>
                  <div class="brxdis-prodhighlight__placeholder">&#127873;</div>
                </#if>
              </div>
              <div class="brxdis-prodhighlight__body">
                <#if prodAttrs["brand"]??>
                  <p class="brxdis-prodhighlight__brand">${prodAttrs["brand"]}</p>
                </#if>
                <h3 class="brxdis-prodhighlight__name">
                  <a href="${prodUrl}">${prodTitle?has_content?then(prodTitle, "Untitled")}</a>
                </h3>
                <#if prodAttrs["description"]??>
                  <p class="brxdis-prodhighlight__desc">${prodAttrs["description"]}</p>
                </#if>
                <#if prodPrice??>
                  <p class="brxdis-prodhighlight__price">${product.currency()!""}&nbsp;${prodPrice?string("0.00")}</p>
                </#if>
              </div>
              <a class="brxdis-prodhighlight__cta" href="${prodUrl}">View Product</a>
            </article>
          </#if>
        </#list>
      </div>
    <#else>
      <div class="brxdis-prodhighlight__empty">No featured products available.</div>
    </#if>
  </#if>
</section>
