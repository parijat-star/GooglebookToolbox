package com.droid.googlebooktoolbox

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droid.googlebooktoolbox.apprestorer.permissions.PermissionState
import com.droid.googlebooktoolbox.apprestorer.ui.MainUiState
import com.droid.googlebooktoolbox.data.SharedPrefsToolboxPreferences
import com.droid.googlebooktoolbox.data.ToolboxPreferences
import com.droid.googlebooktoolbox.featurechecker.data.CompanionPhoneSnapshot
import com.droid.googlebooktoolbox.featurechecker.data.DeviceEnvironmentProvider
import com.droid.googlebooktoolbox.featurechecker.data.DeviceSystemSnapshot
import com.droid.googlebooktoolbox.featurechecker.ui.FeatureCheckerViewModel
import com.droid.googlebooktoolbox.featurechecker.ui.theme.GooglebookFeatureCheckerTheme
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.ui.GooglebookToolboxApp
import com.droid.googlebooktoolbox.ui.ToolboxViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w1280dp-h800dp")
class ToolboxScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private class FakeEnvironmentProvider : DeviceEnvironmentProvider {
    override fun readLocalSystemSnapshot() = DeviceSystemSnapshot(
      deviceModel = "Googlebook Pro",
      deviceManufacturer = "Google",
      isLaptopFormFactor = true,
      buildType = "user",
      buildFingerprint = "google/googlebook/googlebook:17/BP1A/123:user/release-keys",
      androidRelease = "17",
      sdkInt = 37,
      oneUiVersion = null,
      gmsInstalled = true,
      gmsVersionName = "26.38.12",
      crossDeviceInstalled = true,
      crossDevicePackageName = "com.google.ambient.streaming",
      crossDeviceVersionName = "1.0.1400",
      wifiConnected = true,
      isPublicOrCaptiveWifi = false,
      vpnActive = false,
      networkSsidOrDescription = "Connected to Wi-Fi: HomeFiber",
      connectedWifiName = "HomeFiber",
      bluetoothSupported = true,
      bluetoothEnabled = true,
      hasGoogleAccountSignedIn = true,
      continueActivityConfigured = true
    )

