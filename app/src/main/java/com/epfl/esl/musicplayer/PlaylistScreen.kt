package com.epfl.esl.musicplayer

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    onPlaylistClicked:(String)->Unit,
    currentUsername: String = "",
    playlistViewModel: PlaylistViewModel = viewModel (
        factory = PlaylistViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            currentUsername)
    )
) {
    // To add playlist
    var showAddDialog by remember { mutableStateOf(false) }
    // To delete playlist
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf(-1) }

    val context = LocalContext.current

    val myPlaylists by playlistViewModel.myPlaylists.observeAsState(emptyList())
    val friendsPlaylists by playlistViewModel.friendsPlaylists.observeAsState(emptyList())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add playlist")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                LazyColumn(modifier = Modifier.weight(0.5f)) {
                    item{
                        Text(
                            text = "Your playlists",
                        )
                    }
                    items(myPlaylists.size){ index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable(onClick = {
                                    myPlaylists[index].id?.let { id ->
                                        onPlaylistClicked(id)
                                    }
                                })
                            ,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (myPlaylists[index].imageUri != null){
                                AsyncImage(
                                    model = myPlaylists[index].imageUri,
                                    contentDescription = "Playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.defaultplaylist),
                                    contentDescription = "Default playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = "${myPlaylists[index].title ?: "Unknown title"} by ${myPlaylists[index].creator ?: "Unknown creator"}",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    deleteIndex = index
                                    showDeleteDialog = true
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
                LazyColumn(modifier = Modifier.weight(0.5f)) {
                    item{
                        Text(
                            text = "Your friends' playlists",
                        )
                    }
                    items(friendsPlaylists.size){ index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable(onClick = {
                                    friendsPlaylists[index].id?.let { id ->
                                        onPlaylistClicked(id)
                                    }
                                })
                            ,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (friendsPlaylists[index].imageUri != null){
                                AsyncImage(
                                    model = friendsPlaylists[index].imageUri,
                                    contentDescription = "Playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.defaultplaylist),
                                    contentDescription = "Default playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            Text(
                                text = "${friendsPlaylists[index].title ?: "Unknown title"} by ${friendsPlaylists[index].creator ?: "Unknown creator"}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddPlaylistDialog(
                onAdd = { playlistName ->
                    playlistViewModel.addPlaylist(playlistName)
                    showAddDialog = false
                },
                onCancel = {
                    showAddDialog = false
                }
            )
        }

        if (showDeleteDialog){
            DeletePlaylistDialog(
                index = deleteIndex,
                onDelete = { index ->
                    playlistViewModel.deletePlaylist(myPlaylists[index].id ?: "")
                    Toast.makeText(context, "Deleted playlist", Toast.LENGTH_SHORT).show()
                    showDeleteDialog = false
                },
                onCancel = {
                    showDeleteDialog = false
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

@Composable
fun DeletePlaylistDialog(index: Int,
                        onDelete:(Int)->Unit,
                        onCancel:()->Unit,
                        modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("") },
        text = {
            Text("Do you really want to delete this playlist?")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(index)
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

