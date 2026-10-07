from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path
from urllib.request import Request, urlopen

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
CURATED = ROOT / "data" / "curated" / "item-icons-v1"
MANIFEST_PATH = CURATED / "item-icon-manifest-v1.json"
REGISTRY_PATH = CURATED / "icon-cell-registry-v1.json"
SHARED_PATH = CURATED / "shared-icon-cell-resolutions-v1.json"
EXTENSION_PATH = ROOT / "data" / "curated" / "items-catalog-icon-extension-v1.json"
EXTENSION_SHARED_PATH = CURATED / "item-icon-extension-shared-resolutions-v1.json"
CLOSURE_PATH = ROOT / "data" / "curated" / "small-monster-item-icon-resolutions-v1.json"
CLOSURE_CROPS = CURATED / "small-monster-item-closure-crops"
GREAT_SWORD_CLOSURE_PATH = ROOT / "data" / "curated" / "great-sword-item-icon-closure-v1.json"
LONG_SWORD_CLOSURE_PATH = ROOT / "data" / "curated" / "long-sword-item-closure-v1.json"
CONVENTIONAL_MELEE_CLOSURE_PATH = ROOT / "data" / "curated" / "conventional-melee-item-closure-v1.json"
SPECIAL_MELEE_CLOSURE_PATH = ROOT / "data" / "curated" / "special-melee-item-closure-v1.json"
BOWGUN_CLOSURE_PATH = ROOT / "data" / "curated" / "bowgun-item-icon-closure-v1.json"
GENERATED_DATA_PATH = ROOT / "data" / "generated" / "mhp3rd-data.json"
REPORT_PATH = ROOT / "data" / "generated" / "item-icon-extension-v1-report.json"
CLOSURE_REPORT_PATH = ROOT / "data" / "generated" / "small-monster-item-icon-closure-v1-report.json"
GREAT_SWORD_CLOSURE_REPORT_PATH = ROOT / "data" / "generated" / "great-sword-item-icon-closure-v1-report.json"
CACHE = ROOT / "build" / "item-icon-source-cache-v1"
DRAWABLES = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
KOTLIN_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "waillio" / "mhp3rdcompanion" / "ItemIconRegistry.kt"
COMMIT = "7ad3cf1c5ba07ce2e63afdaa1fa37c4671edc3bf"
ATLAS_BASE = f"https://raw.githubusercontent.com/mhp3db/mhp3db.github.io/{COMMIT}/assets/database/spritesheets/"
SOURCE_HASHES = {
    "PAL_0.png": "2184A428136422720F234134823EAFF94F3FE0F214FB6B8331BF13A45C4E1EEC",
    "PAL_1.png": "1A35BE069F5817C2634A9B0D638E96EE6EE03BE370F7D8CFFA6A7029787215BC",
    "PAL_2.png": "5ADAAB0FAC9CF4E6F010C47FD1B0E27C378CC4BA361348117BB5FCE13BCABEBC",
    "PAL_3.png": "F61F2476665999E347C3C9BEF0FE40A9424AA2BBB7883DB81786D52D648E6A9C",
    "PAL_4.png": "459B088CC2662B1D030FC241A2C571F61B1B71B9E606435E059F97CFCEEF4A63",
    "PAL_5.png": "5E6E223DE97D8D395B50F1491052FE8D73DD335355A0FFA7E731F8B8F8F44F3B",
    "PAL_6.png": "64942574D53E7D5056B45F47A0912BBCF51C13524CA0AA754A3D76F2FBC9EF28",
    "PAL_7.png": "3BF4DCB15BA192C589CDE4AECFB2920ABA193AF98F9B9FAB012260224802407D",
    "PAL_8.png": "AFEDCBC03E5B99D0077AAD0269B26FA55AF7845C7792F04F5F12D89763EAF210",
    "PAL_9.png": "8ECDD5452FCD584ADB9F6B5742E2C64252FB27765A7A62679D66711513025D3B",
    "PAL_10.png": "DB9419090FA19ED0D9C3AC742A8D92674C48BD2CA9B7A75025BC69F66A4DBD62",
    "PAL_11.png": "2E6C352AC06C19A21D3BDEF52FD56FB65EB6E8BE30A1F04C97850B758EABA928",
    "PAL_12.png": "E60FDF0A5FEAA0DAF2D52D48A643146C043431FC9B04959F23903A42193D07DE",
    "PAL_13.png": "DF9ED7BF8DDEF67030C8B591A3533408F5D8E28A635BBD20390148C0B7E6D858",
}


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def atlas_source_path(sheet: str) -> Path:
    """Use the ignored cache, fetching the hash-pinned source pack when needed."""
    cached = CACHE / sheet
    if cached.exists():
        return cached
    fetch_sources()
    return cached


