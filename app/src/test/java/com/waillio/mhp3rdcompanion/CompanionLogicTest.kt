package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.*
import org.junit.Assert.*
import org.junit.Test

class CompanionLogicTest {
    private val data = FixtureData(
        monsters = listOf(Monster("narga", "Nargacuga", "fire")),
        materials = listOf(Material("marrow", "Nargacuga Marrow", 5, "", emptyList(), emptyList())),
        quests = listOf(Quest("hunt", "Shadow Hunt", "Nargacuga"))
    )

    @Test fun searchFindsAcrossTypes() {
        val results = CompanionLogic.search(data, "nargacuga")
        assertEquals(3, results.size)
        assertEquals(setOf(EntityType.MATERIAL, EntityType.MONSTER, EntityType.QUEST), results.map { it.type }.toSet())
    }

    @Test fun filterLimitsResults() {
        assertEquals(listOf(EntityType.MATERIAL), CompanionLogic.search(data, "narga", EntityType.MATERIAL).map { it.type })
    }

    @Test fun blankItemsFilterReturnsTheCatalogButBlankGlobalSearchDoesNot() {
        assertEquals(listOf("marrow"), CompanionLogic.search(data, "", EntityType.MATERIAL).map { it.id })
        assertEquals(listOf("hunt"), CompanionLogic.search(data, "", EntityType.QUEST).map { it.id })
        assertTrue(CompanionLogic.search(data, "").isEmpty())
    }

    @Test fun blankWeaponFilterReturnsAllStableWeaponIdsAndNameSearchUsesCanonicalName() {
        val weapon = Weapon(
            stableWeaponId = "weapon_test_001",
            weaponType = "GREAT_SWORD",
            sourceOrdinal = 1,
            sourceName = "Test Source",
            sourceKey = "test",
            displayName = "Test Great Sword",
            nameSource = "TMO",
            rarity = 1,
            attack = 100,
            affinity = 0,
            slots = 0,
            defenseBonus = null,
            special = null,
            sharpness = null,
            forgeRecipe = null,
            upgradeFrom = null,
            upgradesTo = emptyList()
        )
        val weaponData = data.copy(weapons = listOf(weapon))
        assertEquals(listOf("weapon_test_001"), CompanionLogic.search(weaponData, "", EntityType.WEAPON).map { it.id })
        assertEquals(listOf("weapon_test_001"), CompanionLogic.search(weaponData, "great sword").map { it.id })
    }

    @Test fun questStarFiltersUseStructuredStars() {
        val quests = listOf(
            Quest("q6", "Six", "", "High Rank", stars = 6),
            Quest("q7", "Seven", "", "High Rank", stars = 7),
            Quest("q8", "Eight", "", "High Rank", stars = 8)
        )
        assertEquals(listOf("q6"), filterQuests(quests, "", 6).map { it.id })
        assertEquals(listOf("q7"), filterQuests(quests, "", 7).map { it.id })
        assertEquals(listOf("q8"), filterQuests(quests, "", 8).map { it.id })
    }

    @Test fun questCategoryAndAvailableStarFiltersUseStructuredFields() {
        val quests = listOf(
            Quest("v2", "Village Two", "", "Low Rank", stars = 2, category = "Village"),
            Quest("v6", "Village Six", "", "Low Rank", stars = 6, category = "Village"),
            Quest("g6", "Guild Six", "", "High Rank", stars = 6, category = "Guild"),
            Quest("g8", "Guild Eight", "", "High Rank", stars = 8, category = "Guild")
        )
        assertEquals(listOf("v2", "v6"), filterQuests(quests, "", null, QuestCategoryFilter.VILLAGE).map { it.id })
        assertEquals(listOf("g6", "g8"), filterQuests(quests, "", null, QuestCategoryFilter.GUILD).map { it.id })
        assertEquals(listOf(2, 6), validQuestStars(quests, "Village", QuestCategoryFilter.VILLAGE))
        assertEquals(listOf(6, 8), validQuestStars(quests, "", QuestCategoryFilter.GUILD))
    }

