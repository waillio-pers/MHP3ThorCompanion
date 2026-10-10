import hashlib
import json
import sys
import unittest
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "data-pipeline"))

import generate_quest_progression as progression  # noqa: E402


class KeyQuestProgressionAssetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.production_bytes = progression.PRODUCTION.read_bytes()
        cls.production = json.loads(cls.production_bytes.decode("utf-8"))
        cls.production_quests = {quest["id"]: quest for quest in cls.production["quests"]}
        cls.asset_bytes = progression.build_asset()
        cls.asset = json.loads(cls.asset_bytes.decode("utf-8"))

    def test_key_memberships_match_the_accepted_scope_counts(self):
        memberships = [quest_id for step in self.asset["steps"] for quest_id in step["keyQuestIds"]]
        self.assertEqual(53, len(memberships))
        self.assertEqual(53, len(set(memberships)))
        self.assertEqual(
            {"VILLAGE": 22, "GUILD_LOW": 14, "GUILD_HIGH": 17},
            {scope: sum(len(step["keyQuestIds"]) for step in self.asset["steps"] if step["scope"] == scope)
             for scope in ("VILLAGE", "GUILD_LOW", "GUILD_HIGH")},
        )
        self.assertTrue(set(memberships) <= self.production_quests.keys())
        self.assertNotIn("quest_guild_7_star_21", memberships)

    def test_true_urgent_roles_exclude_last_supper_and_include_four_external_references(self):
        mapped = [step["urgent"]["companionQuestId"] for step in self.asset["steps"] if step.get("urgent")]
        blocked = self.asset["blockedUrgents"]
        self.assertEqual(9, len(mapped))
        self.assertEqual(4, len(blocked))
        self.assertEqual(13, len(mapped) + len(blocked))
        self.assertNotIn("quest_village_6_star_16", mapped)
        last_supper = self.asset["optionalChallenges"][0]
        self.assertEqual("quest_village_6_star_16", last_supper["questId"])
        self.assertEqual("OPTIONAL_FINAL_CHALLENGE", last_supper["role"])
        self.assertNotIn(last_supper["questId"], mapped + [entry["companionQuestId"] for entry in blocked])
        self.assertEqual(
            {"Hunt Gigginox", "Hunt Zinogre", "Hunt Nargacuga", "Hunt 2 Zinogre"},
            {entry["objectiveSummary"] for entry in blocked},
        )
        self.assertTrue(all(entry["companionQuestId"] is None and entry["role"] == "URGENT" for entry in blocked))

    def test_seven_required_prerequisites_reference_existing_quests(self):
        expected = {
            ("quest_guild_6_star_20", "quest_guild_6_star_19"),
            ("quest_guild_7_star_08", "quest_guild_7_star_20"),
            ("quest_guild_7_star_18", "quest_guild_7_star_17"),
            ("quest_guild_7_star_27", "quest_guild_7_star_25"),
            ("quest_guild_8_star_08", "quest_guild_8_star_07"),
            ("quest_guild_8_star_20", "quest_guild_8_star_22"),
            ("quest_guild_8_star_24", "quest_guild_8_star_18"),
        }
        actual = {(edge["questId"], edge["prerequisiteQuestId"]) for edge in self.asset["unlockPrerequisites"]}
        self.assertEqual(expected, actual)
        self.assertTrue(all(edge["relation"] == "REQUIRED" for edge in self.asset["unlockPrerequisites"]))
        self.assertTrue(all(left in self.production_quests and right in self.production_quests for left, right in actual))

    def test_guild_low_stars_one_and_two_have_no_key_set(self):
        low_tiers = [step for step in self.asset["steps"] if step["scope"] == "GUILD_LOW" and step["star"] in (1, 2)]
        self.assertEqual({1, 2}, {step["star"] for step in low_tiers})
        self.assertTrue(all(step["completionRule"] == "NO_KEY_SET" and not step["keyQuestIds"] for step in low_tiers))

    def test_jhen_amatsu_sequence_is_explicit_and_has_no_nine_star_step(self):
        jhen = next(step for step in self.asset["steps"] if step["id"] == "GUILD_HIGH_8_STAR")
        amatsu = next(step for step in self.asset["steps"] if step["id"] == "GUILD_HIGH_8_POST_JHEN")
        self.assertEqual("quest_guild_8_urgent_jhen", jhen["urgent"]["companionQuestId"])
        self.assertEqual("GUILD_HIGH_8_POST_JHEN", jhen["nextStepId"])
        self.assertEqual("AFTER_PREVIOUS_URGENT", amatsu["completionRule"])
        self.assertEqual(jhen["urgent"]["id"], amatsu["requiresUrgentId"])
        self.assertEqual("quest_guild_8_urgent_amatsu", amatsu["urgent"]["companionQuestId"])
        self.assertEqual(8, amatsu["star"])
        self.assertFalse(any(step["star"] == 9 for step in self.asset["steps"]))

    def test_all_references_are_valid_and_provenance_excludes_scribd(self):
        refs = {source["id"] for source in self.asset["sources"]}
        for step in self.asset["steps"]:
            self.assertTrue(set(step["sourceReferences"]) <= refs)
            self.assertTrue(set(step["keyQuestIds"]) <= self.production_quests.keys())
            if step.get("urgent"):
                self.assertIn(step["urgent"]["companionQuestId"], self.production_quests)
        for blocked in self.asset["blockedUrgents"]:
            self.assertTrue(set(blocked["sourceReferences"]) <= refs)
        self.assertFalse(any("scribd" in source["url"].lower() for source in self.asset["sources"]))

    def test_generator_and_android_asset_are_byte_deterministic(self):
        self.assertEqual(self.asset_bytes, progression.build_asset())
        self.assertEqual(self.asset_bytes, progression.CANONICAL_OUTPUT.read_bytes())
        self.assertEqual(self.asset_bytes, progression.ANDROID_OUTPUT.read_bytes())
        self.assertEqual(self.asset["production"]["questCount"], len(self.production_quests))
        self.assertEqual(354, self.asset["production"]["questCount"])

    def test_main_production_json_remains_the_accepted_byte_identical_baseline(self):
        digest = hashlib.sha256(self.production_bytes).hexdigest().upper()
        self.assertEqual(9_605_968, len(self.production_bytes))
        self.assertEqual("4A11909F1D1A1C986038831DCB3C3244528F88AAC6F05F2D9A626DBB6E0CAA60", digest)
        android = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-data.json"
        self.assertEqual(self.production_bytes, android.read_bytes())


if __name__ == "__main__":
    unittest.main()
