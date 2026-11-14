package com.droid.googlebooktoolbox.model

enum class ToolboxDestination(
  val navTitle: String,
  val description: String,
  val toolName: String
) {
  COMPANION_CONNECTIVITY(
    navTitle = "Check your companion phone connection status",
    description = "Checks Better Together cross-device requirements, fixes missing dependencies on your Googlebook, and shows what to verify on your phone.",
    toolName = "Googlebook Feature Checker"
  ),
  DESKTOP_RESTORER(
    navTitle = "Enable desktop restorer on reboot",
    description = "Automatically captures and restores open desktop windows, Virtual Desks, multi-display layouts, and Chrome tabs after reboot.",
    toolName = "Googlebook App Restorer"
  ),
  LID_CLOSE_GUARD(
    navTitle = "Allow Antigravity and Terminal tasks to continue when lid close",
    description = "Default OFF. Holds a partial wake lock only while Antigravity or Linux Terminal has an active task so closing the lid won't abort it, then releases so the device can suspend.",
    toolName = "Lid-Close Task Guard"
  )
}

data class ToolboxUiState(
  val selectedDestination: ToolboxDestination = ToolboxDestination.COMPANION_CONNECTIVITY,
  val showWelcomePopup: Boolean = false
)
