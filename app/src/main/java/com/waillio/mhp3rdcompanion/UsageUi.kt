package com.waillio.mhp3rdcompanion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waillio.mhp3rdcompanion.data.ItemUsageFamily
import com.waillio.mhp3rdcompanion.data.ItemUsageFamilyProjection
import com.waillio.mhp3rdcompanion.data.ItemUsageProjection
import com.waillio.mhp3rdcompanion.data.ItemUsageRelation
import com.waillio.mhp3rdcompanion.data.ItemUsageTarget
import com.waillio.mhp3rdcompanion.data.UsageTargetKind
import com.waillio.mhp3rdcompanion.data.Material
import com.waillio.mhp3rdcompanion.data.tradeUsageRouteSummary
import com.waillio.mhp3rdcompanion.data.tradeUsageRouteMetadata

private val WeaponSilhouette = ImageVector.Builder(
    name = "WeaponSilhouette",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 19f); lineTo(5f, 21f); lineTo(14.5f, 11.5f); lineTo(12.5f, 9.5f); close()
        moveTo(9.5f, 12.5f); lineTo(11.5f, 14.5f); lineTo(21f, 5f); lineTo(19f, 3f); close()
        moveTo(5f, 15.5f); lineTo(8.5f, 19f); lineTo(7f, 20.5f); lineTo(3.5f, 17f); close()
        moveTo(7.5f, 8.5f); lineTo(15.5f, 16.5f); lineTo(14f, 18f); lineTo(6f, 10f); close()
    }
}.build()

private fun ItemUsageFamily.icon(): ImageVector = when (this) {
    ItemUsageFamily.WEAPONS -> WeaponSilhouette
    ItemUsageFamily.DECORATIONS -> Icons.Default.Diamond
    ItemUsageFamily.COMBINATIONS -> Icons.Default.Science
    ItemUsageFamily.QUEST_DELIVERY -> Icons.Default.Flag
    ItemUsageFamily.TRADING -> Icons.Default.SwapHoriz
    ItemUsageFamily.FARM -> Icons.Default.LocalFlorist
    ItemUsageFamily.ROASTING -> Icons.Default.Whatshot
    ItemUsageFamily.SCRAP_CONVERSION -> Icons.Default.Sync
}

internal fun usageFamilyIconName(family: ItemUsageFamily): String = when (family) {
    ItemUsageFamily.WEAPONS -> "crossed_blades"
    ItemUsageFamily.SCRAP_CONVERSION -> "curved_conversion_arrows"
    else -> family.name.lowercase()
}

private fun ItemUsageFamily.countLabel(count: Int): String = when (this) {
    ItemUsageFamily.WEAPONS -> "$count weapon${if (count == 1) "" else "s"}"
    ItemUsageFamily.DECORATIONS -> "$count decoration${if (count == 1) "" else "s"}"
    ItemUsageFamily.COMBINATIONS, ItemUsageFamily.FARM -> "$count result${if (count == 1) "" else "s"}"
    ItemUsageFamily.QUEST_DELIVERY -> "$count quest${if (count == 1) "" else "s"}"
    ItemUsageFamily.TRADING -> "$count item${if (count == 1) "" else "s"}"
    ItemUsageFamily.ROASTING -> "$count result${if (count == 1) "" else "s"}"
    ItemUsageFamily.SCRAP_CONVERSION -> "$count scrap${if (count == 1) "" else "s"}"
}

internal fun usageSummary(projection: ItemUsageProjection): String =
    "${projection.familyCount} type${if (projection.familyCount == 1) "" else "s"} · ${projection.totalTargetCount} target${if (projection.totalTargetCount == 1) "" else "s"}"

