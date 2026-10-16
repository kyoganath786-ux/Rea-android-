package com.example.termux

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TermuxBridgeService {

  @GET("api/health")
  suspend fun getHealth(): BridgeHealthResponse

  @GET("api/rea/status")
  suspend fun getReaStatus(): ReaStatusResponse

  @GET("api/rea/tools")
  suspend fun getDiscoveredTools(): List<ReaToolDto>

  @POST("api/rea/doctor")
  suspend fun runDiagnostics(): ReaDoctorResponse

  @POST("api/rea/execute")
  suspend fun executeCommand(@Body request: BridgeExecuteRequest): BridgeExecuteResponse

  @POST("api/rea/analyze")
  suspend fun startAnalysis(@Body request: BridgeAnalysisRequest): BridgeAnalysisJobResponse

  @GET("api/rea/analysis/{jobId}")
  suspend fun getAnalysisJob(@Path("jobId") jobId: String): BridgeAnalysisJobResponse

  @POST("api/rea/analysis/{jobId}/cancel")
  suspend fun cancelAnalysis(@Path("jobId") jobId: String): BridgeCancelResponse
}
