package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.example.model.AssistiveAction
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ActionResult {
  data object Success : ActionResult
  data object AccessibilityDisabled : ActionResult
  data object RequiresApi28 : ActionResult
  data object Failed : ActionResult
}

/**
 * Privacy-first AccessibilityService used strictly to execute user-triggered global navigation
 * actions (Home, Back, Recents, Notifications, Quick Settings, Lock Screen, Screenshot, Power
 * Menu). Window content retrieval is disabled in accessibility_service_config.xml.
 */
class AssistiveAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceRef = WeakReference(this)
    _isConnected.value = true
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Intentionally a no-op: OpenTouch never inspects UI events or screen content.
  }

  override fun onInterrupt() {
    // No ongoing feedback streams to interrupt.
  }

  override fun onUnbind(intent: android.content.Intent?): Boolean {
    serviceRef = null
    _isConnected.value = false
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    serviceRef = null
    _isConnected.value = false
    super.onDestroy()
  }

  fun performAssistiveGlobalAction(action: AssistiveAction): ActionResult {
    return when (action) {
      AssistiveAction.HOME -> performSafe(GLOBAL_ACTION_HOME)
      AssistiveAction.BACK -> performSafe(GLOBAL_ACTION_BACK)
      AssistiveAction.RECENTS -> performSafe(GLOBAL_ACTION_RECENTS)
      AssistiveAction.NOTIFICATIONS -> performSafe(GLOBAL_ACTION_NOTIFICATIONS)
      AssistiveAction.QUICK_SETTINGS -> performSafe(GLOBAL_ACTION_QUICK_SETTINGS)
      AssistiveAction.POWER_MENU -> performSafe(GLOBAL_ACTION_POWER_DIALOG)
      AssistiveAction.LOCK_SCREEN -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          performSafe(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
          ActionResult.RequiresApi28
        }
      }
      AssistiveAction.SCREENSHOT -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          performSafe(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
          ActionResult.RequiresApi28
        }
      }
      AssistiveAction.VOLUME_UP,
      AssistiveAction.VOLUME_DOWN,
      AssistiveAction.MUTE_TOGGLE -> ActionResult.Failed
    }
  }

  private fun performSafe(globalAction: Int): ActionResult {
    return try {
      if (performGlobalAction(globalAction)) ActionResult.Success else ActionResult.Failed
    } catch (_: SecurityException) {
      ActionResult.Failed
    }
  }

  companion object {
    @Volatile private var serviceRef: WeakReference<AssistiveAccessibilityService>? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Checks both the live bound reference and Android's secure settings string so UI state
     * stays accurate immediately after returning from Accessibility Settings.
     */
    fun isServiceEnabled(context: Context): Boolean {
      if (serviceRef?.get() != null && _isConnected.value) return true

      val expectedComponent =
        ComponentName(context, AssistiveAccessibilityService::class.java).flattenToString()
      val enabledServices =
        Settings.Secure.getString(
          context.contentResolver,
          Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

      val splitter = TextUtils.SimpleStringSplitter(':')
      splitter.setString(enabledServices)
      while (splitter.hasNext()) {
        val componentName = splitter.next()
        if (componentName.equals(expectedComponent, ignoreCase = true)) {
          return true
        }
      }
      return false
    }

    fun executeAction(context: Context, action: AssistiveAction): ActionResult {
      return when (action) {
        AssistiveAction.VOLUME_UP -> adjustVolume(context, AudioManager.ADJUST_RAISE)
        AssistiveAction.VOLUME_DOWN -> adjustVolume(context, AudioManager.ADJUST_LOWER)
        AssistiveAction.MUTE_TOGGLE -> adjustVolume(context, AudioManager.ADJUST_TOGGLE_MUTE)
        else -> {
          val service = serviceRef?.get()
          if (service == null) {
            ActionResult.AccessibilityDisabled
          } else {
            service.performAssistiveGlobalAction(action)
          }
        }
      }
    }

    private fun adjustVolume(context: Context, direction: Int): ActionResult {
      val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        ?: return ActionResult.Failed
      return try {
        audioManager.adjustStreamVolume(
          AudioManager.STREAM_MUSIC,
          direction,
          AudioManager.FLAG_SHOW_UI,
        )
        ActionResult.Success
      } catch (_: SecurityException) {
        ActionResult.Failed
      }
    }
  }
}
