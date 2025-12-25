package com.epfl.esl.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

val samplePlaylists = listOf(
    PlaylistItem(name = "Chill Vibes", author = "Spotify"),
    PlaylistItem(name = "Workout Hits", author = "Nike Training"),
    PlaylistItem(name = "Late Night Coding", author = "Dev Beats"),
    PlaylistItem(name = "Morning Acoustic", author = "Indie Folk"),
    PlaylistItem(name = "Top 50 Global", author = "Charts"),
    PlaylistItem(name = "Chill Vibes", author = "Spotify"),
    PlaylistItem(name = "Workout Hits", author = "Nike Training"),
    PlaylistItem(name = "Late Night Coding", author = "Dev Beats"),
    PlaylistItem(name = "Morning Acoustic", author = "Indie Folk"),
    PlaylistItem(name = "Top 50 Global", author = "Charts"),
    PlaylistItem(name = "Chill Vibes", author = "Spotify"),
    PlaylistItem(name = "Workout Hits", author = "Nike Training"),
    PlaylistItem(name = "Late Night Coding", author = "Dev Beats"),
    PlaylistItem(name = "Morning Acoustic", author = "Indie Folk"),
    PlaylistItem(name = "Top 50 Global", author = "Charts"),
)

class HomeViewModel:ViewModel() {
    //private var _playlists = MutableLiveData<List<PlaylistItem>>(listOf())
    //for initial debug
    private var _playlists = MutableLiveData<List<PlaylistItem>>(samplePlaylists)
    val playlists: LiveData<List<PlaylistItem>>
        get() = _playlists

}