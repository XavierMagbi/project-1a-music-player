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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    modifier: Modifier = Modifier,
    playScreenViewModel: PlayScreenViewModel
) {

    val isPlaying by playScreenViewModel.isPlaying.observeAsState(initial = false)
    val currentPosition by playScreenViewModel.currentPosition.observeAsState(0)
    val duration by playScreenViewModel.duration.observeAsState(0)
    val title by playScreenViewModel.title.observeAsState(initial = "")
    val coverImage by playScreenViewModel.coverImage.observeAsState()
    val shuffleOn by playScreenViewModel.shuffleOn.observeAsState(initial = false)
    val repeatMode by playScreenViewModel.repeatMode.observeAsState(0)
    val playlist = playScreenViewModel.currentPlaylist

    // Queue slider state
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Music image
            Image(
                painter = painter,
                contentDescription = "",
                modifier = modifier
                    .size(400.dp)
            )
            // Music title
            Text(
                text = title,
                textAlign = TextAlign.Center
            )
            // Time slider
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { playScreenViewModel.onSeek(it) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            // Time display
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
            // Arrow + Play/Pause buttons
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
            // Shuffle + Queue + Loop buttons
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
                        items(playlist.size) { index ->
                            // Extract id
                            val resId = playlist[index]
                            // Extract metadata
                            //val (trackName, trackImage) = playScreenViewModel.getTrackMetadata(resId)
                            val trackName=""
                            val trackImage=null
                            // Convert form ByteArray to Bitmap
                            val trackPainter = if (trackImage != null) {
                                //BitmapPainter(BitmapFactory.decodeByteArray(trackImage, 0, trackImage.size).asImageBitmap())
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
                                    //painter = trackPainter,
                                    painter=painterResource(id = R.drawable.ic_launcher_foreground),
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
}


