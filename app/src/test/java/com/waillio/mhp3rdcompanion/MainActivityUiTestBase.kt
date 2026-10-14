package com.waillio.mhp3rdcompanion

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Before
import org.junit.Rule

/** Wait for deliberately off-main production-data preparation before legacy UI assertions run. */
abstract class MainActivityUiTestBase {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun awaitMainActivityReferenceData() {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithTag("nav-home").fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun awaitTag(tag: String, useUnmergedTree: Boolean = false) {
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
