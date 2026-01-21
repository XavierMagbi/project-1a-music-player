package com.epfl.esl.musicplayer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth

/*
    Home Screen ViewModel

    Functionality:
    Handles searching for users and adding friends

    Interacts with:
    Firebase Realtime Database - to fetch user profiles and add friends
    Firebase Storage - to fetch profile pictures
 */

// Data class to hold user profile information to be displayed in HomeScreen
data class UserProfile(
    val username: String,
    val image: Bitmap? = null
)

// ViewModel for HomeScreen
class HomeScreenViewModel:ViewModel() {
    // Firebase Realtime Database and Storage references
    private val dataBase = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // References to database node
    private val usersRef = dataBase.getReference("Profiles")

    // ViewModel LiveData variables
    private val _foundUsers = MutableLiveData<List<UserProfile>>(emptyList())
    val foundUsers: LiveData<List<UserProfile>> = _foundUsers

    // Search for users whose usernames start with the given query (case insensitive)
    // Results are displayed dynamically as more users are loaded
    fun searchUsers(query: String) {
        // If query is empty, return empty list. To avoid getting all users from database.
        if (query.isEmpty()) {
            _foundUsers.value = emptyList()
            return
        }

        // Fetches profiles from database and checks if username starts with query
        usersRef.get().addOnSuccessListener { profiles ->
            // Temporary list to hold results
            val results = mutableListOf<UserProfile>()

            // Iterate through each user
            profiles.children.forEach { profile ->
                // Get username and picture GS path
                val username = profile.child("username").getValue(String::class.java)
                val picturePath = profile.child("photo_URL").getValue(String::class.java) ?: ""

                // If username matches query then save the user profile (username and profile picture) into new list
                if (username != null && username.lowercase().startsWith(query.lowercase())) {
                    val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(picturePath)

                    // Get profile picture bytes
                    imageRef.getBytes(Long.MAX_VALUE)
                        .addOnSuccessListener { bytes ->
                            // Convert bytes to Bitmap
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            // Add user profile to temporary list
                            results.add(UserProfile(username, bitmap))
                            // Update LiveData with new updated list
                            _foundUsers.value = results.toList()
                        }
                        .addOnFailureListener {
                            // If failed to get picture, add user profile with null image (no image will be displayed)
                            results.add(UserProfile(username, null))
                            // Update LiveData with new updatedlist
                            _foundUsers.value = results.toList()
                        }
                }
            }
        }.addOnFailureListener {
            _foundUsers.value = emptyList()
        }
    }

    // Adds the user with pressedUsername as a friend to the current user (currentUsername)
    // Simplified friend system. More like a "follow" system. Could be improved in future iterations where friend invites are sent :)
    fun addFriend(pressedUsername: String, currentUsername: String){
        // Fetches profiles from database
        usersRef.get().addOnSuccessListener { snapshot ->
            var currentUserUid: String? = null
            var pressedUserUid: String? = null

            // Find UID linked to current user and targeted friend
            // May not be most efficient but works for small user bases. To be improved in future iterations :)
            snapshot.children.forEach { child ->
                val username = child.child("username").getValue(String::class.java)
                val uid = child.key
                if (username == currentUsername) {
                    currentUserUid = uid
                }
                if (username == pressedUsername) {
                    pressedUserUid = uid
                }
            }
            // Add friend UID
            // Should never be null since button is only shown for existing users
            usersRef.child(currentUserUid!!).child("Friends").child(pressedUserUid!!).setValue(true)
        }.addOnFailureListener { error ->
            Log.e("AddFriend", "Error: ${error.message}")
        }
    }
}
