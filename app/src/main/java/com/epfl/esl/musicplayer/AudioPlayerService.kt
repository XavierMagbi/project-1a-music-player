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

    fun play(uri: Uri) {
        // Stop & release previous MediaPlayer
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        // ---- METADATA EXTRACTION ----
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val metaTitle =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            _title.value = metaTitle ?: "Unknown"

            val metaCover = retriever.embeddedPicture
            _cover.value = metaCover

        } catch (e: Exception) {
            _title.value = "Error reading metadata"
            _cover.value = null
        } finally {
            retriever.release()
        }

        // ---- PLAYBACK ----
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setOnPreparedListener {
                it.start()
                _isPlaying.value = true
                _duration.value = it.duration
                startUpdatingProgress()
            }
            setOnCompletionListener {
                _isPlaying.value = false
                stopUpdatingProgress()
                onCompletionListener?.invoke()
            }
            prepareAsync() // IMPORTANT for remote files
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
