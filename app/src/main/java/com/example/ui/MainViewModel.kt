package com.example.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.SettingsRepository
import com.example.data.SlotUpdateResult
import com.example.model.AssistiveAction
import com.example.model.AssistiveSettings
import com.example.model.OverlayTheme
import com.example.service.AssistiveAccessibilityService
import com.example.service.FloatingButtonService
import com.example.util.PermissionHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
  val settings: AssistiveSettings = AssistiveSettings(),
  val canDrawOverlays: Boolean = false,
  val isAccessibilityEnabled: Boolean = false,
  val hasNotificationPermission: Boolean = true,
  val isServiceRunning: Boolean = false,
  val previewIdle: Boolean = false,
)

sealed interface MainUiEvent {
  data class ShowSnackbar(@StringRes val messageRes: Int) : MainUiEvent
  data object OpenOverlayPermission : MainUiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SettingsRepository(application)

  private val permissionRefreshTrigger = MutableStateFlow(0)
  private val previewIdleState = MutableStateFlow(false)

  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<MainUiEvent>(extraBufferCapacity = 4)
  val events: SharedFlow<MainUiEvent> = _events.asSharedFlow()

  init {
    viewModelScope.launch {
      combine(
          repository.settingsFlow,
          FloatingButtonService.isRunning,
          AssistiveAccessibilityService.isConnected,
          permissionRefreshTrigger,
          previewIdleState,
        ) { settings, serviceRunning, accConnected, _, previewIdle ->
          val context = getApplication<Application>()
          val canDraw = PermissionHelper.canDrawOverlays(context)
          val accEnabled = accConnected || PermissionHelper.isAccessibilityEnabled(context)
          val hasNotif = PermissionHelper.hasNotificationPermission(context)

          MainUiState(
            settings = settings,
            canDrawOverlays = canDraw,
            isAccessibilityEnabled = accEnabled,
            hasNotificationPermission = hasNotif,
            isServiceRunning = serviceRunning,
            previewIdle = previewIdle,
          )
        }
        .collect { state ->
          _uiState.value = state
          // Keep foreground service synchronized with persisted user preference and permission.
          val app = getApplication<Application>()
          if (state.settings.isOverlayEnabled && state.canDrawOverlays && !state.isServiceRunning) {
            FloatingButtonService.start(app)
          } else if (!state.canDrawOverlays && state.isServiceRunning) {
            FloatingButtonService.stop(app)
          }
        }
    }
  }

  fun refreshPermissions() {
    permissionRefreshTrigger.update { it + 1 }
  }

  fun toggleMasterOverlay(enable: Boolean) {
    val app = getApplication<Application>()
    if (enable) {
      if (!PermissionHelper.canDrawOverlays(app)) {
        _events.tryEmit(MainUiEvent.ShowSnackbar(R.string.master_toggle_needs_overlay))
        _events.tryEmit(MainUiEvent.OpenOverlayPermission)
        return
      }
      viewModelScope.launch {
        repository.setOverlayEnabled(true)
        FloatingButtonService.start(app)
      }
    } else {
      viewModelScope.launch {
        repository.setOverlayEnabled(false)
        FloatingButtonService.stop(app)
      }
    }
  }

  fun setPreviewIdle(idle: Boolean) {
    previewIdleState.value = idle
  }

  fun updateButtonSize(sizeDp: Float) {
    viewModelScope.launch { repository.setButtonSizeDp(sizeDp) }
  }

  fun updateIdleOpacity(opacity: Float) {
    viewModelScope.launch { repository.setIdleOpacity(opacity) }
  }

  fun updateOverlayTheme(theme: OverlayTheme) {
    viewModelScope.launch { repository.setOverlayTheme(theme) }
  }

  fun toggleAction(action: AssistiveAction) {
    viewModelScope.launch {
      when (repository.toggleAction(action)) {
        SlotUpdateResult.Updated -> Unit
        SlotUpdateResult.MinLimitReached ->
          _events.emit(MainUiEvent.ShowSnackbar(R.string.slot_limit_min))
        SlotUpdateResult.MaxLimitReached ->
          _events.emit(MainUiEvent.ShowSnackbar(R.string.slot_limit_max))
      }
    }
  }

  fun moveAction(action: AssistiveAction, direction: Int) {
    viewModelScope.launch { repository.moveAction(action, direction) }
  }

  fun resetDefaultActions() {
    viewModelScope.launch { repository.resetDefaultActions() }
  }
}
