#!/usr/bin/env python3
"""
analyze_categories.py - Full category analysis for OpenMeme.

Inventories all categories, detects merge candidates by name similarity,
flags threshold violators, and generates action plans.

Supports three operating modes:
  --mode suggest    : Print proposed changes (default, no files modified)
  --mode autonomous : Apply changes and optionally git commit/push
  --mode interactive: Review each merge one by one

Usage:
    # Full analysis (suggestion mode, default)
    python analyze_categories.py --repo ~/openmeme

    # With threshold filter
    python analyze_categories.py --threshold 2 --repo ~/openmeme

    # Autonomous mode with git
    python analyze_categories.py --mode autonomous --git --repo ~/openmeme

    # Interactive mode
    python analyze_categories.py --mode interactive --repo ~/openmeme

    # Export JSON reports
    python analyze_categories.py --json-out ./reports --repo ~/openmeme
"""

import os
import re
import json
import shutil
import argparse
import difflib
import subprocess
from pathlib import Path
from collections import defaultdict
from datetime import datetime

# ── Hardcoded merge rules (canonical name → list of variants) ──
MERGE_RULES = {
    "south-park": ["southpark"],
    "fairly-oddparents": ["fairly-odd-parents"],
    "courage-the-cowardly-dog": ["courage-cowardly-dog"],
    "reaccion-espanol": ["reaccion-espa-ol"],
    "absurdist-humor": ["absurd", "absurd-humor"],
    "argentina-football": ["argentina-futbol"],
    "football-reaction": ["futbol-reaction"],
    "harry-potter": ["harry-potter-crossover"],
    "cursed": ["cursed-car"],
}

# Franchise subcategory absorption rules
FRANCHISE_ABSORPTION = {
    "simpsons": [
        "simpsons-argentina", "simpsons-argentina-politics", "simpsons-comparison",
        "simpsons-courtroom", "simpsons-krusty", "simpsons-milei", "simpsons-political-satire",
    ],
    "star-wars": [
        "star-wars-dark-humor", "star-wars-force-ghost", "star-wars-prequel",
        "star-wars-reaction", "star-wars-spanish-pun",
    ],
    "dragon-ball": [
        "dragon-ball-argentina-politics", "dragon-ball-reaction",
    ],
    "jojo-bizarre-adventure": [
        "jojo-approaching", "jojo-reference-parody", "jojo-reaction",
    ],
    "wojak": [
        "wojak-argentina-police", "wojak-argentina-politics",
        "wojak-brainlet", "wojak-creepy",
    ],
    "spongebob": [
        "spongebob-caveman", "spongebob-larry", "spongebob-patrick-crying",
    ],
    "pepe": ["pepe-the-frog"],
}

# Reverse map: variant → canonical
VARIANT_TO_CANONICAL = {}
for canonical, variants in {**MERGE_RULES, **FRANCHISE_ABSORPTION}.items():
    for v in variants:
        VARIANT_TO_CANONICAL[v] = canonical


def parse_frontmatter(content: str) -> dict:
    """Extract YAML frontmatter."""
    match = re.search(r"---\n(.*?)\n---", content, re.DOTALL)
    if not match:
        return {}
    data = {}
    for line in match.group(1).split("\n"):
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
            else:
                try:
                    data[key] = int(value)
                except ValueError:
                    data[key] = value
    return data


def get_category_inventory(repo_path: str) -> dict:
    """Build full category inventory with meme counts."""
    memes_dir = Path(repo_path) / "memes"
    inventory = {}

    for cat_dir in sorted(memes_dir.iterdir()):
        if not cat_dir.is_dir():
            continue
        mdx_files = list(cat_dir.glob("*.mdx"))
        image_files = list(cat_dir.glob("*.png")) + list(cat_dir.glob("*.jpg")) + list(cat_dir.glob("*.jpeg")) + list(cat_dir.glob("*.gif")) + list(cat_dir.glob("*.webp"))

        # Exclude localized .{locale}.mdx files from count
        base_mdx = [f for f in mdx_files if not re.search(r'\.[a-z]{2}-[A-Z]{2}\.mdx$', f.name)]

        inventory[cat_dir.name] = {
            "path": str(cat_dir),
            "mdx_count": len(base_mdx),
            "image_count": len(image_files),
            "has_orphan_images": len(image_files) > len(base_mdx),
            "has_mdx_without_images": len(base_mdx) > len(image_files),
            "slugs": [f.stem for f in sorted(base_mdx)],
        }

    return inventory


