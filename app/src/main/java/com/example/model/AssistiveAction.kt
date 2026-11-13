package com.example.model

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

/**
 * Supported quick actions in the floating Assistive Touch menu.
 *
 * Actions either delegate to [android.accessibilityservice.AccessibilityService.performGlobalAction]
 * or adjust device media volume directly via [android.media.AudioManager].
 */
enum class AssistiveAction(
  val id: String,
  @StringRes val labelRes: Int,
  val icon: ImageVector,
  val requiresAccessibility: Boolean,
  val minApi: Int = Build.VERSION_CODES.O,
  val closesMenuOnTap: Boolean = true,
) {
  HOME(
    id = "home",
    labelRes = R.string.act_home,
    icon = Icons.Rounded.Home,
    requiresAccessibility = true,
  ),
  BACK(
    id = "back",
    labelRes = R.string.act_back,
    icon = Icons.AutoMirrored.Rounded.ArrowBack,
    requiresAccessibility = true,
  ),
  RECENTS(
    id = "recents",
    labelRes = R.string.act_recents,
    icon = Icons.Rounded.Apps,
    requiresAccessibility = true,
  ),
  NOTIFICATIONS(
    id = "notifications",
    labelRes = R.string.act_notifications,
    icon = Icons.Rounded.Notifications,
    requiresAccessibility = true,
  ),
  QUICK_SETTINGS(
    id = "quick_settings",
    labelRes = R.string.act_quick_settings,
    icon = Icons.Rounded.Tune,
    requiresAccessibility = true,
  ),
  LOCK_SCREEN(
    id = "lock_screen",
    labelRes = R.string.act_lock_screen,
    icon = Icons.Rounded.Lock,
    requiresAccessibility = true,
    minApi = Build.VERSION_CODES.P,
  ),
  SCREENSHOT(
    id = "screenshot",
    labelRes = R.string.act_screenshot,
    icon = Icons.Rounded.Screenshot,
    requiresAccessibility = true,
    minApi = Build.VERSION_CODES.P,
  ),
  VOLUME_UP(
    id = "volume_up",
    labelRes = R.string.act_volume_up,
    icon = Icons.AutoMirrored.Rounded.VolumeUp,
    requiresAccessibility = false,
    closesMenuOnTap = false,
  ),
  VOLUME_DOWN(
    id = "volume_down",
    labelRes = R.string.act_volume_down,
    icon = Icons.AutoMirrored.Rounded.VolumeDown,
    requiresAccessibility = false,
    closesMenuOnTap = false,
  ),
  POWER_MENU(
    id = "power_menu",
    labelRes = R.string.act_power_menu,
    icon = Icons.Rounded.PowerSettingsNew,
    requiresAccessibility = true,
  ),
  MUTE_TOGGLE(
    id = "mute_toggle",
    labelRes = R.string.act_mute,
    icon = Icons.AutoMirrored.Rounded.VolumeOff,
    requiresAccessibility = false,
    closesMenuOnTap = false,
  );

  companion object {
    const val MIN_SLOTS = 6
    const val MAX_SLOTS = 8

    val DEFAULT_ACTIONS: List<AssistiveAction> =
      listOf(
        HOME,
        BACK,
        RECENTS,
        NOTIFICATIONS,
        QUICK_SETTINGS,
        LOCK_SCREEN,
        SCREENSHOT,
        VOLUME_UP,
      )

    fun fromId(id: String): AssistiveAction? = entries.firstOrNull { it.id == id }
  }
}
