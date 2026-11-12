# Security & Privacy Policy

## Supported Versions

| Version | Supported |
| :--- | :--- |
| `1.3.x` | ✅ Supported |
| `< 1.3.0` | ❌ Upgrade to `1.3.0+` |

## Core Security & Privacy Architecture

**Googlebook Toolbox** combines **Googlebook Feature Checker** and **Googlebook App Restorer** under a strict, defense-in-depth security model verified by automated unit, manifest, and deterministic fuzzing suites (`ToolboxSecurityAndFuzzTest`, `FuzzAndSecurityTest`, and `WorkspaceFuzzAndPropertyTest`):

### 1. 100% Offline Kernel Guarantee (`Zero INTERNET Permission`)
- `AndroidManifest.xml` declares **no `android.permission.INTERNET` permission**.
- Because Android enforces network socket access at the Linux kernel GID level (`inet`), no window title, Accessibility node, Wi-Fi SSID, build fingerprint, or open Chrome tab URL can ever leave the device.
- Cloud backup and device-to-device transfer extraction are disabled (`android:allowBackup="false"`, `@xml/data_extraction_rules`, and `@xml/backup_rules`).

### 2. Zero Root, Zero ADB & Zero Hidden APIs
- Operates exclusively through standard, user-grantable public Android SDK APIs (`ConnectivityManager`, `WifiManager`, `PackageManager`, `AccessibilityService`, `SYSTEM_ALERT_WINDOW`, `UsageStatsManager`, `POST_NOTIFICATIONS`).
- Contains zero native `.so` binaries, zero dynamic code loading (`DexClassLoader`), and zero non-SDK reflection.

### 3. Broadcast, Component & Intent Hardening
- **Least-Privilege Export Surface**:
  - `RestorePromptActivity` and `WorkspaceMonitorForegroundService` are strictly internal (`android:exported="false"`).
  - `WorkspaceCaptureAccessibilityService` enforces `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`.
  - `BootRestoreReceiver` accepts **only** AOSP system `<protected-broadcast>` actions (`LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) and ignores spoofed/custom actions.
- **Malformed Parcelable / Intent DoS Protection**:
  - `MainActivity` guards incoming `Intent` extra reads against `BadParcelableException` / `RuntimeException` from untrusted callers.
- **URI Scheme & Package Pinning Allowlists (Capture & Restore)**:
  - `TwaAndBrowserInspector.normalizeUrl`, `RestorePlanner.resolveLaunch`, and `TabReconciler.missingTabs` enforce a strict `http://` / `https://` allowlist at **both** capture time and restore time — blocking `javascript:`, `intent:`, `file:`, `content:`, and `data:` URI injection even if a snapshot file were tampered with.
  - `SecurityGuard.isValidAndroidPackageName` validates package identifiers against path traversal or query injection before constructing Play Store (`market://`), App Info (`package:`), or workspace restore intents.
  - `IntentFactory.toIntent` and `SystemActionLauncher` preserve explicit package pinning (`setPackage(...)`) and never degrade to unpinned implicit intents.

### 4. Accessibility Tree, WebView & Clipboard Privacy
- **Device-Protected Atomic Storage**: Workspace snapshots (`live_workspace.json` and `pending_restore.json`) are stored inside the app's private Device-Protected Storage sandbox using `AtomicFile` + `fsync`.
- **WebView DOM & Password Exclusion**:
  - `TwaAndBrowserInspector` skips `android.webkit.WebView` page DOM subtrees, inspecting only Chrome's top-level window caption, tab strip, and omnibox.
  - `DiagnosticsExporter` skips web page content and completely redacts `isPassword` accessibility nodes (both `text` and `contentDescription`, plus child subtrees).
- **Sensitive Clipboard Masking**:
  - Diagnostic reports copied to the clipboard set `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+ (`API 33+`) to mask Wi-Fi SSID and build metadata in system clipboard previews.
- **Text Sanitization**:
  - `SecurityGuard.sanitizeUntrustedText` strips control characters, null bytes, and Unicode BiDi override characters (`U+202A..U+202E`, `U+2066..U+2069`) from external strings.

## Reporting a Vulnerability

Please report suspected security vulnerabilities privately via **GitHub Security Advisories** (**Security → Report a vulnerability**) rather than opening a public issue. Include reproduction steps and the affected Googlebook / Android OS version.
