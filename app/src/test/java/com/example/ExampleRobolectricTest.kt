package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.AssistiveAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `app name matches OpenTouch`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OpenTouch", appName)
  }

  @Test
  fun `manifest declares zero INTERNET permission and includes overlay permissions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo =
      context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_PERMISSIONS,
      )
    val permissions = packageInfo.requestedPermissions?.toList().orEmpty()
    assertFalse("INTERNET permission must not be present", permissions.contains(Manifest.permission.INTERNET))
    assertTrue(permissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW))
    assertTrue(permissions.contains(Manifest.permission.FOREGROUND_SERVICE))
  }

  @Test
  fun `action slot deserialization enforces 6 to 8 unique actions`() {
    val serialized = SettingsRepository.serializeActions(AssistiveAction.DEFAULT_ACTIONS)
    val parsed = SettingsRepository.deserializeActions(serialized)
    assertEquals(AssistiveAction.DEFAULT_ACTIONS, parsed)

    // Too few (< 6) falls back safely to defaults
    val tooFew = SettingsRepository.deserializeActions("home,back,recents")
    assertEquals(AssistiveAction.DEFAULT_ACTIONS, tooFew)

    // Valid 6-slot list preserves order
    val sixSlots =
      SettingsRepository.deserializeActions("home,back,recents,notifications,lock_screen,volume_up")
    assertEquals(6, sixSlots.size)
    assertEquals(AssistiveAction.HOME, sixSlots.first())
    assertEquals(AssistiveAction.VOLUME_UP, sixSlots.last())
  }
}
