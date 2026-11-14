package com.droid.googlebooktoolbox.featurechecker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droid.googlebooktoolbox.featurechecker.data.isLaptopOrDesktopFormFactor
import com.droid.googlebooktoolbox.featurechecker.data.isSamsungDevice
import com.droid.googlebooktoolbox.featurechecker.model.CheckStatus
import com.droid.googlebooktoolbox.featurechecker.model.DependencyCheckItem
import com.droid.googlebooktoolbox.featurechecker.model.DependencyId
import com.droid.googlebooktoolbox.featurechecker.model.FeatureCheckerUiState
import com.droid.googlebooktoolbox.featurechecker.model.PhoneReportState
import com.droid.googlebooktoolbox.featurechecker.model.ThemeMode
import com.droid.googlebooktoolbox.featurechecker.ui.theme.LocalStudioColors

/**
 * Android 16 / googlebook.studio grouped card corner radius shape:
 * outer edges are 22.dp, internal edges between stacked rows are 6.dp with 2.dp gap.
 */
fun groupShape(i: Int, n: Int, big: Dp = 22.dp, small: Dp = 6.dp) = when {
  n <= 1 -> RoundedCornerShape(big)
  i == 0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomEnd = small, bottomStart = small)
  i == n - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomEnd = big, bottomStart = big)
  else -> RoundedCornerShape(small)
}

@Composable
private fun CaptionSpacer(color: Color) {
  val top = WindowInsets.captionBar.getTop(LocalDensity.current)
  if (top > 0) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(with(LocalDensity.current) { top.toDp() })
        .background(color)
    )
  }
}

@Composable
private fun HeaderRow(
  background: Color,
  height: Dp = 56.dp,
  content: @Composable RowScope.() -> Unit
) {
  Row(
    Modifier
      .fillMaxWidth()
      .height(height)
      .background(background)
      .padding(horizontal = 24.dp),
    verticalAlignment = Alignment.CenterVertically,
    content = content
  )
}

@Composable
private fun StatusDot(text: String, ok: Boolean) {
  val dotColor = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Box(
      Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(dotColor)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun FeatureCheckerScreen(
  uiState: FeatureCheckerUiState,
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
  modifier: Modifier = Modifier,
  embeddedInToolbox: Boolean = false
) {
  val studio = LocalStudioColors.current
  val chrome = studio.chrome
  val metCount = uiState.dependencies.count { it.isMet }
  val totalCount = uiState.dependencies.size
  val allMet = metCount == totalCount && totalCount > 0

  Surface(
    modifier = modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown &&
          !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed
        ) {
          when (event.key) {
            Key.R -> {
              onRefreshChecks()
              true
            }
            Key.T -> {
              onCycleThemeMode()
              true
            }
            Key.F -> {
              onSendFeedbackReport()
              true
            }
            else -> false
          }
        } else {
          false
        }
      },
    color = MaterialTheme.colorScheme.surface
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val isCompactPhone = maxWidth < 600.dp
      val wide = maxWidth >= 800.dp
      val horizontalPad = when {
        isCompactPhone -> 16.dp
        wide -> 32.dp
        else -> 24.dp
      }

      Column(Modifier.fillMaxSize()) {
        if (!embeddedInToolbox) {
          CaptionSpacer(chrome)
          Box(Modifier.fillMaxWidth().background(chrome)) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
          }
        }

        // Adaptive googlebook.studio Header (2 compact rows on phone < 600dp, 1 wide row on tablet/desktop)
        if (isCompactPhone) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(chrome)
              .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Googlebook Feature Checker",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Spacer(Modifier.width(8.dp))
              RecheckPillButton(onRefreshChecks = onRefreshChecks)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Box(modifier = Modifier.weight(1f)) {
                StatusDot(
                  text = if (allMet) {
                    "All $totalCount met · ${uiState.localDeviceName}"
                  } else {
                    "$metCount/$totalCount met · ${uiState.localDeviceName}"
                  },
                  ok = allMet
                )
              }
              Spacer(Modifier.width(8.dp))
              ConnectedThemeSwitcher(
                currentMode = uiState.themeMode,
                compact = true,
                onSetThemeMode = onSetThemeMode
              )
            }
          }
        } else {
          HeaderRow(background = chrome, height = 56.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = "Googlebook Feature Checker",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
              StatusDot(
                text = if (allMet) {
                  "All $totalCount dependencies met · ${uiState.localDeviceName}"
                } else {
                  "$metCount of $totalCount dependencies met · ${uiState.localDeviceName}"
                },
                ok = allMet
              )
            }

            Spacer(Modifier.weight(1f))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ConnectedThemeSwitcher(
                currentMode = uiState.themeMode,
                compact = false,
                onSetThemeMode = onSetThemeMode
              )
              RecheckPillButton(onRefreshChecks = onRefreshChecks)
            }
          }
        }

        // Optional quiet toast/banner row when an action is triggered
        uiState.statusBannerMessage?.let { bannerMsg ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(chrome)
              .padding(horizontal = horizontalPad, vertical = 6.dp)
              .testTag("status_banner")
          ) {
            Text(
              text = bannerMsg,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        StudioDependenciesColumn(
          uiState = uiState,
          isCompactPhone = isCompactPhone,
          horizontalPad = horizontalPad,
          onToggleCheckMark = onToggleCheckMark,
          onFocusDependency = onFocusDependency,
          onExecuteAction = onExecuteAction,
          onSendFeedbackReport = onSendFeedbackReport,
          onOpenReferenceThread = onOpenReferenceThread
        )
      }
    }
  }
}

