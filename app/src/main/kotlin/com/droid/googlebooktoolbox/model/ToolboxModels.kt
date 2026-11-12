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
  )
}

data class ToolboxUiState(
  val selectedDestination: ToolboxDestination = ToolboxDestination.COMPANION_CONNECTIVITY,
  val showWelcomePopup: Boolean = false
)
