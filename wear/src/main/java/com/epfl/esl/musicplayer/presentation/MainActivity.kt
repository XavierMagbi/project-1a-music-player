/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.epfl.esl.musicplayer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearDevices
import androidx.compose.material3.LinearProgressIndicator
import com.epfl.esl.musicplayer.R
import com.epfl.esl.musicplayer.presentation.theme.Project1amusicplayerTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit




class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var bitmap by mutableStateOf<Bitmap?>(null)
    private var songTitle by mutableStateOf("Hello World!")
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
           Project1amusicplayerTheme {
                HomeScreen(
                    songTitle,
                    bitmap,
                    isPlaying,
                    currentPosition,
                    duration,
                    // These click handlers are placeholders for now
                    onPreviousClick = { },
                    onPlayPauseClick = { },
                    onNextClick = { },
                    onSeek = { }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }
    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/songInfo" }
            .forEach { event ->
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                // Extract song title using the correct key
                songTitle = dataMap.getString("songTitle", "Unknown Title")

                // Extract playback state
                isPlaying = dataMap.getBoolean("isPlaying", false)


                //Extract currentPosition
                currentPosition = dataMap.getInt("currentPosition",0)

                //Extract duration
                 duration = dataMap.getInt("duration",0)

                // Extract album art image bytes using the correct key
                val receivedImageBytes: ByteArray? = dataMap.getByteArray("albumArt")

                // Convert the byte array to a bitmap and update the state
                bitmap = receivedImageBytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }
    }
}


@Composable
fun HomeScreen(
    songTitle: String,
    bitmap: Bitmap?,
    isPlaying: Boolean?,
    currentPosition :Int,
    duration:Int,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Use a Column for simple vertical arrangement
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val context = LocalContext.current
            val displayBitmap = bitmap?.asImageBitmap()
                ?: ImageBitmap.imageResource(context.resources, R.drawable.ic_logo)

            // 1. Album Art Image
            Image(
                bitmap = displayBitmap,
                contentDescription = "album cover",
                modifier = Modifier
                    .size(90.dp)
                    .padding(bottom = 8.dp) // Add space below the image
            )

            // 2. Song Title
            Text(
                text = songTitle,
                style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
                modifier = Modifier.padding(horizontal = 16.dp) // Prevent text from touching screen edges
            )



            val progress = if (duration > 0) {
                currentPosition.toFloat() / duration.toFloat()
            } else {
                0f
            }

            // 2. Use the correct non-interactive LinearProgressIndicator from Material 3
            LinearProgressIndicator(
                progress = { progress }, // Note: M3 uses a lambda for progress
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Time display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime2(currentPosition),
                    style = MaterialTheme.typography.caption3
                )
                Text(
                    text = formatTime2(duration),
                    style = MaterialTheme.typography.caption3
                )
            }


            // 3. Control Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp) // Add space above the icons
            ) {
                // Previous button
                Icon(
                    imageVector = Icons.Filled.FastRewind,
                    contentDescription = "Previous",
                    modifier = Modifier
                        .size(45.dp)
                        .clickable { onPreviousClick() }
                )
                // Play/pause button
                Icon(
                    imageVector = if (isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier
                        .size(60.dp) // Make the central button larger
                        .clickable { onPlayPauseClick() }
                )
                // Next button
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = "Next",
                    modifier = Modifier
                        .size(45.dp)
                        .clickable { onNextClick() }
                )
            }
        }
    }

}


// Function to format milliseconds into MM:SS
fun formatTime2(milliseconds: Int): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)

@Composable
fun HomeScreenPreview() {
    // Pass correct parameters for preview
    HomeScreen(
        songTitle = "A Long Song Title",
        bitmap = null,
        isPlaying = false,
        currentPosition = 60000, // 1 minute
        duration = 240000, // 4 minutes
        onPreviousClick = {},
        onPlayPauseClick = {},
        onNextClick = {},
        onSeek = {}
    )
}


