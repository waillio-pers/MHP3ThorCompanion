package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.MonsterRewardItemRow
import com.waillio.mhp3rdcompanion.data.MonsterRewardRoute

/** One deterministic visual line in the All or method-filtered reward grid. */
internal data class MonsterRewardRouteGridLine(
    val routes: List<MonsterRewardRoute>,
    val spansBothColumns: Boolean
)

internal data class MonsterRewardFilteredGridLine(
    val items: List<MonsterRewardItemRow>,
    val isTwoUp: Boolean
)

/**
 * Packs routes in their accepted order. A route that cannot fit one half first
 * closes any pending pair, then occupies a full line of its own.
 */
internal fun packMonsterRewardRoutes(
    routes: List<MonsterRewardRoute>,
    fitsHalfWidth: (MonsterRewardRoute) -> Boolean
): List<MonsterRewardRouteGridLine> {
    val lines = mutableListOf<MonsterRewardRouteGridLine>()
    val pair = mutableListOf<MonsterRewardRoute>()

    fun flushPair() {
        if (pair.isNotEmpty()) {
            lines += MonsterRewardRouteGridLine(pair.toList(), spansBothColumns = false)
            pair.clear()
        }
    }

    routes.forEach { route ->
        if (!fitsHalfWidth(route)) {
            flushPair()
            lines += MonsterRewardRouteGridLine(listOf(route), spansBothColumns = true)
        } else {
            pair += route
            if (pair.size == 2) flushPair()
        }
    }
    flushPair()
    return lines
}

/**
 * Pairs only consecutive entries that each have one safely fitting route.
 * Unsafe entries and safe-but-unpaired leftovers receive the full row; neither
 * case causes later Items to jump ahead in the accepted Item order.
 */
internal fun packFilteredMonsterRewardItems(
    items: List<MonsterRewardItemRow>,
    fitsHalfWidth: (MonsterRewardItemRow) -> Boolean
): List<MonsterRewardFilteredGridLine> {
    val lines = mutableListOf<MonsterRewardFilteredGridLine>()
    var pending: MonsterRewardItemRow? = null

    fun flushPendingAsFullWidth() {
        pending?.let { lines += MonsterRewardFilteredGridLine(listOf(it), isTwoUp = false) }
        pending = null
    }

    items.forEach { item ->
        if (!fitsHalfWidth(item)) {
            flushPendingAsFullWidth()
            lines += MonsterRewardFilteredGridLine(listOf(item), isTwoUp = false)
        } else {
            val first = pending
            if (first == null) {
                pending = item
            } else {
                lines += MonsterRewardFilteredGridLine(listOf(first, item), isTwoUp = true)
                pending = null
            }
        }
    }
    flushPendingAsFullWidth()
    return lines
}
