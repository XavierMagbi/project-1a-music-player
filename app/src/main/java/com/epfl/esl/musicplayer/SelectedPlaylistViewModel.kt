package com.epfl.esl.musicplayer

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream


class SelectedPlaylistViewModel(application : Application, playlistId: String, currentUsername: String) : AndroidViewModel(application){
    val context = getApplication<Application>().applicationContext
    // To be able to use the given parameters
    private val playlistId = playlistId
    private val currentUsername = currentUsername

    private val database = FirebaseDatabase.getInstance()
    private val playlistsRef = database.getReference("Playlists")

    private val storage = FirebaseStorage.getInstance()
    val musicRef = storage.reference.child("Musics")

    private val _playlistName = MutableLiveData<String>("")
    val playlistName: LiveData<String> get() = _playlistName

    private val _song_id = MutableLiveData<List<String>>(emptyList())
    val song_id: LiveData<List<String>> get() = _song_id

    private val _songs = MutableLiveData<List<musicMetadata>>(emptyList())

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private var playlistListener: ValueEventListener? = null

    private val _filteredSongs = MutableLiveData<List<musicMetadata>>(emptyList())
    val filteredSongs: LiveData<List<musicMetadata>> = _filteredSongs

    //private val _newQueue = MutableLiveData<List<String>>(emptyList())
    //val newQueue : LiveData<List<String>> = _newQueue

    private val _playlistImageUri = MutableLiveData<Uri?>(null)
    val playlistImageUri: LiveData<Uri?> = _playlistImageUri   

    private val _isMyPlaylist = MutableLiveData<Boolean>(false)
    val isMyPlaylist: LiveData<Boolean> = _isMyPlaylist

    init {
        // Start listening for playlist changes
        listenToPlaylist(playlistId)

    }
    private fun listenToPlaylist(playlistId: String) {

        val ref = playlistsRef.child(playlistId)

        playlistListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _playlistName.value =  snapshot.child("name").getValue(String::class.java)

               val playlistAuthor = snapshot.child("author").getValue(String::class.java)
               _isMyPlaylist.value = (playlistAuthor == currentUsername)

                // Get playlist image (stored as string in Realtime Database hence need to convert to URI)
                val photoUrlString = snapshot.child("photo_URL").getValue(String::class.java)
                _playlistImageUri.value = if (photoUrlString != null) Uri.parse(photoUrlString) else null   

                val tracks = snapshot.child("tracks").children
                    .mapNotNull { it.getValue(String::class.java) }
                _song_id.value=tracks

                fetchSongsFromGsPaths(tracks)

            }

            override fun onCancelled(error: DatabaseError) {
                // Optionally handle errors
            }
        }

        ref.addValueEventListener(playlistListener as ValueEventListener)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterSongs(query)
    }

    private fun filterSongs(query: String) {
        val filtered = if (query.isEmpty()) {
            _songs.value ?: emptyList()
        } else {
            _songs.value?.filter { song ->
                (song.title?:"").lowercase().startsWith(query.lowercase())
            } ?: emptyList()
        }
        _filteredSongs.value = filtered
    }

    private fun fetchSongsFromGsPaths(songPaths: List<String>) {
        val songList = MutableList<musicMetadata>(songPaths.size){ musicMetadata() }
        _songs.value=List<musicMetadata>(songPaths.size){ musicMetadata(title = null) }
        //_newQueue.value= emptyList() // reset the queue for this playlist

        if (songPaths.isEmpty()) {
            _songs.value = emptyList()
            _filteredSongs.value = emptyList()
            return
        }

        songPaths.forEachIndexed() { idx, gsPath ->
            val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(gsPath)

            // Download MP3 directly to cache directory
            val tempFile = File(context.cacheDir, fileRef.name)
            fileRef.getFile(tempFile)
                .addOnSuccessListener {

                    // Extract metadata (cover image, title)
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(tempFile.absolutePath)

                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: fileRef.name.replace(".mp3", "")
                    val coverImage = retriever.embeddedPicture
                    retriever.release()

                    // Add to songs list
                    val tempMusic = musicMetadata(
                        title = title,
                        image = coverImage,
                        link = fileRef.downloadUrl.toString(),
                        linkGS = gsPath,
                        datapath = tempFile.absolutePath
                    )

                    // Add to playback queue

                    try {
                        _songs.value = _songs.value.toMutableList().apply {
                            this[idx] = tempMusic
                        }
                    } catch (e:Exception){
                        Log.d("PlaylistFetch","list changed!")
                    }


                    // Update LiveData for UI

                    filterSongs(_searchQuery.value ?: "")

                }
                .addOnFailureListener { e ->
                    Log.e("SelectedPlaylistVM", "Failed to download ${fileRef.name}: ${e.message}")
                    // Add fallback entry if needed
                    songList.add(
                        musicMetadata(
                            title = fileRef.name.replace(".mp3", ""),
                            image = null,
                            link = null
                        )
                    )
                    _songs.value = songList.toList()
                    filterSongs(_searchQuery.value ?: "")
                }
        }
    }

    fun getSongIdList():List<String>{
        val IdList:MutableList<String> =mutableListOf()
        _filteredSongs.value.forEach{item ->
            if (item.datapath!="") {
                IdList.add(item.datapath)
            }
        }

        return IdList
    }


    // ==== To update playlist picture ====

    // Update the profile image URI (inspired from LoginProfileViewModel.kt which itself comes from EE-490(g) labs)
    fun updatePlaylistImage(newImageUri: Uri){
        
        // For transformations such as scaling
        val matrix = Matrix()
        // Get the image bitmap from the URI
        var imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, newImageUri)
        // Scale down the image to reduce size before upload
        val ratio: Float = 5F
        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), false)
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled, 0, 0, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), matrix, true)
        // Convert the bitmap to a byte array and compress it as PNG
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        // Upload new image to Firebase Storage
        val playlistImageRef = storage.reference.child("PlaylistImages/" + playlistId + ".jpg")
        val uploadPlaylistImage = playlistImageRef.putBytes(imageByteArray)

        uploadPlaylistImage.addOnSuccessListener { taskSnapshot ->
            playlistImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                _playlistImageUri.value = downloadUrl  // Update LiveData
                playlistsRef.child(playlistId).child("photo_URL").setValue(downloadUrl.toString())  // Update URL
            }
        }
    }

    // ==== To rename playlist ====
    fun updatePlaylistName(newName: String) {
        _playlistName.value = newName  // Locally update
        playlistsRef.child(playlistId).child("name").setValue(newName)  // Remote update
    }

    // ==== To delete song from playlist ====
    // Function is simplified to delete first instance (in case of multiple times the same music) of concerned music in playlist
    // Would require to rework data structure of Realtime Database otherwise (limited time in project)
    fun deleteSong(linkGS: String) {
        // Get all playlist songs
        val currentTracks = _song_id.value?.toMutableList() ?: return
        // Spot the first index of the song to delete
        val indexToDelete = currentTracks.indexOfFirst { it == linkGS }
        // Remove only the first occurrence
        currentTracks.removeAt(indexToDelete)
        // Update in remote
        playlistsRef.child(playlistId).child("tracks").setValue(currentTracks)
    }

    fun isPlaylistLoaded():Boolean{

        _songs.value.forEach{song->
            if (song.title==null){
                return false
            }
        }
        return true
    }
}



class SelectedPlaylistViewModelFactory(
    private val playlistId: String,
    private val application: Application,
    private val currentUsername: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectedPlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectedPlaylistViewModel( playlistId =playlistId, application = application, currentUsername = currentUsername) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
