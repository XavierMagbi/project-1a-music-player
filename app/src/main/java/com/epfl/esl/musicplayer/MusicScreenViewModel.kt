package com.epfl.esl.musicplayer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MusicScreenViewModel : ViewModel() {
    private val _window =MutableLiveData<String>("playlist")
    val window :LiveData<String> get()=_window



    private val _playlistName = MutableLiveData<String>("")
    val playlistName: LiveData<String> get() = _playlistName

    private val _selectedPlaylistID = MutableLiveData<String>("")
    val selectedPlaylistId: LiveData<String>  get()=_selectedPlaylistID

    fun openPlaylist(playlistId: String) {
        _selectedPlaylistID.value = playlistId
        _window.value = "playscreen"
    }

    fun showPlaylists() {
        _window.value = "playlist"
    }
}
