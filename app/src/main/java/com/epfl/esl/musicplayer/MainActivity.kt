package com.epfl.esl.musicplayer

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Radio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private lateinit var dataClient: DataClient
    private var username by mutableStateOf("")
    private var imageUri by mutableStateOf<Uri?>(null)
    private var uriString by mutableStateOf("")
    private var userKey by mutableStateOf("")


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dataClient = Wearable.getDataClient(this)

        setContent {
            MusicPlayerTheme {
                val equalizerViewModel: EqualizerViewModel = viewModel()
                val playScreenViewModel: PlayScreenViewModel = viewModel {
                    PlayScreenViewModel(this@MainActivity.application, equalizerViewModel)
                }

                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var shouldShowBars by remember { mutableStateOf(false) }
                var showSongBar by remember { mutableStateOf(false) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
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
                                val currentPosition by playScreenViewModel.currentPosition.observeAsState(0)
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
                                    painterResource(id = R.drawable.ic_launcher_foreground)
                                }

                                Column {
                                    if (showSongBar == true) { // Card to be visible only once a music has been picked

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showSongBar=false
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
                                                    text = title?:"No Title",
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
                                                    if ((duration?:0) > 0) currentPosition.toFloat() / (duration?:0.1f).toFloat()
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
                    ){
                            innerPadding ->
                        NavHost(navController = navController,
                            startDestination = "login",
                            modifier = Modifier.padding(innerPadding))
                        {
                            composable("login") {
                                val context = LocalContext.current
                                LoginScreen(
                                    onNavigateToNewRecording = { loginInfo ->
                                        username = loginInfo.username
                                        imageUri = loginInfo.imageUri
                                        userKey = loginInfo.userKey

                                        if (imageUri == null ||username == "") { // Modifiable si on veut autoriser la connexion sans image
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
                                val audioSessionId by playScreenViewModel.audioSessionId.observeAsState(0)
                                EqualizerScreen(
                                    equalizerViewModel = equalizerViewModel,
                                    audioSessionId = audioSessionId ?: 0
                                )
                            }
                            composable("discover") {
                                DiscoverScreen()
                            }
                            composable("music") {
                                MusicScreen(
                                    application = this@MainActivity.application,
                                    currentUsername = username,
                                    playScreenViewModel = playScreenViewModel,
                                    onSongClicked={idx,queue->
                                        playScreenViewModel.changeQueue(queue,idx)
                                        showSongBar=false
                                        navController.navigate("musicPlayer")

                                    }
                                )
                            }
                            composable("musicPlayer"){
                                PlayScreen(
                                    onArrowClicked = {
                                        showSongBar=true
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
}
