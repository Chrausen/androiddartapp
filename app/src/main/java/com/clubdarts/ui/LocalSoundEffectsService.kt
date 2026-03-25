package com.clubdarts.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.clubdarts.util.SoundEffectsService

/**
 * Provides [SoundEffectsService] to any composable in the tree.
 * Populated in [com.clubdarts.MainActivity].
 */
val LocalSoundEffectsService = staticCompositionLocalOf<SoundEffectsService?> { null }
