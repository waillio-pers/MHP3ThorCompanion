package com.waillio.mhp3rdcompanion

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.waillio.mhp3rdcompanion.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class SmallMonsterIntegrationClosureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val data = CompanionRepository(ApplicationProvider.getApplicationContext()).data

    @Test fun allProductionSmallMonstersAreGloballySearchableWithCanonicalNames() {
        data.smallMonsters.forEach { monster ->
            val matches = CompanionLogic.search(data, monster.name)
            assertTrue(matches.any { it.type == EntityType.SMALL_MONSTER && it.id == monster.id })
        }
        val jaggi = CompanionLogic.search(data, "Jaggi")
        assertTrue(jaggi.any { it.id == "small_monster_jaggi" && it.type == EntityType.SMALL_MONSTER })
        assertTrue(jaggi.any { it.id == "monster_great_jaggi" && it.type == EntityType.MONSTER })
        assertFalse(jaggi.any { it.id == "small_monster_jaggi" && it.type == EntityType.MONSTER })
        assertEquals("Gargwa", CompanionLogic.search(data, "Gargwa").single { it.type == EntityType.SMALL_MONSTER }.name)
        assertEquals("Wroggi", CompanionLogic.search(data, "Wroggi").single { it.type == EntityType.SMALL_MONSTER }.name)
        assertEquals("Slagtoth", CompanionLogic.search(data, "Slagtoth").single { it.type == EntityType.SMALL_MONSTER }.name)
        assertTrue(CompanionLogic.search(data, "Gagua").none { it.type == EntityType.SMALL_MONSTER })
        assertTrue(CompanionLogic.search(data, "Froggi").none { it.type == EntityType.SMALL_MONSTER })
        assertTrue(CompanionLogic.search(data, "Zuwaroposu").none { it.type == EntityType.SMALL_MONSTER })
        assertTrue(CompanionLogic.search(data, "Aptanoth").none { it.type == EntityType.SMALL_MONSTER })
    }

    @Test fun acceptedProductionSmallMonsterProjectionRetains306RelationsAnd75Items() {
        val rows = data.materials.flatMap { it.sources }.filter { it.type == MaterialSourceType.SMALL_MONSTER }
        val projection = rows.smallMonsterPresentationProjection()
        assertEquals(306, rows.size)
        assertEquals(306, projection.rawRelationCount)
        assertEquals(20, projection.monsterCount)
        assertEquals(75, rows.mapNotNull { data.materials.firstOrNull { item -> item.sources.any { source -> source.id == it.id } }?.gameItemId }.toSet().size)
        assertEquals(rows.map { it.id }.toSet(), projection.rawRelationIds.toSet())
    }

    @Test fun globalSearchResultUsesAuthenticIconOpensDetailAndRestoresQuery() {
        rule.onNodeWithTag("global-search").assertTextContains("Item, monster, quest, or weapon")
        rule.onNodeWithTag("global-search").performTextInput("Bnahabra")
        rule.onNodeWithTag("result-small_monster-small_monster_bnahabra").assertIsDisplayed()
        rule.onNodeWithTag("small-monster-icon-small_monster_bnahabra", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("result-small_monster-small_monster_bnahabra").performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithText("Bnahabra").assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("global-search").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("Bnahabra"))
        )
        rule.onNodeWithTag("result-small_monster-small_monster_bnahabra").assertIsDisplayed()
    }

    @Test fun smallMonsterFavoritePersistsOpensCorrectDetailAndRecentDeduplicates() {
        openGlobalSmallMonster("Gargwa", "small_monster_gargwa")
        ensureNotFavorite()
        rule.onNodeWithTag("small-monster-favorite").performClick()
        rule.waitForIdle()
        runBlocking {
            withTimeout(5_000) {
                CompanionRepository(ApplicationProvider.getApplicationContext()).favorites.first { entries ->
                    entries.any {
                        it.type == ReferenceEntityType.SMALL_MONSTER && it.entityId == "small_monster_gargwa"
                    }
                }
            }
        }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
        rule.onNodeWithTag("nav-favorites").performClick()
        rule.onNodeWithTag("favorite-small_monster-small_monster_gargwa").assertIsDisplayed().performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithText("Gargwa").assertIsDisplayed()

        rule.onNodeWithTag("nav-home").performClick()
        rule.onAllNodesWithTag("reference-small_monster-small_monster_gargwa").assertCountEquals(2)[1].performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithTag("nav-home").performClick()
        rule.onAllNodesWithTag("reference-small_monster-small_monster_gargwa").assertCountEquals(2)
    }

    @Test fun itemSmallMonsterSourceOpensTypedDestinationAndBackReturnsToItem() {
        val kelbiSource = data.materials.single { it.name == "Kelbi Horn" }.sources.single {
            it.smallMonsterId == "small_monster_kelbi" && it.condition.orEmpty().contains("Stunned", ignoreCase = true) &&
                it.rank == RewardContext.LOW
        }
        openGlobalItem("Kelbi Horn", "item_kelbi_horn")
        rule.onNodeWithTag("item-small-monster-source-${kelbiSource.id}").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithText("Kelbi").assertIsDisplayed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
        rule.onNodeWithText("Kelbi Horn").assertIsDisplayed()
        rule.onNodeWithTag("item-small-monster-source-${kelbiSource.id}").assertExists()
    }

    @Test fun jaggiItemSourceNeverOpensGreatJaggi() {
        val source = data.materials.first { item -> item.sources.any { it.smallMonsterId == "small_monster_jaggi" } }
            .sources.first { it.smallMonsterId == "small_monster_jaggi" }
        val item = data.materials.single { it.sources.any { candidate -> candidate.id == source.id } }
        openGlobalItem(item.name, item.id)
        rule.onNodeWithTag("item-small-monster-source-${source.id}").performScrollTo().performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
        rule.onNodeWithText("Jaggi").assertIsDisplayed()
        rule.onNodeWithText("Great Jaggi").assertDoesNotExist()
    }

    @Test fun typedQuestTargetsUseCorrectSmallMonsterIconsAndIndexHeadingsAreOmitted() {
        rule.onNodeWithTag("nav-search").performClick()
        rule.onNodeWithText("Large Monster Index").assertDoesNotExist()
        rule.onNodeWithTag("field-filter-small_monster").performClick()
        rule.onNodeWithText("Small Monster Index").assertDoesNotExist()
        rule.onNodeWithTag("field-guide-category-strip")
            .performScrollToNode(hasTestTag("field-filter-quest"))
        rule.onNodeWithTag("field-filter-quest").performClick()
        rule.onNodeWithTag("global-search").performTextInput("Jaggi Takedown")
        rule.onNodeWithTag("quest-row-quest_village_1_star_03").assertIsDisplayed()
        rule.onNodeWithTag("small-monster-icon-small_monster_jaggi").assertIsDisplayed()
        rule.onNodeWithTag("monster-artwork-monster_great_jaggi").assertDoesNotExist()

        rule.onNodeWithTag("global-search").performTextClearance()
        rule.onNodeWithTag("global-search").performTextInput("Bnahabra Cleanup Operation")
        rule.onNodeWithTag("quest-row-quest_guild_6_star_08").assertIsDisplayed()
        rule.onNodeWithTag("small-monster-icon-small_monster_bnahabra").assertIsDisplayed()
    }

    @Test fun typedFavoriteAndRecentCodecRoundTripWithoutCoercion() {
        val favorite = listOf(FavoriteEntry(ReferenceEntityType.SMALL_MONSTER, "small_monster_wroggi", 1))
        val recent = ReferenceHistoryLogic.recordRecent(
            ReferenceHistoryLogic.recordRecent(emptyList(), ReferenceEntityType.SMALL_MONSTER, "small_monster_wroggi", 1),
            ReferenceEntityType.SMALL_MONSTER,
            "small_monster_wroggi",
            2
        )
        assertEquals(favorite, ReferenceEntryCodec.decodeFavorites(ReferenceEntryCodec.encodeFavorites(favorite)))
        assertEquals(1, recent.size)
        assertEquals(ReferenceEntityType.SMALL_MONSTER, recent.single().type)
        assertEquals(recent, ReferenceEntryCodec.decodeRecent(ReferenceEntryCodec.encodeRecent(recent)))
    }

    private fun openGlobalSmallMonster(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-small_monster-$id").performClick()
        rule.onNodeWithTag("screen-small-monster").assertIsDisplayed()
    }

    private fun openGlobalItem(name: String, id: String) {
        rule.onNodeWithTag("global-search").performTextInput(name)
        rule.onNodeWithTag("result-material-$id").performClick()
        rule.onNodeWithTag("screen-material").assertIsDisplayed()
    }

    private fun ensureNotFavorite() {
        if (rule.onAllNodesWithContentDescription("Remove from favorites").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription("Remove from favorites").performClick()
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onAllNodesWithContentDescription("Add to favorites").fetchSemanticsNodes().isNotEmpty()
            }
            runBlocking {
                withTimeout(5_000) {
                    CompanionRepository(ApplicationProvider.getApplicationContext()).favorites.first { entries ->
                        entries.none {
                            it.type == ReferenceEntityType.SMALL_MONSTER && it.entityId == "small_monster_gargwa"
                        }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
    }
}
