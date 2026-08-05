# Bloomreach Discovery Plugin — Public Documentation

This folder is a draft documentation set for the **brxm-discovery** plugin, written in the structure and tone used by the [Bloomreach XM Documentation](https://xmdocumentation.bloomreach.com/) site's Open Source Plugins section (see the [SEO Support Plugin](https://xmdocumentation.bloomreach.com/library/concepts/plugins/seo-support/about.html) pages for the reference pattern this follows: About → Configuration → Component Parameters → domain reference pages).

It is intended for **public consumers** of the plugin — brXM implementers evaluating or installing it — as distinct from the existing developer walkthrough in [`../user-guides/`](../user-guides/), which stays code-first and is published to this repository's GitHub Pages site.

> **Screenshots:** Every place a real product screenshot belongs is marked with a `[SCREENSHOT PLACEHOLDER]` callout describing exactly what to capture. None have been fabricated — replace each placeholder with an actual annotated screenshot before publishing.

## Contents

| Page | Covers |
|---|---|
| [About](01-about.md) | What the plugin is, what it does, requirements, alpha status |
| [Installation](02-installation.md) | Maven dependencies, repositories, what bootstraps automatically |
| [Configuration](03-configuration.md) | Global config node, credential resolution, per-channel overrides, environments |
| [Component Parameters](04-component-parameters.md) | Every HST component and its Channel Manager parameters |
| [Recommendations & Visual Search](05-recommendations-and-visual-search.md) | Recommendation widgets, the v2 Pathways API, image-based search |
| [CMS Document Types & Pickers](06-document-types-and-pickers.md) | Product/category/recommendation document types, Open UI pickers, editor workflow |
| [Pixel Tracking & Consent](07-pixel-tracking.md) | Analytics events, consent gating, kill switches |
| [Troubleshooting](08-troubleshooting.md) | Common setup and runtime issues |
| [Release Notes](09-release-notes.md) | What changed in each version |

## Source material

Content is derived from the plugin's current source code and its existing developer guides (`../user-guides/*.md`), re-organized and re-written for a product-documentation audience rather than a step-by-step developer tutorial.
