package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.AssistiveAction
import com.example.model.AssistiveSettings
import com.example.model.OverlayTheme
import com.example.ui.overlay.FloatingOrbButton
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainerDark
import com.example.ui.theme.AmberWarningContainerLight
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainerDark
import com.example.ui.theme.EmeraldSuccessContainerLight
import com.example.util.PermissionHelper
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      viewModel.refreshPermissions()
    }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshPermissions()
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is MainUiEvent.ShowSnackbar -> {
          snackbarHostState.showSnackbar(context.getString(event.messageRes))
        }
        MainUiEvent.OpenOverlayPermission -> {
          PermissionHelper.openOverlaySettings(context)
        }
      }
    }
  }

  if (showPrivacyDialog) {
    PrivacyGuideDialog(onDismiss = { showPrivacyDialog = false })
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(34.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = stringResource(R.string.hero_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { showPrivacyDialog = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("privacy_info_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Shield,
              contentDescription = stringResource(R.string.privacy_dialog_title),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        item(key = "hero_master_card") {
          MasterControlHeroCard(
            uiState = uiState,
            onToggleMaster = viewModel::toggleMasterOverlay,
            onTogglePreviewIdle = { viewModel.setPreviewIdle(!uiState.previewIdle) },
          )
        }

        item(key = "permissions_section") {
          PermissionsSection(
            canDrawOverlays = uiState.canDrawOverlays,
            isAccessibilityEnabled = uiState.isAccessibilityEnabled,
            hasNotificationPermission = uiState.hasNotificationPermission,
            onRequestOverlay = { PermissionHelper.openOverlaySettings(context) },
            onRequestAccessibility = { PermissionHelper.openAccessibilitySettings(context) },
            onRequestNotifications = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                PermissionHelper.openNotificationSettings(context)
              }
            },
          )
        }

        item(key = "appearance_section") {
          AppearanceSettingsCard(
            settings = uiState.settings,
            onSizeChange = viewModel::updateButtonSize,
            onOpacityChange = viewModel::updateIdleOpacity,
            onThemeSelected = viewModel::updateOverlayTheme,
          )
        }

        item(key = "actions_customizer") {
          ActionSlotCustomizerCard(
            selectedActions = uiState.settings.selectedActions,
            onToggleAction = viewModel::toggleAction,
            onMoveAction = viewModel::moveAction,
            onResetDefaults = viewModel::resetDefaultActions,
          )
        }

        item(key = "privacy_footer") {
          PrivacyFooterBanner(onOpenGuide = { showPrivacyDialog = true })
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun MasterControlHeroCard(
  uiState: MainUiState,
  onToggleMaster: (Boolean) -> Unit,
  onTogglePreviewIdle: () -> Unit,
) {
  val isActive = uiState.isServiceRunning || uiState.settings.isOverlayEnabled
  val containerColor by
    animateColorAsState(
      targetValue =
        if (isActive && uiState.canDrawOverlays) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
          MaterialTheme.colorScheme.surface
        },
      animationSpec = tween(250),
      label = "hero_container_color",
    )

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isActive && uiState.canDrawOverlays) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
          } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
          },
      ),
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Live interactive orb preview canvas
      Box(
        modifier =
          Modifier.size(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
              Brush.linearGradient(
                colors =
                  listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  )
              )
            )
            .border(
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onTogglePreviewIdle)
            .testTag("preview_orb_box"),
        contentAlignment = Alignment.Center,
      ) {
        FloatingOrbButton(
          sizeDp = uiState.settings.buttonSizeDp.dp,
          idleOpacity = uiState.settings.idleOpacity,
          isIdle = uiState.previewIdle,
          isPressed = false,
          overlayTheme = uiState.settings.overlayTheme,
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.master_toggle_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val subtitle =
          when {
            !uiState.canDrawOverlays -> stringResource(R.string.master_toggle_needs_overlay)
            isActive -> stringResource(R.string.master_toggle_active)
            else -> stringResource(R.string.master_toggle_inactive)
          }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            if (uiState.previewIdle) {
              "Preview: Idle (${(uiState.settings.idleOpacity * 100).roundToInt()}%) • Tap orb"
            } else {
              "Preview: Active (100%) • Tap orb"
            },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Switch(
        checked = isActive && uiState.canDrawOverlays,
        onCheckedChange = onToggleMaster,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("master_toggle_switch"),
      )
    }
  }
}

@Composable
private fun PermissionsSection(
  canDrawOverlays: Boolean,
  isAccessibilityEnabled: Boolean,
  hasNotificationPermission: Boolean,
  onRequestOverlay: () -> Unit,
  onRequestAccessibility: () -> Unit,
  onRequestNotifications: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SectionHeader(title = stringResource(R.string.section_permissions))

    PermissionStatusCard(
      icon = Icons.Rounded.Layers,
      title = stringResource(R.string.perm_overlay_title),
      description = stringResource(R.string.perm_overlay_desc),
      isGranted = canDrawOverlays,
      grantedLabel = stringResource(R.string.status_granted),
      actionLabel =
        if (canDrawOverlays) {
          stringResource(R.string.action_manage)
        } else {
          stringResource(R.string.action_grant)
        },
      testTag = "grant_overlay_button",
      onClick = onRequestOverlay,
    )

    PermissionStatusCard(
      icon = Icons.Rounded.AccessibilityNew,
      title = stringResource(R.string.perm_accessibility_title),
      description = stringResource(R.string.perm_accessibility_desc),
      isGranted = isAccessibilityEnabled,
      grantedLabel = stringResource(R.string.status_active),
      actionLabel =
        if (isAccessibilityEnabled) {
          stringResource(R.string.action_manage)
        } else {
          stringResource(R.string.action_enable)
        },
      testTag = "enable_accessibility_button",
      onClick = onRequestAccessibility,
    )

    AnimatedVisibility(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
      PermissionStatusCard(
        icon = Icons.Rounded.NotificationsNone,
        title = stringResource(R.string.perm_notifications_title),
        description = stringResource(R.string.perm_notifications_desc),
        isGranted = hasNotificationPermission,
        grantedLabel = stringResource(R.string.status_granted),
        actionLabel = stringResource(R.string.action_grant),
        testTag = "grant_notification_button",
        onClick = onRequestNotifications,
      )
    }
  }
}

