package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.AssistiveAccessibilityService

object PermissionHelper {

  fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

  fun isAccessibilityEnabled(context: Context): Boolean =
    AssistiveAccessibilityService.isServiceEnabled(context)

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun openOverlaySettings(context: Context) {
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
      ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
  }

  fun openAccessibilitySettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }

  fun openNotificationSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }
}
