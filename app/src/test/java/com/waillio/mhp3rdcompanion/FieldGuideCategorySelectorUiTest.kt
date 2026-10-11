package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldGuideCategorySelectorUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun affordanceTracksLeftMiddleAndRightEdgesDuringHorizontalScroll() {
        rule.setContent {
            CompanionTheme {
                FieldGuideCategorySelector(filter = null, hasBlankQuery = true, onFilter = {}, onWeapons = {})
            }
        }
        val strip = rule.onNodeWithTag("field-guide-category-strip")
        val partialDragPx = with(rule.density) { 78.dp.toPx() }
        assertEquals("leftmost", strip.fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        strip.performTouchInput {
            swipeLeft(startX = centerX, endX = centerX - partialDragPx, durationMillis = 1_000)
        }
        rule.waitForIdle()
        assertEquals("middle", strip.fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        strip.performTouchInput { swipeLeft(durationMillis = 900) }
        rule.waitForIdle()
        assertEquals("rightmost", strip.fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        strip.performTouchInput { swipeRight(durationMillis = 900) }
        rule.waitForIdle()
        assertEquals("middle", strip.fetchSemanticsNode().config[SemanticsProperties.StateDescription])

        strip.performTouchInput { swipeRight(durationMillis = 900) }
        rule.waitForIdle()
        assertEquals("leftmost", strip.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun edgeStateMappingShowsOnlyTheAvailableDirection() {
        assertEquals("leftmost", fieldGuideScrollPosition(canScrollBackward = false, canScrollForward = true))
        assertEquals("middle", fieldGuideScrollPosition(canScrollBackward = true, canScrollForward = true))
        assertEquals("rightmost", fieldGuideScrollPosition(canScrollBackward = true, canScrollForward = false))
        assertEquals("no overflow", fieldGuideScrollPosition(canScrollBackward = false, canScrollForward = false))
    }
}
