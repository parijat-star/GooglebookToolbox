package com.droid.googlebooktoolbox

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droid.googlebooktoolbox.apprestorer.WorkspaceRuntime
import com.droid.googlebooktoolbox.apprestorer.boot.BootPromptLauncher
import com.droid.googlebooktoolbox.apprestorer.permissions.PermissionCoordinator
import com.droid.googlebooktoolbox.apprestorer.permissions.PermissionState
import com.droid.googlebooktoolbox.apprestorer.permissions.RequiredPermission
import com.droid.googlebooktoolbox.apprestorer.service.WorkspaceMonitorForegroundService
import com.droid.googlebooktoolbox.apprestorer.ui.MainUiState
import com.droid.googlebooktoolbox.data.SharedPrefsToolboxPreferences
import com.droid.googlebooktoolbox.featurechecker.data.DeviceEnvironmentProvider
import com.droid.googlebooktoolbox.featurechecker.data.SystemDeviceEnvironmentProvider
import com.droid.googlebooktoolbox.featurechecker.ui.FeatureCheckerViewModel
import com.droid.googlebooktoolbox.featurechecker.ui.theme.GooglebookFeatureCheckerTheme
import com.droid.googlebooktoolbox.featurechecker.util.ActionNavigator
import com.droid.googlebooktoolbox.featurechecker.util.SystemActionLauncher
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.ui.GooglebookToolboxApp
import com.droid.googlebooktoolbox.ui.ToolboxViewModel

class MainActivity : ComponentActivity() {

  private lateinit var permissions: PermissionCoordinator
  private lateinit var runtime: WorkspaceRuntime
  private var permissionState by mutableStateOf(PermissionState())
  private var toolboxViewModelRef: ToolboxViewModel? = null

  private val notificationRequest =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    permissions = PermissionCoordinator(this)
    runtime = WorkspaceRuntime.get(this)
    refreshPermissions()

    val toolboxPrefs = SharedPrefsToolboxPreferences(applicationContext)
    val initialDest = resolveDestinationFromIntent(intent)
    val environmentProvider: DeviceEnvironmentProvider = SystemDeviceEnvironmentProvider(applicationContext)
    val actionNavigator: ActionNavigator = SystemActionLauncher(this)

