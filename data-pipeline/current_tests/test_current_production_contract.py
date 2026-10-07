import hashlib
import json
import re
import sys
import unittest
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "data-pipeline"))
sys.path.insert(0, str(ROOT / "tools"))

from item_icon_assets import resolved_icons  # noqa: E402
from verify_rc1_portable import EXPECTED_SCHEMA, EXPECTED_SHA256, check_references  # noqa: E402
from weapon_recipe_usage import build_usage_index  # noqa: E402


PRODUCTION_PATH = ROOT / "data" / "generated" / "mhp3rd-data.json"
ANDROID_PRODUCTION_PATH = ROOT / "app" / "src" / "main" / "assets" / "mhp3rd-data.json"


class CurrentProductionContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.production_bytes = PRODUCTION_PATH.read_bytes()
        cls.data = json.loads(cls.production_bytes.decode("utf-8"))
        cls.item_ids = {row["gameItemId"] for row in cls.data["items"]}

    def test_schema_sha_and_android_payload_are_the_accepted_release_data(self):
        self.assertEqual(EXPECTED_SCHEMA, self.data["schemaVersion"])
        self.assertEqual(EXPECTED_SHA256, hashlib.sha256(self.production_bytes).hexdigest().upper())
        self.assertEqual(self.production_bytes, ANDROID_PRODUCTION_PATH.read_bytes())
        self.assertEqual(978, len(self.data["items"]))
        self.assertEqual(set(range(1, 979)), self.item_ids)
        self.assertEqual(40, len(self.data["monsters"]))
        self.assertEqual(20, len(self.data["smallMonsters"]))
        self.assertEqual(987, len(self.data["weapons"]))
        self.assertEqual(354, len(self.data["quests"]))
        self.assertEqual(42, len(self.data["trainingQuests"]))
        self.assertEqual(100, len(self.data["skillTrees"]))

    def test_canonical_references_resolve_across_current_product_data(self):
        check_references(self.data)

    def test_current_quest_monster_and_weapon_id_links_are_closed(self):
        large_ids = {row["id"] for row in self.data["monsters"]}
        small_ids = {row["id"] for row in self.data["smallMonsters"]}
        quest_ids = {row["id"] for row in self.data["quests"]}
        training_ids = {row["id"] for row in self.data["trainingQuests"]}
        weapon_ids = {row["stableWeaponId"] for row in self.data["weapons"]}

        self.assertEqual(len(self.data["weapons"]), len(weapon_ids))
        self.assertTrue(all(set(row["questIds"]) <= quest_ids for row in self.data["monsters"]))
        self.assertTrue(all(set(row["trainingQuestIds"]) <= training_ids for row in self.data["monsters"]))
        self.assertTrue(all(set(row["questIds"]) <= quest_ids for row in self.data["smallMonsters"]))
        self.assertTrue(all(set(row["trainingQuestIds"]) <= training_ids for row in self.data["smallMonsters"]))
        self.assertTrue(all(set(row["appearingMonsterIds"]) <= large_ids | small_ids for row in self.data["quests"]))
        self.assertTrue(all(set(row["appearingMonsterIds"]) <= large_ids | small_ids for row in self.data["trainingQuests"]))
        self.assertTrue(all(row["questId"] in quest_ids for row in self.data["questRewardDrops"]))

        projection = build_usage_index(self.data)
        self.assertEqual(3834, projection["usageRowCount"])
        self.assertEqual(436, projection["itemsWithUsage"])
        self.assertEqual(87, projection["maxUniqueWeaponFanout"])
        self.assertTrue(all(row["stableWeaponId"] in weapon_ids for row in projection["usageRows"]))
        self.assertTrue(all(row["gameItemId"] in self.item_ids for row in projection["usageRows"]))
        self.assertEqual(projection, build_usage_index(self.data))

    def test_current_skills_jewels_and_crafting_relations_resolve(self):
        item_stable_ids = {row["id"] for row in self.data["items"]}
        skill_ids = {row["stableSkillTreeId"] for row in self.data["skillTrees"]}
        self.assertEqual(284, len(self.data["decorationSkillRelations"]))
        self.assertEqual(198, len(self.data["decorationCraftingRecipes"]))
        for row in self.data["decorationSkillRelations"]:
            self.assertIn(row["stableDecorationItemId"], item_stable_ids)
            self.assertIn(row["stableSkillTreeId"], skill_ids)
        for recipe in self.data["decorationCraftingRecipes"]:
            self.assertIn(recipe["outputGameItemId"], self.item_ids)
            self.assertTrue(all(part["gameItemId"] in self.item_ids for part in recipe["ingredients"]))

    def test_current_map_and_gathering_source_links_resolve_to_runtime_assets(self):
        maps_path = ROOT / "data" / "curated" / "map-nodes-v1.json"
        maps = json.loads(maps_path.read_text(encoding="utf-8"))["maps"]
        map_ids = {row["id"] for row in maps}
        self.assertEqual(6, len(map_ids))
        self.assertEqual(map_ids, {row["locationId"] for row in self.data["gatheringNodes"]})

        drawable_dir = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
        for map_row in maps:
            self.assertTrue((drawable_dir / map_row["image"]).is_file(), map_row["image"])

        node_ids = {row["id"] for row in self.data["gatheringNodes"]}
        self.assertEqual(len(self.data["gatheringNodes"]), len(node_ids))
        self.assertTrue(all(row["nodeId"] in node_ids for row in self.data["gatheringDrops"]))
        self.assertTrue(all(row["gameItemId"] in self.item_ids for row in self.data["gatheringDrops"]))

    def test_current_palico_expedition_rewards_resolve(self):
        expedition_ids = {row["id"] for row in self.data["palicoExpeditions"]}
        expeditions = self.data["palicoExpeditions"]
        self.assertEqual(24, len(expedition_ids))
        self.assertEqual({rank: 6 for rank in range(1, 5)}, {
            rank: sum(row["starRank"] == rank for row in expeditions) for rank in range(1, 5)
        })
        self.assertEqual({1: 50, 2: 100, 3: 200, 4: 400}, {
            rank: next(row["dispatchPointsPerPalico"] for row in expeditions if row["starRank"] == rank)
            for rank in range(1, 5)
        })
        self.assertEqual(662, len(self.data["palicoExpeditionRewardDrops"]))
        drops = self.data["palicoExpeditionRewardDrops"]
        self.assertEqual(662, len({row["id"] for row in drops}))
        self.assertEqual(248, len({row["gameItemId"] for row in drops}))
        self.assertEqual({"SMALL_MONSTER": 149, "LARGE_MONSTER": 77, "GATHERING": 436}, {
            category: sum(row["rewardCategory"] == category for row in drops)
            for category in ("SMALL_MONSTER", "LARGE_MONSTER", "GATHERING")
        })
        self.assertEqual({1: 629, 2: 10, 3: 16, 5: 3, 7: 1, 10: 3}, {
            quantity: sum(row["quantity"] == quantity for row in drops)
            for quantity in (1, 2, 3, 5, 7, 10)
        })
        self.assertEqual(33, sum(row["quantity"] > 1 for row in drops))
        self.assertEqual(36, sum(len(row["unlockCondition"]["unlockAnyOf"]) for row in expeditions))
        self.assertEqual(32, sum(
            choice.get("kind") == "QUEST_CLEAR"
            for row in expeditions for choice in row["unlockCondition"]["unlockAnyOf"]
        ))
        self.assertEqual(12, sum(len(row["unlockCondition"]["unlockAnyOf"]) == 2 for row in expeditions))
        self.assertTrue({200, 598, 599, 600, 601} <= {row["gameItemId"] for row in drops})
        self.assertFalse(any("probability" in row or "gatheringVariant" in row for row in drops))
        self.assertNotIn("fieldPalico", self.data)
        self.assertTrue(all(row["expeditionId"] in expedition_ids for row in drops))
        self.assertTrue(all(row["gameItemId"] in self.item_ids for row in drops))

    def test_current_item_icon_mapping_targets_canonical_items(self):
        manifest, resolved = resolved_icons()
        manifest_ids = {row["gameItemId"] for row in manifest}
        self.assertEqual(561, len(manifest_ids))
        self.assertTrue(manifest_ids <= self.item_ids)
        self.assertEqual(209, len(resolved))

        user_path = ROOT / "data" / "curated" / "item-icons-v1" / "user-adjudication-v1.json"
        user = json.loads(user_path.read_text(encoding="utf-8"))
        self.assertEqual("USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE", user["provenance"])
        self.assertEqual(47, user["selectedCount"])
        self.assertEqual(0, user["unresolvedCount"])
        targets = user["targets"]
        self.assertEqual(47, len(targets))

        registry_path = ROOT / "app" / "src" / "main" / "java" / "com" / "waillio" / "mhp3rdcompanion" / "ItemIconRegistry.kt"
        registry = registry_path.read_text(encoding="utf-8")
        mapping_section = registry.split("private val resourcesByIconKey = mapOf(", 1)[0]
        mapping = {
            int(item_id): icon_key
            for item_id, icon_key in re.findall(r'^\s*(\d+)\s+to\s+"([^"]+)"\s*,', mapping_section, re.M)
        }
        resource_section = registry.split("private val resourcesByIconKey = mapOf(", 1)[1]
        resources = {
            icon_key: resource_name
            for icon_key, resource_name in re.findall(
                r'^\s*"([^"]+)"\s+to\s+R\.drawable\.([A-Za-z0-9_]+)\s*,', resource_section, re.M
            )
        }
        provenance_block = registry.split("private val userAdjudicatedGameItemIds = setOf(", 1)[1].split(")", 1)[0]
        provenance_ids = {int(item_id) for item_id in re.findall(r"^\s*(\d+)\s*,", provenance_block, re.M)}
        self.assertEqual({row["gameItemId"] for row in targets}, provenance_ids)
        self.assertIn('const val USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE = "USER_ADJUDICATED_PHYSICAL_GAME_REFERENCE"', registry)
        self.assertIn("fun provenanceFor(gameItemId: Int)", registry)

        drawable_dir = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
        for target in targets:
            item_id = target["gameItemId"]
            icon_key = f"atlas:{target['atlas']}:{target['x']}:{target['y']}"
            self.assertIn(item_id, self.item_ids)
            self.assertEqual(icon_key, mapping[item_id])
            resource_name = resources[icon_key]
            with Image.open(drawable_dir / f"{resource_name}.png") as image:
                self.assertEqual((16, 16), image.size, resource_name)

    def test_all_final_monster_headers_match_ids_and_expected_crop_size(self):
        monster_ids = {row["id"] for row in self.data["monsters"]}
        header_dir = ROOT / "app" / "src" / "main" / "assets" / "monster-headers"
        headers = sorted(header_dir.glob("*.webp"))
        self.assertEqual({f"{monster_id}.webp" for monster_id in monster_ids}, {path.name for path in headers})
        for path in headers:
            with Image.open(path) as image:
                self.assertEqual((2432, 300), image.size, path.name)


if __name__ == "__main__":
    unittest.main()
