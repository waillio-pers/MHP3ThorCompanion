package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One bounded Thor smoke test for Scrap acquisition direction and disclosure. */
@RunWith(AndroidJUnit4::class)
class ScrapConversionSourcesConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun sourceRowsQuantitiesUnpublishedCourageAndViewAllWorkOnThor() {
        rule.onNodeWithText("Items").performClick()

        openItem("Wood Scraps", "item_wood_scraps")
        scrollToScrap()
        rule.onNodeWithText("Scrap Conversion · 1 materials").assertIsDisplayed()
        rule.onNodeWithText("Yukumo Wood").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×1").assertIsDisplayed()
        capture("scrap-v1-simple-wood.png")
        returnToSearch()

        openItem("Pumpkin Scraps", "item_pumpkin_scraps")
        scrollToScrap()
        rule.onNodeWithText("Bumblepumpkin").assertIsDisplayed()
        rule.onNodeWithText("Use ×1 · Produces ×3").assertIsDisplayed()
        capture("scrap-v1-high-yield.png")
        returnToSearch()

        openItem("Agnaktor Scraps+", "item_agnaktor_scraps_plus")
        scrollToScrap()
        rule.onNodeWithText("Scrap Conversion · 12 materials").assertIsDisplayed()
        rule.onNodeWithText("+6 more materials").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all").performClick()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-conversion-view-all-title").assertIsDisplayed()
        rule.onNodeWithTag("item-scrap-modal-material-545").assertIsDisplayed()
        capture("scrap-v1-max-view-all.png")
        rule.onNodeWithTag("item-scrap-conversion-view-all-list").performScrollToIndex(11)
        rule.onNodeWithTag("item-scrap-modal-material-541").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("item-scrap-conversion-view-all-modal").assertDoesNotExist()
        rule.onNodeWithTag("item-scrap-conversion-family").assertExists()
        returnToSearch()

        openItem("Courage Scraps", "item_courage_scraps")
        scrollToScrap()
        rule.onNodeWithText("Commendation").assertIsDisplayed()
        rule.onNodeWithText("Use ×1").assertIsDisplayed()
        rule.onNodeWithText("Produces ×1").assertDoesNotExist()
        rule.onNodeWithText("NOT_PUBLISHED").assertDoesNotExist()
        capture("scrap-v1-courage-unpublished.png")
    }

    private fun openItem(query: String, stableId: String) {
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput(query)
        rule.onNodeWithTag("result-material-$stableId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        closeSoftKeyboard()
        rule.waitForIdle()
    }

    private fun scrollToScrap() {
        rule.onNodeWithTag("item-scrap-conversion-family").performScrollTo().assertIsDisplayed()
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
        val readDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cat /sdcard/$name")
        val png = android.os.ParcelFileDescriptor.AutoCloseInputStream(readDescriptor).use { it.readBytes() }
        check(png.size > 64) { "Screenshot was not persisted: $name (${png.size} bytes)" }
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size)
        checkNotNull(bitmap) { "Screenshot is not a decodable PNG: $name" }
        try {
            check(bitmap.width == 1240 && bitmap.height == 1080) {
                "Unexpected screenshot geometry for $name: ${bitmap.width}x${bitmap.height}"
            }
        } finally {
            bitmap.recycle()
        }
        val statDescriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("stat -c %s /sdcard/$name")
        val bytes = android.os.ParcelFileDescriptor.AutoCloseInputStream(statDescriptor)
            .use { it.readBytes().decodeToString().trim().toLong() }
        check(bytes > 64L) { "Screenshot was not persisted: $name ($bytes bytes)" }
        println("SCRAP_SOURCE_SCREENSHOT /sdcard/$name $bytes bytes")
    }
}
