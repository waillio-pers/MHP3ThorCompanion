package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focused Sources shell and accepted long-family regressions on Thor_Lower_Screen. */
@RunWith(AndroidJUnit4::class)
class SourcesOuterShellConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val captures = mutableSetOf<String>()

    @Test
    fun familyBoundariesAndClosedFamiliesRemainIntact() {
        rule.onNodeWithText("Items").performClick()

        // A single, short source family must not acquire nested-card padding.
        openItem("Flash Bomb", "item_flash_bomb")
        assertSourceShell()
        rule.onNodeWithTag("source-family-block-combination").assertIsDisplayed()
        assertTrue(
            "The short family heading is present",
            rule.onAllNodesWithText("Combination", substring = true).fetchSemanticsNodes().isNotEmpty()
        )
        capture("sources-shell-v1-short.png")
        returnToSearch()

        // Compact multi-family case: the accepted family order is Monster, then Quest.
        openItem("Alatreon Tail", "item_alatreon_tail")
        assertSourceShell()
        rule.onNodeWithTag("source-family-block-monster-reward").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("source-family-block-quest-reward").assertExists()
        rule.onNodeWithText("Monster Rewards", substring = true).assertExists()
        rule.onNodeWithText("Quest Rewards", substring = true).assertExists()
        capture("sources-shell-v1-multi-family.png")
        returnToSearch()

        // Empty Phial: the long Supply Box family remains preview-capped at six with View All 287.
        openItem("Empty Phial", "item_empty_phial")
        assertSourceShell()
        rule.onNodeWithTag("item-sources-section").performScrollTo().assertIsDisplayed()
        capture("sources-shell-v1-empty-phial.png")
        rule.onNodeWithTag("source-family-block-supply-box").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("supply-box-more-count").assertTextEquals("+281 more")
        rule.onNodeWithText("View all 287", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("supply-box-view-all").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("supply-box-view-all").performClick()
        rule.onNodeWithTag("supply-box-modal").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("supply-box-modal").assertDoesNotExist()
        returnToSearch()

        // Farm remains 19 accepted methods and still opens its own View All.
        openItem("Armored Beakfish", "item_armored_beakfish")
        assertSourceShell()
        rule.onNodeWithTag("source-family-block-farm").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-method-more-count").assertTextEquals("+13 more")
        rule.onNodeWithTag("farm-method-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("farm-method-modal").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("farm-method-modal").assertDoesNotExist()
        returnToSearch()

        // Trading remains 15 routes with its existing modal behavior.
        openItem("Unique Mushroom", "item_unique_mushroom")
        assertSourceShell()
        rule.onNodeWithTag("source-family-block-trading").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-trading-more-routes").assertTextEquals("+9 more routes")
        rule.onNodeWithTag("item-trading-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-trading-view-all-modal").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("item-trading-view-all-modal").assertDoesNotExist()
        returnToSearch()

        // Scrap Conversion keeps its accepted maximum and its family-owned View All.
        openItem("Agnaktor Scraps+", "item_agnaktor_scraps_plus")
        assertSourceShell()
        rule.onNodeWithTag("source-family-block-scrap-conversion").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-more-count").assertTextEquals("+6 more materials")
        rule.onNodeWithTag("item-scrap-conversion-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all-list").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertDoesNotExist()
        returnToSearch()

        // Sunspire Jewel exercises every applicable family before the distinct
        // Usage section. It is not a decoration-crafting output.
        openItem("Sunspire Jewel", "item_sunspire_jewel")
        assertSourceShell()
        listOf(
            "field", "palico-expedition", "quest-reward",
            "special", "training-reward", "farm", "trading"
        ).forEach { family -> rule.onNodeWithTag("source-family-block-$family").assertExists() }
        rule.onNodeWithTag("source-family-block-decoration-crafting").assertDoesNotExist()
        rule.onNodeWithTag("item-usage-section-812").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Usage").assertIsDisplayed()
        capture("sources-shell-v1-sources-to-usage.png")
        returnToSearch()

        // Zero-source Items keep the safe empty behavior: no empty Sources parent.
        openItem("No Coating", "item_no_coating")
        rule.onNodeWithTag("item-sources-section").assertDoesNotExist()
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
        returnToSearch()

        assertEquals("Exactly four requested shell screenshots were captured", 4, captures.size)
    }

    private fun assertSourceShell() {
        rule.onNodeWithTag("item-sources-section").assertExists()
        rule.onNodeWithText("Sources").assertExists()
        rule.onNodeWithText("Raw fixture values are shown without aggregation.").assertDoesNotExist()
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
    }

    private fun openItem(query: String, stableId: String) {
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput(query)
        rule.onNodeWithTag("result-material-$stableId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        closeSoftKeyboard()
        rule.waitForIdle()
    }

    private fun returnToSearch() {
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("global-search").assertExists()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pfd = automation.executeShellCommand("screencap -p /sdcard/$name")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        val readPfd = automation.executeShellCommand("cat /sdcard/$name")
        val png = android.os.ParcelFileDescriptor.AutoCloseInputStream(readPfd).use { it.readBytes() }
        check(png.size > 64) { "Screenshot was not persisted: $name (${png.size} bytes)" }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size)
        checkNotNull(bitmap) { "Screenshot is not a decodable PNG: $name" }
        try {
            assertEquals("Thor screenshot width", 1240, bitmap.width)
            assertEquals("Thor screenshot height", 1080, bitmap.height)
        } finally {
            bitmap.recycle()
        }
        val statPfd = automation.executeShellCommand("stat -c %s /sdcard/$name")
        val byteCount = android.os.ParcelFileDescriptor.AutoCloseInputStream(statPfd).use {
            it.readBytes().decodeToString().trim().toLong()
        }
        assertTrue("Screenshot file is empty", byteCount > 64)
        captures += name
        println("SOURCE_OUTER_SHELL_SCREENSHOT /sdcard/$name $byteCount bytes")
    }
}
