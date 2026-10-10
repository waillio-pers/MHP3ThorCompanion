import hashlib
import json
import re
import sys
import unittest
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "data-pipeline"))

from apply_skill_review import build_reviewed_payload  # noqa: E402


CANONICAL_PATH = ROOT / "data" / "generated" / "mhp3rd-data.json"
ANDROID_PATH = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-data.json"
SOURCE_DIR = ROOT / "data" / "sources" / "skill-review-v1.1"
EXPECTED_PRODUCTION_SHA256 = "4A11909F1D1A1C986038831DCB3C3244528F88AAC6F05F2D9A626DBB6E0CAA60"
ALLOWED_CATEGORIES = {"BLADEMASTER", "GUNNER", "DEFENSE", "UTILITY"}


def _read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


class SkillReviewContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.canonical_bytes = CANONICAL_PATH.read_bytes()
        cls.runtime_bytes = ANDROID_PATH.read_bytes()
        cls.data = json.loads(cls.canonical_bytes.decode("utf-8"))
        cls.trees = {row["stableSkillTreeId"]: row for row in cls.data["skillTrees"]}
        cls.decisions = _read_json(SOURCE_DIR / "final_decisions.json")

    def test_canonical_tree_count_and_many_to_many_category_contract(self):
        self.assertEqual(100, len(self.data["skillTrees"]))
        self.assertEqual(100, len(self.trees))
        for tree in self.data["skillTrees"]:
            self.assertTrue(tree["categories"], tree["stableSkillTreeId"])
            self.assertLessEqual(set(tree["categories"]), ALLOWED_CATEGORIES)

        anti_theft = self.trees["mhp3_skill_tree_037"]["categories"]
        self.assertIn("UTILITY", anti_theft)
        self.assertNotIn("DEFENSE", anti_theft)
        self.assertEqual(
            {
                "BLADEMASTER": 33,
                "GUNNER": 41,
                "DEFENSE": 28,
                "UTILITY": 26,
            },
            {
                category: sum(category in tree["categories"] for tree in self.data["skillTrees"])
                for category in sorted(ALLOWED_CATEGORIES)
            },
        )
        self.assertEqual(
            {
                "BLADEMASTER": 10,
                "BLADEMASTER;DEFENSE;GUNNER": 1,
                "BLADEMASTER;GUNNER": 18,
                "BLADEMASTER;GUNNER;UTILITY": 3,
                "BLADEMASTER;UTILITY": 1,
                "DEFENSE": 26,
                "DEFENSE;UTILITY": 1,
                "GUNNER": 19,
                "UTILITY": 21,
            },
            dict(Counter(";".join(sorted(tree["categories"])) for tree in self.data["skillTrees"])),
        )

    def test_review_generator_is_deterministic_and_runtime_matches(self):
        category_source = _read_json(SOURCE_DIR / "category_candidates.json")
        description_source = _read_json(SOURCE_DIR / "description_candidates.json")
        decisions = _read_json(SOURCE_DIR / "final_decisions.json")
        self.assertEqual(
            self.canonical_bytes,
            build_reviewed_payload(self.canonical_bytes, category_source, description_source, decisions),
        )
        self.assertEqual(self.canonical_bytes, self.runtime_bytes)
        self.assertEqual(9_605_968, len(self.canonical_bytes))
        self.assertEqual(EXPECTED_PRODUCTION_SHA256, hashlib.sha256(self.canonical_bytes).hexdigest().upper())

    def test_user_reviewed_copy_and_audit_corrections(self):
        stun = self.trees["mhp3_skill_tree_005"]
        self.assertEqual(
            "Changes resistance to being stunned: +15 prevents stun, +10 halves stun chance, and -10 doubles stun chance.",
            stun["inGameDescription"],
        )
        self.assertNotRegex(stun["inGameDescription"].lower(), r"duration.*(half|double)|(half|double).*duration")
        self.assertEqual(
            ["Stun is negated.", "Stun chance is halved.", "Stun chance is doubled."],
            [row["effectSummary"] for row in stun["thresholds"]],
        )

        self.assertEqual(
            "Prevents items from being stolen, including by Melynx.",
            self.trees["mhp3_skill_tree_037"]["inGameDescription"],
        )
        self.assertEqual(
            "Prevents items from being stolen. Melynx are the black Felyne-like enemies that commonly steal items.",
            self.trees["mhp3_skill_tree_037"]["thresholds"][0]["effectSummary"],
        )

        auto_guard = self.trees["mhp3_skill_tree_017"]
        self.assertIn("if the equipped weapon can guard", auto_guard["inGameDescription"])
        self.assertIn("incoming attack can be guarded", auto_guard["thresholds"][0]["effectSummary"])
        self.assertIn("current state", auto_guard["thresholds"][0]["effectSummary"])

        guard = self.trees["mhp3_skill_tree_015"]
        self.assertIn("(Blademaster only)", guard["inGameDescription"])
        for row in guard["thresholds"]:
            self.assertIn("knockback and chip damage received while guarding", row["effectSummary"])

        bombardier = self.trees["mhp3_skill_tree_030"]["thresholds"][0]["effectSummary"]
        self.assertIn("1.3× damage", bombardier)
        self.assertIn("bomb-item combination recipes succeed 100%", bombardier)
        self.assertIn("only the physical damage portion is boosted", bombardier)

        capture = self.trees["mhp3_skill_tree_069"]
        self.assertEqual("Increases the number of capture reward slots.", capture["inGameDescription"])
        self.assertTrue(all("capture reward slots" in row["effectSummary"].lower() for row in capture["thresholds"]))

        combo = self.trees["mhp3_skill_tree_054"]
        self.assertEqual("Changes item combination success rate.", combo["inGameDescription"])
        self.assertTrue(all(row["effectSummary"].startswith("Item combination success rate ") for row in combo["thresholds"]))

        constitution = self.trees["mhp3_skill_tree_068"]
        by_name = {row["activatedSkillName"]: row["effectSummary"] for row in constitution["thresholds"]}
        self.assertEqual(
            "Fixed stamina costs for evades, guarding, and Switch Axe swings are reduced to 1/2.",
            by_name["Constitution +2"],
        )
        self.assertEqual(
            "Fixed stamina costs for evades, guarding, and Switch Axe swings increase.",
            by_name["Constitution -1"],
        )
        self.assertEqual(
            "Fixed stamina costs for evades, guarding, and Switch Axe swings increase substantially.",
            by_name["Constitution -2"],
        )

        fate = self.trees["mhp3_skill_tree_051"]
        self.assertIn("additional basic quest reward slots", fate["inGameDescription"])
        self.assertIn("specific item's drop rate", fate["inGameDescription"])
        self.assertTrue(all("basic reward slot" in row["effectSummary"] for row in fate["thresholds"]))

        gloves_off = self.trees["mhp3_skill_tree_085"]
        glove_text = " ".join(row["effectSummary"] for row in gloves_off["thresholds"])
        for phrase in ("90 seconds", "about 5 minutes", "about 150 damage", "+30% Affinity", "+50% Affinity", "continuous stamina drain to 1/4"):
            self.assertIn(phrase, glove_text)
        self.assertNotRegex(glove_text, r"fixed stamina costs? (?:to |are )?1/[45]")

        ko = self.trees["mhp3_skill_tree_097"]
        self.assertIn("KO (stun) buildup", ko["inGameDescription"])
        self.assertIn("hitting a monster's head", ko["inGameDescription"])
        self.assertIn("rounded down at the first decimal", ko["thresholds"][0]["effectSummary"])

        status = self.trees["mhp3_skill_tree_028"]
        self.assertEqual("Changes poison, paralysis, and sleep buildup.", status["inGameDescription"])
        self.assertTrue(all(all(word in row["effectSummary"].lower() for word in ("poison", "paralysis", "sleep")) for row in status["thresholds"]))

        all_copy = [tree["inGameDescription"] for tree in self.data["skillTrees"]]
        all_copy.extend(row["effectSummary"] for tree in self.data["skillTrees"] for row in tree["thresholds"])
        all_copy.extend(row["effectSummary"] for tree in self.data["skillTrees"] for row in tree.get("specialMechanics", []))
        for copy in all_copy:
            self.assertNotRegex(copy, r"do not quantify|evidence insufficient|source conflict|exact multiplier not verified")

    def test_negative_activations_and_separate_assets_remain_intact(self):
        negative_rows = [
            row
            for tree in self.data["skillTrees"]
            for row in tree["thresholds"]
            if row["points"] < 0
        ]
        self.assertGreater(len(negative_rows), 0)
        self.assertTrue(all(row["polarity"] == "NEGATIVE" for row in negative_rows))
        self.assertTrue(all(row["effectSummary"].strip() for row in negative_rows))

        expected = {
            "mhp3rd-charms.json": (1_699_211, "4DF9CCCE3B07C8F7423C7DF053C917A488F137F93F8B6BD576F79AAB019075D7"),
            "mhp3rd-quest-progression.json": (16_702, "43939D9899AC1931510616777D7F869CE00C5DB818B9734460441BD607699D95"),
        }
        for name, (size, digest) in expected.items():
            canonical = ROOT / "data" / "generated" / name
            runtime = ROOT / "app" / "src" / "main" / "assets" / name
            self.assertEqual(canonical.read_bytes(), runtime.read_bytes(), name)
            self.assertEqual(size, canonical.stat().st_size, name)
            self.assertEqual(digest, hashlib.sha256(canonical.read_bytes()).hexdigest().upper(), name)


if __name__ == "__main__":
    unittest.main()
