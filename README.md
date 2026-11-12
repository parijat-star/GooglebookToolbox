# Googlebook Toolbox

<p align="center">
  <img src="docs/icon-512.png" alt="Googlebook Toolbox Logo" width="140" />
</p>

**Googlebook Toolbox** is an all-in-one Android desktop utility suite containing essential tools to manage, diagnose, and enhance your Googlebook.

## Included Tools

1. **Check your companion phone connectivity status (`Googlebook Feature Checker`)**
   - Step-by-step live dependency tracker and one-click fixer for **Googlebook Better Together** (Continue On, Cast My Apps, Quick Access, Notification Sync).
   - Evaluates all 7 cross-device dependencies side-by-side (`This device` vs. `Companion device`): Android 17 / One UI 9.0, Google Play services (`v26.37+`), Cross-device services (`v1.0.1346+`), Private Home Wi-Fi & AP Host Isolation / VPN detection, Bluetooth, Continue Activity settings, and matching `user` / `userdbg` build types.
   - Includes a one-click **Send Feedback Report** builder for Google Feedback.

2. **Enable desktop restorer on reboot (`Googlebook App Restorer`)**
   - Zero-root, zero-ADB automatic capture and restoration of open desktop windows, Virtual Desks, multi-display layouts, snapped/maximized/minimized window states, TWAs, and Chrome tabs across reboots.
   - Guided 4-step user-grantable permission onboarding, cancelable post-reboot countdown popup, interactive desktop layout preview canvas, and local diagnostics exporter.

## First-Launch Experience & Navigation

- **First-Open Pop-up**: On first launch, a welcome modal explains what **Googlebook Toolbox** is and summarizes the included utilities (accessible anytime via the info icon in the left pane).
- **Two-Pane Desktop Layout**: Left navigation pane provides instant switching between **"Check your companion phone connectivity status"** and **"Enable desktop restorer on reboot"**, updating the right utility workspace pane in place.

## Building & Testing

```bash
./gradlew testDebugUnitTest assembleDebug \
  --project-cache-dir="$HOME/.cache/projects/$(basename "$PWD")-$(pwd -P | sha1sum | cut -c1-8)/gradle"
```

Output APK: `app/build/outputs/apk/debug/GooglebookToolbox-debug.apk`
