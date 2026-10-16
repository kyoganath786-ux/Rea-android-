package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CloudConfig
import com.example.ai.CloudModelProvider
import com.example.ai.CloudModelProviderImpl
import com.example.ai.DeviceHardwareStatus
import com.example.ai.LocalModelProvider
import com.example.ai.LocalModelProviderImpl
import com.example.ai.ModelSpec
import com.example.analysis.ProjectAnalysisSummary
import com.example.analysis.ProjectAnalyzer
import com.example.data.local.AnalysisJobEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.FindingEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ToolExecutionEntity
import com.example.data.preferences.AiMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.rea.REAAdapter
import com.example.rea.REAAdapterImpl
import com.example.rea.READiagnosticResult
import com.example.rea.REAToolDefinition
import com.example.termux.TermuxIntegration
import com.example.termux.TermuxStatus
import com.example.tools.ToolDefinition
import com.example.tools.ToolExecutionRequest
import com.example.tools.ToolExecutionResult
import com.example.tools.ToolRegistry
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID

sealed class UiEvent {
  data class ShowSnackbar(val message: String) : UiEvent()
  data class RequestToolConfirmation(val toolName: String, val request: ToolExecutionRequest, val warning: String) : UiEvent()
}

class NickAiViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getInstance(application)
  private val conversationDao = database.conversationDao()
  private val projectDao = database.projectDao()
  private val toolDao = database.toolDao()

  val preferencesRepo = UserPreferencesRepository(application)
  val preferences: StateFlow<UserPreferences> = preferencesRepo.preferencesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

  val localModelProvider: LocalModelProvider = LocalModelProviderImpl(application)
  val cloudModelProvider: CloudModelProvider = CloudModelProviderImpl()
  val termuxIntegration = TermuxIntegration(application)
  val reaAdapter: REAAdapter = REAAdapterImpl()
  val termuxBridgeManager: com.example.termux.TermuxBridgeManager = (reaAdapter as REAAdapterImpl).getBridgeManager()
  val projectAnalyzer = ProjectAnalyzer(application)
  val toolRegistry = ToolRegistry()
  val voiceManager = VoiceAssistantManager(application)

  // Navigation State
  private val _currentScreen = MutableStateFlow("dashboard") // "dashboard", "chat", "analysis", "termux", "tools", "settings"
  val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

  // Chat State
  val conversations: StateFlow<List<ConversationEntity>> = conversationDao.getAllConversations()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
  val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

  private val _isGenerating = MutableStateFlow(false)
  val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

  private val _streamingMessageText = MutableStateFlow("")
  val streamingMessageText: StateFlow<String> = _streamingMessageText.asStateFlow()

  private var activeGenerationJob: Job? = null

  // Analysis State
  val projects: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _activeProjectSummary = MutableStateFlow<ProjectAnalysisSummary?>(null)
  val activeProjectSummary: StateFlow<ProjectAnalysisSummary?> = _activeProjectSummary.asStateFlow()

  private val _isAnalyzing = MutableStateFlow(false)
  val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

  // REA & Termux State
  val termuxStatus: StateFlow<TermuxStatus> = termuxIntegration.status
  private val _reaDoctorResult = MutableStateFlow<READiagnosticResult?>(null)
  val reaDoctorResult: StateFlow<READiagnosticResult?> = _reaDoctorResult.asStateFlow()

  private val _discoveredReaTools = MutableStateFlow<List<REAToolDefinition>>(emptyList())
  val discoveredReaTools: StateFlow<List<REAToolDefinition>> = _discoveredReaTools.asStateFlow()

  private val _bridgeStatusMessage = MutableStateFlow<String?>(null)
  val bridgeStatusMessage: StateFlow<String?> = _bridgeStatusMessage.asStateFlow()

  // Hardware Status
  private val _hardwareStatus = MutableStateFlow(localModelProvider.checkDeviceHardware())
  val hardwareStatus: StateFlow<DeviceHardwareStatus> = _hardwareStatus.asStateFlow()

  // Tool Confirmations
  private val _pendingConfirmation = MutableStateFlow<ToolExecutionResult.RequiresConfirmation?>(null)
  val pendingConfirmation: StateFlow<ToolExecutionResult.RequiresConfirmation?> = _pendingConfirmation.asStateFlow()

  val toolExecutions: StateFlow<List<ToolExecutionEntity>> = toolDao.getAllExecutions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Voice Assistant UI Dialog
  private val _isVoiceDialogOpen = MutableStateFlow(false)
  val isVoiceDialogOpen: StateFlow<Boolean> = _isVoiceDialogOpen.asStateFlow()

  init {
    voiceManager.onTranscriptionComplete = { spokenText ->
      handleVoiceCommand(spokenText)
    }

    viewModelScope.launch {
      // Ensure at least one conversation exists
      val convList = conversationDao.getAllConversations().first()
      if (convList.isEmpty()) {
        createNewChat("Initial Exploration")
      } else {
        selectConversation(convList.first().id)
      }
    }

    // Refresh hardware and tools
    refreshHardware()
  }

  fun navigateTo(screen: String) {
    _currentScreen.value = screen
  }

  fun refreshHardware() {
    _hardwareStatus.value = localModelProvider.checkDeviceHardware()
  }

  fun createNewChat(title: String = "New Analysis Session") {
    viewModelScope.launch {
      val newId = UUID.randomUUID().toString()
      val conv = ConversationEntity(
        id = newId,
        title = title,
        mode = preferences.value.aiMode.name,
        modelId = preferences.value.activeLocalModelId
      )
      conversationDao.insertConversation(conv)
      selectConversation(newId)

      // Add system greeting message
      val welcomeMsg = MessageEntity(
        id = UUID.randomUUID().toString(),
        conversationId = newId,
        sender = "assistant",
        content = "Welcome to **NICK AI**.\n\nI am configured for reverse engineering, code architecture reconstruction, Termux operations, and static APK inspection.\n\n• Current Mode: `${preferences.value.aiMode}`\n• Local Model: `${localModelProvider.activeModel.value?.name ?: "TinyLlama 1.1B"}`\n\nHow can I help you today?"
      )
      conversationDao.insertMessage(welcomeMsg)
    }
  }

  fun selectConversation(id: String) {
    _activeConversationId.value = id
    viewModelScope.launch {
      conversationDao.getMessagesForConversation(id).collect { msgs ->
        _messages.value = msgs
      }
    }
  }

  fun deleteConversation(id: String) {
    viewModelScope.launch {
      conversationDao.deleteConversation(id)
      conversationDao.deleteMessagesForConversation(id)
      val remaining = conversationDao.getAllConversations().first()
      if (remaining.isNotEmpty()) {
        selectConversation(remaining.first().id)
      } else {
        createNewChat()
      }
    }
  }

  fun sendMessage(promptText: String, attachedFileSnippet: String? = null) {
    val convId = _activeConversationId.value ?: return
    if (promptText.isBlank() && attachedFileSnippet.isNullOrBlank()) return

    val fullUserPrompt = if (!attachedFileSnippet.isNullOrBlank()) {
      "$promptText\n\n```\n$attachedFileSnippet\n```"
    } else {
      promptText
    }

    viewModelScope.launch {
      val userMsg = MessageEntity(
        id = UUID.randomUUID().toString(),
        conversationId = convId,
        sender = "user",
        content = fullUserPrompt
      )
      conversationDao.insertMessage(userMsg)

      generateAiResponse(fullUserPrompt, convId)
    }
  }

  private fun generateAiResponse(prompt: String, convId: String) {
    activeGenerationJob?.cancel()
    _isGenerating.value = true
    _streamingMessageText.value = ""

    activeGenerationJob = viewModelScope.launch {
      val currentMode = preferences.value.aiMode
      val fullBuilder = StringBuilder()

      try {
        val stream = when (currentMode) {
          AiMode.LOCAL_ONLY -> {
            localModelProvider.generateStreaming(prompt)
          }
          AiMode.LOCAL_FIRST -> {
            // Local inference first with tool capability
            localModelProvider.generateStreaming(prompt)
          }
          AiMode.CLOUD_ONLY -> {
            val cloudCfg = CloudConfig(
              provider = preferences.value.cloudProvider,
              endpointUrl = preferences.value.cloudEndpoint,
              customApiKey = preferences.value.cloudApiKey,
              consentGranted = preferences.value.cloudConsentGranted
            )
            cloudModelProvider.generateStreaming(prompt, cloudCfg)
          }
          AiMode.AUTO -> {
            // Auto picks local if available, or cloud if consented
            if (localModelProvider.isModelLoaded.value) {
              localModelProvider.generateStreaming(prompt)
            } else {
              val cloudCfg = CloudConfig(
                provider = preferences.value.cloudProvider,
                endpointUrl = preferences.value.cloudEndpoint,
                customApiKey = preferences.value.cloudApiKey,
                consentGranted = preferences.value.cloudConsentGranted
              )
              cloudModelProvider.generateStreaming(prompt, cloudCfg)
            }
          }
        }

        stream.collect { chunk ->
          fullBuilder.append(chunk)
          _streamingMessageText.value = fullBuilder.toString()
        }

        val assistantMsg = MessageEntity(
          id = UUID.randomUUID().toString(),
          conversationId = convId,
          sender = "assistant",
          content = fullBuilder.toString()
        )
        conversationDao.insertMessage(assistantMsg)

        if (preferences.value.ttsAutoSpeak) {
          voiceManager.speak(fullBuilder.toString())
        }

      } catch (e: CancellationException) {
        if (fullBuilder.isNotEmpty()) {
          val partialMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = convId,
            sender = "assistant",
            content = fullBuilder.toString() + "\n\n*[Generation cancelled]*"
          )
          conversationDao.insertMessage(partialMsg)
        }
      } catch (e: Exception) {
        val errorMsg = MessageEntity(
          id = UUID.randomUUID().toString(),
          conversationId = convId,
          sender = "system",
          content = "❌ Generation Error: ${e.message}\n\nPlease check your provider configuration or switch to Local mode in Settings."
        )
        conversationDao.insertMessage(errorMsg)
      } finally {
        _isGenerating.value = false
        _streamingMessageText.value = ""
      }
    }
  }

  fun cancelGeneration() {
    activeGenerationJob?.cancel()
    localModelProvider.cancelGeneration()
    _isGenerating.value = false
    _streamingMessageText.value = ""
  }

  // Voice Assistant Flow
  fun openVoiceAssistant() {
    _isVoiceDialogOpen.value = true
    voiceManager.startListening(preferences.value.ttsLanguage)
  }

  fun closeVoiceAssistant() {
    voiceManager.stopListening()
    voiceManager.stopSpeaking()
    _isVoiceDialogOpen.value = false
  }

  private fun handleVoiceCommand(spokenText: String) {
    if (spokenText.isBlank()) return
    sendMessage(spokenText)
    // Keep voice dialog open briefly to display transcription, then dismiss
    viewModelScope.launch {
      kotlinx.coroutines.delay(1200)
      _isVoiceDialogOpen.value = false
    }
  }

  // Termux & REA Operations
  fun refreshTermuxStatus() {
    termuxIntegration.checkStatus()
  }

  fun runReaDoctor(pastedOutput: String? = null) {
    viewModelScope.launch {
      val result = reaAdapter.runDiagnostics(pastedOutput)
      _reaDoctorResult.value = result
      if (pastedOutput != null) {
        termuxIntegration.ingestDiagnosticOutput(pastedOutput)
      }
    }
  }

  fun testReaBridge(url: String, token: String) {
    viewModelScope.launch {
      _bridgeStatusMessage.value = "Testing bridge at $url..."
      val result = reaAdapter.testBridgeConnection(url, token)
      result.onSuccess { msg ->
        _bridgeStatusMessage.value = "✅ $msg"
        preferencesRepo.updateTermuxBridge(url, token)
        discoverReaTools()
      }.onFailure { err ->
        _bridgeStatusMessage.value = "❌ $err"
      }
    }
  }

  fun discoverReaTools() {
    viewModelScope.launch {
      val tools = reaAdapter.discoverTools()
      _discoveredReaTools.value = tools
    }
  }

  // Project Import & Analysis
  fun importAndAnalyzeProject(uri: Uri, displayName: String) {
    _isAnalyzing.value = true
    viewModelScope.launch {
      try {
        val extractResult = projectAnalyzer.importAndExtractArchive(uri, displayName)
        extractResult.onSuccess { extractedFolder ->
          val summary = projectAnalyzer.analyzeExtractedProject(extractedFolder, displayName)
          _activeProjectSummary.value = summary

          // Save to database
          val projEntity = ProjectEntity(
            id = summary.projectId,
            name = summary.projectName,
            type = if (displayName.endsWith(".apk")) "apk" else "zip",
            path = extractedFolder.absolutePath,
            sizeBytes = extractedFolder.length(),
            status = "COMPLETED",
            summary = "${summary.detectedArchitecture} • ${summary.totalFiles} files • ${summary.securityFindings.size} findings"
          )
          projectDao.insertProject(projEntity)

          val jobEntity = AnalysisJobEntity(
            id = UUID.randomUUID().toString(),
            projectId = summary.projectId,
            status = "COMPLETED",
            completedAt = System.currentTimeMillis(),
            adapterUsed = "builtin_static"
          )
          projectDao.insertAnalysisJob(jobEntity)

          val findingEntities = summary.securityFindings.map { finding ->
            FindingEntity(
              id = UUID.randomUUID().toString(),
              jobId = jobEntity.id,
              projectId = summary.projectId,
              category = "SECURITY",
              severity = finding.severity,
              title = finding.title,
              description = finding.description,
              fileLocation = finding.fileLocation,
              evidence = finding.recommendation
            )
          }
          projectDao.insertFindings(findingEntities)

        }.onFailure { err ->
          _activeProjectSummary.value = null
        }
      } finally {
        _isAnalyzing.value = false
      }
    }
  }

  fun analyzeSampleHarmlessProject() {
    _isAnalyzing.value = true
    viewModelScope.launch {
      // Create a harmless sample Android + Node project for testing
      val sampleDir = File(getApplication<Application>().filesDir, "sample_clean_project")
      if (!sampleDir.exists()) sampleDir.mkdirs()

      val sampleCode = File(sampleDir, "SampleRepository.kt")
      sampleCode.writeText("""
        package com.example.sample
        
        class SampleRepository {
          fun fetchStatus(): String = "SAFE_OFFLINE_VERIFIED"
        }
      """.trimIndent())

      val manifest = File(sampleDir, "AndroidManifest.xml")
      manifest.writeText("""
        <manifest package="com.example.sample">
          <application android:allowBackup="true" />
        </manifest>
      """.trimIndent())

      val summary = projectAnalyzer.analyzeExtractedProject(sampleDir, "Sample-Harmless-App.zip")
      _activeProjectSummary.value = summary

      val projEntity = ProjectEntity(
        id = summary.projectId,
        name = "Sample-Harmless-App.zip",
        type = "harmless_sample",
        path = sampleDir.absolutePath,
        sizeBytes = 1024L,
        status = "COMPLETED",
        summary = "Safe test fixture verified locally"
      )
      projectDao.insertProject(projEntity)
      _isAnalyzing.value = false
    }
  }

  // Tool Confirmations & Execution
  fun requestToolExecution(request: ToolExecutionRequest) {
    viewModelScope.launch {
      val result = toolRegistry.executeTool(request, !preferences.value.requireToolConfirmation) { tool, args ->
        when (tool.name) {
          "query_hardware_status" -> {
            val hw = localModelProvider.checkDeviceHardware()
            JSONObject().apply {
              put("totalRamMb", hw.totalRamMb)
              put("availableRamMb", hw.availableRamMb)
              put("freeStorageMb", hw.freeStorageMb)
            }.toString()
          }
          "run_rea_doctor" -> {
            val doctorRes = reaAdapter.runDiagnostics(args["rawOutput"]?.toString())
            JSONObject().apply {
              put("success", doctorRes.success)
              put("tools", doctorRes.toolsDetected)
            }.toString()
          }
          "generate_reconstruction_spec" -> {
            val sum = _activeProjectSummary.value
            if (sum != null) projectAnalyzer.generateReconstructionSpec(sum) else "No active project analyzed."
          }
          "dispatch_termux_command" -> {
            val cmd = args["command"]?.toString() ?: "node --version"
            val execResult = termuxBridgeManager.executeCommand(cmd)
            if (execResult.isSuccess) {
              val out = execResult.getOrThrow()
              JSONObject().apply {
                put("exit_code", out.exitCode)
                put("stdout", out.stdout)
                put("stderr", out.stderr)
              }.toString()
            } else {
              JSONObject().apply {
                put("error", execResult.exceptionOrNull()?.message)
              }.toString()
            }
          }
          else -> "Tool ${tool.name} completed successfully."
        }
      }

      when (result) {
        is ToolExecutionResult.RequiresConfirmation -> {
          _pendingConfirmation.value = result
        }
        is ToolExecutionResult.Success -> {
          toolDao.insertExecution(
            ToolExecutionEntity(
              id = UUID.randomUUID().toString(),
              toolName = result.toolName,
              argumentsJson = JSONObject(request.arguments).toString(),
              resultJson = result.resultJson,
              status = "SUCCESS",
              durationMs = result.durationMs
            )
          )
        }
        is ToolExecutionResult.Denied -> {
          toolDao.insertExecution(
            ToolExecutionEntity(
              id = UUID.randomUUID().toString(),
              toolName = result.toolName,
              argumentsJson = JSONObject(request.arguments).toString(),
              resultJson = result.reason,
              status = "DENIED",
              durationMs = 0L
            )
          )
        }
        is ToolExecutionResult.Failed -> {
          toolDao.insertExecution(
            ToolExecutionEntity(
              id = UUID.randomUUID().toString(),
              toolName = result.toolName,
              argumentsJson = JSONObject(request.arguments).toString(),
              resultJson = result.error,
              status = "FAILED",
              durationMs = result.durationMs
            )
          )
        }
      }
    }
  }

  fun confirmPendingTool(confirmed: Boolean) {
    val pending = _pendingConfirmation.value ?: return
    _pendingConfirmation.value = null

    if (confirmed) {
      val confirmedReq = pending.request.copy(userConfirmed = true)
      requestToolExecution(confirmedReq)
    } else {
      viewModelScope.launch {
        toolDao.insertExecution(
          ToolExecutionEntity(
            id = UUID.randomUUID().toString(),
            toolName = pending.toolName,
            argumentsJson = JSONObject(pending.request.arguments).toString(),
            resultJson = "User declined tool execution confirmation.",
            status = "DENIED",
            durationMs = 0L
          )
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    voiceManager.release()
  }
}
