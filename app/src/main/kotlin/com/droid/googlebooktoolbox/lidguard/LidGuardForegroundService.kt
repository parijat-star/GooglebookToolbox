package com.droid.googlebooktoolbox.lidguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.droid.googlebooktoolbox.MainActivity
import com.droid.googlebooktoolbox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStreamReader

data class LidGuardServiceSnapshot(
    val running: Boolean = false,
    val wakeLockHeld: Boolean = false,
    val holdDurationSeconds: Long = 0L,
    val lastDecision: WakeLockDecision = WakeLockDecision(
        shouldHoldWakeLock = false,
        mode = WakeLockMode.DISABLED,
        reason = "Disabled. Device suspends normally on lid close.",
    ),
    val lastSignals: ActiveTaskSignals = ActiveTaskSignals(),
)

/**
 * Foreground service that monitors active Antigravity & Linux Terminal tasks while
 * "Allow Antigravity and Terminal tasks to continue when lid close" is enabled.
 *
 * Acquires `PowerManager.PARTIAL_WAKE_LOCK` + `WifiManager.WifiLock` ONLY while an
 * active task (or cooldown grace window) is detected, and immediately releases both
 * when the task finishes so the system can enter suspend.
 */
class LidGuardForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    private lateinit var store: SharedPrefsLidGuardStore
    private lateinit var notificationManager: NotificationManager
    private lateinit var powerManager: PowerManager
    private lateinit var batteryManager: BatteryManager
    private var wifiManager: WifiManager? = null

    private var partialWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var lastActiveSeenAtMs: Long? = null
    private var currentHoldStartedAtMs: Long? = null
    private var lastNotificationReason: String? = null

    override fun onCreate() {
        super.onCreate()
        store = SharedPrefsLidGuardStore(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
        batteryManager = getSystemService(BatteryManager::class.java)
        wifiManager = applicationContext.getSystemService(WifiManager::class.java)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseLocks("Stopped by user")
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_MANUAL_HOLD -> {
                manualHoldUntilMs = System.currentTimeMillis() + MANUAL_HOLD_DURATION_MS
            }
            ACTION_CLEAR_MANUAL_HOLD -> {
                manualHoldUntilMs = 0L
            }
        }

        val config = store.loadConfig()
        if (!config.enabled) {
            releaseLocks("Disabled by user")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title = "Lid-close task guard armed",
                text = "Monitoring Antigravity & Terminal; sleep allowed until a task runs.",
                holding = false,
            ),
        )

        if (monitorJob?.isActive != true) {
            monitorJob = scope.launch {
                while (isActive) {
                    evaluateOnce()
                    delay(POLL_INTERVAL_MS)
                }
            }
        } else {
            scope.launch { evaluateOnce() }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        releaseLocks("Service stopped")
        _snapshot.value = _snapshot.value.copy(
            running = false,
            wakeLockHeld = false,
            holdDurationSeconds = 0L,
        )
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun evaluateOnce() {
        val config = store.loadConfig()
        if (!config.enabled) {
            releaseLocks("Disabled by user")
            stopSelf()
            return
        }

        val nowMs = System.currentTimeMillis()
        val hb = readVmHeartbeat(config.sharedFolderUri)
        val notif = TaskNotificationListenerService.signalState.value
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .takeIf { it in 0..100 } ?: 100
        val isCharging = batteryManager.isCharging
        val interactive = powerManager.isInteractive

        val signals = ActiveTaskSignals(
            vmHeartbeatTimestampMs = if (hb?.active == true) hb.timestampMs else null,
            vmActiveTaskSummary = if (hb?.active == true) hb.summary else null,
            antigravityNotificationActive = notif.antigravityActive,
            terminalNotificationActive = notif.terminalActive,
            notificationSummary = notif.summary,
            manualHoldUntilMs = manualHoldUntilMs,
            isInteractive = interactive,
            batteryPercent = batteryPct,
            isCharging = isCharging,
        )

        val decision = LidGuardEvaluator.evaluate(
            config = config,
            signals = signals,
            nowMs = nowMs,
            lastActiveSeenAtMs = lastActiveSeenAtMs,
            currentHoldStartedAtMs = currentHoldStartedAtMs,
        )

        if (decision.activeSources.isNotEmpty()) {
            lastActiveSeenAtMs = nowMs
        }

        if (decision.shouldHoldWakeLock) {
            currentHoldStartedAtMs = decision.nextTransitionHoldStartedAtMs ?: currentHoldStartedAtMs ?: nowMs
            acquireLocks(config.maxHoldMinutes)
        } else {
            if (currentHoldStartedAtMs != null) {
                store.saveLastReleaseReason(decision.reason)
            }
            currentHoldStartedAtMs = null
            if (decision.mode == WakeLockMode.RELEASED_SAFETY_CUTOFF) {
                manualHoldUntilMs = 0L
                lastActiveSeenAtMs = null
            }
            releaseLocks(null)
        }

        val held = partialWakeLock?.isHeld == true
        val holdDurationSec = if (held && currentHoldStartedAtMs != null) {
            ((nowMs - currentHoldStartedAtMs!!) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        _snapshot.value = LidGuardServiceSnapshot(
            running = true,
            wakeLockHeld = held,
            holdDurationSeconds = holdDurationSec,
            lastDecision = decision,
            lastSignals = signals,
        )

        if (decision.reason != lastNotificationReason) {
            lastNotificationReason = decision.reason
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(
                    title = if (held) "Protecting active task (wake lock held)" else "Lid-close guard idle (sleep allowed)",
                    text = decision.reason,
                    holding = held,
                ),
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireLocks(maxHoldMinutes: Int) {
        val timeoutMs = maxHoldMinutes.coerceAtLeast(5) * 60_000L + 60_000L
        if (partialWakeLock == null) {
            partialWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GooglebookToolbox:LidCloseTaskGuard",
            ).apply {
                setReferenceCounted(false)
            }
        }
        if (partialWakeLock?.isHeld == false) {
            partialWakeLock?.acquire(timeoutMs)
        }

        if (wifiLock == null) {
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                "GooglebookToolbox:LidCloseWifiGuard",
            )?.apply {
                setReferenceCounted(false)
            }
        }
        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
        }
    }

    private fun releaseLocks(reason: String?) {
        if (partialWakeLock?.isHeld == true) {
            try {
                partialWakeLock?.release()
            } catch (_: RuntimeException) {
            }
        }
        if (wifiLock?.isHeld == true) {
            try {
                wifiLock?.release()
            } catch (_: RuntimeException) {
            }
        }
        if (!reason.isNullOrBlank()) {
            store.saveLastReleaseReason(reason)
        }
    }

    private fun readVmHeartbeat(folderUriString: String?): HeartbeatPayload? {
        if (folderUriString.isNullOrBlank()) return null
        val treeUri = try {
            Uri.parse(folderUriString)
        } catch (_: RuntimeException) {
            return null
        }

        val docId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: IllegalArgumentException) {
            return null
        }

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        try {
            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    if (name == LidGuardEvaluator.HEARTBEAT_FILENAME) {
                        val childId = if (idCol >= 0) cursor.getString(idCol) else continue
                        val lastMod = if (modCol >= 0) cursor.getLong(modCol) else 0L
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        val text = contentResolver.openInputStream(fileUri)?.use { input ->
                            InputStreamReader(input, Charsets.UTF_8).use { reader ->
                                val buf = CharArray(LidGuardEvaluator.MAX_HEARTBEAT_BYTES)
                                val count = reader.read(buf)
                                if (count > 0) String(buf, 0, count) else ""
                            }
                        }
                        return LidGuardEvaluator.parseHeartbeatContent(text, lastMod)
                    }
                }
            }
        } catch (_: SecurityException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: Exception) {
            return null
        }
        return null
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_lid_guard),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            description = "Shows when Googlebook Toolbox is holding a wake lock for an active Antigravity or Terminal task."
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, text: String, holding: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_INITIAL_TOOL, MainActivity.TOOL_LID_GUARD)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            901,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_restorer)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setSilent(true)
            .setColorized(holding)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "lid_guard"
        private const val NOTIFICATION_ID = 4207
        private const val POLL_INTERVAL_MS = 3_000L
        const val MANUAL_HOLD_DURATION_MS = 30_000L

        const val ACTION_START = "com.droid.googlebooktoolbox.lidguard.START"
        const val ACTION_STOP = "com.droid.googlebooktoolbox.lidguard.STOP"
        const val ACTION_MANUAL_HOLD = "com.droid.googlebooktoolbox.lidguard.MANUAL_HOLD"
        const val ACTION_CLEAR_MANUAL_HOLD = "com.droid.googlebooktoolbox.lidguard.CLEAR_MANUAL_HOLD"

        @Volatile
        private var manualHoldUntilMs: Long = 0L

        private val _snapshot = MutableStateFlow(LidGuardServiceSnapshot())
        val snapshot: StateFlow<LidGuardServiceSnapshot> = _snapshot.asStateFlow()

        fun startOrSync(context: Context) {
            val intent = Intent(context, LidGuardForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LidGuardForegroundService::class.java))
            _snapshot.value = LidGuardServiceSnapshot(
                running = false,
                wakeLockHeld = false,
                holdDurationSeconds = 0L,
                lastDecision = WakeLockDecision(
                    shouldHoldWakeLock = false,
                    mode = WakeLockMode.DISABLED,
                    reason = "Disabled. Device enters suspend normally on lid close.",
                ),
            )
        }

        fun triggerManualHold(context: Context) {
            manualHoldUntilMs = System.currentTimeMillis() + MANUAL_HOLD_DURATION_MS
            val intent = Intent(context, LidGuardForegroundService::class.java).apply {
                action = ACTION_MANUAL_HOLD
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun clearManualHold(context: Context) {
            manualHoldUntilMs = 0L
            val intent = Intent(context, LidGuardForegroundService::class.java).apply {
                action = ACTION_CLEAR_MANUAL_HOLD
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
