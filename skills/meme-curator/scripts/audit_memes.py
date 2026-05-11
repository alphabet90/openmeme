#!/usr/bin/env python3
"""
audit_memes.py - Audit meme metadata quality and generate SEO reports.

Scores each meme's metadata across 5 dimensions (title, description, tags,
slug, completeness) and produces a quality report with priority sorting.

Usage:
    # Audit a single meme
    python audit_memes.py --source memes/simpsons/homer.mdx --repo ~/openmeme

    # Audit a category folder
    python audit_memes.py --category simpsons --repo ~/openmeme

    # Audit with query filter
    python audit_memes.py --query "football" --repo ~/openmeme

    # Sort by lowest score first (priority queue)
    python audit_memes.py --category simpsons --sort-by-score --repo ~/openmeme

    # Output JSON for batch processing
    python audit_memes.py --category simpsons --json --repo ~/openmeme

    # Filter by score threshold (find poor quality)
    python audit_memes.py --category simpsons --max-score 70 --repo ~/openmeme

    # Filter by date (recent memes only)
    python audit_memes.py --category simpsons --since 2025-04-01 --repo ~/openmeme
"""

import os
import re
import json
import argparse
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


def score_title(title: str) -> int:
    """Score title quality (0-25)."""
    if not title:
        return 0
    length = len(title)
    score = 25

    # Length penalties
    if length < 10:
        score -= 15
    elif length < 20:
        score -= 10
    elif length < 30:
        score -= 5
    elif length > 80:
        score -= 10
    elif length > 70:
        score -= 5

    # ALL CAPS penalty
    if title.upper() == title and len(title) > 5:
        score -= 8

    # Emoji/special chars penalty
    if any(ord(c) > 127 and not c.isalpha() for c in title[:5]):
        score -= 5

    # Excessive punctuation
    if title.count("!") > 1 or title.count("?") > 1:
        score -= 5

    # Generic title penalty
    generic = ["meme", "funny", "lol", "random"]
    if title.lower().strip() in generic:
        score -= 10

    return max(0, score)


def score_description(desc: str) -> int:
    """Score description quality (0-30)."""
    if not desc:
        return 0
    length = len(desc)
    score = 30

    # Length penalties
    if length < 20:
        score -= 20
    elif length < 40:
        score -= 15
    elif length < 60:
        score -= 10
    elif length < 80:
        score -= 5
    elif length > 250:
        score -= 8
    elif length > 200:
        score -= 3

    # Generic filler detection
    generic_starts = [
        "a meme showing", "a funny meme", "this meme shows",
        "image of", "picture of", "just a", "idk",
    ]
    lower = desc.lower()
    if any(lower.startswith(g) for g in generic_starts):
        score -= 8

    # Contains Reddit URL clutter
    if "reddit.com" in desc or "/r/" in desc:
        score -= 5

    # Very vague single-clause
    if length < 60 and "," not in desc and "." not in desc[10:]:
        score -= 5

    return max(0, score)


def score_tags(tags: list) -> int:
    """Score tags quality (0-25)."""
    if not tags:
        return 0
    count = len(tags)
    score = 25

    if count >= 10:
        score = 25
    elif count >= 8:
        score = 22
    elif count >= 5:
        score = 18
    elif count >= 3:
        score = 12
    elif count >= 1:
        score = 6

    # Check for generic/irrelevant tags
    generic = {"funny", "meme", "lol", "random", "idk", "nice", "cool", "good"}
    bad_tags = sum(1 for t in tags if t.lower() in generic)
    score -= bad_tags * 3

    # Check formatting
    for t in tags:
        if " " in t or t != t.lower():
            score -= 2
            break

    return max(0, score)


def score_slug(slug: str, title: str) -> int:
    """Score slug quality (0-10)."""
    if not slug:
        return 0
    score = 10

    if len(slug) > 80:
        score -= 4
    if "_" in slug:
        score -= 3
    if not re.match(r'^[a-z0-9-]+$', slug):
        score -= 3

    # Check if slug relates to title
    title_words = set(re.findall(r'[a-z]+', title.lower())) if title else set()
    slug_words = set(slug.split("-"))
    if title_words and not slug_words & title_words:
        score -= 3

    return max(0, score)


