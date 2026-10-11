package com.waillio.mhp3rdcompanion

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.CompanionRepository
import com.waillio.mhp3rdcompanion.data.RecentEntry
import com.waillio.mhp3rdcompanion.data.ReferenceEntityType
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
class HomeRecentUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun sixRecentItemsRenderWithinThePhoneHomeViewport() {
        val data = CompanionRepository(ApplicationProvider.getApplicationContext<android.content.Context>()).data
        val entries = data.monsters.take(6).mapIndexed { index, monster ->
            RecentEntry(ReferenceEntityType.MONSTER, monster.id, index.toLong())
        }
        rule.setContent {
            CompositionLocalProvider(LocalAppWindowLayout provides appWindowLayout(360, 800)) {
                CompanionTheme {
                    HomeScreen(
                        data = data,
                        recent = entries,
                        favorites = emptyList(),
                        onMaterials = {},
                        onMonsters = {},
                        onMaps = {},
                        onCharmTables = {},
                        onFavorites = {},
                        onEntity = { _, _ -> }
                    )
                }
            }
        }

        val viewport = rule.onNodeWithTag("screen-home").fetchSemanticsNode().boundsInRoot
        entries.forEach { entry ->
            val item = rule.onNodeWithTag("reference-monster-${entry.entityId}")
            item.assertIsDisplayed()
            val bounds = item.fetchSemanticsNode().boundsInRoot
            assertTrue("recent card must stay inside the phone width", bounds.left >= viewport.left && bounds.right <= viewport.right)
            assertTrue("recent card must keep its compact touch height", bounds.height > 40f)
        }
    }
}
