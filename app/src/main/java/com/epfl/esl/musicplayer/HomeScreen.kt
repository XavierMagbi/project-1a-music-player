package com.epfl.esl.musicplayer


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.IconButton
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
    currentUsername: String = ""
) {
    val context = LocalContext.current
    val foundUsers by homeScreenViewModel.foundUsers.observeAsState(emptyList())

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // For research query
        var searchQuery by remember { mutableStateOf("") }

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item{
                Text("Welcome to WristWave!")
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        homeScreenViewModel.searchUsers(query)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search friends...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search friends"
                        )
                    },
                    singleLine = true
                )
            }

            items(foundUsers.size){ index ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (foundUsers[index].image != null) { // Once image has been fetched and converted to bitmap
                        Image(
                            bitmap = foundUsers[index].image!!.asImageBitmap(),
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(50.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(foundUsers[index].username,
                        modifier = Modifier.weight(1f)) // To push add button to right of screen

                    IconButton(onClick = {
                        homeScreenViewModel.addFriend(
                            pressedUsername = foundUsers[index].username,
                            currentUsername = currentUsername)
                        Toast.makeText(context, "Added friend", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add friend"
                        )
                    }
                }
            }
        }
    }



}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen()

}




