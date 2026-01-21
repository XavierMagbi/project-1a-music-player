package com.epfl.esl.musicplayer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

data class Metadata(val title: String, val cover: ByteArray?)

class PlayScreenViewModel (
    application : Application,
    private val equalizerViewModel: EqualizerViewModel = EqualizerViewModel(application),
    private val dataClient : DataClient
) : AndroidViewModel(application) {

    val context = getApplication<Application>().applicationContext
    private val audioPlayer = AudioPlayerService(application.applicationContext)

    // Service variables
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    val coverImage: LiveData<ByteArray?> = audioPlayer.cover
    val audioSessionId: LiveData<Int?> = audioPlayer.audioSessionId

    private var isPlayerInitialized = false
    var originalPlaylist by mutableStateOf(emptyList<String>())
    var currentPlaylist by mutableStateOf(originalPlaylist)
    var currentTrackIndex by mutableStateOf(-1) // Don't want any music highlighted in initalization

    private val _shuffleOn = MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn

    private val _repeatMode = MutableLiveData(0)
    val repeatMode: LiveData<Int> = _repeatMode

    // Function to put Album covers into the same size

    fun scaleTo80dp(context: Context, bitmap: Bitmap): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (80f * density).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
    }

    // Function to test if the watch is connected
    suspend fun hasConnectedWatch(context: Context): Boolean {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            return nodes.isNotEmpty()
        } catch (e: Exception) {
            Log.e("PlayScreenViewModel", "Failed to connect to the watch.", e)
        }
        return false
    }


    // Function to send Song Data to the wear module

     fun sendSongDataToWear(dataClient: DataClient) {

        viewModelScope.launch {

            val ctx = getApplication<Application>().applicationContext

            val currentTitle = title.value ?: "No Title"
            val currentlyPlaying = isPlaying.value ?: false
            val coverArtBytes = coverImage.value
            val currentPosition = currentPosition.value?:0
            val duration = duration.value

            Log.d("PlayScreenViewModel", "Preparing to send song data to watch: '$currentTitle'")

            val lastsongId = currentTitle;

            if (!hasConnectedWatch(ctx)) {
                Log.d("PlayScreenViewModel", "No connected watch node -> skip sending")
                return@launch
            }

            if (currentTitle == lastsongId){
                try {
                    // specific path for static song infos

                    val putStaticDataRequest: PutDataRequest = PutDataMapRequest.create("/static_songInfo").run {
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                        dataMap.putString("songTitle", lastsongId)
                        dataMap.putBoolean("isPlaying", currentlyPlaying)
                        dataMap.putInt("duration",duration)
                        // If coverArtBytes is not null, put it directly into the dataMap.
                        if (coverArtBytes != null) {


                            // PNG has not losses, it just ignores this field when compressing
                            val COMPRESS_QUALITY = 0


                            // Get the bitmap from byte array since, the bitmap has the the resize function
                            val bitmapImage =
                                (BitmapFactory.decodeByteArray(coverArtBytes, 0, coverArtBytes.size))


                            // New bitmap with the correct size , may not return a null object
                            // In order to keep everything the same size , we'll scale it to 80dp

                            val mutableBitmapImage = scaleTo80dp(ctx,bitmapImage)


                            // Get the byte array from tbe bitmap to be returned
                            val outputStream = ByteArrayOutputStream()
                            mutableBitmapImage.compress(Bitmap.CompressFormat.PNG, 0, outputStream)

                            if (mutableBitmapImage != bitmapImage) {
                                mutableBitmapImage.recycle()
                            } // else they are the same, just recycle once

                            bitmapImage.recycle()
                            val sentCover= outputStream.toByteArray()
                            dataMap.putByteArray("albumArt",sentCover)
//2
                        }
                        asPutDataRequest()
                    }

                    putStaticDataRequest.setUrgent()

                    val result = dataClient.putDataItem(putStaticDataRequest).await()
                    Log.d("PhoneTx", "putDataItem OK uri=${result.uri}")

                    Log.d("PlayScreenViewModel", "Successfully sent song data to watch.")

                } catch (e: Exception) {
                    Log.e("PlayScreenViewModel", "Failed to send Static song data to watch.", e)
                }
            }

            try {
                // specific path for dynamic song infos
                val putDynamicDataRequest: PutDataRequest = PutDataMapRequest.create("/dynamic_songInfo").run {
                    dataMap.putInt("currentPosition", currentPosition)
                    asPutDataRequest()
                }

                putDynamicDataRequest.setUrgent()

                val result = dataClient.putDataItem(putDynamicDataRequest).await()
                Log.d("PhoneTx", "putDataItem OK uri=${result.uri}")

                Log.d("PlayScreenViewModel", "Successfully sent song data to watch.")

            } catch(e: Exception){
                Log.e("PlayScreenViewModel", "Failed to send Dynamic song data to watch.", e)
            }

        }
    }

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
        sendSongDataToWear(dataClient)
    }
    // Left arrow button
    fun onLeftArrowClick(){
        if (currentTrackIndex > 0 && currentPosition.value < 3000) {
            currentTrackIndex--
            playCurrentTrack()
        } else {
            audioPlayer.rewind()
        }
        sendSongDataToWear(dataClient)
    }
    // Right arrow button
    fun onRightArrowClick() {
        sendSongDataToWear(dataClient)

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
        sendSongDataToWear(dataClient)
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
    fun getTrackName(path: String): String {
        return path.substringAfterLast("/").substringBeforeLast(".")
    }
    // For sheet queue
    fun getTrackMetadata(resId: String): Metadata {
        val retriever = MediaMetadataRetriever()
        return try {
            // Get URI
            val uri = android.net.Uri.parse(resId)
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

    fun addToQueue(songRef: String) {
        // Add music right after the current track
        //val track = originalPlaylist[index]
        val newPlaylist = currentPlaylist.toMutableList()

        // Insert at position currentTrackIndex + 1 (right after current track)
        newPlaylist.add(currentTrackIndex + 1, songRef)
        currentPlaylist = newPlaylist
        if (originalPlaylist.size==0){
            Toast.makeText(context, "Launch a queue before adding", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
        }
    }

    fun changeQueue(queue:List<String>,idx:Int){
        originalPlaylist=queue
        currentPlaylist=queue
        currentTrackIndex=idx
        playCurrentTrack()
    }
}