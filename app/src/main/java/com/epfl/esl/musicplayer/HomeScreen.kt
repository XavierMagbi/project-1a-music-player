package com.epfl.esl.musicplayer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.widget.Toast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext

/*
    Home Screen Composable

    Functionality:
    Allows user to search for friends and add them
    Friends appear with their name and profile picture

    Navigation:
    Is the main screen after login (or login bypass)
    Can navigate to it via bottom bar
 */

// Home Screen Composable
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
    currentUsername: String = ""
) {
    val context = LocalContext.current // For Toast

    // Get variables from ViewModel
    val foundUsers by homeScreenViewModel.foundUsers.observeAsState(emptyList()) // List of users found from search

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // For search query
        var searchQuery by remember { mutableStateOf("") }

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Welcome text
            item{
                Text("Welcome to WristWave!")
            }
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        // Update found users depending on query
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
            // List of found users
            items(foundUsers.size){ index ->
                // Only display other users to avoid adding yourself as friend
                if (foundUsers[index].username != currentUsername) {
                    // Each row contains profile picture, username and add button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        // Display profile picture if available
                        if (foundUsers[index].image != null) {
                            Image(
                                bitmap = foundUsers[index].image!!.asImageBitmap(),
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        // Username
                        Text(foundUsers[index].username,
                            modifier = Modifier.weight(1f)) // To push add button to right-end of screen
                        // Add friend button
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
}

// Preview will not render with such dependencies




