package com.epfl.esl.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class EqualizerViewModel(
    application: Application
): AndroidViewModel(application) {

    private val _bassLevel = MutableLiveData(0f)
    val bassLevel: LiveData<Float> = _bassLevel

    private val _midLevel = MutableLiveData(0f)
    val midLevel: LiveData<Float> = _midLevel

    private val _trebleLevel = MutableLiveData(0f)
    val trebleLevel: LiveData<Float> = _trebleLevel

    fun updateBassLevel(newLevel: Float) {
        _bassLevel.value = newLevel
    }

    fun updateMidLevel(newLevel: Float) {
        _midLevel.value = newLevel
    }

    fun updateTrebleLevel(newLevel: Float) {
        _trebleLevel.value = newLevel
    }
}