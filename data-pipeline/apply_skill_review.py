#!/usr/bin/env python3
"""Apply the reviewed 1.1 skill categories and descriptions to production data."""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import shutil
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "data" / "sources" / "skill-review-v1.1"
CANONICAL_PATH = ROOT / "data" / "generated" / "mhp3rd-data.json"
ANDROID_PATH = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-data.json"
ALLOWED_CATEGORIES = {"BLADEMASTER", "GUNNER", "DEFENSE", "UTILITY"}
AUDIT_META_LANGUAGE = re.compile(
    r"do not quantify|evidence insufficient|source conflict|exact multiplier not verified",
    re.IGNORECASE,
)


def _json_file(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _matching_end(text: str, start: int) -> int:
    pairs = {"{": "}", "[": "]"}
    opening = text[start]
    if opening not in pairs:
        raise ValueError(f"Expected JSON object/array at character {start}")
    stack = [pairs[opening]]
    in_string = False
    escaped = False
    for index in range(start + 1, len(text)):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char in pairs:
            stack.append(pairs[char])
        elif char in ("}", "]"):
            if not stack or stack.pop() != char:
                raise ValueError(f"Mismatched JSON delimiter at character {index}")
            if not stack:
                return index + 1
    raise ValueError(f"Unclosed JSON value beginning at character {start}")


def _array_object_spans(text: str, opening: int) -> list[tuple[int, int]]:
    if text[opening] != "[":
        raise ValueError(f"Expected JSON array at character {opening}")
    closing = _matching_end(text, opening)
    spans: list[tuple[int, int]] = []
    index = opening + 1
    while index < closing - 1:
        if text[index].isspace() or text[index] == ",":
            index += 1
            continue
        if text[index] != "{":
            raise ValueError(f"Expected an object in JSON array at character {index}")
        end = _matching_end(text, index)
        spans.append((index, end))
        index = end
    return spans


def _array_property(text: str, key: str) -> tuple[int, int] | None:
    match = re.search(rf'(?m)^[ \t]*"{re.escape(key)}"\s*:\s*\[', text)
    if not match:
        return None
    opening = text.index("[", match.start(), match.end())
    return opening, _matching_end(text, opening)


def _replace_string_property(text: str, key: str, value: str) -> str:
    pattern = re.compile(
        rf'(?m)^([ \t]*"{re.escape(key)}"\s*:\s*)"(?:\\.|[^"\\])*"'
    )
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise ValueError(f"Expected one {key!r} property in JSON object, found {len(matches)}")
    match = matches[0]
    encoded = json.dumps(value, ensure_ascii=False)
    return text[: match.start()] + match.group(1) + encoded + text[match.end() :]


def _replace_categories(text: str, categories: list[str]) -> str:
    encoded = json.dumps(categories, ensure_ascii=False, separators=(",", ":"))
    pattern = re.compile(r'(?m)^([ \t]*"categories"\s*:\s*)\[[^\r\n]*\]')
    matches = list(pattern.finditer(text))
    if len(matches) > 1:
        raise ValueError("Skill tree contains duplicate categories properties")
    if matches:
        match = matches[0]
        return text[: match.start()] + match.group(1) + encoded + text[match.end() :]

    display = re.search(
        r'(?m)^(?P<indent>[ \t]*)"displayName"\s*:\s*"(?:\\.|[^"\\])*",(?=\r?$)', text
    )
    if not display:
        raise ValueError("Cannot locate displayName to insert the skill categories property")
    newline = "\r\n" if "\r\n" in text else "\n"
    insertion = f'{newline}{display.group("indent")}"categories": {encoded},'
    return text[: display.end()] + insertion + text[display.end() :]


def _tree_candidates_by_id(rows: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    result = {row["stableSkillTreeId"]: row for row in rows}
    if len(result) != len(rows):
        raise ValueError("Duplicate stableSkillTreeId in reviewed skill trees")
    return result


def build_reviewed_payload(
    production_bytes: bytes,
    category_source: dict[str, Any],
    description_source: dict[str, Any],
    decisions: dict[str, Any],
) -> bytes:
    """Return canonical JSON with only skill categories and descriptions changed."""
    original_data = json.loads(production_bytes.decode("utf-8"))
    if len(original_data.get("skillTrees", [])) != 100:
        raise ValueError("Expected exactly 100 canonical skill trees")
    original_ids = {row["stableSkillTreeId"] for row in original_data["skillTrees"]}

    assignments = _tree_candidates_by_id(category_source["assignments"])
    descriptions = _tree_candidates_by_id(description_source["trees"])
    if set(assignments) != original_ids or set(descriptions) != original_ids:
        raise ValueError("Reviewed category/description IDs must match all canonical skill trees")

    categories_by_id = {skill_id: list(row["categories"]) for skill_id, row in assignments.items()}
    for row in category_source.get("assignments", []):
        tags = row.get("categories")
        if not tags or not set(tags) <= ALLOWED_CATEGORIES:
            raise ValueError(f"Invalid categories for {row['stableSkillTreeId']}: {tags}")
    for override in decisions.get("categoryOverrides", []):
        skill_id = override["stableSkillTreeId"]
        if skill_id not in categories_by_id:
            raise ValueError(f"Category override targets unknown skill tree: {skill_id}")
        categories_by_id[skill_id] = list(override["categories"])
    if any(not tags or not set(tags) <= ALLOWED_CATEGORIES for tags in categories_by_id.values()):
        raise ValueError("Final category assignments must use at least one allowed category")

    activation_rows = description_source["activations"]
    activations_by_key: dict[tuple[str, str, int | None], dict[str, Any]] = {}
    for row in activation_rows:
        key = (row["stableSkillTreeId"], row["activatedSkillName"], row["thresholdPoints"])
        if key in activations_by_key:
            raise ValueError(f"Duplicate reviewed activation: {key}")
        activations_by_key[key] = row

    tree_short_overrides: dict[str, str] = {}
    detailed_overrides: dict[tuple[str, str], str] = {}
    activation_overrides: dict[tuple[str, str], str] = {}
    wording_rules: dict[str, str] = {}
    for correction in decisions.get("mandatoryAuditCorrections", []):
        if correction.get("treeShort"):
            tree_short_overrides[correction["stableSkillTreeId"]] = correction["treeShort"]
    for override in decisions.get("reviewOverrides", []):
        skill_id = override["stableSkillTreeId"]
        if override.get("treeShort"):
            tree_short_overrides[skill_id] = override["treeShort"]
        if override.get("treeDetailed"):
            detailed_overrides[(skill_id, override["name"])] = override["treeDetailed"]
        for name, summary in override.get("activationOverrides", {}).items():
            activation_overrides[(skill_id, name)] = summary
        if override.get("activationWordingRule"):
            wording_rules[skill_id] = override["activationWordingRule"]

    source_text = production_bytes.decode("utf-8")
    skill_array = _array_property(source_text, "skillTrees")
    if skill_array is None:
        raise ValueError("Cannot locate canonical skillTrees array")
    array_start, array_end = skill_array
    tree_spans = _array_object_spans(source_text, array_start)
    if len(tree_spans) != 100:
        raise ValueError(f"Expected 100 skill tree objects, found {len(tree_spans)}")

    output_text = source_text
    threshold_count = 0
    for start, end in reversed(tree_spans):
        tree_text = source_text[start:end]
        tree = json.loads(tree_text)
        skill_id = tree["stableSkillTreeId"]
        tree_candidate = descriptions[skill_id]
        summary = tree_short_overrides.get(skill_id, tree_candidate["proposedConciseDescription"])
        if not summary.strip() or AUDIT_META_LANGUAGE.search(summary):
            raise ValueError(f"Invalid player-facing skill summary for {skill_id}")

        updated = _replace_string_property(tree_text, "inGameDescription", summary)
        updated = _replace_categories(updated, categories_by_id[skill_id])

        thresholds_property = _array_property(updated, "thresholds")
        if thresholds_property is None:
            raise ValueError(f"Missing thresholds for {skill_id}")
        thresholds_start, thresholds_end = thresholds_property
        thresholds_array = updated[thresholds_start:thresholds_end]
        threshold_spans = _array_object_spans(thresholds_array, 0)
        threshold_count += len(threshold_spans)
        threshold_changes: list[tuple[int, int, str]] = []
        for threshold_start, threshold_end in threshold_spans:
            threshold_text = thresholds_array[threshold_start:threshold_end]
            threshold = json.loads(threshold_text)
            key = (skill_id, threshold["activatedSkillName"], threshold["points"])
            candidate = activations_by_key.get(key)
            if candidate is None:
                raise ValueError(f"No reviewed activation candidate for {key}")
            effect = candidate["proposedDetailedDescription"]
            effect = activation_overrides.get((skill_id, threshold["activatedSkillName"]), effect)
            effect = detailed_overrides.get((skill_id, threshold["activatedSkillName"]), effect)
            if skill_id in wording_rules and skill_id == "mhp3_skill_tree_054":
                effect = re.sub(r"^Combination success rate\b", "Item combination success rate", effect)
            if not effect.strip() or AUDIT_META_LANGUAGE.search(effect):
                raise ValueError(f"Invalid player-facing activation detail for {key}: {effect}")
            threshold_changes.append(
                (
                    threshold_start,
                    threshold_end,
                    _replace_string_property(threshold_text, "effectSummary", effect),
                )
            )
        for threshold_start, threshold_end, replacement in reversed(threshold_changes):
            thresholds_array = thresholds_array[:threshold_start] + replacement + thresholds_array[threshold_end:]
        updated = updated[:thresholds_start] + thresholds_array + updated[thresholds_end:]

        specials_property = _array_property(updated, "specialMechanics")
        if specials_property is not None:
            specials_start, specials_end = specials_property
            specials_array = updated[specials_start:specials_end]
            special_spans = _array_object_spans(specials_array, 0)
            special_changes: list[tuple[int, int, str]] = []
            for special_start, special_end in special_spans:
                special_text = specials_array[special_start:special_end]
                special = json.loads(special_text)
                key = (skill_id, special["name"], None)
                candidate = activations_by_key.get(key)
                if candidate is None:
                    raise ValueError(f"No reviewed special-mechanic candidate for {key}")
                effect = candidate["proposedDetailedDescription"]
                if not effect.strip() or AUDIT_META_LANGUAGE.search(effect):
                    raise ValueError(f"Invalid player-facing special detail for {key}")
                special_changes.append(
                    (
                        special_start,
                        special_end,
                        _replace_string_property(special_text, "effectSummary", effect),
                    )
                )
            for special_start, special_end, replacement in reversed(special_changes):
                specials_array = specials_array[:special_start] + replacement + specials_array[special_end:]
            updated = updated[:specials_start] + specials_array + updated[specials_end:]

        output_text = output_text[:start] + updated + output_text[end:]

    if threshold_count != 209:
        raise ValueError(f"Expected 209 threshold activations, found {threshold_count}")
    if set(activations_by_key) != {
        (tree["stableSkillTreeId"], threshold["activatedSkillName"], threshold["points"])
        for tree in original_data["skillTrees"]
        for threshold in tree["thresholds"]
    } | {
        (tree["stableSkillTreeId"], special["name"], None)
        for tree in original_data["skillTrees"]
        for special in tree.get("specialMechanics", [])
    }:
        raise ValueError("Reviewed activation inventory does not exactly match canonical thresholds/special mechanics")

    reviewed_data = json.loads(output_text)
    if {row["stableSkillTreeId"] for row in reviewed_data["skillTrees"]} != original_ids:
        raise ValueError("Stable skill tree IDs changed during transformation")
    if {key: value for key, value in reviewed_data.items() if key != "skillTrees"} != {
        key: value for key, value in original_data.items() if key != "skillTrees"
    }:
        raise ValueError("Transformation modified production data outside skillTrees")
    return output_text.encode("utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify generated canonical/runtime data without writing")
    args = parser.parse_args()

    category_source = _json_file(SOURCE_DIR / "category_candidates.json")
    description_source = _json_file(SOURCE_DIR / "description_candidates.json")
    decisions = _json_file(SOURCE_DIR / "final_decisions.json")
    before = CANONICAL_PATH.read_bytes()
    reviewed = build_reviewed_payload(before, category_source, description_source, decisions)
    if args.check:
        if reviewed != before:
            raise SystemExit("Canonical skill payload is stale; rerun apply_skill_review.py")
        if ANDROID_PATH.read_bytes() != reviewed:
            raise SystemExit("Android skill asset differs; rerun apply_skill_review.py")
    else:
        CANONICAL_PATH.write_bytes(reviewed)
        shutil.copyfile(CANONICAL_PATH, ANDROID_PATH)

    print(f"Canonical bytes: {len(reviewed)}")
    print(f"Canonical SHA-256: {hashlib.sha256(reviewed).hexdigest().upper()}")
    print(f"Android bytes: {ANDROID_PATH.stat().st_size}")
    print(f"Android SHA-256: {hashlib.sha256(ANDROID_PATH.read_bytes()).hexdigest().upper()}")
    print(f"Mode: {'check' if args.check else 'write'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
