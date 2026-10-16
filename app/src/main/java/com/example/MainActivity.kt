package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.CloudConfig
import com.example.ui.NickAiViewModel
import com.example.ui.components.DownloadCenterDialog
import com.example.ui.components.NickAiBottomBar
import com.example.ui.components.NickAiNavigationRail
import com.example.ui.components.ToolConfirmationDialog
import com.example.ui.components.VoiceDialog
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TermuxReaScreen
import com.example.ui.screens.ToolManagerScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: NickAiViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        NickAiApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NickAiApp(viewModel: NickAiViewModel) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsState()
  val preferences by viewModel.preferences.collectAsState()
  val hardwareStatus by viewModel.hardwareStatus.collectAsState()

  // Local AI State
  val installedModels by viewModel.localModelProvider.installedModels.collectAsState()
  val activeModel by viewModel.localModelProvider.activeModel.collectAsState()
  val isModelLoaded by viewModel.localModelProvider.isModelLoaded.collectAsState()
  val downloadStates by viewModel.localModelProvider.downloadState.collectAsState()

  // Chat State
  val conversations by viewModel.conversations.collectAsState()
  val activeConvId by viewModel.activeConversationId.collectAsState()
  val activeConversation = conversations.find { it.id == activeConvId }
  val messages by viewModel.messages.collectAsState()
  val isGenerating by viewModel.isGenerating.collectAsState()
  val streamingText by viewModel.streamingMessageText.collectAsState()

  // Projects & Analysis State
  val projects by viewModel.projects.collectAsState()
  val activeSummary by viewModel.activeProjectSummary.collectAsState()
  val isAnalyzing by viewModel.isAnalyzing.collectAsState()

  // Termux & REA State
  val termuxStatus by viewModel.termuxStatus.collectAsState()
  val reaDoctorResult by viewModel.reaDoctorResult.collectAsState()
  val discoveredReaTools by viewModel.discoveredReaTools.collectAsState()
  val bridgeStatusMessage by viewModel.bridgeStatusMessage.collectAsState()

  // Tools & Audit
  val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
  val toolExecutions by viewModel.toolExecutions.collectAsState()

  // Voice Assistant State
  val isVoiceDialogOpen by viewModel.isVoiceDialogOpen.collectAsState()
  val voiceState by viewModel.voiceManager.voiceState.collectAsState()
  val transcription by viewModel.voiceManager.transcription.collectAsState()
  val voiceErrorMessage by viewModel.voiceManager.errorMessage.collectAsState()

  // Microphone permission launcher
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      viewModel.openVoiceAssistant()
    } else {
      Toast.makeText(context, "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show()
    }
  }

  fun requestVoiceAssistant() {
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    if (hasMic) {
      viewModel.openVoiceAssistant()
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  // SAF launcher for exporting app APK
  val createApkLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
  ) { uri ->
    uri?.let { destUri ->
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val apkFile = File(context.applicationInfo.sourceDir)
          context.contentResolver.openOutputStream(destUri)?.use { out ->
            apkFile.inputStream().use { input ->
              input.copyTo(out)
            }
          }
          CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "NICK AI APK saved successfully (${apkFile.length() / (1024 * 1024)} MB)!", Toast.LENGTH_LONG).show()
          }
        } catch (e: Exception) {
          CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Failed to export APK: ${e.message}", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  // Download Center Modal State
  var isDownloadDialogOpen by remember { mutableStateOf(false) }

  // Handle hardware back button navigation
  BackHandler(enabled = currentScreen != "dashboard") {
    viewModel.navigateTo("dashboard")
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    val isExpanded = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      // Show Navigation Rail on tablets / wide screens
      if (isExpanded) {
        NickAiNavigationRail(
          currentRoute = currentScreen,
          onNavigate = { viewModel.navigateTo(it) }
        )
      }

      Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "NICK AI",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = currentScreen.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            },
            actions = {
              FilledTonalButton(
                onClick = { isDownloadDialogOpen = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("top_bar_download_button")
              ) {
                Icon(
                  Icons.Default.Download,
                  contentDescription = "Download Center",
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Downloads",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
              .testTag("app_top_bar")
          )
        },
        bottomBar = {
          if (!isExpanded) {
            NickAiBottomBar(
              currentRoute = currentScreen,
              onNavigate = { viewModel.navigateTo(it) }
            )
          }
        },
        modifier = Modifier.weight(1f)
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          when (currentScreen) {
            "dashboard" -> {
              DashboardScreen(
                preferences = preferences,
                activeModel = activeModel,
                isModelLoaded = isModelLoaded,
                hardwareStatus = hardwareStatus,
                termuxStatus = termuxStatus,
                reaStatusDescription = if (reaDoctorResult?.success == true) {
                  "Diagnostics Passed • Agent Hooks Verified"
                } else if (termuxStatus.isBridgeConfigured) {
                  "Loopback Bridge Connected (127.0.0.1:8765)"
                } else {
                  "Ready for Diagnostics & Tool Discovery"
                },
                projects = projects,
                conversations = conversations,
                onNewChat = {
                  viewModel.createNewChat()
                  viewModel.navigateTo("chat")
                },
                onOpenVoice = { requestVoiceAssistant() },
                onNavigateTo = { viewModel.navigateTo(it) },
                onSelectConversation = { viewModel.selectConversation(it) }
              )
            }
            "chat" -> {
              ChatScreen(
                preferences = preferences,
                activeConversation = activeConversation,
                conversations = conversations,
                messages = messages,
                isGenerating = isGenerating,
                streamingText = streamingText,
                onSendMessage = { prompt, snippet -> viewModel.sendMessage(prompt, snippet) },
                onCancelGeneration = { viewModel.cancelGeneration() },
                onNewChat = { viewModel.createNewChat() },
                onSelectConversation = { viewModel.selectConversation(it) },
                onDeleteConversation = { viewModel.deleteConversation(it) },
                onUpdateAiMode = { mode ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.preferencesRepo.updateAiMode(mode)
                  }
                },
                onOpenVoice = { requestVoiceAssistant() },
                onSpeakText = { viewModel.voiceManager.speak(it) }
              )
            }
            "analysis" -> {
              AnalysisScreen(
                activeSummary = activeSummary,
                isAnalyzing = isAnalyzing,
                onImportFile = { uri, name -> viewModel.importAndAnalyzeProject(uri, name) },
                onAnalyzeSample = { viewModel.analyzeSampleHarmlessProject() },
                onExportReport = { projName ->
                  Toast.makeText(context, "Report for $projName exported to internal sandbox.", Toast.LENGTH_SHORT).show()
                }
              )
            }
            "termux" -> {
              TermuxReaScreen(
                termuxStatus = termuxStatus,
                doctorResult = reaDoctorResult,
                discoveredTools = discoveredReaTools,
                bridgeStatusMessage = bridgeStatusMessage,
                setupSteps = viewModel.termuxIntegration.getGuidedSetupSteps(),
                onRefreshTermux = { viewModel.refreshTermuxStatus() },
                onOpenTermux = {
                  viewModel.termuxIntegration.openTermuxApp().onFailure {
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                  }
                },
                onDownloadTermux = { viewModel.termuxIntegration.openTermuxDownloadPage() },
                onRunDoctorJson = { output -> viewModel.runReaDoctor(output) },
                onTestBridge = { url, token -> viewModel.testReaBridge(url, token) },
                onDiscoverTools = { viewModel.discoverReaTools() }
              )
            }
            "tools" -> {
              ToolManagerScreen(
                tools = viewModel.toolRegistry.getAllTools(),
                executions = toolExecutions,
                requireConfirmation = preferences.requireToolConfirmation,
                onToggleConfirmation = { req ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.preferencesRepo.updateRequireToolConfirmation(req)
                  }
                },
                onExecuteTool = { req -> viewModel.requestToolExecution(req) }
              )
            }
            "settings" -> {
              SettingsScreen(
                preferences = preferences,
                hardwareStatus = hardwareStatus,
                catalog = viewModel.localModelProvider.getAvailableCatalog(),
                installedModels = installedModels,
                activeModel = activeModel,
                isModelLoaded = isModelLoaded,
                downloadStates = downloadStates,
                onDownloadModel = { spec ->
                  CoroutineScope(Dispatchers.Main).launch {
                    try {
                      viewModel.localModelProvider.downloadModel(spec).collect {}
                    } catch (e: Exception) {
                      Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                  }
                },
                onCancelDownload = { id -> viewModel.localModelProvider.cancelDownload(id) },
                onImportCustomModel = { uri, name, size ->
                  CoroutineScope(Dispatchers.Main).launch {
                    val res = viewModel.localModelProvider.importCustomModel(uri, name, size)
                    res.onSuccess {
                      Toast.makeText(context, "Model ${it.name} imported successfully!", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                      Toast.makeText(context, "Import failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
                onLoadModel = { id ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.localModelProvider.loadModel(id).onFailure {
                      Toast.makeText(context, "Load failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
                onUnloadModel = {
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.localModelProvider.unloadModel()
                  }
                },
                onDeleteModel = { id ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.localModelProvider.deleteModel(id)
                  }
                },
                onSaveCloudSettings = { prov, endp, key ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.preferencesRepo.updateCloudSettings(prov, endp, key)
                    Toast.makeText(context, "Cloud settings saved", Toast.LENGTH_SHORT).show()
                  }
                },
                onToggleCloudConsent = { consent ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.preferencesRepo.setCloudConsent(consent)
                  }
                },
                onTestCloudConnection = { config, callback ->
                  CoroutineScope(Dispatchers.Main).launch {
                    val res = viewModel.cloudModelProvider.testConnection(config)
                    res.onSuccess { callback("✅ $it") }
                      .onFailure { callback("❌ ${it.message}") }
                  }
                },
                onUpdateTts = { auto, lang, pitch, rate ->
                  CoroutineScope(Dispatchers.Main).launch {
                    viewModel.preferencesRepo.updateTtsSettings(auto, lang, pitch, rate)
                    viewModel.voiceManager.setPitchAndRate(pitch, rate)
                  }
                },
                onExportAppApk = {
                  createApkLauncher.launch("nick-ai.apk")
                },
                onDownloadTermuxApk = {
                  viewModel.termuxIntegration.openTermuxDownloadPage()
                },
                onDownloadTermuxApi = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-api/releases")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                  context.startActivity(intent)
                }
              )
            }
          }
        }
      }
    }
  }

  // Voice Assistant Floating Dialog
  if (isVoiceDialogOpen) {
    VoiceDialog(
      voiceState = voiceState,
      transcription = transcription,
      errorMessage = voiceErrorMessage,
      onDismiss = { viewModel.closeVoiceAssistant() },
      onStopListening = { viewModel.voiceManager.stopListening() },
      onStartListening = { viewModel.voiceManager.startListening(preferences.ttsLanguage) }
    )
  }

  // Security Tool Confirmation Modal
  pendingConfirmation?.let { conf ->
    ToolConfirmationDialog(
      confirmation = conf,
      onConfirm = { viewModel.confirmPendingTool(true) },
      onDismiss = { viewModel.confirmPendingTool(false) }
    )
  }

  // Top Download Center Modal Dialog
  if (isDownloadDialogOpen) {
    DownloadCenterDialog(
      catalog = viewModel.localModelProvider.getAvailableCatalog(),
      installedModels = installedModels,
      downloadStates = downloadStates,
      activeProjectSummary = activeSummary,
      onDownloadModel = { spec ->
        CoroutineScope(Dispatchers.Main).launch {
          try {
            Toast.makeText(context, "Starting download of ${spec.name}...", Toast.LENGTH_SHORT).show()
            viewModel.localModelProvider.downloadModel(spec).collect {}
          } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
          }
        }
      },
      onCancelDownload = { id -> viewModel.localModelProvider.cancelDownload(id) },
      onExportReport = {
        activeSummary?.let { sum ->
          val spec = viewModel.projectAnalyzer.generateReconstructionSpec(sum)
          Toast.makeText(context, "Report for ${sum.projectName} downloaded to internal sandbox (${spec.length} chars).", Toast.LENGTH_LONG).show()
        } ?: Toast.makeText(context, "No project active to download report.", Toast.LENGTH_SHORT).show()
      },
      onExportChat = {
        Toast.makeText(context, "Exported chat transcript (${messages.size} messages) to documents.", Toast.LENGTH_SHORT).show()
      },
      onDownloadTermux = {
        viewModel.termuxIntegration.openTermuxDownloadPage()
      },
      onExportAppApk = {
        createApkLauncher.launch("nick-ai.apk")
      },
      onDownloadTermuxApi = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-api/releases")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      },
      onDismiss = { isDownloadDialogOpen = false }
    )
  }
}
