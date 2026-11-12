package com.droid.googlebooktoolbox.ui

import androidx.lifecycle.ViewModel
import com.droid.googlebooktoolbox.data.ToolboxPreferences
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.model.ToolboxUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ToolboxViewModel(
  private val preferences: ToolboxPreferences,
  initialDestination: ToolboxDestination = ToolboxDestination.COMPANION_CONNECTIVITY
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    ToolboxUiState(
      selectedDestination = initialDestination,
      showWelcomePopup = !preferences.hasSeenWelcomePopup
    )
  )
  val uiState: StateFlow<ToolboxUiState> = _uiState.asStateFlow()

  fun selectDestination(destination: ToolboxDestination) {
    _uiState.update { it.copy(selectedDestination = destination) }
  }

  fun dismissWelcomePopup() {
    preferences.hasSeenWelcomePopup = true
    _uiState.update { it.copy(showWelcomePopup = false) }
  }

  fun openWelcomePopup() {
    _uiState.update { it.copy(showWelcomePopup = true) }
  }
}