def detect_similarity_merges(inventory: dict, cutoff: float = 0.75) -> list:
    """Detect merge candidates by name similarity."""
    categories = list(inventory.keys())
    candidates = []

    for i, a in enumerate(categories):
        for b in categories[i + 1:]:
            # Skip if already covered by hardcoded rules
            if b in VARIANT_TO_CANONICAL and VARIANT_TO_CANONICAL[b] == a:
                continue
            if a in VARIANT_TO_CANONICAL and VARIANT_TO_CANONICAL[a] == b:
                continue

            ratio = difflib.SequenceMatcher(None, a, b).ratio()
            if ratio >= cutoff:
                # Determine which should absorb (higher count wins)
                count_a = inventory[a]["mdx_count"]
                count_b = inventory[b]["mdx_count"]
                if count_a >= count_b:
                    source, target = b, a
                else:
                    source, target = a, b

                candidates.append({
                    "source": source,
                    "target": target,
                    "similarity": round(ratio, 3),
                    "source_count": inventory[source]["mdx_count"],
                    "target_count": inventory[target]["mdx_count"],
                    "type": "similarity",
                })

    # Sort by similarity descending
    candidates.sort(key=lambda x: x["similarity"], reverse=True)
    return candidates


def detect_hardcoded_merges(inventory: dict) -> list:
    """Detect merges from hardcoded rules."""
    candidates = []
    all_rules = {**MERGE_RULES, **FRANCHISE_ABSORPTION}

    for canonical, variants in all_rules.items():
        if canonical not in inventory:
            continue
        for variant in variants:
            if variant in inventory:
                candidates.append({
                    "source": variant,
                    "target": canonical,
                    "similarity": 1.0,
                    "source_count": inventory[variant]["mdx_count"],
                    "target_count": inventory[canonical]["mdx_count"],
                    "type": "hardcoded",
                })

    return candidates


def detect_threshold_violators(inventory: dict, threshold: int = 2) -> list:
    """Find categories below the existence threshold."""
    violators = []
    for name, data in inventory.items():
        if data["mdx_count"] < threshold:
            # Suggest target category
            target = suggest_migration_target(name, inventory)
            violators.append({
                "category": name,
                "count": data["mdx_count"],
                "suggested_target": target,
                "action": "delete" if data["mdx_count"] == 0 else "migrate",
            })
    return violators


def suggest_migration_target(category: str, inventory: dict) -> str:
    """Suggest the best target category for a small category."""
    # Check hardcoded rules first
    if category in VARIANT_TO_CANONICAL:
        return VARIANT_TO_CANONICAL[category]

    # Check franchise absorption
    for canonical, variants in FRANCHISE_ABSORPTION.items():
        if category in variants:
            return canonical

    # Generic: check if it ends with a known format suffix
    format_suffixes = {
        "-reaction": "reaction",
        "-shitpost": "shitpost",
        "-humor": "humor",
        "-parody": "parody",
        "-satire": "political-satire-photoshop",
    }
    for suffix, target in format_suffixes.items():
        if category.endswith(suffix) and target in inventory:
            return target

    # Check if stripping a prefix gives a valid category
    prefixes = ["argentina-", "simpsons-", "star-wars-", "anime-"]
    for prefix in prefixes:
        if category.startswith(prefix):
            stripped = category[len(prefix):]
            if stripped in inventory and inventory[stripped]["mdx_count"] >= 5:
                return stripped
            base = prefix.rstrip("-")
            if base in inventory:
                return base

    # Default fallback
    if "argentina" in category:
        return "argentina-humor"
    if category.endswith("-reaction"):
        return "reaction"

    return "others"


def generate_mv_commands(merges: list, inventory: dict, repo_path: str) -> list:
    """Generate mv commands and frontmatter updates for merges."""
    commands = []
    frontmatter_updates = []

    for merge in merges:
        source = merge["source"]
        target = merge["target"]
        source_dir = Path(repo_path) / "memes" / source
        target_dir = Path(repo_path) / "memes" / target

        if not source_dir.exists():
            continue

        for mdx_file in source_dir.glob("*.mdx"):
            # Skip locale variants
            if re.search(r'\.[a-z]{2}-[A-Z]{2}\.mdx$', mdx_file.name):
                continue

            slug = mdx_file.stem
            # Check for slug collision
            dest_mdx = target_dir / mdx_file.name
            counter = 2
            while dest_mdx.exists():
                slug = f"{mdx_file.stem}-{counter}"
                dest_mdx = target_dir / f"{slug}.mdx"
                counter += 1

            commands.append({
                "type": "move_mdx",
                "from": str(mdx_file),
                "to": str(dest_mdx),
                "slug": slug,
            })

            # Check for companion image
            for ext in [".png", ".jpg", ".jpeg", ".gif", ".webp"]:
                img = mdx_file.with_suffix(ext)
                if img.exists():
                    dest_img = target_dir / f"{slug}{ext}"
                    commands.append({
                        "type": "move_image",
                        "from": str(img),
                        "to": str(dest_img),
                    })
                    break

            # Frontmatter update
            commands.append({
                "type": "update_frontmatter",
                "file": str(dest_mdx),
                "old_category": source,
                "new_category": target,
                "add_tags": [source] if source not in FRANCHISE_ABSORPTION.get(target, []) else [],
            })

        # Directory removal
        commands.append({
            "type": "rmdir",
            "path": str(source_dir),
        })

    return commands


