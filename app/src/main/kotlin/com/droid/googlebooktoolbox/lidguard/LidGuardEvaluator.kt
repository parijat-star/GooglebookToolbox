package com.droid.googlebooktoolbox.lidguard

import kotlin.math.ceil

/**
 * Pure deterministic evaluator that decides when [LidGuardForegroundService] should
 * acquire or release `PowerManager.PARTIAL_WAKE_LOCK` + `WifiManager.WifiLock`.
 *
 * Designed for zero system/root permissions: relies on user-granted VM heartbeat files,
 * user-granted `NotificationListenerService` signals, and explicit test holds.
 */
object LidGuardEvaluator {

    const val HEARTBEAT_FILENAME = ".googlebook-toolbox-task-heartbeat"
    const val HEARTBEAT_STALE_MS = 35_000L
    const val MAX_HEARTBEAT_BYTES = 2048

    fun evaluate(
        config: LidGuardConfig,
        signals: ActiveTaskSignals,
        nowMs: Long,
        lastActiveSeenAtMs: Long?,
        currentHoldStartedAtMs: Long?,
    ): WakeLockDecision {
        if (!config.enabled) {
            return WakeLockDecision(
                shouldHoldWakeLock = false,
                mode = WakeLockMode.DISABLED,
                reason = "Disabled. Device enters suspend normally on lid close.",
                nextTransitionHoldStartedAtMs = null,
            )
        }

        // Safety guardrail 1: protect unplugged battery from deep drain.
        if (!signals.isCharging && signals.batteryPercent in 0 until config.lowBatteryCutoffPercent) {
            return WakeLockDecision(
                shouldHoldWakeLock = false,
                mode = WakeLockMode.RELEASED_SAFETY_CUTOFF,
                reason = "Released wake lock: unplugged battery at ${signals.batteryPercent}% (below ${config.lowBatteryCutoffPercent}% floor).",
                nextTransitionHoldStartedAtMs = null,
            )
        }

        // Safety guardrail 2: cap continuous wake-lock duration in case a process hangs.
        if (currentHoldStartedAtMs != null && nowMs >= currentHoldStartedAtMs) {
            val maxHoldMs = config.maxHoldMinutes.coerceAtLeast(5) * 60_000L
            if (nowMs - currentHoldStartedAtMs >= maxHoldMs) {
                return WakeLockDecision(
                    shouldHoldWakeLock = false,
                    mode = WakeLockMode.RELEASED_SAFETY_CUTOFF,
                    reason = "Released wake lock: reached ${config.maxHoldMinutes}m continuous safety limit.",
                    nextTransitionHoldStartedAtMs = null,
                )
            }
        }

        val activeSources = mutableListOf<String>()
        val hbTs = signals.vmHeartbeatTimestampMs
        if (hbTs != null && hbTs <= nowMs + 10_000L && (nowMs - hbTs) <= HEARTBEAT_STALE_MS) {
            val detail = signals.vmActiveTaskSummary?.takeIf { it.isNotBlank() }
            activeSources += if (detail != null) "Linux Terminal ($detail)" else "Linux Terminal VM"
        }
        if (signals.terminalNotificationActive) {
            activeSources += "Terminal Notification"
        }
        if (signals.antigravityNotificationActive) {
            val detail = signals.notificationSummary?.takeIf { it.isNotBlank() }
            activeSources += if (detail != null) "Antigravity ($detail)" else "Antigravity"
        }
        if (signals.manualHoldUntilMs > nowMs) {
            activeSources += "Manual Session Hold"
        }

        if (activeSources.isNotEmpty()) {
            val holdStart = currentHoldStartedAtMs ?: nowMs
            val lidSuffix = if (signals.isInteractive) "" else " · Lid closed, protecting task"
            return WakeLockDecision(
                shouldHoldWakeLock = true,
                mode = WakeLockMode.ACTIVE_HOLDING_WAKELOCK,
                reason = "Holding wake lock for ${activeSources.joinToString(", ")}$lidSuffix.",
                activeSources = activeSources,
                cooldownRemainingSeconds = config.cooldownSeconds,
                nextTransitionHoldStartedAtMs = holdStart,
            )
        }

        // Cool-down grace window after active signals go idle so multi-step VM / LLM turns
        // don't drop the wake lock between back-to-back commands.
        val cooldownMs = config.cooldownSeconds.coerceIn(
            LidGuardConfig.MIN_COOLDOWN_SECONDS,
            LidGuardConfig.MAX_COOLDOWN_SECONDS,
        ) * 1000L

        if (lastActiveSeenAtMs != null && nowMs >= lastActiveSeenAtMs) {
            val elapsed = nowMs - lastActiveSeenAtMs
            if (elapsed < cooldownMs) {
                val remainingSec = ceil((cooldownMs - elapsed) / 1000.0).toInt().coerceAtLeast(1)
                return WakeLockDecision(
                    shouldHoldWakeLock = true,
                    mode = WakeLockMode.ACTIVE_HOLDING_WAKELOCK,
                    reason = "Task just finished — waiting ${remainingSec}s cooldown before allowing suspend.",
                    cooldownRemainingSeconds = remainingSec,
                    nextTransitionHoldStartedAtMs = currentHoldStartedAtMs ?: nowMs,
                )
            }
        }

        return WakeLockDecision(
            shouldHoldWakeLock = false,
            mode = WakeLockMode.IDLE_ALLOW_SUSPEND,
            reason = "No active Antigravity or Terminal task. Wake lock released so device can suspend on lid close.",
            nextTransitionHoldStartedAtMs = null,
        )
    }

