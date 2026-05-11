#!/usr/bin/env python3
"""
classify_meme.py - Classify a single meme into the correct category.

Analyzes a meme's content (title, description, tags) and the existing
category taxonomy to recommend the best primary category with confidence score.

Usage:
    # Classify a meme file
    python classify_meme.py --source memes/nuevo/meme.mdx --repo ~/openmeme

    # With detailed reasoning
    python classify_meme.py --source meme.mdx --repo ~/openmeme --verbose

    # Auto-apply the recommended category
    python classify_meme.py --source meme.mdx --repo ~/openmeme --apply

    # Batch classify from JSON
    python classify_meme.py --batch batch.json --repo ~/openmeme
"""

import os
import re
import json
import argparse
from pathlib import Path


# Franchise keywords mapped to canonical categories
FRANCHISE_KEYWORDS = {
    "simpsons": ["simpsons", "homer", "bart", "marge", "lisa", "maggie", "burns", "moe", "krusty", "skinner", "nelson", "flanders", "apu", "barney", "milhouse", "chief wiggum", "lenny", "carl", "otto"],
    "star-wars": ["star wars", "darth vader", "luke skywalker", "yoda", "obi-wan", "han solo", "chewbacca", "stormtrooper", "jedi", "sith", "force", "lightsaber", "death star"],
    "spongebob": ["spongebob", "patrick", "squidward", "krabs", "bikini bottom", "gary", "sandy", "plankton", "pineapple"],
    "dragon-ball": ["dragon ball", "goku", "vegeta", "piccolo", "gohan", "trunks", "bulma", "frieza", "cell", "buu", "kamehameha", "saiyan", "z fighter"],
    "harry-potter": ["harry potter", "hogwarts", "voldemort", "dumbledore", "hermione", "ron weasley", "snape", "hagrid", "gryffindor", "slytherin"],
    "jojo-bizarre-adventure": ["jojo", "jotaro", "dio", "stand", "za warudo", "ora ora", "muda muda", "bizarre adventure", "joseph joestar"],
    "one-piece": ["one piece", "luffy", "zoro", "nami", "sanji", "chopper", "robin", "franky", "brook", "jinbe", "devil fruit", "going merry"],
    "narcos": ["narcos", "pablo escobar", "cali cartel", "medellin", "plata o plomo"],
    "pepe": ["pepe the frog", "feels bad man", "feels good man", "rare pepe", "apu apustaja", "helper"],
    "wojak": ["wojak", "doomer", "bloomer", "zoomer", "boomer", " feels ", "nojak", "brainlet", "npc"],
    "breaking-bad": ["breaking bad", "walter white", "heisenberg", "jesse pinkman", "saul goodman", "gus fring", "blue meth"],
    "batman": ["batman", "bruce wayne", "joker", "gotham", "dark knight", "robin", "alfred"],
    "lord-of-the-rings": ["lord of the rings", "lotr", "frodo", "gandalf", "aragorn", "legolas", "gollum", "sauron", "mordor", "ring"],
    "fullmetal-alchemist": ["fullmetal alchemist", "edward elric", "alphonse", "roy mustang", "equivalent exchange", "transmutation"],
    "evangelion": ["evangelion", "shinji", "rei", "asuka", "misato", "eva unit", "nerv", "angel", "third impact"],
    "saint-seiya": ["saint seiya", "pegasus", "dragon shiryu", "cygnus", "andromeda", "phoenix", "athena", "cosmo"],
    "yu-gi-oh": ["yu-gi-oh", "yugi", "yami yugi", "kaiba", "joey", "blue eyes", "dark magician", "duel monsters"],
    "yu-gi-oh": ["yugioh"],
    "pokemon": ["pokemon", "pikachu", "ash", "charmander", "bulbasaur", "squirtle", "pokeball", "pokedex"],
    "attack-on-titan": ["attack on titan", "shingeki no kyojin", "eren", "mikasa", "armin", "levi", "titan", "survey corps"],
    "the-boys": ["the boys", "homelander", "butcher", "hughie", "starlight", "a-train", "supes"],
    "invincible": ["invincible", "mark grayson", "omni-man", "atom eve", "viltrumite"],
    "south-park": ["south park", "cartman", "stan", "kyle", "kenny", "butters", "chef"],
    "family-guy": ["family guy", "peter griffin", "stewie", "brian", "lois", "meg", "chris", "quagmire"],
    "futurama": ["futurama", "fry", "leela", "bender", "professor farnsworth", "zoidberg"],
    "rick-and-morty": ["rick and morty", "rick sanchez", "morty", "pickle rick", "wubba lubba"],
    "minecraft": ["minecraft", "creeper", "steve", "enderman", "diamond", "block", "redstone", "craf"],
    "gta": ["gta", "grand theft auto", "cj", "big smoke", "follow the damn train", "san andreas", "vice city"],
    "formula-1": ["formula 1", "f1", "lewis hamilton", "max verstappen", "ferrari", "red bull", "monaco"],
    "warcraft": ["warcraft", "arthas", "lich king", "thrall", "sylvanas", "azeroth", "horde", "alliance"],
    "metal-gear-solid": ["metal gear solid", "solid snake", "big boss", "revolver ocelot", "nanomachines"],
    "skyrim": ["skyrim", "dragonborn", "fus ro dah", "todd howard", "sweetroll", "arrow to the knee"],
}

