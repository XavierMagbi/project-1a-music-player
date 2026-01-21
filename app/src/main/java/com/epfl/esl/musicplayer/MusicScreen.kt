package com.epfl.esl.musicplayer

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MusicScreen(
    application: Application,
    modifier: Modifier = Modifier,
    currentUsername: String = "",
    onSongClicked:(Int,List<String>)->Unit,
    onAddQueue: (String)->Unit,
    //playScreenViewModel: PlayScreenViewModel = viewModel(),
    musicScreenViewModel: MusicScreenViewModel= viewModel(),
) {
    val window by musicScreenViewModel.window.observeAsState()
    val selectedPlaylistId by musicScreenViewModel.selectedPlaylistId.observeAsState()

    if (window == "playlist"){
        PlaylistScreen(
            modifier = modifier,
            currentUsername = currentUsername,
            onPlaylistClicked = { playlistId ->
                musicScreenViewModel.openPlaylist( playlistId)
            }
        )
    } else if (window == "playscreen"){
        selectedPlaylistId?.let { playlistId ->
            SelectedPlaylistScreen(
                application = application,
                playlistId = playlistId,
                onSongClicked = onSongClicked,
                currentUsername = currentUsername,
                onAddQueue =onAddQueue
            )
        }

    }
}