def resource_name(icon_key: str) -> str:
    return "item_icon_" + re.sub(r"[^a-z0-9]+", "_", icon_key.lower()).strip("_")


def fetch_sources() -> None:
    CACHE.mkdir(parents=True, exist_ok=True)
    for name, expected in SOURCE_HASHES.items():
        path = CACHE / name
        if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest().upper() != expected:
            request = Request(ATLAS_BASE + name, headers={"User-Agent": "MHP3rd-Companion-Item-Icons/1.0"})
            with urlopen(request, timeout=60) as response:
                path.write_bytes(response.read())
        actual = hashlib.sha256(path.read_bytes()).hexdigest().upper()
        if actual != expected:
            raise ValueError(f"Pinned source hash mismatch for {name}: {actual}")


def resolved_icons() -> tuple[list[dict], list[dict]]:
    # These two accepted v1 files remain the immutable 351-identity baseline.
    baseline_manifest = load(MANIFEST_PATH)["items"]
    baseline_registry = load(REGISTRY_PATH)["icons"]
    shared = {row["iconKey"]: row for row in load(SHARED_PATH)["resolutions"]}
    if len(baseline_manifest) != 351 or len(baseline_registry) != 114:
        raise ValueError("Expected 351 mappings and 114 unique icon keys")
    resolved_by_key = {}
    for row in baseline_registry:
        if row["renderKind"] == "ATLAS_CELL":
            cell = row["atlas"]
        else:
            cell = shared.get(row["iconKey"])
            if cell is None:
                raise ValueError(f"Missing authentic shared-cell resolution: {row['iconKey']}")
        resolved_by_key[row["iconKey"]] = {
            "iconKey": row["iconKey"],
            "resourceIconKey": row["iconKey"],
            **{key: cell[key] for key in ("sheet", "x", "y", "width", "height")},
        }

    extension = load(EXTENSION_PATH)
    extension_rows = extension["items"]
    queue = {row["sharedKey"] for row in extension["sharedResolutionQueue"]}
    curated_rows = load(EXTENSION_SHARED_PATH)["resolutions"]
    curated = {row["sharedKey"]: row for row in curated_rows}
    if len(extension_rows) != 160 or len(queue) != 35 or set(curated) != queue:
        raise ValueError("Extension must contain 160 targets and resolve all 35 queued shared keys")

    baseline_by_id = {row["gameItemId"]: row for row in baseline_manifest}
    production_names = {
        row["gameItemId"]: row["name"] for row in load(GENERATED_DATA_PATH)["items"]
    }
    manifest = [dict(row) for row in baseline_manifest]
    for row in sorted(extension_rows, key=lambda value: value["gameItemId"]):
        game_item_id = row["gameItemId"]
        if row["resolutionKind"] == "SHARED_FAMILY_NEEDS_ATLAS_CELL_RESOLUTION":
            resolution = curated[row["iconKey"]]
            final_icon_key = resolution["finalIconKey"]
            cell = resolution
        else:
            final_icon_key = row["iconKey"]
            cell = row["atlas"]

        existing = baseline_by_id.get(game_item_id)
        if existing is not None:
            if existing["iconKey"] != final_icon_key:
                raise ValueError(f"Extension changes accepted mapping for gameItemId {game_item_id}")
            continue

        name = production_names.get(game_item_id)
        if name is None:
            raise ValueError(f"Extension identity is not a production Item: {game_item_id}")
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": name,
            "iconKey": final_icon_key,
        })

        normalized = {
            "iconKey": final_icon_key,
            "resourceIconKey": final_icon_key,
            **{key: cell[key] for key in ("sheet", "x", "y", "width", "height")},
        }
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None and prior != normalized:
            raise ValueError(f"Icon key resolves to multiple cells: {final_icon_key}")
        resolved_by_key[final_icon_key] = normalized

    if len(manifest) != 509 or len({row["gameItemId"] for row in manifest}) != 509:
        raise ValueError("Expected 509 pre-closure ItemIconRegistry identities")

    closure = load(CLOSURE_PATH)
    closure_rows = closure["items"]
    if len(closure_rows) != 27 or len({row["gameItemId"] for row in closure_rows}) != 27:
        raise ValueError("Small Monster Item closure must contain 27 unique mappings")
    if len({row["iconKey"] for row in closure_rows}) != 22 or closure.get("unresolved") != 0:
        raise ValueError("Small Monster Item closure must resolve 22 unique icon keys with no gaps")
    existing_ids = {row["gameItemId"] for row in manifest}
    cell_to_resource_key = {
        (row["sheet"], row["x"], row["y"], row["width"], row["height"]): row["resourceIconKey"]
        for row in resolved_by_key.values()
    }
    for row in sorted(closure_rows, key=lambda value: value["gameItemId"]):
        game_item_id = row["gameItemId"]
        if game_item_id in existing_ids:
            raise ValueError(f"Closure changes an accepted icon identity: {game_item_id}")
        if production_names.get(game_item_id) != row["name"]:
            raise ValueError(f"Closure icon name differs from production TMO name: {game_item_id}")
        cell = row["atlas"]
        normalized_cell = {key: cell[key] for key in ("sheet", "x", "y", "width", "height")}
        reference_path = CLOSURE_CROPS / Path(row["cropReference"]["file"]).name
        if hashlib.sha256(reference_path.read_bytes()).hexdigest().upper() != row["cropReference"]["sha256"]:
            raise ValueError(f"Closure reference crop SHA mismatch: {game_item_id}")
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != normalized_cell[key] for key in normalized_cell):
                raise ValueError(f"Closure icon key changes accepted cell: {final_icon_key}")
            resource_icon_key = prior["resourceIconKey"]
        else:
            cell_key = tuple(normalized_cell[key] for key in ("sheet", "x", "y", "width", "height"))
            resource_icon_key = cell_to_resource_key.get(cell_key, final_icon_key)
            resolved_by_key[final_icon_key] = {
                "iconKey": final_icon_key,
                "resourceIconKey": resource_icon_key,
                **normalized_cell,
            }
            cell_to_resource_key.setdefault(cell_key, resource_icon_key)
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": row["name"],
            "iconKey": final_icon_key,
        })

    # The eight Great Sword recipe Items are appended to production by the
    # 4.9C.1 Item stage. Armorskin/Mega Armorskin already have accepted
    # hunt-prep mappings, so reuse those IDs/cells instead of duplicating IDs.
    great_sword_closure = load(GREAT_SWORD_CLOSURE_PATH)
    great_rows = great_sword_closure["items"]
    if len(great_rows) != 8 or len({row["gameItemId"] for row in great_rows}) != 8:
        raise ValueError("Great Sword Item icon closure must contain exactly 8 unique mappings")
    manifest_by_id = {row["gameItemId"]: row for row in manifest}
    for row in sorted(great_rows, key=lambda value: value["gameItemId"]):
        game_item_id = row["gameItemId"]
        name = production_names.get(game_item_id)
        if name != row["name"]:
            raise ValueError(f"Great Sword closure icon name differs from production TMO name: {game_item_id}")
        cell = row["atlas"]
        normalized_cell = {key: cell[key] for key in ("sheet", "x", "y", "width", "height")}
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != normalized_cell[key] for key in normalized_cell):
                raise ValueError(f"Great Sword closure changes accepted cell: {final_icon_key}")
        else:
            resolved_by_key[final_icon_key] = {
                "iconKey": final_icon_key,
                "resourceIconKey": final_icon_key,
                **normalized_cell,
            }
        existing = manifest_by_id.get(game_item_id)
        if existing is not None:
            if existing["iconKey"] != final_icon_key:
                raise ValueError(f"Great Sword closure changes accepted mapping: {game_item_id}")
            continue
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": name,
            "iconKey": final_icon_key,
        })
        manifest_by_id[game_item_id] = manifest[-1]

    # The five Long Sword recipe Items are closed after Great Sword
    # production, before any future Long Sword weapon integration.  Reuse an
    # already resolved cell when the canonical sprite mapping points at one;
    # otherwise add the exact palette/cell to the shared resource registry.
    long_sword_closure = load(LONG_SWORD_CLOSURE_PATH)
    if long_sword_closure.get("source", {}).get("sourceGitBlobSha") != "1548e321a8ac2da7aa31334b919a43052f9c26dd":
        raise ValueError("Long Sword Item icon closure sprite_data.js blob pin changed")
    long_rows = long_sword_closure["items"]
    if len(long_rows) != 5 or len({row["gameItemId"] for row in long_rows}) != 5:
        raise ValueError("Long Sword Item icon closure must contain exactly five unique mappings")
    manifest_by_id = {row["gameItemId"]: row for row in manifest}
    for row in sorted(long_rows, key=lambda value: value["gameItemId"]):
        game_item_id = row["gameItemId"]
        if production_names.get(game_item_id) != row["name"]:
            raise ValueError(f"Long Sword closure icon name differs from production TMO name: {game_item_id}")
        if game_item_id in manifest_by_id:
            raise ValueError(f"Long Sword closure changes an existing icon identity: {game_item_id}")
        cell = row["atlas"]
        normalized_cell = {key: cell[key] for key in ("sheet", "x", "y", "width", "height")}
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Long Sword atlas SHA mismatch: {game_item_id}")
        source_sheet = Image.open(atlas_path).convert("RGBA")
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"]))
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Long Sword raw pixel SHA mismatch: {game_item_id}")
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != normalized_cell[key] for key in normalized_cell):
                raise ValueError(f"Long Sword icon key changes an accepted cell: {final_icon_key}")
        else:
            resolved_by_key[final_icon_key] = {
                "iconKey": final_icon_key,
                "resourceIconKey": final_icon_key,
                **normalized_cell,
            }
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": row["name"],
            "iconKey": final_icon_key,
        })
        manifest_by_id[game_item_id] = manifest[-1]

    # Iteration 4.11A.1-v2 closes the six conventional-melee recipe Item
    # identities. Shared atlas cells intentionally reuse one drawable.
    conventional_closure = load(CONVENTIONAL_MELEE_CLOSURE_PATH)
    conventional_rows = conventional_closure["items"]
    if len(conventional_rows) != 6 or len({row["gameItemId"] for row in conventional_rows}) != 6:
        raise ValueError("Conventional melee Item icon closure must contain exactly six unique mappings")
    if conventional_closure.get("source", {}).get("sourceGitBlobSha") != "1548e321a8ac2da7aa31334b919a43052f9c26dd":
        raise ValueError("Conventional melee closure sprite_data.js blob pin changed")
    manifest_by_id = {row["gameItemId"]: row for row in manifest}
    for row in sorted(conventional_rows, key=lambda value: value["gameItemId"]):
        game_item_id = row["gameItemId"]
        name = production_names.get(game_item_id)
        if name != row["name"]:
            raise ValueError(f"Conventional melee closure icon name differs from production TMO name: {game_item_id}")
        if game_item_id in manifest_by_id:
            raise ValueError(f"Conventional melee closure changes an existing icon identity: {game_item_id}")
        cell = row["atlas"]
        normalized_cell = {key: cell[key] for key in ("sheet", "x", "y", "width", "height")}
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Conventional melee atlas SHA mismatch: {game_item_id}")
        source_sheet = Image.open(atlas_path).convert("RGBA")
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"]))
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Conventional melee raw pixel SHA mismatch: {game_item_id}")
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != normalized_cell[key] for key in normalized_cell):
                raise ValueError(f"Conventional melee icon key changes an accepted cell: {final_icon_key}")
        else:
            cell_key = tuple(normalized_cell[key] for key in ("sheet", "x", "y", "width", "height"))
            resource_icon_key = cell_to_resource_key.get(cell_key, final_icon_key)
            resolved_by_key[final_icon_key] = {
                "iconKey": final_icon_key,
                "resourceIconKey": resource_icon_key,
                **normalized_cell,
            }
            cell_to_resource_key.setdefault(cell_key, resource_icon_key)
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": name,
            "iconKey": final_icon_key,
        })
        manifest_by_id[game_item_id] = manifest[-1]

    # Iteration 4.12A.1 closes the six special-melee recipe Item identities.
    # Resolve each exact sprite_data cell, reusing an existing drawable when
    # the pinned atlas cell is already represented by another Item identity.
    special_closure = load(SPECIAL_MELEE_CLOSURE_PATH)
    special_rows = special_closure["items"]
    if len(special_rows) != 6 or [row["gameItemId"] for row in special_rows] != [74, 629, 633, 634, 635, 637]:
        raise ValueError("Special-melee Item closure must contain the six frozen IDs in numeric order")
    if special_closure.get("source", {}).get("sourceGitBlobSha") != "1548e321a8ac2da7aa31334b919a43052f9c26dd":
        raise ValueError("Special-melee closure sprite_data.js blob pin changed")
    manifest_by_id = {row["gameItemId"]: row for row in manifest}
    for row in special_rows:
        game_item_id = row["gameItemId"]
        name = production_names.get(game_item_id)
        if name != row["name"]:
            raise ValueError(f"Special-melee closure icon name differs from production TMO name: {game_item_id}")
        if game_item_id in manifest_by_id:
            raise ValueError(f"Special-melee closure changes an existing icon identity: {game_item_id}")
        cell = row["atlas"]
        normalized_cell = {key: cell[key] for key in ("sheet", "x", "y", "width", "height")}
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Special-melee atlas SHA mismatch: {game_item_id}")
        source_sheet = Image.open(atlas_path).convert("RGBA")
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"]))
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Special-melee raw pixel SHA mismatch: {game_item_id}")
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != normalized_cell[key] for key in normalized_cell):
                raise ValueError(f"Special-melee icon key changes an accepted cell: {final_icon_key}")
        else:
            cell_key = tuple(normalized_cell[key] for key in ("sheet", "x", "y", "width", "height"))
            resource_icon_key = cell_to_resource_key.get(cell_key, final_icon_key)
            resolved_by_key[final_icon_key] = {
                "iconKey": final_icon_key,
                "resourceIconKey": resource_icon_key,
                **normalized_cell,
            }
            cell_to_resource_key.setdefault(cell_key, resource_icon_key)
        manifest.append({
            "scope": "material",
            "gameItemId": game_item_id,
            "name": name,
            "iconKey": final_icon_key,
        })
        manifest_by_id[game_item_id] = manifest[-1]

    # Iteration 4.15A.1 closes the two bowgun recipe Item identities.  This
    # stage deliberately runs before any future bowgun weapon integration and
    # reuses an existing drawable when the exact source cell is shared.
    bowgun_closure = load(BOWGUN_CLOSURE_PATH)
    bowgun_rows = bowgun_closure["items"]
    if [row["gameItemId"] for row in bowgun_rows] != [610, 618]:
        raise ValueError("Bowgun Item icon closure must contain IDs 610 and 618 in order")
    if bowgun_closure.get("source", {}).get("sourceGitBlobSha") != "1548e321a8ac2da7aa31334b919a43052f9c26dd":
        raise ValueError("Bowgun closure sprite_data.js blob pin changed")
    manifest_by_id = {row["gameItemId"]: row for row in manifest}
    for row in bowgun_rows:
        game_item_id = row["gameItemId"]
        name = production_names.get(game_item_id)
        if name != row["name"]:
            raise ValueError(f"Bowgun closure icon name differs from production TMO name: {game_item_id}")
        if game_item_id in manifest_by_id:
            raise ValueError(f"Bowgun closure changes an existing icon identity: {game_item_id}")
        cell = row["atlas"]
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Bowgun atlas SHA mismatch: {game_item_id}")
        source_sheet = Image.open(atlas_path).convert("RGBA")
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"])).convert("RGBA")
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Bowgun raw pixel SHA mismatch: {game_item_id}")
        final_icon_key = row["iconKey"]
        prior = resolved_by_key.get(final_icon_key)
        if prior is not None:
            if any(prior[key] != cell[key] for key in ("sheet", "x", "y", "width", "height")):
                raise ValueError(f"Bowgun icon key changes an accepted cell: {final_icon_key}")
        else:
            cell_key = tuple(cell[key] for key in ("sheet", "x", "y", "width", "height"))
            resource_icon_key = cell_to_resource_key.get(cell_key, row.get("resourceIconKey", final_icon_key))
            resolved_by_key[final_icon_key] = {"iconKey": final_icon_key, "resourceIconKey": resource_icon_key, **{key: cell[key] for key in ("sheet", "x", "y", "width", "height")}}
            cell_to_resource_key.setdefault(cell_key, resource_icon_key)
        manifest.append({"scope": "material", "gameItemId": game_item_id, "name": name, "iconKey": final_icon_key})
        manifest_by_id[game_item_id] = manifest[-1]

    resolved = list(resolved_by_key.values())
    if len(manifest) != 561 or len({row["gameItemId"] for row in manifest}) != 561:
        raise ValueError("Expected 561 final ItemIconRegistry identities")
    if any(row["width"] != 16 or row["height"] != 16 for row in resolved):
        raise ValueError("All source cells must remain exact 16x16 crops")
    keys = {row["iconKey"] for row in resolved}
    if any(row["iconKey"] not in keys for row in manifest):
        raise ValueError("Manifest refers to an unresolved icon key")
    return manifest, resolved


