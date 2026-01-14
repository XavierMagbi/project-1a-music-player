package com.epfl.esl.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.*

class PlaylistViewModel(playlistId: String) : ViewModel() {

    private val database = FirebaseDatabase.getInstance()
    private val playlistsRef = database.getReference("Playlists")
    private val songsRef = database.getReference("Musics")

    // Selected playlist
    private val _playlistName = MutableLiveData<String>("")
    val playlistName: LiveData<String> get() = _playlistName

    // Songs in the playlist
    private val _songs = MutableLiveData<List<SongItem>>(emptyList())
    val songs: LiveData<List<SongItem>> get() = _songs

    // Listener references so we can remove them in onCleared
    private var playlistListener: ValueEventListener? = null

    init {
        // Start listening for playlist changes
        listenToPlaylist(playlistId)
    }

    private fun listenToPlaylist(playlistId: String) {
        val ref = playlistsRef.child(playlistId)

        playlistListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playlist = snapshot.getValue(PlaylistItem::class.java)
                _playlistName.value = playlist?.name ?: ""

                val trackIds = playlist?.tracks ?: emptyList()
                fetchSongsByIds(trackIds) { songList ->
                    _songs.value = songList
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Optionally handle errors
            }
        }

        ref.addValueEventListener(playlistListener as ValueEventListener)
    }

    private fun fetchSongsByIds(
        songIds: List<String>,
        onResult: (List<SongItem>) -> Unit
    ) {
        if (songIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        val resultList = mutableListOf<SongItem>()
        var completedRequests = 0

        songIds.forEach { id ->
            songsRef.child(id).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.getValue(SongItem::class.java)?.let { resultList.add(it) }
                    completedRequests++
                    if (completedRequests == songIds.size) onResult(resultList)
                }
                .addOnFailureListener {
                    completedRequests++
                    if (completedRequests == songIds.size) onResult(resultList)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playlistListener?.let { playlistsRef.removeEventListener(it) }
    }
}


class PlaylistViewModelFactory(private val playlistId: String) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaylistViewModel(playlistId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}