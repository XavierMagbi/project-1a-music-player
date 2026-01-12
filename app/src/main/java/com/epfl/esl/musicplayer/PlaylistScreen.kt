package com.epfl.esl.musicplayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlaylistScreen(playlistId: String) {
    val viewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModelFactory(playlistId))

    val playlistName by viewModel.playlistName.observeAsState("")
    val songs by viewModel.songs.observeAsState(emptyList())

    Column {


        Text(text = playlistName)
        LazyColumn {
            items(songs) { song ->
                //Text(text = song.Name)
                SongItemRow(
                    songItem = song,
                    onAddClicked={},
                    modifier = Modifier.padding(8.dp))
            }


        }

    }
}
