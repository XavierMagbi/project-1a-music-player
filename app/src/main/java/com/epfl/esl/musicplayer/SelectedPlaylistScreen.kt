package com.epfl.esl.musicplayer

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/*
    Selected Playlist Screen Composable

    Functionality:
    Displays playlist picture, name and songs in said playlist
    Allows searching songs within the playlist
    Allows navigating to PlayScreen when a song is clicked (with corresponding playlistId)
    Allows deleting songs from playlist (if user's playlist)
    Allows renaming playlist (if user's playlist)
    Allows changing playlist picture (if user's playlist)
    Allows adding songs to queue

    Navigation:
    Can navigate to it PlaylistScreen when a playlist is selected 
    or when pressing back arrow from PlayScreen
 */

 // Selected Playlist Screen Composable
@Composable
fun SelectedPlaylistScreen(
    application: Application,
    playlistId: String,                     // Selected playlist ID
    onSongClicked:(Int,List<String>)->Unit, // To navigate to PlayScreen with selected song to play and its queue
    modifier: Modifier = Modifier,
    currentUsername: String,
    onAddQueue:(String)->Unit               // To add song to current queue
) {
    // Instantiate ViewModel
    val selectedPlaylistViewModel: SelectedPlaylistViewModel = viewModel(factory = SelectedPlaylistViewModelFactory(playlistId,application,currentUsername))
    // Get variables from ViewModel
    val searchQuery by selectedPlaylistViewModel.searchQuery.observeAsState(initial = "")               // For search bar
    val filteredSongs by selectedPlaylistViewModel.filteredSongs.observeAsState(emptyList())            // Filtered songs based on search query   
    val playlistName by selectedPlaylistViewModel.playlistName.observeAsState(initial="")               // Playlist name
    val playlistImageUri by selectedPlaylistViewModel.playlistImageUri.observeAsState(initial = null)   // Playlist picture
    val isMyPlaylist by selectedPlaylistViewModel.isMyPlaylist.observeAsState(initial = false)          // Whether the playlist belongs to the current user

    // Composable variables
    val context = LocalContext.current // For Toast messages
    var showRenameDialog by remember { mutableStateOf(false) } // For playlist rename dialog

    // To collect image from image picker
    // From EE-490(g) labs to get result from intent
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data // Get image URI
                selectedPlaylistViewModel.updatePlaylistImage(uri!!) // Send picked image URI to update playlist picture
            }
        }
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn() {
                // Playlist picture
                item{
                    // If playlist has custom picture and was able to fetch it from Firebase Storage, display it
                    if (playlistImageUri != null){
                        AsyncImage(
                            model = playlistImageUri,
                            contentDescription = "Playlist picutre",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .size(150.dp)
                                .clickable(
                                    onClick = {
                                        if (isMyPlaylist){
                                            // Open image picker to update playlist picture
                                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                                            intent.type = "image/*"
                                            resultLauncher.launch(intent)
                                        }
                                    }
                                ),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.default_sound_pic),
                            contentDescription = "Default playlist picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .size(150.dp)
                                .clickable(
                                    onClick = {
                                        if (isMyPlaylist){
                                            // Open image picker to add a playlist picture
                                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                                            intent.type = "image/*"
                                            resultLauncher.launch(intent)
                                        }
                                    }
                                ),
                        )
                    }
                }
                // Playlist name
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = playlistName,
                            textAlign = TextAlign.Center
                        )
                        if (isMyPlaylist) {
                            // To rename playlist (only if my playlist)
                            IconButton(
                                onClick = {
                                    showRenameDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Rename playlist"
                                )
                            }
                        }
                    }
                }
                // Search bar to filter songs within playlist
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { selectedPlaylistViewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search songs...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search music"
                            )
                        },
                        singleLine = true
                    )
                }
                // Songs in playlist
                items(filteredSongs.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable( onClick = {
                                // Allow navigating to PlayScreen with selected song only when all playlist songs are loaded
                                if (selectedPlaylistViewModel.isPlaylistLoaded()) {
                                    onSongClicked(index, selectedPlaylistViewModel.getSongIdList())
                                }
                                //Else show toast
                                else{
                                    Toast.makeText(context,"Wait for playlist to load",Toast.LENGTH_SHORT).show()
                                }
                            }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Song cover image if available, else default music cover image
                        if (filteredSongs[index].image != null) {
                            val bitmap = BitmapFactory.decodeByteArray(filteredSongs[index].image!!, 0, filteredSongs[index].image!!.size)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Cover",
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
                        // Song title "..." when title is null/loading
                        Text(
                            text = filteredSongs[index].title ?: "...",
                            modifier = Modifier.weight(1f)
                        )
                        // Delete song from playlist button (only if my playlist)
                        if (isMyPlaylist) {
                            IconButton(onClick = {
                                selectedPlaylistViewModel.deleteSong(
                                    filteredSongs[index].linkGS ?: ""
                                )
                                Toast.makeText(
                                    context,
                                    "Deleted song from playlist",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.RemoveCircle,
                                    contentDescription = "Delete from playlist button"
                                )
                            }
                        }
                        // Add song to queue button
                        IconButton(onClick = {
                            if (selectedPlaylistViewModel.isPlaylistLoaded()) {
                                onAddQueue(filteredSongs[index].datapath)
                            }
                            else{
                                Toast.makeText(context,"Wait for song to load",Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowCircleRight,
                                contentDescription = "Add to queue"
                            )
                        }
                    }
                }
            }
        }
    }
    // Playlist rename dialog
    if (showRenameDialog) {
        EditPlaylistDialog(
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName -> selectedPlaylistViewModel.updatePlaylistName(newName) }
        )
    }
}

// Dialog Composable to rename playlist
@Composable
fun EditPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit                             // Input: New playlist name
) {
    var newPlaylistName by remember { mutableStateOf("") }  // To store playlist updated name input    
    
    AlertDialog(
        onDismissRequest = onDismiss,                       // If click outside dialog then close it
        title = { Text("Rename Playlist") },
        text = {
            TextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                label = { Text("New playlist name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(newPlaylistName)
                    onDismiss()
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}