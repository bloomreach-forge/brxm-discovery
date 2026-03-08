<#include "../include/imports.ftl">
<@hst.defineObjects/>
<@hst.headContribution keyHint="brxdis-two-col-css">
<style>
.brxdis-two-col{display:grid;grid-template-columns:240px 1fr;gap:1rem 1.5rem;align-items:start}
.brxdis-two-col__content{min-width:0}
@media(max-width:768px){.brxdis-two-col{display:block}}
</style>
</@hst.headContribution>
<@hst.include ref="search"/>
<div class="brxdis-two-col">
  <div class="brxdis-two-col__sidebar"><@hst.include ref="sidebar"/></div>
  <div class="brxdis-two-col__content"><@hst.include ref="content"/></div>
</div>