@Composable
internal fun ItemUsageSection(
    projection: ItemUsageProjection,
    weaponRepository: WeaponRepository?,
    onFamily: (ItemUsageFamily) -> Unit,
    onTarget: (ItemUsageTarget) -> Unit
) {
    val initiallyExpanded = projection.totalTargetCount <= 3
    var expanded by remember(projection.currentItemGameItemId) { mutableStateOf(if (initiallyExpanded) projection.families.map { it.family }.toSet() else emptySet()) }
    var viewAllFamily by remember(projection.currentItemGameItemId) { mutableStateOf<ItemUsageFamilyProjection?>(null) }
    SectionCard(
        "Usage",
        modifier = Modifier.fillMaxWidth().testTag("item-usage-section-${projection.currentItemGameItemId}")
    ) {
        // Compatibility semantics for existing deep-link/UI probes; this is
        // still the single Usage surface, not a second legacy card.
        Box(Modifier.size(1.dp).testTag("material-used-in-weapons"))
        if (projection.families.any { it.family == ItemUsageFamily.WEAPONS && it.targetCount > 6 }) {
            Box(
                Modifier.size(1.dp).clickable { onFamily(ItemUsageFamily.WEAPONS) }
                    .testTag("material-used-in-weapons-view-all")
            )
        }
        Text(
            usageSummary(projection),
            color = AppColors.Ink.copy(alpha = .72f),
            style = AppType.Metadata,
            modifier = Modifier.testTag("item-usage-summary-${projection.currentItemGameItemId}")
        )
        Spacer(Modifier.size(5.dp))
        projection.families.forEach { family ->
            val isExpanded = family.family in expanded
            ItemUsageFamilyCard(
                family = family,
                currentItemGameItemId = projection.currentItemGameItemId,
                expanded = isExpanded,
                weaponRepository = weaponRepository,
                onToggle = { expanded = if (isExpanded) expanded - family.family else expanded + family.family },
                onViewAll = { viewAllFamily = family },
                onTarget = onTarget
            )
            Spacer(Modifier.size(5.dp))
        }
    }
    viewAllFamily?.let { family ->
        UsageViewAllDialog(
            projection = projection,
            family = family,
            weaponRepository = weaponRepository,
            onDismiss = { viewAllFamily = null },
            onTarget = { target ->
                viewAllFamily = null
                onTarget(target)
            }
        )
    }
}

