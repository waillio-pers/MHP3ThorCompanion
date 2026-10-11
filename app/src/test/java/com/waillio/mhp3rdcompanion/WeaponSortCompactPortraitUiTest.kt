package com.waillio.mhp3rdcompanion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
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
class WeaponSortCompactPortraitUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun sortControlsWrapOnNarrowPortraitWithoutClippingTheDirectionLabel() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext<android.content.Context>()).data
        val repository = WeaponRepository.fromProduction(data.weapons, data.materials)
        val screenState = WeaponScreenState().apply { selectedType = "HAMMER" }
        rule.setContent {
            CompositionLocalProvider(LocalAppWindowLayout provides appWindowLayout(360, 800)) {
                CompanionTheme {
                    WeaponsScreen(repository = repository, onItem = {}, screenState = screenState)
                }
            }
        }

        rule.onNodeWithTag("weapon-mode-attack").performClick()
        rule.onNodeWithTag("weapon-sort-direction").assertIsDisplayed()
        rule.onNodeWithTag("weapon-sort-direction").assertContentDescriptionEquals("Attack · High → Low")
        val viewport = rule.onNodeWithTag("weapon-production-hammer").fetchSemanticsNode().boundsInRoot
        val controls = rule.onNodeWithTag("weapon-browse-controls").fetchSemanticsNode().boundsInRoot
        assertTrue("controls should wrap instead of extending beyond the narrow viewport", controls.right <= viewport.right)
        assertTrue("wrapped controls retain at least the existing compact row height", controls.height >= 43f)
    }
}
