package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-xhdpi")
class WeaponRelatedNavigationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun hammerRelatedWeaponLinksUseStableIdsAndBackRestoresPreviousDetails() {
        val preferences = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val originalStep = preferences.getInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP)
        val data = CompanionRepository(ApplicationProvider.getApplicationContext<android.content.Context>()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        val catalog = repository.catalog("HAMMER")
        val worn = catalog.weapons.single { it.name == "Worn Hammer" }
        val weathered = catalog.weapons.single { it.name == "Weathered Hammer" }
        val pulsating = catalog.weapons.single { it.name == "Pulsating Core" }
        try {
            listOf(0, 4, 6).forEach { step ->
                setScale(step)
                rule.onNodeWithTag("nav-search").performClick()
                rule.onNodeWithTag("field-filter-weapons").performClick()
                rule.onNodeWithTag("weapon-type-hammer").performClick()
                rule.onNodeWithTag("weapon-search").performTextClearance()
                rule.onNodeWithTag("weapon-search").performTextInput("Worn Hammer")
                rule.onNodeWithTag("weapon-search").performImeAction()
                rule.onNodeWithTag("weapon-search").assertTextEquals("Worn Hammer")
                rule.onNodeWithTag("weapon-search-result-${worn.id}").performScrollTo().performClick()
                rule.onNodeWithTag("weapon-detail-title-${worn.id}").assertIsDisplayed()
                rule.onNodeWithTag("weapon-upgrade-to-${weathered.id}").performScrollTo().performClick()
                rule.onNodeWithTag("weapon-detail-title-${weathered.id}").assertIsDisplayed()
                rule.onNodeWithTag("weapon-upgrade-to-${pulsating.id}").performScrollTo().performClick()
                rule.onNodeWithTag("weapon-detail-title-${pulsating.id}").assertIsDisplayed()

                rule.activity.onBackPressedDispatcher.onBackPressed()
                rule.onNodeWithTag("weapon-detail-title-${weathered.id}").assertIsDisplayed()
                rule.activity.onBackPressedDispatcher.onBackPressed()
                rule.onNodeWithTag("weapon-detail-title-${worn.id}").assertIsDisplayed()

                rule.onNodeWithTag("weapon-detail-close").performClick()
                rule.onNodeWithTag("weapon-search").performTextClearance()
                rule.onNodeWithTag("weapon-search").performTextInput("Pulsating Core")
                rule.onNodeWithTag("weapon-search").performImeAction()
                rule.onNodeWithTag("weapon-search-result-${pulsating.id}").performClick()
                rule.onNodeWithTag("weapon-detail-title-${pulsating.id}").assertIsDisplayed()
                rule.onNodeWithTag("weapon-related-link-${weathered.id}").performScrollTo().performClick()
                rule.onNodeWithTag("weapon-detail-title-${weathered.id}").assertIsDisplayed()
                rule.activity.onBackPressedDispatcher.onBackPressed()
                rule.onNodeWithTag("weapon-detail-title-${pulsating.id}").assertIsDisplayed()

                rule.onNodeWithTag("weapon-upgrade-path").performScrollTo().assertIsDisplayed()
                if (rule.onAllNodesWithTag("weapon-ancestry-node-title-${worn.id}").fetchSemanticsNodes().isEmpty()) {
                    rule.onNodeWithTag("weapon-upgrade-path").performClick()
                }
                rule.onNodeWithTag("weapon-ancestry-node-title-${worn.id}").performScrollTo().performClick()
                rule.onNodeWithTag("weapon-detail-title-${worn.id}").assertIsDisplayed()
                rule.activity.onBackPressedDispatcher.onBackPressed()
                rule.onNodeWithTag("weapon-detail-title-${pulsating.id}").assertIsDisplayed()
                rule.onNodeWithTag("weapon-detail-close").performClick()
                rule.onNodeWithTag("nav-home").performClick()
                rule.waitForIdle()
            }
        } finally {
            preferences.edit().putInt(UI_SCALE_STEP_KEY, originalStep).commit()
            rule.runOnUiThread { rule.activity.recreate() }
            rule.waitForIdle()
        }
    }

    private fun setScale(step: Int) {
        rule.onNodeWithTag("settings-button").performClick()
        rule.onNodeWithTag("ui-scale-slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(step.toFloat()) }
        val hundredths = 80 + step * 5
        val label = "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
        rule.onNodeWithTag("settings-scale-value", useUnmergedTree = true).assertTextEquals(label)
        rule.onNodeWithTag("settings-done").performClick()
        rule.waitForIdle()
    }
}
