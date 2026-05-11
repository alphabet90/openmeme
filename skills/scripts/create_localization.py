#!/usr/bin/env python3
"""
create_localization.py - Generate localized MDX files and handle git workflow.

Creates {slug}.{locale}.mdx files with culturally-aware translations
and optionally manages git branch/commit/push workflow.

Usage:
    # Generate a single localized MDX
    python create_localization.py --source memes/simpsons/homer.mdx --locale es-AR --repo ~/openmeme

    # Generate for multiple sources (from stdin or file)
    python create_localization.py --batch batch.json --locale es-AR --repo ~/openmeme

    # With full git workflow
    python create_localization.py --source memes/simpsons/homer.mdx --locale es-AR --repo ~/openmeme --git --branch i18n/es-AR-simpsons

    # Auto-generate branch and push
    python create_localization.py --batch batch.json --locale pt-BR --repo ~/openmeme --git --auto-branch --push
"""

import os
import re
import json
import argparse
import subprocess
from pathlib import Path
from datetime import datetime


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


def yaml_str(s: str) -> str:
    """Escape string for YAML frontmatter."""
    return (s or "").replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")


def generate_localized_mdx(source_data: dict, locale: str, translated_content: dict) -> str:
    """
    Generate localized MDX content.

    Args:
        source_data: Parsed frontmatter from source MDX
        locale: Target locale code
        translated_content: Dict with translated fields:
            - title (str)
            - description (str)
            - tags (list[str])
            - body (str, optional extra body content)
            - cultural_context (str, optional cultural notes)

    Returns:
        Complete MDX content as string
    """
    slug = source_data.get("slug", "meme")
    image = source_data.get("image", f"./{slug}.jpg")
    tags = translated_content.get("tags", source_data.get("tags", []))
    tags_str = ", ".join(f'"{yaml_str(t)}"' for t in tags)

    body_extra = translated_content.get("body", "").strip()
    cultural = translated_content.get("cultural_context", "").strip()

    # Cultural context section title by locale
    ctx_titles = {
        "es-AR": "Contexto Cultural",
        "es-MX": "Contexto Cultural",
        "es-ES": "Contexto Cultural",
        "es-CO": "Contexto Cultural",
        "es-CL": "Contexto Cultural",
        "pt-BR": "Contexto Cultural",
        "pt-PT": "Contexto Cultural",
        "en-US": "Cultural Context",
        "en-GB": "Cultural Context",
        "en-CA": "Cultural Context",
        "en-AU": "Cultural Context",
        "fr-FR": "Contexte Culturel",
        "de-DE": "Kultureller Kontext",
        "it-IT": "Contesto Culturale",
        "ja-JP": "\u6587\u5316\u7684\u80cc\u666f",
    }
    ctx_title = ctx_titles.get(locale, "Cultural Context")

    content = f"""---
title: "{yaml_str(translated_content.get('title', source_data.get('title', '')))}"
description: "{yaml_str(translated_content.get('description', source_data.get('description', '')))}"
author: "{yaml_str(source_data.get('author', ''))}"
subreddit: "{yaml_str(source_data.get('subreddit', ''))}"
category: "{yaml_str(source_data.get('category', ''))}"
slug: "{slug}"
locale: "{locale}"
score: {source_data.get("score", 0)}
created_at: "{source_data.get('created_at', '')}"
source_url: "{yaml_str(source_data.get('source_url', ''))}"
post_url: "{yaml_str(source_data.get('post_url', ''))}"
image: "{yaml_str(image)}"
tags: [{tags_str}]
---

# {translated_content.get("title", source_data.get("title", ""))}

{translated_content.get("description", source_data.get("description", ""))}

**Category**: {source_data.get("category", "")} | **Author**: u/{source_data.get("author", "")} | **Score**: {source_data.get("score", 0)} upvotes

[View original post on Reddit]({source_data.get("post_url", "")})
"""

    if body_extra:
        content += f"\n{body_extra}\n"

    if cultural:
        content += f"""
## {ctx_title}

{cultural}
"""

    return content


def git_create_branch(repo_path: str, branch_name: str) -> bool:
    """Create and checkout a new git branch."""
    try:
        subprocess.run(
            ["git", "checkout", "-b", branch_name],
            cwd=repo_path,
            capture_output=True,
            text=True,
            check=True,
        )
        print(f"[git] Created branch: {branch_name}")
        return True
    except subprocess.CalledProcessError as e:
        if "already exists" in e.stderr:
            # Try to checkout existing branch
            try:
                subprocess.run(
                    ["git", "checkout", branch_name],
                    cwd=repo_path,
                    capture_output=True,
                    text=True,
                    check=True,
                )
                print(f"[git] Checked out existing branch: {branch_name}")
                return True
            except subprocess.CalledProcessError:
                print(f"[git] Error: Branch '{branch_name}' exists but checkout failed")
                return False
        print(f"[git] Error creating branch: {e.stderr}")
        return False


def git_add(repo_path: str, paths: list) -> bool:
    """Stage files for commit."""
    if not paths:
        return True
    try:
        # Use relative paths from repo root
        subprocess.run(
            ["git", "add"] + paths,
            cwd=repo_path,
            capture_output=True,
            text=True,
            check=True,
        )
        print(f"[git] Staged {len(paths)} file(s)")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[git] Error staging files: {e.stderr}")
        return False


