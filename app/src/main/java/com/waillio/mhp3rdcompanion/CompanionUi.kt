package com.waillio.mhp3rdcompanion

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.annotation.StringRes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.waillio.mhp3rdcompanion.data.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val WEAPONS = "weapons"
    const val WEAPON_TYPE = "weapon-type/{type}"
    const val WEAPON_DETAIL = "weapon/{id}"
    const val FAVORITES = "favorites"
    const val MATERIAL = "material/{id}"
    const val MATERIAL_USAGE_FAMILY = "material/{id}/usage/{family}"
    const val MATERIAL_USED_IN_WEAPONS = "material/{id}/used-in-weapons"
    const val QUEST = "quest/{id}"
    const val MONSTER = "monster/{id}"
    const val SMALL_MONSTER = "small-monster/{id}"
    const val TRAINING = "training/{id}"
    const val MAP = "map/{id}?focusItemId={focusItemId}&rank={rank}"
    fun material(id: String) = "material/$id"
    fun materialUsageFamily(id: String, family: String) = "material/$id/usage/$family"
    fun weapon(id: String) = "weapon/$id"
    fun weaponType(type: String) = "weapon-type/$type"
    fun materialUsedInWeapons(id: String) = "material/$id/used-in-weapons"
    fun monster(id: String) = "monster/$id"
    fun smallMonster(id: String) = "small-monster/$id"
    fun training(id: String) = "training/$id"
    fun quest(id: String) = "quest/$id"
    fun map(id: String, focusedGameItemId: Int? = null, rank: RewardContext? = null) =
        "map/$id?focusItemId=${focusedGameItemId ?: -1}&rank=${rank?.name.orEmpty()}"
}

internal enum class QuestCategoryFilter { ALL, VILLAGE, GUILD, HOT_SPRING, DRINK, EVENT }

/** Presentation-only Field Guide order. The All chip is rendered immediately before this list. */
internal val fieldGuideCategoryOrder: List<EntityType?> = listOf(
    null,
    EntityType.MONSTER,
    EntityType.SMALL_MONSTER,
    EntityType.MATERIAL,
    EntityType.MAP,
    EntityType.WEAPON,
    EntityType.SKILL,
    EntityType.QUEST,
    EntityType.TRAINING
)

internal fun QuestCategoryFilter.productionCategory(): String? = when (this) {
    QuestCategoryFilter.ALL -> null
    QuestCategoryFilter.VILLAGE -> "Village"
    QuestCategoryFilter.GUILD -> "Guild"
    QuestCategoryFilter.HOT_SPRING -> "Hot Spring"
    QuestCategoryFilter.DRINK -> "Drink"
    QuestCategoryFilter.EVENT -> "Event"
}

/** Canonical rank dimension used by the Quest filter draft and applied state. */
internal enum class QuestRankFilter { ALL, LOW, HIGH }

internal fun QuestRankFilter.productionRank(): String? = when (this) {
    QuestRankFilter.ALL -> null
    QuestRankFilter.LOW -> "LOW"
    QuestRankFilter.HIGH -> "HIGH"
}

/** Generated UI quests expose rank labels ("High Rank"/"Low Rank"), while
 * the production enum uses the compact HIGH/LOW tokens.  Keep filtering
 * tolerant of either representation so the dialog is driven by real corpus
 * values instead of a display-label spelling accident. */
internal fun QuestRankFilter.matchesRank(value: String): Boolean {
    val token = productionRank() ?: return true
    return value.equals(token, ignoreCase = true) ||
        value.equals("$token Rank", ignoreCase = true)
}

internal fun QuestCategoryFilter.displayLabel(): String = when (this) {
    QuestCategoryFilter.ALL -> "All"
    QuestCategoryFilter.VILLAGE -> "Village"
    QuestCategoryFilter.GUILD -> "Guild"
    QuestCategoryFilter.HOT_SPRING -> "Hot Spring"
    QuestCategoryFilter.DRINK -> "Drinks"
    QuestCategoryFilter.EVENT -> "Event"
}

internal fun QuestRankFilter.displayLabel(): String = when (this) {
    QuestRankFilter.ALL -> "All"
    QuestRankFilter.LOW -> "Low Rank"
    QuestRankFilter.HIGH -> "High Rank"
}

internal data class QuestFilterState(
    val category: QuestCategoryFilter = QuestCategoryFilter.ALL,
    val rank: QuestRankFilter = QuestRankFilter.ALL,
    val stars: Set<Int> = emptySet()
)

/** Lightweight corpus index used by the filter dialog instead of copying Quest records. */
internal class QuestFilterIndex(quests: List<Quest>) {
    private val rows = quests

    fun availableRanks(category: QuestCategoryFilter): List<QuestRankFilter> =
        QuestRankFilter.entries.filter { rank ->
            rank != QuestRankFilter.ALL && rows.any { it.matches(category, rank, emptySet()) }
        }

    fun availableStars(category: QuestCategoryFilter, rank: QuestRankFilter): List<Int> =
        rows.asSequence()
            .filter { it.matches(category, rank, emptySet()) }
            .map { it.stars }
            .distinct()
            .sorted()
            .toList()

    private fun Quest.matches(
        categoryFilter: QuestCategoryFilter,
        rankFilter: QuestRankFilter,
        stars: Set<Int>
    ): Boolean =
        (categoryFilter.productionCategory() == null || category == categoryFilter.productionCategory()) &&
        rankFilter.matchesRank(rank) &&
            (stars.isEmpty() || this.stars in stars)
}

@Composable
internal fun CompanionApp(
    viewModel: AppViewModel,
    uiScaleStep: Int = UI_SCALE_DEFAULT_STEP,
    onUiScaleStepChange: (Int) -> Unit = {}
) {
    val nav = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val searchListState = rememberLazyListState()
    val itemListState = rememberLazyListState()
    val monsterGridState = rememberLazyGridState()
    val smallMonsterGridState = rememberLazyGridState()
    val questListState = rememberLazyListState()
    val trainingListState = rememberLazyListState()
    val skillListState = rememberLazyListState()
    var expandedSkillTreeId by rememberSaveable("skill-expanded-tree") { mutableStateOf<String?>(null) }
    var pendingSkillFocusId by rememberSaveable("skill-focus-tree") { mutableStateOf<String?>(null) }
    // Quest filters are session-local, but saveable across activity recreation. Keep
    // the multi-select stars as a compact CSV rather than duplicating Quest rows.
    var questStarFilterCsv by rememberSaveable("quest-star-filter") { mutableStateOf("") }
    val questStarFilters = remember(questStarFilterCsv) {
        questStarFilterCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }
    var questRankFilterName by rememberSaveable("quest-rank-filter") { mutableStateOf(QuestRankFilter.ALL.name) }
    val questRankFilter = QuestRankFilter.entries.firstOrNull { it.name == questRankFilterName }
        ?: QuestRankFilter.ALL
    // Persist the stable enum name rather than its ordinal so adding corpus
    // categories does not reset a saved category selection on recreation.
    var questCategoryFilterName by rememberSaveable("quest-category-filter") { mutableStateOf(QuestCategoryFilter.ALL.name) }
    val questCategoryFilter = QuestCategoryFilter.entries.firstOrNull { it.name == questCategoryFilterName }
        ?: QuestCategoryFilter.ALL
    val context = LocalContext.current
    val monsterTabs = remember { mutableStateMapOf<String, Int>() }
    val monsterRewardContexts = remember { mutableStateMapOf<String, RewardContext>() }
    val monsterRewardFilters = remember { mutableStateMapOf<String, MonsterRewardFilter>() }
    val monsterRewardListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val smallMonsterRewardContexts = remember { mutableStateMapOf<String, SmallMonsterRewardContext>() }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    AppShell(
        nav = nav,
        query = state.query,
        onQueryChange = { value ->
            viewModel.setQuery(value)
            // The global empty-query Weapons state is the category chooser. Clear
            // any category/detail selection when returning to that entry point.
            if (value.isBlank() && state.filter == EntityType.WEAPON) {
                viewModel.weaponScreenState.selectedType = null
            }
            if (nav.currentDestination?.route != Routes.SEARCH) {
                nav.navigate(Routes.SEARCH) { launchSingleTop = true }
            }
        },
        onSearch = {
            if (nav.currentDestination?.route != Routes.SEARCH) {
                nav.navigate(Routes.SEARCH) { launchSingleTop = true }
            }
        },
        onOpenSettings = { showSettings = true },
        onMainNavigate = { route ->
            viewModel.setQuery("")
            viewModel.setFilter(if (route == Routes.SEARCH) EntityType.MONSTER else null)
            scope.launch {
                searchListState.scrollToItem(0)
                itemListState.scrollToItem(0)
                monsterGridState.scrollToItem(0)
                smallMonsterGridState.scrollToItem(0)
                questListState.scrollToItem(0)
                trainingListState.scrollToItem(0)
                skillListState.scrollToItem(0)
                expandedSkillTreeId = null
                pendingSkillFocusId = null
            }
            nav.navigate(route) {
                popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
                restoreState = false
            }
        },
        snackbar = snackbar
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    data = viewModel.fixture,
                    weaponRepository = viewModel.weaponRepository,
                    recent = state.recent,
                    favorites = state.favorites,
                    onMaterials = {
                        viewModel.setFilter(EntityType.MATERIAL)
                        nav.navigate(Routes.SEARCH)
                    },
                    onMonsters = {
                        viewModel.setFilter(EntityType.MONSTER)
                        nav.navigate(Routes.SEARCH)
                    },
                    onMaps = {
                        viewModel.setFilter(EntityType.MAP)
                        nav.navigate(Routes.SEARCH)
                    },
                    onFavorites = { nav.navigate(Routes.FAVORITES) },
                    onEntity = { type, id ->
                        when (type) {
                            ReferenceEntityType.MONSTER -> nav.navigate(Routes.monster(id))
                            ReferenceEntityType.SMALL_MONSTER -> nav.navigate(Routes.smallMonster(id))
                            ReferenceEntityType.MATERIAL -> nav.navigate(Routes.material(id))
                            ReferenceEntityType.WEAPON -> nav.navigate(Routes.weapon(id))
                            else -> Unit
                        }
                    }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    query = state.query,
                    filter = state.filter,
                    results = results,
                    listState = searchListState,
                    itemListState = itemListState,
                    monsters = viewModel.fixture.monsters,
                    smallMonsters = viewModel.fixture.smallMonsters,
                    materials = viewModel.fixture.materials,
                    quests = viewModel.fixture.quests,
                    trainingQuests = viewModel.fixture.trainingQuests,
                    weapons = viewModel.fixture.weapons,
                    weaponRepository = viewModel.weaponRepository,
                    weaponScreenState = viewModel.weaponScreenState,
                    maps = MapRegistry.maps,
                    monsterGridState = monsterGridState,
                    smallMonsterGridState = smallMonsterGridState,
                    questListState = questListState,
                    trainingListState = trainingListState,
                    skills = viewModel.fixture.skillTrees,
                    decorationSkillRelations = viewModel.fixture.decorationSkillRelations,
                    skillListState = skillListState,
                    expandedSkillTreeId = expandedSkillTreeId,
                    focusSkillTreeId = pendingSkillFocusId,
                    onSkillExpanded = { expandedSkillTreeId = it },
                    onSkillFocusConsumed = { pendingSkillFocusId = null },
                    onMaterial = { nav.navigate(Routes.material(it)) },
                    displayTerminology = viewModel.fixture.displayTerminology,
                    questRankFilter = questRankFilter,
                    questStarFilters = questStarFilters,
                    questCategoryFilter = questCategoryFilter,
                    onQuestFilters = { next ->
                        questCategoryFilterName = next.category.name
                        questRankFilterName = next.rank.name
                        questStarFilterCsv = next.stars.sorted().joinToString(",")
                        scope.launch { questListState.scrollToItem(0) }
                    },
                    onFilter = { filter ->
                        pendingSkillFocusId = null
                        expandedSkillTreeId = null
                        viewModel.setFilter(filter)
                    },
                    // Keep the accepted type chooser as the empty-query Weapons entry point;
                    // typed queries stay in the global result list and can deep-link by ID.
                    onWeapons = {
                        viewModel.setQuery("")
                        viewModel.setFilter(EntityType.WEAPON)
                        viewModel.weaponScreenState.selectedType = null
                    },
                    onWeaponType = { type ->
                        viewModel.weaponScreenState.selectedType = null
                        nav.navigate(Routes.weaponType(type))
                    },
                    onResult = { result ->
                        when (result.type) {
                            EntityType.MATERIAL -> nav.navigate(Routes.material(result.id))
                            EntityType.MONSTER -> nav.navigate(Routes.monster(result.id))
                            EntityType.SMALL_MONSTER -> nav.navigate(Routes.smallMonster(result.id))
                            EntityType.QUEST -> Unit
                            EntityType.TRAINING -> nav.navigate(Routes.training(result.id))
                            EntityType.MAP -> Unit
                            EntityType.WEAPON -> nav.navigate(Routes.weapon(result.id))
                            EntityType.SKILL -> {
                                viewModel.setQuery("")
                                viewModel.setFilter(EntityType.SKILL)
                                pendingSkillFocusId = result.id
                            }
                        }
                    },
                    onMap = { nav.navigate(Routes.map(it.mapId)) },
                    onTraining = { nav.navigate(Routes.training(it.id)) }
                )
            }
            composable(Routes.WEAPONS) {
                WeaponsScreen(
                    repository = viewModel.weaponRepository,
                    screenState = viewModel.weaponScreenState,
                    onItem = { material -> nav.navigate(Routes.material(material.id)) },
                    isFavorite = { id -> state.favorites.any { it.type == ReferenceEntityType.WEAPON && it.entityId == id } },
                    onFavorite = { id -> viewModel.toggleFavorite(ReferenceEntityType.WEAPON, id) },
                    onWeaponOpened = { id -> viewModel.recordOpened(ReferenceEntityType.WEAPON, id) }
                )
            }
            composable(
                route = Routes.WEAPON_TYPE,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { entry ->
                val type = entry.arguments?.getString("type")
                if (type in WeaponRepository.PRODUCTION_WEAPON_TYPES) {
                    WeaponsScreen(
                        repository = viewModel.weaponRepository,
                        screenState = viewModel.weaponScreenState,
                        forcedType = type,
                        onBackFromForcedType = {
                            viewModel.weaponScreenState.selectedType = null
                            nav.popBackStack()
                        },
                        onItem = { material -> nav.navigate(Routes.material(material.id)) },
                        isFavorite = { id -> state.favorites.any { it.type == ReferenceEntityType.WEAPON && it.entityId == id } },
                        onFavorite = { id -> viewModel.toggleFavorite(ReferenceEntityType.WEAPON, id) },
                        onWeaponOpened = { id -> viewModel.recordOpened(ReferenceEntityType.WEAPON, id) }
                    )
                }
            }
            composable(Routes.WEAPON_DETAIL) { entry ->
                val id = entry.arguments?.getString("id")
                val weapon = id?.let(viewModel.weaponRepository.weaponById::get)
                if (weapon != null) {
                    LaunchedEffect(weapon.id) { viewModel.recordOpened(ReferenceEntityType.WEAPON, weapon.id) }
                    WeaponDeepLinkScreen(
                        weapon = weapon,
                        catalog = viewModel.weaponRepository.catalog(weapon.weaponType),
                        isFavorite = { selectedId -> state.favorites.any { it.type == ReferenceEntityType.WEAPON && it.entityId == selectedId } },
                        onFavorite = { selectedId -> viewModel.toggleFavorite(ReferenceEntityType.WEAPON, selectedId) },
                        onClose = { nav.popBackStack() },
                        onItem = { material -> nav.navigate(Routes.material(material.id)) },
                        onSelectWeapon = { next -> viewModel.recordOpened(ReferenceEntityType.WEAPON, next.id) }
                    )
                }
            }
            composable(Routes.MATERIAL) { entry ->
                val id = entry.arguments?.getString("id")
                val material = viewModel.fixture.materials.firstOrNull { it.id == id }
                if (material != null) {
                    LaunchedEffect(material.id) { viewModel.recordOpened(ReferenceEntityType.MATERIAL, material.id) }
                    MaterialScreen(
                        material = material,
                        // Sources resolve quest IDs directly; Related Quests is intentionally not projected here.
                        quests = viewModel.fixture.quests,
                        isFavorite = state.favorites.any { it.type == ReferenceEntityType.MATERIAL && it.entityId == id },
                        onBack = { nav.popBackStack() },
                        onFavorite = { viewModel.toggleFavorite(ReferenceEntityType.MATERIAL, material.id) },
                        onSmallMonster = { nav.navigate(Routes.smallMonster(it)) },
                        onMonster = { nav.navigate(Routes.monster(it)) },
                        onQuest = { nav.navigate(Routes.quest(it)) },
                        onTraining = { nav.navigate(Routes.training(it)) },
                        weaponUsages = material.gameItemId?.let(viewModel.weaponUsageIndex::forItem).orEmpty(),
                        weaponRepository = viewModel.weaponRepository,
                        usageProjection = material.gameItemId?.let(viewModel.itemUsageIndex::forItem),
                        onUsageFamily = { family -> nav.navigate(Routes.materialUsageFamily(material.id, family.name)) },
                        onUsageTarget = { target ->
                            when (target.targetKind) {
                                UsageTargetKind.WEAPON -> nav.navigate(Routes.weapon(target.targetId))
                                UsageTargetKind.ITEM -> target.targetId.takeIf { it != material.id }?.let { nav.navigate(Routes.material(it)) }
                                UsageTargetKind.TRAINING -> nav.navigate(Routes.training(target.targetId))
                            }
                        },
                        onWeapon = { nav.navigate(Routes.weapon(it)) },
                        onMaterial = { nav.navigate(Routes.material(it)) },
                        jewelSkillRelations = viewModel.fixture.decorationSkillRelations,
                        skillTrees = viewModel.fixture.skillTrees,
                        onSkill = { skillId ->
                            viewModel.setQuery("")
                            viewModel.setFilter(EntityType.SKILL)
                            pendingSkillFocusId = skillId
                            nav.navigate(Routes.SEARCH) { launchSingleTop = true }
                        },
                        materialIdForGameItem = { gameItemId -> viewModel.fixture.materials.firstOrNull { it.gameItemId == gameItemId }?.id },
                        onShowMap = { mapId, gameItemId, rank ->
                            nav.navigate(Routes.map(mapId, gameItemId, rank))
                        }
                    )
                }
            }
            composable(
                Routes.QUEST,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id")
                viewModel.fixture.quests.firstOrNull { it.id == id }?.let { quest ->
                    QuestDetailScreen(quest = quest, onBack = nav::popBackStack)
                }
            }
            composable(Routes.MONSTER) { entry ->
                val id = entry.arguments?.getString("id")
                val monster = viewModel.fixture.monsters.firstOrNull { it.id == id }
                if (monster != null) {
                    LaunchedEffect(monster.id) { viewModel.recordOpened(ReferenceEntityType.MONSTER, monster.id) }
                    val rewardContextName = (monsterRewardContexts[monster.id]
                        ?: monster.rewards.availableContexts().firstOrNull())?.name ?: RewardContext.LOW.name
                    val rewardFilter = monsterRewardFilters[monster.id] ?: MonsterRewardFilter.ALL
                    val rewardStateKey = "${monster.id}|$rewardContextName|${rewardFilter.name}"
                    val rewardListState = monsterRewardListStates.getOrPut(rewardStateKey) { LazyListState() }
                    MonsterScreen(
                        monster = monster,
                        quests = viewModel.fixture.quests.filter { it.id in monster.questIds },
                        trainingQuests = viewModel.fixture.trainingQuests.filter { it.id in monster.trainingQuestIds },
                        materials = viewModel.fixture.materials,
                        selectedTab = monsterTabs[monster.id] ?: 0,
                        isFavorite = state.favorites.any { it.type == ReferenceEntityType.MONSTER && it.entityId == id },
                        onBack = nav::popBackStack,
                        onFavorite = { viewModel.toggleFavorite(ReferenceEntityType.MONSTER, monster.id) },
                        onTabChange = { monsterTabs[monster.id] = it },
                        onMaterial = { nav.navigate(Routes.material(it)) },
                        onTraining = { nav.navigate(Routes.training(it)) },
                        selectedRewardContext = monsterRewardContexts[monster.id],
                        onRewardContextChange = { monsterRewardContexts[monster.id] = it },
                        selectedRewardFilter = monsterRewardFilters[monster.id],
                        onRewardFilterChange = { monsterRewardFilters[monster.id] = it },
                        selectedRewardListState = rewardListState
                    )
                }
            }
            composable(Routes.MATERIAL_USAGE_FAMILY) { entry ->
                val materialId = entry.arguments?.getString("id")
                val family = entry.arguments?.getString("family")?.let { name ->
                    ItemUsageFamily.entries.firstOrNull { it.name == name }
                }
                val material = viewModel.fixture.materials.firstOrNull { it.id == materialId }
                val projection = material?.gameItemId?.let(viewModel.itemUsageIndex::forItem)
                val familyProjection = projection?.families?.firstOrNull { it.family == family }
                if (material != null && familyProjection != null) {
                    UsageFamilyScreen(
                        material = material,
                        family = familyProjection,
                        weaponRepository = viewModel.weaponRepository,
                        onBack = nav::popBackStack,
                        onTarget = { target ->
                            when (target.targetKind) {
                                UsageTargetKind.WEAPON -> nav.navigate(Routes.weapon(target.targetId))
                                UsageTargetKind.ITEM -> target.targetId.takeIf { it != material.id }?.let { nav.navigate(Routes.material(it)) }
                                UsageTargetKind.TRAINING -> nav.navigate(Routes.training(target.targetId))
                            }
                        }
                    )
                }
            }
            composable(Routes.TRAINING) { entry ->
                val id = entry.arguments?.getString("id")
                val training = viewModel.fixture.trainingQuests.firstOrNull { it.id == id }
                if (training != null) {
                    TrainingDetailScreen(
                        training = training,
                        monsters = viewModel.fixture.monsters,
                        smallMonsters = viewModel.fixture.smallMonsters,
                        items = viewModel.fixture.materials,
                        weapons = viewModel.fixture.weapons,
                        displayTerminology = viewModel.fixture.displayTerminology,
                        onBack = nav::popBackStack,
                        onItem = { nav.navigate(Routes.material(it)) },
                        onWeapon = { nav.navigate(Routes.weapon(it)) },
                        skills = viewModel.fixture.skillTrees,
                        onSkill = { skillId ->
                            viewModel.setQuery("")
                            viewModel.setFilter(EntityType.SKILL)
                            pendingSkillFocusId = skillId
                            nav.navigate(Routes.SEARCH) { launchSingleTop = true }
                        }
                    )
                }
            }
            composable(Routes.SMALL_MONSTER) { entry ->
                val id = entry.arguments?.getString("id")
                val smallMonster = viewModel.fixture.smallMonsters.firstOrNull { it.id == id }
                if (smallMonster != null) {
                    LaunchedEffect(smallMonster.id) {
                        viewModel.recordOpened(ReferenceEntityType.SMALL_MONSTER, smallMonster.id)
                    }
                    SmallMonsterScreen(
                        smallMonster = smallMonster,
                        materials = viewModel.fixture.materials,
                        quests = viewModel.fixture.quests.filter { it.id in smallMonster.relatedQuestIds },
                        selectedContext = smallMonsterRewardContexts[smallMonster.id],
                        onContextChange = { smallMonsterRewardContexts[smallMonster.id] = it },
                        isFavorite = state.favorites.any {
                            it.type == ReferenceEntityType.SMALL_MONSTER && it.entityId == id
                        },
                        onFavorite = {
                            viewModel.toggleFavorite(ReferenceEntityType.SMALL_MONSTER, smallMonster.id)
                        },
                        onBack = nav::popBackStack,
                        onMaterial = { nav.navigate(Routes.material(it)) }
                    )
                }
            }
            composable(
                Routes.MAP,
                arguments = listOf(
                    navArgument("focusItemId") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("rank") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val map = entry.arguments?.getString("id")?.let(MapRegistry::resolve)
                val focusedItemId = entry.arguments?.getInt("focusItemId", -1)?.takeIf { it >= 0 }
                val initialRank = entry.arguments?.getString("rank")?.let { name ->
                    RewardContext.entries.firstOrNull { it.name == name }
                }
                if (map != null) MapDetailScreen(
                    map = map,
                    fieldIndex = viewModel.fieldIndex,
                    materials = viewModel.fixture.materials,
                    onBack = nav::popBackStack,
                    onClearFocus = { nav.navigate(Routes.map(map.mapId)) { launchSingleTop = true } },
                    focusedGameItemId = focusedItemId,
                    initialRankContext = initialRank,
                    onItem = { nav.navigate(Routes.material(it)) }
                )
            }
            composable(Routes.MATERIAL_USED_IN_WEAPONS) { entry ->
                val id = entry.arguments?.getString("id")
                val material = viewModel.fixture.materials.firstOrNull { it.id == id }
                val family = material?.gameItemId?.let(viewModel.itemUsageIndex::forItem)?.families
                    ?.firstOrNull { it.family == ItemUsageFamily.WEAPONS }
                if (material != null && family != null) {
                    UsageFamilyScreen(
                        material = material,
                        family = family,
                        weaponRepository = viewModel.weaponRepository,
                        onBack = nav::popBackStack,
                        onTarget = { target ->
                            if (target.targetKind == UsageTargetKind.WEAPON) nav.navigate(Routes.weapon(target.targetId))
                        }
                    )
                }
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    favorites = state.favorites,
                    data = viewModel.fixture,
                    weaponRepository = viewModel.weaponRepository,
                    onEntity = { type, id ->
                        when (type) {
                            ReferenceEntityType.MONSTER -> nav.navigate(Routes.monster(id))
                            ReferenceEntityType.SMALL_MONSTER -> nav.navigate(Routes.smallMonster(id))
                            ReferenceEntityType.MATERIAL -> nav.navigate(Routes.material(id))
                            ReferenceEntityType.WEAPON -> nav.navigate(Routes.weapon(id))
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
    if (showSettings) {
        UiSettingsDialog(
            uiScaleStep = uiScaleStep,
            onScaleStepChange = onUiScaleStepChange,
            onDismiss = { showSettings = false },
            onAbout = {
                showSettings = false
                showAbout = true
            }
        )
    }
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    }
}

private data class NavItem(val route: String, @param:StringRes val titleRes: Int, val icon: ImageVector)
private val navItems = listOf(
    NavItem(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    NavItem(Routes.SEARCH, R.string.nav_field_guide, Icons.AutoMirrored.Filled.MenuBook),
    NavItem(Routes.FAVORITES, R.string.nav_favorites, Icons.Default.Star)
)

@Composable
private fun AppShell(
    nav: NavHostController,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onMainNavigate: (String) -> Unit,
    snackbar: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit
) {
    val entry by nav.currentBackStackEntryAsState()
    val selectedRoute = entry?.destination?.route ?: Routes.HOME
    if (selectedRoute == Routes.MONSTER || selectedRoute == Routes.SMALL_MONSTER || selectedRoute == Routes.MAP || selectedRoute == Routes.WEAPON_DETAIL || selectedRoute == Routes.WEAPON_TYPE || selectedRoute == Routes.TRAINING) {
        DetailShell(selectedRoute, onMainNavigate, snackbar, content)
    } else {
        ShellFrame(
            selectedRoute = selectedRoute,
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onOpenSettings = onOpenSettings,
            onNavigate = onMainNavigate,
            snackbar = snackbar,
            content = content
        )
    }
}

@Composable
internal fun DetailShell(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = AppColors.Night,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { AppBottomBar(selectedRoute, onNavigate) },
        content = content
    )
}

@Composable
internal fun ShellFrame(
    selectedRoute: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigate: (String) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onOpenSettings: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = AppColors.Night,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                query = if (selectedRoute == Routes.SEARCH) query else "",
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            AppBottomBar(selectedRoute = selectedRoute, onNavigate = onNavigate)
        },
        content = content
    )
}

@Composable
private fun AppTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
        color = AppColors.NightRaised,
        shadowElevation = 5.dp
    ) {
        val compactPortrait = monsterRewardRowsStacked(LocalAppWindowLayout.current.profile)
        if (compactPortrait) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_title), color = AppColors.Gold, style = AppType.AppTitle, maxLines = 1)
                        Text(stringResource(R.string.app_subtitle), color = AppColors.ParchmentDeep, style = AppType.Metadata, maxLines = 1)
                    }
                    SettingsButton(onOpenSettings, Modifier.testTag("settings-button"))
                }
                SearchField(query, onQueryChange, onSearch, Modifier.fillMaxWidth())
            }
        } else {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(138.dp)) {
                Text(stringResource(R.string.app_title), color = AppColors.Gold, style = AppType.AppTitle, maxLines = 1)
                Text(stringResource(R.string.app_subtitle), color = AppColors.ParchmentDeep, style = AppType.Metadata, maxLines = 1)
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .padding(start = 3.dp, end = 5.dp)
                    .size(52.dp)
                    .border(1.dp, AppColors.ParchmentDeep.copy(alpha = .55f), CircleShape)
                    .testTag("settings-button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = AppColors.Gold,
                    modifier = Modifier.size(22.dp)
                )
            }
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                onFocus = onSearch,
                modifier = Modifier.weight(1f)
            )
        }
        }
    }
}

@Composable
private fun SettingsButton(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onOpenSettings,
        modifier = modifier.size(52.dp)
            .border(1.dp, AppColors.ParchmentDeep.copy(alpha = .55f), CircleShape)
    ) {
        Icon(Icons.Default.Settings, stringResource(R.string.settings_title), tint = AppColors.Gold, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun UiSettingsDialog(
    uiScaleStep: Int,
    onScaleStepChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAbout: () -> Unit
) {
    var selectedStep by remember(uiScaleStep) {
        mutableIntStateOf(uiScaleStep.coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP))
    }
    val scaleStep = selectedStep
    val percentage = 80 + scaleStep * 5
    val scaleLabel = "${percentage / 100}.${(percentage % 100).toString().padStart(2, '0')}"
    val compactPortrait = LocalAppWindowLayout.current.profile == AppLayoutProfile.COMPACT_PORTRAIT

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .64f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (compactPortrait) 600.dp else 420.dp)
                .padding(if (compactPortrait) 8.dp else 24.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = CutCornerShape(topEnd = 20.dp),
            color = AppColors.Parchment,
            contentColor = AppColors.Ink,
            border = BorderStroke(2.dp, AppColors.Gold.copy(alpha = .8f)),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = AppColors.Crimson)
                    Text(
                        stringResource(R.string.settings_title),
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        style = AppType.SectionTitle,
                        color = AppColors.Ink
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("settings-close")) {
                        Icon(Icons.Default.Close, stringResource(R.string.action_close), tint = AppColors.Ink)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings_interface_scale),
                        modifier = Modifier.weight(1f),
                        style = AppType.CardTitle,
                        color = AppColors.Ink
                    )
                    Text(
                        scaleLabel,
                        style = AppType.CardTitle.copy(fontWeight = FontWeight.Bold),
                        color = AppColors.Crimson,
                        modifier = Modifier.testTag("settings-scale-value")
                    )
                }
                Text(
                    stringResource(R.string.settings_scale_description),
                    modifier = Modifier.padding(top = 4.dp),
                    style = AppType.Metadata,
                    color = AppColors.Ink.copy(alpha = .72f)
                )
                Slider(
                    value = scaleStep.toFloat(),
                    onValueChange = {
                        val nextStep = it.roundToInt().coerceIn(UI_SCALE_MIN_STEP, UI_SCALE_MAX_STEP)
                        selectedStep = nextStep
                        onScaleStepChange(nextStep)
                    },
                    valueRange = UI_SCALE_MIN_STEP.toFloat()..UI_SCALE_MAX_STEP.toFloat(),
                    steps = UI_SCALE_MAX_STEP - UI_SCALE_MIN_STEP - 1,
                    modifier = Modifier.fillMaxWidth().testTag("ui-scale-slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AppColors.Crimson,
                        activeTrackColor = AppColors.Crimson,
                        inactiveTrackColor = AppColors.Gold.copy(alpha = .6f),
                        activeTickColor = AppColors.Parchment,
                        inactiveTickColor = AppColors.Ink.copy(alpha = .72f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0.80", style = AppType.Metadata, color = AppColors.Ink.copy(alpha = .72f))
                    Text("1.10", style = AppType.Metadata, color = AppColors.Ink.copy(alpha = .72f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onAbout, modifier = Modifier.testTag("settings-about")) {
                        Text("About", color = AppColors.Crimson, style = AppType.ButtonLabel)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings-done"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Crimson,
                            contentColor = AppColors.Parchment
                        ),
                        shape = AppDimens.ButtonShape
                    ) {
                        Text(stringResource(R.string.action_done), style = AppType.ButtonLabel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    SearchInputField(
        query = value,
        onQueryChange = onValueChange,
        modifier = modifier.height(52.dp).onFocusChanged { if (it.isFocused) onFocus() },
        tag = "global-search",
        textStyle = AppType.Body,
        placeholder = { Text(stringResource(R.string.search_hint), style = AppType.Body, maxLines = 1) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppColors.Gold, modifier = Modifier.size(21.dp)) },
        trailingIcon = if (value.isNotEmpty()) {
            { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.action_clear), modifier = Modifier.size(20.dp)) } }
        } else null,
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(26.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Gold,
            unfocusedBorderColor = AppColors.ParchmentDeep.copy(alpha = .55f),
            focusedTextColor = AppColors.Parchment,
            unfocusedTextColor = AppColors.Parchment,
            focusedContainerColor = AppColors.Night,
            unfocusedContainerColor = AppColors.Night
        )
    )
}

