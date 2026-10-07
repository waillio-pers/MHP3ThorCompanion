package com.waillio.mhp3rdcompanion.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import com.waillio.mhp3rdcompanion.MapSourceCrosswalk

private val Context.dataStore by preferencesDataStore("companion_preferences")
private val fixtureJson = Json { ignoreUnknownKeys = true }

class CompanionRepository(private val context: Context) {
    private val generated: GeneratedDataset by lazy {
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        fixtureJson.decodeFromString(json)
    }
    val data: FixtureData by lazy { generated.toFixtureData() }
    val regularQuestSupplyItems: List<RegularQuestSupplyItem> by lazy { data.regularQuestSupplyItems }
    val regularQuestSupplyCoverage: List<RegularQuestSupplyCoverage> by lazy { data.regularQuestSupplyCoverage }
    val palicoExpeditions: List<PalicoExpedition> by lazy { data.palicoExpeditions }
    val palicoExpeditionRewardDrops: List<PalicoExpeditionRewardDrop> by lazy { data.palicoExpeditionRewardDrops }

    fun palicoExpeditionRewardsFor(expeditionId: String): List<PalicoExpeditionRewardDrop> =
        palicoExpeditionRewardDrops.filter { it.expeditionId == expeditionId }

    fun regularQuestSupplyForQuest(questId: String): List<RegularQuestSupplyItem> =
        regularQuestSupplyItems.filter { it.questId == questId }

    fun regularQuestSupplyCoverageForQuest(questId: String): RegularQuestSupplyCoverageStatus? =
        regularQuestSupplyCoverage.firstOrNull { it.questId == questId }?.status
    /** Built once from the generated source graph; Compose never scans gatheringDrops. */
    val fieldIndex: FieldSourceIndex by lazy {
        FieldSourceIndex.build(
            generated.gatheringNodes,
            generated.gatheringDrops,
            generated.items.mapNotNull { item -> item.gameItemId?.let { it to item.name } }.toMap(),
            MapSourceCrosswalk.sourceToMarkers,
            MapSourceCrosswalk.specialSourceNodeIds,
            MapSourceCrosswalk.specialSourceReasonByNodeId
        )
    }

    private val favoritesKey = stringPreferencesKey("favorites_v1")
    private val recentKey = stringPreferencesKey("recent_v1")
    val favorites: Flow<List<FavoriteEntry>> = context.dataStore.data.map {
        ReferenceEntryCodec.decodeFavorites(it[favoritesKey])
    }
    val recent: Flow<List<RecentEntry>> = context.dataStore.data.map {
        ReferenceEntryCodec.decodeRecent(it[recentKey])
    }

    suspend fun saveFavorites(entries: List<FavoriteEntry>) {
        context.dataStore.edit { it[favoritesKey] = ReferenceEntryCodec.encodeFavorites(entries) }
    }

    suspend fun saveRecent(entries: List<RecentEntry>) {
        context.dataStore.edit { it[recentKey] = ReferenceEntryCodec.encodeRecent(entries) }
    }

    /** Canonical TMO terminology lookup. Raw source text remains available on the models. */
    val displayTerminology: DisplayTerminology
        get() = data.displayTerminology

    fun trainingArmorDisplay(raw: String): String =
        displayTerminology.trainingArmorByRaw[raw] ?: raw

    fun trainingCharmDisplay(raw: String?): String? = raw?.let {
        displayTerminology.trainingCharmByRaw[it] ?: it
    }

    /** Resolve each atomic source skill while preserving the source order/separators. */
    fun trainingSkillsDisplay(raw: String?): String? = raw?.let { source ->
        source.split(Regex("[、，,/]+"))
            .map { token -> token.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" · ") { token -> displayTerminology.trainingSkillByRaw[token] ?: token }
            .ifBlank { source }
    }

    fun trainingObjectiveDisplay(stableId: String, fallback: String?): String? =
        displayTerminology.objectiveByStableId[stableId] ?: fallback?.takeIf { it.isNotBlank() }
}

private fun String.rewardLabel(): String = lowercase().split('_').joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
}

private fun String.rankLabel(): String = when (this) {
    "HIGH" -> "High Rank"
    "LOW" -> "Low Rank"
    "GUILD_1_2" -> "1–2★ Guild Hall"
    else -> replace('_', ' ')
}

private fun GeneratedRewardRank.rankLabel(): String = when (this) {
    GeneratedRewardRank.HIGH -> "High Rank"
    GeneratedRewardRank.LOW -> "Low Rank"
    GeneratedRewardRank.GUILD_1_2 -> "1–2★ Guild Hall"
    GeneratedRewardRank.VILLAGE_2_SPECIAL -> "Village 2★ Special"
}

private fun GeneratedQuest.rewardSourceRank(): RewardContext? = when (rank) {
    "LOW" -> RewardContext.LOW
    "HIGH" -> RewardContext.HIGH
    null -> null
    else -> error("Unsupported quest rank for reward source: $rank")
}

