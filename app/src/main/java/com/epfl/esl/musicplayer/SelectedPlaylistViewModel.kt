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
import java.io.ByteArrayOutputStream


class SelectedPlaylistViewModel(application : Application, playlistId: String) : AndroidViewModel(application){
    val context = getApplication<Application>().applicationContext
    private val playlistId = playlistId

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

    private val _newQueue = MutableLiveData<List<String>>(emptyList())
    val newQueue : LiveData<List<String>> = _newQueue

    private val _playlistImageUri = MutableLiveData<Uri?>(null)
    val playlistImageUri: LiveData<Uri?> = _playlistImageUri   

    init {
        // Start listening for playlist changes
        listenToPlaylist(playlistId)

    }
    private fun listenToPlaylist(playlistId: String) {

        val ref = playlistsRef.child(playlistId)

        playlistListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _playlistName.value =  snapshot.child("name").getValue(String::class.java)

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
                song.title!!.lowercase().startsWith(query.lowercase())
            } ?: emptyList()
        }
        _filteredSongs.value = filtered
    }

    private fun fetchSongsFromGsPaths(songPaths: List<String>) {
        val songList = mutableListOf<musicMetadata>()
        _newQueue.value= emptyList() // reset the queue for this playlist

        if (songPaths.isEmpty()) {
            _songs.value = emptyList()
            _filteredSongs.value = emptyList()
            return
        }

        songPaths.forEach { gsPath ->
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
                    songList.add(
                        musicMetadata(
                            title = title,
                            image = coverImage,
                            link = fileRef.downloadUrl.toString() // optional, can keep the URI
                        )
                    )

                    // Add to playback queue
                    _newQueue.value =_newQueue.value + (tempFile.absolutePath)

                    // Update LiveData for UI
                    _songs.value = songList.toList()
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

        playlistImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
            _playlistImageUri.value = downloadUrl  // Update LiveData with new URI
            playlistsRef.child(playlistId).child("photo_URL").setValue(downloadUrl.toString())  // Update in Realtime Database
        }
    }
}



class SelectedPlaylistViewModelFactory(
    private val playlistId: String,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectedPlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SelectedPlaylistViewModel( playlistId =playlistId, application = application ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
