 ---
Complete Wiring: Open UI Picker → HST Component

There are 4 pieces to connect together.

  ---
1. Register the UI Extension

hcm-config/ui-extensions/my-picker-extension.yaml

definitions:
config:
/hippo:configuration/hippo:frontend/cms/ui-extensions:
/myPicker:
jcr:primaryType: frontend:uiExtension
frontend:displayName: My API Picker
frontend:extensionPoint: document.field
frontend:url: /site/my-picker/field
frontend:initialHeightInPixels: 50
frontend:config: '{"apiEndpoint": "/cms/ws/my-api/items", "dialogTitle": "Select Item"}'

  ---
2. Add the Field to Your Document Type Namespace

This is two parts in the same namespace YAML — the node type definition and the editor template.

hcm-config/namespaces/myns/myDocType.yaml

definitions:
config:
/hippo:namespaces/myns/myDocType:
/hipposysedit:nodetype:
/hipposysedit:nodetype:

            # --- NODE TYPE DEFINITION ---
            /myPickerField:
              jcr:primaryType: hipposysedit:field
              hipposysedit:path: myns:myPickerField   # JCR property name
              hipposysedit:type: OpenUiString          # ← tells CMS to use UI Extension
              hipposysedit:mandatory: false
              hipposysedit:multiple: false
              hipposysedit:ordered: false
              hipposysedit:primary: false

        /hipposysedit:prototypes:
          /hipposysedit:prototype:
            jcr:primaryType: myns:myDocType
            myns:myPickerField: ''   # ← empty default value in prototype

        /editor:templates:
          /_default_:

            # --- EDITOR PLUGIN ---
            /myPickerField:
              jcr:primaryType: frontend:plugin
              caption: My Picker
              field: myPickerField
              plugin.class: org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin
              wicket.id: ${cluster.id}.field
              /cluster.options:
                jcr:primaryType: frontend:pluginconfig
                ui.extension: myPicker    # ← name must match the key in ui-extensions YAML

The ui.extension property in cluster.options is what OpenUiStringPlugin reads (line 88 of the source: config.getString("ui.extension")) to look up which registered extension to load.

  ---
3. Read the Value in a HippoBean

@Node(jcrType = "myns:myDocType")
public class MyDocBean extends HippoDocument {

      public String getMyPickerField() {
          return getSingleProperty("myns:myPickerField");
      }
}

  ---
4. Use it in the HST Component

public class MyComponent extends BaseHstComponent {

      @Override
      public void doBeforeRender(HstRequest request, HstResponse response) {
          MyDocBean doc = (MyDocBean) request.getRequestContext().getContentBean();
          String selectedId = doc.getMyPickerField();  // the 'id' returned by dialog.close()
          request.setAttribute("selectedId", selectedId);
      }
}

And in Freemarker:
<#assign selectedId = document.myPickerField />

  ---
How the Value Flows

UI Extension dialog.close("some-id")
→ field.setValue("some-id")
→ AutoSaveBehavior (OpenUiStringPlugin) persists it
→ JCR: /content/documents/.../variant/myns:myPickerField = "some-id"
→ HippoBean.getSingleProperty("myns:myPickerField") → "some-id"
→ HST Component → Template

  ---
The Key Insight

The link between the field and the extension is solely the ui.extension config property in cluster.options. The field type OpenUiString tells the CMS it's an Open UI field, and ui.extension: myPicker tells it which
registered extension to load. Get those two names matching and the rest is standard document type wiring.

 ---
How Open UI Extension Pickers Work

The standard JCR picker is just a built-in field type that browses the JCR tree. When you want to call an external API instead, you replace the field with a UI Extension (extensionPoint: document.field).

The pattern splits into two iframes:

CMS Document Editor
└─ iframe 1: Field Extension   ← renders current value + "Open" button
└─ iframe 2: Dialog        ← fetches API, renders list, returns id

  ---
1. HCM Config (Registration)

# hcm-config/ui-extensions/my-picker.yaml
definitions:
config:
/hippo:configuration/hippo:frontend/cms/ui-extensions:
/myPicker:
jcr:primaryType: frontend:uiExtension
frontend:displayName: My API Picker
frontend:extensionPoint: document.field
frontend:url: /site/my-picker/field   # served by your HST site
frontend:initialHeightInPixels: 50
frontend:config: '{"apiEndpoint": "/cms/ws/my-api/items", "dialogTitle": "Select Item"}'

