<#assign hst=JspTaglibs["http://www.hippoecm.org/jsp/hst/core"] >
<!DOCTYPE html>
<html>
<head>
  <title>Discovery Demo</title>
  <@hst.headContributions/>
</head>
<body>
<nav>
  <a href="<@hst.link path="/"/>">Home</a>
  | <a href="<@hst.link path="/search"/>">Search</a>
  | <a href="<@hst.link path="/category"/>">Category</a>
  | <a href="<@hst.link path="/recommendations"/>">Recommendations</a>
  | <a href="<@hst.link path="/autosuggest"/>">Autosuggest</a>
</nav>
<main>
  <@hst.include ref="main"/>
</main>
</body>
</html>
