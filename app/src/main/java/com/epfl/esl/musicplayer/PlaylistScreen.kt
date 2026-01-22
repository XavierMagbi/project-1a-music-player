package com.epfl.esl.musicplayer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/*
    Playlist Screen Composable

    Functionality:
    Displays user's playlists and friends' playlists
    Allows adding new playlists and deleting existing ones (only user's own playlists)
    Allows navigating to SelectedPlaylistScreen when a playlist is clicked (with corresponding playlistId)

    Navigation:
    Can navigate to it via bottom bar (default screen for MusicScreen)
 */

 // Playlist Screen Composable
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    onPlaylistClicked:(String)->Unit,                   // To navigate to SelectedPlaylistScreen with the corresponding playlistId
    currentUsername: String = "",                       // To load user and friends' playlists
    playlistViewModel: PlaylistViewModel = viewModel (
        factory = PlaylistViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            currentUsername)
    )
) {
    // Get variables from ViewModel
    val myPlaylists by playlistViewModel.myPlaylists.observeAsState(emptyList())            // User's playlist
    val friendsPlaylists by playlistViewModel.friendsPlaylists.observeAsState(emptyList())  // Friends' playlists

    // Composable variables
    var showAddDialog by remember { mutableStateOf(false) }                                 // To show/hide add playlist dialog
    var showDeleteDialog by remember { mutableStateOf(false) }                              // To show/hide delete playlist dialog
    var deleteIndex by remember { mutableStateOf(-1) }                                      // To store index of playlist to delete
    val context = LocalContext.current                                                      // To show Toast messages

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            // Floating action button to add new playlist
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add playlist")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // User's playlists (Upper half of the screen)
                LazyColumn(modifier = Modifier.weight(0.5f)) {
                    item{
                        Text(
                            text = "Your playlists",
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 20.sp
                        )
                    }
                    // Get to SelectedPlaylistScreen with the corresponding playlistId when a playlist is clicked
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
                            // Display playlist image if available/exists, else display default image
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
                                    painter = painterResource(id = R.drawable.default_sound_pic),
                                    contentDescription = "Default playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            // Dispaly playlist title and creator
                            Text(
                                text = "${myPlaylists[index].title ?: "Unknown title"} by ${myPlaylists[index].creator ?: "Unknown creator"}",
                                modifier = Modifier.weight(1f)
                            )
                            // Delete playlist button (only for user's own playlists)
                            IconButton(
                                onClick = {
                                    deleteIndex = index // Update deleteIndex to delete corresponding playlist
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
                // Friends' playlists (Lower half of the screen)
                LazyColumn(modifier = Modifier.weight(0.5f)) {
                    item{
                        Text(
                            text = "Your friends' playlists",
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 20.sp
                        )
                    }
                    // Get to SelectedPlaylistScreen with the corresponding playlistId when a playlist is clicked
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
                            // Display playlist image if available/exists, else display default image
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
                                    painter = painterResource(id = R.drawable.default_sound_pic),
                                    contentDescription = "Default playlist picture",
                                    modifier = Modifier
                                        .width(40.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                            // Display playlist title and creator
                            Text(
                                text = "${friendsPlaylists[index].title ?: "Unknown title"} by ${friendsPlaylists[index].creator ?: "Unknown creator"}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        // Add playlist dialog
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
        // Delete playlist dialog
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

// Dialog Composable to add a new playlist
@Composable
fun AddPlaylistDialog(onAdd: (String)->Unit,         // Input: playlist name to be added
                    onCancel:()->Unit,
                    modifier: Modifier = Modifier
) {             
    var text by remember { mutableStateOf("") }     // To store playlist name input    

    AlertDialog(
        onDismissRequest = onCancel,                // If click outside dialog then close it
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

// Dialog Composable to delete a playlist
@Composable
fun DeletePlaylistDialog(index: Int,
                        onDelete:(Int)->Unit,            // Input: index of playlist to be deleted
                        onCancel:()->Unit,
                        modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onCancel,                    // If click outside dialog then close it
        title = { Text("") },
        text = {
            Text("Do you really want to delete this playlist?")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(index)                    // Delete playlist with given index
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

