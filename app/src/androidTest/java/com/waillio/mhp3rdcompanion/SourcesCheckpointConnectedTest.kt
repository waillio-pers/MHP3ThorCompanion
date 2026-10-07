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

/** One bounded cross-family smoke scenario for the Thor_Lower_Screen AVD. */
@RunWith(AndroidJUnit4::class)
class SourcesCheckpointConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val capturedNames = mutableSetOf<String>()

    @Test
    fun recentSourceFamiliesAndSharedViewAllRemainUsableOnThor() {
        rule.onNodeWithText("Items").performClick()

        // Item-detail overview and Trading source family.
        openItem("Unique Mushroom", "item_unique_mushroom")
        rule.onNodeWithTag("material-header-687").assertIsDisplayed()
        rule.onNodeWithText("Unique Mushroom").assertIsDisplayed()
        capture("checkpoint-item-detail-overview.png")
        rule.onNodeWithTag("item-trading-sources").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Trading · 15 routes").assertIsDisplayed()
        rule.onNodeWithText("Veggie Elder").assertIsDisplayed()
        capture("checkpoint-trading.png")

        // View All preserves the selected Item Detail; Back dismisses only the modal.
        rule.onNodeWithTag("item-trading-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-trading-view-all-modal").assertIsDisplayed()
        rule.onNodeWithText("Veggie Elder · 15 routes").assertIsDisplayed()
        rule.onNodeWithTag("item-trading-view-all-list").assertIsDisplayed()
        capture("checkpoint-view-all.png")
        pressBack()
        rule.onNodeWithTag("item-trading-view-all-modal").assertDoesNotExist()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithTag("item-trading-sources").assertExists()
        returnToSearch()

        // Farm keeps all four published yield/probability pairs distinct.
        openItem("Huskberry", "item_huskberry")
        rule.onNodeWithTag("farm-section-92").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-method-outcome-farm_field_planted_item::inputGameItemId:92-0")
            .assertTextEquals("Yield ×1 · 10%")
        rule.onNodeWithTag("farm-method-outcome-farm_field_planted_item::inputGameItemId:92-1")
            .assertTextEquals("Yield ×2 · 45%")
        rule.onNodeWithTag("farm-method-outcome-farm_field_planted_item::inputGameItemId:92-2")
            .assertTextEquals("Yield ×3 · 40%")
        rule.onNodeWithTag("farm-method-outcome-farm_field_planted_item::inputGameItemId:92-3")
            .assertTextEquals("Yield ×4 · 5%")
        capture("checkpoint-farm.png")
        returnToSearch()

        // Roasting retains both contexts and the result-state wording.
        openItem("Rare Steak", "item_rare_steak")
        rule.onNodeWithTag("item-roasting-family").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-route-roasting_field_bbq_39_43_undercooked")
            .assertTextEquals("Field BBQ · Undercooked · Produces ×1")
        rule.onNodeWithTag("item-roasting-route-roasting_farm_custom_roaster_39_43_undercooked")
            .assertTextEquals("Farm Custom Roaster · Undercooked · Produces ×1")
        capture("checkpoint-roasting.png")
        returnToSearch()

        // Shop aggregation and Peddler profile remain jointly visible.
        openItem("Herb", "item_herb")
        rule.onNodeWithTag("item-shop-family").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Shop · 4 sources").assertIsDisplayed()
        rule.onNodeWithText("General Store / Hunter's Store").assertIsDisplayed()
        rule.onNodeWithText("Peddler").assertIsDisplayed()
        rule.onNodeWithText("Inventory 1 · 10z").assertIsDisplayed()
        capture("checkpoint-shop.png")

        assertEquals("Exactly six checkpoint screenshots are captured", 6, capturedNames.size)
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
        rule.onNodeWithTag("global-search").assertIsDisplayed()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val createPfd = automation.executeShellCommand("screencap -p /sdcard/$name")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(createPfd).use { it.readBytes() }
        val readPfd = automation.executeShellCommand("cat /sdcard/$name")
        val png = android.os.ParcelFileDescriptor.AutoCloseInputStream(readPfd).use { it.readBytes() }
        check(png.size > 64) { "Screenshot was not persisted: $name (${png.size} bytes)" }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size)
        checkNotNull(bitmap) { "Screenshot is not a decodable PNG: $name" }
        try {
            assertEquals("Unexpected Thor AVD screenshot width", 1240, bitmap.width)
            assertEquals("Unexpected Thor AVD screenshot height", 1080, bitmap.height)
        } finally {
            bitmap.recycle()
        }
        val statPfd = automation.executeShellCommand("stat -c %s /sdcard/$name")
        val bytesOnDisk = android.os.ParcelFileDescriptor.AutoCloseInputStream(statPfd).use { it.readBytes().decodeToString().trim().toLong() }
        assertTrue("Persisted checkpoint screenshot is empty: $name", bytesOnDisk > 64L)
        capturedNames += name
        println("SOURCE_CHECKPOINT_SCREENSHOT /sdcard/$name $bytesOnDisk bytes")
    }
}
