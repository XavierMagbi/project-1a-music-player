package com.epfl.esl.musicplayer

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.epfl.esl.musicplayer.ui.theme.MusicPlayerTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material3.LinearProgressIndicator


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    modifier: Modifier = Modifier,
    playScreenViewModel: PlayScreenViewModel = viewModel()
) {

    val isPlaying by playScreenViewModel.isPlaying.observeAsState(initial = false)
    val currentPosition by playScreenViewModel.currentPosition.observeAsState(0)
    val duration by playScreenViewModel.duration.observeAsState(0)
    val title by playScreenViewModel.title.observeAsState(initial = "")
    val coverImage by playScreenViewModel.coverImage.observeAsState()
    val shuffleOn by playScreenViewModel.shuffleOn.observeAsState(initial = false)
    val repeatMode by playScreenViewModel.repeatMode.observeAsState(0)
    val currentTrackIndex = playScreenViewModel.currentTrackIndex
    val playlist = playScreenViewModel.currentPlaylist
    val originalPlaylist = playScreenViewModel.originalPlaylist

    // Queue slider state
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // For research query
    var searchQuery by remember { mutableStateOf("") }

    // Indicate list or player mode
    var isPlayerActive by remember { mutableStateOf(false) }

    // To convert ByteArray to Image
    val painter = if (coverImage != null) {
        val bitmap = remember(coverImage) {
            BitmapFactory.decodeByteArray(
                coverImage,
                0,
                coverImage!!.size
            ).asImageBitmap()
        }
        BitmapPainter(bitmap)
    } else {
        painterResource(id = R.drawable.ic_launcher_foreground)
    }

    // To convert ms to readable time
    fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isPlayerActive){ // Player mode
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Return to list mode
                item{
                    // Arrow back
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isPlayerActive = false }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                }
                // Music image
                item {
                    Image(
                        painter = painter,
                        contentDescription = "",
                        modifier = modifier
                            .size(275.dp)
                    )
                }

                // Music title
                item {
                    Text(
                        text = title,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Time slider
                item {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { playScreenViewModel.onSeek(it) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Time display
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall
                        )

                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "-${formatTime(duration - currentPosition)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Arrow + Play/Pause buttons
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left arrow
                        Icon(
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = "",
                            modifier = modifier
                                .size(100.dp)
                                .weight(1f)
                                .clickable {
                                    playScreenViewModel.onLeftArrowClick()
                                }
                        )
                        // Play/pause
                        Icon(
                            imageVector = if (isPlaying == true)
                                Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "",
                            modifier = modifier
                                .size(200.dp)
                                .weight(1f)
                                .clickable {
                                    playScreenViewModel.onPlayPauseClick()
                                }
                        )
                        // Right arrow
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = "",
                            modifier = modifier
                                .size(100.dp)
                                .weight(1f)
                                .clickable {
                                    playScreenViewModel.onRightArrowClick()
                                }
                        )
                    }
                }

                // Shuffle + Queue + Loop buttons
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle button
                        Icon(
                            imageVector = if (shuffleOn) Icons.Filled.ShuffleOn else Icons.Filled.Shuffle,
                            contentDescription = "",
                            modifier = modifier
                                .size(50.dp)
                                .weight(1f)
                                .clickable {
                                    playScreenViewModel.onShuffleClick()
                                }
                        )
                        // Music queue button
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = "",
                            modifier = modifier
                                .size(50.dp)
                                .weight(1f)
                                .clickable {
                                    showQueue = true
                                }
                        )
                        // Loop button
                        Icon(
                            imageVector = if (repeatMode == 0) Icons.Filled.Repeat else if (repeatMode == 1) Icons.Filled.RepeatOn else Icons.Filled.RepeatOneOn,
                            contentDescription = "",
                            modifier = modifier
                                .size(50.dp)
                                .weight(1f)
                                .clickable {
                                    playScreenViewModel.onRepeatClick()
                                }
                        )
                    }
                }
            }
        } else { // List mode
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
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

                    // Music list from current playlist
                    items(originalPlaylist.size) { index ->
                        // Extract id
                        val resId = originalPlaylist[index]
                        // Extract metadata
                        val (trackName, trackImage) = playScreenViewModel.getTrackMetadata(resId)
                        // Convert form ByteArray to Bitmap
                        val trackPainter = if (trackImage != null) {
                            BitmapPainter(
                                BitmapFactory.decodeByteArray(
                                    trackImage,
                                    0,
                                    trackImage.size
                                ).asImageBitmap()
                            )
                        } else {
                            painterResource(id = R.drawable.ic_launcher_foreground)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playScreenViewModel.playCurrentTrack(index)
                                    isPlayerActive = true
                                }
                                .background(
                                    if (currentTrackIndex == index)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.background
                                )
                        ) {
                            Image(
                                painter = trackPainter,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(4.dp)
                            )
                            Text(
                                text = trackName,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { playScreenViewModel.addToQueue(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowCircleRight,
                                    contentDescription = "Add to queue"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (currentTrackIndex != -1) { // Card to be visible only once a music has been picked
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPlayerActive = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(8.dp)
                            )
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            IconButton(onClick = { playScreenViewModel.onPlayPauseClick() }) {
                                Icon(
                                    imageVector = if (isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause on card"
                                )
                            }
                        }

                        // Time indicator
                        LinearProgressIndicator(
                            progress = {
                                if (duration > 0) currentPosition.toFloat() / duration.toFloat()
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }


    // Queue sheet
    if (showQueue){
            ModalBottomSheet(
                onDismissRequest = { showQueue = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Sheet title
                    Text(
                        text = "Music Queue",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    // Musics in queue
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlist.size - currentTrackIndex - 1) { index ->
                            // Queue index
                            val queueIndex = index + currentTrackIndex + 1
                            // Extract id
                            val resId = playlist[queueIndex]
                            // Extract metadata
                            val (trackName, trackImage) = playScreenViewModel.getTrackMetadata(resId)
                            // Convert form ByteArray to Bitmap
                            val trackPainter = if (trackImage != null) {
                                BitmapPainter(BitmapFactory.decodeByteArray(trackImage, 0, trackImage.size).asImageBitmap())
                            } else {
                                painterResource(id = R.drawable.ic_launcher_foreground)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable{
                                        playScreenViewModel.playCurrentTrack(index)
                                        showQueue = false
                                    }
                            ){
                                Image(
                                    painter = trackPainter,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(4.dp)
                                )
                                Text(
                                    text = trackName
                                )
                            }
                        }
                    }
                }
            }
        }
}


@Preview
@Composable
private fun PlayScreenPreview() {
    MusicPlayerTheme {
        PlayScreen()
    }
}