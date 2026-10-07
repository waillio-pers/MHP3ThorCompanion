package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
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
class ElementWeaknessLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun allFiveElementValuesFitForEveryProductionMonsterAtTargetProfile() {
        val monsters = CompanionRepository(ApplicationProvider.getApplicationContext()).data.monsters
        val currentValues = mutableStateOf(monsters.first().weaknesses)
        val elements = listOf("Fire", "Water", "Thunder", "Ice", "Dragon")

        assertEquals(40, monsters.size)
        rule.setContent {
            CompanionTheme {
                Box(Modifier.fillMaxWidth()) { CompactWeaknesses(currentValues.value) }
            }
        }

        monsters.forEach { monster ->
            rule.runOnIdle { currentValues.value = monster.weaknesses }
            rule.waitForIdle()
            val container = rule.onNodeWithTag("element-weaknesses").fetchSemanticsNode().boundsInRoot
            val cells = elements.map { element ->
                rule.onNodeWithTag("weakness-cell-$element").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.also { bounds ->
                    rule.onNodeWithTag("weakness-value-$element").assertIsDisplayed()
                    assertTrue("$element cell starts outside ${monster.name}", bounds.left >= container.left)
                    assertTrue("$element cell overflows ${monster.name}", bounds.right <= container.right)
                }
            }
            cells.zipWithNext().forEach { (left, right) ->
                assertTrue("Element cells overlap for ${monster.name}", left.right <= right.left)
            }
        }
    }
}