Then on your document type field, you point the field at this extension by name.

  ---
2. Field Extension (iframe 1)

This is the iframe that sits inline in the document editor. Its only job is to show the current value and open the dialog.

// field.js
document.addEventListener('DOMContentLoaded', async () => {
const ui = await UiExtension.register();

    // Read config injected from HCM YAML
    const { apiEndpoint, dialogTitle } = JSON.parse(ui.extension.config);

    // Read current saved value
    const currentValue = await ui.document.field.getValue();
    document.getElementById('display').textContent = currentValue || '(none)';

    // Fit height to content
    await ui.document.field.setHeight('auto');

    document.getElementById('openBtn').addEventListener('click', async () => {
      try {
        // Opens iframe 2 — blocks until dialog.close() or dialog.cancel()
        const selectedId = await ui.dialog.open({
          title: dialogTitle,
          url: '/site/my-picker/dialog',   // dialog iframe URL
          size: 'large',
          value: currentValue,             // passes current value for pre-selection
        });

        // Update field with returned id
        document.getElementById('display').textContent = selectedId;
        await ui.document.field.setValue(selectedId);

      } catch (err) {
        if (err.code !== 'DialogCanceled') {
          console.error(err);
        }
      }
    });
});

  ---
3. Dialog (iframe 2)

This iframe fetches your API, renders the list, and closes with the selected id.

// dialog.js
document.addEventListener('DOMContentLoaded', async () => {
const ui = await UiExtension.register();

    // Get what the parent passed (DialogProperties.value = pre-selected id)
    const options = await ui.dialog.options();
    const { apiEndpoint } = JSON.parse(ui.extension.config);

    // Fetch list from your API
    const items = await fetch(apiEndpoint, { credentials: 'include' })
      .then(r => r.json());

    const list = document.getElementById('list');
    items.forEach(item => {
      const el = document.createElement('li');
      el.textContent = item.name;
      el.dataset.id = item.id;
      if (item.id === options.value) el.classList.add('selected');
      el.addEventListener('click', async () => {
        await ui.dialog.close(item.id);  // ← this resolves the open() promise in iframe 1
      });
      list.appendChild(el);
    });

    document.getElementById('cancelBtn').addEventListener('click', () => ui.dialog.cancel());
});

  ---
4. Key API Methods

┌─────────────────────────────────────┬───────────────┬───────────────────────────────────────────────────────────────────────────────────────────┐
│               Method                │     Where     │                                       What it does                                        │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ UiExtension.register()              │ Both iframes  │ Establishes postMessage bridge with CMS parent                                            │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.extension.config                 │ Field iframe  │ JSON string from frontend:config in YAML                                                  │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.document.field.getValue()        │ Field iframe  │ Gets the currently stored field value                                                     │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.document.field.setValue(id)      │ Field iframe  │ Persists the selected id back to the document                                             │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.document.field.setHeight('auto') │ Field iframe  │ Resizes the inline iframe to fit content                                                  │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.dialog.open({url, title, value}) │ Field iframe  │ Opens the dialog iframe, returns a Promise that resolves with the value passed to close() │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.dialog.options()                 │ Dialog iframe │ Gets the DialogProperties passed from open()                                              │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.dialog.close(id)                 │ Dialog iframe │ Resolves the open() promise in the field iframe with this value                           │
├─────────────────────────────────────┼───────────────┼───────────────────────────────────────────────────────────────────────────────────────────┤
│ ui.dialog.cancel()                  │ Dialog iframe │ Rejects the open() promise with { code: 'DialogCanceled' }                                │
└─────────────────────────────────────┴───────────────┴───────────────────────────────────────────────────────────────────────────────────────────┘

  ---
5. Constraints to Know

- No nesting — you cannot call ui.dialog.open() from inside a dialog. One level deep only.
- Both iframes call UiExtension.register() — same SDK, works in both contexts.
- ui.dialog.options() is dialog-only — calling it in a field extension is a no-op.
- getValue/setValue work in both — the dialog can also read/write the field directly if needed.
- frontend:config is static — it's set at registration time in YAML. To pass runtime values (e.g. which account), use ui.dialog.open({ value: ... }).
