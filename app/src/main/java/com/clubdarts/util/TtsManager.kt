package com.clubdarts.util

import android.content.Context
import android.speech.tts.TextToSpeech
import com.clubdarts.data.model.TtsPhrase
import java.util.Locale

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init() {
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            if (isReady) tts?.language = Locale.UK
        }
    }

    fun announce(
        visitTotal: Int,
        isBust: Boolean,
        isCheckout: Boolean,
        customPhrases: List<TtsPhrase> = emptyList()
    ) {
        if (!isReady) return
        val text = when {
            isCheckout -> "Game shot"
            isBust     -> "Bust"
            customPhrases.isNotEmpty() -> {
                val phrase = customPhrases.random()
                val scoreWord = defaultScoreWord(visitTotal)
                listOf(phrase.before.trim(), scoreWord, phrase.after.trim())
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
            }
            else -> defaultScoreWord(visitTotal)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "visit_$visitTotal")
    }

    private fun defaultScoreWord(visitTotal: Int): String = when {
        visitTotal == 180 -> "One hundred and eighty"
        visitTotal == 100 -> "One hundred"
        visitTotal >= 100 -> "One hundred and ${visitTotal - 100}"
        else              -> visitTotal.toString()
    }

    fun shutdown() { tts?.shutdown(); tts = null; isReady = false }
}
