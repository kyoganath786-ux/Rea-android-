package com.example.termux

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class TermuxBridgeManager(
  private var baseUrl: String = "http://127.0.0.1:8765/",
  initialToken: String? = null
) {

  private var activeToken: String = initialToken ?: resolveDefaultToken()
  private var service: TermuxBridgeService = createService(baseUrl, activeToken)

  companion object {
    const val DEFAULT_LOOPBACK_URL = "http://127.0.0.1:8765/"
    const val DEFAULT_FALLBACK_TOKEN = "nick-local-sec"

    private fun resolveDefaultToken(): String {
      // Check BuildConfig for injected TERMUX_BRIDGE_TOKEN
      return try {
        val field = BuildConfig::class.java.getField("TERMUX_BRIDGE_TOKEN")
        val token = field.get(null) as? String
        if (!token.isNullOrBlank()) token.trim() else DEFAULT_FALLBACK_TOKEN
      } catch (_: Exception) {
        DEFAULT_FALLBACK_TOKEN
      }
    }
  }

  private fun createService(url: String, token: String): TermuxBridgeService {
    val normalizedUrl = if (url.endsWith("/")) url else "$url/"

    val authInterceptor = Interceptor { chain ->
      val original = chain.request()
      val requestBuilder = original.newBuilder()
      if (token.isNotBlank()) {
        requestBuilder.header("Authorization", "Bearer $token")
      }
      requestBuilder.header("X-Requested-By", "NICK-AI-Android")
      requestBuilder.header("Accept", "application/json")
      chain.proceed(requestBuilder.build())
    }

    val loggingInterceptor = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(loggingInterceptor)
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .build()

    val moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(normalizedUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()

    return retrofit.create(TermuxBridgeService::class.java)
  }

  @Synchronized
  fun updateEndpoint(newBaseUrl: String, newToken: String? = null) {
    this.baseUrl = newBaseUrl
    if (newToken != null) {
      this.activeToken = newToken.trim()
    }
    this.service = createService(this.baseUrl, this.activeToken)
  }

  fun getToken(): String = activeToken

  fun getBaseUrl(): String = baseUrl

  fun isConfigured(): Boolean = baseUrl.isNotBlank() && activeToken.isNotBlank()

  suspend fun checkHealth(): Result<BridgeHealthResponse> = withContext(Dispatchers.IO) {
    try {
      val response = service.getHealth()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to connect to Termux bridge at $baseUrl: ${e.message}", e))
    }
  }

  suspend fun getReaStatus(): Result<ReaStatusResponse> = withContext(Dispatchers.IO) {
    try {
      val response = service.getReaStatus()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to retrieve REA status from bridge: ${e.message}", e))
    }
  }

  suspend fun discoverTools(): Result<List<ReaToolDto>> = withContext(Dispatchers.IO) {
    try {
      val tools = service.getDiscoveredTools()
      Result.success(tools)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to discover REA tools: ${e.message}", e))
    }
  }

  suspend fun runDoctor(): Result<ReaDoctorResponse> = withContext(Dispatchers.IO) {
    try {
      val response = service.runDiagnostics()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Doctor diagnostics request failed: ${e.message}", e))
    }
  }

  suspend fun executeCommand(
    command: String,
    args: List<String> = emptyList(),
    cwd: String? = null
  ): Result<BridgeExecuteResponse> = withContext(Dispatchers.IO) {
    // Security check: Guard against arbitrary malicious shell commands
    val cleanCmd = command.trim()
    val allowedPrefixes = listOf("node", "npm", "npx", "pkg", "git", "rea")
    val isPermitted = allowedPrefixes.any { cleanCmd.startsWith(it) } &&
      !cleanCmd.contains("rm -rf /") &&
      !cleanCmd.contains(":(){ :|:& };:") &&
      !cleanCmd.contains("> /dev/null")

    if (!isPermitted) {
      return@withContext Result.failure(
        SecurityException("Command '$cleanCmd' rejected by security policy. Only verified REA and Node CLI operations are permitted.")
      )
    }

    try {
      val request = BridgeExecuteRequest(
        command = cleanCmd,
        args = args,
        cwd = cwd
      )
      val response = service.executeCommand(request)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Bridge command execution failed: ${e.message}", e))
    }
  }

  suspend fun startAnalysis(
    projectPath: String,
    targetFramework: String = "Kotlin/Compose"
  ): Result<BridgeAnalysisJobResponse> = withContext(Dispatchers.IO) {
    try {
      val request = BridgeAnalysisRequest(
        projectPath = projectPath,
        targetFramework = targetFramework
      )
      val response = service.startAnalysis(request)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to start analysis job on bridge: ${e.message}", e))
    }
  }

  suspend fun getAnalysisProgress(jobId: String): Result<BridgeAnalysisJobResponse> = withContext(Dispatchers.IO) {
    try {
      val response = service.getAnalysisJob(jobId)
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to fetch analysis job status ($jobId): ${e.message}", e))
    }
  }

  suspend fun cancelAnalysis(jobId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val response = service.cancelAnalysis(jobId)
      Result.success(response.cancelled)
    } catch (e: Exception) {
      Result.failure(IOException("Failed to cancel analysis job ($jobId): ${e.message}", e))
    }
  }
}
