# Historical charm skill identity bridge

The filtered Timeworn corpus uses 61 historical MHP3DB skill labels. Each label maps one-to-one to a distinct existing Companion `stableSkillTreeId`; the checked-in `skill-identity-crosswalk.csv` is the machine-readable bridge used by the generator. The other 60 labels have exact canonical identity entries in the completed alias audit, and the generator revalidates their canonical IDs and TMO labels against the production skill-tree data before generating the additive asset.

## `Thundr Atk` → `mhp3_skill_tree_090`

The historical label is not resolved by fuzzy English spelling. The audit aligned 2,254 historical `Thundr Atk` rows to current `ThundrAtk` rows, with equal skill points on all 2,254 pairs and no competing mapping. The canonical production skill tree has:

- `stableSkillTreeId`: `mhp3_skill_tree_090`
- exact Japanese name: `雷属性攻撃`
- TMO display label: `ThunderAtk`
- canonical display name: `Thunder Attack`

The exact Japanese skill-tree identity plus the source/TMO identity `ThunderAtk` establish the bridge. The generator fails closed unless this exact Japanese name and TMO label still resolve uniquely to that stable ID. A regression test checks the evidence and the generated crosswalk entry.

No English edit-distance, prefix, or fuzzy alias matching is used by generation or runtime search.
