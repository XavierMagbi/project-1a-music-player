package com.epfl.esl.musicplayer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

//data class to hold extracted metadata
data class Metadata(val title: String, val cover: ByteArray?)

/*
    Play Screen ViewModel

    Functionality:
    hold state to update play screen UI
    manage song queue updates
    receive input from :
        - PlayScreen: Play/Pause, fast forward, rewind, shuffle, loop, seek, queue skips
        - Main Activity: Play/Pause from minimized player
        - Main Activity: Play/Pause, fast forward, rewind from smartwatch messages
        - DiscoverScreen/SelectedPlaylistScreen: Update song queue
    manage equalization and sound FX from equalizer viewModel State
    send song metadata to wear module
 */

class PlayScreenViewModel (
    application : Application, // to retrieve context
    private val equalizerViewModel: EqualizerViewModel = EqualizerViewModel(application), //manage equalization and sound FX
    private val dataClient : DataClient //send music metadata to watch
) : AndroidViewModel(application) // viewmodel structure that is context aware
{
    // Get application context
    val context = getApplication<Application>().applicationContext

    // Initialize audio player service
    private val audioPlayer = AudioPlayerService(application.applicationContext)

    // Audio Service variables
    val isPlaying: LiveData<Boolean?> = audioPlayer.isPlaying
    val currentPosition: LiveData<Int> = audioPlayer.currentPosition
    val duration: LiveData<Int> = audioPlayer.duration
    val title: LiveData<String> = audioPlayer.title
    val coverImage: LiveData<ByteArray?> = audioPlayer.cover
    val audioSessionId: LiveData<Int?> = audioPlayer.audioSessionId

    // ViewModel variables
    private var isPlayerInitialized = false //to correctly init audio player module
    var originalPlaylist by mutableStateOf(emptyList<String>()) //List of paths to tracks in cache memory
    var currentPlaylist by mutableStateOf(originalPlaylist)  //copy playlist to manage shuffle/repeat
    var currentTrackIndex by mutableStateOf(-1) //Special init value to not display tracks when no queue is loaded

    // State variables for PlayScreen
    private val _shuffleOn = MutableLiveData(false)
    val shuffleOn: LiveData<Boolean> = _shuffleOn

    private val _repeatMode = MutableLiveData(0)
    val repeatMode: LiveData<Int> = _repeatMode

    // Function to put scale album covers to send to wear (data api sends <1Mb at a time and always have same size on watch)
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

            // get track metadata to send
            val currentTitle = title.value ?: "No Title"
            val currentlyPlaying = isPlaying.value ?: false
            val coverArtBytes = coverImage.value
            val duration = duration.value

            Log.d("PlayScreenViewModel", "Preparing to send song data to watch: '$currentTitle'")

            //check if there is an available receiver, else don't send
            if (!hasConnectedWatch(ctx)) {
                Log.d("PlayScreenViewModel", "No connected watch node -> skip sending")
                return@launch
            }


            try { //wrap in try statement in case no data module is available
                // put metadata in datamap to send
                val putStaticDataRequest: PutDataRequest = PutDataMapRequest.create("/static_songInfo").run {
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    dataMap.putString("songTitle", currentTitle)
                    dataMap.putBoolean("isPlaying", currentlyPlaying)
                    dataMap.putInt("duration",duration)

                    // If coverArtBytes is not null, put it directly into the dataMap.
                    if (coverArtBytes != null) {

                        // Get the bitmap from byte array since, the bitmap has the the resize function
                        val bitmapImage =
                            (BitmapFactory.decodeByteArray(coverArtBytes, 0, coverArtBytes.size))


                        // rescale image to correct format
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

                    }
                    asPutDataRequest()
                }

                putStaticDataRequest.setUrgent()
                // send data item to wear
                val result = dataClient.putDataItem(putStaticDataRequest).await()
                Log.d("PhoneTx", "putDataItem OK uri=${result.uri}")

                Log.d("PlayScreenViewModel", "Successfully sent song data to watch.")

            } catch (e: Exception) {
                Log.e("PlayScreenViewModel", "Failed to send song data to watch.", e)
            }


        }
    }

    // handle Pause/Play button clicks
    fun onPlayPauseClick(){
        if (isPlaying.value == true){ //pause track if currently playing
            audioPlayer.pause()
        } else {
            if (isPlayerInitialized) { // resume if track is initialized and on pause
                audioPlayer.resume()
            } else { // update audio player if track starts
                playCurrentTrack()
                isPlayerInitialized = true
            }
        }
        sendSongDataToWear(dataClient)
    }


    // handle left arrow button clicks (rewind)
    fun onLeftArrowClick(){
        //if arrow clicked at the beining of track (less than 3secs) play previous track (if exists)
        if (currentTrackIndex > 0 && currentPosition.value < 3000) {
            currentTrackIndex--
            playCurrentTrack()
        } else { // if track has sufficiently been played, rewind to begining of track
            audioPlayer.rewind()
        }
        sendSongDataToWear(dataClient)
    }
    // handle right arrow button click (fast forward)
    fun onRightArrowClick() {
        sendSongDataToWear(dataClient)

        if (_repeatMode.value == 2){ // Repeat track mode
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
    // update audioPlayer to track at current index (called by Play/Pause/Side arrows)
    fun playCurrentTrack(index: Int = currentTrackIndex) {
        try{
        currentTrackIndex = index
        audioPlayer.play(currentPlaylist[index])
        isPlayerInitialized = true
        sendSongDataToWear(dataClient)
        }catch (e:Exception){
            Log.d("playscreen","error")
        }
    }

    // handle timing for slider -> seek to new position in track
    fun onSeek(newPosition: Float){
        audioPlayer.seekTo(newPosition.toInt())

    }

    //viewModel init function
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

    // handle shuffle button click
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

    // handle repeat button click
    fun onRepeatClick(){
        //cylce through repeat states
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

    // get track metadata from path of .mp3 in cache
    fun getTrackMetadata(resPath: String): Metadata {
        val retriever = MediaMetadataRetriever()
        return try {
            // Get URI
            val uri = android.net.Uri.parse(resPath)
            retriever.setDataSource(getApplication(), uri)
            // Extract title
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: getTrackName(resPath) // If no title in metadata => return file name
            // Extract image
            val cover = retriever.embeddedPicture

            Metadata(title, cover)
        } catch (e: Exception) {
            Metadata(getTrackName(resPath), null) // If error => return file name
        } finally {
            retriever.release()
        }
    }

    // add specific track queue -> add the path of .mp3 in current Playlist list
    fun addToQueue(songRef: String) {
        // Add music right after the current track
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

    // update song queue/tracklist (when clicking on a track in a playlist for example)
    fun changeQueue(queue:List<String>,idx:Int){
        originalPlaylist=queue
        currentPlaylist=queue
        currentTrackIndex=idx
        playCurrentTrack()
    }
}