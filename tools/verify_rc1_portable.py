#!/usr/bin/env python3
"""Portable integrity gate for the accepted develop/1.1 factual assets."""

from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import sys
import zipfile
from pathlib import Path
from typing import Any


EXPECTED_SCHEMA = 31
EXPECTED_PRODUCTION_SIZE = 9_605_968
EXPECTED_PRODUCTION_SHA256 = "4A11909F1D1A1C986038831DCB3C3244528F88AAC6F05F2D9A626DBB6E0CAA60"
EXPECTED_CHARM_SIZE = 1_699_211
EXPECTED_CHARM_SHA256 = "4DF9CCCE3B07C8F7423C7DF053C917A488F137F93F8B6BD576F79AAB019075D7"
EXPECTED_QUEST_SIZE = 16_702
EXPECTED_QUEST_SHA256 = "43939D9899AC1931510616777D7F869CE00C5DB818B9734460441BD607699D95"
DATA_RELATIVE_PATH = Path("data/generated/mhp3rd-data.json")
ANDROID_DATA_RELATIVE_PATH = Path("app/src/main/assets/mhp3rd-data.json")
CHARM_RELATIVE_PATH = Path("data/generated/mhp3rd-charms.json")
ANDROID_CHARM_RELATIVE_PATH = Path("app/src/main/assets/mhp3rd-charms.json")
QUEST_RELATIVE_PATH = Path("data/generated/mhp3rd-quest-progression.json")
ANDROID_QUEST_RELATIVE_PATH = Path("app/src/main/assets/mhp3rd-quest-progression.json")
HEADER_RELATIVE_PATH = Path("app/src/main/assets/monster-headers")
APK_DATA_PATH = "assets/mhp3rd-data.json"
APK_CHARM_PATH = "assets/mhp3rd-charms.json"
APK_QUEST_PATH = "assets/mhp3rd-quest-progression.json"
APK_HEADER_PREFIX = "assets/monster-headers/"

EXPECTED_CHARM_CLASSES = ("Queen", "King", "Dragon")
EXPECTED_CHARM_CLASS_COUNTS = {"Queen": 25_227, "King": 28_864, "Dragon": 10_709}


def fail(message: str) -> None:
    raise ValueError(message)


