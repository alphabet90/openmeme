#!/usr/bin/env python3
"""
curate_meme.py - Apply SEO-optimized metadata improvements to meme MDX files.

Reads a source MDX (or batch JSON), applies curated metadata improvements,
rewrites the MDX file, and optionally handles git branch/commit/push.

Usage:
    # Single meme with inline improvements
    python curate_meme.py --source memes/simpsons/homer.mdx \
      --repo ~/openmeme \
      --title "Homer Strangling Bart Rage Template" \
      --description "Homer Simpson aggressively strangling Bart Simpson. Classic reaction template for expressing extreme frustration or anger at someone's foolish actions. From The Simpsons animated series." \
      --tags "simpsons,homer-simpson,bart-simpson,angry,strangling,template,reaction,animated,tv-show,classic,frustration"

    # Batch mode via JSON
    python curate_meme.py --batch improvements.json --repo ~/openmeme

    # With git workflow
    python curate_meme.py --source memes/simpsons/homer.mdx --repo ~/openmeme \
      --title "..." --description "..." --tags "..." --git --auto-branch --push

    # Dry run to preview changes
    python curate_meme.py --source memes/simpsons/homer.mdx --repo ~/openmeme \
      --title "New Title" --description "New desc..." --dry-run
"""

import os
import re
import json
import argparse
import subprocess
from pathlib import Path
from datetime import datetime


def parse_frontmatter(content: str) -> tuple:
    """Extract YAML frontmatter and body from MDX content."""
    match = re.search(r"^(---\n.*?\n---)\n*(.*)$", content, re.DOTALL)
    if not match:
        return {}, content

    fm_text = match.group(1)
    body = match.group(2)

    data = {}
    for line in fm_text.replace("---", "").strip().split("\n"):
        line = line.strip()
        if not line:
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
    return data, body


def yaml_str(s: str) -> str:
    """Escape string for YAML frontmatter."""
    return (s or "").replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")


def build_mdx(data: dict, body: str) -> str:
    """Rebuild MDX content from frontmatter data and body."""
    # Build frontmatter lines
    lines = ["---"]
    field_order = ["title", "description", "author", "subreddit", "category",
                   "slug", "score", "created_at", "source_url", "post_url", "image", "tags"]

    for key in field_order:
        if key not in data:
            continue
        value = data[key]
        if key == "tags" and isinstance(value, list):
            tags_str = ", ".join(f'"{yaml_str(t)}"' for t in value)
            lines.append(f"tags: [{tags_str}]")
        elif isinstance(value, int):
            lines.append(f"{key}: {value}")
        elif isinstance(value, str):
            lines.append(f'{key}: "{yaml_str(value)}"')

    # Add any remaining fields not in order
    for key, value in data.items():
        if key in field_order:
            continue
        if isinstance(value, list):
            items_str = ", ".join(f'"{yaml_str(v)}"' for v in value)
            lines.append(f"{key}: [{items_str}]")
        elif isinstance(value, int):
            lines.append(f"{key}: {value}")
        elif isinstance(value, str):
            lines.append(f'{key}: "{yaml_str(value)}"')

    lines.append("---")

    # Clean body
    body = body.strip()

    return "\n".join(lines) + "\n\n" + body + "\n"


def parse_tags_input(tags_input: str) -> list:
    """Parse tags from comma-separated string or list."""
    if isinstance(tags_input, list):
        return [t.strip() for t in tags_input if t.strip()]
    return [t.strip() for t in tags_input.split(",") if t.strip()]