def git_commit(repo_path: str, message: str) -> bool:
    """Commit staged files."""
    try:
        subprocess.run(
            ["git", "commit", "-m", message],
            cwd=repo_path,
            capture_output=True,
            text=True,
            check=True,
        )
        print(f"[git] Committed: {message.split(chr(10))[0]}")
        return True
    except subprocess.CalledProcessError as e:
        if "nothing to commit" in e.stderr or "nothing to commit" in e.stdout:
            print("[git] Nothing to commit")
            return True
        print(f"[git] Error committing: {e.stderr}")
        return False


def git_push(repo_path: str, branch_name: str) -> bool:
    """Push branch to origin."""
    try:
        subprocess.run(
            ["git", "push", "origin", branch_name],
            cwd=repo_path,
            capture_output=True,
            text=True,
            check=True,
        )
        print(f"[git] Pushed to origin/{branch_name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[git] Error pushing: {e.stderr}")
        return False


def generate_branch_name(locale: str, category: str) -> str:
    """Generate a descriptive branch name."""
    timestamp = datetime.now().strftime("%Y%m%d")
    safe_cat = re.sub(r"[^a-z0-9-]", "-", category.lower())[:30]
    return f"i18n/{locale}-{safe_cat}-{timestamp}"


def write_mdx_file(repo_path: str, source_path: str, locale: str, content: str) -> str:
    """Write localized MDX file next to its source."""
    source = Path(source_path)
    if not source.is_absolute():
        source = Path(repo_path) / source

    slug = source.stem
    # Handle case where source already has locale suffix
    for loc in ["es-AR", "pt-BR", "en-US", "en-GB", "fr-FR", "de-DE", "es-MX", "es-ES"]:
        if slug.endswith(f".{loc}"):
            slug = slug[: -len(loc) - 1]
            break

    dest = source.with_name(f"{slug}.{locale}.mdx")
    dest.write_text(content, encoding="utf-8")
    return str(dest)


def main():
    parser = argparse.ArgumentParser(description="Generate localized MDX files")
    parser.add_argument("--source", help="Source MDX file path (relative to repo)")
    parser.add_argument("--batch", help="JSON file with multiple sources and translations")
    parser.add_argument("--locale", required=True, help="Target locale code (e.g., es-AR)")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--git", action="store_true", help="Enable git workflow")
    parser.add_argument("--branch", help="Git branch name (auto-generated if omitted)")
    parser.add_argument("--auto-branch", action="store_true", help="Auto-generate branch name")
    parser.add_argument("--push", action="store_true", help="Push after commit")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing files")
    parser.add_argument("--output", help="Write output list of created files to JSON")
    args = parser.parse_args()

    repo_path = os.path.abspath(args.repo)
    created_files = []

    # Collect items to process
    items = []

    if args.batch:
        with open(args.batch, "r", encoding="utf-8") as f:
            items = json.load(f)
    elif args.source:
        # Read source MDX
        src_path = Path(args.source)
        if not src_path.is_absolute():
            src_path = Path(repo_path) / src_path
        content = src_path.read_text(encoding="utf-8")
        source_data = parse_frontmatter(content)
        items.append({
            "source_path": args.source,
            "source_data": source_data,
            "translation": {},  # Agent fills this
        })
    else:
        print("Error: Provide --source or --batch")
        return 1

    # Determine git branch
    branch_name = None
    if args.git or args.push:
        if args.branch:
            branch_name = args.branch
        elif args.auto_branch:
            # Use first item's category for branch name
            cat = items[0].get("source_data", {}).get("category", "memes") if items else "memes"
            branch_name = generate_branch_name(args.locale, cat)
        else:
            print("Error: --git requires --branch or --auto-branch")
            return 1

        if not args.dry_run:
            git_create_branch(repo_path, branch_name)

    # Process each item
    for item in items:
        source_path = item.get("source_path", "")
        source_data = item.get("source_data", {})
        translation = item.get("translation", {})

        if not source_data:
            src = Path(source_path)
            if not src.is_absolute():
                src = Path(repo_path) / src
            content = src.read_text(encoding="utf-8")
            source_data = parse_frontmatter(content)

        if not translation:
            print(f"Warning: No translation data for {source_path}, skipping")
            continue

        slug = source_data.get("slug", Path(source_path).stem)
        print(f"\nProcessing: {slug} -> {args.locale}")

        mdx_content = generate_localized_mdx(source_data, args.locale, translation)

        if args.dry_run:
            print("--- DRY RUN ---")
            print(mdx_content[:500])
            print("...")
        else:
            dest_path = write_mdx_file(repo_path, source_path, args.locale, mdx_content)
            rel_path = os.path.relpath(dest_path, repo_path)
            created_files.append(rel_path)
            print(f"  Created: {rel_path}")

    # Git commit and push
    if not args.dry_run and (args.git or args.push) and created_files:
        # Use paths relative to repo
        git_add(repo_path, created_files)

        # Build commit message
        cat = items[0].get("source_data", {}).get("category", "memes") if items else "memes"
        count = len(created_files)
        commit_msg = f"""i18n({args.locale}): localize {count} {cat} meme(s)

- Add cultural context for {args.locale} audience
- Translate descriptions, titles, and tags
- Create {count} localized MDX variant(s)"""

        if git_commit(repo_path, commit_msg) and args.push and branch_name:
            git_push(repo_path, branch_name)

    # Output file list
    if args.output and created_files:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(created_files, f, indent=2)

    print(f"\nDone. Created {len(created_files)} localized file(s).")
    return 0


if __name__ == "__main__":
    exit(main() or 0)
