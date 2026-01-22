package com.epfl.esl.musicplayer.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
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
import android.view.WindowManager
import androidx.compose.runtime.remember
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    // Data Variables
    private var bitmap by mutableStateOf<Bitmap?>(null)
    private var songTitle by mutableStateOf("No Music for now...")
    private var isPlaying by mutableStateOf(false)
    private var duration by mutableIntStateOf(0)
    private var countedPositionMs by mutableIntStateOf(0)
    private var currentPosition by mutableIntStateOf(0)


    // Screen  variables
    private val screenScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var screenJob: Job? = null


    // Sensor variables for wrist movement detection
    private lateinit var sensorManager: SensorManager // Variables to use Gyroscope for wrist shaking
    private var gyro: Sensor? = null
    private lateinit var wristFlickGyroDetector: WristFlickGyroDetector

    private lateinit var wearPlayViewModel : WearPlayViewModel


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        wristFlickGyroDetector = WristFlickGyroDetector(
            onFlick = { dir ->
                Log.d("Main Activity", "Movement Detected$dir")
                if ( dir == WristFlickGyroDetector.Direction.LEFT) {
                    wearPlayViewModel.onRightArrowClick() // Skip Song 
                }
                if (dir == WristFlickGyroDetector.Direction.RIGHT) {
                    wearPlayViewModel.onLeftArrowClick() // Rewind
                }
                if (dir==WristFlickGyroDetector.Direction.UP){
                    wearPlayViewModel.onPlayPauseClick() // Put Pause/Play

                }

            }
        )



        setContent {
           Project1amusicplayerTheme {
                wearPlayViewModel = viewModel()

               // Timer to update the progress bar

               LaunchedEffect(songTitle) {
                   // Reset when a new song is received (songTitle changes)
                   countedPositionMs = 0

               }

               LaunchedEffect(isPlaying,currentPosition) {
                   // Reset when a new song is received (songTitle changes)
                   countedPositionMs = currentPosition

                   while (true) {
                       if (isPlaying) {
                           countedPositionMs += 1000
                           if (countedPositionMs > duration) countedPositionMs = duration
                       }
                       delay(1000L)
                   }
               }

                HomeScreen(
                    songTitle,
                    bitmap,
                    isPlaying,
                    countedPositionMs,
                    duration,
                    flipButton = {isPlaying=!isPlaying},
                    wearPlayViewModel = wearPlayViewModel

                )
            }
        }

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


    }
    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
        sensorManager.unregisterListener(wristFlickGyroDetector)

    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/static_songInfo" }
            .forEach { event ->

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                currentPosition=dataMap.getInt("currentPosition",0)
                songTitle = dataMap.getString("songTitle", "Unknown Title") // Extract song title using the correct key
                isPlaying = dataMap.getBoolean("isPlaying", false) // Extract playback state
                duration = dataMap.getInt("duration", 0)      //Extract duration

                countedPositionMs = currentPosition



                // Extract album art image bytes 
                val receivedImageBytes: ByteArray? = dataMap.getByteArray("albumArt")

                // Convert the byte array to a bitmap and update the state
                bitmap = receivedImageBytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }

                applyScreenPolicy(isPlaying) // Keep the screen on when a music is playing
            }

        dataEvents.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/dynamic_songInfo" }
            .forEach { event ->
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                currentPosition=dataMap.getInt("currentPosition",0)
                countedPositionMs = currentPosition
            }
    }


    private fun applyScreenPolicy(isPlaying: Boolean) {
        // Cancel any pending transitions
        screenJob?.cancel()

        if (isPlaying) {
            // Keep interactive while playing
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Log.d("WearScreen", "KEEP_SCREEN_ON while playing")
        } else {
            // Pause: keep it on briefly, then allow ambient/sleep
            screenJob = screenScope.launch {
                delay(3_000L)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d("WearScreen", "KEEP_SCREEN_ON cleared after pause delay")
            }
        }
    }

}


@Composable
fun HomeScreen(
    songTitle: String,
    bitmap: Bitmap?,
    isPlaying: Boolean?,
    countedPositionMs:Int,
    duration:Int,
    modifier: Modifier = Modifier,
    wearPlayViewModel : WearPlayViewModel=viewModel(),
    flipButton:()-> Unit
    ) {

    var progress by remember { mutableStateOf(0f) }
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
                ?: ImageBitmap.imageResource(context.resources, R.drawable.imageplaylist)

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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    //.basicMarquee(
                        //iterations = Int.MAX_VALUE,
                        //animationMode = Immediately,
                        //repeatDelayMillis = 2,
                        //velocity = 10.dp
                    //)// Prevent text from touching screen edges
            )


            progress = if (duration > 0) {
                countedPositionMs.toFloat() / duration.toFloat()
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
                    text = formatTime2(countedPositionMs),
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
    return String.format(format = buildString {
        append("%02d:%02d")
    }, minutes, seconds)
}


@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)

@Composable
fun HomeScreenPreview() {
    // Pass correct parameters for preview
    HomeScreen(
        songTitle = "A Long Song Title",
        bitmap = null,
        isPlaying = false,  // 1 minute
        countedPositionMs = 60000,
        duration = 240000, // 4 minutes
        flipButton = {}


    )
}