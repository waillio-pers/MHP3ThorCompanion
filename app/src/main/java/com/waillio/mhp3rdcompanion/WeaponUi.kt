package com.waillio.mhp3rdcompanion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.waillio.mhp3rdcompanion.data.Material
import com.waillio.mhp3rdcompanion.data.BowChargeAvailability
import com.waillio.mhp3rdcompanion.data.BowCoatingSupportLevel
import com.waillio.mhp3rdcompanion.data.BowCoatingType
import com.waillio.mhp3rdcompanion.data.BowMechanics
import com.waillio.mhp3rdcompanion.data.GunlanceMechanics
import com.waillio.mhp3rdcompanion.data.HornNote
import com.waillio.mhp3rdcompanion.data.HuntingHornMechanics
import com.waillio.mhp3rdcompanion.data.Weapon
import com.waillio.mhp3rdcompanion.data.WeaponRecipe
import com.waillio.mhp3rdcompanion.data.WeaponSharpness
import com.waillio.mhp3rdcompanion.data.WeaponSpecialType
import com.waillio.mhp3rdcompanion.data.SwitchAxeMechanics
import com.waillio.mhp3rdcompanion.data.LightBowgunMechanics
import com.waillio.mhp3rdcompanion.data.HeavyBowgunMechanics
import com.waillio.mhp3rdcompanion.data.BowgunAmmoLoad
import com.waillio.mhp3rdcompanion.data.BowgunAmmoFamily
import com.waillio.mhp3rdcompanion.data.BowgunRound
import com.waillio.mhp3rdcompanion.data.BowgunAmmoSourceShape
import com.waillio.mhp3rdcompanion.data.canonicalWeaponTypeLabel

internal data class WeaponTreeRow(val weapon: Weapon, val depth: Int, val contextOnly: Boolean = false)

/** The shared fixed scale; source units are not per-row normalized. */
internal const val WEAPON_SHARPNESS_CAPACITY = 89
internal const val GREAT_SWORD_SHARPNESS_CAPACITY = WEAPON_SHARPNESS_CAPACITY

internal fun sharpnessFillFraction(sharpness: WeaponSharpness): Float =
    (sharpness.totalUnits().coerceAtLeast(0).toFloat() / GREAT_SWORD_SHARPNESS_CAPACITY).coerceIn(0f, 1f)

private fun WeaponSharpness.segments(): List<Pair<Int, Color>> = listOf(
    red to SharpnessColors.Red,
    orange to SharpnessColors.Orange,
    yellow to SharpnessColors.Yellow,
    green to SharpnessColors.Green,
    blue to SharpnessColors.Blue,
    white to SharpnessColors.White
)

internal fun initialExpandedIds(repository: WeaponRepository): Set<String> =
    initialExpandedIds(repository.catalog("GREAT_SWORD"))

internal fun initialExpandedIds(repository: WeaponCatalog): Set<String> {
    if (repository.weaponType == "LONG_SWORD") return emptySet()
    val result = linkedSetOf<String>()
    var current = repository.validation.rootIds.singleOrNull()
    while (current != null) {
        result += current
        val children = repository.childrenByWeaponId[current].orEmpty()
        if (children.size != 1) break
        current = children.single().id
    }
    return result
}

internal fun autoExpandedIds(repository: WeaponRepository, weaponId: String): Set<String> =
    autoExpandedIds(repository.catalog("GREAT_SWORD"), weaponId)

internal fun autoExpandedIds(repository: WeaponCatalog, weaponId: String): Set<String> {
    val result = linkedSetOf(weaponId)
    var current = weaponId
    while (true) {
        val children = repository.childrenByWeaponId[current].orEmpty()
        if (children.size != 1) break
        current = children.single().id
        result += current
    }
    return result
}

internal fun toggleExpansion(
    repository: WeaponRepository,
    expandedIds: Set<String>,
    weaponId: String
): Set<String> = toggleExpansion(repository.catalog("GREAT_SWORD"), expandedIds, weaponId)

internal fun toggleExpansion(
    repository: WeaponCatalog,
    expandedIds: Set<String>,
    weaponId: String
): Set<String> {
    if (weaponId in expandedIds) {
        val hidden = linkedSetOf<String>()
        fun collect(id: String) {
            hidden += id
            repository.childrenByWeaponId[id].orEmpty().forEach { collect(it.id) }
        }
        repository.childrenByWeaponId[weaponId].orEmpty().forEach { collect(it.id) }
        return expandedIds - weaponId - hidden
    }
    return expandedIds + autoExpandedIds(repository, weaponId)
}

internal fun visibleTree(repository: WeaponRepository, expandedIds: Set<String>): List<WeaponTreeRow> =
    visibleTree(repository.catalog("GREAT_SWORD"), expandedIds)

internal fun visibleTree(repository: WeaponCatalog, expandedIds: Set<String>): List<WeaponTreeRow> {
    val result = mutableListOf<WeaponTreeRow>()
    fun visit(weaponId: String, depth: Int) {
        val weapon = repository.weaponById[weaponId] ?: return
        result += WeaponTreeRow(weapon, depth)
        if (weaponId in expandedIds) {
            repository.childrenByWeaponId[weaponId].orEmpty().forEach { visit(it.id, depth + 1) }
        }
    }
    repository.validation.rootIds.sortedBy { repository.weaponById[it]?.sourceOrdinal ?: Int.MAX_VALUE }.forEach { visit(it, 0) }
    return result
}

/** Keeps only exact matches and real ancestors needed to show their tree path. */
internal fun filteredWeaponTree(catalog: WeaponCatalog, matchingIds: Set<String>): List<WeaponTreeRow> {
    val hasMatchInBranch = mutableMapOf<String, Boolean>()
    fun branchMatches(id: String): Boolean = hasMatchInBranch.getOrPut(id) {
        id in matchingIds || catalog.childrenByWeaponId[id].orEmpty().any { branchMatches(it.id) }
    }

    val result = mutableListOf<WeaponTreeRow>()
    fun append(id: String, depth: Int) {
        if (!branchMatches(id)) return
        val weapon = catalog.weaponById[id] ?: return
        result += WeaponTreeRow(weapon, depth, contextOnly = id !in matchingIds)
        catalog.childrenByWeaponId[id].orEmpty().forEach { append(it.id, depth + 1) }
    }
    catalog.validation.rootIds
        .sortedBy { catalog.weaponById[it]?.sourceOrdinal ?: Int.MAX_VALUE }
        .forEach { append(it, 0) }
    return result
}

/** Helper for callers that need the complete production graph flattened. */
internal fun flattenTree(repository: WeaponRepository): List<WeaponTreeRow> =
    visibleTree(repository.catalog("GREAT_SWORD"), repository.weaponById.keys)

internal fun flattenTree(repository: WeaponCatalog): List<WeaponTreeRow> =
    visibleTree(repository, repository.weaponById.keys)

internal fun filterWeapons(weapons: List<Weapon>, query: String): List<Weapon> {
    val normalized = query.trim()
    return weapons.filter { normalized.isBlank() || it.name.contains(normalized, ignoreCase = true) }
        .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
}

@Composable
internal fun WeaponsScreen(
    repository: WeaponRepository,
    onItem: (Material) -> Unit,
    screenState: WeaponScreenState = remember { WeaponScreenState() },
    isFavorite: (String) -> Boolean = { false },
    onFavorite: (String) -> Unit = {},
    onWeaponOpened: (String) -> Unit = {},
    onTypeSelected: ((String) -> Unit)? = null,
    forcedType: String? = null,
    onBackFromForcedType: () -> Unit = {}
) {
    val activeType = forcedType ?: screenState.selectedType
    BackHandler(enabled = activeType != null) {
        if (forcedType != null) onBackFromForcedType() else screenState.selectedType = null
    }
    if (activeType != null) {
        val type = activeType
        WeaponTree(
            catalog = repository.catalog(type),
            state = screenState.browseStates.getOrPut(type) { WeaponBrowseState() },
            onBack = {
                if (forcedType != null) onBackFromForcedType() else screenState.selectedType = null
            },
            onItem = onItem,
            isFavorite = isFavorite,
            onFavorite = onFavorite,
            onWeaponOpened = onWeaponOpened
        )
    } else {
        WeaponTypeChooser(repository) { type ->
            if (onTypeSelected != null) onTypeSelected(type) else screenState.selectedType = type
        }
    }
}

internal class WeaponBrowseState {
    var query by mutableStateOf("")
    var selectedWeaponId by mutableStateOf<String?>(null)
    var specialType by mutableStateOf<WeaponSpecialType?>(null)
    var minimumAffinity by mutableStateOf<Int?>(null)
    var rarityFilter by mutableStateOf<Int?>(null)
    var minimumSlots by mutableStateOf<Int?>(null)
    var browseMode by mutableStateOf(WeaponBrowseMode.TREE)
    var descending by mutableStateOf(true)
    var savedTreeIndex by mutableIntStateOf(0)
    var savedTreeOffset by mutableIntStateOf(0)
    var savedSearchIndex by mutableIntStateOf(0)
    var savedSearchOffset by mutableIntStateOf(0)
    var savedFlatIndex by mutableIntStateOf(0)
    var savedFlatOffset by mutableIntStateOf(0)
    var expandedIdsEncoded by mutableStateOf("")
    var initialized by mutableStateOf(false)
    val weaponNavigationHistory = mutableStateListOf<String>()
    val detailStates = mutableStateMapOf<String, WeaponDetailViewState>()

    fun openFromBrowse(weaponId: String) {
        weaponNavigationHistory.clear()
        selectedWeaponId = weaponId
    }

