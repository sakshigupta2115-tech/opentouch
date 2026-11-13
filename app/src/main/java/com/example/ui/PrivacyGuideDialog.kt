package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun PrivacyGuideDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Shield,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
      )
    },
    title = {
      Text(
        text = stringResource(R.string.privacy_dialog_title),
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrivacyGuaranteeRow(stringResource(R.string.privacy_badge_no_internet))
        PrivacyGuaranteeRow(stringResource(R.string.privacy_badge_no_tracking))
        PrivacyGuaranteeRow(stringResource(R.string.privacy_badge_no_content))
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.accessibility_service_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_privacy_dialog_button"),
      ) {
        Text(stringResource(R.string.close))
      }
    },
  )
}

@Composable
private fun PrivacyGuaranteeRow(label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
