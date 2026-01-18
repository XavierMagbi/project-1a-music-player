package com.epfl.esl.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF.length
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import coil.Coil
import coil.request.ImageRequest
import coil.size.Size
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import java.io.ByteArrayOutputStream

class LoginProfileViewModel : ViewModel(){

    private var _username = MutableLiveData<String>("")
    private var _password = MutableLiveData<String>("")
    private var _imageUri = MutableLiveData<Uri?>(null)
    private val _userImageLoadingFinished = MutableLiveData<Boolean?>()

    private var downloadedImageDrawable: Drawable? = null

    var storageRef = FirebaseStorage.getInstance().getReference()

    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?>
        get() = _uploadSuccess
    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent
    val userImageLoadingFinished: LiveData<Boolean?>
        get() = _userImageLoadingFinished

    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")
    var key: String= ""

    val username: LiveData<String>
        get() = _username
    val password: LiveData<String>
        get() = _password
    val imageUri: LiveData<Uri?>
        get() = _imageUri

    fun updateUsername(username: String) {
        _username.postValue(username)
    }
    fun updatePassword(password: String) {
        _password.postValue(password)
    }
    fun updateImageUri(imageUri: Uri?) {
        _imageUri.postValue(imageUri)
    }

    fun String.isLongEnough() = length >= 8
    fun String.hasEnoughDigits() = count(Char::isDigit) > 0
    fun String.isMixedCase() = any(Char::isLowerCase) && any(Char::isUpperCase)
    fun String.hasSpecialChar() = any { it in "!,+^" }


    fun isPasswordValid(password:String):Boolean{
        if(password.isLongEnough() && password.hasEnoughDigits() && password.isMixedCase() && password.hasSpecialChar()){
            return true;
        }

        return false
    }

    suspend fun isUsernameAvailable(): Boolean {
        val usernameToCheck = _username.value?.trim()
        if (usernameToCheck.isNullOrEmpty()) return false

        return suspendCancellableCoroutine { cont ->
            val query = profileRef
                .orderByChild("username")
                .equalTo(usernameToCheck)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (cont.isActive) {
                        // available if no matching username exists
                        cont.resume(!snapshot.exists())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }



    fun sendDataToFireBase(context: Context?) {
        key = profileRef.push().key.toString()
        profileRef.child(key).child("username").setValue(_username.value)
        profileRef.child(key).child("password").setValue(_password.value)

        val profileImageRef = storageRef.child("ProfileImages/" + username.value + ".jpg")

        /** format the image **/
        val matrix = Matrix()
        var imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver,
            _imageUri.value)
        val ratio: Float = 13F
        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), false)
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled, 0, 0, (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), matrix, true)
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        /** upload it to Firebase storage **/
        val uploadProfileImage = profileImageRef.putBytes(imageByteArray)

        uploadProfileImage.addOnFailureListener {
            _uploadSuccess.value = false
        }.addOnSuccessListener { taskSnapshot ->
            profileRef.child(key).child("photo URL").setValue(
                (FirebaseStorage.getInstance()
                    .getReference()).toString() + "ProfileImages/" + username.value
                        + ".jpg")
            _uploadSuccess.value = true
        }
    }


    fun sendDataToWear(context: Context?, dataClient: DataClient, isLogin: Boolean = false) {
        val matrix = Matrix()
        var ratio: Float?
        var imageBitmap: Bitmap?
        if (!isLogin) {
            imageBitmap = MediaStore.Images.Media
                .getBitmap(context?.contentResolver, _imageUri.value)
            ratio = 13F
        } else {
            imageBitmap = downloadedImageDrawable?.toBitmap()
            ratio = 1F
        }
        if (imageBitmap == null) {
            return
        }

        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(),
            false
        )

        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled, 0, 0,
            (imageBitmap.width / ratio).toInt(),
            (imageBitmap.height / ratio).toInt(), matrix, true
        )

        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/userInfo").run {
            dataMap.putLong("timestamp", System.currentTimeMillis())
            dataMap.putByteArray("profileImage", imageByteArray)
            dataMap.putString("username", _username.value ?: "")
            asPutDataRequest()
        }

        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
    }



    fun fetchProfile() {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username")
                        .getValue(String::class.java)
                    if (usernameDatabase != null &&
                        _username.value == usernameDatabase) {
                        val passwordDatabase = user.child("password")
                            .getValue(String::class.java)
                        if (passwordDatabase != null &&
                            _password.value == passwordDatabase) {
                            key = user.key.toString()
                            _profilePresent.value = true
                            break
                        }
                    }
                }
                if (_profilePresent.value != true) {
                    _profilePresent.value = false
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    fun resetProfilePresent() {
        _profilePresent.value = null
    }

    fun loadUserImageUri(context: Context) {
        storageRef.child("ProfileImages/" + _username.value + ".jpg")
            .downloadUrl.addOnSuccessListener { uri ->
                _imageUri.value = uri
                val imageLoader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(Size.ORIGINAL)
                    .target {
                        downloadedImageDrawable = it
                        _userImageLoadingFinished.value = true
                    }
                    .build()
                imageLoader.enqueue(request)
            }.addOnFailureListener {
                _userImageLoadingFinished.value = true
            }
    }
}