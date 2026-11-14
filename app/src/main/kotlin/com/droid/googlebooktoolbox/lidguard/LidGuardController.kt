package com.droid.googlebooktoolbox.lidguard

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinates preferences, readiness checks, and [LidGuardForegroundService] lifecycle
 * for "Allow Antigravity and Terminal tasks to continue when lid close".
 */
class LidGuardController(private val appContext: Context) {

    private val store = SharedPrefsLidGuardStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<LidGuardUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            LidGuardForegroundService.snapshot.collect { snap ->
                val config = store.loadConfig()
                val decision = if (config.enabled) {
                    snap.lastDecision
                } else {
                    WakeLockDecision(
                        shouldHoldWakeLock = false,
                        mode = WakeLockMode.DISABLED,
                        reason = "Disabled (default). Device suspends normally on lid close.",
                    )
                }
                _uiState.value = _uiState.value.copy(
                    config = config,
                    signals = snap.lastSignals,
                    decision = decision,
                    wakeLockCurrentlyHeld = config.enabled && snap.wakeLockHeld,
                    holdDurationSeconds = if (config.enabled) snap.holdDurationSeconds else 0L,
                    lastReleaseReason = store.getLastReleaseReason(),
                )
            }
        }
    }

    fun syncWithPreferences() {
        val config = store.loadConfig()
        if (config.enabled) {
            LidGuardForegroundService.startOrSync(appContext)
        }
        refreshReadiness()
    }

    fun setEnabled(enabled: Boolean) {
        store.saveEnabled(enabled)
        if (enabled) {
            LidGuardForegroundService.startOrSync(appContext)
        } else {
            LidGuardForegroundService.stop(appContext)
        }
        refreshStateFromStore()
    }

    fun setCooldownSeconds(seconds: Int) {
        store.saveCooldownSeconds(seconds)
        refreshStateFromStore()
    }

    fun setLowBatteryCutoffPercent(percent: Int) {
        store.saveLowBatteryCutoffPercent(percent)
        refreshStateFromStore()
    }

    fun setMaxHoldMinutes(minutes: Int) {
        store.saveMaxHoldMinutes(minutes)
        refreshStateFromStore()
    }

    fun persistSharedFolderUri(uri: Uri?) {
        if (uri == null) return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
        }
        store.saveSharedFolderUri(uri.toString())
        refreshReadiness()
        if (store.loadConfig().enabled) {
            LidGuardForegroundService.startOrSync(appContext)
        }
    }

    fun triggerManualTestHold() {
        if (!store.loadConfig().enabled) {
            store.saveEnabled(true)
        }
        LidGuardForegroundService.triggerManualHold(appContext)
        refreshStateFromStore()
    }

    fun clearManualTestHold() {
        LidGuardForegroundService.clearManualHold(appContext)
        refreshStateFromStore()
    }

    fun refreshReadiness() {
        val config = store.loadConfig()
        val pm = appContext.getSystemService(PowerManager::class.java)
        val batteryUnrestricted = pm?.isIgnoringBatteryOptimizations(appContext.packageName) == true
        val notifGranted = TaskNotificationListenerService.isGranted(appContext)
        val folderUriStr = config.sharedFolderUri
        val folderDisplay = folderUriStr?.let { raw ->
            Uri.parse(raw).lastPathSegment?.substringAfterLast(':')?.ifBlank { "Shared folder" }
        }
        val readiness = LidGuardReadiness(
            toolboxBatteryUnrestricted = batteryUnrestricted,
            notificationListenerGranted = notifGranted,
            sharedFolderConfigured = !folderUriStr.isNullOrBlank(),
            sharedFolderDisplayName = folderDisplay,
            terminalInstalled = isPackageInstalled(TERMINAL_PACKAGE),
            antigravityInstalled = isPackageInstalled(ANTIGRAVITY_PACKAGE),
        )
        _uiState.value = _uiState.value.copy(
            config = config,
            readiness = readiness,
            lastReleaseReason = store.getLastReleaseReason(),
            heartbeatWatcherScript = LidGuardEvaluator.buildVmWatcherCommand(),
        )
    }

    fun buildBatteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${appContext.packageName}")
        }

    fun buildNotificationListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun buildAppBatterySettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }

    private fun refreshStateFromStore() {
        val config = store.loadConfig()
        val snap = LidGuardForegroundService.snapshot.value
        val decision = if (config.enabled) {
            snap.lastDecision
        } else {
            WakeLockDecision(
                shouldHoldWakeLock = false,
                mode = WakeLockMode.DISABLED,
                reason = "Disabled (default). Device suspends normally on lid close.",
            )
        }
        _uiState.value = _uiState.value.copy(
            config = config,
            decision = decision,
            wakeLockCurrentlyHeld = config.enabled && snap.wakeLockHeld,
            holdDurationSeconds = if (config.enabled) snap.holdDurationSeconds else 0L,
            lastReleaseReason = store.getLastReleaseReason(),
        )
    }

    private fun buildInitialState(): LidGuardUiState {
        val config = store.loadConfig()
        return LidGuardUiState(
            config = config,
            decision = WakeLockDecision(
                shouldHoldWakeLock = false,
                mode = if (config.enabled) WakeLockMode.IDLE_ALLOW_SUSPEND else WakeLockMode.DISABLED,
                reason = if (config.enabled) {
                    "Monitoring Antigravity & Terminal; sleep allowed until a task runs."
                } else {
                    "Disabled (default). Device suspends normally on lid close."
                },
            ),
            lastReleaseReason = store.getLastReleaseReason(),
            heartbeatWatcherScript = LidGuardEvaluator.buildVmWatcherCommand(),
        )
    }

    private fun isPackageInstalled(packageName: String): Boolean =
        try {
            appContext.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }

    companion object {
        const val TERMINAL_PACKAGE = "com.android.virtualization.terminal"
        const val ANTIGRAVITY_PACKAGE = "com.google.android.antigravity"
    }
}
