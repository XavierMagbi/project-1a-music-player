package com.epfl.esl.musicplayer


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    onPlayerClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick=onPlayerClicked){
                Text("Play")
            }
            Button(onClick = onLogoutClicked) {
                Text(text = stringResource(id = R.string.log_out_button_text))
            }
        }
    }

}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen({},{})

}




