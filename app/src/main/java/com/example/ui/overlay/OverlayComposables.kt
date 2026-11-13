package com.example.ui.overlay

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AssistiveAction
import com.example.model.OverlayTheme
import com.example.ui.theme.OpenTouchDarkColorScheme
import com.example.ui.theme.OpenTouchLightColorScheme
import com.example.ui.theme.OpenTouchTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

@Immutable
data class OverlayPalette(
  val orbOuter: Color,
  val orbInner: Color,
  val orbRing: Color,
  val orbAccentDot: Color,
  val menuSurface: Color,
  val menuBorder: Color,
  val tileSurface: Color,
  val tileIconTint: Color,
  val tileLabel: Color,
  val centerHubBg: Color,
  val centerHubIcon: Color,
)

@Composable
fun resolveOverlayPalette(theme: OverlayTheme): OverlayPalette {
  val context = LocalContext.current
  val systemDark = isSystemInDarkTheme()

  val scheme =
    when (theme) {
      OverlayTheme.LIGHT -> OpenTouchLightColorScheme
      OverlayTheme.DARK -> OpenTouchDarkColorScheme
      OverlayTheme.DYNAMIC -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
          if (systemDark) OpenTouchDarkColorScheme else OpenTouchLightColorScheme
        }
      }
    }

  val isDarkSurface =
    when (theme) {
      OverlayTheme.LIGHT -> false
      OverlayTheme.DARK -> true
      OverlayTheme.DYNAMIC -> systemDark
    }

  return if (isDarkSurface) {
    OverlayPalette(
      orbOuter = Color(0xFF0F172A).copy(alpha = 0.92f),
      orbInner = scheme.primary.copy(alpha = 0.24f),
      orbRing = scheme.primary,
      orbAccentDot = scheme.primary,
      menuSurface = Color(0xFF0F172A).copy(alpha = 0.95f),
      menuBorder = scheme.primary.copy(alpha = 0.32f),
      tileSurface = Color(0xFF1E293B).copy(alpha = 0.92f),
      tileIconTint = scheme.primary,
      tileLabel = Color(0xFFF1F5F9),
      centerHubBg = scheme.primaryContainer,
      centerHubIcon = scheme.onPrimaryContainer,
    )
  } else {
    OverlayPalette(
      orbOuter = Color(0xFFFFFFFF).copy(alpha = 0.94f),
      orbInner = scheme.primary.copy(alpha = 0.14f),
      orbRing = scheme.primary,
      orbAccentDot = scheme.primary,
      menuSurface = Color(0xFFFAFBFD).copy(alpha = 0.97f),
      menuBorder = scheme.primary.copy(alpha = 0.24f),
      tileSurface = scheme.surfaceVariant.copy(alpha = 0.72f),
      tileIconTint = scheme.primary,
      tileLabel = Color(0xFF0F172A),
      centerHubBg = scheme.primaryContainer,
      centerHubIcon = scheme.onPrimaryContainer,
    )
  }
}

/**
 * Circular floating orb button rendered both inside WindowManager and in the live settings preview.
 */
@Composable
fun FloatingOrbButton(
  sizeDp: Dp,
  idleOpacity: Float,
  isIdle: Boolean,
  isPressed: Boolean,
  overlayTheme: OverlayTheme,
  modifier: Modifier = Modifier,
) {
  val palette = resolveOverlayPalette(overlayTheme)
  val animatedAlpha by
    animateFloatAsState(
      targetValue = if (isIdle && !isPressed) idleOpacity else 1f,
      animationSpec = tween(durationMillis = 380),
      label = "orb_idle_alpha",
    )
  val animatedScale by
    animateFloatAsState(
      targetValue = if (isPressed) 1.08f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "orb_press_scale",
    )
  val orbDescription = stringResource(R.string.overlay_cd_floating_button)

  Box(
    modifier =
      modifier
        .size(sizeDp)
        .scale(animatedScale)
        .alpha(animatedAlpha)
        .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
        .clip(CircleShape)
        .background(palette.orbOuter)
        .border(width = 1.5.dp, color = palette.orbRing.copy(alpha = 0.55f), shape = CircleShape)
        .semantics { contentDescription = orbDescription },
    contentAlignment = Alignment.Center,
  ) {
    // Four subtle directional micro-dots around the inner ring
    val dotOffset = sizeDp * 0.34f
    val dotSize = (sizeDp * 0.08f).coerceAtLeast(3.dp)
    Box(
      modifier =
        Modifier.offset(y = -dotOffset)
          .size(dotSize)
          .clip(CircleShape)
          .background(palette.orbAccentDot.copy(alpha = 0.75f))
    )
    Box(
      modifier =
        Modifier.offset(y = dotOffset)
          .size(dotSize)
          .clip(CircleShape)
          .background(palette.orbAccentDot.copy(alpha = 0.75f))
    )
    Box(
      modifier =
        Modifier.offset(x = -dotOffset)
          .size(dotSize)
          .clip(CircleShape)
          .background(palette.orbAccentDot.copy(alpha = 0.75f))
    )
    Box(
      modifier =
        Modifier.offset(x = dotOffset)
          .size(dotSize)
          .clip(CircleShape)
          .background(palette.orbAccentDot.copy(alpha = 0.75f))
    )

    // Translucent inner ring & glowing core
    Box(
      modifier =
        Modifier.size(sizeDp * 0.56f)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(palette.orbInner.copy(alpha = 0.55f), palette.orbInner)
            )
          )
          .border(width = 1.5.dp, color = palette.orbRing, shape = CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier.size(sizeDp * 0.22f)
            .clip(CircleShape)
            .background(palette.orbRing)
      )
    }
  }
}

