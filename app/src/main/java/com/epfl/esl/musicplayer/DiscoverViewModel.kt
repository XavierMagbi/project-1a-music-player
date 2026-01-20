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

data class musicMetadata (
    val title: String? = "",
    val image: ByteArray? = null,
    val link: String? = "",
    val linkGS: String? = ""
)

//Week 5: ViewModels and System Services (slide 8)
class DiscoverViewModelFactory(
    private val application: Application,
    private val currentUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscoverViewModel(application, currentUsername) as T
    }
}

class DiscoverViewModel(
    application: Application,
    private val currentUsername: String
): AndroidViewModel(application) {
    private val storage = FirebaseStorage.getInstance()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _songs = MutableLiveData<List<musicMetadata>>(emptyList())

    private val _filteredSongs = MutableLiveData<List<musicMetadata>>(emptyList())
    val filteredSongs: LiveData<List<musicMetadata>> = _filteredSongs

    // For dialog
    private val playlistRef = FirebaseDatabase.getInstance().getReference("Playlists")

    private val _playlists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val playlists: LiveData<List<playlistMetadata>> = _playlists

    init {
        loadSongs()
        loadPlaylists()
    }

    fun loadSongs() {
        val musicRef = storage.reference.child("Musics")

        musicRef.listAll().addOnSuccessListener { listResult ->
            val songList = mutableListOf<musicMetadata>()

            listResult.items.forEach { fileRef ->
                val link = URLDecoder.decode(fileRef.toString(), "UTF-8") // Otherwise spaces are "%20"

                fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { bytes ->
                    try {
                        val tempFile = File.createTempFile("song", ".mp3")
                        tempFile.writeBytes(bytes)

                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(tempFile.absolutePath)

                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: fileRef.name.replace(".mp3", "")
                        val coverImage = retriever.embeddedPicture
                        retriever.release()

                        songList.add(musicMetadata(title, coverImage, link))
                        _songs.value = songList.toList()

                        filterSongs(_searchQuery.value ?: "")

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

    fun loadPlaylists() {
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>()

            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""
                val id = child.key ?: ""

                if (playlistCreator == currentUsername) {
                    results.add(playlistMetadata(playlistName, playlistCreator, id))
                }
            }

            _playlists.value = results
        }
    }

    fun addSongToPlaylist(playlistId:String, songId:String){
        playlistRef.child(playlistId).get().addOnSuccessListener { snapshot ->
            val currentTracks = snapshot.child("tracks").value as? List<String> ?: emptyList()
            val updatedTracks = currentTracks.toMutableList()
            updatedTracks.add(songId)

            playlistRef.child(playlistId).child("tracks").setValue(updatedTracks)
        }
    }
}