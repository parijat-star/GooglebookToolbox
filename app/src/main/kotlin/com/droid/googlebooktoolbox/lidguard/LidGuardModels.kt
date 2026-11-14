package com.droid.googlebooktoolbox.lidguard

/**
 * Persisted configuration for "Allow Antigravity and Terminal tasks to continue when lid close".
 * Default [enabled] is false as requested.
 */
data class LidGuardConfig(
    val enabled: Boolean = false,
    val cooldownSeconds: Int = DEFAULT_COOLDOWN_SECONDS,
    val lowBatteryCutoffPercent: Int = DEFAULT_LOW_BATTERY_CUTOFF_PERCENT,
    val maxHoldMinutes: Int = DEFAULT_MAX_HOLD_MINUTES,
    val sharedFolderUri: String? = null,
) {
    companion object {
        const val DEFAULT_COOLDOWN_SECONDS = 45
        const val MIN_COOLDOWN_SECONDS = 15
        const val MAX_COOLDOWN_SECONDS = 300
        const val DEFAULT_LOW_BATTERY_CUTOFF_PERCENT = 15
        const val DEFAULT_MAX_HOLD_MINUTES = 120
    }
}

/**
 * Snapshot of all non-privileged signals indicating whether Antigravity (AGY)
 * or Linux Terminal currently has an active ongoing task.
 */
data class ActiveTaskSignals(
    val vmHeartbeatTimestampMs: Long? = null,
    val vmActiveTaskSummary: String? = null,
    val antigravityNotificationActive: Boolean = false,
    val terminalNotificationActive: Boolean = false,
    val notificationSummary: String? = null,
    val manualHoldUntilMs: Long = 0L,
    val isInteractive: Boolean = true,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
)

enum class WakeLockMode {
    DISABLED,
    IDLE_ALLOW_SUSPEND,
    ACTIVE_HOLDING_WAKELOCK,
    RELEASED_SAFETY_CUTOFF,
}

/**
 * Deterministic decision produced by [LidGuardEvaluator].
 */
data class WakeLockDecision(
    val shouldHoldWakeLock: Boolean,
    val mode: WakeLockMode,
    val reason: String,
    val activeSources: List<String> = emptyList(),
    val cooldownRemainingSeconds: Int = 0,
    val nextTransitionHoldStartedAtMs: Long? = null,
)

/**
 * Bounded parse result from the Linux VM heartbeat file inside the user-granted folder.
 */
data class HeartbeatPayload(
    val active: Boolean,
    val timestampMs: Long,
    val summary: String? = null,
)

/**
 * Readiness status for non-root Android power & monitoring prerequisites.
 */
data class LidGuardReadiness(
    val toolboxBatteryUnrestricted: Boolean = false,
    val notificationListenerGranted: Boolean = false,
    val sharedFolderConfigured: Boolean = false,
    val sharedFolderDisplayName: String? = null,
    val terminalInstalled: Boolean = false,
    val antigravityInstalled: Boolean = false,
)

/**
 * UI state rendered by [LidGuardScreen].
 */
data class LidGuardUiState(
    val config: LidGuardConfig = LidGuardConfig(),
    val signals: ActiveTaskSignals = ActiveTaskSignals(),
    val decision: WakeLockDecision = WakeLockDecision(
        shouldHoldWakeLock = false,
        mode = WakeLockMode.DISABLED,
        reason = "Disabled (default). Device suspends normally when lid closes.",
    ),
    val readiness: LidGuardReadiness = LidGuardReadiness(),
    val wakeLockCurrentlyHeld: Boolean = false,
    val holdDurationSeconds: Long = 0L,
    val lastReleaseReason: String? = null,
    val heartbeatWatcherScript: String = "",
)
