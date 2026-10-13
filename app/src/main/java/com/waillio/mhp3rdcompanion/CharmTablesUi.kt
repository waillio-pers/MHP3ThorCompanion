package com.waillio.mhp3rdcompanion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.waillio.mhp3rdcompanion.data.CharmIdentification
import com.waillio.mhp3rdcompanion.data.CharmIdentificationState
import com.waillio.mhp3rdcompanion.data.CharmIdentifyWorkingState
import com.waillio.mhp3rdcompanion.data.CharmObservation
import com.waillio.mhp3rdcompanion.data.CharmPointOperator
import com.waillio.mhp3rdcompanion.data.CharmResultGroup
import com.waillio.mhp3rdcompanion.data.CharmSavedSearch
import com.waillio.mhp3rdcompanion.data.CharmSlotFilterChoice
import com.waillio.mhp3rdcompanion.data.CharmSearchCriteria
import com.waillio.mhp3rdcompanion.data.CharmSlotMode
import com.waillio.mhp3rdcompanion.data.CharmTableIndex
import com.waillio.mhp3rdcompanion.data.CharmTableMode
import com.waillio.mhp3rdcompanion.data.SkillTree
import com.waillio.mhp3rdcompanion.data.TalismanClass
import com.waillio.mhp3rdcompanion.data.CHARM_SEARCH_PREVIEW_LIMIT
import com.waillio.mhp3rdcompanion.data.charmSearchPage
import com.waillio.mhp3rdcompanion.data.charmSearchPageCount
import com.waillio.mhp3rdcompanion.data.charmSearchPreview
import com.waillio.mhp3rdcompanion.data.charmSlotNotation
import com.waillio.mhp3rdcompanion.data.charmSlotFilterChoices

private enum class CharmTablesPage { SEARCH, IDENTIFY }