    @Test fun questFilterStateUsesHumanLabelsContextualAvailabilityAndOrStars() {
        val quests = listOf(
            Quest("v", "Village", "", "LOW", stars = 2, category = "Village"),
            Quest("g6", "Guild 6", "", "HIGH", stars = 6, category = "Guild"),
            Quest("g7", "Guild 7", "", "HIGH", stars = 7, category = "Guild"),
            Quest("g8", "Guild 8", "", "HIGH", stars = 8, category = "Guild"),
            Quest("e3", "Event 3", "", "LOW", stars = 3, category = "Event"),
            Quest("e4", "Event 4", "", "LOW", stars = 4, category = "Event"),
            Quest("e7", "Event 7", "", "HIGH", stars = 7, category = "Event"),
            Quest("drink", "Drink", "", stars = 0, category = "Drink"),
            Quest("spring", "Spring", "", stars = 0, category = "Hot Spring")
        )
        val index = QuestFilterIndex(quests)
        assertEquals(listOf(QuestRankFilter.LOW), index.availableRanks(QuestCategoryFilter.VILLAGE))
        assertEquals(listOf(QuestRankFilter.LOW, QuestRankFilter.HIGH), index.availableRanks(QuestCategoryFilter.EVENT))
        assertEquals(listOf(6, 7, 8), index.availableStars(QuestCategoryFilter.GUILD, QuestRankFilter.HIGH))
        assertEquals(listOf("g6", "g7"), filterQuests(quests, "", QuestFilterState(QuestCategoryFilter.GUILD, QuestRankFilter.HIGH, setOf(6, 7))).map { it.id })
        val normalized = normalizeQuestFilter(
            QuestFilterState(QuestCategoryFilter.VILLAGE, QuestRankFilter.HIGH, setOf(8)),
            index
        )
        assertEquals(QuestRankFilter.ALL, normalized.rank)
        assertTrue(normalized.stars.isEmpty())
        assertEquals("Event · Low Rank · ★3–4", questFilterSummary(QuestFilterState(QuestCategoryFilter.EVENT, QuestRankFilter.LOW, setOf(3, 4))))
        assertEquals("Event · Low Rank · ★3, 5, 7", questFilterSummary(QuestFilterState(QuestCategoryFilter.EVENT, QuestRankFilter.LOW, setOf(3, 5, 7))))
        assertEquals("Filters (3)", "Filters (${questFilterDimensionCount(QuestFilterState(QuestCategoryFilter.EVENT, QuestRankFilter.LOW, setOf(3, 4)))})")
        assertEquals("Drinks", QuestCategoryFilter.DRINK.displayLabel())
        assertEquals("Hot Spring", QuestCategoryFilter.HOT_SPRING.displayLabel())
    }

    @Test fun favoriteToggleAddsWithoutDuplicatesAndRemoves() {
        val once = ReferenceHistoryLogic.toggleFavorite(emptyList(), ReferenceEntityType.MATERIAL, "marrow", 1)
        assertEquals(1, once.size)
        assertTrue(ReferenceHistoryLogic.toggleFavorite(once, ReferenceEntityType.MATERIAL, "marrow", 2).isEmpty())
    }

    @Test fun favoritesAreNewestFirst() {
        var entries = ReferenceHistoryLogic.toggleFavorite(emptyList(), ReferenceEntityType.MONSTER, "narga", 1)
        entries = ReferenceHistoryLogic.toggleFavorite(entries, ReferenceEntityType.MATERIAL, "marrow", 2)
        assertEquals(listOf("marrow", "narga"), entries.map { it.entityId })
    }

    @Test fun recentIsUniqueNewestFirstAndLimitedToFive() {
        var entries = emptyList<RecentEntry>()
        (1..6).forEach { index -> entries = ReferenceHistoryLogic.recordRecent(entries, ReferenceEntityType.MONSTER, "m$index", index.toLong()) }
        entries = ReferenceHistoryLogic.recordRecent(entries, ReferenceEntityType.MONSTER, "m3", 7)
        assertEquals(listOf("m3", "m6", "m5", "m4", "m2"), entries.map { it.entityId })
    }

    @Test fun referenceCodecsRoundTripAndTolerateLegacyGarbage() {
        val favorites = listOf(
            FavoriteEntry(ReferenceEntityType.WEAPON, "weapon_test_001", 4),
            FavoriteEntry(ReferenceEntityType.MATERIAL, "marrow", 3)
        )
        val recent = listOf(
            RecentEntry(ReferenceEntityType.WEAPON, "weapon_test_001", 5),
            RecentEntry(ReferenceEntityType.MONSTER, "narga", 4)
        )
        assertEquals(favorites, ReferenceEntryCodec.decodeFavorites(ReferenceEntryCodec.encodeFavorites(favorites)))
        assertEquals(recent, ReferenceEntryCodec.decodeRecent(ReferenceEntryCodec.encodeRecent(recent)))
        assertTrue(ReferenceEntryCodec.decodeFavorites("legacy").isEmpty())
    }
}
