#!/usr/bin/env python3
"""Portable RC1 gate for the frozen schema-31 production payload and headers."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
import zipfile
from pathlib import Path
from typing import Any


EXPECTED_SCHEMA = 31
EXPECTED_SHA256 = "657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155"
DATA_RELATIVE_PATH = Path("data/generated/mhp3rd-data.json")
ANDROID_DATA_RELATIVE_PATH = Path("app/src/main/assets/mhp3rd-data.json")
HEADER_RELATIVE_PATH = Path("app/src/main/assets/monster-headers")
APK_DATA_PATH = "assets/mhp3rd-data.json"
APK_HEADER_PREFIX = "assets/monster-headers/"


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


def parse_payload(raw: bytes, source: str) -> tuple[dict, set[str]]:
    actual_hash = hashlib.sha256(raw).hexdigest().upper()
    if actual_hash != EXPECTED_SHA256:
        fail(f"{source}: SHA-256 mismatch: {actual_hash}")
    try:
        data = json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        fail(f"{source}: invalid JSON: {exc}")
    if not isinstance(data, dict) or data.get("schemaVersion") != EXPECTED_SCHEMA:
        fail(f"{source}: expected schemaVersion {EXPECTED_SCHEMA}")
    monsters = data.get("monsters")
    if not isinstance(monsters, list) or len(monsters) != 40:
        fail(f"{source}: expected exactly 40 large monsters")
    monster_ids = unique_values(monsters, "id", "monsters")
    check_references(data)
    print(f"PASS {source}: schema {EXPECTED_SCHEMA}, {len(monster_ids)} monsters, SHA-256 pinned")
    return data, monster_ids


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


def verify_apk(apk_path: Path, expected_data: bytes | None = None) -> None:
    with zipfile.ZipFile(apk_path) as apk:
        names = set(apk.namelist())
        raw = apk.read(APK_DATA_PATH)
        _data, monster_ids = parse_payload(raw, "APK data")
        if expected_data is not None and raw != expected_data:
            fail("APK data differs byte-for-byte from canonical JSON")
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
    if root is not None:
        _data, _monster_ids, canonical = verify_root(root)
    if args.apk is not None:
        verify_apk(args.apk, canonical)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, zipfile.BadZipFile, KeyError) as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
