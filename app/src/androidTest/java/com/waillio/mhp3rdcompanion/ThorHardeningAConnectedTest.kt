package com.waillio.mhp3rdcompanion

import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.waillio.mhp3rdcompanion.data.MaterialSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One end-to-end pass for the accepted Thor Lower Screen AVD geometry. */
@RunWith(AndroidJUnit4::class)
class ThorHardeningAConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun thorLowerScreenHardeningScenariosAndEvidence() {
        val fixture = ViewModelProvider(rule.activity)[AppViewModel::class.java].fixture
        val captureDir = "/sdcard/Download/thor-hardening-a-${System.currentTimeMillis()}"
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("mkdir -p $captureDir")
        ).use { it.readBytes() }

        // A. Field Guide categories are in the requested presentation order.
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-all").performClick()
        val categoryTags = listOf(
            "field-filter-all", "field-filter-monster", "field-filter-small_monster",
            "field-filter-material", "field-filter-map", "field-filter-weapons",
            "field-filter-skill", "field-filter-quest", "field-filter-training"
        )
        val categoryLefts = categoryTags.map { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.left }
        assertTrue("Field Guide order is not left-to-right as accepted", categoryLefts.zipWithNext().all { it.first < it.second })
        val stripBounds = rule.onNodeWithTag("field-guide-category-strip").fetchSemanticsNode().boundsInRoot
        val trainingBounds = rule.onNodeWithTag("field-filter-training").fetchSemanticsNode().boundsInRoot
        assertTrue("Training category must remain fully visible at Thor width", trainingBounds.right <= stripBounds.right)
        capture(captureDir, "hardening-a-field-guide.png")

        // B. Real Zinogre content: keep Overview factual values, omit its redundant caption,
        // and verify the instrumented table/list while capturing the visible separators.
        rule.onNodeWithTag("field-filter-monster").performClick()
        val zinogre = fixture.monsters.first { it.name.equals("Zinogre", ignoreCase = true) }
        enterGlobalQuery("Zinogre")
        rule.onNodeWithTag("monster-card-${zinogre.id}").performClick()
        rule.onAllNodesWithText("Normal-state values").assertCountEquals(0)
        rule.onNodeWithTag("monster-tab-2").performClick()
        rule.onNodeWithTag("hitzones-table-header").assertIsDisplayed()
        rule.onNodeWithTag("hitzone-row-0").assertIsDisplayed()
        capture(captureDir, "hardening-a-monster-overview-hitzones.png")

        // Rank badges in the shared Related Quests list remain within their corrected footprint.
        rule.onNodeWithTag("monster-tab-3").performClick()
        val firstRelatedQuest = fixture.quests.firstOrNull { it.id in zinogre.questIds }
            ?: error("Zinogre has no related quest to verify its shared rank badge")
        rule.onNodeWithTag("quest-badge-${firstRelatedQuest.id}")
            .assertIsDisplayed().assertHeightIsAtLeast(46.dp)
        capture(captureDir, "hardening-a-quest-badge.png")
        rule.onNodeWithTag("detail-back").performClick()

        // C. Find a real Item carrying both Small Monster and Palico Expedition sources.
        val sourceCandidate = fixture.materials.firstOrNull { material ->
            val types = material.sources.map { it.type }.toSet()
            MaterialSourceType.SMALL_MONSTER in types && MaterialSourceType.PALICO_EXPEDITION in types
        } ?: error("No production Item contains both Small Monster and Palico Expedition sources")
        rule.onNodeWithTag("field-filter-all").performClick()
        enterGlobalQuery(sourceCandidate.name)
        rule.onNodeWithTag("result-material-${sourceCandidate.id}").performClick()
        rule.onNodeWithTag("item-sources-section").assertExists()
        val smallMonsterTop = rule.onNodeWithTag("source-family-block-small-monster").fetchSemanticsNode().boundsInRoot.top
        val palicoTop = rule.onNodeWithTag("source-family-block-palico-expedition").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Small Monster source must precede Palico Expedition", smallMonsterTop < palicoTop)
        rule.onNodeWithText("Sources").assertExists()
        capture(captureDir, "hardening-a-sources-footer-order.png")
        rule.onNodeWithText("Back").performClick()

        // Items search uses the same shared input state; inject an actual Android delete key.
        rule.onNodeWithTag("field-filter-material").performClick()
        val global = rule.onNodeWithTag("global-search")
        global.performTextClearance()
        global.performTextInput("Iron Ore")
        global.performTextInputSelection(TextRange(3))
        rule.waitForIdle()
        assertEquals(TextRange(3), global.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        closeSoftKeyboard()
        injectDeleteKey()
        global.assertTextEquals("Irn Ore")
        global.performTextClearance()
        closeSoftKeyboard()

        // D. Exercise first/middle/last alphabet letters and retain their full hit targets.
        rule.onNodeWithTag("alphabet-scrubber").assertExists()
        val scrubberBounds = rule.onNodeWithTag("alphabet-scrubber").fetchSemanticsNode().boundsInRoot
        listOf('A', 'M', 'Z').forEach { letter ->
            rule.onNodeWithTag("alphabet-letter-$letter").performClick()
            val labelBounds = rule.onNodeWithTag("alphabet-letter-label-$letter", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            assertTrue("$letter clipped above the Items index", labelBounds.top >= scrubberBounds.top)
            assertTrue("$letter clipped below the Items index", labelBounds.bottom <= scrubberBounds.bottom)
        }
        capture(captureDir, "hardening-a-items-index.png")

        // E. Full category and star labels must be present in the live filter dialog.
        rule.onNodeWithTag("field-filter-quest").performClick()
        rule.onNodeWithTag("quest-filters-button").performClick()
        rule.onNodeWithText("Hot Spring").assertIsDisplayed()
        rule.onNodeWithTag("quest-filter-star-8").assertIsDisplayed()
        rule.onNodeWithText("High Rank").assertIsDisplayed()
        capture(captureDir, "hardening-a-quest-filters.png")
        rule.onNodeWithTag("quest-filter-close").performClick()

        // F. A real hardware KEYCODE_DEL is delivered through the focused Great Sword field.
        rule.onNodeWithTag("field-filter-weapons").performClick()
        rule.onNodeWithTag("weapon-type-great-sword").performClick()
        val weaponSearch = rule.onNodeWithTag("weapon-search")
        weaponSearch.performClick().performTextInput("Vulcan")
        weaponSearch.performTextInputSelection(TextRange(3))
        rule.waitForIdle()
        assertEquals(TextRange(3), weaponSearch.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        assertTrue("Weapon query text should be available and visible", weaponSearch.fetchSemanticsNode().boundsInRoot.height > 0f)
        closeSoftKeyboard()
        injectDeleteKey()
        weaponSearch.assertTextEquals("Vucan")
        closeSoftKeyboard()
        capture(captureDir, "hardening-a-weapon-search.png")

        // G. Skills retains its explanation and category while the duplicate page heading is gone.
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performClick()
        rule.onNodeWithText("How skills work").assertIsDisplayed()
        rule.onAllNodesWithText("SKILLS").assertCountEquals(0)
        capture(captureDir, "hardening-a-skills.png")

        // Third independent shared search surface: Supply Box View All modal.
        rule.onNodeWithTag("field-filter-all").performClick()
        enterGlobalQuery("Empty Phial")
        rule.onNodeWithTag("result-material-item_empty_phial").performClick()
        rule.onNodeWithTag("supply-box-more-count").assertTextEquals("+281 more")
        rule.onNodeWithTag("supply-box-view-all").performScrollTo().performClick()
        val modalSearch = rule.onNodeWithTag("supply-box-modal-search")
        modalSearch.performTextInput("Forest Murmur")
        modalSearch.performTextInputSelection(TextRange(3))
        modalSearch.performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Backspace) }
        assertEquals(
            "Foest Murmur",
            modalSearch.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        )

        val files = ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("ls -1 $captureDir")
        ).use { it.bufferedReader().readLines() }
        assertEquals("Exactly eight requested screenshots must be captured", 8, files.size)
        println("THOR_HARDENING_A_SCREENSHOTS=$captureDir")
        println("THOR_HARDENING_A_HARDWARE_KEYCODE_DEL=PASS")
        println("THOR_HARDENING_A_SEARCH_SURFACES=Items/global, Great Sword, Supply Box modal")
    }

    private fun enterGlobalQuery(query: String) {
        val field = rule.onNodeWithTag("global-search")
        field.performTextClearance()
        field.performTextInput(query)
        rule.waitForIdle()
        closeSoftKeyboard()
    }

    private fun injectDeleteKey() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DEL)
        rule.waitForIdle()
    }

    private fun capture(directory: String, name: String) {
        rule.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand("screencap -p $directory/$name")
        ).use { it.readBytes() }
    }
}
