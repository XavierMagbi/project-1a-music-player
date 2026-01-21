package com.epfl.esl.musicplayer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.epfl.esl.musicplayer.ui.theme.MusicPlayerTheme
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Firebase
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.google.firebase.database.database
import com.google.firebase.storage.storage
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity(),MessageClient.OnMessageReceivedListener {
    private lateinit var dataClient: DataClient
    private var username by mutableStateOf("")
    private var imageUri by mutableStateOf<Uri?>(null)
    private var uriString by mutableStateOf("")
    private var userKey by mutableStateOf("")

    // Request code for image picker intent (to update user image)
    private val IMAGE_PICKER_REQUEST_CODE = 100

    // To update user image
    private val storageRef = Firebase.storage.reference
    private val profileRef = Firebase.database.getReference("Profiles")

    private lateinit var playScreenViewModel: PlayScreenViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dataClient = Wearable.getDataClient(this)

        var initialRoute = "login" // Default start at login
        var shouldShowBarsInit = false // So top and bottom bars are shown in direct login too

        // Wait for ROOM DB to respond
        runBlocking {
            val userDb = DatabaseProvider.getDatabase(this@MainActivity) // Get instance
            val storedUser = userDb.userDao().getUser() // Fetch saved user
            if (storedUser != null){
                initialRoute = "home" // If a user is stored, start at home
                username = storedUser.username // Get username as we do not go through login
                userKey = storedUser.userKey   // Get userKey as we do not go through login
                shouldShowBarsInit = true // Show bars if direct login
                Log.d("MainActivity", "User auto-logged in: ${username}") // For debug
            }
        }

        setContent {
            MusicPlayerTheme {
                // Load profile image from Firebase Storage in case of direct login
                if (username.isNotEmpty()) {
                    LaunchedEffect(username) {
                        // Get reference to profile image
                        val profileImageRef = Firebase.storage.reference
                            .child("ProfileImages/$username.jpg")

                        // Get image URL
                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                            imageUri = uri
                        }
                    }
                }

                val equalizerViewModel: EqualizerViewModel = viewModel()
                playScreenViewModel =  viewModel {
                    PlayScreenViewModel(
                        this@MainActivity.application,
                        equalizerViewModel,
                        dataClient
                    )
                }
                val musicScreenViewModel:MusicScreenViewModel= viewModel()

                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var shouldShowBars by rememberSaveable { mutableStateOf(shouldShowBarsInit) }
                //var showSongBar by remember { mutableStateOf(false) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            // Sign off button
                            NavigationDrawerItem(
                                label = {
                                    Text("Sign out")
                                },
                                icon = {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        shouldShowBars = false
                                        navController.navigate("login") {
                                            popUpTo(navController.graph.id) {
                                                inclusive = true
                                            }
                                        }
                                        // Delete stored user in ROOM DB
                                        val userDb = DatabaseProvider.getDatabase(this@MainActivity)
                                        userDb.userDao().deleteUser()
                                        Log.d("MainActivity", "User deleted from ROOM DB: ${username}")
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            // Update profile picture
                            NavigationDrawerItem(
                                label = {
                                    Text("Update profile picture")
                                },
                                icon = {
                                    Icon(Icons.Default.AccountBox, contentDescription = null)
                                },
                                selected = false,
                                onClick = {
                                    // Call as a coroutine to avoid blocking main thread
                                    scope.launch {
                                        openImagePicker()
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                    }
                ) {
                    Scaffold(
                        topBar = {
                            if (shouldShowBars) {
                                TopAppBar(
                                    navigationIcon = {
                                        IconButton(onClick = {
                                            scope.launch {
                                                drawerState.open()
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Filled.Menu,
                                                contentDescription = getString(
                                                    R.string.menu_icon_content_description
                                                )
                                            )
                                        }
                                    },
                                    title = {
                                        Text(text = stringResource(id = R.string.app_name))
                                    },
                                    actions = {
                                        AsyncImage(
                                            model = imageUri,
                                            contentDescription = "Profile picture",
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(end = 8.dp)
                                        )
                                    }
                                )
                            }
                        },
                        bottomBar = {

                            if (shouldShowBars) {
                                val currentPosition by playScreenViewModel.currentPosition.observeAsState(
                                    0
                                )
                                val duration by playScreenViewModel.duration.observeAsState()
                                val isPlaying by playScreenViewModel.isPlaying.observeAsState()
                                val title by playScreenViewModel.title.observeAsState()
                                val coverImage by playScreenViewModel.coverImage.observeAsState()
                                val painter = if (coverImage != null) {
                                    val bitmap = remember(coverImage) {
                                        BitmapFactory.decodeByteArray(
                                            coverImage,
                                            0,
                                            coverImage!!.size
                                        ).asImageBitmap()
                                    }
                                    BitmapPainter(bitmap)
                                } else {
                                    painterResource(id = R.drawable.defaultplaylist)
                                }
                                val navBackStackEntry by navController.currentBackStackEntryAsState()
                                Column {
                                    if (
                                        playScreenViewModel.currentTrackIndex!=-1
                                        &&  navBackStackEntry?.destination?.route !="musicPlayer"
                                        ) { // Card to be visible only once a music has been picked

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {

                                                    navController.navigate("musicPlayer")
                                                },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    painter = painter,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .padding(8.dp)
                                                )
                                                Text(
                                                    text = title ?: "No Title",
                                                    modifier = Modifier.weight(1f),
                                                    overflow = TextOverflow.Ellipsis,
                                                    maxLines = 1
                                                )
                                                IconButton(onClick = { playScreenViewModel.onPlayPauseClick() }) {
                                                    Icon(
                                                        imageVector = if (isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                        contentDescription = "Play/Pause on card"
                                                    )
                                                }


                                            }

                                            // Time indicator
                                            LinearProgressIndicator(
                                                progress = {
                                                    if ((duration
                                                            ?: 0) > 0
                                                    ) currentPosition.toFloat() / (duration
                                                        ?: 0.1f).toFloat()
                                                    else 0f
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }


                                    }



                                    NavigationBar {
                                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                                        val currentRoute = navBackStackEntry?.destination?.route

                                        NavigationBarItem(
                                            selected = currentRoute == "home",
                                            onClick = {
                                                navController.navigate("home")
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Home,
                                                    contentDescription = getString(
                                                        R.string.home_content_description
                                                    ),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = { Text(getString(R.string.home_navigation_label)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "equalizer",
                                            onClick = {
                                                navController.navigate("equalizer")
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Tune,
                                                    contentDescription = getString(
                                                        R.string.equalizer_content_description
                                                    ),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = { Text(getString(R.string.equalizer_navigation_label)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "discover",
                                            onClick = {
                                                navController.navigate("discover")
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Radio,
                                                    contentDescription = getString(
                                                        R.string.discover_content_description
                                                    ),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            },
                                            label = { Text(getString(R.string.discover_navigation_label)) }
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute == "music",
                                            onClick = {
                                                musicScreenViewModel.showPlaylists()
                                                navController.navigate("music")
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.MusicNote,
                                                    contentDescription = getString(
                                                        R.string.playlists_content_description
                                                    )
                                                )
                                            },
                                            label = { Text(getString(R.string.playlists_navigation_label)) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = initialRoute, // Start at login if no user is logged in or home otherwise
                            modifier = Modifier.padding(innerPadding)
                        )
                        {
                            composable("login") {
                                val context = LocalContext.current
                                LoginProfileScreen(
                                    onNavigateToNewRecording = { loginInfo ->
                                        username = loginInfo.username
                                        imageUri = loginInfo.imageUri
                                        userKey = loginInfo.userKey

                                        if (imageUri == null || username == "") { // Modifiable si on veut autoriser la connexion sans image
                                            Toast.makeText(
                                                context, "Pick an image and a username!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            uriString = URLEncoder.encode(
                                                imageUri.toString(),
                                                StandardCharsets.UTF_8.toString()
                                            )
                                            shouldShowBars = true
                                            navController.navigate("home") {
                                                popUpTo(navController.graph.id) {
                                                    inclusive = true
                                                }
                                            }
                                        }
                                    },
                                    dataClient
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    currentUsername = username
                                )
                            }
                            composable("equalizer") {
                                val audioSessionId by playScreenViewModel.audioSessionId.observeAsState(
                                    0
                                )
                                EqualizerScreen(
                                    equalizerViewModel = equalizerViewModel,
                                    audioSessionId = audioSessionId ?: 0
                                )
                            }
                            composable("discover") {
                                DiscoverScreen(
                                    currentUsername = username,
                                    onSongClicked = { queue,idx ->
                                        playScreenViewModel.changeQueue(queue, idx)

                                        navController.navigate("musicPlayer")

                                    },
                                    onAddQueue = {song->
                                        playScreenViewModel.addToQueue(song)
                                    }
                                )
                            }
                            composable("music") {
                                MusicScreen(
                                    application = this@MainActivity.application,
                                    currentUsername = username,
                                    //playScreenViewModel = playScreenViewModel,
                                    onSongClicked = { idx, queue ->
                                        playScreenViewModel.changeQueue(queue, idx)

                                        navController.navigate("musicPlayer")

                                    },
                                    onAddQueue = {song->
                                        playScreenViewModel.addToQueue(song)

                                    },
                                    musicScreenViewModel = musicScreenViewModel
                                )
                            }
                            composable("musicPlayer") {
                                PlayScreen(
                                    onArrowClicked = {

                                        navController.popBackStack()
                                    },
                                    playScreenViewModel = playScreenViewModel
                                )

                            }
                        }

                    }
                }
            }
        }
    }

    // ==== To update user picture ====

    // Launch intent
    private fun openImagePicker() {
        // Intent to open phone's image gallery and pick an image
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // Start the activity with a request code to identify the result later
        startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE)
    }

    // Once intent is done (override function and call its super method)
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        // Super method call
        super.onActivityResult(requestCode, resultCode, intent)

        // If the aintent was to pick an image and was successful
        if (requestCode == IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the intent's data
            val selectedImageUri = intent?.data
            // If URI is not null => update the profile image
            if (selectedImageUri != null) {
                updateProfileImage(selectedImageUri)
            }
        }
    }

    // Update the profile image URI (inspired from LoginProfileViewModel.kt which itself comes from EE-490(g) labs)
    private fun updateProfileImage(newImageUri: Uri) {

        // For transformations such as scaling
        val matrix = Matrix()
        // Get the image bitmap from the URI
        var imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, newImageUri)
        // Scale down the image to reduce size before upload
        val ratio: Float = 13F
        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), false
        )
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled, 0, 0, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), matrix, true
        )
        // Convert the bitmap to a byte array and compress it as PNG
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        // Upload new image to Firebase Storage
        val profileImageRef = storageRef.child("ProfileImages/" + username + ".jpg")
        val uploadProfileImage = profileImageRef.putBytes(imageByteArray)

        uploadProfileImage.addOnSuccessListener { taskSnapshot ->
            // Update the image URI
            imageUri = newImageUri
            // Update the photo_URL in the Realtime Database  
            profileRef.child(userKey).child("photo_URL").setValue(
                storageRef.toString() + "ProfileImages/" + username + ".jpg"
            )
        }
    }



    override fun onMessageReceived(messageEvent: MessageEvent) {

        if(messageEvent.path == "PlayPause"){
            playScreenViewModel.onPlayPauseClick()
        }
        if(messageEvent.path == "LeftArrow"){
            playScreenViewModel.onLeftArrowClick()
        }
        if(messageEvent.path == "RightArrow"){
            playScreenViewModel.onRightArrowClick()
        }

    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Log.d("Main Activity","addListener attached")
    }
    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        Log.d("Main Activity","removedListener attached")
    }



}



