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
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext

/*
    Play Screen Composable

    Functionality:
    Allows user view current song/associated metadata and next songs in queue
    User can interact with playback by pressing the playback control buttons
    User can go to a song in queue by pressing the display queue button and the title of his choice

    Navigation:
    Can navigate to by pressing a song on the Selected Playlist screen or in the Discover screen
    When a song has already been played user can navigate by pressing the minimized playback info
        on the bottom bar
    Navigating out of this screen will display the minimized playback info
 */

// Play screen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onArrowClicked:()->Unit,                //Function delegation to navigate out of screen
    modifier: Modifier = Modifier,
    playScreenViewModel: PlayScreenViewModel = viewModel() //viewModel to manage State
) {

    //get variables from viewModel
    val isPlaying by playScreenViewModel.isPlaying.observeAsState(initial = false) //if a song is playing
    val currentPosition by playScreenViewModel.currentPosition.observeAsState(0) //current playback time
    val duration by playScreenViewModel.duration.observeAsState(0) // duration of the current track
    val title by playScreenViewModel.title.observeAsState(initial = "") //title of the track
    val coverImage by playScreenViewModel.coverImage.observeAsState() //current track cover art
    val shuffleOn by playScreenViewModel.shuffleOn.observeAsState(initial = false) //to know if queue shuffling is on
    val repeatMode by playScreenViewModel.repeatMode.observeAsState(0) // to which playback mode is activated (no repeat, repeat queue,repeat track)
    val currentTrackIndex = playScreenViewModel.currentTrackIndex // current index in queue of track
    val playlist = playScreenViewModel.currentPlaylist //list of paths to tracks to display queue


    // Queue slider state
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // convert cover image (byte array) to painter or set default image if no image exists
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
        painterResource(id = R.drawable.default_sound_pic)
    }

    // function to convert ms to readable time
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
        //Lazy column to put UI elements and avoid resizing
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                // Arrow to navigate out of playScreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick =  onArrowClicked) {
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
                    modifier = Modifier
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = Immediately,
                            repeatDelayMillis = 200,
                            velocity =  20.dp
                        ),// Make the text auto-scroll when too long
                    minLines = 2,


                )
            }

            // Time slider - show how much time is elapsed and seek to specific time in song
            item {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { playScreenViewModel.onSeek(it) }, //handle when clicking on a timestamp on slider
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Time display - display current time and remaining time in correct format
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

            // Arrows + Play/Pause button - clickable icons
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // rewind arrow
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
                            Icons.Filled.Pause else Icons.Filled.PlayArrow, //change icon if play or pause
                        contentDescription = "",
                        modifier = modifier
                            .size(200.dp)
                            .weight(1f)
                            .clickable {
                                playScreenViewModel.onPlayPauseClick()
                            }
                    )
                    // fast forward arrow
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
                    // repeat button
                    Icon(
                        imageVector = if (repeatMode == 0) Icons.Filled.Repeat
                                        else if (repeatMode == 1) Icons.Filled.RepeatOn
                                        else Icons.Filled.RepeatOneOn,
                        // change icon depending on repeat mode
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


    }


    // Queue sheet - display next songs to come and play specific song if clicked
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // text to explain it is the music queue
                Text(
                    text = "Music Queue",
                    style = MaterialTheme.typography.headlineSmall
                )
                // Lazy column to display a scrollable list of songs in queue
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(playlist.size - currentTrackIndex - 1) { index ->
                        // extract next songs index
                        val queueIndex = index + currentTrackIndex + 1
                        // retrieve .mp3 location to extract metadata
                        val resId = playlist[queueIndex]
                        // Extract metadata
                        val (trackName, trackImage) = playScreenViewModel.getTrackMetadata(resId)
                        // Convert form ByteArray metadata to Bitmap
                        val trackPainter = if (trackImage != null) {
                            BitmapPainter(
                                BitmapFactory.decodeByteArray(
                                    trackImage,
                                    0,
                                    trackImage.size
                                ).asImageBitmap()
                            )
                        } else {
                            painterResource(id = R.drawable.default_sound_pic)
                        } // show default image if no album art

                        // row composable to show image and title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playScreenViewModel.playCurrentTrack(queueIndex)
                                    showQueue = false
                                }
                        ) {
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

