package com.epfl.esl.musicplayer

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel

/*
    Music Screen Composable

    Functionality:
    Top level screen
    Alternate between Playlist Screen and Selected Playlist Screen based on ViewModel state

    Navigation:
    Can navigate to it via bottom bar
 */

// Music Screen Composable
@Composable
fun MusicScreen(
    application: Application,
    modifier: Modifier = Modifier,
    currentUsername: String = "",                           // To load user and friends' playlists (on Playlist Screen and Selected Playlist Screen)
    onSongClicked:(Int,List<String>)->Unit,                 // To play song when clicked (on Selected Playlist Screen)
    onAddQueue: (String)->Unit,                             // To add song to queue when clicked (on Selected Playlist Screen)
    musicScreenViewModel: MusicScreenViewModel= viewModel()
) {
    // Get variables from ViewModel
    val window by musicScreenViewModel.window.observeAsState() // To indicate which screen to show
    val selectedPlaylistId by musicScreenViewModel.selectedPlaylistId.observeAsState() // To indicate which playlist to show (on Selected Playlist Screen)

    if (window == "playlist"){
        PlaylistScreen(
            modifier = modifier,
            currentUsername = currentUsername,
            // Navigate to Selected Playlist Screen when a playlist is clicked
            onPlaylistClicked = { playlistId ->
                musicScreenViewModel.openPlaylist( playlistId)
            }
        )
    } else if (window == "playscreen"){
        selectedPlaylistId?.let { playlistId ->
            SelectedPlaylistScreen(
                application = application,
                playlistId = playlistId,        // To specify which playlist to load
                onSongClicked = onSongClicked,  // To playback selected song
                currentUsername = currentUsername,
                onAddQueue =onAddQueue          // To add song to queue
            )
        }
    }
}