package com.epfl.esl.musicplayer

import android.app.Application
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.util.Log

/*
    Equalizer ViewModel

    Functionality:
    Handles equalizer settings and pad sound play

    Interacts with:
    Android MediaPlayer API - to play pad sounds
    Android Equalizer API - to adjust audio equalizer settings
 */

// ViewModel for EqualizerScreen
class EqualizerViewModel(
    application: Application
): AndroidViewModel(application) {

    // ViewModel LiveData variables (Frequency band levels)
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

    // Equalizer instance
    private var equalizer: Equalizer? = null

    // MediaPlayer instance and app context (required for playing sounds)
    private val appContext = application.applicationContext
    private var padSoundPlayer: MediaPlayer? = null

    // Used by PlayScreenViewModel to set equalizer on current music being played
    fun setEqualizer(audioSessionId: Int) {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }

            // Apply old settings to new equalizer
            applyCurrentSettings()
        } catch (e: Exception) {
            equalizer = null
        }
    }

    // Apply current ViewModel settings to equalizer instance (For next song to have set settings)
    private fun applyCurrentSettings() {
        equalizer?.let {
            it.setBandLevel(0, subBassLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(1, bassLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(2, midrangeLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(3, upperMidLevel.value?.toInt()?.toShort() ?: 0)
            it.setBandLevel(4, trebleLevel.value?.toInt()?.toShort() ?: 0)
        }
    }

    // Update Sub-Bass level and apply to equalizer
    fun updateSubBassLevel(newLevel: Float) {
        _subBassLevel.value = newLevel

        equalizer?.setBandLevel(0, newLevel.toInt().toShort())
    }

    // Update Bass level and apply to equalizer
    fun updateBassLevel(newLevel: Float) {
        _bassLevel.value = newLevel

        equalizer?.setBandLevel(1, newLevel.toInt().toShort())
    }

    // Update Midrange level and apply to equalizer
    fun updateMidLevel(newLevel: Float) {
        _midrangeLevel.value = newLevel

        equalizer?.setBandLevel(2, newLevel.toInt().toShort())
    }

    // Update Upper Midrange level and apply to equalizer
    fun updateUpperMidLevel(newLevel: Float) {
        _upperMidLevel.value = newLevel

        equalizer?.setBandLevel(3, newLevel.toInt().toShort())
    }

    // Update Treble level and apply to equalizer
    fun updateTrebleLevel(newLevel: Float) {
        _trebleLevel.value = newLevel

        equalizer?.setBandLevel(4, newLevel.toInt().toShort())
    }

    // Reset all equalizer settings to default (0 mB)
    fun resetAll(){
        _subBassLevel.value = 0f
        _bassLevel.value = 0f
        _midrangeLevel.value = 0f
        _upperMidLevel.value = 0f
        _trebleLevel.value = 0f

        equalizer?.setBandLevel(0, 0)
        equalizer?.setBandLevel(1, 0)
        equalizer?.setBandLevel(2, 0)
        equalizer?.setBandLevel(3, 0)
        equalizer?.setBandLevel(4, 0)
    }

    // Play pad sound corresponding to padId
    // Much simplified for this iteration (uses RAW files and can only play one at a time) :)
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
            padSoundPlayer?.release()
            padSoundPlayer = MediaPlayer.create(appContext, soundResId)
            padSoundPlayer?.start()
            Log.d("EqualizerViewModel", "Playing pad sound for padId: $padId" )
        } catch (e: Exception) {
            Log.e("EqualizerViewModel", "Error playing pad sound: ${e.message}")
        }
    }
}