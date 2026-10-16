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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.rea.READiagnosticResult
import com.example.rea.REAToolDefinition
import com.example.termux.TermuxIntegration
import com.example.termux.TermuxSetupStep
import com.example.termux.TermuxStatus

@Composable
fun TermuxReaScreen(
  termuxStatus: TermuxStatus,
  doctorResult: READiagnosticResult?,
  discoveredTools: List<REAToolDefinition>,
  bridgeStatusMessage: String?,
  setupSteps: List<TermuxSetupStep>,
  onRefreshTermux: () -> Unit,
  onOpenTermux: () -> Unit,
  onDownloadTermux: () -> Unit,
  onRunDoctorJson: (String?) -> Unit,
  onTestBridge: (String, String) -> Unit,
  onDiscoverTools: () -> Unit
) {
  val context = LocalContext.current
  var pastedDoctorOutput by remember { mutableStateOf("") }
  var bridgeUrl by remember { mutableStateOf("http://127.0.0.1:8765") }
  var bridgeToken by remember { mutableStateOf("nick-local-sec") }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .testTag("termux_rea_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header & Termux Environment Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
          .testTag("termux_status_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(12.dp)
                  .clip(CircleShape)
                  .background(if (termuxStatus.isTermuxInstalled) Color(0xFF10B981) else Color(0xFFF59E0B))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Termux Environment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            IconButton(onClick = onRefreshTermux, modifier = Modifier.testTag("termux_refresh_button")) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh Termux Status")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (termuxStatus.isTermuxInstalled) {
              "Termux application is installed on device (${termuxStatus.termuxVersionName ?: "Available"})."
            } else {
              "Termux is NOT detected. Standard Android apps cannot execute Termux shell directly without user-guided commands or local loopback bridge."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (termuxStatus.isTermuxInstalled) {
              Button(
                onClick = onOpenTermux,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("termux_open_app_button")
              ) {
                Icon(Icons.Default.Launch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Termux", color = Color.Black, style = MaterialTheme.typography.labelSmall)
              }
            } else {
              Button(
                onClick = onDownloadTermux,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("termux_download_button")
              ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Termux (GitHub)", color = Color.Black, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }

    // Android/Bionic Libc Runtime Note
    item {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
          Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Android Bionic Runtime Compatibility",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Termux uses Android Bionic libc. Complex native bindings (like glibc-compiled Tree-Sitter parsers) may require 'proot-distro' or our localhost HTTP bridge to communicate with NICK AI safely.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Guided Step-by-Step Setup
    item {
      Text(
        text = "Guided Setup & Diagnostic Workflow",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    items(setupSteps) { step ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Step ${step.stepNumber}: ${step.title}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
              onClick = {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("Termux Command", step.command))
              },
              modifier = Modifier
                .size(32.dp)
                .testTag("copy_step_${step.stepNumber}")
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = "Copy Command", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(text = step.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = Color(0xFF090D16),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "$ ${step.command}",
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    }

    // REA Doctor Diagnostics Ingestion Box
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Capture REA Diagnostic Output",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Paste output from 'npx -y rea-agents@latest doctor --json' to verify REA installation and agent hooks:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = pastedDoctorOutput,
            onValueChange = { pastedDoctorOutput = it },
            placeholder = { Text("Paste JSON output here...", style = MaterialTheme.typography.bodySmall) },
            maxLines = 6,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("termux_doctor_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary
            )
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onRunDoctorJson(pastedDoctorOutput.ifBlank { null }) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("termux_verify_doctor_button")
            ) {
              Text("Verify Diagnostic JSON", color = Color.Black, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = {
                // Pre-populate mock healthy output for simulation verification
                pastedDoctorOutput = "{\"status\": \"ok\", \"node_supported\": true, \"tools\": [\"rea_inspect_tree\", \"rea_summarize_code\", \"rea_trace_deps\"]}"
                onRunDoctorJson(pastedDoctorOutput)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("termux_sample_doctor_button")
            ) {
              Text("Sample Output", style = MaterialTheme.typography.labelSmall)
            }
          }

          // Diagnostic Outcome Card
          if (doctorResult != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              color = if (doctorResult.success) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF43F5E).copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (doctorResult.success) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (doctorResult.success) Color(0xFF10B981) else Color(0xFFF43F5E),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (doctorResult.success) "REA Health Check Passed" else "REA Diagnostics Incomplete",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (doctorResult.success) Color(0xFF10B981) else Color(0xFFF43F5E)
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))
                doctorResult.recommendations.forEach { rec ->
                  Text(text = "• $rec", style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }
    }

    // Bridge Connection Manager (Localhost / Remote)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "REA Bridge & Tool Discovery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Connect to local Termux loopback bridge (http://127.0.0.1:8765) or remote host:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = bridgeUrl,
            onValueChange = { bridgeUrl = it },
            label = { Text("Bridge URL") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("termux_bridge_url_input")
          )

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = bridgeToken,
            onValueChange = { bridgeToken = it },
            label = { Text("Auth Token") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("termux_bridge_token_input")
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onTestBridge(bridgeUrl, bridgeToken) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("termux_test_bridge_button")
            ) {
              Text("Test Bridge", color = Color.Black, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = onDiscoverTools,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("termux_discover_tools_button")
            ) {
              Text("Discover Tools", style = MaterialTheme.typography.labelSmall)
            }
          }

          if (bridgeStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = bridgeStatusMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }

          if (discoveredTools.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Discovered REA Tools (${discoveredTools.size}):",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            discoveredTools.forEach { tool ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = tool.name,
                      style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = tool.source,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(text = tool.description, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }
    }
  }
}
