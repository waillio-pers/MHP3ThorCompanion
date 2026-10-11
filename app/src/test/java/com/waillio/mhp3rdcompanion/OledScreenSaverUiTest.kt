package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OledScreenSaverUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun saverCoversDialogAndConsumesWakeTapWithoutFiringUnderlyingControl() {
        val saverActive = mutableStateOf(true)
        val dialogActionCount = mutableIntStateOf(0)
        val underlyingActionCount = mutableIntStateOf(0)
        var dialogOpen = true

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = { underlyingActionCount.intValue++ },
                    modifier = Modifier.fillMaxSize().testTag("underlying-delete")
                ) { Text("Delete") }
                if (dialogOpen) {
                    TrackedDialog(onDismissRequest = { dialogOpen = false }) {
                        Button(
                            onClick = { dialogActionCount.intValue++ },
                            modifier = Modifier.testTag("dialog-action")
                        ) { Text("Dialog action") }
                    }
                }
                if (saverActive.value) {
                    OledScreenSaverDialog(onWake = { saverActive.value = false })
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("oled-screen-saver").assertIsDisplayed().performTouchInput { click(center) }
        rule.waitForIdle()

        rule.onNodeWithTag("oled-screen-saver").assertDoesNotExist()
        rule.onNodeWithTag("dialog-action").assertIsDisplayed()
        assertEquals(0, dialogActionCount.intValue)
        assertEquals(0, underlyingActionCount.intValue)
    }

    @Test
    fun dialogWindowInputIsReportedAsUserActivity() {
        val activityCount = mutableIntStateOf(0)
        rule.setContent {
            CompositionLocalProvider(LocalUserActivityReporter provides { activityCount.intValue++ }) {
                TrackedDialog(onDismissRequest = {}) {
                    Button(onClick = {}, modifier = Modifier.testTag("dialog-touch-target")) { Text("Touch") }
                }
            }
        }
        rule.waitForIdle()
        val before = activityCount.intValue
        rule.onNodeWithTag("dialog-touch-target").performTouchInput { click(center) }
        rule.waitForIdle()
        assertTrue("Dialog window input should report activity", activityCount.intValue > before)
    }
}
