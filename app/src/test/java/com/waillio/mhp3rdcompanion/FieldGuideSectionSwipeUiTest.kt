package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.waillio.mhp3rdcompanion.data.Material
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldGuideSectionSwipeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun settingOffDisablesPagerDrag() {
        mount(enabled = false, initialPage = FieldGuideRootPage.LARGE_MONSTERS)

        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.LARGE_MONSTERS.id)
        rule.onNodeWithTag("field-filter-monster").assertIsSelected()
    }

    @Test
    fun realPagerMovesOnePageAndSynchronizesSelectedChip() {
        mount(enabled = true, initialPage = FieldGuideRootPage.LARGE_MONSTERS)

        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.SMALL_MONSTERS.id)
        rule.onNodeWithTag("field-filter-small_monster").assertIsSelected()
    }

    @Test
    fun charmPageIsImmediatelyAfterQuestsAndRootWeaponsParticipates() {
        mount(enabled = true, initialPage = FieldGuideRootPage.QUESTS)
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.CHARM_TABLES.id)
        rule.onNodeWithTag("field-filter-charm_tables").assertIsSelected()

        rule.onNodeWithTag("field-filter-weapons").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.WEAPONS.id)
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.SKILLS.id)
    }

    @Test
    fun firstPageDoesNotWrap() {
        mount(enabled = true, initialPage = FieldGuideRootPage.ALL)
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.ALL.id)
    }

    @Test
    fun lastPageDoesNotWrap() {
        mount(enabled = true, initialPage = FieldGuideRootPage.TRAINING)
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.TRAINING.id)
    }

    @Test
    fun rootMapsIndexParticipatesInCanonicalSectionPaging() {
        mount(enabled = true, initialPage = FieldGuideRootPage.MAPS)
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.WEAPONS.id)
        rule.onNodeWithTag("field-filter-weapons").assertIsSelected()
    }

    @Test
    fun itemsPageHasRealContentBeforeItsCategoryBecomesSelected() {
        val material = Material("preview-item", "Herb", description = "Healing item", sources = emptyList(), uses = emptyList())
        mount(enabled = true, initialPage = FieldGuideRootPage.MAPS, materials = listOf(material))

        // Items is the preceding pager page; the active Maps search result list is empty.
        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeRight() }
        rule.waitForIdle()

        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.ITEMS.id)
        rule.onNodeWithTag("result-material-preview-item").assertExists()
    }

    @Test
    fun weaponTreeChildKeepsItsHorizontalGestureFromRootPager() {
        mount(enabled = true, initialPage = FieldGuideRootPage.WEAPONS, weaponTypeSelected = true)

        rule.onNodeWithTag("field-guide-pager").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        rule.onNodeWithTag("selected-section").assertTextEquals(FieldGuideRootPage.WEAPONS.id)
    }

    private fun mount(enabled: Boolean, initialPage: FieldGuideRootPage, weaponTypeSelected: Boolean = false, materials: List<Material> = emptyList()) {
        rule.setContent {
            CompanionTheme {
                PagerHarness(enabled, initialPage, weaponTypeSelected, materials)
            }
        }
        rule.waitForIdle()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PagerHarness(enabled: Boolean, initialPage: FieldGuideRootPage, weaponTypeSelected: Boolean, materials: List<Material> = emptyList()) {
    var selected by remember { mutableStateOf(initialPage) }
    val weaponState = remember { WeaponScreenState() }
    if (weaponTypeSelected) weaponState.selectedType = "GREAT_SWORD"
    val filter = selected.entityType
    ColumnForTest {
        androidx.compose.material3.Text(selected.id, modifier = Modifier.testTag("selected-section"))
        SearchScreen(
            query = "",
            filter = filter,
            results = emptyList(),
            materials = materials,
            listState = rememberLazyListState(),
            weaponScreenState = weaponState,
            onFilter = { selected = fieldGuidePageForCategory(it) },
            onResult = {},
            fieldGuideSwipeEnabled = enabled,
            selectedCharmTablesRoot = selected == FieldGuideRootPage.CHARM_TABLES,
            onFieldGuidePageSelected = { selected = it },
            charmTablesContent = { Box(Modifier.fillMaxSize().testTag("charm-page-content")) }
        )
    }
}

@Composable
private fun ColumnForTest(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) { content() }
}