private fun GeneratedDataset.toFixtureData(): FixtureData {
    val monsterNames = monsters.associate { it.id to it.name }
    val smallMonsterNames = smallMonsters.associate { it.id to it.name }
    val itemNames = items.associate { it.id to it.name }
    val itemGameIds = items.associate { it.id to it.gameItemId }
    val itemNamesByGameItemId = items.mapNotNull { item ->
        item.gameItemId?.let { gameItemId -> gameItemId to item.name }
    }.toMap()
    val partsByMonster = breakableParts.groupBy { it.monsterId }
    val hitzonesByMonster = hitzones.groupBy { it.monsterId }
    val semanticsByMonster = hitzoneStateSemantics.groupBy { it.monsterId }
    val rewardsByMonster = rewards.groupBy { it.monsterId }
    val invasionRewardsByMonster = invasionRewardDrops.groupBy { it.monsterId }
    val smallRewardsByMonster = smallMonsterRewards.groupBy { it.smallMonsterId }
    val smallTipsByMonster = smallMonsterTips.groupBy { it.smallMonsterId }
    val directQuestIdsBySmallMonster = buildMap<String, MutableList<String>> {
        quests.forEach { quest ->
            quest.objectiveTargets
                .asSequence()
                .filter { it.entityKind == QuestTargetEntityKind.SMALL_MONSTER }
                .map { it.entityId }
                .distinct()
                .forEach { smallMonsterId -> getOrPut(smallMonsterId) { mutableListOf() }.add(quest.id) }
        }
    }
    val prepItemsById = huntPrepItemCatalog.associate { it.gameItemId to it.name }
    val huntPrepByMonster = monsterHuntPrep.associateBy { it.monsterId }
    val trainingQuestsForUi = trainingQuests.map { quest ->
        TrainingQuest(
            id = quest.id,
            trainingClass = quest.trainingClass,
            challengeSeries = quest.challengeSeries,
            canonicalDisplayName = quest.canonicalDisplayName,
            japaneseName = quest.japaneseName,
            tmoName = quest.tmoName,
            stars = quest.stars,
            location = quest.location,
            objective = quest.objective,
            objectiveTargets = quest.objectiveTargets.map { target ->
                TrainingObjectiveTarget(target.entityKind, target.entityId, target.count, target.countMode, target.objectiveType, target.gameItemId)
            },
            appearingMonsterIds = quest.appearingMonsterIds,
            participantLimit = quest.participantLimit,
            timeLimitSeconds = quest.timeLimitSeconds,
            suppliedItems = quest.suppliedItems.map { item ->
                TrainingSuppliedItem(item.gameItemId, item.quantity, item.quantitySemantics)
            },
            loadouts = quest.loadouts.map { loadout ->
                TrainingLoadout(
                    weaponType = loadout.weaponType,
                    weaponStableId = loadout.weaponStableId,
                    armorRaw = loadout.armorRaw,
                    charmRaw = loadout.charmRaw,
                    skillsRaw = loadout.skillsRaw,
                    suppliedItems = loadout.suppliedItems.map { item ->
                        TrainingSuppliedItem(item.gameItemId, item.quantity, item.quantitySemantics)
                    }
                )
            },
            resultGrades = TrainingResultGrades(
                provisional = quest.resultGrades.provisional,
                rows = quest.resultGrades.rows.map { grade -> TrainingGrade(grade.grade, grade.thresholdSeconds, grade.condition) }
            ),
            rewards = quest.rewards.map { reward ->
                TrainingReward(
                    id = reward.id,
                    gameItemId = reward.gameItemId,
                    quantity = reward.quantity,
                    quantitySemantics = reward.quantitySemantics,
                    probabilityValuePercent = reward.probability.valuePercent,
                    probabilitySemantics = reward.probability.semantics,
                    rewardType = reward.rewardType
                )
            }
        )
    }
    val itemCombinationRecipesForUi = itemCombinationRecipes.map { recipe ->
        ItemCombinationRecipe(
            recipeNumber = recipe.recipeNumber,
            outputGameItemId = recipe.outputGameItemId,
            ingredientAGameItemId = recipe.ingredientAGameItemId,
            ingredientBGameItemId = recipe.ingredientBGameItemId,
            baseSuccessPercent = recipe.baseSuccessPercent,
            outputQuantityMin = recipe.outputQuantityMin,
            outputQuantityMax = recipe.outputQuantityMax,
            ingredientAvailability = recipe.ingredientAvailability,
            successOverrideSourceNote = recipe.successOverrideSourceNote,
            sourceNote = recipe.sourceNote,
            sourceId = recipe.sourceId,
            sourceUrl = recipe.sourceUrl
        )
    }
    val itemCombinationFailuresForUi = itemCombinationFailureRelations.map { relation ->
        ItemCombinationFailureRelation(
            mechanism = relation.mechanism,
            outputGameItemId = relation.outputGameItemId,
            probabilitySemantics = relation.probabilitySemantics,
            sourceId = relation.sourceId,
            sourceUrl = relation.sourceUrl,
            sourceNote = relation.sourceNote
        )
    }
    val decorationCraftingRecipesForUi = decorationCraftingRecipes.map { recipe ->
        DecorationCraftingRecipe(
            id = recipe.id,
            outputGameItemId = recipe.outputGameItemId,
            rankContext = recipe.rankContext,
            ingredients = recipe.ingredients.map { ingredient ->
                DecorationCraftingIngredient(ingredient.gameItemId, ingredient.quantity, ingredient.quantitySemantics)
            }
        )
    }
    val monstersForUi = monsters.map { monster ->
        val generatedZones = hitzonesByMonster[monster.id].orEmpty()
        fun List<GeneratedHitzone>.toUiHitzones() = map {
            Hitzone(it.bodyPart, it.cut, it.impact, it.shot, it.fire, it.water, it.thunder, it.ice, it.dragon)
        }
        val zones = generatedZones.filter { it.state == "normal" }.toUiHitzones()
        val semantics = semanticsByMonster[monster.id].orEmpty().mapNotNull { semantic ->
            semantic.semanticState?.let {
                HitzoneStateSemantic(semantic.sourceState, it, it.semanticLabel())
            }
        }
        val alternateStates = generatedZones.filter { it.state != "normal" }.groupBy { it.state }.map { (state, values) ->
            val label = semantics.firstOrNull { it.sourceState == state }?.label ?: state.semanticLabel()
            AlternateHitzoneState(state, label, values.toUiHitzones())
        }
        val strongestParts = zones.sortedByDescending { maxOf(it.cut, it.impact, it.shot) }.take(3).map { it.part }
        val generatedPrep = huntPrepByMonster[monster.id]
        val huntPrep = generatedPrep?.let { prep ->
            MonsterHuntPrep(
                coverageStatus = when (prep.threatCoverageStatus) {
                    GeneratedThreatCoverageStatus.LISTED -> ThreatCoverageStatus.LISTED
                    GeneratedThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE -> ThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE
                },
                threats = prep.threats.map { threat ->
                    HunterThreat(threat.id, threat.type.toUiType(), threat.context)
                },
                counterItems = prep.counterItems.map { counter ->
                    HuntPrepCounterItem(
                        itemGameId = counter.itemGameId,
                        itemName = requireNotNull(prepItemsById[counter.itemGameId]) {
                            "Missing Hunt Prep catalog item ${counter.itemGameId}"
                        },
                        counterFor = counter.counterFor.map { it.toUiType() }
                    )
                },
                tacticalTools = prep.tacticalTools.map { tool ->
                    TacticalTool(
                        itemGameId = tool.itemGameId,
                        itemName = requireNotNull(prepItemsById[tool.itemGameId]) {
                            "Missing Hunt Prep catalog item ${tool.itemGameId}"
                        },
                        priority = when (tool.priority) {
                            GeneratedTacticalPriority.CORE -> TacticalPriority.CORE
                            GeneratedTacticalPriority.RECOMMENDED -> TacticalPriority.RECOMMENDED
                            GeneratedTacticalPriority.CONDITIONAL -> TacticalPriority.CONDITIONAL
                        },
                        reason = tool.reason
                    )
                }
            )
        }
        Monster(
            id = monster.id,
            name = monster.name,
            subtitle = monster.monsterClass.orEmpty(),
            type = monster.monsterClass.orEmpty(),
            threatLevel = monster.threat,
            weaknesses = ElementValues(
                fire = zones.maxOfOrNull { it.fire } ?: 0,
                water = zones.maxOfOrNull { it.water } ?: 0,
                thunder = zones.maxOfOrNull { it.thunder } ?: 0,
                ice = zones.maxOfOrNull { it.ice } ?: 0,
                dragon = zones.maxOfOrNull { it.dragon } ?: 0
            ),
            recommendedParts = strongestParts,
            breakableParts = partsByMonster[monster.id].orEmpty().map {
                BreakablePart(it.name, if (it.kind == "SEVERABLE") "Sever" else "Break")
            },
            rewards = rewardsByMonster[monster.id].orEmpty().map {
                MonsterReward(
                    item = itemNames[it.itemId].orEmpty(),
                    sourceType = it.method.rewardLabel(),
                    condition = it.condition,
                    chance = "${it.chance}%",
                    rank = RewardContext.valueOf(it.rank.name),
                    quantity = it.quantity,
                    gameItemId = itemGameIds[it.itemId],
                    relationId = it.id
                )
            },
            hitzones = zones,
            alternateHitzones = alternateStates,
            hitzoneStateSemantics = semantics,
            questIds = monster.questIds,
            huntPrep = huntPrep,
            trainingQuestIds = monster.trainingQuestIds,
            invasionRewards = invasionRewardsByMonster[monster.id].orEmpty().map { drop ->
                InvasionReward(
                    id = drop.id,
                    monsterId = drop.monsterId,
                    context = drop.context,
                    gameItemId = drop.gameItemId,
                    quantity = drop.quantity,
                    probabilityValuePercent = drop.probabilityValuePercent,
                    probabilitySemantics = drop.probabilitySemantics,
                    probabilityBand = InvasionProbabilityBand.valueOf(drop.probabilityBand.name)
                )
            }
        )
    }
    val questsForUi = quests.map { quest ->
        val targets = quest.objectiveTargets.filter { it.entityKind == QuestTargetEntityKind.LARGE_MONSTER }.mapNotNull { target ->
            monsterNames[target.entityId]?.let { name ->
                when {
                    target.countMode == QuestTargetCountMode.AT_LEAST -> "$name (at least ${target.count})"
                    target.count > 1 -> "$name ×${target.count}"
                    else -> name
                }
            }
        }
        Quest(
            id = quest.id,
            name = quest.name,
            subtitle = "${quest.category} · ${quest.stars}★ · ${quest.location.orEmpty()}",
            rank = quest.rank?.rankLabel().orEmpty(),
            location = quest.location.orEmpty(),
            target = targets.joinToString(", "),
            relevantReward = "",
            objective = quest.objective.orEmpty(),
            stars = quest.stars,
            category = quest.category,
            objectiveTargetMonsterIds = quest.objectiveTargets
                .filter { it.entityKind == QuestTargetEntityKind.LARGE_MONSTER }
                .map { it.entityId },
            objectiveTargetSmallMonsterIds = quest.objectiveTargets
                .filter { it.entityKind == QuestTargetEntityKind.SMALL_MONSTER }
                .map { it.entityId },
            canonicalNumber = quest.canonicalNumber
        )
    }
    val roastingConversionsForUi = roastingConversions.map { conversion ->
        RoastingConversion(
            id = conversion.id,
            context = conversion.context,
            inputGameItemId = conversion.inputGameItemId,
            inputQuantity = conversion.inputQuantity,
            outputGameItemId = conversion.outputGameItemId,
            outputQuantity = conversion.outputQuantity,
            resultState = conversion.resultState,
            resultSemantics = conversion.resultSemantics,
            probabilityValuePercent = conversion.probability?.valuePercent,
            supportedToolGameItemIds = conversion.supportedToolGameItemIds,
            facilityId = conversion.facilityId,
            batchCapacity = conversion.batchCapacity
        )
    }
    // Objective targets are a validation view only. The production questIds
    // array is the authoritative relation and also includes appearing-only
    // quests that are intentionally absent from objectiveTargets.
    smallMonsters.forEach { monster ->
        val direct = directQuestIdsBySmallMonster[monster.id].orEmpty()
        require(direct.all { it in monster.questIds }) {
            "Small monster questIds omit an objective-target quest: ${monster.id}"
        }
    }
    val smallMonstersForUi = smallMonsters.map { monster ->
        SmallMonster(
            id = monster.id,
            name = monster.name,
            monsterClass = monster.monsterClass,
            rewards = smallRewardsByMonster[monster.id].orEmpty().map { reward ->
                SmallMonsterReward(
                    id = reward.id,
                    context = SmallMonsterRewardContext.valueOf(reward.context.name),
                    method = SmallMonsterRewardMethod.valueOf(reward.method.name),
                    condition = reward.condition,
                    conditionDetails = reward.conditionDetails?.let {
                        SmallMonsterRewardConditionDetails(it.locations, it.wingColor, it.color)
                    },
                    rolls = reward.rolls,
                    gameItemId = reward.gameItemId,
                    itemName = reward.itemName,
                    chancePercent = reward.chancePercent,
                    quantity = reward.quantity
                )
            },
            tips = smallTipsByMonster[monster.id].orEmpty().map { tip ->
                SmallMonsterTip(tip.id, SmallMonsterTipKind.valueOf(tip.kind.name), tip.text)
            },
            relatedQuestIds = monster.questIds,
            trainingQuestIds = monster.trainingQuestIds
        )
    }.sortedBy { it.name.lowercase() }
    val rewardsByItem = rewards.groupBy { it.itemId }
    val invasionSourcesByGameItem = invasionRewardDrops.groupBy { it.gameItemId }.mapValues { (_, drops) ->
        drops.map { drop ->
            MaterialSource(
                id = drop.id,
                type = MaterialSourceType.INVASION_REWARD,
                name = monsterNames[drop.monsterId].orEmpty(),
                monsterId = drop.monsterId,
                context = drop.context,
                invasionContext = drop.context,
                method = "INVASION_REWARD",
                quantity = drop.quantity,
                probabilitySemantics = drop.probabilitySemantics,
                probabilityBand = InvasionProbabilityBand.valueOf(drop.probabilityBand.name)
            )
        }
    }
    val nodesById = gatheringNodes.associateBy { it.id }
    val facilitiesById = farmFacilities.associateBy { it.id }
    val farmProfilesById = farmProfiles.associateBy { it.id }
    val generatedQuestsById = quests.associateBy { it.id }
    val questsById = questsForUi.associateBy { it.id }
    val regularQuestSupplyItemsForUi = regularQuestSupplyItems.map { row ->
        RegularQuestSupplyItem(
            id = row.id,
            questId = row.questId,
            gameItemId = row.gameItemId,
            availabilityTiming = row.availabilityTiming.name,
            quantityValue = row.quantityValue,
            quantityNotation = row.quantityNotation,
            bundleCount = row.bundleCount,
            quantitySemantics = when (row.quantitySemantics) {
                GeneratedRegularQuestSupplyQuantitySemantics.FINITE_SINGLE_BUNDLE -> RegularQuestSupplyQuantitySemantics.FINITE_SINGLE_BUNDLE
                GeneratedRegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE -> RegularQuestSupplyQuantitySemantics.FINITE_MULTI_BUNDLE
                GeneratedRegularQuestSupplyQuantitySemantics.INFINITE -> RegularQuestSupplyQuantitySemantics.INFINITE
            },
            distributionSemantics = row.distributionSemantics,
            lifecycle = when (row.lifecycle) {
                GeneratedRegularQuestSupplyLifecycle.QUEST_LOCAL_SUPPLY_ONLY -> RegularQuestSupplyLifecycle.QUEST_LOCAL_SUPPLY_ONLY
                GeneratedRegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY -> RegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY
            },
            persistentAcquisition = row.persistentAcquisition
        )
    }
    val regularQuestSupplyCoverageForUi = regularQuestSupplyCoverage.map { row ->
        RegularQuestSupplyCoverage(
            questId = row.questId,
            status = when (row.status) {
                GeneratedRegularQuestSupplyCoverageStatus.DATA_PRESENT -> RegularQuestSupplyCoverageStatus.DATA_PRESENT
                GeneratedRegularQuestSupplyCoverageStatus.EXPLICIT_NO_SUPPLY -> RegularQuestSupplyCoverageStatus.EXPLICIT_NO_SUPPLY
                GeneratedRegularQuestSupplyCoverageStatus.SOURCE_UNAVAILABLE -> RegularQuestSupplyCoverageStatus.SOURCE_UNAVAILABLE
            }
        )
    }
    val palicoExpeditionsForUi = palicoExpeditions.map { expedition ->
        PalicoExpedition(
            id = expedition.id,
            starRank = expedition.starRank,
            canonicalName = expedition.canonicalName,
            dispatchPointsPerPalico = expedition.dispatchPointsPerPalico,
            unlockAnyOf = expedition.unlockCondition.unlockAnyOf.map { choice ->
                PalicoExpeditionUnlockChoice(choice.kind, choice.questId, choice.facilityId)
            },
            facilityUnlockAnyOf = expedition.unlockCondition.facilityUnlockAnyOf.map { choice ->
                PalicoExpeditionUnlockChoice(choice.kind, choice.questId, choice.facilityId)
            }
        )
    }
    val expeditionsById = palicoExpeditionsForUi.associateBy { it.id }
    val expeditionSourcesByGameItem = palicoExpeditionRewardDrops.groupBy { it.gameItemId }.mapValues { (_, drops) ->
        drops.map { drop ->
            val expedition = requireNotNull(expeditionsById[drop.expeditionId]) {
                "Missing Palico Expedition ${drop.expeditionId}"
            }
            MaterialSource(
                id = drop.id,
                type = MaterialSourceType.PALICO_EXPEDITION,
                name = expedition.canonicalName,
                context = "★${expedition.starRank} · ${expedition.dispatchPointsPerPalico} pts / Palico",
                method = when (drop.rewardCategory) {
                    "SMALL_MONSTER" -> "Small Monster"
                    "LARGE_MONSTER" -> "Large Monster"
                    "GATHERING" -> "Gathering"
                    else -> drop.rewardCategory.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
                },
                quantity = drop.quantity,
                expeditionId = drop.expeditionId,
                expeditionRewardCategory = drop.rewardCategory,
                expeditionStarRank = expedition.starRank,
                expeditionCostPerPalico = expedition.dispatchPointsPerPalico,
                probabilitySemantics = "NOT_PUBLISHED"
            )
        }
    }
    val gatheringSourcesByGameItem = gatheringDrops.withIndex().groupBy({ it.value.gameItemId }, { indexed ->
        val drop = indexed.value
        val node = requireNotNull(nodesById[drop.nodeId]) { "Missing gathering node ${drop.nodeId}" }
        MaterialSource(
            id = "material_field_${indexed.index.toString().padStart(5, '0')}",
            type = when (node.method) {
                GeneratedGatheringMethod.GATHERING -> MaterialSourceType.GATHERING
                GeneratedGatheringMethod.MINING -> MaterialSourceType.MINING
                GeneratedGatheringMethod.BUGNET -> MaterialSourceType.BUG
                GeneratedGatheringMethod.FISHING -> MaterialSourceType.FISHING
            },
            name = node.locationName,
            locationId = node.locationId,
            nodeId = node.id,
            rank = RewardContext.valueOf(drop.rankContext.name),
            context = structuralGatheringContext(node.id, node.area, node.pointIndex),
            method = node.method.name,
            chance = drop.probability.valuePercent,
            quantity = drop.quantity,
            quantityExplicit = drop.quantityExplicit
        )
    })
    val farmSourcesByGameItem = farmDrops.withIndex().groupBy({ it.value.gameItemId }, { indexed ->
        val drop = indexed.value
        val facility = requireNotNull(facilitiesById[drop.facilityId]) { "Missing farm facility ${drop.facilityId}" }
        MaterialSource(
            id = "material_farm_${indexed.index.toString().padStart(5, '0')}",
            type = MaterialSourceType.FARM,
            name = "Yukumo Farm",
            facilityId = facility.id,
            profileId = drop.profileId,
            profileTriggerKnown = drop.profileId?.let { farmProfilesById[it]?.triggerKnown },
            sourceOrdinal = indexed.index,
            inputGameItemId = drop.inputGameItemId,
            inputItemName = drop.inputGameItemId?.let(itemNamesByGameItemId::get),
            farmAction = farmActionLabel(facility, drop.inputGameItemId != null),
            method = "${facility.facilityType}_${facility.tier}",
            chance = drop.probability.valuePercent,
            quantity = drop.quantity,
            condition = drop.condition,
            probabilitySemantics = drop.probability.semantics.name,
            quantitySemantics = drop.quantitySemantics
        )
    })
    val questSourcesByGameItem = questRewardDrops.withIndex().groupBy({ it.value.gameItemId }, { indexed ->
        val drop = indexed.value
        val generatedQuest = requireNotNull(drop.questId?.let(generatedQuestsById::get)) {
            "Missing quest for quest reward source ${drop.questId}"
        }
        val quest = questsById[generatedQuest.id]
        MaterialSource(
            id = "material_quest_${indexed.index.toString().padStart(5, '0')}",
            type = MaterialSourceType.QUEST_REWARD,
            name = quest?.name ?: drop.sourceQuestKey,
            questId = drop.questId,
            rank = generatedQuest.rewardSourceRank(),
            context = drop.condition,
            method = drop.rewardPool.name,
            condition = drop.condition,
            chance = drop.probability.valuePercent,
            quantity = drop.quantity,
            probabilitySemantics = drop.probability.semantics.name,
            rewardPool = drop.rewardPool.name,
            // The generated sourceId identifies the source document rather than
            // an individual row; append the canonical array ordinal to retain
            // a stable raw relation identity without changing production data.
            rewardRelationId = "${drop.sourceId}:${indexed.index}"
        )
    })
    // Supply-box rows are a distinct acquisition mechanism. Only accepted
    // persistent rows participate in the Item reverse graph; quest-local
    // provisions remain available through the Quest Supply projection.
    val supplySourcesByGameItem = regularQuestSupplyItemsForUi
        .asSequence()
        .filter { it.persistentAcquisition }
        .groupBy { it.gameItemId }
        .mapValues { (_, rows) ->
            rows.groupBy { it.questId }.map { (questId, questRows) ->
                val quest = questsById[questId]
                MaterialSource(
                    id = "material_supply_${questId}_${questRows.first().gameItemId}",
                    type = MaterialSourceType.SUPPLY_BOX,
                    name = quest?.name ?: questId,
                    questId = questId,
                    method = "SUPPLY_BOX",
                    context = "Supply box layout",
                    sourceNote = questRows.joinToString(" · ") { row ->
                        when {
                            row.quantitySemantics == RegularQuestSupplyQuantitySemantics.INFINITE -> "∞"
                            row.quantityValue != null && (row.bundleCount ?: 1) > 1 -> "×${row.quantityValue} · ${row.bundleCount} bundles"
                            row.quantityValue != null -> "×${row.quantityValue}"
                            row.quantityNotation != null -> "×${row.quantityNotation}"
                            row.bundleCount != null && row.bundleCount > 1 -> "${row.bundleCount} bundles"
                            else -> "layout"
                        }
                    },
                    supplyQuantityValue = questRows.singleOrNull()?.quantityValue,
                    supplyQuantityNotation = questRows.singleOrNull()?.quantityNotation,
                    supplyBundleCount = questRows.singleOrNull()?.bundleCount,
                    supplyDistributionSemantics = questRows.singleOrNull()?.distributionSemantics,
                    supplyAvailabilityTiming = questRows.singleOrNull()?.availabilityTiming,
                    supplyLifecycle = RegularQuestSupplyLifecycle.PERSISTENT_NORMAL_SUPPLY,
                    supplyPersistentAcquisition = true,
                    supplyEntries = questRows.map { row ->
                        SupplyBoxEntry(
                            id = row.id,
                            gameItemId = row.gameItemId,
                            quantityValue = row.quantityValue,
                            quantityNotation = row.quantityNotation,
                            bundleCount = row.bundleCount,
                            quantitySemantics = row.quantitySemantics,
                            distributionSemantics = row.distributionSemantics,
                            availabilityTiming = row.availabilityTiming,
                            lifecycle = row.lifecycle,
                            persistentAcquisition = row.persistentAcquisition
                        )
                    }
                )
            }
        }
    val smallMonsterSourcesByGameItem = smallMonsterRewards.groupBy({ it.gameItemId }, { reward ->
        val detail = listOfNotNull(
            reward.condition?.rewardLabel(),
            reward.conditionDetails?.color?.let { "Color: ${it.rewardLabel()}" },
            reward.conditionDetails?.wingColor?.let { "Wing color: ${it.rewardLabel()}" },
            reward.conditionDetails?.locations?.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Locations: ")
        ).joinToString(" · ")
        MaterialSource(
            id = reward.id,
            type = MaterialSourceType.SMALL_MONSTER,
            name = smallMonsterNames[reward.smallMonsterId].orEmpty(),
            smallMonsterId = reward.smallMonsterId,
            rank = RewardContext.valueOf(reward.context.name),
            method = reward.method.name,
            condition = detail.ifBlank { null },
            conditionKey = reward.condition,
            conditionDetails = reward.conditionDetails?.let {
                MaterialSourceConditionDetails(it.locations, it.wingColor, it.color)
            },
            chance = reward.chancePercent,
            quantity = reward.quantity,
            rolls = reward.rolls,
            probabilitySemantics = reward.probabilitySemantics.name
        )
    })
    val trainingSourcesByGameItem = trainingQuestsForUi
        .withIndex()
        .flatMap { (trainingOrder, training) -> training.rewards.mapIndexed { sourceOrdinal, reward -> Triple(trainingOrder, sourceOrdinal, training to reward) } }
        .groupBy({ (_, _, trainingAndReward) -> trainingAndReward.second.gameItemId }, { (trainingOrder, sourceOrdinal, trainingAndReward) ->
            val training = trainingAndReward.first
            val reward = trainingAndReward.second
            MaterialSource(
                id = reward.id,
                type = MaterialSourceType.TRAINING_REWARD,
                name = training.canonicalDisplayName,
                trainingQuestId = training.id,
                trainingClass = training.trainingClass,
                trainingMissionOrder = trainingOrder,
                sourceOrdinal = sourceOrdinal + 1,
                method = reward.rewardType.takeIf { it.isNotBlank() },
                chance = reward.probabilityValuePercent,
                quantity = reward.quantity,
                quantitySemantics = reward.quantitySemantics,
                probabilitySemantics = reward.probabilitySemantics
            )
        })
    val combinationSourcesByGameItem = itemCombinationRecipesForUi
        .groupBy { it.outputGameItemId }
        .mapValues { (_, recipes) -> recipes.map { recipe ->
            MaterialSource(
                id = "material_combination_${recipe.recipeNumber.toString().padStart(3, '0')}",
                type = MaterialSourceType.COMBINATION,
                name = "Combine",
                method = "COMBINATION",
                recipeNumber = recipe.recipeNumber,
                ingredientAGameItemId = recipe.ingredientAGameItemId,
                ingredientAName = itemNamesByGameItemId[recipe.ingredientAGameItemId],
                ingredientBGameItemId = recipe.ingredientBGameItemId,
                ingredientBName = itemNamesByGameItemId[recipe.ingredientBGameItemId],
                baseSuccessPercent = recipe.baseSuccessPercent,
                outputQuantityMin = recipe.outputQuantityMin,
                outputQuantityMax = recipe.outputQuantityMax,
                ingredientAvailability = recipe.ingredientAvailability,
                successOverrideSourceNote = recipe.successOverrideSourceNote,
                sourceNote = recipe.sourceNote
            )
        }}
    val combinationFailureSourcesByGameItem = itemCombinationFailuresForUi
        .groupBy { it.outputGameItemId }
        .mapValues { (_, relations) -> relations.map { relation ->
            MaterialSource(
                id = "material_combination_failure_${relation.outputGameItemId}",
                type = MaterialSourceType.COMBINATION_FAILURE,
                name = "Failed combination",
                method = relation.mechanism,
                probabilitySemantics = relation.probabilitySemantics
            )
        }}
    val decorationSourcesByGameItem = buildMap<Int, MutableList<MaterialSource>> {
        decorationCraftingRecipesForUi.forEach { recipe ->
            recipe.ingredients.forEachIndexed { index, ingredient ->
                val ingredientName = itemNamesByGameItemId[ingredient.gameItemId] ?: "Item ${ingredient.gameItemId}"
                val outputName = itemNamesByGameItemId[recipe.outputGameItemId] ?: "Decoration ${recipe.outputGameItemId}"
                val sourceId = "${recipe.id}_ingredient_${index + 1}"
                // Output-side row: one row per ingredient, grouped by recipe in the UI.
                getOrPut(recipe.outputGameItemId) { mutableListOf() }.add(
                    MaterialSource(
                        id = sourceId,
                        type = MaterialSourceType.DECORATION_CRAFTING,
                        name = ingredientName,
                        context = recipe.rankContext.rankLabel(),
                        method = "DECORATION_CRAFTING",
                        quantity = ingredient.quantity,
                        quantitySemantics = ingredient.quantitySemantics,
                        sourceNote = outputName,
                        decorationRecipeId = recipe.id,
                        decorationOutputGameItemId = recipe.outputGameItemId,
                        decorationIngredientGameItemId = ingredient.gameItemId
                    )
                )
                // Reverse usage row: shown on the ingredient Item Detail and links back
                // to the decoration output through the common Item callback.
                getOrPut(ingredient.gameItemId) { mutableListOf() }.add(
                    MaterialSource(
                        id = "${sourceId}_usage",
                        type = MaterialSourceType.DECORATION_CRAFTING,
                        name = outputName,
                        context = recipe.rankContext.rankLabel(),
                        method = "DECORATION_CRAFTING",
                        quantity = ingredient.quantity,
                        quantitySemantics = ingredient.quantitySemantics,
                        sourceNote = ingredientName,
                        decorationRecipeId = recipe.id,
                        decorationOutputGameItemId = recipe.outputGameItemId,
                        decorationIngredientGameItemId = ingredient.gameItemId
                    )
                )
            }
        }
    }
    val shopProfilesById = itemShopPeddlerProfiles.associateBy { it.profileId }
    val shopConditionsById = itemShopUnlockConditions.associateBy { it.conditionId }
    val shopRelationsForUi = itemShopPurchaseRelations.map { relation ->
        ItemShopPurchaseRelation(
            id = relation.id,
            shopId = relation.shopId,
            inventoryProfileId = relation.inventoryProfileId,
            gameItemId = relation.gameItemId,
            priceZenny = relation.priceZenny,
            availabilityConditionId = relation.availabilityConditionId,
            sourceId = relation.sourceId,
            sourceUrl = relation.sourceUrl
        )
    }
    val shopSourcesByGameItem = shopRelationsForUi.groupBy { it.gameItemId }.mapValues { (_, relations) ->
        relations.map { relation ->
            val profile = relation.inventoryProfileId?.let(shopProfilesById::get)
            val condition = relation.availabilityConditionId?.let(shopConditionsById::get)
            MaterialSource(
                id = relation.id,
                type = MaterialSourceType.SHOP_PURCHASE,
                name = when (relation.shopId) {
                    "GENERAL_STORE" -> "General Store"
                    "HUNTERS_STORE" -> "Hunter's Store"
                    "PEDDLER" -> "Peddler"
                    else -> relation.shopId
                },
                shopId = relation.shopId,
                shopGameItemId = relation.gameItemId,
                shopInventoryProfileId = relation.inventoryProfileId,
                shopProfileSemantics = profile?.semantics,
                shopSelectionSemantics = profile?.selectionSemantics,
                priceZenny = relation.priceZenny,
                shopAvailabilityConditionId = relation.availabilityConditionId,
                shopAvailabilityConditionSemantics = condition?.semantics,
                condition = condition?.displayLabel,
                method = profile?.semantics,
                sourceUrl = relation.sourceUrl
            )
        }
    }
    val tradeConditionsById = itemTradeExchangeConditions.associateBy { it.conditionId }
    val tradeSourcesByGameItem = itemTradeExchangeRelations.groupBy { it.outputGameItemId }.mapValues { (_, relations) ->
        relations.map { relation ->
            val condition = relation.availabilityConditionId?.let(tradeConditionsById::get)
            val isElder = relation.mechanism == "VEGGIE_ELDER_ITEM_EXCHANGE"
            MaterialSource(
                id = relation.id,
                type = MaterialSourceType.TRADE,
                name = if (isElder) "Veggie Elder" else "Farm Manager",
                inputGameItemId = relation.inputGameItemId,
                inputItemName = relation.inputGameItemId?.let(itemNamesByGameItemId::get),
                context = if (isElder) tradeLocationContextLabel(relation.scopeType, relation.mapId, itemVeggieElderLocations) else null,
                method = relation.currencyType ?: "ITEM_EXCHANGE",
                condition = if (isElder) null else condition?.displayLabel,
                quantity = relation.outputQuantity,
                sourceUrl = relation.sourceUrl,
                tradeMechanism = relation.mechanism,
                tradeScopeType = relation.scopeType,
                tradeMapId = relation.mapId,
                tradeInputQuantity = relation.inputQuantity,
                tradeCurrencyType = relation.currencyType,
                tradeCurrencyCost = relation.currencyCost,
                tradeAvailabilityConditionId = relation.availabilityConditionId,
                tradeQuestContext = relation.questContext,
                tradeInteractionWindow = relation.interactionWindow,
                tradeSourceId = relation.sourceId
            )
        }
    }
    val specialFreeSourcesByGameItem = itemSpecialFreeAcquisitionRelations
        .groupBy { it.outputGameItemId }
        .mapValues { (_, relations) -> relations.map { relation ->
            val palicoTransition = Regex("PALICO_(SELF|GIFTED|RECEIVED)_(\\d+)_TO_(\\d+)")
                .find(relation.relationId)
            MaterialSource(
                id = relation.relationId,
                type = MaterialSourceType.SPECIAL_FREE,
                name = relation.giverId.orEmpty().ifBlank { "Special" },
                questId = relation.questId,
                context = relation.scopeType,
                method = relation.mechanism,
                condition = relation.conditionType,
                chance = relation.probabilityPercent,
                quantity = relation.outputQuantity,
                probabilitySemantics = relation.probabilitySemantics,
                quantitySemantics = relation.quantitySemantics,
                sourceUrl = relation.sourceUrl,
                specialFreeMechanism = relation.mechanism,
                specialFreeGiverId = relation.giverId,
                specialFreeTrigger = relation.triggerSemantics,
                specialFreeOutcomeSelection = relation.outcomeSelectionSemantics,
                specialFreeScopeType = relation.scopeType,
                specialFreeMapId = relation.mapId,
                specialFreeCompletionSetId = relation.completionSetId,
                specialFreeConditionType = relation.conditionType,
                specialFreeCounterType = relation.counterType,
                specialFreeInterval = relation.interval,
                specialFreeThresholdMin = relation.thresholdMin,
                specialFreeThresholdMax = relation.thresholdMax,
                specialFreeQuestContext = relation.questContext,
                specialFreeInteractionWindow = relation.interactionWindow,
                specialFreePalicoOrigin = palicoTransition?.groupValues?.getOrNull(1)?.let { if (it == "SELF") "SELF_HIRED" else "RECEIVED" },
                specialFreeAffectionFrom = palicoTransition?.groupValues?.getOrNull(2)?.toIntOrNull(),
                specialFreeAffectionTo = palicoTransition?.groupValues?.getOrNull(3)?.toIntOrNull(),
                specialFreeSourceDisagreement = relation.sourceDisagreement,
                specialFreeAccuracyReviewRequired = relation.accuracyReviewRequired
            )
        }}
    val scrapSourcesByGameItem = itemScrapConversionRules
        .groupBy { it.outputScrapGameItemId }
        .mapValues { (_, rules) -> rules.map { rule ->
            MaterialSource(
                id = scrapConversionRelationId(rule.inputGameItemId, rule.outputScrapGameItemId),
                type = MaterialSourceType.SCRAP_CONVERSION,
                // `MaterialSource.name` is the current material (the output item);
                // keep the consumed material in the dedicated input fields.
                name = itemNamesByGameItemId[rule.outputScrapGameItemId] ?: "Scrap ${rule.outputScrapGameItemId}",
                inputGameItemId = rule.inputGameItemId,
                inputItemName = itemNamesByGameItemId[rule.inputGameItemId],
                scrapOutputGameItemId = rule.outputScrapGameItemId,
                scrapInputQuantity = rule.inputQuantity,
                scrapTriggerModes = rule.triggerModes,
                method = "MATERIAL_CONVERSION",
                context = "Palico smith handoff or hunter gear crafting",
                quantity = rule.outputQuantity,
                quantitySemantics = rule.quantitySemantics,
                sourceUrl = rule.sourceUrl,
                sourceNote = rule.triggerModes.joinToString(" · ")
            )
        }}
    val roastingSourcesByGameItem = buildMap<Int, MutableList<MaterialSource>> {
        roastingConversionsForUi.forEach { conversion ->
            val contextLabel = roastingContextLabel(conversion.context)
            val inputName = itemNamesByGameItemId[conversion.inputGameItemId]
            val outputName = itemNamesByGameItemId[conversion.outputGameItemId]
            // Output-side acquisition row.
            getOrPut(conversion.outputGameItemId) { mutableListOf() }.add(
                MaterialSource(
                    id = "${conversion.id}_output",
                    type = MaterialSourceType.ROASTING,
                    name = contextLabel,
                    inputGameItemId = conversion.inputGameItemId,
                    inputItemName = inputName,
                    outputGameItemId = conversion.outputGameItemId,
                    outputItemName = outputName,
                    roastingInputQuantity = conversion.inputQuantity,
                    context = conversion.context,
                    method = conversion.resultState,
                    resultState = conversion.resultState,
                    resultSemantics = conversion.resultSemantics,
                    quantity = conversion.outputQuantity,
                    supportedToolGameItemIds = conversion.supportedToolGameItemIds,
                    facilityId = conversion.facilityId,
                    batchCapacity = conversion.batchCapacity,
                    probabilitySemantics = "NOT_PUBLISHED"
                )
            )
            // Input-side reverse usage row.  The Spit remains mechanism
            // metadata and is intentionally never projected as an ingredient.
            getOrPut(conversion.inputGameItemId) { mutableListOf() }.add(
                MaterialSource(
                    id = "${conversion.id}_usage",
                    type = MaterialSourceType.ROASTING,
                    name = contextLabel,
                    inputGameItemId = conversion.inputGameItemId,
                    inputItemName = inputName,
                    outputGameItemId = conversion.outputGameItemId,
                    outputItemName = outputName,
                    roastingInputQuantity = conversion.inputQuantity,
                    context = conversion.context,
                    method = conversion.resultState,
                    resultState = conversion.resultState,
                    resultSemantics = conversion.resultSemantics,
                    quantity = conversion.outputQuantity,
                    supportedToolGameItemIds = conversion.supportedToolGameItemIds,
                    facilityId = conversion.facilityId,
                    batchCapacity = conversion.batchCapacity,
                    probabilitySemantics = "NOT_PUBLISHED"
                )
            )
        }
    }
    val materialsForUi = items.map { item ->
        val itemRewards = rewardsByItem[item.id].orEmpty()
        val gameItemId = requireNotNull(item.gameItemId) { "Missing gameItemId for ${item.id}" }
        val questSources = questSourcesByGameItem[gameItemId].orEmpty()
        val sources = itemRewards.map {
            MaterialSource(
                id = it.id,
                type = MaterialSourceType.MONSTER_REWARD,
                name = monsterNames[it.monsterId].orEmpty(),
                monsterId = it.monsterId,
                rank = RewardContext.valueOf(it.rank.name),
                method = it.method,
                condition = it.condition,
                chance = it.chance,
                quantity = it.quantity
            )
        } + smallMonsterSourcesByGameItem[gameItemId].orEmpty() +
            gatheringSourcesByGameItem[gameItemId].orEmpty() +
            farmSourcesByGameItem[gameItemId].orEmpty() + questSources +
            trainingSourcesByGameItem[gameItemId].orEmpty() +
            combinationSourcesByGameItem[gameItemId].orEmpty() +
            combinationFailureSourcesByGameItem[gameItemId].orEmpty() +
            shopSourcesByGameItem[gameItemId].orEmpty() +
            tradeSourcesByGameItem[gameItemId].orEmpty() +
            supplySourcesByGameItem[gameItemId].orEmpty() +
            specialFreeSourcesByGameItem[gameItemId].orEmpty() +
            scrapSourcesByGameItem[gameItemId].orEmpty() +
            decorationSourcesByGameItem[gameItemId].orEmpty() +
            roastingSourcesByGameItem[gameItemId].orEmpty() +
            invasionSourcesByGameItem[gameItemId].orEmpty() +
            expeditionSourcesByGameItem[gameItemId].orEmpty()
        Material(
            id = item.id,
            name = item.name,
            rarity = null,
            description = sources.sourceTypeSummary(),
            sources = sources,
            uses = emptyList(),
            aliases = item.aliases,
            gameItemId = gameItemId
        )
    }
    val weaponsForUi = weapons.map { it.toRuntime() }
    return FixtureData(
        monsters = monstersForUi,
        materials = materialsForUi,
        quests = questsForUi,
        smallMonsters = smallMonstersForUi,
        weapons = weaponsForUi,
        huntingHornSongCatalog = huntingHornSongCatalog.map { it.toRuntime() },
        trainingQuests = trainingQuestsForUi,
        itemCombinationRecipes = itemCombinationRecipesForUi,
        itemCombinationFailureRelations = itemCombinationFailuresForUi,
        itemShopPurchaseRelations = shopRelationsForUi,
        itemShopPeddlerProfiles = itemShopPeddlerProfiles.map { ItemShopPeddlerProfile(it.profileId, it.semantics, it.selectionSemantics, it.availabilityConditionId) },
        itemShopUnlockConditions = itemShopUnlockConditions.map { ItemShopUnlockCondition(it.conditionId, it.sourceText, it.displayLabel, it.semantics, it.sourceUrl) },
        itemTradeExchangeRelations = itemTradeExchangeRelations.map { relation ->
            ItemTradeExchangeRelation(relation.id, relation.mechanism, relation.scopeType, relation.mapId, relation.inputGameItemId, relation.inputQuantity, relation.currencyType, relation.currencyCost, relation.outputGameItemId, relation.outputQuantity, relation.availabilityConditionId, relation.questContext, relation.interactionWindow, relation.sourceId, relation.sourceUrl)
        },
        itemTradeExchangeConditions = itemTradeExchangeConditions.map { condition ->
            ItemTradeExchangeCondition(condition.conditionId, condition.sourceText, condition.displayLabel, condition.semantics, condition.sourceId, condition.sourceUrl)
        },
        itemVeggieElderLocations = itemVeggieElderLocations.map { location ->
            ItemVeggieElderLocation(location.mapId, location.sourceJapaneseMapName, location.elderLocationRaw, location.locationType, location.areaNumber, location.sourceId, location.sourceUrl)
        },
        decorationCraftingRecipes = decorationCraftingRecipesForUi,
        itemScrapConversionRules = itemScrapConversionRules,
        roastingConversions = roastingConversionsForUi,
        regularQuestSupplyItems = regularQuestSupplyItemsForUi,
        regularQuestSupplyCoverage = regularQuestSupplyCoverageForUi,
        invasionRewardDrops = invasionRewardDrops.flatMap { drop ->
            listOf(InvasionReward(
                id = drop.id,
                monsterId = drop.monsterId,
                context = drop.context,
                gameItemId = drop.gameItemId,
                quantity = drop.quantity,
                probabilityValuePercent = drop.probabilityValuePercent,
                probabilitySemantics = drop.probabilitySemantics,
                probabilityBand = InvasionProbabilityBand.valueOf(drop.probabilityBand.name)
            ))
        },
        palicoExpeditions = palicoExpeditionsForUi,
        palicoExpeditionRewardDrops = palicoExpeditionRewardDrops.map { drop ->
            PalicoExpeditionRewardDrop(drop.id, drop.expeditionId, drop.gameItemId, drop.rewardCategory, drop.quantity)
        },
        skillTrees = skillTrees.map { tree ->
            SkillTree(
                stableSkillTreeId = tree.stableSkillTreeId,
                displayName = tree.uiDisplayName ?: tree.displayName,
                tmoDisplayName = tree.tmoDisplayName,
                jpName = tree.jpName,
                inGameDescription = tree.inGameDescription.replace("\\n", "\n"),
                thresholds = tree.thresholds.map { threshold ->
                    SkillThreshold(threshold.points, threshold.activatedSkillName, threshold.polarity, threshold.effectSummary)
                },
                specialMechanics = tree.specialMechanics.map { special ->
                    SkillSpecialMechanic(special.type, special.name, special.effectSummary)
                }
            )
        },
        decorationSkillRelations = decorationSkillRelations.map { relation ->
            DecorationSkillRelation(
                stableDecorationItemId = relation.stableDecorationItemId,
                stableSkillTreeId = relation.stableSkillTreeId,
                points = relation.points,
                slotCost = relation.slotCost
            )
        },
        displayTerminology = displayTerminology?.toRuntime() ?: DisplayTerminology()
    )
}

