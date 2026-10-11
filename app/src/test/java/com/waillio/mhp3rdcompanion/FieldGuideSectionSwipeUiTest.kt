package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.waillio.mhp3rdcompanion.data.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun settingOffLeavesSectionUnchanged() {
        val harness = mount(enabled = false)

        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertEquals(null, harness.selected.value)
        rule.onNodeWithTag("field-filter-all").assertIsSelected()
    }

    @Test
    fun intentionalSwipeMovesOneAdjacentSectionAndShowsItsChip() {
        val harness = mount(enabled = true, initialCategory = EntityType.MAP)

        // A full-width swipe still advances only one category: Map -> Weapons.
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertEquals(EntityType.WEAPON, harness.selected.value)
        rule.onNodeWithTag("field-filter-weapons").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithTag("selected-section").assertTextEquals(EntityType.WEAPON.name)

        // The Weapons surface is deliberately excluded from further section swipes.
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(EntityType.WEAPON, harness.selected.value)
    }

    @Test
    fun rightSwipeMovesExactlyOneSectionBack() {
        val harness = mount(enabled = true, initialCategory = EntityType.SMALL_MONSTER)

        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeRight() }
        rule.waitForIdle()

        assertEquals(EntityType.MONSTER, harness.selected.value)
        rule.onNodeWithTag("field-filter-monster").assertIsDisplayed().assertIsSelected()
        rule.onNodeWithTag("selected-section").assertTextEquals(EntityType.MONSTER.name)
    }

    @Test
    fun firstAndLastSectionsDoNotWrap() {
        val first = mount(enabled = true, initialCategory = null)
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeRight() }
        rule.waitForIdle()
        assertEquals(null, first.selected.value)

        first.selected.value = EntityType.TRAINING
        rule.waitForIdle()
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(EntityType.TRAINING, first.selected.value)
    }

    @Test
    fun shortAndVerticalDominantDragsDoNotNavigate() {
        val harness = mount(enabled = true, initialCategory = EntityType.MONSTER)
        val shortDistancePx = with(rule.density) { 40.dp.toPx() }
        val horizontalPx = with(rule.density) { 90.dp.toPx() }
        val verticalPx = with(rule.density) { 100.dp.toPx() }

        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(-shortDistancePx, 0f))
            up()
        }
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput {
            val start = center
            down(start)
            moveTo(start + Offset(-horizontalPx, verticalPx))
            up()
        }
        rule.waitForIdle()

        assertEquals(EntityType.MONSTER, harness.selected.value)
    }

    @Test
    fun nestedHorizontalChildConsumesSwipe() {
        val horizontal = mount(
            enabled = true,
            initialCategory = EntityType.MAP,
            horizontalChild = true
        )
        rule.onNodeWithTag("gesture-horizontal-child").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertTrue(horizontal.horizontalScroll!!.value > 0)
        assertEquals(EntityType.MAP, horizontal.selected.value)
    }

    @Test
    fun sectionSwipeCancelsClickableChildTap() {
        val harness = mount(
            enabled = true,
            initialCategory = EntityType.MAP,
            clickableChild = true
        )

        rule.onNodeWithTag("gesture-clickable-child").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertEquals(EntityType.WEAPON, harness.selected.value)
        assertEquals(false, harness.clickableChild!!.value)
    }

    @Test
    fun verticalListScrollsNormallyWithoutChangingSection() {
        val vertical = mount(
            enabled = true,
            initialCategory = EntityType.MONSTER,
            verticalChild = true
        )
        rule.onNodeWithTag("gesture-vertical-child").performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertTrue(vertical.verticalList!!.firstVisibleItemIndex > 0)
        assertEquals(EntityType.MONSTER, vertical.selected.value)
    }

    @Test
    fun swipeIntoOffscreenCategoryBringsSelectedChipIntoView() {
        val harness = mount(enabled = true)
        rule.onNodeWithTag("field-filter-weapons").assertIsNotDisplayed()

        repeat(4) {
            rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
            rule.waitForIdle()
        }
        assertEquals(EntityType.MAP, harness.selected.value)
        rule.onNodeWithTag("field-guide-swipe-region").performTouchInput { swipeLeft() }
        rule.waitForIdle()

        assertEquals(EntityType.WEAPON, harness.selected.value)
        rule.onNodeWithTag("field-filter-weapons").assertIsDisplayed()
    }

    private fun mount(
        enabled: Boolean,
        initialCategory: EntityType? = null,
        horizontalChild: Boolean = false,
        verticalChild: Boolean = false,
        clickableChild: Boolean = false
    ): SwipeHarnessHandles {
        var handles: SwipeHarnessHandles? = null
        rule.setContent {
            val selected = remember(initialCategory) { mutableStateOf(initialCategory) }
            val horizontal = if (horizontalChild) rememberScrollState() else null
            val vertical = if (verticalChild) rememberLazyListState() else null
            val clicked = if (clickableChild) remember { mutableStateOf(false) } else null
            handles = SwipeHarnessHandles(selected, horizontal, vertical, clicked)
            SwipeHarness(enabled, selected, horizontal, vertical, clicked)
        }
        rule.waitForIdle()
        return requireNotNull(handles)
    }
}

private data class SwipeHarnessHandles(
    val selected: MutableState<EntityType?>,
    val horizontalScroll: ScrollState?,
    val verticalList: LazyListState?,
    val clickableChild: MutableState<Boolean>?
)

@Composable
private fun SwipeHarness(
    enabled: Boolean,
    selected: MutableState<EntityType?>,
    horizontalScroll: ScrollState?,
    verticalList: LazyListState?,
    clickableChild: MutableState<Boolean>?
) {
    Column(Modifier.fillMaxSize()) {
        FieldGuideCategorySelector(
            filter = selected.value,
            hasBlankQuery = true,
            onFilter = { selected.value = it },
            onWeapons = { selected.value = EntityType.WEAPON }
        )
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .fieldGuideSectionSwipeInput(fieldGuideSwipeAllowed(enabled, selected.value)) { direction ->
                    adjacentFieldGuideSection(selected.value, direction)?.let { destination ->
                        selectFieldGuideCategory(
                            destination.category,
                            hasBlankQuery = true,
                            onFilter = { selected.value = it },
                            onWeapons = { selected.value = EntityType.WEAPON }
                        )
                    }
                }
                .testTag("field-guide-swipe-region")
        ) {
            when {
                horizontalScroll != null -> Row(
                    Modifier.fillMaxWidth().height(110.dp)
                        .horizontalScroll(horizontalScroll)
                        .testTag("gesture-horizontal-child")
                ) {
                    repeat(8) { index ->
                        Spacer(Modifier.width(110.dp).fillMaxHeight())
                    }
                }
                verticalList != null -> LazyColumn(
                    Modifier.fillMaxWidth().height(220.dp).testTag("gesture-vertical-child"),
                    state = verticalList
                ) {
                    items((0..19).toList()) { index ->
                        Spacer(Modifier.fillMaxWidth().height(72.dp).testTag("gesture-vertical-row-$index"))
                    }
                }
                clickableChild != null -> Box(
                    Modifier.fillMaxWidth().height(180.dp)
                        .clickable { clickableChild.value = true }
                        .testTag("gesture-clickable-child")
                )
            }
            androidx.compose.material3.Text(
                selected.value?.name ?: "ALL",
                modifier = Modifier.align(Alignment.BottomCenter).testTag("selected-section")
            )
        }
    }
}
