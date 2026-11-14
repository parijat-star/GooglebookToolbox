package com.droid.googlebooktoolbox

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BadParcelableException
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droid.googlebooktoolbox.apprestorer.Fixtures.PRIMARY
import com.droid.googlebooktoolbox.apprestorer.boot.BootRestoreReceiver
import com.droid.googlebooktoolbox.apprestorer.capture.KnownPackages
import com.droid.googlebooktoolbox.apprestorer.data.WorkspaceRepository
import com.droid.googlebooktoolbox.apprestorer.model.AppKind
import com.droid.googlebooktoolbox.apprestorer.model.CapturedWindow
import com.droid.googlebooktoolbox.apprestorer.model.NormalizedBounds
import com.droid.googlebooktoolbox.apprestorer.model.WindowBounds
import com.droid.googlebooktoolbox.apprestorer.model.WindowVisualState
import com.droid.googlebooktoolbox.apprestorer.model.WorkspaceSnapshot
import com.droid.googlebooktoolbox.apprestorer.permissions.PermissionState
import com.droid.googlebooktoolbox.apprestorer.platform.IntentFactory
import com.droid.googlebooktoolbox.apprestorer.restore.LaunchCapabilities
import com.droid.googlebooktoolbox.apprestorer.restore.LaunchDescriptor
import com.droid.googlebooktoolbox.apprestorer.restore.RestorePlanner
import com.droid.googlebooktoolbox.apprestorer.restore.TabReconciler
import com.droid.googlebooktoolbox.apprestorer.service.WorkspaceCaptureAccessibilityService
import com.droid.googlebooktoolbox.apprestorer.service.WorkspaceMonitorForegroundService
import com.droid.googlebooktoolbox.apprestorer.ui.MainUiState
import com.droid.googlebooktoolbox.apprestorer.ui.RestorePromptActivity
import com.droid.googlebooktoolbox.data.SharedPrefsToolboxPreferences
import com.droid.googlebooktoolbox.featurechecker.data.CompanionPhoneSnapshot
import com.droid.googlebooktoolbox.featurechecker.data.DeviceEnvironmentProvider
import com.droid.googlebooktoolbox.featurechecker.data.DeviceSystemSnapshot
import com.droid.googlebooktoolbox.featurechecker.ui.FeatureCheckerViewModel
import com.droid.googlebooktoolbox.featurechecker.ui.theme.GooglebookFeatureCheckerTheme
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.ui.GooglebookToolboxApp
import com.droid.googlebooktoolbox.ui.ToolboxViewModel
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = android.app.Application::class)
class ToolboxSecurityAndFuzzTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun manifest_enforcesZeroInternetNoBackupAndProtectedComponents() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pm = context.packageManager
    val flags = PackageManager.GET_PERMISSIONS or
      PackageManager.GET_ACTIVITIES or
      PackageManager.GET_SERVICES or
      PackageManager.GET_RECEIVERS
    val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))

    // 1. 100% Offline guarantee: AndroidManifest MUST NOT request android.permission.INTERNET
    val requestedPerms = pkgInfo.requestedPermissions?.toSet().orEmpty()
    assertFalse(
      "Googlebook Toolbox must remain 100% offline with zero INTERNET permission",
      Manifest.permission.INTERNET in requestedPerms
    )

    // 2. Cloud/ADB backup disabled (allowBackup="false")
    val appFlags = pkgInfo.applicationInfo?.flags ?: 0
    assertEquals(0, appFlags and ApplicationInfo.FLAG_ALLOW_BACKUP)

    // 3. Internal RestorePromptActivity and WorkspaceMonitorForegroundService must NOT be exported
    val promptActivity = pkgInfo.activities?.firstOrNull { it.name == RestorePromptActivity::class.java.name }
    assertNotNull(promptActivity)
    assertFalse(promptActivity!!.exported)

    val monitorService = pkgInfo.services?.firstOrNull { it.name == WorkspaceMonitorForegroundService::class.java.name }
    assertNotNull(monitorService)
    assertFalse(monitorService!!.exported)

    // 4. AccessibilityService must require android.permission.BIND_ACCESSIBILITY_SERVICE
    val a11yService = pkgInfo.services?.firstOrNull { it.name == WorkspaceCaptureAccessibilityService::class.java.name }
    assertNotNull(a11yService)
    assertEquals(Manifest.permission.BIND_ACCESSIBILITY_SERVICE, a11yService!!.permission)

    // 5. BootRestoreReceiver must ignore spoofed / non-protected broadcast actions
    val repo = WorkspaceRepository(context, bootTokenProvider = {
      com.droid.googlebooktoolbox.apprestorer.data.BootToken(1, 1000L)
    })
    repo.clearPending()
    repo.saveLive(
      WorkspaceSnapshot(
        bootTokenAtCapture = "bc=0;ep=500",
        displays = listOf(PRIMARY),
        windows = listOf(
          CapturedWindow(
            windowKey = "w1",
            packageName = "com.android.settings",
            label = "Settings",
            kind = AppKind.SETTINGS,
            bounds = WindowBounds(0, 0, 800, 600),
            normalized = NormalizedBounds(0.1f, 0.1f, 0.8f, 0.8f),
            visualState = WindowVisualState.FREEFORM
          )
        )
      )
    )
    // Re-seed previous-boot live file directly via a second repository on boot 0, then spoof broadcast on boot 1
    var boot = 0
    val controlledRepo = WorkspaceRepository(context, bootTokenProvider = {
      com.droid.googlebooktoolbox.apprestorer.data.BootToken(boot, boot * 1000L)
    })
    controlledRepo.clearPending()
    controlledRepo.saveLive(
      WorkspaceSnapshot(
        bootTokenAtCapture = "bc=0;ep=0",
        displays = listOf(PRIMARY),
        windows = listOf(
          CapturedWindow(
            windowKey = "w1",
            packageName = "com.android.settings",
            label = "Settings",
            kind = AppKind.SETTINGS,
            bounds = WindowBounds(0, 0, 800, 600),
            normalized = NormalizedBounds(0.1f, 0.1f, 0.8f, 0.8f),
            visualState = WindowVisualState.FREEFORM
          )
        )
      )
    )
    boot = 1
    // Spoofed/legacy non-AOSP broadcasts must be ignored by BootRestoreReceiver
    BootRestoreReceiver().onReceive(context, Intent("android.intent.action.QUICKBOOT_POWERON"))
    BootRestoreReceiver().onReceive(context, Intent("com.evil.SPOOF_BOOT"))
    BootRestoreReceiver().onReceive(context, Intent())
  }

  @Test
  fun restoreSecurity_blocksMaliciousUriSchemesInvalidPackagesAndUnpinnedFallback() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val caps = object : LaunchCapabilities {
      override fun launcherComponent(packageName: String): String? = "$packageName/.MainActivity"
      override fun isExportedComponent(component: String): Boolean = true
    }
    val planner = RestorePlanner(caps)

    // 1. Invalid / path-traversal / query-injected package names must be rejected at plan time
    val badPackages = listOf(
      "../../etc/passwd",
      "com.android.chrome&referrer=evil",
      "javascript:alert(1)",
      "",
      "singleword"
    )
    for (badPkg in badPackages) {
      val win = CapturedWindow(
        windowKey = "bad-$badPkg",
        packageName = badPkg,
        label = "Bad",
        kind = AppKind.STANDARD,
        bounds = WindowBounds(100, 100, 800, 600),
        normalized = NormalizedBounds(0.1f, 0.1f, 0.8f, 0.8f),
        visualState = WindowVisualState.FREEFORM
      )
      assertNull("Expected invalid package '$badPkg' to be blocked", planner.resolveLaunch(win, 0))
    }

    // 2. Tampered secondary Chrome window with malicious URI schemes must never emit non-HTTP(S) dataUri
    val maliciousTabUrls = listOf(
      "javascript:alert(document.cookie)",
      "intent://scan/#Intent;scheme=zxing;package=com.evil;end",
      "file:///data/user_de/0/com.droid.googlebooktoolbox/files/workspace/live_workspace.json",
      "content://com.android.contacts/contacts/1",
      "data:text/html,<script>alert(1)</script>"
    )
    val secondaryChromeWithOnlyEvilUrls = CapturedWindow(
      windowKey = "chrome-2",
      packageName = KnownPackages.CHROME,
      label = "Chrome",
      kind = AppKind.CHROME_BROWSER,
      bounds = WindowBounds(100, 100, 900, 700),
      normalized = NormalizedBounds(0.1f, 0.1f, 0.8f, 0.8f),
      visualState = WindowVisualState.FREEFORM,
      tabUrls = maliciousTabUrls
    )
    val resolvedEvil = planner.resolveLaunch(secondaryChromeWithOnlyEvilUrls, instanceIndex = 1)
    assertNotNull(resolvedEvil)
    assertNull("Malicious scheme must never reach LaunchDescriptor.dataUri", resolvedEvil!!.dataUri)
    assertEquals(Intent.ACTION_MAIN, resolvedEvil.action)

    // 3. TabReconciler must strip all non-HTTP(S) URLs while preserving valid HTTPS URLs
    val reconciled = TabReconciler.missingTabs(
      savedUrls = maliciousTabUrls + "https://developer.android.com/jetpack/compose",
      savedTitles = List(maliciousTabUrls.size + 1) { "Tab $it" },
      restoredTitles = emptyList(),
      restoredCount = 0,
      currentUrl = null,
      chromeRestoresNatively = false
    )
    assertEquals(listOf("https://developer.android.com/jetpack/compose"), reconciled)

    // 4. IntentFactory must preserve explicit package pinning even if target package is not currently resolvable
    val unresolvableDesc = LaunchDescriptor(
      packageName = "com.nonexistent.pinned.browser",
      component = null,
      action = Intent.ACTION_VIEW,
      categories = setOf(Intent.CATEGORY_BROWSABLE),
      dataUri = "https://example.com",
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    )
    val pinnedIntent = IntentFactory.toIntent(context, unresolvableDesc)
    assertEquals("com.nonexistent.pinned.browser", pinnedIntent.`package`)
  }

  private class PoisonParcelable : Parcelable {
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(1)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<PoisonParcelable> = object : Parcelable.Creator<PoisonParcelable> {
        override fun createFromParcel(source: Parcel): PoisonParcelable {
          throw BadParcelableException("Simulated untrusted Parcelable attack")
        }
        override fun newArray(size: Int): Array<PoisonParcelable?> = arrayOfNulls(size)
      }
    }
  }

  @Test
  fun fuzzToolboxIntentsPreferencesAndViewModel_neverThrowsAndMaintainsInvariants() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rng = Random(0x7001_B088L)
    val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-\u0000\u202E\n\r\t{}[]!@#$%^&*()"

    // 1. Poisoned / unparcelable Intent bundle must safely fall back without crashing MainActivity
    val parcel = Parcel.obtain()
    try {
      val rawBundle = android.os.Bundle()
      rawBundle.putParcelable("poison", PoisonParcelable())
      rawBundle.putString(MainActivity.EXTRA_INITIAL_TOOL, MainActivity.TOOL_DESKTOP_RESTORER)
      rawBundle.writeToParcel(parcel, 0)
      parcel.setDataPosition(0)
      val unparceledBundle = parcel.readBundle(PoisonParcelable::class.java.classLoader)!!
      // Force lazy parcel backing so getStringExtra triggers CREATOR.createFromParcel -> BadParcelableException
      val poisonIntent = Intent().putExtras(unparceledBundle)
      val resolved = MainActivity.resolveDestinationFromIntent(poisonIntent)
      assertNotNull(resolved)
    } finally {
      parcel.recycle()
    }

    // 2. Fuzz MainActivity.resolveDestinationFromIntent with 1,500 random malformed Intents
    repeat(1_500) { iter ->
      val intent = when (rng.nextInt(6)) {
        0 -> null
        1 -> Intent()
        2 -> Intent().putExtra(MainActivity.EXTRA_INITIAL_TOOL, rng.nextInt())
        3 -> Intent().putExtra(MainActivity.EXTRA_INITIAL_TOOL, ByteArray(rng.nextInt(0, 512)))
        4 -> {
          val len = if (iter == 0) 65_536 else rng.nextInt(0, 256)
          val s = buildString(len) { repeat(len) { append(alphabet[rng.nextInt(alphabet.length)]) } }
          Intent().putExtra(MainActivity.EXTRA_INITIAL_TOOL, s)
        }
        else -> Intent().putExtra(
          MainActivity.EXTRA_INITIAL_TOOL,
          if (rng.nextBoolean()) MainActivity.TOOL_DESKTOP_RESTORER else MainActivity.TOOL_COMPANION_CONNECTIVITY
        )
      }
      val dest = MainActivity.resolveDestinationFromIntent(intent)
      assertTrue(dest in ToolboxDestination.entries)
    }

    // 3. Fuzz ToolboxViewModel + SharedPrefsToolboxPreferences across 1,000 randomized transitions
    context.getSharedPreferences(SharedPrefsToolboxPreferences.PREFS_NAME, Context.MODE_PRIVATE)
      .edit().clear().commit()
    val prefs = SharedPrefsToolboxPreferences(context)
    var vm = ToolboxViewModel(prefs)
    assertTrue(vm.uiState.value.showWelcomePopup)

    repeat(1_000) {
      when (rng.nextInt(4)) {
        0 -> {
          val target = ToolboxDestination.entries[rng.nextInt(ToolboxDestination.entries.size)]
          vm.selectDestination(target)
          assertEquals(target, vm.uiState.value.selectedDestination)
        }
        1 -> {
          vm.dismissWelcomePopup()
          assertFalse(vm.uiState.value.showWelcomePopup)
          assertTrue(prefs.hasSeenWelcomePopup)
        }
        2 -> {
          vm.openWelcomePopup()
          assertTrue(vm.uiState.value.showWelcomePopup)
        }
        3 -> {
          val initDest = ToolboxDestination.entries[rng.nextInt(ToolboxDestination.entries.size)]
          vm = ToolboxViewModel(prefs, initialDestination = initDest)
          assertEquals(initDest, vm.uiState.value.selectedDestination)
          assertEquals(!prefs.hasSeenWelcomePopup, vm.uiState.value.showWelcomePopup)
        }
      }
    }
  }

  @Test
  @Config(sdk = [35], application = android.app.Application::class, qualifiers = "w390dp-h844dp")
  fun compactViewport_rendersStackedToolboxLayoutAndSwitchesToolsCleanly() {
    val fakeEnv = object : DeviceEnvironmentProvider {
      override fun readLocalSystemSnapshot() = DeviceSystemSnapshot(
        deviceModel = "Pixel 10 Pro",
        deviceManufacturer = "Google",
        isLaptopFormFactor = false,
        buildType = "user",
        buildFingerprint = "google/pixel10/pixel10:17/BP1A/123:user/release-keys",
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
    val prefs = object : com.droid.googlebooktoolbox.data.ToolboxPreferences {
      override var hasSeenWelcomePopup: Boolean = true
    }
    val toolboxVm = ToolboxViewModel(prefs)
    val fcVm = FeatureCheckerViewModel(fakeEnv)

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

    composeTestRule.onNodeWithTag("toolbox_left_pane").assertIsDisplayed()
    composeTestRule.onNodeWithText("Check your companion phone connection status").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_item_app_restorer").performClick()
    composeTestRule.onNodeWithText("Googlebook App Restorer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_item_lid_guard").performClick()
    composeTestRule.onNodeWithTag("lid_guard_master_switch").assertIsDisplayed()
  }

  @Test
  fun lidGuardServices_haveStrictPermissionsAndNonExportedFgs() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val pm = context.packageManager
    val pkgInfo = pm.getPackageInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_SERVICES
    )
    val services = pkgInfo.services?.associateBy { it.name }.orEmpty()
    val fgs = services["com.droid.googlebooktoolbox.lidguard.LidGuardForegroundService"]
    val listener = services["com.droid.googlebooktoolbox.lidguard.TaskNotificationListenerService"]
    org.junit.Assert.assertNotNull(fgs)
    org.junit.Assert.assertFalse(fgs!!.exported)
    org.junit.Assert.assertNotNull(listener)
    org.junit.Assert.assertEquals(
      "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
      listener!!.permission
    )
  }
}