# Format keywords
FORMAT_KEYWORDS = {
    "reaction": ["reaction", "reacting", "response", "responds", "face when", "mfw", "tfw", "me when", "me after"],
    "template": ["template", "format", "blank", "exploitable", "customizable"],
    "shitpost": ["shitpost", "low effort", "ironic", "shit posting"],
    "comparison": ["comparison", "vs", "versus", "better than", "worse than"],
    "cursed": ["cursed", "blursed", "unsettling", "creepy"],
    "surreal": ["surreal", "absurdist", "weird", "strange", "bizarre"],
    "wholesome": ["wholesome", "heartwarming", "feel good", "made me smile"],
    "meta-meme": ["meta", "self-referential", "meme about memes"],
}

# Region keywords
REGION_KEYWORDS = {
    "argentina": ["argentina", "argentino", "argentinian", "buenos aires", "mate", "asado", "che", "boludo", " AFIP", "carpincho"],
    "mexico": ["mexico", "mexicano", "mexican", "chinga", "pendejo", "wey"],
    "spain": ["spain", "españa", "español", "spanish", "barcelona", "madrid"],
    "brazil": ["brazil", "brasil", "brazilian", "br", "hue", "carnaval"],
    "latam": ["latam", "latin america", "latinoamerica", "latino"],
}


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


def detect_franchise(text: str) -> tuple:
    """Detect franchise from text. Returns (franchise, confidence)."""
    text_lower = text.lower()
    scores = {}

    for franchise, keywords in FRANCHISE_KEYWORDS.items():
        for kw in keywords:
            if kw in text_lower:
                scores[franchise] = scores.get(franchise, 0) + 1

    if not scores:
        return None, 0

    best = max(scores, key=scores.get)
    confidence = min(100, scores[best] * 35)
    return best, confidence


def detect_format(text: str) -> tuple:
    """Detect format type from text."""
    text_lower = text.lower()
    scores = {}

    for fmt, keywords in FORMAT_KEYWORDS.items():
        for kw in keywords:
            if kw in text_lower:
                scores[fmt] = scores.get(fmt, 0) + 1

    if not scores:
        return None, 0

    best = max(scores, key=scores.get)
    confidence = min(80, scores[best] * 30)
    return best, confidence


def detect_region(text: str) -> tuple:
    """Detect region from text."""
    text_lower = text.lower()
    scores = {}

    for region, keywords in REGION_KEYWORDS.items():
        for kw in keywords:
            if kw in text_lower:
                scores[region] = scores.get(region, 0) + 1

    if not scores:
        return None, 0

    best = max(scores, key=scores.get)
    return best, min(90, scores[best] * 40)


def get_existing_categories(repo_path: str) -> set:
    """Get set of existing category names."""
    memes_dir = Path(repo_path) / "memes"
    if not memes_dir.exists():
        return set()
    return {d.name for d in memes_dir.iterdir() if d.is_dir()}


