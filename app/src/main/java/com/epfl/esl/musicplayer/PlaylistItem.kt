package com.epfl.esl.musicplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PlaylistItem (
    val name: String? = "",
    val author: String? = "",
    val id: String?=""
)

//playlistitem row composable

@Composable
fun PlaylistItemRow(playlistItem: PlaylistItem, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier.padding(8.dp)) {
            Text(
                text = playlistItem.name ?: "",
                style = TextStyle(fontSize = 20.sp, color = Color.Black)
            )
            Text(text = playlistItem.author ?: "")
        }
    }
}