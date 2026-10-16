package com.example.tools

import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ToolDefinition(
  val name: String,
  val description: String,
  val sourceAdapter: String, // "LOCAL_SYSTEM", "REA_ADAPTER", "TERMUX_BRIDGE"
  val inputSchema: String,
  val outputSchema: String,
  val requiredPermissions: List<String>,
  val timeoutMs: Long = 10000L,
  val requiresConfirmation: Boolean = false,
  val modifiesFiles: Boolean = false,
  val transmitsExternally: Boolean = false
)

data class ToolExecutionRequest(
  val toolName: String,
  val arguments: Map<String, Any>,
  val userConfirmed: Boolean = false
)

sealed class ToolExecutionResult {
  data class Success(val toolName: String, val resultJson: String, val durationMs: Long) : ToolExecutionResult()
  data class RequiresConfirmation(val toolName: String, val request: ToolExecutionRequest, val warningMessage: String) : ToolExecutionResult()
  data class Denied(val toolName: String, val reason: String) : ToolExecutionResult()
  data class Failed(val toolName: String, val error: String, val durationMs: Long) : ToolExecutionResult()
}

class ToolRegistry {

  private val tools = mutableMapOf<String, ToolDefinition>()

  init {
    registerDefaultTools()
  }

  private fun registerDefaultTools() {
    register(
      ToolDefinition(
        name = "query_hardware_status",
        description = "Returns current device RAM, available memory, and storage metrics to verify model compatibility.",
        sourceAdapter = "LOCAL_SYSTEM",
        inputSchema = "{}",
        outputSchema = "{\"totalRamMb\": number, \"availableRamMb\": number, \"freeStorageMb\": number}",
        requiredPermissions = emptyList(),
        requiresConfirmation = false,
        modifiesFiles = false,
        transmitsExternally = false
      )
    )

    register(
      ToolDefinition(
        name = "run_rea_doctor",
        description = "Executes or parses REA diagnostics to verify Node.js, agent hooks, and Termux compatibility.",
        sourceAdapter = "REA_ADAPTER",
        inputSchema = "{\"rawOutput\": \"string?\"}",
        outputSchema = "{\"success\": boolean, \"tools\": [\"string\"]}",
        requiredPermissions = emptyList(),
        requiresConfirmation = false,
        modifiesFiles = false,
        transmitsExternally = false
      )
    )

    register(
      ToolDefinition(
        name = "inspect_project_archive",
        description = "Performs safe static extraction and architectural inspection on uploaded ZIP or APK files.",
        sourceAdapter = "LOCAL_SYSTEM",
        inputSchema = "{\"project_id\": \"string\"}",
        outputSchema = "{\"totalFiles\": number, \"architecture\": string, \"securityIssues\": [\"string\"]}",
        requiredPermissions = emptyList(),
        requiresConfirmation = false,
        modifiesFiles = false,
        transmitsExternally = false
      )
    )

    register(
      ToolDefinition(
        name = "generate_reconstruction_spec",
        description = "Generates technical architecture blueprint and code implementation guidelines from findings.",
        sourceAdapter = "LOCAL_SYSTEM",
        inputSchema = "{\"project_id\": \"string\", \"target_framework\": \"string\"}",
        outputSchema = "{\"blueprintMarkdown\": string}",
        requiredPermissions = emptyList(),
        requiresConfirmation = false,
        modifiesFiles = false,
        transmitsExternally = false
      )
    )

    register(
      ToolDefinition(
        name = "export_report_to_storage",
        description = "Saves generated analysis report and blueprints to external document storage via SAF.",
        sourceAdapter = "LOCAL_SYSTEM",
        inputSchema = "{\"report_id\": \"string\", \"file_name\": \"string\"}",
        outputSchema = "{\"savedUri\": string}",
        requiredPermissions = emptyList(),
        requiresConfirmation = true, // Consequential: writes to user storage
        modifiesFiles = true,
        transmitsExternally = false
      )
    )

    register(
      ToolDefinition(
        name = "dispatch_termux_command",
        description = "Sends an authorized, whitelisted diagnostic command to Termux via RUN_COMMAND or local bridge.",
        sourceAdapter = "TERMUX_BRIDGE",
        inputSchema = "{\"command\": \"string\"}",
        outputSchema = "{\"stdout\": string, \"exitCode\": number}",
        requiredPermissions = listOf("com.termux.permission.RUN_COMMAND"),
        requiresConfirmation = true, // Consequential: executes external command
        modifiesFiles = false,
        transmitsExternally = false
      )
    )
  }

  fun register(definition: ToolDefinition) {
    tools[definition.name] = definition
  }

  fun getTool(name: String): ToolDefinition? = tools[name]

  fun getAllTools(): List<ToolDefinition> = tools.values.toList()

  suspend fun executeTool(
    request: ToolExecutionRequest,
    hasGlobalConfirmation: Boolean = false,
    executor: suspend (ToolDefinition, Map<String, Any>) -> String
  ): ToolExecutionResult {
    val startTime = System.currentTimeMillis()
    val tool = getTool(request.toolName)
      ?: return ToolExecutionResult.Denied(request.toolName, "Tool '${request.toolName}' is not registered in NICK AI security registry.")

    // Require confirmation check
    if (tool.requiresConfirmation && !request.userConfirmed && !hasGlobalConfirmation) {
      val warning = when {
        tool.modifiesFiles -> "This tool will modify or create files on your device."
        tool.transmitsExternally -> "This tool will transmit analysis data to an external endpoint."
        tool.sourceAdapter == "TERMUX_BRIDGE" -> "This tool will execute a command in the Termux environment."
        else -> "This tool requires explicit user confirmation before running."
      }
      return ToolExecutionResult.RequiresConfirmation(request.toolName, request, warning)
    }

    // Prohibit arbitrary unsafe commands
    if (tool.name == "dispatch_termux_command") {
      val cmd = request.arguments["command"]?.toString() ?: ""
      val allowedPrefixes = listOf("node", "npm", "npx", "pkg", "git", "rea")
      val isSafe = allowedPrefixes.any { cmd.trim().startsWith(it) } &&
        !cmd.contains("rm -rf /") &&
        !cmd.contains(":(){ :|:& };:") &&
        !cmd.contains("> /dev/null")
      if (!isSafe) {
        return ToolExecutionResult.Denied(request.toolName, "Command rejected: Arbitrary shell execution is forbidden. Only verified REA and Node commands are permitted.")
      }
    }

    return try {
      val output = withTimeout(tool.timeoutMs) {
        executor(tool, request.arguments)
      }
      val duration = System.currentTimeMillis() - startTime
      ToolExecutionResult.Success(tool.name, output, duration)
    } catch (e: Exception) {
      val duration = System.currentTimeMillis() - startTime
      ToolExecutionResult.Failed(tool.name, e.message ?: "Execution failed", duration)
    }
  }
}
