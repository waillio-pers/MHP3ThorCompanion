package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionLogic
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.EntityType
import com.waillio.mhp3rdcompanion.data.GeneratedDataset
import com.waillio.mhp3rdcompanion.data.GeneratedHunterThreatType
import com.waillio.mhp3rdcompanion.data.GeneratedStatusEffect
import com.waillio.mhp3rdcompanion.data.GeneratedTacticalPriority
import com.waillio.mhp3rdcompanion.data.GeneratedThreatCoverageStatus
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import com.waillio.mhp3rdcompanion.data.counterSummaries
import com.waillio.mhp3rdcompanion.data.availableContexts
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class GeneratedDataIntegrationTest {
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test
    fun iteration47faMatchesAuditedSmallMonsterItemClosure() {
        val bytes = ApplicationProvider.getApplicationContext<android.content.Context>()
            .assets.open("mhp3rd-data.json").use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
    }

    @Test
    fun productionRepositoryLoadsGeneratedDataset() {
        assertEquals(40, data.monsters.size)
        val zinogre = data.monsters.single { it.id == "monster_zinogre" }
        assertEquals("Zinogre", zinogre.name)
        assertTrue(zinogre.hitzones.isNotEmpty())
        assertTrue(zinogre.rewards.any { it.item == "Zinogre Claw+" })
    }

    @Test
    fun longSwordRecipeItemsAreClosedWithoutEnteringWeaponProduction() {
        val expected = mapOf(
            200 to "Sharqskin",
            619 to "Longsword Codex",
            623 to "Lrg Ancient Fang",
            624 to "Swordmaster Book",
            638 to "Dengeki+ Ticket",
        )
        assertEquals(978, data.materials.size)
        expected.forEach { (gameItemId, name) ->
            val item = data.materials.single { it.gameItemId == gameItemId }
            assertEquals(name, item.name)
            assertTrue(item.sources.isNotEmpty())
        }
        assertEquals(987, data.weapons.size)
        assertEquals(91, data.weapons.count { it.weaponType == "GREAT_SWORD" })
        assertEquals(85, data.weapons.count { it.weaponType == "LONG_SWORD" })
        assertEquals(94, data.weapons.count { it.weaponType == "SWORD_AND_SHIELD" })
        assertEquals(82, data.weapons.count { it.weaponType == "DUAL_BLADES" })
        assertEquals(93, data.weapons.count { it.weaponType == "HAMMER" })
        assertEquals(96, data.weapons.count { it.weaponType == "LANCE" })
        assertEquals(85, data.weapons.count { it.weaponType == "HUNTING_HORN" })
        assertEquals(77, data.weapons.count { it.weaponType == "GUNLANCE" })
        assertEquals(71, data.weapons.count { it.weaponType == "SWITCH_AXE" })
        assertEquals(80, data.weapons.count { it.weaponType == "BOW" })
        assertEquals(67, data.weapons.count { it.weaponType == "LIGHT_BOWGUN" })
        assertEquals(66, data.weapons.count { it.weaponType == "HEAVY_BOWGUN" })
    }

    @Test
    fun bowgunItemClosureAppendsOnlyCanonicalRecipeItems() {
        assertEquals(978, data.materials.size)
        val bowgunItems = listOf(610, 618).map { id -> data.materials.single { it.gameItemId == id } }
        assertEquals(listOf("Hot Spring Tckt", "Bowgun Codex"), bowgunItems.map { it.name })
        assertTrue(bowgunItems.all { it.sources.any { source -> source.type == MaterialSourceType.SPECIAL_FREE } })
        assertEquals(133, data.weapons.count { it.weaponType == "LIGHT_BOWGUN" || it.weaponType == "HEAVY_BOWGUN" })
    }

    @Test
    fun monsterInitialLettersComeOnlyFromProductionNames() {
        val letters = monsterInitialLetters(data.monsters)
        assertEquals(data.monsters.map { it.name.trim().first().uppercaseChar() }.distinct().sorted(), letters)
        assertEquals(letters.distinct(), letters)
    }

    @Test
    fun repositoryKeepsNormalAndAlternateHitzoneStatesWithSemantics() {
        val zinogre = data.monsters.single { it.id == "monster_zinogre" }
        assertTrue(zinogre.hitzones.isNotEmpty())
        assertEquals("Supercharged", zinogre.alternateHitzones.single().label)
        assertEquals("alternate", zinogre.alternateHitzones.single().stateId)
        assertTrue(zinogre.alternateHitzones.single().hitzones.isNotEmpty())
        assertEquals("supercharged", zinogre.hitzoneStateSemantics.single().semanticState)
        assertTrue(data.monsters.single { it.id == "monster_arzuros" }.alternateHitzones.isEmpty())
    }

    @Test
    fun importedMonsterIdsWorkInSearch() {
        val results = CompanionLogic.search(data, "Amatsu", EntityType.MONSTER)
        assertEquals("monster_amatsu", results.single().id)
    }

    @Test
    fun legacyItemAliasFindsCanonicalTmoName() {
        assertEquals(978, data.materials.size)
        val result = CompanionLogic.search(data, "Nargacuga Marrow", EntityType.MATERIAL).single()
        assertEquals("item_nargacuga_marrow", result.id)
        assertEquals("Narga Marrow", result.name)
    }

    @Test
    fun promotedItemsBrowseSearchAndExposeFactualSourceSummaries() {
        val allItems = CompanionLogic.search(data, "", EntityType.MATERIAL)
        assertEquals(978, allItems.size)
        val names = allItems.map { it.name }.toSet()
        assertTrue(setOf("Divine Rhino", "Sushifish", "Machalite Ore", "Armor Sphere").all(names::contains))
        assertEquals("item_divine_rhino", CompanionLogic.search(data, "divine rhino", EntityType.MATERIAL).single().id)
        assertEquals(15, data.materials.count { it.description.isBlank() })
        assertTrue(data.materials.filter { it.sources.isNotEmpty() }.all { it.description.isNotBlank() })
        assertTrue(data.materials.none { it.description == "Production material reference." })
        assertTrue(data.materials.single { it.id == "item_divine_rhino" }.description.contains("Bug Gathering"))
        assertTrue(data.materials.single { it.id == "item_machalite_ore" }.description.contains("Mining"))
    }

    @Test
    fun materialSourceCoverageMatchesCurrentProductionRewards() {
        val monsterSourcesByMaterial = data.materials.associate { material ->
            material.id to material.sources.filter { it.type == MaterialSourceType.MONSTER_REWARD }
        }

        assertEquals(978, data.materials.size)
        assertEquals(15, data.materials.count { it.sources.isEmpty() })
        assertEquals(963, data.materials.count { it.sources.isNotEmpty() })
        assertEquals(331, monsterSourcesByMaterial.count { it.value.isNotEmpty() })
        assertEquals(647, monsterSourcesByMaterial.count { it.value.isEmpty() })
        assertEquals(1535, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.MONSTER_REWARD })
        assertEquals(2132, data.materials.flatMap { it.sources }.count {
            it.type in setOf(MaterialSourceType.GATHERING, MaterialSourceType.MINING, MaterialSourceType.BUG, MaterialSourceType.FISHING)
        })
        assertEquals(595, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.FARM })
        assertEquals(4206, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.QUEST_REWARD })
        assertEquals(306, data.materials.flatMap { it.sources }.count { it.type == MaterialSourceType.SMALL_MONSTER })
    }

    @Test
    fun materialMonsterRewardSourcesPreserveGeneratedFields() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        val uiSources = data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.MONSTER_REWARD }
            .associateBy { it.id }
        val monsterNames = generated.monsters.associate { it.id to it.name }

        assertEquals(generated.rewards.size, uiSources.size)
        generated.rewards.forEach { reward ->
            val source = uiSources.getValue(reward.id)
            assertEquals(MaterialSourceType.MONSTER_REWARD, source.type)
            assertEquals(reward.monsterId, source.monsterId)
            assertEquals(monsterNames.getValue(reward.monsterId), source.name)
            assertEquals(reward.rank.name, source.rank?.name)
            assertEquals(reward.method, source.method)
            assertEquals(reward.condition, source.condition)
            assertEquals(reward.chance, source.chance)
            assertEquals(reward.quantity, source.quantity)
            assertEquals(null, source.questId)
            assertEquals(null, source.locationId)
        }
    }

    @Test
    fun smallMonsterRewardsProjectIntoItemSourcesWithoutFlatteningConditions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        val sources = data.materials.flatMap { it.sources }
            .filter { it.type == MaterialSourceType.SMALL_MONSTER }
            .associateBy { it.id }
        val names = generated.smallMonsters.associate { it.id to it.name }

        assertEquals(306, sources.size)
        val promotedIds = generated.items.drop(494).map { it.gameItemId }.toSet()
        assertEquals(86, data.materials.filter { it.gameItemId in promotedIds }
            .flatMap { it.sources }.count { it.type == MaterialSourceType.SMALL_MONSTER })
        assertTrue(data.materials.filter { it.gameItemId in promotedIds }
            .filter { material -> material.sources.any { it.type == MaterialSourceType.SMALL_MONSTER } }
            .all { it.description.contains("Small Monster") })
        generated.smallMonsterRewards.forEach { reward ->
            val source = sources.getValue(reward.id)
            assertEquals(reward.smallMonsterId, source.smallMonsterId)
            assertEquals(names.getValue(reward.smallMonsterId), source.name)
            assertEquals(reward.context.name, source.rank?.name)
            assertEquals(reward.method.name, source.method)
            assertEquals(reward.chancePercent, source.chance)
            assertEquals(reward.quantity, source.quantity)
            assertEquals(reward.rolls, source.rolls)
            assertEquals(reward.probabilitySemantics.name, source.probabilitySemantics)
        }
        val bnahabraVariant = generated.smallMonsterRewards.first {
            it.smallMonsterId == "small_monster_bnahabra" && it.conditionDetails?.wingColor != null
        }
        val bnahabraSource = sources.getValue(bnahabraVariant.id)
        assertTrue(bnahabraSource.condition.orEmpty().contains("Wing color:"))
        assertTrue(bnahabraSource.condition.orEmpty().contains("Locations:"))
        val altarothGold = generated.smallMonsterRewards.first {
            it.smallMonsterId == "small_monster_altaroth" && it.conditionDetails?.color == "GOLD"
        }
        assertTrue(sources.getValue(altarothGold.id).condition.orEmpty().contains("Color: Gold"))
        val gargwaEgg = generated.smallMonsterRewards.first { it.condition == "GOLD_EGG" }
        assertTrue(sources.getValue(gargwaEgg.id).condition.orEmpty().contains("Gold Egg"))
    }

    @Test
    fun materialSourceSchemaV1LoadsStrictArraysAndRegressionAnchors() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)

        assertEquals(31, generated.schemaVersion)
        assertEquals(1, generated.materialSourcesVersion)
        assertEquals(236, generated.gatheringNodes.size)
        assertEquals(2132, generated.gatheringDrops.size)
        assertEquals(28, generated.farmFacilities.size)
        assertEquals(595, generated.farmDrops.size)
        assertEquals(4206, generated.questRewardDrops.size)
        assertTrue(generated.gatheringDrops.all {
            it.probability.valuePercent == null && it.probability.semantics.name == "NOT_PUBLISHED"
        })

        val tundraNode = generated.gatheringNodes.single {
            it.id == "gathering_tundra_area_2_point_3_mining"
        }
        assertEquals("MINING", tundraNode.method.name)
        assertEquals(setOf("LOW", "HIGH"), generated.gatheringDrops.filter {
            it.nodeId == tundraNode.id && it.gameItemId == 233
        }.map { it.rankContext.name }.toSet())

        val iceFarm = generated.farmDrops.filter { it.gameItemId == 233 && it.facilityId in setOf("farm_mining_point_base", "farm_mining_point_plus_1") }.associate {
            it.facilityId to it.probability.valuePercent
        }
        assertEquals(7, iceFarm["farm_mining_point_base"])
        assertEquals(3, iceFarm["farm_mining_point_plus_1"])

        val keenbone = generated.questRewardDrops.filter { it.gameItemId == 242 }.associateBy { it.questId }
        assertEquals(13, keenbone.getValue("quest_guild_6_star_10").probability.valuePercent)
        assertEquals(2, keenbone.getValue("quest_guild_6_star_11").quantity)
        assertEquals(10, keenbone.getValue("quest_guild_6_star_22").probability.valuePercent)
    }

    @Test
    fun materialSourceTypeContractIncludesPlannedAuditedOrigins() {
        assertEquals(
            setOf(
                "MONSTER_REWARD", "SMALL_MONSTER", "QUEST_REWARD", "GATHERING", "MINING",
                "BUG", "FISHING", "FARM", "TRADE", "TRAINING_REWARD",
                "COMBINATION", "COMBINATION_FAILURE", "SHOP_PURCHASE", "SPECIAL_FREE",
                "SCRAP_CONVERSION", "DECORATION_CRAFTING", "ROASTING", "INVASION_REWARD",
                "SUPPLY_BOX", "PALICO_EXPEDITION"
            ),
            MaterialSourceType.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun enrichedOptionalArraysDecodeWithProductionModels() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        assertEquals(31, generated.schemaVersion)
        assertEquals(1, generated.enrichmentVersion)
        assertEquals(145, generated.statusEffects.size)
        assertEquals(20, generated.hitzoneStateSemantics.size)
        assertEquals(18, generated.itemEffects.size)
        assertEquals(4, generated.monsterBehavior.size)
        assertEquals(5, generated.sourceCatalog.size)
        assertEquals(979, generated.itemNameSource?.extractedItemStringCount)
        assertEquals(978, generated.itemNameSource?.canonicalCurrentItemEntities)
        assertEquals(10, generated.sourceCorrections.size)
        assertTrue(generated.items.all { it.gameItemId != null })
        assertEquals("tmo61_r754", generated.questNameSource?.id)
        assertEquals("mh_wiki_guild_quests", generated.questLocationSource?.id)
        assertTrue(generated.quests.all { !it.location.isNullOrBlank() })
        assertTrue(generated.quests.all { !it.objective.isNullOrBlank() })
    }

    @Test
    fun canonicalQuestStructureLoadsWithoutLegacyTargets() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        assertEquals(354, generated.quests.size)
        val expected = mapOf(
            "quest_guild_8_star_35" to "Surrounded by Enemies",
            "quest_guild_8_star_36" to "Sitting Hellfire",
            "quest_guild_8_star_37" to "God of Avalanche",
            "quest_guild_8_star_38" to "The Brilliant Darkness",
            "quest_guild_8_urgent_jhen" to "Rumble in the Great Desert",
            "quest_guild_8_urgent_amatsu" to "The Dancing Storm"
        )
        val names = generated.quests.associate { it.id to it.name }
        expected.forEach { (id, name) -> assertEquals(name, names[id]) }
        val qurupeco = generated.quests.single { it.id == "quest_guild_8_star_27" }.objectiveTargets.single()
        assertEquals(2, qurupeco.count)
        assertEquals("AT_LEAST", qurupeco.countMode.name)
        assertEquals("LARGE_MONSTER", qurupeco.entityKind.name)
        assertEquals("monster_qurupeco", qurupeco.entityId)
        val harvest = generated.quests.single { it.id == "quest_guild_6_star_01" }
        assertTrue(harvest.objectiveTargets.isEmpty())
        assertFalse(harvest.appearingMonsterIds.isEmpty())
        assertTrue(json.indexOf("\"targetMonsterIds\"") == -1)
    }

    @Test
    fun expandedQuestCorpusHasExactVillageAndGuildLowCoverage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        val counts = generated.quests.groupingBy { "${it.category}_${it.stars}" }.eachCount()
        assertEquals(mapOf(
            "Village_1" to 5, "Village_2" to 17, "Village_3" to 19,
            "Village_4" to 17, "Village_5" to 17, "Village_6" to 16,
            "Guild_1" to 9, "Guild_2" to 12, "Guild_3" to 19,
            "Guild_4" to 24, "Guild_5" to 22,
            "Guild_6" to 23, "Guild_7" to 35, "Guild_8" to 40,
            "Hot Spring_1" to 1, "Hot Spring_2" to 1, "Hot Spring_3" to 1,
            "Hot Spring_4" to 3, "Hot Spring_5" to 1,
            "Drink_1" to 2, "Drink_2" to 3, "Drink_3" to 2, "Drink_4" to 4,
            "Drink_5" to 4, "Drink_6" to 1, "Drink_7" to 1, "Drink_8" to 3,
            "Event_3" to 3, "Event_4" to 6, "Event_5" to 4, "Event_7" to 4, "Event_8" to 35
        ), counts)
        assertEquals(444, generated.quests.sumOf { it.objectiveTargets.size })
        val forestMurmur = generated.quests.single { it.name == "Forest Murmur" }
        assertEquals("quest_village_2_star_05", forestMurmur.id)
        assertEquals(listOf("monster_bulldrome", "monster_zinogre"), forestMurmur.appearingMonsterIds)
        assertEquals("The Festival of Fear", generated.quests.single { it.id == "quest_guild_5_star_22" }.name)
    }

    @Test
    fun smallMonsterSchemaLoadsAsSeparateStrictDataLayer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)

        assertEquals(31, generated.schemaVersion)
        assertEquals(1, generated.smallMonsterDataVersion)
        assertEquals(40, generated.monsters.size)
        assertEquals(20, generated.smallMonsters.size)
        assertTrue(generated.monsters.map { it.id }.toSet().intersect(generated.smallMonsters.map { it.id }.toSet()).isEmpty())
        assertEquals(306, generated.smallMonsterRewards.size)
        assertEquals(mapOf("GUILD_1_2" to 86, "LOW" to 100, "HIGH" to 120),
            generated.smallMonsterRewards.groupingBy { it.context.name }.eachCount())
        assertEquals(mapOf("BODY_CARVE" to 243, "SHINY_DROP" to 63),
            generated.smallMonsterRewards.groupingBy { it.method.name }.eachCount())
        assertEquals(75, generated.smallMonsterRewards.map { it.gameItemId }.distinct().size)
        assertEquals(75, generated.smallMonsterItemCatalog.count { it.catalogStatus.name == "CURRENT_ITEM" })
        assertEquals(0, generated.smallMonsterItemCatalog.count { it.catalogStatus.name == "PENDING_ITEM_PROMOTION" })
        assertEquals(978, generated.items.size)

        val targets = generated.quests.flatMap { it.objectiveTargets }
        assertEquals(444, targets.size)
        assertEquals(402, targets.count { it.entityKind.name == "LARGE_MONSTER" })
        assertEquals(42, targets.count { it.entityKind.name == "SMALL_MONSTER" })
        val bullfango = generated.quests.single { it.id == "quest_village_1_star_04" }.objectiveTargets.single()
        assertEquals("SMALL_MONSTER", bullfango.entityKind.name)
        assertEquals("small_monster_bullfango", bullfango.entityId)
        assertTrue(generated.quests.take(275).all { quest -> quest.appearingMonsterIds.none { it.startsWith("small_monster_") } })
    }

    @Test
    fun normalizedQuestRelationsReachRequiredMonsters() {
        assertTrue(data.monsters.single { it.id == "monster_alatreon" }.questIds.contains("quest_guild_8_star_38"))
        assertTrue(data.monsters.single { it.id == "monster_akantor" }.questIds.contains("quest_guild_8_star_36"))
        assertTrue(data.monsters.single { it.id == "monster_ukanlos" }.questIds.contains("quest_guild_8_star_37"))
        assertTrue(data.monsters.single { it.id == "monster_jhen_mohran" }.questIds.contains("quest_guild_8_urgent_jhen"))
        assertTrue(data.monsters.single { it.id == "monster_amatsu" }.questIds.contains("quest_guild_8_urgent_amatsu"))
    }

    @Test
    fun mechanicalAuditV1LoadsWithExpectedCorrections() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        assertEquals(1535, generated.rewards.size)
        assertEquals(105, generated.breakableParts.size)
        assertEquals(145, generated.statusEffects.size)
        assertEquals(4, generated.monsterBehavior.size)
        assertTrue(generated.sourceCatalog.any { it.id == "mhp3_atwiki" })
        assertEquals(5, generated.sourceCorrections.count { it.id.startsWith("mechanical_v1_") })

        val volvidon = generated.monsters.single { it.id == "monster_volvidon" }
        assertTrue(volvidon.breakablePartIds.contains("monster_volvidon_part_back"))
        assertFalse(volvidon.breakablePartIds.contains("monster_volvidon_part_lower_shell"))
        assertEquals(5, generated.statusEffects.count { it.monsterId == "monster_volvidon" })
        assertTrue(generated.monsterBehavior.any { it.id == "monster_volvidon_behavior" })

        val brute = generated.statusEffects.single { it.id == "monster_brute_tigrex_status_paralysis" }
        assertEquals(200, brute.initialThreshold)
        assertEquals(100, brute.thresholdIncrease)
        assertEquals(780, brute.maximumThreshold)
        val zinogre = generated.hitzoneStateSemantics.single { it.id == "monster_zinogre_state_alternate_semantics" }
        assertEquals("alternate", zinogre.sourceState)
        assertEquals("supercharged", zinogre.semanticState)
        assertEquals("high", zinogre.confidence)
    }

    @Test
    fun rewardRankAuditV1LoadsSpecialContext() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        val zinogre = generated.rewards.filter { it.monsterId == "monster_zinogre" }
        assertEquals(12, zinogre.count { it.rank == com.waillio.mhp3rdcompanion.data.GeneratedRewardRank.VILLAGE_2_SPECIAL })
        assertEquals(0, zinogre.count { it.rank == com.waillio.mhp3rdcompanion.data.GeneratedRewardRank.GUILD_1_2 })
        assertTrue(generated.rewards.filter { it.monsterId == "monster_gold_rathian" }.all { it.rank == com.waillio.mhp3rdcompanion.data.GeneratedRewardRank.HIGH })
        assertTrue(generated.rewards.filter { it.monsterId == "monster_steel_uragaan" }.all { it.rank == com.waillio.mhp3rdcompanion.data.GeneratedRewardRank.HIGH })
        assertEquals(3, generated.sourceCorrections.count { it.id.startsWith("reward_rank_v1_") })
    }

    @Test
    fun statusSchemaV4LoadsExplicitPayloadsAndDecimalDuration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)
        assertEquals(31, generated.schemaVersion)

        val alatreonPoison = generated.statusEffects.single {
            it.monsterId == "monster_alatreon" && it.status == "POISON"
        }
        assertEquals(75, alatreonPoison.damage)
        assertEquals(null, alatreonPoison.durationSec)
        assertEquals(null, alatreonPoison.staminaDamage)

        val volvidonExhaust = generated.statusEffects.single {
            it.monsterId == "monster_volvidon" && it.status == "EXHAUST"
        }
        assertEquals(200, volvidonExhaust.staminaDamage)
        assertEquals(null, volvidonExhaust.durationSec)
        assertEquals(null, volvidonExhaust.damage)

        val synthetic = """{"id":"decimal","monsterId":"monster_arzuros","status":"PARALYSIS","initialThreshold":1,"thresholdIncrease":1,"maximumThreshold":1,"decayAmount":5,"decayIntervalSec":10,"durationSec":12.5,"damage":null,"staminaDamage":null,"immune":false,"sourceId":"mhp3_atwiki"}"""
        val decimal = Json.decodeFromString<GeneratedStatusEffect>(synthetic)
        assertEquals(12.5, decimal.durationSec ?: 0.0, 0.0)
    }

    @Test
    fun huntPrepV1LoadsAsStrictModelsWithoutChangingLegacyLayers() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = context.assets.open("mhp3rd-data.json").bufferedReader().use { it.readText() }
        val generated = Json { ignoreUnknownKeys = true }.decodeFromString<GeneratedDataset>(json)

        assertEquals(31, generated.schemaVersion)
        assertEquals(1, generated.huntPrepVersion)
        assertEquals(17, generated.huntPrepItemCatalog.size)
        assertEquals(40, generated.monsterHuntPrep.size)
        assertEquals(67, generated.monsterHuntPrep.sumOf { it.threats.size })
        assertEquals(16, generated.monsterHuntPrep.sumOf { it.tacticalTools.size })
        assertEquals(7, generated.monsterHuntPrep.count { it.tacticalTools.isNotEmpty() })
        assertEquals(4, generated.monsterHuntPrep.count {
            it.threatCoverageStatus == GeneratedThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE
        })
        assertTrue(generated.monsters.all { it.threat == null })
        assertEquals(978, generated.items.size)
        assertEquals(145, generated.statusEffects.size)

        val alatreon = generated.monsterHuntPrep.single { it.monsterId == "monster_alatreon" }
        assertEquals(setOf("Red/black state", "Blue/white state"), alatreon.threats.map { it.context }.toSet())
        val deviljho = generated.monsterHuntPrep.single { it.monsterId == "monster_deviljho" }
        assertTrue(deviljho.threats.any { it.type == GeneratedHunterThreatType.TERRAIN_ELEMENTAL_BLIGHT })
        val nargacuga = generated.monsterHuntPrep.single { it.monsterId == "monster_nargacuga" }
        assertTrue(nargacuga.tacticalTools.any {
            it.itemGameId == 61 && it.priority == GeneratedTacticalPriority.CONDITIONAL
        })
        assertFalse(generated.huntPrepItemCatalog.any { it.name == "Hot Drink" || it.name == "Cool Drink" })
    }

    @Test
    fun repositoryResolvesHuntPrepCatalogNamesAndPrioritiesForOverview() {
        val wroggi = data.monsters.single { it.id == "monster_great_wroggi" }.huntPrep!!
        val poison = wroggi.counterSummaries().single { it.threatType == com.waillio.mhp3rdcompanion.data.HunterThreatType.POISON }
        assertEquals(listOf("Antidote", "Herbal Medicine"), poison.itemNames)

        val nibelsnarf = data.monsters.single { it.id == "monster_nibelsnarf" }.huntPrep!!
        assertTrue(nibelsnarf.counterSummaries().any {
            it.threatType == com.waillio.mhp3rdcompanion.data.HunterThreatType.WATERBLIGHT && it.itemNames == listOf("Nulberry")
        })
        assertEquals(
            setOf(com.waillio.mhp3rdcompanion.data.TacticalPriority.CORE),
            nibelsnarf.tacticalTools.map { it.priority }.toSet()
        )

        val nargacuga = data.monsters.single { it.id == "monster_nargacuga" }.huntPrep!!
        assertEquals(com.waillio.mhp3rdcompanion.data.ThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE, nargacuga.coverageStatus)
        assertTrue(nargacuga.tacticalTools.all { it.priority == com.waillio.mhp3rdcompanion.data.TacticalPriority.CONDITIONAL })
        assertTrue(data.monsters.all { it.huntPrep != null })
    }

    @Test
    fun repositoryPreservesDistinctProductionRewardContexts() {
        val zinogre = data.monsters.single { it.id == "monster_zinogre" }
        assertEquals(
            listOf(
                com.waillio.mhp3rdcompanion.data.RewardContext.LOW,
                com.waillio.mhp3rdcompanion.data.RewardContext.HIGH,
                com.waillio.mhp3rdcompanion.data.RewardContext.VILLAGE_2_SPECIAL
            ),
            zinogre.rewards.availableContexts()
        )
        assertEquals(12, zinogre.rewards.count { it.rank == com.waillio.mhp3rdcompanion.data.RewardContext.VILLAGE_2_SPECIAL })
        assertEquals(25, zinogre.rewards.count { it.rank == com.waillio.mhp3rdcompanion.data.RewardContext.LOW })
        assertEquals(33, zinogre.rewards.count { it.rank == com.waillio.mhp3rdcompanion.data.RewardContext.HIGH })

        val greatJaggi = data.monsters.single { it.id == "monster_great_jaggi" }
        assertEquals(
            listOf(
                com.waillio.mhp3rdcompanion.data.RewardContext.LOW,
                com.waillio.mhp3rdcompanion.data.RewardContext.HIGH,
                com.waillio.mhp3rdcompanion.data.RewardContext.GUILD_1_2
            ),
            greatJaggi.rewards.availableContexts()
        )
        val deviljho = data.monsters.single { it.id == "monster_deviljho" }
        assertEquals(listOf(com.waillio.mhp3rdcompanion.data.RewardContext.HIGH), deviljho.rewards.availableContexts())
    }
}
