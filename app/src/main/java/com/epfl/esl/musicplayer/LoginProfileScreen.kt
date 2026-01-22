package com.epfl.esl.musicplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.epfl.esl.musicplayer.ui.theme.MusicPlayerTheme
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.DataClient
import kotlinx.coroutines.launch

/*
    Login Screen Composable
    Code inspired by EE-490(g) labs

    Functionality:
    Allows new user to sign up with username, password and profile image
    Allows existing user to sign in with username and password

    Navigation:
    Default screen in case a user is not already logged in
    User has signed off through drawable menu
 */

// Login Screen Composable
@Composable
fun LoginProfileScreen(
    onNavigateToNewRecording: ((LoginInfo) -> Unit),            // To pass user info to rest of app
    dataClient: DataClient,                                     // To send data to Wear module
    modifier: Modifier = Modifier,                               
    loginProfileViewModel: LoginProfileViewModel = viewModel()
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Composable variables
        var isLoading by remember { mutableStateOf(false) } // For UI load animation
        val context = LocalContext.current

        // To collect image from image picker
        // From EE-490(g) labs to get result from intent
        val resultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    loginProfileViewModel.updateImageUri(uri)
                }
            }
        )
        
        // Get variables from ViewModel
        val username by loginProfileViewModel.username.observeAsState(initial = "")
        val password by loginProfileViewModel.password.observeAsState(initial = "")
        val imageUri by loginProfileViewModel.imageUri.observeAsState(initial = null)

        val uploadSuccess by loginProfileViewModel.uploadSuccess.observeAsState(initial = true)
        if (uploadSuccess == false) {
            Toast.makeText(
                context,
                "Profile image upload to firebase failed.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val profilePresent by loginProfileViewModel.profilePresent.observeAsState()
        profilePresent?.let { present ->
            if (present) {
                loginProfileViewModel.loadUserImageUri(context)
            } else {
                isLoading = false
                Toast.makeText(context, "Incorrect username/password",
                    Toast.LENGTH_LONG).show()
                loginProfileViewModel.resetProfilePresent()
            }
        }

        val userImageLoadingFinished by loginProfileViewModel.userImageLoadingFinished.observeAsState()
        userImageLoadingFinished?.let {
            val loginInfo = LoginInfo(
                loginProfileViewModel.username.value ?: "",
                loginProfileViewModel.imageUri.value,
                loginProfileViewModel.key
            )
            onNavigateToNewRecording(loginInfo)
        }
        
        // Set up Login Profile Content Composable
        LoginProfileContent(
            imageUri = imageUri,
            username = username,
            password = password,
            onUsernameChanged = { newValue -> loginProfileViewModel.updateUsername(newValue) }, // Update username textfield entered value
            onPasswordChanged = { newValue -> loginProfileViewModel.updatePassword(newValue) }, // Update password textfield entered value
            onSignInButtonClicked = { // Fetch profile from Firebase in case of sign in
                isLoading = true
                loginProfileViewModel.fetchProfile(context)
            },
            onSignUpButtonClicked = { 
                isLoading = true
                loginProfileViewModel.sendDataToFireBase(context)           // Register user to Firebase
                val userData = LoginInfo(                                   // Prepare user data to send to rest of app
                    loginProfileViewModel.username.value ?: "",
                    loginProfileViewModel.imageUri.value,
                    loginProfileViewModel.key
                )
                onNavigateToNewRecording(userData)                          // Navigate to rest of app
            },
            onPickImageButtonClicked = {                                    // Launch image picker intent
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                resultLauncher.launch(intent)
            },
            modifier
        )

        // Show loading animation if isLoading is true
        if (isLoading) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background
                .copy(alpha = 0.5f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

// Login Profile Content Composable
@Composable
fun LoginProfileContent(
    imageUri: Uri?,     
    username: String,
    password: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInButtonClicked: () -> Unit,
    onSignUpButtonClicked: () -> Unit,
    onPickImageButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    loginProfileViewModel: LoginProfileViewModel = viewModel()
) {
    // Composable variables
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // For launching coroutines

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(R.string.app_logo_description),
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(bottom = 8.dp)
        )
        // If no image selected, show default image
        if (imageUri == null) {
            Image(
                painter = painterResource(id = R.drawable.pick_image),
                contentDescription = stringResource(R.string.default_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable {
                        onPickImageButtonClicked()
                    }
            )
        // Else show selected image
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.picked_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable {
                        onPickImageButtonClicked()
                    }
            )
        }
        // Username input field
        TextField(
            value = username,
            onValueChange = onUsernameChanged,
            label = {
                Text(stringResource(R.string.username_hint))
            },
            textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        // Password input field
        TextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = {
                Text(stringResource(R.string.password_hint))
            },
            textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = modifier.fillMaxWidth()
        )
        // Sign In button
        Button(
            onClick = {
                // If no username has been entered
                if (username.isBlank()) {
                    Toast.makeText(context, "Enter username", Toast.LENGTH_SHORT).show()
                // If no password has been entered
                } else if (password.isBlank()) {
                    Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                // Else proceed with sign in
                } else {
                    Log.d("LoginScreen", "Sign In button CLICKED!")
                    onSignInButtonClicked()
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = stringResource(R.string.sign_in_button_text))
        }
        // Sign Up button
        OutlinedButton(
            onClick = {
                scope.launch {
                    // If no username has been entered
                    if (username.isBlank()) {
                        Toast.makeText(context, "Enter username", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    // If no password has been entered
                    if (password.isBlank()) {
                        Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    // Else proceed to validate password strength
                    if (!loginProfileViewModel.isPasswordValid(password)) {
                        Toast.makeText(
                            context,
                            "Password must include min. 8 long, upper+lower case, a digit, and one of: ! , + ^",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    // If no image has been selected
                    if (imageUri == null) {
                        Toast.makeText(context, "Pick an image", Toast.LENGTH_SHORT).show()
                        return@launch
                    }                   
                    // Check if username is available
                    val available = loginProfileViewModel.isUsernameAvailable()
                    if (!available) {
                        Toast.makeText(context, "Username already exists , please try another one", Toast.LENGTH_LONG)
                            .show()
                        return@launch
                    }
                    Log.d("LoginScreen", "Sign Up button CLICKED!")
                    // If all checks out proceed with sign up
                    onSignUpButtonClicked()
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = stringResource(R.string.sign_up_button_text))
        }
    }
}



// === Previews ===

// Login Profile Screen Preview
@Preview
@Composable
fun LoginProfileScreenPreview() {
    MusicPlayerTheme {
        val context = LocalContext.current
        val dataClient = Wearable.getDataClient(context)
        LoginProfileScreen({}, dataClient )
    }
}

// Login Profile Content Preview
@Preview
@Composable
fun LoginProfileContentPreview() {
    MusicPlayerTheme {
        LoginProfileContent(null, "", "", {}, {}, {}, {}, {})
    }
}