@Composable
internal fun CharmTablesScreen(
    index: CharmTableIndex?,
    skillTrees: List<SkillTree>,
    currentTable: Int?,
    loadingFailed: Boolean,
    identifyWorkingState: CharmIdentifyWorkingState,
    savedSearches: List<CharmSavedSearch> = emptyList(),
    onUpdateIdentifyWorkingState: ((CharmIdentifyWorkingState) -> CharmIdentifyWorkingState) -> Unit,
    onSaveCurrentTable: (Int?) -> Unit,
    onSaveSearch: (String, CharmSearchCriteria) -> Unit = { _, _ -> },
    onDeleteSearch: (String) -> Unit = {}
) {
    if (index == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (loadingFailed) {
                Text("Charm table data could not be loaded.", color = AppColors.Parchment, style = AppType.SectionTitle)
                Text("The production game-reference database was not changed.", color = AppColors.ParchmentDeep, style = AppType.Body)
            } else {
                CircularProgressIndicator(color = AppColors.Gold)
                Text("Loading historical Timeworn tables…", color = AppColors.Parchment, style = AppType.Body)
            }
        }
        return
    }

    var page by rememberSaveable { mutableStateOf(CharmTablesPage.SEARCH.name) }
    val selectedPage = CharmTablesPage.entries.firstOrNull { it.name == page } ?: CharmTablesPage.SEARCH
    val skillsById = remember(skillTrees) { skillTrees.associateBy { it.stableSkillTreeId } }
    val skill1Choices = remember(index) { index.skillIdsAtPosition(1) }
    val skill2Choices = remember(index) { index.skillIdsAtPosition(2) }
    val availableClasses = remember(index) { index.availableClasses() }

    var skill1Id by rememberSaveable { mutableStateOf<String?>(null) }
    var skill1Points by rememberSaveable { mutableStateOf("") }
    var skill1OperatorName by rememberSaveable { mutableStateOf(CharmPointOperator.GREATER_OR_EQUAL.name) }
    var skill2Id by rememberSaveable { mutableStateOf<String?>(null) }
    var skill2Points by rememberSaveable { mutableStateOf("") }
    var skill2OperatorName by rememberSaveable { mutableStateOf(CharmPointOperator.GREATER_OR_EQUAL.name) }
    var slots by rememberSaveable { mutableStateOf<Int?>(null) }
    var slotsExact by rememberSaveable { mutableStateOf(false) }
    var tableModeName by rememberSaveable { mutableStateOf(CharmTableMode.ANY.name) }
    var explicitTable by rememberSaveable { mutableIntStateOf(1) }
    var className by rememberSaveable { mutableStateOf("") }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAllResults by rememberSaveable { mutableStateOf(false) }
    var resultPage by rememberSaveable { mutableIntStateOf(0) }
    val tableMode = CharmTableMode.entries.firstOrNull { it.name == tableModeName } ?: CharmTableMode.ANY
    val skill1Operator = CharmPointOperator.entries.firstOrNull { it.name == skill1OperatorName } ?: CharmPointOperator.GREATER_OR_EQUAL
    val skill2Operator = CharmPointOperator.entries.firstOrNull { it.name == skill2OperatorName } ?: CharmPointOperator.GREATER_OR_EQUAL
    val talismanClass = availableClasses.firstOrNull { it.name == className }
    val criteria = remember(skill1Id, skill1Points, skill1Operator, skill2Id, skill2Points, skill2Operator, slots, slotsExact, tableMode, explicitTable, talismanClass) {
        CharmSearchCriteria(
            skill1Id = skill1Id,
            skill1Points = skill1Points.toIntOrNull(),
            skill1Mode = skill1Operator,
            skill2Id = skill2Id,
            skill2Points = skill2Points.toIntOrNull(),
            skill2Mode = skill2Operator,
            slots = slots,
            slotMode = if (slotsExact) CharmSlotMode.EXACT else CharmSlotMode.MINIMUM,
            tableMode = tableMode,
            explicitTable = explicitTable,
            talismanClass = talismanClass
        )
    }
    val searchResults = remember(index, criteria, currentTable) { index.search(criteria, currentTable) }
    val identifyObservations = identifyWorkingState.observations
    val identifyResult = remember(index, identifyObservations) { index.identify(identifyObservations) }
    val listState = rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(criteria, currentTable) { resultPage = 0 }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("screen-charm-tables"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 7.dp)
    ) {
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 560.dp) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Charm Tables", color = AppColors.Parchment, style = AppType.ScreenTitle, modifier = Modifier.weight(1f).testTag("charm-tables-title"), maxLines = 1)
                        CompactCurrentTable(currentTable, onSaveCurrentTable)
                        CharmHelpButton { showHelp = true }
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Charm Tables", color = AppColors.Parchment, style = AppType.ScreenTitle, modifier = Modifier.weight(1f).testTag("charm-tables-title"), maxLines = 1)
                            CharmHelpButton { showHelp = true }
                        }
                        CompactCurrentTable(currentTable, onSaveCurrentTable, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CharmTablesPage.SEARCH to "Search", CharmTablesPage.IDENTIFY to "Identify").forEach { (which, label) ->
                    val selected = selectedPage == which
                    FilterChip(
                        selected = selected,
                        onClick = { page = which.name },
                        label = { Text(label) },
                        shape = AppDimens.ButtonShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.Plum,
                            selectedLabelColor = AppColors.Parchment,
                            containerColor = AppColors.NightRaised,
                            labelColor = AppColors.Parchment
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = AppColors.ParchmentDeep.copy(alpha = .65f), selectedBorderColor = AppColors.Gold),
                        modifier = Modifier.weight(1f).testTag(if (which == CharmTablesPage.SEARCH) "charm-page-search" else "charm-page-identify")
                    )
                }
            }
        }
        if (selectedPage == CharmTablesPage.SEARCH) {
            item {
                CharmSearchFilters(
                    skillTrees = skillTrees,
                    skillsById = skillsById,
                    skill1Choices = skill1Choices,
                    skill2Choices = skill2Choices,
                    availableClasses = availableClasses,
                    skill1Id = skill1Id,
                    onSkill1 = { skill1Id = it; if (it == null) skill1Points = "" },
                    skill1Points = skill1Points,
                    onSkill1Points = { skill1Points = filterSignedPoints(it) },
                    skill1Operator = skill1Operator,
                    onSkill1Operator = { skill1OperatorName = it.name },
                    skill2Id = skill2Id,
                    onSkill2 = { skill2Id = it; if (it == null) skill2Points = "" },
                    skill2Points = skill2Points,
                    onSkill2Points = { skill2Points = filterSignedPoints(it) },
                    skill2Operator = skill2Operator,
                    onSkill2Operator = { skill2OperatorName = it.name },
                    slots = slots,
                    onSlots = { value, exact -> slots = value; slotsExact = exact },
                    tableMode = tableMode,
                    explicitTable = explicitTable,
                    currentTable = currentTable,
                    onTableMode = { mode, table -> tableModeName = mode.name; table?.let { explicitTable = it } },
                    talismanClass = talismanClass,
                    onClass = { className = it?.name.orEmpty() },
                    savedSearches = savedSearches,
                    criteria = criteria,
                    onRestoreSearch = { restored ->
                        skill1Id = restored.skill1Id
                        skill1Points = restored.skill1Points?.toString().orEmpty()
                        skill1OperatorName = restored.skill1Mode.name
                        skill2Id = restored.skill2Id
                        skill2Points = restored.skill2Points?.toString().orEmpty()
                        skill2OperatorName = restored.skill2Mode.name
                        slots = restored.slots
                        slotsExact = restored.slotMode == CharmSlotMode.EXACT
                        tableModeName = restored.tableMode.name
                        explicitTable = restored.explicitTable ?: 1
                        className = restored.talismanClass?.name.orEmpty()
                        resultPage = 0
                    },
                    onSaveSearch = onSaveSearch,
                    onDeleteSearch = onDeleteSearch
                )
            }
            item {
                when {
                    !criteria.hasFilters -> EmptyInfoCard("Choose a skill or another filter", "Find exact Timeworn outcomes across Tables 1–12.", "charm-search-start")
                    tableMode == CharmTableMode.CURRENT && currentTable == null -> EmptyInfoCard("Current Table is unknown", "Choose a specific table or set your Current Table first.", "charm-search-current-unknown")
                    searchResults.isEmpty() -> EmptyInfoCard("No matching charms", "No charms match the current filters.", "charm-search-empty")
                    else -> Text(
                        if (searchResults.size > CHARM_SEARCH_PREVIEW_LIMIT) "${searchResults.size} results · showing first $CHARM_SEARCH_PREVIEW_LIMIT" else "${searchResults.size} results",
                        color = AppColors.Gold, style = AppType.SectionTitle, modifier = Modifier.testTag("charm-search-count")
                    )
                }
            }
            items(charmSearchPreview(searchResults), key = { group -> group.signature.toStableKey() }) { group ->
                CharmResultRow(group, index, skillsById)
            }
            if (searchResults.size > CHARM_SEARCH_PREVIEW_LIMIT) {
                item {
                    OutlinedButton(
                        onClick = { resultPage = 0; showAllResults = true },
                        modifier = Modifier.fillMaxWidth().testTag("charm-view-all"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Parchment),
                        border = BorderStroke(1.dp, AppColors.Gold)
                    ) { Text("View all ${searchResults.size} results") }
                }
            }
        } else {
            item {
                IdentifyWorkflow(
                    index = index,
                    skillTrees = skillTrees,
                    skillsById = skillsById,
                    skill1Choices = skill1Choices,
                    skill2Choices = skill2Choices,
                    availableClasses = availableClasses,
                    identifyState = identifyWorkingState,
                    onUpdate = onUpdateIdentifyWorkingState,
                    poolIdentification = identifyResult,
                    currentTable = currentTable,
                    onSaveCurrentTable = onSaveCurrentTable
                )
            }
        }
    }

    if (showAllResults) {
        BackHandler { showAllResults = false }
        AllCharmResultsDialog(
            groups = searchResults,
            pageIndex = resultPage,
            onPage = { resultPage = it },
            index = index,
            skillsById = skillsById,
            onDismiss = { showAllResults = false }
        )
    }
    if (showHelp) CharmHelpDialog(onDismiss = { showHelp = false })
}

