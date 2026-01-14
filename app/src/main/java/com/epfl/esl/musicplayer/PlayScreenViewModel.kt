package com.epfl.esl.musicplayer

import android.app.Application
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.storage.FirebaseStorage

data class Metadata(val title: String, val cover: ByteArray?)

class PlayScreenViewModel (
    application : Application,
    audioPlayer: AudioPlayerService = AudioPlayerService(application.applicationContext)
) : AndroidViewModel(application) {

    //private val audioPlayer = AudioPlayerService(application.applicationContext)
    private val audioPlayer = audioPlayer
    // Service variables
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    val coverImage: LiveData<ByteArray?> = audioPlayer.cover

    private var isPlayerInitialized = false
    private val originalPlaylist = emptyList<SongItem>()
    var currentPlaylist by mutableStateOf<List<SongItem>>(emptyList())

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
        val song = currentPlaylist[index]
        var storageRef = FirebaseStorage.getInstance().getReference(song.Path?:"")

        storageRef.downloadUrl
            .addOnSuccessListener { uri ->
                audioPlayer.play(uri)
                isPlayerInitialized = true
            }
            .addOnFailureListener {
                // handle error (toast / log)
            }

    }
    // To get timing for slider
    fun onSeek(newPosition: Float){
        audioPlayer.seekTo(newPosition.toInt())
    }

    init {
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

    fun changePlaylist(newList: List<SongItem>,newIdx:Int){
        currentPlaylist=newList
        currentTrackIndex=newIdx
        onSeek(0f)
        playCurrentTrack()

    }
}

class PlayScreenViewModelFactory(
    private val application: Application,
    private val audioPlayerService: AudioPlayerService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayScreenViewModel(
                application = application,
                audioPlayer = audioPlayerService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
