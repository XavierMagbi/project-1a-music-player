package com.epfl.esl.musicplayer

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue




@Composable
fun PlayScreen(
    modifier: Modifier = Modifier,
    playScreenViewModel: PlayScreenViewModel = viewModel()
) {

    val isPlaying by playScreenViewModel.isPlaying.observeAsState(initial = false)
    val currentPosition by playScreenViewModel.currentPosition.observeAsState(0)
    val duration by playScreenViewModel.duration.observeAsState(0)
    val title by playScreenViewModel.title.observeAsState(initial = "")


    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
        ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "",
                modifier = modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Text(
                text = title
            )
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = {playScreenViewModel.onSeek(it)},
                valueRange = 0f..duration.toFloat(), // From zero to duration
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
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
                Icon(
                    imageVector = if (isPlaying == true) Icons.Filled.Pause else
                        Icons.Filled.PlayArrow,
                    contentDescription = "",
                    modifier = modifier
                        .size(200.dp)
                        .weight(1f)
                        .clickable {
                            playScreenViewModel.onPlayPauseClick()
                        }
                )
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = "",
                    modifier = modifier
                        .size(100.dp)
                        .weight(1f)
                )
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