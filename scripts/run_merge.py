#!/usr/bin/env python3
"""Execute Argentina category merges as approved in interactive session."""

import os
import re
import shutil
from pathlib import Path

MEMES_DIR = Path("/home/user/openmeme/memes")

# Merge map: source_category -> (dest_category, extra_tags_to_add)
MERGES = {
    # Group 1: Politics
    "argentina-politica": ("argentina-politics", []),

    # Group 2: Football -> argentina-futbol
    "argentina-football": ("argentina-futbol", []),
    "futbol-shitpost": ("argentina-futbol", ["shitpost"]),
    "argentina-football-shitpost": ("argentina-futbol", ["shitpost"]),
    "argentina-football-fandom": ("argentina-futbol", []),
    "argentina-football-parody": ("argentina-futbol", ["parody"]),
    "argentina-football-cat": ("argentina-futbol", ["cat"]),
    "futbol-reaction": ("argentina-futbol", ["reaction"]),
    "football-reaction": ("argentina-futbol", ["reaction"]),
    "football-humor": ("argentina-futbol", []),
    "cursed-football": ("argentina-futbol", ["cursed"]),
    "argentina-deporte": ("argentina-futbol", []),
    "uruguay-futbol": ("argentina-futbol", ["uruguay"]),

    # Group 3: TV -> argentina-tv
    "argentina-tv-reaction": ("argentina-tv", ["reaction"]),
    "argentina-farandula": ("argentina-tv", ["farandula"]),
    "argentina-cronica-tv": ("argentina-tv", ["cronica"]),
    "argentina-tinelli-terminator": ("argentina-tv", ["tinelli", "terminator"]),
    "spiderman-argentina-tv": ("argentina-tv", ["spiderman"]),
    "casados-con-hijos-argentina": ("argentina-tv", ["casados-con-hijos"]),
    "argentina-comedy-film": ("argentina-tv", []),

    # Group 4: Franchises
    "dragon-ball-argentina-politics": ("argentina-politics", ["dragon-ball"]),
    "wojak-argentina-politics": ("argentina-politics", ["wojak"]),
    "planet-of-the-apes-argentina": ("argentina-politics", ["planet-of-the-apes"]),
    "anime-argentina": ("argentina-politics", ["anime"]),
    "saint-seiya-argentina": ("saint-seiya", ["argentina"]),
    "one-piece-argentina": ("one-piece", ["argentina"]),
    "wojak-argentina-police": ("wojak", ["argentina", "police"]),

    # Group 5a: -> argentina (generic new category)
    "argentina-orgullo": ("argentina", ["orgullo"]),
    "nostalgia-argentina": ("argentina", ["nostalgia"]),
    "argentina-clima": ("argentina", ["clima"]),
    "argentina-malvinas": ("argentina", ["malvinas"]),
    "argentina-superhero-satire": ("argentina", ["superhero", "satire"]),
    "argentina-rauf-superhero": ("argentina", ["rauf", "superhero"]),
    "argentina-naming-joke": ("argentina", ["naming"]),
    "argentina-graffiti-humor": ("argentina", ["graffiti"]),
    "argentina-lookalike": ("argentina", ["lookalike"]),
    "argentina-mourning": ("argentina", ["mourning"]),
    "argentina-crying-cat": ("argentina", ["crying-cat"]),
    "argentina-carpincho": ("argentina", ["carpincho"]),
    "argentina-colectivero": ("argentina", ["colectivero"]),
    "argentina-joker": ("argentina", ["joker"]),

    # Group 5b: AFIP -> argentina-politics
    "kilroy-argentina-taxes": ("argentina-politics", ["afip", "taxes"]),
    "argentina-afip": ("argentina-politics", ["afip"]),
}


def update_frontmatter(file_path: Path, new_category: str, extra_tags: list[str]) -> None:
    content = file_path.read_text(encoding="utf-8")

    # Update category field
    content = re.sub(
        r'^(category:\s*)["\']?[^"\'}\n]+["\']?',
        f'\\g<1>"{new_category}"',
        content,
        flags=re.MULTILINE,
    )

    # Add extra tags if not already present
    if extra_tags:
        def add_tags(m: re.Match) -> str:
            tags_str = m.group(0)
            for tag in extra_tags:
                if f'"{tag}"' not in tags_str and f"'{tag}'" not in tags_str:
                    # Insert before closing bracket
                    tags_str = re.sub(r'(\])', f', "{tag}"\\1', tags_str, count=1)
            return tags_str

        content = re.sub(r'tags:\s*\[[^\]]*\]', add_tags, content, flags=re.DOTALL)

    file_path.write_text(content, encoding="utf-8")


def merge_category(src: str, dest: str, extra_tags: list[str]) -> int:
    src_dir = MEMES_DIR / src
    dest_dir = MEMES_DIR / dest

    if not src_dir.exists():
        print(f"  SKIP (not found): {src}")
        return 0

    dest_dir.mkdir(parents=True, exist_ok=True)

    moved = 0
    for file in src_dir.iterdir():
        dest_file = dest_dir / file.name
        if dest_file.exists():
            # Avoid slug collision: append -2
            stem = file.stem
            suffix = file.suffix
            dest_file = dest_dir / f"{stem}-2{suffix}"
            print(f"  COLLISION: {file.name} → {dest_file.name}")

        shutil.move(str(file), str(dest_file))

        if dest_file.suffix == ".mdx":
            update_frontmatter(dest_file, dest, extra_tags)

        moved += 1

    src_dir.rmdir()
    print(f"  MERGED: {src} → {dest} ({moved} files)")
    return moved


def main() -> None:
    # Create new categories
    for new_cat in ["argentina", "one-piece"]:
        (MEMES_DIR / new_cat).mkdir(exist_ok=True)
        print(f"Created: {new_cat}/")

    total = 0
    for src, (dest, tags) in MERGES.items():
        total += merge_category(src, dest, tags)

    print(f"\nDone. {total} files moved across {len(MERGES)} category merges.")


if __name__ == "__main__":
    main()
