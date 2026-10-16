package com.example.termux

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TermuxStatus(
  val isTermuxInstalled: Boolean,
  val isRunCommandPermissionGranted: Boolean,
  val termuxVersionName: String?,
  val isBridgeConfigured: Boolean,
  val detectedNodeVersion: String?,
  val lastDiagnosticOutput: String?,
  val isOperational: Boolean
)

data class TermuxSetupStep(
  val stepNumber: Int,
  val title: String,
  val command: String,
  val explanation: String,
  val expectedOutput: String
)

class TermuxIntegration(private val context: Context) {

  private var isBridgeConfigured = false
  private var detectedNodeVersion: String? = null
  private var lastDiagnosticOutput: String? = null

  private val _status = MutableStateFlow(computeStatus())
  val status: StateFlow<TermuxStatus> = _status.asStateFlow()

  companion object {
    const val TERMUX_PACKAGE = "com.termux"
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    const val TERMUX_RELEASES_URL = "https://github.com/termux/termux-app/releases"
  }

  private fun computeStatus(): TermuxStatus {
    val pm = context.packageManager
    var isInstalled = false
    var versionName: String? = null

    try {
      val pkgInfo = pm.getPackageInfo(TERMUX_PACKAGE, 0)
      isInstalled = true
      versionName = pkgInfo.versionName
    } catch (_: PackageManager.NameNotFoundException) {
      isInstalled = false
    }

    val hasRunCommandPerm = context.checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

    return TermuxStatus(
      isTermuxInstalled = isInstalled,
      isRunCommandPermissionGranted = hasRunCommandPerm,
      termuxVersionName = versionName,
      isBridgeConfigured = isBridgeConfigured,
      detectedNodeVersion = detectedNodeVersion,
      lastDiagnosticOutput = lastDiagnosticOutput,
      isOperational = isInstalled && (hasRunCommandPerm || isBridgeConfigured)
    )
  }

  fun checkStatus(): TermuxStatus {
    val newStatus = computeStatus()
    _status.value = newStatus
    return newStatus
  }

  fun getGuidedSetupSteps(): List<TermuxSetupStep> = listOf(
    TermuxSetupStep(
      stepNumber = 1,
      title = "Update Packages & Install Node.js LTS",
      command = "pkg update -y && pkg install -y nodejs-lts git",
      explanation = "Installs Node.js LTS and Git in the Termux environment. Required by REA CLI.",
      expectedOutput = "Installed: nodejs-lts, git"
    ),
    TermuxSetupStep(
      stepNumber = 2,
      title = "Verify Node.js & NPM Runtime",
      command = "node --version && npm --version",
      explanation = "Confirms that Node.js 18+ or 20+ LTS is properly initialized.",
      expectedOutput = "v20.x.x\n10.x.x"
    ),
    TermuxSetupStep(
      stepNumber = 3,
      title = "Run REA Health & Diagnostics",
      command = "npx -y rea-agents@latest doctor --json",
      explanation = "Tests environment compatibility, dependencies, agent hooks, and produces structured diagnostic JSON.",
      expectedOutput = "{\"status\": \"ok\", \"node\": \"20.x\", \"tools\": [...]}"
    ),
    TermuxSetupStep(
      stepNumber = 4,
      title = "Inspect REA CLI Documentation",
      command = "npx -y rea-agents@latest --help",
      explanation = "Displays documented subcommands, analysis modules, and MCP server arguments.",
      expectedOutput = "Usage: rea [options] [command]"
    ),
    TermuxSetupStep(
      stepNumber = 5,
      title = "Start NICK AI Termux Loopback Bridge",
      command = "npx -y rea-agents@latest serve --port 8765 --token nick-local-sec",
      explanation = "Binds a local MCP / HTTP bridge on 127.0.0.1:8765 with local token authentication for NICK AI applet.",
      expectedOutput = "REA bridge listening on 127.0.0.1:8765"
    )
  )

  fun copyToClipboard(text: String, label: String = "Termux Command") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
  }

  fun openTermuxApp(): Result<Boolean> {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(TERMUX_PACKAGE)
    return if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(launchIntent)
      Result.success(true)
    } else {
      Result.failure(IllegalStateException("Termux is not installed on this device."))
    }
  }

  fun openTermuxDownloadPage() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TERMUX_RELEASES_URL)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }

  fun ingestDiagnosticOutput(rawOutput: String): TermuxStatus {
    val nodeRegex = Regex("v([0-9]+\\.[0-9]+\\.[0-9]+)")
    val match = nodeRegex.find(rawOutput)
    if (match != null) {
      detectedNodeVersion = match.value
    }
    if (rawOutput.contains("listening") || rawOutput.contains("status\": \"ok\"")) {
      isBridgeConfigured = true
    }
    lastDiagnosticOutput = rawOutput
    val updated = computeStatus()
    _status.value = updated
    return updated
  }
}
