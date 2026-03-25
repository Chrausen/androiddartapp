package com.clubdarts.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

enum class DartSound(val fileName: String) {
    DART_HIT("dart_hit.wav"),
    BUST("bust.wav"),
    CHECKOUT("checkout.wav")
}

class SoundEffectsService(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<DartSound, Int>()
    private var loadedCount = 0
    private var isMuted = false

    fun init() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attributes)
            .build()

        pool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loadedCount++
        }

        DartSound.entries.forEach { sound ->
            try {
                context.assets.openFd("sounds/${sound.fileName}").use { fd ->
                    soundIds[sound] = pool.load(fd, 1)
                }
            } catch (_: Exception) {
                soundIds[sound] = 0  // file not yet present — skip silently
            }
        }

        soundPool = pool
    }

    fun playSound(sound: DartSound) {
        if (isMuted) return
        val id = soundIds[sound] ?: return
        if (id == 0) return
        soundPool?.play(id, 1f, 1f, 1, 0, 1.0f)
    }

    fun setMuted(muted: Boolean) { isMuted = muted }

    fun shutdown() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        loadedCount = 0
    }
}
