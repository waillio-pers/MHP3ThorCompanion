package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
class AdaptiveCompactPortraitUiTest : MainActivityUiTestBase() {

    @Test
    fun compactPhoneChromeSettingsAndAboutRemainReachableAtEveryScaleEndpoint() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalStep = preferences.getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
        try {
            listOf(0, 4, 6).forEach { step ->
                rule.onNodeWithTag("adaptive-profile-compact_portrait").assertExists()
                rule.onNodeWithTag("settings-button").performClick()
                rule.onNodeWithTag("ui-scale-slider")
                    .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(step.toFloat()) }
                rule.onNodeWithTag("settings-done").performClick()
                rule.waitForIdle()

                rule.onNodeWithTag("global-search").assertIsDisplayed()
                rule.onNodeWithTag("settings-button").assertIsDisplayed()
                val searchBounds = rule.onNodeWithTag("global-search").fetchSemanticsNode().boundsInRoot
                assertTrue("Phone search field must retain usable width at scale $step", searchBounds.width > 190f)
                assertTrue("Search field must retain enough vertical room at scale $step", searchBounds.height >= 44f)
                val settingsBounds = rule.onNodeWithTag("settings-button").fetchSemanticsNode().boundsInRoot
                assertTrue("Search field and settings control should have matching heights", kotlin.math.abs(searchBounds.height - settingsBounds.height) < 1f)
                rule.onNodeWithTag("nav-home").assertIsDisplayed()
                rule.onNodeWithTag("nav-search").assertIsDisplayed()
                rule.onNodeWithTag("nav-favorites").assertIsDisplayed()

                rule.onNodeWithTag("settings-button").performClick()
                rule.onNodeWithTag("settings-about").performClick()
                rule.onNodeWithTag("about-dialog").assertIsDisplayed()
                rule.onNodeWithTag("about-done").assertIsDisplayed()
                rule.onNodeWithTag("about-scroll-content")
                    .performScrollToNode(hasTestTag("about-source-link-17"))
                rule.onNodeWithTag("about-source-link-17").assertIsDisplayed()
                rule.onNodeWithTag("about-done").performClick()
                rule.waitForIdle()
            }
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, originalStep).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    @Test
    fun globalSearchShowsMidStringEditsAndDeletionWithoutSubmitting() {
        val search = rule.onNodeWithTag("global-search")
        search.performTextInput("nargacuga")
        search.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("nargacuga")))
        search.performTextInputSelection(TextRange(4, 4))
        search.performTextInput("a")
        search.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("nargaacuga")))
        search.performTextClearance()
        search.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }
}
