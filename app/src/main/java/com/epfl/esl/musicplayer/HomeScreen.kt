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
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.runtime.LaunchedEffect

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeScreenViewModel: HomeScreenViewModel = viewModel()
) {
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
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }

                //  Fetch image from Firebase Storage
                LaunchedEffect(foundUsers[index].picturePath) {
                    val storage = FirebaseStorage.getInstance()
                    val path = foundUsers[index].picturePath

                    // Firebase requires relative path not complete path
                    val filePath = path.replace("gs://muzikproject1a.firebasestorage.app/", "")
                    // Fetch in bytes
                    storage.reference.child(filePath).getBytes(Long.MAX_VALUE)
                        .addOnSuccessListener { bytes ->
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (bitmap != null) { // Once image has been fetched and converted to bitmap
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Profile picture",
                            modifier = Modifier
                                .size(50.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    Text(foundUsers[index].username)
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