def apply_commands(commands: list, repo_path: str, dry_run: bool = False) -> dict:
    """Execute or preview commands."""
    stats = {"moved": 0, "deleted": 0, "updated": 0, "errors": 0}

    for cmd in commands:
        if dry_run:
            continue

        try:
            if cmd["type"] == "move_mdx":
                shutil.move(cmd["from"], cmd["to"])
                stats["moved"] += 1
            elif cmd["type"] == "move_image":
                if os.path.exists(cmd["from"]):
                    shutil.move(cmd["from"], cmd["to"])
            elif cmd["type"] == "rmdir":
                if os.path.exists(cmd["path"]) and not os.listdir(cmd["path"]):
                    os.rmdir(cmd["path"])
                    stats["deleted"] += 1
            elif cmd["type"] == "update_frontmatter":
                _update_category_in_mdx(cmd["file"], cmd["old_category"], cmd["new_category"], cmd.get("add_tags", []))
                stats["updated"] += 1
        except Exception as e:
            stats["errors"] += 1
            print(f"  Error: {e}")

    return stats


def _update_category_in_mdx(mdx_path: str, old_cat: str, new_cat: str, add_tags: list):
    """Update category field and add tags in MDX frontmatter."""
    path = Path(mdx_path)
    if not path.exists():
        return
    content = path.read_text(encoding="utf-8")

    # Update category line
    content = re.sub(
        rf'^category:\s*"{re.escape(old_cat)}"',
        f'category: "{new_cat}"',
        content,
        flags=re.MULTILINE,
    )

    # Add tags if needed
    if add_tags:
        # Find tags line
        tags_match = re.search(r'^tags:\s*\[(.*?)\]', content, re.MULTILINE)
        if tags_match:
            existing = [t.strip().strip('"').strip("'") for t in tags_match.group(1).split(",") if t.strip()]
            new_tags = existing[:]
            for t in add_tags:
                if t not in new_tags:
                    new_tags.append(t)
            if len(new_tags) > len(existing):
                tags_str = ", ".join(f'"{t}"' for t in new_tags)
                content = content[:tags_match.start()] + f"tags: [{tags_str}]" + content[tags_match.end():]

    path.write_text(content, encoding="utf-8")


def git_create_branch(repo_path: str, branch_name: str) -> bool:
    try:
        subprocess.run(["git", "checkout", "-b", branch_name], cwd=repo_path, capture_output=True, text=True, check=True)
        return True
    except subprocess.CalledProcessError:
        try:
            subprocess.run(["git", "checkout", branch_name], cwd=repo_path, capture_output=True, text=True, check=True)
            return True
        except:
            return False


def git_commit_and_push(repo_path: str, branch_name: str, message: str) -> bool:
    try:
        subprocess.run(["git", "add", "-A"], cwd=repo_path, capture_output=True, text=True, check=True)
        subprocess.run(["git", "commit", "-m", message], cwd=repo_path, capture_output=True, text=True, check=True)
        subprocess.run(["git", "push", "origin", branch_name], cwd=repo_path, capture_output=True, text=True, check=True)
        return True
    except subprocess.CalledProcessError as e:
        print(f"[git] Error: {e}")
        return False