    fun openRelatedWeapon(weaponId: String) {
        selectedWeaponId?.takeIf { it != weaponId }?.let(weaponNavigationHistory::add)
        selectedWeaponId = weaponId
    }

    fun navigateBackFromDetail() {
        selectedWeaponId = weaponNavigationHistory.removeLastOrNull()
    }

    fun closeDetail() {
        weaponNavigationHistory.clear()
        selectedWeaponId = null
    }
}

internal class WeaponDetailViewState {
    val scrollState = ScrollState(0)
    var upgradePathExpanded by mutableStateOf(false)
    var derivedUpgradePath by mutableStateOf<WeaponAncestryResult?>(null)
}

/** Navigation-owned state keeps each type's forest/detail context alive while
 * a recipe opens the existing Item Detail route. */
internal class WeaponScreenState {
    var selectedType by mutableStateOf<String?>(null)
    val browseStates = mutableStateMapOf<String, WeaponBrowseState>()
}

internal fun weaponTypeLabel(weaponType: String): String = canonicalWeaponTypeLabel(weaponType)

@Composable
private fun WeaponTypeChooser(repository: WeaponRepository, onType: (String) -> Unit) {
    val layout = LocalAppWindowLayout.current
    val columns = weaponChooserColumns(layout.widthDp - 28, layout.profile)
    val chooserTypes = remember(repository) {
        WeaponRepository.PRODUCTION_WEAPON_TYPES.map { type ->
            type to repository.catalog(type)
        }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding, vertical = 4.dp)
            .testTag("weapons-chooser"),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(bottom = 5.dp)
        ) {
            items(chooserTypes.size) { index ->
                val (type, catalog) = chooserTypes[index]
                Surface(
                    onClick = { onType(type) },
                    modifier = Modifier.fillMaxWidth().height(62.dp).testTag("weapon-type-${type.lowercase().replace('_', '-') }"),
                    color = AppColors.Parchment,
                    shape = AppDimens.CardShape,
                    border = BorderStroke(AppDimens.CardBorder, AppColors.ParchmentDeep)
                ) {
                    Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(29.dp).testTag("weapon-chooser-${type.lowercase().replace('_', '-')}-icon"),
                            contentAlignment = Alignment.Center
                        ) {
                            WeaponIcon(type, 1, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                weaponTypeLabel(type),
                                color = AppColors.Ink,
                                style = AppType.CardTitle.copy(fontSize = 14.sp, lineHeight = 15.sp),
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                "${catalog.validation.weaponCount} · ${catalog.validation.rootIds.size} trees",
                                color = AppColors.Ink.copy(alpha = .68f),
                                style = AppType.Metadata.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                        Text("›", color = AppColors.Crimson, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponTree(
    catalog: WeaponCatalog,
    state: WeaponBrowseState,
    onBack: () -> Unit,
    onItem: (Material) -> Unit,
    isFavorite: (String) -> Boolean,
    onFavorite: (String) -> Unit,
    onWeaponOpened: (String) -> Unit
) {
    if (!state.initialized) {
        state.expandedIdsEncoded = initialExpandedIds(catalog).joinToString("|")
        state.initialized = true
    }
    val treeState = rememberLazyListState()
    val searchState = rememberLazyListState()
    val flatState = rememberLazyListState()
    var showFilters by remember(state) { mutableStateOf(false) }
    val filters = WeaponBrowseFilters(
        specialType = state.specialType,
        minimumAffinity = state.minimumAffinity,
        rarity = state.rarityFilter,
        minimumSlots = state.minimumSlots
    )
    val elementOptions = remember(catalog) { availableWeaponSpecialTypes(catalog) }
    val affinityOptions = remember(catalog) { availableWeaponAffinities(catalog) }
    val rarityOptions = remember(catalog) { availableWeaponRarities(catalog) }
    val slotOptions = remember(catalog) { availableMinimumWeaponSlots(catalog) }
    val expandedIds = remember(state.expandedIdsEncoded) {
        state.expandedIdsEncoded.split('|').filter { it.isNotBlank() }.toSet()
    }
    val treeRows = remember(catalog, expandedIds, filters, state.query, state.browseMode) {
        when {
            state.browseMode != WeaponBrowseMode.TREE || state.query.isNotBlank() -> emptyList()
            filters.isActive -> {
                val matchingIds = catalog.weapons.asSequence()
                    .filter { it.matches(filters) }
                    .map { it.id }
                    .toSet()
                filteredWeaponTree(catalog, matchingIds)
            }
            else -> visibleTree(catalog, expandedIds)
        }
    }
    val searchResults = remember(catalog, state.query, filters) {
        browseWeapons(catalog, state.query, filters, WeaponBrowseMode.TREE)
    }
    val flatResults = remember(catalog, state.query, filters, state.browseMode, state.descending) {
        if (state.browseMode == WeaponBrowseMode.TREE) emptyList()
        else browseWeapons(catalog, state.query, filters, state.browseMode, state.descending)
    }
    val selectedWeapon = state.selectedWeaponId?.let(catalog.weaponById::get)

    BackHandler(enabled = selectedWeapon != null) { state.navigateBackFromDetail() }
    LaunchedEffect(treeRows) {
        if (treeRows.isNotEmpty() && (state.savedTreeIndex > 0 || state.savedTreeOffset > 0)) {
            treeState.scrollToItem(state.savedTreeIndex.coerceAtMost(treeRows.lastIndex), state.savedTreeOffset)
        }
    }
    LaunchedEffect(state.query, searchResults) {
        if (state.query.isNotBlank() && searchResults.isNotEmpty() && (state.savedSearchIndex > 0 || state.savedSearchOffset > 0)) {
            searchState.scrollToItem(state.savedSearchIndex.coerceAtMost(searchResults.lastIndex), state.savedSearchOffset)
        }
    }
    LaunchedEffect(flatResults) {
        if (flatResults.isNotEmpty() && (state.savedFlatIndex > 0 || state.savedFlatOffset > 0)) {
            flatState.scrollToItem(state.savedFlatIndex.coerceAtMost(flatResults.lastIndex), state.savedFlatOffset)
        }
    }
    LaunchedEffect(treeState) {
        snapshotFlow { treeState.firstVisibleItemIndex to treeState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> state.savedTreeIndex = index; state.savedTreeOffset = offset }
    }
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.firstVisibleItemIndex to searchState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> state.savedSearchIndex = index; state.savedSearchOffset = offset }
    }
    LaunchedEffect(flatState) {
        snapshotFlow { flatState.firstVisibleItemIndex to flatState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> state.savedFlatIndex = index; state.savedFlatOffset = offset }
    }

    Box(
        Modifier.fillMaxSize().testTag(
            if (catalog.weaponType == "GREAT_SWORD") "great-sword-production"
            else "weapon-production-${catalog.weaponType.lowercase()}"
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = AppDimens.ScreenPadding, vertical = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("weapon-type-toolbar"), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("weapons-back"),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Back", style = AppType.ButtonLabel, maxLines = 1)
                }
                Spacer(Modifier.width(3.dp))
                Text(weaponTypeLabel(catalog.weaponType), color = AppColors.Parchment, style = AppType.ScreenTitle.copy(fontSize = 22.sp, lineHeight = 25.sp), maxLines = 1)
                Spacer(Modifier.weight(1f))
                WeaponSearchField(
                    weaponType = catalog.weaponType,
                    query = state.query,
                    onQueryChange = { state.query = it },
                    modifier = Modifier.width(190.dp)
                )
            }
            WeaponBrowseControls(
                mode = state.browseMode,
                descending = state.descending,
                activeFilterCount = filters.activeCount,
                onOpenFilters = { showFilters = true },
                onModeSelected = { state.browseMode = it },
                onToggleDirection = { state.descending = !state.descending }
            )
            if (filters.isActive) {
                Row(
                    Modifier.fillMaxWidth().height(27.dp).testTag("weapon-active-filter-summary"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        filters.summary(),
                        modifier = Modifier.weight(1f),
                        color = AppColors.ParchmentDeep,
                        style = AppType.Metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = {
                            state.specialType = null
                            state.minimumAffinity = null
                            state.rarityFilter = null
                            state.minimumSlots = null
                        },
                        modifier = Modifier.testTag("weapon-filters-clear-inline"),
                        contentPadding = PaddingValues(horizontal = 5.dp)
                    ) { Text("Clear filters", style = AppType.Metadata, maxLines = 1) }
                }
            }
            when {
                state.browseMode == WeaponBrowseMode.TREE && state.query.isBlank() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("weapon-tree"),
                    state = treeState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (treeRows.isEmpty() && filters.isActive) {
                        item {
                            WeaponFilterEmptyState(onClear = {
                                state.specialType = null
                                state.minimumAffinity = null
                                state.rarityFilter = null
                                state.minimumSlots = null
                            })
                        }
                    } else {
                        items(treeRows, key = { it.weapon.id }) { row ->
                            WeaponRow(
                                weapon = row.weapon,
                                depth = row.depth,
                                hasChildren = !filters.isActive && catalog.childrenByWeaponId[row.weapon.id].orEmpty().isNotEmpty(),
                                expanded = row.weapon.id in expandedIds,
                                contextOnly = row.contextOnly,
                                modifier = Modifier.testTag("weapon-row-${row.weapon.id}"),
                                onToggleExpansion = {
                                    state.expandedIdsEncoded = toggleExpansion(catalog, expandedIds, row.weapon.id).joinToString("|")
                                }
                            ) {
                                state.openFromBrowse(row.weapon.id)
                                onWeaponOpened(row.weapon.id)
                            }
                        }
                    }
                }
                }
                state.browseMode != WeaponBrowseMode.TREE -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f).testTag("weapon-flat-results"),
                        state = flatState,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        if (flatResults.isEmpty()) {
                            item {
                                WeaponFilterEmptyState(onClear = if (filters.isActive) ({
                                    state.specialType = null
                                    state.minimumAffinity = null
                                    state.rarityFilter = null
                                    state.minimumSlots = null
                                }) else null, query = state.query, weaponType = catalog.weaponType)
                            }
                        } else {
                            items(flatResults, key = { "flat-${state.browseMode.name.lowercase()}-${it.id}" }) { weapon ->
                                WeaponRow(
                                    weapon = weapon,
                                    depth = 0,
                                    hasChildren = false,
                                    expanded = false,
                                    browseMode = state.browseMode,
                                    modifier = Modifier.testTag("weapon-flat-result-${weapon.id}"),
                                    onToggleExpansion = {}
                                ) {
                                    state.openFromBrowse(weapon.id)
                                    onWeaponOpened(weapon.id)
                                }
                            }
                        }
                    }
                }
                else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("weapon-search-results"),
                    state = searchState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item {
                            WeaponFilterEmptyState(
                                onClear = if (filters.isActive) ({
                                    state.specialType = null
                                    state.minimumAffinity = null
                                    state.rarityFilter = null
                                    state.minimumSlots = null
                                }) else null,
                                query = state.query,
                                weaponType = catalog.weaponType
                            )
                        }
                    } else {
                        items(searchResults, key = { "search-${it.id}" }) { weapon ->
                            WeaponRow(
                                weapon = weapon,
                                depth = 0,
                                hasChildren = false,
                                expanded = false,
                                modifier = Modifier.testTag("weapon-search-result-${weapon.id}"),
                                onToggleExpansion = {}
                            ) {
                                state.openFromBrowse(weapon.id)
                                onWeaponOpened(weapon.id)
                            }
                        }
                    }
                }
                }
            }
        }

        if (showFilters) {
            WeaponBrowseFilterDialog(
                filters = filters,
                elementOptions = elementOptions,
                affinityOptions = affinityOptions,
                rarityOptions = rarityOptions,
                slotOptions = slotOptions,
                onFiltersChanged = { next ->
                    state.specialType = next.specialType
                    state.minimumAffinity = next.minimumAffinity
                    state.rarityFilter = next.rarity
                    state.minimumSlots = next.minimumSlots
                },
                onDismiss = { showFilters = false }
            )
        }

        if (selectedWeapon != null) {
            WeaponDetailOverlay(
                weapon = selectedWeapon,
                catalog = catalog,
                detailState = state.detailStates.getOrPut(selectedWeapon.id) { WeaponDetailViewState() },
                onClose = { state.closeDetail() },
                onSelectWeapon = {
                    state.openRelatedWeapon(it.id)
                    onWeaponOpened(it.id)
                },
                onItem = onItem,
                isFavorite = isFavorite( selectedWeapon.id ),
                onFavorite = { onFavorite(selectedWeapon.id) }
            )
        }
    }
}

