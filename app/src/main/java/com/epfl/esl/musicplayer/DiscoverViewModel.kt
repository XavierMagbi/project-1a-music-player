package com.epfl.esl.musicplayer


import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.FirebaseStorage
import java.io.File

data class musicMetadata (
    val title: String? = "",
    val image: ByteArray? = null
)

class DiscoverViewModel: ViewModel() {
    private val storage = FirebaseStorage.getInstance()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _songs = MutableLiveData<List<musicMetadata>>(emptyList())

    private val _filteredSongs = MutableLiveData<List<musicMetadata>>(emptyList())
    val filteredSongs: LiveData<List<musicMetadata>> = _filteredSongs

    init {
        loadSongs()
    }

    fun loadSongs() {
        val musicRef = storage.reference.child("Musics")

        musicRef.listAll().addOnSuccessListener { listResult ->
            val songList = mutableListOf<musicMetadata>()

            listResult.items.forEach { fileRef ->
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

                        songList.add(musicMetadata(title, coverImage))
                        _songs.value = songList.toList()

                        // So we do not need an inital query at start
                        filterSongs(_searchQuery.value ?: "")

                        tempFile.delete()
                    } catch (e: Exception) {
                        Log.e("DiscoverViewModel", "Error: ${e.message}")
                        songList.add(musicMetadata(fileRef.name.replace(".mp3", ""), null))
                        _songs.value = songList.toList()

                        // So we do not need an inital query at start
                        filterSongs(_searchQuery.value ?: "")
                    }
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
}