package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey val id: String,
  val title: String,
  val createdAt: Long = System.currentTimeMillis(),
  val mode: String = "LOCAL_FIRST", // LOCAL_ONLY, LOCAL_FIRST, CLOUD_ONLY, AUTO
  val modelId: String = "tinyllama-1.1b"
)

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey val id: String,
  val conversationId: String,
  val sender: String, // "user", "assistant", "system", "tool"
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val toolCallJson: String? = null,
  val citationsJson: String? = null,
  val isAudioPrompt: Boolean = false
)

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val name: String,
  val type: String, // "zip", "apk", "source_tree", "harmless_sample"
  val path: String,
  val sizeBytes: Long = 0L,
  val analyzedAt: Long = System.currentTimeMillis(),
  val status: String = "IMPORTED", // IMPORTED, ANALYZING, COMPLETED, FAILED
  val summary: String = ""
)

@Entity(tableName = "analysis_jobs")
data class AnalysisJobEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val startedAt: Long = System.currentTimeMillis(),
  val completedAt: Long? = null,
  val status: String = "RUNNING", // RUNNING, COMPLETED, FAILED, CANCELLED
  val adapterUsed: String = "builtin_static", // "rea_termux", "rea_bridge", "rea_remote", "builtin_static"
  val rawOutput: String? = null
)

@Entity(tableName = "findings")
data class FindingEntity(
  @PrimaryKey val id: String,
  val jobId: String,
  val projectId: String,
  val category: String, // "ARCHITECTURE", "SECURITY", "DEPENDENCY", "COMPONENT", "METRICS"
  val severity: String, // "INFO", "WARNING", "HIGH"
  val title: String,
  val description: String,
  val fileLocation: String? = null,
  val evidence: String? = null
)

@Entity(tableName = "tool_executions")
data class ToolExecutionEntity(
  @PrimaryKey val id: String,
  val toolName: String,
  val timestamp: Long = System.currentTimeMillis(),
  val argumentsJson: String,
  val resultJson: String,
  val status: String, // "SUCCESS", "DENIED", "FAILED"
  val durationMs: Long
)