def git_create_branch(repo_path: str, branch_name: str) -> bool:
    """Create and checkout a new git branch."""
    try:
        subprocess.run(
            ["git", "checkout", "-b", branch_name],
            cwd=repo_path, capture_output=True, text=True, check=True,
        )
        print(f"[git] Created branch: {branch_name}")
        return True
    except subprocess.CalledProcessError as e:
        if "already exists" in e.stderr:
            try:
                subprocess.run(
                    ["git", "checkout", branch_name],
                    cwd=repo_path, capture_output=True, text=True, check=True,
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
        subprocess.run(
            ["git", "add"] + paths,
            cwd=repo_path, capture_output=True, text=True, check=True,
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
            cwd=repo_path, capture_output=True, text=True, check=True,
        )
        print(f"[git] Committed: {message.split(chr(10))[0]}")
        return True
    except subprocess.CalledProcessError as e:
        if "nothing to commit" in e.stderr:
            print("[git] Nothing to commit")
            return True
        print(f"[git] Error committing: {e.stderr}")
        return False


def git_push(repo_path: str, branch_name: str) -> bool:
    """Push branch to origin."""
    try:
        subprocess.run(
            ["git", "push", "origin", branch_name],
            cwd=repo_path, capture_output=True, text=True, check=True,
        )
        print(f"[git] Pushed to origin/{branch_name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[git] Error pushing: {e.stderr}")
        return False


def generate_branch_name(category: str, count: int) -> str:
    """Generate a descriptive branch name."""
    timestamp = datetime.now().strftime("%Y%m%d")
    safe_cat = re.sub(r"[^a-z0-9-]", "-", category.lower())[:30]
    return f"curate/{safe_cat}-{timestamp}"


def apply_curation(source_path: str, repo_path: str, improvements: dict) -> str:
    """Apply improvements to a meme MDX file. Returns relative path."""
    src = Path(source_path)
    if not src.is_absolute():
        src = Path(repo_path) / src

    content = src.read_text(encoding="utf-8")
    data, body = parse_frontmatter(content)

    # Store original values for diff
    original = {
        "title": data.get("title", ""),
        "description": data.get("description", ""),
        "tags": list(data.get("tags", [])),
    }

    # Apply improvements
    if "title" in improvements:
        data["title"] = improvements["title"]
    if "description" in improvements:
        data["description"] = improvements["description"]
    if "tags" in improvements:
        data["tags"] = parse_tags_input(improvements["tags"])

    # Update body title heading to match new title
    new_title = data.get("title", "")
    body = re.sub(r'^# .+$', f"# {new_title}", body, count=1, flags=re.MULTILINE)

    # Rebuild MDX
    new_content = build_mdx(data, body)

    # Write file
    src.write_text(new_content, encoding="utf-8")

    return str(src.relative_to(Path(repo_path)))


def main():
    parser = argparse.ArgumentParser(description="Curate meme metadata")
    parser.add_argument("--source", help="Source MDX file path (relative to repo)")
    parser.add_argument("--batch", help="JSON file with multiple improvements")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--title", help="Optimized title")
    parser.add_argument("--description", help="Optimized description")
    parser.add_argument("--tags", help="Optimized tags (comma-separated)")
    parser.add_argument("--git", action="store_true", help="Enable git workflow")
    parser.add_argument("--branch", help="Git branch name")
    parser.add_argument("--auto-branch", action="store_true", help="Auto-generate branch name")
    parser.add_argument("--push", action="store_true", help="Push after commit")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing files")
    parser.add_argument("--output", help="Write report JSON to file")
    args = parser.parse_args()

    repo_path = os.path.abspath(args.repo)
    modified_files = []
    report = []

    # Collect items
    items = []
    if args.batch:
        with open(args.batch, "r", encoding="utf-8") as f:
            items = json.load(f)
    elif args.source:
        improvements = {}
        if args.title: improvements["title"] = args.title
        if args.description: improvements["description"] = args.description
        if args.tags: improvements["tags"] = args.tags
        items.append({"source_path": args.source, "improvements": improvements})
    else:
        print("Error: Provide --source or --batch")
        return 1

    # Determine category for branch naming
    category = "memes"
    if items:
        sp = items[0].get("source_path", "")
        parts = sp.split("/")
        if len(parts) >= 3 and parts[0] == "memes":
            category = parts[1]

    # Git branch setup
    branch_name = None
    if args.git or args.push:
        if args.branch:
            branch_name = args.branch
        elif args.auto_branch:
            branch_name = generate_branch_name(category, len(items))
        else:
            print("Error: --git requires --branch or --auto-branch")
            return 1

        if not args.dry_run:
            git_create_branch(repo_path, branch_name)

    # Process each item
    for item in items:
        source_path = item.get("source_path", "")
        improvements = item.get("improvements", {})

        if not improvements:
            print(f"Warning: No improvements for {source_path}, skipping")
            continue

        src = Path(source_path)
        if not src.is_absolute():
            src = Path(repo_path) / src

        slug = src.stem
        # Remove locale suffix from slug display
        for loc in ["es-AR", "pt-BR", "en-US", "en-GB", "fr-FR", "de-DE"]:
            if slug.endswith(f".{loc}"):
                slug = slug[: -len(loc) - 1]
                break

        print(f"\nCurating: {slug}")

        # Preview diff
        if args.dry_run:
            content = src.read_text(encoding="utf-8")
            data, _ = parse_frontmatter(content)
            print("  DRY RUN — Changes preview:")
            if "title" in improvements:
                old = data.get("title", "")
                new = improvements["title"]
                print(f"    Title: \"{old[:50]}...\" ({len(old)} chars) → \"{new[:50]}...\" ({len(new)} chars)")
            if "description" in improvements:
                old = data.get("description", "")
                new = improvements["description"]
                print(f"    Desc:  {len(old)} chars → {len(new)} chars")
            if "tags" in improvements:
                old = data.get("tags", [])
                new = parse_tags_input(improvements["tags"])
                print(f"    Tags:  {len(old)} tags → {len(new)} tags (+{len(new) - len(old)})")
            continue

        # Apply
        rel_path = apply_curation(source_path, repo_path, improvements)
        modified_files.append(rel_path)

        # Build report entry
        content = src.read_text(encoding="utf-8")
        data, _ = parse_frontmatter(content)
        report.append({
            "slug": slug,
            "path": rel_path,
            "title": data.get("title", ""),
            "title_length": len(data.get("title", "")),
            "description_length": len(data.get("description", "")),
            "tag_count": len(data.get("tags", [])),
        })

        print(f"  ✓ Updated: {rel_path}")

    # Git commit and push
    if not args.dry_run and modified_files:
        if args.git or args.push:
            git_add(repo_path, modified_files)

            count = len(modified_files)
            commit_msg = f"""curate({category}): optimize {count} meme entries for SEO

- Rewrite titles for searchability (30-70 chars)
- Expand descriptions with scene details and usage context
- Enrich tags for discoverability
- Standardize metadata quality"""

            if git_commit(repo_path, commit_msg) and args.push and branch_name:
                git_push(repo_path, branch_name)

    # Summary
    if report:
        print(f"\n{'='*50}")
        print(f"  CURATION SUMMARY")
        print(f"{'='*50}")
        print(f"  Memes curated: {len(report)}")
        avg_title = sum(r["title_length"] for r in report) / len(report)
        avg_desc = sum(r["description_length"] for r in report) / len(report)
        avg_tags = sum(r["tag_count"] for r in report) / len(report)
        print(f"  Avg title length: {avg_title:.0f} chars")
        print(f"  Avg description:  {avg_desc:.0f} chars")
        print(f"  Avg tags:         {avg_tags:.1f}")

    if args.output and report:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, ensure_ascii=False)

    print(f"\nDone. Modified {len(modified_files)} file(s).")
    return 0


if __name__ == "__main__":
    exit(main() or 0)