def score_completeness(data: dict) -> int:
    """Score frontmatter completeness (0-10)."""
    required = ["title", "description", "slug", "category", "tags", "image"]
    optional = ["author", "subreddit", "score", "created_at", "source_url", "post_url"]

    score = 0
    for f in required:
        if data.get(f):
            score += 1

    # Optional fields bonus
    optional_present = sum(1 for f in optional if data.get(f))
    score += min(4, optional_present // 2)

    return min(10, score)


def audit_meme(mdx_path: str) -> dict:
    """Audit a single meme and return full report."""
    try:
        content = Path(mdx_path).read_text(encoding="utf-8")
    except Exception as e:
        return {"path": mdx_path, "error": str(e), "total": 0}

    data = parse_frontmatter(content)
    if not data:
        return {"path": mdx_path, "error": "No frontmatter found", "total": 0}

    title_score = score_title(data.get("title", ""))
    desc_score = score_description(data.get("description", ""))
    tags_score = score_tags(data.get("tags", []))
    slug_score = score_slug(data.get("slug", ""), data.get("title", ""))
    comp_score = score_completeness(data)
    total = title_score + desc_score + tags_score + slug_score + comp_score

    # Identify issues
    issues = []
    title = data.get("title", "")
    desc = data.get("description", "")
    tags = data.get("tags", [])

    if len(title) > 80:
        issues.append("title_too_long")
    elif len(title) < 20:
        issues.append("title_too_short")
    if title.upper() == title and len(title) > 5:
        issues.append("title_all_caps")
    if len(desc) < 60:
        issues.append("description_too_short")
    if len(desc) < 20:
        issues.append("description_missing")
    if any(g in desc.lower() for g in ["a meme showing", "a funny"]):
        issues.append("description_generic")
    if len(tags) < 5:
        issues.append("too_few_tags")
    if len(tags) < 3:
        issues.append("tag_starvation")

    grade = "F"
    if total >= 90: grade = "A+"
    elif total >= 80: grade = "A"
    elif total >= 70: grade = "B"
    elif total >= 60: grade = "C"
    elif total >= 50: grade = "D"

    return {
        "path": mdx_path,
        "slug": data.get("slug", ""),
        "title": data.get("title", ""),
        "scores": {
            "title": title_score,
            "description": desc_score,
            "tags": tags_score,
            "slug": slug_score,
            "completeness": comp_score,
            "total": total,
        },
        "grade": grade,
        "issues": issues,
        "metrics": {
            "title_length": len(title),
            "description_length": len(desc),
            "tag_count": len(tags),
        },
    }


def find_memes(repo_path: str, category: str = None, query: str = None,
               since: str = None, max_score: int = None) -> list:
    """Find memes matching filters."""
    memes_dir = Path(repo_path) / "memes"
    if not memes_dir.exists():
        return []

    results = []

    if category:
        target_dir = memes_dir / category
        if target_dir.exists():
            mdx_files = list(target_dir.glob("*.mdx"))
        else:
            return []
    else:
        mdx_files = list(memes_dir.rglob("*.mdx"))

    # Exclude localized variants
    mdx_files = [f for f in mdx_files if not re.search(r'\.[a-z]{2}-[A-Z]{2}\.mdx$', f.name)]

    for mdx in mdx_files:
        try:
            content = mdx.read_text(encoding="utf-8")
        except:
            continue

        data = parse_frontmatter(content)

        # Query filter
        if query:
            q = query.lower()
            searchable = " ".join([
                data.get("title", ""),
                data.get("description", ""),
                data.get("category", ""),
                " ".join(data.get("tags", [])),
            ]).lower()
            if q not in searchable:
                continue

        # Date filter
        if since:
            created = data.get("created_at", "")
            if created and created < since:
                continue

        # Score filter (applied after audit)
        results.append(str(mdx))

    return results


def main():
    parser = argparse.ArgumentParser(description="Audit meme metadata quality")
    parser.add_argument("--source", help="Single MDX file to audit")
    parser.add_argument("--category", help="Category folder to audit")
    parser.add_argument("--query", help="Search query filter")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--sort-by-score", action="store_true", help="Sort by lowest score first")
    parser.add_argument("--max-score", type=int, help="Only show memes with score <= N")
    parser.add_argument("--since", help="Only audit memes created after YYYY-MM-DD")
    parser.add_argument("--limit", type=int, default=50, help="Max results")
    parser.add_argument("--output", help="Write JSON report to file")
    args = parser.parse_args()

    repo_path = os.path.abspath(args.repo)

    # Collect targets
    if args.source:
        targets = [args.source]
    else:
        targets = find_memes(repo_path, args.category, args.query, args.since)

    if not targets:
        print("No memes found matching criteria.")
        return 1

    # Audit each
    reports = []
    for t in targets:
        if not Path(t).is_absolute():
            t = os.path.join(repo_path, t)
        report = audit_meme(t)
        reports.append(report)

    # Sort by score if requested
    if args.sort_by_score:
        reports.sort(key=lambda r: r.get("scores", {}).get("total", 0))
    else:
        reports.sort(key=lambda r: r.get("scores", {}).get("total", 0), reverse=True)

    # Filter by max score
    if args.max_score is not None:
        reports = [r for r in reports if r.get("scores", {}).get("total", 100) <= args.max_score]

    reports = reports[:args.limit]

    if not reports:
        print("No memes match the score filter.")
        return 0

    if args.json or args.output:
        output = json.dumps(reports, indent=2, ensure_ascii=False)
        if args.output:
            Path(args.output).write_text(output, encoding="utf-8")
            print(f"Report written to {args.output}")
        else:
            print(output)
        return 0

    # Human-readable report
    total_memes = len(reports)
    avg_score = sum(r["scores"]["total"] for r in reports) / total_memes if reports else 0

    print(f"\n{'='*60}")
    print(f"  MEME CURATOR AUDIT REPORT")
    print(f"{'='*60}")
    print(f"  Scope: {args.category or args.query or args.source or 'all memes'}")
    print(f"  Memes audited: {total_memes}")
    print(f"  Average score: {avg_score:.1f}/100")

    # Grade distribution
    grades = {}
    for r in reports:
        g = r["grade"]
        grades[g] = grades.get(g, 0) + 1
    print(f"  Grade distribution: ", end="")
    for g in ["A+", "A", "B", "C", "D", "F"]:
        if g in grades:
            print(f"{g}={grades[g]}", end=" ")
    print()
    print(f"{'='*60}\n")

    # Top issues
    all_issues = []
    for r in reports:
        all_issues.extend(r.get("issues", []))
    if all_issues:
        from collections import Counter
        issue_counts = Counter(all_issues)
        print("  Top issues:")
        for issue, count in issue_counts.most_common(5):
            print(f"    - {issue}: {count} memes")
        print()

    # Individual reports
    for i, r in enumerate(reports, 1):
        s = r["scores"]
        slug = r.get("slug", "unknown")
        title = r.get("title", "Untitled")
        grade = r["grade"]
        issues = r.get("issues", [])

        # Color grade
        grade_display = grade
        if grade in ("A+", "A"):
            grade_display = f"[{grade}]"
        elif grade in ("B", "C"):
            grade_display = f"[{grade}]"
        else:
            grade_display = f"[{grade}]"

        print(f"  {i}. {title[:55]}{'...' if len(title) > 55 else ''}")
        print(f"     Slug: {slug}")
        print(f"     Score: {s['total']}/100 {grade_display} | Title: {s['title']}/25 | Desc: {s['description']}/30 | Tags: {s['tags']}/25 | Slug: {s['slug']}/10 | Comp: {s['completeness']}/10")
        if issues:
            print(f"     Issues: {', '.join(issues[:4])}")
        print()

    # Summary stats
    if total_memes > 1:
        print(f"{'='*60}")
        print(f"  SUMMARY")
        print(f"{'='*60}")
        needs_work = sum(1 for r in reports if r["scores"]["total"] < 80)
        print(f"  Memes needing improvement (score < 80): {needs_work}/{total_memes}")
        print(f"  Potential score gain if all curated to 90: +{sum(max(0, 90 - r['scores']['total']) for r in reports):.0f} points")
        print()


if __name__ == "__main__":
    exit(main() or 0)
