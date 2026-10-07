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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Thor regression: remove the redundant Item card without breaking source navigation. */
@RunWith(AndroidJUnit4::class)
class RelatedQuestsRemovalConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun noItemRelatedCardAndQuestSourcesStillResolve() {
        rule.onNodeWithText("Items").performClick()

        openItem("Altaroth Jaw", "item_altaroth_jaw")
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
        val rewardQuestTag = "item-quest-reward-source-quest_village_2_star_09"
        rule.onNodeWithText("Quest Rewards · 1 quest").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(rewardQuestTag).performScrollTo().assertIsDisplayed()
        capture("related-quests-after-typical.png")
        rule.onNodeWithTag(rewardQuestTag).performClick()
        rule.onNodeWithText("An Afternoon in the Forest").assertIsDisplayed()
        returnToSearchFromDetail()

        openItem("Empty Phial", "item_empty_phial")
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
        rule.onNodeWithText("Supply Box · 287 quests").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("item-supply-box-source-quest_village_1_star_02").assertExists()
        rule.onNodeWithText("View all 287").performScrollTo().assertIsDisplayed()
        capture("related-quests-after-max.png")
        rule.onNodeWithText("View all 287").performClick()
        rule.onNodeWithTag("supply-box-modal").assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("supply-box-modal").assertDoesNotExist()
        rule.onNodeWithText("Back").performScrollTo().performClick()
        rule.onNodeWithTag("global-search").assertIsDisplayed()

        openItem("Commendation", "item_commendation")
        rule.onNodeWithText("Related Quests").assertDoesNotExist()
        rule.onNodeWithText("After clearing Rumble in the Great Desert", substring = true)
            .performScrollTo().performClick()
        rule.onNodeWithText("Rumble in the Great Desert").assertIsDisplayed()
    }

    private fun openItem(query: String, stableId: String) {
        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput(query)
        rule.onNodeWithTag("result-material-$stableId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        closeSoftKeyboard()
        rule.waitForIdle()
    }

    private fun returnToSearchFromDetail() {
        rule.onNodeWithText("Back").performScrollTo().performClick()
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
        check(png.size > 64) { "Screenshot was not persisted: $name" }
        val bitmap = checkNotNull(android.graphics.BitmapFactory.decodeByteArray(png, 0, png.size))
        try {
            check(bitmap.width == 1240 && bitmap.height == 1080) {
                "Unexpected screenshot geometry: ${bitmap.width}x${bitmap.height}"
            }
        } finally {
            bitmap.recycle()
        }
        println("RELATED_QUESTS_AFTER_SCREENSHOT /sdcard/$name bytes=${png.size}")
    }
}