    override fun probeCompanionPhone(): CompanionPhoneSnapshot? = null
  }

  private class InMemoryToolboxPrefs(override var hasSeenWelcomePopup: Boolean = false) : ToolboxPreferences

  @Test
  fun firstOpen_showsExplanationPopup_andDismissesOnGetStarted() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences(SharedPrefsToolboxPreferences.PREFS_NAME, Context.MODE_PRIVATE)
      .edit().clear().commit()
    val prefs = SharedPrefsToolboxPreferences(context)
    assertFalse(prefs.hasSeenWelcomePopup)

    val toolboxVm = ToolboxViewModel(prefs)
    val fcVm = FeatureCheckerViewModel(FakeEnvironmentProvider())

    composeTestRule.setContent {
      val tbState by toolboxVm.uiState.collectAsState()
      val fcState by fcVm.uiState.collectAsState()
      GooglebookFeatureCheckerTheme(themeMode = fcState.themeMode) {
        GooglebookToolboxApp(
          toolboxState = tbState,
          onSelectDestination = toolboxVm::selectDestination,
          onDismissWelcomePopup = toolboxVm::dismissWelcomePopup,
          onOpenWelcomePopup = toolboxVm::openWelcomePopup,
          featureCheckerState = fcState,
          onSelectFeature = fcVm::selectFeature,
          onToggleCheckMark = fcVm::toggleDependencyCheckMark,
          onFocusDependency = fcVm::setFocusedDependencyIndex,
          onMoveFocusNext = fcVm::moveFocusNext,
          onMoveFocusPrevious = fcVm::moveFocusPrevious,
          onToggleFocusedDependency = fcVm::toggleFocusedDependency,
          onExecuteAction = {},
          onRefreshChecks = fcVm::refreshChecks,
          onSetThemeMode = fcVm::setThemeMode,
          onCycleThemeMode = fcVm::cycleThemeMode,
          onSendFeedbackReport = {},
          onOpenReferenceThread = {},
          appRestorerState = MainUiState(permissions = PermissionState()),
          onGrantRestorerPermission = {},
          onOpenRestorerAppInfo = {},
          onCaptureWorkspace = {},
          onOpenRestorePrompt = {},
          onDismissPendingRestore = {},
          onExportDiagnostics = {},
          onAutoRestoreChange = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("welcome_popup_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Welcome to Googlebook Toolbox", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("collection of useful utilities", substring = true, ignoreCase = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("welcome_popup_dismiss_button").performClick()
    assertTrue(prefs.hasSeenWelcomePopup)
    composeTestRule.onNodeWithTag("welcome_popup_dialog").assertDoesNotExist()
  }

  @Test
  fun leftPaneNavigation_switchesRightPaneBetweenFeatureCheckerAndAppRestorer() {
    val prefs = InMemoryToolboxPrefs(hasSeenWelcomePopup = true)
    val toolboxVm = ToolboxViewModel(prefs)
    val fcVm = FeatureCheckerViewModel(FakeEnvironmentProvider())

    composeTestRule.setContent {
      val tbState by toolboxVm.uiState.collectAsState()
      val fcState by fcVm.uiState.collectAsState()
      GooglebookFeatureCheckerTheme(themeMode = fcState.themeMode) {
        GooglebookToolboxApp(
          toolboxState = tbState,
          onSelectDestination = toolboxVm::selectDestination,
          onDismissWelcomePopup = toolboxVm::dismissWelcomePopup,
          onOpenWelcomePopup = toolboxVm::openWelcomePopup,
          featureCheckerState = fcState,
          onSelectFeature = fcVm::selectFeature,
          onToggleCheckMark = fcVm::toggleDependencyCheckMark,
          onFocusDependency = fcVm::setFocusedDependencyIndex,
          onMoveFocusNext = fcVm::moveFocusNext,
          onMoveFocusPrevious = fcVm::moveFocusPrevious,
          onToggleFocusedDependency = fcVm::toggleFocusedDependency,
          onExecuteAction = {},
          onRefreshChecks = fcVm::refreshChecks,
          onSetThemeMode = fcVm::setThemeMode,
          onCycleThemeMode = fcVm::cycleThemeMode,
          onSendFeedbackReport = {},
          onOpenReferenceThread = {},
          appRestorerState = MainUiState(permissions = PermissionState()),
          onGrantRestorerPermission = {},
          onOpenRestorerAppInfo = {},
          onCaptureWorkspace = {},
          onOpenRestorePrompt = {},
          onDismissPendingRestore = {},
          onExportDiagnostics = {},
          onAutoRestoreChange = {}
        )
      }
    }

    // Left pane displays logo + both requested navigation options
    composeTestRule.onNodeWithTag("toolbox_left_pane").assertIsDisplayed()
    composeTestRule.onNodeWithTag("toolbox_logo_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithText("Check your companion phone connectivity status").assertIsDisplayed()
    composeTestRule.onNodeWithText("Enable desktop restorer on reboot").assertIsDisplayed()

    // Right pane initially displays Googlebook Feature Checker UI
    composeTestRule.onNodeWithTag("toolbox_right_pane").assertIsDisplayed()
    composeTestRule.onNodeWithText("Googlebook Feature Checker").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recheck_all_button").assertIsDisplayed()

    // Click "Enable desktop restorer on reboot" -> Right pane shows Googlebook App Restorer UI
    composeTestRule.onNodeWithTag("nav_item_app_restorer").performClick()
    composeTestRule.onNodeWithText("Googlebook App Restorer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Restore automatically after reboot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("capture_button").assertIsDisplayed()

    // Click "Check your companion phone connectivity status" -> Right pane returns to Feature Checker UI
    composeTestRule.onNodeWithTag("nav_item_feature_checker").performClick()
    composeTestRule.onNodeWithText("Googlebook Feature Checker").assertIsDisplayed()
    composeTestRule.assertEqualsDest(ToolboxDestination.COMPANION_CONNECTIVITY, toolboxVm.uiState.value.selectedDestination)

    // Clicking the Info button in the left pane re-opens the Toolbox explanation popup
    composeTestRule.onNodeWithTag("open_about_popup_button").performClick()
    composeTestRule.onNodeWithTag("welcome_popup_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("welcome_popup_dismiss_button").performClick()
    composeTestRule.onNodeWithTag("welcome_popup_dialog").assertDoesNotExist()
  }

  @Test
  fun resolveDestinationFromIntent_routesMonitorNotificationToDesktopRestorer() {
    val defaultDest = MainActivity.resolveDestinationFromIntent(null)
    org.junit.Assert.assertEquals(ToolboxDestination.COMPANION_CONNECTIVITY, defaultDest)

    val restorerIntent = android.content.Intent().putExtra(
      MainActivity.EXTRA_INITIAL_TOOL,
      MainActivity.TOOL_DESKTOP_RESTORER
    )
    org.junit.Assert.assertEquals(
      ToolboxDestination.DESKTOP_RESTORER,
      MainActivity.resolveDestinationFromIntent(restorerIntent)
    )
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertEqualsDest(
    expected: ToolboxDestination,
    actual: ToolboxDestination
  ) {
    org.junit.Assert.assertEquals(expected, actual)
  }
}
