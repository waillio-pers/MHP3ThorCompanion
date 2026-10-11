package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.testTag
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
import com.waillio.mhp3rdcompanion.data.CharmPointMode
import com.waillio.mhp3rdcompanion.data.CharmResultGroup
import com.waillio.mhp3rdcompanion.data.CharmSearchCriteria
import com.waillio.mhp3rdcompanion.data.CharmSlotMode
import com.waillio.mhp3rdcompanion.data.CharmTableIndex
import com.waillio.mhp3rdcompanion.data.CharmTableMode
import com.waillio.mhp3rdcompanion.data.SkillTree
import com.waillio.mhp3rdcompanion.data.TalismanClass

private enum class CharmTablesPage { SEARCH, IDENTIFY }

@Composable
internal fun CharmTablesScreen(
    index: CharmTableIndex?,
    skillTrees: List<SkillTree>,
    currentTable: Int?,
    loadingFailed: Boolean,
    identifyWorkingState: CharmIdentifyWorkingState,
    onUpdateIdentifyWorkingState: ((CharmIdentifyWorkingState) -> CharmIdentifyWorkingState) -> Unit,
    onSaveCurrentTable: (Int?) -> Unit
) {
    if (index == null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

    var skill1Id by rememberSaveable { mutableStateOf<String?>(null) }
    var skill1Points by rememberSaveable { mutableStateOf("") }
    var skill1Exact by rememberSaveable { mutableStateOf(false) }
    var skill2Id by rememberSaveable { mutableStateOf<String?>(null) }
    var skill2Points by rememberSaveable { mutableStateOf("") }
    var skill2Exact by rememberSaveable { mutableStateOf(false) }
    var slots by rememberSaveable { mutableStateOf<Int?>(null) }
    var slotsExact by rememberSaveable { mutableStateOf(false) }
    var tableModeName by rememberSaveable { mutableStateOf(CharmTableMode.ANY.name) }
    var explicitTable by rememberSaveable { mutableIntStateOf(1) }
    var className by rememberSaveable { mutableStateOf("") }
    val tableMode = CharmTableMode.entries.firstOrNull { it.name == tableModeName } ?: CharmTableMode.ANY
    val talismanClass = TalismanClass.entries.firstOrNull { it.name == className }
    val criteria = remember(skill1Id, skill1Points, skill1Exact, skill2Id, skill2Points, skill2Exact, slots, slotsExact, tableMode, explicitTable, talismanClass) {
        CharmSearchCriteria(
            skill1Id = skill1Id,
            skill1Points = skill1Points.toIntOrNull(),
            skill1Mode = if (skill1Exact) CharmPointMode.EXACT else CharmPointMode.MINIMUM,
            skill2Id = skill2Id,
            skill2Points = skill2Points.toIntOrNull(),
            skill2Mode = if (skill2Exact) CharmPointMode.EXACT else CharmPointMode.MINIMUM,
            slots = slots,
            slotMode = if (slotsExact) CharmSlotMode.EXACT else CharmSlotMode.MINIMUM,
            tableMode = tableMode,
            explicitTable = explicitTable,
            talismanClass = talismanClass
        )
    }
    val searchResults = remember(index, criteria, currentTable) { index.search(criteria, currentTable) }

    val observations = identifyWorkingState.observations
    val editingObservation = identifyWorkingState.editingObservation
    val identification = remember(index, observations) { index.identify(observations) }

    LazyColumn(
        Modifier.fillMaxSize().testTag("screen-charm-tables"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Charm Tables", color = AppColors.Parchment, style = AppType.ScreenTitle)
                Text("Historical Timeworn Charm results · Tables 1–12", color = AppColors.ParchmentDeep, style = AppType.Metadata)
            }
        }
        item {
            CurrentTableCard(currentTable = currentTable, onSaveCurrentTable = onSaveCurrentTable)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPage == CharmTablesPage.SEARCH,
                    onClick = { page = CharmTablesPage.SEARCH.name },
                    label = { Text("Search charms") },
                    shape = AppDimens.ButtonShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Crimson,
                        selectedLabelColor = AppColors.Parchment,
                        containerColor = AppColors.NightRaised,
                        labelColor = AppColors.Parchment
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedPage == CharmTablesPage.SEARCH,
                        borderColor = AppColors.ParchmentDeep.copy(alpha = .65f),
                        selectedBorderColor = AppColors.Gold
                    ),
                    modifier = Modifier.weight(1f).testTag("charm-page-search")
                )
                FilterChip(
                    selected = selectedPage == CharmTablesPage.IDENTIFY,
                    onClick = { page = CharmTablesPage.IDENTIFY.name },
                    label = { Text("Identify my table") },
                    shape = AppDimens.ButtonShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Crimson,
                        selectedLabelColor = AppColors.Parchment,
                        containerColor = AppColors.NightRaised,
                        labelColor = AppColors.Parchment
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedPage == CharmTablesPage.IDENTIFY,
                        borderColor = AppColors.ParchmentDeep.copy(alpha = .65f),
                        selectedBorderColor = AppColors.Gold
                    ),
                    modifier = Modifier.weight(1f).testTag("charm-page-identify")
                )
            }
        }
        if (selectedPage == CharmTablesPage.SEARCH) {
            item {
                CharmSearchFilters(
                    skillTrees = skillTrees,
                    skillsById = skillsById,
                    skill1Id = skill1Id,
                    onSkill1 = { skill1Id = it },
                    skill1Points = skill1Points,
                    onSkill1Points = { skill1Points = it.filter { char -> char == '-' || char.isDigit() }.take(3) },
                    skill1Exact = skill1Exact,
                    onSkill1Exact = { skill1Exact = it },
                    skill2Id = skill2Id,
                    onSkill2 = { skill2Id = it },
                    skill2Points = skill2Points,
                    onSkill2Points = { skill2Points = it.filter { char -> char == '-' || char.isDigit() }.take(3) },
                    skill2Exact = skill2Exact,
                    onSkill2Exact = { skill2Exact = it },
                    slots = slots,
                    onSlots = { slots = it },
                    slotsExact = slotsExact,
                    onSlotsExact = { slotsExact = it },
                    tableMode = tableMode,
                    explicitTable = explicitTable,
                    onTableMode = { mode, table -> tableModeName = mode.name; table?.let { explicitTable = it } },
                    talismanClass = talismanClass,
                    onClass = { className = it?.name.orEmpty() }
                )
            }
            item {
                if (!criteria.hasFilters) {
                    EmptyInfoCard("Choose a skill or another filter", "Set Skill 1 or Skill 2 to search historical Timeworn outcomes. Source-state counts are not drop probabilities.", "charm-search-start")
                } else if (tableMode == CharmTableMode.CURRENT && currentTable == null) {
                    EmptyInfoCard("Current Table is unknown", "Set a current table or choose Any table / a specific table to search.", "charm-search-current-unknown")
                } else if (searchResults.isEmpty()) {
                    EmptyInfoCard("No matching charms", "No charms match the current filters. Try a lower minimum, another table, or Any table.", "charm-search-empty")
                } else {
                    Text("${searchResults.size} observable signatures", color = AppColors.Gold, style = AppType.SectionTitle, modifier = Modifier.testTag("charm-search-count"))
                }
            }
            items(searchResults, key = { group -> group.signature.toStableKey() }) { group ->
                CharmResultCard(group = group, index = index, skillsById = skillsById)
            }
        } else {
            item {
                IdentifyWorkflow(
                    skillTrees = skillTrees,
                    skillsById = skillsById,
                    observations = observations,
                    onObservations = { value -> onUpdateIdentifyWorkingState { it.copy(observations = value) } },
                    identification = identification,
                    currentTable = currentTable,
                    onSaveCurrentTable = onSaveCurrentTable,
                    editingObservation = editingObservation,
                    onEditingObservation = { value -> onUpdateIdentifyWorkingState { it.copy(editingObservation = value) } },
                    formClassName = identifyWorkingState.formClass.name,
                    onFormClass = { value -> onUpdateIdentifyWorkingState { it.copy(formClass = value) } },
                    formSkill1Id = identifyWorkingState.formSkill1Id,
                    onFormSkill1 = { value -> onUpdateIdentifyWorkingState { it.copy(formSkill1Id = value) } },
                    formSkill1Points = identifyWorkingState.formSkill1Points,
                    onFormSkill1Points = { value -> onUpdateIdentifyWorkingState { it.copy(formSkill1Points = value.filter { char -> char == '-' || char.isDigit() }.take(3)) } },
                    formSkill2Id = identifyWorkingState.formSkill2Id,
                    onFormSkill2 = { value -> onUpdateIdentifyWorkingState { it.copy(formSkill2Id = value) } },
                    formSkill2Points = identifyWorkingState.formSkill2Points,
                    onFormSkill2Points = { value -> onUpdateIdentifyWorkingState { it.copy(formSkill2Points = value.filter { char -> char == '-' || char.isDigit() }.take(3)) } },
                    formSlots = identifyWorkingState.formSlots,
                    onFormSlots = { value -> onUpdateIdentifyWorkingState { it.copy(formSlots = value) } },
                    onClearForm = {
                        onUpdateIdentifyWorkingState {
                            it.copy(
                                editingObservation = -1,
                                formClass = TalismanClass.QUEEN,
                                formSkill1Id = null,
                                formSkill1Points = "",
                                formSkill2Id = null,
                                formSkill2Points = "",
                                formSlots = null
                            )
                        }
                    },
                    onLoadObservationForEdit = { position, observation ->
                        onUpdateIdentifyWorkingState {
                            it.copy(
                                editingObservation = position,
                                formClass = observation.talismanClass,
                                formSkill1Id = observation.skill1Id,
                                formSkill1Points = observation.skill1Points.toString(),
                                formSkill2Id = observation.skill2Id,
                                formSkill2Points = observation.skill2Id?.let { observation.skill2Points.toString() }.orEmpty(),
                                formSlots = observation.slots
                            )
                        }
                    }
                )
            }
        }
        item { CharmTableHelpCard() }
    }
}

