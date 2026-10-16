package com.example.termux

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BridgeHealthResponse(
  @Json(name = "status") val status: String,
  @Json(name = "version") val version: String? = null,
  @Json(name = "uptime") val uptime: Long? = null,
  @Json(name = "node_version") val nodeVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class ReaStatusResponse(
  @Json(name = "status") val status: String,
  @Json(name = "package_version") val packageVersion: String? = null,
  @Json(name = "mcp_active") val mcpActive: Boolean = false,
  @Json(name = "active_jobs") val activeJobs: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReaToolDto(
  @Json(name = "name") val name: String,
  @Json(name = "description") val description: String,
  @Json(name = "schema") val schema: String? = null,
  @Json(name = "safe") val safe: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ReaDoctorResponse(
  @Json(name = "status") val status: String,
  @Json(name = "node_supported") val nodeSupported: Boolean = true,
  @Json(name = "tools") val tools: List<String> = emptyList(),
  @Json(name = "recommendations") val recommendations: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BridgeExecuteRequest(
  @Json(name = "command") val command: String,
  @Json(name = "args") val args: List<String> = emptyList(),
  @Json(name = "cwd") val cwd: String? = null,
  @Json(name = "timeout_ms") val timeoutMs: Long = 10000L
)

@JsonClass(generateAdapter = true)
data class BridgeExecuteResponse(
  @Json(name = "exit_code") val exitCode: Int,
  @Json(name = "stdout") val stdout: String,
  @Json(name = "stderr") val stderr: String = "",
  @Json(name = "duration_ms") val durationMs: Long = 0L
)

@JsonClass(generateAdapter = true)
data class BridgeAnalysisRequest(
  @Json(name = "project_path") val projectPath: String,
  @Json(name = "target_framework") val targetFramework: String = "Kotlin/Compose",
  @Json(name = "options") val options: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class BridgeAnalysisJobResponse(
  @Json(name = "job_id") val jobId: String,
  @Json(name = "status") val status: String, // "QUEUED", "RUNNING", "COMPLETED", "FAILED"
  @Json(name = "progress") val progress: Float = 0f,
  @Json(name = "summary") val summary: String? = null,
  @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BridgeCancelResponse(
  @Json(name = "job_id") val jobId: String,
  @Json(name = "cancelled") val cancelled: Boolean
)
