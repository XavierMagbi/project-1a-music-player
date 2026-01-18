package com.epfl.esl.musicplayer

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import android.app.Application


data class playlistMetadata (
    val title: String? = "",
    val creator: String? = "",
    val id: String? = ""
)

class PlaylistViewModel(
    application: Application
): AndroidViewModel(application) {
    private val playlistRef = FirebaseDatabase.getInstance().getReference("Playlists")

    private val _playlists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val playlists: LiveData<List<playlistMetadata>> = _playlists

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>()

            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""
                val id = child.key ?: ""

                results.add(playlistMetadata(playlistName, playlistCreator, id))
            }

            _playlists.value = results
        }
    }

    fun addPlaylist(name: String, currentUsername: String) {
        if (name.isBlank()) return

        val key = playlistRef.push().key ?: return
        val playlist = mapOf(
            "name" to name,
            "author" to currentUsername
        )
        playlistRef.child(key).setValue(playlist).addOnSuccessListener {
            loadPlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return

        playlistRef.child(playlistId).removeValue().addOnSuccessListener {
            loadPlaylists()
        }

    }

}