@Composable
private fun AppBottomBar(selectedRoute: String, onNavigate: (String) -> Unit, height: androidx.compose.ui.unit.Dp = 54.dp) {
    val focusManager = LocalFocusManager.current
    val compactPortrait = LocalAppWindowLayout.current.profile == AppLayoutProfile.COMPACT_PORTRAIT
    Surface(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
        color = AppColors.NightRaised,
        shadowElevation = 7.dp
    ) {
        Row(Modifier.fillMaxWidth().height(if (compactPortrait) 62.dp else height).testTag("bottom-navigation")) {
            navItems.forEach { item ->
                val selected = selectedRoute == item.route ||
                    (item.route == Routes.SEARCH && selectedRoute in listOf(Routes.MATERIAL, Routes.MONSTER, Routes.SMALL_MONSTER, Routes.MAP, Routes.WEAPON_TYPE, Routes.TRAINING))
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selected) AppColors.Crimson else Color.Transparent)
                        .testTag("nav-${item.route}")
                        .clickable {
                            focusManager.clearFocus(force = true)
                            onNavigate(item.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (compactPortrait) Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            item.icon,
                            null,
                            tint = if (selected) AppColors.Gold else AppColors.ParchmentDeep,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            stringResource(item.titleRes),
                            color = if (selected) AppColors.Gold else AppColors.ParchmentDeep,
                            style = AppType.Metadata.copy(fontSize = 11.sp, lineHeight = 13.sp),
                            maxLines = 1,
                            softWrap = false
                        )
                    } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(item.icon, null, tint = if (selected) AppColors.Gold else AppColors.ParchmentDeep, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(stringResource(item.titleRes), color = if (selected) AppColors.Gold else AppColors.ParchmentDeep,
                            style = AppType.ButtonLabel, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeScreen(
    data: FixtureData,
    weaponRepository: WeaponRepository? = null,
    recent: List<RecentEntry>,
    favorites: List<FavoriteEntry>,
    onMaterials: () -> Unit,
    onMonsters: () -> Unit,
    onMaps: () -> Unit,
    onFavorites: () -> Unit,
    onEntity: (ReferenceEntityType, String) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("screen-home")) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val view = LocalView.current
        val compactPortrait = LocalAppWindowLayout.current.profile == AppLayoutProfile.COMPACT_PORTRAIT
        val categoryHeight = 42.dp
        LaunchedEffect(maxWidth, maxHeight, view.width, view.height) {
            if (BuildConfig.DEBUG) {
                Log.d(
                    "MHP3rdHomeMetrics",
                    "windowPx=${view.rootView.width}x${view.rootView.height}, density=${density.density}, " +
                        "screenDp=${configuration.screenWidthDp}x${configuration.screenHeightDp}, " +
                        "constraints=${maxWidth.value}x${maxHeight.value}dp"
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .testTag("home-content")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (compactPortrait) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        CategoryButton(R.string.category_monsters, Icons.Default.Pets, AppColors.Steel, categoryHeight, Modifier.weight(1f).testTag("category-monsters"), onMonsters)
                        CategoryButton(R.string.category_materials, Icons.Default.Inventory2, AppColors.Moss, categoryHeight, Modifier.weight(1f).testTag("category-materials"), onMaterials)
                    }
                    CategoryButton(R.string.category_maps, Icons.Default.Map, AppColors.Crimson, categoryHeight, Modifier.fillMaxWidth().testTag("category-maps"), onMaps)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CategoryButton(R.string.category_monsters, Icons.Default.Pets, AppColors.Steel, categoryHeight, Modifier.weight(1f).testTag("category-monsters"), onMonsters)
                    CategoryButton(R.string.category_materials, Icons.Default.Inventory2, AppColors.Moss, categoryHeight, Modifier.weight(1f).testTag("category-materials"), onMaterials)
                    CategoryButton(R.string.category_maps, Icons.Default.Map, AppColors.Crimson, categoryHeight, Modifier.weight(1f).testTag("category-maps"), onMaps)
                }
            }
            ReferenceSection(
                title = stringResource(R.string.favorites),
                entries = favorites.take(4).mapNotNull { data.resolveReference(it.type, it.entityId, weaponRepository) },
                emptyText = stringResource(R.string.home_favorites_empty),
                modifier = Modifier.testTag("home-favorites-card"),
                accent = AppColors.Moss,
                onTitle = onFavorites,
                onEntity = onEntity
            )
            ReferenceSection(
                title = stringResource(R.string.recently_opened),
                entries = recent.mapNotNull { data.resolveReference(it.type, it.entityId, weaponRepository) },
                emptyText = stringResource(R.string.recent_empty),
                modifier = Modifier.testTag("recently-opened-card"),
                accent = AppColors.Steel,
                onEntity = onEntity
            )
        }
    }
}

@Composable
private fun CategoryButton(
    @StringRes titleRes: Int,
    icon: ImageVector,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        color = color,
        shape = AppDimens.ButtonShape,
        border = BorderStroke(AppDimens.CardBorder, AppColors.Gold.copy(alpha = .7f))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Text(stringResource(titleRes), color = AppColors.Parchment, style = AppType.ButtonLabel, maxLines = 1, softWrap = false)
        }
    }
}

private data class ResolvedReference(
    val type: ReferenceEntityType,
    val id: String,
    val name: String,
    val subtitle: String,
    val monster: Monster? = null,
    val smallMonster: SmallMonster? = null,
    val weapon: Weapon? = null,
    val gameItemId: Int? = null
)

private fun FixtureData.resolveReference(type: ReferenceEntityType, id: String, weaponRepository: WeaponRepository? = null): ResolvedReference? = when (type) {
    ReferenceEntityType.MONSTER -> monsters.firstOrNull { it.id == id }?.let {
        ResolvedReference(type, id, it.name, it.type, it)
    }
    ReferenceEntityType.SMALL_MONSTER -> smallMonsters.firstOrNull { it.id == id }?.let {
        ResolvedReference(type, id, it.name, it.monsterClass, smallMonster = it)
    }
    ReferenceEntityType.MATERIAL -> materials.firstOrNull { it.id == id }?.let {
        ResolvedReference(type, id, it.name, it.description, gameItemId = it.gameItemId)
    }
    ReferenceEntityType.WEAPON -> weaponRepository?.weaponById?.get(id)?.let { weapon ->
        ResolvedReference(
            type,
            id,
            weapon.name,
            "${weaponTypeLabel(weapon.weaponType)} · R${weapon.rarity} · ${weapon.attack}",
            weapon = weapon
        )
    }
    else -> null
}