@Composable
private fun PermissionStatusCard(
  icon: ImageVector,
  title: String,
  description: String,
  isGranted: Boolean,
  grantedLabel: String,
  actionLabel: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val isDark = androidx.compose.foundation.isSystemInDarkTheme()
  val badgeBg =
    if (isGranted) {
      if (isDark) EmeraldSuccessContainerDark else EmeraldSuccessContainerLight
    } else {
      if (isDark) AmberWarningContainerDark else AmberWarningContainerLight
    }
  val badgeFg = if (isGranted) EmeraldSuccess else AmberWarning

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              }
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint =
            if (isGranted) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            shape = CircleShape,
            color = badgeBg,
          ) {
            Text(
              text = if (isGranted) grantedLabel else stringResource(R.string.status_required),
              style = MaterialTheme.typography.labelSmall,
              color = badgeFg,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      if (isGranted) {
        OutlinedButton(
          onClick = onClick,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
        ) {
          Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
        }
      } else {
        Button(
          onClick = onClick,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.minimumInteractiveComponentSize().testTag(testTag),
        ) {
          Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
private fun AppearanceSettingsCard(
  settings: AssistiveSettings,
  onSizeChange: (Float) -> Unit,
  onOpacityChange: (Float) -> Unit,
  onThemeSelected: (OverlayTheme) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SectionHeader(title = stringResource(R.string.section_appearance))

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Button Size Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.setting_button_size),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "${settings.buttonSizeDp.roundToInt()} dp",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = settings.buttonSizeDp,
            onValueChange = onSizeChange,
            valueRange =
              AssistiveSettings.MIN_BUTTON_SIZE_DP..AssistiveSettings.MAX_BUTTON_SIZE_DP,
            modifier = Modifier.testTag("button_size_slider"),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Idle Opacity Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.setting_idle_opacity),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "${(settings.idleOpacity * 100).roundToInt()}%",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = settings.idleOpacity,
            onValueChange = onOpacityChange,
            valueRange = AssistiveSettings.MIN_IDLE_OPACITY..AssistiveSettings.MAX_IDLE_OPACITY,
            modifier = Modifier.testTag("idle_opacity_slider"),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Overlay Theme Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.setting_theme),
            style = MaterialTheme.typography.titleSmall,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OverlayTheme.entries.forEach { theme ->
              val selected = settings.overlayTheme == theme
              FilterChip(
                selected = selected,
                onClick = { onThemeSelected(theme) },
                label = {
                  Text(
                    text = stringResource(theme.labelRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                leadingIcon =
                  if (selected) {
                    {
                      Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  } else {
                    null
                  },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ),
                modifier =
                  Modifier.weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("theme_chip_${theme.id}"),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionSlotCustomizerCard(
  selectedActions: List<AssistiveAction>,
  onToggleAction: (AssistiveAction) -> Unit,
  onMoveAction: (AssistiveAction, Int) -> Unit,
  onResetDefaults: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SectionHeader(title = stringResource(R.string.section_actions))
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "${selectedActions.size} / ${AssistiveAction.MAX_SLOTS}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      TextButton(
        onClick = onResetDefaults,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("reset_actions_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.action_reset_defaults))
      }
    }

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
      tonalElevation = 1.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
          text = stringResource(R.string.actions_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Toggleable pool of all actions
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          AssistiveAction.entries.forEach { action ->
            val isSelected = selectedActions.contains(action)
            FilterChip(
              selected = isSelected,
              onClick = { onToggleAction(action) },
              label = { Text(text = stringResource(action.labelRes)) },
              leadingIcon = {
                Icon(
                  imageVector = action.icon,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              modifier = Modifier.minimumInteractiveComponentSize().testTag("action_chip_${action.id}"),
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        // Active menu slot ordering list
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          selectedActions.forEachIndexed { index, action ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                  .padding(horizontal = 12.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(24.dp),
              )
              Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(action.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = { onMoveAction(action, -1) },
                enabled = index > 0,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowUpward,
                  contentDescription = "Move up",
                  modifier = Modifier.size(18.dp),
                )
              }
              IconButton(
                onClick = { onMoveAction(action, 1) },
                enabled = index < selectedActions.lastIndex,
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowDownward,
                  contentDescription = "Move down",
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacyFooterBanner(onOpenGuide: () -> Unit) {
  Surface(
    onClick = onOpenGuide,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    modifier = Modifier.fillMaxWidth().testTag("privacy_footer_card"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.VerifiedUser,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.privacy_badge_no_internet),
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = stringResource(R.string.privacy_badge_no_tracking),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.padding(horizontal = 4.dp),
  )
}
