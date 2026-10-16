package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
  IDLE,
  LISTENING,
  PROCESSING,
  SPEAKING,
  ERROR
}

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

  private var speechRecognizer: SpeechRecognizer? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsInitialized = false

  private val _voiceState = MutableStateFlow(VoiceState.IDLE)
  val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

  private val _transcription = MutableStateFlow("")
  val transcription: StateFlow<String> = _transcription.asStateFlow()

  private val _rmsDb = MutableStateFlow(0f)
  val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  var onTranscriptionComplete: ((String) -> Unit)? = null

  init {
    initializeTts()
  }

  private fun initializeTts() {
    textToSpeech = TextToSpeech(context, this)
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsInitialized = true
      textToSpeech?.language = Locale.US
      textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _voiceState.value = VoiceState.SPEAKING
        }

        override fun onDone(utteranceId: String?) {
          _voiceState.value = VoiceState.IDLE
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _voiceState.value = VoiceState.IDLE
        }
      })
    } else {
      _errorMessage.value = "Text-to-Speech initialization failed on device."
    }
  }

  fun setLanguage(locale: Locale) {
    if (isTtsInitialized) {
      textToSpeech?.language = locale
    }
  }

  fun setPitchAndRate(pitch: Float, rate: Float) {
    if (isTtsInitialized) {
      textToSpeech?.setPitch(pitch)
      textToSpeech?.setSpeechRate(rate)
    }
  }

  fun startListening(languageCode: String = "en-US") {
    stopSpeaking()

    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      _errorMessage.value = "Speech recognition service is not available on this device."
      _voiceState.value = VoiceState.ERROR
      return
    }

    speechRecognizer?.destroy()
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
      setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          _voiceState.value = VoiceState.LISTENING
          _transcription.value = ""
          _errorMessage.value = null
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
          _rmsDb.value = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          _voiceState.value = VoiceState.PROCESSING
        }

        override fun onError(error: Int) {
          val errorText = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech recognition error ($error)"
          }
          _errorMessage.value = errorText
          _voiceState.value = VoiceState.ERROR
        }

        override fun onResults(results: Bundle?) {
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val spokenText = matches?.firstOrNull() ?: ""
          _transcription.value = spokenText
          _voiceState.value = VoiceState.IDLE
          if (spokenText.isNotBlank()) {
            onTranscriptionComplete?.invoke(spokenText)
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          matches?.firstOrNull()?.let {
            _transcription.value = it
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
      })
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    speechRecognizer?.startListening(intent)
  }

  fun stopListening() {
    speechRecognizer?.stopListening()
    _voiceState.value = VoiceState.IDLE
  }

  fun speak(text: String) {
    if (!isTtsInitialized) return
    stopListening()
    // Strip markdown formatting for cleaner speech
    val cleanText = text
      .replace(Regex("[#*`_~>\\[\\]]"), "")
      .replace(Regex("http\\S+"), "link")
      .trim()
    textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "nick_speech_${System.currentTimeMillis()}")
  }

  fun stopSpeaking() {
    if (isTtsInitialized) {
      textToSpeech?.stop()
      if (_voiceState.value == VoiceState.SPEAKING) {
        _voiceState.value = VoiceState.IDLE
      }
    }
  }

  fun release() {
    speechRecognizer?.destroy()
    speechRecognizer = null
    textToSpeech?.stop()
    textToSpeech?.shutdown()
    textToSpeech = null
    isTtsInitialized = false
  }
}
