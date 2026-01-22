package com.epfl.esl.musicplayer

import android.graphics.BitmapFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/*
    Discover Screen Composable

    Functionality:
    Allows user to search for songs and add them to any of their playlists: 
        -All musics stored in database are initially shown for the user to discover
    Songs appear with their title and cover image

    Navigation:
    Can navigate to it via bottom bar
 */

// Discover Screen Composable
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    currentUsername: String = "",                       // To load user playlists
    onSongClicked:(List<String>,Int)->Unit,             // To play song when clicked
    onAddQueue:(String)->Unit,                          // To add song to queue
    discoverViewModel: DiscoverViewModel = viewModel (  
        factory = DiscoverViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            currentUsername)
    )

){
    // Get variables from ViewModel
    val filteredSongs by discoverViewModel.filteredSongs.observeAsState(emptyList()) // List of songs found from search
    val playlists by discoverViewModel.playlists.observeAsState(emptyList())         // User playlists
    val searchQuery by discoverViewModel.searchQuery.observeAsState("")              // Search query

    // Composable variables
    var showDialog by remember { mutableStateOf(false) } // For add to playlist dialog
    var selectedSongLink by remember { mutableStateOf("") } // To keep track of the selected song to add to playlist

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn() {
            // Discover text
            item {
                Text(
                    text = stringResource(id = R.string.discover_text),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { discoverViewModel.updateSearchQuery(it) },
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
            // List of songs
            items(filteredSongs.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        // Navigate to PlayScreen, plays the selected song and updates queue
                        .clickable(onClick = {
                            onSongClicked(discoverViewModel.getSongIdList(),index)
                        })
                    ,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display cover image if available, else display default music/playlist image
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
                    // Song title
                    Text(
                        text = filteredSongs[index].title ?: "Unknown title",
                        modifier = Modifier.weight(1f)
                    )
                    // Add to playlist button
                    IconButton(
                        onClick = {
                            // Opens dialog to add song to selected playlist
                            showDialog = true
                            selectedSongLink = filteredSongs[index].link ?: "Unknown link"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Add to playlist"
                        )
                    }
                    // Add to queue button
                    IconButton(
                        onClick = {
                        onAddQueue(filteredSongs[index].datapath)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowCircleRight,
                            contentDescription = "Add to queue"
                        )
                    }
                }
            }
        }
        // Add to playlist dialog
        if(showDialog){
            AddSongToPlaylistDialog(
                playlists = playlists,
                onAdd = {playlistId->
                    discoverViewModel.addSongToPlaylist(
                        playlistId = playlistId,
                        songId = selectedSongLink
                    )
                    showDialog=false
                },
                onCancel = {showDialog=false})
        }
    }
}

// Dialog Composable to add a song to a selected playlist
@Composable
fun AddSongToPlaylistDialog(
    playlists: List<playlistMetadata>,
    onAdd: (String) -> Unit,            // Input: playlistId
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel, // If click outside dialog then close it
        title = { Text("Add to playlist") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp) // max height for scrolling
            ) {
                // List of playlists to select from
                LazyColumn {
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlaylistId = playlist.id } // Update selectedPlaylistId on row click
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Does the same as clicking on the row. Showcased that the playlist has been selected
                            RadioButton(
                                selected = selectedPlaylistId == playlist.id,
                                onClick = { selectedPlaylistId = playlist.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = playlist.title?:"")
                        }
                    }
                }
            }
        },
        // Add to playlist button
        confirmButton = {
            TextButton(
                enabled = selectedPlaylistId != null, // Enable button only if a playlist is selected
                onClick = { selectedPlaylistId?.let { onAdd(it) } } // Call onAdd with selected playlistId
            ) {
                Text("Add")
            }
        },
        // Cancel operation button
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

// Preview will not render with such dependencies

