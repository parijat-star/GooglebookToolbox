package com.droid.googlebooktoolbox.lidguard

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LidGuardEvaluatorAndFuzzTest {

    @Test
    fun defaultConfig_isDisabledByDefault() {
        val defaults = LidGuardConfig()
        assertFalse(defaults.enabled)

        val store = SharedPrefsLidGuardStore(ApplicationProvider.getApplicationContext())
        assertFalse(store.loadConfig().enabled)
    }

    @Test
    fun whenDisabled_neverHoldsWakeLockEvenWithActiveTasks() {
        val now = 1_700_000_000_000L
        val decision = LidGuardEvaluator.evaluate(
            config = LidGuardConfig(enabled = false),
            signals = ActiveTaskSignals(
                vmHeartbeatTimestampMs = now - 1000L,
                antigravityNotificationActive = true,
                terminalNotificationActive = true,
                manualHoldUntilMs = now + 30_000L,
            ),
            nowMs = now,
            lastActiveSeenAtMs = now - 1000L,
            currentHoldStartedAtMs = now - 5000L,
        )
        assertFalse(decision.shouldHoldWakeLock)
        assertEquals(WakeLockMode.DISABLED, decision.mode)
    }

    @Test
    fun whenEnabled_acquiresWakeLockForActiveTaskAndReleasesAfterCooldown() {
        val t0 = 1_700_000_000_000L
        val config = LidGuardConfig(enabled = true, cooldownSeconds = 45)

        // 1. Active Linux VM build heartbeat -> acquire wake lock
        val activeDecision = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(
                vmHeartbeatTimestampMs = t0 - 2_000L,
                vmActiveTaskSummary = "gradlew",
                isInteractive = false,
            ),
            nowMs = t0,
            lastActiveSeenAtMs = null,
            currentHoldStartedAtMs = null,
        )
        assertTrue(activeDecision.shouldHoldWakeLock)
        assertEquals(WakeLockMode.ACTIVE_HOLDING_WAKELOCK, activeDecision.mode)
        assertTrue(activeDecision.activeSources.any { it.contains("gradlew") })
        assertTrue(activeDecision.reason.contains("Lid closed"))

        // 2. Task completes (heartbeat stops, stale after 35s), 10s elapsed since last active -> stays in 45s cooldown
        val tCooldown = t0 + 10_000L
        val cooldownDecision = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(isInteractive = false),
            nowMs = tCooldown,
            lastActiveSeenAtMs = t0,
            currentHoldStartedAtMs = t0,
        )
        assertTrue(cooldownDecision.shouldHoldWakeLock)
        assertEquals(WakeLockMode.ACTIVE_HOLDING_WAKELOCK, cooldownDecision.mode)
        assertEquals(35, cooldownDecision.cooldownRemainingSeconds)

        // 3. After cooldown window elapses -> releases wake lock so device suspends
        val tExpired = t0 + 46_000L
        val releaseDecision = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(isInteractive = false),
            nowMs = tExpired,
            lastActiveSeenAtMs = t0,
            currentHoldStartedAtMs = t0,
        )
        assertFalse(releaseDecision.shouldHoldWakeLock)
        assertEquals(WakeLockMode.IDLE_ALLOW_SUSPEND, releaseDecision.mode)
        assertNull(releaseDecision.nextTransitionHoldStartedAtMs)
    }

    @Test
    fun safetyCutoffs_releaseWakeLockOnLowUnpluggedBatteryOrMaxHoldTimeout() {
        val t0 = 1_700_000_000_000L
        val config = LidGuardConfig(
            enabled = true,
            lowBatteryCutoffPercent = 15,
            maxHoldMinutes = 60,
        )

        val lowBatteryUnplugged = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(
                antigravityNotificationActive = true,
                batteryPercent = 12,
                isCharging = false,
            ),
            nowMs = t0,
            lastActiveSeenAtMs = t0,
            currentHoldStartedAtMs = t0 - 60_000L,
        )
        assertFalse(lowBatteryUnplugged.shouldHoldWakeLock)
        assertEquals(WakeLockMode.RELEASED_SAFETY_CUTOFF, lowBatteryUnplugged.mode)

        val lowBatteryCharging = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(
                antigravityNotificationActive = true,
                batteryPercent = 12,
                isCharging = true,
            ),
            nowMs = t0,
            lastActiveSeenAtMs = t0,
            currentHoldStartedAtMs = t0 - 60_000L,
        )
        assertTrue(lowBatteryCharging.shouldHoldWakeLock)

        val maxHoldExceeded = LidGuardEvaluator.evaluate(
            config = config,
            signals = ActiveTaskSignals(
                terminalNotificationActive = true,
                batteryPercent = 80,
                isCharging = true,
            ),
            nowMs = t0 + 61 * 60_000L,
            lastActiveSeenAtMs = t0 + 61 * 60_000L,
            currentHoldStartedAtMs = t0,
        )
        assertFalse(maxHoldExceeded.shouldHoldWakeLock)
        assertEquals(WakeLockMode.RELEASED_SAFETY_CUTOFF, maxHoldExceeded.mode)
    }

    @Test
    fun parseHeartbeatContent_handlesValidAndFuzzedPayloadsSafely() {
        val valid = LidGuardEvaluator.parseHeartbeatContent(
            raw = "active=1\nts=1700000000\ntask=gradle,python\n",
            fileLastModifiedMs = 1234L,
        )
        assertNotNull(valid)
        assertTrue(valid!!.active)
        assertEquals(1_700_000_000_000L, valid.timestampMs)
        assertEquals("gradle,python", valid.summary)

        val rng = Random(20261113)
        repeat(200) {
            val len = rng.nextInt(0, 4096)
            val fuzzed = buildString(len) {
                repeat(len) { append(rng.nextInt(0, 128).toChar()) }
            }
            // Must never throw or allocate unbounded strings.
            val parsed = LidGuardEvaluator.parseHeartbeatContent(fuzzed, 5000L)
            if (parsed?.summary != null) {
                assertTrue(parsed.summary!!.length <= 80)
            }
        }
    }
}
