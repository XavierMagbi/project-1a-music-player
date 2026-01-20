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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val selectedPlaylistViewModel: SelectedPlaylistViewModel = viewModel(factory = SelectedPlaylistViewModelFactory(playlistId,application))
    val searchQuery by selectedPlaylistViewModel.searchQuery.observeAsState(initial = "")
    val songs by selectedPlaylistViewModel.song_id.observeAsState(initial = emptyList())
    val filteredSongs by selectedPlaylistViewModel.filteredSongs.observeAsState(emptyList())
    val playlistName by selectedPlaylistViewModel.playlistName.observeAsState(initial="")
    val newQueue by selectedPlaylistViewModel.newQueue.observeAsState(emptyList())

    // For playlist picture
    val playlistImageUri by selectedPlaylistViewModel.playlistImageUri.observeAsState(initial = null)

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
                                .width(150.dp)
                                .clickable(
                                    onClick = {
                                        // Open image picker
                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                        intent.type = "image/*"
                                        resultLauncher.launch(intent)
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.defaultplaylist),
                            contentDescription = "Default playlist picture",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .width(150.dp)
                                .clickable(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                        intent.type = "image/*"
                                        resultLauncher.launch(intent)
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                // Playlist name
                item {
                    Text(
                        text = playlistName,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
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
                            .clickable( onClick = {onSongClicked(index,newQueue)}),
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

                        IconButton(onClick = {
                            // STILL TO BE CORRECTED AFTER MERGE
                            Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
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
}

