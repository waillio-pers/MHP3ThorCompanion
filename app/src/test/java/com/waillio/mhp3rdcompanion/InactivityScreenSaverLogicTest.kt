package com.waillio.mhp3rdcompanion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InactivityScreenSaverLogicTest {
    @Test
    fun timeoutDefaultsOffPersistsEverySupportedValueAndNormalizesInvalidValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("oled-screen-saver-logic-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        assertEquals(0, readOledScreenSaverTimeoutMinutes(preferences))
        OLED_SCREEN_SAVER_TIMEOUT_OPTIONS.forEach { minutes ->
            persistOledScreenSaverTimeoutMinutes(preferences, minutes)
            assertEquals(minutes, readOledScreenSaverTimeoutMinutes(preferences))
        }
        listOf(-1, 3, 4, 30).forEach { invalid ->
            preferences.edit().putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, invalid).commit()
            assertEquals(0, readOledScreenSaverTimeoutMinutes(preferences))
        }
        preferences.edit().clear().commit()
    }

    @Test
    fun supportedDurationsMapToSemanticMinuteValues() {
        assertEquals(0L, oledScreenSaverTimeoutMillis(0))
        assertEquals(60_000L, oledScreenSaverTimeoutMillis(1))
        assertEquals(120_000L, oledScreenSaverTimeoutMillis(2))
        assertEquals(300_000L, oledScreenSaverTimeoutMillis(5))
        assertEquals(600_000L, oledScreenSaverTimeoutMillis(10))
        assertEquals(900_000L, oledScreenSaverTimeoutMillis(15))
        assertEquals(0L, oledScreenSaverTimeoutMillis(3))
    }

    @Test
    fun retainedDurationMigratesLegacyStateAndActiveValueWinsOverStaleRetainedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("oled-screen-saver-migration-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        assertEquals(1, readOledScreenSaverLastDurationMinutes(preferences))
        assertEquals(1, preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, -1))

        preferences.edit()
            .putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 5)
            .putInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, 15)
            .commit()
        assertEquals(5, readOledScreenSaverLastDurationMinutes(preferences))
        assertEquals(5, preferences.getInt(OLED_SCREEN_SAVER_LAST_DURATION_KEY, -1))

        preferences.edit().putInt(OLED_SCREEN_SAVER_TIMEOUT_KEY, 0).commit()
        assertEquals(5, readOledScreenSaverLastDurationMinutes(preferences))
        preferences.edit().clear().commit()
    }

    @Test
    fun offNeverActivatesAndActivityRestartsOnlyOnePendingTimeout() = runTest {
        val off = newController(backgroundScope, 0)
        off.onForegroundResumed()
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertFalse(off.saverActive.value)

        val controller = newController(backgroundScope, 1)
        controller.onForegroundResumed()
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        repeat(3) {
            advanceTimeBy(200)
            controller.onUserActivity()
            runCurrent()
        }
        assertFalse(controller.saverActive.value)
        advanceTimeBy(999)
        runCurrent()
        assertFalse(controller.saverActive.value)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(controller.saverActive.value)
    }

    @Test
    fun wakeRestartsTimerAndChangingTimeoutResetsOrCancelsIt() = runTest {
        val controller = newController(backgroundScope, 1)
        controller.onForegroundResumed()
        runCurrent()
        advanceTimeBy(900)
        controller.setTimeoutMinutes(2)
        runCurrent()
        advanceTimeBy(1_999)
        runCurrent()
        assertFalse(controller.saverActive.value)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(controller.saverActive.value)

        controller.wake()
        runCurrent()
        assertFalse(controller.saverActive.value)
        advanceTimeBy(1_999)
        runCurrent()
        assertFalse(controller.saverActive.value)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(controller.saverActive.value)

        controller.setTimeoutMinutes(1)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(controller.saverActive.value)
        controller.setTimeoutMinutes(0)
        assertFalse(controller.saverActive.value)
        advanceTimeBy(10_000)
        runCurrent()
        assertFalse(controller.saverActive.value)
    }

    @Test
    fun backgroundSuspendsTimerAndResumeStartsFreshInterval() = runTest {
        val controller = newController(backgroundScope, 1)
        controller.onForegroundResumed()
        runCurrent()
        advanceTimeBy(500)
        controller.onForegroundPaused()
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertFalse(controller.saverActive.value)

        controller.onForegroundResumed()
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertFalse(controller.saverActive.value)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(controller.saverActive.value)

        controller.onForegroundPaused()
        runCurrent()
        assertFalse(controller.saverActive.value)
    }

    @Test
    fun recreationKeepsPersistedSettingButNeverRestoresActiveSaverState() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("oled-screen-saver-recreation-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        persistOledScreenSaverTimeoutMinutes(preferences, 1)

        val beforeRecreation = newController(backgroundScope, readOledScreenSaverTimeoutMinutes(preferences))
        beforeRecreation.onForegroundResumed()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(beforeRecreation.saverActive.value)
        beforeRecreation.onForegroundPaused()
        runCurrent()

        val afterRecreation = newController(backgroundScope, readOledScreenSaverTimeoutMinutes(preferences))
        assertFalse(afterRecreation.saverActive.value)
        afterRecreation.onForegroundResumed()
        runCurrent()
        advanceTimeBy(999)
        runCurrent()
        assertFalse(afterRecreation.saverActive.value)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(afterRecreation.saverActive.value)
        preferences.edit().clear().commit()
    }

    private fun newController(scope: CoroutineScope, minutes: Int) =
        InactivityScreenSaverController(scope, minutes) { it * 1_000L }
}
