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

    private val _title = MutableLiveData<String>("")
    val title: LiveData<String>
        get() = _title

    private val _cover = MutableLiveData<ByteArray?>(null)
    val cover: LiveData<ByteArray?>
        get() = _cover


    var onCompletionListener: (() -> Unit)? = null

    fun play(musicResId: Int){
        // To stop playing the initial song when switching to another one
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        // To extract metadata through URI Android
        val retriever = MediaMetadataRetriever()
        try {
            // Get URI
            val uri = Uri.parse("android.resource://${context.packageName}/$musicResId")
            retriever.setDataSource(context, uri)
            // Get title
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            _title.value = metaTitle ?: "Unknown"
            // Get cover image
            val metaCover = retriever.embeddedPicture
            _cover.value = metaCover
        } catch (e: Exception) {
            _title.value = "Error with metadata lecture"
            _cover.value = null
        } finally {
            retriever.release()
        }

        // To play the music
        mediaPlayer = MediaPlayer.create(context, musicResId) // Requires APK hence Context to access musics
        mediaPlayer?.start()
        _isPlaying.value = true
        startUpdatingProgress()
        _duration.value = mediaPlayer?.duration ?: 0

        // To handle end of music
        mediaPlayer?.setOnCompletionListener {
            _isPlaying.value = false
            stopUpdatingProgress()
            onCompletionListener?.invoke()
        }
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
