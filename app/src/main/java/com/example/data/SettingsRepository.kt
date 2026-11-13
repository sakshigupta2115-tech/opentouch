package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AssistiveAction
import com.example.model.AssistiveSettings
import com.example.model.OverlayTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "opentouch_settings")

sealed interface SlotUpdateResult {
  data object Updated : SlotUpdateResult
  data object MinLimitReached : SlotUpdateResult
  data object MaxLimitReached : SlotUpdateResult
}

/**
 * Local-only DataStore repository persisting OpenTouch overlay appearance, position, and
 * customizable 6–8 action slots. No network or telemetry is ever used.
 */
class SettingsRepository(private val context: Context) {

  private object Keys {
    val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
    val BUTTON_SIZE_DP = floatPreferencesKey("button_size_dp")
    val IDLE_OPACITY = floatPreferencesKey("idle_opacity")
    val OVERLAY_THEME = stringPreferencesKey("overlay_theme")
    val SELECTED_ACTIONS = stringPreferencesKey("selected_actions")
    val SNAPPED_TO_RIGHT = booleanPreferencesKey("snapped_to_right")
    val Y_FRACTION = floatPreferencesKey("y_fraction")
  }

  val settingsFlow: Flow<AssistiveSettings> =
    context.settingsDataStore.data.map { prefs ->
      val rawSize = prefs[Keys.BUTTON_SIZE_DP] ?: AssistiveSettings.DEFAULT_BUTTON_SIZE_DP
      val rawOpacity = prefs[Keys.IDLE_OPACITY] ?: AssistiveSettings.DEFAULT_IDLE_OPACITY
      AssistiveSettings(
        isOverlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
        buttonSizeDp =
          rawSize.coerceIn(
            AssistiveSettings.MIN_BUTTON_SIZE_DP,
            AssistiveSettings.MAX_BUTTON_SIZE_DP,
          ),
        idleOpacity =
          rawOpacity.coerceIn(
            AssistiveSettings.MIN_IDLE_OPACITY,
            AssistiveSettings.MAX_IDLE_OPACITY,
          ),
        overlayTheme = OverlayTheme.fromId(prefs[Keys.OVERLAY_THEME]),
        selectedActions = deserializeActions(prefs[Keys.SELECTED_ACTIONS]),
        snappedToRight = prefs[Keys.SNAPPED_TO_RIGHT] ?: true,
        yFraction = (prefs[Keys.Y_FRACTION] ?: 0.42f).coerceIn(0.05f, 0.90f),
      )
    }

  suspend fun setOverlayEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
  }

  suspend fun setButtonSizeDp(sizeDp: Float) {
    val clamped =
      sizeDp.coerceIn(
        AssistiveSettings.MIN_BUTTON_SIZE_DP,
        AssistiveSettings.MAX_BUTTON_SIZE_DP,
      )
    context.settingsDataStore.edit { it[Keys.BUTTON_SIZE_DP] = clamped }
  }

  suspend fun setIdleOpacity(opacity: Float) {
    val clamped =
      opacity.coerceIn(
        AssistiveSettings.MIN_IDLE_OPACITY,
        AssistiveSettings.MAX_IDLE_OPACITY,
      )
    context.settingsDataStore.edit { it[Keys.IDLE_OPACITY] = clamped }
  }

  suspend fun setOverlayTheme(theme: OverlayTheme) {
    context.settingsDataStore.edit { it[Keys.OVERLAY_THEME] = theme.id }
  }

  suspend fun saveButtonPosition(snappedToRight: Boolean, yFraction: Float) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.SNAPPED_TO_RIGHT] = snappedToRight
      prefs[Keys.Y_FRACTION] = yFraction.coerceIn(0.05f, 0.90f)
    }
  }

  suspend fun toggleAction(action: AssistiveAction): SlotUpdateResult {
    var result: SlotUpdateResult = SlotUpdateResult.Updated
    context.settingsDataStore.edit { prefs ->
      val current = deserializeActions(prefs[Keys.SELECTED_ACTIONS]).toMutableList()
      if (current.contains(action)) {
        if (current.size <= AssistiveAction.MIN_SLOTS) {
          result = SlotUpdateResult.MinLimitReached
          return@edit
        }
        current.remove(action)
      } else {
        if (current.size >= AssistiveAction.MAX_SLOTS) {
          result = SlotUpdateResult.MaxLimitReached
          return@edit
        }
        current.add(action)
      }
      prefs[Keys.SELECTED_ACTIONS] = serializeActions(current)
    }
    return result
  }

  suspend fun moveAction(action: AssistiveAction, direction: Int) {
    if (direction == 0) return
    context.settingsDataStore.edit { prefs ->
      val current = deserializeActions(prefs[Keys.SELECTED_ACTIONS]).toMutableList()
      val index = current.indexOf(action)
      if (index == -1) return@edit
      val targetIndex = (index + direction).coerceIn(0, current.lastIndex)
      if (targetIndex == index) return@edit
      current.removeAt(index)
      current.add(targetIndex, action)
      prefs[Keys.SELECTED_ACTIONS] = serializeActions(current)
    }
  }

  suspend fun resetDefaultActions() {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.SELECTED_ACTIONS] = serializeActions(AssistiveAction.DEFAULT_ACTIONS)
    }
  }

  companion object {
    internal fun serializeActions(actions: List<AssistiveAction>): String =
      actions.joinToString(",") { it.id }

    internal fun deserializeActions(raw: String?): List<AssistiveAction> {
      if (raw.isNullOrBlank()) return AssistiveAction.DEFAULT_ACTIONS
      val parsed =
        raw.split(",")
          .mapNotNull { AssistiveAction.fromId(it.trim()) }
          .distinct()
      return if (parsed.size in AssistiveAction.MIN_SLOTS..AssistiveAction.MAX_SLOTS) {
        parsed
      } else {
        AssistiveAction.DEFAULT_ACTIONS
      }
    }
  }
}
