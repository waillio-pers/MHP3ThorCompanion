package com.waillio.mhp3rdcompanion

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Single Thor lower-screen visual regression for the Sources orphan divider. */
@RunWith(AndroidJUnit4::class)
class SourceDividerA1ConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun baggiScaleStartsWithVisibleSmallMonsterFamilyAndNoEmptyCraftingShell() {
        val fixture = ViewModelProvider(rule.activity)[AppViewModel::class.java].fixture
        val baggiScale = fixture.materials.single { it.id == "item_baggi_scale" }
        assertTrue("Fixture should resolve the accepted Baggi Scale item", baggiScale.name == "Baggi Scale")

        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        val search = rule.onNodeWithTag("global-search")
        search.performTextClearance()
        search.performTextInput("Baggi Scale")
        rule.waitForIdle()
        closeSoftKeyboard()
        rule.onNodeWithTag("result-material-item_baggi_scale").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()

        rule.onNodeWithTag("item-sources-section").assertExists()
        rule.onNodeWithText("Sources").assertExists()
        rule.onNodeWithTag("source-family-block-decoration-crafting").assertDoesNotExist()
        val familyTags = listOf(
            "source-family-block-small-monster",
            "source-family-block-monster-reward",
            "source-family-block-palico-expedition"
        )
        familyTags.forEach { rule.onNodeWithTag(it).assertExists() }

        val familyTops = familyTags.map { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue("Small Monster, Monster Rewards and Palico Expedition order changed", familyTops.zipWithNext().all { it.first < it.second })
        val sourcesHeader = rule.onNodeWithText("Sources").fetchSemanticsNode().boundsInRoot
        val firstFamily = rule.onNodeWithTag(familyTags.first()).fetchSemanticsNode().boundsInRoot
        val gapDp = (firstFamily.top - sourcesHeader.bottom) / rule.activity.resources.displayMetrics.density
        assertTrue("First family should start after compact body spacing, got ${gapDp}dp", gapDp in 8f..18f)
        assertTrue("First family boundary must have measurable bounds", firstFamily.width > 0f && firstFamily.height > 0f)

        rule.waitForIdle()
        val captureDir = "/sdcard/Download/thor-hardening-a1-f07-${System.currentTimeMillis()}"
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("mkdir -p $captureDir"))
            .use { it.readBytes() }
        ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("screencap -p $captureDir/hardening-a1-sources-divider-fixed.png")
        ).use { it.readBytes() }
        println("THOR_HARDENING_A1_F07_SCREENSHOT=$captureDir/hardening-a1-sources-divider-fixed.png")
        println("THOR_HARDENING_A1_F07_FIRST_FAMILY_GAP_DP=$gapDp")
        println("THOR_HARDENING_A1_F07_FAMILY_ORDER=${familyTags.joinToString(",")}")
    }
}
