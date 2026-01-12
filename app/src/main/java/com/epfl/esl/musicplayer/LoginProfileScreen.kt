package com.epfl.esl.musicplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
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

@Composable
fun LoginProfileScreen(
    onNavigateToNewRecording: ((LoginInfo) -> Unit),
    dataClient: DataClient,
    modifier: Modifier = Modifier,
    loginProfileViewModel: LoginProfileViewModel = viewModel()
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var isLoading by remember { mutableStateOf(false) }
        val username by loginProfileViewModel.username.observeAsState(initial = "")
        val password by loginProfileViewModel.password.observeAsState(initial = "")
        val imageUri by loginProfileViewModel.imageUri.observeAsState(initial = null)
        val resultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    loginProfileViewModel.updateImageUri(uri)
                }
            }
        )

        val context = LocalContext.current

        LoginProfileContent(
            imageUri = imageUri,
            username = username,
            password = password,
            onUsernameChanged = { newValue -> loginProfileViewModel.updateUsername(newValue) },
            onPasswordChanged = { newValue -> loginProfileViewModel.updatePassword(newValue) },
            onSignInButtonClicked = {
                isLoading = false
                loginProfileViewModel.fetchProfile()
            },
            onSignUpButtonClicked = {
                loginProfileViewModel.sendDataToFireBase(context)
                loginProfileViewModel.sendDataToWear(context, dataClient)
                val userData = LoginInfo(
                    loginProfileViewModel.username.value ?: "",
                    loginProfileViewModel.imageUri.value,
                    loginProfileViewModel.key
                )
                onNavigateToNewRecording(userData)

            },
            onPickImageButtonClicked = {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                resultLauncher.launch(intent)
            },
            modifier
        )

        val uploadSuccess by loginProfileViewModel.uploadSuccess
            .observeAsState(initial = true)
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

        val userImageLoadingFinished by loginProfileViewModel.userImageLoadingFinished
            .observeAsState()
        userImageLoadingFinished?.let {
            val loginInfo = LoginInfo(
                loginProfileViewModel.username.value ?: "",
                loginProfileViewModel.imageUri.value,
                loginProfileViewModel.key
            )
            loginProfileViewModel.sendDataToWear(context, dataClient, true)
            onNavigateToNewRecording(loginInfo)
        }

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.picked_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clickable {
                        onPickImageButtonClicked()
                    }
            )
        }
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
        Button(
            onClick = {
                if (username.isBlank()) {
                    Toast.makeText(context, "Enter username", Toast.LENGTH_SHORT).show()
                } else if (password.isBlank()) {
                    Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                } else {
                    onSignInButtonClicked()
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = stringResource(R.string.sign_in_button_text))
        }
        OutlinedButton(
            onClick = {
                    scope.launch {
                        if (username.isBlank()) {
                            Toast.makeText(context, "Enter username", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (password.isBlank()) {
                            Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        if (!loginProfileViewModel.isPasswordValid(password)) {
                            Toast.makeText(
                                context,
                                "Password must include upper+lower case, a digit, and one of ! , + ^",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        if (imageUri == null) {
                            Toast.makeText(context, "Pick an image", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Only now call Firebase
                        val available = loginProfileViewModel.isUsernameAvailable()
                        if (!available) {
                            Toast.makeText(context, "Username already exists , please try another one", Toast.LENGTH_LONG)
                                .show()
                            return@launch
                        }

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




@Preview
@Composable
fun LoginProfileScreenPreview() {
    MusicPlayerTheme {
        val context = LocalContext.current
        val dataClient = Wearable.getDataClient(context)
        LoginProfileScreen({}, dataClient )
    }
}

@Preview
@Composable
fun LoginProfileContentPreview() {
    MusicPlayerTheme {
        LoginProfileContent(null, "", "", {}, {}, {}, {}, {})
    }
}
