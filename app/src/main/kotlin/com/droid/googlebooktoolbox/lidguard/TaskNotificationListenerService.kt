package com.droid.googlebooktoolbox.lidguard

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationSignalState(
    val antigravityActive: Boolean = false,
    val terminalActive: Boolean = false,
    val summary: String? = null,
    val updatedAtMs: Long = 0L,
)

/**
 * Monitors active ongoing notifications from Antigravity and Android's Linux Terminal
 * (`com.android.virtualization.terminal`) when the user grants Notification Access.
 * Zero network or root permissions required.
 */
class TaskNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _signalState.value = NotificationSignalState()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        val current = try {
            activeNotifications ?: emptyArray()
        } catch (_: SecurityException) {
            emptyArray()
        }
        _signalState.value = evaluateNotifications(current, System.currentTimeMillis())
    }

    companion object {
        private val TERMINAL_PACKAGES = setOf(
            "com.android.virtualization.terminal",
        )

        private val ANTIGRAVITY_PACKAGE_HINTS = listOf(
            "antigravity",
            "com.google.android.antigravity",
        )

        private val IDLE_KEYWORDS = listOf(
            "idle",
            "completed",
            "finished",
            "ready",
            "stopped",
        )

        private val ACTIVE_KEYWORDS = listOf(
            "running",
            "executing",
            "building",
            "working",
            "processing",
            "streaming",
            "compiling",
            "installing",
            "testing",
            "task",
            "vm",
            "terminal",
        )

        private val _signalState = MutableStateFlow(NotificationSignalState())
        val signalState: StateFlow<NotificationSignalState> = _signalState.asStateFlow()

        fun isGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val componentName = ComponentName(context, TaskNotificationListenerService::class.java)
            val flat = componentName.flattenToString()
            val shortFlat = componentName.flattenToShortString()
            return enabledListeners.split(':').any { entry ->
                val trimmed = entry.trim()
                trimmed.equals(flat, ignoreCase = true) ||
                    trimmed.equals(shortFlat, ignoreCase = true)
            }
        }

        internal fun evaluateNotifications(
            notifications: Array<StatusBarNotification>,
            nowMs: Long,
        ): NotificationSignalState {
            var antigravityActive = false
            var terminalActive = false
            var summary: String? = null

            for (sbn in notifications) {
                val pkg = sbn.packageName?.lowercase() ?: continue
                val notification = sbn.notification ?: continue
                val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                    (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0

                val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                val combined = "$title $text".lowercase()

                if (pkg in TERMINAL_PACKAGES) {
                    if (isOngoing && IDLE_KEYWORDS.none { combined.contains(it) }) {
                        terminalActive = true
                        if (summary == null && title.isNotBlank()) {
                            summary = title.take(60)
                        }
                    }
                    continue
                }

                if (ANTIGRAVITY_PACKAGE_HINTS.any { pkg.contains(it) }) {
                    val hasActiveHint = ACTIVE_KEYWORDS.any { combined.contains(it) }
                    val hasIdleHint = IDLE_KEYWORDS.any { combined.contains(it) }
                    if ((isOngoing || hasActiveHint) && !hasIdleHint) {
                        antigravityActive = true
                        if (title.isNotBlank()) {
                            summary = title.take(60)
                        }
                    }
                }
            }

            return NotificationSignalState(
                antigravityActive = antigravityActive,
                terminalActive = terminalActive,
                summary = summary,
                updatedAtMs = nowMs,
            )
        }
    }
}
