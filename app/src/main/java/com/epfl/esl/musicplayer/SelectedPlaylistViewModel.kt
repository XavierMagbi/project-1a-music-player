package com.epfl.esl.musicplayer

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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
import androidx.lifecycle.AndroidViewModel
import java.io.ByteArrayOutputStream

/*
    Selected Playlist Screen ViewModel

    Functionality:
    Fetch songs in selected playlist and extract metadata for display
    Allows to delete song from playlist
    Allows to rename playlist and update playlist picture 
    Allows to add songs to playback queue

    Interacts with:
    Firebase Realtime Database - to fetch selected playlist metadata and tracks
    Firebase Storage - to fetch playlist songs .mp3 files and extract metadata
 */

 // ViewModel Factory as currentUsername and playlistId are required to identify playlists and its accesses
// Inspiration EE-490(g) Week 5: ViewModels and System Services (slide 8)
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

// ViewModel for SelectedPlaylistScreen
class SelectedPlaylistViewModel(application : Application, playlistId: String, currentUsername: String) : AndroidViewModel(application){
    // Get context to access cache directory
    val context = getApplication<Application>().applicationContext

    private val playlistId = playlistId
    private val currentUsername = currentUsername

    // Firebase Realtime Database and Storage references
    private val database = FirebaseDatabase.getInstance()
    private val playlistsRef = database.getReference("Playlists")
    private val storage = FirebaseStorage.getInstance()
    val musicRef = storage.reference.child("Musics")

    // ViewModel LiveData variables
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

    private val _playlistImageUri = MutableLiveData<Uri?>(null)
    val playlistImageUri: LiveData<Uri?> = _playlistImageUri   

    private val _isMyPlaylist = MutableLiveData<Boolean>(false)
    val isMyPlaylist: LiveData<Boolean> = _isMyPlaylist

    // Initialize by loading songs from playlist
    init {
        listenToPlaylist(playlistId) 
    }

    // Listen to changes in the selected playlist in Realtime Database
    private fun listenToPlaylist(playlistId: String) {
        val ref = playlistsRef.child(playlistId)
        // Describe listener for playlist data
        playlistListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _playlistName.value =  snapshot.child("name").getValue(String::class.java) // Update playlist name
               val playlistAuthor = snapshot.child("author").getValue(String::class.java)  // Get playlist author
               _isMyPlaylist.value = (playlistAuthor == currentUsername)                   // Update access rights indicator

                // Get playlist image
                val photoUrlString = snapshot.child("photo_URL").getValue(String::class.java)
                _playlistImageUri.value = if (photoUrlString != null) Uri.parse(photoUrlString) else null   
                // Get playlist tracks
                val tracks = snapshot.child("tracks").children
                    .mapNotNull { it.getValue(String::class.java) }
                _song_id.value=tracks
                // Fetch songs metadata from their IDs paths
                fetchSongsFromGsPaths(tracks)
            }
            override fun onCancelled(error: DatabaseError) {
                // Optionally handle errors
            }
        }
        // Attach listener to correct place in Realtime Database
        ref.addValueEventListener(playlistListener as ValueEventListener)
    }

    // Update search query and filter songs accordingly
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterSongs(query)
    }

    // Filter songs based on search query (case-insensitive, starts with)
    private fun filterSongs(query: String) {
        // If query is empty, show all songs
        val filtered = if (query.isEmpty()) {
            _songs.value ?: emptyList()
        } else {
            _songs.value?.filter { song ->
                (song.title?:"").lowercase().startsWith(query.lowercase())
            } ?: emptyList()
        }
        _filteredSongs.value = filtered
    }

    // Load songs from their gs:// paths in Firebase Storage, extract metadata and store in LiveData
    private fun fetchSongsFromGsPaths(songPaths: List<String>) {
        val songList = MutableList<musicMetadata>(songPaths.size){ musicMetadata() }    // Temporary list to hold songs
        _songs.value=List<musicMetadata>(songPaths.size){ musicMetadata(title = null) } // Indicate loading state

        if (songPaths.isEmpty()) {
            _songs.value = emptyList()
            _filteredSongs.value = emptyList()
            return
        }
        // For each song path in playlist
        songPaths.forEachIndexed() { idx, gsPath ->
            // Get reference from gs:// path
            val fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(gsPath)
            // Download MP3 directly to cache directory
            val tempFile = File(context.cacheDir, fileRef.name)
            fileRef.getFile(tempFile)
                .addOnSuccessListener {
                    // Set up retriever to extract metadata
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(tempFile.absolutePath)
                    // Extract title
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?: fileRef.name.replace(".mp3", "")
                    // Extract cover image
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

    // Get queue of song datapaths for playback
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

    // Check if playlist songs are fully loaded (all titles have been changed from their initial null value)
    fun isPlaylistLoaded():Boolean{
        _songs.value.forEach{song->
            if (song.title==null){
                return false
            }
        }
        return true
    }
}


