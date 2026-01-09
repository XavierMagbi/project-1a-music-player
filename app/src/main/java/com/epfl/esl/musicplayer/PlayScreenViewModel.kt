package com.epfl.esl.musicplayer

import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class Metadata(val title: String, val cover: ByteArray?)

class PlayScreenViewModel (
    application : Application,
    private val equalizerViewModel: EqualizerViewModel = EqualizerViewModel(application)
) : AndroidViewModel(application) {

    private val audioPlayer = AudioPlayerService(application.applicationContext)

    // Service variables
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    val coverImage: LiveData<ByteArray?> = audioPlayer.cover
    val audioSessionId: LiveData<Int?> = audioPlayer.audioSessionId

    private var isPlayerInitialized = false
    private val originalPlaylist = listOf (
        R.raw.music0,
        R.raw.music1,
        R.raw.music2
    )
    var currentPlaylist by mutableStateOf(originalPlaylist)
    var currentTrackIndex by mutableStateOf(0)

    private val _shuffleOn = MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn

    private val _repeatMode = MutableLiveData(0)
    val repeatMode: LiveData<Int> = _repeatMode

    // Pause/Play button
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
    // Left arrow button
    fun onLeftArrowClick(){
        if (currentTrackIndex > 0 && currentPosition.value < 3000) {
            currentTrackIndex--
            playCurrentTrack()
        } else {
            audioPlayer.rewind()
        }
    }
    // Right arrow button
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
    // Play track at current index (called by Play/Pause/Side arrows)
    fun playCurrentTrack(index: Int = currentTrackIndex) {
        currentTrackIndex = index
        audioPlayer.play(currentPlaylist[index])
        isPlayerInitialized = true
    }
    // To get timing for slider
    fun onSeek(newPosition: Float){
        audioPlayer.seekTo(newPosition.toInt())
    }

    init {
        // Initialize equalizer
        audioPlayer.audioSessionId.observeForever { sessionId ->
            if (sessionId != null && sessionId > 0) {
                equalizerViewModel.setEqualizer(sessionId)
            }
        }
        // To handle end of music
        audioPlayer.onCompletionListener = {
            onRightArrowClick()
        }
    }
    // Shuffle button
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
    // Repeat button
    fun onRepeatClick(){
        _repeatMode.value = when (_repeatMode.value) {
            0 -> 1    // Go to classic repeat of playlist
            1 -> 2    // Go to one track repeat
            else -> 0 // No repeat
        }
    }
    // In case metadata has no title
    fun getTrackName(resId: Int): String {
        return getApplication<Application>().resources.getResourceEntryName(resId)
    }
    // For sheet queue
    fun getTrackMetadata(resId: Int): Metadata {
        val retriever = MediaMetadataRetriever()
        return try {
            // Get URI
            val uri = android.net.Uri.parse("android.resource://${getApplication<Application>().packageName}/$resId")
            retriever.setDataSource(getApplication(), uri)
            // Extract title
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: getTrackName(resId) // If no title in metadata => return file name
            // Extract image
            val cover = retriever.embeddedPicture

            Metadata(title, cover)
        } catch (e: Exception) {
            Metadata(getTrackName(resId), null) // If error => return file name
        } finally {
            retriever.release()
        }
    }
}