@Composable
private fun CurrentTableCard(currentTable: Int?, onSaveCurrentTable: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("current-charm-table-card"),
        color = AppColors.Parchment,
        shape = AppDimens.CardShape,
        border = BorderStroke(1.dp, AppColors.ParchmentDeep)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Current Table", color = AppColors.Ink, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                Text(currentTable?.let { "Table $it" } ?: "Unknown", color = AppColors.Crimson, style = AppType.CardTitle, modifier = Modifier.testTag("current-charm-table-value"))
            }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag("current-charm-table-change"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Crimson),
                    border = BorderStroke(1.dp, AppColors.Crimson)
                ) {
                    Text("Change")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Unknown") }, onClick = { onSaveCurrentTable(null); expanded = false })
                    (1..12).forEach { table ->
                        DropdownMenuItem(text = { Text("Table $table") }, onClick = { onSaveCurrentTable(table); expanded = false })
                    }
                }
            }
            if (currentTable != null) {
                IconButton(onClick = { onSaveCurrentTable(null) }, modifier = Modifier.size(40.dp).testTag("current-charm-table-clear")) {
                    Icon(Icons.Default.Close, contentDescription = "Clear current table", tint = AppColors.Crimson)
                }
            }
        }
    }
}

@Composable
private fun CharmSearchFilters(
    skillTrees: List<SkillTree>,
    skillsById: Map<String, SkillTree>,
    skill1Id: String?, onSkill1: (String?) -> Unit,
    skill1Points: String, onSkill1Points: (String) -> Unit, skill1Exact: Boolean, onSkill1Exact: (Boolean) -> Unit,
    skill2Id: String?, onSkill2: (String?) -> Unit,
    skill2Points: String, onSkill2Points: (String) -> Unit, skill2Exact: Boolean, onSkill2Exact: (Boolean) -> Unit,
    slots: Int?, onSlots: (Int?) -> Unit, slotsExact: Boolean, onSlotsExact: (Boolean) -> Unit,
    tableMode: CharmTableMode, explicitTable: Int, onTableMode: (CharmTableMode, Int?) -> Unit,
    talismanClass: TalismanClass?, onClass: (TalismanClass?) -> Unit
) {
    SectionCard(title = "Search filters", accent = AppColors.Moss, contentPadding = 9.dp, modifier = Modifier.testTag("charm-search-filters")) {
        Text("Skill points: use a minimum or an exact value for each selected skill.", color = AppColors.Ink, style = AppType.Metadata)
        Spacer(Modifier.height(7.dp))
        CharmSkillPicker("Skill 1", skillTrees, skillsById[skill1Id]?.displayName, false, onSkill1, "charm-search-skill1")
        if (skill1Id != null) {
            PointInputAndMode("Skill 1 points", skill1Points, onSkill1Points, skill1Exact, onSkill1Exact, "charm-search-skill1-points")
        }
        Spacer(Modifier.height(7.dp))
        CharmSkillPicker("Skill 2 · optional", skillTrees, skillsById[skill2Id]?.displayName, true, onSkill2, "charm-search-skill2")
        if (skill2Id != null) PointInputAndMode("Skill 2 points", skill2Points, onSkill2Points, skill2Exact, onSkill2Exact, "charm-search-skill2-points")
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChoiceMenu(
                label = "Slots",
                selected = slots?.let { if (slotsExact) "Exactly $it" else "At least $it" } ?: "Any",
                options = listOf("Any") + (0..3).map { "Minimum $it" } + (0..3).map { "Exact $it" },
                onSelect = { option ->
                    when {
                        option == "Any" -> onSlots(null)
                        option.startsWith("Minimum ") -> { onSlotsExact(false); onSlots(option.substringAfterLast(' ').toInt()) }
                        else -> { onSlotsExact(true); onSlots(option.substringAfterLast(' ').toInt()) }
                    }
                },
                modifier = Modifier.weight(1f).testTag("charm-search-slots")
            )
            ChoiceMenu(
                label = "Class",
                selected = talismanClass?.label ?: "Any",
                options = listOf("Any") + TalismanClass.entries.map { it.label },
                onSelect = { value -> onClass(TalismanClass.entries.firstOrNull { it.label == value }) },
                modifier = Modifier.weight(1f).testTag("charm-search-class")
            )
        }
        Spacer(Modifier.height(7.dp))
        val tableLabel = when (tableMode) {
            CharmTableMode.ANY -> "Any table"
            CharmTableMode.CURRENT -> "Current table"
            CharmTableMode.EXPLICIT -> "Table $explicitTable"
        }
        ChoiceMenu(
            label = "Table",
            selected = tableLabel,
            options = listOf("Any table", "Current table") + (1..12).map { "Table $it" },
            onSelect = { value ->
                when (value) {
                    "Any table" -> onTableMode(CharmTableMode.ANY, null)
                    "Current table" -> onTableMode(CharmTableMode.CURRENT, null)
                    else -> onTableMode(CharmTableMode.EXPLICIT, value.substringAfter(' ').toIntOrNull())
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("charm-search-table")
        )
    }
}

@Composable
private fun PointInputAndMode(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    exact: Boolean,
    onExact: (Boolean) -> Unit,
    tag: String
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SignedPointsField(
            title = title,
            value = value,
            onValue = onValue,
            tag = tag,
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = !exact,
            onClick = { onExact(false) },
            label = { Text("At least") },
            modifier = Modifier.testTag("$tag-minimum"),
            colors = charmFilterChipColors(onParchment = true),
            border = charmFilterChipBorder(selected = !exact, onParchment = true)
        )
        FilterChip(
            selected = exact,
            onClick = { onExact(true) },
            label = { Text("Exact") },
            modifier = Modifier.testTag("$tag-exact"),
            colors = charmFilterChipColors(onParchment = true),
            border = charmFilterChipBorder(selected = exact, onParchment = true)
        )
    }
}