private fun GeneratedDisplayTerminology.toRuntime(): DisplayTerminology = DisplayTerminology(
    trainingArmorByRaw = trainingArmorByRaw,
    trainingSkillByRaw = trainingSkillByRaw,
    trainingCharmByRaw = trainingCharmByRaw,
    objectiveByStableId = objectiveByStableId
)

private fun roastingContextLabel(context: String): String = when (context) {
    "FIELD_BBQ" -> "Field BBQ"
    "FARM_CUSTOM_ROASTER" -> "Farm Custom Roaster"
    else -> context.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

internal fun structuralGatheringContext(nodeId: String, rawArea: String, rawPointIndex: String): String {
    val area = Regex("_area_(base_camp|\\d+)_point_", RegexOption.IGNORE_CASE)
        .find(nodeId)?.groupValues?.get(1)
        ?.let { if (it.equals("base_camp", ignoreCase = true)) "Base Camp" else it }
        ?: rawArea.trim().substringBefore(' ').substringBefore('(').ifBlank { "?" }
    val point = Regex("_point_(\\d+)(?:_|$)", RegexOption.IGNORE_CASE)
        .find(nodeId)?.groupValues?.get(1)
        ?: rawPointIndex.trim().takeWhile { it.isDigit() || it in '\u2460'..'\u2473' }.ifBlank { "?" }
    return "Area $area · Point $point"
}

private fun farmActionLabel(facility: GeneratedFarmFacility, hasInput: Boolean): String = when {
    hasInput && facility.facilityType == "FIELD" -> "Plant this:"
    facility.tier == "ADDITIONAL_SLOT" -> "Bonus harvest slot"
    else -> listOf(
        facility.facilityType.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
        facility.tier.takeUnless { it in setOf("INITIAL", "BASE") }
            ?.lowercase()?.split('_')?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    ).filterNotNull().joinToString(" · ")
}

fun List<MaterialSource>.sourceTypeSummary(): String {
    val types = map { it.type }.toSet()
    return listOfNotNull(
        "Monster".takeIf { MaterialSourceType.MONSTER_REWARD in types },
        "Small Monster".takeIf { MaterialSourceType.SMALL_MONSTER in types },
        "Quest".takeIf { MaterialSourceType.QUEST_REWARD in types },
        "Training".takeIf { MaterialSourceType.TRAINING_REWARD in types },
        "Mining".takeIf { MaterialSourceType.MINING in types },
        "Bug Gathering".takeIf { MaterialSourceType.BUG in types },
        "Fishing".takeIf { MaterialSourceType.FISHING in types },
        "Gathering".takeIf { MaterialSourceType.GATHERING in types },
        "Farm".takeIf { MaterialSourceType.FARM in types },
        "Trade".takeIf { MaterialSourceType.TRADE in types },
        "Shop".takeIf { MaterialSourceType.SHOP_PURCHASE in types },
        "Scrap conversion".takeIf { MaterialSourceType.SCRAP_CONVERSION in types },
        "Special".takeIf { MaterialSourceType.SPECIAL_FREE in types },
        "Crafting".takeIf { MaterialSourceType.DECORATION_CRAFTING in types },
        "Roasting".takeIf { MaterialSourceType.ROASTING in types },
        "Invasion Reward".takeIf { MaterialSourceType.INVASION_REWARD in types },
        "Supply Box".takeIf { MaterialSourceType.SUPPLY_BOX in types },
        "Palico Expedition".takeIf { MaterialSourceType.PALICO_EXPEDITION in types },
        "Combination".takeIf { MaterialSourceType.COMBINATION in types || MaterialSourceType.COMBINATION_FAILURE in types }
    ).joinToString(" · ")
}

private fun GeneratedHunterThreatType.toUiType(): HunterThreatType =
    HunterThreatType.valueOf(name)

private fun String.semanticLabel(): String = split('_').joinToString(" ") { word ->
    when (word.lowercase()) {
        "le" -> "≤"
        else -> word.replaceFirstChar { it.uppercase() }
    }
}