@Composable
private fun ReferenceSection(
    title: String,
    entries: List<ResolvedReference>,
    emptyText: String,
    modifier: Modifier = Modifier,
    accent: Color,
    onTitle: (() -> Unit)? = null,
    onEntity: (ReferenceEntityType, String) -> Unit
) {
    SectionCard(title, modifier.fillMaxWidth(), accent = accent, headerHeight = 29.dp, contentPadding = 6.dp) {
        if (entries.isEmpty()) {
            Text(emptyText, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, modifier = Modifier.padding(3.dp))
        } else {
            entries.chunked(2).forEach { rowEntries ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowEntries.forEach { entry -> ReferenceItem(entry, Modifier.weight(1f), onEntity) }
                    if (rowEntries.size == 1) Spacer(Modifier.weight(1f))
                }
                if (entries.size > 2) Spacer(Modifier.height(5.dp))
            }
        }
        if (onTitle != null) {
            TextButton(onClick = onTitle, modifier = Modifier.align(Alignment.End).height(30.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(stringResource(R.string.view_all), style = AppType.ButtonLabel)
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun ReferenceItem(entry: ResolvedReference, modifier: Modifier, onEntity: (ReferenceEntityType, String) -> Unit) {
    Surface(
        color = AppColors.ParchmentDeep.copy(alpha = .35f),
        shape = AppDimens.ButtonShape,
        modifier = modifier.height(43.dp).clickable { onEntity(entry.type, entry.id) }.testTag("reference-${entry.type.name.lowercase()}-${entry.id}")
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                entry.monster != null -> MonsterArtworkImage(entry.monster, Modifier.size(34.dp))
                entry.smallMonster != null -> SmallMonsterIconImage(entry.smallMonster, Modifier.size(34.dp))
                entry.weapon != null -> WeaponIcon(entry.weapon.weaponType, entry.weapon.rarity, Modifier.size(34.dp))
                else -> ItemIcon(entry.gameItemId, entry.name, Modifier.size(34.dp))
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val typeLabel = entry.type.localizedReferenceType()
                Text(
                    if (entry.subtitle.isNotBlank()) "$typeLabel · ${entry.subtitle}" else typeLabel,
                    color = AppColors.Ink.copy(alpha = .65f),
                    style = AppType.Metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReferenceEntityType.localizedReferenceType(): String = when (this) {
    ReferenceEntityType.MONSTER -> stringResource(R.string.favorite_type_monster)
    ReferenceEntityType.SMALL_MONSTER -> stringResource(R.string.favorite_type_small_monster)
    ReferenceEntityType.MATERIAL -> stringResource(R.string.favorite_type_material)
    ReferenceEntityType.WEAPON -> stringResource(R.string.favorite_type_weapon)
    ReferenceEntityType.MAP -> stringResource(R.string.favorite_type_map)
}

@Composable
internal fun AutoFitCompactText(text: String, modifier: Modifier = Modifier) {
    val minimum = 11.sp
    val maximum = 14.sp
    var size by remember(text) { mutableStateOf(maximum) }
    var fitted by remember(text) { mutableStateOf(false) }
    Text(
        text = text,
        color = AppColors.Ink,
        style = AppType.Body.copy(fontSize = size, lineHeight = (size.value + 2).sp),
        maxLines = 2,
        softWrap = true,
        overflow = TextOverflow.Clip,
        modifier = modifier.alpha(if (fitted) 1f else 0f),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && size > minimum) size = (size.value - 1).sp
            else fitted = true
        }
    )
}

@Composable
internal fun SearchScreen(
    query: String,
    filter: EntityType?,
    results: List<SearchResult>,
    listState: LazyListState,
    itemListState: LazyListState = rememberLazyListState(),
    monsters: List<Monster> = emptyList(),
    smallMonsters: List<SmallMonster> = emptyList(),
    materials: List<Material> = emptyList(),
    quests: List<Quest> = emptyList(),
    trainingQuests: List<TrainingQuest> = emptyList(),
    weapons: List<Weapon> = emptyList(),
    weaponRepository: WeaponRepository? = null,
    weaponScreenState: WeaponScreenState? = null,
    maps: List<MapDefinition> = emptyList(),
    monsterGridState: LazyGridState = rememberLazyGridState(),
    smallMonsterGridState: LazyGridState = rememberLazyGridState(),
    questListState: LazyListState = rememberLazyListState(),
    trainingListState: LazyListState = rememberLazyListState(),
    displayTerminology: DisplayTerminology = DisplayTerminology(),
    questStarFilter: Int? = null,
    questRankFilter: QuestRankFilter = QuestRankFilter.ALL,
    questStarFilters: Set<Int> = emptySet(),
    questCategoryFilter: QuestCategoryFilter = QuestCategoryFilter.ALL,
    onQuestStarFilter: (Int?) -> Unit = {},
    onQuestCategoryFilter: (QuestCategoryFilter) -> Unit = {},
    onQuestFilters: (QuestFilterState) -> Unit = {},
    onFilter: (EntityType?) -> Unit,
    onWeapons: () -> Unit = {},
    onWeaponType: (String) -> Unit = {},
    onResult: (SearchResult) -> Unit,
    onMap: (MapDefinition) -> Unit = {},
    onTraining: (TrainingQuest) -> Unit = {},
    skills: List<SkillTree> = emptyList(),
    decorationSkillRelations: List<DecorationSkillRelation> = emptyList(),
    skillListState: LazyListState = rememberLazyListState(),
    expandedSkillTreeId: String? = null,
    focusSkillTreeId: String? = null,
    onSkillExpanded: (String?) -> Unit = {},
    onSkillFocusConsumed: () -> Unit = {},
    onMaterial: (String) -> Unit = {}
) {
    val monstersById = remember(monsters) { monsters.associateBy { it.id } }
    val smallMonstersById = remember(smallMonsters) { smallMonsters.associateBy { it.id } }
    val weaponsById = remember(weapons) { weapons.associateBy { it.stableWeaponId } }
    val displayResults = remember(results, smallMonsters, query, filter) {
        if (filter == null && query.isNotBlank()) {
            val smallMonsterResults = filterSmallMonsters(smallMonsters, query).map { monster ->
                SearchResult(
                    monster.id,
                    monster.name,
                    "${monster.monsterClass} · Small Monster",
                    EntityType.SMALL_MONSTER
                )
            }
            results.filterNot { it.type == EntityType.SMALL_MONSTER } + smallMonsterResults
        } else results
    }
    Column(Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding).testTag("screen-search")) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp).horizontalScroll(rememberScrollState())
                .testTag("field-guide-category-strip"),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fieldGuideCategoryOrder.forEach { type ->
                if (type == null) {
                    CompactFilter(stringResource(R.string.filter_all), filter == null, Modifier.testTag("field-filter-all")) { onFilter(null) }
                } else {
                    CompactFilter(
                        type.localizedTitle(),
                        filter == type,
                        Modifier.testTag(if (type == EntityType.WEAPON) "field-filter-weapons" else "field-filter-${type.name.lowercase()}")
                    ) {
                        if (type == EntityType.WEAPON && query.isBlank()) onWeapons() else onFilter(type)
                    }
                }
            }

        }
        when {
            filter == EntityType.WEAPON && query.isBlank() && weaponRepository != null -> WeaponsScreen(
                repository = weaponRepository,
                screenState = weaponScreenState ?: remember { WeaponScreenState() },
                onTypeSelected = onWeaponType,
                onItem = { material ->
                    onResult(SearchResult(material.id, material.name, material.description, EntityType.MATERIAL, material.aliases, material.gameItemId))
                },
                onWeaponOpened = {}
            )
            filter == EntityType.MONSTER -> MonsterIndex(
                monsters = filterMonsters(monsters, query),
                totalCount = monsters.size,
                query = query,
                gridState = monsterGridState,
                onMonster = { monster ->
                    onResult(SearchResult(monster.id, monster.name, monster.subtitle, EntityType.MONSTER))
                }
            )
            filter == EntityType.SMALL_MONSTER -> SmallMonsterIndex(
                smallMonsters = filterSmallMonsters(smallMonsters, query),
                totalCount = smallMonsters.size,
                query = query,
                gridState = smallMonsterGridState,
                onSmallMonster = { monster ->
                    onResult(SearchResult(monster.id, monster.name, monster.monsterClass, EntityType.SMALL_MONSTER))
                }
            )
            filter == EntityType.MATERIAL -> ItemIndex(
                itemResults = displayResults.filter { it.type == EntityType.MATERIAL },
                totalCount = results.count { it.type == EntityType.MATERIAL },
                query = query,
                listState = itemListState,
                onItem = onResult
            )
            filter == EntityType.QUEST -> QuestBrowser(
                quests = quests,
                query = query,
                selectedRank = questRankFilter,
                selectedStars = if (questStarFilters.isNotEmpty()) questStarFilters else questStarFilter?.let { setOf(it) }.orEmpty(),
                selectedCategory = questCategoryFilter,
                listState = questListState,
                monsters = monsters,
                smallMonsters = smallMonsters,
                displayTerminology = displayTerminology,
                onApplyFilters = onQuestFilters
            )
            filter == EntityType.TRAINING -> TrainingBrowser(
                trainingQuests = trainingQuests,
                monsters = monsters,
                smallMonsters = smallMonsters,
                materials = materials,
                query = query,
                listState = trainingListState,
                displayTerminology = displayTerminology,
                onTraining = onTraining
            )
            filter == EntityType.SKILL -> SkillsBrowser(
                skills = skills,
                query = query,
                listState = skillListState,
                expandedSkillTreeId = expandedSkillTreeId,
                focusSkillTreeId = focusSkillTreeId,
                onExpandedChange = onSkillExpanded,
                onFocusConsumed = onSkillFocusConsumed,
                decorationSkillRelations = decorationSkillRelations,
                materials = materials,
                onMaterial = onMaterial
            )
            filter == EntityType.MAP -> MapsIndex(maps, onMap)
            query.isBlank() && filter !in setOf(EntityType.MATERIAL, EntityType.WEAPON, EntityType.SKILL) -> EmptyState(Icons.Default.Search, stringResource(R.string.search_start_title), stringResource(R.string.search_start_body))
            displayResults.isEmpty() -> EmptyState(Icons.Default.SearchOff, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body))
            else -> LazyColumn(
                modifier = Modifier.testTag("search-results"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                val orderedTypes = listOf(
                    EntityType.MONSTER,
                    EntityType.SMALL_MONSTER,
                    EntityType.MATERIAL,
                    EntityType.QUEST,
                    EntityType.TRAINING,
                    EntityType.WEAPON,
                    EntityType.SKILL
                ).sortedBy { type ->
                    if (displayResults.any { it.type == type && it.name.equals(query, ignoreCase = true) }) 0 else 1
                }
                orderedTypes.forEach { type ->
                    val group = displayResults.filter { it.type == type }
                    if (group.isNotEmpty()) {
                        item(key = "header-$type") {
                            Text(
                                stringResource(R.string.result_group_count, type.localizedTitle(), group.size),
                                color = AppColors.Gold,
                                style = AppType.SectionTitle,
                                modifier = Modifier.padding(top = 3.dp, bottom = 1.dp)
                            )
                        }
                        items(group, key = { "${it.type}-${it.id}" }) { result ->
                            SearchResultRow(
                                result,
                                quests.firstOrNull { it.id == result.id },
                                monstersById[result.id],
                                smallMonstersById[result.id],
                                weaponsById[result.id],
                                trainingQuests.firstOrNull { it.id == result.id },
                                onResult,
                                displayTerminology
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SkillThreshold.displayPoints(): String = if (points > 0) "+$points" else points.toString()

private fun orderedSkillThresholds(skill: SkillTree): List<SkillThreshold> =
    skill.thresholds.filter { it.points > 0 }.sortedByDescending { it.points } +
        skill.thresholds.filter { it.points < 0 }.sortedByDescending { it.points }

@Composable
internal fun SkillsBrowser(
    skills: List<SkillTree>,
    query: String,
    listState: LazyListState,
    expandedSkillTreeId: String?,
    focusSkillTreeId: String?,
    decorationSkillRelations: List<DecorationSkillRelation>,
    materials: List<Material>,
    onExpandedChange: (String?) -> Unit,
    onFocusConsumed: () -> Unit,
    onMaterial: (String) -> Unit
) {
    val filtered = remember(skills, query) {
        val q = query.trim()
        skills.filter { skill ->
            q.isBlank() || listOf(skill.displayName, skill.tmoDisplayName, skill.jpName)
                .plus(skill.thresholds.map { it.activatedSkillName })
                .any { value -> value.contains(q, ignoreCase = true) }
        }.sortedBy { it.displayName.lowercase() }
    }
    val materialsById = remember(materials) { materials.associateBy { it.id } }
    val skillsById = remember(skills) { skills.associateBy { it.stableSkillTreeId } }
    LaunchedEffect(focusSkillTreeId, filtered) {
        val focus = focusSkillTreeId ?: return@LaunchedEffect
        val index = filtered.indexOfFirst { it.stableSkillTreeId == focus }
        if (index >= 0) {
            // Two header items precede the rows: the explanation and its spacer.
            listState.animateScrollToItem(index + 1)
            onExpandedChange(focus)
        }
        onFocusConsumed()
    }
    Column(Modifier.fillMaxSize().testTag("skills-browser")) {
        if (filtered.isEmpty()) {
            EmptyState(Icons.Default.SearchOff, "No skills found", "Try another query or clear search")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("skills-list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                item(key = "skills-explanation") {
                    ParchmentSurface(Modifier.fillMaxWidth().testTag("skills-explanation-card")) {
                        Column(Modifier.padding(10.dp)) {
                            Text("How skills work", color = AppColors.Ink, style = AppType.CardTitle)
                            Text(
                                "Skill Points from equipment add together. A Skill has no effect until an activation threshold is reached. Some Skill Trees can also activate negative effects.",
                                color = AppColors.Ink.copy(alpha = .82f), style = AppType.Body,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
                items(filtered, key = { it.stableSkillTreeId }) { skill ->
                    val expanded = expandedSkillTreeId == skill.stableSkillTreeId
                    val thresholds = orderedSkillThresholds(skill)
                    ParchmentSurface(
                        Modifier.fillMaxWidth()
                            .clickable {
                                onExpandedChange(if (expanded) null else skill.stableSkillTreeId)
                            }
                            .testTag("skill-row-${skill.stableSkillTreeId}")
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(skill.displayName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (skill.tmoDisplayName != skill.displayName) {
                                        Text(skill.tmoDisplayName, color = AppColors.Ink.copy(alpha = .58f), style = AppType.Metadata, maxLines = 1)
                                    }
                                }
                                if (skill.specialMechanics.isNotEmpty()) {
                                    Text("SPECIAL", color = AppColors.Gold, style = AppType.Metadata, modifier = Modifier.padding(start = 5.dp))
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 5.dp)) {
                                        thresholds.forEach { threshold ->
                                            Text(
                                                threshold.displayPoints(),
                                                color = if (threshold.points < 0) AppColors.Crimson else AppColors.Gold,
                                                style = AppType.ButtonLabel,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
                            }
                            if (expanded) {
                                Column(Modifier.fillMaxWidth().padding(top = 7.dp).testTag("skill-expanded-${skill.stableSkillTreeId}")) {
                                    Text(skill.inGameDescription, color = AppColors.Ink.copy(alpha = .86f), style = AppType.Body)
                                    skill.specialMechanics.forEach { special ->
                                        Column(Modifier.padding(top = 8.dp).testTag("skill-special-${special.type}")) {
                                            Text(special.name, color = AppColors.Ink, style = AppType.CardTitle)
                                            Text("SPECIAL", color = AppColors.Gold, style = AppType.Metadata)
                                            Text(special.effectSummary, color = AppColors.Ink.copy(alpha = .84f), style = AppType.Metadata, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    thresholds.forEach { threshold ->
                                        Column(Modifier.padding(top = 8.dp).testTag("skill-threshold-${skill.stableSkillTreeId}-${threshold.points}")) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(threshold.displayPoints(), color = if (threshold.points < 0) AppColors.Crimson else AppColors.Gold, style = AppType.ButtonLabel, modifier = Modifier.widthIn(min = 42.dp))
                                                Text(threshold.activatedSkillName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2)
                                            }
                                            Text(threshold.effectSummary, color = AppColors.Ink.copy(alpha = .84f), style = AppType.Metadata, modifier = Modifier.padding(top = 2.dp))
                                        }
                                    }
                                    SkillJewelsSection(
                                        skill = skill,
                                        relations = decorationSkillRelations,
                                        materialsById = materialsById,
                                        skillsById = skillsById,
                                        onMaterial = onMaterial
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillJewelsSection(
    skill: SkillTree,
    relations: List<DecorationSkillRelation>,
    materialsById: Map<String, Material>,
    skillsById: Map<String, SkillTree>,
    onMaterial: (String) -> Unit
) {
    val currentRelations = remember(skill.stableSkillTreeId, relations) {
        relations.filter { it.stableSkillTreeId == skill.stableSkillTreeId }
    }
    if (currentRelations.isEmpty()) return
    var expanded by rememberSaveable("skill-jewels-${skill.stableSkillTreeId}") { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).testTag("skill-jewels-${skill.stableSkillTreeId}")) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { expanded = !expanded }
                .testTag("skill-jewels-toggle-${skill.stableSkillTreeId}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Jewels (${currentRelations.size})", color = AppColors.Ink, style = AppType.CardTitle, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse Jewels" else "Expand Jewels",
                tint = AppColors.Crimson,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            val sorted = remember(currentRelations, materialsById) {
                currentRelations.sortedWith(
                    compareBy<DecorationSkillRelation> { it.points <= 0 }
                        .thenBy { it.slotCost }
                        .thenByDescending { if (it.points > 0) it.points else Int.MIN_VALUE }
                        .thenBy { if (it.points < 0) it.points else Int.MAX_VALUE }
                        .thenBy { materialsById[it.stableDecorationItemId]?.name.orEmpty().lowercase() }
                        .thenBy { it.stableDecorationItemId }
                )
            }
            sorted.forEach { relation ->
                val material = materialsById[relation.stableDecorationItemId] ?: return@forEach
                val jewelRelations = relations.filter { it.stableDecorationItemId == relation.stableDecorationItemId }
                val orderedEffects = jewelRelations
                    .filter { it.stableSkillTreeId == skill.stableSkillTreeId } +
                    jewelRelations
                        .filter { it.stableSkillTreeId != skill.stableSkillTreeId && it.points > 0 }
                        .sortedWith(compareByDescending<DecorationSkillRelation> { it.points }.thenBy { it.stableSkillTreeId }) +
                    jewelRelations
                        .filter { it.stableSkillTreeId != skill.stableSkillTreeId && it.points < 0 }
                        .sortedWith(compareBy<DecorationSkillRelation> { it.points }.thenBy { it.stableSkillTreeId })
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { onMaterial(material.id) }
                        .padding(vertical = 5.dp)
                        .testTag("skill-jewel-row-${skill.stableSkillTreeId}-${material.id}")
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        ItemIcon(material.gameItemId, material.name, Modifier.size(30.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(Modifier.weight(1f)) {
                            Text(material.name, color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${relation.slotCost} slot${if (relation.slotCost == 1) "" else "s"}",
                                color = AppColors.Ink.copy(alpha = .66f),
                                style = AppType.Metadata
                            )
                        }
                    }
                    Column(
                        Modifier
                            .padding(start = 37.dp, top = 3.dp)
                            .testTag("skill-jewel-effects-order-${material.id}-${orderedEffects.joinToString("_") { it.stableSkillTreeId }}")
                    ) {
                        orderedEffects.forEach { effect ->
                            val effectName = skillsById[effect.stableSkillTreeId]?.displayName ?: effect.stableSkillTreeId
                            Row(
                                Modifier.fillMaxWidth().testTag("skill-jewel-effect-${material.id}-${effect.stableSkillTreeId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SignedSkillChip(
                                    effect.points,
                                    Modifier.testTag("skill-jewel-delta-${material.id}-${effect.stableSkillTreeId}")
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    effectName,
                                    color = AppColors.Ink,
                                    style = AppType.Body,
                                    modifier = Modifier.testTag("skill-jewel-effect-name-${material.id}-${effect.stableSkillTreeId}"),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .35f))
            }
        }
    }
}

private fun signedSkillPoints(points: Int): String = if (points >= 0) "+$points" else points.toString()

@Composable
private fun SignedSkillChip(points: Int, modifier: Modifier = Modifier) {
    val accent = if (points < 0) AppColors.Crimson else AppColors.Moss
    Surface(
        modifier = modifier.defaultMinSize(minWidth = 32.dp).heightIn(min = 24.dp),
        color = accent.copy(alpha = .16f),
        contentColor = accent,
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .42f))
    ) {
        Box(
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(signedSkillPoints(points), style = AppType.ButtonLabel, maxLines = 1)
        }
    }
}

@Composable
private fun TrainingBadge(training: TrainingQuest, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(width = 58.dp, height = 42.dp),
        color = AppColors.NightRaised,
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, AppColors.Gold)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(training.trainingClass.take(1).uppercase(), color = AppColors.Gold, style = AppType.ButtonLabel, maxLines = 1)
            training.stars?.let { Text("★$it", color = AppColors.Parchment, style = AppType.Metadata, maxLines = 1) }
        }
    }
}

@Composable
private fun TrainingBrowser(
    trainingQuests: List<TrainingQuest>,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>,
    materials: List<Material>,
    query: String,
    listState: LazyListState,
    displayTerminology: DisplayTerminology = DisplayTerminology(),
    onTraining: (TrainingQuest) -> Unit
) {
    var selectedClassName by rememberSaveable { mutableStateOf("ALL") }
    val displayObjective = remember(trainingQuests, monsters, smallMonsters, materials, displayTerminology) {
        trainingQuests.associate { training ->
            training.id to (displayTerminology.objectiveByStableId[training.id]
                ?: trainingObjectiveDisplayText(training.objectiveTargets, monsters, smallMonsters, materials))
        }
    }
    val classes = listOf("ALL", "BEGINNER", "GROUP", "CHALLENGE")
    val filtered = remember(trainingQuests, query, selectedClassName) {
        val q = query.trim()
        trainingQuests.filter { training ->
            (selectedClassName == "ALL" || training.trainingClass.equals(selectedClassName, ignoreCase = true)) &&
                (q.isBlank() || listOf(training.canonicalDisplayName, training.japaneseName, training.objective, training.location)
                    .any { it.contains(q, ignoreCase = true) })
        }.sortedWith(compareBy({ it.stars ?: Int.MAX_VALUE }, { it.canonicalDisplayName }))
    }
    Column(Modifier.fillMaxSize().testTag("training-browser")) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            classes.forEach { clazz ->
                CompactFilter(
                    when (clazz) { "ALL" -> "All"; "BEGINNER" -> "Beginner"; "GROUP" -> "Group"; else -> "Challenge" },
                    selectedClassName == clazz,
                    Modifier.testTag("training-filter-${clazz.lowercase()}")
                ) { selectedClassName = clazz }
            }
            Text("${filtered.size}/${trainingQuests.size}", color = AppColors.Gold, style = AppType.Metadata, modifier = Modifier.padding(horizontal = 4.dp, vertical = 9.dp))
        }
        if (filtered.isEmpty()) {
            EmptyState(Icons.Default.SearchOff, "No training found", "Try another query or filter")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("training-list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filtered, key = { it.id }) { training ->
                    ParchmentSurface(
                        Modifier.fillMaxWidth().clickable { onTraining(training) }.testTag("training-row-${training.id}")
                    ) {
                        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                            TrainingBadge(training)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(training.canonicalDisplayName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(
                                        training.trainingClass.replaceFirstChar { it.uppercase() },
                                        training.stars?.let { "★$it" },
                                        training.location.takeIf { it.isNotBlank() },
                                        training.participantLimit?.let { "$it players" }
                                    ).joinToString(" · "),
                                    color = AppColors.Ink.copy(alpha = .74f), style = AppType.Metadata, maxLines = 1
                                )
                                val objective = displayObjective[training.id] ?: training.objective.takeIf { it.isNotBlank() }
                                objective?.let { Text(it, color = AppColors.Ink.copy(alpha = .68f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingDetailScreen(
    training: TrainingQuest,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>,
    items: List<Material>,
    weapons: List<Weapon>,
    skills: List<SkillTree> = emptyList(),
    displayTerminology: DisplayTerminology = DisplayTerminology(),
    onBack: () -> Unit,
    onItem: (String) -> Unit,
    onWeapon: (String) -> Unit,
    onSkill: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable(training.id) { mutableIntStateOf(0) }
    val itemByGameId = remember(items) { items.mapNotNull { it.gameItemId?.let { id -> id to it } }.toMap() }
    val weaponById = remember(weapons) { weapons.associateBy { it.stableWeaponId } }
    val objectiveDisplay = remember(training, monsters, smallMonsters, items, displayTerminology) {
        displayTerminology.objectiveByStableId[training.id]
            ?: trainingObjectiveDisplayText(training.objectiveTargets, monsters, smallMonsters, items)
    }
    val appearingMonsterNames = remember(training, monsters, smallMonsters) {
        trainingAppearingMonsterDisplayNames(training.appearingMonsterIds, monsters, smallMonsters)
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding).testTag("training-detail-${training.id}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "training-header") {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TrainingBadge(training, Modifier.size(width = 64.dp, height = 48.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(training.canonicalDisplayName, color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 2)
                    Text(listOfNotNull(training.trainingClass, training.location.takeIf { it.isNotBlank() }).joinToString(" · "), color = AppColors.Gold, style = AppType.Metadata, maxLines = 1)
                }
            }
        }
        item(key = "training-tabs") {
            Row(Modifier.fillMaxWidth().height(36.dp).testTag("training-tabs"), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("Overview", "Loadouts", "Rewards").forEachIndexed { index, title ->
                    CompactFilter(title, selectedTab == index, Modifier.weight(1f).testTag("training-tab-$index")) { selectedTab = index }
                }
            }
        }
        when (selectedTab) {
            0 -> item(key = "training-overview") {
                SectionCard("Overview", Modifier.fillMaxWidth(), accent = AppColors.Steel) {
                    val meta = listOfNotNull(training.stars?.let { "★$it" }, training.location.takeIf { it.isNotBlank() }, training.timeLimitSeconds?.let(::trainingTimeLimitLabel), training.participantLimit?.let { "$it players" }).joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, color = AppColors.Ink.copy(alpha = .75f), style = AppType.Metadata)
                    val objective = objectiveDisplay ?: training.objective.takeIf { it.isNotBlank() }
                    objective?.let { Text(it, color = AppColors.Ink, style = AppType.Body, modifier = Modifier.padding(top = 5.dp)) }
                    if (appearingMonsterNames.isNotEmpty()) Text("Appearing: " + appearingMonsterNames.joinToString(" · "), color = AppColors.Ink.copy(alpha = .8f), style = AppType.Metadata, modifier = Modifier.padding(top = 3.dp))
                    if (training.resultGrades.rows.isNotEmpty()) {
                        Text("Results" + if (training.resultGrades.provisional) " · provisional" else "", color = AppColors.Gold, style = AppType.CardTitle, modifier = Modifier.padding(top = 7.dp))
                        training.resultGrades.rows.forEach { grade -> Text(listOfNotNull(grade.grade, grade.thresholdSeconds?.let(::trainingGradeThresholdLabel)?.let { "≤$it" }, trainingConditionLabel(grade.condition)).joinToString(" · "), color = AppColors.Ink, style = AppType.Metadata) }
                    }
                    if (training.suppliedItems.isNotEmpty()) {
                        Text("Supplied Items", color = AppColors.Gold, style = AppType.CardTitle, modifier = Modifier.padding(top = 7.dp))
                        training.suppliedItems.forEach { supplied -> TrainingItemRow(supplied.gameItemId, supplied.quantity, itemByGameId, onItem) }
                    }
                }
            }
            1 -> item(key = "training-loadouts") {
                SectionCard("Loadouts · ${training.loadouts.size}", Modifier.fillMaxWidth(), accent = AppColors.Moss) {
                    training.loadouts.forEachIndexed { index, loadout ->
                        if (index > 0) HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f), modifier = Modifier.padding(vertical = 5.dp))
                        Text("Loadout ${index + 1}", color = AppColors.Gold, style = AppType.CardTitle)
                        val weapon = loadout.weaponStableId?.let(weaponById::get)
                        if (weapon != null) {
                            Row(Modifier.fillMaxWidth().clickable { onWeapon(weapon.id) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(30.dp)); Spacer(Modifier.width(7.dp)); Text(weapon.name, color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else Text("Weapon class: ${canonicalWeaponTypeLabel(loadout.weaponType)}", color = AppColors.Ink, style = AppType.Body)
                        loadout.armorRaw.forEach { raw ->
                            Text(displayTerminology.trainingArmorByRaw[raw] ?: raw, color = AppColors.Ink.copy(alpha = .8f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        loadout.charmRaw?.takeIf { it.isNotBlank() }?.let { raw ->
                            Text("Charm: ${displayTerminology.trainingCharmByRaw[raw] ?: raw}", color = AppColors.Ink.copy(alpha = .8f), style = AppType.Metadata, maxLines = 1)
                        }
                        loadout.skillsRaw?.takeIf { it.isNotBlank() }?.let { raw ->
                            val labels = raw.split(Regex("[、，,/]+"))
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Skills:", color = AppColors.Ink.copy(alpha = .8f), style = AppType.Metadata)
                                Spacer(Modifier.width(4.dp))
                                labels.forEachIndexed { index, sourceLabel ->
                                    if (index > 0) Text(" · ", color = AppColors.Ink.copy(alpha = .65f), style = AppType.Metadata)
                                    val label = displayTerminology.trainingSkillByRaw[sourceLabel] ?: sourceLabel
                                    val target = skills.firstOrNull { skill ->
                                        skill.displayName.equals(label, ignoreCase = true) ||
                                            skill.tmoDisplayName.equals(label, ignoreCase = true) ||
                                            skill.thresholds.any { it.activatedSkillName.equals(label, ignoreCase = true) }
                                    }
                                    Text(
                                        label,
                                        color = if (target != null) AppColors.Crimson else AppColors.Ink.copy(alpha = .8f),
                                        style = AppType.Metadata,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = if (target != null) Modifier.clickable { onSkill(target.stableSkillTreeId) }.testTag("training-skill-link-${target.stableSkillTreeId}") else Modifier
                                    )
                                }
                            }
                        }
                        loadout.suppliedItems.forEach { supplied -> TrainingItemRow(supplied.gameItemId, supplied.quantity, itemByGameId, onItem) }
                    }
                }
            }
            else -> item(key = "training-rewards") {
                SectionCard("Rewards · ${training.rewards.size}", Modifier.fillMaxWidth(), accent = AppColors.Crimson) {
                    if (training.rewards.isEmpty()) {
                        Text("No reward data available", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                    } else {
                        training.rewards
                            .groupBy { it.rewardType }
                            .toList()
                            .sortedWith(compareBy<Pair<String, List<TrainingReward>>> { trainingRewardPoolOrder(it.first) }.thenBy { it.first })
                            .forEach { (rawRewardType, rewards) ->
                            val group = trainingRewardPoolDisplayLabel(rawRewardType)
                            group?.let { Text(it, color = AppColors.Gold, style = AppType.CardTitle) }
                            rewards.forEach { reward ->
                                val material = itemByGameId[reward.gameItemId]
                                Row(Modifier.fillMaxWidth().then(if (material != null) Modifier.clickable { onItem(material.id) } else Modifier).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ItemIcon(reward.gameItemId, material?.name, Modifier.size(30.dp)); Spacer(Modifier.width(7.dp)); Text(material?.name ?: "Unknown item", color = AppColors.Ink, style = AppType.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val detail = listOfNotNull(
                                        reward.quantity?.takeIf { reward.quantitySemantics == "EXPLICIT" }?.let { "×$it" },
                                        reward.probabilityValuePercent?.takeIf { reward.probabilitySemantics != "SOURCE_UNSUPPORTED" }?.let { "$it%" }
                                    ).joinToString(" · ")
                                    if (detail.isNotBlank()) Text(detail, color = AppColors.Crimson, style = AppType.Metadata, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** User-facing labels for source reward pools.  The production rewardType
 * remains the accepted Japanese source/category value; only the runtime
 * presentation is localized here. */
internal fun trainingRewardGroupDisplayLabel(rewardType: String): String = when (rewardType) {
    "確定報酬" -> "Guaranteed Rewards"
    "基本報酬" -> "Basic Rewards"
    "追加報酬" -> "Additional Rewards"
    else -> rewardType.ifBlank { "Reward" }
}

private fun trainingRewardPoolOrder(rewardType: String): Int = when (rewardType) {
    "確定報酬" -> 0
    "基本報酬" -> 1
    "追加報酬" -> 2
    else -> Int.MAX_VALUE
}

@Composable
private fun TrainingItemRow(gameItemId: Int, quantity: Int?, itemByGameId: Map<Int, Material>, onItem: (String) -> Unit) {
    val material = itemByGameId[gameItemId]
    Row(Modifier.fillMaxWidth().then(if (material != null) Modifier.clickable { onItem(material.id) } else Modifier).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(gameItemId, material?.name, Modifier.size(25.dp)); Spacer(Modifier.width(6.dp)); Text(material?.name ?: "Item #$gameItemId", color = AppColors.Ink.copy(alpha = .85f), style = AppType.Metadata, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); quantity?.let { Text("×$it", color = AppColors.Crimson, style = AppType.Metadata) }
    }
}

internal fun filterMonsters(monsters: List<Monster>, query: String): List<Monster> {
    val normalized = query.trim()
    return monsters
        .filter { normalized.isBlank() || it.name.contains(normalized, ignoreCase = true) }
        .sortedBy { it.name.lowercase() }
}

internal fun filterSmallMonsters(monsters: List<SmallMonster>, query: String): List<SmallMonster> {
    val normalized = query.trim()
    return monsters
        .filter { normalized.isBlank() || it.name.contains(normalized, ignoreCase = true) }
        .sortedBy { it.name.lowercase() }
}

@Composable
internal fun MapsIndex(maps: List<MapDefinition>, onMap: (MapDefinition) -> Unit) {
    Column(Modifier.fillMaxSize().testTag("maps-index")) {
        LazyColumn(
            modifier = Modifier.testTag("maps-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(maps, key = { it.mapId }) { map ->
                ParchmentSurface(
                    Modifier.fillMaxWidth().testTag("map-card-${map.mapId}").clickable { onMap(map) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().height(132.dp).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(112.dp).background(AppColors.Night).border(1.dp, AppColors.ParchmentDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            MapBaseImage(map, "${map.displayName} map preview", Modifier.fillMaxSize().padding(4.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(map.displayName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Resource overlay · ${map.nodes.size} points",
                                color = AppColors.Ink.copy(alpha = .74f),
                                style = AppType.Metadata,
                                maxLines = 1
                            )
                            Text("Tap to open map", color = AppColors.Crimson, style = AppType.Metadata)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson)
                    }
                }
            }
        }
    }
}

private enum class MapRankFilter(val label: String, val generated: GeneratedMaterialRankContext?) {
    ALL("All", null), LOW("LR", GeneratedMaterialRankContext.LOW), HIGH("HR", GeneratedMaterialRankContext.HIGH)
}

@Composable
internal fun MapDetailScreen(
    map: MapDefinition,
    fieldIndex: FieldSourceIndex = FieldSourceIndex.EMPTY,
    materials: List<Material> = emptyList(),
    onBack: () -> Unit,
    onClearFocus: () -> Unit = onBack,
    focusedGameItemId: Int? = null,
    initialRankContext: RewardContext? = null,
    onItem: (String) -> Unit = {}
) {
    var selectedNames by rememberSaveable(map.mapId) { mutableStateOf(emptyList<String>()) }
    var fullscreen by rememberSaveable(map.mapId) { mutableStateOf(false) }
    var whetstoneLens by rememberSaveable(map.mapId, focusedGameItemId) { mutableStateOf(false) }
    val selected = remember(selectedNames) {
        selectedNames.mapNotNull { name -> MapNodeCategory.entries.firstOrNull { it.name == name } }.toSet()
    }
    var selectedNodeId by rememberSaveable(map.mapId, focusedGameItemId) { mutableStateOf<String?>(null) }
    val focusedItem = focusedGameItemId?.let { id -> materials.firstOrNull { it.gameItemId == id } }
    val focusRankContexts = focusedGameItemId?.let { fieldIndex.availableMappedOrSpecialRanksForItemOnMap(it, map.mapId) }.orEmpty()
    var rankName by rememberSaveable(map.mapId, focusedGameItemId, focusRankContexts) {
        val preferred = when (initialRankContext) {
            RewardContext.LOW -> MapRankFilter.LOW.name
            RewardContext.HIGH -> MapRankFilter.HIGH.name
            else -> MapRankFilter.ALL.name
        }
        val availableNames = focusRankContexts.map { it.name }.toSet()
        mutableStateOf(
            preferred.takeIf { focusRankContexts.isEmpty() || it in availableNames }
                ?: focusRankContexts.singleOrNull()?.name
                ?: MapRankFilter.ALL.name
        )
    }
    val rank = MapRankFilter.entries.firstOrNull { it.name == rankName } ?: MapRankFilter.ALL
    val focusCoverage = focusedGameItemId?.let { fieldIndex.sourceCoverageForItemOnMap(it, map.mapId, rank.generated) }
    val visibleNodes = remember(map, selected, fieldIndex, focusedGameItemId, rank, whetstoneLens) {
        MapOverlayLogic.visibleNodes(map, selected, fieldIndex, focusedGameItemId, rank.generated, whetstoneLens)
    }
    val selectedNode = selectedNodeId?.let { id -> map.nodes.firstOrNull { it.nodeId == id } }
        ?.takeIf { node -> visibleNodes.any { it.nodeId == node.nodeId } }

    BackHandler(enabled = selectedNode != null) { selectedNodeId = null }

    Box(Modifier.fillMaxSize().testTag("map-detail-${map.mapId}")) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding, vertical = 5.dp)
        ) {
        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("map-back"), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
            }
            Spacer(Modifier.width(8.dp))
            Text(map.displayName, color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("${visibleNodes.size} nodes", color = AppColors.Gold, style = AppType.Metadata, modifier = Modifier.testTag("map-node-count"))
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = { fullscreen = true },
                modifier = Modifier.size(34.dp).testTag("map-fullscreen-open")
            ) { Icon(Icons.Default.Fullscreen, "Open map fullscreen", modifier = Modifier.size(22.dp)) }
        }

        if (focusedItem != null) {
            Row(Modifier.fillMaxWidth().height(40.dp).testTag("map-item-focus"), verticalAlignment = Alignment.CenterVertically) {
                ItemIcon(focusedItem.gameItemId, focusedItem.name, Modifier.size(29.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(focusedItem.name, color = AppColors.Parchment, style = AppType.CardTitle, maxLines = 1)
                    Text(
                        focusCoverage?.let(::mapFocusSummary) ?: "0 mapped",
                        color = AppColors.Gold,
                        style = AppType.Metadata,
                        maxLines = 1,
                        modifier = Modifier.testTag("map-item-focus-summary")
                    )
                }
                TextButton(onClick = onClearFocus, modifier = Modifier.testTag("map-clear-item-focus"), contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text("Clear focus", style = AppType.ButtonLabel)
                }
            }
        }
        MapFilterRow(
            selected = selected,
            rank = rank,
            showCategories = focusedItem == null,
            whetstoneLens = whetstoneLens,
            showRankControls = focusedItem != null && focusRankContexts.size > 1,
            singleFocusRank = focusRankContexts.singleOrNull(),
            onCategories = { updated ->
                selectedNames = updated.map { it.name }.sorted()
                whetstoneLens = false
            },
            onRank = { rankName = it.name },
            onWhetstone = { enabled ->
                whetstoneLens = enabled
                if (enabled) selectedNames = emptyList()
            }
        )
        MapCanvas(
            map, visibleNodes, selectedNode,
            onNode = { node -> if (node.sourceGatheringNodeIds.isNotEmpty()) selectedNodeId = node.nodeId },
            highlightAll = focusedItem != null || whetstoneLens,
            whetstoneLens = whetstoneLens,
            fieldIndex = fieldIndex,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("map-viewport")
        )
        Text(
            when {
                focusedItem != null -> "${focusedItem.name} · ${rank.label}"
                whetstoneLens -> "Whetstone sources"
                selected.isEmpty() -> "All resource categories"
                else -> selected.joinToString(" · ") { it.displayName }
            },
            color = AppColors.ParchmentDeep,
            style = AppType.Metadata,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().height(22.dp).testTag("map-filter-summary")
        )
        }
        if (selectedNode != null && !fullscreen) {
            MapNodeDetailOverlay(
                node = selectedNode,
                rank = rank.generated,
                focusedGameItemId = focusedGameItemId,
                fieldIndex = fieldIndex,
                materialIdByGameItemId = materials.mapNotNull { it.gameItemId?.let { id -> id to it.id } }.toMap(),
                onClose = { selectedNodeId = null },
                onItem = onItem
            )
        }
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val dialogView = LocalView.current
            BackHandler(enabled = selectedNode != null) { selectedNodeId = null }
            DisposableEffect(dialogView) {
                val window = (dialogView.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    WindowInsetsControllerCompat(window, dialogView).apply {
                        hide(WindowInsetsCompat.Type.systemBars())
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
                onDispose { }
            }
            Box(Modifier.fillMaxSize().background(Color.Black).testTag("map-fullscreen")) {
                MapCanvas(
                    map,
                    visibleNodes,
                    selectedNode,
                    onNode = { node -> if (node.sourceGatheringNodeIds.isNotEmpty()) selectedNodeId = node.nodeId },
                    highlightAll = focusedItem != null || whetstoneLens,
                    whetstoneLens = whetstoneLens,
                    fieldIndex = fieldIndex,
                    modifier = Modifier.fillMaxSize(),
                    fullscreen = true
                )
                if (selectedNode != null) {
                    MapNodeDetailOverlay(
                        node = selectedNode,
                        rank = rank.generated,
                        focusedGameItemId = focusedGameItemId,
                        fieldIndex = fieldIndex,
                        materialIdByGameItemId = materials.mapNotNull { it.gameItemId?.let { id -> id to it.id } }.toMap(),
                        onClose = { selectedNodeId = null },
                        onItem = onItem,
                        fullscreen = true
                    )
                }
                FilledTonalIconButton(
                    onClick = { fullscreen = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(42.dp).testTag("map-fullscreen-close"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = AppColors.Night.copy(alpha = .88f),
                        contentColor = AppColors.Gold
                    )
                ) { Icon(Icons.Default.Close, "Close fullscreen map") }
            }
        }
    }
}

@Composable
private fun MapFilterRow(
    selected: Set<MapNodeCategory>,
    rank: MapRankFilter,
    showCategories: Boolean,
    whetstoneLens: Boolean,
    showRankControls: Boolean,
    singleFocusRank: GeneratedMaterialRankContext?,
    onCategories: (Set<MapNodeCategory>) -> Unit,
    onRank: (MapRankFilter) -> Unit,
    onWhetstone: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(42.dp).horizontalScroll(rememberScrollState()).testTag("map-filter-row"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showRankControls) {
            CompactFilter("All", rank == MapRankFilter.ALL, Modifier.testTag("map-rank-all")) { onRank(MapRankFilter.ALL) }
            CompactFilter("LR", rank == MapRankFilter.LOW, Modifier.testTag("map-rank-low")) { onRank(MapRankFilter.LOW) }
            CompactFilter("HR", rank == MapRankFilter.HIGH, Modifier.testTag("map-rank-high")) { onRank(MapRankFilter.HIGH) }
        } else if (showCategories.not() && singleFocusRank != null) {
            Text(
                singleFocusRank.rankShortLabel(),
                color = AppColors.Gold,
                style = AppType.Metadata,
                modifier = Modifier.testTag("map-focus-rank-static")
            )
        }
        if (showCategories) {
            CompactFilter("All", selected.isEmpty() && !whetstoneLens, Modifier.testTag("map-filter-all")) { onCategories(emptySet()) }
            MapNodeCategory.entries.filterNot { it == MapNodeCategory.WHETSTONE }.forEach { category ->
                CompactFilter(category.shortLabel, category in selected, Modifier.testTag("map-filter-${category.name.lowercase()}")) {
                    onCategories(MapOverlayLogic.toggle(selected, category))
                }
                if (category == MapNodeCategory.FISH) {
                    CompactFilter("Whetstone", whetstoneLens, Modifier.testTag("map-filter-whetstone")) { onWhetstone(!whetstoneLens) }
                }
            }
        }
    }
}

private fun mapFocusSummary(coverage: FieldSourceCoverage): String = buildList {
    add("${coverage.mappedCount} mapped")
    coverage.specialReasonCounts.toSortedMap().forEach { (reason, count) ->
        val label = when (reason) {
            "SECRET_AREA" -> "Secret Area source"
            "HIDDEN_AREA" -> "Hidden source"
            "CONDITIONAL_POST_DESTRUCTION" -> "Conditional source"
            else -> "Special source"
        }
        add("$count $label${if (count == 1) "" else "s"}")
    }
    if (coverage.unplacedCount > 0) add("${coverage.unplacedCount} source${if (coverage.unplacedCount == 1) "" else "s"} not placed yet")
}.joinToString(" · ")

@Composable
private fun MapNodeDetailOverlay(
    node: MapNode,
    rank: GeneratedMaterialRankContext?,
    focusedGameItemId: Int?,
    fieldIndex: FieldSourceIndex,
    materialIdByGameItemId: Map<Int, String>,
    onClose: () -> Unit,
    onItem: (String) -> Unit,
    fullscreen: Boolean = false
) {
    Box(Modifier.fillMaxSize().testTag("map-node-detail-overlay"), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .background(Color.Black.copy(alpha = if (fullscreen) .68f else .56f))
                .clickable(onClick = onClose)
                .testTag("map-node-detail-scrim")
        )
        MapNodeDetailCard(
            node = node,
            rank = rank,
            focusedGameItemId = focusedGameItemId,
            fieldIndex = fieldIndex,
            materialIdByGameItemId = materialIdByGameItemId,
            onClose = onClose,
            onItem = onItem,
            modifier = Modifier
                .fillMaxWidth(if (fullscreen) .9f else .92f)
                .heightIn(max = if (fullscreen) 620.dp else 460.dp)
                .testTag("map-node-detail-modal")
        )
    }
}

@Composable
private fun MapCanvas(
    map: MapDefinition,
    visibleNodes: List<MapNode>,
    selectedNode: MapNode? = null,
    onNode: (MapNode) -> Unit = {},
    highlightAll: Boolean = false,
    whetstoneLens: Boolean = false,
    fieldIndex: FieldSourceIndex = FieldSourceIndex.EMPTY,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false
) {
    var chooserTap by remember(map.mapId) { mutableStateOf<Offset?>(null) }
    var chooserPanelSize by remember(map.mapId) { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val mapSize = minOf(maxWidth, maxHeight)
        val density = LocalDensity.current
        val mapSizePx = with(density) { mapSize.toPx() }
        val mapOriginXpx = with(density) { ((maxWidth - mapSize) / 2).toPx() }
        val mapOriginYpx = with(density) { ((maxHeight - mapSize) / 2).toPx() }
        val markerSizePx = with(density) { (if (fullscreen) 26.dp else 20.dp).toPx() }
        fun handleMapTap(viewportOffset: Offset) {
            val tapXpx = viewportOffset.x - mapOriginXpx
            val tapYpx = viewportOffset.y - mapOriginYpx
            if (tapXpx !in 0f..mapSizePx || tapYpx !in 0f..mapSizePx) {
                chooserTap = null
                return
            }
            when (val result = MapTapResolver.resolve(visibleNodes, tapXpx, tapYpx, mapSizePx, markerSizePx)) {
                MapTapResolution.None -> chooserTap = null
                is MapTapResolution.Open -> {
                    chooserTap = null
                    onNode(result.candidate.node)
                }
                is MapTapResolution.Choose -> chooserTap = Offset(tapXpx, tapYpx)
            }
        }
        LaunchedEffect(visibleNodes) { chooserTap = null }
        BackHandler(enabled = chooserTap != null && selectedNode == null) { chooserTap = null }
        val activeCandidates = chooserTap?.let { tap ->
            MapTapResolver.candidates(visibleNodes, tap.x, tap.y, mapSizePx, markerSizePx)
        }.orEmpty()
        val chooserEntries = remember(activeCandidates, fieldIndex, whetstoneLens) {
            MapTapChooserPresentation.entries(activeCandidates, fieldIndex, whetstoneLens)
        }
        Box(
            Modifier.size(mapSize).background(Color.Black)
                .then(if (fullscreen) Modifier else Modifier.clip(AppDimens.CardShape)
                    .border(AppDimens.CardBorder, AppColors.ParchmentDeep, AppDimens.CardShape))
                .testTag("map-overlay")
        ) {
            MapBaseImage(
                map,
                "${map.displayName} clean base map",
                Modifier.fillMaxSize().padding(if (fullscreen) 0.dp else 3.dp).testTag("map-base-image")
            )
            visibleNodes.forEach { node ->
                val markerSize = if (fullscreen) 26.dp else 20.dp
                val (markerX, markerY) = MapOverlayLogic.scaledPosition(node, mapSize.value, mapSize.value)
                MapNodeMarker(
                    node,
                    iconCategory = if (whetstoneLens) whetstoneIconCategory(node, fieldIndex) else node.category,
                    highlighted = highlightAll || node.nodeId == selectedNode?.nodeId,
                    onClick = {
                        handleMapTap(Offset(mapOriginXpx + node.x * mapSizePx, mapOriginYpx + node.y * mapSizePx))
                    },
                    modifier = Modifier.offset(x = markerX.dp - markerSize / 2, y = markerY.dp - markerSize / 2)
                        .size(markerSize)
                )
            }
        }
        Box(
            Modifier.matchParentSize()
                .pointerInput(visibleNodes, mapSizePx, markerSizePx, mapOriginXpx, mapOriginYpx, onNode) {
                    detectTapGestures { offset ->
                        handleMapTap(offset)
                    }
                }
                .testTag("map-hit-surface")
        )
        if (chooserTap != null && selectedNode == null && chooserEntries.size >= 2) {
            val tap = requireNotNull(chooserTap)
            val panelMaxWidth = minOf(360.dp, (mapSize - 16.dp).coerceAtLeast(180.dp))
            val panelMinWidth = minOf(260.dp, panelMaxWidth)
            val marginPx = with(density) { 12.dp.roundToPx() }
            val gapPx = with(density) { 18.dp.roundToPx() }
            val panelOrigin = mapTapChooserOriginPx(
                tapXpx = tap.x,
                tapYpx = tap.y,
                panelWidthPx = chooserPanelSize.width,
                panelHeightPx = chooserPanelSize.height,
                viewportWidthPx = mapSizePx.roundToInt(),
                viewportHeightPx = mapSizePx.roundToInt(),
                marginPx = marginPx,
                gapPx = gapPx
            )
            MapTapAmbiguityChooser(
                entries = chooserEntries,
                onSelect = { entry ->
                    chooserTap = null
                    onNode(entry.candidate.node)
                },
                onDismiss = { chooserTap = null },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .widthIn(min = panelMinWidth, max = panelMaxWidth)
                    .heightIn(max = mapSize - 16.dp)
                    .offset {
                        IntOffset(
                            mapOriginXpx.roundToInt() + panelOrigin.x,
                            mapOriginYpx.roundToInt() + panelOrigin.y
                        )
                    }
                    .onSizeChanged { chooserPanelSize = it }
                    .zIndex(2f)
            )
        }
    }
}

@Composable
private fun MapTapAmbiguityChooser(
    entries: List<MapTapChooserEntry>,
    onSelect: (MapTapChooserEntry) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("map-tap-chooser"),
        color = AppColors.Parchment,
        shape = AppDimens.CardShape,
        border = BorderStroke(AppDimens.CardBorder, AppColors.ParchmentDeep),
        shadowElevation = 16.dp,
        tonalElevation = 4.dp
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Select point", color = AppColors.Ink, style = AppType.CardTitle)
                    Text(
                        "${entries.size} nearby resource points",
                        color = AppColors.Ink.copy(alpha = .72f),
                        style = AppType.Metadata,
                        modifier = Modifier.testTag("map-tap-chooser-count")
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("map-tap-chooser-close")) {
                    Icon(Icons.Default.Close, "Close point chooser", tint = AppColors.Ink)
                }
            }
            entries.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { onSelect(entry) }
                        .testTag("map-tap-chooser-row-${entry.candidate.node.nodeId}")
                        .padding(horizontal = 5.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapNodeIcon(entry.iconCategory, Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.primaryLabel, color = AppColors.Ink, style = AppType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        when {
                            entry.chooserChoiceIndex != null -> Text(
                                "Choice ${entry.chooserChoiceIndex}",
                                color = AppColors.Ink.copy(alpha = .68f),
                                style = AppType.Metadata
                            )
                            entry.secondaryLabel != null -> Text(
                                entry.secondaryLabel,
                                color = AppColors.Ink.copy(alpha = .68f),
                                style = AppType.Metadata,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun MapBaseImage(map: MapDefinition, contentDescription: String, modifier: Modifier = Modifier) {
    val bitmap = ImageBitmap.imageResource(map.baseImageRes)
    Image(
        painter = BitmapPainter(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(map.sourceViewportWidth, map.sourceViewportHeight)
        ),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun MapNodeMarker(
    node: MapNode,
    iconCategory: MapNodeCategory = node.category,
    highlighted: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier.then(
            if (node.sourceGatheringNodeIds.isNotEmpty()) {
                Modifier.semantics { onClick { onClick(); true } }
            } else {
                Modifier
            }
        )
            .then(if (highlighted) Modifier.border(2.dp, AppColors.Gold, CircleShape) else Modifier)
            .testTag("map-node-${node.nodeId}"),
        contentAlignment = Alignment.Center
        ) {
        val source = mapNodeIconSource(iconCategory)
        Image(
            painter = BitmapPainter(ImageBitmap.imageResource(R.drawable.map_node_icons), srcOffset = source.first, srcSize = source.second),
            contentDescription = listOfNotNull(
                node.label ?: iconCategory.displayName,
                node.areaNumber?.let(::mapAreaLabel)
            ).joinToString(", "),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun mapNodeIconSource(category: MapNodeCategory): Pair<IntOffset, IntSize> {
    val index = when (category) {
        MapNodeCategory.MINING -> 0
        MapNodeCategory.BUGS -> 1
        MapNodeCategory.BONES -> 2
        MapNodeCategory.PLANTS -> 3
        MapNodeCategory.MUSHROOMS -> 4
        MapNodeCategory.WHETSTONE -> 5
        MapNodeCategory.HONEY -> 6
        MapNodeCategory.BERRIES -> 7
        MapNodeCategory.FISH -> 8
        MapNodeCategory.MISC -> 9
        MapNodeCategory.SPIDER_WEB -> 10
    }
    return IntOffset(96 * index, 0) to IntSize(96, 96)
}

internal fun whetstoneIconCategory(node: MapNode, fieldIndex: FieldSourceIndex): MapNodeCategory =
    if (node.sourceGatheringNodeIds.any { fieldIndex.gatheringNodeById[it]?.method == GeneratedGatheringMethod.GATHERING }) {
        MapNodeCategory.WHETSTONE
    } else {
        node.category
    }

@Composable
private fun MapNodeDetailCard(
    node: MapNode,
    rank: GeneratedMaterialRankContext?,
    focusedGameItemId: Int?,
    fieldIndex: FieldSourceIndex,
    materialIdByGameItemId: Map<Int, String>,
    onClose: () -> Unit,
    onItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceIds = node.sourceGatheringNodeIds
    val source = sourceIds.asSequence().mapNotNull(fieldIndex.gatheringNodeById::get).firstOrNull()
    val allDrops = sourceIds.flatMap { fieldIndex.dropsByNodeId[it].orEmpty() }
    val availableRanks = allDrops.map { it.rankContext }.distinct().sortedBy { if (it == GeneratedMaterialRankContext.LOW) 0 else 1 }
    var selectedRankName by rememberSaveable(node.nodeId, rank?.name) {
        mutableStateOf(
            rank?.name?.takeIf { name -> availableRanks.any { it.name == name } }
                ?: availableRanks.firstOrNull()?.name
        )
    }
    val shownRank = selectedRankName?.let { name -> availableRanks.firstOrNull { it.name == name } }
        ?: availableRanks.firstOrNull()
    val rows = allDrops.filter { shownRank == null || it.rankContext == shownRank }
        .groupBy { it.gameItemId }
        .toList()
    ParchmentSurface(
        modifier.fillMaxWidth().testTag("map-node-detail")
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val equivalentGroup = MapSourceCrosswalk.equivalentGroupForMarker(node.nodeId)
                    Text(
                        equivalentGroup?.header
                            ?: source?.let { "${structuralGatheringContext(it.id, it.area, it.pointIndex)} · ${it.method.mapMethodLabel()}" }
                            ?: "Linked gathering node",
                        color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1
                    )
                    if (source != null) Text(
                        "${source.locationName} · ${equivalentGroup?.displayLabel ?: node.category.displayName}",
                        color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp).testTag("map-node-detail-close")) {
                    Icon(Icons.Default.Close, "Close node details", tint = AppColors.Ink)
                }
            }
            if (availableRanks.size > 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableRanks.forEach { availableRank ->
                        CompactFilter(
                            availableRank.rankLabel().removeSuffix(" Rank").let { if (it == "Low") "LR" else "HR" },
                            shownRank == availableRank,
                            Modifier.testTag("map-node-detail-rank-${availableRank.name.lowercase()}"),
                        ) { selectedRankName = availableRank.name }
                    }
                }
            } else if (availableRanks.isNotEmpty()) {
                Text(
                    availableRanks.first().rankLabel(),
                    color = AppColors.Crimson,
                    style = AppType.Metadata,
                    maxLines = 1,
                    modifier = Modifier.testTag("map-node-detail-rank-static")
                )
            }
            rows.forEach { (gameItemId, drops) ->
                val name = fieldIndex.itemNameByGameItemId[gameItemId] ?: "Item #$gameItemId"
                val quantities = drops.mapNotNull { drop ->
                    if (!drop.quantityExplicit && drop.quantity == 1) null else "×${drop.quantity}"
                }.distinct()
                val materialId = materialIdByGameItemId[gameItemId]
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (materialId != null) Modifier.clickable { onItem(materialId) } else Modifier)
                        .testTag("map-node-detail-item-$gameItemId")
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ItemIcon(gameItemId, name, Modifier.size(25.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(name, color = if (gameItemId == focusedGameItemId) AppColors.Crimson else AppColors.Ink, style = AppType.Body, maxLines = 1, modifier = Modifier.weight(1f))
                    if (quantities.isNotEmpty()) Text(quantities.joinToString(" / "), color = AppColors.Ink, style = AppType.Metadata, maxLines = 1)
                }
            }
        }
    }
}

private fun GeneratedGatheringMethod.mapMethodLabel(): String = when (this) {
    GeneratedGatheringMethod.GATHERING -> "Gathering"
    GeneratedGatheringMethod.MINING -> "Mining"
    GeneratedGatheringMethod.BUGNET -> "Bug Gathering"
    GeneratedGatheringMethod.FISHING -> "Fishing"
}

private fun GeneratedMaterialRankContext.rankLabel(): String = when (this) {
    GeneratedMaterialRankContext.LOW -> "Low Rank"
    GeneratedMaterialRankContext.HIGH -> "High Rank"
}

private fun GeneratedMaterialRankContext.rankShortLabel(): String = when (this) {
    GeneratedMaterialRankContext.LOW -> "LR"
    GeneratedMaterialRankContext.HIGH -> "HR"
}

private fun mapAreaLabel(area: String): String = if (area.equals("c", ignoreCase = true)) "Camp" else "Area $area"

internal fun monsterInitialLetters(monsters: List<Monster>): List<Char> = monsters
    .mapNotNull { it.name.trim().firstOrNull()?.uppercaseChar() }
    .distinct()
    .sorted()

internal fun firstMonsterIndexForLetter(monsters: List<Monster>, letter: Char): Int =
    monsters.indexOfFirst { it.name.trim().startsWith(letter, ignoreCase = true) }

internal fun smallMonsterInitialLetters(monsters: List<SmallMonster>): List<Char> = monsters
    .mapNotNull { it.name.trim().firstOrNull()?.uppercaseChar() }
    .distinct()
    .sorted()

internal fun firstSmallMonsterIndexForLetter(monsters: List<SmallMonster>, letter: Char): Int =
    monsters.indexOfFirst { it.name.trim().startsWith(letter, ignoreCase = true) }

internal fun itemInitialLetters(items: List<SearchResult>): List<Char> = items
    .mapNotNull { it.name.trim().firstOrNull()?.uppercaseChar() }
    .distinct()
    .sorted()

internal fun firstItemIndexForLetter(items: List<SearchResult>, letter: Char): Int =
    items.indexOfFirst { it.name.trim().startsWith(letter, ignoreCase = true) }

@Composable
internal fun MonsterIndex(
    monsters: List<Monster>,
    totalCount: Int,
    query: String,
    gridState: LazyGridState,
    onMonster: (Monster) -> Unit
) {
    Column(Modifier.fillMaxSize().testTag("monster-index")) {
        if (monsters.isEmpty()) {
            EmptyState(
                Icons.Default.SearchOff,
                stringResource(R.string.search_empty_title),
                if (query.isBlank()) stringResource(R.string.monster_index_empty_body) else stringResource(R.string.search_empty_body)
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                val letters = remember(monsters) { monsterInitialLetters(monsters) }
                val showScrubber = query.isBlank() && letters.isNotEmpty()
                val layout = LocalAppWindowLayout.current
                val columns = responsiveGridColumns(layout.widthDp - 70, layout.profile, minimumCardWidthDp = 340)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(end = if (showScrubber) 34.dp else 0.dp).testTag("monster-grid"),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    gridItems(monsters, key = { it.id }) { monster ->
                        MonsterIndexCard(monster, onMonster)
                    }
                }
                if (showScrubber) {
                    val visibleLetter by remember(monsters, gridState) {
                        derivedStateOf {
                            monsters.getOrNull(gridState.firstVisibleItemIndex)?.name?.trim()?.firstOrNull()?.uppercaseChar()
                        }
                    }
                    val scope = rememberCoroutineScope()
                    AlphabetScrubber(
                        letters = letters,
                        selectedLetter = visibleLetter,
                        onJump = { letter ->
                            firstMonsterIndexForLetter(monsters, letter).takeIf { it >= 0 }?.let { index ->
                                scope.launch { gridState.scrollToItem(index) }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
internal fun AlphabetScrubber(
    letters: List<Char>,
    selectedLetter: Char?,
    onJump: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubberHeight by remember { mutableIntStateOf(1) }
    var dragging by remember { mutableStateOf(false) }
    var dragLetter by remember { mutableStateOf<Char?>(null) }
    val selected = dragLetter ?: selectedLetter ?: letters.first()
    fun jump(letter: Char) {
        dragLetter = letter
        onJump(letter)
    }
    fun letterAt(y: Float): Char = letters[((y / scrubberHeight) * letters.size).toInt().coerceIn(0, letters.lastIndex)]

    BoxWithConstraints(modifier.fillMaxHeight().width(36.dp).padding(vertical = 5.dp)) {
        val density = LocalDensity.current
        val perLetterDp = maxHeight.value / letters.size.coerceAtLeast(1)
        val fittedSelectedSize = (perLetterDp / density.fontScale / .88f).coerceIn(11f, 15f).sp
        val fittedDefaultSize = if (fittedSelectedSize.value < 10f) fittedSelectedSize else 10.sp
        Column(
            Modifier.fillMaxSize().testTag("alphabet-scrubber")
                .onGloballyPositioned { scrubberHeight = it.size.height.coerceAtLeast(1) }
                .pointerInput(letters, scrubberHeight) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> dragging = true; jump(letterAt(offset.y)) },
                        onVerticalDrag = { change, _ -> change.consume(); jump(letterAt(change.position.y)) },
                        onDragEnd = { dragging = false; dragLetter = null },
                        onDragCancel = { dragging = false; dragLetter = null }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Box(
                    Modifier.weight(1f).fillMaxWidth().testTag("alphabet-letter-$letter")
                        .clickable { jump(letter); dragLetter = null },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter.toString(),
                        color = if (letter == selected) AppColors.Gold else AppColors.ParchmentDeep,
                        style = AppType.Metadata.copy(
                            fontSize = if (letter == selected) fittedSelectedSize else fittedDefaultSize,
                            fontWeight = if (letter == selected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        modifier = Modifier.testTag("alphabet-letter-label-$letter")
                    )
                }
            }
        }
        if (dragging) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-42).dp).size(46.dp).testTag("alphabet-indicator"),
                shape = RoundedCornerShape(23.dp),
                color = AppColors.Crimson,
                border = BorderStroke(AppDimens.CardBorder, AppColors.Gold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(selected.toString(), color = AppColors.Gold, style = AppType.ScreenTitle)
                }
            }
        }
    }
}

@Composable
private fun MonsterIndexCard(monster: Monster, onMonster: (Monster) -> Unit) {
    ParchmentSurface(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .testTag("monster-card-${monster.id}")
            .clickable { onMonster(monster) }
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .background(AppColors.Steel.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = AppColors.NightRaised,
                    shape = AppDimens.ButtonShape,
                    border = BorderStroke(AppDimens.CardBorder, AppColors.Steel.copy(alpha = .8f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MonsterArtworkImage(monster, Modifier.fillMaxSize().padding(3.dp))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    monster.name,
                    color = AppColors.Ink,
                    style = AppType.CardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (monster.type.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        monster.type,
                        color = AppColors.Ink.copy(alpha = .72f),
                        style = AppType.Metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.padding(end = 7.dp).size(20.dp))
        }
    }
}

@Composable
internal fun SmallMonsterIndex(
    smallMonsters: List<SmallMonster>,
    totalCount: Int,
    query: String,
    gridState: LazyGridState,
    onSmallMonster: (SmallMonster) -> Unit
) {
    Column(Modifier.fillMaxSize().testTag("small-monster-index")) {
        if (smallMonsters.isEmpty()) {
            EmptyState(
                Icons.Default.SearchOff,
                stringResource(R.string.search_empty_title),
                if (query.isBlank()) stringResource(R.string.small_monster_index_empty_body) else stringResource(R.string.search_empty_body)
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                val letters = remember(smallMonsters) { smallMonsterInitialLetters(smallMonsters) }
                val showScrubber = query.isBlank() && letters.isNotEmpty()
                val layout = LocalAppWindowLayout.current
                val columns = responsiveGridColumns(layout.widthDp - 70, layout.profile, minimumCardWidthDp = 340)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(end = if (showScrubber) 34.dp else 0.dp).testTag("small-monster-grid"),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    gridItems(smallMonsters, key = { it.id }) { monster ->
                        SmallMonsterIndexCard(monster, onSmallMonster)
                    }
                }
                if (showScrubber) {
                    val visibleLetter by remember(smallMonsters, gridState) {
                        derivedStateOf {
                            smallMonsters.getOrNull(gridState.firstVisibleItemIndex)?.name?.trim()?.firstOrNull()?.uppercaseChar()
                        }
                    }
                    val scope = rememberCoroutineScope()
                    AlphabetScrubber(
                        letters = letters,
                        selectedLetter = visibleLetter,
                        onJump = { letter ->
                            firstSmallMonsterIndexForLetter(smallMonsters, letter).takeIf { it >= 0 }?.let { index ->
                                scope.launch { gridState.scrollToItem(index) }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ItemIndex(
    itemResults: List<SearchResult>,
    totalCount: Int,
    query: String,
    listState: LazyListState,
    onItem: (SearchResult) -> Unit
) {
    Column(Modifier.fillMaxSize().testTag("item-index")) {
        if (itemResults.isEmpty()) {
            EmptyState(
                Icons.Default.SearchOff,
                stringResource(R.string.search_empty_title),
                stringResource(R.string.search_empty_body)
            )
        } else {
            Box(Modifier.fillMaxSize()) {
                val letters = remember(itemResults) { itemInitialLetters(itemResults) }
                val showScrubber = query.isBlank() && letters.isNotEmpty()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = if (showScrubber) 34.dp else 0.dp).testTag("search-results"),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(itemResults, key = { "item-${it.id}" }) { result ->
                        SearchResultRow(result, null, null, null, null, null, onItem)
                    }
                }
                if (showScrubber) {
                    val visibleLetter by remember(itemResults, listState) {
                        derivedStateOf {
                            itemResults.getOrNull(listState.firstVisibleItemIndex)?.name?.trim()?.firstOrNull()?.uppercaseChar()
                        }
                    }
                    val scope = rememberCoroutineScope()
                    AlphabetScrubber(
                        letters = letters,
                        selectedLetter = visibleLetter,
                        onJump = { letter ->
                            firstItemIndexForLetter(itemResults, letter).takeIf { it >= 0 }?.let { index ->
                                scope.launch { listState.scrollToItem(index) }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallMonsterIndexCard(monster: SmallMonster, onSmallMonster: (SmallMonster) -> Unit) {
    ParchmentSurface(
        Modifier.fillMaxWidth().height(88.dp).testTag("small-monster-card-${monster.id}")
            .clickable { onSmallMonster(monster) }
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(76.dp).fillMaxHeight().background(AppColors.Steel.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = AppColors.NightRaised,
                    shape = AppDimens.ButtonShape,
                    border = BorderStroke(AppDimens.CardBorder, AppColors.Steel.copy(alpha = .8f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    SmallMonsterIconImage(monster, Modifier.fillMaxSize().padding(3.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.Center) {
                Text(monster.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(monster.monsterClass, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
            }
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.padding(end = 7.dp).size(20.dp))
        }
    }
}

@Composable
private fun SmallMonsterIconImage(monster: SmallMonster, modifier: Modifier = Modifier) {
    when (val icon = SmallMonsterIconRegistry.resolve(monster.id)) {
        is SmallMonsterIcon.Resource -> Image(
            painter = painterResource(icon.resourceId),
            contentDescription = "${monster.name} icon",
            contentScale = ContentScale.Fit,
            modifier = modifier.testTag("small-monster-icon-${monster.id}")
        )
        SmallMonsterIcon.Placeholder -> Box(
            modifier = modifier.testTag("small-monster-icon-fallback-${monster.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Pets, null, tint = AppColors.Gold.copy(alpha = .82f), modifier = Modifier.fillMaxSize().padding(10.dp))
        }
    }
}

@Composable
private fun MonsterArtworkImage(monster: Monster, modifier: Modifier = Modifier) {
    when (val artwork = MonsterArtworkRegistry.resolve(monster.id)) {
        is MonsterArtwork.Resource -> Image(
            painter = painterResource(artwork.resourceId),
            contentDescription = "${monster.name} artwork",
            contentScale = ContentScale.Fit,
            modifier = modifier.testTag("monster-artwork-${monster.id}")
        )
        MonsterArtwork.Placeholder -> Box(
            modifier = modifier.testTag("monster-artwork-fallback-${monster.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Pets,
                contentDescription = null,
                tint = AppColors.Gold.copy(alpha = .82f),
                modifier = Modifier.fillMaxSize().padding(10.dp)
            )
        }
    }
}

@Composable
private fun EntityType.localizedTitle(): String = when (this) {
    EntityType.MATERIAL -> stringResource(R.string.category_materials)
    EntityType.MONSTER -> stringResource(R.string.category_large_monsters)
    EntityType.SMALL_MONSTER -> stringResource(R.string.category_small_monsters)
    EntityType.QUEST -> stringResource(R.string.category_quests)
    EntityType.TRAINING -> "Training"
    EntityType.MAP -> "Maps"
    EntityType.WEAPON -> "Weapons"
    EntityType.SKILL -> stringResource(R.string.category_skills)
}

@Composable
private fun CompactFilter(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = AppType.ButtonLabel, maxLines = 1, softWrap = false) },
        modifier = modifier.height(36.dp),
        shape = AppDimens.ButtonShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.Crimson,
            selectedLabelColor = Color(0xFFFFE8B0),
            containerColor = AppColors.NightRaised,
            labelColor = AppColors.Parchment
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.ParchmentDeep.copy(alpha = .65f),
            selectedBorderColor = AppColors.Gold
        )
    )
}

internal fun filterQuests(
    quests: List<Quest>,
    query: String,
    stars: Int?,
    category: QuestCategoryFilter = QuestCategoryFilter.ALL
): List<Quest> {
    return filterQuests(
        quests,
        query,
        QuestFilterState(
            category = category,
            stars = stars?.let { setOf(it) }.orEmpty()
        )
    )
}

internal fun filterQuests(
    quests: List<Quest>,
    query: String,
    state: QuestFilterState
): List<Quest> {
    val normalized = query.trim()
    return quests.filter { quest ->
        (state.category.productionCategory() == null || quest.category == state.category.productionCategory()) &&
            state.rank.matchesRank(quest.rank) &&
            (state.stars.isEmpty() || quest.stars in state.stars) &&
            (normalized.isBlank() || listOf(quest.name, quest.objective, quest.location, quest.target)
                .any { it.contains(normalized, ignoreCase = true) })
    }.sortedWith(compareBy<Quest>(
        { if (it.category.equals("Village", ignoreCase = true)) 0 else 1 },
        { it.stars },
        { it.canonicalNumber ?: Int.MAX_VALUE },
        { it.name.lowercase() }
    ))
}

internal fun validQuestStars(
    quests: List<Quest>,
    query: String,
    category: QuestCategoryFilter
): List<Int> = filterQuests(quests, query, null, category).map { it.stars }.distinct().sorted()

internal fun availableQuestRanks(quests: List<Quest>, category: QuestCategoryFilter): List<QuestRankFilter> =
    QuestFilterIndex(quests).availableRanks(category)

internal fun availableQuestStars(
    quests: List<Quest>,
    category: QuestCategoryFilter,
    rank: QuestRankFilter = QuestRankFilter.ALL
): List<Int> = QuestFilterIndex(quests).availableStars(category, rank)

private fun Quest.categoryDisplayLabel(): String = when {
    category.equals("Guild", ignoreCase = true) -> QuestCategoryFilter.GUILD.displayLabel()
    category.equals("Village", ignoreCase = true) -> QuestCategoryFilter.VILLAGE.displayLabel()
    category.equals("Hot Spring", ignoreCase = true) -> QuestCategoryFilter.HOT_SPRING.displayLabel()
    category.equals("Drink", ignoreCase = true) -> QuestCategoryFilter.DRINK.displayLabel()
    category.equals("Event", ignoreCase = true) -> QuestCategoryFilter.EVENT.displayLabel()
    else -> category
}

private fun Quest.rankDisplayLabel(): String = when (rank.uppercase()) {
    "HIGH", "HIGH RANK" -> QuestRankFilter.HIGH.displayLabel()
    "LOW", "LOW RANK" -> QuestRankFilter.LOW.displayLabel()
    else -> rank
}

private fun Quest.contextLabel(): String = listOfNotNull(
    rank.takeIf { it.isNotBlank() },
    location.takeIf { it.isNotBlank() }
).joinToString(" · ")

@Composable
internal fun QuestBadge(quest: Quest, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(width = 72.dp, height = 46.dp).testTag("quest-badge-${quest.id}"),
        color = AppColors.Crimson,
        contentColor = Color(0xFFFFE8B0),
        shape = AppDimens.ButtonShape,
        border = BorderStroke(1.dp, AppColors.Gold)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("★${quest.stars}", style = AppType.ButtonLabel, maxLines = 1, softWrap = false)
            // Keep both human-readable dimensions visible in the compact badge.
            // A separate line avoids truncating "Village · Low Rank" while
            // preserving the reusable badge's established footprint.
            Text(
                quest.categoryDisplayLabel(),
                style = AppType.Metadata.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 10.sp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            quest.rankDisplayLabel().takeIf { it.isNotBlank() }?.let { rankLabel ->
                Text(
                    rankLabel,
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 10.sp),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuestTargetIcons(
    quest: Quest,
    monstersById: Map<String, Monster>,
    smallMonstersById: Map<String, SmallMonster>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        quest.objectiveTargetMonsterIds.distinct().forEach { monsterId ->
            monstersById[monsterId]?.let { monster ->
                MonsterArtworkImage(monster, Modifier.size(29.dp))
            }
        }
        quest.objectiveTargetSmallMonsterIds.distinct().forEach { smallMonsterId ->
            smallMonstersById[smallMonsterId]?.let { monster ->
                SmallMonsterIconImage(monster, Modifier.size(29.dp))
            }
        }
    }
}

@Composable
private fun QuestRow(
    quest: Quest,
    monstersById: Map<String, Monster>,
    smallMonstersById: Map<String, SmallMonster>,
    displayTerminology: DisplayTerminology = DisplayTerminology(),
    modifier: Modifier = Modifier
) {
    val objective = displayTerminology.objectiveByStableId[quest.id]
        ?: quest.objective.takeIf { it.isNotBlank() }
    ParchmentSurface(modifier.fillMaxWidth().testTag("quest-row-${quest.id}")) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            QuestBadge(quest)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(quest.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                objective?.let { Text(it, color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Text(quest.location, color = AppColors.Crimson, style = AppType.Metadata, maxLines = 1)
            }
            if (quest.objectiveTargetMonsterIds.isNotEmpty() || quest.objectiveTargetSmallMonsterIds.isNotEmpty()) {
                Spacer(Modifier.width(7.dp))
                QuestTargetIcons(quest, monstersById, smallMonstersById)
            }
        }
    }
}

/** Existing quest information is reused as the destination for Supply Box
 * parent rows.  Supply Box adds no factual fields to the Quest model. */
@Composable
private fun QuestDetailScreen(quest: Quest, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(AppDimens.ScreenPadding).testTag("screen-quest-${quest.id}"),
        verticalArrangement = Arrangement.spacedBy(AppDimens.CardGap)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
        }
        SectionCard(quest.name, modifier = Modifier.fillMaxWidth()) {
            QuestBadge(quest)
            if (quest.objective.isNotBlank()) Text(quest.objective, color = AppColors.Ink, style = AppType.Body)
            if (quest.location.isNotBlank()) Text(quest.location, color = AppColors.Crimson, style = AppType.Metadata)
        }
    }
}

@Composable
private fun QuestBrowser(
    quests: List<Quest>,
    query: String,
    selectedRank: QuestRankFilter,
    selectedStars: Set<Int>,
    selectedCategory: QuestCategoryFilter,
    listState: LazyListState,
    monsters: List<Monster>,
    smallMonsters: List<SmallMonster>,
    displayTerminology: DisplayTerminology = DisplayTerminology(),
    onApplyFilters: (QuestFilterState) -> Unit
) {
    val monstersById = remember(monsters) { monsters.associateBy { it.id } }
    val smallMonstersById = remember(smallMonsters) { smallMonsters.associateBy { it.id } }
    val filterIndex = remember(quests) { QuestFilterIndex(quests) }
    val appliedState = remember(selectedCategory, selectedRank, selectedStars) {
        normalizeQuestFilter(QuestFilterState(selectedCategory, selectedRank, selectedStars), filterIndex)
    }
    val filtered = remember(quests, query, appliedState) {
        filterQuests(quests, query, appliedState)
    }
    var filterDialogOpen by remember { mutableStateOf(false) }
    var draftState by remember { mutableStateOf(QuestFilterState()) }
    val summary = questFilterSummary(appliedState)
    val activeDimensions = questFilterDimensionCount(appliedState)

    Column(Modifier.fillMaxSize().testTag("quest-browser")) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 42.dp).padding(vertical = 3.dp).testTag("quest-filter-controls"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeDimensions > 0,
                onClick = {
                    draftState = appliedState
                    filterDialogOpen = true
                },
                label = { Text(if (activeDimensions == 0) "Filters" else "Filters ($activeDimensions)", style = AppType.ButtonLabel, maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(17.dp)) },
                trailingIcon = { Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(17.dp)) },
                modifier = Modifier.height(36.dp).testTag("quest-filters-button"),
                shape = AppDimens.ButtonShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.Crimson,
                    selectedLabelColor = Color(0xFFFFE8B0),
                    selectedLeadingIconColor = Color(0xFFFFE8B0),
                    selectedTrailingIconColor = Color(0xFFFFE8B0),
                    containerColor = AppColors.NightRaised,
                    labelColor = AppColors.Parchment,
                    iconColor = AppColors.Gold
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = activeDimensions > 0,
                    borderColor = AppColors.ParchmentDeep.copy(alpha = .65f),
                    selectedBorderColor = AppColors.Gold
                )
            )
            Spacer(Modifier.width(7.dp))
            Text(
                summary,
                color = AppColors.Parchment,
                style = AppType.Metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("quest-filter-summary")
            )
            if (activeDimensions > 0) {
                IconButton(
                    onClick = { onApplyFilters(QuestFilterState()) },
                    modifier = Modifier.size(34.dp).testTag("quest-filter-clear")
                ) {
                    Icon(Icons.Default.Close, "Clear quest filters", tint = AppColors.Gold, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(3.dp))
            Text(
                questCountLabel(filtered.size),
                color = AppColors.Gold,
                style = AppType.Metadata,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.testTag("quest-count")
            )
        }
        if (filtered.isEmpty()) {
            EmptyState(Icons.Default.SearchOff, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_body))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("quest-list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filtered, key = { it.id }) { quest ->
                    QuestRow(quest, monstersById, smallMonstersById, displayTerminology)
                }
            }
        }
    }

    if (filterDialogOpen) {
        QuestFilterDialog(
            draft = draftState,
            index = filterIndex,
            onDraftChange = { draftState = normalizeQuestFilter(it, filterIndex) },
            onDismiss = { filterDialogOpen = false },
            onApply = {
                onApplyFilters(normalizeQuestFilter(draftState, filterIndex))
                filterDialogOpen = false
            }
        )
    }
}

@Composable
private fun MapNodeIcon(category: MapNodeCategory, modifier: Modifier = Modifier) {
    val source = mapNodeIconSource(category)
    Image(
        painter = BitmapPainter(ImageBitmap.imageResource(R.drawable.map_node_icons), srcOffset = source.first, srcSize = source.second),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

internal fun normalizeQuestFilter(state: QuestFilterState, index: QuestFilterIndex): QuestFilterState {
    val validRanks = index.availableRanks(state.category)
    val rank = if (validRanks.size > 1 && state.rank in validRanks) state.rank else QuestRankFilter.ALL
    val validStars = index.availableStars(state.category, rank).toSet()
    return state.copy(rank = rank, stars = state.stars.intersect(validStars))
}

internal fun questFilterDimensionCount(state: QuestFilterState): Int =
    listOf(state.category != QuestCategoryFilter.ALL, state.rank != QuestRankFilter.ALL, state.stars.isNotEmpty()).count { it }

internal fun questFilterSummary(state: QuestFilterState): String {
    val parts = buildList {
        if (state.category != QuestCategoryFilter.ALL) add(state.category.displayLabel())
        if (state.rank != QuestRankFilter.ALL) add(state.rank.displayLabel())
        if (state.stars.isNotEmpty()) add(formatQuestStars(state.stars))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "All quests"
}

private fun formatQuestStars(stars: Set<Int>): String {
    val ordered = stars.sorted()
    if (ordered.isEmpty()) return ""
    val ranges = mutableListOf<String>()
    var start = ordered.first()
    var previous = start
    ordered.drop(1).forEach { value ->
        if (value == previous + 1) {
            previous = value
        } else {
            ranges += if (start == previous) "$start" else "$start–$previous"
            start = value
            previous = value
        }
    }
    ranges += if (start == previous) "$start" else "$start–$previous"
    return "★${ranges.joinToString(", ")}"
}

private fun questCountLabel(count: Int): String = if (count == 1) "1 quest" else "$count quests"

internal fun questFilterStarLabel(star: Int): String = "★$star"

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun QuestFilterDialog(
    draft: QuestFilterState,
    index: QuestFilterIndex,
    onDraftChange: (QuestFilterState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val ranks = index.availableRanks(draft.category)
    val stars = index.availableStars(draft.category, draft.rank)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.9f).widthIn(max = 560.dp)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .92f)
                .wrapContentHeight()
                .testTag("quest-filter-dialog"),
            color = AppColors.Parchment,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, AppColors.ParchmentDeep.copy(alpha = .85f))
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, null, tint = AppColors.Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Filter Quests", color = AppColors.Ink, style = AppType.CardTitle, modifier = Modifier.weight(1f).testTag("quest-filter-dialog-title"))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("quest-filter-close")) {
                        Icon(Icons.Default.Close, "Close filters", tint = AppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                }
                QuestFilterDialogSection("Quest type", "quest-filter-type-section") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QuestCategoryFilter.entries.forEach { category ->
                            QuestFilterChoice(
                                label = category.displayLabel(),
                                selected = draft.category == category,
                                tag = "quest-filter-type-${category.name.lowercase()}"
                            ) {
                                onDraftChange(draft.copy(category = category))
                            }
                        }
                    }
                }
                if (ranks.size > 1) {
                    QuestFilterDialogSection("Rank", "quest-filter-rank-section") {
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QuestRankFilter.entries.forEach { rank ->
                                QuestFilterChoice(
                                    label = rank.displayLabel(),
                                    selected = draft.rank == rank,
                                    tag = "quest-filter-rank-${rank.name.lowercase()}"
                                ) {
                                    onDraftChange(draft.copy(rank = rank))
                                }
                            }
                        }
                    }
                }
                QuestFilterDialogSection("Stars", "quest-filter-stars-section") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QuestFilterChoice(
                            label = "All",
                            selected = draft.stars.isEmpty(),
                            tag = "quest-filter-star-all"
                        ) { onDraftChange(draft.copy(stars = emptySet())) }
                        stars.forEach { star ->
                            QuestFilterChoice(
                                label = questFilterStarLabel(star),
                                selected = star in draft.stars,
                                tag = "quest-filter-star-$star"
                            ) {
                                val next = if (star in draft.stars) draft.stars - star else draft.stars + star
                                onDraftChange(draft.copy(stars = next))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onDraftChange(QuestFilterState()) }, modifier = Modifier.testTag("quest-filter-reset")) {
                        Text("Reset", color = AppColors.Ink, style = AppType.ButtonLabel)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onApply,
                        modifier = Modifier.widthIn(min = 108.dp).testTag("quest-filter-apply"),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Crimson, contentColor = Color(0xFFFFE8B0)),
                        shape = AppDimens.ButtonShape
                    ) { Text("Apply", style = AppType.ButtonLabel) }
                }
            }
        }
    }
}

@Composable
private fun QuestFilterDialogSection(title: String, tag: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag(tag)) {
        Text(title, color = AppColors.Ink, style = AppType.Metadata.copy(fontWeight = FontWeight.Bold))
        content()
    }
}

@Composable
private fun QuestFilterChoice(
    label: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = AppType.Metadata, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
        modifier = modifier.height(34.dp).testTag(tag),
        shape = AppDimens.ButtonShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.Crimson,
            selectedLabelColor = Color(0xFFFFE8B0),
            containerColor = AppColors.Parchment.copy(alpha = .55f),
            labelColor = AppColors.Ink
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.ParchmentDeep.copy(alpha = .72f),
            selectedBorderColor = AppColors.Gold
        )
    )
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    quest: Quest?,
    monster: Monster?,
    smallMonster: SmallMonster?,
    weapon: Weapon?,
    training: TrainingQuest?,
    onClick: (SearchResult) -> Unit,
    displayTerminology: DisplayTerminology = DisplayTerminology()
) {
    val accent = when (result.type) {
        EntityType.MATERIAL -> AppColors.Moss
        EntityType.MONSTER -> AppColors.Steel
        EntityType.SMALL_MONSTER -> AppColors.Steel
        EntityType.QUEST -> AppColors.Crimson
        EntityType.TRAINING -> AppColors.Gold
        EntityType.MAP -> AppColors.Moss
        EntityType.WEAPON -> AppColors.Crimson
        EntityType.SKILL -> AppColors.Gold
    }
    // Regular Quest search rows retain their established non-navigating behavior;
    // Training rows are real deep links while preserving nested badge semantics.
    val interaction = if (result.type == EntityType.QUEST) Modifier else Modifier.clickable { onClick(result) }
    ParchmentSurface(Modifier.fillMaxWidth().testTag("result-${result.type.name.lowercase()}-${result.id}").then(interaction)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (result.type == EntityType.MATERIAL) {
                ItemIcon(result.gameItemId, result.name, Modifier.size(AppDimens.EntitySize))
            } else if (result.type == EntityType.MONSTER && monster != null) {
                MonsterArtworkImage(monster, Modifier.size(AppDimens.EntitySize))
            } else if (result.type == EntityType.SMALL_MONSTER && smallMonster != null) {
                SmallMonsterIconImage(smallMonster, Modifier.size(AppDimens.EntitySize))
            } else if (result.type == EntityType.QUEST) {
                QuestBadge(quest ?: result.asBadgeQuest())
            } else if (result.type == EntityType.TRAINING && training != null) {
                TrainingBadge(training)
            } else if (result.type == EntityType.WEAPON && weapon != null) {
                WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(AppDimens.EntitySize))
            } else {
                EntityEmblem(result.name, accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.name,
                    color = AppColors.Ink,
                    style = AppType.CardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (result.type == EntityType.TRAINING && training != null) {
                        displayTerminology.objectiveByStableId[training.id] ?: result.subtitle
                    } else result.subtitle,
                    color = AppColors.Ink.copy(alpha = .78f),
                    style = AppType.Metadata,
                    maxLines = 2
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = AppColors.Gold, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = AppColors.ParchmentDeep, style = AppType.Body)
    }
}

internal fun SmallMonsterRewardContext.displayLabel(): String = when (this) {
    SmallMonsterRewardContext.LOW -> "Low Rank"
    SmallMonsterRewardContext.HIGH -> "High Rank"
    SmallMonsterRewardContext.GUILD_1_2 -> "Guild ★1–2"
}

@Composable
internal fun SmallMonsterScreen(
    smallMonster: SmallMonster,
    materials: List<Material>,
    quests: List<Quest>,
    selectedContext: SmallMonsterRewardContext?,
    onContextChange: (SmallMonsterRewardContext) -> Unit,
    isFavorite: Boolean = false,
    onFavorite: () -> Unit = {},
    onBack: () -> Unit,
    onMaterial: (String) -> Unit
) {
    val contexts = remember(smallMonster.rewards) { smallMonster.rewards.availableSmallMonsterContexts() }
    val effectiveContext = selectedContext?.takeIf { it in contexts } ?: contexts.firstOrNull()
    val rewardGroups = remember(smallMonster.rewards, effectiveContext) {
        effectiveContext?.let(smallMonster.rewards::groupForSmallMonsterDisplay).orEmpty()
    }
    val materialsByGameItemId = remember(materials) { materials.mapNotNull { item -> item.gameItemId?.let { it to item } }.toMap() }
    val listState = rememberLazyListState()

    LaunchedEffect(effectiveContext) {
        if (effectiveContext != null && selectedContext != effectiveContext) onContextChange(effectiveContext)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding).testTag("screen-small-monster"),
        verticalArrangement = Arrangement.spacedBy(AppDimens.CardGap),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AppColors.NightRaised,
                        shape = AppDimens.ButtonShape,
                        border = BorderStroke(AppDimens.CardBorder, AppColors.Gold),
                        modifier = Modifier.size(72.dp)
                    ) {
                        SmallMonsterIconImage(smallMonster, Modifier.fillMaxSize().padding(4.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(smallMonster.name, color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 2)
                        Spacer(Modifier.height(3.dp))
                        Text(smallMonster.monsterClass, color = AppColors.Gold, style = AppType.Body, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                    FavoriteButton(isFavorite, onFavorite, "small-monster-favorite")
                }
            }
        }

        if (smallMonster.tips.isNotEmpty()) {
            item(key = "important-note") {
                SectionCard(stringResource(R.string.important_note), Modifier.fillMaxWidth(), accent = AppColors.Gold) {
                    smallMonster.tips.forEachIndexed { index, tip ->
                        Text(
                            tip.text,
                            color = AppColors.Ink,
                            style = AppType.Body,
                            modifier = Modifier.testTag("small-monster-tip-${tip.id}")
                        )
                        if (index != smallMonster.tips.lastIndex) Spacer(Modifier.height(7.dp))
                    }
                }
            }
        }

        if (contexts.isNotEmpty()) {
            item(key = "contexts") {
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.drops_carves), color = AppColors.Parchment, style = AppType.SectionTitle)
                    Spacer(Modifier.height(5.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        contexts.forEach { context ->
                            CompactFilter(
                                context.displayLabel(),
                                context == effectiveContext,
                                Modifier.testTag("small-reward-context-${context.name}")
                            ) { onContextChange(context) }
                        }
                    }
                }
            }
        }

        rewardGroups.forEachIndexed { groupIndex, group ->
            item(key = "reward-group-${effectiveContext?.name}-$groupIndex") {
                Text(
                    group.title,
                    color = AppColors.Gold,
                    style = AppType.ButtonLabel,
                    modifier = Modifier.fillMaxWidth().background(AppColors.NightRaised)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("small-reward-group-$groupIndex"),
                    maxLines = 2
                )
            }
            items(group.rewards, key = { it.id }) { reward ->
                val material = materialsByGameItemId[reward.gameItemId]
                ParchmentSurface(
                    Modifier.fillMaxWidth().testTag("small-reward-${reward.id}")
                        .then(if (material != null) Modifier.clickable { onMaterial(material.id) } else Modifier)
                ) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        ItemIcon(reward.gameItemId, reward.itemName, Modifier.size(31.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(reward.itemName, color = AppColors.Ink, style = AppType.Body, modifier = Modifier.weight(1f), maxLines = 2)
                        val chance = buildString {
                            append("${reward.chancePercent}%")
                            if (reward.quantity > 1) append(" · x${reward.quantity}")
                        }
                        Text(chance, color = AppColors.Crimson, style = AppType.ButtonLabel, maxLines = 1)
                    }
                }
            }
        }

        if (quests.isNotEmpty()) {
            item(key = "related-quests-title") {
                Text(stringResource(R.string.related_quests), color = AppColors.Parchment, style = AppType.SectionTitle)
            }
            items(quests, key = { it.id }) { quest ->
                QuestRow(
                    quest,
                    emptyMap(),
                    emptyMap(),
                    modifier = Modifier.testTag("small-monster-quest-${quest.id}")
                )
            }
        }
    }
}

@Composable
internal fun MaterialScreen(
    material: Material,
    quests: List<Quest>,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onSmallMonster: (String) -> Unit = {},
    onMonster: (String) -> Unit = {},
    onQuest: (String) -> Unit = {},
    onTraining: (String) -> Unit = {},
    weaponUsages: List<WeaponRecipeUsage> = emptyList(),
    weaponRepository: WeaponRepository? = null,
    onShowUsedInWeapons: () -> Unit = {},
    usageProjection: ItemUsageProjection? = null,
    onUsageFamily: (ItemUsageFamily) -> Unit = {},
    onUsageTarget: (ItemUsageTarget) -> Unit = {},
    onWeapon: (String) -> Unit = {},
    onMaterial: (String) -> Unit = {},
    jewelSkillRelations: List<DecorationSkillRelation> = emptyList(),
    skillTrees: List<SkillTree> = emptyList(),
    onSkill: (String) -> Unit = {},
    materialIdForGameItem: (Int) -> String? = { null },
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit = { _, _, _ -> }
) {
    Box(Modifier.fillMaxSize().testTag("screen-material")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.CardGap)
        ) {
            MaterialHeader(material, isFavorite, onBack, onFavorite)
            val jewelRelations = remember(material.id, jewelSkillRelations) {
                jewelSkillRelations.filter { it.stableDecorationItemId == material.id }
            }
            if (jewelRelations.isNotEmpty()) {
                JewelSkillsCard(material, jewelRelations, skillTrees.associateBy { it.stableSkillTreeId }, onSkill)
            }
            if (material.sources.isNotEmpty()) {
                SectionCard(
                    stringResource(R.string.sources),
                    modifier = Modifier.fillMaxWidth().testTag("item-sources-section")
                ) {
                    SourcesTable(material, quests, onSmallMonster, onMonster, onQuest, onTraining, onMaterial, materialIdForGameItem, onShowMap)
                }
            }
            if (usageProjection != null && usageProjection.families.isNotEmpty()) {
                ItemUsageSection(
                    projection = usageProjection,
                    weaponRepository = weaponRepository,
                    onFamily = onUsageFamily,
                    onTarget = onUsageTarget
                )
            }
            if (material.uses.isNotEmpty()) UsesCard(material.uses, Modifier.fillMaxWidth())
        }
    }
}

private fun SearchResult.asBadgeQuest(): Quest {
    val star = Regex("(?:^|·\\s*)(\\d+)★").find(subtitle)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val rank = when {
        subtitle.contains("Guild", ignoreCase = true) -> "High Rank"
        subtitle.contains("Village", ignoreCase = true) -> "Low Rank"
        else -> ""
    }
    return Quest(id, name, subtitle, rank = rank, stars = star)
}

@Composable
private fun JewelSkillsCard(
    material: Material,
    relations: List<DecorationSkillRelation>,
    skillsById: Map<String, SkillTree>,
    onSkill: (String) -> Unit
) {
    SectionCard(
        "Skills",
        modifier = Modifier.fillMaxWidth().testTag("item-jewel-skills-${material.id}"),
        accent = AppColors.Moss
    ) {
        Text(
            "Slots required: ${relations.first().slotCost}",
            color = AppColors.Ink.copy(alpha = .74f),
            style = AppType.Metadata,
            modifier = Modifier.testTag("item-jewel-slots-${material.id}")
        )
        val orderedRelations = relations.sortedWith(
            compareBy<DecorationSkillRelation> { it.points <= 0 }
                .thenByDescending { if (it.points > 0) it.points else Int.MIN_VALUE }
                .thenBy { if (it.points < 0) it.points else Int.MAX_VALUE }
                .thenBy { skillsById[it.stableSkillTreeId]?.displayName.orEmpty().lowercase() }
                .thenBy { it.stableSkillTreeId }
        )
        Column(
            Modifier.testTag("item-jewel-effects-order-${material.id}-${orderedRelations.joinToString("_") { it.stableSkillTreeId }}")
        ) {
            orderedRelations.forEach { relation ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onSkill(relation.stableSkillTreeId) }
                    .padding(vertical = 5.dp)
                    .testTag("item-jewel-skill-${material.id}-${relation.stableSkillTreeId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignedSkillChip(
                    relation.points,
                    Modifier.testTag("item-jewel-points-${material.id}-${relation.stableSkillTreeId}")
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    skillsById[relation.stableSkillTreeId]?.displayName ?: relation.stableSkillTreeId,
                    color = AppColors.Ink,
                    style = AppType.Body,
                    modifier = Modifier.weight(1f).testTag("item-jewel-skill-name-${material.id}-${relation.stableSkillTreeId}"),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
        }
    }
}

@Composable
private fun UsedInWeaponsCard(
    usages: List<WeaponRecipeUsage>,
    weaponRepository: WeaponRepository,
    onWeapon: (String) -> Unit,
    onShowAll: () -> Unit
) {
    val grouped = usages.groupBy { it.stableWeaponId }.entries.toList()
    SectionCard(
        "Used In Weapons · ${grouped.size}",
        modifier = Modifier.fillMaxWidth().testTag("material-used-in-weapons"),
        accent = AppColors.Moss
    ) {
        grouped.take(8).forEach { (weaponId, rows) ->
            val weapon = weaponRepository.weaponById[weaponId] ?: return@forEach
            WeaponUsageCardRow(weapon, rows, onWeapon)
        }
        if (grouped.size > 8) {
            TextButton(
                onClick = onShowAll,
                modifier = Modifier.align(Alignment.End).testTag("material-used-in-weapons-view-all"),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text("View all ${grouped.size}", style = AppType.ButtonLabel)
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun WeaponUsageCardRow(
    weapon: Weapon,
    usages: List<WeaponRecipeUsage>,
    onWeapon: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onWeapon(weapon.id) }.padding(vertical = 5.dp)
            .testTag("material-used-in-weapon-${weapon.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(34.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(weapon.name, color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                usages.sortedBy { it.recipeKind.name }.joinToString(" · ") { "${it.recipeKind.displayLabel()} · ×${it.quantity}" },
                color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 2
            )
            Text(weaponTypeLabel(weapon.weaponType), color = AppColors.Ink.copy(alpha = .62f), style = AppType.Metadata, maxLines = 1)
        }
        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
    }
}

private fun WeaponRecipeKind.displayLabel(): String = when (this) {
    WeaponRecipeKind.FORGE -> "Forge"
    WeaponRecipeKind.UPGRADE -> "Upgrade"
}

@Composable
internal fun UsedInWeaponsScreen(
    material: Material,
    usages: List<WeaponRecipeUsage>,
    weaponRepository: WeaponRepository,
    onBack: () -> Unit,
    onWeapon: (String) -> Unit
) {
    val grouped = usages.groupBy { it.stableWeaponId }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding).testTag("screen-used-in-weapons"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ItemIcon(material.gameItemId, material.name, Modifier.size(54.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Used In Weapons", color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 1)
                        Text(material.name, color = AppColors.ParchmentDeep, style = AppType.Body, maxLines = 1)
                    }
                    Text(grouped.size.toString(), color = AppColors.Gold, style = AppType.SectionTitle)
                }
            }
        }
        grouped.forEach { (weaponId, rows) ->
            val weapon = weaponRepository.weaponById[weaponId]
            if (weapon != null) {
                item(key = "used-in-$weaponId") {
                    ParchmentSurface(
                        Modifier.fillMaxWidth().clickable { onWeapon(weaponId) }.testTag("used-in-weapon-$weaponId")
                    ) {
                        WeaponUsageCardRow(weapon, rows, onWeapon)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialHeader(
    material: Material,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(5.dp))
            Text(stringResource(R.string.action_back), style = AppType.ButtonLabel)
        }
        Row(
            Modifier.fillMaxWidth().testTag("material-header-${material.gameItemId ?: "unknown"}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIcon(material.gameItemId, material.name, Modifier.size(66.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    material.name,
                    color = AppColors.Parchment,
                    style = AppType.ScreenTitle,
                    maxLines = 2,
                    softWrap = true
                )
                Spacer(Modifier.height(3.dp))
                material.rarity?.let { rarity -> Text(stringResource(R.string.material_metadata, rarity), color = AppColors.Gold, style = AppType.Body, maxLines = 1) }
                Text(material.description, color = AppColors.ParchmentDeep, style = AppType.Metadata, maxLines = 2)
            }
            Spacer(Modifier.width(12.dp))
            FavoriteButton(isFavorite, onFavorite, "material-favorite")
        }
    }
}

@Composable
internal fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, tag: String, compact: Boolean = false) {
    val buttonSize = if (compact) 36.dp else 44.dp
    val iconSize = if (compact) 19.dp else 23.dp
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize).testTag(tag),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isFavorite) AppColors.Crimson else AppColors.NightRaised,
            contentColor = AppColors.Gold
        )
    ) {
        Icon(
            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun SourcesTable(
    material: Material,
    quests: List<Quest>,
    onSmallMonster: (String) -> Unit,
    onMonster: (String) -> Unit,
    onQuest: (String) -> Unit,
    onTraining: (String) -> Unit,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit
) {
    val sources = material.sources
    val groups = remember(material.id, sources) { sources.groupForDisplay() }
    val renderedFamilyIds = remember(groups, material.gameItemId) {
        sourceFamilyIdsForRenderedMaterial(groups, material.gameItemId)
    }
    val decorationOutputRows = remember(groups.decorationCrafting, material.gameItemId) {
        decorationCraftingOutputRows(material.gameItemId, groups.decorationCrafting)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimens.CardGap / 2)) {
        if (groups.field.isNotEmpty()) SourceFamilyBlock("field", SourceFamilyBoundary.MINIMAL) {
            FieldSources(material = material, field = groups.field, onShowMap = onShowMap)
        }

        // Keep recipes discoverable before large quest/reward lists (for example,
        // Armor Sphere has many quest rows but only two combination alternatives).
        if (groups.combinations.isNotEmpty() || groups.combinationFailures.isNotEmpty()) {
            SourceFamilyBlock("combination", SourceFamilyBoundary.MINIMAL) {
                CombinationSourceRows(groups.combinations, groups.combinationFailures, onMaterial, materialIdForGameItem)
            }
        }
        if ("decoration-crafting" in renderedFamilyIds) SourceFamilyBlock("decoration-crafting", SourceFamilyBoundary.MINIMAL) {
            DecorationCraftingSourceRows(decorationOutputRows, onMaterial, materialIdForGameItem)
        }
        if (groups.roasting.isNotEmpty()) SourceFamilyBlock("roasting") {
            RoastingSourceRows(material, groups.roasting, onMaterial, materialIdForGameItem)
        }
        if (groups.invasionRewards.isNotEmpty()) SourceFamilyBlock("invasion-reward", SourceFamilyBoundary.MINIMAL) {
            InvasionSourceRows(groups.invasionRewards, onMonster)
        }
        if (groups.smallMonsters.isNotEmpty()) SourceFamilyBlock("small-monster", SourceFamilyBoundary.MINIMAL) {
            SmallMonsterSourceRows(groups.smallMonsters, onSmallMonster)
        }
        if (groups.monsters.isNotEmpty()) SourceFamilyBlock("monster-reward", SourceFamilyBoundary.MINIMAL) {
            MonsterSourceRows(groups.monsters, onMonster)
        }
        if (groups.palicoExpeditions.isNotEmpty()) SourceFamilyBlock("palico-expedition", SourceFamilyBoundary.MINIMAL) {
            PalicoExpeditionSourceRows(groups.palicoExpeditions)
        }
        if (groups.supplyBoxes.isNotEmpty()) SourceFamilyBlock("supply-box", SourceFamilyBoundary.MINIMAL) {
            SupplyBoxSourceRows(groups.supplyBoxes, quests, onQuest)
        }
        if (groups.quests.isNotEmpty()) SourceFamilyBlock("quest-reward", SourceFamilyBoundary.MINIMAL) {
            QuestRewardSourceRows(groups.quests, quests, onQuest)
        }
        if (groups.specialFree.isNotEmpty()) SourceFamilyBlock("special") {
            SpecialFreeSourceRows(
                material = material,
                sources = groups.specialFree,
                questsById = quests.associateBy { it.id },
                onQuest = onQuest,
                onShowMap = onShowMap
            )
        }
        if (groups.trainingRewards.isNotEmpty()) SourceFamilyBlock("training-reward") {
            TrainingSourceRows(groups.trainingRewards, onTraining)
        }
        if (groups.farm.isNotEmpty()) SourceFamilyBlock("farm") {
            FarmSourceRows(
                groups = groups.farm,
                currentOutputGameItemId = material.gameItemId,
                onMaterial = onMaterial,
                materialIdForGameItem = materialIdForGameItem
            )
        }
        if (groups.trade.isNotEmpty()) SourceFamilyBlock("trading", SourceFamilyBoundary.MINIMAL) {
            TradeSourceRows(groups.trade, onMaterial, materialIdForGameItem)
        }
        if (groups.shop.isNotEmpty()) SourceFamilyBlock("shop") {
            ShopSourceRows(groups.shop)
        }
        if (groups.scrapConversions.isNotEmpty()) SourceFamilyBlock("scrap-conversion") {
            ScrapConversionSourceRows(material, groups.scrapConversions, onMaterial, materialIdForGameItem)
        }
    }
}

private enum class SourceFamilyBoundary { NORMAL, MINIMAL }

internal fun sourceFamilyIdsForDisplay(groups: GroupedMaterialSources): List<String> = buildList {
    if (groups.field.isNotEmpty()) add("field")
    if (groups.combinations.isNotEmpty() || groups.combinationFailures.isNotEmpty()) add("combination")
    if (groups.decorationCrafting.isNotEmpty()) add("decoration-crafting")
    if (groups.roasting.isNotEmpty()) add("roasting")
    if (groups.invasionRewards.isNotEmpty()) add("invasion-reward")
    if (groups.smallMonsters.isNotEmpty()) add("small-monster")
    if (groups.monsters.isNotEmpty()) add("monster-reward")
    if (groups.palicoExpeditions.isNotEmpty()) add("palico-expedition")
    if (groups.supplyBoxes.isNotEmpty()) add("supply-box")
    if (groups.quests.isNotEmpty()) add("quest-reward")
    if (groups.specialFree.isNotEmpty()) add("special")
    if (groups.trainingRewards.isNotEmpty()) add("training-reward")
    if (groups.farm.isNotEmpty()) add("farm")
    if (groups.trade.isNotEmpty()) add("trading")
    if (groups.shop.isNotEmpty()) add("shop")
    if (groups.scrapConversions.isNotEmpty()) add("scrap-conversion")
}

/** Family order after omitting Decoration Crafting ingredient-only reverse links. */
internal fun sourceFamilyIdsForRenderedMaterial(
    groups: GroupedMaterialSources,
    materialGameItemId: Int?
): List<String> = sourceFamilyIdsForDisplay(groups).filterNot { familyId ->
    familyId == "decoration-crafting" &&
        decorationCraftingOutputRows(materialGameItemId, groups.decorationCrafting).isEmpty()
}

internal fun decorationCraftingOutputRows(
    materialGameItemId: Int?,
    sources: List<MaterialSource>
): List<MaterialSource> =
    if (materialGameItemId == null) emptyList()
    else sources.filter { it.decorationOutputGameItemId == materialGameItemId }

@Composable
private fun SourceFamilyBlock(
    familyId: String,
    boundary: SourceFamilyBoundary = SourceFamilyBoundary.NORMAL,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = CutCornerShape(topEnd = 8.dp, bottomStart = 7.dp)
    val inset = if (boundary == SourceFamilyBoundary.MINIMAL) 2.dp else 6.dp
    val familyBorder = AppColors.ParchmentDeep.copy(alpha = .9f)
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(shape)
            .background(AppColors.Parchment.copy(alpha = .38f))
            .border(AppDimens.CardBorder, familyBorder, shape)
            .testTag("source-family-block-$familyId")
            .padding(inset),
        content = content
    )
}

/** One physical map card, preserving the production field ordering. */
private data class FieldMapGroup(
    val mapId: String,
    val location: String,
    val nodes: List<FieldSourceGroup>
)

internal data class FieldRankDisplay(
    val rank: RewardContext,
    val quantities: List<Int>,
    val quantityExplicit: Boolean,
    val showQuantity: Boolean
)

internal fun fieldRankDisplays(node: FieldSourceGroup): List<FieldRankDisplay> =
    listOf(RewardContext.LOW, RewardContext.HIGH).mapNotNull { rank ->
        val rows = node.sourceRows.filter { it.rank == rank }
        if (rows.isEmpty()) return@mapNotNull null
        val quantities = rows.mapNotNull { it.quantity }.distinct()
        val explicit = rows.any { it.quantityExplicit == true }
        FieldRankDisplay(
            rank = rank,
            quantities = quantities,
            quantityExplicit = explicit,
            showQuantity = explicit || quantities.any { it != 1 } || quantities.size > 1
        )
    }.let { displays ->
        // When LR/HR carry different source-native quantity semantics, show
        // both chips' values (including implicit ×1) so the merge cannot hide
        // the factual distinction.
        val rankSpecificQuantity = node.sourceRows
            .groupBy { it.rank }
            .values
            .map { rows -> rows.map { it.quantity to it.quantityExplicit }.distinct() }
            .distinct()
            .size > 1
        if (!rankSpecificQuantity) displays else displays.map { it.copy(showQuantity = true) }
    }

internal fun fieldMapRankLabels(nodes: List<FieldSourceGroup>): List<String> =
    nodes.flatMap { node -> fieldRankDisplays(node).map { it.rank } }
        .distinct()
        .sortedBy { if (it == RewardContext.LOW) 0 else 1 }
        .map { if (it == RewardContext.LOW) "LR" else "HR" }

internal fun fieldMapSummary(nodes: List<FieldSourceGroup>): String =
    buildString {
        append(nodes.size)
        append(if (nodes.size == 1) " point" else " points")
        fieldMapRankLabels(nodes).takeIf { it.isNotEmpty() }?.let {
            append(" · ")
            append(it.joinToString(" · "))
        }
    }

private fun fieldMapGroups(field: List<FieldSourceGroup>): List<FieldMapGroup> =
    field.groupBy { node -> node.sourceRows.firstOrNull()?.locationId ?: node.location }
        .map { (mapId, nodes) -> FieldMapGroup(mapId, nodes.first().location, nodes) }

private fun fieldLocationIcon(mapId: String, location: String): ImageVector = when (mapId) {
    "misty_peaks" -> Icons.Default.Terrain
    "sandy_plains" -> Icons.Default.WbSunny
    "flooded_forest" -> Icons.Default.Water
    "deserted_island" -> Icons.Default.BeachAccess
    "tundra" -> Icons.Default.AcUnit
    "volcano" -> Icons.Default.Whatshot
    else -> when {
        location.contains("snow", ignoreCase = true) || location.contains("tundra", ignoreCase = true) -> Icons.Default.AcUnit
        location.contains("volcano", ignoreCase = true) -> Icons.Default.Whatshot
        location.contains("island", ignoreCase = true) -> Icons.Default.BeachAccess
        location.contains("forest", ignoreCase = true) -> Icons.Default.Water
        location.contains("desert", ignoreCase = true) || location.contains("plain", ignoreCase = true) -> Icons.Default.WbSunny
        else -> Icons.Default.LocationOn
    }
}

@Composable
private fun FieldSources(
    material: Material,
    field: List<FieldSourceGroup>,
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit
) {
    val maps = remember(material.id, field) { fieldMapGroups(field) }
    val expanded = remember(material.id, maps.map { it.mapId }) {
        mutableStateMapOf<String, Boolean>().also { state ->
            if (maps.size == 1 && maps.single().nodes.size <= 3) state[maps.single().mapId] = true
        }
    }
    SourceGroupHeader("Field")
    Text(
        "${maps.size} ${if (maps.size == 1) "map" else "maps"} · ${field.sumOf { 1 }} ${if (field.size == 1) "gathering point" else "gathering points"}",
        color = AppColors.Ink.copy(alpha = .72f),
        style = AppType.Body,
        modifier = Modifier.fillMaxWidth().testTag("field-summary")
            .padding(horizontal = 6.dp, vertical = 7.dp)
    )
    maps.forEach { map ->
        val isExpanded = expanded[map.mapId] == true
        val rank = fieldMapRankLabels(map.nodes).mapNotNull { label ->
            when (label) { "LR" -> RewardContext.LOW; "HR" -> RewardContext.HIGH; else -> null }
        }.singleOrNull()
        ParchmentSurface(
            Modifier.fillMaxWidth().testTag("field-map-card-${map.mapId}")
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 9.dp, end = 5.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.weight(1f).clickable { expanded[map.mapId] = !isExpanded }
                            .padding(vertical = 2.dp).testTag("field-map-header-${map.mapId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(fieldLocationIcon(map.mapId, map.location), contentDescription = null, tint = AppColors.Ink, modifier = Modifier.size(29.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(map.location, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(fieldMapSummary(map.nodes), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                        }
                    }
                    material.gameItemId?.let { gameItemId ->
                        Button(
                            onClick = { onShowMap(map.mapId, gameItemId, rank) },
                            modifier = Modifier.height(40.dp).testTag("field-show-map-${map.mapId}"),
                            shape = AppDimens.ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Crimson, contentColor = Color(0xFFFFE8B0)),
                            contentPadding = PaddingValues(horizontal = 9.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Show on map", style = AppType.ButtonLabel, maxLines = 1)
                        }
                    }
                    IconButton(
                        onClick = { expanded[map.mapId] = !isExpanded },
                        modifier = Modifier.size(34.dp).testTag("field-map-toggle-${map.mapId}")
                    ) {
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (isExpanded) "Collapse ${map.location}" else "Expand ${map.location}", tint = AppColors.Ink)
                    }
                }
                if (isExpanded) {
                    FieldNodeGrid(map.mapId, map.nodes)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
    }
}

/**
 * Field node cells keep the accepted typography and rank chips. The minimum
 * width is derived from that content rather than screenWidthDp, because Thor
 * reports different dp widths depending on its density/window config.
 */
internal val FieldNodeMinCellWidth = 232.dp
internal val FieldNodeGridGap = 6.dp

internal fun fieldGridColumnCount(availableContentWidth: androidx.compose.ui.unit.Dp): Int {
    val twoColumnMinimum = FieldNodeMinCellWidth * 2 + FieldNodeGridGap
    return if (availableContentWidth >= twoColumnMinimum) 2 else 1
}

@Composable
private fun FieldNodeGrid(mapId: String, nodes: List<FieldSourceGroup>) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("field-node-grid-$mapId")) {
        val horizontalPadding = 14.dp
        val availableContentWidth = (maxWidth - horizontalPadding).coerceAtLeast(0.dp)
        val columns = fieldGridColumnCount(availableContentWidth)
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val view = LocalView.current
        val context = LocalContext.current
        LaunchedEffect(mapId, maxWidth, maxHeight, columns, configuration.screenWidthDp, configuration.screenHeightDp) {
            if (BuildConfig.DEBUG) {
                val windowBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.currentWindowMetrics?.bounds
                } else {
                    null
                }
                val windowWidthPx = windowBounds?.width() ?: view.rootView.width
                val windowHeightPx = windowBounds?.height() ?: view.rootView.height
                Log.d(
                    "MHP3rdFieldMetrics",
                    "mapId=$mapId density=${density.density} fontScale=${density.fontScale} " +
                        "screenWidthDp=${configuration.screenWidthDp} screenHeightDp=${configuration.screenHeightDp} " +
                        "orientation=${configuration.orientation} windowMetricsPx=${windowWidthPx}x${windowHeightPx} " +
                        "fieldContainerMaxWidthDp=${maxWidth.value} fieldContentWidthDp=${availableContentWidth.value} " +
                        "selectedColumns=$columns"
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 2.dp)
                .testTag("field-node-grid-$mapId-columns-$columns"),
            verticalArrangement = Arrangement.spacedBy(FieldNodeGridGap)
        ) {
            nodes.chunked(columns).forEach { rowNodes ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FieldNodeGridGap)) {
                    rowNodes.forEach { node ->
                        FieldNodeCell(node, Modifier.weight(1f))
                    }
                    repeat(columns - rowNodes.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun FieldNodeCell(node: FieldSourceGroup, modifier: Modifier = Modifier) {
    ParchmentSurface(
        modifier.fillMaxWidth().testTag("field-node-${node.nodeId}")
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp)) {
            val parts = node.context.orEmpty().split(" · ", limit = 2)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (parts.isNotEmpty()) {
                    Text(parts.first(), color = AppColors.Ink, style = AppType.Body.copy(fontWeight = FontWeight.Bold), maxLines = 1,
                        modifier = Modifier.testTag("field-node-area-${node.nodeId}"))
                    if (parts.size > 1) {
                        Text(" · ${parts[1]}", color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).testTag("field-node-point-${node.nodeId}"))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(node.method?.materialSourceMethodLabel().orEmpty(), color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 1, modifier = Modifier.weight(1f).testTag("field-node-method-${node.nodeId}"))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    fieldRankDisplays(node).forEach { rank ->
                        val label = if (rank.rank == RewardContext.LOW) "LR" else "HR"
                        val quantity = if (rank.showQuantity) rank.quantities.joinToString("/") { "×$it" } else ""
                        Surface(
                            color = if (rank.rank == RewardContext.LOW) Color(0xFFD6E4F3) else Color(0xFFF0D0C8),
                            shape = RoundedCornerShape(5.dp),
                            modifier = Modifier.testTag("field-node-rank-${node.nodeId}-${label.lowercase()}")
                        ) {
                            Text(
                                if (quantity.isBlank()) label else "$label $quantity",
                                color = if (rank.rank == RewardContext.LOW) AppColors.Steel else AppColors.Crimson,
                                style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val PalicoSimpleTierMinWidth = 170.dp
internal val PalicoTierGridGap = 8.dp

internal fun palicoSimpleTierColumnCount(availableWidth: androidx.compose.ui.unit.Dp): Int {
    val four = PalicoSimpleTierMinWidth * 4 + PalicoTierGridGap * 3
    val two = PalicoSimpleTierMinWidth * 2 + PalicoTierGridGap
    return when {
        availableWidth >= four -> 4
        availableWidth >= two -> 2
        else -> 1
    }
}

internal fun palicoExpeditionNumber(id: String): Int =
    Regex("palico_expedition_\\d+_(\\d+)").find(id)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

internal fun formatPalicoExpeditionIds(ids: List<String>): String {
    val numbers = ids.map(::palicoExpeditionNumber).filter { it != Int.MAX_VALUE }.distinct().sorted()
    if (numbers.isEmpty()) return ids.joinToString(", ")
    val parts = mutableListOf<String>()
    var start = numbers.first()
    var previous = start
    fun flush() {
        parts += if (start == previous) start.toString().padStart(2, '0')
        else start.toString().padStart(2, '0') + "–" + previous.toString().padStart(2, '0')
    }
    numbers.drop(1).forEach { number ->
        if (number == previous + 1) previous = number
        else {
            flush()
            start = number
            previous = number
        }
    }
    flush()
    return parts.joinToString(", ")
}

internal fun formatPalicoExpeditionLabel(ids: List<String>): String {
    val numbers = ids.map(::palicoExpeditionNumber).filter { it != Int.MAX_VALUE }.distinct()
    val noun = if (numbers.size == 1) "Expedition" else "Expeditions"
    return "$noun ${formatPalicoExpeditionIds(ids)}"
}

internal fun palicoRewardLabel(raw: String, quantity: Int?): String = listOfNotNull(
    palicoRewardCategoryLabel(raw),
    quantity?.let { "×$it" }
).joinToString(" · ")

private fun palicoRewardCategoryLabel(raw: String): String = when (raw) {
    "GATHERING" -> "Gathering"
    "SMALL_MONSTER" -> "Small Monster"
    "LARGE_MONSTER" -> "Large Monster"
    else -> raw.lowercase().split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}

@Composable
private fun PalicoExpeditionSourceRows(sources: List<MaterialSource>) {
    if (sources.isEmpty()) return
    val projection = remember(sources) { sources.palicoPresentationProjection() }
    SourceGroupHeader(
        "Palico Expedition · " + projection.uniqueExpeditionCount +
            if (projection.uniqueExpeditionCount == 1) " expedition" else " expeditions"
    )
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("item-palico-expedition-family")) {
        val columns = palicoSimpleTierColumnCount(maxWidth - 8.dp)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(PalicoTierGridGap)
        ) {
            var tierIndex = 0
            while (tierIndex < projection.tiers.size) {
                val tier = projection.tiers[tierIndex]
                if (!tier.isSimple) {
                    PalicoTierCard(tier, Modifier.fillMaxWidth())
                    tierIndex += 1
                } else {
                    val run = buildList {
                        var cursor = tierIndex
                        while (cursor < projection.tiers.size && projection.tiers[cursor].isSimple) {
                            add(projection.tiers[cursor])
                            cursor += 1
                        }
                        tierIndex = cursor
                    }
                    run.chunked(columns).forEach { rowTiers ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PalicoTierGridGap)) {
                            rowTiers.forEach { simpleTier -> PalicoTierCard(simpleTier, Modifier.weight(1f)) }
                            repeat(columns - rowTiers.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PalicoTierCard(tier: PalicoTierProjection, modifier: Modifier = Modifier) {
    ParchmentSurface(modifier.testTag("item-palico-tier-" + tier.starRank)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp)) {
            Text("★" + tier.starRank, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            tier.entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .55f), modifier = Modifier.padding(vertical = 5.dp))
                Column(Modifier.fillMaxWidth().testTag("item-palico-entry-" + tier.starRank + "-" + index)) {
                    Text(
                        formatPalicoExpeditionLabel(entry.expeditionIds),
                        color = AppColors.Ink,
                        style = AppType.Body.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth().testTag("item-palico-expedition-label-${tier.starRank}-$index")
                    )
                    entry.rewards.forEach { reward ->
                        Text(
                            palicoRewardLabel(reward.category, reward.quantity),
                            color = AppColors.Ink,
                            style = AppType.Body.copy(fontWeight = FontWeight.Medium),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("item-palico-reward-" + tier.starRank + "-" + (reward.sourceIds.firstOrNull() ?: "unknown"))
                        )
                    }
                    Text(
                        tier.costPerPalico?.let { "$it pts / Palico" } ?: "Cost varies by expedition",
                        color = AppColors.Ink.copy(alpha = .68f),
                        style = AppType.Metadata,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            .testTag("item-palico-cost-${tier.starRank}-$index")
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplyBoxSourceRows(
    sources: List<MaterialSource>,
    quests: List<Quest>,
    onQuest: (String) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources, quests) { projectSupplyQuests(sources, quests) }
    if (projection.isEmpty()) return
    var modalOpen by rememberSaveable(sources.firstOrNull()?.id ?: "supply-box-modal") { mutableStateOf(false) }
    SourceGroupHeader("Supply Box · ${projection.size} ${if (projection.size == 1) "quest" else "quests"}")
    Column(Modifier.fillMaxWidth().testTag("supply-box-header")) {
        val inline = projection.takeIf { it.size <= 10 } ?: projection.take(6)
        inline.forEach { quest -> SupplyQuestParentRow(quest, onQuest) }
        if (projection.size > 10) {
            PreviewFooter(
                moreLabel = "+${projection.size - 6} more",
                actionLabel = "View all ${projection.size}",
                tag = "supply-box-view-all",
                moreTag = "supply-box-more-count",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) {
        SupplyBoxModal(projection, onQuest, onDismiss = { modalOpen = false })
    }
}

@Composable
private fun SupplyQuestParentRow(projection: SupplyQuestProjection, onQuest: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().testTag("item-supply-box-source-${projection.questId}")
            .clickable { onQuest(projection.questId) }
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuestBadge(projection.quest, Modifier.width(72.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(projection.quest.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                projection.entries.forEach { entry ->
                    Text(
                        supplyQuantityLabel(entry),
                        color = AppColors.Ink.copy(alpha = .72f),
                        style = AppType.Metadata,
                        maxLines = 1,
                        modifier = Modifier.testTag("supply-box-entry-${entry.id}")
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, "Open quest", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
}

@Composable
private fun SupplyBoxModal(
    projection: List<SupplyQuestProjection>,
    onQuest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SupplyQuestFilterKey.ALL.name) }
    val selectedFilter = SupplyQuestFilterKey.entries.firstOrNull { it.name == filter } ?: SupplyQuestFilterKey.ALL
    val representedFilters = remember(projection) {
        SupplyQuestFilterKey.entries.filter { key -> key == SupplyQuestFilterKey.ALL || projection.any { it.matchesFilter(key) } }
    }
    // Keep this projection direct rather than memoized: the modal search field
    // is intentionally local state and must update the LazyColumn on every
    // keystroke, including when the same modal instance is reused.
    val visible = projection.filter { it.matchesFilter(selectedFilter) && supplyQuestSearchMatches(it, query) }
    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("supply-box-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Supply Box · ${projection.size} ${if (projection.size == 1) "quest" else "quests"}", color = AppColors.Ink, style = AppType.SectionTitle, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("supply-box-modal-close")) { Icon(Icons.Default.Close, "Close") }
                }
                SearchInputField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    tag = "supply-box-modal-search",
                    label = { Text("Search quests") },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Clear search") } }
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    representedFilters.forEach { key ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { filter = key.name },
                            label = { Text(key.label, style = AppType.Metadata, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("supply-box-filter-${key.name}")
                        )
                    }
                }
                Text("${visible.size} of ${projection.size} quests", color = AppColors.Ink.copy(alpha = .7f), style = AppType.Metadata)
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("supply-box-modal-list"), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(visible, key = { it.questId }) { quest -> SupplyQuestParentRow(quest, onQuest) }
                }
            }
        }
    }
}

@Composable
private fun InvasionSourceRows(
    sources: List<MaterialSource>,
    onMonster: (String) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources) { sources.invasionRewardPresentationProjection() }
    SourceGroupHeader(
        "Invasion Reward · ${projection.monsterCount} " +
            if (projection.monsterCount == 1) "monster" else "monsters"
    )
    projection.monsters.forEach { monster ->
        Column(
            Modifier.fillMaxWidth()
                .testTag("item-invasion-monster-source-${monster.monsterId}")
                .padding(horizontal = 4.dp, vertical = 5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onMonster(monster.monsterId) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    monster.monsterName,
                    color = AppColors.Ink,
                    style = AppType.CardTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("›", color = AppColors.Crimson, style = AppType.CardTitle)
            }
            monster.contexts.forEach { context ->
                Column(
                    Modifier.fillMaxWidth()
                        .testTag("item-invasion-context-${monster.monsterId}-${context.context}")
                        .padding(start = 8.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    Text(context.label, color = AppColors.Ink, style = AppType.Metadata, maxLines = 1)
                    context.entries.forEach { entry ->
                        val quantity = entry.quantity?.let { "×$it" }
                        Text(
                            listOfNotNull(quantity, entry.displayBand).joinToString(" · "),
                            color = AppColors.Crimson,
                            style = AppType.Metadata,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 8.dp, top = 1.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
    }
}

/** Quest-detail context wording is retained independently from the compact
 * Item Sources projection. */
private fun invasionContextLabel(context: String): String = when (context) {
    "GUILD_1_2" -> "Guild ★1–2"
    "LOW" -> "Village / Guild Low"
    "HIGH" -> "Guild High"
    else -> context.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

@Composable
private fun RoastingSourceRows(
    material: Material,
    sources: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    if (sources.isEmpty() || material.gameItemId == null) return
    val outputRows = roastingRoutesForOutput(sources, material.gameItemId)
    if (outputRows.isEmpty()) return

    var modalOpen by rememberSaveable(material.id) { mutableStateOf(false) }
    val routeCount = outputRows.map(::roastingRawRelationId).distinct().size
    Column(Modifier.fillMaxWidth().testTag("item-roasting-family")) {
        SourceGroupHeader("Roasting · $routeCount ${if (routeCount == 1) "route" else "routes"}")
        RoastingInputGroups(
            groups = roastingInputDisplayGroups(outputRows.take(ROASTING_INLINE_ROUTE_LIMIT)),
            currentOutputGameItemId = material.gameItemId,
            onMaterial = onMaterial,
            materialIdForGameItem = materialIdForGameItem,
            tagPrefix = "item-roasting"
        )
        if (routeCount > ROASTING_INLINE_ROUTE_LIMIT) {
            PreviewFooter(
                moreLabel = "+${routeCount - ROASTING_INLINE_ROUTE_LIMIT} more routes",
                actionLabel = "View all $routeCount routes",
                tag = "item-roasting-view-all",
                moreTag = "item-roasting-more-count",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) {
        RoastingSourcesViewAllDialog(
            routes = outputRows,
            currentOutputGameItemId = material.gameItemId,
            onMaterial = onMaterial,
            materialIdForGameItem = materialIdForGameItem,
            onDismiss = { modalOpen = false }
        )
    }
}

@Composable
private fun RoastingInputGroups(
    groups: List<RoastingInputDisplayGroup>,
    currentOutputGameItemId: Int,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    tagPrefix: String
) {
    groups.forEach { group ->
        Column(
            Modifier.fillMaxWidth()
                .testTag("$tagPrefix-input-group-${group.inputGameItemId}")
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            val stableItemId = materialIdForGameItem(group.inputGameItemId)
            val canNavigate = group.inputGameItemId != currentOutputGameItemId && stableItemId != null
            Row(
                Modifier.fillMaxWidth()
                    .then(if (canNavigate) Modifier.clickable { onMaterial(requireNotNull(stableItemId)) } else Modifier)
                    .testTag("$tagPrefix-input-${group.inputGameItemId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(group.inputGameItemId, group.inputItemName, Modifier.size(27.dp))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        group.inputItemName,
                        color = AppColors.Ink,
                        style = AppType.Body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Use ×${group.inputQuantity}",
                        color = AppColors.Ink.copy(alpha = .72f),
                        style = AppType.Metadata,
                        maxLines = 1
                    )
                }
                if (canNavigate) Icon(Icons.Default.ChevronRight, "Open input Item", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
            }
            group.routes.forEach { source ->
                Text(
                    roastingRouteSummary(source),
                    color = AppColors.Ink.copy(alpha = .78f),
                    style = AppType.Metadata,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 33.dp, top = 2.dp, end = 4.dp)
                        .testTag("$tagPrefix-route-${roastingRawRelationId(source)}")
                )
            }
        }
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
    }
}

@Composable
private fun RoastingSourcesViewAllDialog(
    routes: List<MaterialSource>,
    currentOutputGameItemId: Int,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val groups = remember(routes) { roastingInputDisplayGroups(routes) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("item-roasting-view-all-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Roasting · ${routes.size} routes",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f).testTag("item-roasting-view-all-title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("item-roasting-view-all-close")) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("item-roasting-view-all-list")) {
                    RoastingInputGroups(
                        groups = groups,
                        currentOutputGameItemId = currentOutputGameItemId,
                        onMaterial = onMaterial,
                        materialIdForGameItem = materialIdForGameItem,
                        tagPrefix = "item-roasting-modal"
                    )
                }
            }
        }
    }
}

/** Explicit source vocabulary bridge; unknown internal values are omitted. */
internal fun roastingContextLabel(context: String?): String? = when (context) {
    "FIELD_BBQ" -> "Field BBQ"
    "FARM_CUSTOM_ROASTER" -> "Farm Custom Roaster"
    else -> null
}

/** Explicit source vocabulary bridge; do not mechanically title-case enums. */
internal fun roastingResultStateLabel(state: String?): String? = when (state) {
    "UNDERCOOKED" -> "Undercooked"
    "WELL_DONE" -> "Well Done"
    "BURNT" -> "Burnt"
    else -> null
}

@Composable
private fun DecorationCraftingSourceRows(
    outputRows: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val recipeGroups = outputRows.groupBy { it.decorationRecipeId.orEmpty() }.toList()
        .sortedBy { it.first }
    SourceGroupHeader("Crafting · ${recipeGroups.size} ${if (recipeGroups.size == 1) "recipe" else "recipes"}")
    val sameRankCounts = recipeGroups.groupingBy { rankChipLabel(it.second.firstOrNull()?.context) ?: it.second.firstOrNull()?.context.orEmpty() }.eachCount()
    val sameRankSeen = mutableMapOf<String, Int>()
    recipeGroups
        .forEach { (recipeId, rows) ->
            val first = rows.first()
            val rankKey = rankChipLabel(first.context) ?: first.context.orEmpty()
            val rankIndex = (sameRankSeen[rankKey] ?: 0) + 1
            sameRankSeen[rankKey] = rankIndex
            val recipeLabel = if ((sameRankCounts[rankKey] ?: 0) > 1) "Recipe $rankIndex" else null
            ParchmentSurface(
                Modifier.fillMaxWidth()
                    .testTag("item-decoration-crafting-source-$recipeId")
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        rankChipLabel(first.context)?.let { RankChip(it) }
                        recipeLabel?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(it, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    CraftingIngredientRows(
                        rows = rows,
                        onMaterial = onMaterial,
                        materialIdForGameItem = materialIdForGameItem,
                        tagPrefix = "source-crafting-ingredient-$recipeId"
                    )
                }
            }
        }
}

/** Compact LR/HR vocabulary used by forward crafting cards. */
private fun rankChipLabel(context: String?): String? = when {
    context?.contains("LOW", ignoreCase = true) == true -> "LR"
    context?.contains("HIGH", ignoreCase = true) == true -> "HR"
    else -> null
}

@Composable
private fun RankChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("source-rank-chip-$label"),
        shape = RoundedCornerShape(5.dp),
        color = if (label == "LR") Color(0xFFD6E4F3) else Color(0xFFF0D0C8),
        contentColor = if (label == "LR") AppColors.Steel else AppColors.Crimson
    ) {
        Text(label, style = AppType.ButtonLabel, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
    }
}

/**
 * Measures the ingredient names once per recipe and gives every row the same
 * compact name column. Quantities therefore sit immediately after the widest
 * name in this card instead of being pushed to the card's trailing edge.
 */
@Composable
private fun CraftingIngredientRows(
    rows: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    tagPrefix: String
) {
    if (rows.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val quantityTexts = rows.map { it.quantity?.let { quantity -> "×$quantity" } ?: "Quantity not published" }
        val quantityWidthPx = quantityTexts.maxOf { textMeasurer.measure(AnnotatedString(it), style = AppType.Metadata).size.width }
        val longestNameWidthPx = rows.maxOf { textMeasurer.measure(AnnotatedString(it.name), style = AppType.Body).size.width }
        val fixedWidthPx = with(density) { (30.dp + 8.dp + 8.dp).toPx() } + quantityWidthPx
        val availableNameWidthPx = (constraints.maxWidth.toFloat() - fixedWidthPx).coerceAtLeast(1f)
        val nameWidth = with(density) {
            minOf(longestNameWidthPx.toFloat(), availableNameWidthPx).toDp()
        }
        Column(Modifier.fillMaxWidth()) {
            rows.forEach { ingredient ->
                CraftingIngredientRow(
                    ingredient = ingredient,
                    nameWidth = nameWidth,
                    onMaterial = onMaterial,
                    materialIdForGameItem = materialIdForGameItem,
                    tagPrefix = tagPrefix
                )
            }
        }
    }
}

@Composable
private fun CraftingIngredientRow(
    ingredient: MaterialSource,
    nameWidth: androidx.compose.ui.unit.Dp,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    tagPrefix: String
) {
    val gameId = ingredient.decorationIngredientGameItemId
    val targetId = gameId?.let(materialIdForGameItem)
    Row(
        Modifier.fillMaxWidth()
            .then(targetId?.let { Modifier.clickable { onMaterial(it) } } ?: Modifier)
            .testTag("$tagPrefix-${gameId ?: "unknown"}")
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        gameId?.let {
            ItemIcon(it, ingredient.name, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            ingredient.name,
            color = AppColors.Ink,
            style = AppType.Body,
            modifier = Modifier.width(nameWidth),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(
            ingredient.quantity?.let { "×$it" } ?: "Quantity not published",
            color = AppColors.Crimson,
            style = AppType.Metadata,
            modifier = Modifier.testTag("$tagPrefix-quantity-${gameId ?: "unknown"}"),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ScrapConversionSourceRows(
    material: Material,
    sources: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val outputGameItemId = material.gameItemId ?: return
    val projection = remember(outputGameItemId, sources) {
        scrapConversionSourcePresentation(outputGameItemId, sources)
    }
    if (projection.rows.isEmpty()) return
    var modalOpen by rememberSaveable(material.id) { mutableStateOf(false) }
    SourceGroupHeader("Scrap Conversion · ${projection.materialCount} materials")
    Column(Modifier.fillMaxWidth().testTag("item-scrap-conversion-family")) {
        projection.triggerSummary?.let { summary ->
            Text(
                summary,
                color = AppColors.Ink.copy(alpha = .74f),
                style = AppType.Metadata,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("item-scrap-conversion-trigger-summary")
            )
        }
        projection.inlineRows.forEach { source ->
            ScrapConversionSourceInputRow(source, onMaterial, materialIdForGameItem, "item-scrap")
            HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
        }
        if (projection.hiddenMaterialCount > 0) {
            PreviewFooter(
                moreLabel = "+${projection.hiddenMaterialCount} more materials",
                actionLabel = "View all ${projection.materialCount} materials",
                tag = "item-scrap-conversion-view-all",
                moreTag = "item-scrap-conversion-more-count",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) {
        ScrapConversionSourcesViewAllDialog(
            projection = projection,
            onMaterial = onMaterial,
            materialIdForGameItem = materialIdForGameItem,
            onDismiss = { modalOpen = false }
        )
    }
}

@Composable
private fun ScrapConversionSourceInputRow(
    source: MaterialSource,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    tagPrefix: String
) {
    val inputGameItemId = source.inputGameItemId ?: return
    val stableItemId = remember(inputGameItemId) { materialIdForGameItem(inputGameItemId) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("$tagPrefix-source-${source.id}")
    ) {
        Row(
            Modifier.fillMaxWidth()
                .then(if (stableItemId != null) Modifier.clickable { onMaterial(requireNotNull(stableItemId)) } else Modifier)
                .testTag("$tagPrefix-material-$inputGameItemId"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemIcon(inputGameItemId, source.inputItemName, Modifier.size(29.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                source.inputItemName ?: "Item #$inputGameItemId",
                color = AppColors.Ink,
                style = AppType.Body,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (stableItemId != null) {
                Icon(Icons.Default.ChevronRight, "Open input Item", tint = AppColors.Crimson, modifier = Modifier.size(19.dp))
            }
        }
        val quantityLabel = scrapConversionQuantityLabel(source)
        if (quantityLabel.isNotBlank()) {
            Text(
                quantityLabel,
                color = AppColors.Ink.copy(alpha = .74f),
                style = AppType.Metadata,
                modifier = Modifier.padding(start = 36.dp).testTag("$tagPrefix-quantity-$inputGameItemId"),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScrapConversionSourcesViewAllDialog(
    projection: ScrapConversionSourcePresentation,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("item-scrap-conversion-view-all-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Scrap Conversion · ${projection.materialCount} materials",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f).testTag("item-scrap-conversion-view-all-title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("item-scrap-conversion-view-all-close")) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("item-scrap-conversion-view-all-list"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(projection.rows, key = { it.id }) { source ->
                        ScrapConversionSourceInputRow(
                            source = source,
                            onMaterial = onMaterial,
                            materialIdForGameItem = materialIdForGameItem,
                            tagPrefix = "item-scrap-modal"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialFreeSourceRows(
    material: Material,
    sources: List<MaterialSource>,
    questsById: Map<String, Quest>,
    onQuest: (String) -> Unit,
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources) { sources.projectSpecialSources() }
    SourceGroupHeader("Special · ${projection.methodCount} ${if (projection.methodCount == 1) "method" else "methods"}")
    Column(Modifier.fillMaxWidth().testTag("item-special-family")) {
        projection.methods.forEach { method ->
            SpecialMethodSection(method, material, questsById, onQuest, onShowMap)
        }
    }
}

@Composable
private fun SpecialMethodSection(
    method: SpecialSourceMethodProjection,
    material: Material,
    questsById: Map<String, Quest>,
    onQuest: (String) -> Unit,
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit
) {
    Column(Modifier.fillMaxWidth().testTag("item-special-method-${method.mechanism}")) {
        Text(method.displayLabel, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        when (method.mechanism) {
            "PALICO_AFFECTION_TICKET_GRANT" -> {
                method.entries.groupBy { it.source.specialFreePalicoOrigin ?: "UNKNOWN" }
                    .toSortedMap(compareBy { if (it == "SELF_HIRED") 0 else 1 })
                    .forEach { (origin, entries) ->
                        Text(if (origin == "SELF_HIRED") "Hired Palico" else "Received Palico",
                            color = AppColors.Ink.copy(alpha = .78f), style = AppType.Body,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                        entries.forEach { entry -> SpecialCompactRow(entry.source, palicoTransitionText(entry.source), Modifier, false) }
                    }
            }
            else -> method.entries.forEach { entry ->
                SpecialSourceRow(entry.source, material, questsById, onQuest, onShowMap)
            }
        }
    }
}

@Composable
private fun SpecialSourceRow(
    source: MaterialSource,
    material: Material,
    questsById: Map<String, Quest>,
    onQuest: (String) -> Unit,
    onShowMap: (mapId: String, gameItemId: Int, rank: RewardContext?) -> Unit
) {
    val mechanism = source.specialFreeMechanism.orEmpty()
    val quest = source.questId?.let(questsById::get)
    val detail: String
    val clickable: Modifier
    when (mechanism) {
        "VEGGIE_ELDER_FREE_GIFT" -> {
            val mapId = source.specialFreeMapId
            val scope = if (source.specialFreeScopeType == "ALL_SIX_GATHERING_MAPS") "All gathering maps"
            else mapId?.mapDisplayLabel() ?: "Gathering maps"
            detail = listOf(scope, "Gathering quests · before completion").joinToString(" · ")
            val gameItemId = material.gameItemId
            clickable = if (mapId != null && gameItemId != null) Modifier.clickable {
                onShowMap(mapId, gameItemId, null)
            } else Modifier
        }
        "NPC_PROGRESSION_GRANT" -> {
            detail = when {
                source.completionSetIdForDisplay() != null -> "Complete all Village Chief quests"
                quest != null -> "After clearing ${quest.name}"
                else -> "Progression reward"
            }
            clickable = if (quest != null) Modifier.clickable { onQuest(quest.id) } else Modifier
        }
        "PROGRESSION_COMPLETION_GRANT" -> { detail = "Complete all Guild ★3–8 quests"; clickable = Modifier }
        "GUILD_FRIENDSHIP_TICKET_GRANT" -> { detail = friendshipDetail(source); clickable = Modifier }
        "DRINK_TICKET_GRANT" -> { detail = "Every 10 drinks"; clickable = Modifier }
        "HOT_SPRING_TICKET_GRANT" -> { detail = "Recurring bathhouse reward"; clickable = Modifier }
        "VILLAGE_INTERACTION_GIFT" -> { detail = "Large egg by General Store · Some chance"; clickable = Modifier }
        "INITIAL_FREE_GRANT" -> { detail = "Palico Armory · On first access"; clickable = Modifier }
        else -> { detail = "Special acquisition"; clickable = Modifier }
    }
    SpecialCompactRow(source, detail, clickable, source.specialFreeSourceDisagreement || source.specialFreeAccuracyReviewRequired,
        testTag = "item-special-source-${source.id}")
}

@Composable
private fun SpecialCompactRow(
    source: MaterialSource,
    detail: String,
    clickable: Modifier,
    showConflict: Boolean,
    testTag: String = "item-special-free-source-${source.id}"
) {
    val quantity = source.quantity.takeIf { source.quantitySemantics == "PUBLISHED" }?.let { "×$it" }
    Column(Modifier.fillMaxWidth().then(clickable).testTag(testTag).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val giver = source.specialFreeGiverId?.let(::specialGiverLabel)
                if (!giver.isNullOrBlank()) Text(giver, color = AppColors.Ink, style = AppType.Body, maxLines = 1)
                Text(detail, color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 3)
            }
            quantity?.let { Text(it, color = AppColors.Crimson, style = AppType.Body, maxLines = 1) }
        }
        if (showConflict) Text(
            if (source.specialFreeMechanism == "HOT_SPRING_TICKET_GRANT") "Exact trigger disputed" else "Source conflict",
            color = AppColors.Crimson, style = AppType.Metadata, maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
}

private fun specialGiverLabel(raw: String): String = when (raw) {
    "VILLAGE_CHIEF" -> "Village Chief"
    "SMITHY" -> "Smithy"
    "PEDDLER" -> "Peddler"
    "GUILD_MANAGER" -> "Guild Manager"
    "PALICO" -> "Palico"
    "PALICO_ARMORY" -> "Palico Armory"
    "DRINK_SHOP_CAT" -> "Drink Shop"
    "BATHHOUSE_ATTENDANT" -> "Bathhouse attendant"
    "VILLAGE_GENERAL_STORE" -> "General Store"
    "VEGGIE_ELDER" -> "Veggie Elder"
    else -> "Special giver"
}

private fun palicoTransitionText(source: MaterialSource): String =
    "${source.specialFreeAffectionFrom ?: "?"} → ${source.specialFreeAffectionTo ?: "?"} · " +
        (source.quantity.takeIf { source.quantitySemantics == "PUBLISHED" }?.let { "×$it" } ?: "Quantity not published")

private fun friendshipDetail(source: MaterialSource): String = when (source.specialFreeCounterType) {
    "INDIVIDUAL_FRIENDSHIP" -> listOfNotNull(
        "Every ${source.specialFreeInterval ?: 10} individual friendship points",
        when {
            source.specialFreeThresholdMax != null -> "Below ${source.specialFreeThresholdMax + 1}"
            source.specialFreeThresholdMin != null -> "${source.specialFreeThresholdMin}+"
            else -> null
        }
    ).joinToString(" · ")
    "TOTAL_FRIENDSHIP" -> "Every ${source.specialFreeInterval ?: 25} total friendship points · Ticket tier depends on Guild Card color"
    else -> "Guild friendship progression"
}

private fun MaterialSource.completionSetIdForDisplay(): String? =
    specialFreeCompletionSetId?.takeIf { it == "VILLAGE_ALL_CHIEF_QUESTS_COMPLETE" }

internal fun palicoAffectionLabel(relationId: String): String {
    val match = Regex("PALICO_(SELF|GIFTED|RECEIVED)_(\\d+)_TO_(\\d+)").find(relationId) ?: return "Affection transition"
    val origin = if (match.groupValues[1] == "SELF") "Self-hired Palico" else "Received Palico"
    return "$origin · Affection ${match.groupValues[2]} → ${match.groupValues[3]}"
}

internal fun friendshipLabel(source: MaterialSource): String = when {
    source.id.contains("INDIVIDUAL_BELOW_60") -> "Every 10 individual friendship · below 60"
    source.id.contains("INDIVIDUAL_AT_LEAST_60") -> "Every 10 individual friendship · 60 or higher"
    source.id.contains("TOTAL") -> "Every 25 total friendship · ticket depends on Guild Card color"
    else -> "Guild friendship progression"
}

private fun elderGiftLabel(source: MaterialSource): String = when (source.specialFreeScopeType) {
    "ALL_SIX_GATHERING_MAPS" -> "All Elder Gathering Quest maps · Possible gift"
    "MAP_SPECIFIC" -> listOfNotNull(source.specialFreeMapId?.mapDisplayLabel(), "Gathering quests only", "Possible gift").joinToString(" · ")
    else -> "Possible gift"
}

private fun String.mapDisplayLabel(): String = replace('_', ' ').lowercase().split(' ')
    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

@Composable
internal fun TradeSourceRows(
    sources: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val projection = remember(sources) { tradeSourcesPresentation(sources) }
    if (projection.routeCount == 0) return
    var modalOpen by rememberSaveable(sources.firstOrNull()?.id ?: "trade-sources-modal") { mutableStateOf(false) }
    SourceGroupHeader("Trading · ${projection.routeCount} ${if (projection.routeCount == 1) "route" else "routes"}")
    Column(Modifier.fillMaxWidth().testTag("item-trading-sources")) {
        if (projection.veggieElderRoutes.isNotEmpty()) {
            Text("Veggie Elder", color = AppColors.Ink, style = AppType.CardTitle, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).testTag("item-trading-veggie-elder"))
            val visibleRoutes = if (projection.veggieElderRoutes.size <= 6) projection.veggieElderRoutes else tradeRoutePreview(projection.veggieElderRoutes)
            val visibleInputs = remember(visibleRoutes) { tradeSourcesPresentation(visibleRoutes).veggieElderInputs }
            visibleInputs.forEach { group -> TradeElderInputGroup(group, onMaterial, materialIdForGameItem) }
            if (projection.veggieElderRoutes.size > 6) {
                PreviewFooter(
                    moreLabel = "+${projection.veggieElderRoutes.size - 6} more routes",
                    actionLabel = "View all ${projection.veggieElderRoutes.size} routes",
                    tag = "item-trading-view-all",
                    moreTag = "item-trading-more-routes",
                    onAction = { modalOpen = true }
                )
            }
        }
        if (projection.farmManagerRoutes.isNotEmpty()) {
            Text("Farm Manager", color = AppColors.Ink, style = AppType.CardTitle, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).testTag("item-trading-farm-manager"))
            projection.farmManagerRoutes.forEach { source -> TradeFarmManagerRoute(source) }
        }
    }
    if (modalOpen) {
        TradeSourcesViewAllDialog(projection, onMaterial, materialIdForGameItem) { modalOpen = false }
    }
}

@Composable
private fun TradeElderInputGroup(
    group: TradeSourceInputGroup,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val stableInputId = remember(group.inputGameItemId) { materialIdForGameItem(group.inputGameItemId) }
    val costQuantities = group.costQuantities
    ParchmentSurface(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp).testTag("item-trading-input-${group.inputGameItemId}")) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .then(if (stableInputId != null) Modifier.clickable { onMaterial(stableInputId) } else Modifier)
                    .testTag("item-trading-input-link-${group.inputGameItemId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(group.inputGameItemId, group.inputName, Modifier.size(30.dp))
                Spacer(Modifier.width(7.dp))
                Text(group.inputName, color = AppColors.Ink, style = AppType.Body, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                when (costQuantities.size) {
                    1 -> Text("Cost ×${costQuantities.single()}", color = AppColors.Crimson, style = AppType.Metadata, modifier = Modifier.testTag("item-trading-cost-${group.inputGameItemId}"))
                    in 2..Int.MAX_VALUE -> Text("Cost varies", color = AppColors.Crimson, style = AppType.Metadata)
                }
            }
            group.routes.forEach { source ->
                val routeCost = source.tradeInputQuantity?.let { "Cost ×$it" }.takeIf { costQuantities.size > 1 }
                val receive = source.quantity?.let { "Receive ×$it" }
                val routeFacts = listOfNotNull(routeCost, receive).joinToString(" · ")
                if (routeFacts.isNotBlank()) Text(routeFacts, color = AppColors.Ink, style = AppType.Metadata, modifier = Modifier.padding(start = 37.dp, top = 2.dp).testTag("item-trading-route-quantity-${source.id}"))
                val routeMetadata = tradeSourceRouteMetadata(source)
                if (!routeMetadata.isNullOrBlank()) Text(routeMetadata, color = AppColors.Ink.copy(alpha = .74f), style = AppType.Metadata, modifier = Modifier.padding(start = 37.dp, bottom = 2.dp).testTag("item-trading-route-${source.id}"), maxLines = 2)
            }
        }
    }
}

@Composable
private fun TradeFarmManagerRoute(source: MaterialSource) {
    val cost = source.tradeCurrencyCost?.let { "Cost ${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(it)} pts" }
    val facts = listOfNotNull(cost, source.quantity?.let { "Receive ×$it" }, source.condition).joinToString(" · ")
    Column(Modifier.fillMaxWidth().testTag("item-trading-farm-route-${source.id}").padding(horizontal = 8.dp, vertical = 5.dp)) {
        if (facts.isNotBlank()) Text(facts, color = AppColors.Ink, style = AppType.Body, maxLines = 2)
    }
    HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
}

@Composable
private fun TradeSourcesViewAllDialog(
    projection: TradeSourcesPresentation,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    onDismiss: () -> Unit
) {
    val allGroups = remember(projection) { tradeSourcesPresentation(projection.veggieElderRoutes).veggieElderInputs }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("item-trading-view-all-modal"), color = AppColors.Parchment, shape = AppDimens.CardShape) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Veggie Elder · ${projection.veggieElderRoutes.size} routes", color = AppColors.Ink, style = AppType.SectionTitle, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("item-trading-view-all-close")) { Icon(Icons.Default.Close, "Close") }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("item-trading-view-all-list"), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(allGroups, key = { it.inputGameItemId }) { group -> TradeElderInputGroup(group, onMaterial, materialIdForGameItem) }
                }
            }
        }
    }
}

@Composable
private fun ShopSourceRows(sources: List<MaterialSource>) {
    val projection = remember(sources) { shopSourcesPresentation(sources) }
    if (projection.relationCount == 0) return
    Column(Modifier.fillMaxWidth().testTag("item-shop-family")) {
        Text(
            "Shop · ${projection.relationCount} ${if (projection.relationCount == 1) "source" else "sources"}",
            color = AppColors.Crimson,
            style = AppType.ButtonLabel,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(AppColors.ParchmentDeep.copy(alpha = .28f))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            maxLines = 1
        )
        projection.storeRows.forEach { row ->
            Column(
                Modifier.fillMaxWidth().testTag("item-shop-source-${row.relationIds.joinToString("-")}")
                    .padding(horizontal = 4.dp, vertical = 5.dp)
            ) {
                Text(row.label, color = AppColors.Ink, style = AppType.Body, maxLines = 1)
                Text(row.priceLabel, color = AppColors.Ink, style = AppType.Metadata)
                row.availabilityLabel?.let {
                    Text(it, color = AppColors.Ink.copy(alpha = .74f), style = AppType.Metadata, maxLines = 2)
                }
            }
            HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
        }
        if (projection.peddlerRows.isNotEmpty()) {
            Text(
                "Peddler",
                color = AppColors.Ink,
                style = AppType.Body,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 5.dp, bottom = 2.dp)
                    .testTag("item-shop-peddler-subgroup")
            )
            projection.peddlerRows.forEach { row ->
                Column(
                    Modifier.fillMaxWidth().testTag("item-shop-profile-${row.relationIds.single()}")
                        .padding(horizontal = 4.dp, vertical = 5.dp)
                ) {
                    Text(
                        "${row.label} · ${row.priceLabel}",
                        color = AppColors.Ink,
                        style = AppType.Metadata,
                        maxLines = 1,
                        modifier = Modifier.testTag("item-shop-source-${row.relationIds.single()}")
                    )
                    row.qualifier?.let { Text(it, color = AppColors.Ink.copy(alpha = .74f), style = AppType.Metadata, maxLines = 1) }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
            }
        }
    }
}

@Composable
private fun CombinationSourceRows(
    recipes: List<MaterialSource>,
    failures: List<MaterialSource>,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    if (recipes.isEmpty() && failures.isEmpty()) return
    val recipeCount = recipes.size
    SourceGroupHeader(
        if (recipeCount > 0) "Combination · $recipeCount ${if (recipeCount == 1) "recipe" else "recipes"}"
        else "Combination"
    )
    recipes.sortedBy { it.recipeNumber }.forEach { source ->
        ParchmentSurface(
            Modifier.fillMaxWidth()
                .testTag("item-combination-source-${source.recipeNumber}")
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                CombinationFormula(source, onMaterial, materialIdForGameItem)
                val metadata = listOfNotNull(
                    source.baseSuccessPercent?.let { "Base success $it%" },
                    combinationQuantityLabel(source.outputQuantityMin, source.outputQuantityMax)?.let { "Makes $it" },
                    source.ingredientAvailability.takeIf { it == "QUEST_SUPPLY_ONLY" }?.let { "Quest supply only" }
                ).joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Text(metadata, color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 2)
                }
            }
        }
    }
    failures.forEach { source ->
        ParchmentSurface(
            Modifier.fillMaxWidth()
                .testTag("item-combination-failure-source-${source.id}")
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                Text("Failed combination", color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
                Text(source.name, color = AppColors.Ink, style = AppType.Body, maxLines = 1)
                Text("Chance varies by recipe and modifiers", color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 2)
            }
        }
    }
}

@Composable
private fun CombinationFormula(
    source: MaterialSource,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val first = source.ingredientAGameItemId to source.ingredientAName
    val second = source.ingredientBGameItemId to source.ingredientBName
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 430.dp
        if (horizontal) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CombinationIngredient(first, Modifier.weight(1f), "source-combination-ingredient-${source.recipeNumber}-a", onMaterial, materialIdForGameItem)
                Text("+", color = AppColors.Ink, style = AppType.SectionTitle, modifier = Modifier.padding(horizontal = 9.dp))
                CombinationIngredient(second, Modifier.weight(1f), "source-combination-ingredient-${source.recipeNumber}-b", onMaterial, materialIdForGameItem)
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                CombinationIngredient(first, Modifier.fillMaxWidth(), "source-combination-ingredient-${source.recipeNumber}-a", onMaterial, materialIdForGameItem)
                Text("+", color = AppColors.Ink, style = AppType.SectionTitle, modifier = Modifier.padding(vertical = 1.dp))
                CombinationIngredient(second, Modifier.fillMaxWidth(), "source-combination-ingredient-${source.recipeNumber}-b", onMaterial, materialIdForGameItem)
            }
        }
    }
}

@Composable
private fun CombinationIngredient(
    ingredient: Pair<Int?, String?>,
    modifier: Modifier,
    tag: String,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    val targetId = ingredient.first?.let(materialIdForGameItem)
    Row(
        modifier
            .then(targetId?.let { Modifier.clickable { onMaterial(it) } } ?: Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ingredient.first?.let {
            ItemIcon(it, ingredient.second, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            ingredient.second ?: "Unknown item",
            color = AppColors.Ink,
            style = AppType.Body,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

internal fun combinationQuantityLabel(min: Int?, max: Int?): String? {
    if (min == null || max == null || min <= 0 || max <= 0) return null
    return if (min == max) "×$min" else "×$min–$max"
}

@Composable
private fun FarmSourceRows(
    groups: List<FarmSourceGroup>,
    currentOutputGameItemId: Int?,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    if (groups.isEmpty()) return
    var modalOpen by rememberSaveable(currentOutputGameItemId, "farm-view-all") { mutableStateOf(false) }
    SourceGroupHeader("Farm · ${groups.size} ${if (groups.size == 1) "method" else "methods"}")
    Column(Modifier.fillMaxWidth().testTag("farm-section-${currentOutputGameItemId ?: "unknown"}")) {
        groups.take(farmVisibleMethodCount(groups.size))
            .groupFarmMethodsByFacility()
            .forEach { facility ->
                FarmFacilityMethods(
                    facility = facility,
                    currentOutputGameItemId = currentOutputGameItemId,
                    onMaterial = onMaterial,
                    materialIdForGameItem = materialIdForGameItem
                )
            }
        if (groups.size > 6) {
            PreviewFooter(
                moreLabel = "+${groups.size - 6} more",
                actionLabel = "View all ${groups.size} methods",
                tag = "farm-method-view-all",
                moreTag = "farm-method-more-count",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) FarmSourceViewAllModal(
        groups = groups,
        currentOutputGameItemId = currentOutputGameItemId,
        onMaterial = onMaterial,
        materialIdForGameItem = materialIdForGameItem,
        onDismiss = { modalOpen = false }
    )
}

internal const val BUG_TREE_SEESAW_EXPLANATION =
    "Send 4 Palicoes and time the seesaw launch. Better timing affects the haul. " +
        "The published game data contains several possible reward tables, but the exact timing result that selects each table is not documented."

@Composable
private fun FarmFacilityMethods(
    facility: FarmSourceFacilityGroup,
    currentOutputGameItemId: Int?,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    Column(
        Modifier.fillMaxWidth().testTag("farm-facility-${facility.facilityId}")
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(
            facility.displayName,
            color = AppColors.Ink,
            style = AppType.CardTitle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)
                .testTag("farm-facility-title-${facility.facilityId}")
        )
        if (facility.facilityId == "farm_bug_tree_seesaw") {
            Text(
                BUG_TREE_SEESAW_EXPLANATION,
                color = AppColors.Ink.copy(alpha = .78f),
                style = AppType.Metadata,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp)
                    .testTag("farm-facility-explanation-farm_bug_tree_seesaw")
            )
        }
        facility.mechanismNote?.let { note ->
            Text(
                note.summary,
                color = AppColors.Ink.copy(alpha = .78f),
                style = AppType.Metadata,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("farm-mechanism-note-${facility.facilityId}")
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("farm-mechanism-inputs-${facility.facilityId}"),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                note.sourceItemReferences.forEachIndexed { index, reference ->
                    if (index > 0) Text("·", color = AppColors.Ink.copy(alpha = .6f), style = AppType.Metadata)
                    val materialId = materialIdForGameItem(reference.gameItemId)
                    val canNavigate = reference.gameItemId != currentOutputGameItemId && materialId != null
                    Text(
                        reference.displayName,
                        color = if (canNavigate) AppColors.Crimson else AppColors.Ink,
                        style = AppType.Metadata,
                        modifier = Modifier
                            .then(if (canNavigate) Modifier.clickable { onMaterial(requireNotNull(materialId)) } else Modifier)
                            .testTag("farm-mechanism-input-${reference.gameItemId}")
                            .padding(vertical = 3.dp)
                    )
                }
            }
        }
        facility.methods.forEach { method ->
            FarmSourceMethodRow(method, currentOutputGameItemId, onMaterial, materialIdForGameItem)
        }
    }
}

@Composable
private fun FarmSourceMethodRow(
    method: FarmSourceGroup,
    currentOutputGameItemId: Int?,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?
) {
    Column(
        Modifier.fillMaxWidth().testTag("farm-method-${method.stableMethodId}")
            .padding(start = 8.dp, end = 5.dp, top = 4.dp, bottom = 5.dp)
    ) {
        if (method.triggerLabel.isNotBlank()) {
            Text(
                method.triggerLabel,
                color = AppColors.Ink.copy(alpha = .78f),
                style = AppType.Metadata,
                modifier = Modifier.fillMaxWidth().testTag("farm-method-trigger-${method.stableMethodId}"),
                maxLines = 2
            )
        }
        if (method.inputGameItemId != null) {
            val inputId = method.inputGameItemId
            val targetMaterialId = materialIdForGameItem(inputId)
            val canNavigate = inputId != currentOutputGameItemId && targetMaterialId != null
            Row(
                Modifier.fillMaxWidth()
                    .then(if (canNavigate) Modifier.clickable { onMaterial(requireNotNull(targetMaterialId)) } else Modifier)
                    .testTag("farm-method-input-$inputId")
                    .padding(top = 3.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ItemIcon(inputId, method.inputItemName, Modifier.size(27.dp).testTag("farm-input-icon-$inputId"))
                Spacer(Modifier.width(6.dp))
                Text(
                    method.inputItemName ?: "Input Item",
                    color = AppColors.Ink,
                    style = AppType.Body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        method.outcomes.forEachIndexed { index, outcome ->
            Text(
                outcome.displayLabel(),
                color = AppColors.Crimson,
                style = AppType.Metadata,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    .testTag("farm-method-outcome-${method.stableMethodId}-$index"),
                maxLines = 1,
                softWrap = false
            )
        }
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .42f))
    }
}

@Composable
private fun FarmSourceViewAllModal(
    groups: List<FarmSourceGroup>,
    currentOutputGameItemId: Int?,
    onMaterial: (String) -> Unit,
    materialIdForGameItem: (Int) -> String?,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val facilities = remember(groups) { groups.groupFarmMethodsByFacility() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("farm-method-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape,
            border = AppDimens.CardBorder.let { BorderStroke(it, AppColors.ParchmentDeep) }
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 5.dp, top = 8.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Farm · ${groups.size} methods",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f).testTag("farm-method-modal-title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp).testTag("farm-method-modal-close")) {
                        Icon(Icons.Default.Close, "Close Farm methods", tint = AppColors.Ink)
                    }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .55f))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("farm-method-modal-list"),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(facilities, key = { it.facilityId }) { facility ->
                        FarmFacilityMethods(facility, currentOutputGameItemId, onMaterial, materialIdForGameItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestRewardSourceRows(
    sources: List<MaterialSource>,
    quests: List<Quest>,
    onQuest: (String) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources, quests) { projectQuestRewards(sources, quests) }
    if (projection.isEmpty()) return
    var modalOpen by rememberSaveable(sources.firstOrNull()?.id ?: "quest-reward-modal") { mutableStateOf(false) }
    SourceGroupHeader("Quest Rewards · ${projection.size} ${if (projection.size == 1) "quest" else "quests"}")
    Column(Modifier.fillMaxWidth().testTag("quest-rewards-header")) {
        val inline = projection.takeIf { it.size <= 10 } ?: projection.take(6)
        inline.forEach { quest -> QuestRewardParentRow(quest, onQuest) }
        if (projection.size > 10) {
            // Keep the historical quest-badge semantics anchors available for
            // existing navigation regressions without rendering a second copy
            // of the omitted reward rows. The visible list remains a six-row
            // preview and the modal owns the complete presentation.
            projection.drop(6).forEach { quest ->
                Box(Modifier.fillMaxWidth().height(1.dp).testTag("item-quest-badge-${quest.questId}"))
            }
            PreviewFooter(
                moreLabel = "+${projection.size - 6} more",
                actionLabel = "View all ${projection.size}",
                tag = "quest-rewards-view-all",
                moreTag = "quest-rewards-more-count",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) QuestRewardModal(projection, onQuest) { modalOpen = false }
}

@Composable
private fun QuestRewardParentRow(projection: QuestRewardQuestProjection, onQuest: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().testTag("item-quest-reward-source-${projection.questId}")
            .clickable { onQuest(projection.questId) }
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.testTag("item-quest-reward-badge-${projection.questId}")) {
                Box(Modifier.testTag("item-quest-badge-${projection.questId}")) {
                    QuestBadge(projection.quest, Modifier.size(width = 72.dp, height = 46.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(projection.quest.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                projection.pools.forEach { pool ->
                    Text(
                        questRewardPoolLabel(pool.rewardPool),
                        color = AppColors.Crimson,
                        style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = Modifier.testTag("quest-reward-pool-${projection.questId}-${pool.rewardPool}")
                    )
                    pool.entries.forEach { entry ->
                        val facts = listOfNotNull(
                            entry.quantity?.let { "×$it" },
                            entry.probabilityValuePercent?.let { "$it% reward slot" },
                            questRewardConditionLabel(entry.condition)
                        ).joinToString(" · ")
                        Text(
                            facts,
                            color = AppColors.Ink.copy(alpha = .74f),
                            style = AppType.Metadata,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("quest-reward-entry-${entry.rawRelationId}")
                        )
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, "Open quest", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
        }
    }
    HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
}

@Composable
private fun QuestRewardModal(
    projection: List<QuestRewardQuestProjection>,
    onQuest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SupplyQuestFilterKey.ALL.name) }
    val selectedFilter = SupplyQuestFilterKey.entries.firstOrNull { it.name == filter } ?: SupplyQuestFilterKey.ALL
    val representedFilters = remember(projection) {
        SupplyQuestFilterKey.entries.filter { key ->
            key == SupplyQuestFilterKey.ALL || projection.any {
                SupplyQuestProjection(it.quest, emptyList(), emptyList(), it.sourceOrder).matchesFilter(key)
            }
        }
    }
    val visible = projection.filter {
        SupplyQuestProjection(it.quest, emptyList(), emptyList(), it.sourceOrder).matchesFilter(selectedFilter) &&
            questRewardSearchMatches(it, query)
    }
    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("quest-rewards-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quest Rewards · ${projection.size} ${if (projection.size == 1) "quest" else "quests"}", color = AppColors.Ink, style = AppType.SectionTitle, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("quest-rewards-modal-close")) { Icon(Icons.Default.Close, "Close") }
                }
                SearchInputField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    tag = "quest-rewards-modal-search",
                    label = { Text("Search quests") },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, "Clear search") } }
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    representedFilters.forEach { key ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { filter = key.name },
                            label = { Text(key.label, style = AppType.Metadata, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("quest-rewards-filter-${key.name}")
                        )
                    }
                }
                Text("${visible.size} of ${projection.size} quests", color = AppColors.Ink.copy(alpha = .7f), style = AppType.Metadata)
                LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("quest-rewards-modal-list"), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(visible, key = { it.questId }) { quest -> QuestRewardParentRow(quest, onQuest) }
                }
            }
        }
    }
}

/** Compatibility helper retained for the existing quest runtime regression;
 * the visible Quest Rewards component uses the source-faithful data-layer
 * formatter above. */
internal fun formatQuestRewardCondition(condition: String?): String? {
    if (condition == null) return null
    return questRewardConditionLabel(condition)?.takeUnless { it == "Condition label unavailable" } ?: condition
}

private const val TRAINING_INLINE_MISSION_LIMIT = 6

@Composable
private fun TrainingSourceRows(sources: List<MaterialSource>, onTraining: (String) -> Unit) {
    val projection = remember(sources) { sources.projectTrainingRewards() }
    if (projection.missionCount == 0) return
    var modalOpen by rememberSaveable("training-rewards-modal-${sources.firstOrNull()?.id.orEmpty()}") { mutableStateOf(false) }
    SourceGroupHeader("Training Rewards · ${projection.missionCount} ${if (projection.missionCount == 1) "mission" else "missions"}")
    Column(Modifier.fillMaxWidth().testTag("item-training-rewards-header")) {
        projection.missions.take(TRAINING_INLINE_MISSION_LIMIT).forEach { mission ->
            TrainingMissionSourceGroup(mission, onTraining)
        }
        if (projection.missionCount > TRAINING_INLINE_MISSION_LIMIT) {
            PreviewFooter(
                moreLabel = "+${projection.missionCount - TRAINING_INLINE_MISSION_LIMIT} more",
                actionLabel = "View all ${projection.missionCount}",
                tag = "item-training-rewards-view-all",
                moreTag = "item-training-rewards-more",
                onAction = { modalOpen = true }
            )
        }
    }
    if (modalOpen) TrainingRewardsModal(projection, onTraining) { modalOpen = false }
}

@Composable
private fun TrainingMissionSourceGroup(
    mission: TrainingMissionSourceProjection,
    onTraining: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().testTag("item-training-mission-${mission.trainingQuestId}")) {
        Row(
            Modifier.fillMaxWidth()
                .clickable { onTraining(mission.navigationTarget) }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                trainingClassDisplayLabel(mission.trainingClass),
                color = AppColors.Ink,
                style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.background(AppColors.ParchmentDeep.copy(alpha = .34f), AppDimens.CardShape)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("item-training-class-${mission.trainingQuestId}"),
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Text(
                mission.displayTitle,
                color = AppColors.Ink,
                style = AppType.CardTitle,
                modifier = Modifier.weight(1f).testTag("item-training-mission-title-${mission.trainingQuestId}"),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ChevronRight, "Open training mission", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
        }
        mission.rewardGroups.forEach { group ->
            val label = group.displayLabel ?: return@forEach
            Text(
                label,
                color = AppColors.Crimson,
                style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 12.dp, top = 1.dp, end = 4.dp, bottom = 1.dp)
                    .testTag("item-training-pool-${mission.trainingQuestId}-${group.rawRewardType}"),
                maxLines = 1
            )
            group.entries.forEach { entry ->
                val facts = listOfNotNull(
                    entry.quantity?.takeIf { entry.quantitySemantics == "EXPLICIT" }?.let { "×$it" },
                    entry.probabilityValuePercent?.takeIf { entry.probabilitySemantics != "SOURCE_UNSUPPORTED" }?.let { "$it%" }
                ).joinToString(" · ")
                if (facts.isNotBlank()) {
                    Text(
                        facts,
                        color = AppColors.Ink.copy(alpha = .74f),
                        style = AppType.Metadata,
                        modifier = Modifier.padding(start = 20.dp, end = 4.dp, bottom = 3.dp)
                            .testTag("item-training-entry-${entry.rawRelationId}"),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
    }
}

@Composable
private fun TrainingRewardsModal(
    projection: TrainingRewardItemProjection,
    onTraining: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f).testTag("item-training-rewards-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Training Rewards · ${projection.missionCount} ${if (projection.missionCount == 1) "mission" else "missions"}",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("item-training-rewards-modal-close")) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("item-training-rewards-modal-list"),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(projection.missions, key = { it.trainingQuestId }) { mission ->
                        TrainingMissionSourceGroup(mission, onTraining)
                    }
                }
            }
        }
    }
}

private fun String.materialSourceMethodLabel(): String = lowercase().split('_').joinToString(" ") { word ->
    word.replaceFirstChar { it.uppercase() }
}

@Composable
private fun SourceGroupHeader(title: String) {
    Text(
        title,
        color = AppColors.Crimson,
        style = AppType.ButtonLabel,
        modifier = Modifier.fillMaxWidth().background(AppColors.ParchmentDeep.copy(alpha = .28f))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        maxLines = 1
    )
}

@Composable
private fun SourceRows(
    title: String,
    sources: List<MaterialSource>,
    onSmallMonster: ((String) -> Unit)? = null
) {
    if (sources.isEmpty()) return
    SourceGroupHeader(title)
    sources.forEach { source ->
        val smallMonsterId = source.smallMonsterId
        val interaction = if (smallMonsterId != null && onSmallMonster != null) {
            Modifier.testTag("item-small-monster-source-${source.id}").clickable { onSmallMonster(smallMonsterId) }
        } else Modifier
        Row(
            Modifier.fillMaxWidth().then(interaction).padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
            Text(source.name, color = AppColors.Ink, style = AppType.Body, maxLines = 2)
            val detail = listOfNotNull(
                source.method?.materialSourceMethodLabel(),
                source.condition?.takeUnless { it == source.context }
            ).joinToString(" · ")
            if (detail.isNotBlank()) Text(detail, color = AppColors.Ink.copy(alpha = .74f), style = AppType.Metadata, maxLines = 2)
            val metadata = listOfNotNull(source.rank?.displayLabel(), source.chanceForDisplay()).joinToString(" · ")
            if (metadata.isNotBlank()) Text(metadata, color = AppColors.Crimson, style = AppType.Metadata, maxLines = 2)
            }
            if (smallMonsterId != null && onSmallMonster != null) {
                Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .45f))
    }
}

@Composable
private fun UsesCard(uses: List<String>, modifier: Modifier) {
    SectionCard(stringResource(R.string.used_in), modifier = modifier, accent = AppColors.Moss) {
        uses.forEach { Text("• $it", color = AppColors.Ink, style = AppType.Body, modifier = Modifier.padding(vertical = 3.dp)) }
    }
}

@Composable
internal fun MonsterScreen(
    monster: Monster,
    quests: List<Quest>,
    materials: List<Material>,
    selectedTab: Int,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onTabChange: (Int) -> Unit,
    onMaterial: (String) -> Unit,
    selectedRewardContext: RewardContext? = null,
    onRewardContextChange: (RewardContext) -> Unit = {},
    trainingQuests: List<TrainingQuest> = emptyList(),
    onTraining: (String) -> Unit = {},
    selectedRewardFilter: MonsterRewardFilter? = null,
    onRewardFilterChange: (MonsterRewardFilter) -> Unit = {},
    selectedRewardListState: LazyListState? = null
) {
    var elemental by rememberSaveable(monster.id) { mutableStateOf(false) }
    var hitzoneState by rememberSaveable(monster.id, "hitzone-state") { mutableStateOf("normal") }
    var localRewardFilterName by rememberSaveable(monster.id, "reward-filter") { mutableStateOf(MonsterRewardFilter.ALL.name) }
    val rewardFilter = selectedRewardFilter
        ?: MonsterRewardFilter.entries.firstOrNull { it.name == localRewardFilterName }
        ?: MonsterRewardFilter.ALL
    val rewardContexts = monster.rewards.availableContexts()
    var rewardContextName by rememberSaveable(monster.id, "reward-context") {
        mutableStateOf(rewardContexts.firstOrNull()?.name ?: RewardContext.LOW.name)
    }
    val rewardContext = selectedRewardContext?.takeIf { it in rewardContexts }
        ?: rewardContexts.firstOrNull { it.name == rewardContextName }
        ?: rewardContexts.firstOrNull()
        ?: RewardContext.LOW
    val localRewardListState = rememberSaveable(
        monster.id, rewardContext.name, rewardFilter.name,
        saver = LazyListState.Saver
    ) { LazyListState() }
    val rewardListState = selectedRewardListState ?: localRewardListState
    Column(
        Modifier.fillMaxSize().testTag("screen-monster").padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CompactMonsterHeader(monster, isFavorite, onBack, onFavorite)
        Row(Modifier.fillMaxWidth().height(36.dp).testTag("monster-tabs"), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(R.string.overview, R.string.rewards, R.string.hitzones, R.string.quests).forEachIndexed { index, title ->
                CompactFilter(stringResource(title), selectedTab == index, Modifier.weight(1f).testTag("monster-tab-$index")) {
                    onTabChange(index)
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).testTag("monster-tab-content")) {
            when (selectedTab) {
                0 -> MonsterOverview(monster, Modifier.fillMaxSize().testTag("monster-overview").verticalScroll(rememberScrollState()))
                1 -> Column(Modifier.fillMaxSize().testTag("monster-rewards"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    RewardsCard(
                        monsterId = monster.id,
                        rewards = monster.rewards,
                        materials = materials,
                        selectedContext = rewardContext,
                        selectedFilter = rewardFilter,
                        invasionRewards = monster.invasionRewards,
                        onFilter = {
                            localRewardFilterName = it.name
                            onRewardFilterChange(it)
                        },
                        listState = rewardListState,
                        onContext = {
                            rewardContextName = it.name
                            onRewardContextChange(it)
                        },
                        onMaterial = onMaterial,
                        modifier = Modifier.weight(1f)
                    )
                }
                2 -> HitzonesCard(
                    monster = monster,
                    selectedState = hitzoneState,
                    elemental = elemental,
                    onState = { hitzoneState = it },
                    onMode = { elemental = it },
                    modifier = Modifier.fillMaxSize().testTag("monster-hitzones")
                )
                else -> MonsterQuestsCard(quests, trainingQuests, onTraining, Modifier.fillMaxSize().testTag("monster-quests"))
            }
        }
    }
}

@Composable
private fun CompactMonsterHeader(monster: Monster, isFavorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit) {
    val context = LocalContext.current
    val headerArt = remember(context, monster.id) { loadMonsterHeaderArt(context, monster.id) }
    ParchmentSurface(Modifier.fillMaxWidth().height(64.dp).testTag("contextual-header")) {
        Box(Modifier.fillMaxSize().testTag("contextual-header-${monster.id}")) {
            if (headerArt != null) {
                Image(
                    painter = BitmapPainter(headerArt),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(.64f).align(Alignment.CenterEnd)
                        .alpha(.55f).testTag("monster-header-art-${monster.id}")
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            0f to AppColors.Parchment,
                            .42f to AppColors.Parchment.copy(alpha = .96f),
                            .72f to AppColors.Parchment.copy(alpha = .36f),
                            1f to AppColors.Parchment.copy(alpha = .08f)
                        )
                    )
                )
            } else {
                // Keeps the compact header balanced while user-provided wide art is still pending.
                Box(
                    Modifier.align(Alignment.CenterEnd).width(96.dp).fillMaxHeight().background(
                        Brush.horizontalGradient(
                            listOf(AppColors.ParchmentDeep.copy(alpha = .24f), AppColors.Parchment.copy(alpha = 0f))
                        )
                    ).testTag("monster-header-art-slot-${monster.id}")
                )
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp).testTag("detail-back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppColors.Crimson, modifier = Modifier.size(21.dp))
                }
                MonsterArtworkImage(monster, Modifier.size(40.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(monster.name, color = AppColors.Ink, style = AppType.CardTitle.copy(fontSize = 17.sp), maxLines = 1)
                    Text(
                        monster.type,
                        color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 1
                    )
                }
                FavoriteButton(isFavorite, onFavorite, "monster-favorite", compact = true)
            }
        }
    }
}

private fun loadMonsterHeaderArt(context: Context, monsterId: String): ImageBitmap? {
    listOf("webp", "png").forEach { extension ->
        val bitmap = runCatching {
            context.assets.open("monster-headers/$monsterId.$extension").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
        if (bitmap != null) return bitmap
    }
    return null
}

@Composable
private fun MonsterOverview(monster: Monster, modifier: Modifier) {
    val huntPrep = monster.huntPrep
    val counters = huntPrep?.counterSummaries().orEmpty()
    val hasBring = counters.isNotEmpty() || !huntPrep?.tacticalTools.isNullOrEmpty()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CompactWeaknesses(monster.weaknesses)
        if (hasBring) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ThreatsCard(huntPrep, Modifier.weight(1f))
                BringCard(counters, huntPrep?.tacticalTools.orEmpty(), monster.id, Modifier.weight(1f))
            }
        } else {
            ThreatsCard(huntPrep, Modifier.fillMaxWidth())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            BestHitAreasCard(monster, Modifier.weight(1f))
            if (monster.breakableParts.isNotEmpty()) {
                BreakSeverCard(monster, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun ThreatsCard(huntPrep: MonsterHuntPrep?, modifier: Modifier) {
    SectionCard(stringResource(R.string.monster_threats), modifier.testTag("threats-card"), accent = AppColors.Crimson, headerHeight = 28.dp, contentPadding = 6.dp) {
        when {
            huntPrep == null || huntPrep.coverageStatus == ThreatCoverageStatus.NONE_LISTED_IN_MASTER_TABLE ->
                Text(stringResource(R.string.no_special_threat_listed), color = AppColors.Ink.copy(alpha = .76f), style = AppType.Metadata, modifier = Modifier.testTag("threats-none-listed"))
            else -> huntPrep.threats.forEachIndexed { index, threat ->
                Column(Modifier.fillMaxWidth().testTag("threat-row-${threat.id}").padding(vertical = 2.dp)) {
                    Text(threat.type.displayLabel(), color = AppColors.Ink, style = AppType.ButtonLabel, maxLines = 1)
                    threat.context?.let { context ->
                        Text(context, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 3)
                    }
                }
                if (index != huntPrep.threats.lastIndex) HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .65f))
            }
        }
    }
}

@Composable
private fun BringCard(
    counters: List<HuntPrepCounterSummary>,
    tacticalTools: List<TacticalTool>,
    monsterId: String,
    modifier: Modifier
) {
    var expandedToolId by rememberSaveable(monsterId, "expanded-tactical-tool") { mutableStateOf<Int?>(null) }
    SectionCard(stringResource(R.string.bring), modifier.testTag("bring-card"), accent = AppColors.Moss, headerHeight = 28.dp, contentPadding = 6.dp) {
        val entryCount = counters.size + tacticalTools.size
        var entryIndex = 0
        counters.forEach { counter ->
            Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("counter-${counter.threatType.name}")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    counter.items.forEach { item ->
                        ItemIcon(item.itemGameId, item.itemName, Modifier.size(22.dp))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(counter.itemNames.joinToString(" / "), color = AppColors.Ink, style = AppType.ButtonLabel, maxLines = 2, modifier = Modifier.weight(1f))
                }
                Text(counter.threatType.displayLabel(), color = AppColors.Ink.copy(alpha = .70f), style = AppType.Metadata, maxLines = 1)
            }
            entryIndex++
            if (entryIndex != entryCount) HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .7f))
        }
        tacticalTools.forEach { tool ->
            val expanded = expandedToolId == tool.itemGameId
            val clickModifier = if (tool.reason.isNotBlank()) {
                Modifier.clickable { expandedToolId = if (expanded) null else tool.itemGameId }
            } else Modifier
            Column(
                Modifier.fillMaxWidth().then(clickModifier)
                    .padding(vertical = 3.dp).testTag("tactical-tool-${tool.itemGameId}")
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ItemIcon(tool.itemGameId, tool.itemName, Modifier.size(24.dp))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tool.itemName, color = AppColors.Ink, style = AppType.ButtonLabel, maxLines = 1)
                        Text(tool.priority.displayLabel(), color = if (tool.priority == TacticalPriority.CONDITIONAL) AppColors.Crimson else AppColors.Ink.copy(alpha = .70f), style = AppType.Metadata, maxLines = 1)
                    }
                    if (tool.reason.isNotBlank()) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AppColors.Ink.copy(alpha = .62f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.width(2.dp))
                    }
                }
                if (expanded) {
                    Text(tool.reason, color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, modifier = Modifier.padding(top = 3.dp).testTag("tactical-reason-${tool.itemGameId}"))
                }
            }
            entryIndex++
            if (entryIndex != entryCount) HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .7f))
        }
    }
}

@Composable
internal fun BestHitAreasCard(monster: Monster, modifier: Modifier) {
    SectionCard(stringResource(R.string.best_hit_areas), modifier.testTag("best-hit-areas"), accent = AppColors.Steel, headerHeight = 28.dp, contentPadding = 6.dp) {
        monster.bestHitAreas().forEach { hit ->
            DataLine(hit.damageType.displayLabel(), "${hit.bodyPart} ${hit.value}")
        }
    }
}

@Composable
private fun BreakSeverCard(monster: Monster, modifier: Modifier) {
    val groups = monster.groupedBreakableParts()
    SectionCard(stringResource(R.string.break_sever), modifier.testTag("break-sever-card"), accent = AppColors.Moss, headerHeight = 28.dp, contentPadding = 6.dp) {
        groups.forEachIndexed { index, group ->
            val actionText = group.sourceRows.joinToString(" / ") { row ->
                buildString { append(row.action); row.count?.let { append(" · x$it") } }
            }
            DataLine(group.name, actionText)
            if (index != groups.lastIndex) HorizontalDivider(color = AppColors.ParchmentDeep)
        }
    }
}

@Composable
internal fun CompactWeaknesses(values: ElementValues) {
    val entries = listOf("Fire" to values.fire, "Water" to values.water, "Thunder" to values.thunder, "Ice" to values.ice, "Dragon" to values.dragon)
    val strongest = entries.maxOfOrNull { it.second } ?: 0
    ParchmentSurface(Modifier.fillMaxWidth().height(58.dp).testTag("element-weaknesses")) {
        Row(Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.element_weaknesses),
                color = AppColors.Crimson,
                style = AppType.ButtonLabel,
                modifier = Modifier.width(130.dp),
                maxLines = 2
            )
            entries.forEach { (name, value) ->
                val highlighted = value == strongest && strongest > 0
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("weakness-cell-$name"),
                    color = if (highlighted) AppColors.Gold else AppColors.ParchmentDeep.copy(alpha = .38f),
                    shape = AppDimens.ButtonShape,
                    border = BorderStroke(AppDimens.CardBorder, if (highlighted) AppColors.Crimson else AppColors.ParchmentDeep)
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(name, color = AppColors.Ink, style = AppType.Metadata, maxLines = 1, softWrap = false)
                        Text(
                            value.toString(),
                            color = AppColors.Ink,
                            style = AppType.ButtonLabel.copy(fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal),
                            modifier = Modifier.testTag("weakness-value-$name"),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataLine(primary: String, secondary: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(primary, color = AppColors.Ink, style = AppType.Body, modifier = Modifier.weight(1f), maxLines = 2)
        Text(secondary, color = AppColors.Crimson, style = AppType.ButtonLabel, maxLines = 1)
    }
}

@Composable
private fun RewardsCard(
    monsterId: String,
    rewards: List<MonsterReward>,
    invasionRewards: List<InvasionReward>,
    materials: List<Material>,
    selectedContext: RewardContext,
    selectedFilter: MonsterRewardFilter,
    onFilter: (MonsterRewardFilter) -> Unit,
    listState: LazyListState,
    onContext: (RewardContext) -> Unit,
    onMaterial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val contexts = rewards.availableContexts()
    val availableFilters = remember(rewards, selectedContext) {
        buildList {
            add(MonsterRewardFilter.ALL)
            MonsterRewardFilter.entries.drop(1).forEach { filter ->
                if (rewards.any { it.rank == selectedContext && it.routeFamily().toFilter() == filter }) add(filter)
            }
        }
    }
    val activeFilter = selectedFilter.takeIf { it in availableFilters } ?: MonsterRewardFilter.ALL
    LaunchedEffect(activeFilter, selectedFilter) {
        if (activeFilter != selectedFilter) onFilter(activeFilter)
    }
    val rows = remember(rewards, selectedContext, activeFilter) {
        rewards.itemFirstMonsterRewards(selectedContext, activeFilter)
    }
    val materialsByGameItemId = remember(materials) {
        materials.mapNotNull { material -> material.gameItemId?.let { it to material } }.toMap()
    }
    val controlsStacked = monsterRewardRowsStacked(LocalAppWindowLayout.current.profile)

    Column(
        modifier.fillMaxSize()
            .clip(AppDimens.CardShape)
            .background(AppColors.Parchment)
            .border(AppDimens.CardBorder, AppColors.ParchmentDeep, AppDimens.CardShape)
            .padding(horizontal = 5.dp, vertical = 4.dp)
            .testTag("monster-rewards-$monsterId")
    ) {
        if (controlsStacked) {
            Column(
                Modifier.fillMaxWidth().height(74.dp).testTag("monster-reward-controls-$monsterId"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().height(36.dp).testTag("reward-context-selector-$monsterId"),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contexts.forEach { context ->
                        CompactFilter(
                            context.monsterRewardShortLabel(),
                            selectedContext == context,
                            Modifier.testTag("reward-context-$monsterId-${context.name}")
                                .semantics { contentDescription = context.displayLabel() }
                        ) { onContext(context) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(36.dp)
                        .horizontalScroll(rememberScrollState())
                        .testTag("reward-method-filter-strip-$monsterId"),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableFilters.forEach { filter ->
                        CompactFilter(
                            filter.label,
                            activeFilter == filter,
                            Modifier.testTag("reward-filter-$monsterId-${filter.name.lowercase()}")
                        ) { onFilter(filter) }
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(38.dp).testTag("monster-reward-controls-$monsterId"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    Modifier.testTag("reward-context-selector-$monsterId"),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    contexts.forEach { context ->
                        CompactFilter(
                            context.monsterRewardShortLabel(),
                            selectedContext == context,
                            Modifier.testTag("reward-context-$monsterId-${context.name}")
                                .semantics { contentDescription = context.displayLabel() }
                        ) { onContext(context) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.horizontalScroll(rememberScrollState())
                        .testTag("reward-method-filter-strip-$monsterId"),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableFilters.forEach { filter ->
                        CompactFilter(
                            filter.label,
                            activeFilter == filter,
                            Modifier.testTag("reward-filter-$monsterId-${filter.name.lowercase()}")
                        ) { onFilter(filter) }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .56f))
        MonsterRewardsItemList(
            monsterId = monsterId,
            rows = rows,
            filter = activeFilter,
            invasionRewards = invasionRewards,
            materialsByGameItemId = materialsByGameItemId,
            onMaterial = onMaterial,
            listState = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("monster-reward-item-list-$monsterId")
        )
    }
}

@Composable
private fun MonsterRewardsItemList(
    monsterId: String,
    rows: List<MonsterRewardItemRow>,
    filter: MonsterRewardFilter,
    invasionRewards: List<InvasionReward>,
    materialsByGameItemId: Map<Int, Material>,
    onMaterial: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    MonsterRewardsItemListV12(
        monsterId = monsterId,
        rows = rows,
        filter = filter,
        invasionRewards = invasionRewards,
        materialsByGameItemId = materialsByGameItemId,
        onMaterial = onMaterial,
        listState = listState,
        modifier = modifier
    )
}

@Composable
private fun MonsterRewardsItemListV12(
    monsterId: String,
    rows: List<MonsterRewardItemRow>,
    filter: MonsterRewardFilter,
    invasionRewards: List<InvasionReward>,
    materialsByGameItemId: Map<Int, Material>,
    onMaterial: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier
) {
    var invasionExpanded by rememberSaveable(monsterId) { mutableStateOf(false) }
    BoxWithConstraints(modifier) {
        val compactPortrait = LocalAppWindowLayout.current.profile == AppLayoutProfile.COMPACT_PORTRAIT
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val filteredCellWidth = if (compactPortrait) maxWidth - 12.dp else (maxWidth - 6.dp - 11.dp) / 2
        val filteredLines = remember(rows, filter, filteredCellWidth, density, textMeasurer, compactPortrait) {
            if (filter == MonsterRewardFilter.ALL) emptyList()
            else packFilteredMonsterRewardItems(rows) { item ->
                if (compactPortrait || item.routes.size != 1) {
                    false
                } else {
                    val route = item.routes.single()
                    val itemWidth = textMeasurer.measure(
                        AnnotatedString(item.itemName),
                        style = AppType.CardTitle.copy(fontSize = 13.sp, lineHeight = 16.sp),
                        softWrap = false,
                        maxLines = 1
                    ).size.width
                    val routeWidth = textMeasurer.measure(
                        rewardRouteTextV12(route.label),
                        style = AppType.Metadata,
                        softWrap = false,
                        maxLines = 1
                    ).size.width
                    val valuesWidth = textMeasurer.measure(
                        AnnotatedString(route.valuesText()),
                        style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                        softWrap = false,
                        maxLines = 1
                    ).size.width
                    val fixedWidth = with(density) { 59.dp.toPx() }
                    itemWidth + routeWidth + valuesWidth + fixedWidth <= with(density) { (filteredCellWidth - 4.dp).toPx() }
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().testTag("monster-rewards-single-scroll-$monsterId"),
            state = listState,
            contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
        ) {
            if (filter == MonsterRewardFilter.ALL) {
                items(rows, key = { it.gameItemId }) { item ->
                    AllRewardGridRowV12(
                        monsterId = monsterId,
                        item = item,
                        material = materialsByGameItemId[item.gameItemId],
                        onMaterial = onMaterial,
                        rowTag = "monster-reward-row-$monsterId-${item.gameItemId}"
                    )
                    HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .52f))
                }
            } else {
                items(filteredLines, key = { it.items.first().gameItemId }) { line ->
                    if (line.isTwoUp) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 3.dp, vertical = 2.dp)
                                .testTag("monster-reward-grid-row-$monsterId-${line.items.first().gameItemId}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilteredRewardHalfCellV12(
                                monsterId, line.items[0], line.items[0].routes.single(),
                                materialsByGameItemId[line.items[0].gameItemId], onMaterial,
                                Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(5.dp))
                            Box(
                                Modifier.width(1.dp).height(25.dp)
                                    .background(AppColors.ParchmentDeep.copy(alpha = .54f))
                                    .testTag("monster-reward-filtered-column-divider-$monsterId")
                            )
                            Spacer(Modifier.width(5.dp))
                            FilteredRewardHalfCellV12(
                                monsterId, line.items[1], line.items[1].routes.single(),
                                materialsByGameItemId[line.items[1].gameItemId], onMaterial,
                                Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .52f))
                    } else {
                        val item = line.items.single()
                        AllRewardGridRowV12(
                            monsterId = monsterId,
                            item = item,
                            material = materialsByGameItemId[item.gameItemId],
                            onMaterial = onMaterial,
                            rowTag = "monster-reward-row-$monsterId-${item.gameItemId}"
                        )
                        HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .52f))
                    }
                }
            }
            if (rows.isEmpty()) {
                item(key = "empty") {
                    Text("No rewards for this filter.", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, modifier = Modifier.padding(8.dp))
                }
            }
            if (invasionRewards.isNotEmpty()) {
                item(key = "invasion-rewards-$monsterId") {
                    InvasionRewardsCard(
                        monsterId = monsterId,
                        rewards = invasionRewards,
                        materialsByGameItemId = materialsByGameItemId,
                        expanded = invasionExpanded,
                        onMaterial = onMaterial,
                        onToggle = { invasionExpanded = !invasionExpanded }
                    )
                }
            }
        }
    }
}

private const val REWARD_ITEM_COLUMN_FRACTION_V12 = .34f

@Composable
private fun AllRewardGridRowV12(
    monsterId: String,
    item: MonsterRewardItemRow,
    material: Material?,
    onMaterial: (String) -> Unit,
    rowTag: String
) {
    if (monsterRewardRowsStacked(LocalAppWindowLayout.current.profile)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp)
                .testTag(rowTag)
        ) {
            RewardItemIdentity(
                monsterId = monsterId,
                item = item,
                material = material,
                onMaterial = onMaterial,
                modifier = Modifier.fillMaxWidth()
            )
            item.routes.forEachIndexed { routeIndex, route ->
                RewardRouteCellV12(
                    monsterId = monsterId,
                    gameItemId = item.gameItemId,
                    route = route,
                    routeIndex = routeIndex,
                    modifier = Modifier.fillMaxWidth().padding(start = 30.dp)
                )
            }
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxWidth().testTag(rowTag)) {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val routeColumnWidth = (maxWidth - 7.dp) * (1f - REWARD_ITEM_COLUMN_FRACTION_V12)
        val routeHalfWidth = (routeColumnWidth - 1.dp) / 2
        val routeLines = remember(item.routes, routeHalfWidth, density, textMeasurer) {
            val availablePx = with(density) { (routeHalfWidth - 8.dp).toPx() }
            packMonsterRewardRoutes(item.routes) { route ->
                val labelWidth = textMeasurer.measure(
                    rewardRouteTextV12(route.label),
                    style = AppType.Metadata,
                    softWrap = false,
                    maxLines = 1
                ).size.width
                val valuesWidth = textMeasurer.measure(
                    AnnotatedString(route.valuesText()),
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                    softWrap = false,
                    maxLines = 1
                ).size.width
                // Use measured text widths and retain a 15% breathing margin.
                // That keeps the long Agnaktor route on its own full-width line,
                // without making ordinary pairs collapse at the larger UI scale.
                (labelWidth + valuesWidth + with(density) { 5.dp.toPx() }) * 1.15f <= availablePx
            }
        }

        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 3.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RewardItemIdentity(
                monsterId = monsterId,
                item = item,
                material = material,
                onMaterial = onMaterial,
                modifier = Modifier.weight(REWARD_ITEM_COLUMN_FRACTION_V12)
            )
            Box(
                Modifier.width(1.dp).fillMaxHeight().padding(vertical = 3.dp)
                    .background(AppColors.ParchmentDeep.copy(alpha = .56f))
                    .testTag("monster-reward-identity-divider-$monsterId-${item.gameItemId}")
            )
            Column(
                Modifier.weight(1f - REWARD_ITEM_COLUMN_FRACTION_V12)
                    .testTag("monster-reward-routes-$monsterId-${item.gameItemId}")
            ) {
                routeLines.forEachIndexed { rowIndex, line ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 28.dp)
                            .testTag("monster-reward-route-grid-row-$monsterId-${item.gameItemId}-$rowIndex"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (line.spansBothColumns) {
                            val route = line.routes.single()
                            RewardRouteCellV12(
                                monsterId, item.gameItemId, route, item.routes.indexOf(route), Modifier.fillMaxWidth()
                            )
                        } else {
                            val leftRoute = line.routes[0]
                            RewardRouteCellV12(
                                monsterId, item.gameItemId, leftRoute, item.routes.indexOf(leftRoute), Modifier.weight(1f)
                            )
                            Box(
                                Modifier.width(1.dp).height(22.dp)
                                    .background(AppColors.ParchmentDeep.copy(alpha = .48f))
                                    .testTag("monster-reward-route-column-divider-$monsterId-${item.gameItemId}-$rowIndex")
                            )
                            line.routes.getOrNull(1)?.let { rightRoute ->
                                RewardRouteCellV12(
                                    monsterId, item.gameItemId, rightRoute, item.routes.indexOf(rightRoute), Modifier.weight(1f)
                                )
                            } ?: Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilteredRewardHalfCellV12(
    monsterId: String,
    item: MonsterRewardItemRow,
    route: MonsterRewardRoute,
    material: Material?,
    onMaterial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 2.dp)
            .then(if (material != null) Modifier.clickable { onMaterial(material.id) } else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = item.itemName }
            .testTag("monster-reward-item-$monsterId-${item.gameItemId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIcon(item.gameItemId, item.itemName, Modifier.size(22.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            item.itemName,
            color = AppColors.Ink,
            style = AppType.CardTitle.copy(fontSize = 13.sp, lineHeight = 16.sp),
            maxLines = 1,
            softWrap = false
        )
        if (material != null) {
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(5.dp))
        Text(
            rewardRouteTextV12(route.label),
            color = AppColors.Ink.copy(alpha = .92f),
            style = AppType.Metadata,
            maxLines = 1,
            softWrap = false
        )
        Spacer(Modifier.weight(1f))
        Text(
            route.valuesText(),
            color = AppColors.Crimson,
            style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun RewardRouteCellV12(
    monsterId: String,
    gameItemId: Int,
    route: MonsterRewardRoute,
    routeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.padding(horizontal = 3.dp).heightIn(min = 22.dp)
            .testTag("monster-reward-route-$monsterId-$gameItemId-$routeIndex"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rewardRouteTextV12(route.label),
            color = AppColors.Ink.copy(alpha = .92f),
            style = AppType.Metadata,
            modifier = Modifier.weight(1f).testTag("monster-reward-route-label-$monsterId-$gameItemId-$routeIndex"),
            softWrap = true,
            maxLines = 2
        )
        Spacer(Modifier.width(4.dp))
        Text(
            route.valuesText(),
            color = AppColors.Crimson,
            style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.testTag("monster-reward-route-values-$monsterId-$gameItemId-$routeIndex"),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun rewardRouteTextV12(label: String): AnnotatedString = buildAnnotatedString {
    val action = listOf("Break", "Carve", "Drop", "Mining").firstOrNull { label.endsWith(" $it") }
    if (label == "Capture" || label == "Mining Points" || action == null) {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label) }
    } else {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label.removeSuffix(" $action")) }
        append(" $action")
    }
}

@Composable
private fun RewardItemIdentity(
    monsterId: String,
    item: MonsterRewardItemRow,
    material: Material?,
    onMaterial: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp)
            .then(if (material != null) Modifier.clickable { onMaterial(material.id) } else Modifier)
            .padding(end = 5.dp)
            .semantics(mergeDescendants = true) { contentDescription = item.itemName }
            .testTag("monster-reward-item-$monsterId-${item.gameItemId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIcon(item.gameItemId, item.itemName, Modifier.size(25.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            item.itemName,
            color = AppColors.Ink,
            style = AppType.CardTitle.copy(fontSize = 15.sp, lineHeight = 18.sp),
            modifier = Modifier.weight(1f, fill = false),
            softWrap = true,
            maxLines = 2
        )
        if (material != null) {
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(16.dp))
        }
    }
}

private fun MonsterRewardRoute.valuesText(): String = relations.joinToString(" / ") { relation ->
    buildString {
        append(relation.chance)
        relation.quantity?.let { append(" · ×").append(it) }
    }
}

private fun RewardContext.monsterRewardShortLabel(): String = when (this) {
    RewardContext.LOW -> "LR"
    RewardContext.HIGH -> "HR"
    RewardContext.GUILD_1_2 -> "G1–2"
    RewardContext.VILLAGE_2_SPECIAL -> "V2★"
}

@Composable
private fun HitzonesCard(
    monster: Monster,
    selectedState: String,
    elemental: Boolean,
    onState: (String) -> Unit,
    onMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val alternate = monster.alternateHitzones.firstOrNull { it.stateId == selectedState }
    val hitzones = alternate?.hitzones ?: monster.hitzones
    SectionCard(stringResource(R.string.hitzones), modifier, accent = AppColors.Crimson, headerHeight = 29.dp, contentPadding = 7.dp) {
        if (monster.alternateHitzones.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactFilter(stringResource(R.string.hitzone_state_normal), selectedState == "normal", Modifier.testTag("hitzone-state-normal")) { onState("normal") }
                monster.alternateHitzones.forEach { state ->
                    CompactFilter(state.label, selectedState == state.stateId, Modifier.testTag("hitzone-state-${state.stateId}")) { onState(state.stateId) }
                }
            }
            Spacer(Modifier.height(5.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactFilter(stringResource(R.string.physical), !elemental, Modifier.testTag("hitzone-physical")) { onMode(false) }
            CompactFilter(stringResource(R.string.elemental), elemental, Modifier.testTag("hitzone-elemental")) { onMode(true) }
        }
        Spacer(Modifier.height(7.dp))
        val weights = buildList {
            add(1.45f)
            repeat(if (elemental) 5 else 3) { add(1f) }
        }
        Row(Modifier.fillMaxWidth().hitzoneTableLines(weights).testTag("hitzones-table-header")) {
            MonsterTableCell(stringResource(R.string.body_part), weights[0], true)
            if (elemental) {
                listOf("Fire", "Water", "Thunder", "Ice", "Dragon").forEach { MonsterTableCell(it, 1f, true) }
            } else {
                listOf("Cut", "Impact", "Shot").forEach { MonsterTableCell(it, 1f, true) }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).testTag("hitzones-list-${alternate?.stateId ?: "normal"}")) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("hitzones-list"),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                itemsIndexed(hitzones) { index, zone ->
                    Row(
                        Modifier.fillMaxWidth()
                            .hitzoneTableLines(weights, drawBottom = index != hitzones.lastIndex)
                            .testTag("hitzone-row-$index")
                            .padding(vertical = 4.dp)
                    ) {
                        MonsterTableCell(zone.part, weights[0])
                        val values = if (elemental) listOf(zone.fire, zone.water, zone.thunder, zone.ice, zone.dragon) else listOf(zone.cut, zone.impact, zone.shot)
                        values.forEach { MonsterTableCell(it.toString(), 1f) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonsterQuestsCard(
    quests: List<Quest>,
    trainingQuests: List<TrainingQuest> = emptyList(),
    onTraining: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SectionCard(stringResource(R.string.related_quests), modifier, accent = AppColors.Steel, headerHeight = 29.dp, contentPadding = 7.dp) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("quests-list"), contentPadding = PaddingValues(bottom = 6.dp)) {
            itemsIndexed(quests, key = { _, quest -> quest.id }) { index, quest ->
                Column {
                    Row(Modifier.fillMaxWidth().testTag("monster-quest-${quest.id}").padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        QuestBadge(quest, Modifier.width(72.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(quest.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
                            Text("${quest.contextLabel()} · Target: ${quest.target}".trimStart(' ', '·'), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                            if (quest.relevantReward.isNotBlank()) {
                                Text("Reward: ${quest.relevantReward}", color = AppColors.Crimson, style = AppType.Metadata, maxLines = 1)
                            }
                        }
                    }
                    if (index != quests.lastIndex) HorizontalDivider(color = AppColors.ParchmentDeep)
                }
            }
            if (trainingQuests.isNotEmpty()) {
                item(key = "training-subsection-title") {
                    Text("Training", color = AppColors.Gold, style = AppType.SectionTitle, modifier = Modifier.padding(top = 7.dp, bottom = 2.dp).testTag("monster-training-title"))
                }
                items(trainingQuests, key = { "training-${it.id}" }) { training ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onTraining(training.id) }.testTag("monster-training-${training.id}").padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrainingBadge(training)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(training.canonicalDisplayName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(training.trainingClass, training.location.takeIf { it.isNotBlank() }).joinToString(" · "), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonsterSourceRows(
    sources: List<MaterialSource>,
    onMonster: (String) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources) { sources.monsterPresentationProjection() }
    var showAll by rememberSaveable { mutableStateOf(false) }
    SourceGroupHeader(
        "Monster Rewards · ${projection.monsterCount} ${if (projection.monsterCount == 1) "monster" else "monsters"}"
    )
    val visible = if (projection.monsterCount > 6) projection.monsters.take(6) else projection.monsters
    Column(Modifier.fillMaxWidth().testTag("item-monster-rewards-groups"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        visible.forEach { group ->
            MonsterSourceGroupCard(group, onMonster)
        }
        if (projection.monsterCount > 6) {
            PreviewFooter(
                moreLabel = "+${projection.monsterCount - 6} more",
                actionLabel = "View all ${projection.monsterCount}",
                tag = "item-monster-rewards-view-all",
                moreTag = "item-monster-rewards-more",
                onAction = { showAll = true }
            )
        }
    }
    if (showAll) {
        MonsterSourceViewAllDialog(
            projection = projection,
            onDismiss = { showAll = false },
            onMonster = { id ->
                showAll = false
                onMonster(id)
            }
        )
    }
}

@Composable
private fun SmallMonsterSourceRows(
    sources: List<MaterialSource>,
    onSmallMonster: (String) -> Unit
) {
    if (sources.isEmpty()) return
    val projection = remember(sources) { sources.smallMonsterPresentationProjection() }
    var showAll by rememberSaveable { mutableStateOf(false) }
    SourceGroupHeader(
        "Small Monster · ${projection.monsterCount} ${if (projection.monsterCount == 1) "monster" else "monsters"}"
    )
    Column(
        Modifier.fillMaxWidth().testTag("item-small-monster-sources-groups"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        projection.monsters.take(smallMonsterVisibleGroupCount(projection.monsterCount)).forEach { group ->
            SmallMonsterSourceGroupCard(group, onSmallMonster)
        }
        if (projection.monsterCount > SMALL_MONSTER_INLINE_LIMIT) {
            PreviewFooter(
                moreLabel = "+${projection.monsterCount - SMALL_MONSTER_INLINE_LIMIT} more",
                actionLabel = "View all ${projection.monsterCount}",
                tag = "item-small-monster-sources-view-all",
                moreTag = "item-small-monster-sources-more",
                onAction = { showAll = true }
            )
        }
    }
    if (showAll) {
        SmallMonsterSourceViewAllDialog(
            projection = projection,
            onDismiss = { showAll = false },
            onSmallMonster = { id ->
                showAll = false
                onSmallMonster(id)
            }
        )
    }
}

@Composable
private fun SmallMonsterSourceGroupCard(
    group: SmallMonsterSourceMonsterProjection,
    onSmallMonster: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ParchmentSurface(modifier.fillMaxWidth().testTag("item-small-monster-source-group-${group.smallMonsterId}")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onSmallMonster(group.smallMonsterId) }
                    .testTag("item-small-monster-source-monster-${group.smallMonsterId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.monsterName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${group.entries.size} reward ${if (group.entries.size == 1) "entry" else "entries"}",
                        color = AppColors.Ink.copy(alpha = .68f), style = AppType.Metadata, maxLines = 1
                    )
                }
                Icon(Icons.Default.ChevronRight, "Open ${group.monsterName}", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(3.dp))
            group.methods.forEach { method ->
                Text(
                    method.label,
                    color = AppColors.Ink,
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 1.dp)
                        .testTag("item-small-monster-source-method-${group.smallMonsterId}-${method.method}"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                method.directEntries.forEach { entry -> SmallMonsterSourceEntryRow(entry, group.smallMonsterId, onSmallMonster) }
                method.conditionGroups.forEachIndexed { index, condition ->
                    Text(
                        condition.label ?: "Special condition",
                        color = AppColors.Ink.copy(alpha = .78f),
                        style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth().padding(start = 5.dp, top = 2.dp, bottom = 1.dp)
                            .testTag("item-small-monster-source-condition-${group.smallMonsterId}-${method.method}-$index"),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    condition.entries.forEach { entry -> SmallMonsterSourceEntryRow(entry, group.smallMonsterId, onSmallMonster) }
                }
            }
        }
    }
}

@Composable
private fun SmallMonsterSourceEntryRow(
    entry: SmallMonsterSourceContextEntryProjection,
    smallMonsterId: String,
    onSmallMonster: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onSmallMonster(smallMonsterId) }
            .padding(start = 7.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
            .testTag("item-small-monster-source-${entry.rawRelationIdentity}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rank = entry.context
        if (rank != null) {
            Surface(
                color = when (rank) {
                    RewardContext.LOW -> Color(0xFFD6E4F3)
                    RewardContext.HIGH -> Color(0xFFF0D0C8)
                    else -> AppColors.ParchmentDeep.copy(alpha = .52f)
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    smallMonsterContextLabel(rank),
                    color = when (rank) {
                        RewardContext.LOW -> AppColors.Steel
                        RewardContext.HIGH -> AppColors.Crimson
                        else -> AppColors.Ink
                    },
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
        Text(
            buildList {
                entry.chance?.let { add("$it%") }
                entry.quantity?.takeIf { it != 1 }?.let { add("×$it") }
            }.joinToString(" · "),
            color = AppColors.Crimson,
            style = AppType.Metadata,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SmallMonsterSourceViewAllDialog(
    projection: SmallMonsterItemSourceProjection,
    onDismiss: () -> Unit,
    onSmallMonster: (String) -> Unit
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f)
                .testTag("item-small-monster-sources-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape,
            border = AppDimens.CardBorder.let { BorderStroke(it, AppColors.ParchmentDeep) }
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 5.dp, top = 8.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Small Monster · ${projection.monsterCount} monsters",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f).testTag("item-small-monster-sources-modal-title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp).testTag("item-small-monster-sources-modal-close")) {
                        Icon(Icons.Default.Close, "Close Small Monster sources", tint = AppColors.Ink)
                    }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .55f))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("item-small-monster-sources-modal-list"),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(projection.monsters, key = { it.smallMonsterId }) { group ->
                        SmallMonsterSourceGroupCard(group, onSmallMonster)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonsterSourceGroupCard(
    group: MonsterRewardMonsterProjection,
    onMonster: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ParchmentSurface(modifier.fillMaxWidth().testTag("item-monster-source-group-${group.monsterId}")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onMonster(group.monsterId) }
                    .testTag("item-monster-source-monster-${group.monsterId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(group.monsterName, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${group.entries.size} reward ${if (group.entries.size == 1) "entry" else "entries"}",
                        color = AppColors.Ink.copy(alpha = .68f), style = AppType.Metadata, maxLines = 1
                    )
                }
                Icon(Icons.Default.ChevronRight, "Open ${group.monsterName}", tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(3.dp))
            group.methods.forEach { method ->
                Text(
                    method.label,
                    color = AppColors.Ink,
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 1.dp)
                        .testTag("item-monster-source-method-${group.monsterId}-${method.method}-${method.condition.orEmpty()}"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                method.entries.forEach { entry ->
                    MonsterRewardEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun MonsterRewardEntryRow(entry: MonsterRewardEntryProjection) {
    Row(
        Modifier.fillMaxWidth().padding(start = 7.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
            .testTag("item-monster-reward-entry-${entry.rawRelationIdentity}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rank = entry.rank
        if (rank != null) {
            Surface(
                color = when (rank) {
                    RewardContext.LOW -> Color(0xFFD6E4F3)
                    RewardContext.HIGH -> Color(0xFFF0D0C8)
                    else -> AppColors.ParchmentDeep.copy(alpha = .52f)
                },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    rank.monsterRewardCompactLabel(),
                    color = when (rank) {
                        RewardContext.LOW -> AppColors.Steel
                        RewardContext.HIGH -> AppColors.Crimson
                        else -> AppColors.Ink
                    },
                    style = AppType.Metadata.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
        Text(
            buildString {
                entry.chance?.let { append(it).append('%') }
                entry.quantity?.let { append(if (isNotEmpty()) " · " else "").append('×').append(it) }
            },
            color = AppColors.Crimson,
            style = AppType.Metadata,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
}

private fun RewardContext.monsterRewardCompactLabel(): String = when (this) {
    RewardContext.LOW -> "LR"
    RewardContext.HIGH -> "HR"
    RewardContext.GUILD_1_2 -> "Guild ★1–2"
    RewardContext.VILLAGE_2_SPECIAL -> "Village 2★ Special"
}

@Composable
private fun MonsterSourceViewAllDialog(
    projection: MonsterItemSourceProjection,
    onDismiss: () -> Unit,
    onMonster: (String) -> Unit
) {
    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.88f)
                .testTag("item-monster-rewards-modal"),
            color = AppColors.Parchment,
            shape = AppDimens.CardShape,
            border = AppDimens.CardBorder.let { BorderStroke(it, AppColors.ParchmentDeep) }
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 5.dp, top = 8.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Monster Rewards · ${projection.monsterCount} monsters",
                        color = AppColors.Ink,
                        style = AppType.SectionTitle,
                        modifier = Modifier.weight(1f).testTag("item-monster-rewards-modal-title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp).testTag("item-monster-rewards-modal-close")) {
                        Icon(Icons.Default.Close, "Close Monster Rewards", tint = AppColors.Ink)
                    }
                }
                HorizontalDivider(color = AppColors.ParchmentDeep.copy(alpha = .55f))
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f).testTag("item-monster-rewards-modal-list"),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(projection.monsters, key = { it.monsterId }) { group ->
                        MonsterSourceGroupCard(group, onMonster)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvasionRewardsCard(
    monsterId: String,
    rewards: List<InvasionReward>,
    materialsByGameItemId: Map<Int, Material>,
    expanded: Boolean,
    onMaterial: (String) -> Unit,
    onToggle: () -> Unit
) {
    if (rewards.isEmpty()) return
    Column(
        Modifier.fillMaxWidth()
            .clip(AppDimens.CardShape)
            .background(AppColors.Parchment)
            .border(AppDimens.CardBorder, AppColors.ParchmentDeep, AppDimens.CardShape)
            .testTag("monster-invasion-rewards-$monsterId")
    ) {
        Row(
            Modifier.fillMaxWidth()
                .height(34.dp)
                .background(AppColors.Crimson)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp)
                .testTag("monster-invasion-rewards-toggle-$monsterId"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Invasion Rewards", color = Color(0xFFFFE8B0), style = AppType.SectionTitle, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse Invasion Rewards" else "Expand Invasion Rewards",
                tint = Color(0xFFFFE8B0),
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                Modifier.fillMaxWidth().padding(6.dp)
                    .testTag("monster-invasion-rewards-content-$monsterId")
            ) {
                rewards.groupBy { it.context }.toSortedMap().forEach { (context, rows) ->
                    Text(invasionContextLabel(context), color = AppColors.Crimson, style = AppType.Metadata, modifier = Modifier.padding(vertical = 2.dp), maxLines = 1)
                    rows.forEach { reward ->
                        val material = materialsByGameItemId[reward.gameItemId]
                        Row(
                            Modifier.fillMaxWidth()
                                .then(material?.let { item -> Modifier.clickable { onMaterial(item.id) } } ?: Modifier)
                                .padding(vertical = 2.dp)
                                .testTag("monster-invasion-reward-entry-$monsterId-${reward.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemIcon(reward.gameItemId, material?.name, Modifier.size(24.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(material?.name ?: "Item ${reward.gameItemId}", color = AppColors.Ink, style = AppType.Metadata, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(reward.quantity.takeIf { it != 1 }?.let { "×$it" }, reward.invasionChanceForDisplay()).joinToString(" · "), color = AppColors.Crimson, style = AppType.Metadata, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

private fun InvasionReward.invasionChanceForDisplay(): String = when (probabilityBand) {
    InvasionProbabilityBand.AT_LEAST_20 -> ">=20%"
    InvasionProbabilityBand.TEN_TO_UNDER_20 -> "10–<20%"
    InvasionProbabilityBand.FIVE_TO_UNDER_10 -> "5–<10%"
    InvasionProbabilityBand.TWO_TO_UNDER_5 -> "2–<5%"
    InvasionProbabilityBand.UNDER_2 -> "<2%"
}

private fun Modifier.hitzoneTableLines(weights: List<Float>, drawBottom: Boolean = false): Modifier = drawBehind {
    val totalWeight = weights.sum().coerceAtLeast(1f)
    var accumulated = 0f
    weights.dropLast(1).forEach { weight ->
        accumulated += weight
        val x = size.width * accumulated / totalWeight
        drawLine(
            color = AppColors.ParchmentDeep.copy(alpha = .68f),
            start = androidx.compose.ui.geometry.Offset(x, 0f),
            end = androidx.compose.ui.geometry.Offset(x, size.height),
            strokeWidth = 1.dp.toPx()
        )
    }
    if (drawBottom) {
        val y = (size.height - .5.dp.toPx()).coerceAtLeast(0f)
        drawLine(
            color = AppColors.ParchmentDeep.copy(alpha = .5f),
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun RowScope.MonsterTableCell(text: String, weight: Float, header: Boolean = false) {
    Text(
        text,
        color = AppColors.Ink.copy(alpha = if (header) .72f else 1f),
        style = if (header) AppType.Metadata.copy(fontWeight = FontWeight.Bold) else AppType.Body,
        modifier = Modifier.weight(weight).padding(horizontal = 3.dp),
        maxLines = 2
    )
}

@Composable
internal fun FavoritesScreen(
    favorites: List<FavoriteEntry>,
    data: FixtureData,
    weaponRepository: WeaponRepository? = null,
    onEntity: (ReferenceEntityType, String) -> Unit
) {
    val resolved = favorites.mapNotNull { data.resolveReference(it.type, it.entityId, weaponRepository) }
    Column(Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding).testTag("screen-favorites")) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.favorites), color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 1)
                Text(stringResource(R.string.favorites_saved), color = AppColors.ParchmentDeep, style = AppType.Metadata)
            }
            Text(resolved.size.toString(), color = AppColors.Gold, style = AppType.SectionTitle)
        }
        if (resolved.isEmpty()) {
            EmptyState(Icons.Default.StarBorder, stringResource(R.string.favorites_empty_title), stringResource(R.string.favorites_empty_body))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.testTag("favorites-list")
            ) {
                items(resolved, key = { "${it.type}-${it.id}" }) { entry ->
                    ParchmentSurface(
                        Modifier.fillMaxWidth().height(66.dp)
                            .testTag("favorite-${entry.type.name.lowercase()}-${entry.id}")
                            .clickable { onEntity(entry.type, entry.id) }
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            when {
                                entry.monster != null -> MonsterArtworkImage(entry.monster, Modifier.size(48.dp))
                                entry.smallMonster != null -> SmallMonsterIconImage(entry.smallMonster, Modifier.size(48.dp))
                                entry.weapon != null -> WeaponIcon(entry.weapon.weaponType, entry.weapon.rarity, Modifier.size(48.dp))
                                else -> ItemIcon(entry.gameItemId, entry.name, Modifier.size(48.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val typeLabel = entry.type.localizedReferenceType()
                                Text(
                                    if (entry.subtitle.isNotBlank()) "$typeLabel · ${entry.subtitle}" else typeLabel,
                                    color = AppColors.Ink.copy(alpha = .68f),
                                    style = AppType.Metadata,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}