def unique_resource_rows(resolved: list[dict]) -> list[dict]:
    resources = {}
    for row in resolved:
        key = row.get("resourceIconKey", row["iconKey"])
        normalized = {"iconKey": key, **{field: row[field] for field in ("sheet", "x", "y", "width", "height")}}
        prior = resources.get(key)
        if prior is not None and prior != normalized:
            raise ValueError(f"Shared drawable key resolves to multiple source cells: {key}")
        resources[key] = normalized
    return list(resources.values())


def write_assets(resolved: list[dict]) -> None:
    DRAWABLES.mkdir(parents=True, exist_ok=True)
    for stale in DRAWABLES.glob("item_icon_*.png"):
        stale.unlink()
    sheets: dict[str, Image.Image] = {}
    resources = unique_resource_rows(resolved)
    for row in resources:
        if row["sheet"] not in sheets:
            sheets[row["sheet"]] = Image.open(atlas_source_path(row["sheet"])).convert("RGBA")
        sheet = sheets[row["sheet"]]
        x, y = row["x"], row["y"]
        crop = sheet.crop((x, y, x + row["width"], y + row["height"]))
        output = DRAWABLES / f"{resource_name(row['iconKey'])}.png"
        crop.save(output)
        with Image.open(output) as generated:
            if generated.convert("RGBA").tobytes() != crop.tobytes():
                raise ValueError(f"Generated bitmap pixels changed for {row['iconKey']}")
    for row in load(CLOSURE_PATH)["items"]:
        cell = row["atlas"]
        sheet = sheets.setdefault(cell["sheet"], Image.open(atlas_source_path(cell["sheet"])).convert("RGBA"))
        source_crop = sheet.crop((cell["x"], cell["y"], cell["x"] + 16, cell["y"] + 16)).convert("RGBA")
        reference_path = CLOSURE_CROPS / Path(row["cropReference"]["file"]).name
        with Image.open(reference_path) as reference:
            if reference.size != (16, 16) or reference.convert("RGBA").tobytes() != source_crop.tobytes():
                raise ValueError(f"Closure crop differs from pinned source pixels: {row['gameItemId']}")
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["cropReference"]["pixelSha256"]:
            raise ValueError(f"Closure raw pixel SHA mismatch: {row['gameItemId']}")
    # Great Sword recipe-item closure cells use the same pinned atlas pipeline.
    # Verify both the immutable source sheet and the generated drawable pixels;
    # this prevents a visually similar or recompressed replacement from being
    # silently accepted.
    resolved_by_icon = {row["iconKey"]: row for row in resolved}
    for row in load(GREAT_SWORD_CLOSURE_PATH)["items"]:
        cell = row["atlas"]
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Great Sword atlas SHA mismatch: {row['gameItemId']}")
        source_sheet = sheets.setdefault(cell["sheet"], Image.open(atlas_path).convert("RGBA"))
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + 16, cell["y"] + 16)).convert("RGBA")
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Great Sword raw pixel SHA mismatch: {row['gameItemId']}")
        resolved_row = resolved_by_icon.get(row["iconKey"])
        if resolved_row is None:
            raise ValueError(f"Great Sword icon key is not resolved: {row['iconKey']}")
        output_path = DRAWABLES / f"{resource_name(resolved_row.get('resourceIconKey', row['iconKey']))}.png"
        if not output_path.exists():
            raise ValueError(f"Great Sword drawable is missing: {row['gameItemId']}")
        with Image.open(output_path) as generated:
            if generated.size != (16, 16) or generated.convert("RGBA").tobytes() != source_crop.tobytes():
                raise ValueError(f"Great Sword drawable differs from pinned source pixels: {row['gameItemId']}")
    for row in load(CONVENTIONAL_MELEE_CLOSURE_PATH)["items"]:
        cell = row["atlas"]
        atlas_path = atlas_source_path(cell["sheet"])
        source_sheet = sheets.setdefault(cell["sheet"], Image.open(atlas_path).convert("RGBA"))
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"])).convert("RGBA")
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Conventional melee raw pixel SHA mismatch: {row['gameItemId']}")
        resolved_row = resolved_by_icon.get(row["iconKey"])
        if resolved_row is None:
            raise ValueError(f"Conventional melee icon key is not resolved: {row['iconKey']}")
        output_path = DRAWABLES / f"{resource_name(resolved_row.get('resourceIconKey', row['iconKey']))}.png"
        if not output_path.exists():
            raise ValueError(f"Conventional melee drawable is missing: {row['gameItemId']}")
        with Image.open(output_path) as generated:
            if generated.size != (16, 16) or generated.convert("RGBA").tobytes() != source_crop.tobytes():
                raise ValueError(f"Conventional melee drawable differs from pinned source pixels: {row['gameItemId']}")
    # Special-melee recipe-item closure uses the same exact pinned atlas-cell
    # pipeline.  Shared cells (for example Dengeki/Famitsu G tickets) reuse a
    # single drawable without changing their numeric Item identities.
    for row in load(SPECIAL_MELEE_CLOSURE_PATH)["items"]:
        cell = row["atlas"]
        atlas_path = atlas_source_path(cell["sheet"])
        atlas_sha = hashlib.sha256(atlas_path.read_bytes()).hexdigest().upper()
        if atlas_sha != row["atlasSha256"]:
            raise ValueError(f"Special-melee atlas SHA mismatch: {row['gameItemId']}")
        source_sheet = sheets.setdefault(cell["sheet"], Image.open(atlas_path).convert("RGBA"))
        source_crop = source_sheet.crop((cell["x"], cell["y"], cell["x"] + cell["width"], cell["y"] + cell["height"])).convert("RGBA")
        pixel_sha = hashlib.sha256(source_crop.tobytes()).hexdigest().upper()
        if pixel_sha != row["pixelSha256"]:
            raise ValueError(f"Special-melee raw pixel SHA mismatch: {row['gameItemId']}")
        resolved_row = resolved_by_icon.get(row["iconKey"])
        if resolved_row is None:
            raise ValueError(f"Special-melee icon key is not resolved: {row['iconKey']}")
        output_path = DRAWABLES / f"{resource_name(resolved_row.get('resourceIconKey', row['iconKey']))}.png"
        if not output_path.exists():
            raise ValueError(f"Special-melee drawable is missing: {row['gameItemId']}")
        with Image.open(output_path) as generated:
            if generated.size != (16, 16) or generated.convert("RGBA").tobytes() != source_crop.tobytes():
                raise ValueError(f"Special-melee drawable differs from pinned source pixels: {row['gameItemId']}")
    generated = list(DRAWABLES.glob("item_icon_*.png"))
    sizes = []
    for path in generated:
        with Image.open(path) as image:
            sizes.append(image.size)
    if len(generated) != len(resources) or any(size != (16, 16) for size in sizes):
        raise ValueError(f"Expected exactly {len(resources)} generated 16x16 bitmap resources")


