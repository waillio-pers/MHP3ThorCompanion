package com.waillio.mhp3rdcompanion

import com.waillio.mhp3rdcompanion.data.PreferenceDecodeResult
import com.waillio.mhp3rdcompanion.data.ReferenceEntryCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceDecodeResultTest {
    @Test
    fun absentValidEmptyAndMalformedHistoryHaveDistinctInternalResults() {
        assertTrue(ReferenceEntryCodec.decodeFavoritesResult(null) is PreferenceDecodeResult.Absent)
        assertEquals(
            PreferenceDecodeResult.Valid(emptyList<com.waillio.mhp3rdcompanion.data.FavoriteEntry>()),
            ReferenceEntryCodec.decodeFavoritesResult("[]")
        )
        val malformed = ReferenceEntryCodec.decodeFavoritesResult("legacy")
        assertTrue(malformed is PreferenceDecodeResult.Malformed)
        assertEquals(emptyList<Any>(), ReferenceEntryCodec.decodeFavorites("legacy"))
    }
}
