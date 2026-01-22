package com.epfl.esl.musicplayer

import android.graphics.Bitmap

// ==== Data Classes ====

// Data class to hold user profile information to be displayed in HomeScreen
data class UserProfile(
    val username: String,
    val image: Bitmap? = null
)

// Data class to hold music metadata to be displayed in DiscoverScreen and used for playback
data class musicMetadata (
    val title: String? = "",
    val image: ByteArray? = null,
    val link: String? = "",
    val linkGS: String? = "",
    val datapath: String=""
)