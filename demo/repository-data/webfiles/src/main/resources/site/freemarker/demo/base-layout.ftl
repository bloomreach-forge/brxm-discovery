<!doctype html>
<#include "../include/imports.ftl">
<html lang="en">
  <head>
    <meta charset="utf-8"/>
    <link rel="stylesheet" href="<@hst.webfile  path="/css/bootstrap.css"/>" type="text/css"/>
    <#if hstRequest.requestContext.channelManagerPreviewRequest>
      <link rel="stylesheet" href="<@hst.webfile  path="/css/cms-request.css"/>" type="text/css"/>
    </#if>
    <@hst.headContributions categoryExcludes="htmlBodyEnd, scripts" xhtml=true/>
  </head>
  <body>
    <div class="container">
      <div class="row">
        <div class="col-md-10 col-md-offset-1">
          <@hst.include ref="top"/>
        </div>
      </div>
      <div class="row">
        <div class="col-md-10 col-md-offset-1">
          <@hst.include ref="menu"/>
        </div>
      </div>
      <div class="row">
        <div class="col-md-10 col-md-offset-1">
          <@hst.include ref="main"/>
        </div>
      </div>
      <div class="row">
        <div class="col-md-10 col-md-offset-1">
          <@hst.include ref="footer"/>
        </div>
      </div>
    </div>
    <@hst.headContributions categoryIncludes="htmlBodyEnd, scripts" xhtml=true/>
  </body>
</html>