@Composable
private fun SignedPointsField(
    title: String,
    value: String,
    onValue: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
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
        colors = charmTextFieldColors()
    )
}

@Composable
private fun IdentifyWorkflow(
    skillTrees: List<SkillTree>,
    skillsById: Map<String, SkillTree>,
    observations: List<CharmObservation>,
    onObservations: (List<CharmObservation>) -> Unit,
    identification: CharmIdentification,
    currentTable: Int?,
    onSaveCurrentTable: (Int?) -> Unit,
    editingObservation: Int,
    onEditingObservation: (Int) -> Unit,
    formClassName: String, onFormClass: (TalismanClass) -> Unit,
    formSkill1Id: String?, onFormSkill1: (String?) -> Unit,
    formSkill1Points: String, onFormSkill1Points: (String) -> Unit,
    formSkill2Id: String?, onFormSkill2: (String?) -> Unit,
    formSkill2Points: String, onFormSkill2Points: (String) -> Unit,
    formSlots: Int?, onFormSlots: (Int?) -> Unit,
    onClearForm: () -> Unit,
    onLoadObservationForEdit: (Int, CharmObservation) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionCard(title = "Identify My Table", accent = AppColors.Steel, contentPadding = 9.dp) {
            Text("Enter appraised Timeworn talismans exactly as shown. Skill 1 and Skill 2 order matters.", color = AppColors.Ink, style = AppType.Body)
            Spacer(Modifier.height(8.dp))
            ChoiceMenu(
                label = "Talisman class",
                selected = TalismanClass.entries.firstOrNull { it.name == formClassName }?.label ?: "Queen",
                options = TalismanClass.entries.map { it.label },
                onSelect = { value -> TalismanClass.entries.firstOrNull { it.label == value }?.let(onFormClass) },
                modifier = Modifier.fillMaxWidth().testTag("charm-identify-class")
            )
            Spacer(Modifier.height(8.dp))
            CharmSkillPicker("Skill 1 · order is significant", skillTrees, skillsById[formSkill1Id]?.displayName, false, onFormSkill1, "charm-identify-skill1")
            SignedPointsField(
                title = "Skill 1 points",
                value = formSkill1Points,
                onValue = onFormSkill1Points,
                tag = "charm-identify-skill1-points",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            CharmSkillPicker("Skill 2 · optional, keep displayed order", skillTrees, skillsById[formSkill2Id]?.displayName, true, onFormSkill2, "charm-identify-skill2")
            if (formSkill2Id != null) {
                SignedPointsField(
                    title = "Skill 2 points",
                    value = formSkill2Points,
                    onValue = onFormSkill2Points,
                    tag = "charm-identify-skill2-points",
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            ChoiceMenu(
                label = "Slots",
                selected = formSlots?.toString() ?: "Select slots",
                options = (0..3).map(Int::toString),
                onSelect = { onFormSlots(it.toIntOrNull()) },
                modifier = Modifier.fillMaxWidth().testTag("charm-identify-slots")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val firstId = formSkill1Id ?: return@Button
                        val firstPoints = formSkill1Points.toIntOrNull() ?: return@Button
                        val slotsValue = formSlots ?: return@Button
                        val secondPoints = if (formSkill2Id == null) 0 else formSkill2Points.toIntOrNull() ?: return@Button
                        val observation = CharmObservation(
                            talismanClass = TalismanClass.entries.firstOrNull { it.name == formClassName } ?: TalismanClass.QUEEN,
                            skill1Id = firstId,
                            skill1Points = firstPoints,
                            skill2Id = formSkill2Id,
                            skill2Points = secondPoints,
                            slots = slotsValue
                        )
                        onObservations(if (editingObservation in observations.indices) observations.toMutableList().also { it[editingObservation] = observation } else observations + observation)
                        onClearForm()
                    },
                    modifier = Modifier.weight(1f).testTag("charm-identify-add"),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Crimson, contentColor = AppColors.Parchment)
                ) {
                    Text(if (editingObservation in observations.indices) "Update observation" else "Add observation")
                }
                if (editingObservation in observations.indices) {
                    OutlinedButton(
                        onClick = onClearForm,
                        modifier = Modifier.testTag("charm-identify-cancel-edit"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Crimson),
                        border = BorderStroke(1.dp, AppColors.ParchmentDeep)
                    ) { Text("Cancel") }
                }
            }
            if (formSkill1Id == null || formSkill1Points.toIntOrNull() == null || formSlots == null || (formSkill2Id != null && formSkill2Points.toIntOrNull() == null)) {
                Text("Choose Skill 1, enter its points, and select slots before adding.", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, modifier = Modifier.padding(top = 5.dp))
            }
        }

        if (observations.isNotEmpty()) {
            SectionCard(title = "Observed talismans (${observations.size})", accent = AppColors.Moss, contentPadding = 7.dp) {
                observations.forEachIndexed { index, observation ->
                    ObservationCard(
                        observation = observation,
                        skillsById = skillsById,
                        onEdit = {
                            onLoadObservationForEdit(index, observation)
                        },
                        onRemove = {
                            onObservations(observations.filterIndexed { row, _ -> row != index })
                            if (editingObservation == index) onClearForm()
                            else if (editingObservation > index) onEditingObservation(editingObservation - 1)
                        },
                        modifier = Modifier.testTag("charm-observation-$index")
                    )
                }
            }
        }

        IdentificationResult(
            identification = identification,
            observationCount = observations.size,
            currentTable = currentTable,
            onSaveCurrentTable = onSaveCurrentTable
        )
    }
}

