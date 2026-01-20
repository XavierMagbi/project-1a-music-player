package com.epfl.esl.musicplayer

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.FirebaseDatabase
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


data class playlistMetadata (
    val title: String? = "",
    val creator: String? = "",
    val id: String? = "",
    val imageUri: Uri? = null
)

//Week 5: ViewModels and System Services (slide 8)
class PlaylistViewModelFactory(
    private val application: Application,
    private val currentUsername: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaylistViewModel(application, currentUsername) as T
    }
}

class PlaylistViewModel(
    application: Application,
    private val currentUsername: String
): AndroidViewModel(application) {
    private val playlistRef = FirebaseDatabase.getInstance().getReference("Playlists")
    private val profilesRef = FirebaseDatabase.getInstance().getReference("Profiles")

    private val _myPlaylists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val myPlaylists: LiveData<List<playlistMetadata>> = _myPlaylists

    private val _friendsPlaylists = MutableLiveData<List<playlistMetadata>>(emptyList())
    val friendsPlaylists: LiveData<List<playlistMetadata>> = _friendsPlaylists

    init {
        loadMyPlaylists()
        loadFriendsPlaylists()
    }

    fun loadMyPlaylists() {
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>()

            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""
                val id = child.key ?: ""

                // Get playlist image (stored as string in Realtime Database hence need to convert to URI)
                val photoUrlString = child.child("photo_URL").getValue(String::class.java)
                val imageUri = if (photoUrlString != null) Uri.parse(photoUrlString) else null

                if (playlistCreator == currentUsername) {
                    results.add(playlistMetadata(playlistName, playlistCreator, id, imageUri))
                }
            }

            _myPlaylists.value = results
        }
    }

    fun loadFriendsPlaylists(){
        // Get current user profile to get his friends (Requires change in Firebase rules. Required username indexation)
        profilesRef.orderByChild("username").equalTo(currentUsername).get().addOnSuccessListener { snapshot ->
            // Get friends usernames (Requires such a split with filterFriendsPlaylists to avoid race conditions)

            snapshot.children.forEach { myProfile ->
                val friendUsernames = mutableListOf<String>()
                var loadedCount = 0

                val currentUserId = myProfile.key

                // Get current user friends
                val friendsRef = profilesRef.child(currentUserId!!).child("Friends")

                friendsRef.get().addOnSuccessListener { friendsSnapshot ->
                    val friendIds = friendsSnapshot.children.map { it.key }.filterNotNull()

                    // Get the friends usernames (not ID because "author" is in username)
                    friendIds.forEach { friendId ->
                        profilesRef.child(friendId).child("username").get().addOnSuccessListener { usernameSnapshot ->
                            val friendUsername = usernameSnapshot.value as? String
                            if (friendUsername != null) {
                                friendUsernames.add(friendUsername)
                            }

                            loadedCount++

                            if (loadedCount == friendIds.size) {
                                filterFriendsPlaylists(friendUsernames)
                            }
                        }
                    }
                }
            }
        }
    }

    fun filterFriendsPlaylists(friendUsernames: List<String>){
        // Filter playlists by username
        playlistRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<playlistMetadata>()

            snapshot.children.forEach { child ->
                val playlistName = child.child("name").getValue(String::class.java)
                val playlistCreator = child.child("author").getValue(String::class.java) ?: ""
                val id = child.key ?: ""

                // Get playlist image (stored as string in Realtime Database hence need to convert to URI)
                val photoUrlString = child.child("photo_URL").getValue(String::class.java)
                val imageUri = if (photoUrlString != null) Uri.parse(photoUrlString) else null

                if (friendUsernames.contains(playlistCreator)) {
                    results.add(playlistMetadata(playlistName, playlistCreator, id, imageUri))
                }
            }
            _friendsPlaylists.value = results
        }
    }

    fun addPlaylist(name: String) {
        if (name.isBlank()) return

        val key = playlistRef.push().key ?: return
        val playlist = mapOf(
            "name" to name,
            "author" to currentUsername
        )
        playlistRef.child(key).setValue(playlist).addOnSuccessListener {
            loadMyPlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId.isBlank()) return

        playlistRef.child(playlistId).removeValue().addOnSuccessListener {
            loadMyPlaylists()
        }

    }

}