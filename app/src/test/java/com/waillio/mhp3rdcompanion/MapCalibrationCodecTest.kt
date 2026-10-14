package com.waillio.mhp3rdcompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCalibrationCodecTest {
    @Test
    fun absentEmptyValidAndPartiallyMalformedCalibrationAreDistinguishable() {
        assertTrue(MapCalibrationCodec.decodeResult(null) is MapCalibrationDecodeResult.Absent)
        assertEquals(MapCalibrationDecodeResult.Valid(emptyMap()), MapCalibrationCodec.decodeResult(""))

        val calibration = MapNodeCalibration(dxSteps = -2, dySteps = 1, exists = false, note = "verify")
        val encoded = MapCalibrationCodec.encode(mapOf("node-1" to calibration))
        assertEquals(MapCalibrationDecodeResult.Valid(mapOf("node-1" to calibration)), MapCalibrationCodec.decodeResult(encoded))

        val malformed = MapCalibrationCodec.decodeResult("$encoded\nbroken-row")
        assertTrue(malformed is MapCalibrationDecodeResult.Malformed)
        assertEquals(1, (malformed as MapCalibrationDecodeResult.Malformed).skippedRows)
        assertEquals(mapOf("node-1" to calibration), malformed.safeValues())
    }
}
