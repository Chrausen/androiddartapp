package com.clubdarts.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundEffectsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()   // filename -> SoundPool stream ID
    private var throwFileNames: List<String> = emptyList()
    private var isMuted = false
    private var volume = 1f

    companion object {
        private const val CLICK    = "click.wav"
        private const val BUST     = "bust.wav"
        private const val CHECKOUT = "checkout.wav"
    }

    fun init() {
        if (soundPool != null) return   // already initialised (singleton guard)

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()

        soundPool = pool

        // Load named single sounds
        listOf(CLICK, BUST, CHECKOUT).forEach { load(pool, it) }

        // Dynamically load every throw_*.wav found in assets/sounds/
        val throws = context.assets.list("sounds")
            ?.filter { it.startsWith("throw") && it.endsWith(".wav") }
            ?: emptyList()
        throws.forEach { load(pool, it) }
        throwFileNames = throws
    }

    private fun load(pool: SoundPool, fileName: String) {
        try {
            context.assets.openFd("sounds/$fileName").use { fd ->
                soundIds[fileName] = pool.load(fd, 1)
            }
        } catch (_: Exception) {
            soundIds[fileName] = 0   // file absent — silent no-op at play time
        }
    }

    fun playClick()       = play(CLICK)
    fun playBust()        = play(BUST)
    fun playCheckout()    = play(CHECKOUT)
    fun playRandomThrow() {
        if (throwFileNames.isEmpty()) return
        play(throwFileNames.random())
    }

    private fun play(fileName: String) {
        if (isMuted) return
        val id = soundIds[fileName] ?: return
        if (id == 0) return
        soundPool?.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun setMuted(muted: Boolean) { isMuted = muted }
    fun isMuted(): Boolean = isMuted
    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }
    fun getVolume(): Float = volume

    fun shutdown() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        throwFileNames = emptyList()
    }
}
