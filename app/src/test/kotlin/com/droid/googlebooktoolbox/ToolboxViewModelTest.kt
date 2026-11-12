package com.droid.googlebooktoolbox

import com.droid.googlebooktoolbox.data.ToolboxPreferences
import com.droid.googlebooktoolbox.model.ToolboxDestination
import com.droid.googlebooktoolbox.ui.ToolboxViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolboxViewModelTest {

  private class FakeToolboxPreferences(
    override var hasSeenWelcomePopup: Boolean = false
  ) : ToolboxPreferences

  @Test
  fun firstLaunch_showsWelcomePopupAndDefaultsToCompanionConnectivity() {
    val prefs = FakeToolboxPreferences(hasSeenWelcomePopup = false)
    val vm = ToolboxViewModel(prefs)

    assertTrue(vm.uiState.value.showWelcomePopup)
    assertEquals(ToolboxDestination.COMPANION_CONNECTIVITY, vm.uiState.value.selectedDestination)
  }

  @Test
  fun dismissWelcomePopup_hidesPopupAndPersistsPreferenceForSubsequentLaunches() {
    val prefs = FakeToolboxPreferences(hasSeenWelcomePopup = false)
    val vm = ToolboxViewModel(prefs)

    vm.dismissWelcomePopup()
    assertFalse(vm.uiState.value.showWelcomePopup)
    assertTrue(prefs.hasSeenWelcomePopup)

    // Subsequent launch should NOT auto-show the welcome popup
    val secondVm = ToolboxViewModel(prefs)
    assertFalse(secondVm.uiState.value.showWelcomePopup)

    // Clicking About/Info re-opens the popup on demand
    secondVm.openWelcomePopup()
    assertTrue(secondVm.uiState.value.showWelcomePopup)
  }

  @Test
  fun selectDestination_switchesBetweenCompanionConnectivityAndDesktopRestorer() {
    val prefs = FakeToolboxPreferences(hasSeenWelcomePopup = true)
    val vm = ToolboxViewModel(prefs)

    assertEquals(ToolboxDestination.COMPANION_CONNECTIVITY, vm.uiState.value.selectedDestination)

    vm.selectDestination(ToolboxDestination.DESKTOP_RESTORER)
    assertEquals(ToolboxDestination.DESKTOP_RESTORER, vm.uiState.value.selectedDestination)

    vm.selectDestination(ToolboxDestination.COMPANION_CONNECTIVITY)
    assertEquals(ToolboxDestination.COMPANION_CONNECTIVITY, vm.uiState.value.selectedDestination)
  }

  @Test
  fun initialDestination_respectsDeepLinkOverride() {
    val prefs = FakeToolboxPreferences(hasSeenWelcomePopup = true)
    val vm = ToolboxViewModel(prefs, initialDestination = ToolboxDestination.DESKTOP_RESTORER)
    assertEquals(ToolboxDestination.DESKTOP_RESTORER, vm.uiState.value.selectedDestination)
  }
}
