# Data sources and credits

MHP3rd Companion ships a reviewed snapshot of structured game reference data. The public repository includes the accepted production data and current integrity checks, but not the private research archive or every intermediate file used to prepare that snapshot.

The project consulted these community sources:

- [VioletKIRA's Monster Data Guide](https://gamefaqs.gamespot.com/psp/991479-monster-hunter-portable-3rd/faqs/61490) for structured monster, hitzone, and reward-table information.
- [MHP3@Wiki](https://w.atwiki.jp/mhp3/) and [MHP3.net](https://www.mhp3rd.net/book/index.html) for cross-checking mechanics, item effects, gathering, and farm information.
- [Monster Hunter Wiki](https://monsterhunter.fandom.com/wiki/Category:MHP3_Database) for roster, English naming, classes, and quest reference information.
- Team Maverick ONE's English translation vocabulary, from a user-supplied TMO PSP v6.1 revision 754 data file. That source file is not included in this repository.

The item-icon helper refers to palette sheets from the pinned `mhp3db/mhp3db.github.io` commit `7ad3cf1c5ba07ce2e63afdaa1fa37c4671edc3bf`. The full sheets are not included; the application contains only its current small runtime icon resources. The helper verifies source hashes if those sheets are fetched for maintenance. No license or ownership claim is made for third-party material.

These sources are credited for provenance and cross-checking. This document does not grant permission to redistribute their content. Source prose, original community artwork, and the supplied game data file are not bundled here. Third-party names, translations, data, trademarks, and game-derived visuals remain subject to their respective owners and source terms; see [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## Historical Timeworn Charm Tables (1.1 additive data)

The separate `mhp3rd-charms.json` asset is generated from the **historical** `mikejsavage/MHP3DB` repository at commit [`bfbab18460d95d921d4a3492644b46bfc5ca1f3c`](https://github.com/mikejsavage/MHP3DB/tree/bfbab18460d95d921d4a3492644b46bfc5ca1f3c). The audited upstream `charms.csv` SHA-256 is `470D807A054A1585FBB84F21E7CCCAC3A6996FBF74D86A6981A80B9FF14EEDEB`. The checked-in, deliberately filtered source contains only the 64,800 standard Timeworn states in Tables 1–12; its SHA-256 is recorded in `data/sources/mhp3db-timeworn/README.md` and verified by the deterministic generator. Tables 13–17 and all other charm categories are not included in this app corpus.

The source repository's `license.txt` (WTFPL v2) is preserved alongside the filtered extract. The original license notice is not a claim that MHP3DB independently owns every game-derived fact in the dataset, and it does not relicense third-party material under this app's MIT license. The canonical skill crosswalk and Japanese identity bridge for the historical `Thundr Atk` label are documented with the source files. MHP3DB is a community database, not an official CAPCOM source.
