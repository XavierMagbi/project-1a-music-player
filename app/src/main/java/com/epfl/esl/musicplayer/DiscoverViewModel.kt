package com.epfl.esl.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class DiscoverViewModel: ViewModel() {
    var storageRef = FirebaseStorage.getInstance().getReference()

    private var _songs = MutableLiveData<List<SongItem>>(listOf())
    val songs: LiveData<List<SongItem>>
        get() = _songs

    private var _playlists = MutableLiveData<List<PlaylistItem>>(listOf())
    val playlists: LiveData<List<PlaylistItem>>
        get() = _playlists


    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val songsRef: DatabaseReference = database.getReference("Musics")
    val playlistRef: DatabaseReference = database.getReference("Playlists")

    init {
        listenForSongs()
        listenForPlaylist()
    }

    fun listenForSongs(username: String? = null) { //string for later when it only listens for a specific profile
        songsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val arrayList: ArrayList<SongItem> = ArrayList()
                for (child in dataSnapshot.children) {
                    val song = child.getValue(SongItem::class.java)

                    if (song != null) {
                        // Optional filtering by author
                        /*
                        if (author == null || playlist.author == author) {
                            list.add(playlist)
                        }
                         */
                        arrayList.add(song)
                    }
                }
                _songs.value = arrayList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun listenForPlaylist(username: String? = null) { //string for later when it only listens for a specific profile
        playlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val arrayList: ArrayList<PlaylistItem> = ArrayList()
                for (child in dataSnapshot.children) {
                    val playlist = child.getValue(PlaylistItem::class.java)

                    if (playlist != null) {
                        // Optional filtering by author
                        /*
                        if (author == null || playlist.author == author) {
                            list.add(playlist)
                        }
                         */
                        arrayList.add(playlist)
                    }
                }
                _playlists.value = arrayList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addSongToPlaylist(playlistId:String,songId:String){
        val playlistRef = database.getReference("Playlists").child(playlistId)

        playlistRef.get().addOnSuccessListener { snapshot ->
            val playlist = snapshot.getValue(PlaylistItem::class.java)
            val currentTracks = playlist?.tracks ?: emptyList()
            val updatedTracks = currentTracks + songId
            playlistRef.child("tracks").setValue(updatedTracks)
        }

    }
}