package com.clubdarts.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.clubdarts.ui.game.DartInput

/**
 * Wraps Android's [SpeechRecognizer] to capture a short voice utterance and parse it into
 * up to three [DartInput]s.
 *
 * **Thread safety**: [startListening] must be called from the main thread (SpeechRecognizer
 * requirement). The [onResult] callback is delivered on the main thread.
 *
 * Accepted voice patterns (case-insensitive):
 * - Plain number 1–20 or "twenty-five" / "25"  → single
 * - "double <number>" / "d <number>"            → double
 * - "triple <number>" / "treble <number>"       → triple
 * - "bull" / "bullseye"                         → double bull (50 pts)
 * - "outer bull"                                → single 25
 * - "miss" / "missed" / "zero" / "out"          → miss (0 pts)
 */
class VoiceInputManager(private val context: Context) {

    sealed class Result {
        /** Successfully parsed darts (list may be empty if nothing was understood). */
        data class Darts(val darts: List<DartInput>) : Result()
        /** SpeechRecognizer error code, or -1 when recognition is not available. */
        data class Failure(val errorCode: Int) : Result()
    }

    private var recognizer: SpeechRecognizer? = null

    /**
     * Start a single recognition session. [onResult] is called exactly once on the main thread.
     * Call [stopListening] to cancel early.
     */
    fun startListening(onResult: (Result) -> Unit) {
        stopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onResult(Result.Failure(-1))
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            private var delivered = false

            private fun deliver(r: Result) {
                if (!delivered) {
                    delivered = true
                    onResult(r)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                deliver(Result.Darts(parseVoiceInput(text)))
            }

            override fun onError(errorCode: Int) = deliver(Result.Failure(errorCode))

            // Unused callbacks required by the interface
            override fun onPartialResults(partial: Bundle?) {}
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Give the user time to say all three darts before silence detection fires
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0L)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    internal fun parseVoiceInput(raw: String): List<DartInput> {
        val tokens = raw.lowercase()
            .replace("-", " ")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }

        val darts = mutableListOf<DartInput>()
        var i = 0
        while (i < tokens.size && darts.size < 3) {
            val token = tokens[i]
            when {
                // Miss
                token in MISS_WORDS -> {
                    darts += DartInput(score = 0, multiplier = 0)
                    i++
                }
                // Bull / bullseye → double bull (50 pts)
                token in BULL_WORDS -> {
                    darts += DartInput(score = 25, multiplier = 2)
                    i++
                }
                // "outer bull" → single 25
                token == "outer" && i + 1 < tokens.size && tokens[i + 1] in BULL_WORDS -> {
                    darts += DartInput(score = 25, multiplier = 1)
                    i += 2
                }
                // "double <number|bull>"
                token in DOUBLE_WORDS -> {
                    if (i + 1 < tokens.size) {
                        val next = tokens[i + 1]
                        when {
                            next in BULL_WORDS -> {
                                darts += DartInput(score = 25, multiplier = 2)
                                i += 2
                            }
                            parseNumber(next)?.let { it in 1..20 } == true -> {
                                darts += DartInput(score = parseNumber(next)!!, multiplier = 2)
                                i += 2
                            }
                            else -> i++
                        }
                    } else i++
                }
                // "triple <number>"
                token in TRIPLE_WORDS -> {
                    if (i + 1 < tokens.size) {
                        val next = tokens[i + 1]
                        if (parseNumber(next)?.let { it in 1..20 } == true) {
                            darts += DartInput(score = parseNumber(next)!!, multiplier = 3)
                            i += 2
                        } else i++
                    } else i++
                }
                // Plain number
                else -> {
                    val n = parseNumber(token)
                    when {
                        n == 25              -> { darts += DartInput(score = 25, multiplier = 1); i++ }
                        n != null && n in 1..20 -> { darts += DartInput(score = n, multiplier = 1); i++ }
                        else                 -> i++
                    }
                }
            }
        }
        return darts
    }

    private fun parseNumber(token: String): Int? = token.toIntOrNull() ?: WORD_TO_NUMBER[token]

    companion object {
        private val MISS_WORDS   = setOf("miss", "missed", "zero", "out", "bounce", "bounceout")
        private val BULL_WORDS   = setOf("bull", "bulls", "bullseye")
        private val DOUBLE_WORDS = setOf("double", "d")
        private val TRIPLE_WORDS = setOf("triple", "treble", "t")

        private val WORD_TO_NUMBER = mapOf(
            "one" to 1,        "two" to 2,        "three" to 3,      "four" to 4,
            "five" to 5,       "six" to 6,         "seven" to 7,      "eight" to 8,
            "nine" to 9,       "ten" to 10,        "eleven" to 11,    "twelve" to 12,
            "thirteen" to 13,  "fourteen" to 14,   "fifteen" to 15,   "sixteen" to 16,
            "seventeen" to 17, "eighteen" to 18,   "nineteen" to 19,  "twenty" to 20,
            "twentyfive" to 25
        )
    }
}
