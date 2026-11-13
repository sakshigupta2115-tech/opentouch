package com.example.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import com.example.model.AssistiveAction
import com.example.model.AssistiveSettings
import com.example.ui.overlay.FloatingOrbButton
import com.example.ui.overlay.QuickActionMenuOverlay
import com.example.util.PermissionHelper
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware foreground service managing the WindowManager floating button overlay and the
 * expandable Jetpack Compose quick-action menu.
 *
 * Designed for minimal RAM/battery overhead: the quick-action menu view is attached only while
 * expanded, and idle opacity fades cleanly via a single suspended timer.
 */
class FloatingButtonService :
  Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val store = ViewModelStore()
  private val savedStateController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val viewModelStore: ViewModelStore
    get() = store

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateController.savedStateRegistry

  private lateinit var windowManager: WindowManager
  private lateinit var settingsRepository: SettingsRepository

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val settingsState = MutableStateFlow(AssistiveSettings())
  private val isIdleState = MutableStateFlow(false)
  private val isPressedState = MutableStateFlow(false)

  private var floatingView: ComposeView? = null
  private var floatingParams: WindowManager.LayoutParams? = null
  private var menuView: ComposeView? = null

  private var snapAnimator: ValueAnimator? = null
  private var idleTimerJob: Job? = null
  private var touchSlop: Int = 16
  private var lastKnownWidthPx: Int = 0
  private var lastKnownHeightPx: Int = 0

  override fun onCreate() {
    super.onCreate()
    savedStateController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    settingsRepository = SettingsRepository(applicationContext)
    touchSlop = ViewConfiguration.get(this).scaledTouchSlop

    createNotificationChannel()
    startForegroundCompat()
    _isRunning.value = true

    updateScreenBounds()
    observeSettings()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      serviceScope.launch {
        settingsRepository.setOverlayEnabled(false)
        stopSelf()
      }
      return START_NOT_STICKY
    }

    if (!Settings.canDrawOverlays(this)) {
      stopSelf()
      return START_NOT_STICKY
    }

    if (floatingView == null && menuView == null) {
      attachFloatingButton()
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    updateScreenBounds()
    repositionButtonToEdge(animate = false)
  }

  override fun onDestroy() {
    _isRunning.value = false
    idleTimerJob?.cancel()
    snapAnimator?.cancel()
    removeMenuOverlay()
    removeFloatingButton()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    store.clear()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun observeSettings() {
    serviceScope.launch {
      settingsRepository.settingsFlow.collect { updated ->
        val previousSize = settingsState.value.buttonSizeDp
        settingsState.value = updated
        if (floatingView != null && previousSize != updated.buttonSizeDp) {
          updateFloatingButtonLayoutSize(updated.buttonSizeDp)
          repositionButtonToEdge(animate = false)
        }
      }
    }
  }

  private fun startForegroundCompat() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun createNotificationChannel() {
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = getString(R.string.notification_channel_description)
        setShowBadge(false)
      }
    val manager = getSystemService(NotificationManager::class.java)
    manager?.createNotificationChannel(channel)
  }

  private fun buildNotification(): Notification {
    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val stopIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, FloatingButtonService::class.java).apply { action = ACTION_STOP_SERVICE },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(getString(R.string.notification_title))
      .setContentText(getString(R.string.notification_text))
      .setContentIntent(openAppIntent)
      .setOngoing(true)
      .setSilent(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .addAction(0, getString(R.string.notification_action_stop), stopIntent)
      .build()
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun attachFloatingButton() {
    if (floatingView != null || !Settings.canDrawOverlays(this)) return

    updateScreenBounds()
    val settings = settingsState.value
    val buttonPx = dpToPx(settings.buttonSizeDp)
    val windowPadPx = dpToPx(WINDOW_PADDING_DP * 2)
    val windowSizePx = buttonPx + windowPadPx
    val marginPx = dpToPx(EDGE_MARGIN_DP)

    val startX =
      if (settings.snappedToRight) {
        (lastKnownWidthPx - windowSizePx - marginPx).coerceAtLeast(marginPx)
      } else {
        marginPx
      }
    val startY =
      ((lastKnownHeightPx - windowSizePx) * settings.yFraction)
        .roundToInt()
        .coerceIn(
          dpToPx(TOP_SAFE_DP),
          (lastKnownHeightPx - windowSizePx - dpToPx(BOTTOM_SAFE_DP)).coerceAtLeast(dpToPx(TOP_SAFE_DP)),
        )

    val params =
      WindowManager.LayoutParams(
        windowSizePx,
        windowSizePx,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = startX
        y = startY
      }

    val composeView =
      ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@FloatingButtonService)
        setViewTreeViewModelStoreOwner(this@FloatingButtonService)
        setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
          val currentSettings by settingsState.collectAsState()
          val isIdle by isIdleState.collectAsState()
          val isPressed by isPressedState.collectAsState()
          androidx.compose.foundation.layout.Box(
            modifier =
              androidx.compose.ui.Modifier.fillMaxSize().padding(WINDOW_PADDING_DP.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
          ) {
            FloatingOrbButton(
              sizeDp = currentSettings.buttonSizeDp.dp,
              idleOpacity = currentSettings.idleOpacity,
              isIdle = isIdle,
              isPressed = isPressed,
              overlayTheme = currentSettings.overlayTheme,
            )
          }
        }
      }

    var initialX = 0
    var initialY = 0
    var initialTouchX = 0f
    var initialTouchY = 0f
    var isDragging = false

    composeView.setOnTouchListener { _, event ->
      val currentParams = floatingParams ?: return@setOnTouchListener false
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          snapAnimator?.cancel()
          wakeFromIdle()
          isPressedState.value = true
          initialX = currentParams.x
          initialY = currentParams.y
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          isDragging = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = event.rawX - initialTouchX
          val dy = event.rawY - initialTouchY
          if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
            isDragging = true
          }
          if (isDragging) {
            val sizePx = currentParams.width
            val minY = dpToPx(TOP_SAFE_DP)
            val maxY = (lastKnownHeightPx - sizePx - dpToPx(BOTTOM_SAFE_DP)).coerceAtLeast(minY)
            val maxX = (lastKnownWidthPx - sizePx).coerceAtLeast(0)
            currentParams.x = (initialX + dx).roundToInt().coerceIn(0, maxX)
            currentParams.y = (initialY + dy).roundToInt().coerceIn(minY, maxY)
            runCatching { windowManager.updateViewLayout(composeView, currentParams) }
          }
          true
        }
        MotionEvent.ACTION_UP -> {
          isPressedState.value = false
          if (isDragging) {
            snapToNearestEdgeAndSave()
            scheduleIdleFade()
          } else {
            openQuickActionMenu()
          }
          true
        }
        MotionEvent.ACTION_CANCEL -> {
          isPressedState.value = false
          snapToNearestEdgeAndSave()
          scheduleIdleFade()
          true
        }
        else -> false
      }
    }

    floatingParams = params
    floatingView = composeView
    runCatching { windowManager.addView(composeView, params) }
    wakeFromIdle()
    scheduleIdleFade()
  }

  private fun removeFloatingButton() {
    floatingView?.let { view ->
      runCatching { windowManager.removeViewImmediate(view) }
    }
    floatingView = null
    floatingParams = null
  }

  private fun updateFloatingButtonLayoutSize(sizeDp: Float) {
    val view = floatingView ?: return
    val params = floatingParams ?: return
    val newSizePx = dpToPx(sizeDp) + dpToPx(WINDOW_PADDING_DP * 2)
    params.width = newSizePx
    params.height = newSizePx
    runCatching { windowManager.updateViewLayout(view, params) }
  }

  private fun snapToNearestEdgeAndSave() {
    val view = floatingView ?: return
    val params = floatingParams ?: return
    updateScreenBounds()

    val sizePx = params.width
    val marginPx = dpToPx(EDGE_MARGIN_DP)
    val centerX = params.x + sizePx / 2
    val snapRight = centerX >= lastKnownWidthPx / 2
    val targetX =
      if (snapRight) {
        (lastKnownWidthPx - sizePx - marginPx).coerceAtLeast(marginPx)
      } else {
        marginPx
      }

    val yFraction =
      if (lastKnownHeightPx > sizePx) {
        params.y.toFloat() / (lastKnownHeightPx - sizePx).toFloat()
      } else {
        0.42f
      }

    serviceScope.launch {
      settingsRepository.saveButtonPosition(snappedToRight = snapRight, yFraction = yFraction)
    }

    snapAnimator?.cancel()
    snapAnimator =
      ValueAnimator.ofInt(params.x, targetX).apply {
        duration = 240L
        interpolator = OvershootInterpolator(0.55f)
        addUpdateListener { animator ->
          val currentView = floatingView ?: return@addUpdateListener
          val currentParams = floatingParams ?: return@addUpdateListener
          currentParams.x = animator.animatedValue as Int
          runCatching { windowManager.updateViewLayout(currentView, currentParams) }
        }
        start()
      }
  }

  private fun repositionButtonToEdge(animate: Boolean) {
    val view = floatingView ?: return
    val params = floatingParams ?: return
    val settings = settingsState.value
    val sizePx = params.width
    val marginPx = dpToPx(EDGE_MARGIN_DP)
    val targetX =
      if (settings.snappedToRight) {
        (lastKnownWidthPx - sizePx - marginPx).coerceAtLeast(marginPx)
      } else {
        marginPx
      }
    val minY = dpToPx(TOP_SAFE_DP)
    val maxY = (lastKnownHeightPx - sizePx - dpToPx(BOTTOM_SAFE_DP)).coerceAtLeast(minY)
    val targetY = ((lastKnownHeightPx - sizePx) * settings.yFraction).roundToInt().coerceIn(minY, maxY)

    if (!animate) {
      params.x = targetX
      params.y = targetY
      runCatching { windowManager.updateViewLayout(view, params) }
    } else {
      snapToNearestEdgeAndSave()
    }
  }

  private fun openQuickActionMenu() {
    if (menuView != null) return
    idleTimerJob?.cancel()
    floatingView?.visibility = View.GONE

    val params =
      WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
      ).apply {
        gravity = Gravity.CENTER
      }

    val overlay =
      ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@FloatingButtonService)
        setViewTreeViewModelStoreOwner(this@FloatingButtonService)
        setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        isFocusableInTouchMode = true
        requestFocus()
        setOnKeyListener { _, keyCode, event ->
          if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            closeQuickActionMenu()
            true
          } else {
            false
          }
        }
        setContent {
          val settings by settingsState.collectAsState()
          val accessibilityConnected by AssistiveAccessibilityService.isConnected.collectAsState()
          QuickActionMenuOverlay(
            actions = settings.selectedActions,
            overlayTheme = settings.overlayTheme,
            isAccessibilityConnected = accessibilityConnected,
            onActionSelected = { action -> handleQuickAction(action) },
            onOpenAccessibilitySettings = {
              closeQuickActionMenu()
              PermissionHelper.openAccessibilitySettings(this@FloatingButtonService)
            },
            onOpenAppSettings = {
              closeQuickActionMenu()
              val intent =
                Intent(this@FloatingButtonService, MainActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
              startActivity(intent)
            },
            onDismiss = { closeQuickActionMenu() },
          )
        }
      }

    menuView = overlay
    runCatching { windowManager.addView(overlay, params) }
  }

  private fun closeQuickActionMenu(restoreButtonImmediately: Boolean = true) {
    removeMenuOverlay()
    if (restoreButtonImmediately) {
      floatingView?.visibility = View.VISIBLE
      wakeFromIdle()
      scheduleIdleFade()
    }
  }

  private fun removeMenuOverlay() {
    menuView?.let { view ->
      runCatching { windowManager.removeViewImmediate(view) }
    }
    menuView = null
  }

  private fun handleQuickAction(action: AssistiveAction) {
    if (action == AssistiveAction.SCREENSHOT) {
      // Hide menu and button briefly so the screenshot captures the underlying screen cleanly.
      closeQuickActionMenu(restoreButtonImmediately = false)
      serviceScope.launch {
        delay(180L)
        val result = AssistiveAccessibilityService.executeAction(this@FloatingButtonService, action)
        reportActionFailureIfNeeded(result)
        delay(450L)
        floatingView?.visibility = View.VISIBLE
        wakeFromIdle()
        scheduleIdleFade()
      }
      return
    }

    if (action.closesMenuOnTap) {
      closeQuickActionMenu(restoreButtonImmediately = true)
    }

    val result = AssistiveAccessibilityService.executeAction(this, action)
    reportActionFailureIfNeeded(result)
  }

  private fun reportActionFailureIfNeeded(result: ActionResult) {
    when (result) {
      ActionResult.Success -> Unit
      ActionResult.AccessibilityDisabled -> {
        Toast.makeText(this, R.string.toast_enable_accessibility, Toast.LENGTH_SHORT).show()
      }
      ActionResult.RequiresApi28 -> {
        Toast.makeText(this, R.string.toast_requires_android_9, Toast.LENGTH_SHORT).show()
      }
      ActionResult.Failed -> {
        Toast.makeText(this, R.string.toast_action_unavailable, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun wakeFromIdle() {
    idleTimerJob?.cancel()
    isIdleState.value = false
  }

  private fun scheduleIdleFade() {
    idleTimerJob?.cancel()
    idleTimerJob =
      serviceScope.launch {
        delay(IDLE_DELAY_MS)
        isIdleState.value = true
      }
  }

  private fun updateScreenBounds() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val metrics = windowManager.currentWindowMetrics
      val insets =
        metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.displayCutout())
      lastKnownWidthPx = metrics.bounds.width() - insets.left - insets.right
      lastKnownHeightPx = metrics.bounds.height()
    } else {
      val displayMetrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getMetrics(displayMetrics)
      lastKnownWidthPx = displayMetrics.widthPixels
      lastKnownHeightPx = displayMetrics.heightPixels
    }
  }

  private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).roundToInt()

  companion object {
    private const val CHANNEL_ID = "opentouch_overlay_channel"
    private const val NOTIFICATION_ID = 1001
    private const val IDLE_DELAY_MS = 3000L
    private const val EDGE_MARGIN_DP = 8f
    private const val WINDOW_PADDING_DP = 6f
    private const val TOP_SAFE_DP = 32f
    private const val BOTTOM_SAFE_DP = 48f

    const val ACTION_STOP_SERVICE = "com.example.action.STOP_OVERLAY_SERVICE"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start(context: Context) {
      val intent = Intent(context, FloatingButtonService::class.java)
      context.startForegroundService(intent)
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, FloatingButtonService::class.java))
    }
  }
}
