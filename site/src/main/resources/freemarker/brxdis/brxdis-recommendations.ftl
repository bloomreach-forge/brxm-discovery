<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"]>
<@hst.defineObjects/>
<#assign resolvedProductPage><@hst.link path="/product"/></#assign>
<#assign resolvedProductPage = resolvedProductPage?trim>
<@hst.headContribution keyHint="brxdis-recs-css">
<style>
.brxdis-recs{font-family:system-ui,-apple-system,sans-serif;margin:1rem 0;position:relative}
.brxdis-recs__header{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:.875rem}
.brxdis-recs__title{font-size:1.125rem;font-weight:700;color:#111827;margin:0}
.brxdis-recs__widget{font-size:.75rem;color:#9ca3af}
.brxdis-recs__misconfig{border:2px solid #f59e0b;background:#fffbeb;padding:.625rem .875rem;border-radius:7px;font-size:.8125rem;color:#78350f;margin-bottom:.75rem}
.brxdis-recs__track{display:flex;gap:1rem;overflow-x:auto;padding-bottom:.75rem;scroll-snap-type:x mandatory;-ms-overflow-style:none;scrollbar-width:none}
.brxdis-recs__track::-webkit-scrollbar{display:none}
.brxdis-recs__card{flex:0 0 200px;background:#fff;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;display:flex;flex-direction:column;scroll-snap-align:start;transition:box-shadow .2s,transform .2s}
.brxdis-recs__card:hover{box-shadow:0 6px 20px rgba(0,0,0,.1);transform:translateY(-2px)}
.brxdis-recs__img{aspect-ratio:1;background:#f3f4f6;overflow:hidden}
.brxdis-recs__img img{width:100%;height:100%;object-fit:cover;display:block}
.brxdis-recs__placeholder{display:flex;align-items:center;justify-content:center;height:100%;color:#d1d5db;font-size:2.25rem}
.brxdis-recs__body{padding:.75rem;flex:1;display:flex;flex-direction:column;gap:.25rem}
.brxdis-recs__name{font-size:.875rem;font-weight:600;color:#111827;line-height:1.35;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.brxdis-recs__name a{color:inherit;text-decoration:none}
.brxdis-recs__name a:hover{color:#2563eb}
.brxdis-recs__price{font-size:.9375rem;font-weight:700;color:#111827;margin-top:auto;padding-top:.375rem}
.brxdis-recs__cta{margin:.25rem .75rem .75rem;padding:.4375rem;background:#2563eb;color:#fff;border:none;border-radius:6px;font-size:.8125rem;font-weight:500;text-align:center;text-decoration:none;display:block;transition:background .15s}
.brxdis-recs__cta:hover{background:#1d4ed8;color:#fff}
.brxdis-recs__empty{padding:2rem 1rem;text-align:center;color:#6b7280;font-size:.875rem;border:1px dashed #e5e7eb;border-radius:8px}
.brxdis-recs__viewport{position:relative;overflow:hidden}
.brxdis-recs__viewport::before,.brxdis-recs__viewport::after{content:'';position:absolute;top:0;bottom:0;width:3.5rem;z-index:1;pointer-events:none;opacity:0;transition:opacity .2s}
.brxdis-recs__viewport::before{left:0;background:linear-gradient(to right,#fff 20%,transparent)}
.brxdis-recs__viewport::after{right:0;background:linear-gradient(to left,#fff 20%,transparent)}
.brxdis-recs__viewport.has-overflow-left::before{opacity:1}
.brxdis-recs__viewport.has-overflow-right::after{opacity:1}
.brxdis-recs__btn{position:absolute;top:50%;transform:translateY(-50%);z-index:2;background:#fff;border:1px solid #e5e7eb;border-radius:50%;width:2rem;height:2rem;display:flex;align-items:center;justify-content:center;font-size:1.25rem;line-height:1;cursor:pointer;box-shadow:0 2px 6px rgba(0,0,0,.12);transition:opacity .2s,box-shadow .15s;opacity:0;pointer-events:none}
.brxdis-recs__btn:hover{box-shadow:0 4px 12px rgba(0,0,0,.2)}
.brxdis-recs__btn--prev{left:.3rem}
.brxdis-recs__btn--next{right:.3rem}
.brxdis-recs__btn.is-visible{opacity:1;pointer-events:auto}
.brxdis-band-badge{font-size:.75rem;color:#374151;border-radius:6px;padding:.3rem .6rem;margin-bottom:.5rem;display:inline-block}
.brxdis-band-badge--green{background:#f0fdf4;border:1px solid #86efac}
.brxdis-band-badge--blue{background:#eff6ff;border:1px solid #93c5fd}
</style>
</@hst.headContribution>

<#-- @ftlvariable name="editMode" type="java.lang.Boolean" -->
<#-- @ftlvariable name="dataSource" type="java.lang.String" -->
<#-- @ftlvariable name="label" type="java.lang.String" -->
<#if (editMode!false)>
  <#if (dataSource!"standalone") == "productDetailBand">
    <div class="brxdis-band-badge brxdis-band-badge--green">&#128204; Recommendations &mdash; reads PID from product detail label: <strong>${label!"default"}</strong></div>
  <#else>
    <div class="brxdis-band-badge brxdis-band-badge--blue">&#128204; Recommendations &mdash; standalone PID resolution</div>
  </#if>
</#if>

<section class="brxdis-recs" data-brxdis-carousel>
  <#if document??>
    <@hst.manageContent hippobean=document parameterName="document" rootPath="brxdis/widgets"/>
  <#else>
    <@hst.manageContent documentTemplateQuery="new-brxdis-recommendationDocument"
        parameterName="document" rootPath="brxdis/widgets" defaultPath="widgets"/>
  </#if>
  <div class="brxdis-recs__header">
    <h2 class="brxdis-recs__title">You May Also Like</h2>
    <#if (editMode!false) && widgetId?has_content>
      <span class="brxdis-recs__widget">Widget:&nbsp;${widgetId}</span>
    </#if>
  </div>

  <#if brxdis_warning??>
    <div class="brxdis-recs__misconfig">
      &#9888;&nbsp;<strong>Not configured:</strong> ${brxdis_warning}
    </div>
  </#if>

  <#if products?? && products?has_content>
    <div class="brxdis-recs__viewport">
      <button class="brxdis-recs__btn brxdis-recs__btn--prev" aria-label="Scroll left">&#8249;</button>
      <div class="brxdis-recs__track" role="list">
        <#list products as product>
          <#assign prodImageUrl = product.imageUrl()!>
          <#assign prodTitle = product.title()!"">
          <#assign prodUrl = product.url()!"">
          <#assign prodAttrs = product.attributes()!{}>
          <#assign recommendationHref = resolvedProductPage + "?pid=" + ((product.id()!"")?url('UTF-8'))>
          <#assign recommendationHref = recommendationHref + "&brxdis_event=widget-click">
          <#if widgetId?has_content>
            <#assign recommendationHref = recommendationHref + "&brxdis_wid=" + widgetId?url('UTF-8')>
          </#if>
          <#if widgetType?has_content>
            <#assign recommendationHref = recommendationHref + "&brxdis_wty=" + widgetType?url('UTF-8')>
          </#if>
          <#if widgetResultId?has_content>
            <#assign recommendationHref = recommendationHref + "&brxdis_wrid=" + widgetResultId?url('UTF-8')>
          </#if>
          <#if widgetQuery?has_content>
            <#assign recommendationHref = recommendationHref + "&brxdis_wq=" + widgetQuery?url('UTF-8')>
          </#if>
          <article class="brxdis-recs__card" role="listitem">
            <div class="brxdis-recs__img">
              <#if prodImageUrl?has_content>
                <img src="${prodImageUrl}" alt="${prodTitle}"/>
              <#else>
                <div class="brxdis-recs__placeholder">&#128722;</div>
              </#if>
            </div>
            <div class="brxdis-recs__body">
              <h3 class="brxdis-recs__name">
                <a href="${recommendationHref}">${prodTitle?has_content?then(prodTitle, "Untitled")}</a>
              </h3>
              <#if (showDescription!false) && prodAttrs["description"]??>
                <p class="brxdis-recs__desc" style="font-size:.8rem;color:#6b7280;margin:0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">${prodAttrs["description"]}</p>
              </#if>
              <#if (showPrice!true) && product.price()??>
                <p class="brxdis-recs__price">${product.currency()!""}&nbsp;${product.price()?string("0.00")}</p>
              </#if>
            </div>
            <a class="brxdis-recs__cta" href="${recommendationHref}">View Product</a>
          </article>
        </#list>
      </div>
      <button class="brxdis-recs__btn brxdis-recs__btn--next" aria-label="Scroll right">&#8250;</button>
    </div>
  <#elseif (editMode!false)>
    <div class="brxdis-recs__empty">&#128736; Select a <strong>Recommendation Document</strong> in component properties to show recommendations.</div>
  <#else>
    <div class="brxdis-recs__empty">No recommendations available.</div>
  </#if>
</section>
<script>
(function () {
  var CARD_STEP = 216; /* 200px card + 16px gap */
  document.querySelectorAll('[data-brxdis-carousel]').forEach(function (section) {
    var track = section.querySelector('.brxdis-recs__track');
    var vp    = section.querySelector('.brxdis-recs__viewport');
    var prev  = section.querySelector('.brxdis-recs__btn--prev');
    var next  = section.querySelector('.brxdis-recs__btn--next');
    if (!track || !vp) return;

    function update() {
      var atStart = track.scrollLeft <= 0;
      var atEnd   = track.scrollLeft + track.clientWidth >= track.scrollWidth - 1;
      vp.classList.toggle('has-overflow-left',  !atStart);
      vp.classList.toggle('has-overflow-right', !atEnd);
      if (prev) prev.classList.toggle('is-visible', !atStart);
      if (next) next.classList.toggle('is-visible', !atEnd);
    }

    if (prev) prev.addEventListener('click', function () {
      track.scrollBy({ left: -CARD_STEP, behavior: 'smooth' });
    });
    if (next) next.addEventListener('click', function () {
      track.scrollBy({ left:  CARD_STEP, behavior: 'smooth' });
    });
    track.addEventListener('scroll', update, { passive: true });
    update();
  });
}());
</script>