def print_suggestion_report(merges: list, threshold_violators: list, inventory: dict, commands: list):
    """Print detailed suggestion report."""
    print(f"\n{'='*70}")
    print(f"  MEME CLASSIFIER — MODO SUGERENCIA")
    print(f"{'='*70}")

    total_cats = len(inventory)
    empty = sum(1 for d in inventory.values() if d["mdx_count"] == 0)
    single = sum(1 for d in inventory.values() if d["mdx_count"] == 1)
    below_thresh = sum(1 for d in inventory.values() if d["mdx_count"] < 2)

    print(f"\n  INVENTARIO ACTUAL")
    print(f"  {'─'*50}")
    print(f"  Total categorías:     {total_cats}")
    print(f"  Con 10+ memes:        {sum(1 for d in inventory.values() if d['mdx_count'] >= 10)}")
    print(f"  Con 2-9 memes:        {sum(1 for d in inventory.values() if 2 <= d['mdx_count'] < 10)}")
    print(f"  Con 1 meme:           {single}")
    print(f"  Vacías:               {empty}")

    print(f"\n  MERGES DETECTADOS: {len(merges)}")
    print(f"  {'─'*50}")

    hardcoded = [m for m in merges if m["type"] == "hardcoded"]
    similarity = [m for m in merges if m["type"] == "similarity"]

    if hardcoded:
        print(f"\n  Reglas predefinidas ({len(hardcoded)}):")
        for m in hardcoded:
            print(f"    mv memes/{m['source']}/* → memes/{m['target']}/")
            print(f"      ({m['source_count']} memes) → ({m['target_count']} memes)")

    if similarity:
        print(f"\n  Por similitud ({len(similarity)}):")
        for m in similarity[:15]:
            print(f"    mv memes/{m['source']}/* → memes/{m['target']}/")
            print(f"      similitud: {m['similarity']:.0%} | ({m['source_count']} → {m['target_count']} memes)")
        if len(similarity) > 15:
            print(f"    ... y {len(similarity) - 15} más")

    print(f"\n  UMBRAL DE EXISTENCIA (< 2 memes): {len(threshold_violators)}")
    print(f"  {'─'*50}")

    to_delete = [v for v in threshold_violators if v["action"] == "delete"]
    to_migrate = [v for v in threshold_violators if v["action"] == "migrate"]

    if to_delete:
        print(f"\n  Eliminar ({len(to_delete)}):")
        for v in to_delete[:10]:
            print(f"    rmdir memes/{v['category']}/  (vacía)")

    if to_migrate:
        print(f"\n  Migrar ({len(to_migrate)}):")
        for v in to_migrate[:15]:
            print(f"    mv memes/{v['category']}/* → memes/{v['suggested_target']}/")
        if len(to_migrate) > 15:
            print(f"    ... y {len(to_migrate) - 15} más")

    affected = len(commands)
    final_estimate = total_cats - len(merges) - len(to_delete)

    print(f"\n{'='*70}")
    print(f"  RESUMEN")
    print(f"  {'─'*50}")
    print(f"  Categorías a mergear:     {len(merges)}")
    print(f"  Categorías a eliminar:    {len(to_delete)}")
    print(f"  Categorías a migrar:      {len(to_migrate)}")
    print(f"  Archivos afectados:       {affected}")
    print(f"  Estimación final:         ~{final_estimate} categorías (desde {total_cats})")
    print(f"  Reducción:                {total_cats - final_estimate} categorías ({(total_cats - final_estimate) / total_cats * 100:.0f}%)")
    print(f"\n  Modo actual: SUGERENCIA (no se modificaron archivos)")
    print(f"  Para aplicar: re-ejecutar con --mode autonomous")
    print(f"{'='*70}\n")


def run_interactive_mode(merges: list, threshold_violators: list, inventory: dict, commands: list, repo_path: str):
    """Interactive mode: review each merge one by one."""
    print(f"\n{'='*70}")
    print(f"  MEME CLASSIFIER — MODO INTERACTIVO")
    print(f"  Revisaremos {len(merges)} merges candidatos")
    print(f"{'='*70}\n")

    approved = []

    for i, merge in enumerate(merges, 1):
        print(f"\n[{i}/{len(merges)}] {merge['source']} → {merge['target']}")
        print(f"  {merge['source']}: {merge['source_count']} memes")
        print(f"  {merge['target']}: {merge['target_count']} memes")
        print(f"  Similitud: {merge.get('similarity', 1.0):.0%} | Tipo: {merge['type']}")

        # Show sample memes from source
        source_dir = Path(repo_path) / "memes" / merge["source"]
        if source_dir.exists():
            samples = list(source_dir.glob("*.mdx"))[:2]
            for s in samples:
                try:
                    content = s.read_text(encoding="utf-8")
                    fm = parse_frontmatter(content)
                    print(f"  Ejemplo: {fm.get('title', s.stem)[:60]}")
                except:
                    pass

        while True:
            resp = input(f"  ¿Merge? [y/n/s/q]: ").strip().lower()
            if resp in ("y", "yes", "sí", "si"):
                approved.append(merge)
                break
            elif resp in ("n", "no"):
                break
            elif resp in ("s", "skip"):
                break
            elif resp in ("q", "quit"):
                print("  Saliendo del modo interactivo.")
                return approved

    print(f"\n  Aprobados: {len(approved)}/{len(merges)} merges")
    return approved