    /**
     * Parses a bounded heartbeat payload written by the Linux VM helper script.
     * Format (line-delimited key=value):
     *   active=1
     *   ts=1731542400000
     *   task=gradle,python
     */
    fun parseHeartbeatContent(raw: String?, fileLastModifiedMs: Long): HeartbeatPayload? {
        if (raw.isNullOrBlank()) return null
        val bounded = raw.take(MAX_HEARTBEAT_BYTES)
        var active: Boolean? = null
        var ts = fileLastModifiedMs
        var summary: String? = null

        bounded.lineSequence().take(16).forEach { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim().lowercase()
            val value = line.substring(idx + 1).trim()
            when (key) {
                "active" -> active = value == "1" || value.equals("true", ignoreCase = true)
                "ts", "timestamp" -> {
                    val parsed = value.toLongOrNull()
                    if (parsed != null && parsed > 0L) {
                        // Normalize epoch seconds to milliseconds if needed.
                        ts = if (parsed < 1_000_000_000_000L) parsed * 1000L else parsed
                    }
                }
                "task", "tasks", "summary" -> {
                    summary = value.filter { !it.isISOControl() }.take(80).ifBlank { null }
                }
            }
        }

        val isActive = active ?: return null
        return HeartbeatPayload(
            active = isActive,
            timestampMs = ts,
            summary = summary,
        )
    }

    /**
     * One-line shell installer + watcher script that the user can copy into the Android
     * Linux Terminal VM. It monitors busy build/agent processes inside the VM and touches
     * the shared heartbeat file only while work is running.
     */
    fun buildVmWatcherCommand(sharedFolderPathHint: String = "/mnt/shared/Documents"): String {
        return """
cat << 'EOF' > ~/.local/bin/agy-lid-heartbeat.sh && chmod +x ~/.local/bin/agy-lid-heartbeat.sh && nohup ~/.local/bin/agy-lid-heartbeat.sh >/tmp/agy-lid-heartbeat.log 2>&1 &
#!/usr/bin/env bash
set -u
HB_DIR="${sharedFolderPathHint.trimEnd('/')}"
HB_FILE="${'$'}HB_DIR/$HEARTBEAT_FILENAME"
PATTERN='(gradle|gradlew|java|javac|python|python3|node|npm|pnpm|yarn|cargo|rustc|gcc|g\+\+|clang|make|cmake|ninja|git|curl|wget|apt|dpkg|pip|uv|pytest|docker|podman)'
mkdir -p "${'$'}HB_DIR" 2>/dev/null || true
while true; do
  TASKS=$(pgrep -fa "${'$'}PATTERN" | awk '{print ${'$'}2}' | xargs -r -n1 basename 2>/dev/null | sort -u | head -n 4 | paste -sd ',' -)
  NOW_MS=$(( $(date +%s) * 1000 ))
  if [ -n "${'$'}TASKS" ]; then
    printf 'active=1\nts=%s\ntask=%s\n' "${'$'}NOW_MS" "${'$'}TASKS" > "${'$'}HB_FILE"
  else
    printf 'active=0\nts=%s\ntask=\n' "${'$'}NOW_MS" > "${'$'}HB_FILE"
  fi
  sleep 5
done
EOF
        """.trimIndent()
    }
}
