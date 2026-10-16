package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nick_ai_settings")

enum class AiMode {
  LOCAL_ONLY,
  LOCAL_FIRST,
  CLOUD_ONLY,
  AUTO
}

data class UserPreferences(
  val aiMode: AiMode = AiMode.LOCAL_FIRST,
  val activeLocalModelId: String = "tinyllama-1.1b",
  val cloudProvider: String = "gemini",
  val cloudEndpoint: String = "https://generativelanguage.googleapis.com",
  val cloudApiKey: String = "",
  val cloudConsentGranted: Boolean = false,
  val termuxBridgeUrl: String = "http://127.0.0.1:8765",
  val termuxBridgeToken: String = "",
  val remoteReaUrl: String = "",
  val remoteReaToken: String = "",
  val ttsAutoSpeak: Boolean = true,
  val ttsLanguage: String = "en-US",
  val ttsPitch: Float = 1.0f,
  val ttsSpeechRate: Float = 1.0f,
  val requireToolConfirmation: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

  private object PreferencesKeys {
    val AI_MODE = stringPreferencesKey("ai_mode")
    val ACTIVE_LOCAL_MODEL = stringPreferencesKey("active_local_model")
    val CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
    val CLOUD_ENDPOINT = stringPreferencesKey("cloud_endpoint")
    val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
    val CLOUD_CONSENT = booleanPreferencesKey("cloud_consent_granted")
    val TERMUX_BRIDGE_URL = stringPreferencesKey("termux_bridge_url")
    val TERMUX_BRIDGE_TOKEN = stringPreferencesKey("termux_bridge_token")
    val REMOTE_REA_URL = stringPreferencesKey("remote_rea_url")
    val REMOTE_REA_TOKEN = stringPreferencesKey("remote_rea_token")
    val TTS_AUTO_SPEAK = booleanPreferencesKey("tts_auto_speak")
    val TTS_LANGUAGE = stringPreferencesKey("tts_language")
    val TTS_PITCH = floatPreferencesKey("tts_pitch")
    val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
    val REQUIRE_TOOL_CONFIRM = booleanPreferencesKey("require_tool_confirm")
  }

  val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
    val modeStr = preferences[PreferencesKeys.AI_MODE] ?: AiMode.LOCAL_FIRST.name
    val aiMode = try {
      AiMode.valueOf(modeStr)
    } catch (_: Exception) {
      AiMode.LOCAL_FIRST
    }

    UserPreferences(
      aiMode = aiMode,
      activeLocalModelId = preferences[PreferencesKeys.ACTIVE_LOCAL_MODEL] ?: "tinyllama-1.1b",
      cloudProvider = preferences[PreferencesKeys.CLOUD_PROVIDER] ?: "gemini",
      cloudEndpoint = preferences[PreferencesKeys.CLOUD_ENDPOINT] ?: "https://generativelanguage.googleapis.com",
      cloudApiKey = preferences[PreferencesKeys.CLOUD_API_KEY] ?: "",
      cloudConsentGranted = preferences[PreferencesKeys.CLOUD_CONSENT] ?: false,
      termuxBridgeUrl = preferences[PreferencesKeys.TERMUX_BRIDGE_URL] ?: "http://127.0.0.1:8765",
      termuxBridgeToken = preferences[PreferencesKeys.TERMUX_BRIDGE_TOKEN] ?: "",
      remoteReaUrl = preferences[PreferencesKeys.REMOTE_REA_URL] ?: "",
      remoteReaToken = preferences[PreferencesKeys.REMOTE_REA_TOKEN] ?: "",
      ttsAutoSpeak = preferences[PreferencesKeys.TTS_AUTO_SPEAK] ?: true,
      ttsLanguage = preferences[PreferencesKeys.TTS_LANGUAGE] ?: "en-US",
      ttsPitch = preferences[PreferencesKeys.TTS_PITCH] ?: 1.0f,
      ttsSpeechRate = preferences[PreferencesKeys.TTS_SPEECH_RATE] ?: 1.0f,
      requireToolConfirmation = preferences[PreferencesKeys.REQUIRE_TOOL_CONFIRM] ?: true
    )
  }

  suspend fun updateAiMode(mode: AiMode) {
    context.dataStore.edit { it[PreferencesKeys.AI_MODE] = mode.name }
  }

  suspend fun updateActiveLocalModel(modelId: String) {
    context.dataStore.edit { it[PreferencesKeys.ACTIVE_LOCAL_MODEL] = modelId }
  }

  suspend fun updateCloudSettings(provider: String, endpoint: String, apiKey: String) {
    context.dataStore.edit {
      it[PreferencesKeys.CLOUD_PROVIDER] = provider
      it[PreferencesKeys.CLOUD_ENDPOINT] = endpoint
      it[PreferencesKeys.CLOUD_API_KEY] = apiKey
    }
  }

  suspend fun setCloudConsent(granted: Boolean) {
    context.dataStore.edit { it[PreferencesKeys.CLOUD_CONSENT] = granted }
  }

  suspend fun updateTermuxBridge(url: String, token: String) {
    context.dataStore.edit {
      it[PreferencesKeys.TERMUX_BRIDGE_URL] = url
      it[PreferencesKeys.TERMUX_BRIDGE_TOKEN] = token
    }
  }

  suspend fun updateRemoteRea(url: String, token: String) {
    context.dataStore.edit {
      it[PreferencesKeys.REMOTE_REA_URL] = url
      it[PreferencesKeys.REMOTE_REA_TOKEN] = token
    }
  }

  suspend fun updateTtsSettings(autoSpeak: Boolean, language: String, pitch: Float, speechRate: Float) {
    context.dataStore.edit {
      it[PreferencesKeys.TTS_AUTO_SPEAK] = autoSpeak
      it[PreferencesKeys.TTS_LANGUAGE] = language
      it[PreferencesKeys.TTS_PITCH] = pitch
      it[PreferencesKeys.TTS_SPEECH_RATE] = speechRate
    }
  }

  suspend fun updateRequireToolConfirmation(require: Boolean) {
    context.dataStore.edit { it[PreferencesKeys.REQUIRE_TOOL_CONFIRM] = require }
  }
}