@Composable
private fun ObservationCard(
    observation: CharmObservation,
    skillsById: Map<String, SkillTree>,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.fillMaxWidth().padding(top = 5.dp), color = AppColors.ParchmentDeep.copy(alpha = .35f), shape = AppDimens.ButtonShape) {
        Row(Modifier.fillMaxWidth().padding(start = 9.dp, end = 2.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${observation.talismanClass.label} · ${skillsById[observation.skill1Id]?.displayName ?: observation.skill1Id} ${formatPoints(observation.skill1Points)}" +
                    (observation.skill2Id?.let { " · ${skillsById[it]?.displayName ?: it} ${formatPoints(observation.skill2Points)}" } ?: "") + " · ${observation.slots} slots",
                    color = AppColors.Ink, style = AppType.Body)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp).testTag("charm-observation-edit")) {
                Icon(Icons.Default.Edit, contentDescription = "Edit observation", tint = AppColors.Crimson)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp).testTag("charm-observation-remove")) {
                Icon(Icons.Default.Close, contentDescription = "Remove observation", tint = AppColors.Crimson)
            }
        }
    }
}

@Composable
private fun IdentificationResult(
    identification: CharmIdentification,
    observationCount: Int,
    currentTable: Int?,
    onSaveCurrentTable: (Int?) -> Unit
) {
    SectionCard(title = "Table match", accent = AppColors.Crimson, contentPadding = 10.dp, modifier = Modifier.testTag("charm-identify-result")) {
        when (identification.state) {
            CharmIdentificationState.NO_OBSERVATIONS -> Text("Add one or more exact Timeworn observations to compare them against Tables 1–12.", color = AppColors.Ink, style = AppType.Body)
            CharmIdentificationState.ONE_TABLE -> {
                Text("Table ${identification.tables.single()}", color = AppColors.Crimson, style = AppType.ScreenTitle, modifier = Modifier.testTag("charm-identify-one-table"))
                Text("All $observationCount observation(s) match this table.", color = AppColors.Ink, style = AppType.Body)
                if (currentTable != identification.tables.single()) {
                    Button(
                        onClick = { onSaveCurrentTable(identification.tables.single()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("charm-identify-save-table"),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Crimson, contentColor = AppColors.Parchment)
                    ) {
                        Text("Save as Current Table")
                    }
                } else {
                    Text("This is already your Current Table.", color = AppColors.Moss, style = AppType.Body)
                }
            }
            CharmIdentificationState.MULTIPLE_TABLES -> {
                Text("Possible tables: ${identification.tables.joinToString(", ")}", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.testTag("charm-identify-multiple"))
                Text("Another exact Timeworn charm may narrow the result.", color = AppColors.Ink, style = AppType.Body)
            }
            CharmIdentificationState.ALL_TABLES -> {
                Text("All 12 tables remain possible", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.testTag("charm-identify-all"))
                Text("This charm appears in every standard table and does not narrow the result. Add a different Timeworn charm.", color = AppColors.Ink, style = AppType.Body)
            }
            CharmIdentificationState.NO_MATCH -> {
                Text("No exact table match", color = AppColors.Crimson, style = AppType.SectionTitle, modifier = Modifier.testTag("charm-identify-no-match"))
                Text("Check the talisman class, displayed skill order, both skill values, and slot count. No table is guessed.", color = AppColors.Ink, style = AppType.Body)
            }
        }
        if (currentTable != null) Text("Current Table: $currentTable", color = AppColors.Moss, style = AppType.Metadata, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun CharmResultCard(group: CharmResultGroup, index: CharmTableIndex, skillsById: Map<String, SkillTree>) {
    val signature = group.signature
    val skill1Id = index.skillTreeId(signature.skill1Index)
    val skill2Id = index.skillTreeId(signature.skill2Index)
    val skill1Name = skill1Id?.let { skillsById[it]?.displayName } ?: skill1Id ?: "Unknown skill"
    val skill2Name = skill2Id?.let { skillsById[it]?.displayName }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("charm-result-${signature.toStableKey()}"),
        color = AppColors.Parchment,
        shape = AppDimens.CardShape,
        border = BorderStroke(1.dp, AppColors.ParchmentDeep)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(index.classAt(signature.classIndex)?.label ?: "Talisman", color = AppColors.Crimson, style = AppType.Metadata, fontWeight = FontWeight.Bold)
                Text("${signature.slots} slots", color = AppColors.Ink, style = AppType.Metadata)
                Spacer(Modifier.weight(1f))
                Text(group.tables.joinToString(" · ") { "T$it" }, color = AppColors.Steel, style = AppType.Metadata, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("charm-result-tables"))
            }
            Text("$skill1Name ${formatPoints(signature.skill1Points)}", color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2)
            if (skill2Name != null) Text("$skill2Name ${formatPoints(signature.skill2Points)}", color = AppColors.Ink, style = AppType.CardTitle, maxLines = 2)
        }
    }
}

@Composable
private fun CharmTableHelpCard() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "About charm tables", accent = AppColors.Steel, contentPadding = 9.dp, modifier = Modifier.testTag("charm-table-help")) {
        Text("This 1.1 feature identifies standard tables from Timeworn Charm results. It cannot read the active table from the game.", color = AppColors.Ink, style = AppType.Body)
        Spacer(Modifier.height(5.dp))
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.testTag("charm-table-help-toggle"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Crimson),
            border = BorderStroke(1.dp, AppColors.ParchmentDeep)
        ) {
            Text(if (expanded) "Hide details" else "Table behavior")
        }
        if (expanded) {
            Text(
                "MHP3 uses standard charm tables 1–12 for this workflow. The active table is selected when the game fully starts. Fully restarting the game can result in a different table. Sleeping or returning to character selection is not a guaranteed table change.",
                color = AppColors.Ink, style = AppType.Body, modifier = Modifier.padding(top = 4.dp).testTag("charm-table-help-details")
            )
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
    selectedName: String?,
    allowNone: Boolean,
    onSelect: (String?) -> Unit,
    tag: String
) {
    var open by rememberSaveable(tag) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { open = true }.testTag(tag),
        color = AppColors.Parchment.copy(alpha = .45f),
        shape = AppDimens.ButtonShape,
        border = BorderStroke(1.dp, AppColors.ParchmentDeep)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                Text(selectedName ?: if (allowNone) "None" else "Choose a skill", color = AppColors.Ink, style = AppType.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Search, contentDescription = "Search skills", tint = AppColors.Crimson)
        }
    }
    if (open) {
        var query by rememberSaveable(tag) { mutableStateOf("") }
        val matches = remember(skillTrees, query) { filterCharmSkills(skillTrees, query) }
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
                            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); open = false }, modifier = Modifier.testTag("$tag-picker-none"))
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
    selectedContainerColor = AppColors.Crimson,
    selectedLabelColor = AppColors.Parchment,
    containerColor = if (onParchment) Color.Transparent else AppColors.NightRaised,
    labelColor = if (onParchment) AppColors.Ink else AppColors.Parchment
)

@Composable
private fun charmFilterChipBorder(selected: Boolean, onParchment: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = if (onParchment) AppColors.ParchmentDeep.copy(alpha = .72f) else AppColors.ParchmentDeep.copy(alpha = .65f),
    selectedBorderColor = AppColors.Gold
)

@Composable
private fun formatPoints(points: Int): String = if (points >= 0) "+$points" else points.toString()

internal fun filterCharmSkills(skillTrees: List<SkillTree>, query: String): List<SkillTree> =
    skillTrees.asSequence().filter { skill ->
        query.isBlank() || skill.displayName.contains(query, true) || skill.tmoDisplayName.contains(query, true) || skill.jpName.contains(query, true)
    }.sortedBy { it.displayName.lowercase() }.toList()

private fun com.waillio.mhp3rdcompanion.data.CharmSignature.toStableKey(): String =
    "${classIndex}_${skill1Index}_${skill1Points}_${skill2Index}_${skill2Points}_${slots}"
