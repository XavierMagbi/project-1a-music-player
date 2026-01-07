package com.epfl.esl.musicplayer


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    equalizerViewModel: EqualizerViewModel = viewModel(),
    audioSessionId: Int
) {

    // Equalizer for music. Remember to avoid equalizer reinitialization every screen rebuild
    val equalizer = remember{
        try {
            android.media.audiofx.Equalizer(0, audioSessionId).apply{ enabled = true } // 0: Standard priority, Enabled required as Equalizer object is off by default
        } catch (e: Exception) {
            null
        }
    }

    // Slider position
    val smallFreqLevel by equalizerViewModel.smallFreqLevel.observeAsState(0f)

    // Set equalizer limits
    val minLevel = equalizer?.bandLevelRange?.get(0)?.toFloat() ?: -1500f
    val maxLevel = equalizer?.bandLevelRange?.get(1)?.toFloat() ?: 1500f


    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Small frequencies"
            )

            Text(
                text = "${(smallFreqLevel / 100).toInt()} dB"
            )

            Slider(
                value = smallFreqLevel,
                onValueChange = { newValue ->
                    // Update slider position
                    equalizerViewModel.updateSmallFreqLevel(newValue)
                    // Update gain for small frequencies here
                    equalizer?.setBandLevel(0, newValue.toInt().toShort())
                },
                valueRange = minLevel..maxLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

