package com.epfl.esl.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class EqualizerViewModel(
    application: Application
): AndroidViewModel(application) {

    private val _smallFreqLevel = MutableLiveData(0f)
    val smallFreqLevel: LiveData<Float> = _smallFreqLevel

    fun updateSmallFreqLevel(newLevel: Float) {
        _smallFreqLevel.value = newLevel
    }
}