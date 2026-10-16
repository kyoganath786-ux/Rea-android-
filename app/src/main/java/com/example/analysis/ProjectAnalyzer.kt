package com.example.analysis

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ProjectAnalysisSummary(
  val projectId: String,
  val projectName: String,
  val fileType: String,
  val totalFiles: Int,
  val totalLinesOfCode: Int,
  val detectedLanguages: List<String>,
  val detectedArchitecture: String,
  val components: List<String>,
  val securityFindings: List<SecuritySmell>,
  val reconstructionProposal: ReconstructionProposal
)

data class SecuritySmell(
  val title: String,
  val severity: String, // "INFO", "WARNING", "HIGH"
  val description: String,
  val fileLocation: String?,
  val recommendation: String
)

data class ReconstructionProposal(
  val suggestedArchitecture: String,
  val coreModules: List<String>,
  val suggestedDataModels: List<String>,
  val suggestedApiEndpoints: List<String>,
  val implementationPlan: String
)

class ProjectAnalyzer(private val context: Context) {

  private val projectDir = File(context.filesDir, "analyzed_projects")

  init {
    if (!projectDir.exists()) projectDir.mkdirs()
  }

  suspend fun importAndExtractArchive(uri: Uri, displayName: String): Result<File> = withContext(Dispatchers.IO) {
    try {
      val safeName = displayName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
      val destFolder = File(projectDir, "proj_${System.currentTimeMillis()}_$safeName")
      if (!destFolder.exists()) destFolder.mkdirs()

      val destCanonicalPath = destFolder.canonicalPath

      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        if (displayName.endsWith(".zip", ignoreCase = true) || displayName.endsWith(".apk", ignoreCase = true)) {
          ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            var extractedCount = 0
            val maxFiles = 1000 // Guard against zip bombs
            var totalExtractedBytes = 0L
            val maxBytes = 50 * 1024 * 1024L // 50MB limit

            while (entry != null) {
              extractedCount++
              if (extractedCount > maxFiles) {
                throw SecurityException("Archive exceeds maximum allowed file count ($maxFiles)")
              }

              val file = File(destFolder, entry.name)
              // Path traversal defense (ZipSlip prevention)
              val fileCanonicalPath = file.canonicalPath
              if (!fileCanonicalPath.startsWith(destCanonicalPath + File.separator) && fileCanonicalPath != destCanonicalPath) {
                throw SecurityException("Malicious path traversal attempt detected in archive entry: ${entry.name}")
              }

              if (entry.isDirectory) {
                file.mkdirs()
              } else {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { fos ->
                  val buffer = ByteArray(4096)
                  var len: Int
                  while (zis.read(buffer).also { len = it } > 0) {
                    totalExtractedBytes += len
                    if (totalExtractedBytes > maxBytes) {
                      throw SecurityException("Archive uncompressed size exceeds limit (50MB)")
                    }
                    fos.write(buffer, 0, len)
                  }
                }
              }
              zis.closeEntry()
              entry = zis.nextEntry
            }
          }
        } else {
          // Single source/text file import
          val destFile = File(destFolder, safeName)
          FileOutputStream(destFile).use { fos ->
            inputStream.copyTo(fos)
          }
        }
      } ?: return@withContext Result.failure(IllegalStateException("Unable to open stream for URI"))

