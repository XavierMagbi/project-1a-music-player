package com.epfl.esl.musicplayer

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.LaunchedEffect
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    currentUsername: String = "",
    onSongClicked:(List<String>,Int)->Unit,
    onAddQueue:(String)->Unit,
    discoverViewModel: DiscoverViewModel = viewModel (
        factory = DiscoverViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application,
            currentUsername)
    )

){
    // For research query
    val searchQuery by discoverViewModel.searchQuery.observeAsState("")
    val filteredSongs by discoverViewModel.filteredSongs.observeAsState(emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var selectedSongLink by remember { mutableStateOf("") }

    val playlists by discoverViewModel.playlists.observeAsState(emptyList())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn() {
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

            items(filteredSongs.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable(onClick = {
                            onSongClicked(discoverViewModel.getSongIdList(),index)
                        })
                    ,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Music file",
                            modifier = Modifier
                                .width(40.dp)
                                .padding(end = 8.dp)
                        )
                    }

                    Text(
                        text = filteredSongs[index].title ?: "Unknown title",
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            showDialog = true
                            selectedSongLink = filteredSongs[index].link ?: "Unknown link"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add to playlist",
                            tint = MaterialTheme.colorScheme.error
                        )

                    }

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

//composable to add song to a playlist
@Composable
fun AddSongToPlaylistDialog(
    playlists: List<playlistMetadata>,
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
                            Text(text = playlist.title?:"")

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
