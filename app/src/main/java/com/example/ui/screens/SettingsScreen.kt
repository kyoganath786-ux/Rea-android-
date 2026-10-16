package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.CloudConfig
import com.example.ai.DeviceHardwareStatus
import com.example.ai.ModelDownloadState
import com.example.ai.ModelSpec
import com.example.data.preferences.UserPreferences

@Composable
fun SettingsScreen(
  preferences: UserPreferences,
  hardwareStatus: DeviceHardwareStatus,
  catalog: List<ModelSpec>,
  installedModels: List<ModelSpec>,
  activeModel: ModelSpec?,
  isModelLoaded: Boolean,
  downloadStates: Map<String, ModelDownloadState>,
  onDownloadModel: (ModelSpec) -> Unit,
  onCancelDownload: (String) -> Unit,
  onImportCustomModel: (Uri, String, Long) -> Unit,
  onLoadModel: (String) -> Unit,
  onUnloadModel: () -> Unit,
  onDeleteModel: (String) -> Unit,
  onSaveCloudSettings: (String, String, String) -> Unit,
  onToggleCloudConsent: (Boolean) -> Unit,
  onTestCloudConnection: (CloudConfig, (String) -> Unit) -> Unit,
  onUpdateTts: (Boolean, String, Float, Float) -> Unit,
  onExportAppApk: () -> Unit = {},
  onDownloadTermuxApk: () -> Unit = {},
  onDownloadTermuxApi: () -> Unit = {}
) {
  var cloudProvider by remember { mutableStateOf(preferences.cloudProvider) }
  var cloudEndpoint by remember { mutableStateOf(preferences.cloudEndpoint) }
  var cloudApiKey by remember { mutableStateOf(preferences.cloudApiKey) }
  var cloudTestResult by remember { mutableStateOf<String?>(null) }

  var ttsAutoSpeak by remember { mutableStateOf(preferences.ttsAutoSpeak) }
  var ttsPitch by remember { mutableFloatStateOf(preferences.ttsPitch) }
  var ttsSpeechRate by remember { mutableFloatStateOf(preferences.ttsSpeechRate) }

  val modelFilePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let {
      onImportCustomModel(it, "custom-model.gguf", 0L)
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
      .testTag("settings_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Hardware Status Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Device Hardware & Memory Limits",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("RAM Total", style = MaterialTheme.typography.labelSmall)
                Text("${hardwareStatus.totalRamMb} MB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("RAM Available", style = MaterialTheme.typography.labelSmall)
                Text(
                  "${hardwareStatus.availableRamMb} MB",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (hardwareStatus.isLowMemory) Color(0xFFF43F5E) else Color(0xFF10B981)
                )
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text("Storage Free", style = MaterialTheme.typography.labelSmall)
                Text("${hardwareStatus.freeStorageMb} MB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // Local AI Model Manager
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Local GGUF Models",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedButton(
          onClick = { modelFilePicker.launch(arrayOf("*/*")) },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("import_custom_gguf_button")
        ) {
          Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Import GGUF", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    items(catalog) { model ->
      val isInstalled = installedModels.any { it.id == model.id }
      val isActive = activeModel?.id == model.id
      val downloadState = downloadStates[model.id]

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp)
          )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                text = "${model.quantization} • ${model.fileSizeBytes / (1024 * 1024)} MB • Min RAM: ${model.minRamMb} MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (isActive) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "ACTIVE",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(text = model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          // Download Progress
          if (downloadState is ModelDownloadState.Downloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { downloadState.progress },
              modifier = Modifier.fillMaxWidth(),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Downloading: ${(downloadState.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
              IconButton(onClick = { onCancelDownload(model.id) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isInstalled) {
              if (isActive) {
                OutlinedButton(
                  onClick = onUnloadModel,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("Unload", style = MaterialTheme.typography.labelSmall)
                }
              } else {
                Button(
                  onClick = { onLoadModel(model.id) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text("Load Model", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                }
              }

              IconButton(
                onClick = { onDeleteModel(model.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Model", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              }
            } else {
              Button(
                onClick = { onDownloadModel(model) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .height(32.dp)
                  .testTag("download_model_${model.id}")
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }
      }
    }

    // Cloud AI Provider (Optional)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Optional Cloud AI Configuration",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Cloud inference is strictly optional. NICK AI operates offline by default and never transmits private code without explicit consent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = cloudProvider,
            onValueChange = { cloudProvider = it },
            label = { Text("Provider (gemini / openai_compatible)") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = cloudEndpoint,
            onValueChange = { cloudEndpoint = it },
            label = { Text("Endpoint URL") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = cloudApiKey,
            onValueChange = { cloudApiKey = it },
            label = { Text("API Key (Stored in Private App Sandbox)") },
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Authorize Cloud Data Transmission", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              Text("Explicit permission required before transmitting prompts to external APIs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
              checked = preferences.cloudConsentGranted,
              onCheckedChange = onToggleCloudConsent,
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.tertiary
              ),
              modifier = Modifier.testTag("toggle_cloud_consent")
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                onSaveCloudSettings(cloudProvider, cloudEndpoint, cloudApiKey)
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("save_cloud_settings_button")
            ) {
              Text("Save Credentials", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = {
                val cfg = CloudConfig(
                  provider = cloudProvider,
                  endpointUrl = cloudEndpoint,
                  customApiKey = cloudApiKey,
                  consentGranted = preferences.cloudConsentGranted
                )
                onTestCloudConnection(cfg) { res ->
                  cloudTestResult = res
                }
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("test_cloud_connection_button")
            ) {
              Text("Test Connection", style = MaterialTheme.typography.labelSmall)
            }
          }

          if (cloudTestResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = cloudTestResult ?: "",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // Voice Assistant Settings
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "NICK Voice Assistant Settings",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Auto-Speak AI Responses", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
              Text("Read aloud assistant responses via Android TTS engine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
              checked = ttsAutoSpeak,
              onCheckedChange = {
                ttsAutoSpeak = it
                onUpdateTts(it, preferences.ttsLanguage, ttsPitch, ttsSpeechRate)
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.secondary
              )
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text("Speech Pitch (${"%.1f".format(ttsPitch)}x)", style = MaterialTheme.typography.labelSmall)
          Slider(
            value = ttsPitch,
            onValueChange = {
              ttsPitch = it
              onUpdateTts(ttsAutoSpeak, preferences.ttsLanguage, it, ttsSpeechRate)
            },
            valueRange = 0.5f..1.5f
          )

          Spacer(modifier = Modifier.height(6.dp))
          Text("Speech Rate (${"%.1f".format(ttsSpeechRate)}x)", style = MaterialTheme.typography.labelSmall)
          Slider(
            value = ttsSpeechRate,
            onValueChange = {
              ttsSpeechRate = it
              onUpdateTts(ttsAutoSpeak, preferences.ttsLanguage, ttsPitch, it)
            },
            valueRange = 0.5f..1.5f
          )
        }
      }
    }

    // APK Download & Package Center
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
          .testTag("settings_apk_download_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "APK Download & Package Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "READY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Export and download verified APK packages directly to your device storage:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))

          // 1. NICK AI App APK
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                    text = "NICK AI Application APK",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Package: com.aistudio.nickai.vzkw • Debug Signed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Button(
                  onClick = onExportAppApk,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("settings_download_app_apk_button")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Download APK", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Exports the running app APK (or app-debug.apk) to your device Downloads or Documents folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 2. Official Termux APK
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                    text = "Termux Official APK",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "GitHub Release v0.118+ • Android Terminal Environment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Button(
                  onClick = onDownloadTermuxApk,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("settings_download_termux_apk_button")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Termux APK", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Required for executing REA CLI agents (npx -y rea-agents@latest) and running the localhost bridge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 3. Termux:API Addon APK
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                    text = "Termux:API Addon APK",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Companion APK for Android hardware access in terminal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                OutlinedButton(
                  onClick = onDownloadTermuxApi,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("settings_download_termux_api_button")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("API Addon", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }
  }
}