      Result.success(destFolder)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun analyzeExtractedProject(projectFolder: File, originalName: String): ProjectAnalysisSummary = withContext(Dispatchers.Default) {
    val allFiles = projectFolder.walkTopDown().filter { it.isFile }.toList()
    val totalFiles = allFiles.size
    val languages = mutableSetOf<String>()
    var totalLoc = 0
    val securityFindings = mutableListOf<SecuritySmell>()
    val components = mutableListOf<String>()

    val isApk = originalName.endsWith(".apk", ignoreCase = true)

    if (isApk) {
      // Static APK analysis
      components.add("APK Package Container")
      val dexFiles = allFiles.filter { it.name.endsWith(".dex") }
      components.add("DEX Classes (${dexFiles.size} dex binaries)")

      // Check for AndroidManifest.xml presence
      val manifestFile = allFiles.find { it.name.equals("AndroidManifest.xml", ignoreCase = true) }
      if (manifestFile != null) {
        components.add("Binary AndroidManifest")
        securityFindings.add(
          SecuritySmell(
            title = "Static Analysis Only",
            severity = "INFO",
            description = "APK contents are analyzed statically. Untrusted code execution is prohibited.",
            fileLocation = "AndroidManifest.xml",
            recommendation = "Review exported components, permissions, and network security configuration."
          )
        )
      }

      // Check for assets and native libraries
      val nativeLibs = allFiles.filter { it.name.endsWith(".so") }
      if (nativeLibs.isNotEmpty()) {
        components.add("Native Libraries (${nativeLibs.size} .so files)")
        securityFindings.add(
          SecuritySmell(
            title = "Native Binaries Present",
            severity = "WARNING",
            description = "APK contains compiled native ELF libraries (.so) which cannot run directly in standard Termux without compatibility layer.",
            fileLocation = "lib/",
            recommendation = "Use static disassembler or remote REA analysis for ARM/x86 native symbols."
          )
        )
      }
    } else {
      // Source code tree analysis
      allFiles.forEach { file ->
        val ext = file.extension.lowercase()
        when (ext) {
          "kt", "kts" -> languages.add("Kotlin")
          "java" -> languages.add("Java")
          "js", "jsx" -> languages.add("JavaScript")
          "ts", "tsx" -> languages.add("TypeScript")
          "py" -> languages.add("Python")
          "json" -> languages.add("JSON")
          "xml" -> languages.add("XML")
          "c", "cpp", "h" -> languages.add("C/C++")
          "rs" -> languages.add("Rust")
        }

        // Count lines of code for text files under 2MB
        if (file.length() < 2 * 1024 * 1024) {
          try {
            val lines = file.readLines()
            totalLoc += lines.size

            // Fast static security scans
            lines.forEachIndexed { index, line ->
              val lineLower = line.lowercase()
              if (lineLower.contains("api_key") || lineLower.contains("apikey") || lineLower.contains("password = \"")) {
                if (!lineLower.contains("buildconfig") && !lineLower.contains("getval") && !lineLower.contains("placeholder") && line.contains("=")) {
                  securityFindings.add(
                    SecuritySmell(
                      title = "Potential Hardcoded Secret",
                      severity = "HIGH",
                      description = "Found credential assignment in source code.",
                      fileLocation = "${file.name}:${index + 1}",
                      recommendation = "Extract credentials to Android Keystore, BuildConfig, or AI Studio Secrets panel."
                    )
                  )
                }
              }
              if (lineLower.contains("android:debuggable=\"true\"")) {
                securityFindings.add(
                  SecuritySmell(
                    title = "Debuggable Flag Enabled",
                    severity = "HIGH",
                    description = "Application manifest has android:debuggable enabled.",
                    fileLocation = "${file.name}:${index + 1}",
                    recommendation = "Set android:debuggable=\"false\" for production release."
                  )
                )
              }
              if (lineLower.contains("usescleartexttraffic=\"true\"")) {
                securityFindings.add(
                  SecuritySmell(
                    title = "Cleartext HTTP Traffic Allowed",
                    severity = "WARNING",
                    description = "Manifest permits unencrypted HTTP transmission.",
                    fileLocation = "${file.name}:${index + 1}",
                    recommendation = "Enforce HTTPS with networkSecurityConfig."
                  )
                )
              }
            }
          } catch (_: Exception) {}
        }
      }
    }

    val architecture = when {
      allFiles.any { it.name.contains("ViewModel") || it.name.contains("Compose") } -> "MVVM (Jetpack Compose)"
      allFiles.any { it.name.contains("Activity") || it.name.contains("Fragment") } -> "Android Standard / MVP"
      allFiles.any { it.name.contains("package.json") } -> "Node.js / Full-stack"
      allFiles.any { it.name.contains("requirements.txt") || it.name.contains("pyproject.toml") } -> "Python Module"
      else -> "Modular Source Tree"
    }

    val proposal = ReconstructionProposal(
      suggestedArchitecture = architecture,
      coreModules = listOf("core-domain", "data-persistence", "network-api", "ui-presentation"),
      suggestedDataModels = listOf("UserSession", "AnalyzedProject", "SecurityReport", "ExecutionAuditLog"),
      suggestedApiEndpoints = listOf("GET /api/v1/projects", "POST /api/v1/analyze", "GET /api/v1/findings"),
      implementationPlan = """
        1. Setup Kotlin Gradle build with Android Jetpack Compose and Room.
        2. Define domain entities with validation and path traversal defenses.
        3. Implement UI state containers using StateFlow and M3 components.
        4. Integrate secure local tool-calling pipeline with user confirmation.
      """.trimIndent()
    )

    ProjectAnalysisSummary(
      projectId = projectFolder.name,
      projectName = originalName,
      fileType = if (isApk) "Android APK" else "Source Archive",
      totalFiles = totalFiles,
      totalLinesOfCode = totalLoc,
      detectedLanguages = languages.toList().ifEmpty { listOf(if (isApk) "Bytecode (DEX)" else "Unknown") },
      detectedArchitecture = architecture,
      components = components.ifEmpty { listOf("App Module", "Data Layer", "UI Layer") },
      securityFindings = securityFindings,
      reconstructionProposal = proposal
    )
  }

  fun generateReconstructionSpec(summary: ProjectAnalysisSummary): String {
    return """
      # Project Reconstruction Blueprint: ${summary.projectName}
      
      ## 1. Executive Summary
      - Target Architecture: ${summary.detectedArchitecture}
      - Detected Languages: ${summary.detectedLanguages.joinToString(", ")}
      - Total Files: ${summary.totalFiles} | Total LOC: ${summary.totalLinesOfCode}
      
      ## 2. Core Modules
      ${summary.reconstructionProposal.coreModules.joinToString("\n") { "- `$it`" }}
      
      ## 3. Data Models
      ${summary.reconstructionProposal.suggestedDataModels.joinToString("\n") { "```kotlin\ndata class $it(\n  val id: String,\n  val timestamp: Long = System.currentTimeMillis()\n)\n```" }}
      
      ## 4. Security Mitigations
      ${if (summary.securityFindings.isEmpty()) "- No critical security violations detected." else summary.securityFindings.joinToString("\n") { "- [${it.severity}] ${it.title}: ${it.recommendation}" }}
      
      ## 5. Build & Test Instructions
      ```bash
      # Build project locally
      gradle build
      gradle testDebugUnitTest
      ```
    """.trimIndent()
  }
}
