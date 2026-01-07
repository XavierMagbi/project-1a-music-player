package com.epfl.esl.musicplayer


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.epfl.esl.musicplayer.ui.theme.MusicPlayerTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    equalizerViewModel: EqualizerViewModel = viewModel(),
    audioSessionId: Int
) {
    // Bands
    val subBassLevel by equalizerViewModel.subBassLevel.observeAsState(0f)
    val bassLevel by equalizerViewModel.bassLevel.observeAsState(0f)
    val midrangeLevel by equalizerViewModel.midrangeLevel.observeAsState(0f)
    val upperMidLevel by equalizerViewModel.upperMidLevel.observeAsState(0f)
    val trebleLevel by equalizerViewModel.trebleLevel.observeAsState(0f)

    // Set equalizer limits
    val minLevel = -2000f
    val maxLevel = 2000f

    // For equalizer settings to be remembered between songs
//    LaunchedEffect(audioSessionId) {
//        equalizerViewModel.setEqualizer(audioSessionId)
//    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (audioSessionId > 0) {
                // Sub-Bass
                Text(text = "Sub-Bass (60 Hz): ${(subBassLevel / 100).toInt()} dB")
                Slider(
                    value = subBassLevel,
                    onValueChange = { newValue ->
                        equalizerViewModel.updateSubBassLevel(newValue)
                    },
                    valueRange = minLevel..maxLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                // Bass
                Text(text = "Bass (230 Hz): ${(bassLevel / 100).toInt()} dB")
                Slider(
                    value = bassLevel,
                    onValueChange = { newValue ->
                        equalizerViewModel.updateBassLevel(newValue)
                    },
                    valueRange = minLevel..maxLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                // Midrange
                Text(text = "Midrange (910 Hz): ${(midrangeLevel / 100).toInt()} dB")
                Slider(
                    value = midrangeLevel,
                    onValueChange = { newValue ->
                        equalizerViewModel.updateMidLevel(newValue)
                    },
                    valueRange = minLevel..maxLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                // Upper-Midrange
                Text(text = "Upper-Midrange (3600 Hz): ${(upperMidLevel / 100).toInt()} dB")
                Slider(
                    value = upperMidLevel,
                    onValueChange = { newValue ->
                        equalizerViewModel.updateUpperMidLevel(newValue)
                    },
                    valueRange = minLevel..maxLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                // Treble
                Text(text = "Treble (14 kHz): ${(trebleLevel / 100).toInt()} dB")
                Slider(
                    value = trebleLevel,
                    onValueChange = { newValue ->
                        equalizerViewModel.updateTrebleLevel(newValue)
                    },
                    valueRange = minLevel..maxLevel,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    IconButton(
                        onClick = {
                            equalizerViewModel.resetAll()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                    // General gain like round volume button
                }
            } else {
                Text("Play a song first to use the equalizer")
            }
        }
    }
}


@Preview
@Composable
private fun EqualizerScreenPreview() {
    MusicPlayerTheme {
        EqualizerScreen(audioSessionId = 0)
    }
}