@Composable
internal fun WeaponSearchField(
    weaponType: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val searchTextStyle = AppType.Body.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = true)
    )
    SearchInputField(
        query = query,
        onQueryChange = onQueryChange,
        // Keep the normal compact Material field height even when the toolbar
        // is measured under a tight parent constraint.
        modifier = modifier.heightIn(min = 56.dp),
        tag = "weapon-search",
        textStyle = searchTextStyle,
        placeholder = {
            Text(
                "Search ${weaponTypeLabel(weaponType)} names",
                style = searchTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = AppColors.Gold) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag("weapon-search-clear")) {
                    Icon(Icons.Default.Close, "Clear weapon search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions.Default,
        shape = AppDimens.ButtonShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Gold,
            unfocusedBorderColor = AppColors.ParchmentDeep.copy(alpha = .65f),
            focusedTextColor = AppColors.Parchment,
            unfocusedTextColor = AppColors.Parchment,
            focusedPlaceholderColor = AppColors.ParchmentDeep,
            unfocusedPlaceholderColor = AppColors.ParchmentDeep
        )
    )
}

@Composable
private fun WeaponBrowseControls(
    mode: WeaponBrowseMode,
    descending: Boolean,
    activeFilterCount: Int,
    onOpenFilters: () -> Unit,
    onModeSelected: (WeaponBrowseMode) -> Unit,
    onToggleDirection: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(43.dp).horizontalScroll(rememberScrollState())
            .testTag("weapon-browse-controls"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeaponBrowseChip(
            label = if (activeFilterCount == 0) "Filters" else "Filters · $activeFilterCount",
            selected = activeFilterCount > 0,
            modifier = Modifier.testTag("weapon-filters-open"),
            onClick = onOpenFilters
        )
        WeaponBrowseMode.entries.forEach { option ->
            WeaponBrowseChip(
                label = when (option) {
                    WeaponBrowseMode.TREE -> "Tree"
                    WeaponBrowseMode.ATTACK -> "Attack"
                    WeaponBrowseMode.RARITY -> "Rarity"
                },
                selected = mode == option,
                modifier = Modifier.testTag("weapon-mode-${option.name.lowercase()}"),
                onClick = { onModeSelected(option) }
            )
        }
        if (mode != WeaponBrowseMode.TREE) {
            TextButton(
                onClick = onToggleDirection,
                modifier = Modifier.testTag("weapon-sort-direction")
                    .semantics { contentDescription = if (descending) "Descending order" else "Ascending order" },
                contentPadding = PaddingValues(horizontal = 7.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Parchment)
            ) {
                Text(if (descending) "↓" else "↑", style = AppType.CardTitle, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WeaponBrowseChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        color = if (selected) AppColors.Crimson else AppColors.Parchment.copy(alpha = .10f),
        shape = AppDimens.ButtonShape,
        border = BorderStroke(1.dp, if (selected) AppColors.Gold else AppColors.ParchmentDeep.copy(alpha = .72f))
    ) {
        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = AppColors.Parchment,
                style = AppType.ButtonLabel.copy(fontSize = 12.sp),
                maxLines = 1
            )
        }
    }
}

private data class WeaponFilterChoice<T : Any>(
    val value: T?,
    val label: String,
    val tag: String
)

@Composable
private fun WeaponBrowseFilterDialog(
    filters: WeaponBrowseFilters,
    elementOptions: List<WeaponSpecialType>,
    affinityOptions: List<Int>,
    rarityOptions: List<Int>,
    slotOptions: List<Int>,
    onFiltersChanged: (WeaponBrowseFilters) -> Unit,
    onDismiss: () -> Unit
) {
    val elementChoices = remember(elementOptions) {
        listOf(WeaponFilterChoice<WeaponSpecialType>(null, "Any", "weapon-filter-element-any")) +
            elementOptions.map { type ->
                WeaponFilterChoice(type, type.displayLabel(), "weapon-filter-element-${type.name.lowercase()}")
            }
    }
    val affinityChoices = remember(affinityOptions) {
        listOf(WeaponFilterChoice<Int>(null, "Any", "weapon-filter-affinity-any")) +
            affinityOptions.map { value ->
                WeaponFilterChoice(value, "≥ $value%", "weapon-filter-affinity-value-${value.toString().replace("-", "minus-")}")
            }
    }
    val rarityChoices = remember(rarityOptions) {
        listOf(WeaponFilterChoice<Int>(null, "Any", "weapon-filter-rarity-any")) +
            rarityOptions.map { value -> WeaponFilterChoice(value, "R$value", "weapon-filter-rarity-value-$value") }
    }
    val slotChoices = remember(slotOptions) {
        listOf(WeaponFilterChoice<Int>(null, "Any", "weapon-filter-slots-any")) +
            slotOptions.map { value -> WeaponFilterChoice(value, "≥ $value", "weapon-filter-slots-value-$value") }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weapon filters", color = AppColors.Ink, style = AppType.SectionTitle) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeaponFilterSelector(
                        title = "Element / status",
                        selectedLabel = filters.specialType?.displayLabel() ?: "Any",
                        testTag = "weapon-filter-element",
                        choices = elementChoices,
                        modifier = Modifier.weight(1f)
                    ) { onFiltersChanged(filters.copy(specialType = it)) }
                    WeaponFilterSelector(
                        title = "Min affinity",
                        selectedLabel = filters.minimumAffinity?.let { "≥ $it%" } ?: "Any",
                        testTag = "weapon-filter-affinity",
                        choices = affinityChoices,
                        modifier = Modifier.weight(1f)
                    ) { onFiltersChanged(filters.copy(minimumAffinity = it)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeaponFilterSelector(
                        title = "Exact rarity",
                        selectedLabel = filters.rarity?.let { "R$it" } ?: "Any",
                        testTag = "weapon-filter-rarity",
                        choices = rarityChoices,
                        modifier = Modifier.weight(1f)
                    ) { onFiltersChanged(filters.copy(rarity = it)) }
                    WeaponFilterSelector(
                        title = "Min slots",
                        selectedLabel = filters.minimumSlots?.let { "≥ $it" } ?: "Any",
                        testTag = "weapon-filter-slots",
                        choices = slotChoices,
                        modifier = Modifier.weight(1f)
                    ) { onFiltersChanged(filters.copy(minimumSlots = it)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("weapon-filters-done")) {
                Text("Done", color = AppColors.Crimson)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onFiltersChanged(WeaponBrowseFilters()) },
                modifier = Modifier.testTag("weapon-filters-clear-dialog")
            ) { Text("Clear filters", color = AppColors.Ink) }
        },
        containerColor = AppColors.Parchment,
        titleContentColor = AppColors.Ink,
        textContentColor = AppColors.Ink
    )
}

@Composable
private fun <T : Any> WeaponFilterSelector(
    title: String,
    selectedLabel: String,
    testTag: String,
    choices: List<WeaponFilterChoice<T>>,
    modifier: Modifier = Modifier,
    onSelected: (T?) -> Unit
) {
    var expanded by remember(testTag) { mutableStateOf(false) }
    Column(modifier) {
        Text(title, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
        Box {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().height(38.dp).testTag(testTag),
                color = AppColors.Parchment.copy(alpha = .55f),
                shape = AppDimens.ButtonShape,
                border = BorderStroke(1.dp, AppColors.ParchmentDeep)
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedLabel,
                        color = AppColors.Ink,
                        style = AppType.Metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("⌄", color = AppColors.Crimson, style = AppType.CardTitle)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp).background(AppColors.Parchment)
            ) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.label, color = AppColors.Ink, style = AppType.Metadata) },
                        onClick = {
                            onSelected(choice.value)
                            expanded = false
                        },
                        modifier = Modifier.testTag(choice.tag),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeaponFilterEmptyState(
    onClear: (() -> Unit)?,
    query: String = "",
    weaponType: String? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp).testTag("weapon-filter-empty-state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            when {
                query.isNotBlank() -> "No ${weaponType?.let(::weaponTypeLabel) ?: "weapons"} match ‘$query’."
                else -> "No weapons match these filters."
            },
            color = AppColors.Ink.copy(alpha = .76f),
            style = AppType.Body,
            modifier = Modifier.testTag("weapon-search-no-results")
        )
        if (onClear != null) {
            TextButton(onClick = onClear, modifier = Modifier.testTag("weapon-filters-clear-empty")) {
                Text("Clear filters", color = AppColors.Crimson)
            }
        }
    }
}

