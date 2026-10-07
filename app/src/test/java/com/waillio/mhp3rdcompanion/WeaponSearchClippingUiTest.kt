package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w620dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WeaponSearchClippingUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun fieldFitsToolbarAndTypedTextAtMinimumDefaultAndMaximumUiScale() {
        val scale = mutableFloatStateOf(0.80f)
        val query = mutableStateOf("")
        rule.setContent {
            val platformDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = platformDensity.density * scale.floatValue,
                    fontScale = platformDensity.fontScale
                )
            ) {
                CompanionTheme {
                    Row(
                        Modifier.fillMaxWidth().height(56.dp).testTag("weapon-search-toolbar"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeaponSearchField(
                            weaponType = "HAMMER",
                            query = query.value,
                            onQueryChange = { query.value = it },
                            modifier = Modifier.width(190.dp)
                        )
                    }
                }
            }
        }

        listOf(0.80f, 1.00f, 1.10f).forEach { value ->
            rule.runOnIdle { scale.floatValue = value }
            rule.waitForIdle()
            val field = rule.onNodeWithTag("weapon-search")
            field.assertIsDisplayed()
            val toolbarHeight = rule.onNodeWithTag("weapon-search-toolbar").fetchSemanticsNode().boundsInRoot.height
            val fieldHeight = field.fetchSemanticsNode().boundsInRoot.height
            assertTrue("Search field is vertically constrained at UI scale $value", fieldHeight >= toolbarHeight * .95f)
            rule.onNodeWithText("Search Hammer", useUnmergedTree = true, substring = true).assertIsDisplayed()

            field.performClick()
            field.assertIsFocused()
            field.performTextInput("Hammer")
            field.assertTextContains("Hammer")
            rule.onNodeWithTag("weapon-search-clear").performClick()
            rule.runOnIdle { assertEquals("query cleared at UI scale $value", "", query.value) }
            rule.onNodeWithText("Search Hammer", useUnmergedTree = true, substring = true).assertIsDisplayed()
        }
    }
}
