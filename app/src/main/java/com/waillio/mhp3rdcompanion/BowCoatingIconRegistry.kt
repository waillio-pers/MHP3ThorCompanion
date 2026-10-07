package com.waillio.mhp3rdcompanion

import androidx.annotation.DrawableRes
import com.waillio.mhp3rdcompanion.data.BowCoatingType

/** Authentic local MHP3 item-atlas cells for the seven bow coating families. */
data class BowCoatingIconRef(
    @param:DrawableRes val resourceId: Int,
    val sourceIconKey: String
)

object BowCoatingIconRegistry {
    private val resources = mapOf(
        BowCoatingType.POWER to BowCoatingIconRef(R.drawable.bow_coating_power, "atlas:PAL_9:32:48"),
        BowCoatingType.CLOSE_RANGE to BowCoatingIconRef(R.drawable.bow_coating_close_range, "atlas:PAL_0:32:48"),
        BowCoatingType.POISON to BowCoatingIconRef(R.drawable.bow_coating_poison, "atlas:PAL_4:32:48"),
        BowCoatingType.PARALYSIS to BowCoatingIconRef(R.drawable.bow_coating_paralysis, "atlas:PAL_10:32:48"),
        BowCoatingType.SLEEP to BowCoatingIconRef(R.drawable.bow_coating_sleep, "atlas:PAL_2:32:48"),
        BowCoatingType.PAINT to BowCoatingIconRef(R.drawable.bow_coating_paint, "atlas:PAL_3:32:48"),
        BowCoatingType.EXHAUST to BowCoatingIconRef(R.drawable.bow_coating_exhaust, "atlas:PAL_12:32:48")
    )

    val mappingCount: Int get() = resources.size

    fun resolve(type: BowCoatingType): BowCoatingIconRef? = resources[type]
}
