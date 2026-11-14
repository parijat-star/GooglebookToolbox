package com.droid.googlebooktoolbox.lidguard

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droid.googlebooktoolbox.featurechecker.ui.theme.LocalStudioColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LidGuardScreen(
    uiState: LidGuardUiState,
    onEnabledChange: (Boolean) -> Unit,
    onCooldownChange: (Int) -> Unit,
    onLowBatteryCutoffChange: (Int) -> Unit,
    onMaxHoldMinutesChange: (Int) -> Unit,
    onPickSharedFolder: () -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenAppBatterySettings: (String) -> Unit,
    onTriggerManualHold: () -> Unit,
    onClearManualHold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studio = LocalStudioColors.current
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Primary toggle + live wake-lock state card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = if (uiState.config.enabled) cs.primary.copy(alpha = 0.45f) else cs.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    uiState.wakeLockCurrentlyHeld -> studio.metGreenContainer
                                    uiState.config.enabled -> cs.primaryContainer
                                    else -> cs.surfaceContainerHigh
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LaptopChromebook,
                            contentDescription = null,
                            tint = when {
                                uiState.wakeLockCurrentlyHeld -> studio.onMetGreenContainer
                                uiState.config.enabled -> cs.primary
                                else -> cs.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Antigravity and Terminal tasks to continue when lid close",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Default is OFF. When ON, Toolbox holds a partial wake lock & Wi-Fi lock only while an active Antigravity or Linux Terminal task is running, then releases it so the device suspends.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }

                    Switch(
                        checked = uiState.config.enabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = cs.onPrimary,
                            checkedTrackColor = cs.primary,
                            uncheckedTrackColor = cs.surfaceContainerHighest,
                        ),
                        modifier = Modifier.testTag("lid_guard_master_switch"),
                    )
                }

                // Live status pill & reason
                val (badgeLabel, badgeBg, badgeFg) = when (uiState.decision.mode) {
                    WakeLockMode.ACTIVE_HOLDING_WAKELOCK -> Triple(
                        "Wake Lock Held · Protecting Task (${uiState.holdDurationSeconds}s)",
                        studio.metGreenContainer,
                        studio.onMetGreenContainer,
                    )
                    WakeLockMode.IDLE_ALLOW_SUSPEND -> Triple(
                        "Monitoring · Idle (Sleep Allowed)",
                        cs.primaryContainer,
                        cs.onPrimaryContainer,
                    )
                    WakeLockMode.RELEASED_SAFETY_CUTOFF -> Triple(
                        "Safety Cutoff · Sleep Allowed",
                        studio.advisoryContainer,
                        studio.onAdvisoryContainer,
                    )
                    WakeLockMode.DISABLED -> Triple(
                        "Off · Device Suspends Normally",
                        cs.surfaceContainerHigh,
                        cs.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeBg,
                        modifier = Modifier.testTag("lid_guard_status_badge"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeFg),
                            )
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeFg,
                            )
                        }
                    }
                }

                Text(
                    text = uiState.decision.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.testTag("lid_guard_decision_reason"),
                )

                if (!uiState.lastReleaseReason.isNullOrBlank()) {
                    Text(
                        text = "Last wake-lock release: ${uiState.lastReleaseReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onTriggerManualHold,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("lid_guard_test_hold_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test 30s Hold")
                    }
                    if (uiState.wakeLockCurrentlyHeld) {
                        OutlinedButton(
                            onClick = onClearManualHold,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Release Test Hold")
                        }
                    }
                }
            }
        }

        // Readiness checklist (0 root / 0 system permissions)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
            border = BorderStroke(1.dp, cs.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Non-Root Setup & App Freezer Protection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    text = "Android turns off the display on lid close while preserving PARTIAL_WAKE_LOCK. Grant these user permissions so Android's App Freezer does not pause Toolbox, Terminal, or Antigravity when the lid closes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )

                ReadinessActionRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Toolbox Unrestricted Battery",
                    subtitle = if (uiState.readiness.toolboxBatteryUnrestricted) {
                        "Granted — Toolbox won't be frozen when screen turns off."
                    } else {
                        "Recommended so Toolbox keeps holding the wake lock after lid close."
                    },
                    ready = uiState.readiness.toolboxBatteryUnrestricted,
                    actionLabel = if (uiState.readiness.toolboxBatteryUnrestricted) "Granted" else "Allow",
                    onAction = onRequestBatteryUnrestricted,
                )

                ReadinessActionRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notification Access (Antigravity & Terminal)",
                    subtitle = if (uiState.readiness.notificationListenerGranted) {
                        "Granted — detects active ongoing task notifications automatically."
                    } else {
                        "Lets Toolbox see active Antigravity & Terminal task notifications."
                    },
                    ready = uiState.readiness.notificationListenerGranted,
                    actionLabel = if (uiState.readiness.notificationListenerGranted) "Configured" else "Open Settings",
                    onAction = onOpenNotificationListenerSettings,
                )

                ReadinessActionRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Shared VM Heartbeat Folder",
                    subtitle = if (uiState.readiness.sharedFolderConfigured) {
                        "Connected: ${uiState.readiness.sharedFolderDisplayName ?: "Shared folder"} (reads ${LidGuardEvaluator.HEARTBEAT_FILENAME})."
                    } else {
                        "Select Documents (or shared VM folder) so Toolbox reads Terminal build heartbeats."
                    },
                    ready = uiState.readiness.sharedFolderConfigured,
                    actionLabel = if (uiState.readiness.sharedFolderConfigured) "Change" else "Choose Folder",
                    onAction = onPickSharedFolder,
                )

                if (uiState.readiness.terminalInstalled) {
                    ReadinessActionRow(
                        icon = Icons.Default.Terminal,
                        title = "Linux Terminal Battery Setting",
                        subtitle = "Set Battery usage -> Unrestricted on Terminal so VM isn't paused when lid closes.",
                        ready = false,
                        actionLabel = "App Info",
                        onAction = { onOpenAppBatterySettings(LidGuardController.TERMINAL_PACKAGE) },
                    )
                }

                if (uiState.readiness.antigravityInstalled) {
                    ReadinessActionRow(
                        icon = Icons.Default.Bolt,
                        title = "Antigravity Battery Setting",
                        subtitle = "Set Battery usage -> Unrestricted on Antigravity so active turns keep running.",
                        ready = false,
                        actionLabel = "App Info",
                        onAction = { onOpenAppBatterySettings(LidGuardController.ANTIGRAVITY_PACKAGE) },
                    )
                }
            }
        }

        // Linux Terminal VM Heartbeat Helper Script
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
            border = BorderStroke(1.dp, cs.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linux Terminal VM Watcher (One-Time Setup)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                        )
                        Text(
                            text = "Run inside Linux Terminal so builds (gradle, python, node, cargo, git) signal Toolbox automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "VM Heartbeat Watcher",
                                    uiState.heartbeatWatcherScript,
                                ),
                            )
                            Toast.makeText(
                                context,
                                "Copied VM watcher command to clipboard",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = uiState.heartbeatWatcherScript,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 6,
                    )
                }
            }
        }

        // Guardrail controls
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
            border = BorderStroke(1.dp, cs.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Sleep Guardrails",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )

                Text(
                    text = "Cooldown grace window after task finishes (prevents dropping wake lock between steps):",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(15, 45, 90, 180).forEach { seconds ->
                        FilterChip(
                            selected = uiState.config.cooldownSeconds == seconds,
                            onClick = { onCooldownChange(seconds) },
                            label = { Text("${seconds}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primaryContainer,
                                selectedLabelColor = cs.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Text(
                    text = "Unplugged battery floor (auto-release wake lock below this %):",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(10, 15, 25, 35).forEach { pct ->
                        FilterChip(
                            selected = uiState.config.lowBatteryCutoffPercent == pct,
                            onClick = { onLowBatteryCutoffChange(pct) },
                            label = { Text("$pct%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primaryContainer,
                                selectedLabelColor = cs.onPrimaryContainer,
                            ),
                        )
                    }
                }

                Text(
                    text = "Max continuous hold safety cap:",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(60, 120, 240, 480).forEach { mins ->
                        FilterChip(
                            selected = uiState.config.maxHoldMinutes == mins,
                            onClick = { onMaxHoldMinutesChange(mins) },
                            label = { Text("${mins / 60}h") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primaryContainer,
                                selectedLabelColor = cs.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ready: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val studio = LocalStudioColors.current
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (ready) Icons.Default.CheckCircle else icon,
            contentDescription = null,
            tint = if (ready) studio.metGreen else cs.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(actionLabel)
        }
    }
}
