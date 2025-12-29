package com.epfl.esl.musicplayer

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import kotlin.text.get

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
    private val originalPlaylist = listOf (
        R.raw.music0,
        R.raw.music1,
        R.raw.music2
    )
    private var currentPlaylist = originalPlaylist
    private var currentTrackIndex = 0

    private val _shuffleOn = androidx.lifecycle.MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn

    private val _repeatMode = androidx.lifecycle.MutableLiveData(0)
    val repeatMode: LiveData<Int> = _repeatMode

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
        if (_shuffleOn.value){
            currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
            playCurrentTrack()
        } else {
            if (_repeatMode.value == 2){ // Repeat one mode
                audioPlayer.rewind()
            } else if (currentTrackIndex < currentPlaylist.size - 1) { // No repeat
                currentTrackIndex++
                playCurrentTrack()
            } else if (_repeatMode.value == 1) { // End of playlist but with repeat mode on
                currentTrackIndex = 0
                playCurrentTrack()
            }
        }
    }

    private fun playCurrentTrack() {
        audioPlayer.play(currentPlaylist[currentTrackIndex])
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

    fun onShuffleClick() {
        _shuffleOn.value = !(_shuffleOn.value ?: false)

        if (_shuffleOn.value) {
            // Activate shuffle
            currentPlaylist = originalPlaylist.shuffled()
        } else {
            // Deactivate shuffle
            currentPlaylist = originalPlaylist
        }
    }

    fun onRepeatClick(){
        _repeatMode.value = when (_repeatMode.value) {
            0 -> 1    // Go to classic repeat of playlist
            1 -> 2    // Go to one track repeat
            else -> 0 // No repeat
        }
    }
}