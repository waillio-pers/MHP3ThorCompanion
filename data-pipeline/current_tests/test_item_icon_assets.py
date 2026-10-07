import hashlib
import json
import sys
import unittest
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from item_icon_assets import (
    CLOSURE_PATH, CONVENTIONAL_MELEE_CLOSURE_PATH, DRAWABLES, EXTENSION_PATH, EXTENSION_SHARED_PATH, GREAT_SWORD_CLOSURE_PATH,
    KOTLIN_PATH, LONG_SWORD_CLOSURE_PATH, MANIFEST_PATH, SHARED_PATH, resolved_icons, resource_name, unique_resource_rows,
    SPECIAL_MELEE_CLOSURE_PATH, BOWGUN_CLOSURE_PATH,
)


class ItemIconAssetsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest, cls.resolved = resolved_icons()

    def test_manifest_has_exact_audited_identity_and_reuse_counts(self):
        self.assertEqual(561, len(self.manifest))
        self.assertEqual(544, sum(row["scope"] == "material" for row in self.manifest))
        self.assertEqual(17, sum(row["scope"] == "hunt_prep" for row in self.manifest))
        self.assertEqual(209, len(self.resolved))
        self.assertEqual(209, len({row["iconKey"] for row in self.resolved}))
        self.assertEqual(208, len(unique_resource_rows(self.resolved)))
        by_id = {row["gameItemId"]: row["iconKey"] for row in self.manifest}
        self.assertEqual("atlas:PAL_2:144:0", by_id[233])
        self.assertEqual("atlas:PAL_13:144:0", by_id[218])
        self.assertEqual("atlas:PAL_3:64:16", by_id[242])
        self.assertEqual("atlas:PAL_12:32:32", by_id[12])
        self.assertEqual(by_id[21], by_id[22])
        self.assertEqual(by_id[731], by_id[732])
        self.assertEqual(by_id[729], by_id[730])

    def test_all_eight_shared_keys_have_authentic_atlas_cell_resolutions(self):
        shared = json.loads(SHARED_PATH.read_text(encoding="utf-8"))["resolutions"]
        self.assertEqual(8, len(shared))
        self.assertEqual(8, len({row["iconKey"] for row in shared}))
        self.assertTrue(all(row["width"] == 16 and row["height"] == 16 for row in shared))
        fertile = next(row for row in shared if row["iconKey"] == "shared:dung:orange")
        self.assertEqual("medium", fertile["confidence"])
        self.assertEqual("PAL_6.png", fertile["sheet"])

    def test_all_extension_targets_and_queued_shared_keys_resolve(self):
        extension = json.loads(EXTENSION_PATH.read_text(encoding="utf-8"))
        target_ids = {row["gameItemId"] for row in extension["items"]}
        by_id = {row["gameItemId"]: row["iconKey"] for row in self.manifest}
        self.assertEqual(160, len(target_ids))
        self.assertTrue(target_ids.issubset(by_id))
        self.assertEqual(35, len(extension["sharedResolutionQueue"]))
        resolutions = json.loads(EXTENSION_SHARED_PATH.read_text(encoding="utf-8"))["resolutions"]
        self.assertEqual(
            {row["sharedKey"] for row in extension["sharedResolutionQueue"]},
            {row["sharedKey"] for row in resolutions},
        )
        self.assertEqual("atlas:PAL_6:128:0", by_id[173])
        self.assertEqual("atlas:PAL_12:128:0", by_id[174])

    def test_generated_resources_are_one_exact_bitmap_per_icon_key(self):
        generated = sorted(DRAWABLES.glob("item_icon_*.png"))
        resources = unique_resource_rows(self.resolved)
        # The current app registry packages 307 item-icon PNGs. This pipeline
        # owns the 208 atlas-crop resources in its resolved manifest; the
        # remaining app resources are not 16x16 atlas crops.
        self.assertEqual(307, len(generated))
        expected_names = {f"{resource_name(row['iconKey'])}.png" for row in resources}
        generated_by_name = {path.name: path for path in generated}
        self.assertEqual(208, len(expected_names))
        self.assertTrue(expected_names <= generated_by_name.keys())
        sizes = []
        for name in expected_names:
            path = generated_by_name[name]
            with Image.open(path) as image:
                sizes.append(image.size)
        self.assertTrue(all(size == (16, 16) for size in sizes))

    def test_generated_runtime_registry_contains_no_display_name_lookup(self):
        source = KOTLIN_PATH.read_text(encoding="utf-8")
        self.assertIn("fun resolve(gameItemId: Int)", source)
        self.assertNotIn("Ice Crystal", source)
        self.assertNotIn("Antidote", source)
        self.assertEqual(958, source.count(' to "atlas:') + source.count(' to "shared:'))

    def test_special_melee_item_closure_has_exact_authentic_mappings_and_reuse(self):
        closure = json.loads(SPECIAL_MELEE_CLOSURE_PATH.read_text(encoding="utf-8"))
        target = {row["gameItemId"]: row for row in closure["items"]}
        final = {row["gameItemId"]: row for row in self.manifest}
        self.assertEqual({74, 629, 633, 634, 635, 637}, set(target))
        self.assertEqual("1548e321a8ac2da7aa31334b919a43052f9c26dd", closure["source"]["sourceGitBlobSha"])
        self.assertEqual([74, 629, 633, 634, 635, 637], [row["gameItemId"] for row in closure["items"]])
        resolved = {row["iconKey"]: row for row in self.resolved}
        for row in target.values():
            self.assertEqual(final[row["gameItemId"]]["iconKey"], row["iconKey"])
            source = resolved[row["iconKey"]]
            for field in ("sheet", "x", "y", "width", "height"):
                self.assertEqual(row["atlas"][field], source[field])
            output = DRAWABLES / f"{resource_name(source.get('resourceIconKey', row['iconKey']))}.png"
            with Image.open(output) as image:
                self.assertEqual((16, 16), image.size)
                self.assertEqual(row["pixelSha256"], hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest().upper())
        self.assertEqual(final[634]["iconKey"], final[637]["iconKey"])

    def test_bowgun_item_closure_has_exact_cells_and_reuses_hot_spring(self):
        closure = json.loads(BOWGUN_CLOSURE_PATH.read_text(encoding="utf-8"))
        final = {row["gameItemId"]: row for row in self.manifest}
        self.assertEqual([610, 618], [row["gameItemId"] for row in closure["items"]])
        self.assertEqual("atlas:PAL_2:96:48", final[610]["iconKey"])
        self.assertEqual("atlas:PAL_5:48:64", final[618]["iconKey"])
        self.assertEqual(final[610]["iconKey"], final[623]["iconKey"])
        self.assertEqual(16, closure["items"][0]["atlas"]["width"])
        self.assertEqual(16, closure["items"][1]["atlas"]["height"])

    def test_small_monster_item_closure_has_exact_mappings_and_cell_dedup(self):
        closure = json.loads(CLOSURE_PATH.read_text(encoding="utf-8"))
        target = {row["gameItemId"]: row for row in closure["items"]}
        final = {row["gameItemId"]: row for row in self.manifest}
        self.assertEqual(27, len(target))
        self.assertEqual(22, len({row["iconKey"] for row in target.values()}))
        self.assertTrue(all(final[game_id]["iconKey"] == row["iconKey"] for game_id, row in target.items()))
        resolved = {row["iconKey"]: row for row in self.resolved}
        self.assertTrue(all(row["iconKey"] in resolved for row in target.values()))
        target_resource_keys = {resolved[row["iconKey"]]["resourceIconKey"] for row in target.values()}
        self.assertEqual(22, len({row["iconKey"] for row in target.values()}))
        self.assertEqual(22, len(target_resource_keys))
        self.assertEqual("shared:dung:orange", resolved["atlas:PAL_6:96:16"]["resourceIconKey"])

    def test_great_sword_recipe_item_closure_has_exact_authentic_mappings(self):
        closure = json.loads(GREAT_SWORD_CLOSURE_PATH.read_text(encoding="utf-8"))
        target = {row["gameItemId"]: row for row in closure["items"]}
        final = {row["gameItemId"]: row for row in self.manifest}
        expected = {19, 21, 22, 606, 608, 609, 626, 627}
        self.assertEqual(expected, set(target))
        self.assertTrue(all(final[game_id]["iconKey"] == row["iconKey"] for game_id, row in target.items()))
        self.assertTrue(all(row["resolutionMethod"] == "MHP3DB_SPRITE_DATA_EXACT" or
                            row["resolutionMethod"] == "MHP3DB_SPRITE_DATA_EXACT_REUSED_HUNT_PREP"
                            for row in target.values()))
        resolved = {row["iconKey"]: row for row in self.resolved}
        self.assertTrue(all(row["iconKey"] in resolved for row in target.values()))
        self.assertEqual(final[21]["iconKey"], final[22]["iconKey"])
        for row in target.values():
            source = resolved[row["iconKey"]]
            self.assertEqual(row["atlas"]["sheet"], source["sheet"])
            self.assertEqual(row["atlas"]["x"], source["x"])
            self.assertEqual(row["atlas"]["y"], source["y"])
            self.assertEqual(row["atlas"]["width"], source["width"])
            self.assertEqual(row["atlas"]["height"], source["height"])
            output = DRAWABLES / f"{resource_name(source.get('resourceIconKey', row['iconKey']))}.png"
            with Image.open(output) as image:
                self.assertEqual((16, 16), image.size)
                self.assertEqual(
                    row["pixelSha256"],
                    hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest().upper(),
                )

    def test_long_sword_recipe_item_closure_has_exact_authentic_mappings(self):
        closure = json.loads(LONG_SWORD_CLOSURE_PATH.read_text(encoding="utf-8"))
        target = {row["gameItemId"]: row for row in closure["items"]}
        final = {row["gameItemId"]: row for row in self.manifest}
        expected = {200, 619, 623, 624, 638}
        self.assertEqual(expected, set(target))
        self.assertTrue(all(final[game_id]["iconKey"] == row["iconKey"] for game_id, row in target.items()))
        self.assertEqual("1548e321a8ac2da7aa31334b919a43052f9c26dd", closure["source"]["sourceGitBlobSha"])
        resolved = {row["iconKey"]: row for row in self.resolved}
        for row in target.values():
            source = resolved[row["iconKey"]]
            self.assertEqual(row["atlas"]["sheet"], source["sheet"])
            self.assertEqual(row["atlas"]["x"], source["x"])
            self.assertEqual(row["atlas"]["y"], source["y"])
            self.assertEqual(row["atlas"]["width"], source["width"])
            self.assertEqual(row["atlas"]["height"], source["height"])
            output = DRAWABLES / f"{resource_name(source.get('resourceIconKey', row['iconKey']))}.png"
            with Image.open(output) as image:
                self.assertEqual((16, 16), image.size)
                self.assertEqual(row["pixelSha256"], hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest().upper())

    def test_conventional_melee_item_closure_has_exact_authentic_mappings_and_reuse(self):
        closure = json.loads(CONVENTIONAL_MELEE_CLOSURE_PATH.read_text(encoding="utf-8"))
        target = {row["gameItemId"]: row for row in closure["items"]}
        final = {row["gameItemId"]: row for row in self.manifest}
        self.assertEqual({31, 611, 617, 621, 628, 632}, set(target))
        self.assertEqual("1548e321a8ac2da7aa31334b919a43052f9c26dd", closure["source"]["sourceGitBlobSha"])
        self.assertEqual(final[611]["iconKey"], final[632]["iconKey"])
        self.assertEqual(final[621]["iconKey"], final[628]["iconKey"])
        resolved = {row["iconKey"]: row for row in self.resolved}
        for row in target.values():
            source = resolved[row["iconKey"]]
            for field in ("sheet", "x", "y", "width", "height"):
                self.assertEqual(row["atlas"][field], source[field])
            output = DRAWABLES / f"{resource_name(source.get('resourceIconKey', row['iconKey']))}.png"
            with Image.open(output) as image:
                self.assertEqual((16, 16), image.size)
                self.assertEqual(row["pixelSha256"], hashlib.sha256(image.convert("RGBA").tobytes()).hexdigest().upper())


if __name__ == "__main__":
    unittest.main()
