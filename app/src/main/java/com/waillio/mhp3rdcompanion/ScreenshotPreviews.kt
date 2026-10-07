package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import com.waillio.mhp3rdcompanion.data.*

private const val AYN_THOR_DEVICE = "spec:width=620dp,height=540dp,dpi=320"

private val previewMaterial = Material(
    id = "zinogre_claw",
    name = "Zinogre Claw+",
    rarity = 6,
    description = "A charged Zinogre claw.",
    sources = listOf(
        MaterialSource(
            id = "preview_zinogre_claw",
            type = MaterialSourceType.MONSTER_REWARD,
            name = "Zinogre",
            monsterId = "monster_zinogre",
            rank = RewardContext.HIGH,
            method = "PART_BREAK",
            condition = "Break forelegs",
            chance = 22,
            quantity = 1
        )
    ),
    uses = listOf("Thunder Sword · Required: 4")
)

private val previewQuest = Quest("thunder_call", "Call of Thunder", "Zinogre · High Rank")

private val previewFixture = FixtureData(
    monsters = listOf(Monster("zinogre", "Zinogre", "Fanged Wyvern · Weakness: Ice")),
    materials = listOf(
        previewMaterial,
        previewMaterial.copy(id = "narga_marrow", name = "Nargacuga Marrow"),
        previewMaterial.copy(id = "narga_fang", name = "Nargacuga Fang+"),
        previewMaterial.copy(id = "thunderbug", name = "Thunderbug Jewel")
    ),
    quests = listOf(previewQuest)
)

@Composable
private fun PreviewShell(
    selectedRoute: String,
    query: String = "",
    content: @Composable (PaddingValues) -> Unit
) {
    CompanionTheme {
        if (selectedRoute == Routes.MONSTER) {
            DetailShell(selectedRoute = selectedRoute, onNavigate = {}, content = content)
        } else {
            ShellFrame(
                selectedRoute = selectedRoute,
                query = query,
                onQueryChange = {},
                onSearch = {},
                onNavigate = {},
                content = content
            )
        }
    }
}

@Preview(name = "Screenshot · Home · 1240x1080", device = AYN_THOR_DEVICE, showSystemUi = false)
@Composable
private fun HomeScreenshotPreview() {
    PreviewShell(Routes.HOME) { padding ->
        Box(Modifier.padding(padding)) {
            HomeScreen(
                data = previewFixture,
                recent = listOf(RecentEntry(ReferenceEntityType.MONSTER, "zinogre", 2), RecentEntry(ReferenceEntityType.MATERIAL, "zinogre_claw", 1)),
                favorites = listOf(FavoriteEntry(ReferenceEntityType.MONSTER, "zinogre", 1)),
                onMaterials = {},
                onMonsters = {},
                onMaps = {},
                onFavorites = {},
                onEntity = { _, _ -> },
            )
        }
    }
}

@Preview(name = "Screenshot · Monster · 1240x1080", device = AYN_THOR_DEVICE, showSystemUi = false)
@Composable
private fun MonsterScreenshotPreview() {
    val monster = Monster(
        id = "zinogre", name = "Zinogre", subtitle = "Fanged Wyvern - Weakness: Ice",
        type = "Fanged Wyvern", threatLevel = 6, weaknesses = ElementValues(1, 2, 0, 3, 1),
        recommendedParts = listOf("Head", "Forelegs", "Back"),
        breakableParts = listOf(BreakablePart("Horns", "Break", 2), BreakablePart("Tail", "Sever")),
        rewards = listOf(MonsterReward("Zinogre Claw+", "Part Break", "Forelegs", "22%")),
        hitzones = listOf(Hitzone("Head", 55, 60, 45, 10, 20, 0, 25, 5)), questIds = listOf("thunder_call")
    )
    PreviewShell(Routes.MONSTER) { padding ->
        val tab = remember { mutableIntStateOf(0) }
        Box(Modifier.padding(padding)) {
            MonsterScreen(
                monster = monster,
                quests = listOf(previewQuest),
                materials = previewFixture.materials,
                selectedTab = tab.intValue,
                isFavorite = false,
                onBack = {},
                onFavorite = {},
                onTabChange = { tab.intValue = it },
                onMaterial = {}
            )
        }
    }
}

@Preview(name = "Screenshot · Search · 1240x1080", device = AYN_THOR_DEVICE, showSystemUi = false)
@Composable
private fun SearchScreenshotPreview() {
    PreviewShell(Routes.SEARCH, query = "Zinogre") { padding ->
        Box(Modifier.padding(padding)) {
            SearchScreen(
                query = "Zinogre",
                filter = null,
                results = listOf(
                    SearchResult("zinogre_claw", "Zinogre Claw+", "A charged Zinogre claw.", EntityType.MATERIAL),
                    SearchResult("zinogre", "Zinogre", "Fanged Wyvern · Weakness: Ice", EntityType.MONSTER),
                    SearchResult("thunder_call", "Call of Thunder", "Zinogre · High Rank", EntityType.QUEST)
                ),
                listState = rememberLazyListState(),
                onFilter = {},
                onResult = {}
            )
        }
    }
}

@Preview(name = "Screenshot · Material · 1240x1080", device = AYN_THOR_DEVICE, showSystemUi = false)
@Composable
private fun MaterialScreenshotPreview() {
    PreviewShell(Routes.MATERIAL) { padding ->
        Box(Modifier.padding(padding)) {
            MaterialScreen(
                material = previewMaterial,
                quests = listOf(previewQuest),
                isFavorite = false,
                onBack = {},
                onFavorite = {}
            )
        }
    }
}

@Preview(name = "Screenshot · Favorites · 1240x1080", device = AYN_THOR_DEVICE, showSystemUi = false)
@Composable
private fun FavoritesScreenshotPreview() {
    PreviewShell(Routes.FAVORITES) { padding ->
        Box(Modifier.padding(padding)) {
            FavoritesScreen(
                favorites = listOf(
                    FavoriteEntry(ReferenceEntityType.MONSTER, "zinogre", 2),
                    FavoriteEntry(ReferenceEntityType.MATERIAL, "zinogre_claw", 1)
                ),
                data = previewFixture,
                onEntity = { _, _ -> }
            )
        }
    }
}