def walk_objects(value: Any):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk_objects(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_objects(child)


def unique_values(rows: list[dict], key: str, label: str) -> set[Any]:
    values = [row.get(key) for row in rows]
    if any(value is None for value in values):
        fail(f"{label}: missing {key}")
    result = set(values)
    if len(result) != len(values):
        fail(f"{label}: duplicate {key}")
    return result


def check_references(data: dict) -> None:
    item_ids = unique_values(data.get("items", []), "gameItemId", "items")
    stable_item_ids = unique_values(data.get("items", []), "id", "items")
    monster_ids = unique_values(data.get("monsters", []), "id", "monsters")
    monster_ids.update(unique_values(data.get("smallMonsters", []), "id", "smallMonsters"))
    quest_ids = unique_values(data.get("quests", []), "id", "quests")
    training_quest_ids = unique_values(data.get("trainingQuests", []), "id", "trainingQuests")
    all_quest_ids = quest_ids | training_quest_ids
    breakable_part_ids = unique_values(data.get("breakableParts", []), "id", "breakableParts")
    weapon_ids = unique_values(data.get("weapons", []), "stableWeaponId", "weapons")
    skill_tree_ids = unique_values(data.get("skillTrees", []), "stableSkillTreeId", "skillTrees")

    reference_sets = {
        "itemId": stable_item_ids,
        "monsterId": monster_ids,
        "questId": all_quest_ids,
        "stableWeaponId": weapon_ids,
        "stableSkillTreeId": skill_tree_ids,
    }
    for row in walk_objects(data):
        for key, known in reference_sets.items():
            reference = row.get(key)
            if reference is not None and reference not in known:
                fail(f"unresolved {key} reference: {reference}")
        for key, known in (
            ("questIds", all_quest_ids),
            ("trainingQuestIds", training_quest_ids),
            ("appearingMonsterIds", monster_ids),
            ("breakablePartIds", breakable_part_ids),
        ):
            references = row.get(key)
            if references is None:
                continue
            if not isinstance(references, list):
                fail(f"{key} must be a list")
            for reference in references:
                if reference not in known:
                    fail(f"unresolved {key} reference: {reference}")
        for key, reference in row.items():
            if key.endswith("GameItemId") and reference is not None and reference not in item_ids:
                fail(f"unresolved {key} reference: {reference}")


def parse_json_asset(raw: bytes, source: str, expected_size: int, expected_sha256: str) -> dict:
    if len(raw) != expected_size:
        fail(f"{source}: expected {expected_size} bytes, got {len(raw)}")
    actual_hash = hashlib.sha256(raw).hexdigest().upper()
    if actual_hash != expected_sha256:
        fail(f"{source}: SHA-256 mismatch: {actual_hash}")
    try:
        data = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail(f"{source}: invalid JSON: {exc}")
    if not isinstance(data, dict):
        fail(f"{source}: JSON root must be an object")
    print(f"PASS {source}: {len(raw)} bytes, SHA-256 {actual_hash}")
    return data


def parse_payload(raw: bytes, source: str) -> tuple[dict, set[str]]:
    data = parse_json_asset(raw, source, EXPECTED_PRODUCTION_SIZE, EXPECTED_PRODUCTION_SHA256)
    if data.get("schemaVersion") != EXPECTED_SCHEMA:
        fail(f"{source}: expected schemaVersion {EXPECTED_SCHEMA}")
    monsters = data.get("monsters")
    if not isinstance(monsters, list) or len(monsters) != 40:
        fail(f"{source}: expected exactly 40 large monsters")
    monster_ids = unique_values(monsters, "id", "monsters")
    skill_trees = data.get("skillTrees")
    if not isinstance(skill_trees, list) or len(skill_trees) != 100:
        fail(f"{source}: expected exactly 100 canonical skill trees")
    unique_values(skill_trees, "stableSkillTreeId", "skillTrees")
    check_references(data)
    print(f"PASS {source}: schema {EXPECTED_SCHEMA}, 100 canonical skill trees")
    return data, monster_ids


def verify_charm_payload(raw: bytes, source: str) -> dict:
    asset = parse_json_asset(raw, source, EXPECTED_CHARM_SIZE, EXPECTED_CHARM_SHA256)
    if asset.get("schemaVersion") != 1:
        fail(f"{source}: expected schemaVersion 1")
    metadata = asset.get("source")
    if not isinstance(metadata, dict):
        fail(f"{source}: missing source metadata")
    expected_tables = list(range(1, 13))
    if metadata.get("tables") != expected_tables:
        fail(f"{source}: source tables must be exactly 1..12")
    if metadata.get("rowsPerTable") != 5_400 or metadata.get("sourceRows") != 64_800:
        fail(f"{source}: expected 5,400 rows/table and 64,800 source rows")

    classes = asset.get("classes")
    if classes != list(EXPECTED_CHARM_CLASSES):
        fail(f"{source}: expected Timeworn classes {list(EXPECTED_CHARM_CLASSES)}; Common is not accepted")
    rows = asset.get("rows")
    if not isinstance(rows, list) or len(rows) != 64_800:
        fail(f"{source}: expected exactly 64,800 source states")

    table_counts: Counter[int] = Counter()
    class_counts: Counter[str] = Counter()
    for index, row in enumerate(rows):
        if not isinstance(row, list) or len(row) != 9:
            fail(f"{source}: row {index} must contain nine packed fields")
        table = row[1]
        class_index = row[3]
        if not isinstance(table, int) or table not in expected_tables:
            fail(f"{source}: row {index} has an invalid table")
        if not isinstance(class_index, int) or not 0 <= class_index < len(classes):
            fail(f"{source}: row {index} has an invalid class index")
        table_counts[table] += 1
        class_counts[classes[class_index]] += 1

    if table_counts != Counter({table: 5_400 for table in expected_tables}):
        fail(f"{source}: expected exactly 5,400 source states in each table 1..12")
    if dict(class_counts) != EXPECTED_CHARM_CLASS_COUNTS:
        fail(f"{source}: unexpected rarity distribution: {dict(class_counts)}")
    if "Common" in class_counts:
        fail(f"{source}: Common rarity is not present in the accepted Timeworn corpus")
    print(f"PASS {source}: 64,800 states; 12 tables x 5,400; {dict(class_counts)}; no Common")
    return asset


def verify_quest_payload(raw: bytes, source: str, production: dict) -> dict:
    asset = parse_json_asset(raw, source, EXPECTED_QUEST_SIZE, EXPECTED_QUEST_SHA256)
    steps = asset.get("steps")
    if not isinstance(steps, list):
        fail(f"{source}: steps must be a list")
    key_ids = [quest_id for step in steps if isinstance(step, dict) for quest_id in step.get("keyQuestIds", [])]
    if len(key_ids) != 53 or len(set(key_ids)) != 53:
        fail(f"{source}: expected 53 unique Key quests")

    production_quest_ids = unique_values(production.get("quests", []), "id", "quests")
    if not set(key_ids) <= production_quest_ids:
        fail(f"{source}: a Key quest does not resolve to the production quest corpus")

    mapped_urgent_ids = [
        step["urgent"].get("companionQuestId")
        for step in steps
        if isinstance(step, dict) and isinstance(step.get("urgent"), dict)
    ]
    blocked_urgents = asset.get("blockedUrgents")
    if not isinstance(blocked_urgents, list) or len(mapped_urgent_ids) != 9 or len(blocked_urgents) != 4:
        fail(f"{source}: expected 9 mapped and 4 evidence-blocked progression Urgents")
    if any(not urgent_id for urgent_id in mapped_urgent_ids):
        fail(f"{source}: a mapped progression Urgent has no production quest ID")
    if not set(mapped_urgent_ids) <= production_quest_ids:
        fail(f"{source}: a mapped progression Urgent does not resolve to the production quest corpus")
    if any(entry.get("role") != "URGENT" or entry.get("companionQuestId") is not None for entry in blocked_urgents):
        fail(f"{source}: blocked progression Urgents must remain explicitly unresolved")
    optional_ids = {entry.get("questId") for entry in asset.get("optionalChallenges", []) if isinstance(entry, dict)}
    if optional_ids & (set(key_ids) | set(mapped_urgent_ids)):
        fail(f"{source}: optional challenges must not be counted as progression keys or Urgents")

    prerequisites = asset.get("unlockPrerequisites")
    if not isinstance(prerequisites, list) or len(prerequisites) != 7:
        fail(f"{source}: expected exactly seven prerequisite relations")
    for relation in prerequisites:
        if not isinstance(relation, dict) or relation.get("questId") not in production_quest_ids or relation.get("prerequisiteQuestId") not in production_quest_ids:
            fail(f"{source}: prerequisite relation references an unknown production quest")

    print(f"PASS {source}: 53 Key quests, 13 progression Urgents, 7 prerequisite relations")
    return asset


def check_header_entries(names: set[str], monster_ids: set[str], prefix: str, source: str) -> None:
    actual = {name[len(prefix):] for name in names if name.startswith(prefix)}
    expected = {f"{monster_id}.webp" for monster_id in monster_ids}
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        fail(f"{source}: header coverage mismatch; missing={missing}, extra={extra}")


def check_webp(name: str, raw: bytes) -> None:
    if len(raw) < 16 or raw[:4] != b"RIFF" or raw[8:12] != b"WEBP":
        fail(f"{name}: not a WebP image")


def verify_root(root: Path) -> tuple[dict, set[str], bytes]:
    root = root.resolve()
    canonical_path = root / DATA_RELATIVE_PATH
    android_path = root / ANDROID_DATA_RELATIVE_PATH
    header_dir = root / HEADER_RELATIVE_PATH
    canonical = canonical_path.read_bytes()
    android_asset = android_path.read_bytes()
    if canonical != android_asset:
        fail("canonical JSON and Android asset differ byte-for-byte")
    data, monster_ids = parse_payload(canonical, "canonical data")
    charm_path = root / CHARM_RELATIVE_PATH
    android_charm_path = root / ANDROID_CHARM_RELATIVE_PATH
    charm = charm_path.read_bytes()
    if charm != android_charm_path.read_bytes():
        fail("canonical Charm JSON and Android asset differ byte-for-byte")
    verify_charm_payload(charm, "canonical Charm data")
    print("PASS Android Charm asset: byte-identical")

    quest_path = root / QUEST_RELATIVE_PATH
    android_quest_path = root / ANDROID_QUEST_RELATIVE_PATH
    quest = quest_path.read_bytes()
    if quest != android_quest_path.read_bytes():
        fail("canonical Quest Progression JSON and Android asset differ byte-for-byte")
    verify_quest_payload(quest, "canonical Quest Progression data", data)
    print("PASS Android Quest Progression asset: byte-identical")

    headers = {
        path.name: path.read_bytes()
        for path in header_dir.glob("*")
        if path.is_file() and not path.name.startswith(".")
    }
    check_header_entries(set(headers), monster_ids, "", "source headers")
    for name, raw in sorted(headers.items()):
        check_webp(name, raw)
    print(f"PASS Android asset: byte-identical; {len(headers)} complete WebP headers")
    return data, monster_ids, canonical


def verify_apk(apk_path: Path, expected_assets: dict[str, bytes] | None = None) -> None:
    with zipfile.ZipFile(apk_path) as apk:
        names = set(apk.namelist())
        production_raw = apk.read(APK_DATA_PATH)
        production, monster_ids = parse_payload(production_raw, "APK production data")
        charm_raw = apk.read(APK_CHARM_PATH)
        verify_charm_payload(charm_raw, "APK Charm data")
        quest_raw = apk.read(APK_QUEST_PATH)
        verify_quest_payload(quest_raw, "APK Quest Progression data", production)
        apk_assets = {
            APK_DATA_PATH: production_raw,
            APK_CHARM_PATH: charm_raw,
            APK_QUEST_PATH: quest_raw,
        }
        if expected_assets is not None:
            for apk_name, raw in apk_assets.items():
                if raw != expected_assets[apk_name]:
                    fail(f"APK {apk_name} differs byte-for-byte from canonical JSON")
        header_names = {name for name in names if name.startswith(APK_HEADER_PREFIX)}
        check_header_entries(header_names, monster_ids, APK_HEADER_PREFIX, "APK headers")
        for name in sorted(header_names):
            check_webp(name, apk.read(name))
    print(f"PASS APK: embedded JSON and {len(monster_ids)} monster headers verified")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, help="clean source tree to verify")
    parser.add_argument("--apk", type=Path, help="APK to inspect independently")
    args = parser.parse_args()
    root = args.root
    if root is None and args.apk is None:
        root = Path(__file__).resolve().parents[1]
    canonical: bytes | None = None
    expected_assets: dict[str, bytes] | None = None
    if root is not None:
        _data, _monster_ids, canonical = verify_root(root)
        expected_assets = {
            APK_DATA_PATH: canonical,
            APK_CHARM_PATH: (root.resolve() / CHARM_RELATIVE_PATH).read_bytes(),
            APK_QUEST_PATH: (root.resolve() / QUEST_RELATIVE_PATH).read_bytes(),
        }
    if args.apk is not None:
        verify_apk(args.apk, expected_assets)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, zipfile.BadZipFile, KeyError) as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
