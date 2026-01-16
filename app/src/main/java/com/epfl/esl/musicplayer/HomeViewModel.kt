package com.epfl.esl.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.FirebaseDatabase

class HomeScreenViewModel:ViewModel() {

    private val dataBase = FirebaseDatabase.getInstance()
    private val usersRef = dataBase.getReference("Profiles")

    // For search query
    private val _foundUsers = MutableLiveData<List<String>>(emptyList())
    val foundUsers: LiveData<List<String>> = _foundUsers

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _foundUsers.value = emptyList()
            return
        }

        // Fetches profiles from database and checks if username starts with query
        usersRef.get().addOnSuccessListener { snapshot ->
            val results = mutableListOf<String>()


            snapshot.children.forEach { child ->
                val username = child.child("username").getValue(String::class.java)
                // If username matches query then save this into new list
                if (username != null && username.lowercase().startsWith(query.lowercase())) {
                    results.add(username)
                }
            }

            _foundUsers.value = results
        }.addOnFailureListener {
            _foundUsers.value = emptyList()
        }
    }
}