package com.epfl.esl.musicplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.epfl.esl.musicplayer.ui.theme.MusicPlayerTheme
import com.google.android.gms.wearable.DataClient
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var dataClient: DataClient
    private var username by mutableStateOf("")
    private var imageUri by mutableStateOf<Uri?>(null)
    private var uriString by mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var shouldShowBottomMenu by remember { mutableStateOf(true) }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            NavigationDrawerItem(
                                label = {
                                    Text(text = getString(R.string.about_navigation_drawer_item_text))
                                },
                                icon = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = getString(
                                            R.string.about_icon_description
                                        ),
                                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                    )
                                },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        //navController.navigate("about")
                                        drawerState.close()
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(text = stringResource(id = R.string.app_name))
                                },
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
                                }
                            )
                        },
                        bottomBar = {
                            if (shouldShowBottomMenu) {
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
                                                imageVector = Icons.Filled.Home ,
                                                contentDescription = getString(
                                                    R.string.home_content_description
                                                )
                                            )
                                        },
                                        label = { Text(getString(R.string.home_navigation_label)) }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute == "player",
                                        onClick = {
                                            navController.navigate("player")
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircleFilled ,
                                                contentDescription = getString(
                                                    R.string.player_content_description
                                                )
                                            )
                                        },
                                        label = { Text(getString(R.string.player_navigation_label)) }
                                    )


                                }
                            }
                        }
                    ){
                            innerPadding ->
                        NavHost(navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding))
                        {
                            composable("home") {
                                HomeScreen(onPlayerClicked = { })
                            }
                            composable("player") {
                                PlayScreen()
                            }
                        }

                    }
                }
            }
        }
    }
}