@Composable
private fun WeaponRow(
    weapon: Weapon,
    depth: Int,
    hasChildren: Boolean,
    expanded: Boolean,
    contextOnly: Boolean = false,
    browseMode: WeaponBrowseMode? = null,
    modifier: Modifier = Modifier,
    onToggleExpansion: () -> Unit,
    onClick: () -> Unit
) {
    val compactRow = weaponRowsStacked(LocalAppWindowLayout.current.widthDp)
    Box(modifier.fillMaxWidth()) {
        val indentation = if (compactRow) (depth * 8).coerceAtMost(72).dp else (depth * 20).coerceAtMost(112).dp
        Surface(
            modifier = Modifier.fillMaxWidth().height(if (compactRow) 64.dp else 50.dp).clickable(onClick = onClick),
            color = AppColors.Parchment.copy(alpha = .96f),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, AppColors.ParchmentDeep.copy(alpha = .7f))
        ) {
            if (compactRow) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        TreeConnectors(depth, Modifier.width(indentation), step = 8.dp)
                        if (hasChildren) {
                            IconButton(
                                onClick = onToggleExpansion,
                                modifier = Modifier.size(30.dp).testTag("weapon-expand-${weapon.id}")
                            ) {
                                Text(
                                    text = if (expanded) "−" else "+",
                                    color = AppColors.Crimson,
                                    style = AppType.CardTitle,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Spacer(Modifier.width(30.dp))
                        }
                        WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(24.dp).testTag("weapon-icon-${weapon.id}"))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            weapon.name,
                            color = rarityVisual(weapon.rarity, weapon.weaponType).nameColor,
                            style = AppType.CardTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).testTag("weapon-name-${weapon.id}")
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "R${weapon.rarity}",
                            color = rarityVisual(weapon.rarity, weapon.weaponType).nameColor,
                            style = AppType.Metadata,
                            modifier = Modifier.width(27.dp).testTag("weapon-rarity-${weapon.id}")
                        )
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width((indentation + 58.dp).coerceAtMost(100.dp)))
                        Text(
                            weapon.compactRowDetails(browseMode, contextOnly),
                            color = AppColors.Ink.copy(alpha = if (contextOnly) .62f else .76f),
                            style = AppType.Metadata.copy(fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f).then(
                                if (contextOnly) Modifier.testTag("weapon-path-only-${weapon.id}") else Modifier
                            )
                        )
                        weapon.sharpness?.takeIf { browseMode == null }?.let { sharpness ->
                            Spacer(Modifier.width(6.dp))
                            WeaponSharpnessBar(
                                sharpness.normal,
                                Modifier.width(82.dp).testTag("weapon-sharpness-normal-${weapon.id}")
                            )
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxSize().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    TreeConnectors(depth, Modifier.width(indentation))
                    if (hasChildren) {
                        IconButton(
                            onClick = onToggleExpansion,
                            modifier = Modifier.size(30.dp).testTag("weapon-expand-${weapon.id}")
                        ) {
                            Text(
                                text = if (expanded) "−" else "+",
                                color = AppColors.Crimson,
                                style = AppType.CardTitle,
                                maxLines = 1
                            )
                        }
                    } else {
                        Spacer(Modifier.width(30.dp))
                    }
                    WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(29.dp).testTag("weapon-icon-${weapon.id}"))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        weapon.name,
                        color = rarityVisual(weapon.rarity, weapon.weaponType).nameColor,
                        style = AppType.CardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("weapon-name-${weapon.id}")
                    )
                    Text("R${weapon.rarity}", color = rarityVisual(weapon.rarity, weapon.weaponType).nameColor, style = AppType.Metadata, modifier = Modifier.width(27.dp).testTag("weapon-rarity-${weapon.id}"))
                    when {
                        contextOnly -> Text(
                            "Path only",
                            color = AppColors.Ink.copy(alpha = .62f),
                            style = AppType.Metadata.copy(fontSize = 10.sp),
                            modifier = Modifier.testTag("weapon-path-only-${weapon.id}")
                        )
                        browseMode != null -> Column(
                            Modifier.width(112.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                if (browseMode == WeaponBrowseMode.ATTACK) "Attack ${weapon.attack}" else "R${weapon.rarity}",
                                color = AppColors.Ink.copy(alpha = .88f),
                                style = AppType.Metadata.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.testTag("weapon-flat-primary-${weapon.id}")
                            )
                            Text(
                                if (browseMode == WeaponBrowseMode.ATTACK) "R${weapon.rarity} · Aff ${weapon.affinityPercent}%"
                                else "Attack ${weapon.attack} · Aff ${weapon.affinityPercent}%",
                                color = AppColors.Ink.copy(alpha = .70f),
                                style = AppType.Metadata.copy(fontSize = 10.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Text(
                                "${weapon.special?.type?.displayLabel() ?: "—"} · ${weapon.slots} slots",
                                color = AppColors.Ink.copy(alpha = .70f),
                                style = AppType.Metadata.copy(fontSize = 10.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                        else -> Column(Modifier.width(69.dp), horizontalAlignment = Alignment.End) {
                            Text(
                                buildString {
                                    append(weapon.attack)
                                    if (weapon.affinityPercent != 0) append(" · ${weapon.affinityPercent}%")
                                    if (weapon.slots > 0) append(" · ${"●".repeat(weapon.slots)}")
                                },
                                color = AppColors.Ink.copy(alpha = .76f),
                                style = AppType.Metadata,
                                maxLines = 1
                            )
                            if (weapon.defenseBonus != null) Text("Def +${weapon.defenseBonus}", color = AppColors.Ink.copy(alpha = .62f), style = AppType.Metadata, maxLines = 1)
                        }
                    }
                    weapon.sharpness?.takeIf { browseMode == null }?.let { sharpness ->
                        Spacer(Modifier.width(6.dp))
                        WeaponSharpnessBar(sharpness.normal, Modifier.width(82.dp).testTag("weapon-sharpness-normal-${weapon.id}"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeConnectors(depth: Int, modifier: Modifier = Modifier, step: Dp = 20.dp) {
    if (depth == 0) {
        Spacer(modifier)
        return
    }
    Canvas(modifier.fillMaxHeight()) {
        val stepPx = step.toPx()
        val railColor = AppColors.Crimson.copy(alpha = .46f)
        for (level in 0 until depth) {
            val x = size.width - stepPx / 2f - level * stepPx
            drawLine(railColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
        }
        val elbowX = size.width - stepPx / 2f
        drawLine(railColor, androidx.compose.ui.geometry.Offset(elbowX, size.height / 2f), androidx.compose.ui.geometry.Offset(size.width, size.height / 2f), strokeWidth = 1.5.dp.toPx())
    }
}

private fun Weapon.compactRowDetails(mode: WeaponBrowseMode?, contextOnly: Boolean): String = when {
    contextOnly -> "Path only"
    mode == WeaponBrowseMode.ATTACK ->
        "Attack $attack · R$rarity · Aff $affinityPercent% · ${special?.type?.displayLabel() ?: "—"} · $slots slots"
    mode == WeaponBrowseMode.RARITY ->
        "R$rarity · Attack $attack · Aff $affinityPercent% · ${special?.type?.displayLabel() ?: "—"} · $slots slots"
    else -> buildString {
        append(attack)
        if (affinityPercent != 0) append(" · $affinityPercent%")
        if (slots > 0) append(" · ${"●".repeat(slots)}")
        defenseBonus?.let { append(" · Def +").append(it) }
    }
}

@Composable
internal fun WeaponIcon(weaponType: String, rarity: Int = 1, modifier: Modifier = Modifier) {
    val ref = WeaponIconRegistry.resolve(weaponType, rarity) ?: return
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            bitmap = ImageBitmap.imageResource(ref.resourceId),
            contentDescription = ref.contentDescription,
            modifier = Modifier.fillMaxSize().padding(2.dp),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
private fun WeaponSharpnessBar(sharpness: WeaponSharpness, modifier: Modifier = Modifier) {
    val total = sharpness.totalUnits().coerceAtLeast(0)
    Box(
        modifier.height(9.dp)
            .background(AppColors.ParchmentDeep.copy(alpha = .18f), RoundedCornerShape(2.dp))
            .border(1.dp, AppColors.Ink.copy(alpha = .35f), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        if (total > 0) {
            Row(Modifier.fillMaxWidth(sharpnessFillFraction(sharpness)).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                sharpness.segments().filter { it.first > 0 }.forEach { (units, color) ->
                    Box(Modifier.weight(units.toFloat() / total.toFloat()).fillMaxHeight().background(color))
                }
            }
        }
    }
}

@Composable
internal fun WeaponDetailOverlay(
    weapon: Weapon,
    catalog: WeaponCatalog,
    onClose: () -> Unit,
    onSelectWeapon: (Weapon) -> Unit,
    onItem: (Material) -> Unit,
    isFavorite: Boolean = false,
    onFavorite: () -> Unit = {},
    detailState: WeaponDetailViewState = remember(weapon.id) { WeaponDetailViewState() }
) {
    val detailScrollState = detailState.scrollState
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)).testTag("weapon-detail-overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(.94f).fillMaxHeight(.98f)
                .clip(AppDimens.CardShape)
                .background(AppColors.Parchment)
                .border(1.dp, AppColors.ParchmentDeep, AppDimens.CardShape)
                .verticalScroll(detailScrollState)
                .padding(15.dp)
                .testTag("weapon-detail-modal")
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                WeaponIcon(weapon.weaponType, weapon.rarity, Modifier.size(38.dp).testTag("weapon-detail-icon-${weapon.id}"))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(weapon.name, color = rarityVisual(weapon.rarity, weapon.weaponType).nameColor, style = AppType.ScreenTitle, maxLines = 2, modifier = Modifier.testTag("weapon-detail-title-${weapon.id}"))
                    Text("${weaponTypeLabel(weapon.weaponType)} · R${weapon.rarity}", color = AppColors.Crimson, style = AppType.Body)
                }
                FavoriteButton(isFavorite, onFavorite, "weapon-favorite", compact = true)
                Spacer(Modifier.width(3.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp).testTag("weapon-detail-close")) {
                    Icon(Icons.Default.Close, "Close weapon details", tint = AppColors.Ink)
                }
            }
            Spacer(Modifier.height(6.dp))
            WeaponStats(weapon)
            WeaponMechanicsSection(weapon, catalog)
            weapon.sharpness?.let { profile ->
                Text("Sharpness", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Text("Normal", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                WeaponSharpnessBar(profile.normal, Modifier.fillMaxWidth().height(13.dp).testTag("weapon-detail-sharpness-normal-${weapon.id}"))
                if (profile.plusOne != null) {
                    Spacer(Modifier.height(5.dp))
                    Text("Sharpness +1", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                    WeaponSharpnessBar(profile.plusOne, Modifier.fillMaxWidth().height(13.dp).testTag("weapon-detail-sharpness-plus-one-${weapon.id}"))
                }
            }
            val parentEdge = catalog.parentEdgeByWeaponId[weapon.id]
            // Immediate acquisition/progression answers come before the optional
            // historical path.  The ancestry model is intentionally derived only
            // after the user expands the accordion (and then cached for this detail
            // instance).
            val acquisitionSections = buildList {
                weapon.forgeRecipe?.let { recipe ->
                    add(
                        WeaponAcquisitionSectionSpec(
                            sectionTitle = "Forge",
                            title = weaponRankedTitle(weapon),
                            method = "Forge",
                            recipe = recipe,
                            tag = "weapon-forge",
                            iconWeapon = weapon
                        )
                    )
                }
                parentEdge?.let { edge ->
                    catalog.weaponById[edge.fromWeaponId]?.let { parent ->
                        add(
                        WeaponAcquisitionSectionSpec(
                            sectionTitle = "Upgrade from",
                            title = weaponTransitionTitle(parent, weapon, linkWeaponId = parent.id),
                                method = "Upgrade",
                                recipe = edge.recipe,
                                tag = "weapon-upgrade-from-${weapon.id}",
                                iconWeapon = weapon
                            )
                        )
                    }
                }
            }
            ResponsiveAcquisitionSections(acquisitionSections, catalog, onItem, onSelectWeapon)
            catalog.orphanUpgradeRecipeOf(weapon.id)?.let { orphan ->
                WeaponRecipeSection(
                    sectionTitle = "Upgrade",
                    title = weaponRankedTitle(weapon),
                    method = "Upgrade",
                    recipe = orphan,
                    catalog = catalog,
                    onItem = onItem,
                    tag = "weapon-orphan-upgrade-${weapon.id}",
                    iconWeapon = weapon
                )
            }
            val children = catalog.childrenOf(weapon.id)
            if (children.isNotEmpty()) {
                Text("Upgrades to", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                ResponsiveRecipeCards(
                    cards = children.map { child ->
                        val childRecipe = catalog.parentEdgeByWeaponId[child.id]?.recipe
                            ?: child.upgradeFrom?.recipe
                        WeaponRecipeCardSpec(
                            title = weaponTransitionTitle(weapon, child),
                            method = "Upgrade",
                            recipe = childRecipe,
                            iconWeapon = child,
                            tag = "weapon-upgrade-to-${child.id}",
                            onClick = { onSelectWeapon(child) }
                        )
                    },
                    catalog = catalog,
                    onItem = onItem,
                    sectionTag = "weapon-upgrades-to-cards"
                )
            }
            UpgradePathAccordion(
                weapon = weapon,
                catalog = catalog,
                onItem = onItem,
                onSelectWeapon = onSelectWeapon,
                expanded = detailState.upgradePathExpanded,
                derived = detailState.derivedUpgradePath,
                onToggle = {
                    if (detailState.derivedUpgradePath == null) {
                        detailState.derivedUpgradePath = catalog.ancestryFor(weapon.id)
                    }
                    detailState.upgradePathExpanded = !detailState.upgradePathExpanded
                }
            )
        }
    }
}

@Composable
private fun UpgradePathAccordion(
    weapon: Weapon,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    onSelectWeapon: (Weapon) -> Unit,
    expanded: Boolean,
    derived: WeaponAncestryResult?,
    onToggle: () -> Unit
) {
    // A root weapon can still have useful ancestry context even when its own
    // forge payload is unpublished; keep the accordion available for that
    // compact base node rather than hiding it behind a recipe check.
    val hasMeaningfulPath = derived?.nodes?.isNotEmpty() == true ||
        weapon.upgradeFrom != null ||
        weapon.forgeRecipe != null ||
        catalog.childrenOf(weapon.id).isNotEmpty()
    if (!hasMeaningfulPath && !expanded) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("weapon-upgrade-path-container"),
        color = AppColors.ParchmentDeep.copy(alpha = .18f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, AppColors.ParchmentDeep.copy(alpha = .7f))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 9.dp, vertical = 7.dp)
                    .testTag("weapon-upgrade-path"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upgrade path", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse upgrade path" else "Expand upgrade path",
                    tint = AppColors.Crimson,
                    modifier = Modifier.size(20.dp).testTag("weapon-upgrade-path-chevron")
                )
            }
            if (expanded) {
                val ancestry = derived ?: return@Column
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ancestry.nodes.forEachIndexed { index, node ->
                        WeaponAncestryNodeCard(node, catalog, onItem, node.weapon.id == weapon.id, onSelectWeapon)
                        if (index < ancestry.nodes.lastIndex) {
                            Text(
                                "↓",
                                color = AppColors.Crimson.copy(alpha = .76f),
                                style = AppType.SectionTitle,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).testTag("weapon-ancestry-connector-$index"),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    if (ancestry.cycleDetected || ancestry.missingParentId != null) {
                        val issue = ancestry.missingParentId?.let { "missing parent $it" } ?: "cycle detected"
                        Text(
                            "Ancestry data incomplete · $issue",
                            color = AppColors.Ink.copy(alpha = .72f),
                            style = AppType.Metadata,
                            modifier = Modifier.padding(top = 4.dp).testTag("weapon-ancestry-warning")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponAncestryNodeCard(
    node: WeaponAncestryNode,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    isCurrent: Boolean,
    onSelectWeapon: (Weapon) -> Unit
) {
    val outline = if (isCurrent) AppColors.Gold else AppColors.ParchmentDeep.copy(alpha = .7f)
    Surface(
        modifier = Modifier.fillMaxWidth(.88f).testTag("weapon-ancestry-node-${node.weapon.id}"),
        color = if (isCurrent) AppColors.Gold.copy(alpha = .14f) else WeaponRecipeSurfaceColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, outline)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WeaponIcon(node.weapon.weaponType, node.weapon.rarity, Modifier.size(30.dp))
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        weaponRankedTitle(node.weapon),
                        color = AppColors.Ink,
                        style = AppType.CardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .then(if (!isCurrent) Modifier.clickable {
                                catalog.weaponById[node.weapon.id]?.let(onSelectWeapon)
                            } else Modifier)
                            .testTag("weapon-ancestry-node-title-${node.weapon.id}")
                    )
                }
                if (isCurrent) {
                    Text("Current", color = AppColors.Crimson, style = AppType.Metadata, modifier = Modifier.testTag("weapon-ancestry-current"))
                }
            }
            if (node.recipe != null) {
                val cost = node.recipe.zenny?.let { " · ${it}z" }.orEmpty()
                Text(
                    "${if (node.method == WeaponAncestryMethod.FORGE) "Forge" else "Upgrade"}$cost",
                    color = AppColors.Ink.copy(alpha = .75f),
                    style = AppType.Metadata,
                    modifier = Modifier.padding(top = 3.dp).testTag("weapon-ancestry-recipe-${node.weapon.id}")
                )
                if (node.recipe.ingredients.isNotEmpty()) {
                    RecipeHeaderDivider("weapon-ancestry-recipe-${node.weapon.id}-materials")
                    WeaponRecipeRows(node.recipe, catalog, onItem, "weapon-ancestry-recipe-${node.weapon.id}-materials")
                }
            } else {
                Text(
                    "Base weapon",
                    color = AppColors.Ink.copy(alpha = .68f),
                    style = AppType.Metadata,
                    modifier = Modifier.padding(top = 3.dp).testTag("weapon-ancestry-base-${node.weapon.id}")
                )
            }
            node.directForgeRecipe?.let { recipe ->
                val cost = recipe.zenny?.let { " · ${it}z" }.orEmpty()
                RecipeHeaderDivider("weapon-ancestry-direct-forge-${node.weapon.id}")
                Text(
                    "Direct forge$cost",
                    color = AppColors.Crimson,
                    style = AppType.Metadata,
                    modifier = Modifier.padding(top = 4.dp).testTag("weapon-ancestry-direct-forge-${node.weapon.id}")
                )
                WeaponRecipeRows(recipe, catalog, onItem, "weapon-ancestry-direct-forge-${node.weapon.id}-materials")
            }
        }
    }
}

@Composable
internal fun WeaponDeepLinkScreen(
    weapon: Weapon,
    catalog: WeaponCatalog,
    isFavorite: (String) -> Boolean,
    onFavorite: (String) -> Unit,
    onClose: () -> Unit,
    onItem: (Material) -> Unit,
    onSelectWeapon: (Weapon) -> Unit = {}
) {
    var selected by remember(weapon.id) { mutableStateOf(weapon) }
    val navigationHistory = remember(weapon.id) { mutableStateListOf<String>() }
    val detailStates = remember(catalog) { mutableStateMapOf<String, WeaponDetailViewState>() }
    BackHandler(enabled = navigationHistory.isNotEmpty()) {
        val previousId = navigationHistory.removeLastOrNull()
        if (previousId != null) selected = catalog.weaponById[previousId] ?: selected
    }
    WeaponDetailOverlay(
        weapon = selected,
        catalog = catalog,
        detailState = detailStates.getOrPut(selected.id) { WeaponDetailViewState() },
        onClose = onClose,
        onSelectWeapon = { next ->
            if (selected.id != next.id) navigationHistory.add(selected.id)
            selected = next
            onSelectWeapon(next)
        },
        onItem = onItem,
        isFavorite = isFavorite(selected.id),
        onFavorite = { onFavorite(selected.id) }
    )
}

@Composable
private fun WeaponMechanicsSection(weapon: Weapon, catalog: WeaponCatalog) {
    when (val mechanics = weapon.mechanics) {
        is HuntingHornMechanics -> {
            Text("Notes", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("weapon-hh-notes"))
            Row(Modifier.fillMaxWidth().testTag("weapon-hh-note-order"), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                mechanics.notes.forEachIndexed { index, note ->
                    Surface(
                        modifier = Modifier.size(32.dp)
                            .semantics { contentDescription = "${note.name.lowercase()} note" }
                            .testTag("weapon-hh-note-$index-${note.name}"),
                        color = hornNoteColor(note),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, if (note == HornNote.WHITE) AppColors.Ink else Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(note.name.take(1), color = if (note == HornNote.WHITE || note == HornNote.YELLOW) AppColors.Ink else Color.White, style = AppType.ButtonLabel)
                        }
                    }
                }
            }
            val songs = catalog.songsFor(weapon)
            Text("Melodies / Songs", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).testTag("weapon-hh-songs"))
            Column(Modifier.fillMaxWidth().testTag("weapon-hh-song-list")) {
                songs.forEachIndexed { index, song ->
                    Text(
                        "[${song.sequence.joinToString(" ")}] ${song.effectName}",
                        color = AppColors.Ink,
                        style = AppType.Body,
                        modifier = Modifier.padding(vertical = 2.dp).testTag("weapon-hh-song-$index")
                    )
                }
            }
        }
        is GunlanceMechanics -> {
            Text("Shelling", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("weapon-gl-shelling"))
            Text("${mechanics.shellingType.name.lowercase().replaceFirstChar { it.uppercase() }} Lv ${mechanics.shellingLevel}", color = AppColors.Ink, style = AppType.Body)
        }
        is SwitchAxeMechanics -> {
            Text("Phial", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("weapon-sa-phial"))
            Text(mechanics.phialType.name.lowercase().replaceFirstChar { it.uppercase() }, color = AppColors.Ink, style = AppType.Body)
        }
        is BowMechanics -> {
            Text("Charges", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("weapon-bow-charges"))
            Column(Modifier.fillMaxWidth().testTag("weapon-bow-charge-list")) {
                mechanics.charges.forEachIndexed { index, charge ->
                    val loadUp = if (charge.availability == BowChargeAvailability.REQUIRES_LOAD_UP) " · Load Up" else ""
                    Text(
                        "${index + 1}. ${charge.shotType.name.lowercase().replaceFirstChar { it.uppercase() }} Lv ${charge.shotLevel}$loadUp",
                        color = AppColors.Ink,
                        style = AppType.Body,
                        modifier = Modifier.padding(vertical = 1.dp).testTag("weapon-bow-charge-$index")
                    )
                }
            }
            Text("Arc Shot", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp).testTag("weapon-bow-arc-shot"))
            Text(mechanics.arcShot.name.lowercase().replaceFirstChar { it.uppercase() }, color = AppColors.Ink, style = AppType.Body)
            Text("Coatings", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp).testTag("weapon-bow-coatings"))
            val coatingsByType = mechanics.coatings.associateBy { it.coatingType }
            Column(Modifier.fillMaxWidth().testTag("weapon-bow-coating-list")) {
                BowCoatingType.values().toList().chunked(2).forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth().testTag("weapon-bow-coating-row-$rowIndex")) {
                        row.forEach { type ->
                            val support = coatingsByType[type]
                            val label = type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                            val state = when (support?.supportLevel) {
                                BowCoatingSupportLevel.ENHANCED -> "Enhanced"
                                BowCoatingSupportLevel.NORMAL -> "Supported"
                                null -> "—"
                            }
                            Row(
                                Modifier.weight(1f).padding(start = 0.dp, top = 1.dp, end = 6.dp, bottom = 1.dp)
                                    .testTag("weapon-bow-coating-${type.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BowCoatingIcon(type, Modifier.size(19.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "$label · $state",
                                    color = if (support == null) AppColors.Ink.copy(alpha = .45f) else AppColors.Ink,
                                    style = AppType.Metadata.copy(fontSize = 12.sp, lineHeight = 14.sp),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        is LightBowgunMechanics -> BowgunMechanicsSection(
            shared = mechanics.shared,
            rapidFire = mechanics.rapidFire,
            crouchFire = emptyList(),
            tag = "weapon-lbg"
        )
        is HeavyBowgunMechanics -> BowgunMechanicsSection(
            shared = mechanics.shared,
            rapidFire = emptyList(),
            crouchFire = mechanics.crouchFire,
            tag = "weapon-hbg"
        )
        null -> Unit
    }
}

@Composable
private fun BowCoatingIcon(type: BowCoatingType, modifier: Modifier = Modifier) {
    BowCoatingIconRegistry.resolve(type)?.let { ref ->
        Image(
            bitmap = ImageBitmap.imageResource(ref.resourceId),
            contentDescription = type.name.lowercase().replace('_', ' '),
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None
        )
    }
}

@Composable
private fun BowgunMechanicsSection(
    shared: com.waillio.mhp3rdcompanion.data.BowgunSharedMechanics,
    rapidFire: List<com.waillio.mhp3rdcompanion.data.BowgunRapidFire>,
    crouchFire: List<com.waillio.mhp3rdcompanion.data.BowgunCrouchFire>,
    tag: String
) {
    Text("Reload · Recoil · Deviation", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("$tag-shared"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        WeaponStat("Reload", shared.reload.value.displayLabel(), Modifier.weight(1f))
        WeaponStat("Recoil", shared.recoil.value.displayLabel(), Modifier.weight(1f))
        WeaponStat("Deviation", deviationLabel(shared.deviation.magnitude.name, shared.deviation.direction.name), Modifier.weight(1f))
    }
    if (rapidFire.isNotEmpty()) {
        Text("Rapid Fire", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp).testTag("$tag-rapid-fire"))
        rapidFire.forEachIndexed { index, entry ->
            BowgunSpecialFireRow(
                round = entry.round,
                detail = "${entry.burstCount}-shot · ${entry.recoil.value.displayLabel()}",
                tag = "$tag-rapid-fire-$index"
            )
        }
    }
    if (crouchFire.isNotEmpty()) {
        Text("Crouch Fire", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp).testTag("$tag-crouch-fire"))
        crouchFire.forEachIndexed { index, entry ->
            BowgunSpecialFireRow(
                round = entry.round,
                detail = null,
                tag = "$tag-crouch-fire-$index"
            )
        }
    }
    Text("Ammo", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp).testTag("$tag-ammo"))
    Column(Modifier.fillMaxWidth().testTag("$tag-ammo-table")) {
        ammoMatrixGroups.forEach { group ->
            val rows = shared.ammoLoads.filter { it.round.ammoType in group.families }
            if (rows.isNotEmpty()) {
                BowgunAmmoGroup(
                    group = group,
                    loads = rows,
                    modifier = Modifier.testTag("$tag-ammo-group-${group.key}")
                )
            }
        }
    }
}

@Composable
private fun BowgunSpecialFireRow(round: BowgunRound, detail: String?, tag: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 1.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BowgunAmmoIcon(round.ammoType, Modifier.size(23.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            ammoRoundLabel(round) + (detail?.let { " · $it" } ?: ""),
            color = AppColors.Ink,
            style = AppType.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BowgunAmmoGroup(
    group: AmmoMatrixGroup,
    loads: List<BowgunAmmoLoad>,
    modifier: Modifier = Modifier
) {
    val shapes = loads.map { it.sourceShape }.distinct()
    val shape = shapes.singleOrNull()
    if (shape == null || shape != group.sourceShape) {
        Text(
            "Ammo source-shape mismatch",
            color = AppColors.Crimson,
            style = AppType.Metadata,
            modifier = modifier.fillMaxWidth().testTag("ammo-matrix-shape-mismatch-${group.key}")
        )
        return
    }
    val columns = ammoMatrixColumns(shape)
    Text(
        group.title,
        color = AppColors.Ink.copy(alpha = .78f),
        style = AppType.Metadata,
        modifier = modifier.fillMaxWidth().padding(top = 3.dp, bottom = 1.dp)
    )
    Row(Modifier.fillMaxWidth().height(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(23.dp + 5.dp + 86.dp))
        columns.forEach { column ->
            Text(column.label, color = AppColors.Ink.copy(alpha = .52f), style = AppType.Metadata, modifier = Modifier.weight(1f), maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    val byFamily = loads.groupBy { it.round.ammoType }
    byFamily.forEach { (family, familyLoads) ->
        val capacities = familyLoads.associate { it.round.level to it.capacity }
        Row(
            Modifier.fillMaxWidth().height(28.dp).testTag("ammo-matrix-row-${family.name.lowercase()}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BowgunAmmoIcon(family, Modifier.size(23.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                ammoFamilyLabel(family).removeSuffix(" S"),
                color = AppColors.Ink,
                style = AppType.Metadata,
                modifier = Modifier.widthIn(min = 64.dp, max = 86.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            columns.forEach { column ->
                val capacity = capacities[column.level]
                Text(
                    ammoCapacityLabel(capacity),
                    color = if (capacity == null || capacity == 0) AppColors.Ink.copy(alpha = .48f) else AppColors.Ink,
                    style = AppType.Metadata,
                    modifier = Modifier.weight(1f).testTag("ammo-matrix-cell-${family.name.lowercase()}-${column.level ?: 0}"),
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BowgunAmmoIcon(family: BowgunAmmoFamily, modifier: Modifier) {
    AmmoIconRegistry.resolve(family)?.let { ref ->
        Image(
            bitmap = ImageBitmap.imageResource(ref.resourceId),
            contentDescription = ammoFamilyLabel(family),
            modifier = modifier,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None
        )
    }
}

internal data class AmmoMatrixColumn(val level: Int?, val label: String)

internal fun ammoMatrixColumns(shape: BowgunAmmoSourceShape): List<AmmoMatrixColumn> = when (shape) {
    BowgunAmmoSourceShape.THREE_LEVEL -> listOf(
        AmmoMatrixColumn(1, "Lv1"), AmmoMatrixColumn(2, "Lv2"), AmmoMatrixColumn(3, "Lv3")
    )
    BowgunAmmoSourceShape.TWO_LEVEL -> listOf(
        AmmoMatrixColumn(1, "Lv1"), AmmoMatrixColumn(2, "Lv2")
    )
    BowgunAmmoSourceShape.SINGLE_CAPACITY -> listOf(AmmoMatrixColumn(null, "Load"))
}

internal data class AmmoMatrixGroup(
    val key: String,
    val title: String,
    val families: Set<BowgunAmmoFamily>,
    val sourceShape: BowgunAmmoSourceShape
)

internal val ammoMatrixGroups: List<AmmoMatrixGroup> = listOf(
    AmmoMatrixGroup(
        "physical", "Physical",
        setOf(BowgunAmmoFamily.NORMAL_S, BowgunAmmoFamily.PIERCE_S, BowgunAmmoFamily.PELLET_S, BowgunAmmoFamily.CRAG_S, BowgunAmmoFamily.CLUST_S),
        BowgunAmmoSourceShape.THREE_LEVEL
    ),
    AmmoMatrixGroup(
        "status", "Status / Recovery",
        setOf(BowgunAmmoFamily.RECOV_S, BowgunAmmoFamily.POISON_S, BowgunAmmoFamily.PARA_S, BowgunAmmoFamily.SLEEP_S, BowgunAmmoFamily.EXHAUST_S),
        BowgunAmmoSourceShape.TWO_LEVEL
    ),
    AmmoMatrixGroup(
        "element", "Element",
        setOf(BowgunAmmoFamily.FLAMING_S, BowgunAmmoFamily.WATER_S, BowgunAmmoFamily.THUNDER_S, BowgunAmmoFamily.FREEZE_S, BowgunAmmoFamily.DRAGON_S),
        BowgunAmmoSourceShape.SINGLE_CAPACITY
    ),
    AmmoMatrixGroup(
        "utility", "Utility",
        setOf(BowgunAmmoFamily.TRANQ_S, BowgunAmmoFamily.PAINT_S, BowgunAmmoFamily.DEMON_S, BowgunAmmoFamily.ARMOR_S, BowgunAmmoFamily.SLICING_S),
        BowgunAmmoSourceShape.SINGLE_CAPACITY
    )
)

internal fun ammoCapacityLabel(capacity: Int?): String = when {
    capacity == null || capacity == 0 -> "—"
    else -> capacity.toString()
}

private fun ammoFamilyLabel(family: BowgunAmmoFamily): String = when (family) {
    BowgunAmmoFamily.NORMAL_S -> "Normal S"
    BowgunAmmoFamily.PIERCE_S -> "Pierce S"
    BowgunAmmoFamily.PELLET_S -> "Pellet S"
    BowgunAmmoFamily.CRAG_S -> "Crag S"
    BowgunAmmoFamily.CLUST_S -> "Clust S"
    BowgunAmmoFamily.RECOV_S -> "Recov S"
    BowgunAmmoFamily.POISON_S -> "Poison S"
    BowgunAmmoFamily.PARA_S -> "Para S"
    BowgunAmmoFamily.SLEEP_S -> "Sleep S"
    BowgunAmmoFamily.EXHAUST_S -> "Exhaust S"
    BowgunAmmoFamily.FLAMING_S -> "Flaming S"
    BowgunAmmoFamily.WATER_S -> "Water S"
    BowgunAmmoFamily.THUNDER_S -> "Thunder S"
    BowgunAmmoFamily.FREEZE_S -> "Freeze S"
    BowgunAmmoFamily.DRAGON_S -> "Dragon S"
    BowgunAmmoFamily.TRANQ_S -> "Tranq S"
    BowgunAmmoFamily.PAINT_S -> "Paint S"
    BowgunAmmoFamily.DEMON_S -> "Demon S"
    BowgunAmmoFamily.ARMOR_S -> "Armor S"
    BowgunAmmoFamily.SLICING_S -> "Slicing S"
}

private fun ammoRoundLabel(round: BowgunRound): String = ammoFamilyLabel(round.ammoType) + (round.level?.let { " Lv$it" } ?: "")

private fun Enum<*>.displayLabel(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun deviationLabel(magnitude: String, direction: String): String = when {
    magnitude == "NONE" -> "None"
    direction == "NONE" -> magnitude.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    else -> "${magnitude.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} ${direction.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }}"
}

private fun hornNoteColor(note: HornNote): Color = when (note) {
    HornNote.WHITE -> AppColors.Parchment
    HornNote.PURPLE -> Color(0xFF76558F)
    HornNote.RED -> Color(0xFFB4473A)
    HornNote.BLUE -> Color(0xFF4C79A8)
    HornNote.GREEN -> Color(0xFF5F8B5A)
    HornNote.CYAN -> Color(0xFF4A9EA5)
    HornNote.YELLOW -> Color(0xFFD5B84A)
    HornNote.ORANGE -> Color(0xFFD77C3F)
}

@Composable
private fun WeaponStats(weapon: Weapon) {
    Row(Modifier.fillMaxWidth().testTag("weapon-stats"), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        WeaponStat("Attack", weapon.attack.toString(), Modifier.weight(1f))
        weapon.attackBoosted?.let { WeaponStat("+15", it.toString(), Modifier.weight(1f)) }
        WeaponStat("Affinity", "${weapon.affinityPercent}%", Modifier.weight(1f))
        WeaponStat("Slots", if (weapon.slots == 0) "—" else "●".repeat(weapon.slots), Modifier.weight(1f))
        if (weapon.defenseBonus != null) WeaponStat("Defense", "+${weapon.defenseBonus}", Modifier.weight(1f))
    }
    weapon.special?.let { special ->
        Text(
            "${special.type.name.lowercase().replaceFirstChar { it.uppercase() }} ${special.value}",
            color = AppColors.Ink.copy(alpha = .78f),
            style = AppType.Metadata,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun WeaponStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(AppColors.ParchmentDeep.copy(alpha = .27f)).padding(5.dp)) {
        Text(label, color = AppColors.Ink.copy(alpha = .65f), style = AppType.Metadata, maxLines = 1)
        Text(value, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
    }
}

private val WeaponRankSpanStyle = SpanStyle(
    color = AppColors.Ink.copy(alpha = .62f),
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold
)

private val WeaponRecipeSurfaceColor = AppColors.ParchmentDeep.copy(alpha = .22f)

/** Rank is always read from the canonical production rarity field. */
private fun weaponRankedTitle(weapon: Weapon): AnnotatedString = buildAnnotatedString {
    withStyle(WeaponRankSpanStyle) { append("[R${weapon.rarity}]") }
    append(" ${weapon.name}")
}

private const val WeaponReferenceAnnotation = "weapon-id"

private fun weaponTransitionTitle(
    from: Weapon,
    to: Weapon,
    linkWeaponId: String? = null
): AnnotatedString = buildAnnotatedString {
    withStyle(WeaponRankSpanStyle) { append("[R${from.rarity}]") }
    append(" ")
    if (from.id == linkWeaponId) {
        pushStringAnnotation(WeaponReferenceAnnotation, from.id)
        withStyle(SpanStyle(color = AppColors.Crimson, textDecoration = TextDecoration.Underline)) { append(from.name) }
        pop()
    } else append(from.name)
    append(" → ")
    withStyle(WeaponRankSpanStyle) { append("[R${to.rarity}]") }
    append(" ${to.name}")
}

@Composable
private fun ResponsiveAcquisitionSections(
    sections: List<WeaponAcquisitionSectionSpec>,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    onSelectWeapon: (Weapon) -> Unit
) {
    if (sections.isEmpty()) return
    BoxWithConstraints(
        Modifier.fillMaxWidth().testTag("weapon-acquisition-sections")
    ) {
        val useTwoColumns = sections.size == 2 && maxWidth >= 560.dp
        if (useTwoColumns) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                sections.forEach { section ->
                    RecipeSectionColumn(
                        section = section,
                        catalog = catalog,
                        onItem = onItem,
                        onSelectWeapon = onSelectWeapon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                sections.forEach { section ->
                    RecipeSectionColumn(section, catalog, onItem, onSelectWeapon, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private data class WeaponAcquisitionSectionSpec(
    val sectionTitle: String,
    val title: AnnotatedString,
    val method: String,
    val recipe: WeaponRecipe,
    val tag: String,
    val iconWeapon: Weapon
)

@Composable
private fun RecipeSectionColumn(
    section: WeaponAcquisitionSectionSpec,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    onSelectWeapon: (Weapon) -> Unit,
    modifier: Modifier
) {
    Column(modifier.testTag("${section.tag}-column")) {
        Text(
            section.sectionTitle,
            color = AppColors.Crimson,
            style = AppType.SectionTitle,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                .testTag("${section.tag}-section")
        )
        WeaponRecipeCard(
            title = section.title,
            method = section.method,
            recipe = section.recipe,
            catalog = catalog,
            onItem = onItem,
            onSelectWeapon = onSelectWeapon,
            modifier = Modifier.testTag(section.tag),
            iconWeapon = section.iconWeapon,
            materialsTag = "${section.tag}-materials"
        )
    }
}

@Composable
private fun WeaponRecipeSection(
    sectionTitle: String,
    title: AnnotatedString,
    method: String,
    recipe: WeaponRecipe,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    tag: String,
    iconWeapon: Weapon
) {
    Text(
        sectionTitle,
        color = AppColors.Crimson,
        style = AppType.SectionTitle,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).testTag("$tag-section")
    )
    WeaponRecipeCard(
        title = title,
        method = method,
        recipe = recipe,
        catalog = catalog,
        onItem = onItem,
        modifier = Modifier.testTag(tag),
        iconWeapon = iconWeapon,
        materialsTag = "$tag-materials"
    )
}

private data class WeaponRecipeCardSpec(
    val title: AnnotatedString,
    val method: String,
    val recipe: WeaponRecipe?,
    val iconWeapon: Weapon,
    val tag: String,
    val onClick: (() -> Unit)? = null
)

@Composable
private fun ResponsiveRecipeCards(
    cards: List<WeaponRecipeCardSpec>,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    sectionTag: String
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag(sectionTag)) {
        // The Thor detail surface is roughly 582dp wide after its modal inset;
        // two cards still have enough room for a readable title/material cell
        // there, while narrow phone layouts naturally fall back to one column.
        val useTwoColumns = cards.size > 1 && maxWidth >= 560.dp
        if (useTwoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                cards.chunked(2).forEachIndexed { rowIndex, row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { card ->
                            WeaponRecipeCard(
                                title = card.title,
                                method = card.method,
                                recipe = card.recipe,
                                catalog = catalog,
                                onItem = onItem,
                                modifier = Modifier.weight(1f).testTag(card.tag),
                                iconWeapon = card.iconWeapon,
                                materialsTag = "${card.tag}-materials",
                                onClick = card.onClick
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                cards.forEach { card ->
                    WeaponRecipeCard(
                        title = card.title,
                        method = card.method,
                        recipe = card.recipe,
                        catalog = catalog,
                        onItem = onItem,
                        modifier = Modifier.fillMaxWidth().testTag(card.tag),
                        iconWeapon = card.iconWeapon,
                        materialsTag = "${card.tag}-materials",
                        onClick = card.onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun WeaponRecipeCard(
    title: AnnotatedString,
    method: String,
    recipe: WeaponRecipe?,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    modifier: Modifier = Modifier,
    iconWeapon: Weapon,
    materialsTag: String,
    onClick: (() -> Unit)? = null,
    onSelectWeapon: ((Weapon) -> Unit)? = null
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Surface(
        modifier = cardModifier,
        color = WeaponRecipeSurfaceColor,
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, AppColors.ParchmentDeep.copy(alpha = .82f))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WeaponIcon(iconWeapon.weaponType, iconWeapon.rarity, Modifier.size(27.dp))
                Spacer(Modifier.width(6.dp))
                val linkedWeaponId = title.getStringAnnotations(WeaponReferenceAnnotation, 0, title.length)
                    .firstOrNull()?.item
                if (linkedWeaponId != null && onSelectWeapon != null) {
                    Text(
                        text = title,
                        color = rarityVisual(iconWeapon.rarity, iconWeapon.weaponType).nameColor,
                        style = AppType.CardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f).clickable {
                            catalog.weaponById[linkedWeaponId]?.let(onSelectWeapon)
                        }
                            .testTag("weapon-related-link-$linkedWeaponId"),
                    )
                } else {
                    Text(
                        title,
                        color = rarityVisual(iconWeapon.rarity, iconWeapon.weaponType).nameColor,
                        style = AppType.CardTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "$method · ${recipe?.zenny?.let { "${it}z" } ?: "Zenny unpublished"}",
                color = AppColors.Ink.copy(alpha = .72f),
                style = AppType.Metadata,
                modifier = Modifier.padding(top = 2.dp).testTag("$materialsTag-method")
            )
            if (recipe?.ingredients?.isNotEmpty() == true) {
                RecipeHeaderDivider(materialsTag)
                WeaponRecipeRows(recipe, catalog, onItem, materialsTag)
            }
        }
    }
}

@Composable
private fun RecipeHeaderDivider(tag: String) {
    HorizontalDivider(
        color = AppColors.ParchmentDeep.copy(alpha = .55f),
        thickness = 1.dp,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
            .testTag("$tag-header-divider")
    )
}

@Composable
private fun WeaponRecipeRows(
    recipe: WeaponRecipe,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    tag: String
) {
    val ingredients = recipe.ingredients
    val left = ingredients.filterIndexed { index, _ -> index % 2 == 0 }
    val right = ingredients.filterIndexed { index, _ -> index % 2 == 1 }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            left.forEachIndexed { rowIndex, material ->
                RecipeMaterialCell(material, rowIndex * 2, catalog, onItem, tag)
            }
        }
        if (right.isNotEmpty()) {
            VerticalDivider(
                color = AppColors.ParchmentDeep.copy(alpha = .55f),
                thickness = 1.dp,
                modifier = Modifier.fillMaxHeight().padding(vertical = 3.dp)
                    .testTag("$tag-column-divider")
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                right.forEachIndexed { rowIndex, material ->
                    RecipeMaterialCell(material, rowIndex * 2 + 1, catalog, onItem, tag)
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RecipeMaterialCell(
    material: com.waillio.mhp3rdcompanion.data.WeaponRecipeIngredient,
    ingredientIndex: Int,
    catalog: WeaponCatalog,
    onItem: (Material) -> Unit,
    tag: String
) {
    val item = material.gameItemId.let(catalog::recipeItem)
    val itemLabel = item?.name ?: "Item #${material.gameItemId}"
    Row(
        Modifier.fillMaxWidth().heightIn(min = 31.dp)
            .then(if (item != null) Modifier.clickable { onItem(item) } else Modifier)
            .padding(horizontal = 2.dp)
            .testTag("$tag-item-${material.gameItemId}-$ingredientIndex"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemIcon(material.gameItemId, itemLabel, Modifier.size(24.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            "$itemLabel · x${material.quantity}",
            color = AppColors.Ink,
            style = AppType.Metadata,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
