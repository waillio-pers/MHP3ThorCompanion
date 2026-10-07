package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

/** One bounded Thor smoke test; each requested image includes the actual Shop section. */
@RunWith(AndroidJUnit4::class)
class ShopSourcesConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shopSourcesShowInitialUnlockMultiProfileAndDownloadBonusCases() {
        rule.onNodeWithText("Items").performClick()

        openItem("Herb", "item_herb")
        rule.onNodeWithTag("material-header-151").assertIsDisplayed()
        scrollToShop()
        rule.onNodeWithTag("item-shop-family").assertIsDisplayed()
        rule.onNodeWithText("Shop · 4 sources").assertIsDisplayed()
        rule.onNodeWithTag("item-shop-source-item_shop_purchase_001-item_shop_purchase_087")
            .assertIsDisplayed()
        rule.onNodeWithText("General Store / Hunter's Store").assertIsDisplayed()
        rule.onNodeWithText("20z").assertIsDisplayed()
        rule.onNodeWithText("Initial stock").assertIsDisplayed()
        rule.onNodeWithTag("item-shop-profile-item_shop_purchase_173").assertIsDisplayed()
        rule.onNodeWithText("Inventory 1 · 10z").assertIsDisplayed()
        rule.onAllNodesWithText("Half-price inventory").assertCountEquals(2)
        capture("shop-v1-initial-general-hunter.png")

        returnToSearch()
        openItem("Normal S Lv3", "item_normal_s_lv3")
        rule.onNodeWithTag("material-header-96").assertIsDisplayed()
        scrollToShop()
        rule.onNodeWithTag("item-shop-source-item_shop_purchase_022-item_shop_purchase_108")
            .assertIsDisplayed()
        rule.onNodeWithText("5z").assertIsDisplayed()
        rule.onNodeWithText("Either: Village ★4 or Guild ★4 shop expansion").assertIsDisplayed()
        capture("shop-v1-unlock-condition.png")

        returnToSearch()
        openItem("Psychoserum", "item_psychoserum")
        rule.onNodeWithTag("material-header-28").assertIsDisplayed()
        scrollToShop()
        rule.onNodeWithText("Shop · 5 sources").assertIsDisplayed()
        listOf(196, 226, 240, 273, 321).forEach { id ->
            rule.onNodeWithTag("item-shop-profile-item_shop_purchase_${id.toString().padStart(3, '0')}").assertExists()
        }
        rule.onNodeWithText("Inventory 1 · 150z").assertIsDisplayed()
        rule.onNodeWithText("Inventory 2 · 300z").assertIsDisplayed()
        rule.onNodeWithText("Inventory 3 · 150z").assertIsDisplayed()
        rule.onNodeWithText("Inventory 4 · 300z").assertIsDisplayed()
        rule.onNodeWithText("Download bonus inventory · 300z").assertIsDisplayed()
        rule.onAllNodesWithText("Half-price inventory").assertCountEquals(2)
        capture("shop-v1-multi-peddler.png")

        returnToSearch()
        openItem("Raw Meat", "item_raw_meat")
        rule.onNodeWithTag("material-header-39").assertIsDisplayed()
        scrollToShop()
        rule.onNodeWithTag("item-shop-profile-item_shop_purchase_288").assertIsDisplayed()
        rule.onNodeWithText("Download bonus inventory · 50z").assertIsDisplayed()
        rule.onNodeWithText("PEDDLER_PATTERN_5").assertDoesNotExist()
        rule.onNodeWithText("DOWNLOAD_BONUS_SPECIAL_INVENTORY").assertDoesNotExist()
        capture("shop-v1-download-bonus.png")
    }

    private fun openItem(query: String, stableId: String) {
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput(query)
        rule.onNodeWithTag("result-material-$stableId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        closeSoftKeyboard()
        rule.waitForIdle()
    }

    private fun scrollToShop() {
        rule.onNodeWithTag("item-shop-family").performScrollTo().assertIsDisplayed()
        rule.waitForIdle()
    }

    private fun returnToSearch() {
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("global-search").assertIsDisplayed()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /sdcard/$name")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        println("SHOP_SCREENSHOT /sdcard/$name")
    }
}