    val viewModelFactory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
          modelClass.isAssignableFrom(ToolboxViewModel::class.java) ->
            ToolboxViewModel(toolboxPrefs, initialDest) as T
          modelClass.isAssignableFrom(FeatureCheckerViewModel::class.java) ->
            FeatureCheckerViewModel(environmentProvider) as T
          else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
      }
    }

    setContent {
      val toolboxVm: ToolboxViewModel = viewModel(factory = viewModelFactory)
      toolboxViewModelRef = toolboxVm
      val featureCheckerVm: FeatureCheckerViewModel = viewModel(factory = viewModelFactory)

      val toolboxState by toolboxVm.uiState.collectAsState()
      val fcState by featureCheckerVm.uiState.collectAsState()

      val live by runtime.live.collectAsState()
      val pending by runtime.pending.collectAsState()
      val connected by runtime.serviceConnected.collectAsState()
      val restore by runtime.restoreProgress.collectAsState()
      val frozen by runtime.frozen.collectAsState()
      val diag by runtime.diagnosticsStatus.collectAsState()
      val auto by runtime.autoRestore.collectAsState()

      val wifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
      ) { grantedMap ->
        if (grantedMap.values.any { it }) {
          featureCheckerVm.refreshChecks()
        }
      }

      // Only prompt for Wi-Fi SSID location permission after welcome popup is dismissed
      // and when the Companion Connectivity utility is active.
      LaunchedEffect(toolboxState.showWelcomePopup, toolboxState.selectedDestination) {
        if (!toolboxState.showWelcomePopup &&
          toolboxState.selectedDestination == ToolboxDestination.COMPANION_CONNECTIVITY
        ) {
          val hasFine = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
          ) == PackageManager.PERMISSION_GRANTED
          if (!hasFine) {
            wifiPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          }
        }
      }

      GooglebookFeatureCheckerTheme(themeMode = fcState.themeMode) {
        GooglebookToolboxApp(
          toolboxState = toolboxState,
          onSelectDestination = toolboxVm::selectDestination,
          onDismissWelcomePopup = toolboxVm::dismissWelcomePopup,
          onOpenWelcomePopup = toolboxVm::openWelcomePopup,
          featureCheckerState = fcState,
          onSelectFeature = featureCheckerVm::selectFeature,
          onToggleCheckMark = featureCheckerVm::toggleDependencyCheckMark,
          onFocusDependency = featureCheckerVm::setFocusedDependencyIndex,
          onMoveFocusNext = featureCheckerVm::moveFocusNext,
          onMoveFocusPrevious = featureCheckerVm::moveFocusPrevious,
          onToggleFocusedDependency = featureCheckerVm::toggleFocusedDependency,
          onExecuteAction = { item ->
            val msg = actionNavigator.executeDependencyAction(item)
            featureCheckerVm.setStatusBannerMessage(msg)
          },
          onRefreshChecks = featureCheckerVm::refreshChecks,
          onSetThemeMode = featureCheckerVm::setThemeMode,
          onCycleThemeMode = featureCheckerVm::cycleThemeMode,
          onSendFeedbackReport = {
            val report = featureCheckerVm.buildDiagnosticFeedbackReport()
            val msg = actionNavigator.sendFeedbackReportToGoogle(report)
            featureCheckerVm.setStatusBannerMessage(msg)
          },
          onOpenReferenceThread = {
            val msg = actionNavigator.openReferenceSupportThread()
            featureCheckerVm.setStatusBannerMessage(msg)
          },
          appRestorerState = MainUiState(
            permissions = permissionState,
            serviceConnected = connected,
            live = live,
            pending = pending,
            restore = restore,
            frozen = frozen,
            diagnosticsStatus = diag,
            autoRestore = auto
          ),
          onGrantRestorerPermission = ::grantRestorerPermission,
          onOpenRestorerAppInfo = { safeStart(permissions.appDetailsIntent()) },
          onCaptureWorkspace = { runtime.manualCapture(deepScan = true) },
          onOpenRestorePrompt = { BootPromptLauncher.launchPrompt(this, manualReview = true) },
          onDismissPendingRestore = { runtime.dismissPending() },
          onExportDiagnostics = { runtime.exportDiagnostics() },
          onAutoRestoreChange = { runtime.setAutoRestore(it) }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val hasInitialTool = try {
      intent.hasExtra(EXTRA_INITIAL_TOOL)
    } catch (_: RuntimeException) {
      false
    }
    if (hasInitialTool) {
      toolboxViewModelRef?.selectDestination(resolveDestinationFromIntent(intent))
    }
  }

  override fun onResume() {
    super.onResume()
    refreshPermissions()
    runtime.refreshPending()
    if (permissionState.isGranted(RequiredPermission.ACCESSIBILITY)) {
      WorkspaceMonitorForegroundService.start(this)
    }
  }

  private fun refreshPermissions() {
    permissionState = permissions.state()
  }

  private fun grantRestorerPermission(p: RequiredPermission) {
    if (p == RequiredPermission.NOTIFICATIONS) {
      notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
      return
    }
    val settingsIntent = permissions.settingsIntent(p) ?: return
    if (!safeStart(settingsIntent)) {
      safeStart(permissions.fallbackIntent(p))
    }
  }

  private fun safeStart(intent: Intent): Boolean =
    try {
      startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: SecurityException) {
      false
    }

  companion object {
    const val EXTRA_INITIAL_TOOL = "initial_tool"
    const val TOOL_DESKTOP_RESTORER = "DESKTOP_RESTORER"
    const val TOOL_COMPANION_CONNECTIVITY = "COMPANION_CONNECTIVITY"

    fun resolveDestinationFromIntent(intent: Intent?): ToolboxDestination {
      val toolExtra = try {
        intent?.getStringExtra(EXTRA_INITIAL_TOOL)
      } catch (_: RuntimeException) {
        null
      }
      return when (toolExtra) {
        TOOL_DESKTOP_RESTORER -> ToolboxDestination.DESKTOP_RESTORER
        else -> ToolboxDestination.COMPANION_CONNECTIVITY
      }
    }
  }
}
