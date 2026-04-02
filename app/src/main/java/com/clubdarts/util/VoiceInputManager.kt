package com.clubdarts.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.clubdarts.ui.game.DartInput

/**
 * Wraps Android's [SpeechRecognizer] and streams parsed [DartInput]s in real time via
 * partial results, so each recognised dart is delivered the moment it is confirmed rather
 * than waiting for the full utterance to finish.
 *
 * **How streaming works**
 * Android returns cumulative partial results as the user speaks.  We parse the full
 * partial text on every update and emit only the *newly-confirmed* darts (those beyond
 * [processedCount]).  The last parsed dart is always held back on a partial update because
 * the recogniser might still be mid-word (e.g. "double" without the number yet).  It is
 * released when the final result arrives.
 *
 * Example session — user says "double twenty  triple eighteen  miss":
 * ```
 * partial "double"            → parse []        → emit nothing (nothing complete)
 * partial "double twenty"     → parse [D20]     → emit nothing (last dart held back)
 * partial "double twenty tri" → parse [D20]     → emit D20 (confirmed by new token)
 * partial "…triple eighteen"  → parse [D20,T18] → emit T18 held back
 * final   "…miss"             → parse [D20,T18,Miss] → emit Miss (all released)
 * ```
 *
 * **Thread safety**: [startListening] must be called from the main thread.
 * All callbacks are delivered on the main thread.
 *
 * Accepted patterns (case-insensitive):
 * - Plain number 1–20 or 25            → single
 * - "double"/"d" + number or "bull"    → double
 * - "triple"/"treble"/"t" + number     → triple
 * - "bull" / "bullseye"                → double bull (50 pts)
 * - "outer bull"                       → single 25
 * - "miss" / "missed" / "zero" / "out" → miss (0 pts)
 */
class VoiceInputManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    /**
     * Start a recognition session.
     *
     * @param onDart  Called immediately each time a dart is confirmed. May be called
     *                multiple times during the session. Delivered on the main thread.
     * @param onDone  Called exactly once when the session ends (final result, error, or
     *                after [stopListening]). Delivered on the main thread.
     */
    fun startListening(onDart: (DartInput) -> Unit, onDone: () -> Unit) {
        stopListening()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onDone()
            return
        }

        // How many darts from the parsed list have already been delivered upstream.
        var processedCount = 0

        /**
         * Parse [text] and deliver any newly-confirmed darts.
         * [keepLast] = true on partial updates (last dart may still be incomplete).
         * [keepLast] = false on the final result (release everything).
         */
        fun flush(text: String, keepLast: Boolean) {
            val darts = parseVoiceInput(text)
            val deliverUpTo = if (keepLast) darts.size - 1 else darts.size
            for (i in processedCount until deliverUpTo) {
                onDart(darts[i])
            }
            processedCount = maxOf(processedCount, deliverUpTo)
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partial: Bundle?) {
                val text = partial
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                flush(text, keepLast = true)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                flush(text, keepLast = false)
                onDone()
            }

            override fun onError(errorCode: Int) { onDone() }

            // Required by the interface; not needed here
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)   // ← stream partial results
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Give the user time to announce up to three darts
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
                token in MISS_WORDS -> {
                    darts += DartInput(score = 0, multiplier = 0)
                    i++
                }
                token in BULL_WORDS -> {
                    darts += DartInput(score = 25, multiplier = 2)
                    i++
                }
                token == "outer" && i + 1 < tokens.size && tokens[i + 1] in BULL_WORDS -> {
                    darts += DartInput(score = 25, multiplier = 1)
                    i += 2
                }
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
                token in TRIPLE_WORDS -> {
                    if (i + 1 < tokens.size) {
                        val next = tokens[i + 1]
                        if (parseNumber(next)?.let { it in 1..20 } == true) {
                            darts += DartInput(score = parseNumber(next)!!, multiplier = 3)
                            i += 2
                        } else i++
                    } else i++
                }
                else -> {
                    val n = parseNumber(token)
                    when {
                        n == 25                  -> { darts += DartInput(score = 25, multiplier = 1); i++ }
                        n != null && n in 1..20  -> { darts += DartInput(score = n,  multiplier = 1); i++ }
                        else                     -> i++
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
