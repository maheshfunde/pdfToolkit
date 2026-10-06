package com.yashodatech.pdftoolkit.pdf

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

enum class TtsAccent(val label: String, val shortName: String, val locale: Locale) {
    INDIAN_ENGLISH("Indian (English)", "🇮🇳 Indian", Locale("en", "IN")),
    INDIAN_HINDI("Hindi (India)", "🇮🇳 Hindi", Locale("hi", "IN")),
    SYSTEM_DEFAULT("System Default", "🌐 Default", Locale.getDefault())
}

class PdfTtsManager(
    private val context: Context,
    private val onStateChange: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set
    var isSpeaking = false
        private set
    var currentSpeed: Float = 1.0f
        private set
    var currentAccent: TtsAccent = TtsAccent.INDIAN_ENGLISH
        private set
    var currentVoiceName: String? = null
        private set
    var currentText: String? = null
        private set

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyAccentAndVoice(currentAccent)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onStateChange()
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    currentText = null
                    onStateChange()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    currentText = null
                    onStateChange()
                }
            })
            onStateChange()
        }
    }

    private fun applyAccentAndVoice(accent: TtsAccent) {
        val targetLocale = accent.locale
        try {
            tts?.language = targetLocale

            // Query voices for natural Indian inflection
            val allVoices = tts?.voices
            if (!allVoices.isNullOrEmpty()) {
                val matchingVoices = allVoices.filter { voice ->
                    voice.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    (targetLocale.country.isBlank() || voice.locale.country.equals(targetLocale.country, ignoreCase = true)) &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }

                // Pick neural / high quality voice (network voices on Pixel 7 are natural WaveNet models)
                val bestVoice = matchingVoices.maxWithOrNull(
                    compareBy<Voice> { it.quality }
                        .thenBy { if (it.name.contains("network", ignoreCase = true)) 1 else 0 }
                        .thenBy { if (!it.name.contains("local", ignoreCase = true)) 1 else 0 }
                ) ?: matchingVoices.firstOrNull()

                if (bestVoice != null) {
                    tts?.voice = bestVoice
                    currentVoiceName = bestVoice.name
                }
            }
        } catch (_: Exception) {}

        tts?.setPitch(1.0f)
        tts?.setSpeechRate(currentSpeed)
    }

    fun speak(text: String, utteranceId: String = "pdf_tts_page") {
        if (!isInitialized || text.isBlank()) return
        currentText = text
        tts?.setSpeechRate(currentSpeed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        isSpeaking = true
        onStateChange()
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        currentText = null
        onStateChange()
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        tts?.setSpeechRate(speed)
        // If already speaking, immediately restart so speed change is audible in real-time
        val textToResume = currentText
        if (isSpeaking && !textToResume.isNullOrBlank()) {
            tts?.stop()
            tts?.setSpeechRate(speed)
            tts?.speak(textToResume, TextToSpeech.QUEUE_FLUSH, null, "pdf_tts_page")
        }
        onStateChange()
    }

    fun setAccent(accent: TtsAccent) {
        currentAccent = accent
        applyAccentAndVoice(accent)
        val textToResume = currentText
        if (isSpeaking && !textToResume.isNullOrBlank()) {
            tts?.stop()
            tts?.setSpeechRate(currentSpeed)
            tts?.speak(textToResume, TextToSpeech.QUEUE_FLUSH, null, "pdf_tts_page")
        }
        onStateChange()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
        isInitialized = false
        currentText = null
    }
}