@Composable
private fun CharmHelpButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(34.dp).clickable(onClick = onClick).testTag("charm-help-open"),
        shape = AppDimens.ButtonShape,
        color = AppColors.Plum,
        border = BorderStroke(1.dp, AppColors.Gold)
    ) {
        Box(contentAlignment = Alignment.Center) { Text("?", color = AppColors.Parchment, style = AppType.ButtonLabel) }
    }
}

@Composable
private fun CompactCurrentTable(currentTable: Int?, onSaveCurrentTable: (Int?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier.heightIn(min = 36.dp).testTag("current-charm-table-card"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Current Table:", color = AppColors.Parchment, style = AppType.Metadata, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(currentTable?.let { "T$it" } ?: "Unknown", color = AppColors.Gold, style = AppType.ButtonLabel, modifier = Modifier.testTag("current-charm-table-value"), maxLines = 1)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = 32.dp).testTag("current-charm-table-change"), contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Parchment), border = BorderStroke(1.dp, AppColors.ParchmentDeep)) {
                    Text("Change", style = AppType.Metadata)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Unknown") }, onClick = { onSaveCurrentTable(null); expanded = false })
                    (1..12).forEach { table -> DropdownMenuItem(text = { Text("Table $table") }, onClick = { onSaveCurrentTable(table); expanded = false }) }
                }
            }
            if (currentTable != null) IconButton(onClick = { onSaveCurrentTable(null) }, modifier = Modifier.size(32.dp).testTag("current-charm-table-clear")) {
                Icon(Icons.Default.Close, contentDescription = "Clear current table", tint = AppColors.Gold)
            }
    }
}

@Composable
private fun CharmSearchFilters(
    skillTrees: List<SkillTree>,
    skillsById: Map<String, SkillTree>,
    skill1Choices: Set<String>, skill2Choices: Set<String>, availableClasses: List<TalismanClass>,
    skill1Id: String?, onSkill1: (String?) -> Unit,
    skill1Points: String, onSkill1Points: (String) -> Unit, skill1Operator: CharmPointOperator, onSkill1Operator: (CharmPointOperator) -> Unit,
    skill2Id: String?, onSkill2: (String?) -> Unit,
    skill2Points: String, onSkill2Points: (String) -> Unit, skill2Operator: CharmPointOperator, onSkill2Operator: (CharmPointOperator) -> Unit,
    slots: Int?, onSlots: (Int?, Boolean) -> Unit,
    tableMode: CharmTableMode, explicitTable: Int, currentTable: Int?, onTableMode: (CharmTableMode, Int?) -> Unit,
    talismanClass: TalismanClass?, onClass: (TalismanClass?) -> Unit,
    savedSearches: List<CharmSavedSearch>, criteria: CharmSearchCriteria,
    onRestoreSearch: (CharmSearchCriteria) -> Unit, onSaveSearch: (String, CharmSearchCriteria) -> Unit, onDeleteSearch: (String) -> Unit
) {
    SectionCard(title = "Search", accent = AppColors.Plum, contentPadding = 7.dp, modifier = Modifier.testTag("charm-search-filters")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            CharmClassSelector("Class / Rarity", talismanClass, availableClasses, "charm-search-class", onClass, Modifier.weight(1f))
            CharmSlotSelector(slots, criteria.slotMode == CharmSlotMode.EXACT, "charm-search-slots", onSlots, Modifier.weight(1f))
        }
        Spacer(Modifier.height(5.dp))
        CharmSearchSkillLine("Skill 1", skillTrees, skill1Choices, skill1Id, skillsById[skill1Id]?.displayName, false, onSkill1, skill1Points, onSkill1Points, skill1Operator, onSkill1Operator, "charm-search-skill1")
        Spacer(Modifier.height(4.dp))
        CharmSearchSkillLine("Skill 2", skillTrees, skill2Choices, skill2Id, skillsById[skill2Id]?.displayName, true, onSkill2, skill2Points, onSkill2Points, skill2Operator, onSkill2Operator, "charm-search-skill2")
        Spacer(Modifier.height(5.dp))
        TableFilterControl(tableMode, explicitTable, currentTable, onTableMode)
        Spacer(Modifier.height(3.dp))
        SavedSearchesPanel(savedSearches, criteria, skillsById, onRestoreSearch, onSaveSearch, onDeleteSearch)
    }
}

@Composable
private fun CharmSearchSkillLine(
    title: String,
    skillTrees: List<SkillTree>,
    allowedSkillIds: Set<String>,
    selectedId: String?,
    selectedName: String?,
    allowNone: Boolean,
    onSelect: (String?) -> Unit,
    points: String,
    onPoints: (String) -> Unit,
    operator: CharmPointOperator,
    onOperator: (CharmPointOperator) -> Unit,
    tag: String
) {
    if (selectedId == null) {
        CharmSkillPicker(title, skillTrees, allowedSkillIds, selectedName, allowNone, onSelect, tag)
    } else {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 460.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    CharmSkillPicker(title, skillTrees, allowedSkillIds, selectedName, allowNone, onSelect, tag, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        SignedPointsField("Points", points, onPoints, "$tag-points", Modifier.weight(1f))
                        OperatorMenu(operator, onOperator, "$tag-points-operator", Modifier.width(64.dp))
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    CharmSkillPicker(title, skillTrees, allowedSkillIds, selectedName, allowNone, onSelect, tag, Modifier.weight(1.3f))
                    SignedPointsField("Points", points, onPoints, "$tag-points", Modifier.weight(.8f))
                    OperatorMenu(operator, onOperator, "$tag-points-operator", Modifier.width(64.dp))
                }
            }
        }
    }
}

@Composable
private fun OperatorMenu(operator: CharmPointOperator, onOperator: (CharmPointOperator) -> Unit, tag: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag(tag), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, AppColors.ParchmentDeep), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text(operator.symbol, style = AppType.CardTitle)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CharmPointOperator.choices.forEach { option ->
                DropdownMenuItem(text = { Text(option.symbol) }, onClick = { onOperator(option); open = false }, modifier = Modifier.testTag("$tag-${option.name.lowercase()}"))
            }
        }
    }
}

