package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class OverlayTheme(
  val id: String,
  @StringRes val labelRes: Int,
) {
  LIGHT("light", R.string.theme_light),
  DARK("dark", R.string.theme_dark),
  DYNAMIC("dynamic", R.string.theme_dynamic);

  companion object {
    fun fromId(id: String?): OverlayTheme = entries.firstOrNull { it.id == id } ?: DYNAMIC
  }
}

data class AssistiveSettings(
  val isOverlayEnabled: Boolean = false,
  val buttonSizeDp: Float = DEFAULT_BUTTON_SIZE_DP,
  val idleOpacity: Float = DEFAULT_IDLE_OPACITY,
  val overlayTheme: OverlayTheme = OverlayTheme.DYNAMIC,
  val selectedActions: List<AssistiveAction> = AssistiveAction.DEFAULT_ACTIONS,
  val snappedToRight: Boolean = true,
  val yFraction: Float = 0.42f,
) {
  companion object {
    const val MIN_BUTTON_SIZE_DP = 44f
    const val MAX_BUTTON_SIZE_DP = 76f
    const val DEFAULT_BUTTON_SIZE_DP = 56f

    const val MIN_IDLE_OPACITY = 0.15f
    const val MAX_IDLE_OPACITY = 0.90f
    const val DEFAULT_IDLE_OPACITY = 0.40f
  }
}
