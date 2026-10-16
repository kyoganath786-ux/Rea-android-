package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
  @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun getConversationById(id: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteConversation(id: String)

  @Query("DELETE FROM conversations")
  suspend fun clearAllConversations()

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)
}

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY analyzedAt DESC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: String): ProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: ProjectEntity)

  @Update
  suspend fun updateProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProject(id: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAnalysisJob(job: AnalysisJobEntity)

  @Query("SELECT * FROM analysis_jobs WHERE projectId = :projectId ORDER BY startedAt DESC")
  fun getJobsForProject(projectId: String): Flow<List<AnalysisJobEntity>>

  @Query("SELECT * FROM findings WHERE projectId = :projectId ORDER BY severity DESC")
  fun getFindingsForProject(projectId: String): Flow<List<FindingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFindings(findings: List<FindingEntity>)

  @Query("DELETE FROM findings WHERE projectId = :projectId")
  suspend fun deleteFindingsForProject(projectId: String)
}

@Dao
interface ToolDao {
  @Query("SELECT * FROM tool_executions ORDER BY timestamp DESC LIMIT 100")
  fun getAllExecutions(): Flow<List<ToolExecutionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExecution(execution: ToolExecutionEntity)

  @Query("DELETE FROM tool_executions")
  suspend fun clearHistory()
}
