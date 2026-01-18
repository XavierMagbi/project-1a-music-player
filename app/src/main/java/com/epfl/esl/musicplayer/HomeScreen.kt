package com.epfl.esl.musicplayer


import android.net.Uri
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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage

@Composable
fun HomeScreen(
    username: String,
    imageUri: Uri?,
    userKey: String,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Welcome $username",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp, end = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)), // circle background
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = stringResource(
                            R.string.user_image_content_description
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
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
    HomeScreen("", null,"",{},{})

}




