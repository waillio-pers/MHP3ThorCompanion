package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkillFilterAutoHideUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun portraitFilterRowHidesAfterDownwardScrollAndReturnsOnUpwardScroll() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertDoesNotExist()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()
    }
}
