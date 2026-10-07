package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.WeaponSpecialType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeaponBrowseControlsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun compactFiltersAndSortSurviveDetailBackAndTreeSwitch() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext<android.content.Context>()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        val catalog = repository.catalog("HAMMER")
        val target = catalog.weapons.first { it.special?.type == WeaponSpecialType.FIRE && it.slots > 0 }
        val minimumAffinity = availableWeaponAffinities(catalog).first()
        val minimumSlots = availableMinimumWeaponSlots(catalog).first()

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-weapons").performClick()
        rule.onNodeWithTag("weapon-type-hammer").performClick()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
        rule.onNodeWithTag("weapon-mode-tree").assertIsDisplayed()

        rule.onNodeWithTag("weapon-filters-open").performClick()
        rule.onNodeWithTag("weapon-filter-element").performClick()
        rule.onNodeWithTag("weapon-filter-element-fire").performClick()
        rule.onNodeWithTag("weapon-filter-affinity").performClick()
        rule.onNodeWithTag("weapon-filter-affinity-value-${minimumAffinity.toString().replace("-", "minus-")}")
            .performClick()
        rule.onNodeWithTag("weapon-filter-rarity").performClick()
        rule.onNodeWithTag("weapon-filter-rarity-value-${target.rarity}").performClick()
        rule.onNodeWithTag("weapon-filter-slots").performClick()
        rule.onNodeWithTag("weapon-filter-slots-value-$minimumSlots").performClick()
        rule.onNodeWithTag("weapon-filters-done").performClick()

        rule.onNodeWithTag("weapon-active-filter-summary").assertIsDisplayed()
        rule.onNodeWithTag("weapon-mode-attack").performClick()
        rule.onNodeWithTag("weapon-search").performTextInput("no weapon has this name")
        rule.onNodeWithTag("weapon-search").performImeAction()
        rule.onNodeWithTag("weapon-filter-empty-state").assertIsDisplayed()
        rule.onNodeWithTag("weapon-active-filter-summary").assertIsDisplayed()
        rule.onNodeWithTag("weapon-search-clear").performClick()
        rule.onNodeWithTag("weapon-sort-direction").performClick() // ascending
        rule.onNodeWithContentDescription("Ascending order").assertExists()

        rule.onNodeWithTag("weapon-search").performTextInput(target.name)
        rule.onNodeWithTag("weapon-search").performImeAction()
        val expected = browseWeapons(
            catalog,
            target.name,
            WeaponBrowseFilters(WeaponSpecialType.FIRE, minimumAffinity, target.rarity, minimumSlots),
            WeaponBrowseMode.ATTACK,
            descending = false
        ).first()
        assertTrue(expected.id == target.id || expected.name == target.name)
        rule.onNodeWithTag("weapon-flat-result-${expected.id}").performScrollTo().performClick()
        rule.onNodeWithTag("weapon-detail-title-${expected.id}").assertIsDisplayed()
        rule.onNodeWithTag("weapon-detail-close").performClick()

        rule.onNodeWithTag("weapon-active-filter-summary").assertIsDisplayed()
        rule.onNodeWithTag("weapon-search").assertTextContains(target.name)
        rule.onNodeWithTag("weapon-flat-results").assertIsDisplayed()
        rule.onNodeWithContentDescription("Ascending order").assertExists()

        rule.onNodeWithTag("weapon-mode-tree").performClick()
        rule.onNodeWithTag("weapon-search-results").assertIsDisplayed()
        rule.onNodeWithTag("weapon-mode-rarity").performClick()
        rule.onNodeWithTag("weapon-flat-results").assertIsDisplayed()
        rule.onNodeWithTag("weapon-search").assertTextContains(target.name)
        rule.onNodeWithTag("weapon-mode-tree").performClick()
        rule.onNodeWithTag("weapon-search-results").assertIsDisplayed()
        rule.onNodeWithTag("weapon-filters-clear-inline").performClick()
        rule.onNodeWithTag("weapon-active-filter-summary").assertDoesNotExist()
        rule.onNodeWithTag("weapon-search-clear").performClick()
        rule.onNodeWithTag("weapon-tree").assertIsDisplayed()
        val defaultTree = visibleTree(catalog, initialExpandedIds(catalog))
        if (defaultTree.isNotEmpty()) rule.onNodeWithTag("weapon-row-${defaultTree.first().weapon.id}").assertExists()
    }
}
