package com.epfl.esl.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/*
    Music Screen ViewModel

    Functionality:
    Update top level "window" state and updates selected playlist ID accordingly
 */

 // ViewModel for MusicScreen
class MusicScreenViewModel : ViewModel() {
    // ViewModel LiveData variables
    private val _window =MutableLiveData<String>("playlist")
    val window :LiveData<String> get()=_window

    private val _selectedPlaylistID = MutableLiveData<String>("")
    val selectedPlaylistId: LiveData<String>  get()=_selectedPlaylistID

    // Called when a playlist is selected to open Selected Playlist Screen
    fun openPlaylist(playlistId: String) {
        _selectedPlaylistID.value = playlistId
        _window.value = "playscreen"
    }

    // Called to return to Playlist Screen (called in bottom bar navigation)
    fun showPlaylists() {
        _window.value = "playlist"
    }
}
