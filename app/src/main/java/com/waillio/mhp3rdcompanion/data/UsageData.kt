package com.waillio.mhp3rdcompanion.data

/** The eight source-backed inverse acquisition families accepted for Item Detail. */
enum class ItemUsageFamily(
    val displayLabel: String,
    val targetNoun: String
) {
    WEAPONS("Weapons", "weapons"),
    DECORATIONS("Decorations", "decorations"),
    COMBINATIONS("Combinations", "results"),
    QUEST_DELIVERY("Quest Delivery", "quests"),
    TRADING("Trading with Veggie Elder", "items"),
    FARM("Farm", "results"),
    ROASTING("Roasting", "results"),
    SCRAP_CONVERSION("Scrap Conversion", "scraps")
}

enum class UsageTargetKind { ITEM, WEAPON, TRAINING }

/** One raw, source-backed relation. UI grouping must never discard these rows. */
data class ItemUsageRelation(
    val id: String,
    val currentItemGameItemId: Int,
    val family: ItemUsageFamily,
    val targetId: String,
    val targetKind: UsageTargetKind,
    val targetGameItemId: Int? = null,
    val targetName: String,
    val routeLabel: String? = null,
    /** Quantity consumed from the current Item, when the source publishes it. */
    val quantity: Int? = null,
    /** Quantity produced by the target route, when applicable. */
    val outputQuantity: Int? = null,
    val outputQuantityMin: Int? = null,
    val outputQuantityMax: Int? = null,
    val quantitySemantics: String? = null,
    val rank: String? = null,
    val chancePercent: Int? = null,
    val context: String? = null,
    val otherIngredientGameItemId: Int? = null,
    val otherIngredientName: String? = null,
    val successPercent: Int? = null,
    val resultState: String? = null,
    /** Raw trade route identity/provenance, populated only for Item exchanges. */
    val mapId: String? = null,
    val scopeType: String? = null,
    val availabilityConditionId: String? = null,
    val questContext: String? = null,
    val interactionWindow: String? = null,
    val sourceId: String? = null,
    val sourceUrl: String? = null,
    val sourceOrder: Int
)

data class ItemUsageTarget(
    val targetId: String,
    val targetKind: UsageTargetKind,
    val targetGameItemId: Int?,
    val targetName: String,
    val relations: List<ItemUsageRelation>
) {
    val targetIdentity: String get() = "${targetKind.name}:$targetId"
}

data class ItemUsageFamilyProjection(
    val family: ItemUsageFamily,
    val targets: List<ItemUsageTarget>,
    val rawRelationCount: Int
) {
    val targetCount: Int get() = targets.size
}

data class ItemUsageProjection(
    val currentItemGameItemId: Int,
    val families: List<ItemUsageFamilyProjection>
) {
    val rawRelationCount: Int get() = families.sumOf { it.rawRelationCount }
    val totalTargetCount: Int get() = families.flatMap { family -> family.targets.map { it.targetIdentity } }.toSet().size
    val familyCount: Int get() = families.size
}

/**
 * Stable reverse index built once from the production FixtureData. It is kept
 * outside Compose so Item Detail recomposition never scans the 5,125-row
 * universe.
 */
