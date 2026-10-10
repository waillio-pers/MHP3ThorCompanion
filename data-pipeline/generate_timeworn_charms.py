#!/usr/bin/env python3
"""Build the additive MHP3DB historical Timeworn Tables 1-12 asset.

The checked-in input is the explicitly filtered 1-12 extract. The optional
--prepare-sources operation is used only when refreshing that extract from
the pinned historical upstream CSV and requires the audit crosswalk.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "data" / "sources" / "mhp3db-timeworn"
SOURCE_CSV = SOURCE_DIR / "timeworn-charms-tables-1-12.csv"
SKILL_CROSSWALK = SOURCE_DIR / "skill-identity-crosswalk.csv"
PRODUCTION_JSON = ROOT / "data" / "generated" / "mhp3rd-data.json"
CANONICAL_OUTPUT = ROOT / "data" / "generated" / "mhp3rd-charms.json"
ANDROID_OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-charms.json"

SOURCE_COMMIT = "bfbab18460d95d921d4a3492644b46bfc5ca1f3c"
UPSTREAM_CSV_SHA256 = "470D807A054A1585FBB84F21E7CCCAC3A6996FBF74D86A6981A80B9FF14EEDEB"
EXPECTED_TABLES = tuple(range(1, 13))
ROWS_PER_TABLE = 5_400
EXPECTED_ROW_COUNT = len(EXPECTED_TABLES) * ROWS_PER_TABLE
CLASSES = ("Queen", "King", "Dragon")
CLASS_BY_SOURCE = {
    "Queen Talisman": 0,
    "King Talisman": 1,
    "Dragon Talisman": 2,
}
SKILL_PATTERN = re.compile(r"^(.*?)\s+([+-]\d+)$")


def sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest().upper()


def load_csv(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        if reader.fieldnames is None:
            raise ValueError(f"CSV has no header: {path}")
        return list(reader.fieldnames), list(reader)


def write_csv(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def prepare_sources(
    upstream_csv: Path,
    audit_crosswalk: Path,
    production_json: Path,
) -> tuple[str, str]:
    upstream_bytes = upstream_csv.read_bytes()
    if sha256(upstream_bytes) != UPSTREAM_CSV_SHA256:
        raise ValueError("Pinned historical MHP3DB CSV SHA-256 mismatch")

    fields, all_rows = load_csv(upstream_csv)
    required = {"ID", "Charm Type", "Charm Name", "Skill 1", "Skill 2", "Charm Slot", "Table No."}
    if set(fields) != required:
        raise ValueError(f"Unexpected upstream columns: {fields}")
    standard_rows = [
        row for row in all_rows
        if row["Charm Type"] == "Timeworn Charm" and row["Table No."].isdigit()
        and int(row["Table No."]) in EXPECTED_TABLES
    ]
    counts = Counter(int(row["Table No."]) for row in standard_rows)
    if len(standard_rows) != EXPECTED_ROW_COUNT or counts != Counter({table: ROWS_PER_TABLE for table in EXPECTED_TABLES}):
        raise ValueError(f"Upstream standard-table census mismatch: {counts}")

    production = json.loads(production_json.read_text(encoding="utf-8"))
    skills_by_id = {skill["stableSkillTreeId"]: skill for skill in production["skillTrees"]}
    audit_rows = {row["historicalSkill"]: row for row in load_csv(audit_crosswalk)[1]}
    if len(audit_rows) != 61:
        raise ValueError(f"Expected 61 audited historical skill labels, got {len(audit_rows)}")

    bridge_rows: list[dict[str, str]] = []
    for historical_label in sorted(audit_rows):
        audit = audit_rows[historical_label]
        stable_id = audit["companionSkillId"].strip()
        evidence = "EXACT_COMPANION_LABEL"
        if historical_label == "Thundr Atk":
            if audit["proposedCurrentLabel"] != "ThundrAtk" or audit["rowSupportEqualPoints"] != "2254":
                raise ValueError("Thundr Atk alignment evidence changed")
            if audit["oneToOne"] != "YES" or audit["competingMappings"] != "{}":
                raise ValueError("Thundr Atk positional mapping is not one-to-one")
            japanese_matches = [
                skill for skill in production["skillTrees"]
                if skill.get("jpName") == "雷属性攻撃" and skill.get("tmoDisplayName") == "ThunderAtk"
            ]
            if len(japanese_matches) != 1:
                raise ValueError("Japanese 雷属性攻撃 identity is not unique")
            stable_id = japanese_matches[0]["stableSkillTreeId"]
            evidence = "JP_NAME_雷属性攻撃_AND_TMO_ThunderAtk; 2254_POSITIONAL_ROWS"
        if not stable_id or stable_id not in skills_by_id:
            raise ValueError(f"Historical skill has no canonical skill-tree ID: {historical_label}")
        canonical = skills_by_id[stable_id]
        if historical_label != "Thundr Atk" and audit["companionIdentityStatus"] != "EXACT_COMPANION_LABEL":
            raise ValueError(f"Historical skill identity is not exact/adjudicated: {historical_label}")
        if historical_label != "Thundr Atk" and canonical["tmoDisplayName"] != audit["companionTmoDisplayName"]:
            raise ValueError(f"Audited canonical alias disagrees for {historical_label}")
        bridge_rows.append({
            "historicalSkill": historical_label,
            "stableSkillTreeId": stable_id,
            "canonicalTmoLabel": canonical["tmoDisplayName"],
            "identityEvidence": evidence,
        })

    if len({row["stableSkillTreeId"] for row in bridge_rows}) != 61:
        raise ValueError("Historical skill labels do not resolve one-to-one to canonical skill trees")

    write_csv(SOURCE_CSV, fields, standard_rows)
    write_csv(SKILL_CROSSWALK, ["historicalSkill", "stableSkillTreeId", "canonicalTmoLabel", "identityEvidence"], bridge_rows)
    return sha256(SOURCE_CSV.read_bytes()), sha256(SKILL_CROSSWALK.read_bytes())


def _load_skill_mapping(path: Path, production: dict[str, Any]) -> tuple[dict[str, str], list[str]]:
    rows = load_csv(path)[1]
    if len(rows) != 61:
        raise ValueError(f"Expected exactly 61 skill identity mappings, got {len(rows)}")
    skill_ids_in_production = {row["stableSkillTreeId"] for row in production["skillTrees"]}
    mapping: dict[str, str] = {}
    for row in rows:
        label = row["historicalSkill"].strip()
        stable_id = row["stableSkillTreeId"].strip()
        if not label or not stable_id or stable_id not in skill_ids_in_production:
            raise ValueError(f"Unresolved historical skill mapping: {row}")
        if label in mapping:
            raise ValueError(f"Duplicate historical skill label: {label}")
        mapping[label] = stable_id
    if len(set(mapping.values())) != 61:
        raise ValueError("Historical skill labels must resolve to distinct canonical IDs")
    return mapping, sorted(set(mapping.values()))


def _parse_skill(value: str, mapping: dict[str, str]) -> tuple[str, int]:
    match = SKILL_PATTERN.fullmatch(value.strip())
    if match is None:
        raise ValueError(f"Unexpected historical skill-point value: {value!r}")
    label, raw_points = match.groups()
    if label not in mapping:
        raise ValueError(f"Unresolved historical skill label: {label}")
    return mapping[label], int(raw_points)


def build_asset(source_csv: Path, crosswalk_csv: Path, production_json: Path) -> bytes:
    input_bytes = source_csv.read_bytes()
    if sha256(input_bytes) != EXPECTED_CURATED_SOURCE_SHA256:
        raise ValueError("Filtered historical Timeworn CSV SHA-256 mismatch")
    crosswalk_bytes = crosswalk_csv.read_bytes()
    if sha256(crosswalk_bytes) != EXPECTED_CROSSWALK_SHA256:
        raise ValueError("Canonical skill identity crosswalk SHA-256 mismatch")
    production_bytes = production_json.read_bytes()
    production = json.loads(production_bytes.decode("utf-8"))
    mapping, skill_ids = _load_skill_mapping(crosswalk_csv, production)
    skill_indices = {skill_id: index for index, skill_id in enumerate(skill_ids)}
    input_fields, source_rows = load_csv(source_csv)
    required = {"ID", "Charm Type", "Charm Name", "Skill 1", "Skill 2", "Charm Slot", "Table No."}
    if set(input_fields) != required:
        raise ValueError(f"Unexpected filtered source columns: {input_fields}")
    if len(source_rows) != EXPECTED_ROW_COUNT:
        raise ValueError(f"Expected {EXPECTED_ROW_COUNT} source rows; got {len(source_rows)}")

    table_counts: Counter[int] = Counter()
    source_ids: set[int] = set()
    packed_rows: list[list[int]] = []
    for row in source_rows:
        if row["Charm Type"] != "Timeworn Charm":
            raise ValueError("Non-Timeworn source row leaked into the compact corpus")
        table = int(row["Table No."])
        if table not in EXPECTED_TABLES:
            raise ValueError(f"Out-of-scope table leaked into source: {table}")
        source_id = int(row["ID"])
        if source_id in source_ids:
            raise ValueError(f"Duplicate historical source ID: {source_id}")
        source_ids.add(source_id)
        class_name = row["Charm Name"]
        if class_name not in CLASS_BY_SOURCE:
            raise ValueError(f"Unknown talisman class: {class_name}")
        slots = int(row["Charm Slot"])
        if slots not in range(4):
            raise ValueError(f"Invalid slot count: {slots}")
        skill1_id, skill1_points = _parse_skill(row["Skill 1"], mapping)
        skill2 = row["Skill 2"].strip()
        if skill2:
            skill2_id, skill2_points = _parse_skill(skill2, mapping)
            skill2_index = skill_indices[skill2_id]
        else:
            skill2_index, skill2_points = -1, 0
        packed_rows.append([
            0, table, source_id, CLASS_BY_SOURCE[class_name],
            skill_indices[skill1_id], skill1_points, skill2_index, skill2_points, slots,
        ])
        table_counts[table] += 1
    if table_counts != Counter({table: ROWS_PER_TABLE for table in EXPECTED_TABLES}):
        raise ValueError(f"Table row-count census mismatch: {table_counts}")

    canonical_trees = {row["stableSkillTreeId"] for row in production["skillTrees"]}
    if len(mapping) != 61 or set(mapping.values()) - canonical_trees:
        raise ValueError("Not all 61 historical skill labels resolve to production canonical IDs")
    thundr_tree = next((row for row in production["skillTrees"] if row["stableSkillTreeId"] == mapping.get("Thundr Atk")), None)
    if not thundr_tree or thundr_tree.get("jpName") != "雷属性攻撃" or thundr_tree.get("tmoDisplayName") != "ThunderAtk":
        raise ValueError("Thundr Atk exact Japanese/source identity bridge failed")

    asset = {
        "schemaVersion": 1,
        "source": {
            "category": "TIMEWORN",
            "repository": "https://github.com/mikejsavage/MHP3DB",
            "commit": SOURCE_COMMIT,
            "upstreamCsvSha256": UPSTREAM_CSV_SHA256,
            "inputCsvSha256": sha256(input_bytes),
            "skillCrosswalkSha256": sha256(crosswalk_bytes),
            "license": "WTFPL-2.0",
            "tables": list(EXPECTED_TABLES),
            "rowsPerTable": ROWS_PER_TABLE,
            "sourceRows": EXPECTED_ROW_COUNT,
        },
        "sourceCategories": ["TIMEWORN"],
        "classes": list(CLASSES),
        "skills": skill_ids,
        # Each tuple is: [category, table, sourceId, class, skill1, points1,
        # skill2(-1 means absent), points2, slots]. Source order is preserved.
        "rows": packed_rows,
    }
    return (json.dumps(asset, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepare-sources", action="store_true", help="extract standard Tables 1-12 and identity crosswalk")
    parser.add_argument("--upstream-csv", type=Path)
    parser.add_argument("--audit-crosswalk", type=Path)
    parser.add_argument("--production", type=Path, default=PRODUCTION_JSON)
    parser.add_argument("--source-csv", type=Path, default=SOURCE_CSV)
    parser.add_argument("--skill-crosswalk", type=Path, default=SKILL_CROSSWALK)
    parser.add_argument("--canonical-output", type=Path, default=CANONICAL_OUTPUT)
    parser.add_argument("--android-output", type=Path, default=ANDROID_OUTPUT)
    args = parser.parse_args()

    if args.prepare_sources:
        if args.upstream_csv is None or args.audit_crosswalk is None:
            parser.error("--prepare-sources requires --upstream-csv and --audit-crosswalk")
        source_hash, crosswalk_hash = prepare_sources(args.upstream_csv, args.audit_crosswalk, args.production)
        print(f"Filtered CSV SHA-256: {source_hash}")
        print(f"Skill crosswalk SHA-256: {crosswalk_hash}")
        return

    generated = build_asset(args.source_csv, args.skill_crosswalk, args.production)
    for output in (args.canonical_output, args.android_output):
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(generated)
    print(f"Generated {EXPECTED_ROW_COUNT} source states; {len(generated)} bytes; SHA-256 {sha256(generated)}")


# Filled from the exact, immutable filtered source files created by --prepare-sources.
EXPECTED_CURATED_SOURCE_SHA256 = "86F8296AE0DA68045791ED4448F335D83DCAB9D858DC44266CB606F845876466"
EXPECTED_CROSSWALK_SHA256 = "6AF4F24E3FF7E7D8AFF5BAA5597F7B084319E97AB3730B92BD7B9DAD28B34A32"


if __name__ == "__main__":
    main()
