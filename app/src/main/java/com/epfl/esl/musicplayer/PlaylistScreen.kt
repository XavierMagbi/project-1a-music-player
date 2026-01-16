package com.epfl.esl.musicplayer

import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.database.FirebaseDatabase
import java.io.File

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    playlistViewModel: PlaylistViewModel = viewModel(),
    currentUsername: String = ""
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val playlists by playlistViewModel.playlists.observeAsState(emptyList())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add playlist")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyColumn() {
                    items(playlists.size){ index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${playlists[index].title ?: "Unknown title"} by ${playlists[index].creator ?: "Unknown creator"}",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    playlistViewModel.deletePlaylist(playlists[index].id ?: "")
                                    Toast.makeText(context, "Deleted playlist", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete playlist button"
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddPlaylistDialog(
                onAdd = { playlistName ->
                    playlistViewModel.addPlaylist(playlistName, currentUsername)
                    showDialog = false
                },
                onCancel = {
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddPlaylistDialog(onAdd: (String)->Unit,
                      onCancel:()->Unit,
                      modifier: Modifier = Modifier) {
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