@Composable
private fun UsageViewAllDialog(
    projection: ItemUsageProjection,
    family: ItemUsageFamilyProjection,
    weaponRepository: WeaponRepository?,
    onDismiss: () -> Unit,
    onTarget: (ItemUsageTarget) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("item-usage-view-all-modal"),
        containerColor = AppColors.Parchment,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(family.family.icon(), null, tint = AppColors.Ink, modifier = Modifier.size(29.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(family.family.displayLabel, color = AppColors.Ink, style = AppType.SectionTitle, maxLines = 1)
                    Text(family.family.countLabel(family.targetCount), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("item-usage-view-all-modal-close"), contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        },
        text = {
            LazyColumn(
                Modifier.fillMaxWidth().fillMaxHeight(.72f).testTag("item-usage-view-all-modal-list"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(family.targets, key = { it.targetIdentity }) { target ->
                    UsageTargetRow(target, family.family, projection.currentItemGameItemId, weaponRepository, onTarget)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ItemUsageFamilyCard(
    family: ItemUsageFamilyProjection,
    currentItemGameItemId: Int,
    expanded: Boolean,
    weaponRepository: WeaponRepository?,
    onToggle: () -> Unit,
    onViewAll: () -> Unit,
    onTarget: (ItemUsageTarget) -> Unit
) {
    val tag = "item-usage-family-${family.family.name}"
    ParchmentSurface(Modifier.fillMaxWidth().testTag(tag)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 7.dp)
                    .testTag("item-usage-family-toggle-${family.family.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(family.family.icon(), null, tint = AppColors.Ink, modifier = Modifier.size(29.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(family.family.displayLabel, color = AppColors.Ink, style = AppType.CardTitle, maxLines = 1)
                    Text(family.family.countLabel(family.targetCount), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1)
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight, null, tint = AppColors.Ink, modifier = Modifier.size(22.dp))
            }
            if (expanded) {
                family.targets.take(6).forEach { target ->
                    UsageTargetRow(target, family.family, currentItemGameItemId, weaponRepository, onTarget)
                }
                if (family.targetCount > 6) {
                    PreviewFooter(
                        moreLabel = "+${family.targetCount - 6} more ${family.family.targetNoun}",
                        actionLabel = "View all ${family.targetCount}",
                        tag = "item-usage-view-all-${family.family.name}",
                        onAction = onViewAll,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageTargetRow(
    target: ItemUsageTarget,
    family: ItemUsageFamily,
    currentItemGameItemId: Int,
    weaponRepository: WeaponRepository?,
    onTarget: (ItemUsageTarget) -> Unit
) {
    val clickableTarget = target.targetKind != UsageTargetKind.ITEM || target.targetGameItemId != currentItemGameItemId
    Row(
        Modifier.fillMaxWidth()
            .then(if (clickableTarget) Modifier.clickable { onTarget(target) } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("item-usage-target-${family.name}-${target.targetId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (target.targetKind) {
            UsageTargetKind.WEAPON -> weaponRepository?.weaponById?.get(target.targetId)?.let { WeaponIcon(it.weaponType, it.rarity, Modifier.size(31.dp)) }
            UsageTargetKind.ITEM -> ItemIcon(target.targetGameItemId, target.targetName, Modifier.size(31.dp))
            UsageTargetKind.TRAINING -> Icon(Icons.Default.Flag, target.targetName, tint = AppColors.Ink, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(target.targetName, color = AppColors.Ink, style = AppType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (target.relations.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(.25f).padding(vertical = 2.dp),
                    color = AppColors.ParchmentDeep.copy(alpha = .55f),
                    thickness = 1.dp
                )
            }
            target.relations.sortedBy { it.sourceOrder }.forEach { relation ->
                if (family == ItemUsageFamily.DECORATIONS) {
                    UsageDecorationRelationLine(relation)
                } else if (family == ItemUsageFamily.COMBINATIONS) {
                    UsageCombinationRelationLine(relation)
                } else if (family == ItemUsageFamily.TRADING) {
                    UsageTradingRelationLine(relation)
                } else {
                    Text(usageRelationSummary(family, relation), color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (clickableTarget) Icon(Icons.Default.ChevronRight, null, tint = AppColors.Crimson, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun UsageTradingRelationLine(relation: ItemUsageRelation) {
    Column(Modifier.testTag("item-usage-trading-route-${relation.id}")) {
        Text(
            listOfNotNull(relation.outputQuantity?.let { "Receive ×$it" }, relation.quantity?.let { "Cost ×$it" }).joinToString(" · "),
            color = AppColors.Ink.copy(alpha = .72f),
            style = AppType.Metadata,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        tradeUsageRouteMetadata(relation)?.let {
            Text(it, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun UsageCombinationRelationLine(relation: ItemUsageRelation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.testTag("item-usage-combination-relation-${relation.id}")
    ) {
        Text("with", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
        if (relation.otherIngredientGameItemId != null && relation.otherIngredientName != null) {
            ItemIcon(relation.otherIngredientGameItemId, relation.otherIngredientName, Modifier.size(18.dp))
            Text(relation.otherIngredientName, color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        relation.successPercent?.let { Text("· $it%", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata) }
        when {
            relation.outputQuantityMin != null && relation.outputQuantityMax != null && relation.outputQuantityMin != relation.outputQuantityMax -> Text("· ×${relation.outputQuantityMin}–${relation.outputQuantityMax}", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
            relation.outputQuantityMin != null -> Text("· ×${relation.outputQuantityMin}", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
        }
    }
}

@Composable
private fun UsageDecorationRelationLine(relation: ItemUsageRelation) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val rank = relation.rank?.let {
            when {
                it.contains("HIGH", ignoreCase = true) -> "HR"
                it.contains("LOW", ignoreCase = true) -> "LR"
                else -> it
            }
        }
        if (rank != null) {
            val high = rank == "HR"
            Surface(
                color = (if (high) AppColors.Crimson else AppColors.Steel).copy(alpha = .18f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
                modifier = Modifier.testTag("item-usage-rank-chip-$rank")
            ) {
                Text(
                    rank,
                    color = if (high) AppColors.Crimson else AppColors.Steel,
                    style = AppType.Metadata.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
        relation.quantity?.let {
            if (rank != null) Text("·", color = AppColors.Ink.copy(alpha = .52f), style = AppType.Metadata)
            Text("×$it", color = AppColors.Ink.copy(alpha = .72f), style = AppType.Metadata)
        }
    }
}

internal fun usageRelationSummary(family: ItemUsageFamily, relation: ItemUsageRelation): String = when (family) {
    ItemUsageFamily.WEAPONS -> "${relation.routeLabel ?: "Route"} · ×${relation.quantity ?: "?"}"
    ItemUsageFamily.DECORATIONS -> listOfNotNull(
        relation.rank?.let { if (it.contains("HIGH", true)) "HR" else if (it.contains("LOW", true)) "LR" else it },
        relation.quantity?.let { "×$it" }
    ).joinToString(" · ")
    ItemUsageFamily.COMBINATIONS -> listOfNotNull(
        relation.otherIngredientName?.let { "with $it" },
        relation.successPercent?.let { "$it%" },
        when {
            relation.outputQuantityMin != null && relation.outputQuantityMax != null && relation.outputQuantityMin != relation.outputQuantityMax -> "×${relation.outputQuantityMin}–${relation.outputQuantityMax}"
            relation.outputQuantityMin != null -> "×${relation.outputQuantityMin}"
            else -> null
        }
    ).joinToString(" · ")
    ItemUsageFamily.QUEST_DELIVERY -> "Deliver ×${relation.quantity ?: "?"}"
    ItemUsageFamily.TRADING -> tradeUsageRouteSummary(relation)
    // Facility identifiers are source/projection metadata, not user-facing
    // Farm terminology. The family and target already provide the context.
    ItemUsageFamily.FARM -> listOfNotNull(relation.outputQuantity?.let { "Yield ×$it" }, relation.chancePercent?.let { "$it%" }).joinToString(" · ")
    ItemUsageFamily.ROASTING -> listOfNotNull(
        relation.quantity?.let { "Use ×$it" },
        relation.outputQuantity?.let { "Produces ×$it" },
        roastingResultStateLabel(relation.resultState),
        roastingContextLabel(relation.context)
    ).joinToString(" · ")
    // quantitySemantics is an internal provenance enum.  Published quantities
    // remain visible; the enum itself must never leak into the product UI.
    ItemUsageFamily.SCRAP_CONVERSION -> listOfNotNull(relation.quantity?.let { "Use ×$it" }, relation.outputQuantity?.let { "Produces ×$it" }).joinToString(" · ")
}


@Composable
internal fun UsageFamilyScreen(
    material: Material,
    family: ItemUsageFamilyProjection,
    weaponRepository: WeaponRepository?,
    onBack: () -> Unit,
    onTarget: (ItemUsageTarget) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = AppDimens.ScreenPadding).testTag("screen-item-usage-${family.family.name}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            if (family.family == ItemUsageFamily.WEAPONS) {
                Box(Modifier.size(1.dp).testTag("screen-used-in-weapons"))
            }
            androidx.compose.material3.TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(5.dp))
                Text("Back", style = AppType.ButtonLabel)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ItemIcon(material.gameItemId, material.name, Modifier.size(52.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(family.family.displayLabel, color = AppColors.Parchment, style = AppType.ScreenTitle, maxLines = 1)
                    Text(material.name, color = AppColors.ParchmentDeep, style = AppType.Body, maxLines = 1)
                }
                Text(family.family.countLabel(family.targetCount), color = AppColors.Gold, style = AppType.Metadata)
            }
        }
        items(family.targets, key = { it.targetIdentity }) { target ->
            ParchmentSurface(Modifier.fillMaxWidth()) {
                UsageTargetRow(target, family.family, material.gameItemId ?: -1, weaponRepository, onTarget)
            }
        }
    }
}
