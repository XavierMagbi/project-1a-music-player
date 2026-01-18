package com.epfl.esl.musicplayer

import android.app.Application
import android.graphics.BitmapFactory
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SelectedPlaylistScreen(
    application: Application,
    playlistId: String,
    onSongClicked:(Int,List<String>)->Unit,
    modifier: Modifier = Modifier
) {
    val selectedPlaylistViewModel: SelectedPlaylistViewModel = viewModel(factory = SelectedPlaylistViewModelFactory(playlistId,application))
    var searchQuery by remember { mutableStateOf("") }
    val songs by selectedPlaylistViewModel.song_id.observeAsState(initial = emptyList())
    val filteredSongs by selectedPlaylistViewModel.filteredSongs.observeAsState(emptyList())
    val playlistName by selectedPlaylistViewModel.playlistName.observeAsState(initial="")
    val newQueue by selectedPlaylistViewModel.newQueue.observeAsState(emptyList())
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            /*
            Button(
                onClick = {
                    onSongClicked(1, listOf(R.raw.music0, R.raw.music1, R.raw.music2))
                }
            ) {}*/
            LazyColumn() {
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



                    }
                }

            }


        }
    }
    
}