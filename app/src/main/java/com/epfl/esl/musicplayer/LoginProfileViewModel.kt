package com.epfl.esl.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import coil.Coil
import coil.request.ImageRequest
import coil.size.Size
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
    Login Profile ViewModel

    Functionality:
    Handles user sign in/up

    Interacts with:
    Firebase Realtime Database - to register/fetch existing user profiles
    Firebase Storage - to store/fetch profile images
 */

// ViewModel for LoginProfileScreen
class LoginProfileViewModel : ViewModel(){

    // ViewModel LiveData variables
    private var _username = MutableLiveData<String>("")
    val username: LiveData<String>
        get() = _username
    private var _password = MutableLiveData<String>("")
    val password: LiveData<String>
        get() = _password
    private var _imageUri = MutableLiveData<Uri?>(null)
    val imageUri: LiveData<Uri?>
        get() = _imageUri
    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?>
        get() = _uploadSuccess
    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent
    private val _userImageLoadingFinished = MutableLiveData<Boolean?>()
    val userImageLoadingFinished: LiveData<Boolean?>
        get() = _userImageLoadingFinished

    // Firebase Realtime Database and Storage references
    var storageRef = FirebaseStorage.getInstance().getReference()
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")

    var key: String= ""
    private var downloadedImageDrawable: Drawable? = null

    // Functions to update LiveData variables
    fun updateUsername(username: String) {
        _username.postValue(username)
    }
    fun updatePassword(password: String) {
        _password.postValue(password)
    }
    fun updateImageUri(imageUri: Uri?) {
        _imageUri.postValue(imageUri)
    }

    // Password validation functions defining requirements
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

    // Check if a username free
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

    // Send new profile data to Firebase Realtime Database and Storage (for sign up)
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
            profileRef.child(key).child("photo_URL").setValue(
                (FirebaseStorage.getInstance()
                    .getReference()).toString() + "ProfileImages/" + username.value
                        + ".jpg")
            _uploadSuccess.value = true
        }
    }

    // For sign in to fetch data from Firebase Realtime Database
    // Checks if profile exists and if password matches
    fun fetchProfile(context: Context) {
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    // Get username from database and compare if exists
                    val usernameDatabase = user.child("username")
                        .getValue(String::class.java)
                    if (usernameDatabase != null &&
                        _username.value == usernameDatabase) {
                        // If username exists, check password
                        val passwordDatabase = user.child("password")
                            .getValue(String::class.java)
                        if (passwordDatabase != null &&
                            _password.value == passwordDatabase) {
                            key = user.key.toString()
                            _profilePresent.value = true
                            
                            // For StayConnected feature
                            // Use coroutine to avoid blocking main thread
                            CoroutineScope(Dispatchers.IO).launch {
                                // Get the database instance/generate it if does not exist yet
                                val userDb = DatabaseProvider.getDatabase(context)
                                userDb.userDao().deleteUser() // Clear previous user
                                userDb.userDao().insertUser(  // Insert the logged in user
                                    User(
                                        id = 0,
                                        username = _username.value ?: "",
                                        userKey = key
                                    )
                                )
                                Log.d("LoginProfileVM", "User stored in ROOM DB")
                            }
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

    // Reset the profile presence status
    fun resetProfilePresent() {
        _profilePresent.value = null
    }

    // Load user profile image from Firebase Storage for display after sign in
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