@Composable
private fun CharmSlotSelector(slots: Int?, exact: Boolean, tag: String, onSelect: (Int?, Boolean) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val selected = charmSlotFilterChoices.firstOrNull { it.slots == slots && (it.mode == CharmSlotMode.EXACT) == exact }?.label ?: "Any"
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag(tag), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, AppColors.ParchmentDeep)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("Slots", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                Text(selected, color = AppColors.Ink, style = AppType.Body, maxLines = 1)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.widthIn(min = 190.dp)) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                charmSlotFilterChoices.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        pair.forEach { choice ->
                            FilterChip(
                                selected = choice.slots == slots && (choice.mode == CharmSlotMode.EXACT) == exact,
                                onClick = { onSelect(choice.slots, choice.mode == CharmSlotMode.EXACT); open = false },
                                label = { Text(choice.label) },
                                modifier = Modifier.weight(1f).testTag("$tag-choice-${choice.label.replace('=', 'e').replace('>', 'g') }"),
                                colors = charmFilterChipColors(onParchment = false),
                                border = charmFilterChipBorder(choice.slots == slots && (choice.mode == CharmSlotMode.EXACT) == exact, false)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharmClassSelector(
    title: String, selected: TalismanClass?, available: List<TalismanClass>, tag: String,
    onSelect: (TalismanClass?) -> Unit, modifier: Modifier = Modifier, isError: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag(tag), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, if (isError) AppColors.Crimson else AppColors.ParchmentDeep)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(title, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    selected?.let { CharmClassMark(it, Modifier.size(16.dp)) }
                    Text(selected?.label ?: "Any", color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Any") }, onClick = { onSelect(null); open = false }, modifier = Modifier.testTag("$tag-any"))
            available.forEach { rarity ->
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CharmClassMark(rarity, Modifier.size(20.dp)); Text(rarity.label) } },
                    onClick = { onSelect(rarity); open = false }, modifier = Modifier.testTag("$tag-${rarity.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun CharmClassMark(talismanClass: TalismanClass, modifier: Modifier = Modifier) {
    val resourceId = when (talismanClass) {
        TalismanClass.QUEEN -> R.drawable.charm_rarity_queen
        TalismanClass.KING -> R.drawable.charm_rarity_king
        TalismanClass.DRAGON -> R.drawable.charm_rarity_dragon
    }
    Image(
        bitmap = ImageBitmap.imageResource(resourceId),
        contentDescription = "${talismanClass.label} rarity icon",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = modifier
    )
}

@Composable
private fun TableFilterControl(tableMode: CharmTableMode, explicitTable: Int, currentTable: Int?, onSelect: (CharmTableMode, Int?) -> Unit) {
    val label = when (tableMode) {
        CharmTableMode.ANY -> "Any Table"
        CharmTableMode.CURRENT -> "Current Table"
        CharmTableMode.EXPLICIT -> "Table $explicitTable"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ChoiceMenu(
                label = "Table", selected = label,
                options = listOf("Any Table") + (1..12).map { "Table $it" } + listOf("Current Table"),
                onSelect = { value -> when {
                    value == "Any Table" -> onSelect(CharmTableMode.ANY, null)
                    value == "Current Table" -> onSelect(CharmTableMode.CURRENT, null)
                    else -> onSelect(CharmTableMode.EXPLICIT, value.substringAfter(' ').toIntOrNull())
                } },
                modifier = Modifier.weight(1f).testTag("charm-search-table")
            )
            OutlinedButton(
                onClick = { onSelect(CharmTableMode.CURRENT, currentTable) },
                enabled = currentTable != null,
                modifier = Modifier.heightIn(min = 46.dp).testTag("charm-use-current-table"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Ink,
                    disabledContentColor = AppColors.Ink.copy(alpha = .68f)
                ),
                border = BorderStroke(1.dp, AppColors.Plum)
            ) { Text("Use current\ntable", style = AppType.Metadata, maxLines = 2) }
        }
        if (currentTable == null) {
            Text("Set a Current Table first.", modifier = Modifier.padding(start = 3.dp, top = 2.dp).testTag("charm-use-current-table-hint"), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
        }
    }
}

@Composable
private fun SavedSearchesPanel(
    savedSearches: List<CharmSavedSearch>, criteria: CharmSearchCriteria, skillsById: Map<String, SkillTree>,
    onRestore: (CharmSearchCriteria) -> Unit, onSave: (String, CharmSearchCriteria) -> Unit, onDelete: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showSave by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var editingName by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { if (savedSearches.isNotEmpty()) expanded = !expanded }, enabled = savedSearches.isNotEmpty(), modifier = Modifier.weight(1f).heightIn(min = 38.dp).testTag("charm-saved-searches-toggle"), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink, disabledContentColor = AppColors.Ink.copy(alpha = .7f)), border = BorderStroke(1.dp, AppColors.ParchmentDeep)) {
            Text("Saved searches (${savedSearches.size})", style = AppType.Metadata, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse saved searches" else "Expand saved searches", tint = AppColors.Crimson, modifier = Modifier.size(18.dp))
        }
        OutlinedButton(onClick = { focusManager.clearFocus(force = true); keyboardController?.hide(); name = defaultCharmSearchName(criteria, skillsById); editingName = false; showSave = true }, modifier = Modifier.heightIn(min = 38.dp).testTag("charm-save-search"), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, AppColors.Plum)) {
            Text("Save search", style = AppType.Metadata)
        }
    }
    if (expanded && savedSearches.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            savedSearches.forEach { search ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onRestore(search.criteria) }.testTag("charm-saved-search-${search.id}"),
                        color = AppColors.ParchmentDeep.copy(alpha = .28f),
                        shape = AppDimens.ButtonShape,
                        border = BorderStroke(1.dp, AppColors.ParchmentDeep.copy(alpha = .75f))
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(search.name, modifier = Modifier.weight(1f), color = AppColors.Ink, style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ChevronRight, "Load saved search", tint = AppColors.Crimson, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { onDelete(search.id) }, modifier = Modifier.size(32.dp).testTag("charm-saved-search-delete-${search.id}")) { Icon(Icons.Default.Close, "Delete saved search", tint = AppColors.Crimson) }
                }
            }
        }
    }
    if (showSave) {
        TrackedDialog(onDismissRequest = { showSave = false }) {
            Surface(Modifier.fillMaxWidth(.9f).widthIn(max = 430.dp).testTag("charm-save-search-dialog"), color = AppColors.Parchment, shape = AppDimens.CardShape, border = BorderStroke(1.dp, AppColors.Plum)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save search", color = AppColors.Ink, style = AppType.SectionTitle)
                    if (editingName) {
                        OutlinedTextField(value = name, onValueChange = { name = it.take(48) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("charm-save-search-name"), colors = charmTextFieldColors())
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f).testTag("charm-save-search-name"), color = AppColors.Ink, style = AppType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            OutlinedButton(onClick = { editingName = true }, modifier = Modifier.testTag("charm-save-search-edit-name"), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, AppColors.ParchmentDeep)) { Text("Edit") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showSave = false; editingName = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(onClick = { focusManager.clearFocus(force = true); keyboardController?.hide(); onSave(name, criteria); showSave = false; editingName = false }, modifier = Modifier.weight(1f).testTag("charm-save-search-confirm"), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Plum, contentColor = AppColors.Parchment)) { Text("Save") }
                    }
                }
            }
        }
    }
}

