import csv
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "data-pipeline"))

import generate_timeworn_charms as charms  # noqa: E402


class TimewornCharmAssetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source_rows = charms.load_csv(charms.SOURCE_CSV)[1]
        cls.crosswalk_rows = charms.load_csv(charms.SKILL_CROSSWALK)[1]
        cls.production_bytes = charms.PRODUCTION_JSON.read_bytes()
        cls.asset_bytes = charms.build_asset(charms.SOURCE_CSV, charms.SKILL_CROSSWALK, charms.PRODUCTION_JSON)
        cls.asset = json.loads(cls.asset_bytes.decode("utf-8"))

    def test_scope_is_exactly_tables_one_through_twelve_at_5400_each(self):
        self.assertEqual(64_800, len(self.source_rows))
        counts = {table: 0 for table in range(1, 13)}
        for row in self.source_rows:
            table = int(row["Table No."])
            self.assertIn(table, range(1, 13))
            self.assertEqual("Timeworn Charm", row["Charm Type"])
            counts[table] += 1
        self.assertEqual({table: 5_400 for table in range(1, 13)}, counts)
        self.assertEqual(64_800, len(self.asset["rows"]))
        expected_tables = list(range(1, 13))
        self.assertEqual(expected_tables, self.asset["source"]["tables"])
        self.assertEqual(expected_tables, sorted({row[1] for row in self.asset["rows"]}))
        self.assertEqual(5_400, self.asset["source"]["rowsPerTable"])
        self.assertEqual(64_800, self.asset["source"]["sourceRows"])
        self.assertEqual(["TIMEWORN"], self.asset["sourceCategories"])
        self.assertEqual(["Queen", "King", "Dragon"], self.asset["classes"])

    def test_all_source_ids_and_multiplicity_are_preserved_in_source_order(self):
        source_ids = [int(row["ID"]) for row in self.source_rows]
        packed_ids = [row[2] for row in self.asset["rows"]]
        self.assertEqual(source_ids, packed_ids)
        self.assertEqual(64_800, len(set(packed_ids)))
        self.assertTrue(all(len(row) == 9 for row in self.asset["rows"]))

    def test_classes_slots_and_skill_indices_are_closed(self):
        self.assertEqual(61, len(self.asset["skills"]))
        self.assertTrue(set(self.asset["skills"]) <= {row["stableSkillTreeId"] for row in json.loads(self.production_bytes)["skillTrees"]})
        self.assertTrue(all(row[3] in (0, 1, 2) for row in self.asset["rows"]))
        self.assertTrue(all(row[4] in range(61) for row in self.asset["rows"]))
        self.assertTrue(all(row[6] == -1 or row[6] in range(61) for row in self.asset["rows"]))
        self.assertTrue(all(row[8] in (0, 1, 2, 3) for row in self.asset["rows"]))
        self.assertTrue(all(row[7] == 0 for row in self.asset["rows"] if row[6] == -1))

    def test_all_61_historical_labels_map_one_to_one_to_companion_skill_trees(self):
        mapping = {row["historicalSkill"]: row["stableSkillTreeId"] for row in self.crosswalk_rows}
        self.assertEqual(61, len(mapping))
        self.assertEqual(61, len(set(mapping.values())))
        production_ids = {row["stableSkillTreeId"] for row in json.loads(self.production_bytes)["skillTrees"]}
        self.assertTrue(set(mapping.values()) <= production_ids)
        historical_labels = set()
        for row in self.source_rows:
            for field in ("Skill 1", "Skill 2"):
                if row[field]:
                    match = charms.SKILL_PATTERN.fullmatch(row[field])
                    self.assertIsNotNone(match, row[field])
                    historical_labels.add(match.group(1))
        self.assertEqual(61, len(historical_labels))
        self.assertEqual(historical_labels, set(mapping))

    def test_thundr_atk_bridge_uses_exact_japanese_and_source_identity(self):
        row = next(row for row in self.crosswalk_rows if row["historicalSkill"] == "Thundr Atk")
        canonical = next(item for item in json.loads(self.production_bytes)["skillTrees"] if item["stableSkillTreeId"] == row["stableSkillTreeId"])
        self.assertEqual("mhp3_skill_tree_090", row["stableSkillTreeId"])
        self.assertEqual("JP_NAME_雷属性攻撃_AND_TMO_ThunderAtk; 2254_POSITIONAL_ROWS", row["identityEvidence"])
        self.assertEqual("雷属性攻撃", canonical["jpName"])
        self.assertEqual("ThunderAtk", canonical["tmoDisplayName"])
        self.assertEqual("Thunder Attack", canonical["displayName"])

    def test_generator_output_is_deterministic_and_android_asset_identical(self):
        self.assertEqual(self.asset_bytes, charms.build_asset(charms.SOURCE_CSV, charms.SKILL_CROSSWALK, charms.PRODUCTION_JSON))
        self.assertEqual(self.asset_bytes, charms.CANONICAL_OUTPUT.read_bytes())
        self.assertEqual(self.asset_bytes, charms.ANDROID_OUTPUT.read_bytes())
        self.assertEqual("4DF9CCCE3B07C8F7423C7DF053C917A488F137F93F8B6BD576F79AAB019075D7", hashlib.sha256(self.asset_bytes).hexdigest().upper())
        self.assertEqual(1_699_211, len(self.asset_bytes))

    def test_upstream_provenance_and_filtered_input_hashes_are_gated(self):
        source = self.asset["source"]
        self.assertEqual(charms.SOURCE_COMMIT, source["commit"])
        self.assertEqual(charms.UPSTREAM_CSV_SHA256, source["upstreamCsvSha256"])
        self.assertEqual(charms.EXPECTED_CURATED_SOURCE_SHA256, source["inputCsvSha256"])
        self.assertEqual(charms.EXPECTED_CROSSWALK_SHA256, source["skillCrosswalkSha256"])
        self.assertEqual("WTFPL-2.0", source["license"])
        with tempfile.TemporaryDirectory() as tmp:
            bad_source = Path(tmp) / "tampered.csv"
            bad_source.write_bytes(charms.SOURCE_CSV.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "CSV SHA-256 mismatch"):
                charms.build_asset(bad_source, charms.SKILL_CROSSWALK, charms.PRODUCTION_JSON)

    def test_existing_production_payload_matches_the_reviewed_1_1_baseline(self):
        android = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-data.json"
        expected_hash = "4A11909F1D1A1C986038831DCB3C3244528F88AAC6F05F2D9A626DBB6E0CAA60"
        self.assertEqual(9_605_968, len(self.production_bytes))
        self.assertEqual(expected_hash, hashlib.sha256(self.production_bytes).hexdigest().upper())
        self.assertEqual(self.production_bytes, android.read_bytes())


if __name__ == "__main__":
    unittest.main()
