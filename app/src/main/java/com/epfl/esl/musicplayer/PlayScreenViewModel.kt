package com.epfl.esl.musicplayer

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

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
    val originalPlaylist = listOf (
        R.raw.music0,
        R.raw.music1,
        R.raw.music2
    )
    var currentPlaylist by mutableStateOf(originalPlaylist)
    var currentTrackIndex by mutableStateOf(-1) // Don't want any music highlighted in initalization

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
        if (_repeatMode.value == 2){ // Repeat one mode
            audioPlayer.rewind()
        } else if (currentTrackIndex < currentPlaylist.size - 1) { // No repeat
            // Check if now-finished music is already present within playlist
            val currentTrack = currentPlaylist[currentTrackIndex]
            val existsBeforeCurrent = currentPlaylist.subList(0, currentTrackIndex).contains(currentTrack)

            // If does exist => Music was added to queue hence need to delete from currentPlaylist
            if (existsBeforeCurrent) {
                currentPlaylist = currentPlaylist.toMutableList().apply {
                    removeAt(currentTrackIndex)
                }
            } else { // Was not a queue-added music => Keep going normally
                currentTrackIndex++
            }
            playCurrentTrack()
        } else if (_repeatMode.value == 1) { // End of playlist but with repeat mode on
            // Check if now-finished music is already present within playlist
            val currentTrack = currentPlaylist[currentTrackIndex]
            val existsBeforeCurrent = currentPlaylist.subList(0, currentTrackIndex).contains(currentTrack)

            // If does exist => Music was added to queue hence need to delete from currentPlaylist
            if (existsBeforeCurrent) {
                currentPlaylist = currentPlaylist.toMutableList().apply {
                    removeAt(currentTrackIndex)
                }
            }

            currentTrackIndex = 0
            playCurrentTrack()
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
            val currentTrack = currentPlaylist[currentTrackIndex]
            val shuffled = originalPlaylist.shuffled().toMutableList()
            shuffled.remove(currentTrack)  // Removes currentTrack
            shuffled.add(0, currentTrack)  // Add currentTrack at position 0
            currentPlaylist = shuffled
            currentTrackIndex = 0  // Current index is reset to 0
        } else {
            // Deactivate shuffle
            currentTrackIndex = originalPlaylist.indexOf(currentPlaylist[currentTrackIndex])
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

    fun addToQueue(index: Int) {
        // Add music right after the current track
        val track = originalPlaylist[index]
        val newPlaylist = currentPlaylist.toMutableList()

        // Insert at position currentTrackIndex + 1 (right after current track)
        newPlaylist.add(currentTrackIndex + 1, track)
        currentPlaylist = newPlaylist
    }

    fun changeQueue(queue:List<Int>,idx:Int){
        currentPlaylist=queue
        currentTrackIndex=idx
        playCurrentTrack()

    }

}