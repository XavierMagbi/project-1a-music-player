package com.epfl.esl.musicplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SongItem (
    val Artist: String? = "",
    val Name: String = "",
    val Path: String?="",
    val Id: String =""
)

@Composable
fun SongItemRow(
    songItem: SongItem,
    onAddClicked:()->Unit,
    onSongClicked:(List<SongItem>, Int) -> Unit,
    currentPlaylist:List<SongItem>,
    songIdx:Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClicked(currentPlaylist,songIdx)}
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

                Column(modifier.padding(8.dp)) {
                    Text(
                        text = songItem.Name ?: "",
                        style = TextStyle(fontSize = 20.sp, color = Color.Black)
                    )
                    Text(text = songItem.Artist ?: "")
                }
                IconButton(
                    onClick = onAddClicked
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.error
                    )
                }


        }
    }
}