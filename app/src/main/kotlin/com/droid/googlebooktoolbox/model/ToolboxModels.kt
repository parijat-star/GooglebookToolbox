package com.droid.googlebooktoolbox.model

enum class ToolboxDestination(
  val navTitle: String,
  val toolName: String
) {
  COMPANION_CONNECTIVITY(
    navTitle = "Check your companion phone connectivity status",
    toolName = "Googlebook Feature Checker"
  ),
  DESKTOP_RESTORER(
    navTitle = "Enable desktop restorer on reboot",
    toolName = "Googlebook App Restorer"
  )
}

data class ToolboxUiState(
  val selectedDestination: ToolboxDestination = ToolboxDestination.COMPANION_CONNECTIVITY,
  val showWelcomePopup: Boolean = false
)
