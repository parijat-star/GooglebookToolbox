package com.droid.googlebooktoolbox.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

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
  val density = LocalDensity.current

  val featureCheckerHoverSource = remember { MutableInteractionSource() }
  val appRestorerHoverSource = remember { MutableInteractionSource() }
  val isFeatureCheckerHovered by featureCheckerHoverSource.collectIsHoveredAsState()
  val isAppRestorerHovered by appRestorerHoverSource.collectIsHoveredAsState()

  val hoveredDestination = when {
    isFeatureCheckerHovered -> ToolboxDestination.COMPANION_CONNECTIVITY
    isAppRestorerHovered -> ToolboxDestination.DESKTOP_RESTORER
    else -> null
  }

  var containerOriginInRoot by remember { mutableStateOf(Offset.Zero) }
  val itemBoundsInRoot = remember { mutableStateMapOf<ToolboxDestination, Rect>() }

  if (toolboxState.showWelcomePopup) {
    ToolboxWelcomeDialog(onDismiss = onDismissWelcomePopup)
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(Modifier.fillMaxSize()) {
      // Top window caption / status bar inset strip across the unified Toolbox window
      val captionTop = WindowInsets.captionBar.getTop(density)
      if (captionTop > 0) {
        Box(
          Modifier
            .fillMaxWidth()
            .height(with(density) { captionTop.toDp() })
            .background(studio.chrome)
        )
      }
      Box(Modifier.fillMaxWidth().background(studio.chrome)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
      }

      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .onGloballyPositioned { coords ->
            containerOriginInRoot = coords.boundsInRoot().topLeft
          }
      ) {
        val useSideBySidePanes = maxWidth >= 600.dp

        if (useSideBySidePanes) {
          Row(Modifier.fillMaxSize()) {
            // Left Navigation Pane
            ToolboxLeftPane(
              selectedDestination = toolboxState.selectedDestination,
              featureCheckerHoverSource = featureCheckerHoverSource,
              appRestorerHoverSource = appRestorerHoverSource,
              onItemPositioned = { dest, rect -> itemBoundsInRoot[dest] = rect },
              onSelectDestination = onSelectDestination,
              onOpenWelcomePopup = onOpenWelcomePopup,
              compact = false,
              modifier = Modifier
                .width(332.dp)
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
              featureCheckerHoverSource = featureCheckerHoverSource,
              appRestorerHoverSource = appRestorerHoverSource,
              onItemPositioned = { dest, rect -> itemBoundsInRoot[dest] = rect },
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

        // Non-interactive cursor hover pop-up anchored directly below the hovered left-pane option
        hoveredDestination?.let { dest ->
          val anchorRect = itemBoundsInRoot[dest]
          val offsetX = if (anchorRect != null) {
            (anchorRect.left - containerOriginInRoot.x + with(density) { 6.dp.toPx() })
              .roundToInt()
              .coerceAtLeast(with(density) { 12.dp.toPx() }.roundToInt())
          } else {
            with(density) { 24.dp.toPx() }.roundToInt()
          }
          val offsetY = if (anchorRect != null) {
            (anchorRect.bottom - containerOriginInRoot.y + with(density) { 8.dp.toPx() }).roundToInt()
          } else {
            with(density) { 140.dp.toPx() }.roundToInt()
          }
          val popupTag = when (dest) {
            ToolboxDestination.COMPANION_CONNECTIVITY -> "hover_popup_feature_checker"
            ToolboxDestination.DESKTOP_RESTORER -> "hover_popup_app_restorer"
          }

          ToolboxHoverPopup(
            description = dest.description,
            modifier = Modifier
              .offset { IntOffset(offsetX, offsetY) }
              .testTag(popupTag)
          )
        }
      }
    }
  }
}

@Composable
private fun ToolboxLeftPane(
  selectedDestination: ToolboxDestination,
  featureCheckerHoverSource: MutableInteractionSource,
  appRestorerHoverSource: MutableInteractionSource,
  onItemPositioned: (ToolboxDestination, Rect) -> Unit,
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
        GooglebookToolboxLogo(size = if (compact) 40.dp else 48.dp)
        Text(
          text = "Googlebook Toolbox",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      ToolboxNavigationOptionCard(
        title = ToolboxDestination.COMPANION_CONNECTIVITY.navTitle,
        icon = Icons.Default.Phonelink,
        selected = selectedDestination == ToolboxDestination.COMPANION_CONNECTIVITY,
        compact = compact,
        hoverInteractionSource = featureCheckerHoverSource,
        onPositioned = { rect -> onItemPositioned(ToolboxDestination.COMPANION_CONNECTIVITY, rect) },
        onClick = { onSelectDestination(ToolboxDestination.COMPANION_CONNECTIVITY) },
        modifier = Modifier.testTag("nav_item_feature_checker")
      )

      ToolboxNavigationOptionCard(
        title = ToolboxDestination.DESKTOP_RESTORER.navTitle,
        icon = Icons.Default.Restore,
        selected = selectedDestination == ToolboxDestination.DESKTOP_RESTORER,
        compact = compact,
        hoverInteractionSource = appRestorerHoverSource,
        onPositioned = { rect -> onItemPositioned(ToolboxDestination.DESKTOP_RESTORER, rect) },
        onClick = { onSelectDestination(ToolboxDestination.DESKTOP_RESTORER) },
        modifier = Modifier.testTag("nav_item_app_restorer")
      )
    }
  }
}

@Composable
private fun ToolboxNavigationOptionCard(
  title: String,
  icon: ImageVector,
  selected: Boolean,
  compact: Boolean,
  hoverInteractionSource: MutableInteractionSource,
  onPositioned: (Rect) -> Unit,
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
  val iconTint = if (selected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.primary
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot()) }
      .clip(RoundedCornerShape(20.dp))
      .background(containerColor)
      .hoverable(interactionSource = hoverInteractionSource)
      .clickable(onClick = onClick)
      .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = if (compact) 11.dp else 14.dp),
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

    Text(
      text = title,
      style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.SemiBold,
      color = titleColor,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun ToolboxHoverPopup(
  description: String,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current
  Surface(
    modifier = modifier.widthIn(max = 320.dp),
    shape = RoundedCornerShape(14.dp),
    color = studio.card,
    tonalElevation = 4.dp,
    shadowElevation = 8.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Text(
      text = description,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
    )
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
        .widthIn(max = 460.dp)
        .fillMaxWidth(0.90f)
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
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Googlebook Toolbox is a handy toolbox of useful utilities and tools designed to help you manage, troubleshoot, and get the most out of your Googlebook — all in one place.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
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
