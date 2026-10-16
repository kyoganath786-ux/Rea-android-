package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tools.ToolExecutionResult
import com.example.voice.VoiceState

@Composable
fun VoiceDialog(
  voiceState: VoiceState,
  transcription: String,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onStopListening: () -> Unit,
  onStartListening: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (voiceState == VoiceState.LISTENING) 1.25f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        .testTag("voice_assistant_dialog")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "NICK Voice Assistant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("voice_close_button")) {
            Icon(Icons.Default.Close, contentDescription = "Close Voice Assistant")
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animated Mic Sphere
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(96.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(
              when (voiceState) {
                VoiceState.LISTENING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                VoiceState.SPEAKING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                VoiceState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            )
        ) {
          FilledIconButton(
            onClick = {
              if (voiceState == VoiceState.LISTENING) onStopListening() else onStartListening()
            },
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = when (voiceState) {
                VoiceState.LISTENING -> MaterialTheme.colorScheme.primary
                VoiceState.SPEAKING -> MaterialTheme.colorScheme.secondary
                VoiceState.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
              }
            ),
            modifier = Modifier
              .size(64.dp)
              .testTag("voice_toggle_mic_button")
          ) {
            Icon(
              imageVector = when (voiceState) {
                VoiceState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                VoiceState.LISTENING -> Icons.Default.Mic
                else -> Icons.Default.Mic
              },
              contentDescription = "Microphone State",
              modifier = Modifier.size(32.dp),
              tint = Color.Black
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // State indicator label
        Text(
          text = when (voiceState) {
            VoiceState.LISTENING -> "Listening to your voice..."
            VoiceState.PROCESSING -> "Analyzing speech request..."
            VoiceState.SPEAKING -> "NICK is responding..."
            VoiceState.ERROR -> errorMessage ?: "Voice recognition error"
            VoiceState.IDLE -> "Tap microphone to speak"
          },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = if (voiceState == VoiceState.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        // Live transcription output
        if (transcription.isNotBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "\"$transcription\"",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
            Button(
              onClick = onStopListening,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("voice_stop_button")
            ) {
              Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Stop")
            }
          }
        }
      }
    }
  }
}

@Composable
fun ToolConfirmationDialog(
  confirmation: ToolExecutionResult.RequiresConfirmation,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Default.Warning,
        contentDescription = "Security Warning",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Security Confirmation Required",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column {
        Text(
          text = "Tool: ${confirmation.toolName}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = confirmation.warningMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Arguments: ${confirmation.request.arguments}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("confirm_tool_button")
      ) {
        Text("Authorize & Run", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("deny_tool_button")
      ) {
        Text("Deny")
      }
    },
    modifier = Modifier.testTag("tool_confirmation_dialog")
  )
}
