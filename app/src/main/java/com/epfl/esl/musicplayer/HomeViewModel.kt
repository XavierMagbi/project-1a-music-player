package com.epfl.esl.musicplayer

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

data class UserProfile(
    val username: String,
    val picturePath: String
)

class HomeScreenViewModel:ViewModel() {
    // Firebase
    private val dataBase = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = dataBase.getReference("Profiles")
    private val imageRef = storage.getReference("ProfileImages")

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
                    results.add(UserProfile(username, picturePath))
                }
            }

            _foundUsers.value = results
        }.addOnFailureListener {
            _foundUsers.value = emptyList()
        }
    }
}