package com.epfl.esl.musicplayer

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/*
    Audio Player Service
    Called from PlayScreenViewModel exclusively

    Functionality:
    Handles audio playback using Android MediaPlayer API

    Interacts with:
    Android MediaPlayer API - to play, pause, seek audio tracks
 */

class AudioPlayerService (
    private val context: Context
){
    // Service variables
    private var mediaPlayer: MediaPlayer? = null
    private var updateJob: Job? = null
    var onCompletionListener: (() -> Unit)? = null

    // Service LiveData variables
    // Is music currently playing
    private val _isPlaying = MutableLiveData<Boolean?>()
    val isPlaying: LiveData<Boolean?>
        get() = _isPlaying

    // Current music position
    private val _currentPosition = MutableLiveData<Int>(0)
    val currentPosition: LiveData<Int>
        get() = _currentPosition

    // Overall music duration
    private val _duration = MutableLiveData<Int>(0)
    val duration: LiveData<Int>
        get() = _duration

    // Music title set in .mp3 metadata
    private val _title = MutableLiveData<String>("")
    val title: LiveData<String>
        get() = _title

    // Music cover image set in .mp3 metadata
    private val _cover = MutableLiveData<ByteArray?>(null)
    val cover: LiveData<ByteArray?>
        get() = _cover

    // For equalizer
    private val _audioSessionId = MutableLiveData<Int?>(null)
    val audioSessionId: LiveData<Int?>
        get() = _audioSessionId

    // Service functions
    fun play(musicResId: String){
        // To stop playing the initial song when switching to another one
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        // To extract metadata for PlayScreen
        val retriever = MediaMetadataRetriever()
        try {
            // Set up retriever
            val uri = Uri.parse(musicResId)
            retriever.setDataSource(context, uri)
            // Extract title
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            _title.value = metaTitle ?: "Unknown"
            // Extract cover image
            val metaCover = retriever.embeddedPicture
            _cover.value = metaCover
        } catch (e: Exception) {
            _title.value = "Error with metadata lecture"
            _cover.value = null
        } finally {
            retriever.release()
        }

        // Set up MediaPlayer
        mediaPlayer = MediaPlayer.create(context, Uri.parse(musicResId)) 
        // Fetch audio session ID for equalizer
        _audioSessionId.value = mediaPlayer?.audioSessionId
        // Start playback
        mediaPlayer?.start()
        _isPlaying.value = true
        startUpdatingProgress() // For music time progression tracking
        _duration.value = mediaPlayer?.duration ?: 0 // Get duration for slider

        // Handle end of music playback
        mediaPlayer?.setOnCompletionListener {
            _isPlaying.value = false
            stopUpdatingProgress()
            onCompletionListener?.invoke()
        }
    }

    // Handle pause in music playback
    fun pause(){
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopUpdatingProgress()
    }

    // Handle resume in music playback
    fun resume(){
        mediaPlayer?.start()
        _isPlaying.value = true
        startUpdatingProgress()
    }

    // Handle rewind in music playback
    fun rewind(){
        mediaPlayer?.seekTo(0)
        _currentPosition.postValue(0)
    }

    // Handle slider user movement in music playback
    fun seekTo(position: Int){
        mediaPlayer?.seekTo(position)
        _currentPosition.postValue(position)
    }

    // Update current position every 500ms when music is playing
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

    // Stop updating current position
    private fun stopUpdatingProgress() {
        updateJob?.cancel()
    }
}
