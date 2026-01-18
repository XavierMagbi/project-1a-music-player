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

data class UserProfile(
    val username: String,
    val image: Bitmap? = null
)

class HomeScreenViewModel:ViewModel() {
    // Firebase
    private val dataBase = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = dataBase.getReference("Profiles")
    private val imageRef = storage.getReference("ProfileImages")
    private val auth = FirebaseAuth.getInstance()

    // For search query
    private val _foundUsers = MutableLiveData<List<UserProfile>>(emptyList())
    val foundUsers: LiveData<List<UserProfile>> = _foundUsers

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _foundUsers.value = emptyList()
            return
        }

        // Fetches profiles from database and checks if username starts with query
        usersRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<UserProfile>()

            snapshot.children.forEach { child ->
                val username = child.child("username").getValue(String::class.java)
                val picturePath = child.child("photo_URL").getValue(String::class.java) ?: ""

                // If username matches query then save this into new list
                if (username != null && username.lowercase().startsWith(query.lowercase())) {
                    // ✅ FETCH BITMAP ICI
                    val filePath = picturePath.replace("gs://muzikproject1a.firebasestorage.app/", "")
                    storage.reference.child(filePath).getBytes(Long.MAX_VALUE)
                        .addOnSuccessListener { bytes ->
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            results.add(UserProfile(username, bitmap))
                            _foundUsers.value = results.toList()
                        }
                        .addOnFailureListener {
                            results.add(UserProfile(username, null))
                            _foundUsers.value = results.toList()
                        }
                }
            }

            _foundUsers.value = results
        }.addOnFailureListener {
            _foundUsers.value = emptyList()
        }
    }

    fun addFriend(pressedUsername: String, currentUsername: String){
        // Fetches profiles from database
        usersRef.get().addOnSuccessListener { snapshot ->
            var currentUserUid: String? = null
            var pressedUserUid: String? = null

            // Find UID linked to current user and targeted friend
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
            usersRef.child(currentUserUid!!).child("Friends").child(pressedUserUid!!).setValue(true)

        }.addOnFailureListener { error ->
            Log.e("AddFriend", "Error: ${error.message}")
        }
    }
}