/**
 * Full-screen overlay menu with a dimmed scrim. Tapping outside the card closes the menu.
 * Displays 6–8 customizable quick actions in a clean symmetrical radial-grid layout.
 */
@Composable
fun QuickActionMenuOverlay(
  actions: List<AssistiveAction>,
  overlayTheme: OverlayTheme,
  isAccessibilityConnected: Boolean,
  onActionSelected: (AssistiveAction) -> Unit,
  onOpenAccessibilitySettings: () -> Unit,
  onOpenAppSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  OpenTouchTheme(
    darkTheme =
      when (overlayTheme) {
        OverlayTheme.DARK -> true
        OverlayTheme.LIGHT -> false
        OverlayTheme.DYNAMIC -> isSystemInDarkTheme()
      },
    dynamicColor = overlayTheme == OverlayTheme.DYNAMIC,
  ) {
    val palette = resolveOverlayPalette(overlayTheme)
    val haptic = LocalHapticFeedback.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color.Black.copy(alpha = 0.42f))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismiss,
          )
          .testTag("quick_action_scrim"),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedVisibility(
        visible = visible,
        enter =
          fadeIn(tween(160)) +
            scaleIn(
              initialScale = 0.86f,
              animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
            ),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f),
      ) {
        Surface(
          modifier =
            Modifier.padding(horizontal = 24.dp)
              .widthIn(max = 340.dp)
              .fillMaxWidth()
              // Absorb clicks inside the menu card so they don't trigger scrim dismiss
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
              )
              .testTag("quick_action_menu_card"),
          shape = RoundedCornerShape(28.dp),
          color = palette.menuSurface,
          border = BorderStroke(1.dp, palette.menuBorder),
          tonalElevation = 12.dp,
          shadowElevation = 20.dp,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            // Compact Header
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier =
                    Modifier.size(10.dp).clip(CircleShape).background(palette.orbRing)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleMedium,
                  color = palette.tileLabel,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onOpenAppSettings,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.overlay_cd_menu_settings),
                    tint = palette.tileLabel.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                  )
                }
                IconButton(
                  onClick = onDismiss,
                  modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.overlay_cd_menu_close),
                    tint = palette.tileLabel.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }

            if (!isAccessibilityConnected) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                onClick = onOpenAccessibilitySettings,
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.overlay_accessibility_banner),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.tileLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radial-Grid Hybrid Action Layout:
            // When 8 actions are present, arrange them in a 3x3 ring around a central Close orb.
            // For 6 or 7 actions, render a balanced 3-column grid.
            if (actions.size == 8) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ActionGridTile(actions[0], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[0])
                  }
                  ActionGridTile(actions[1], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[1])
                  }
                  ActionGridTile(actions[2], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[2])
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ActionGridTile(actions[3], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[3])
                  }
                  CenterCloseHubTile(palette, Modifier.weight(1f), onDismiss)
                  ActionGridTile(actions[4], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[4])
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ActionGridTile(actions[5], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[5])
                  }
                  ActionGridTile(actions[6], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[6])
                  }
                  ActionGridTile(actions[7], palette, Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onActionSelected(actions[7])
                  }
                }
              }
            } else {
              val rows = actions.chunked(3)
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    rowItems.forEach { action ->
                      ActionGridTile(action, palette, Modifier.weight(1f)) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActionSelected(action)
                      }
                    }
                    repeat(3 - rowItems.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActionGridTile(
  action: AssistiveAction,
  palette: OverlayPalette,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val label = stringResource(action.labelRes)
  val interactionSource = remember { MutableInteractionSource() }
  Column(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .height(82.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(palette.tileSurface)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true),
          role = Role.Button,
          onClick = onClick,
        )
        .padding(horizontal = 6.dp, vertical = 10.dp)
        .testTag("overlay_action_${action.id}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = action.icon,
      contentDescription = label,
      tint = palette.tileIconTint,
      modifier = Modifier.size(26.dp),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      color = palette.tileLabel,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun CenterCloseHubTile(
  palette: OverlayPalette,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
) {
  val closeDesc = stringResource(R.string.overlay_cd_menu_close)
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .height(82.dp),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.size(56.dp)
          .clip(CircleShape)
          .background(palette.centerHubBg)
          .border(1.5.dp, palette.orbRing.copy(alpha = 0.5f), CircleShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            role = Role.Button,
            onClick = onDismiss,
          )
          .semantics { contentDescription = closeDesc },
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Close,
        contentDescription = closeDesc,
        tint = palette.centerHubIcon,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}