def classify_meme(mdx_path: str, repo_path: str) -> dict:
    """Classify a meme and return recommendation."""
    path = Path(mdx_path)
    if not path.exists():
        return {"error": f"File not found: {mdx_path}"}

    content = path.read_text(encoding="utf-8")
    data = parse_frontmatter(content)
    current_category = data.get("category", "")

    # Combine searchable text
    searchable = " ".join([
        data.get("title", ""),
        data.get("description", ""),
        " ".join(data.get("tags", [])),
    ])

    existing = get_existing_categories(repo_path)

    # Detect signals
    franchise, franchise_conf = detect_franchise(searchable)
    fmt, fmt_conf = detect_format(searchable)
    region, region_conf = detect_region(searchable)

    # Build recommendation
    reasoning = []
    confidence = 0
    primary = None
    secondary_tags = []

    # Rule 1: Franchise match → always primary
    if franchise and franchise in existing:
        primary = franchise
        confidence = franchise_conf
        reasoning.append(f"Franchise detected: '{franchise}' ({franchise_conf}% confidence)")

        # Region secondary
        if region and region != franchise:
            secondary_tags.append(region)
            reasoning.append(f"Region tag: '{region}'")

        # Format secondary
        if fmt:
            secondary_tags.append(fmt)
            reasoning.append(f"Format tag: '{fmt}'")

    # Rule 2: No franchise, but format match
    elif fmt and fmt in existing:
        primary = fmt
        confidence = fmt_conf
        reasoning.append(f"Format category: '{fmt}' ({fmt_conf}% confidence)")

        if region:
            secondary_tags.append(region)
            reasoning.append(f"Region tag: '{region}'")

    # Rule 3: Region-specific, no franchise/format
    elif region and f"{region}-humor" in existing:
        primary = f"{region}-humor"
        confidence = region_conf
        reasoning.append(f"Region humor: '{primary}' ({region_conf}% confidence)")

    # Rule 4: Default to existing category if no better match
    elif current_category in existing:
        primary = current_category
        confidence = 30
        reasoning.append(f"Keeping existing category '{current_category}' (low confidence, no better match)")

    # Rule 5: No match at all
    else:
        primary = "others"
        confidence = 20
        reasoning.append("No clear match found. Suggest 'others' or review for new category creation.")

    # Determine if current category should change
    should_change = primary and primary != current_category

    return {
        "slug": data.get("slug", path.stem),
        "current_category": current_category,
        "recommended_category": primary,
        "confidence": confidence,
        "should_change": should_change,
        "secondary_tags": secondary_tags,
        "reasoning": reasoning,
        "signals": {
            "franchise": franchise,
            "format": fmt,
            "region": region,
        },
    }


def apply_classification(mdx_path: str, recommendation: dict):
    """Update MDX file with recommended category and tags."""
    path = Path(mdx_path)
    content = path.read_text(encoding="utf-8")

    old_cat = recommendation["current_category"]
    new_cat = recommendation["recommended_category"]

    # Update category
    content = re.sub(
        rf'^category:\s*"{re.escape(old_cat)}"',
        f'category: "{new_cat}"',
        content,
        flags=re.MULTILINE,
    )

    # Add secondary tags
    if recommendation["secondary_tags"]:
        tags_match = re.search(r'^tags:\s*\[(.*?)\]', content, re.MULTILINE)
        if tags_match:
            existing = [t.strip().strip('"').strip("'") for t in tags_match.group(1).split(",") if t.strip()]
            new_tags = existing[:]
            for t in recommendation["secondary_tags"]:
                if t not in new_tags:
                    new_tags.append(t)
            if len(new_tags) > len(existing):
                tags_str = ", ".join(f'"{t}"' for t in new_tags)
                content = content[:tags_match.start()] + f"tags: [{tags_str}]" + content[tags_match.end():]

    path.write_text(content, encoding="utf-8")
    print(f"  ✓ Applied: {old_cat} → {new_cat}")


def main():
    parser = argparse.ArgumentParser(description="Classify memes into categories")
    parser.add_argument("--source", help="MDX file to classify")
    parser.add_argument("--batch", help="JSON file with multiple memes")
    parser.add_argument("--repo", default=".", help="Path to OpenMeme repository")
    parser.add_argument("--apply", action="store_true", help="Apply the recommended category")
    parser.add_argument("--verbose", action="store_true", help="Show detailed reasoning")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    args = parser.parse_args()

    results = []

    if args.source:
        result = classify_meme(args.source, args.repo)
        results.append(result)

        if args.apply and "error" not in result and result.get("should_change"):
            apply_classification(args.source, result)

    elif args.batch:
        with open(args.batch, "r", encoding="utf-8") as f:
            items = json.load(f)
        for item in items:
            result = classify_meme(item.get("path", ""), args.repo)
            results.append(result)
            if args.apply and "error" not in result and result.get("should_change"):
                apply_classification(item["path"], result)
    else:
        print("Error: Provide --source or --batch")
        return 1

    # Output
    for result in results:
        if "error" in result:
            print(f"Error: {result['error']}")
            continue

        if args.json:
            print(json.dumps(result, indent=2, ensure_ascii=False))
            continue

        slug = result["slug"]
        current = result["current_category"]
        recommended = result["recommended_category"]
        conf = result["confidence"]

        # Confidence indicator
        if conf >= 80:
            indicator = "✅"
        elif conf >= 60:
            indicator = "⚠️"
        else:
            indicator = "❓"

        print(f"\n{indicator} {slug}")
        print(f"   Current:    {current}")
        print(f"   Recommend:  {recommended}")
        print(f"   Confidence: {conf}%")

        if result["secondary_tags"]:
            print(f"   Tags to add: {', '.join(result['secondary_tags'])}")

        if args.verbose:
            print(f"   Reasoning:")
            for r in result["reasoning"]:
                print(f"     • {r}")

    return 0


if __name__ == "__main__":
    exit(main() or 0)
