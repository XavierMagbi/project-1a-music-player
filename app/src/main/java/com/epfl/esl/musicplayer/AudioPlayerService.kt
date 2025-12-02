package com.epfl.esl.musicplayer

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayerService(
    private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun play(@RawRes audioResId: Int) {
        // Libérer les ressources si une autre musique jouait
        releasePlayer()

        mediaPlayer = MediaPlayer.create(context, audioResId).apply {
            setOnCompletionListener {
                // Mettre à jour l'état quand la musique est terminée
                _isPlaying.value = false
            }
        }
        mediaPlayer?.start()
        _isPlaying.value = true
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
    }

    fun resume() {
        // Ne reprendre la lecture que si le lecteur existe
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            _isPlaying.value = true
        }
    }

    // Fonction cruciale pour libérer les ressources système
    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }
}