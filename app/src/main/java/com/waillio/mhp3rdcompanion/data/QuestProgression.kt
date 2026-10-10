package com.waillio.mhp3rdcompanion.data

import kotlinx.serialization.Serializable

@Serializable
internal data class QuestProgressionData(
    val schemaVersion: Int,
    val evidenceAsOf: String,
    val production: QuestProgressionProduction,
    val sources: List<QuestProgressionSource>,
    val steps: List<QuestProgressionStep>,
    val blockedUrgents: List<BlockedProgressionUrgent>,
    val optionalChallenges: List<OptionalQuestChallenge>,
    val unlockPrerequisites: List<QuestUnlockPrerequisite>,
    val multiplayerHelp: QuestProgressionHelp
) {
    val keyQuestIds: Set<String> by lazy { steps.flatMapTo(mutableSetOf()) { it.keyQuestIds } }
    val mappedUrgentQuestIds: Set<String> by lazy { steps.mapNotNullTo(mutableSetOf()) { it.urgent?.companionQuestId } }
    val optionalChallengeQuestIds: Set<String> by lazy { optionalChallenges.mapTo(mutableSetOf()) { it.questId } }
    val questIdsThatUnlockKeys: Set<String> by lazy { unlockPrerequisites.mapTo(mutableSetOf()) { it.prerequisiteQuestId } }

    fun roleFor(questId: String): QuestProgressionRole? = when {
        questId in keyQuestIds -> QuestProgressionRole.KEY
        questId in mappedUrgentQuestIds -> QuestProgressionRole.URGENT
        questId in optionalChallengeQuestIds -> QuestProgressionRole.OPTIONAL_FINAL_CHALLENGE
        else -> null
    }

    fun prerequisiteFor(questId: String): String? =
        unlockPrerequisites.firstOrNull { it.questId == questId }?.prerequisiteQuestId

    companion object {
        fun empty() = QuestProgressionData(
            schemaVersion = 1,
            evidenceAsOf = "",
            production = QuestProgressionProduction(0, 0, ""),
            sources = emptyList(),
            steps = emptyList(),
            blockedUrgents = emptyList(),
            optionalChallenges = emptyList(),
            unlockPrerequisites = emptyList(),
            multiplayerHelp = QuestProgressionHelp("", "")
        )
    }
}

@Serializable
internal data class QuestProgressionProduction(
    val questCount: Int,
    val bytes: Long,
    val sha256: String
)

@Serializable
internal data class QuestProgressionSource(
    val id: String,
    val title: String,
    val url: String,
    val use: String
)

@Serializable
internal data class QuestProgressionStep(
    val id: String,
    val scope: String,
    val rank: String,
    val star: Int,
    val phase: String,
    val completionRule: String,
    val keyQuestIds: List<String>,
    val urgent: QuestProgressionUrgent? = null,
    val blockedUrgentId: String? = null,
    val requiresUrgentId: String? = null,
    val nextStepId: String? = null,
    val advancesTo: String? = null,
    val sourceReferences: List<String> = emptyList()
)

@Serializable
internal data class QuestProgressionUrgent(
    val id: String,
    val companionQuestId: String?,
    val objectiveSummary: String,
    val summaryLabel: String? = null,
    val sourceReferences: List<String> = emptyList()
)

@Serializable
internal data class BlockedProgressionUrgent(
    val id: String,
    val role: String,
    val scope: String,
    val rank: String,
    val star: Int,
    val phase: String,
    val companionQuestId: String?,
    val objectiveSummary: String,
    val stepId: String,
    val advancesTo: String? = null,
    val sourceReferences: List<String> = emptyList()
)

@Serializable
internal data class OptionalQuestChallenge(
    val questId: String,
    val role: String,
    val label: String,
    val unlockSummary: String,
    val sourceReferences: List<String> = emptyList()
)

@Serializable
internal data class QuestUnlockPrerequisite(
    val questId: String,
    val prerequisiteQuestId: String,
    val relation: String,
    val sourceReferences: List<String> = emptyList()
)

@Serializable
internal data class QuestProgressionHelp(
    val scope: String,
    val text: String
)

internal enum class QuestProgressionRole { KEY, URGENT, OPTIONAL_FINAL_CHALLENGE }