@Composable
private fun ConnectedThemeSwitcher(
  currentMode: ThemeMode,
  compact: Boolean,
  onSetThemeMode: (ThemeMode) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    ThemeMode.entries.forEachIndexed { idx, mode ->
      val selected = currentMode == mode
      val shape = when (idx) {
        0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
        ThemeMode.entries.lastIndex -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
        else -> RoundedCornerShape(6.dp)
      }
      Box(
        modifier = Modifier
          .height(if (compact) 30.dp else 34.dp)
          .clip(shape)
          .background(
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer
          )
          .semantics {
            role = Role.Tab
            this.selected = selected
          }
          .clickable { onSetThemeMode(mode) }
          .padding(horizontal = if (compact) 9.dp else 12.dp)
          .testTag("theme_chip_${mode.name}"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = mode.displayName,
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
    }
  }
}

@Composable
private fun RecheckPillButton(onRefreshChecks: () -> Unit) {
  Box(
    modifier = Modifier
      .height(34.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.secondaryContainer)
      .clickable(role = Role.Button, onClick = onRefreshChecks)
      .padding(horizontal = 12.dp)
      .testTag("recheck_all_button"),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Re-check",
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "Re-check",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer
      )
    }
  }
}

private const val DEPENDENCY_INFO_WEIGHT = 1.25f
private const val THIS_DEVICE_COL_WEIGHT = 0.85f
private const val COMPANION_DEVICE_COL_WEIGHT = 1.05f

