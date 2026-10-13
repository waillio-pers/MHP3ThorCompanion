package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkillFilterAutoHideUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun portraitFilterRowHidesAfterDownwardScrollAndReturnsOnUpwardScroll() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertDoesNotExist()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()
    }

    @Test
    fun filteredAlphabetShowsOnlyLiveLettersAndJumpsToTheFirstVisibleSkill() {
        val fixture = androidx.lifecycle.ViewModelProvider(rule.activity)[AppViewModel::class.java].fixture
        val allLetters = skillInitialLetters(fixture.skillTrees)
        val firstLetter = allLetters.first()
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("alphabet-letter-$firstLetter").performClick()
        val firstSkill = fixture.skillTrees.sortedBy { it.displayName.lowercase() }
            .first { it.displayName.trim().startsWith(firstLetter, ignoreCase = true) }
        rule.onNodeWithTag("skill-row-${firstSkill.stableSkillTreeId}").assertIsDisplayed()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").performClick()
        rule.onNodeWithTag("skills-category-checkbox-UTILITY").performClick()
        rule.onNodeWithTag("skills-category-filter-done").performClick()
        val utilityLetters = skillInitialLetters(
            com.waillio.mhp3rdcompanion.data.filterSkillTreesByCategories(fixture.skillTrees, setOf("UTILITY"))
                .sortedBy { it.displayName.lowercase() }
        )
        utilityLetters.forEach { letter -> rule.onNodeWithTag("alphabet-letter-$letter").assertExists() }
        val absentLetter = ('A'..'Z').first { it !in utilityLetters }
        rule.onNodeWithTag("alphabet-letter-$absentLetter").assertDoesNotExist()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlphabetScrubberGestureUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun activeDragContinuesWhenScrubberHeightChanges() {
        val lastJump = AtomicReference<Char?>(null)
        rule.setContent {
            var scrubberHeight by remember { mutableIntStateOf(600) }
            var resized by remember { mutableStateOf(false) }
            Box(Modifier.fillMaxSize()) {
                AlphabetScrubber(
                    letters = ('A'..'Z').toList(),
                    selectedLetter = lastJump.get(),
                    onJump = { letter ->
                        lastJump.set(letter)
                        if (!resized) {
                            resized = true
                            scrubberHeight = 550
                        }
                    },
                    modifier = Modifier.height(scrubberHeight.dp).align(Alignment.CenterEnd)
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("alphabet-scrubber").performTouchInput {
            swipe(
                start = Offset(centerX, height * 0.1f),
                end = Offset(centerX, height * 0.9f),
                durationMillis = 900
            )
        }
        rule.waitForIdle()

        assertTrue("Drag must keep reaching near-bottom letters after the resize", lastJump.get() in 'W'..'Z')
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w827dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkillFilterThorAutoHideUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun thorFilterBarUsesRealScrollableContentInsteadOfPortraitProfile() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertDoesNotExist()

        rule.onNodeWithTag("skills-list").performTouchInput { swipeDown() }
        rule.waitForIdle()
        rule.onNodeWithTag("skills-category-filter-button").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-port-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkillAlphabetTallPhoneUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tallPhoneShowsFilteredAlphabetAndJumpsToLiveSkill() {
        val fixture = androidx.lifecycle.ViewModelProvider(rule.activity)[AppViewModel::class.java].fixture
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithTag("field-filter-skill").performScrollTo().performClick()
        rule.onNodeWithTag("skills-alphabet-scrubber").assertIsDisplayed()
        rule.onNodeWithTag("skills-category-filter-button").performClick()
        rule.onNodeWithTag("skills-category-checkbox-GUNNER").performClick()
        rule.onNodeWithTag("skills-category-filter-done").performClick()

        val gunnerSkills = com.waillio.mhp3rdcompanion.data.filterSkillTreesByCategories(fixture.skillTrees, setOf("GUNNER"))
            .sortedBy { it.displayName.lowercase() }
        val letters = skillInitialLetters(gunnerSkills)
        letters.forEach { rule.onNodeWithTag("alphabet-letter-${it}").assertExists() }
        val targetLetter = letters.last()
        rule.onNodeWithTag("alphabet-letter-$targetLetter").performClick()
        val target = gunnerSkills[firstSkillIndexForLetter(gunnerSkills, targetLetter)]
        rule.onNodeWithTag("skill-row-${target.stableSkillTreeId}").assertIsDisplayed()
    }
}
