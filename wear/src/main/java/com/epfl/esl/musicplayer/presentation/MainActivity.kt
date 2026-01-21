/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.epfl.esl.musicplayer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode.Companion.Immediately
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.material3.LinearProgressIndicator
import com.epfl.esl.musicplayer.R
import com.epfl.esl.musicplayer.presentation.theme.Project1amusicplayerTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.cancel


class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var bitmap by mutableStateOf<Bitmap?>(null)
    private var songTitle by mutableStateOf("Hello World!")
    private var currentSong by mutableStateOf(songTitle)
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)
    private lateinit var sensorManager: SensorManager // Variables to use Gyroscope for wrist shaking
    private var gyro: Sensor? = null
    private lateinit var wristFlickGyroDetector: WristFlickGyroDetector
    // A scope tied to the Activity
    private val screenScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var turnOffJob: Job? = null

    private lateinit var wearPlayViewModel : WearPlayViewModel


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        wristFlickGyroDetector = WristFlickGyroDetector(
            onFlick = { dir ->
                Log.d("Main Activity","Movement Detected" + dir.toString())
                if (dir == WristFlickGyroDetector.Direction.LEFT || dir == WristFlickGyroDetector.Direction.RIGHT) {
                    wearPlayViewModel.onRightArrowClick()
                }

                // simplest: always "next"
                // or: LEFT=prev, RIGHT=next
            }
        )

        setContent {
           Project1amusicplayerTheme {
                wearPlayViewModel = viewModel()
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
                    onSeek = { },
                    flipButton = {isPlaying=!isPlaying},
                    wearPlayViewModel = wearPlayViewModel

                )
            }
        }

        //if (currentSong == songTitle){ pulseScreenFor(4_000L) }
        // Turn on the screen for 4 seconds if the screen is awake


    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)

        gyro?.let {
            sensorManager.registerListener(
                wristFlickGyroDetector,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        Log.d("Main Activity","addListener attached")
    }
    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        sensorManager.unregisterListener(wristFlickGyroDetector)
        Log.d("Main Activity","removedListener attached")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        //Log.d("Main Activity","rx")
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/static_songInfo" }
            .forEach { event ->

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                // Extract song title using the correct key
                songTitle = dataMap.getString("songTitle", "Unknown Title")

                // Extract playback state
                isPlaying = dataMap.getBoolean("isPlaying", false)

                //Extract duration
                 duration = dataMap.getInt("duration",0)

                // Extract album art image bytes using the correct key
                val receivedImageBytes: ByteArray? = dataMap.getByteArray("albumArt")

                // Convert the byte array to a bitmap and update the state
                bitmap = receivedImageBytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/dynamic_songInfo" }
            .forEach { event ->
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                currentPosition = dataMap.getInt("currentPosition", 0)
            }
    }

    // UNUSED FOR NOW CHECK WHEN IMPLEMENTED
    private fun pulseScreenFor(delayMs: Long = 4_000L) {
        // Keep screen on now
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d("WearScreen", "KEEP_SCREEN_ON enabled")

        // Cancel any previously scheduled "turn off"
        turnOffJob?.cancel()

        // Schedule clearing the flag after delay
        turnOffJob = screenScope.launch {
            delay(delayMs)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d("WearScreen", "KEEP_SCREEN_ON cleared (system may turn screen off)")
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
    wearPlayViewModel : WearPlayViewModel=viewModel(),
    flipButton:()-> Unit,
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
                    .size(80.dp)
                    .padding(bottom = 8.dp) // Add space below the image
            )

            // 2. Song Title
            Text(
                text = songTitle,
                style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
                maxLines = 1, // Number of Line
                softWrap = false, // Prevent Wrapping of text
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = Immediately,
                        repeatDelayMillis = 2,
                        velocity = 10.dp
                    )// Prevent text from touching screen edges
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
                    .padding(horizontal = 10.dp)
                    .padding(top = 10.dp)

            )

            // Time display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime2(currentPosition),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime2(duration),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // 3. Control Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth() // Add space above the icons
            ) {
                // Previous button
                Icon(
                    imageVector = Icons.Filled.FastRewind,
                    contentDescription = "Previous",
                    modifier = Modifier
                        .weight(2f)
                        .size(40.dp)
                        .clickable { wearPlayViewModel.onLeftArrowClick() }
                )
                // Play/pause button
                Icon(
                    imageVector = if (isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier
                        .weight(1f)
                        .size(50.dp) // Make the central button larger
                        .clickable {
                            wearPlayViewModel.onPlayPauseClick()
                            flipButton()
                        }
                )
                // Next button
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = "Next",
                    modifier = Modifier
                        .weight(2f)
                        .size(40.dp)
                        .clickable { wearPlayViewModel.onRightArrowClick() }
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
        onSeek = {},
        flipButton = {}


    )
}


