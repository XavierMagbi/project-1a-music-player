package com.epfl.esl.musicplayer

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerService (
    private val context: Context
){
    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null

    private val _isPlaying = MutableLiveData<Boolean?>()
    val isPlaying: LiveData<Boolean?>
        get() = _isPlaying

    private val _currentPosition = MutableLiveData<Int>(0)
    val currentPosition: LiveData<Int>
        get() = _currentPosition

    private val _duration = MutableLiveData<Int>(0)
    val duration: LiveData<Int>
        get() = _duration

    fun play(musicResId: Int){
        mediaPlayer = MediaPlayer.create(context, musicResId) // Requires APK hence Context to access musics
        mediaPlayer?.start()
        _isPlaying.value = true
        startUpdatingProgress()

        _duration.value = mediaPlayer?.duration ?: 0
    }

    fun pause(){
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopUpdatingProgress()
    }

    fun resume(){
        mediaPlayer?.start()
        _isPlaying.value = true
        startUpdatingProgress()
    }

    fun rewind(){
        mediaPlayer?.seekTo(0)
        _currentPosition.postValue(0)
    }

    fun seekTo(position: Int){
        mediaPlayer?.seekTo(position)
        _currentPosition.postValue(position)
    }

    private fun startUpdatingProgress() {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.postValue(it.currentPosition)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopUpdatingProgress() {
        updateJob?.cancel()
    }
}
