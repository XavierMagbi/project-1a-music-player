package com.epfl.esl.musicplayer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

val samplePlaylists = listOf(
    PlaylistItem(name = "Chill Vibes", author = "Spotify"),
    PlaylistItem(name = "Workout Hits", author = "Nike Training"),
    PlaylistItem(name = "Late Night Coding", author = "Dev Beats"),
    PlaylistItem(name = "Morning Acoustic", author = "Indie Folk"),
    PlaylistItem(name = "Top 50 Global", author = "Charts")
)

class HomeViewModel:ViewModel() {
    private var _playlists = MutableLiveData<List<PlaylistItem>>(listOf())
    //for initial debug
    //private var _playlists = MutableLiveData<List<PlaylistItem>>(samplePlaylists)
    val playlists: LiveData<List<PlaylistItem>>
        get() = _playlists


    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val playlistRef: DatabaseReference = database.getReference("Playlists")
    var key: String = ""

    init {
        listenForPlaylist()
    }


    //setter function to add a new playlist
    fun addPlaylist(name: String) {
        if (name.isBlank()) return

        val newPlaylist = PlaylistItem(
            name = name, author = "jon"
        )

        //replace with server call and refresh (or maybe a dedicated refresh button)
        //val current = _playlists.value ?: emptyList()
        //_playlists.value = current + newPlaylist
        addPlaylistToFireBase(name = name, context = null)

    }
    private fun addPlaylistToFireBase(name:String,context: Context?){
        key = playlistRef.push().key.toString()
        val playlist = PlaylistItem(
            id = key,
            name = name,
            author = "jon"
        )
        playlistRef.child(key).setValue(playlist)

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



}