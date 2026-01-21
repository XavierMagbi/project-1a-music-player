package com.epfl.esl.musicplayer

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun SelectedPlaylistScreen(
    application: Application,
    playlistId: String,
    onSongClicked:(Int,List<String>)->Unit,
    modifier: Modifier = Modifier,
    currentUsername: String,
    onAddQueue:(String)->Unit
) {
    val context = LocalContext.current
    
    val selectedPlaylistViewModel: SelectedPlaylistViewModel = viewModel(factory = SelectedPlaylistViewModelFactory(playlistId,application,currentUsername))
    val searchQuery by selectedPlaylistViewModel.searchQuery.observeAsState(initial = "")
    val songs by selectedPlaylistViewModel.song_id.observeAsState(initial = emptyList())
    val filteredSongs by selectedPlaylistViewModel.filteredSongs.observeAsState(emptyList())
    val playlistName by selectedPlaylistViewModel.playlistName.observeAsState(initial="")
    //val newQueue by selectedPlaylistViewModel.newQueue.observeAsState(emptyList())

    // For playlist picture
    val playlistImageUri by selectedPlaylistViewModel.playlistImageUri.observeAsState(initial = null)
    // For access to change playlist picture (cannot change Friends' playlists)
    val isMyPlaylist by selectedPlaylistViewModel.isMyPlaylist.observeAsState(initial = false)

    // From EE-490(g) labs to get result from intent
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                selectedPlaylistViewModel.updatePlaylistImage(uri!!)
            }
        }
    )
    
    // For rename dialog
    var showRenameDialog by remember { mutableStateOf(false) }

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
                                            // Open image picker
                                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                                            intent.type = "image/*"
                                            resultLauncher.launch(intent)
                                        }
                                    }
                                ),
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.defaultplaylist),
                            contentDescription = "Default playlist picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .size(150.dp)
                                .clickable(
                                    onClick = {
                                        if (isMyPlaylist){
                                            // Open image picker
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
                        horizontalArrangement = Arrangement.Center  //
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

                // Search bar
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

                items(filteredSongs.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable( onClick = {
                                if (selectedPlaylistViewModel.isPlaylistLoaded()) {
                                    onSongClicked(index, selectedPlaylistViewModel.getSongIdList())
                                }
                                else{
                                    Toast.makeText(context,"Wait for playlist to load",Toast.LENGTH_SHORT).show()
                                }
                            }),
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
                            text = filteredSongs[index].title ?: "...",
                            modifier = Modifier.weight(1f)
                        )

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

    if (showRenameDialog) {
        EditPlaylistDialog(
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName -> selectedPlaylistViewModel.updatePlaylistName(newName) }
        )
    }
}


@Composable
fun EditPlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
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