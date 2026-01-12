package com.epfl.esl.musicplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DiscoverScreen(
    discoverViewModel: DiscoverViewModel= viewModel(),
    modifier: Modifier=Modifier
){
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var showDialog by remember { mutableStateOf(false) }
        var selectedSongId by remember { mutableStateOf("") }

        val songs by discoverViewModel.songs.observeAsState()
        val playlists by discoverViewModel.playlists.observeAsState()


        Column()
        {
            //stylize text
            Text(text = stringResource(id = R.string.discover_text))
            songs?.let {
                LazyColumn {
                    items(it) { song ->
                        SongItemRow(
                            songItem = song,
                            onAddClicked={
                                selectedSongId=song.Id
                                showDialog=true},
                            modifier = modifier.padding(8.dp))
                    }
                }
            }
            if(showDialog){
                AddSongToPlaylistDialog(
                    playlists = playlists?: emptyList(),
                    onAdd = {id->
                        discoverViewModel.addSongToPlaylist(
                            songId = selectedSongId,
                            playlistId = id
                        )
                        showDialog=false
                    },
                    onCancel = {showDialog=false})
            }
        }
    }

}













//composable to add song to a playlist
@Composable
fun AddSongToPlaylistDialog(
    playlists: List<PlaylistItem>,
    onAdd: (String) -> Unit,      // playlistId
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = { Text("Add to playlist") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp) // max height for scrolling
            ) {
                LazyColumn {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlaylistId = playlist.id }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlaylistId == playlist.id,
                                onClick = { selectedPlaylistId = playlist.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = playlist.name?:"")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedPlaylistId != null,
                onClick = { selectedPlaylistId?.let { onAdd(it) } }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
