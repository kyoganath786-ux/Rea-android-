package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.preferences.AiMode
import com.example.data.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  preferences: UserPreferences,
  activeConversation: ConversationEntity?,
  conversations: List<ConversationEntity>,
  messages: List<MessageEntity>,
  isGenerating: Boolean,
  streamingText: String,
  onSendMessage: (String, String?) -> Unit,
  onCancelGeneration: () -> Unit,
  onNewChat: () -> Unit,
  onSelectConversation: (String) -> Unit,
  onDeleteConversation: (String) -> Unit,
  onUpdateAiMode: (AiMode) -> Unit,
  onOpenVoice: () -> Unit,
  onSpeakText: (String) -> Unit
) {
  val context = LocalContext.current
  var textInput by remember { mutableStateOf("") }
  var attachedSnippet by remember { mutableStateOf<String?>(null) }
  var isAttachModalOpen by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()

  // Scroll to bottom when new messages arrive or when streaming
  LaunchedEffect(messages.size, streamingText) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("chat_screen")
  ) {
    // Top Bar: Mode Selector & Active Conversation Meta
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeConversation?.title ?: "Chat Session",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1
            )
            Text(
              text = "Model: ${preferences.activeLocalModelId} • ${preferences.aiMode.name}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row {
            IconButton(
              onClick = onNewChat,
              modifier = Modifier.testTag("chat_new_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
            }
            if (activeConversation != null) {
              IconButton(
                onClick = { onDeleteConversation(activeConversation.id) },
                modifier = Modifier.testTag("chat_delete_button")
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Chat", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        // Operating Mode Selector Chips
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AiMode.values().forEach { mode ->
            val isSelected = preferences.aiMode == mode
            FilterChip(
              selected = isSelected,
              onClick = { onUpdateAiMode(mode) },
              label = {
                Text(
                  mode.name.replace("_", " "),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.testTag("chip_mode_${mode.name}")
            )
          }
        }
      }
    }

    // Attachment Pill Banner (if any attached file/snippet)
    if (attachedSnippet != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Attachment included (${attachedSnippet?.length} chars)", style = MaterialTheme.typography.labelSmall)
          }
          IconButton(onClick = { attachedSnippet = null }, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove attachment", modifier = Modifier.size(14.dp))
          }
        }
      }
    }

    // Messages List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(messages) { message ->
        ChatMessageCard(
          message = message,
          onCopy = {
            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            clip.setPrimaryClip(ClipData.newPlainText("NICK AI Message", message.content))
          },
          onSpeak = { onSpeakText(message.content) }
        )
      }

      // Live streaming preview card
      if (isGenerating && streamingText.isNotBlank()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
              .testTag("chat_streaming_card")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "NICK AI Generating...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                  )
                }

                Button(
                  onClick = onCancelGeneration,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(28.dp)
                    .testTag("chat_cancel_button")
                ) {
                  Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = streamingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = {
            // Quick code snippet helper
            attachedSnippet = "// Sample Source Code / Manifest Snippet\nclass AnalysisTarget {\n    val endpoint = \"https://api.internal\"\n}"
          },
          modifier = Modifier.testTag("chat_attach_button")
        ) {
          Icon(Icons.Default.AttachFile, contentDescription = "Attach Snippet", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        IconButton(
          onClick = onOpenVoice,
          modifier = Modifier.testTag("chat_voice_button")
        ) {
          Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.secondary)
        }

        OutlinedTextField(
          value = textInput,
          onValueChange = { textInput = it },
          placeholder = { Text("Ask NICK or enter reverse engineering request...", style = MaterialTheme.typography.bodySmall) },
          maxLines = 4,
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("chat_text_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            if (textInput.isNotBlank() || attachedSnippet != null) {
              onSendMessage(textInput, attachedSnippet)
              textInput = ""
              attachedSnippet = null
            }
          },
          enabled = !isGenerating && (textInput.isNotBlank() || attachedSnippet != null),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = CircleShape,
          modifier = Modifier
            .size(46.dp)
            .testTag("chat_send_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
fun ChatMessageCard(
  message: MessageEntity,
  onCopy: () -> Unit,
  onSpeak: () -> Unit
) {
  val isUser = message.sender == "user"
  val isSystem = message.sender == "system"

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = when {
          isUser -> MaterialTheme.colorScheme.primaryContainer
          isSystem -> MaterialTheme.colorScheme.surfaceVariant
          else -> MaterialTheme.colorScheme.surface
        }
      ),
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
      ),
      modifier = Modifier
        .fillMaxWidth(if (isUser) 0.88f else 0.94f)
        .border(
          width = 1.dp,
          color = when {
            isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            isSystem -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
          },
          shape = RoundedCornerShape(16.dp)
        )
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = when (message.sender) {
              "user" -> "You"
              "assistant" -> "NICK AI"
              else -> "System / Diagnostic"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
          )

          if (!isUser) {
            Row {
              IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak response", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Render code blocks cleanly if present
        if (message.content.contains("```")) {
          FormattedMarkdownContent(message.content)
        } else {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
fun FormattedMarkdownContent(content: String) {
  val parts = content.split("```")
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    parts.forEachIndexed { index, part ->
      if (index % 2 == 1) {
        // Code Block
        val lines = part.trim().lines()
        val language = if (lines.isNotEmpty() && lines.first().length < 15 && !lines.first().contains(" ")) lines.first() else "code"
        val codeBody = if (lines.size > 1 && language != "code") lines.drop(1).joinToString("\n") else part.trim()

        Surface(
          color = Color(0xFF090D16),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = language.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = codeBody,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = Color(0xFFE2E8F0)
            )
          }
        }
      } else {
        // Normal text
        if (part.trim().isNotBlank()) {
          Text(
            text = part.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
