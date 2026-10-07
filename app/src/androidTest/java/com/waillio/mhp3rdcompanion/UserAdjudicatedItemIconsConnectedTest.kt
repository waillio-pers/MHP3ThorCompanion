package com.waillio.mhp3rdcompanion

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One connected Thor lower-screen smoke across six user-selected icon families. */
@RunWith(AndroidJUnit4::class)
class UserAdjudicatedItemIconsConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun selectedItemsOpenNormallyAndShowTheirUserChosenIcons() {
        val samples = listOf(
            Sample("Iron Pickaxe", "item_iron_pickaxe", 85, "user-icon-iron-pickaxe.png"),
            Sample("Antidote Horn", "item_antidote_horn", 147, "user-icon-antidote-horn.png"),
            Sample("Poison Smoke Bmb", "item_poison_smoke_bmb", 66, "user-icon-poison-smoke.png"),
            Sample("Golden Egg", "item_golden_egg", 604, "user-icon-golden-egg.png"),
            Sample("Empty Phial", "item_empty_phial", 130, "user-icon-empty-phial.png"),
            Sample("Rusted Fragment", "item_rusted_fragment", 598, "user-icon-fragment.png"),
        )
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-material").performClick()
        val captureDir = "/sdcard/Download/thor-hardening-b1-user-icons"
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p $captureDir"))
            .use { it.readBytes() }

        samples.forEach { sample ->
            val search = rule.onNodeWithTag("global-search")
            search.performTextClearance()
            search.performTextInput(sample.name)
            rule.waitForIdle()
            closeSoftKeyboard()
            rule.onNodeWithTag("result-material-${sample.itemId}").performClick()
            rule.onNodeWithTag("screen-material").assertIsDisplayed()
            rule.onNodeWithTag("item-icon-${sample.gameItemId}").assertIsDisplayed()
            assertTrue(
                "Fallback icon must remain absent for ${sample.name}",
                rule.onAllNodesWithTag("item-icon-fallback").fetchSemanticsNodes().isEmpty()
            )
            rule.onNodeWithContentDescription(sample.name, useUnmergedTree = true).assertIsDisplayed()
            rule.waitForIdle()
            ParcelFileDescriptor.AutoCloseInputStream(
                automation.executeShellCommand("screencap -p $captureDir/${sample.screenshotName}")
            ).use { it.readBytes() }
            println("THOR_B1_SCREENSHOT=$captureDir/${sample.screenshotName}")
            rule.onNodeWithText("Back").performClick()
            rule.waitForIdle()
        }
        println("THOR_B1_CONNECTED_SAMPLE_COUNT=${samples.size}")
    }

    private data class Sample(val name: String, val itemId: String, val gameItemId: Int, val screenshotName: String)
}
