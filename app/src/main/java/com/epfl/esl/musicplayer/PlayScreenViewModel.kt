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

    // Service variables
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    val coverImage: LiveData<ByteArray?> = audioPlayer.cover

    private var isPlayerInitialized = false
    private val playlist = listOf (
        R.raw.music0,
        R.raw.music1,
        R.raw.music2
    )
    private var currentTrackIndex = 0



    fun onPlayPauseClick(){
        if (isPlaying.value == true){
            audioPlayer.pause()
        } else {
            if (isPlayerInitialized) {
                audioPlayer.resume()
            } else {
                playCurrentTrack()
                isPlayerInitialized = true
            }
        }
    }

    fun onLeftArrowClick(){
        if (currentTrackIndex > 0 && currentPosition.value < 3000) {
            currentTrackIndex--
            playCurrentTrack()
        } else {
            audioPlayer.rewind()
        }
    }

    fun onRightArrowClick() {
        if (currentTrackIndex < playlist.size - 1) {
            currentTrackIndex++
            playCurrentTrack()
        }
    }

    private fun playCurrentTrack() {
        audioPlayer.play(playlist[currentTrackIndex])
        isPlayerInitialized = true
    }

    fun onSeek(newPosition: Float){
        audioPlayer.seekTo(newPosition.toInt())
    }

    init {
        // To handle end of music
        audioPlayer.onCompletionListener = {
            onRightArrowClick()
        }
    }
}