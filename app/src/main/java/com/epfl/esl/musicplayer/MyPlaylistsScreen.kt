package com.epfl.esl.musicplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MyPlaylistsScreen(onPlaylistClicked: (String) -> Unit,
                      myPlalistsViewModel: MyPlaylistsViewModel = viewModel(),
                      modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {



        var showDialog by remember { mutableStateOf(false) }
        val playlists by myPlalistsViewModel.playlists.observeAsState()

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add playlist")
                }
            }
        ) { padding ->
            Column( modifier = Modifier.padding(padding))
            {
                //stylize text
                Text(text = stringResource(id = R.string.my_playlist_text) )
                playlists?.let {
                    LazyColumn{
                        items(it) { playlist ->
                            PlaylistItemRow(playlistItem = playlist,
                                onPlaylistClicked=onPlaylistClicked,
                                onDeleteClicked ={id->
                                    myPlalistsViewModel.deletePlaylist(id)
                                } ,
                                modifier=modifier.padding(8.dp))
                        }
                    }
                }
                if (showDialog) {
                    AddPlaylistDialog(
                        onAdd = { name ->
                            myPlalistsViewModel.addPlaylist(name)
                            showDialog = false
                        },
                        onCancel = {
                            showDialog = false
                        }
                    )
                }


            }

        }


    }

}


//Add playlist dialog composable, ref:
// developer.android.com/develop/ui/compose/components/dialog
@Composable
fun AddPlaylistDialog(onAdd: (String)->Unit,
                      onCancel:()->Unit,
                      modifier: Modifier = Modifier)
{
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("New playlist") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Playlist name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(text)
                }
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