internal fun defaultCharmSearchName(criteria: CharmSearchCriteria, skillsById: Map<String, SkillTree>): String {
    val parts = mutableListOf<String>()
    fun addSkill(id: String?, points: Int?, operator: CharmPointOperator) {
        id?.let { skill ->
            val label = skillsById[skill]?.displayName ?: skill
            parts += if (points == null) label else "$label ${operator.symbol} ${formatPoints(points)}"
        }
    }
    addSkill(criteria.skill1Id, criteria.skill1Points, criteria.skill1Mode)
    addSkill(criteria.skill2Id, criteria.skill2Points, criteria.skill2Mode)
    criteria.slots?.let { parts += (if (criteria.slotMode == CharmSlotMode.EXACT) "=" else ">=") + it + " slots" }
    criteria.talismanClass?.let { parts += it.label }
    when (criteria.tableMode) {
        CharmTableMode.ANY -> Unit
        CharmTableMode.CURRENT -> parts += "Current Table"
        CharmTableMode.EXPLICIT -> parts += "Table ${criteria.explicitTable ?: 1}"
    }
    return parts.joinToString(" · ").ifBlank { "Charm search" }.take(48)
}

@Composable
private fun CharmResultRow(group: CharmResultGroup, index: CharmTableIndex, skillsById: Map<String, SkillTree>, tagPrefix: String = "charm-result") {
    val signature = group.signature
    val rarity = index.classAt(signature.classIndex)
    val skill1 = index.skillTreeId(signature.skill1Index)?.let { skillsById[it]?.displayName ?: it } ?: "Unknown"
    val skill2 = index.skillTreeId(signature.skill2Index)?.let { skillsById[it]?.displayName ?: it }
    Surface(Modifier.fillMaxWidth().testTag("$tagPrefix-${signature.toStableKey()}"), color = AppColors.Parchment, shape = AppDimens.ButtonShape, border = BorderStroke(1.dp, AppColors.ParchmentDeep)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 540.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rarity != null) CharmClassMark(rarity, Modifier.size(20.dp))
                    Text(rarity?.label ?: "?", Modifier.width(54.dp), color = AppColors.Ink, style = AppType.Metadata, maxLines = 1)
                    Column(Modifier.weight(1.15f)) {
                        Text("$skill1 ${formatPoints(signature.skill1Points)}", color = AppColors.Ink, style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(skill2?.let { "$it ${formatPoints(signature.skill2Points)}" } ?: "—", color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(charmSlotNotation(signature.slots), Modifier.width(42.dp).testTag("charm-result-slots"), color = AppColors.Ink, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                    Text(group.tables.joinToString("·") { "T$it" }, Modifier.widthIn(min = 44.dp, max = 105.dp).testTag("charm-result-tables"), color = AppColors.Plum, style = AppType.Metadata, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (rarity != null) CharmClassMark(rarity, Modifier.size(18.dp))
                        Text(rarity?.label ?: "?", color = AppColors.Ink, style = AppType.Metadata, maxLines = 1)
                        Text("${charmSlotNotation(signature.slots)} · ${group.tables.joinToString("·") { "T$it" }}", Modifier.weight(1f).testTag("charm-result-tables"), color = AppColors.Plum, style = AppType.Metadata, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("$skill1 ${formatPoints(signature.skill1Points)}", color = AppColors.Ink, style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(skill2?.let { "$it ${formatPoints(signature.skill2Points)}" } ?: "—", color = AppColors.Ink.copy(alpha = .78f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AllCharmResultsDialog(
    groups: List<CharmResultGroup>, pageIndex: Int, onPage: (Int) -> Unit, index: CharmTableIndex,
    skillsById: Map<String, SkillTree>, onDismiss: () -> Unit
) {
    val pageCount = charmSearchPageCount(groups.size)
    val safePage = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val pageGroups = remember(groups, safePage) { charmSearchPage(groups, safePage) }
    BackHandler(onBack = onDismiss)
    TrackedDialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxWidth(.98f).fillMaxHeight(.96f).testTag("charm-view-all-dialog"), color = AppColors.Night, shape = AppDimens.CardShape, border = BorderStroke(1.dp, AppColors.Gold)) {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("All ${groups.size} results", color = AppColors.Parchment, style = AppType.SectionTitle, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp).testTag("charm-view-all-close")) { Icon(Icons.Default.Close, "Close all results", tint = AppColors.Parchment) }
                }
                Text("Stable order: class, Skill 1, points, Skill 2, points, then slots.", color = AppColors.ParchmentDeep, style = AppType.Metadata)
                LazyColumn(Modifier.weight(1f).padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(pageGroups, key = { it.signature.toStableKey() }) { group -> CharmResultRow(group, index, skillsById, "charm-all-result") }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onPage(safePage - 1) }, enabled = safePage > 0, modifier = Modifier.testTag("charm-results-previous")) { Text("Previous") }
                    Text("Page ${safePage + 1} / ${pageCount.coerceAtLeast(1)}", color = AppColors.Parchment, style = AppType.Metadata, modifier = Modifier.weight(1f).testTag("charm-results-page"))
                    OutlinedButton(onClick = { onPage(safePage + 1) }, enabled = safePage + 1 < pageCount, modifier = Modifier.testTag("charm-results-next")) { Text("Next") }
                }
            }
        }
    }
}

@Composable
private fun CharmHelpDialog(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    TrackedDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(.9f).widthIn(max = 520.dp).testTag("charm-help-dialog"), color = AppColors.Parchment, shape = AppDimens.CardShape, border = BorderStroke(1.dp, AppColors.Plum)) {
            Column(Modifier.padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Charm Tables", color = AppColors.Ink, style = AppType.SectionTitle)
                Text("WHAT ARE CHARM TABLES?", color = AppColors.Crimson, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                Text("A full game startup selects one of the standard charm tables. It affects which Timeworn Charm results can occur.", modifier = Modifier.testTag("charm-help-what"), color = AppColors.Ink, style = AppType.Body)
                Text("HOW DOES THE TABLE CHANGE?", color = AppColors.Crimson, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                Text("The table is selected when the game fully starts. A full restart may give you another table; sleeping or returning to character selection is not guaranteed to change it. This app cannot read the active table automatically.", modifier = Modifier.testTag("charm-help-change"), color = AppColors.Ink, style = AppType.Body)
                Text("HOW TO USE THIS TOOL", color = AppColors.Crimson, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                Text("Search to find possible Timeworn Charms. Set Current Table to restrict searches when you already know it. In Identify, enter appraised charms; adding more exact charms narrows the possible tables. Save the result as Current Table to store it on this device.", modifier = Modifier.testTag("charm-help-tool"), color = AppColors.Ink, style = AppType.Body)
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End).testTag("charm-help-close"), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Plum, contentColor = AppColors.Parchment)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun SignedPointsField(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { updated ->
            fieldValue = updated
            onValue(updated.text)
        },
        label = { Text(title) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = {
            IconButton(
                onClick = {
                    val toggled = when {
                        value.isEmpty() -> "-"
                        value == "-" -> ""
                        value.startsWith('-') -> value.drop(1)
                        else -> "-$value"
                    }
                    fieldValue = TextFieldValue(toggled, selection = TextRange(toggled.length))
                    onValue(toggled)
                },
                modifier = Modifier.size(36.dp).testTag("$tag-sign-toggle")
            ) {
                Text("±", modifier = Modifier.semantics { contentDescription = "Toggle negative sign" })
            }
        },
        modifier = modifier.testTag(tag),
        isError = isError,
        colors = charmTextFieldColors()
    )
}

@Composable
private fun IdentifyWorkflow(
    index: CharmTableIndex,
    skillTrees: List<SkillTree>,
    skillsById: Map<String, SkillTree>,
    skill1Choices: Set<String>,
    skill2Choices: Set<String>,
    availableClasses: List<TalismanClass>,
    identifyState: CharmIdentifyWorkingState,
    onUpdate: ((CharmIdentifyWorkingState) -> CharmIdentifyWorkingState) -> Unit,
    poolIdentification: CharmIdentification,
    currentTable: Int?,
    onSaveCurrentTable: (Int?) -> Unit
) {
    val observations = identifyState.observations
    val editingObservation = identifyState.editingObservation
    var formTouched by rememberSaveable { mutableStateOf(false) }
    val formSkill1Id = identifyState.formSkill1Id
    val formSkill2Id = identifyState.formSkill2Id
    val formSkill1Points = identifyState.formSkill1Points
    val formSkill2Points = identifyState.formSkill2Points
    val formSlots = identifyState.formSlots
    val formClass = identifyState.formClass
    val formComplete = formSkill1Id != null && formSkill1Id in skill1Choices && formSkill1Points.toIntOrNull() != null &&
        formSlots != null && formSlots in 0..3 && formClass in availableClasses &&
        (formSkill2Id == null || (formSkill2Id in skill2Choices && formSkill2Points.toIntOrNull() != null))
    val liveObservation = if (formComplete) CharmObservation(
        talismanClass = formClass,
        skill1Id = requireNotNull(formSkill1Id),
        skill1Points = requireNotNull(formSkill1Points.toIntOrNull()),
        skill2Id = formSkill2Id,
        skill2Points = formSkill2Points.toIntOrNull() ?: 0,
        slots = requireNotNull(formSlots)
    ) else null
    val liveIdentification = remember(index, liveObservation) { liveObservation?.let { index.identify(listOf(it)) } }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionCard(title = "Identify a charm", accent = AppColors.Plum, contentPadding = 7.dp, modifier = Modifier.testTag("charm-identify-form")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CharmClassSelector("Class / Rarity", formClass, availableClasses, "charm-identify-class", { value -> formTouched = true; value?.let { onUpdate { state -> state.copy(formClass = it) } } }, Modifier.weight(1f))
                IdentifySlotsSelector(formSlots, "charm-identify-slots", { value -> formTouched = true; onUpdate { state -> state.copy(formSlots = value) } }, Modifier.weight(1f), isError = formTouched && formSlots == null)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                CharmSkillPicker("Skill 1", skillTrees, skill1Choices, skillsById[formSkill1Id]?.displayName, false, { value ->
                    formTouched = true
                    onUpdate { state -> state.copy(formSkill1Id = value, formSkill1Points = if (value == null) "" else state.formSkill1Points) }
                }, "charm-identify-skill1", Modifier.weight(1.35f), isError = formTouched && formSkill1Id !in skill1Choices)
                SignedPointsField("Points", formSkill1Points, { value -> formTouched = true; onUpdate { state -> state.copy(formSkill1Points = filterSignedPoints(value)) } }, "charm-identify-skill1-points", Modifier.weight(.85f), isError = formTouched && formSkill1Points.toIntOrNull() == null)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                CharmSkillPicker("Skill 2", skillTrees, skill2Choices, skillsById[formSkill2Id]?.displayName, true, { value ->
                    formTouched = true
                    onUpdate { state -> state.copy(formSkill2Id = value, formSkill2Points = if (value == null) "" else state.formSkill2Points) }
                }, "charm-identify-skill2", Modifier.weight(1.35f), isError = formTouched && formSkill2Id != null && formSkill2Id !in skill2Choices)
                if (formSkill2Id != null) SignedPointsField("Points", formSkill2Points, { value -> formTouched = true; onUpdate { state -> state.copy(formSkill2Points = filterSignedPoints(value)) } }, "charm-identify-skill2-points", Modifier.weight(.85f), isError = formTouched && formSkill2Points.toIntOrNull() == null)
                else Spacer(Modifier.weight(.85f))
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val charm = liveObservation ?: return@Button
                        val updated = if (editingObservation in observations.indices) observations.toMutableList().also { it[editingObservation] = charm } else observations + charm
                        onUpdate { it.copy(observations = updated, editingObservation = -1, formClass = TalismanClass.QUEEN, formSkill1Id = null, formSkill1Points = "", formSkill2Id = null, formSkill2Points = "", formSlots = null) }
                        formTouched = false
                    },
                    enabled = formComplete,
                    modifier = Modifier.weight(1f).testTag("charm-identify-add"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Plum, contentColor = AppColors.Parchment)
                ) { Text(if (editingObservation in observations.indices) "Update pool item" else "Add to pool") }
                if (editingObservation in observations.indices) OutlinedButton(onClick = {
                    onUpdate { it.copy(editingObservation = -1, formClass = TalismanClass.QUEEN, formSkill1Id = null, formSkill1Points = "", formSkill2Id = null, formSkill2Points = "", formSlots = null) }
                    formTouched = false
                }, modifier = Modifier.testTag("charm-identify-cancel-edit")) { Text("Cancel") }
            }
        }

        if (liveIdentification == null && formTouched) {
            Text("Complete the highlighted fields.", color = AppColors.ParchmentDeep, style = AppType.Metadata, modifier = Modifier.testTag("charm-identify-incomplete"))
        }
        liveIdentification?.let { result ->
            CandidateTableResult("Current charm", result, currentTable, onSaveCurrentTable, "live")
        }

        if (observations.isNotEmpty()) {
            SectionCard(title = "Charm pool · ${observations.size}", accent = AppColors.Plum, contentPadding = 6.dp, modifier = Modifier.testTag("charm-identify-pool")) {
                observations.forEachIndexed { position, observation ->
                    CharmPoolRow(
                        position, observation, skillsById,
                        onEdit = {
                            formTouched = true
                            onUpdate { it.copy(
                                editingObservation = position,
                                formClass = observation.talismanClass,
                                formSkill1Id = observation.skill1Id,
                                formSkill1Points = observation.skill1Points.toString(),
                                formSkill2Id = observation.skill2Id,
                                formSkill2Points = observation.skill2Id?.let { observation.skill2Points.toString() }.orEmpty(),
                                formSlots = observation.slots
                            ) }
                        },
                        onRemove = {
                            onUpdate { state ->
                                val nextEditing = when {
                                    state.editingObservation == position -> -1
                                    state.editingObservation > position -> state.editingObservation - 1
                                    else -> state.editingObservation
                                }
                                state.copy(observations = state.observations.filterIndexed { i, _ -> i != position }, editingObservation = nextEditing)
                            }
                        },
                        modifier = Modifier.testTag("charm-observation-$position")
                    )
                }
            }
            CandidateTableResult("Pool intersection", poolIdentification, currentTable, onSaveCurrentTable, "pool")
        }
    }
}

@Composable
private fun IdentifySlotsSelector(
    slots: Int?, tag: String, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier, isError: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag(tag), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink), border = BorderStroke(1.dp, if (isError) AppColors.Crimson else AppColors.ParchmentDeep)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("Slots", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                Text(slots?.let(::charmSlotNotation) ?: "Select", color = AppColors.Ink, style = AppType.Body)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (0..3).forEach { count ->
                DropdownMenuItem(text = { Text("${charmSlotNotation(count)} · $count") }, onClick = { onSelect(count); open = false }, modifier = Modifier.testTag("$tag-value-$count"))
            }
        }
    }
}