def write_kotlin(manifest: list[dict], resolved: list[dict]) -> None:
    mapping_rows = "\n".join(
        f'        {row["gameItemId"]} to "{row["iconKey"]}",' for row in sorted(manifest, key=lambda value: value["gameItemId"])
    )
    resource_rows = "\n".join(
        f'        "{row["iconKey"]}" to R.drawable.{resource_name(row.get("resourceIconKey", row["iconKey"]))},' for row in sorted(resolved, key=lambda value: value["iconKey"])
    )
    content = f'''package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes

data class ItemIconRef(val iconKey: String, @param:DrawableRes val resourceId: Int)

/** Generated from the audited item-icon manifest. Runtime identity is gameItemId only. */
object ItemIconRegistry {{
    private val iconKeysByGameItemId = mapOf(
{mapping_rows}
    )

    private val resourcesByIconKey = mapOf(
{resource_rows}
    )

    val mappingCount: Int get() = iconKeysByGameItemId.size
    val uniqueIconKeyCount: Int get() = resourcesByIconKey.size
    val uniqueResourceCount: Int get() = {len(unique_resource_rows(resolved))}
    val productionGameItemIds: Set<Int> get() = iconKeysByGameItemId.keys

    fun resolve(gameItemId: Int): ItemIconRef? {{
        val iconKey = iconKeysByGameItemId[gameItemId] ?: return null
        val resourceId = resourcesByIconKey[iconKey] ?: return null
        return ItemIconRef(iconKey, resourceId)
    }}
}}
'''
    KOTLIN_PATH.write_text(content, encoding="utf-8", newline="\n")


