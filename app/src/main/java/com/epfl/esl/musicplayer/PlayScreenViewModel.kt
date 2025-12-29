package com.epfl.esl.musicplayer

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class PlayScreenViewModel (
    application : Application
) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayerService(application.applicationContext)
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    private var isPlayerInitialized = false
    private val musicId = 0

    fun onPlayPauseClick(){
        if (isPlaying.value == true){
            audioPlayer.pause()
        } else {
            if (isPlayerInitialized) {
                audioPlayer.resume()
            } else {
                audioPlayer.play(R.raw.testmusic)
                isPlayerInitialized = true
            }
        }
    }

    fun onLeftArrowClick(){
        audioPlayer.rewind()
    }

    fun onSeek(newPosition: Float){
        audioPlayer.seekTo(newPosition.toInt())
    }
}