def main():
    parser = argparse.ArgumentParser(description="Analyze and standardize OpenMeme categories")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--threshold", type=int, default=2, help="Minimum memes per category")
    parser.add_argument("--mode", choices=["suggest", "autonomous", "interactive"], default="suggest",
                        help="Operating mode")
    parser.add_argument("--git", action="store_true", help="Create branch and commit (autonomous mode)")
    parser.add_argument("--json-out", help="Directory to write JSON reports")
    parser.add_argument("--similarity-cutoff", type=float, default=0.75, help="Similarity threshold")
    args = parser.parse_args()

    repo_path = os.path.abspath(args.repo)

    print(f"[analyze] Scanning {repo_path}/memes/ ...")
    inventory = get_category_inventory(repo_path)

    # Detect merges
    hardcoded = detect_hardcoded_merges(inventory)
    similarity = detect_similarity_merges(inventory, args.similarity_cutoff)

    # Deduplicate: hardcoded takes precedence
    hardcoded_sources = {m["source"] for m in hardcoded}
    similarity = [m for m in similarity if m["source"] not in hardcoded_sources]

    all_merges = hardcoded + similarity

    # Detect threshold violators
    violators = detect_threshold_violators(inventory, args.threshold)

    # Generate commands
    all_changes = all_merges + [{"source": v["category"], "target": v["suggested_target"],
                                  "source_count": v["count"], "target_count": 0, "type": "threshold"}
                                 for v in violators if v["action"] == "migrate"]
    commands = generate_mv_commands(all_changes, inventory, repo_path)

    # Export JSON if requested
    if args.json_out:
        out_dir = Path(args.json_out)
        out_dir.mkdir(parents=True, exist_ok=True)
        (out_dir / "inventory.json").write_text(json.dumps(inventory, indent=2), encoding="utf-8")
        (out_dir / "merges.json").write_text(json.dumps(all_merges, indent=2), encoding="utf-8")
        (out_dir / "violators.json").write_text(json.dumps(violators, indent=2), encoding="utf-8")
        print(f"[analyze] Reports written to {args.json_out}/")

    # Mode dispatch
    if args.mode == "suggest":
        print_suggestion_report(all_merges, violators, inventory, commands)

    elif args.mode == "interactive":
        approved = run_interactive_mode(all_merges, violators, inventory, commands, repo_path)
        if approved:
            approved_cmds = generate_mv_commands(approved, inventory, repo_path)
            print(f"\nAplicando {len(approved)} merges aprobados...")
            stats = apply_commands(approved_cmds, repo_path, dry_run=False)
            print(f"  Movidos: {stats['moved']} | Actualizados: {stats['updated']} | Errores: {stats['errors']}")

    elif args.mode == "autonomous":
        print_suggestion_report(all_merges, violators, inventory, commands)

        # Safety check
        affected_memes = sum(1 for c in commands if c["type"] == "move_mdx")
        if affected_memes > 50:
            print(f"\n  ⚠️  {affected_memes} memes serán afectados.")
            resp = input("  ¿Continuar? [y/N]: ").strip().lower()
            if resp not in ("y", "yes"):
                print("  Cancelado.")
                return 0

        if args.git:
            branch = f"classify/category-cleanup-{datetime.now().strftime('%Y%m%d')}"
            git_create_branch(repo_path, branch)

        print("\n[analyze] Aplicando cambios...")
        stats = apply_commands(commands, repo_path, dry_run=False)
        print(f"  Movidos: {stats['moved']} | Eliminadas: {stats['deleted']} | Actualizados: {stats['updated']} | Errores: {stats['errors']}")

        if args.git:
            msg = f"""classify: standardize categories — merge duplicates, apply threshold

- Merge {len(all_merges)} duplicate/similar categories
- Migrate {len([v for v in violators if v['action'] == 'migrate'])} sub-threshold categories
- Delete {len([v for v in violators if v['action'] == 'delete'])} empty categories
- Update frontmatter in affected memes"""
            git_commit_and_push(repo_path, branch, msg)
            print(f"[git] Pushed to {branch}")

    return 0


if __name__ == "__main__":
    exit(main() or 0)
