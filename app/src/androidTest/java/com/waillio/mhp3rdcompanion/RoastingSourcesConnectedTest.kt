package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One focused Thor smoke flow; it captures no more than the three requested real cases. */
@RunWith(AndroidJUnit4::class)
class RoastingSourcesConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun roastingSourcesShowSingleMultiRouteAndMaximumCasesOnThor() {
        rule.onNodeWithText("Items").performClick()

        // One real route within the maximum-route output Item.
        openItem("Rare Fish", "item_rare_fish")
        rule.onNodeWithTag("material-header-48").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-family").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-input-group-199").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-route-roasting_farm_custom_roaster_199_48_undercooked")
            .assertTextEquals("Farm Custom Roaster · Undercooked · Produces ×1")
        rule.onNodeWithText("Armored Beakfish").assertIsDisplayed()
        listOf(199, 193, 192, 189, 190, 187).forEach { inputId ->
            rule.onNodeWithTag("item-roasting-input-group-$inputId").assertExists()
        }
        listOf(195, 186).forEach { inputId ->
            rule.onNodeWithTag("item-roasting-input-group-$inputId").assertDoesNotExist()
        }
        rule.onNodeWithTag("item-roasting-more-count").assertExists()
        rule.onNodeWithTag("item-roasting-view-all").assertExists()
        saveScreenshot("roasting-v1-single.png")

        // Same Raw Meat -> Rare Steak pair remains two separate factual routes.
        returnToSearch()
        openItem("Rare Steak", "item_rare_steak")
        rule.onNodeWithTag("material-header-43").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-family").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-input-group-39").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-route-roasting_field_bbq_39_43_undercooked")
            .assertTextEquals("Field BBQ · Undercooked · Produces ×1")
        rule.onNodeWithTag("item-roasting-route-roasting_farm_custom_roaster_39_43_undercooked")
            .assertTextEquals("Farm Custom Roaster · Undercooked · Produces ×1")
        rule.onNodeWithText("Raw Meat").assertIsDisplayed()
        saveScreenshot("roasting-v1-multi-route.png")

        // The eighth real route is reachable only through the required first-six preview modal.
        returnToSearch()
        openItem("Rare Fish", "item_rare_fish")
        rule.onNodeWithTag("item-roasting-view-all").performScrollTo().performClick()
        rule.onNodeWithTag("item-roasting-view-all-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-view-all-title").assertTextEquals("Roasting · 8 routes")
        rule.onNodeWithTag("item-roasting-modal-input-group-199").assertIsDisplayed()
        rule.onNodeWithTag("item-roasting-modal-route-roasting_farm_custom_roaster_199_48_undercooked")
            .assertTextEquals("Farm Custom Roaster · Undercooked · Produces ×1")
        listOf(199, 193, 192, 189, 190, 187, 195, 186).forEach { inputId ->
            rule.onNodeWithTag("item-roasting-modal-input-group-$inputId").assertExists()
        }
        saveScreenshot("roasting-v1-maximum.png")
    }

    private fun openItem(query: String, stableItemId: String) {
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput(query)
        rule.onNodeWithTag("result-material-$stableItemId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        closeSoftKeyboard()
        rule.waitForIdle()
    }

    private fun returnToSearch() {
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("global-search").assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        rule.waitForIdle()
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/$name")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        println("ROASTING_SCREENSHOT /sdcard/$name")
    }
}