@Composable
private fun StudioDependenciesColumn(
  uiState: FeatureCheckerUiState,
  isCompactPhone: Boolean,
  horizontalPad: Dp,
  onToggleCheckMark: (DependencyId) -> Unit,
  onFocusDependency: (Int) -> Unit,
  onExecuteAction: (DependencyCheckItem) -> Unit,
  onSendFeedbackReport: () -> Unit,
  onOpenReferenceThread: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = horizontalPad)
      .testTag("dependencies_lazy_column"),
    contentPadding = PaddingValues(vertical = if (isCompactPhone) 16.dp else 24.dp),
    verticalArrangement = Arrangement.spacedBy(if (isCompactPhone) 18.dp else 24.dp)
  ) {
    // Group 1: Dependencies Checklist with "This device" & "Companion device" column headers
    item {
      if (isCompactPhone) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Dependencies",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "This device",
              modifier = Modifier
                .weight(1f)
                .testTag("column_header_this_device"),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Companion device",
              modifier = Modifier
                .weight(1f)
                .testTag("column_header_companion_device"),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Dependencies",
            modifier = Modifier.weight(DEPENDENCY_INFO_WEIGHT),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "This device",
            modifier = Modifier
              .weight(THIS_DEVICE_COL_WEIGHT)
              .testTag("column_header_this_device"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Companion device",
            modifier = Modifier
              .weight(COMPANION_DEVICE_COL_WEIGHT)
              .testTag("column_header_companion_device"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    items(
      count = uiState.dependencies.size,
      key = { idx -> uiState.dependencies[idx].id.name }
    ) { index ->
      val item = uiState.dependencies[index]
      val shape = groupShape(index, uiState.dependencies.size)
      DependencySettingRow(
        item = item,
        shape = shape,
        isFocused = index == uiState.focusedDependencyIndex,
        isCompactPhone = isCompactPhone,
        onToggleCheckMark = {
          onFocusDependency(index)
          onToggleCheckMark(item.id)
        },
        onFocus = { onFocusDependency(index) },
        onExecuteAction = { onExecuteAction(item) }
      )
    }

    // Group 2: Companion Phone Dependency Check / Tip
    item {
      CompanionPhoneGroup(
        phoneReportState = uiState.phoneReportState
      )
    }

    // Group 3: Troubleshooting & Feedback Report to Google
    item {
      TroubleshootingFeedbackGroup(
        onSendFeedbackReport = onSendFeedbackReport,
        onOpenReferenceThread = onOpenReferenceThread
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DependencySettingRow(
  item: DependencyCheckItem,
  shape: RoundedCornerShape,
  isFocused: Boolean,
  isCompactPhone: Boolean,
  onToggleCheckMark: () -> Unit,
  onFocus: () -> Unit,
  onExecuteAction: () -> Unit
) {
  val studio = LocalStudioColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  val bgColor = when {
    isFocused || isHovered -> MaterialTheme.colorScheme.surfaceContainerLow
    else -> studio.card
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(bgColor)
      .hoverable(interactionSource)
      .focusable()
      .onKeyEvent { event ->
        if (item.shouldShowActionButton &&
          event.type == KeyEventType.KeyDown &&
          (event.key == Key.Spacebar || event.key == Key.Enter)
        ) {
          onExecuteAction()
          true
        } else {
          false
        }
      }
      .clickable { onFocus() }
      .padding(horizontal = if (isCompactPhone) 14.dp else 20.dp, vertical = 14.dp)
      .testTag("dependency_card_${item.id.name}"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    @Suppress("UNUSED_VARIABLE")
    val unusedToggle = onToggleCheckMark
    if (isCompactPhone) {
      // Compact Phone (< 600dp): Dependency description on top, followed by 2-column ("This device" | "Companion device") grid
      DependencyDescriptionBlock(item = item)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "This device",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
          ThisDeviceStatusCell(
            item = item,
            onToggleCheckMark = onToggleCheckMark,
            onFocus = onFocus,
            onExecuteAction = onExecuteAction
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "Companion device",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
          CompanionDeviceStatusCell(
            item = item,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    } else {
      // Tablet & Googlebook Desktop (>= 600dp): 3-column table row aligned with ("Dependencies" | "This device" | "Companion device")
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        DependencyDescriptionBlock(
          item = item,
          modifier = Modifier.weight(DEPENDENCY_INFO_WEIGHT)
        )

        ThisDeviceStatusCell(
          item = item,
          onToggleCheckMark = onToggleCheckMark,
          onFocus = onFocus,
          onExecuteAction = onExecuteAction,
          modifier = Modifier.weight(THIS_DEVICE_COL_WEIGHT)
        )

        CompanionDeviceStatusCell(
          item = item,
          modifier = Modifier.weight(COMPANION_DEVICE_COL_WEIGHT)
        )
      }
    }
  }
}

@Composable
private fun DependencyDescriptionBlock(
  item: DependencyCheckItem,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = "${item.stepNumber}. ${item.title}",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = if (item.requirementSummary.isNotBlank()) {
        "${item.detectedStatus} · ${item.requirementSummary}"
      } else {
        item.detectedStatus
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (!item.isMet && item.status != CheckStatus.ADVISORY) {
      Text(
        text = item.whyNotOrFixInstruction,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary
      )
    }
    if (!item.warningOrAdvisoryNote.isNullOrBlank()) {
      Text(
        text = item.warningOrAdvisoryNote,
        style = MaterialTheme.typography.bodySmall,
        color = studio.onAdvisoryContainer,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(studio.advisoryContainer)
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("advisory_note_${item.id.name}")
      )
    }
  }
}

@Composable
private fun ThisDeviceStatusCell(
  item: DependencyCheckItem,
  onToggleCheckMark: () -> Unit,
  onFocus: () -> Unit,
  onExecuteAction: () -> Unit,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current
  @Suppress("UNUSED_VARIABLE")
  val unusedToggle = onToggleCheckMark

  Column(
    modifier = modifier.testTag("this_device_column_${item.id.name}"),
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    when (item.status) {
      CheckStatus.ADVISORY -> {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(studio.advisoryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("advisory_badge_${item.id.name}")
        ) {
          Text(
            text = "Verify",
            style = MaterialTheme.typography.labelMedium,
            color = studio.onAdvisoryContainer,
            maxLines = 1,
            softWrap = false
          )
        }
        Button(
          onClick = {
            onFocus()
            onExecuteAction()
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
          modifier = Modifier.testTag("action_button_${item.id.name}")
        ) {
          Text(
            text = item.actionLabel,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }

      CheckStatus.NOT_MET, CheckStatus.PERMISSION_REQUIRED -> {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(studio.warnContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("not_met_badge_${item.id.name}")
        ) {
          Text(
            text = "Not met",
            style = MaterialTheme.typography.labelMedium,
            color = studio.onWarnContainer,
            maxLines = 1,
            softWrap = false
          )
        }
        Button(
          onClick = {
            onFocus()
            onExecuteAction()
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
          modifier = Modifier.testTag("action_button_${item.id.name}")
        ) {
          Text(
            text = item.actionLabel,
            style = MaterialTheme.typography.labelMedium
          )
        }
      }

      CheckStatus.MET, CheckStatus.WARNING -> {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(studio.metGreenContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("met_badge_${item.id.name}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = studio.onMetGreenContainer,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = if (item.status == CheckStatus.WARNING) "Met (Check)" else "Met",
              style = MaterialTheme.typography.labelMedium,
              color = studio.onMetGreenContainer,
              maxLines = 1,
              softWrap = false
            )
          }
        }

        if (item.hasUpdateAvailable) {
          val secondaryLabel = if (item.actionLabel.startsWith("Grant")) {
            item.actionLabel
          } else {
            "Update Available"
          }
          Button(
            onClick = {
              onFocus()
              onExecuteAction()
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("update_available_button_${item.id.name}")
          ) {
            Text(
              text = secondaryLabel,
              style = MaterialTheme.typography.labelMedium
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CompanionDeviceStatusCell(
  item: DependencyCheckItem,
  modifier: Modifier = Modifier
) {
  val studio = LocalStudioColors.current

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(studio.chrome.copy(alpha = 0.6f))
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("companion_device_column_${item.id.name}"),
    verticalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    when (item.companionIsMet) {
      true -> {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(studio.metGreenContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = studio.onMetGreenContainer,
              modifier = Modifier.size(12.dp)
            )
            Text(
              text = "Met",
              style = MaterialTheme.typography.labelMedium,
              color = studio.onMetGreenContainer,
              maxLines = 1,
              softWrap = false
            )
          }
        }
        Text(
          text = item.phoneRequirementNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      false -> {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(studio.warnContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "Not met",
            style = MaterialTheme.typography.labelMedium,
            color = studio.onWarnContainer,
            maxLines = 1,
            softWrap = false
          )
        }
        Text(
          text = item.phoneRequirementNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.tertiary
        )
      }

      null -> {
        Text(
          text = item.phoneRequirementNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun CompanionPhoneGroup(
  phoneReportState: PhoneReportState
) {
  val studio = LocalStudioColors.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("companion_phone_section"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Other device",
      modifier = Modifier.padding(horizontal = 20.dp),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary
    )

    when (phoneReportState) {
      is PhoneReportState.Connected -> {
        val rowsCount = phoneReportState.items.size + 1
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(groupShape(0, rowsCount))
              .background(studio.card)
              .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Column {
                Text(
                  text = phoneReportState.snapshot.phoneModel,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Other device reported live status",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          phoneReportState.items.forEachIndexed { idx, phoneItem ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(groupShape(idx + 1, rowsCount))
                .background(studio.card)
                .padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = phoneItem.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${phoneItem.statusText} · ${if (phoneItem.isMet) "Met" else "Not met"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (phoneItem.isMet) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.tertiary
                }
              )
            }
          }
        }
      }

      is PhoneReportState.Unreachable -> {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          // Row 1: Tip to run the same app on the phone (no "Check phone" button)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(groupShape(0, 2))
              .background(studio.card)
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Icon(
              imageVector = Icons.Default.PhoneAndroid,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = phoneReportState.tipTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = phoneReportState.tipMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Row 2: Highlighted with a light green background so "What your phone also needs" pops out
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(groupShape(1, 2))
              .background(studio.metGreenContainer)
              .padding(horizontal = 20.dp, vertical = 14.dp)
              .testTag("phone_needs_highlight_card"),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Text(
              text = "What the other device also needs",
              style = MaterialTheme.typography.titleSmall,
              color = studio.onMetGreenContainer
            )
            phoneReportState.phoneRequirementsChecklist.forEach { req ->
              Text(
                text = "• $req",
                style = MaterialTheme.typography.bodySmall,
                color = studio.onMetGreenContainer
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TroubleshootingFeedbackGroup(
  onSendFeedbackReport: () -> Unit,
  onOpenReferenceThread: () -> Unit
) {
  val studio = LocalStudioColors.current
  val isPhone = !isLaptopOrDesktopFormFactor(LocalContext.current)
  val isSamsungPhone = isPhone && isSamsungDevice()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("google_feedback_card"),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Still not working?",
      modifier = Modifier.padding(horizontal = 20.dp),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary
    )

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      // Row 1: Send bug / feedback report to Google
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 64.dp)
          .clip(groupShape(0, 2))
          .background(studio.card)
          .clickable(role = Role.Button, onClick = onSendFeedbackReport)
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .testTag("send_feedback_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "Send a bug to Google using feedback report",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = when {
              isSamsungPhone -> "Opens Google Settings (? -> Send feedback)"
              isPhone -> "Opens Settings -> About Phone -> Send Feedback"
              else -> "Copies your device & dependency diagnostics and opens Google Feedback Report"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text(
            text = "Send Feedback Report",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary
          )
        }
      }

      // Row 2: Open community thread
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 58.dp)
          .clip(groupShape(1, 2))
          .background(studio.card)
          .clickable(role = Role.Button, onClick = onOpenReferenceThread)
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .testTag("bottom_support_thread_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = "Update on Better Together setup & Samsung Galaxy rollout",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "support.google.com/googlebook/thread/472325070",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
