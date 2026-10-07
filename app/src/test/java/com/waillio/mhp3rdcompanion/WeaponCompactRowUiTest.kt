package com.waillio.mhp3rdcompanion

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeaponCompactRowUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun narrowPhoneTreeGivesNamesASeparateStatsLineWithoutDroppingTreeControls() {
        rule.onNodeWithTag("adaptive-profile-compact_portrait").assertExists()
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-weapons").performScrollTo().performClick()
        rule.onNodeWithTag("weapon-type-great-sword").performClick()

        val rootId = "weapon_great_sword_001"
        val rootRow = rule.onNodeWithTag("weapon-row-$rootId").fetchSemanticsNode().boundsInRoot
        val rootName = rule.onNodeWithTag("weapon-name-$rootId", useUnmergedTree = true)
        val rootNameBounds = rootName.fetchSemanticsNode().boundsInRoot
        val rootRarityBounds = rule.onNodeWithTag("weapon-rarity-$rootId", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val compactRowHeight = with(rule.density) { 64.dp.toPx() }

        rootName.assertTextEquals("Old Yukumo Grt Sword")
        rootName.assertIsDisplayed()
        assertTrue("the compact phone row uses its two-line layout", rootRow.height >= compactRowHeight - 1f)
        assertTrue("weapon name has room to wrap instead of collapsing beside fixed stats", rootNameBounds.width >= with(rule.density) { 120.dp.toPx() })
        assertTrue("the name does not collide with rarity", rootNameBounds.right <= rootRarityBounds.left + 1f)
        rule.onNodeWithTag("weapon-expand-$rootId").assertIsDisplayed()
        rule.onNodeWithTag("weapon-sharpness-normal-$rootId", useUnmergedTree = true).assertExists()

        rule.onNodeWithTag("weapon-expand-$rootId").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("weapon-expand-$rootId").assertTextEquals("−")
        rule.onNodeWithTag("weapon-row-weapon_great_sword_002").performScrollTo().assertIsDisplayed()
        saveScreenshot()
        val childId = "weapon_great_sword_002"
        val childBounds = rule.onNodeWithTag("weapon-row-$childId").fetchSemanticsNode().boundsInRoot
        assertTrue("expanded children keep the compact layout", childBounds.height >= compactRowHeight - 1f)
        rule.onNodeWithTag("weapon-name-$childId", useUnmergedTree = true).assertTextEquals("Yukumo Edge")
        rule.onNodeWithTag("weapon-sharpness-normal-$childId", useUnmergedTree = true).assertExists()
    }

    private fun saveScreenshot() {
        val screenshot = File(System.getProperty("user.dir"), "build/reports/mobile-ui-microfix/weapon-tree-360dp.png")
        screenshot.parentFile?.mkdirs()
        val decor = rule.activity.window.decorView
        require(decor.width > 0 && decor.height > 0) { "Window has no laid out pixels for screenshot" }
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { decor.draw(Canvas(bitmap)) }
        FileOutputStream(screenshot).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
