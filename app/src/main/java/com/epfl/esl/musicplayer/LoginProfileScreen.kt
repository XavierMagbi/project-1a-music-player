package com.epfl.esl.musicplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
fun LoginProfileScreen(
    onEnterButtonClicked: ((LoginInfo) -> Unit),
    dataClient: DataClient,
    modifier: Modifier = Modifier,
    loginProfileViewModel: LoginProfileViewModel = viewModel()
) {

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val context = LocalContext.current

        val username by loginProfileViewModel.username.observeAsState(initial = "")
        val password by loginProfileViewModel.password.observeAsState(initial = "")
        val imageUri by loginProfileViewModel.imageUri.observeAsState(initial = null)


        var isEditingMode by remember { mutableStateOf(true) }


        var resultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    loginProfileViewModel.updateImageUri(uri)
                }
            }
        )

        if (isEditingMode) {
            LoginProfileContentEditing(
                username = username,
                password = password,
                onUsernameChanged = { newValue -> loginProfileViewModel.updateUsername(newValue) },
                onPasswordChanged = { newValue -> loginProfileViewModel.updatePassword(newValue) },
                onContinueButtonClicked = { isEditingMode = false },
                onPickImageButtonClicked = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"
                    resultLauncher.launch(intent)
                },
                imageUri,
                modifier
            )
        } else {
            LoginProfileContentDisplaying(
                username = username,
                onUpdateButtonClicked = {
                    isEditingMode = true
                },
                onLogOutButtonClicked = {
                    loginProfileViewModel.updateUsername("")
                    loginProfileViewModel.updatePassword("")
                    loginProfileViewModel.updateImageUri(null)
                    isEditingMode = true
                },
                onEnterButtonClicked = { loginInfo ->
                    loginProfileViewModel
                        .sendDataToWear(context.applicationContext, dataClient)
                    onEnterButtonClicked(loginInfo)
                },
                imageUri,
                modifier
            )
        }
    }
}

@Composable
fun LoginProfileContentEditing(
    username: String,
    password: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onContinueButtonClicked: () -> Unit,
    onPickImageButtonClicked: () -> Unit,
    imageUri: Uri? = Uri.EMPTY,
    modifier: Modifier = Modifier
){

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (imageUri == null) {
            Image(
                painter = painterResource(id = R.drawable.user_image),
                contentDescription = stringResource(R.string.default_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.picked_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
        TextField(
            value = username,
            onValueChange = onUsernameChanged,
            label = {
                Text(stringResource(R.string.username_hint))
            },
            textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        TextField(
            value = password,
            onValueChange = onPasswordChanged,
            textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            label = {
                Text(stringResource(R.string.password_hint))
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Button(
                onClick = onContinueButtonClicked,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.confirm_button_text))
            }
            Button(
                onClick = onPickImageButtonClicked,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.pick_image_button_text))
            }
        }
    }
}



@Composable
fun LoginProfileContentDisplaying(
    username: String,
    onUpdateButtonClicked: () -> Unit,
    onLogOutButtonClicked: () -> Unit,
    onEnterButtonClicked: (LoginInfo) -> Unit,
    imageUri: Uri? = Uri.EMPTY,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (imageUri == null) {
            Image(
                painter = painterResource(id = R.drawable.user_image),
                contentDescription = stringResource(R.string.default_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        } else {
            AsyncImage(
                model = imageUri,
                contentDescription = stringResource(R.string.picked_user_image),
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
        Text(
            text = username,
            style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.password_hidden_text),
            style = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            modifier = modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Button(
                onClick = onUpdateButtonClicked,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.update_button_text))
            }
            Button(
                onClick = onLogOutButtonClicked,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.log_out_button_text))
            }
            Button(
                onClick = {
                    val loginInfo = LoginInfo(username, imageUri)
                    onEnterButtonClicked(loginInfo)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.enter_button_text))
            }
        }
    }
}


@Preview
@Composable
fun LoginProfileScreenPreview() {
    MusicPlayerTheme {
        val context = LocalContext.current
        val dataClient = Wearable.getDataClient(context)
        LoginProfileScreen({}, dataClient)
    }
}

@Preview
@Composable
fun LoginProfileDisplayingPreview() {
    MusicPlayerTheme {
        LoginProfileContentDisplaying("username", {}, {}, {})
    }
}
@Preview
@Composable
fun LoginProfileSEditingPreview() {
    MusicPlayerTheme {
        LoginProfileContentEditing("", "", {}, {}, {}, {})
    }
}
