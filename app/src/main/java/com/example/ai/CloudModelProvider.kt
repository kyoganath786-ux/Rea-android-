package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CloudConfig(
  val provider: String = "gemini", // "gemini", "openai_compatible"
  val endpointUrl: String = "https://generativelanguage.googleapis.com",
  val model: String = "gemini-2.5-flash",
  val customApiKey: String = "",
  val consentGranted: Boolean = false
)

interface CloudModelProvider {
  fun isConfigured(config: CloudConfig): Boolean
  suspend fun testConnection(config: CloudConfig): Result<String>
  fun generateStreaming(prompt: String, config: CloudConfig): Flow<String>
}

class CloudModelProviderImpl : CloudModelProvider {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  private fun resolveApiKey(config: CloudConfig): String {
    if (config.customApiKey.isNotBlank()) return config.customApiKey.trim()
    // Check injected BuildConfig if present
    return try {
      val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
      val key = field.get(null) as? String ?: ""
      key
    } catch (_: Exception) {
      ""
    }
  }

  override fun isConfigured(config: CloudConfig): Boolean {
    val key = resolveApiKey(config)
    return key.isNotBlank() && config.consentGranted
  }

  override suspend fun testConnection(config: CloudConfig): Result<String> {
    if (!config.consentGranted) {
      return Result.failure(SecurityException("Cloud data transfer consent not granted. Please review and approve in Settings."))
    }
    val apiKey = resolveApiKey(config)
    if (apiKey.isBlank()) {
      return Result.failure(IllegalArgumentException("No API key configured. Provide an API key in Settings or AI Studio Secrets."))
    }

    return try {
      if (config.provider == "gemini") {
        val testUrl = "${config.endpointUrl.trimEnd('/')}/v1beta/models/${config.model}:generateContent?key=$apiKey"
        val jsonPayload = JSONObject().apply {
          put("contents", JSONArray().apply {
            put(JSONObject().apply {
              put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", "Respond with 'PONG'") })
              })
            })
          })
        }
        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(testUrl).post(body).build()

        client.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            Result.success("Connected successfully to Gemini API (${config.model})")
          } else {
            val errorBody = response.body?.string() ?: ""
            Result.failure(IOException("HTTP ${response.code}: $errorBody"))
          }
        }
      } else {
        // OpenAI-compatible endpoint
        val testUrl = "${config.endpointUrl.trimEnd('/')}/v1/chat/completions"
        val jsonPayload = JSONObject().apply {
          put("model", config.model)
          put("messages", JSONArray().apply {
            put(JSONObject().apply {
              put("role", "user")
              put("content", "Ping")
            })
          })
          put("max_tokens", 5)
        }
        val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(testUrl)
          .header("Authorization", "Bearer $apiKey")
          .post(body)
          .build()

        client.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            Result.success("Connected to ${config.provider} (${config.model})")
          } else {
            Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
          }
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override fun generateStreaming(prompt: String, config: CloudConfig): Flow<String> = flow {
    if (!config.consentGranted) {
      throw SecurityException("Cloud data transfer consent not granted. Please enable cloud consent in Settings.")
    }
    val apiKey = resolveApiKey(config)
    if (apiKey.isBlank()) {
      throw IllegalArgumentException("No Cloud API key provided.")
    }

    val url = if (config.provider == "gemini") {
      "${config.endpointUrl.trimEnd('/')}/v1beta/models/${config.model}:generateContent?key=$apiKey"
    } else {
      "${config.endpointUrl.trimEnd('/')}/v1/chat/completions"
    }

    try {
      if (config.provider == "gemini") {
        val payload = JSONObject().apply {
          put("contents", JSONArray().apply {
            put(JSONObject().apply {
              put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
              })
            })
          })
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
          throw IOException("Gemini API error ${response.code}: ${response.body?.string()}")
        }

        val raw = response.body?.string() ?: ""
        val json = JSONObject(raw)
        val text = json.optJSONArray("candidates")
          ?.optJSONObject(0)
          ?.optJSONObject("content")
          ?.optJSONArray("parts")
          ?.optJSONObject(0)
          ?.optString("text") ?: "No response from Gemini API"

        // Simulate streaming delivery for smooth UI rendering
        val words = text.split(" ")
        for (word in words) {
          emit("$word ")
          delay(20)
        }
      } else {
        val payload = JSONObject().apply {
          put("model", config.model)
          put("messages", JSONArray().apply {
            put(JSONObject().apply {
              put("role", "user")
              put("content", prompt)
            })
          })
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(url)
          .header("Authorization", "Bearer $apiKey")
          .post(body)
          .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
          throw IOException("API error ${response.code}: ${response.body?.string()}")
        }

        val raw = response.body?.string() ?: ""
        val json = JSONObject(raw)
        val text = json.optJSONArray("choices")
          ?.optJSONObject(0)
          ?.optJSONObject("message")
          ?.optString("content") ?: "No content"

        val words = text.split(" ")
        for (word in words) {
          emit("$word ")
          delay(20)
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      throw e
    }
  }.flowOn(Dispatchers.IO)
}
