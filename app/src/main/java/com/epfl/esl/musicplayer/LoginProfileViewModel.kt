package com.epfl.esl.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

import java.io.ByteArrayOutputStream

class LoginProfileViewModel : ViewModel(){

    private var _username = MutableLiveData<String>("")
    private var _password = MutableLiveData<String>("")
    private var _imageUri = MutableLiveData<Uri?>(null)

    var storageRef = FirebaseStorage.getInstance().getReference()

    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?>
        get() = _uploadSuccess
    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent

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

    fun sendDataToFireBase(context: Context?) {
        key = profileRef.push().key.toString()
        profileRef.child(key).child("username").setValue(_username.value)
        profileRef.child(key).child("password").setValue(_password.value)
    }


    fun sendDataToWear(context: Context?, dataClient: DataClient) {

        val matrix = Matrix()

        val profileImageRef = storageRef.child("ProfileImages/" + username.value+ ".jpg")


        //1

        var imageBitmap = MediaStore.Images.Media.getBitmap(
            context?.contentResolver,
            _imageUri.value
        )

        //2
        var ratio: Float = 13F

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

        //4
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()

        val request: PutDataRequest = PutDataMapRequest.create("/userInfo").run {
            dataMap.putByteArray("profileImage", imageByteArray)
            dataMap.putString("username", _username.value ?: "")
            asPutDataRequest()
        }

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



        request.setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)
    }
}