/**
 * Stretch the dialog iframe to fill its hosting container.
 *
 * Two container environments are supported:
 *
 *  Angular Material (Experience Manager / cdk-overlay-*)
 *    We prefer mat-mdc-dialog-content — the content area that excludes the
 *    title bar brXM renders outside the iframe.  Using the full container
 *    (mat-mdc-dialog-container) makes the iframe ~50 px taller than the
 *    visible area, which produces double scrollbars.
 *
 *  Wicket (Content section / wicket-modal)
 *    We detect the Wicket content-area divs (w_content_1, w_content) and
 *    read their clientHeight directly.  The old height:100% walk-up does
 *    not work because Wicket uses absolutely-positioned containers whose
 *    percentage height does not propagate correctly.
 *
 * No-op when cross-origin access is blocked.
 */
function stretchDialogIframe() {
  try {
    var iframes = window.parent.document.querySelectorAll("iframe");
    for (var i = 0; i < iframes.length; i++) {
      if (iframes[i].contentWindow !== window) continue;

      var f = iframes[i];
      f.style.width   = "100%";
      f.style.display = "block";
      f.style.border  = "0";

      // Walk up the parent chain to find the best host to measure height from.
      //
      // The key insight: mat-dialog-content auto-sizes to its content (the
      // iframe at 150 px default) — measuring it produces a circular result.
      // We must keep walking up to the element that brXM explicitly sizes.
      //
      // Priority (highest → lowest):
      //   1. openui-dialog-content  (brie-dialog-content — brXM's properly-sized
      //      content wrapper; the title bar is a sibling, not inside this element)
      //   2. mat-mdc-dialog-container / cdk-dialog-container / mat-dialog-container
      //      (Angular Material full container — fallback for non-brXM embeddings)
      //   3. w_content_1 / w_content  (Wicket modal content area)
      //
      // mat-mdc-dialog-content / mat-dialog-content is recorded as innerScrollEl
      // so we can back-propagate height:100% from the host down to it.
      var host          = null;  // element whose clientHeight drives the iframe
      var innerScrollEl = null;  // mat-dialog-content — stretched to fill host
      var el = f.parentElement;
      var depth = 0;

      while (el && depth < 15) {
        var cls = (typeof el.className === "string") ? el.className : "";

        // Record mat-dialog-content but do NOT stop — it auto-sizes to content.
        if (!innerScrollEl &&
            (cls.indexOf("mat-mdc-dialog-content") >= 0 ||
             cls.indexOf("mat-dialog-content")     >= 0)) {
          innerScrollEl = el;
          el = el.parentElement; depth++; continue;
        }

        // brXM Experience Manager: the element that actually has the explicit height.
        if (cls.indexOf("openui-dialog-content") >= 0) {
          host = el;
          break;
        }

        // Angular Material full container (fallback — includes title bar height).
        if (cls.indexOf("mat-mdc-dialog-container") >= 0 ||
            cls.indexOf("cdk-dialog-container")     >= 0 ||
            cls.indexOf("mat-dialog-container")     >= 0) {
          host = el;
          break;
        }

        // Wicket modal content divs (w_content_1 is nested inside w_content).
        if (cls.indexOf("w_content_1") >= 0 ||
            cls.indexOf("w_content")   >= 0) {
          host = el;
          break;
        }

        el = el.parentElement;
        depth++;
      }

      // Depth exhausted without finding a known container — use innerScrollEl as
      // last resort (better than nothing, even if height may be under-counted).
      if (!host) host = innerScrollEl;

      function applyHeight() {
        if (!host) {
          f.style.height = Math.max(Math.floor(window.parent.innerHeight * 0.72), 420) + "px";
          return;
        }

        // Prevent the host itself from growing its own scrollbar.
        host.style.overflow = "hidden";

        var h = host.clientHeight;

        // Stretch innerScrollEl (mat-dialog-content) to fill the host.
        // height:100% does NOT work when the parent fills its space via flexbox
        // rather than an explicit CSS height — so we use the same pixel value.
        // max-height:none removes Angular Material's built-in 65 vh cap.
        if (innerScrollEl && innerScrollEl !== host) {
          innerScrollEl.style.height    = h > 50 ? h + "px" : "100%";
          innerScrollEl.style.maxHeight = "none";
          innerScrollEl.style.padding   = "0";
          innerScrollEl.style.overflow  = "hidden";
        }

        if (h > 50) {
          f.style.height = h + "px";
        } else {
          // Fallback: comfortable fraction of the parent viewport.
          f.style.height = Math.max(Math.floor(window.parent.innerHeight * 0.72), 420) + "px";
        }
      }

      // Apply immediately, then retry to catch the Angular Material open animation.
      applyHeight();
      window.parent.requestAnimationFrame(applyHeight);
      window.parent.setTimeout(applyHeight, 150);
      window.parent.setTimeout(applyHeight, 350);

      // Stay reactive to dialog resize (e.g. window resize).
      if (window.parent.ResizeObserver) {
        new window.parent.ResizeObserver(applyHeight).observe(host || f);
      }

      break;
    }
  } catch (e) { /* cross-origin or sandboxed — silently ignored */ }
}

/**
 * Sync the document body height to the iframe's actual viewport height.
 * Once stretchDialogIframe() sets an explicit pixel height on the iframe,
 * window.innerHeight updates to match.  We apply that value to html+body so
 * the flex layout fills the full viewport rather than collapsing to content.
 */
function syncBodyToViewport() {
  var lastH = 0;
  function apply() {
    var h = window.innerHeight;
    if (h > 100 && h !== lastH) {
      lastH = h;
      document.documentElement.style.height = h + "px";
      document.body.style.height = h + "px";
    }
  }
  requestAnimationFrame(apply);
  setTimeout(apply, 150);
  setTimeout(apply, 350);
  setTimeout(apply, 600);
  if (typeof ResizeObserver !== "undefined") {
    new ResizeObserver(apply).observe(document.documentElement);
  }
}

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
