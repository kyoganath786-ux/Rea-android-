package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.DeviceHardwareStatus
import com.example.ai.ModelSpec
import com.example.data.local.ConversationEntity
import com.example.data.local.ProjectEntity
import com.example.data.preferences.UserPreferences
import com.example.termux.TermuxStatus

@Composable
fun DashboardScreen(
  preferences: UserPreferences,
  activeModel: ModelSpec?,
  isModelLoaded: Boolean,
  hardwareStatus: DeviceHardwareStatus,
  termuxStatus: TermuxStatus,
  reaStatusDescription: String,
  projects: List<ProjectEntity>,
  conversations: List<ConversationEntity>,
  onNewChat: () -> Unit,
  onOpenVoice: () -> Unit,
  onNavigateTo: (String) -> Unit,
  onSelectConversation: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .testTag("dashboard_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Brand Banner
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
          .testTag("dashboard_header_card")
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(12.dp)
                  .clip(CircleShape)
                  .background(if (isModelLoaded) Color(0xFF10B981) else Color(0xFFF59E0B))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "NICK AI",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = preferences.aiMode.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "On-Device Local AI • REA Software Analysis • Termux Bridge",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Quick Action Bar
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Button(
              onClick = onNewChat,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("dashboard_new_chat_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Chat", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = onOpenVoice,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("dashboard_voice_assistant_button")
            ) {
              Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Voice", color = Color.Black, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // System Status Matrix
    item {
      Text(
        text = "Subsystem Health & Status",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Local Model Card
        StatusIndicatorCard(
          title = "Local AI Inference Engine",
          subtitle = if (isModelLoaded) "${activeModel?.name ?: "TinyLlama 1.1B"} (Loaded & Offline Ready)" else "No Local Model Loaded",
          details = "RAM: ${hardwareStatus.availableRamMb}MB free / ${hardwareStatus.totalRamMb}MB • Disk: ${hardwareStatus.freeStorageMb}MB free",
          statusColor = if (isModelLoaded) Color(0xFF10B981) else Color(0xFFF59E0B),
          statusText = if (isModelLoaded) "ONLINE (OFFLINE SAFE)" else "NOT LOADED",
          icon = Icons.Default.Memory,
          onClick = { onNavigateTo("settings") },
          testTag = "status_card_local_ai"
        )

        // REA Reverse Engineer Card
        StatusIndicatorCard(
          title = "REA (Reverse Engineer Anything)",
          subtitle = reaStatusDescription,
          details = "Engine: illumi-ai/REA • Mode: ${if (termuxStatus.isBridgeConfigured) "Local Loopback Bridge" else "Diagnostics & Local AST"}",
          statusColor = if (termuxStatus.isBridgeConfigured) Color(0xFF10B981) else Color(0xFF38BDF8),
          statusText = if (termuxStatus.isBridgeConfigured) "BRIDGE CONNECTED" else "READY FOR PROBE",
          icon = Icons.Default.Security,
          onClick = { onNavigateTo("termux") },
          testTag = "status_card_rea"
        )

        // Termux Environment Card
        StatusIndicatorCard(
          title = "Termux Integration",
          subtitle = if (termuxStatus.isTermuxInstalled) "Termux App Detected (${termuxStatus.termuxVersionName ?: "Installed"})" else "Termux Not Detected on Device",
          details = "RUN_COMMAND Perm: ${if (termuxStatus.isRunCommandPermissionGranted) "GRANTED" else "MANUAL/NOT GRANTED"} • Node: ${termuxStatus.detectedNodeVersion ?: "Run probe"}",
          statusColor = if (termuxStatus.isTermuxInstalled) Color(0xFF10B981) else Color(0xFFF59E0B),
          statusText = if (termuxStatus.isTermuxInstalled) "INSTALLED" else "NEEDS SETUP",
          icon = Icons.Default.Terminal,
          onClick = { onNavigateTo("termux") },
          testTag = "status_card_termux"
        )

        // Cloud AI Card
        StatusIndicatorCard(
          title = "Cloud AI Provider (Optional)",
          subtitle = "Provider: ${preferences.cloudProvider} (${preferences.aiMode.name})",
          details = "Consent: ${if (preferences.cloudConsentGranted) "GRANTED" else "NOT AUTHORIZED"} • Offline Isolation Enforced",
          statusColor = if (preferences.cloudConsentGranted) Color(0xFF8B5CF6) else Color(0xFF64748B),
          statusText = if (preferences.cloudConsentGranted) "AUTHORIZED" else "DISABLED/ISOLATED",
          icon = Icons.Default.Cloud,
          onClick = { onNavigateTo("settings") },
          testTag = "status_card_cloud_ai"
        )
      }
    }

    // Recent Projects Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Analyzed Projects",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedButton(
          onClick = { onNavigateTo("analysis") },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("dashboard_view_all_projects_button")
        ) {
          Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Inspect", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    if (projects.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "No projects analyzed yet",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { onNavigateTo("analysis") },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Analyze Sample or Import Archive", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    } else {
      items(projects.take(3)) { project ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateTo("analysis") }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = project.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = project.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = project.type.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    // Recent Conversations Section
    item {
      Text(
        text = "Recent Sessions",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    items(conversations.take(3)) { conv ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onSelectConversation(conv.id)
            onNavigateTo("chat")
          }
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = conv.title,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Mode: ${conv.mode} • Model: ${conv.modelId}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun StatusIndicatorCard(
  title: String,
  subtitle: String,
  details: String,
  statusColor: Color,
  statusText: String,
  icon: ImageVector,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(statusColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            color = statusColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = details,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }
    }
  }
}