class ItemUsageIndex private constructor(
    private val byGameItemId: Map<Int, ItemUsageProjection>,
    val allRelations: List<ItemUsageRelation>
) {
    fun forItem(gameItemId: Int): ItemUsageProjection? = byGameItemId[gameItemId]

    companion object {
        fun build(data: FixtureData): ItemUsageIndex {
            val materialByGameId = data.materials.mapNotNull { material ->
                material.gameItemId?.let { it to material }
            }.toMap()
            val itemNameByGameId = materialByGameId.mapValues { it.value.name }
            val trainingNameById = data.trainingQuests.associate { it.id to it.canonicalDisplayName }
            val relationRows = mutableListOf<ItemUsageRelation>()
            var sourceOrder = 0

            fun add(row: ItemUsageRelation) {
                if (row.targetName.isNotBlank()) relationRows += row
            }

            data.weapons.forEach { weapon ->
                weapon.forgeRecipe?.ingredients.orEmpty().forEach { ingredient ->
                    add(
                        ItemUsageRelation(
                            id = "${weapon.stableWeaponId}:FORGE:${ingredient.gameItemId}:$sourceOrder",
                            currentItemGameItemId = ingredient.gameItemId,
                            family = ItemUsageFamily.WEAPONS,
                            targetId = weapon.stableWeaponId,
                            targetKind = UsageTargetKind.WEAPON,
                            targetName = weapon.name,
                            routeLabel = "Forge",
                            quantity = ingredient.quantity,
                            sourceOrder = sourceOrder++
                        ).takeIf { ingredient.gameItemId in materialByGameId } ?: return@forEach
                    )
                }
                weapon.upgradeFrom?.recipe?.ingredients.orEmpty().forEach { ingredient ->
                    add(
                        ItemUsageRelation(
                            id = "${weapon.stableWeaponId}:UPGRADE:${ingredient.gameItemId}:$sourceOrder",
                            currentItemGameItemId = ingredient.gameItemId,
                            family = ItemUsageFamily.WEAPONS,
                            targetId = weapon.stableWeaponId,
                            targetKind = UsageTargetKind.WEAPON,
                            targetName = weapon.name,
                            routeLabel = "Upgrade",
                            quantity = ingredient.quantity,
                            context = weapon.upgradeFrom?.fromWeaponId,
                            sourceOrder = sourceOrder++
                        ).takeIf { ingredient.gameItemId in materialByGameId } ?: return@forEach
                    )
                }
            }

            data.decorationCraftingRecipes.forEach { recipe ->
                recipe.ingredients.forEachIndexed { ingredientIndex, ingredient ->
                    val target = materialByGameId[recipe.outputGameItemId]
                    if (target != null) {
                        add(
                            ItemUsageRelation(
                                id = "${recipe.id}:ingredient:${ingredientIndex + 1}",
                                currentItemGameItemId = ingredient.gameItemId,
                                family = ItemUsageFamily.DECORATIONS,
                                targetId = target.id,
                                targetKind = UsageTargetKind.ITEM,
                                targetGameItemId = recipe.outputGameItemId,
                                targetName = target.name,
                                quantity = ingredient.quantity,
                                quantitySemantics = ingredient.quantitySemantics,
                                rank = recipe.rankContext,
                                sourceOrder = sourceOrder++
                            ).takeIf { ingredient.gameItemId in materialByGameId } ?: return@forEachIndexed
                        )
                    }
                }
            }

            data.itemCombinationRecipes.forEach { recipe ->
                val ingredients = listOf(
                    "A" to recipe.ingredientAGameItemId,
                    "B" to recipe.ingredientBGameItemId
                )
                val target = materialByGameId[recipe.outputGameItemId]
                if (target != null) ingredients.forEach { (slot, gameItemId) ->
                    val otherGameItemId = if (slot == "A") recipe.ingredientBGameItemId else recipe.ingredientAGameItemId
                    add(
                        ItemUsageRelation(
                            id = "combination:${recipe.recipeNumber}:$slot",
                            currentItemGameItemId = gameItemId,
                            family = ItemUsageFamily.COMBINATIONS,
                            targetId = target.id,
                            targetKind = UsageTargetKind.ITEM,
                            targetGameItemId = recipe.outputGameItemId,
                            targetName = target.name,
                            outputQuantityMin = recipe.outputQuantityMin,
                            outputQuantityMax = recipe.outputQuantityMax,
                            otherIngredientGameItemId = otherGameItemId,
                            otherIngredientName = itemNameByGameId[otherGameItemId],
                            successPercent = recipe.baseSuccessPercent,
                            context = recipe.recipeNumber.toString(),
                            sourceOrder = sourceOrder++
                        ).takeIf { gameItemId in materialByGameId } ?: return@forEach
                    )
                }
            }

            // Farm usage is source-native in the generated Farm output rows;
            // only rows carrying inputGameItemId are genuine inverse relations.
            data.materials.forEach { target ->
                target.sources.filter { it.type == MaterialSourceType.FARM && it.inputGameItemId != null }
                    .forEach { source ->
                        val input = source.inputGameItemId ?: return@forEach
                        add(
                            ItemUsageRelation(
                                id = source.id,
                                currentItemGameItemId = input,
                                family = ItemUsageFamily.FARM,
                                targetId = target.id,
                                targetKind = UsageTargetKind.ITEM,
                                targetGameItemId = target.gameItemId,
                                targetName = target.name,
                                routeLabel = source.farmAction,
                                outputQuantity = source.quantity,
                                quantitySemantics = source.quantitySemantics,
                                chancePercent = source.chance,
                                context = source.facilityId ?: source.method,
                                sourceOrder = sourceOrder++
                            ).takeIf { input in materialByGameId } ?: return@forEach
                        )
                    }
            }

            data.itemTradeExchangeRelations.forEach { relation ->
                val input = relation.inputGameItemId ?: return@forEach
                val target = materialByGameId[relation.outputGameItemId] ?: return@forEach
                add(
                    ItemUsageRelation(
                        id = relation.id,
                        currentItemGameItemId = input,
                        family = ItemUsageFamily.TRADING,
                        targetId = target.id,
                        targetKind = UsageTargetKind.ITEM,
                        targetGameItemId = relation.outputGameItemId,
                        targetName = target.name,
                        routeLabel = "Cost",
                        quantity = relation.inputQuantity,
                        outputQuantity = relation.outputQuantity,
                        context = tradeLocationContextLabel(relation.scopeType, relation.mapId, data.itemVeggieElderLocations),
                        mapId = relation.mapId,
                        scopeType = relation.scopeType,
                        availabilityConditionId = relation.availabilityConditionId,
                        questContext = relation.questContext,
                        interactionWindow = relation.interactionWindow,
                        sourceId = relation.sourceId,
                        sourceUrl = relation.sourceUrl,
                        sourceOrder = sourceOrder++
                    ).takeIf { input in materialByGameId } ?: return@forEach
                )
            }

            data.roastingConversions.forEach { conversion ->
                val target = materialByGameId[conversion.outputGameItemId] ?: return@forEach
                add(
                    ItemUsageRelation(
                        id = conversion.id,
                        currentItemGameItemId = conversion.inputGameItemId,
                        family = ItemUsageFamily.ROASTING,
                        targetId = target.id,
                        targetKind = UsageTargetKind.ITEM,
                        targetGameItemId = conversion.outputGameItemId,
                        targetName = target.name,
                        routeLabel = "Use",
                        quantity = conversion.inputQuantity,
                        outputQuantity = conversion.outputQuantity,
                        context = conversion.context,
                        resultState = conversion.resultState,
                        sourceOrder = sourceOrder++
                    ).takeIf { conversion.inputGameItemId in materialByGameId } ?: return@forEach
                )
            }

            data.itemScrapConversionRules.forEach { rule ->
                val target = materialByGameId[rule.outputScrapGameItemId] ?: return@forEach
                add(
                    ItemUsageRelation(
                        id = "scrap:${rule.inputGameItemId}:${rule.outputScrapGameItemId}",
                        currentItemGameItemId = rule.inputGameItemId,
                        family = ItemUsageFamily.SCRAP_CONVERSION,
                        targetId = target.id,
                        targetKind = UsageTargetKind.ITEM,
                        targetGameItemId = rule.outputScrapGameItemId,
                        targetName = target.name,
                        routeLabel = "Use",
                        quantity = rule.inputQuantity,
                        outputQuantity = rule.outputQuantity,
                        quantitySemantics = rule.quantitySemantics,
                        context = rule.triggerModes.joinToString(" · "),
                        sourceOrder = sourceOrder++
                    ).takeIf { rule.inputGameItemId in materialByGameId } ?: return@forEach
                )
            }

            data.trainingQuests.forEach { training ->
                training.objectiveTargets.filter { it.entityKind == "ITEM" && it.gameItemId != null }
                    .forEach { objective ->
                        val input = objective.gameItemId ?: return@forEach
                        add(
                            ItemUsageRelation(
                                id = "${training.id}:delivery:${input}:${sourceOrder}",
                                currentItemGameItemId = input,
                                family = ItemUsageFamily.QUEST_DELIVERY,
                                targetId = training.id,
                                targetKind = UsageTargetKind.TRAINING,
                                targetName = trainingNameById[training.id] ?: training.id,
                                routeLabel = "Deliver",
                                quantity = objective.count,
                                context = objective.objectiveType,
                                sourceOrder = sourceOrder++
                            ).takeIf { input in materialByGameId } ?: return@forEach
                        )
                    }
            }

            // Group directly by the source item carried by every relation. This
            // preserves duplicate routes and keeps the projection independent of
            // any UI ordering or reverse-index heuristics.
            val projections = relationRows.groupBy { it.currentItemGameItemId }.mapValues { (current, rows) ->
                val families = ItemUsageFamily.entries.mapNotNull { family ->
                    val familyRows = rows.filter { it.family == family }
                    if (familyRows.isEmpty()) return@mapNotNull null
                    val targets = familyRows.groupBy { it.targetKind to it.targetId }.values.map { targetRows ->
                        val first = targetRows.first()
                        ItemUsageTarget(first.targetId, first.targetKind, first.targetGameItemId, first.targetName, targetRows)
                    }
                    ItemUsageFamilyProjection(family, targets, familyRows.size)
                }
                ItemUsageProjection(current, families)
            }
            return ItemUsageIndex(projections, relationRows)
        }
    }
}
