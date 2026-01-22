package com.epfl.esl.musicplayer

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/*
    Playlist Screen ViewModel

    Functionality:
    Displays user playlists and friends playlists, allows adding and deleting playlists (only acting on personal playlists)

    Interacts with:
    Firebase Realtime Database - to fetch user and friends' playlists metadata
 */

// ViewModel Factory as currentUsername is required to identify user and friends' playlists
// Inspiration EE-490(g) Week 5: ViewModels and System Services (slide 8)
class PlaylistViewModelFactory(
    private val application: Application,
    private val currentUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaylistViewModel(application, currentUsername) as T
    }
}

// ViewModel for PlaylistScreen
class PlaylistViewModel(
    application: Application,
    private val currentUsername: String
): AndroidViewModel(application) {
    // Firebase Realtime Database references
    private val playlistRef = FirebaseDatabase.getInstance().getReference("Playlists")
    private val profilesRef = FirebaseDatabase.getInstance().getReference("Profiles")

    // ViewModel LiveData variables
    private val _myPlaylists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val myPlaylists: LiveData<List<playlistMetadata>> = _myPlaylists

    private val _friendsPlaylists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val friendsPlaylists: LiveData<List<playlistMetadata>> = _friendsPlaylists

    // Initialize by loading user and friends' playlists
    init {
        loadMyPlaylists()
        loadFriendsPlaylists()
    }
    
    // Load user's playlists from Firebase Realtime Database
    private fun loadMyPlaylists() {
        // Filter playlists by username
        // Could be optimized by restructuring Firebase by having same ID as user and have each personnal playlist
        // as sub-node with the corresponding user ID. For next project iteration :)
        playlistRef.get().addOnSuccessListener { snapshot ->
            // Temporary list to hold playlists
            val results = mutableListOf<playlistMetadata>()

            // Iterate through each playlist
            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)             // Get playlist name
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""  // Get playlist author
                val id = child.key ?: ""                                                        // Get playlist ID (For SelectedPlaylistScreen)      
                val photoUrlString = child.child("photo_URL").getValue(String::class.java)      // Get playlist image
                val imageUri = if (photoUrlString != null) Uri.parse(photoUrlString) else null  // Set it to URI
                // If playlist author is current user add to user playlists
                if (playlistCreator == currentUsername) {                          
                    results.add(playlistMetadata(playlistName, playlistCreator, id, imageUri))
                }
            }
            _myPlaylists.value = results // Update LiveData
        }
    }

    // Load friends' playlists from Firebase Realtime Database
    // Get friends' usernames first, then filter playlists
    fun loadFriendsPlaylists(){
        // Would also benefit from restructuring Firebase DB for optimization in next iteration :)
        // to avoid looping over playlists to get friends' playlists
        // First get current user ID from username for query to get friends list
        profilesRef.orderByChild("username").equalTo(currentUsername).get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { myProfile ->
                val friendUsernames = mutableListOf<String>() // To hold friends usernames
                var loadedCount = 0                           // To track number of loaded friends
                val currentUserId = myProfile.key             // Get current user ID

                // Assume user ID is not null as user is logged in
                val friendsRef = profilesRef.child(currentUserId!!).child("Friends")
                // Get friends IDs
                friendsRef.get().addOnSuccessListener { friendsSnapshot ->
                    val friendIds = friendsSnapshot.children.map { it.key }.filterNotNull()
                    // Get the friends usernames (ID is not sufficient because "author" is in username)
                    friendIds.forEach { friendId ->
                        profilesRef.child(friendId).child("username").get().addOnSuccessListener { usernameSnapshot ->
                            val friendUsername = usernameSnapshot.value as? String
                            if (friendUsername != null) {
                                friendUsernames.add(friendUsername)
                            }
                            loadedCount++
                            // Once all friends' usernames are loaded, filter playlists
                            if (loadedCount == friendIds.size) {
                                filterFriendsPlaylists(friendUsernames)
                            }
                        }
                    }
                }
            }
        }
    }

    // Load friends' playlists based on friends' usernames
    // Could modularize a single function with loadMyPlaylists but for clarity in this first iterations keep separate :)
    fun filterFriendsPlaylists(friendUsernames: List<String>){
        // Filter playlists by username
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>() // To hold friends' playlists
            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)             // Get playlist name
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""  // Get playlist author
                val id = child.key ?: ""                                                        // Get playlist ID (For SelectedPlaylistScreen)
                val photoUrlString = child.child("photo_URL").getValue(String::class.java)      // Get playlist image
                val imageUri = if (photoUrlString != null) Uri.parse(photoUrlString) else null
                // If playlist author is part of friends list, add to friends' playlists
                if (friendUsernames.contains(playlistCreator)) {
                    results.add(playlistMetadata(playlistName, playlistCreator, id, imageUri))
                }
            }
            _friendsPlaylists.value = results
        }
    }

    // Add new playlist to Firebase Realtime Database
    fun addPlaylist(name: String) {
        if (name.isBlank()) return // If name is empty, do nothing. Such logic could also be implemented in Dialog to unable "Add" button if selection is empty
        // Create new empty playlist
        val key = playlistRef.push().key ?: return
        val playlist = mapOf(
            "name" to name,
            "author" to currentUsername
        )
        playlistRef.child(key).setValue(playlist).addOnSuccessListener {
            // Reload user playlists to showcase new playlist
            loadMyPlaylists()
        }
    }

    // Delete user playlist from Firebase Realtime Database
    fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return
        playlistRef.child(playlistId).removeValue().addOnSuccessListener {
            // Reload user playlists to reflect deletion
            loadMyPlaylists()
        }
    }
}