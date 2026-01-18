package com.epfl.esl.musicplayer

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.util.Log

class EqualizerViewModel(
    application: Application
): AndroidViewModel(application) {

    private val _subBassLevel = MutableLiveData(0f)
    val subBassLevel: LiveData<Float> = _subBassLevel

    private val _bassLevel = MutableLiveData(0f)
    val bassLevel: LiveData<Float> = _bassLevel

    private val _midrangeLevel = MutableLiveData(0f)
    val midrangeLevel: LiveData<Float> = _midrangeLevel

    private val _upperMidLevel = MutableLiveData(0f)
    val upperMidLevel: LiveData<Float> = _upperMidLevel

    private val _trebleLevel = MutableLiveData(0f)
    val trebleLevel: LiveData<Float> = _trebleLevel

    private var currentEqualizer: Equalizer? = null

    // For pads
    private val appContext = application.applicationContext
    private var padSoundPlayer: MediaPlayer? = null

    fun setEqualizer(audioSessionId: Int) {
        try {
            currentEqualizer = Equalizer(0, audioSessionId).apply { enabled = true }

            // Apply old settings to new equalizer
            applyCurrentSettings()
        } catch (e: Exception) {
            currentEqualizer = null
        }
    }

    private fun applyCurrentSettings() {
        currentEqualizer?.let {
            it.setBandLevel(0, subBassLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(1, bassLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(2, midrangeLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(3, upperMidLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(4, trebleLevel.value?.toInt()?.toShort() ?: 0)
        }
    }

    fun updateSubBassLevel(newLevel: Float) {
        _subBassLevel.value = newLevel

        currentEqualizer?.setBandLevel(0, newLevel.toInt().toShort())
    }

    fun updateBassLevel(newLevel: Float) {
        _bassLevel.value = newLevel

        currentEqualizer?.setBandLevel(1, newLevel.toInt().toShort())
    }

    fun updateMidLevel(newLevel: Float) {
        _midrangeLevel.value = newLevel

        currentEqualizer?.setBandLevel(2, newLevel.toInt().toShort())
    }

    fun updateUpperMidLevel(newLevel: Float) {
        _upperMidLevel.value = newLevel

        currentEqualizer?.setBandLevel(3, newLevel.toInt().toShort())
    }

    fun updateTrebleLevel(newLevel: Float) {
        _trebleLevel.value = newLevel

        currentEqualizer?.setBandLevel(4, newLevel.toInt().toShort())
    }

    fun resetAll(){
        _subBassLevel.value = 0f
        _bassLevel.value = 0f
        _midrangeLevel.value = 0f
        _upperMidLevel.value = 0f
        _trebleLevel.value = 0f

        currentEqualizer?.setBandLevel(0, 0)
        currentEqualizer?.setBandLevel(1, 0)
        currentEqualizer?.setBandLevel(2, 0)
        currentEqualizer?.setBandLevel(3, 0)
        currentEqualizer?.setBandLevel(4, 0)
    }

    fun playPadSound(padId: Int) {
        try {
            // Mapper ID to corresponding file
            val soundResId = when(padId) {
                0 -> R.raw.pad_sound0
                1 -> R.raw.pad_sound1
                2 -> R.raw.pad_sound2
                3 -> R.raw.pad_sound3
                4 -> R.raw.pad_sound4
                5 -> R.raw.pad_sound5
                else -> R.raw.pad_sound0
            }

            // Play pad sound
            padSoundPlayer = MediaPlayer.create(appContext, soundResId)
            padSoundPlayer?.start()

        } catch (e: Exception) {
            Log.e("EqualizerViewModel", "Error playing pad sound: ${e.message}")
        }
    }
}