package com.waillio.mhp3rdcompanion

import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.WeaponSpecialType
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeaponBrowseFiltersTest {
    private val repository by lazy {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data
        WeaponRepository.fromProduction(data.weapons, data.materials)
    }

    private val hammer by lazy { repository.catalog("HAMMER") }

    @Test
    fun defaultTreeModeAndUnfilteredClassOrderRemainUnchanged() {
        val state = WeaponBrowseState()
        assertEquals(WeaponBrowseMode.TREE, state.browseMode)
        assertEquals(null, state.specialType)
        assertEquals(null, state.minimumAffinity)
        assertEquals(null, state.rarityFilter)
        assertEquals(null, state.minimumSlots)
        assertFalse(WeaponBrowseFilters().isActive)
        val expanded = initialExpandedIds(hammer)
        assertTrue(visibleTree(hammer, expanded).isNotEmpty())
        assertEquals(
            hammer.weapons.sortedBy { it.sourceOrdinal }.map { it.id },
            browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.TREE).map { it.id }
        )
        val search = browseWeapons(hammer, "iron", WeaponBrowseFilters(), WeaponBrowseMode.TREE)
        assertEquals(search.sortedWith(compareBy({ it.name.lowercase() }, { it.id })).map { it.id }, search.map { it.id })
    }

    @Test
    fun optionsAreRealStructuredValuesAndScopedToHammer() {
        val types = availableWeaponSpecialTypes(hammer)
        val affinities = availableWeaponAffinities(hammer)
        val rarities = availableWeaponRarities(hammer)
        val slots = availableMinimumWeaponSlots(hammer)
        assertEquals(types.distinct(), types)
        assertEquals(hammer.weapons.mapNotNull { it.special?.type }.toSet(), types.toSet())
        assertEquals(hammer.weapons.map { it.affinity }.distinct().sorted(), affinities)
        assertEquals(hammer.weapons.map { it.rarity }.distinct().sorted(), rarities)
        assertEquals((1..(hammer.weapons.maxOf { it.slots })).toList(), slots)
        assertEquals(-70, affinities.first())
        assertEquals(40, affinities.last())
        assertEquals(1..7, rarities.first()..rarities.last())
        assertEquals(3, slots.last())
        assertEquals(
            setOf(WeaponSpecialType.DRAGON, WeaponSpecialType.FIRE, WeaponSpecialType.ICE, WeaponSpecialType.PARALYSIS,
                WeaponSpecialType.POISON, WeaponSpecialType.SLEEP, WeaponSpecialType.THUNDER, WeaponSpecialType.WATER),
            repository.weapons.mapNotNull { it.special?.type }.toSet()
        )
        assertEquals(
            listOf(-70, -50, -40, -30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50),
            repository.weapons.map { it.affinity }.distinct().sorted()
        )
        assertEquals(-70, repository.weapons.minOf { it.affinity })
        assertEquals(50, repository.weapons.maxOf { it.affinity })
        assertEquals(1..7, repository.weapons.minOf { it.rarity }..repository.weapons.maxOf { it.rarity })
        assertEquals(0..3, repository.weapons.minOf { it.slots }..repository.weapons.maxOf { it.slots })
    }

    @Test
    fun hammerFireReturnsOnlyStructuredFireHammers() {
        val filters = WeaponBrowseFilters(specialType = WeaponSpecialType.FIRE)
        val results = browseWeapons(hammer, "", filters, WeaponBrowseMode.TREE)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.weaponType == "HAMMER" && it.special?.type == WeaponSpecialType.FIRE })
        assertEquals(hammer.weapons.filter { it.special?.type == WeaponSpecialType.FIRE }.map { it.id }.toSet(), results.map { it.id }.toSet())
    }

    @Test
    fun minimumAffinityUsesInclusiveGreaterThanOrEqual() {
        val threshold = 10
        val actual = browseWeapons(hammer, "", WeaponBrowseFilters(minimumAffinity = threshold), WeaponBrowseMode.TREE)
        assertTrue(actual.isNotEmpty())
        assertTrue(actual.all { it.affinity >= threshold })
        assertTrue(hammer.weapons.filter { it.affinity == threshold }.all { item -> actual.any { it.id == item.id } })
        assertFalse(actual.any { it.affinity < threshold })
    }

    @Test
    fun rarityFilterIsAnExactStructuredMatch() {
        val expectedRarity = 5
        val actual = browseWeapons(hammer, "", WeaponBrowseFilters(rarity = expectedRarity), WeaponBrowseMode.TREE)
        assertTrue(actual.isNotEmpty())
        assertTrue(actual.all { it.rarity == expectedRarity })
        assertEquals(hammer.weapons.filter { it.rarity == expectedRarity }.map { it.id }.toSet(), actual.map { it.id }.toSet())
    }

    @Test
    fun minimumSlotsUsesInclusiveThreshold() {
        val expectedMinimum = 2
        val actual = browseWeapons(hammer, "", WeaponBrowseFilters(minimumSlots = expectedMinimum), WeaponBrowseMode.TREE)
        assertTrue(actual.isNotEmpty())
        assertTrue(actual.all { it.slots >= expectedMinimum })
        assertTrue(hammer.weapons.filter { it.slots == expectedMinimum }.all { item -> actual.any { it.id == item.id } })
        assertFalse(actual.any { it.slots < expectedMinimum })
    }

    @Test
    fun elementAndAffinityFiltersCombineWithAnd() {
        val fireHammer = hammer.weapons.first { it.special?.type == WeaponSpecialType.FIRE }
        val filters = WeaponBrowseFilters(specialType = WeaponSpecialType.FIRE, minimumAffinity = fireHammer.affinity)
        val actual = browseWeapons(hammer, "", filters, WeaponBrowseMode.TREE)
        assertTrue(actual.any { it.id == fireHammer.id })
        assertEquals(
            hammer.weapons.filter { it.special?.type == WeaponSpecialType.FIRE && it.affinity >= fireHammer.affinity }.map { it.id }.toSet(),
            actual.map { it.id }.toSet()
        )
    }

    @Test
    fun elementRarityAndSlotsCombineWithAnd() {
        val fireHammer = hammer.weapons.first { it.special?.type == WeaponSpecialType.FIRE && it.slots > 0 }
        val filters = WeaponBrowseFilters(
            specialType = WeaponSpecialType.FIRE,
            rarity = fireHammer.rarity,
            minimumSlots = fireHammer.slots
        )
        val actual = browseWeapons(hammer, "", filters, WeaponBrowseMode.TREE)
        assertTrue(actual.any { it.id == fireHammer.id })
        assertEquals(
            hammer.weapons.filter {
                it.special?.type == WeaponSpecialType.FIRE && it.rarity == fireHammer.rarity && it.slots >= fireHammer.slots
            }.map { it.id }.toSet(),
            actual.map { it.id }.toSet()
        )
    }

    @Test
    fun allFourFiltersCombineWithAndAndClearRestoresClassResults() {
        val fireHammer = hammer.weapons.first { it.special?.type == WeaponSpecialType.FIRE && it.slots > 0 }
        val filters = WeaponBrowseFilters(WeaponSpecialType.FIRE, fireHammer.affinity, fireHammer.rarity, fireHammer.slots)
        val actual = browseWeapons(hammer, "", filters, WeaponBrowseMode.TREE)
        assertTrue(actual.any { it.id == fireHammer.id })
        val expected = hammer.weapons.filter {
            it.special?.type == WeaponSpecialType.FIRE && it.affinity >= fireHammer.affinity &&
                it.rarity == fireHammer.rarity && it.slots >= fireHammer.slots
        }
        assertEquals(expected.map { it.id }.toSet(), actual.map { it.id }.toSet())
        val cleared = browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.TREE)
        assertEquals(hammer.weapons.map { it.id }.toSet(), cleared.map { it.id }.toSet())
    }

    @Test
    fun attackSortSupportsBothDirectionsAndDeterministicCanonicalTies() {
        val descending = browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.ATTACK, descending = true)
        val ascending = browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.ATTACK, descending = false)
        assertEquals(descending.sortedWith(compareByDescending<com.waillio.mhp3rdcompanion.data.Weapon> { it.attack }.thenBy { it.sourceOrdinal }.thenBy { it.id }).map { it.id }, descending.map { it.id })
        assertEquals(ascending.sortedWith(compareBy<com.waillio.mhp3rdcompanion.data.Weapon> { it.attack }.thenBy { it.sourceOrdinal }.thenBy { it.id }).map { it.id }, ascending.map { it.id })
        assertTrue(descending.zipWithNext().all { (left, right) -> left.attack >= right.attack })
        assertTrue(ascending.zipWithNext().all { (left, right) -> left.attack <= right.attack })
        assertEquals(hammer.weapons.maxOf { it.attack }, descending.first().attack)
        assertDeterministicTies(descending) { it.attack }
        assertDeterministicTies(ascending) { it.attack }
    }

    @Test
    fun raritySortSupportsBothDirectionsAndDeterministicCanonicalTies() {
        val descending = browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.RARITY, descending = true)
        val ascending = browseWeapons(hammer, "", WeaponBrowseFilters(), WeaponBrowseMode.RARITY, descending = false)
        assertTrue(descending.zipWithNext().all { (left, right) -> left.rarity >= right.rarity })
        assertTrue(ascending.zipWithNext().all { (left, right) -> left.rarity <= right.rarity })
        assertDeterministicTies(descending) { it.rarity }
        assertDeterministicTies(ascending) { it.rarity }
    }

    @Test
    fun rarityFilterAndAttackSortAreIndependent() {
        val filters = WeaponBrowseFilters(rarity = 5)
        val actual = browseWeapons(hammer, "", filters, WeaponBrowseMode.ATTACK, descending = true)
        assertTrue(actual.all { it.rarity == 5 })
        assertTrue(actual.zipWithNext().all { (left, right) -> left.attack >= right.attack })
        assertEquals(hammer.weapons.filter { it.rarity == 5 }.map { it.id }.toSet(), actual.map { it.id }.toSet())
    }

    @Test
    fun slotsFilterAndRaritySortAreIndependent() {
        val filters = WeaponBrowseFilters(minimumSlots = 2)
        val actual = browseWeapons(hammer, "", filters, WeaponBrowseMode.RARITY, descending = true)
        assertTrue(actual.all { it.slots >= 2 })
        assertTrue(actual.zipWithNext().all { (left, right) -> left.rarity >= right.rarity })
        assertEquals(hammer.weapons.filter { it.slots >= 2 }.map { it.id }.toSet(), actual.map { it.id }.toSet())
    }

    @Test
    fun searchFiltersAndFlatSortComposeAndImpossibleQueryDoesNotRelaxFilters() {
        val fireHammer = hammer.weapons.first { it.special?.type == WeaponSpecialType.FIRE && it.slots > 0 }
        val filters = WeaponBrowseFilters(specialType = WeaponSpecialType.FIRE, minimumSlots = 1)
        val query = fireHammer.name
        val actual = browseWeapons(hammer, query, filters, WeaponBrowseMode.ATTACK, descending = true)
        assertTrue(actual.isNotEmpty())
        assertEquals(
            hammer.weapons.filter {
                it.name.contains(query, ignoreCase = true) && it.special?.type == WeaponSpecialType.FIRE && it.slots >= 1
            }.sortedWith(compareByDescending<com.waillio.mhp3rdcompanion.data.Weapon> { it.attack }.thenBy { it.sourceOrdinal }.thenBy { it.id }).map { it.id },
            actual.map { it.id }
        )
        assertTrue(browseWeapons(hammer, "no such hammer", filters, WeaponBrowseMode.ATTACK).isEmpty())
    }

    @Test
    fun filteredTreeKeepsOnlyMatchesAndRealStructuralAncestorsInSourceOrder() {
        val scenario = availableWeaponSpecialTypes(hammer)
            .flatMap { type -> availableWeaponRarities(hammer).map { rarity -> WeaponBrowseFilters(type, rarity = rarity) } }
            .firstNotNullOfOrNull { filters ->
                val matches = hammer.weapons.filter { it.matches(filters) }.map { it.id }.toSet()
                filteredWeaponTree(hammer, matches).takeIf { rows -> rows.any { it.contextOnly } }?.let { filters to it }
            }
        requireNotNull(scenario) { "Production Hammer trees should contain a filtered descendant with a structural ancestor." }
        val (filters, rows) = scenario
        val matchIds = hammer.weapons.filter { it.matches(filters) }.map { it.id }.toSet()
        assertTrue(rows.filterNot { it.contextOnly }.all { it.weapon.id in matchIds && it.weapon.matches(filters) })
        assertTrue(rows.filter { it.contextOnly }.all { it.weapon.id !in matchIds && !it.weapon.matches(filters) })
        assertTrue(rows.all { it.weapon.id in hammer.weaponById })
        val rowIds = rows.map { it.weapon.id }.toSet()
        assertEquals(
            flattenTree(hammer).map { it.weapon.id }.filter { it in rowIds },
            rows.map { it.weapon.id }
        )
    }

    @Test
    fun productionWeaponJsonShaRemainsUnchanged() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sha = context.assets.open("mhp3rd-data.json").use { input ->
            MessageDigest.getInstance("SHA-256").digest(input.readBytes()).joinToString("") { "%02X".format(it) }
        }
        assertEquals("657CFA9376FC8CBE56C67E67D4D1A27C455D21021F51E21B62427C8DDBB26155", sha)
    }

    private fun assertDeterministicTies(
        weapons: List<com.waillio.mhp3rdcompanion.data.Weapon>,
        value: (com.waillio.mhp3rdcompanion.data.Weapon) -> Int
    ) {
        weapons.groupBy(value).values.forEach { tied ->
            assertEquals(tied.sortedWith(compareBy({ it.sourceOrdinal }, { it.id })).map { it.id }, tied.map { it.id })
        }
    }
}
