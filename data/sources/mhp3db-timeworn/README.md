# MHP3DB historical Timeworn source extract

This directory is the reproducible, filtered source for the additive 1.1 charm asset. It contains no Mystery, Block A/Shining, community-curated, or Tables 13–17 records.

- Repository: <https://github.com/mikejsavage/MHP3DB>
- Historical commit: `bfbab18460d95d921d4a3492644b46bfc5ca1f3c`
- Audited upstream `charms.csv` SHA-256: `470D807A054A1585FBB84F21E7CCCAC3A6996FBF74D86A6981A80B9FF14EEDEB`
- Checked-in filtered CSV: `timeworn-charms-tables-1-12.csv`
- Filtered CSV SHA-256: `86F8296AE0DA68045791ED4448F335D83DCAB9D858DC44266CB606F845876466`
- Scope: exactly 64,800 `Timeworn Charm` source rows, Tables 1–12, exactly 5,400 rows per table
- Canonical skill crosswalk SHA-256: `6AF4F24E3FF7E7D8AFF5BAA5597F7B084319E97AB3730B92BD7B9DAD28B34A32`
- License notice: [license.txt](license.txt), WTFPL v2 from the historical repository

The filtered CSV preserves each upstream row and source `ID` in source order; it does not deduplicate states. `data-pipeline/generate_timeworn_charms.py` checks the upstream historical checksum when regenerating this extract, checks the checked-in extract/crosswalk hashes when building, and emits identical canonical and Android assets.

The full upstream CSV had 65,362 Timeworn rows; its 562 rows outside Tables 1–12 are intentionally absent from this checked-in corpus and generated asset. The source repository's license file is retained as provenance, not as a claim that MHP3DB independently owns all game-derived facts. No third-party data is relicensed under the Companion MIT license.

See [IDENTITY-BRIDGE.md](IDENTITY-BRIDGE.md) for the canonical skill mapping, including the exact Japanese/source identity evidence for `Thundr Atk`.
