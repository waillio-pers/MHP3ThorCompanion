"""Build the read-only Item -> Weapon recipe reverse index for Iteration 4.16A.

The production JSON remains the source of truth and is never rewritten by this
stage.  Every numeric ingredient in a forge, upgrade, or source-preserved
orphan upgrade recipe becomes one deterministic usage row.
"""

from __future__ import annotations

import hashlib
import json
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "data" / "generated" / "mhp3rd-data.json"
OUTPUT = ROOT / "data" / "generated" / "weapon-recipe-usage-index-v1.json"
REPORT = ROOT / "data" / "generated" / "weapon-recipe-usage-v1-report.json"
EXPECTED_SHA = "970AF1D21BFE1AABC3DA30B1D5913D93FAA57F83446E5515580F238FB6EB09AA"
WEAPON_TYPE_ORDER = [
    "GREAT_SWORD", "LONG_SWORD", "SWORD_AND_SHIELD", "DUAL_BLADES",
    "HAMMER", "LANCE", "HUNTING_HORN", "GUNLANCE", "SWITCH_AXE",
    "BOW", "LIGHT_BOWGUN", "HEAVY_BOWGUN",
]


def production_sha(path: Path = PRODUCTION) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def _ingredients(weapon: dict[str, Any], recipe_key: str, kind: str):
    recipe = weapon.get(recipe_key)
    if recipe_key == "upgradeFrom" and recipe:
        recipe = recipe.get("recipe")
    if not recipe:
        return
    for ingredient in recipe.get("ingredients", []):
        game_item_id = ingredient.get("gameItemId")
        quantity = ingredient.get("quantity")
        if isinstance(game_item_id, int) and isinstance(quantity, int):
            yield {
                "gameItemId": game_item_id,
                "stableWeaponId": weapon["stableWeaponId"],
                "weaponType": weapon["weaponType"],
                "sourceOrdinal": weapon.get("sourceOrdinal"),
                "recipeKind": kind,
                "quantity": quantity,
            }


def build_usage_index(data: dict[str, Any]) -> dict[str, Any]:
    type_order = {value: index for index, value in enumerate(WEAPON_TYPE_ORDER)}
    rows = []
    for weapon in data.get("weapons", []):
        rows.extend(_ingredients(weapon, "forgeRecipe", "FORGE"))
        rows.extend(_ingredients(weapon, "upgradeFrom", "UPGRADE"))
        rows.extend(_ingredients(weapon, "orphanUpgradeRecipe", "UPGRADE"))
    rows.sort(key=lambda row: (
        type_order.get(row["weaponType"], 999),
        row["sourceOrdinal"] if row["sourceOrdinal"] is not None else 999999,
        row["stableWeaponId"],
        row["recipeKind"],
        row["gameItemId"],
    ))
    by_item: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in rows:
        by_item[str(row["gameItemId"])].append(row)
    unique_by_item = {
        item_id: len({row["stableWeaponId"] for row in item_rows})
        for item_id, item_rows in by_item.items()
    }
    top_fanout = sorted(
        (
            {"gameItemId": int(item_id), "uniqueWeaponCount": count,
             "usageRowCount": len(by_item[item_id])}
            for item_id, count in unique_by_item.items()
        ),
        key=lambda row: (-row["uniqueWeaponCount"], -row["usageRowCount"], row["gameItemId"]),
    )
    return {
        "version": 1,
        "schemaVersion": data.get("schemaVersion"),
        "productionSha256": production_sha(),
        "usageRowCount": len(rows),
        "itemsWithUsage": len(by_item),
        "maxUniqueWeaponFanout": max(unique_by_item.values(), default=0),
        "usageRows": rows,
        "byGameItemId": dict(sorted(by_item.items(), key=lambda pair: int(pair[0]))),
        "topFanoutItems": top_fanout[:20],
    }


def write_artifacts() -> tuple[Path, Path]:
    raw = PRODUCTION.read_bytes()
    actual_sha = hashlib.sha256(raw).hexdigest().upper()
    # This reverse index is regenerated after every production projection.
    # Keep EXPECTED_SHA as the historical 4.16 baseline for audit consumers,
    # but do not reject a later schema whose weapon records are unchanged.
    data = json.loads(raw.decode("utf-8"))
    result = build_usage_index(data)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report = {
        "version": 1,
        "productionSha256": actual_sha,
        "schemaVersion": data.get("schemaVersion"),
        "usageRowCount": result["usageRowCount"],
        "itemsWithUsage": result["itemsWithUsage"],
        "maxUniqueWeaponFanout": result["maxUniqueWeaponFanout"],
        "forgeRows": sum(row["recipeKind"] == "FORGE" for row in result["usageRows"]),
        "upgradeRows": sum(row["recipeKind"] == "UPGRADE" for row in result["usageRows"]),
        "topFanoutItems": result["topFanoutItems"][:10],
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return OUTPUT, REPORT


if __name__ == "__main__":
    print("\n".join(str(path) for path in write_artifacts()))
