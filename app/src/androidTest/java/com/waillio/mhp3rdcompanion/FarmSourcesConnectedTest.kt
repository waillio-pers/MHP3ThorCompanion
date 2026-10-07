package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.view.inputmethod.InputMethodManager

@RunWith(AndroidJUnit4::class)
class FarmSourcesConnectedTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun thorFarmLabelAndMiningCartPassCapturesRequiredFarmStates() {
        val screenshotDir = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("hardening-b")
        ).apply { mkdirs() }

        // Mining Point methods now rely on the facility title; the generic label is absent.
        openItem("Iron Ore", "item_iron_ore", 218)
        rule.onNodeWithTag("farm-section-218").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-facility-title-farm_mining_point_base")
            .assertTextEquals("Mining Point · initial")
        rule.onNodeWithTag("farm-method-trigger-farm_mining_point_base::default").assertDoesNotExist()
        rule.onNodeWithTag("farm-method-outcome-farm_mining_point_base::default-0")
            .assertTextEquals("Yield ×1 · 24%")
        rule.onNodeWithTag("farm-facility-title-farm_mining_point_plus_1")
            .assertTextEquals("Mining Point · +1")
        rule.onNodeWithTag("farm-method-trigger-farm_mining_point_plus_1::default").assertDoesNotExist()
        rule.onNodeWithTag("farm-method-outcome-farm_mining_point_plus_1::default-0")
            .assertTextEquals("Yield ×1 · 10%")
        rule.onNodeWithText("Facility result", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Trigger details unresolved", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("farm-method-outcome-farm_mining_point_base::default-0").performScrollTo()
        hideKeyboard()
        saveScreenshot(screenshotDir, "farm-mining-point-no-generic-label.png")

        // The broad selector mechanism is source-backed, while all six reward profiles stay unnamed.
        openItem("Rainbow Crystal", "item_rainbow_crystal", 234)
        rule.onNodeWithTag("farm-section-234").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("farm-mechanism-note-farm_mining_cart")
            .assertTextEquals("Up to 4 Palicoes go mining; the supplied tool affects failure, normal, or success rates.")
        rule.onNodeWithTag("farm-mechanism-input-84").assertHasClickAction()
        rule.onNodeWithTag("farm-mechanism-input-85").assertHasClickAction()
        rule.onNodeWithTag("farm-mechanism-input-86").assertHasClickAction()
        rule.onNodeWithTag("farm-mechanism-input-170").assertHasClickAction()
        for (profile in listOf(2, 4, 6)) {
            val profileId = "farm_mining_cart_profile_${profile.toString().padStart(2, '0')}"
            val methodTag = "farm_mining_cart::profile:$profileId"
            rule.onNodeWithTag("farm-method-$methodTag").assertExists()
            rule.onNodeWithTag("farm-method-trigger-$methodTag").assertDoesNotExist()
        }
        rule.onNodeWithText("Yield ×2 · 20%").assertExists()
        rule.onNodeWithText("Yield ×1 · 16%").assertExists()
        rule.onNodeWithText("Facility result", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Trigger details unresolved", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("TRIGGER_BLOCKED", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithTag("farm-method-outcome-farm_mining_cart::profile:farm_mining_cart_profile_02-0")
            .performScrollTo()
        hideKeyboard()
        saveScreenshot(screenshotDir, "farm-mining-cart-source-backed-note.png")
        assertTrue(screenshotDir.listFiles()?.count { it.name.startsWith("farm-") } == 2)
    }

    private fun openItem(query: String, stableId: String, gameItemId: Int) {
        val search = rule.onNodeWithTag("global-search")
        search.performTextClearance()
        search.performTextInput(query)
        rule.onNodeWithTag("result-material-$stableId").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithTag("material-header-$gameItemId").assertExists()
    }

    private fun saveScreenshot(directory: File, name: String) {
        rule.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        checkNotNull(bitmap) { "Android UI screenshot capture returned null" }
        FileOutputStream(File(directory, name)).use { stream ->
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
        }
        bitmap.recycle()
    }

    private fun hideKeyboard() {
        rule.runOnUiThread {
            val inputMethodManager = rule.activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(rule.activity.window.decorView.windowToken, 0)
        }
        rule.waitForIdle()
    }
}
