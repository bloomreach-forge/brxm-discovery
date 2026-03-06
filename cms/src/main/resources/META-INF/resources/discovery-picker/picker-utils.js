function buildApiUrl(ui, path, params) {
  var base = (ui && ui.baseUrl ? ui.baseUrl.replace(/\/$/, "") : window.location.origin);
  var url  = base + "/ws" + path + "?";
  url += Object.keys(params)
    .filter(function (k) { return params[k] !== undefined && params[k] !== ""; })
    .map(function (k) { return encodeURIComponent(k) + "=" + encodeURIComponent(params[k]); })
    .join("&");
  return url;
}

function setStatusEl(el, type, msg, loading) {
  el.replaceChildren();
  if (loading) {
    var s = document.createElement("span");
    s.className = "spinner";
    el.appendChild(s);
  }
  el.appendChild(document.createTextNode(msg));
  el.className = type === "error" ? "error" : "";
}

function esc(s) {
  return String(s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;")
    .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function escAttr(s) {
  return String(s).replace(/"/g, "&quot;");
}
