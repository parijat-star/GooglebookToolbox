# Contributing to Googlebook Toolbox

Thank you for contributing to **Googlebook Toolbox**! Please follow the guidelines below when opening issues or submitting pull requests.

## Prerequisites

- **JDK 17+** (JDK 21 supported)
- **Android SDK** (`compileSdk = 36`, `minSdk = 33`, `targetSdk = 36`)

## Building, Testing & Fuzzing Locally

1. **Run all unit tests, Robolectric Compose UI tests, SDK integration tests, security audits, and deterministic fuzzers**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. **Run only the security & fuzzing suites**:
   ```bash
   ./gradlew testDebugUnitTest \
     --tests "com.droid.googlebooktoolbox.ToolboxSecurityAndFuzzTest" \
     --tests "com.droid.googlebooktoolbox.featurechecker.FuzzAndSecurityTest" \
     --tests "com.droid.googlebooktoolbox.apprestorer.WorkspaceFuzzAndPropertyTest"
   ```

3. **Run Android Lint static analysis**:
   ```bash
   ./gradlew lintDebug
   ```

4. **Assemble Debug & R8-minified Release APKs**:
   ```bash
   ./gradlew assembleDebug assembleRelease
   ```

## Adding a New Tool to Googlebook Toolbox

Googlebook Toolbox is built to grow with new utilities that supercharge Googlebook usage:

1. Create a self-contained subpackage under `com.droid.googlebooktoolbox.<toolname>/` with pure-Kotlin domain/state logic and an adaptive Compose screen (`embeddedInToolbox: Boolean = true`).
2. Register a new entry in `ToolboxDestination` (`model/ToolboxModels.kt`) with a concise `navTitle` for the left pane and a clear `description` for its cursor hover pop-up.
3. Wire the destination in `GooglebookToolboxApp.kt` (`ToolboxLeftPane` + `ToolboxRightPaneContent`) and add unit, Robolectric Compose UI, and fuzz tests under `app/src/test/`.

## Architecture & Security Guidelines

- **Pure-Kotlin Testable Engines**: Keep core dependency-evaluation, Wi-Fi isolation analysis, window geometry classification, Virtual Desk inspection, and restore planning logic pure Kotlin behind lightweight interfaces (`DeviceEnvironmentProvider`, `ActionNavigator`, `UiNode`, `AppCatalog`, `LaunchCapabilities`, `RestoreDriver`) so all invariants run in milliseconds on the host JVM.
- **Add Tests & Fuzzers Together**: Accompany new toolbox utilities, dependency checks, or workspace restoration logic with unit/Compose UI tests and property/fuzz invariants.
- **Preserve the 100% Offline & Zero-Root Guarantee**:
  - **Never** add `android.permission.INTERNET` to `AndroidManifest.xml`.
  - **Never** introduce root, ADB, hidden/non-SDK reflection, or dynamic code loading.
  - Validate all external URIs, package names, and incoming `Intent` extras through `SecurityGuard` and `TwaAndBrowserInspector.normalizeUrl`.
