#!/usr/bin/env python3
"""Generate the additive MHP3 Key Quest progression asset."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "data" / "sources" / "key-quest-progression.json"
PRODUCTION = ROOT / "data" / "generated" / "mhp3rd-data.json"
CANONICAL_OUTPUT = ROOT / "data" / "generated" / "mhp3rd-quest-progression.json"
ANDROID_OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-quest-progression.json"
# Provenance of the accepted quest corpus before the additive 1.1 skill review.
PRODUCTION_BYTES = 9_600_571
PRODUCTION_SHA256 = "657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155"
QUEST_CORPUS_SHA256 = "5DBD2CFB5814CBC7A6325FB29932B850C31FCAEA3CEB44151CDBAA66B30B5256"
EXPECTED_KEY_COUNTS = {"VILLAGE": 22, "GUILD_LOW": 14, "GUILD_HIGH": 17}
EXPECTED_BLOCKED_URGENTS = {
    "Hunt Gigginox",
    "Hunt Zinogre",
    "Hunt Nargacuga",
    "Hunt 2 Zinogre",
}


def sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest().upper()


def _check_source_refs(value: Any, source_ids: set[str]) -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            if key == "sourceReferences":
                if not isinstance(child, list) or not set(child) <= source_ids:
                    raise ValueError(f"Unknown source reference: {child}")
            else:
                _check_source_refs(child, source_ids)
    elif isinstance(value, list):
        for child in value:
            _check_source_refs(child, source_ids)


def build_asset(source_path: Path = SOURCE, production_path: Path = PRODUCTION) -> bytes:
    source = json.loads(source_path.read_text(encoding="utf-8"))
    production_bytes = production_path.read_bytes()
    production = json.loads(production_bytes.decode("utf-8"))
    quests = production.get("quests", [])
    quest_ids = {quest["id"] for quest in quests}
    if len(quests) != 354 or len(quest_ids) != 354:
        raise ValueError(f"Expected the unchanged 354-quest production corpus, got {len(quests)}")
    quest_bytes = json.dumps(quests, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    if sha256(quest_bytes) != QUEST_CORPUS_SHA256:
        raise ValueError("Canonical quest corpus no longer matches its accepted source baseline")

    source_ids = {entry["id"] for entry in source["sources"]}
    if len(source_ids) != len(source["sources"]):
        raise ValueError("Duplicate progression source IDs")
    _check_source_refs(source, source_ids)
    steps = source["steps"]
    step_ids = {step["id"] for step in steps}
    if len(step_ids) != len(steps):
        raise ValueError("Duplicate progression step IDs")

    key_ids: list[str] = []
    mapped_urgent_ids: list[str] = []
    for step in steps:
        if step["phase"] == "KEY_SET":
            if step["completionRule"] == "ALL_LISTED_KEYS" and not step["keyQuestIds"]:
                raise ValueError(f"Key step has an empty required set: {step['id']}")
            if step["completionRule"] == "NO_KEY_SET" and step["keyQuestIds"]:
                raise ValueError(f"No-key step contains keys: {step['id']}")
            key_ids.extend(step["keyQuestIds"])
        elif step["phase"] == "AFTER_URGENT_FOLLOW_UP":
            if step["completionRule"] != "AFTER_PREVIOUS_URGENT" or step.get("requiresUrgentId") is None:
                raise ValueError(f"Follow-up urgent is not structurally linked: {step['id']}")
        else:
            raise ValueError(f"Unsupported step phase: {step['phase']}")
        if step.get("nextStepId") and step["nextStepId"] not in step_ids:
            raise ValueError(f"Unknown next step: {step['nextStepId']}")
        urgent = step.get("urgent")
        if urgent is not None:
            if urgent["companionQuestId"] not in quest_ids:
                raise ValueError(f"Mapped urgent is not a production quest: {urgent['companionQuestId']}")
            mapped_urgent_ids.append(urgent["companionQuestId"])
        blocked_urgent_id = step.get("blockedUrgentId")
        if blocked_urgent_id and blocked_urgent_id not in {entry["id"] for entry in source["blockedUrgents"]}:
            raise ValueError(f"Unknown blocked urgent: {blocked_urgent_id}")

    if len(key_ids) != 53 or len(set(key_ids)) != 53:
        raise ValueError(f"Expected 53 unique Key quest memberships, got {len(key_ids)}")
    if set(key_ids) - quest_ids:
        raise ValueError(f"Key references missing from production: {sorted(set(key_ids) - quest_ids)}")
    counts = {
        scope: sum(len(step["keyQuestIds"]) for step in steps if step["scope"] == scope)
        for scope in EXPECTED_KEY_COUNTS
    }
    if counts != EXPECTED_KEY_COUNTS:
        raise ValueError(f"Key count mismatch: {counts}")
    if "quest_guild_7_star_21" in key_ids:
        raise ValueError("Jade Barroth must remain outside the Key set")

    blocked = source["blockedUrgents"]
    if len(blocked) != 4 or {urgent["objectiveSummary"] for urgent in blocked} != EXPECTED_BLOCKED_URGENTS:
        raise ValueError("The four evidence-backed blocked urgent identities changed")
    if any(urgent["companionQuestId"] is not None or urgent["role"] != "URGENT" for urgent in blocked):
        raise ValueError("Blocked urgent must remain progression-only and role URGENT")
    if len(mapped_urgent_ids) != 9 or len(set(mapped_urgent_ids)) != 9:
        raise ValueError(f"Expected 9 distinct mapped progression urgents, got {mapped_urgent_ids}")
    if len(mapped_urgent_ids) + len(blocked) != 13:
        raise ValueError("Expected exactly 13 progression urgents")

    prerequisites = source["unlockPrerequisites"]
    if len(prerequisites) != 7:
        raise ValueError(f"Expected exactly 7 required unlock prerequisites, got {len(prerequisites)}")
    for prerequisite in prerequisites:
        if prerequisite["relation"] != "REQUIRED" or prerequisite["questId"] not in quest_ids or prerequisite["prerequisiteQuestId"] not in quest_ids:
            raise ValueError(f"Invalid unlock prerequisite: {prerequisite}")
    if len(source["optionalChallenges"]) != 1:
        raise ValueError("Expected the separately modeled Last Supper optional challenge")
    optional = source["optionalChallenges"][0]
    if optional["questId"] != "quest_village_6_star_16" or optional["role"] != "OPTIONAL_FINAL_CHALLENGE":
        raise ValueError("The Last Supper must remain an optional final challenge")
    if optional["questId"] in mapped_urgent_ids or optional["questId"] in key_ids:
        raise ValueError("The Last Supper cannot be a progression urgent or key")
    if any(step["star"] == 9 for step in steps):
        raise ValueError("MHP3 has no 9-star Guild progression step")

    # Keep the payload separate and compact: only progression references and source provenance.
    payload = {
        "schemaVersion": 1,
        "evidenceAsOf": source["evidenceAsOf"],
        "production": {"questCount": len(quests), "bytes": PRODUCTION_BYTES, "sha256": PRODUCTION_SHA256},
        "sources": source["sources"],
        "steps": steps,
        "blockedUrgents": blocked,
        "optionalChallenges": source["optionalChallenges"],
        "unlockPrerequisites": prerequisites,
        "multiplayerHelp": source["multiplayerHelp"],
    }
    return (json.dumps(payload, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail unless canonical and Android outputs are current")
    args = parser.parse_args()
    content = build_asset()
    if args.check:
        for output in (CANONICAL_OUTPUT, ANDROID_OUTPUT):
            if not output.exists() or output.read_bytes() != content:
                raise SystemExit(f"Generated asset is stale: {output}")
    else:
        for output in (CANONICAL_OUTPUT, ANDROID_OUTPUT):
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(content)
    print(f"Wrote/verified {len(content)} bytes, SHA-256 {sha256(content)}")


if __name__ == "__main__":
    main()
