package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AutoFitTextTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun compactNamesRemainCompleteWithoutEllipsis() {
        val names = listOf("Nargacuga Marrow", "Thunderbug Jewel", "Zinogre Claw+", "Monster Keenbone")
        rule.setContent {
            CompanionTheme {
                Column {
                    names.forEachIndexed { index, name ->
                        AutoFitCompactText(name, Modifier.width(130.dp).testTag("name-$index"))
                    }
                }
            }
        }
        names.forEachIndexed { index, name ->
            rule.onNodeWithTag("name-$index").assertIsDisplayed().assertTextEquals(name)
        }
    }
}
