package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ai.ModelDownloadState
import com.example.ai.ModelSpec
import com.example.analysis.ProjectAnalysisSummary

@Composable
fun DownloadCenterDialog(
  catalog: List<ModelSpec>,
  installedModels: List<ModelSpec>,
  downloadStates: Map<String, ModelDownloadState>,
  activeProjectSummary: ProjectAnalysisSummary?,
  onDownloadModel: (ModelSpec) -> Unit,
  onCancelDownload: (String) -> Unit,
  onExportReport: () -> Unit,
  onExportChat: () -> Unit,
  onDownloadTermux: () -> Unit,
  onExportAppApk: () -> Unit = {},
  onDownloadTermuxApi: () -> Unit = {},
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: AI Models, 1: APK Packages, 2: Reports & Logs

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        .testTag("download_center_dialog")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Download,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Download Center",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("download_center_close_button")) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          contentColor = MaterialTheme.colorScheme.primary
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("AI Models", style = MaterialTheme.typography.labelSmall) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("APK Packages", style = MaterialTheme.typography.labelSmall) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Reports", style = MaterialTheme.typography.labelSmall) }
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
          0 -> {
            // AI Models Tab
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.height(340.dp)
            ) {
              items(catalog) { model ->
                val isInstalled = installedModels.any { it.id == model.id }
                val downloadState = downloadStates[model.id]

                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = model.name,
                          style = MaterialTheme.typography.bodyMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = "${model.parameterSize} • ${model.quantization} • ${model.fileSizeBytes / (1024 * 1024)} MB",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      if (isInstalled) {
                        Surface(
                          color = Color(0xFF10B981).copy(alpha = 0.2f),
                          shape = RoundedCornerShape(6.dp)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("INSTALLED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                          }
                        }
                      } else {
                        Button(
                          onClick = { onDownloadModel(model) },
                          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                          shape = RoundedCornerShape(8.dp),
                          modifier = Modifier
                            .height(32.dp)
                            .testTag("dialog_download_${model.id}")
                        ) {
                          Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Download", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                      }
                    }

                    // Progress indicator if downloading
                    if (downloadState is ModelDownloadState.Downloading) {
                      Spacer(modifier = Modifier.height(8.dp))
                      LinearProgressIndicator(
                        progress = { downloadState.progress },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "${(downloadState.progress * 100).toInt()}% downloaded",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onCancelDownload(model.id) }, modifier = Modifier.size(20.dp)) {
                          Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
          1 -> {
            // APK Packages Tab
            Column(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.height(340.dp)
            ) {
              // 1. NICK AI APK
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("NICK AI App APK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                      Text("Direct package export • Signed APK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                      onClick = onExportAppApk,
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier
                        .height(34.dp)
                        .testTag("dialog_download_app_apk_button")
                    ) {
                      Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Download APK", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Save the compiled NICK AI APK to your device Downloads or Documents storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // 2. Official Termux APK
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("Termux Official APK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                      Text("GitHub Release v0.118+ • Terminal Shell", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                      onClick = onDownloadTermux,
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier
                        .height(34.dp)
                        .testTag("dialog_download_termux_apk_button")
                    ) {
                      Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Termux APK", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Terminal environment required to run REA CLI agents (npx -y rea-agents@latest) and local loopback bridge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // 3. Termux:API Companion APK
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("Termux:API Addon APK", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                      Text("Android hardware bridge permissions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                      onClick = onDownloadTermuxApi,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier
                        .height(34.dp)
                        .testTag("dialog_download_termux_api_button")
                    ) {
                      Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Termux:API", style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }
          2 -> {
            // Reports & Code Exports Tab
            Column(
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.height(340.dp)
            ) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analysis Report & Blueprint", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = if (activeProjectSummary != null) {
                      "Download complete architecture blueprint and findings for ${activeProjectSummary.projectName}."
                    } else {
                      "No project analyzed yet. Analyze a sample project or import an archive first."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Button(
                    onClick = onExportReport,
                    enabled = activeProjectSummary != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_download_report_button")
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Report (.md)", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }

              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat Session Transcript", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Export the entire conversation history, code snippets, and tool logs as Markdown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  OutlinedButton(
                    onClick = onExportChat,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_download_transcript_button")
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Transcript (.txt)", style = MaterialTheme.typography.labelSmall)
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
