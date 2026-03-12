function initPickerField(cfg) {
  "use strict";

  var ui           = null;
  var documentId   = "";
  var currentValue = "";

  var valueDisplay = document.getElementById("value-display");
  var clearBtn     = document.getElementById("clear-btn");
  var browseBtn    = document.getElementById("browse-btn");

  UiExtension.register().then(function (registeredUi) {
    ui = registeredUi;

    ui.document.field.getValue().then(function (val) {
      currentValue = val || "";
      refreshDisplay(currentValue);
    });

    ui.document.get().then(function (doc) {
      documentId = doc.id || "";
      setEditMode(doc.mode === "edit");
    });

    ui.document.observe(function (doc) {
      documentId = doc.id || "";
      setEditMode(doc.mode === "edit");
    });

    ui.document.field.setHeight(52);
  }).catch(function () {
    valueDisplay.textContent = "Failed to connect to CMS";
    valueDisplay.classList.add("empty");
  });

  browseBtn.addEventListener("click", function () {
    if (!ui) return;
    var dialogUrl = (ui.baseUrl || "").replace(/\/$/, "") +
      "/discovery-picker/" + cfg.dialogPath;

    ui.dialog.open({
      title: cfg.dialogTitle,
      url:   dialogUrl,
      size:  "large",
      value: JSON.stringify({ documentId: documentId, currentValue: currentValue })
    })
    .then(function (selectedId) {
      if (selectedId) {
        currentValue = String(selectedId);
        ui.document.field.setValue(currentValue);
        refreshDisplay(currentValue);
      }
    })
    .catch(function (err) {
      if (err && err.code !== "DialogCanceled") {
        console.error("brxm-discovery " + cfg.errorLabel + " dialog error:", err);
      }
    });
  });

  clearBtn.addEventListener("click", function () {
    currentValue = "";
    if (ui) ui.document.field.setValue("");
    refreshDisplay("");
  });

  function setEditMode(editable) {
    browseBtn.disabled = !editable;
    clearBtn.disabled  = !editable;
  }

  function refreshDisplay(value) {
    if (value) {
      valueDisplay.textContent = value;
      valueDisplay.classList.remove("empty");
      clearBtn.style.display = "";
    } else {
      valueDisplay.textContent = cfg.placeholder;
      valueDisplay.classList.add("empty");
      clearBtn.style.display = "none";
    }
  }
}
