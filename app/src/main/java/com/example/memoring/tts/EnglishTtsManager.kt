package com.example.memoring.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed interface TtsState {
    data object Initializing : TtsState
    data object Ready : TtsState
    data class Error(val message: String) : TtsState
}

class EnglishTtsManager(context: Context) {
    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    val state: StateFlow<TtsState> = _state.asStateFlow()
    private var released = false
    private var tts: TextToSpeech? = null

    init {
        // Activity를 보관하지 않도록 applicationContext만 캡처한다.
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { result ->
            val engine = tts
            if (result != TextToSpeech.SUCCESS || engine == null) {
                _state.value = TtsState.Error("TTS 초기화에 실패했습니다.")
            } else {
                val us = engine.setLanguage(Locale.US)
                val languageResult = if (unsupported(us)) engine.setLanguage(Locale.ENGLISH) else us
                _state.value = if (unsupported(languageResult))
                    TtsState.Error("기기에서 영어 TTS 언어를 지원하지 않습니다.")
                else TtsState.Ready
            }
        }
    }

    fun speakWord(word: String) = speak(word)
    fun speakSentence(sentence: String) = speak(sentence)

    private fun speak(text: String) {
        val value = text.trim()
        if (value.isEmpty() || released || _state.value !is TtsState.Ready) return
        tts?.speak(value, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() { tts?.stop() }
    fun release() {
        if (released) return
        released = true
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun unsupported(result: Int) =
        result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED
}