def run() -> None:
    fetch_sources()
    manifest, resolved = resolved_icons()
    resources = unique_resource_rows(resolved)
    write_assets(resolved)
    write_kotlin(manifest, resolved)
    baseline = load(MANIFEST_PATH)["items"]
    baseline_keys = {row["iconKey"] for row in baseline}
    extension_ids = {row["gameItemId"] for row in load(EXTENSION_PATH)["items"]}
    great_rows = load(GREAT_SWORD_CLOSURE_PATH)["items"]
    great_ids = {row["gameItemId"] for row in great_rows}
    baseline_ids = {row["gameItemId"] for row in baseline}
    great_reused = sorted(great_ids & baseline_ids)
    great_new_materials = sorted(great_ids - baseline_ids)
    great_keys = {row["iconKey"] for row in great_rows}
    great_baseline_keys = baseline_keys | {
        row["iconKey"] for row in load(CLOSURE_PATH)["items"]
    }
    long_rows = load(LONG_SWORD_CLOSURE_PATH)["items"]
    conventional_rows = load(CONVENTIONAL_MELEE_CLOSURE_PATH)["items"]
    conventional_keys = {row["iconKey"] for row in conventional_rows}
    keys_before_conventional = great_baseline_keys | great_keys | {row["iconKey"] for row in long_rows}
    special_rows = load(SPECIAL_MELEE_CLOSURE_PATH)["items"]
    special_keys = {row["iconKey"] for row in special_rows}
    keys_before_special = keys_before_conventional | conventional_keys
    bowgun_rows = load(BOWGUN_CLOSURE_PATH)["items"]
    bowgun_keys = {row["iconKey"] for row in bowgun_rows}
    keys_before_bowgun = keys_before_special | special_keys
    report = {
        "mappingCount": len(manifest),
        "newRegistryIdentities": len(manifest) - len(baseline),
        "uniqueIconResources": len(resources),
        "newUniqueDrawables": len({row["iconKey"] for row in manifest if row["gameItemId"] in extension_ids} - baseline_keys),
        "extensionTargets": len(extension_ids),
        "extensionReusingExistingIdentity": sorted(extension_ids & {row["gameItemId"] for row in baseline}),
        "extensionReusingExistingDrawables": sum(
            row["gameItemId"] in extension_ids and row["iconKey"] in baseline_keys for row in manifest
        ),
        "resolvedSharedKeys": 35,
        "unresolvedSharedKeys": [],
        "smallMonsterClosure": {
            "mappingCountBefore": 509,
            "mappingCountAfter": len(manifest),
            "newMappings": 27,
            "targetUniqueIconKeys": 22,
            "existingResourceReuses": 22 - (len(resources) - 186),
            "newUniqueDrawables": len(resources) - 186,
            "finalUniqueResourceCount": len(resources),
            "unresolved": 0,
            "old509MappingsUnchanged": True,
            "targetGameItemIds": sorted(row["gameItemId"] for row in load(CLOSURE_PATH)["items"]),
            "referenceCropsPixelEqualToPinnedSource": True,
        },
        "greatSwordClosure": {
            "mappingCount": len(great_rows),
            "reusedExistingMappings": great_reused,
            "newMaterialMappings": great_new_materials,
            "uniqueIconKeys": len(great_keys),
            "newUniqueIconKeys": len(great_keys - great_baseline_keys),
            "unresolved": 0,
            "sourcePixelChecks": True,
            "targetGameItemIds": sorted(great_ids),
        },
        "longSwordClosure": {
            "mappingCount": len(long_rows),
            "targetGameItemIds": sorted(row["gameItemId"] for row in long_rows),
            "newUniqueIconKeys": len({row["iconKey"] for row in long_rows} - great_baseline_keys),
            "unresolved": 0,
            "sourcePixelChecks": True,
            "sourceGitBlobSha": load(LONG_SWORD_CLOSURE_PATH)["source"]["sourceGitBlobSha"],
        },
        "conventionalMeleeClosure": {
            "mappingCount": len(conventional_rows),
            "targetGameItemIds": sorted(row["gameItemId"] for row in conventional_rows),
            "newUniqueIconKeys": len(conventional_keys - keys_before_conventional),
            "unresolved": 0,
            "sourcePixelChecks": True,
            "sourceGitBlobSha": load(CONVENTIONAL_MELEE_CLOSURE_PATH)["source"]["sourceGitBlobSha"],
            "sharedCells": {
                "PAL_0:144:32": [31],
                "PAL_9:96:48": [621, 628],
                "PAL_12:96:48": [611, 632],
            },
        },
        "specialMeleeClosure": {
            "mappingCount": len(special_rows),
            "targetGameItemIds": sorted(row["gameItemId"] for row in special_rows),
            "newUniqueIconKeys": len(special_keys - keys_before_special),
            "reusedExistingIconKeys": sorted(special_keys & keys_before_special),
            "unresolved": 0,
            "sourcePixelChecks": True,
            "sourceGitBlobSha": load(SPECIAL_MELEE_CLOSURE_PATH)["source"]["sourceGitBlobSha"],
            "sharedCells": {
                "PAL_10:96:48": [634, 637],
                "PAL_9:144:32": [74],
            },
        },
        "bowgunItemClosure": {
            "mappingCount": len(bowgun_rows),
            "targetGameItemIds": sorted(row["gameItemId"] for row in bowgun_rows),
            "newUniqueIconKeys": len(bowgun_keys - keys_before_bowgun),
            "reusedExistingIconKeys": sorted(bowgun_keys & keys_before_bowgun),
            "unresolved": 0,
            "sourcePixelChecks": True,
            "sourceGitBlobSha": load(BOWGUN_CLOSURE_PATH)["source"]["sourceGitBlobSha"],
            "hotSpringTcktReusesGameItemId": 623,
        },
    }
    REPORT_PATH.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    CLOSURE_REPORT_PATH.write_text(
        json.dumps(report["smallMonsterClosure"], indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    GREAT_SWORD_CLOSURE_REPORT_PATH.write_text(
        json.dumps(report["greatSwordClosure"], indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"Generated {len(manifest)} gameItemId mappings -> {len(resources)} exact shared 16x16 resources")


if __name__ == "__main__":
    run()