@Composable
private fun CharmPoolRow(
    position: Int, observation: CharmObservation, skillsById: Map<String, SkillTree>,
    onEdit: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth().padding(top = 2.dp), color = AppColors.ParchmentDeep.copy(alpha = .28f), shape = AppDimens.ButtonShape) {
        Row(Modifier.fillMaxWidth().padding(start = 7.dp, end = 1.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CharmClassMark(observation.talismanClass, Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                val first = skillsById[observation.skill1Id]?.displayName ?: observation.skill1Id
                val second = observation.skill2Id?.let { skillsById[it]?.displayName ?: it }?.let { " · $it ${formatPoints(observation.skill2Points)}" }.orEmpty()
                Text("$first ${formatPoints(observation.skill1Points)}$second", color = AppColors.Ink, style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${charmSlotNotation(observation.slots)} · ${observation.talismanClass.label}", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).testTag("charm-observation-edit-$position")) { Icon(Icons.Default.Edit, "Edit pool entry", tint = AppColors.Plum) }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp).testTag("charm-observation-remove-$position")) { Icon(Icons.Default.Close, "Remove pool entry", tint = AppColors.Crimson) }
        }
    }
}

@Composable
private fun CandidateTableResult(
    title: String, identification: CharmIdentification, currentTable: Int?, onSaveCurrentTable: (Int?) -> Unit, source: String
) {
    SectionCard(title = title, accent = AppColors.Plum, contentPadding = 7.dp, modifier = Modifier.testTag("charm-identify-result-$source")) {
        when (identification.state) {
            CharmIdentificationState.NO_OBSERVATIONS -> Text("Enter a charm to check.", color = AppColors.Ink, style = AppType.Metadata)
            CharmIdentificationState.ONE_TABLE -> Text("Possible table: ${identification.tables.single()}", color = AppColors.Plum, style = AppType.SectionTitle, modifier = Modifier.testTag(if (source == "live") "charm-identify-live-one-table" else "charm-identify-pool-one-table"))
            CharmIdentificationState.MULTIPLE_TABLES -> Text("Possible tables:", color = AppColors.Ink, style = AppType.Metadata, modifier = Modifier.testTag(if (source == "live") "charm-identify-live-multiple" else "charm-identify-pool-multiple"))
            CharmIdentificationState.ALL_TABLES -> Text("All 12 tables remain possible.", color = AppColors.Ink, style = AppType.Metadata)
            CharmIdentificationState.NO_MATCH -> Text("No exact table match.", color = AppColors.Crimson, style = AppType.Metadata, modifier = Modifier.testTag(if (source == "live") "charm-identify-live-no-match" else "charm-identify-pool-no-match"))
        }
        if (identification.tables.isNotEmpty()) {
            if (identification.tables.size == 1) {
                val candidate = identification.tables.single()
                Button(
                    onClick = { onSaveCurrentTable(candidate) },
                    enabled = currentTable != candidate,
                    modifier = Modifier.testTag(if (source == "live") "charm-identify-save-table" else "charm-identify-pool-save-table-$candidate"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Plum, contentColor = AppColors.Parchment)
                ) {
                    Text(if (currentTable == candidate) "Table $candidate · current" else "Save Table $candidate")
                }
            } else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    identification.tables.forEach { candidate ->
                        val alreadySaved = currentTable == candidate
                        Button(
                            onClick = { onSaveCurrentTable(candidate) },
                            enabled = !alreadySaved,
                            modifier = Modifier.testTag("charm-identify-$source-save-table-$candidate"),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Plum, contentColor = AppColors.Parchment)
                        ) { Text(if (alreadySaved) "Table $candidate · current" else "Save Table $candidate", style = AppType.Metadata) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInfoCard(title: String, body: String, tag: String) {
    SectionCard(title = title, accent = AppColors.Steel, contentPadding = 10.dp, modifier = Modifier.testTag(tag)) {
        Text(body, color = AppColors.Ink, style = AppType.Body)
    }
}

@Composable
private fun CharmSkillPicker(
    title: String,
    skillTrees: List<SkillTree>,
    allowedSkillIds: Set<String>,
    selectedName: String?,
    allowNone: Boolean,
    onSelect: (String?) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var open by rememberSaveable(tag) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { open = true }.testTag(tag),
        color = AppColors.Parchment.copy(alpha = .45f),
        shape = AppDimens.ButtonShape,
        border = BorderStroke(1.dp, if (isError) AppColors.Crimson else AppColors.ParchmentDeep)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                Text(selectedName ?: if (allowNone) "None / Any" else "Any", color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selectedName != null) {
                IconButton(onClick = { onSelect(null) }, modifier = Modifier.size(30.dp).testTag("$tag-clear")) { Icon(Icons.Default.Close, contentDescription = "Clear $title", tint = AppColors.Crimson) }
            } else Icon(Icons.Default.Search, contentDescription = "Search skills", tint = AppColors.Crimson)
        }
    }
    if (open) {
        var query by rememberSaveable(tag) { mutableStateOf("") }
        val matches = remember(skillTrees, allowedSkillIds, query) { filterCharmSkills(skillTrees, query, allowedSkillIds) }
        TrackedDialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(color = AppColors.Parchment, shape = AppDimens.CardShape, border = BorderStroke(1.dp, AppColors.Gold), modifier = Modifier.fillMaxWidth(.94f).fillMaxHeight(.82f).widthIn(max = 560.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text(title, color = AppColors.Ink, style = AppType.SectionTitle)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search skills") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("$tag-picker-search"),
                        colors = charmTextFieldColors()
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp).padding(top = 6.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (allowNone) {
                            DropdownMenuItem(text = { Text("None / Any Skill 2") }, onClick = { onSelect(null); open = false }, modifier = Modifier.testTag("$tag-picker-none"))
                        }
                        matches.forEach { skill ->
                            DropdownMenuItem(
                                text = { Text(skill.displayName, color = AppColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { onSelect(skill.stableSkillTreeId); open = false },
                                modifier = Modifier.testTag("$tag-picker-option-${skill.stableSkillTreeId}")
                            )
                        }
                        if (matches.isEmpty()) Text("No skills match.", color = AppColors.Ink, style = AppType.Body, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceMenu(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("choice-menu-$label"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Ink),
            border = BorderStroke(1.dp, AppColors.ParchmentDeep)
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                Text(selected, color = AppColors.Ink, style = AppType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 420.dp)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
private fun charmTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppColors.Ink,
    unfocusedTextColor = AppColors.Ink,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedBorderColor = AppColors.Crimson,
    unfocusedBorderColor = AppColors.ParchmentDeep,
    focusedLabelColor = AppColors.Crimson,
    unfocusedLabelColor = AppColors.Ink.copy(alpha = .72f),
    cursorColor = AppColors.Crimson,
    focusedTrailingIconColor = AppColors.Crimson,
    unfocusedTrailingIconColor = AppColors.Crimson
)

@Composable
private fun charmFilterChipColors(onParchment: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = AppColors.Plum,
    selectedLabelColor = AppColors.Parchment,
    containerColor = if (onParchment) Color.Transparent else AppColors.NightRaised,
    labelColor = if (onParchment) AppColors.Ink else AppColors.Parchment
)

@Composable
private fun charmFilterChipBorder(selected: Boolean, onParchment: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = if (onParchment) AppColors.ParchmentDeep.copy(alpha = .72f) else AppColors.ParchmentDeep.copy(alpha = .65f),
    selectedBorderColor = AppColors.Plum
)

private fun formatPoints(points: Int): String = if (points > 0) "+$points" else points.toString()

private fun filterSignedPoints(value: String): String {
    val negative = value.trimStart().startsWith('-')
    val digits = value.filter(Char::isDigit).take(3)
    return (if (negative) "-" else "") + digits
}

internal fun filterCharmSkills(skillTrees: List<SkillTree>, query: String): List<SkillTree> =
    skillTrees.asSequence().filter { skill ->
        query.isBlank() || skill.displayName.contains(query, true) || skill.tmoDisplayName.contains(query, true) || skill.jpName.contains(query, true)
    }.sortedBy { it.displayName.lowercase() }.toList()

internal fun filterCharmSkills(skillTrees: List<SkillTree>, query: String, allowedSkillIds: Set<String>): List<SkillTree> =
    filterCharmSkills(skillTrees, query).filter { it.stableSkillTreeId in allowedSkillIds }

private fun com.waillio.mhp3rdcompanion.data.CharmSignature.toStableKey(): String =
    "${classIndex}_${skill1Index}_${skill1Points}_${skill2Index}_${skill2Points}_${slots}"
