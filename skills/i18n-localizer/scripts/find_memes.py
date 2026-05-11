#!/usr/bin/env python3
"""
find_memes.py - Search OpenMeme memes by query term.

Searches recursively through memes/ directory for MDX files matching
title, description, category, tags, or slug. Excludes already-localized
files when a target locale is specified.

Usage:
    python find_memes.py <query> [--locale <locale>] [--repo <path>]
    python find_memes.py simpsons --locale es-AR --repo ~/openmeme
"""

import os
import re
import json
import argparse
from pathlib import Path


def parse_frontmatter(content: str) -> dict:
    """Extract YAML frontmatter from MDX content."""
    match = re.search(r"---\n(.*?)\n---", content, re.DOTALL)
    if not match:
        return {}

    fm = match.group(1)
    data = {}
    for line in fm.split("\n"):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        m = re.match(r'^([a-z_]+):\s*(.*)$', line)
        if m:
            key, value = m.group(1), m.group(2).strip()
            if value.startswith("[") and value.endswith("]"):
                items = value[1:-1].split(",")
                data[key] = [s.strip().strip('"').strip("'") for s in items if s.strip()]
            elif value.startswith('"') and value.endswith('"'):
                data[key] = value[1:-1]
            elif value.startswith("'") and value.endswith("'"):
                data[key] = value[1:-1]
            else:
                try:
                    data[key] = int(value)
                except ValueError:
                    data[key] = value
    return data


def search_memes(repo_path: str, query: str, exclude_locale: str = None) -> list:
    """
    Search memes directory for matches.

    Args:
        repo_path: Path to OpenMeme repository
        query: Search term
        exclude_locale: If set, exclude .{locale}.mdx files from results

    Returns:
        List of dicts with meme metadata and file path
    """
    memes_dir = Path(repo_path) / "memes"
    if not memes_dir.exists():
        return []

    query_lower = query.lower()
    results = []
    seen_slugs = set()

    for mdx_file in memes_dir.rglob("*.mdx"):
        # Skip files that already have locale suffix (e.g., .es-AR.mdx)
        name = mdx_file.name
        if exclude_locale:
            if f".{exclude_locale}." in name:
                continue
            # Also skip if base .mdx has a matching .{locale}.mdx variant
            stem = mdx_file.stem  # e.g., "homer" from "homer.mdx"
            locale_variant = mdx_file.with_name(f"{stem}.{exclude_locale}.mdx")
            if locale_variant.exists():
                continue

        try:
            content = mdx_file.read_text(encoding="utf-8")
            data = parse_frontmatter(content)
        except Exception:
            continue

        slug = str(data.get("slug", mdx_file.stem))
        title = str(data.get("title", "")).lower()
        desc = str(data.get("description", "")).lower()
        category = str(data.get("category", "")).lower()
        tags = [t.lower() for t in data.get("tags", [])]

        # Determine if this is a base file or localized file
        base_slug = slug
        for loc in ["es-AR", "pt-BR", "en-US", "en-GB", "fr-FR", "de-DE"]:
            if name.endswith(f".{loc}.mdx"):
                base_slug = slug  # Keep original slug
                break

        # Check for match
        match = False
        if query_lower in title:
            match = True
        elif query_lower in desc:
            match = True
        elif query_lower in category:
            match = True
        elif query_lower in tags:
            match = True
        elif query_lower in slug.lower():
            match = True
        elif query_lower in str(mdx_file).lower():
            match = True

        if match and slug not in seen_slugs:
            seen_slugs.add(slug)
            # Check if locale variant already exists
            has_locale = False
            if exclude_locale:
                parent = mdx_file.parent
                locale_file = parent / f"{slug}.{exclude_locale}.mdx"
                has_locale = locale_file.exists()

            results.append({
                "path": str(mdx_file),
                "slug": slug,
                "title": data.get("title", ""),
                "description": data.get("description", ""),
                "category": data.get("category", ""),
                "tags": data.get("tags", []),
                "score": data.get("score", 0),
                "author": data.get("author", ""),
                "subreddit": data.get("subreddit", ""),
                "has_locale_variant": has_locale,
                "image": data.get("image", ""),
            })

    # Sort by score descending
    results.sort(key=lambda x: x.get("score", 0), reverse=True)
    return results


def main():
    parser = argparse.ArgumentParser(description="Search OpenMeme memes")
    parser.add_argument("query", help="Search term")
    parser.add_argument("--locale", help="Exclude already-localized files for this locale")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--limit", type=int, default=50, help="Max results")
    args = parser.parse_args()

    results = search_memes(args.repo, args.query, args.locale)
    results = results[:args.limit]

    if args.json:
        print(json.dumps(results, indent=2, ensure_ascii=False))
    else:
        print(f"Found {len(results)} meme(s) matching '{args.query}':\n")
        for i, r in enumerate(results, 1):
            status = " [ALREADY LOCALIZED]" if r["has_locale_variant"] else ""
            print(f"  {i}. {r['title']}{status}")
            print(f"     Slug: {r['slug']} | Category: {r['category']} | Score: {r['score']}")
            print(f"     Path: {r['path']}")
            if r["description"]:
                desc = r["description"][:80]
                if len(r["description"]) > 80:
                    desc += "..."
                print(f"     {desc}")
            print()


if __name__ == "__main__":
    main()
