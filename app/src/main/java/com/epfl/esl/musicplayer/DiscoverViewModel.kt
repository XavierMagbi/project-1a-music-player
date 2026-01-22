package com.epfl.esl.musicplayer

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.net.URLDecoder
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider

/*
    Discover Screen ViewModel

    Functionality:
    Handles searching for songs and adding them to user playlists

    Interacts with:
    Firebase Realtime Database - to fetch user playlists and add songs to them
    Firebase Storage - to fetch songs .mp3 files and extract metadata
 */

// ViewModel Factory as currentUsername is required to identify user's playlists
// Inspiration EE-490(g) Week 5: ViewModels and System Services (slide 8)
class DiscoverViewModelFactory(
    private val application: Application,
    private val currentUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscoverViewModel(application, currentUsername) as T
    }
}

// ViewModel for DiscoverScreen
class DiscoverViewModel(
    application: Application,
    private val currentUsername: String
): AndroidViewModel(application) {
    // Get context to access cache directory
    val context = getApplication<Application>().applicationContext

    // Firebase Realtime Database and Storage references
    private val musicRef = FirebaseStorage.getInstance().getReference("Musics")
    private val playlistRef = FirebaseDatabase.getInstance().getReference("Playlists")

    // ViewModel LiveData variables
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _filteredSongs = MutableLiveData<List<musicMetadata>>(emptyList())
    val filteredSongs: LiveData<List<musicMetadata>> = _filteredSongs

    private val _playlists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val playlists: LiveData<List<playlistMetadata>> = _playlists

    private val _songs = MutableLiveData<List<musicMetadata>>(emptyList())

    // Initialize by loading songs and playlists
    init {
        loadSongs()
        loadPlaylists()
    }

    // Load songs from Firebase Storage, extract metadata and store in LiveData
    private fun loadSongs() {
        // List all music files in Firebase Storage
        // May not be scalable for large number of files but sufficient for demo purposes
        // Next iterations may propose randomly selected songs instead of all and user would need to query for specific songs :)
        musicRef.listAll().addOnSuccessListener { listResult ->
            // Temporary list to hold songs
            val songList = mutableListOf<musicMetadata>()

            // Iterate through each music file
            listResult.items.forEach { fileRef ->
                val link = URLDecoder.decode(fileRef.toString(), "UTF-8") // Otherwise spaces are "%20"

                // Download the file bytes
                fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                    try {
                        // Create temp file to extract metadata
                        val tempFile = File.createTempFile("song", ".mp3")
                        tempFile.writeBytes(bytes)
                        // Extract metadata using MediaMetadataRetriever
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(tempFile.absolutePath)
                        // Get title and cover image
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: fileRef.name.replace(".mp3", "")
                        val coverImage = retriever.embeddedPicture
                        retriever.release()
                        // Download song and add link to metadata
                        val songFile = File(context.cacheDir, fileRef.name)
                        songFile.writeBytes(bytes)
                        // Add music metadata to list
                        songList.add(musicMetadata(title, coverImage, link, datapath = songFile.absolutePath))
                        _songs.value = songList.toList() // Update LiveData

                        // Filter songs based on current search query
                        filterSongs(_searchQuery.value ?: "")
                        // Delete temp file
                        tempFile.delete()
                    } catch (e: Exception) {
                        Log.e("DiscoverViewModel", "Error: ${e.message}")
                        songList.add(musicMetadata(fileRef.name.replace(".mp3", ""), null, link))
                        _songs.value = songList.toList()
                        filterSongs(_searchQuery.value ?: "")
                    }
                }.addOnFailureListener {
                    Log.e("DiscoverViewModel", "Error getting download URL: ${it.message}")
                }
            }
        }
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
                song.title!!.lowercase().startsWith(query.lowercase())
            } ?: emptyList()
        }
        _filteredSongs.value = filtered
    }

    // Load playlists from Firebase Realtime Database, extract its title metadata and store in LiveData
    private fun loadPlaylists() {
        // Get all playlists from database
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>()

            // For each playlist, check if created by current user and add to list
            snapshot.children.forEach { child ->
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""

                if (playlistCreator == currentUsername) {
                    val playlistName = child.child("name").getValue(String::class.java)
                    val id = child.key ?: ""
                    results.add(playlistMetadata(playlistName, playlistCreator, id))
                }
            }
            _playlists.value = results
        }
    }

    // Get current tracks in playlist, add new songId and update database
    fun addSongToPlaylist(playlistId:String, songId:String){
        // Get current tracks in playlist
        playlistRef.child(playlistId).get().addOnSuccessListener { snapshot ->
            val currentTracks = snapshot.child("tracks").value as? List<String> ?: emptyList()
            val updatedTracks = currentTracks.toMutableList()
            // Add new songId
            updatedTracks.add(songId)
            // Update playlist in database
            playlistRef.child(playlistId).child("tracks").setValue(updatedTracks)
        }
    }

    // Get list of song datapaths from filtered songs to be used for playback
    fun getSongIdList():List<String>{
        val IdList:MutableList<String> =mutableListOf()
        _filteredSongs.value.forEach{item ->
            if (item.datapath!="") {
                IdList.add(item.datapath)
            }
        }
        return IdList
    }
}