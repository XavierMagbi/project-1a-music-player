package com.epfl.esl.musicplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(onPlayerClicked: () -> Unit,
               homeViewModel: HomeViewModel = viewModel(),
               modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val playlists by homeViewModel.playlists.observeAsState()
        playlists?.let {
            LazyColumn {
                items(it) { playlist ->
                    PlaylistItemRow(playlistItem = playlist, modifier=modifier.padding(8.dp))
                }
            }
        }

    }

}


@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen({})
    
}

