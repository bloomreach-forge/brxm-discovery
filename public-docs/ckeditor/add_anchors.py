#!/usr/bin/env python3
"""Rewrite pandoc-generated heading ids as CKEditor-safe <a id name> anchors.

CKEditor strips id attributes from block elements (h1-h4) on paste, which
breaks same-page "ON THIS PAGE" TOC links and cross-page fragment links.
The Anchor plugin does preserve <a id="..." name="..."></a> elements, so
this script moves each heading's id onto a leading anchor tag instead.

Anchor values are derived from the heading text (GitHub-slug style) rather
than trusted verbatim from pandoc's output, so a mismatch between a
heading's rendered text and its id is caught instead of silently copied.
"""
import html
import re
import sys
from pathlib import Path

HEADING_RE = re.compile(r'<(h[1-4]) id="([^"]*)">(.*?)</\1>')
HREF_RE = re.compile(r'href="#([^"]*)"')


def derive_anchor(inner_html: str) -> str:
    text = re.sub(r'<[^>]+>', '', inner_html)
    text = html.unescape(text)
    text = text.lower()
    text = re.sub(r'[^a-z0-9\s-]', '', text)
    return text.replace(' ', '-')


def process(path: Path) -> None:
    original = path.read_text(encoding='utf-8')
    id_map = {}

    def replace_heading(match: re.Match) -> str:
        tag, old_id, inner = match.group(1), match.group(2), match.group(3)
        new_id = derive_anchor(inner)
        if new_id != old_id:
            print(f'{path.name}: id mismatch for heading {inner!r}: '
                  f'pandoc={old_id!r} derived={new_id!r}', file=sys.stderr)
        id_map[old_id] = new_id
        anchor = f'<a id="{new_id}" name="{new_id}"></a>'
        return f'<{tag}>{anchor}{inner}</{tag}>'

    updated = HEADING_RE.sub(replace_heading, original)

    def replace_href(match: re.Match) -> str:
        old_id = match.group(1)
        new_id = id_map.get(old_id, old_id)
        return f'href="#{new_id}"'

    updated = HREF_RE.sub(replace_href, updated)

    if updated != original:
        path.write_text(updated, encoding='utf-8')
        print(f'{path.name}: updated')
    else:
        print(f'{path.name}: no change')


def main() -> None:
    ckeditor_dir = Path(__file__).parent
    for html_path in sorted(ckeditor_dir.glob('*.html')):
        process(html_path)


if __name__ == '__main__':
    main()
