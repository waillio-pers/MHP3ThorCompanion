package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-mdpi")
class ThorHeaderUiTest : MainActivityUiTestBase() {
    @Test fun settingsDiameterMatchesSearchHeight() {
        val settings = rule.onNodeWithTag("settings-button").fetchSemanticsNode().boundsInRoot
        val search = rule.onNodeWithTag("global-search").fetchSemanticsNode().boundsInRoot
        assertEquals(search.height, settings.height, 1f)
        assertEquals(settings.width, settings.height, 1f)
    }
}
