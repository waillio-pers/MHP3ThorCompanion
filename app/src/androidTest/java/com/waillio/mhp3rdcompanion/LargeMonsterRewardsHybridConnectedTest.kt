package com.waillio.mhp3rdcompanion

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.MonsterRewardFilter
import com.waillio.mhp3rdcompanion.data.RewardContext
import com.waillio.mhp3rdcompanion.data.itemFirstMonsterRewards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Connected rewards smoke/regression on the local Thor_Lower_Screen AVD. */
@RunWith(AndroidJUnit4::class)
class LargeMonsterRewardsHybridConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun compactRewardsFlowsRunOnLargeScreenAvd() {
        val settings = rule.activity.getSharedPreferences(UI_SETTINGS_PREFS, Context.MODE_PRIVATE)
        settings.edit().putInt(UI_SCALE_STEP_KEY, UI_SCALE_DEFAULT_STEP).commit()
        rule.runOnUiThread { rule.activity.recreate() }
        rule.waitForIdle()

        val data = CompanionRepository(rule.activity).data
        val agnaktor = data.monsters.single { it.id == "monster_agnaktor" }
        val rathian = data.monsters.single { it.name == "Rathian" }
        val rathianCarapace = data.materials.single { it.name == "Rathian Carapace" }
        assertEquals(1535, data.monsters.sumOf { it.rewards.size })
        assertEquals(40, data.monsters.count { it.rewards.isNotEmpty() })
        val agnaktorScaleLabels = agnaktor.rewards.itemFirstMonsterRewards(RewardContext.LOW)
            .single { it.itemName == "Agnaktor Scale" }.routes.map { it.label }
        assertEquals(
            "production keeps every accepted Agnaktor Scale method distinct",
            setOf("Body Carve", "Chest Break", "Dorsal Fin Break", "Front Legs/Hind Legs Break", "Capture", "Shiny Drop"),
            agnaktorScaleLabels.toSet()
        )
        val rathianCapture = rathian.rewards.itemFirstMonsterRewards(RewardContext.HIGH, MonsterRewardFilter.CAPTURE)
        assertTrue("Rathian HR Capture has enough Items for two-up rows", rathianCapture.size >= 4)

        val captureDir = "/sdcard/Download/mhp3rd-large-monster-rewards-rc1-${System.currentTimeMillis()}"
        shell("mkdir -p $captureDir")

        openMonster(agnaktor.id, agnaktor.name)
        openRewardsTab()
        selectRank(agnaktor.id, high = false)
        selectFilter(agnaktor.id, MonsterRewardFilter.ALL)
        capture(captureDir, "01-agnaktor-lr-all-grid.png")

        selectRank(agnaktor.id, high = true)
        selectFilter(agnaktor.id, MonsterRewardFilter.ALL)
        capture(captureDir, "02-agnaktor-hr-all-grid.png")

        selectFilter(agnaktor.id, MonsterRewardFilter.CARVE)
        capture(captureDir, "07-filter-carve-adaptive.png")

        selectFilter(agnaktor.id, MonsterRewardFilter.SHINY)
        capture(captureDir, "05-filter-shiny-two-up.png")

        selectFilter(agnaktor.id, MonsterRewardFilter.BREAK)
        capture(captureDir, "06-filter-break-adaptive.png")

        openMonster(rathian.id, rathian.name)
        openRewardsTab()
        selectRank(rathian.id, high = true)
        selectFilter(rathian.id, MonsterRewardFilter.ALL)
        capture(captureDir, "03-rathian-hr-all-alignment.png")

        selectFilter(rathian.id, MonsterRewardFilter.CAPTURE)
        capture(captureDir, "04-rathian-hr-capture-two-up.png")

        // A half-width Item remains navigable, and Back restores the active
        // context and filter without relying on a device-specific tap location.
        rule.onNodeWithTag("monster-reward-item-${rathian.id}-${rathianCarapace.gameItemId}")
            .performScrollTo().performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithTag("screen-monster").assertIsDisplayed()
        rule.onNodeWithTag("reward-context-${rathian.id}-HIGH").assertIsSelected()
        rule.onNodeWithTag("reward-filter-${rathian.id}-capture").assertIsSelected()

        val screenshots = shell("ls -1 $captureDir").lines().filter { it.endsWith(".png") }
        assertEquals("exactly seven requested screenshots were captured", 7, screenshots.size)
        println("LARGE_MONSTER_REWARDS_RC1_SCREENSHOTS=$captureDir")
        println("LARGE_MONSTER_REWARDS_RC1_CONNECTED=PASS")
        println("LARGE_MONSTER_REWARDS_RC1_AVD=Thor_Lower_Screen; UI scale=1.00x")
        println("LARGE_MONSTER_REWARDS_RC1_NAVIGATION_RESTORATION=PASS")
    }

    private fun openMonster(monsterId: String, name: String) {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-monster").performClick()
        val search = rule.onNodeWithTag("global-search")
        search.performTextClearance()
        search.performTextInput(name)
        rule.onNodeWithTag("monster-card-$monsterId").performScrollTo().performClick()
        rule.onNodeWithTag("screen-monster").assertIsDisplayed()
        rule.onNodeWithText("Overview").assertIsDisplayed()
        rule.onNodeWithText(name).assertIsDisplayed()
    }

    private fun openRewardsTab() {
        rule.onNodeWithTag("monster-tab-1").performClick()
        rule.waitForIdle()
    }

    private fun selectRank(monsterId: String, high: Boolean) {
        val context = if (high) RewardContext.HIGH else RewardContext.LOW
        rule.onNodeWithTag("reward-context-$monsterId-${context.name}").performClick()
        rule.onNodeWithTag("reward-context-$monsterId-${context.name}").assertIsSelected()
    }

    private fun selectFilter(monsterId: String, filter: MonsterRewardFilter) {
        rule.onNodeWithTag("reward-filter-$monsterId-${filter.name.lowercase()}").performClick()
        rule.onNodeWithTag("reward-filter-$monsterId-${filter.name.lowercase()}").assertIsSelected()
        rule.waitForIdle()
    }

    private fun capture(directory: String, filename: String) {
        rule.waitForIdle()
        val path = "$directory/$filename"
        shell("screencap -p $path")
        val listing = shell("ls -l $path")
        assertTrue("lower-screen screenshot exists: $filename ($listing)", listing.contains(filename))
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).use { it.bufferedReader().readText() }
}
