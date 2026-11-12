package com.droid.googlebooktoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droid.googlebooktoolbox.apprestorer.permissions.RequiredPermission
import com.droid.googlebooktoolbox.apprestorer.ui.MainScreen
import com.droid.googlebooktoolbox.apprestorer.ui.MainUiState
import com.droid.googlebooktoolbox.featurechecker.model.DependencyCheckItem
import com.droid.googlebooktoolbox.featurechecker.model.DependencyId
import com.droid.googlebooktoolbox.featurechecker.model.FeatureCheckerUiState
import com.droid.googlebooktoolbox.featurechecker.model.ThemeMode
import com.droid.googlebooktoolbox.featurechecker.ui.FeatureCheckerScreen
import com.droid.googlebooktoolbox.featurechecker.ui.theme.LocalStudioColors
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.model.ToolboxUiState

@Composable
fun GooglebookToolboxApp(
  toolboxState: ToolboxUiState,
  onSelectDestination: (ToolboxDestination) -> Unit,
  onDismissWelcomePopup: () -> Unit,
  onOpenWelcomePopup: () -> Unit,
  featureCheckerState: FeatureCheckerUiState,
  onSelectFeature: (String) -> Unit,
  onToggleCheckMark: (DependencyId) -> Unit,
  onFocusDependency: (Int) -> Unit,
  onMoveFocusNext: () -> Unit,
  onMoveFocusPrevious: () -> Unit,
  onToggleFocusedDependency: () -> Unit,
  onExecuteAction: (DependencyCheckItem) -> Unit,
  onRefreshChecks: () -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onCycleThemeMode: () -> Unit,
  onSendFeedbackReport: () -> Unit,
  onOpenReferenceThread: () -> Unit,
  appRestorerState: MainUiState,
  onGrantRestorerPermission: (RequiredPermission) -> Unit,
  onOpenRestorerAppInfo: () -> Unit,
  onCaptureWorkspace: () -> Unit,
  onOpenRestorePrompt: () -> Unit,
  onDismissPendingRestore: () -> Unit,
  onExportDiagnostics: () -> Unit,
  onAutoRestoreChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current
  val metCount = featureCheckerState.dependencies.count { it.isMet }
  val totalDeps = featureCheckerState.dependencies.size
  val restorerStatus = when {
    appRestorerState.isArmed -> "Armed & capturing before reboot"
    else -> "${appRestorerState.permissions.grantedCount}/${RequiredPermission.entries.size} setup steps complete"
  }

  if (toolboxState.showWelcomePopup) {
    ToolboxWelcomeDialog(onDismiss = onDismissWelcomePopup)
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(Modifier.fillMaxSize()) {
      // Top window caption / status bar inset strip across the unified Toolbox window
      val captionTop = WindowInsets.captionBar.getTop(LocalDensity.current)
      if (captionTop > 0) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { captionTop.toDp() })
            .background(studio.chrome)
        )
      }
      Box(Modifier.fillMaxWidth().background(studio.chrome)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
      }

      BoxWithConstraints(Modifier.fillMaxSize()) {
        val useSideBySidePanes = maxWidth >= 600.dp

        if (useSideBySidePanes) {
          Row(Modifier.fillMaxSize()) {
            // Left Navigation Pane
            ToolboxLeftPane(
              selectedDestination = toolboxState.selectedDestination,
              featureCheckerStatus = "$metCount of $totalDeps dependencies met",
              appRestorerStatus = restorerStatus,
              appRestorerArmed = appRestorerState.isArmed,
              featureCheckerAllMet = metCount == totalDeps && totalDeps > 0,
              onSelectDestination = onSelectDestination,
              onOpenWelcomePopup = onOpenWelcomePopup,
              compact = false,
              modifier = Modifier
                .width(312.dp)
                .fillMaxHeight()
                .background(studio.chrome)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .testTag("toolbox_left_pane")
            )

            VerticalDivider(
              thickness = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )

            // Right Active Utility Pane
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("toolbox_right_pane")
            ) {
              ToolboxRightPaneContent(
                selectedDestination = toolboxState.selectedDestination,
                featureCheckerState = featureCheckerState,
                onSelectFeature = onSelectFeature,
                onToggleCheckMark = onToggleCheckMark,
                onFocusDependency = onFocusDependency,
                onMoveFocusNext = onMoveFocusNext,
                onMoveFocusPrevious = onMoveFocusPrevious,
                onToggleFocusedDependency = onToggleFocusedDependency,
                onExecuteAction = onExecuteAction,
                onRefreshChecks = onRefreshChecks,
                onSetThemeMode = onSetThemeMode,
                onCycleThemeMode = onCycleThemeMode,
                onSendFeedbackReport = onSendFeedbackReport,
                onOpenReferenceThread = onOpenReferenceThread,
                appRestorerState = appRestorerState,
                onGrantRestorerPermission = onGrantRestorerPermission,
                onOpenRestorerAppInfo = onOpenRestorerAppInfo,
                onCaptureWorkspace = onCaptureWorkspace,
                onOpenRestorePrompt = onOpenRestorePrompt,
                onDismissPendingRestore = onDismissPendingRestore,
                onExportDiagnostics = onExportDiagnostics,
                onAutoRestoreChange = onAutoRestoreChange
              )
            }
          }
        } else {
          // Compact screen fallback (e.g. when opened on a companion phone)
          Column(Modifier.fillMaxSize()) {
            ToolboxLeftPane(
              selectedDestination = toolboxState.selectedDestination,
              featureCheckerStatus = "$metCount/$totalDeps met",
              appRestorerStatus = restorerStatus,
              appRestorerArmed = appRestorerState.isArmed,
              featureCheckerAllMet = metCount == totalDeps && totalDeps > 0,
              onSelectDestination = onSelectDestination,
              onOpenWelcomePopup = onOpenWelcomePopup,
              compact = true,
              modifier = Modifier
                .fillMaxWidth()
                .background(studio.chrome)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("toolbox_left_pane")
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("toolbox_right_pane")
            ) {
              ToolboxRightPaneContent(
                selectedDestination = toolboxState.selectedDestination,
                featureCheckerState = featureCheckerState,
                onSelectFeature = onSelectFeature,
                onToggleCheckMark = onToggleCheckMark,
                onFocusDependency = onFocusDependency,
                onMoveFocusNext = onMoveFocusNext,
                onMoveFocusPrevious = onMoveFocusPrevious,
                onToggleFocusedDependency = onToggleFocusedDependency,
                onExecuteAction = onExecuteAction,
                onRefreshChecks = onRefreshChecks,
                onSetThemeMode = onSetThemeMode,
                onCycleThemeMode = onCycleThemeMode,
                onSendFeedbackReport = onSendFeedbackReport,
                onOpenReferenceThread = onOpenReferenceThread,
                appRestorerState = appRestorerState,
                onGrantRestorerPermission = onGrantRestorerPermission,
                onOpenRestorerAppInfo = onOpenRestorerAppInfo,
                onCaptureWorkspace = onCaptureWorkspace,
                onOpenRestorePrompt = onOpenRestorePrompt,
                onDismissPendingRestore = onDismissPendingRestore,
                onExportDiagnostics = onExportDiagnostics,
                onAutoRestoreChange = onAutoRestoreChange
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ToolboxLeftPane(
  selectedDestination: ToolboxDestination,
  featureCheckerStatus: String,
  appRestorerStatus: String,
  appRestorerArmed: Boolean,
  featureCheckerAllMet: Boolean,
  onSelectDestination: (ToolboxDestination) -> Unit,
  onOpenWelcomePopup: () -> Unit,
  compact: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 18.dp)
  ) {
    // Brand Header with Custom Logo + About Trigger
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        GooglebookToolboxLogo(size = if (compact) 40.dp else 50.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Googlebook Toolbox",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Utilities to manage your Googlebook",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      IconButton(
        onClick = onOpenWelcomePopup,
        modifier = Modifier
          .size(36.dp)
          .testTag("open_about_popup_button")
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = "About Googlebook Toolbox",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }

    if (!compact) {
      Text(
        text = "TOOLS",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 6.dp)
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ToolboxNavigationOptionCard(
        title = ToolboxDestination.COMPANION_CONNECTIVITY.navTitle,
        subtitle = featureCheckerStatus,
        icon = Icons.Default.Phonelink,
        selected = selectedDestination == ToolboxDestination.COMPANION_CONNECTIVITY,
        statusPositive = featureCheckerAllMet,
        compact = compact,
        onClick = { onSelectDestination(ToolboxDestination.COMPANION_CONNECTIVITY) },
        modifier = Modifier.testTag("nav_item_feature_checker")
      )

      ToolboxNavigationOptionCard(
        title = ToolboxDestination.DESKTOP_RESTORER.navTitle,
        subtitle = appRestorerStatus,
        icon = Icons.Default.Restore,
        selected = selectedDestination == ToolboxDestination.DESKTOP_RESTORER,
        statusPositive = appRestorerArmed,
        compact = compact,
        onClick = { onSelectDestination(ToolboxDestination.DESKTOP_RESTORER) },
        modifier = Modifier.testTag("nav_item_app_restorer")
      )
    }
  }
}

@Composable
private fun ToolboxNavigationOptionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  selected: Boolean,
  statusPositive: Boolean,
  compact: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current
  val containerColor = if (selected) {
    MaterialTheme.colorScheme.primary
  } else {
    studio.card
  }
  val titleColor = if (selected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  val subColor = if (selected) {
    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }
  val iconTint = if (selected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.primary
  }
  val dotColor = when {
    selected -> MaterialTheme.colorScheme.onPrimary
    statusPositive -> studio.metGreen
    else -> MaterialTheme.colorScheme.tertiary
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(containerColor)
      .clickable(onClick = onClick)
      .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 10.dp else 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(if (compact) 34.dp else 38.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(
          if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
          else MaterialTheme.colorScheme.secondaryContainer
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp)
      )
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Text(
        text = title,
        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = titleColor
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(dotColor)
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = subColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun ToolboxRightPaneContent(
  selectedDestination: ToolboxDestination,
  featureCheckerState: FeatureCheckerUiState,
  onSelectFeature: (String) -> Unit,
  onToggleCheckMark: (DependencyId) -> Unit,
  onFocusDependency: (Int) -> Unit,
  onMoveFocusNext: () -> Unit,
  onMoveFocusPrevious: () -> Unit,
  onToggleFocusedDependency: () -> Unit,
  onExecuteAction: (DependencyCheckItem) -> Unit,
  onRefreshChecks: () -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onCycleThemeMode: () -> Unit,
  onSendFeedbackReport: () -> Unit,
  onOpenReferenceThread: () -> Unit,
  appRestorerState: MainUiState,
  onGrantRestorerPermission: (RequiredPermission) -> Unit,
  onOpenRestorerAppInfo: () -> Unit,
  onCaptureWorkspace: () -> Unit,
  onOpenRestorePrompt: () -> Unit,
  onDismissPendingRestore: () -> Unit,
  onExportDiagnostics: () -> Unit,
  onAutoRestoreChange: (Boolean) -> Unit
) {
  when (selectedDestination) {
    ToolboxDestination.COMPANION_CONNECTIVITY -> {
      FeatureCheckerScreen(
        uiState = featureCheckerState,
        onSelectFeature = onSelectFeature,
        onToggleCheckMark = onToggleCheckMark,
        onFocusDependency = onFocusDependency,
        onMoveFocusNext = onMoveFocusNext,
        onMoveFocusPrevious = onMoveFocusPrevious,
        onToggleFocusedDependency = onToggleFocusedDependency,
        onExecuteAction = onExecuteAction,
        onRefreshChecks = onRefreshChecks,
        onSetThemeMode = onSetThemeMode,
        onCycleThemeMode = onCycleThemeMode,
        onSendFeedbackReport = onSendFeedbackReport,
        onOpenReferenceThread = onOpenReferenceThread,
        embeddedInToolbox = true
      )
    }

    ToolboxDestination.DESKTOP_RESTORER -> {
      MainScreen(
        state = appRestorerState,
        onGrant = onGrantRestorerPermission,
        onOpenAppInfo = onOpenRestorerAppInfo,
        onCapture = onCaptureWorkspace,
        onOpenRestorePrompt = onOpenRestorePrompt,
        onDismissPending = onDismissPendingRestore,
        onExportDiagnostics = onExportDiagnostics,
        onAutoRestoreChange = onAutoRestoreChange,
        embeddedInToolbox = true
      )
    }
  }
}

@Composable
fun ToolboxWelcomeDialog(
  onDismiss: () -> Unit
) {
  val studio = LocalStudioColors.current
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth(0.92f)
        .testTag("welcome_popup_dialog"),
      shape = RoundedCornerShape(28.dp),
      color = studio.card,
      tonalElevation = 6.dp,
      shadowElevation = 12.dp
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        GooglebookToolboxLogo(size = 76.dp)

        Text(
          text = "Welcome to Googlebook Toolbox",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "Googlebook Toolbox is a collection of useful utilities and tools to manage, diagnose, and get more out of your Googlebook — all in one unified workspace.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          WelcomeToolRow(
            icon = Icons.Default.Phonelink,
            title = "Companion Phone Connectivity Checker",
            description = "Verify all 7 Better Together cross-device dependencies across your Googlebook and phone with one-click fixes."
          )
          WelcomeToolRow(
            icon = Icons.Default.Restore,
            title = "Desktop App & Workspace Restorer",
            description = "Automatically capture and restore open desktop windows, Virtual Desks, multi-display layouts, and Chrome tabs after reboot."
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier.testTag("welcome_popup_dismiss_button")
          ) {
            Text(
              text = "Get Started",
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WelcomeToolRow(
  icon: ImageVector,
  title: String,
  description: String
) {
  val studio = LocalStudioColors.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(studio.chrome)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .padding(top = 2.dp)
        .size(22.dp)
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
